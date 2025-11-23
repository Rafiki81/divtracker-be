package com.rafiki18.divtracker_be.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rafiki18.divtracker_be.marketdata.FinnhubClient;
import com.rafiki18.divtracker_be.model.InstrumentFundamentals;
import com.rafiki18.divtracker_be.model.InstrumentFundamentals.DataQuality;
import com.rafiki18.divtracker_be.model.InstrumentFundamentals.DataSource;
import com.rafiki18.divtracker_be.repository.InstrumentFundamentalsRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing instrument fundamentals snapshots.
 * Handles caching, refreshing, and fallback when Finnhub is unavailable.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InstrumentFundamentalsService {

    private final InstrumentFundamentalsRepository fundamentalsRepository;
    private final FinnhubClient finnhubClient;

    private static final int STALE_HOURS = 24;

    /**
     * Get fundamentals for a ticker, fetching from Finnhub if needed.
     * Strategy:
     * 1. Check cache
     * 2. If fresh (< 24h), return cached
     * 3. If stale or missing, fetch from Finnhub
     * 4. If Finnhub fails, return stale data (better than nothing)
     *
     * @param ticker Stock ticker symbol
     * @return Fundamentals data, or empty if no data available
     */
    @Transactional
    public Optional<InstrumentFundamentals> getFundamentals(String ticker) {
        String normalizedTicker = ticker.trim().toUpperCase();
        log.debug("Getting fundamentals for ticker: {}", normalizedTicker);

        // Check cache first
        Optional<InstrumentFundamentals> cached = fundamentalsRepository
                .findByTickerIgnoreCase(normalizedTicker);

        if (cached.isPresent()) {
            InstrumentFundamentals fundamentals = cached.get();
            
            // If fresh, return immediately
            if (!fundamentals.isStale()) {
                log.debug("Returning fresh cached fundamentals for {}", normalizedTicker);
                return cached;
            }
            
            log.debug("Cached fundamentals for {} are stale, will try to refresh", normalizedTicker);
        }

        // Try to fetch from Finnhub
        if (finnhubClient.isEnabled()) {
            try {
                Optional<InstrumentFundamentals> fresh = fetchFromFinnhub(normalizedTicker);
                
                if (fresh.isPresent()) {
                    log.info("Successfully fetched and cached fresh fundamentals for {}", normalizedTicker);
                    return fresh;
                }
            } catch (Exception e) {
                log.warn("Failed to fetch fundamentals from Finnhub for {}: {}", 
                        normalizedTicker, e.getMessage());
            }
        } else {
            log.debug("Finnhub is disabled, using cached data");
        }

        // Fallback: return stale data if available
        if (cached.isPresent()) {
            InstrumentFundamentals stale = cached.get();
            stale.setDataQuality(DataQuality.STALE);
            fundamentalsRepository.save(stale);
            
            log.warn("Returning stale fundamentals for {} (last updated: {})", 
                    normalizedTicker, stale.getLastUpdatedAt());
            return Optional.of(stale);
        }

        log.warn("No fundamentals available for {}", normalizedTicker);
        return Optional.empty();
    }

    /**
     * Force refresh fundamentals from Finnhub.
     *
     * @param ticker Stock ticker symbol
     * @return Refreshed fundamentals, or empty if fetch failed
     */
    @Transactional
    public Optional<InstrumentFundamentals> refreshFundamentals(String ticker) {
        String normalizedTicker = ticker.trim().toUpperCase();
        log.info("Force refreshing fundamentals for {}", normalizedTicker);

        if (!finnhubClient.isEnabled()) {
            log.warn("Cannot refresh fundamentals: Finnhub is disabled");
            return fundamentalsRepository.findByTickerIgnoreCase(normalizedTicker);
        }

        return fetchFromFinnhub(normalizedTicker);
    }

    /**
     * Fetch complete fundamentals from Finnhub and save to cache.
     */
    private Optional<InstrumentFundamentals> fetchFromFinnhub(String ticker) {
        log.debug("Fetching fundamentals from Finnhub for {}", ticker);

        try {
            // Fetch all data in parallel
            Optional<Map<String, Object>> profile = finnhubClient.fetchCompanyProfile(ticker);
            Optional<Map<String, Object>> quote = finnhubClient.fetchQuote(ticker);
            Optional<Map<String, Object>> metricsResponse = finnhubClient.fetchAllMetrics(ticker);

            // Extract metrics map
            Optional<Map<?, ?>> metrics = metricsResponse
                    .map(m -> m.get("metric"))
                    .filter(Map.class::isInstance)
                    .map(m -> (Map<?, ?>) m);

            // Build fundamentals entity
            InstrumentFundamentals.InstrumentFundamentalsBuilder builder = InstrumentFundamentals.builder()
                    .ticker(ticker)
                    .source(DataSource.FINNHUB)
                    .lastUpdatedAt(LocalDateTime.now());

            // Profile data
            profile.ifPresent(p -> {
                builder.companyName(getString(p, "name"));
                builder.exchange(getString(p, "exchange"));
                builder.currency(getString(p, "currency"));
                builder.sector(getString(p, "finnhubIndustry"));
                builder.sharesOutstanding(getLong(p, "shareOutstanding"));
                builder.marketCapitalization(getBigDecimal(p, "marketCapitalization"));
            });

            // Quote data
            quote.ifPresent(q -> {
                builder.currentPrice(getBigDecimal(q, "c"));
                builder.priceChange(getBigDecimal(q, "d"));
                builder.priceChangePercent(getBigDecimal(q, "dp"));
                builder.previousClose(getBigDecimal(q, "pc"));
                builder.high52Week(getBigDecimal(q, "h"));
                builder.low52Week(getBigDecimal(q, "l"));
            });

            // Metrics data
            metrics.ifPresent(m -> {
                // Valuation
                builder.peTTM(getBigDecimal(m, "peTTM"));
                builder.priceToBook(getBigDecimal(m, "pbAnnual"));
                builder.priceToSales(getBigDecimal(m, "psAnnual"));

                // Risk
                builder.beta(getBigDecimal(m, "beta"));

                // Profitability
                builder.epsTTM(getBigDecimal(m, "epsTTM"));
                builder.operatingMargin(getBigDecimal(m, "operatingMarginTTM"));
                builder.profitMargin(getBigDecimal(m, "netProfitMarginTTM"));
                builder.roe(getBigDecimal(m, "roeTTM"));
                builder.roa(getBigDecimal(m, "roaTTM"));

                // Dividends
                builder.dividendYield(getBigDecimal(m, "dividendYieldIndicatedAnnual"));
                builder.dividendPerShare(getBigDecimal(m, "dividendPerShareAnnual"));
                builder.payoutRatio(getBigDecimal(m, "payoutRatioAnnual"));
            });

            // Calculate FCF per share manually (Finnhub doesn't provide these fields directly)
            // FCF per share = freeCashFlow / shareOutstanding
            profile.ifPresent(p -> {
                Long sharesOutstanding = getLong(p, "shareOutstanding");
                if (sharesOutstanding != null && sharesOutstanding > 0) {
                    metrics.ifPresent(m -> {
                        // FCF TTM per share
                        BigDecimal fcfTTM = getBigDecimal(m, "freeCashFlowTTM");
                        if (fcfTTM != null) {
                            BigDecimal fcfPerShareTTM = fcfTTM.divide(
                                BigDecimal.valueOf(sharesOutstanding), 
                                4, 
                                java.math.RoundingMode.HALF_UP
                            );
                            builder.fcfPerShareTTM(fcfPerShareTTM);
                            log.debug("Calculated fcfPerShareTTM for {}: {} / {} = {}", 
                                ticker, fcfTTM, sharesOutstanding, fcfPerShareTTM);
                        }
                        
                        // FCF Annual per share
                        BigDecimal fcfAnnual = getBigDecimal(m, "freeCashFlowAnnual");
                        if (fcfAnnual != null) {
                            BigDecimal fcfPerShareAnnual = fcfAnnual.divide(
                                BigDecimal.valueOf(sharesOutstanding), 
                                4, 
                                java.math.RoundingMode.HALF_UP
                            );
                            builder.fcfPerShareAnnual(fcfPerShareAnnual);
                            log.debug("Calculated fcfPerShareAnnual for {}: {} / {} = {}", 
                                ticker, fcfAnnual, sharesOutstanding, fcfPerShareAnnual);
                        }
                    });
                }
            });

            InstrumentFundamentals fundamentals = builder.build();

            // Determine data quality
            if (fundamentals.hasMinimumData()) {
                fundamentals.setDataQuality(DataQuality.COMPLETE);
                log.info("Complete fundamentals for {}: currentPrice={}, fcfPerShare={}", 
                        ticker, 
                        fundamentals.getCurrentPrice(),
                        fundamentals.getBestFcfPerShare());
            } else {
                fundamentals.setDataQuality(DataQuality.PARTIAL);
                log.warn("Fundamentals for {} are incomplete - Missing required fields: currentPrice={}, fcfPerShareTTM={}, fcfPerShareAnnual={}", 
                        ticker,
                        fundamentals.getCurrentPrice(),
                        fundamentals.getFcfPerShareTTM(),
                        fundamentals.getFcfPerShareAnnual());
            }

            // Save to cache
            InstrumentFundamentals saved = fundamentalsRepository.save(fundamentals);
            log.info("Cached fundamentals for {} with quality: {}", 
                    ticker, saved.getDataQuality());

            return Optional.of(saved);

        } catch (Exception e) {
            log.error("Failed to fetch fundamentals from Finnhub for {}: {}", 
                    ticker, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Get current price from cached fundamentals or fetch fresh.
     */
    public Optional<BigDecimal> getCurrentPrice(String ticker) {
        return getFundamentals(ticker)
                .map(InstrumentFundamentals::getCurrentPrice);
    }

    /**
     * Get FCF per share from cached fundamentals or fetch fresh.
     */
    public Optional<BigDecimal> getFcfPerShare(String ticker) {
        return getFundamentals(ticker)
                .map(InstrumentFundamentals::getBestFcfPerShare);
    }

    /**
     * Get PE ratio from cached fundamentals.
     */
    public Optional<BigDecimal> getPE(String ticker) {
        return getFundamentals(ticker)
                .map(InstrumentFundamentals::getPeTTM);
    }

    /**
     * Get beta from cached fundamentals.
     */
    public Optional<BigDecimal> getBeta(String ticker) {
        return getFundamentals(ticker)
                .map(InstrumentFundamentals::getBeta);
    }

    // Helper methods for safe extraction
    private String getString(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private BigDecimal getBigDecimal(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            log.debug("Cannot parse {} as BigDecimal: {}", key, value);
            return null;
        }
    }

    private Long getLong(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            log.debug("Cannot parse {} as Long: {}", key, value);
            return null;
        }
    }
}
