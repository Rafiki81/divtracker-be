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

    public Optional<BigDecimal> fetchFreeCashFlowPerShare(String ticker) {
        return fetchMap(ticker, "metrics", builder -> builder
                .path("/stock/metric")
                .queryParam("symbol", ticker)
                .queryParam("metric", "all")
            .queryParam("token", properties.getApiKey())
                .build())
                .flatMap(body -> Optional.ofNullable(body.get("metric")))
        .filter(Map.class::isInstance)
        .map(raw -> (Map<?, ?>) raw)
        .flatMap(metric -> extractDecimal(firstNonNull(metric,
            "freeCashFlowPerShareTTM",
            "freeCashFlowPerShareAnnual")))
                .filter(value -> value.compareTo(BigDecimal.ZERO) > 0);
    }

    /**
     * Search for stock symbols by query (name or ticker).
     * Uses Finnhub Symbol Search API.
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
                    .map(this::mapToTickerSearchResult)
                    .filter(result -> result.getSymbol() != null)
                    .limit(20) // Limit to top 20 results
                    .collect(Collectors.toList());

        } catch (Exception ex) {
            log.warn("Finnhub symbol search failed for query '{}': {}", query, ex.getMessage());
            return Collections.emptyList();
        }
    }

    public boolean isEnabled() {
        return properties.isEnabled() && StringUtils.hasText(properties.getApiUrl());
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

    private Object firstNonNull(Map<?, ?> source, String... keys) {
        for (String key : keys) {
            if (source.containsKey(key) && source.get(key) != null) {
                return source.get(key);
            }
        }
        return null;
    }
}
