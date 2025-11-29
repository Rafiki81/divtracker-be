package com.rafiki18.divtracker_be.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.rafiki18.divtracker_be.model.InstrumentFundamentals;
import com.rafiki18.divtracker_be.model.MarketPriceTick;
import com.rafiki18.divtracker_be.repository.InstrumentFundamentalsRepository;
import com.rafiki18.divtracker_be.repository.MarketPriceTickRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebhookProcessingService Tests")
class WebhookProcessingServiceTest {

    @Mock
    private MarketPriceTickRepository marketPriceTickRepository;

    @Mock
    private InstrumentFundamentalsRepository instrumentFundamentalsRepository;

    @InjectMocks
    private WebhookProcessingService webhookProcessingService;

    @Captor
    private ArgumentCaptor<MarketPriceTick> tickCaptor;

    @Captor
    private ArgumentCaptor<InstrumentFundamentals> fundamentalsCaptor;

    @Nested
    @DisplayName("processWebhookPayload() method")
    class ProcessWebhookPayloadTests {

        @Test
        @DisplayName("should ignore non-trade events")
        void processWebhookPayload_nonTradeEvent_ignoresPayload() {
            Map<String, Object> payload = Map.of(
                    "event", "ping",
                    "data", List.of()
            );

            webhookProcessingService.processWebhookPayload(payload);

            verify(marketPriceTickRepository, never()).save(any());
            verify(instrumentFundamentalsRepository, never()).save(any());
        }

        @Test
        @DisplayName("should ignore payload with null data")
        void processWebhookPayload_nullData_ignoresPayload() {
            Map<String, Object> payload = new HashMap<>();
            payload.put("event", "trade");
            payload.put("data", null);

            webhookProcessingService.processWebhookPayload(payload);

            verify(marketPriceTickRepository, never()).save(any());
        }

        @Test
        @DisplayName("should ignore payload with empty data")
        void processWebhookPayload_emptyData_ignoresPayload() {
            Map<String, Object> payload = Map.of(
                    "event", "trade",
                    "data", List.of()
            );

            webhookProcessingService.processWebhookPayload(payload);

            verify(marketPriceTickRepository, never()).save(any());
        }

        @Test
        @DisplayName("should process valid trade data and save tick")
        void processWebhookPayload_validTrade_savesTick() {
            InstrumentFundamentals fundamentals = InstrumentFundamentals.builder()
                    .ticker("AAPL")
                    .currentPrice(new BigDecimal("170.00"))
                    .build();

            when(instrumentFundamentalsRepository.findByTickerIgnoreCase("AAPL"))
                    .thenReturn(Optional.of(fundamentals));

            Map<String, Object> trade = Map.of(
                    "s", "AAPL",
                    "p", "175.50",
                    "t", String.valueOf(System.currentTimeMillis()),
                    "v", "1000"
            );

            Map<String, Object> payload = Map.of(
                    "event", "trade",
                    "data", List.of(trade)
            );

            webhookProcessingService.processWebhookPayload(payload);

            verify(marketPriceTickRepository).save(tickCaptor.capture());
            MarketPriceTick savedTick = tickCaptor.getValue();

            assertThat(savedTick.getTicker()).isEqualTo("AAPL");
            assertThat(savedTick.getPrice()).isEqualByComparingTo(new BigDecimal("175.50"));
            assertThat(savedTick.getVolume()).isEqualByComparingTo(new BigDecimal("1000"));
            assertThat(savedTick.getSource()).isEqualTo("FINNHUB_WEBHOOK");
        }

        @Test
        @DisplayName("should update fundamentals with new price")
        void processWebhookPayload_validTrade_updatesFundamentals() {
            BigDecimal oldPrice = new BigDecimal("170.00");
            InstrumentFundamentals fundamentals = InstrumentFundamentals.builder()
                    .ticker("AAPL")
                    .currentPrice(oldPrice)
                    .build();

            when(instrumentFundamentalsRepository.findByTickerIgnoreCase("AAPL"))
                    .thenReturn(Optional.of(fundamentals));

            Map<String, Object> trade = Map.of(
                    "s", "AAPL",
                    "p", "175.50",
                    "t", String.valueOf(System.currentTimeMillis()),
                    "v", "1000"
            );

            Map<String, Object> payload = Map.of(
                    "event", "trade",
                    "data", List.of(trade)
            );

            webhookProcessingService.processWebhookPayload(payload);

            verify(instrumentFundamentalsRepository).save(fundamentalsCaptor.capture());
            InstrumentFundamentals updatedFundamentals = fundamentalsCaptor.getValue();

            assertThat(updatedFundamentals.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("175.50"));
            assertThat(updatedFundamentals.getDailyChangePercent()).isNotNull();
        }

