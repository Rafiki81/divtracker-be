package com.rafiki18.divtracker_be.marketdata;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;

import com.rafiki18.divtracker_be.config.properties.FinnhubProperties;
import com.rafiki18.divtracker_be.dto.TickerSearchResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class FinnhubClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE = new ParameterizedTypeReference<>() {};

    private final WebClient finnhubWebClient;
    private final FinnhubProperties properties;

    public Optional<BigDecimal> fetchCurrentPrice(String ticker) {
        return fetchMap(ticker, "quote", builder -> builder
                .path("/quote")
                .queryParam("symbol", ticker)
            .queryParam("token", properties.getApiKey())
                .build())
                .flatMap(body -> extractDecimal(body.get("c")));
    }

    /**
     * Fetch cash flow financials from Finnhub.
     * Returns the most recent quarterly data containing operatingCashFlow and capitalExpenditure.
     * 
     * @param ticker Stock ticker symbol
     * @param frequency "annual" or "quarterly" (defaults to quarterly for most recent data)
     * @return Map containing cash flow data, or empty if not found
     */
    public Optional<Map<String, Object>> fetchCashFlowFinancials(String ticker, String frequency) {
        Optional<Map<String, Object>> result = fetchMap(ticker, "financials-cf", builder -> builder
                .path("/stock/financials")
                .queryParam("symbol", ticker)
                .queryParam("statement", "cf")
                .queryParam("freq", frequency)
                .queryParam("token", properties.getApiKey())
                .build());
        
        if (result.isPresent()) {
            log.info("Finnhub cash flow financials for {} ({}): {}", ticker, frequency, result.get());
        } else {
            log.warn("No cash flow financials returned from Finnhub for {} ({})", ticker, frequency);
        }
        
        return result;
    }
    
    /**
     * Calculate FCF and FCF per share from cash flow statement.
     * 
     * FCF = Operating Cash Flow - Capital Expenditure
     * FCF per Share = FCF / Shares Outstanding
     * 
     * @param ticker Stock ticker symbol
     * @return Map with "fcf" and "fcfPerShare" keys, or empty if data unavailable
     */
    public Optional<Map<String, BigDecimal>> calculateFCF(String ticker) {
        // Fetch annual cash flow data (more stable, eliminates seasonality)
        Optional<Map<String, Object>> cfData = fetchCashFlowFinancials(ticker, "annual");
        
        if (cfData.isEmpty()) {
            log.debug("No cash flow data available for {}", ticker);
            return Optional.empty();
        }
        
        // Extract financials array
        Object financialsObj = cfData.get().get("financials");
        if (!(financialsObj instanceof List<?> financialsList) || financialsList.isEmpty()) {
            log.debug("No financials array in cash flow data for {}", ticker);
            return Optional.empty();
        }
        
        // Get most recent annual data (first element)
        if (!(financialsList.get(0) instanceof Map<?, ?> latestAnnual)) {
            log.debug("Invalid financials data format for {}", ticker);
            return Optional.empty();
        }
        
        // Extract operating cash flow and capex
        Optional<BigDecimal> operatingCashFlow = extractDecimal(latestAnnual.get("operatingCashFlow"));
        Optional<BigDecimal> capex = extractDecimal(latestAnnual.get("capitalExpenditure"));
        
        if (operatingCashFlow.isEmpty() || capex.isEmpty()) {
            log.debug("Missing operatingCashFlow or capitalExpenditure for {}", ticker);
            return Optional.empty();
        }
        
        // Calculate FCF = Operating Cash Flow - Capital Expenditure
        // Note: capex is usually negative in financial statements
        BigDecimal fcf = operatingCashFlow.get().add(capex.get());
        
        log.info("Calculated FCF for {}: {} - ({}) = {}", 
                ticker, operatingCashFlow.get(), capex.get(), fcf);
        
        // Fetch profile to get shares outstanding
        Optional<Map<String, Object>> profile = fetchCompanyProfile(ticker);
        
        if (profile.isEmpty()) {
            log.debug("No profile data to calculate FCF per share for {}", ticker);
            return Optional.of(Map.of("fcf", fcf));
        }
        
        Optional<BigDecimal> sharesOutstanding = extractDecimal(profile.get().get("shareOutstanding"));
        
        if (sharesOutstanding.isEmpty() || sharesOutstanding.get().compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("Invalid or missing shareOutstanding for {}", ticker);
            return Optional.of(Map.of("fcf", fcf));
        }
        
        // Calculate FCF per share
        BigDecimal fcfPerShare = fcf.divide(sharesOutstanding.get(), 4, java.math.RoundingMode.HALF_UP);
        
        log.info("Calculated FCF per share for {}: {} / {} = {}", 
                ticker, fcf, sharesOutstanding.get(), fcfPerShare);
        
        return Optional.of(Map.of(
            "fcf", fcf,
            "fcfPerShare", fcfPerShare
        ));
    }

    /**
     * Fetch complete company profile from Finnhub.
     * 
     * @param ticker Stock ticker symbol
     * @return Map containing profile data, or empty if not found
     */
    public Optional<Map<String, Object>> fetchCompanyProfile(String ticker) {
        Optional<Map<String, Object>> result = fetchMap(ticker, "profile", builder -> builder
                .path("/stock/profile2")
                .queryParam("symbol", ticker)
                .queryParam("token", properties.getApiKey())
                .build());
        
        if (result.isPresent()) {
            log.info("Finnhub profile FULL RESPONSE for {}: {}", ticker, result.get());
            log.debug("Finnhub profile for {}: name={}, exchange={}, currency={}", 
                    ticker, 
                    result.get().get("name"),
                    result.get().get("exchange"),
                    result.get().get("currency"));
        } else {
            log.warn("No profile data returned from Finnhub for {}", ticker);
        }
        
        return result;
    }

    /**
     * Fetch essential ratios/metrics for a ticker (PE, beta, D/E ratio).
     * Only fetches the minimum data needed for valuation analysis.
     * 
     * @param ticker Stock ticker symbol
     * @return Map containing metrics with only the essential ratios
     */
    public Optional<Map<String, Object>> fetchEssentialMetrics(String ticker) {
        Optional<Map<String, Object>> result = fetchMap(ticker, "metrics", builder -> builder
                .path("/stock/metric")
                .queryParam("symbol", ticker)
                .queryParam("metric", "all")
                .queryParam("token", properties.getApiKey())
                .build());
        
        if (result.isPresent()) {
            log.info("Finnhub essential metrics FULL RESPONSE for {}: {}", ticker, result.get());
            Object metricObj = result.get().get("metric");
            if (metricObj instanceof Map<?, ?> metrics) {
                log.info("Finnhub essential metrics parsed for {}: peAnnual={}, beta={}, debtToEquity={}", 
                        ticker,
                        metrics.get("peAnnual"),
                        metrics.get("beta"),
                        metrics.get("totalDebt/totalEquityQuarterly"));
            } else {
                log.warn("Metrics response for {} has no 'metric' map", ticker);
            }
        } else {
            log.warn("No metrics data returned from Finnhub for {}", ticker);
        }
        
        return result;
    }
    
    /**
     * @deprecated Use fetchEssentialMetrics() instead. This method fetches too much unnecessary data.
     */
    @Deprecated(since = "2025-11-23", forRemoval = true)
    public Optional<Map<String, Object>> fetchAllMetrics(String ticker) {
        return fetchEssentialMetrics(ticker);
    }

    /**
     * Fetch quote data (price, change, etc.) for a ticker.
     * 
     * @param ticker Stock ticker symbol
     * @return Map containing quote data, or empty if not found
     */
    public Optional<Map<String, Object>> fetchQuote(String ticker) {
        Optional<Map<String, Object>> result = fetchMap(ticker, "quote", builder -> builder
                .path("/quote")
                .queryParam("symbol", ticker)
                .queryParam("token", properties.getApiKey())
                .build());
        
        if (result.isPresent()) {
            log.info("Finnhub quote FULL RESPONSE for {}: {}", ticker, result.get());
            log.debug("Finnhub quote for {}: currentPrice={}, change={}, previousClose={}", 
                    ticker,
                    result.get().get("c"),
                    result.get().get("d"),
                    result.get().get("pc"));
        } else {
            log.warn("No quote data returned from Finnhub for {}", ticker);
        }
        
        return result;
    }

    /**
     * Lookup symbols from Finnhub using Symbol Lookup API.
     * Returns all symbols from US exchanges that match the query.
     * 
     * @param query Ticker symbol to look up (e.g., "BAM", "AAPL")
     * @return List of matching symbols with full details
     */
    public List<TickerSearchResult> lookupSymbol(String query) {
        if (!isEnabled() || !StringUtils.hasText(query)) {
            log.debug("Finnhub disabled or empty query, returning empty results");
            return Collections.emptyList();
        }

        try {
            // Get all US exchange symbols
            Object response = finnhubWebClient.get()
                    .uri(builder -> builder
                            .path("/stock/symbol")
                            .queryParam("exchange", "US")
                            .queryParam("token", properties.getApiKey())
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                    .timeout(Duration.ofSeconds(10))
                    .blockOptional()
                    .orElse(Collections.emptyList());

            if (!(response instanceof List<?> symbolList)) {
                log.debug("No symbols returned from Finnhub");
                return Collections.emptyList();
            }

            String normalizedQuery = query.trim().toUpperCase();
            
            return symbolList.stream()
                    .filter(Map.class::isInstance)
                    .map(item -> (Map<?, ?>) item)
                    .filter(item -> {
                        String symbol = getString(item, "symbol");
                        return symbol != null && symbol.toUpperCase().startsWith(normalizedQuery);
                    })
                    .map(this::mapToTickerSearchResult)
                    .filter(result -> result.getSymbol() != null)
                    .limit(20) // Limit to top 20 matches
                    .collect(Collectors.toList());

        } catch (Exception ex) {
            log.warn("Finnhub symbol lookup failed for query '{}': {}", query, ex.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Search for stock symbols by query (name or ticker).
     * Uses Finnhub Symbol Search API for fuzzy search by company name.
     * 
     * @param query Search term (company name or ticker symbol)
     * @return List of matching ticker results
     */
    public List<TickerSearchResult> searchSymbols(String query) {
        if (!isEnabled() || !StringUtils.hasText(query)) {
            log.debug("Finnhub disabled or empty query, returning empty results");
            return Collections.emptyList();
        }

        try {
            Map<String, Object> response = finnhubWebClient.get()
                    .uri(builder -> builder
                            .path("/search")
                            .queryParam("q", query.trim())
                            .queryParam("token", properties.getApiKey())
                            .build())
                    .retrieve()
                    .bodyToMono(MAP_TYPE)
                    .timeout(REQUEST_TIMEOUT)
                    .blockOptional()
                    .orElse(Collections.emptyMap());

            Object resultObj = response.get("result");
            if (!(resultObj instanceof List<?> resultList)) {
                log.debug("No results found for query: {}", query);
                return Collections.emptyList();
            }

            return resultList.stream()
                    .filter(Map.class::isInstance)
                    .map(item -> (Map<?, ?>) item)
                    .map(this::mapSearchResultToTickerSearchResult)
                    .filter(result -> result.getSymbol() != null)
                    .limit(20) // Limit to top 20 results
                    .collect(Collectors.toList());

        } catch (Exception ex) {
            log.warn("Finnhub symbol search failed for query '{}': {}", query, ex.getMessage());
            return Collections.emptyList();
        }
    }

    public boolean isEnabled() {
        return properties.isEnabled() 
            && StringUtils.hasText(properties.getApiUrl())
            && StringUtils.hasText(properties.getApiKey());
    }

    private TickerSearchResult mapToTickerSearchResult(Map<?, ?> data) {
        return TickerSearchResult.builder()
                .symbol(getString(data, "symbol"))
                .description(getString(data, "description"))
                .type(getString(data, "type"))
                .exchange(getString(data, "displaySymbol"))
                .currency(getString(data, "currency"))
                .figi(getString(data, "figi"))
                .build();
    }

    /**
     * Map Finnhub search API response to TickerSearchResult.
     * Search API returns different field names than symbol lookup.
     */
    private TickerSearchResult mapSearchResultToTickerSearchResult(Map<?, ?> data) {
        return TickerSearchResult.builder()
                .symbol(getString(data, "symbol"))
                .description(getString(data, "description"))
                .type(getString(data, "type"))
                .exchange(getString(data, "primary")) // Search API uses "primary" for exchange
                .currency(getString(data, "currency"))
                .figi(getString(data, "figi"))
                .build();
    }

    private String getString(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private Optional<Map<String, Object>> fetchMap(String ticker, String operation,
                                                   Function<UriBuilder, java.net.URI> uriFunction) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        try {
            return finnhubWebClient.get()
                    .uri(uriFunction)
                    .retrieve()
                    .bodyToMono(MAP_TYPE)
                    .timeout(REQUEST_TIMEOUT)
                    .blockOptional();
        } catch (Exception ex) {
            log.warn("Finnhub {} request failed for {}: {}", operation, ticker, ex.getMessage());
            return Optional.empty();
        }
    }

    private Optional<BigDecimal> extractDecimal(Object value) {
        if (value == null) {
            return Optional.empty();
        }
        if (value instanceof Number number) {
            return Optional.of(BigDecimal.valueOf(number.doubleValue()));
        }
        try {
            return Optional.of(new BigDecimal(value.toString()));
        } catch (NumberFormatException ex) {
            log.debug("Unable to parse numeric value from {}", value);
            return Optional.empty();
        }
    }
}
