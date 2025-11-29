package com.rafiki18.divtracker_be.controller;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rafiki18.divtracker_be.model.InstrumentFundamentals;
import com.rafiki18.divtracker_be.model.MarketPriceTick;
import com.rafiki18.divtracker_be.repository.InstrumentFundamentalsRepository;
import com.rafiki18.divtracker_be.repository.MarketPriceTickRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "finnhub.webhook-secret=test-secret-12345"
})
@DisplayName("FinnhubWebhookController Integration Tests")
class FinnhubWebhookControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MarketPriceTickRepository marketPriceTickRepository;

    @Autowired
    private InstrumentFundamentalsRepository instrumentFundamentalsRepository;

    @Value("${finnhub.webhook-secret}")
    private String webhookSecret;

    private static final String WEBHOOK_ENDPOINT = "/api/webhooks/finnhub";
    private static final String SECRET_HEADER = "X-Finnhub-Secret";

    @BeforeEach
    void setUp() {
        marketPriceTickRepository.deleteAll();
        instrumentFundamentalsRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        marketPriceTickRepository.deleteAll();
        instrumentFundamentalsRepository.deleteAll();
    }

    /**
     * Wait for async processing to complete.
     * Polls the database until expected condition is met or timeout.
     */
    private void waitForAsyncProcessing(int expectedTicks) throws InterruptedException {
        int attempts = 0;
        while (attempts < 20) { // Max 2 seconds
            Thread.sleep(100);
            if (marketPriceTickRepository.count() >= expectedTicks) {
                return;
            }
            attempts++;
        }
    }

    @Nested
    @DisplayName("Security Tests")
    class SecurityTests {

        @Test
        @DisplayName("Should reject request without secret header")
        void shouldRejectRequestWithoutSecret() throws Exception {
            String payload = createTradePayload("AAPL", 172.15, 1732285432000L, 1000);

            mockMvc.perform(post(WEBHOOK_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
                    .andExpect(status().isUnauthorized());

            assertThat(marketPriceTickRepository.findAll()).isEmpty();
        }

        @Test
        @DisplayName("Should reject request with invalid secret")
        void shouldRejectRequestWithInvalidSecret() throws Exception {
            String payload = createTradePayload("AAPL", 172.15, 1732285432000L, 1000);

            mockMvc.perform(post(WEBHOOK_ENDPOINT)
                    .header(SECRET_HEADER, "wrong-secret")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
                    .andExpect(status().isUnauthorized());

            assertThat(marketPriceTickRepository.findAll()).isEmpty();
        }

        @Test
        @DisplayName("Should reject request with empty secret")
        void shouldRejectRequestWithEmptySecret() throws Exception {
            String payload = createTradePayload("AAPL", 172.15, 1732285432000L, 1000);

            mockMvc.perform(post(WEBHOOK_ENDPOINT)
                    .header(SECRET_HEADER, "")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
                    .andExpect(status().isUnauthorized());

            assertThat(marketPriceTickRepository.findAll()).isEmpty();
        }

        @Test
        @DisplayName("Should accept request with valid secret")
        void shouldAcceptRequestWithValidSecret() throws Exception {
            String payload = createTradePayload("AAPL", 172.15, 1732285432000L, 1000);

            mockMvc.perform(post(WEBHOOK_ENDPOINT)
                    .header(SECRET_HEADER, webhookSecret)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Trade Processing Tests")
    class TradeProcessingTests {

        @Test
        @DisplayName("Should save single trade to market_price_ticks when fundamentals exist")
        void shouldSaveSingleTrade() throws Exception {
            // Create fundamentals so the tick gets saved
            createFundamentalsForTicker("AAPL");
            
            String payload = createTradePayload("AAPL", 172.15, 1732285432000L, 1000);

            mockMvc.perform(post(WEBHOOK_ENDPOINT)
                    .header(SECRET_HEADER, webhookSecret)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
                    .andExpect(status().isOk());

            // Give async processing time to complete
            Thread.sleep(100);

            List<MarketPriceTick> ticks = marketPriceTickRepository.findAll();
            assertThat(ticks).hasSize(1);

            MarketPriceTick tick = ticks.get(0);
            assertThat(tick.getTicker()).isEqualTo("AAPL");
            assertThat(tick.getPrice()).isEqualByComparingTo(new BigDecimal("172.15"));
            assertThat(tick.getVolume()).isEqualByComparingTo(new BigDecimal("1000"));
            assertThat(tick.getSource()).isEqualTo("FINNHUB_WEBHOOK");
            assertThat(tick.getTradeTimestamp()).isNotNull();
            assertThat(tick.getReceivedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should save multiple trades from single webhook when fundamentals exist")
        void shouldSaveMultipleTrades() throws Exception {
            // Create fundamentals for all tickers
            createFundamentalsForTicker("AAPL");
            createFundamentalsForTicker("MSFT");
            createFundamentalsForTicker("GOOGL");
            
            String payload = """
                {
                  "event": "trade",
                  "data": [
                    {"s": "AAPL", "p": 172.15, "t": 1732285432000, "v": 1000},
                    {"s": "MSFT", "p": 378.50, "t": 1732285433000, "v": 500},
                    {"s": "GOOGL", "p": 140.25, "t": 1732285434000, "v": 750}
                  ]
                }
                """;

            mockMvc.perform(post(WEBHOOK_ENDPOINT)
                    .header(SECRET_HEADER, webhookSecret)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
                    .andExpect(status().isOk());

            // Give async processing time to complete
            Thread.sleep(100);

            List<MarketPriceTick> ticks = marketPriceTickRepository.findAll();
            assertThat(ticks).hasSize(3);
            assertThat(ticks).extracting(MarketPriceTick::getTicker)
                    .containsExactlyInAnyOrder("AAPL", "MSFT", "GOOGL");
        }

        @Test
        @DisplayName("Should only save trades for tickers with fundamentals")
        void shouldOnlySaveTradesForTrackedTickers() throws Exception {
            // Only create fundamentals for AAPL
            createFundamentalsForTicker("AAPL");
            
            String payload = """
                {
                  "event": "trade",
                  "data": [
                    {"s": "AAPL", "p": 172.15, "t": 1732285432000, "v": 1000},
                    {"s": "MSFT", "p": 378.50, "t": 1732285433000, "v": 500},
                    {"s": "GOOGL", "p": 140.25, "t": 1732285434000, "v": 750}
                  ]
                }
                """;

            mockMvc.perform(post(WEBHOOK_ENDPOINT)
                    .header(SECRET_HEADER, webhookSecret)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
                    .andExpect(status().isOk());

            // Give async processing time to complete
            Thread.sleep(100);

            // Only AAPL should be saved
            List<MarketPriceTick> ticks = marketPriceTickRepository.findAll();
            assertThat(ticks).hasSize(1);
            assertThat(ticks.get(0).getTicker()).isEqualTo("AAPL");
        }

        @Test
        @DisplayName("Should normalize ticker to uppercase")
        void shouldNormalizeTickerToUppercase() throws Exception {
            // Create fundamentals (uppercase)
            createFundamentalsForTicker("AAPL");
            
            // Send webhook with lowercase ticker
            String payload = createTradePayload("aapl", 172.15, 1732285432000L, 1000);

            mockMvc.perform(post(WEBHOOK_ENDPOINT)
                    .header(SECRET_HEADER, webhookSecret)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
                    .andExpect(status().isOk());

            Thread.sleep(100);

            List<MarketPriceTick> ticks = marketPriceTickRepository.findAll();
            assertThat(ticks).hasSize(1);
            assertThat(ticks.get(0).getTicker()).isEqualTo("AAPL");
        }

        @Test
        @DisplayName("Should handle trade without volume")
        void shouldHandleTradeWithoutVolume() throws Exception {
            createFundamentalsForTicker("AAPL");
            
            String payload = """
                {
                  "event": "trade",
                  "data": [
                    {"s": "AAPL", "p": 172.15, "t": 1732285432000}
                  ]
                }
                """;

            mockMvc.perform(post(WEBHOOK_ENDPOINT)
                    .header(SECRET_HEADER, webhookSecret)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
                    .andExpect(status().isOk());

            Thread.sleep(100);

            List<MarketPriceTick> ticks = marketPriceTickRepository.findAll();
            assertThat(ticks).hasSize(1);
            assertThat(ticks.get(0).getVolume()).isNull();
        }

        @Test
        @DisplayName("Should ignore non-trade events")
        void shouldIgnoreNonTradeEvents() throws Exception {
            String payload = """
                {
                  "event": "ping",
                  "data": []
                }
                """;

            mockMvc.perform(post(WEBHOOK_ENDPOINT)
                    .header(SECRET_HEADER, webhookSecret)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
                    .andExpect(status().isOk());

            Thread.sleep(100);

            assertThat(marketPriceTickRepository.findAll()).isEmpty();
        }

        @Test
        @DisplayName("Should skip trades with incomplete data")
        void shouldSkipTradesWithIncompleteData() throws Exception {
            // Create fundamentals for both AAPL and MSFT
            createFundamentalsForTicker("AAPL");
            createFundamentalsForTicker("MSFT");
            
            String payload = """
                {
                  "event": "trade",
                  "data": [
                    {"s": "AAPL", "t": 1732285432000},
                    {"p": 172.15, "t": 1732285432000},
                    {"s": "MSFT", "p": 378.50, "t": 1732285433000, "v": 500}
                  ]
                }
                """;

            mockMvc.perform(post(WEBHOOK_ENDPOINT)
                    .header(SECRET_HEADER, webhookSecret)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
                    .andExpect(status().isOk());

            Thread.sleep(100);

            // Only the complete MSFT trade should be saved (AAPL missing price, second missing symbol)
            List<MarketPriceTick> ticks = marketPriceTickRepository.findAll();
            assertThat(ticks).hasSize(1);
            assertThat(ticks.get(0).getTicker()).isEqualTo("MSFT");
        }
    }

    @Nested
    @DisplayName("InstrumentFundamentals Update Tests")
    class FundamentalsUpdateTests {

        @Test
        @DisplayName("Should update currentPrice in existing InstrumentFundamentals")
        void shouldUpdateCurrentPriceInFundamentals() throws Exception {
            // Create existing fundamentals for AAPL with old price
            InstrumentFundamentals fundamentals = InstrumentFundamentals.builder()
                    .ticker("AAPL")
                    .companyName("Apple Inc")
                    .currentPrice(new BigDecimal("170.00"))
                    .currency("USD")
                    .build();
            instrumentFundamentalsRepository.save(fundamentals);

            // Send webhook with new price
            String payload = createTradePayload("AAPL", 175.50, 1732285432000L, 1000);

            mockMvc.perform(post(WEBHOOK_ENDPOINT)
                    .header(SECRET_HEADER, webhookSecret)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
                    .andExpect(status().isOk());

            // Wait for async processing and then verify - need to poll as it runs in another thread
            waitForAsyncProcessing(1);
            Thread.sleep(200); // Extra time for DB update

            // Verify price was updated
            InstrumentFundamentals updated = instrumentFundamentalsRepository.findByTickerIgnoreCase("AAPL").orElseThrow();
            assertThat(updated.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("175.50"));
        }

        @Test
        @DisplayName("Should calculate daily change percent when updating price")
        void shouldCalculateDailyChangePercent() throws Exception {
            // Create existing fundamentals with old price (no daily change)
            InstrumentFundamentals fundamentals = InstrumentFundamentals.builder()
                    .ticker("AAPL")
                    .companyName("Apple Inc")
                    .currentPrice(new BigDecimal("100.00"))
                    .currency("USD")
                    .build();
            instrumentFundamentalsRepository.save(fundamentals);

            // Send webhook with 10% higher price
            String payload = createTradePayload("AAPL", 110.00, 1732285432000L, 1000);

            mockMvc.perform(post(WEBHOOK_ENDPOINT)
                    .header(SECRET_HEADER, webhookSecret)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
                    .andExpect(status().isOk());

            // Wait for async processing and then verify
            waitForAsyncProcessing(1);
            Thread.sleep(200); // Extra time for DB update

            InstrumentFundamentals updated = instrumentFundamentalsRepository.findByTickerIgnoreCase("AAPL").orElseThrow();
            // Change should be (110 - 100) / 100 * 100 = 10%
            assertThat(updated.getDailyChangePercent()).isEqualByComparingTo(new BigDecimal("10.0000"));
        }

        @Test
        @DisplayName("Should ignore ticks for untracked tickers (not in fundamentals)")
        void shouldIgnoreTicksForUntrackedTickers() throws Exception {
            // No fundamentals exist for AAPL - it's not tracked
            String payload = createTradePayload("AAPL", 172.15, 1732285432000L, 1000);

            mockMvc.perform(post(WEBHOOK_ENDPOINT)
                    .header(SECRET_HEADER, webhookSecret)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
                    .andExpect(status().isOk());

            Thread.sleep(100);

            // Tick should NOT be saved for untracked ticker
            assertThat(marketPriceTickRepository.findAll()).isEmpty();
            // No fundamentals created either
            assertThat(instrumentFundamentalsRepository.findByTickerIgnoreCase("AAPL")).isEmpty();
        }

        @Test
        @DisplayName("Should handle case-insensitive ticker matching for fundamentals")
        void shouldHandleCaseInsensitiveTickerMatching() throws Exception {
            // Create fundamentals with uppercase ticker
            InstrumentFundamentals fundamentals = InstrumentFundamentals.builder()
                    .ticker("AAPL")
                    .companyName("Apple Inc")
                    .currentPrice(new BigDecimal("170.00"))
                    .currency("USD")
                    .build();
            instrumentFundamentalsRepository.save(fundamentals);

            // Send webhook with lowercase ticker
            String payload = createTradePayload("aapl", 175.50, 1732285432000L, 1000);

            mockMvc.perform(post(WEBHOOK_ENDPOINT)
                    .header(SECRET_HEADER, webhookSecret)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
                    .andExpect(status().isOk());

            // Wait for async processing
            waitForAsyncProcessing(1);
            Thread.sleep(200); // Extra time for DB update

            // Should still update the fundamentals
            InstrumentFundamentals updated = instrumentFundamentalsRepository.findByTickerIgnoreCase("AAPL").orElseThrow();
            assertThat(updated.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("175.50"));
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should return 400 for invalid JSON")
        void shouldReturn400ForInvalidJson() throws Exception {
            mockMvc.perform(post(WEBHOOK_ENDPOINT)
                    .header(SECRET_HEADER, webhookSecret)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("not valid json"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle empty data array gracefully")
        void shouldHandleEmptyDataArray() throws Exception {
            String payload = """
                {
                  "event": "trade",
                  "data": []
                }
                """;

            mockMvc.perform(post(WEBHOOK_ENDPOINT)
                    .header(SECRET_HEADER, webhookSecret)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
                    .andExpect(status().isOk());

            assertThat(marketPriceTickRepository.findAll()).isEmpty();
        }

        @Test
        @DisplayName("Should handle null data gracefully")
        void shouldHandleNullData() throws Exception {
            String payload = """
                {
                  "event": "trade"
                }
                """;

            mockMvc.perform(post(WEBHOOK_ENDPOINT)
                    .header(SECRET_HEADER, webhookSecret)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
                    .andExpect(status().isOk());

            assertThat(marketPriceTickRepository.findAll()).isEmpty();
        }
    }

    /**
     * Helper method to create a trade payload JSON string.
     */
    private String createTradePayload(String symbol, double price, long timestamp, int volume) {
        return String.format(java.util.Locale.US, """
            {
              "event": "trade",
              "data": [
                {"s": "%s", "p": %.2f, "t": %d, "v": %d}
              ]
            }
            """, symbol, price, timestamp, volume);
    }

    /**
     * Helper method to create instrument fundamentals for a ticker.
     * This is needed because only tracked tickers (with fundamentals) get price ticks saved.
     */
    private void createFundamentalsForTicker(String ticker) {
        InstrumentFundamentals fundamentals = InstrumentFundamentals.builder()
                .ticker(ticker)
                .companyName(ticker + " Company")
                .currentPrice(new BigDecimal("100.00"))
                .currency("USD")
                .build();
        instrumentFundamentalsRepository.save(fundamentals);
    }
}