        @Test
        @DisplayName("should calculate daily change percent correctly")
        void processWebhookPayload_validTrade_calculatesDailyChange() {
            BigDecimal oldPrice = new BigDecimal("100.00");
            InstrumentFundamentals fundamentals = InstrumentFundamentals.builder()
                    .ticker("MSFT")
                    .currentPrice(oldPrice)
                    .build();

            when(instrumentFundamentalsRepository.findByTickerIgnoreCase("MSFT"))
                    .thenReturn(Optional.of(fundamentals));

            // New price is 110, change should be +10%
            Map<String, Object> trade = Map.of(
                    "s", "MSFT",
                    "p", "110.00",
                    "t", String.valueOf(System.currentTimeMillis())
            );

            Map<String, Object> payload = Map.of(
                    "event", "trade",
                    "data", List.of(trade)
            );

            webhookProcessingService.processWebhookPayload(payload);

            verify(instrumentFundamentalsRepository).save(fundamentalsCaptor.capture());
            InstrumentFundamentals updated = fundamentalsCaptor.getValue();

            assertThat(updated.getDailyChangePercent()).isEqualByComparingTo(new BigDecimal("10.0000"));
        }

        @Test
        @DisplayName("should not calculate daily change when old price is zero")
        void processWebhookPayload_zeroPreviousPrice_noDailyChange() {
            InstrumentFundamentals fundamentals = InstrumentFundamentals.builder()
                    .ticker("TSLA")
                    .currentPrice(BigDecimal.ZERO)
                    .build();

            when(instrumentFundamentalsRepository.findByTickerIgnoreCase("TSLA"))
                    .thenReturn(Optional.of(fundamentals));

            Map<String, Object> trade = Map.of(
                    "s", "TSLA",
                    "p", "250.00",
                    "t", String.valueOf(System.currentTimeMillis())
            );

            Map<String, Object> payload = Map.of(
                    "event", "trade",
                    "data", List.of(trade)
            );

            webhookProcessingService.processWebhookPayload(payload);

            verify(instrumentFundamentalsRepository).save(fundamentalsCaptor.capture());
            InstrumentFundamentals updated = fundamentalsCaptor.getValue();

            assertThat(updated.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("250.00"));
            assertThat(updated.getDailyChangePercent()).isNull();
        }

        @Test
        @DisplayName("should not calculate daily change when old price is null")
        void processWebhookPayload_nullPreviousPrice_noDailyChange() {
            InstrumentFundamentals fundamentals = InstrumentFundamentals.builder()
                    .ticker("NVDA")
                    .currentPrice(null)
                    .build();

            when(instrumentFundamentalsRepository.findByTickerIgnoreCase("NVDA"))
                    .thenReturn(Optional.of(fundamentals));

            Map<String, Object> trade = Map.of(
                    "s", "NVDA",
                    "p", "500.00",
                    "t", String.valueOf(System.currentTimeMillis())
            );

            Map<String, Object> payload = Map.of(
                    "event", "trade",
                    "data", List.of(trade)
            );

            webhookProcessingService.processWebhookPayload(payload);

            verify(instrumentFundamentalsRepository).save(fundamentalsCaptor.capture());
            InstrumentFundamentals updated = fundamentalsCaptor.getValue();

            assertThat(updated.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("500.00"));
            assertThat(updated.getDailyChangePercent()).isNull();
        }

        @Test
        @DisplayName("should ignore trades for untracked tickers")
        void processWebhookPayload_untrackedTicker_ignoresTrade() {
            when(instrumentFundamentalsRepository.findByTickerIgnoreCase("UNKNOWN"))
                    .thenReturn(Optional.empty());

            Map<String, Object> trade = Map.of(
                    "s", "UNKNOWN",
                    "p", "100.00",
                    "t", String.valueOf(System.currentTimeMillis())
            );

            Map<String, Object> payload = Map.of(
                    "event", "trade",
                    "data", List.of(trade)
            );

            webhookProcessingService.processWebhookPayload(payload);

            verify(marketPriceTickRepository, never()).save(any());
            verify(instrumentFundamentalsRepository, never()).save(any());
        }

        @Test
        @DisplayName("should handle trade without volume")
        void processWebhookPayload_noVolume_savesWithNullVolume() {
            InstrumentFundamentals fundamentals = InstrumentFundamentals.builder()
                    .ticker("GOOG")
                    .currentPrice(new BigDecimal("140.00"))
                    .build();

            when(instrumentFundamentalsRepository.findByTickerIgnoreCase("GOOG"))
                    .thenReturn(Optional.of(fundamentals));

            Map<String, Object> trade = Map.of(
                    "s", "GOOG",
                    "p", "145.00",
                    "t", String.valueOf(System.currentTimeMillis())
            );

            Map<String, Object> payload = Map.of(
                    "event", "trade",
                    "data", List.of(trade)
            );

            webhookProcessingService.processWebhookPayload(payload);

            verify(marketPriceTickRepository).save(tickCaptor.capture());
            MarketPriceTick savedTick = tickCaptor.getValue();

            assertThat(savedTick.getVolume()).isNull();
        }

