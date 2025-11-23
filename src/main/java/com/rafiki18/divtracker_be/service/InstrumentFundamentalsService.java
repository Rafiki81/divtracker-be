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
            
            // If fresh AND has minimum data, return immediately
            if (!fundamentals.isStale() && fundamentals.hasMinimumData()) {
                log.debug("Returning fresh cached fundamentals for {}", normalizedTicker);
                return cached;
            }
            
            log.debug("Cached fundamentals for {} are stale or incomplete, will try to refresh", normalizedTicker);
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
            // Fetch all data
            Optional<Map<String, Object>> profile = finnhubClient.fetchCompanyProfile(ticker);
            Optional<Map<String, Object>> quote = finnhubClient.fetchQuote(ticker);
            Optional<Map<String, Object>> metricsResponse = finnhubClient.fetchEssentialMetrics(ticker);
            Optional<Map<String, BigDecimal>> fcfData = finnhubClient.calculateFCF(ticker);

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
                builder.currency(getString(p, "currency"));
                builder.sector(getString(p, "finnhubIndustry"));
                builder.shareOutstanding(getBigDecimal(p, "shareOutstanding"));
            });

            // Quote data - only current price
            BigDecimal currentPrice = quote.map(q -> getBigDecimal(q, "c")).orElse(null);
            quote.ifPresent(q -> {
                builder.currentPrice(getBigDecimal(q, "c"));
            });

            // Metrics data - only essential ratios
            metrics.ifPresent(m -> {
                // Valuation
                builder.peAnnual(getBigDecimal(m, "peAnnual"));
                
                // Risk
                builder.beta(getBigDecimal(m, "beta"));
                
                // Debt
                builder.debtToEquityRatio(getBigDecimal(m, "totalDebt/totalEquityQuarterly"));
                
                // Dividends
                builder.dividendPerShareAnnual(getBigDecimal(m, "dividendPerShareAnnual"));
                builder.dividendYield(getBigDecimal(m, "currentDividendYieldTTM"));
                builder.dividendGrowthRate5Y(getBigDecimal(m, "dividendGrowthRate5Y"));

                // Growth
                builder.epsGrowth5Y(getBigDecimal(m, "epsGrowth5Y"));
                builder.revenueGrowth5Y(getBigDecimal(m, "revenueGrowth5Y"));

                // Fallback FCF calculation if explicit FCF data is missing
                // Try to calculate from Price / FCF per share ratio (pfcfShareAnnual)
                BigDecimal pfcfShareAnnual = getBigDecimal(m, "pfcfShareAnnual");
                if (currentPrice != null && pfcfShareAnnual != null && pfcfShareAnnual.compareTo(BigDecimal.ZERO) != 0) {
                    try {
                        BigDecimal calculatedFcfPerShare = currentPrice.divide(pfcfShareAnnual, 4, java.math.RoundingMode.HALF_UP);
                        builder.fcfPerShareAnnual(calculatedFcfPerShare);
                        log.debug("Calculated fallback FCF per share for {}: {} / {} = {}", 
                                ticker, currentPrice, pfcfShareAnnual, calculatedFcfPerShare);
                        
                        // Also try to calculate total FCF if shares outstanding is available
                        BigDecimal shares = profile.map(p -> getBigDecimal(p, "shareOutstanding")).orElse(null);
                        if (shares != null) {
                            BigDecimal calculatedFcfTotal = calculatedFcfPerShare.multiply(shares);
                            builder.fcfAnnual(calculatedFcfTotal);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to calculate fallback FCF: {}", e.getMessage());
                    }
                }
            });

            // FCF data from cash flow statement (annual)
            fcfData.ifPresent(fcf -> {
                BigDecimal fcfTotal = fcf.get("fcf");
                BigDecimal fcfPerShare = fcf.get("fcfPerShare");
                
                if (fcfTotal != null) {
                    builder.fcfAnnual(fcfTotal);
                    log.debug("FCF annual for {}: {}", ticker, fcfTotal);
                }
                
                if (fcfPerShare != null) {
                    builder.fcfPerShareAnnual(fcfPerShare);
                    log.debug("FCF per share annual for {}: {}", ticker, fcfPerShare);
                }
            });

            InstrumentFundamentals fundamentals = builder.build();

            // Determine data quality
            if (fundamentals.hasMinimumData()) {
                fundamentals.setDataQuality(DataQuality.COMPLETE);
                log.info("Complete fundamentals for {}: currentPrice={}, fcfPerShareAnnual={}", 
                        ticker, 
                        fundamentals.getCurrentPrice(),
                        fundamentals.getFcfPerShare());
            } else {
                fundamentals.setDataQuality(DataQuality.PARTIAL);
                log.warn("Fundamentals for {} are incomplete - Missing required fields: currentPrice={}, fcfPerShareAnnual={}", 
                        ticker,
                        fundamentals.getCurrentPrice(),
                        fundamentals.getFcfPerShareAnnual());
            }

            // Save to cache
            InstrumentFundamentals saved = fundamentalsRepository.save(fundamentals);
            log.info("Cached fundamentals for {} with quality: {}", 
                    ticker, saved.getDataQuality());
            log.info("FULL SAVED FUNDAMENTALS for {}: {}", ticker, saved);

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
                .map(InstrumentFundamentals::getFcfPerShare);
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
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            log.debug("Cannot parse {} as BigDecimal: {}", key, value);
            return null;
        }
    }
}
