package com.rafiki18.divtracker_be.service;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.rafiki18.divtracker_be.dto.WatchlistItemResponse;
import com.rafiki18.divtracker_be.marketdata.FinnhubClient;

@ExtendWith(MockitoExtension.class)
@DisplayName("WatchlistValuationService Tests")
class WatchlistValuationServiceTest {

    @Mock
    private FinnhubClient finnhubClient;

    @InjectMocks
    private WatchlistValuationService valuationService;

    private WatchlistItemResponse baseResponse;

    @BeforeEach
    void setUp() {
        baseResponse = WatchlistItemResponse.builder()
                .ticker("AAPL")
                .targetPfcf(new BigDecimal("20.00"))
                .targetPrice(new BigDecimal("150.00"))
                .build();
    }

    @Nested
    @DisplayName("enrich() method")
    class EnrichTests {

        @Test
        @DisplayName("should return null when response is null")
        void enrich_nullResponse_returnsNull() {
            WatchlistItemResponse result = valuationService.enrich(null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should return unchanged response when ticker is null")
        void enrich_nullTicker_returnsUnchanged() {
            WatchlistItemResponse response = WatchlistItemResponse.builder().build();
            
            WatchlistItemResponse result = valuationService.enrich(response);
            
            assertThat(result).isEqualTo(response);
        }

        @Test
        @DisplayName("should return unchanged response when ticker is empty")
        void enrich_emptyTicker_returnsUnchanged() {
            WatchlistItemResponse response = WatchlistItemResponse.builder()
                    .ticker("")
                    .build();
            
            WatchlistItemResponse result = valuationService.enrich(response);
            
            assertThat(result).isEqualTo(response);
        }

        @Test
        @DisplayName("should return unchanged response when Finnhub is disabled")
        void enrich_finnhubDisabled_returnsUnchanged() {
            when(finnhubClient.isEnabled()).thenReturn(false);
            
            WatchlistItemResponse result = valuationService.enrich(baseResponse);
            
            assertThat(result).isEqualTo(baseResponse);
        }

        @Test
        @DisplayName("should set currentPrice when available")
        void enrich_withCurrentPrice_setsCurrentPrice() {
            BigDecimal currentPrice = new BigDecimal("175.00");
            
            when(finnhubClient.isEnabled()).thenReturn(true);
            when(finnhubClient.fetchCurrentPrice("AAPL")).thenReturn(Optional.of(currentPrice));
            when(finnhubClient.calculateFCF("AAPL")).thenReturn(Optional.empty());
            
            WatchlistItemResponse result = valuationService.enrich(baseResponse);
            
            assertThat(result.getCurrentPrice()).isEqualTo(currentPrice);
        }

        @Test
        @DisplayName("should set freeCashFlowPerShare when FCF data available")
        void enrich_withFcfData_setsFcfPerShare() {
            BigDecimal fcfPerShare = new BigDecimal("8.50");
            
            when(finnhubClient.isEnabled()).thenReturn(true);
            when(finnhubClient.fetchCurrentPrice("AAPL")).thenReturn(Optional.empty());
            when(finnhubClient.calculateFCF("AAPL")).thenReturn(Optional.of(
                    java.util.Map.of("fcfPerShare", fcfPerShare)));
            
            WatchlistItemResponse result = valuationService.enrich(baseResponse);
            
            assertThat(result.getFreeCashFlowPerShare()).isEqualTo(fcfPerShare);
        }

        @Test
        @DisplayName("should calculate actualPfcf when price and FCF available")
        void enrich_withPriceAndFcf_calculatesActualPfcf() {
            BigDecimal currentPrice = new BigDecimal("175.00");
            BigDecimal fcfPerShare = new BigDecimal("8.75");
            // actualPfcf = 175 / 8.75 = 20.0000
            
            when(finnhubClient.isEnabled()).thenReturn(true);
            when(finnhubClient.fetchCurrentPrice("AAPL")).thenReturn(Optional.of(currentPrice));
            when(finnhubClient.calculateFCF("AAPL")).thenReturn(Optional.of(
                    java.util.Map.of("fcfPerShare", fcfPerShare)));
            
            WatchlistItemResponse result = valuationService.enrich(baseResponse);
            
            assertThat(result.getActualPfcf()).isEqualByComparingTo(new BigDecimal("20.00"));
        }

        @Test
        @DisplayName("should not calculate actualPfcf when FCF is zero")
        void enrich_withZeroFcf_noActualPfcf() {
            BigDecimal currentPrice = new BigDecimal("175.00");
            BigDecimal fcfPerShare = BigDecimal.ZERO;
            
            when(finnhubClient.isEnabled()).thenReturn(true);
            when(finnhubClient.fetchCurrentPrice("AAPL")).thenReturn(Optional.of(currentPrice));
            when(finnhubClient.calculateFCF("AAPL")).thenReturn(Optional.of(
                    java.util.Map.of("fcfPerShare", fcfPerShare)));
            
            WatchlistItemResponse result = valuationService.enrich(baseResponse);
            
            assertThat(result.getActualPfcf()).isNull();
        }

        @Test
        @DisplayName("should calculate fairPriceByPfcf when targetPfcf and FCF available")
        void enrich_withTargetPfcfAndFcf_calculatesFairPrice() {
            BigDecimal currentPrice = new BigDecimal("175.00");
            BigDecimal fcfPerShare = new BigDecimal("8.50");
            // fairPrice = 8.50 × 20.00 = 170.00
            
            when(finnhubClient.isEnabled()).thenReturn(true);
            when(finnhubClient.fetchCurrentPrice("AAPL")).thenReturn(Optional.of(currentPrice));
            when(finnhubClient.calculateFCF("AAPL")).thenReturn(Optional.of(
                    java.util.Map.of("fcfPerShare", fcfPerShare)));
            
            WatchlistItemResponse result = valuationService.enrich(baseResponse);
            
            assertThat(result.getFairPriceByPfcf()).isEqualByComparingTo(new BigDecimal("170.00"));
        }

        @Test
        @DisplayName("should calculate discount when fairPrice is positive")
        void enrich_withFairPrice_calculatesDiscount() {
            BigDecimal currentPrice = new BigDecimal("160.00");
            BigDecimal fcfPerShare = new BigDecimal("8.50");
            // fairPrice = 8.50 × 20.00 = 170.00
            // discount = (170 - 160) / 170 = 0.0588
            
            when(finnhubClient.isEnabled()).thenReturn(true);
            when(finnhubClient.fetchCurrentPrice("AAPL")).thenReturn(Optional.of(currentPrice));
            when(finnhubClient.calculateFCF("AAPL")).thenReturn(Optional.of(
                    java.util.Map.of("fcfPerShare", fcfPerShare)));
            
            WatchlistItemResponse result = valuationService.enrich(baseResponse);
            
            assertThat(result.getDiscountToFairPrice()).isNotNull();
            assertThat(result.getDiscountToFairPrice().doubleValue()).isCloseTo(0.0588, org.assertj.core.data.Offset.offset(0.001));
        }

        @Test
        @DisplayName("should set undervalued=true when price < fairPrice")
        void enrich_priceUnderFairPrice_setsUndervaluedTrue() {
            BigDecimal currentPrice = new BigDecimal("160.00");
            BigDecimal fcfPerShare = new BigDecimal("8.50");
            // fairPrice = 170.00 > currentPrice = 160.00
            
            when(finnhubClient.isEnabled()).thenReturn(true);
            when(finnhubClient.fetchCurrentPrice("AAPL")).thenReturn(Optional.of(currentPrice));
            when(finnhubClient.calculateFCF("AAPL")).thenReturn(Optional.of(
                    java.util.Map.of("fcfPerShare", fcfPerShare)));
            
            WatchlistItemResponse result = valuationService.enrich(baseResponse);
            
            assertThat(result.getUndervalued()).isTrue();
        }

        @Test
        @DisplayName("should set undervalued=false when price > fairPrice")
        void enrich_priceOverFairPrice_setsUndervaluedFalse() {
            BigDecimal currentPrice = new BigDecimal("180.00");
            BigDecimal fcfPerShare = new BigDecimal("8.50");
            // fairPrice = 170.00 < currentPrice = 180.00
            
            when(finnhubClient.isEnabled()).thenReturn(true);
            when(finnhubClient.fetchCurrentPrice("AAPL")).thenReturn(Optional.of(currentPrice));
            when(finnhubClient.calculateFCF("AAPL")).thenReturn(Optional.of(
                    java.util.Map.of("fcfPerShare", fcfPerShare)));
            
            WatchlistItemResponse result = valuationService.enrich(baseResponse);
            
            assertThat(result.getUndervalued()).isFalse();
        }

        @Test
        @DisplayName("should calculate deviationFromTargetPrice when targetPrice set")
        void enrich_withTargetPrice_calculatesDeviation() {
            BigDecimal currentPrice = new BigDecimal("165.00");
            // deviation = (165 - 150) / 150 = 0.10
            
            when(finnhubClient.isEnabled()).thenReturn(true);
            when(finnhubClient.fetchCurrentPrice("AAPL")).thenReturn(Optional.of(currentPrice));
            when(finnhubClient.calculateFCF("AAPL")).thenReturn(Optional.empty());
            
            WatchlistItemResponse result = valuationService.enrich(baseResponse);
            
            assertThat(result.getDeviationFromTargetPrice()).isEqualByComparingTo(new BigDecimal("0.10"));
        }

        @Test
        @DisplayName("should not calculate deviation when targetPrice is zero")
        void enrich_withZeroTargetPrice_noDeviation() {
            WatchlistItemResponse responseWithZeroTarget = WatchlistItemResponse.builder()
                    .ticker("AAPL")
                    .targetPrice(BigDecimal.ZERO)
                    .build();
            
            BigDecimal currentPrice = new BigDecimal("165.00");
            
            when(finnhubClient.isEnabled()).thenReturn(true);
            when(finnhubClient.fetchCurrentPrice("AAPL")).thenReturn(Optional.of(currentPrice));
            when(finnhubClient.calculateFCF("AAPL")).thenReturn(Optional.empty());
            
            WatchlistItemResponse result = valuationService.enrich(responseWithZeroTarget);
            
            assertThat(result.getDeviationFromTargetPrice()).isNull();
        }

        @Test
        @DisplayName("should calculate negative deviation when price below target")
        void enrich_priceBelowTarget_negativeDeviation() {
            BigDecimal currentPrice = new BigDecimal("135.00");
            // deviation = (135 - 150) / 150 = -0.10
            
            when(finnhubClient.isEnabled()).thenReturn(true);
            when(finnhubClient.fetchCurrentPrice("AAPL")).thenReturn(Optional.of(currentPrice));
            when(finnhubClient.calculateFCF("AAPL")).thenReturn(Optional.empty());
            
            WatchlistItemResponse result = valuationService.enrich(baseResponse);
            
            assertThat(result.getDeviationFromTargetPrice()).isEqualByComparingTo(new BigDecimal("-0.10"));
        }

        @Test
        @DisplayName("should handle all calculations correctly with full data")
        void enrich_fullData_allCalculationsCorrect() {
            BigDecimal currentPrice = new BigDecimal("160.00");
            BigDecimal fcfPerShare = new BigDecimal("10.00");
            // actualPfcf = 160 / 10 = 16.00
            // fairPrice = 10 × 20 = 200.00
            // discount = (200 - 160) / 200 = 0.20
            // undervalued = true (160 < 200)
            // deviation = (160 - 150) / 150 = 0.0667
            
            when(finnhubClient.isEnabled()).thenReturn(true);
            when(finnhubClient.fetchCurrentPrice("AAPL")).thenReturn(Optional.of(currentPrice));
            when(finnhubClient.calculateFCF("AAPL")).thenReturn(Optional.of(
                    java.util.Map.of("fcfPerShare", fcfPerShare)));
            
            WatchlistItemResponse result = valuationService.enrich(baseResponse);
            
            assertThat(result.getCurrentPrice()).isEqualByComparingTo(currentPrice);
            assertThat(result.getFreeCashFlowPerShare()).isEqualByComparingTo(fcfPerShare);
            assertThat(result.getActualPfcf()).isEqualByComparingTo(new BigDecimal("16.00"));
            assertThat(result.getFairPriceByPfcf()).isEqualByComparingTo(new BigDecimal("200.00"));
            assertThat(result.getDiscountToFairPrice()).isEqualByComparingTo(new BigDecimal("0.20"));
            assertThat(result.getUndervalued()).isTrue();
            assertThat(result.getDeviationFromTargetPrice().doubleValue()).isCloseTo(0.0667, org.assertj.core.data.Offset.offset(0.001));
        }
    }
}