        @Test
        @DisplayName("should skip trades with missing required fields")
        void processWebhookPayload_missingSymbol_skipsTrade() {
            Map<String, Object> trade = new HashMap<>();
            trade.put("p", "100.00");
            trade.put("t", String.valueOf(System.currentTimeMillis()));

            Map<String, Object> payload = Map.of(
                    "event", "trade",
                    "data", List.of(trade)
            );

            webhookProcessingService.processWebhookPayload(payload);

            verify(marketPriceTickRepository, never()).save(any());
        }

        @Test
        @DisplayName("should skip trades with missing price")
        void processWebhookPayload_missingPrice_skipsTrade() {
            Map<String, Object> trade = Map.of(
                    "s", "AAPL",
                    "t", String.valueOf(System.currentTimeMillis())
            );

            Map<String, Object> payload = Map.of(
                    "event", "trade",
                    "data", List.of(trade)
            );

            webhookProcessingService.processWebhookPayload(payload);

            verify(marketPriceTickRepository, never()).save(any());
        }

        @Test
        @DisplayName("should skip trades with missing timestamp")
        void processWebhookPayload_missingTimestamp_skipsTrade() {
            Map<String, Object> trade = Map.of(
                    "s", "AAPL",
                    "p", "175.00"
            );

            Map<String, Object> payload = Map.of(
                    "event", "trade",
                    "data", List.of(trade)
            );

            webhookProcessingService.processWebhookPayload(payload);

            verify(marketPriceTickRepository, never()).save(any());
        }

        @Test
        @DisplayName("should process multiple trades in single payload")
        void processWebhookPayload_multipleTrades_processesAll() {
            InstrumentFundamentals aaplFundamentals = InstrumentFundamentals.builder()
                    .ticker("AAPL")
                    .currentPrice(new BigDecimal("170.00"))
                    .build();

            InstrumentFundamentals msftFundamentals = InstrumentFundamentals.builder()
                    .ticker("MSFT")
                    .currentPrice(new BigDecimal("380.00"))
                    .build();

            when(instrumentFundamentalsRepository.findByTickerIgnoreCase("AAPL"))
                    .thenReturn(Optional.of(aaplFundamentals));
            when(instrumentFundamentalsRepository.findByTickerIgnoreCase("MSFT"))
                    .thenReturn(Optional.of(msftFundamentals));

            Map<String, Object> trade1 = Map.of(
                    "s", "AAPL",
                    "p", "175.00",
                    "t", String.valueOf(System.currentTimeMillis())
            );

            Map<String, Object> trade2 = Map.of(
                    "s", "MSFT",
                    "p", "385.00",
                    "t", String.valueOf(System.currentTimeMillis())
            );

            Map<String, Object> payload = Map.of(
                    "event", "trade",
                    "data", List.of(trade1, trade2)
            );

            webhookProcessingService.processWebhookPayload(payload);

            verify(marketPriceTickRepository, org.mockito.Mockito.times(2)).save(any());
            verify(instrumentFundamentalsRepository, org.mockito.Mockito.times(2)).save(any());
        }

        @Test
        @DisplayName("should normalize ticker to uppercase")
        void processWebhookPayload_lowercaseTicker_normalizesToUppercase() {
            InstrumentFundamentals fundamentals = InstrumentFundamentals.builder()
                    .ticker("AAPL")
                    .currentPrice(new BigDecimal("170.00"))
                    .build();

            when(instrumentFundamentalsRepository.findByTickerIgnoreCase("AAPL"))
                    .thenReturn(Optional.of(fundamentals));

            Map<String, Object> trade = Map.of(
                    "s", "aapl",
                    "p", "175.00",
                    "t", String.valueOf(System.currentTimeMillis())
            );

            Map<String, Object> payload = Map.of(
                    "event", "trade",
                    "data", List.of(trade)
            );

            webhookProcessingService.processWebhookPayload(payload);

            verify(marketPriceTickRepository).save(tickCaptor.capture());
            MarketPriceTick savedTick = tickCaptor.getValue();

            assertThat(savedTick.getTicker()).isEqualTo("AAPL");
        }
    }

    @Nested
    @DisplayName("processWebhookPayloadAsync() method")
    class ProcessWebhookPayloadAsyncTests {

        @Test
        @DisplayName("should delegate to synchronous processing")
        void processWebhookPayloadAsync_validPayload_delegatesToSync() {
            InstrumentFundamentals fundamentals = InstrumentFundamentals.builder()
                    .ticker("AAPL")
                    .currentPrice(new BigDecimal("170.00"))
                    .build();

            when(instrumentFundamentalsRepository.findByTickerIgnoreCase("AAPL"))
                    .thenReturn(Optional.of(fundamentals));

            Map<String, Object> trade = Map.of(
                    "s", "AAPL",
                    "p", "175.00",
                    "t", String.valueOf(System.currentTimeMillis())
            );

            Map<String, Object> payload = Map.of(
                    "event", "trade",
                    "data", List.of(trade)
            );

            webhookProcessingService.processWebhookPayloadAsync(payload);

            verify(marketPriceTickRepository).save(any());
            verify(instrumentFundamentalsRepository).save(any());
        }
    }
}
