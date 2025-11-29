package com.rafiki18.divtracker_be.marketdata;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;

import com.rafiki18.divtracker_be.config.properties.FinnhubProperties;
import com.rafiki18.divtracker_be.dto.TickerSearchResult;

import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
@DisplayName("FinnhubClient Tests")
class FinnhubClientTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec<?> requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec<?> requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private FinnhubProperties properties;
    private FinnhubClient finnhubClient;

    @BeforeEach
    void setUp() {
        properties = new FinnhubProperties();
        properties.setApiUrl("https://finnhub.io/api/v1");
        properties.setApiKey("test-api-key");
        finnhubClient = new FinnhubClient(webClient, properties);
    }

    @SuppressWarnings("unchecked")
    private void setupWebClientMock() {
        when(webClient.get()).thenReturn((WebClient.RequestHeadersUriSpec) requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Nested
    @DisplayName("isEnabled() Tests")
    class IsEnabledTests {

        @Test
        @DisplayName("should return true when API key and URL are configured")
        void shouldReturnTrueWhenConfigured() {
            assertThat(finnhubClient.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("should return false when API key is missing")
        void shouldReturnFalseWhenApiKeyMissing() {
            properties.setApiKey(null);
            assertThat(finnhubClient.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("should return false when API key is empty")
        void shouldReturnFalseWhenApiKeyEmpty() {
            properties.setApiKey("");
            assertThat(finnhubClient.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("should return false when API URL is missing")
        void shouldReturnFalseWhenApiUrlMissing() {
            properties.setApiUrl(null);
            assertThat(finnhubClient.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("should return false when API URL is empty")
        void shouldReturnFalseWhenApiUrlEmpty() {
            properties.setApiUrl("");
            assertThat(finnhubClient.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("fetchCurrentPrice() Tests")
    class FetchCurrentPriceTests {

        @Test
        @DisplayName("should return current price when API returns valid data")
        void shouldReturnCurrentPrice() {
            setupWebClientMock();
            Map<String, Object> response = Map.of("c", 150.25, "d", 1.5, "pc", 148.75);
            when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                    .thenReturn(Mono.just(response));

            Optional<BigDecimal> result = finnhubClient.fetchCurrentPrice("AAPL");

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualByComparingTo(new BigDecimal("150.25"));
        }

        @Test
        @DisplayName("should return empty when API returns null price")
        void shouldReturnEmptyWhenNullPrice() {
            setupWebClientMock();
            Map<String, Object> response = Map.of("d", 1.5, "pc", 148.75);
            when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                    .thenReturn(Mono.just(response));

            Optional<BigDecimal> result = finnhubClient.fetchCurrentPrice("AAPL");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty when client is disabled")
        void shouldReturnEmptyWhenDisabled() {
            properties.setApiKey(null);
            
            Optional<BigDecimal> result = finnhubClient.fetchCurrentPrice("AAPL");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty when API call fails")
        void shouldReturnEmptyOnApiError() {
            setupWebClientMock();
            when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                    .thenReturn(Mono.error(new RuntimeException("API Error")));

            Optional<BigDecimal> result = finnhubClient.fetchCurrentPrice("AAPL");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should handle 403 forbidden error gracefully")
        void shouldHandle403Error() {
            setupWebClientMock();
            when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                    .thenReturn(Mono.error(new RuntimeException("403 Forbidden - Plan limit reached")));

            Optional<BigDecimal> result = finnhubClient.fetchCurrentPrice("AAPL");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("fetchQuote() Tests")
    class FetchQuoteTests {

        @Test
        @DisplayName("should return quote data when API returns valid data")
        void shouldReturnQuoteData() {
            setupWebClientMock();
            Map<String, Object> response = Map.of(
                    "c", 150.25,
                    "d", 1.5,
                    "dp", 1.01,
                    "h", 152.0,
                    "l", 149.0,
                    "o", 149.5,
                    "pc", 148.75
            );
            when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                    .thenReturn(Mono.just(response));

            Optional<Map<String, Object>> result = finnhubClient.fetchQuote("AAPL");

            assertThat(result).isPresent();
            assertThat(result.get()).containsKey("c");
            assertThat(result.get().get("c")).isEqualTo(150.25);
        }

        @Test
        @DisplayName("should return empty when client is disabled")
        void shouldReturnEmptyWhenDisabled() {
            properties.setApiKey(null);
            
            Optional<Map<String, Object>> result = finnhubClient.fetchQuote("AAPL");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("fetchCompanyProfile() Tests")
    class FetchCompanyProfileTests {

        @Test
        @DisplayName("should return company profile when API returns valid data")
        void shouldReturnCompanyProfile() {
            setupWebClientMock();
            Map<String, Object> response = Map.of(
                    "name", "Apple Inc",
                    "exchange", "NASDAQ",
                    "currency", "USD",
                    "shareOutstanding", 15800000000.0,
                    "marketCapitalization", 2500000000000.0
            );
            when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                    .thenReturn(Mono.just(response));

            Optional<Map<String, Object>> result = finnhubClient.fetchCompanyProfile("AAPL");

            assertThat(result).isPresent();
            assertThat(result.get().get("name")).isEqualTo("Apple Inc");
            assertThat(result.get().get("exchange")).isEqualTo("NASDAQ");
        }

        @Test
        @DisplayName("should return empty when no profile data")
        void shouldReturnEmptyWhenNoProfile() {
            setupWebClientMock();
            when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                    .thenReturn(Mono.empty());

            Optional<Map<String, Object>> result = finnhubClient.fetchCompanyProfile("INVALID");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("fetchEssentialMetrics() Tests")
    class FetchEssentialMetricsTests {

        @Test
        @DisplayName("should return metrics when API returns valid data")
        void shouldReturnMetrics() {
            setupWebClientMock();
            Map<String, Object> metrics = Map.of(
                    "peAnnual", 28.5,
                    "beta", 1.2,
                    "totalDebt/totalEquityQuarterly", 0.5
            );
            Map<String, Object> response = Map.of("metric", metrics);
            when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                    .thenReturn(Mono.just(response));

            Optional<Map<String, Object>> result = finnhubClient.fetchEssentialMetrics("AAPL");

            assertThat(result).isPresent();
            assertThat(result.get()).containsKey("metric");
        }

        @Test
        @DisplayName("should handle response without metric map")
        void shouldHandleNoMetricMap() {
            setupWebClientMock();
            Map<String, Object> response = Map.of("status", "ok");
            when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                    .thenReturn(Mono.just(response));

            Optional<Map<String, Object>> result = finnhubClient.fetchEssentialMetrics("AAPL");

            assertThat(result).isPresent();
            assertThat(result.get()).doesNotContainKey("metric");
        }
    }

    @Nested
    @DisplayName("fetchCashFlowFinancials() Tests")
    class FetchCashFlowFinancialsTests {

        @Test
        @DisplayName("should return cash flow data when API returns valid data")
        void shouldReturnCashFlowData() {
            setupWebClientMock();
            Map<String, Object> financials = Map.of(
                    "operatingCashFlow", 100000000.0,
                    "capitalExpenditure", -20000000.0
            );
            Map<String, Object> response = Map.of(
                    "financials", List.of(financials),
                    "symbol", "AAPL"
            );
            when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                    .thenReturn(Mono.just(response));

            Optional<Map<String, Object>> result = finnhubClient.fetchCashFlowFinancials("AAPL", "annual");

            assertThat(result).isPresent();
            assertThat(result.get()).containsKey("financials");
        }

        @Test
        @DisplayName("should return empty when client is disabled")
        void shouldReturnEmptyWhenDisabled() {
            properties.setApiKey(null);
            
            Optional<Map<String, Object>> result = finnhubClient.fetchCashFlowFinancials("AAPL", "annual");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("calculateFCF() Tests")
    class CalculateFCFTests {

        @Test
        @DisplayName("should calculate FCF and FCF per share correctly")
        void shouldCalculateFCFAndFCFPerShare() {
            setupWebClientMock();
            
            // Cash flow response
            Map<String, Object> financials = Map.of(
                    "operatingCashFlow", 100000000.0,
                    "capitalExpenditure", -20000000.0
            );
            Map<String, Object> cfResponse = Map.of("financials", List.of(financials));
            
            // Profile response
            Map<String, Object> profileResponse = Map.of("shareOutstanding", 1000000.0);
            
            when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                    .thenReturn(Mono.just(cfResponse))
                    .thenReturn(Mono.just(profileResponse));

            Optional<Map<String, BigDecimal>> result = finnhubClient.calculateFCF("AAPL");

            assertThat(result).isPresent();
            assertThat(result.get()).containsKey("fcf");
            assertThat(result.get()).containsKey("fcfPerShare");
            // FCF = 100M - 20M = 80M
            assertThat(result.get().get("fcf")).isEqualByComparingTo(new BigDecimal("80000000"));
            // FCF per share = 80M / 1M = 80
            assertThat(result.get().get("fcfPerShare")).isEqualByComparingTo(new BigDecimal("80"));
        }

        @Test
        @DisplayName("should return FCF only when profile not available")
        void shouldReturnFCFOnlyWhenNoProfile() {
            setupWebClientMock();
            
            Map<String, Object> financials = Map.of(
                    "operatingCashFlow", 100000000.0,
                    "capitalExpenditure", -20000000.0
            );
            Map<String, Object> cfResponse = Map.of("financials", List.of(financials));
            
            when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                    .thenReturn(Mono.just(cfResponse))
                    .thenReturn(Mono.empty());

            Optional<Map<String, BigDecimal>> result = finnhubClient.calculateFCF("AAPL");

            assertThat(result).isPresent();
            assertThat(result.get()).containsKey("fcf");
            assertThat(result.get()).doesNotContainKey("fcfPerShare");
        }

        @Test
        @DisplayName("should return empty when no cash flow data")
        void shouldReturnEmptyWhenNoCashFlowData() {
            setupWebClientMock();
            when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                    .thenReturn(Mono.empty());

            Optional<Map<String, BigDecimal>> result = finnhubClient.calculateFCF("AAPL");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty when financials array is empty")
        void shouldReturnEmptyWhenFinancialsEmpty() {
            setupWebClientMock();
            Map<String, Object> cfResponse = Map.of("financials", Collections.emptyList());
            when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                    .thenReturn(Mono.just(cfResponse));

            Optional<Map<String, BigDecimal>> result = finnhubClient.calculateFCF("AAPL");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty when missing operatingCashFlow")
        void shouldReturnEmptyWhenMissingOperatingCashFlow() {
            setupWebClientMock();
            Map<String, Object> financials = Map.of("capitalExpenditure", -20000000.0);
            Map<String, Object> cfResponse = Map.of("financials", List.of(financials));
            when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                    .thenReturn(Mono.just(cfResponse));

            Optional<Map<String, BigDecimal>> result = finnhubClient.calculateFCF("AAPL");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty when missing capitalExpenditure")
        void shouldReturnEmptyWhenMissingCapex() {
            setupWebClientMock();
            Map<String, Object> financials = Map.of("operatingCashFlow", 100000000.0);
            Map<String, Object> cfResponse = Map.of("financials", List.of(financials));
            when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                    .thenReturn(Mono.just(cfResponse));

            Optional<Map<String, BigDecimal>> result = finnhubClient.calculateFCF("AAPL");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return FCF only when shares outstanding is zero")
        void shouldReturnFCFOnlyWhenSharesZero() {
            setupWebClientMock();
            
            Map<String, Object> financials = Map.of(
                    "operatingCashFlow", 100000000.0,
                    "capitalExpenditure", -20000000.0
            );
            Map<String, Object> cfResponse = Map.of("financials", List.of(financials));
            Map<String, Object> profileResponse = Map.of("shareOutstanding", 0.0);
            
            when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                    .thenReturn(Mono.just(cfResponse))
                    .thenReturn(Mono.just(profileResponse));

            Optional<Map<String, BigDecimal>> result = finnhubClient.calculateFCF("AAPL");

            assertThat(result).isPresent();
            assertThat(result.get()).containsKey("fcf");
            assertThat(result.get()).doesNotContainKey("fcfPerShare");
        }

        @Test
        @DisplayName("should return empty when client is disabled")
        void shouldReturnEmptyWhenDisabled() {
            properties.setApiKey(null);
            
            Optional<Map<String, BigDecimal>> result = finnhubClient.calculateFCF("AAPL");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("lookupSymbol() Tests")
    class LookupSymbolTests {

        @Test
        @SuppressWarnings("unchecked")
        @DisplayName("should return matching symbols")
        void shouldReturnMatchingSymbols() {
            when(webClient.get()).thenReturn((WebClient.RequestHeadersUriSpec) requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            
            List<Map<String, Object>> symbolList = List.of(
                    Map.of("symbol", "AAPL", "description", "Apple Inc", "type", "Common Stock", "displaySymbol", "AAPL"),
                    Map.of("symbol", "AAPLX", "description", "Apple ETF", "type", "ETF", "displaySymbol", "AAPLX"),
                    Map.of("symbol", "MSFT", "description", "Microsoft Corp", "type", "Common Stock", "displaySymbol", "MSFT")
            );
            when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                    .thenReturn(Mono.just(symbolList).timeout(Duration.ofSeconds(10)));

            List<TickerSearchResult> results = finnhubClient.lookupSymbol("AAPL");

            assertThat(results).hasSize(2);
            assertThat(results.get(0).getSymbol()).isEqualTo("AAPL");
            assertThat(results.get(1).getSymbol()).isEqualTo("AAPLX");
        }

        @Test
        @DisplayName("should return empty list when disabled")
        void shouldReturnEmptyWhenDisabled() {
            properties.setApiKey(null);
            
            List<TickerSearchResult> results = finnhubClient.lookupSymbol("AAPL");

            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should return empty list when query is empty")
        void shouldReturnEmptyWhenQueryEmpty() {
            List<TickerSearchResult> results = finnhubClient.lookupSymbol("");

            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should return empty list when query is null")
        void shouldReturnEmptyWhenQueryNull() {
            List<TickerSearchResult> results = finnhubClient.lookupSymbol(null);

            assertThat(results).isEmpty();
        }

        @Test
        @SuppressWarnings("unchecked")
        @DisplayName("should handle API error gracefully")
        void shouldHandleApiError() {
            when(webClient.get()).thenReturn((WebClient.RequestHeadersUriSpec) requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                    .thenReturn(Mono.error(new RuntimeException("Network error")));

            List<TickerSearchResult> results = finnhubClient.lookupSymbol("AAPL");

            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("searchSymbols() Tests")
    class SearchSymbolsTests {

        @Test
        @DisplayName("should return search results")
        void shouldReturnSearchResults() {
            setupWebClientMock();
            
            List<Map<String, Object>> resultList = List.of(
                    Map.of("symbol", "AAPL", "description", "Apple Inc", "type", "Common Stock"),
                    Map.of("symbol", "MSFT", "description", "Microsoft Corp", "type", "Common Stock")
            );
            Map<String, Object> response = Map.of("result", resultList);
            when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                    .thenReturn(Mono.just(response));

            List<TickerSearchResult> results = finnhubClient.searchSymbols("Apple");

            assertThat(results).hasSize(2);
            assertThat(results.get(0).getSymbol()).isEqualTo("AAPL");
            assertThat(results.get(0).getDescription()).isEqualTo("Apple Inc");
        }

        @Test
        @DisplayName("should return empty list when disabled")
        void shouldReturnEmptyWhenDisabled() {
            properties.setApiKey(null);
            
            List<TickerSearchResult> results = finnhubClient.searchSymbols("Apple");

            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should return empty list when query is empty")
        void shouldReturnEmptyWhenQueryEmpty() {
            List<TickerSearchResult> results = finnhubClient.searchSymbols("");

            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should return empty list when query is null")
        void shouldReturnEmptyWhenQueryNull() {
            List<TickerSearchResult> results = finnhubClient.searchSymbols(null);

            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should return empty list when no results found")
        void shouldReturnEmptyWhenNoResults() {
            setupWebClientMock();
            Map<String, Object> response = Map.of("result", Collections.emptyList());
            when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                    .thenReturn(Mono.just(response));

            List<TickerSearchResult> results = finnhubClient.searchSymbols("XYZABC123");

            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should handle missing result field")
        void shouldHandleMissingResultField() {
            setupWebClientMock();
            Map<String, Object> response = Map.of("count", 0);
            when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                    .thenReturn(Mono.just(response));

            List<TickerSearchResult> results = finnhubClient.searchSymbols("Apple");

            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should handle API error gracefully")
        void shouldHandleApiError() {
            setupWebClientMock();
            when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                    .thenReturn(Mono.error(new RuntimeException("Network error")));

            List<TickerSearchResult> results = finnhubClient.searchSymbols("Apple");

            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should limit results to 20")
        void shouldLimitResultsTo20() {
            setupWebClientMock();
            
            // Create 25 results
            List<Map<String, Object>> resultList = new java.util.ArrayList<>();
            for (int i = 0; i < 25; i++) {
                resultList.add(Map.of("symbol", "SYM" + i, "description", "Company " + i, "type", "Stock"));
            }
            Map<String, Object> response = Map.of("result", resultList);
            when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                    .thenReturn(Mono.just(response));

            List<TickerSearchResult> results = finnhubClient.searchSymbols("SYM");

            assertThat(results).hasSize(20);
        }
    }

    @Nested
    @DisplayName("extractDecimal() Edge Cases")
    class ExtractDecimalTests {

        @Test
        @DisplayName("should handle string numeric values")
        void shouldHandleStringNumericValues() {
            setupWebClientMock();
            Map<String, Object> response = Map.of("c", "150.25");
            when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                    .thenReturn(Mono.just(response));

            Optional<BigDecimal> result = finnhubClient.fetchCurrentPrice("AAPL");

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualByComparingTo(new BigDecimal("150.25"));
        }

        @Test
        @DisplayName("should handle integer values")
        void shouldHandleIntegerValues() {
            setupWebClientMock();
            Map<String, Object> response = Map.of("c", 150);
            when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                    .thenReturn(Mono.just(response));

            Optional<BigDecimal> result = finnhubClient.fetchCurrentPrice("AAPL");

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualByComparingTo(new BigDecimal("150"));
        }
    }
}
