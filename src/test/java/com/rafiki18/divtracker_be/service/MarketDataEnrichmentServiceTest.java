package com.rafiki18.divtracker_be.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.rafiki18.divtracker_be.model.InstrumentFundamentals;

@ExtendWith(MockitoExtension.class)
@DisplayName("MarketDataEnrichmentService Tests")
class MarketDataEnrichmentServiceTest {

    @Mock
    private InstrumentFundamentalsService fundamentalsService;

    @InjectMocks
    private MarketDataEnrichmentService enrichmentService;

    private InstrumentFundamentals completeFundamentals;

    @BeforeEach
    void setUp() {
        completeFundamentals = InstrumentFundamentals.builder()
                .ticker("AAPL")
                .companyName("Apple Inc.")
                .currentPrice(new BigDecimal("173.50"))
                .fcfPerShareTTM(new BigDecimal("6.32"))
                .peTTM(new BigDecimal("27.45"))
                .beta(new BigDecimal("1.29"))
                .dataQuality(InstrumentFundamentals.DataQuality.COMPLETE)
                .source(InstrumentFundamentals.DataSource.FINNHUB)
                .lastUpdatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should fetch complete market data array")
    void shouldFetchCompleteMarketData() {
        // Given
        when(fundamentalsService.getFundamentals("AAPL")).thenReturn(Optional.of(completeFundamentals));

        // When
        BigDecimal[] result = enrichmentService.fetchMarketData("AAPL");

        // Then
        assertThat(result).hasSize(4);
        assertThat(result[0]).isEqualByComparingTo("173.50"); // price
        assertThat(result[1]).isEqualByComparingTo("6.32");   // fcf
        assertThat(result[2]).isEqualByComparingTo("27.45");  // pe
        assertThat(result[3]).isEqualByComparingTo("1.29");   // beta
        
        verify(fundamentalsService).getFundamentals("AAPL");
    }

    @Test
    @DisplayName("Should return nulls when no fundamentals available")
    void shouldReturnNullsWhenNoFundamentals() {
        // Given
        when(fundamentalsService.getFundamentals("INVALID")).thenReturn(Optional.empty());

        // When
        BigDecimal[] result = enrichmentService.fetchMarketData("INVALID");

        // Then
        assertThat(result).hasSize(4);
        assertThat(result[0]).isNull();
        assertThat(result[1]).isNull();
        assertThat(result[2]).isNull();
        assertThat(result[3]).isNull();
        
        verify(fundamentalsService).getFundamentals("INVALID");
    }

    @Test
    @DisplayName("Should handle partial fundamentals with missing metrics")
    void shouldHandlePartialFundamentals() {
        // Given
        InstrumentFundamentals partialFundamentals = InstrumentFundamentals.builder()
                .ticker("PARTIAL")
                .currentPrice(new BigDecimal("100.00"))
                .fcfPerShareTTM(new BigDecimal("5.00"))
                // Missing PE and beta
                .dataQuality(InstrumentFundamentals.DataQuality.PARTIAL)
                .lastUpdatedAt(LocalDateTime.now())
                .build();
        
        when(fundamentalsService.getFundamentals("PARTIAL")).thenReturn(Optional.of(partialFundamentals));

        // When
        BigDecimal[] result = enrichmentService.fetchMarketData("PARTIAL");

        // Then
        assertThat(result).hasSize(4);
        assertThat(result[0]).isEqualByComparingTo("100.00");
        assertThat(result[1]).isEqualByComparingTo("5.00");
        assertThat(result[2]).isNull(); // PE missing
        assertThat(result[3]).isNull(); // beta missing
    }

    @Test
    @DisplayName("Should use getBestFcfPerShare for FCF value")
    void shouldUseBestFcfPerShare() {
        // Given - fundamentals with both TTM and Annual FCF
        InstrumentFundamentals fundamentalsWithBothFcf = InstrumentFundamentals.builder()
                .ticker("MSFT")
                .currentPrice(new BigDecimal("379.95"))
                .fcfPerShareTTM(new BigDecimal("9.12"))
                .fcfPerShareAnnual(new BigDecimal("8.50"))
                .peTTM(new BigDecimal("35.21"))
                .beta(new BigDecimal("0.89"))
                .dataQuality(InstrumentFundamentals.DataQuality.COMPLETE)
                .lastUpdatedAt(LocalDateTime.now())
                .build();
        
        when(fundamentalsService.getFundamentals("MSFT")).thenReturn(Optional.of(fundamentalsWithBothFcf));

        // When
        BigDecimal[] result = enrichmentService.fetchMarketData("MSFT");

        // Then
        // Should prefer TTM over Annual
        assertThat(result[1]).isEqualByComparingTo("9.12");
    }

    @Test
    @DisplayName("Should get full fundamentals object")
    void shouldGetFullFundamentals() {
        // Given
        when(fundamentalsService.getFundamentals("AAPL")).thenReturn(Optional.of(completeFundamentals));

        // When
        InstrumentFundamentals result = enrichmentService.getFundamentals("AAPL");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTicker()).isEqualTo("AAPL");
        assertThat(result.getCompanyName()).isEqualTo("Apple Inc.");
        assertThat(result.getDataQuality()).isEqualTo(InstrumentFundamentals.DataQuality.COMPLETE);
        
        verify(fundamentalsService).getFundamentals("AAPL");
    }

    @Test
    @DisplayName("Should return null when fundamentals not found")
    void shouldReturnNullWhenFundamentalsNotFound() {
        // Given
        when(fundamentalsService.getFundamentals("NOTFOUND")).thenReturn(Optional.empty());

        // When
        InstrumentFundamentals result = enrichmentService.getFundamentals("NOTFOUND");

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should always report as available with caching")
    void shouldAlwaysBeAvailable() {
        // When
        boolean available = enrichmentService.isAvailable();

        // Then
        assertThat(available).isTrue();
    }

    @Test
    @DisplayName("Should force refresh market data")
    void shouldForceRefreshMarketData() {
        // Given
        String ticker = "AAPL";

        // When
        enrichmentService.refreshMarketData(ticker);

        // Then
        verify(fundamentalsService).refreshFundamentals(ticker);
    }

    @Test
    @DisplayName("Should handle stale fundamentals gracefully")
    void shouldHandleStaleFundamentalsGracefully() {
        // Given
        InstrumentFundamentals staleFundamentals = InstrumentFundamentals.builder()
                .ticker("STALE")
                .currentPrice(new BigDecimal("50.00"))
                .fcfPerShareTTM(new BigDecimal("2.50"))
                .peTTM(new BigDecimal("20.00"))
                .beta(new BigDecimal("1.15"))
                .dataQuality(InstrumentFundamentals.DataQuality.STALE)
                .lastUpdatedAt(LocalDateTime.now().minusDays(2))
                .build();
        
        when(fundamentalsService.getFundamentals("STALE")).thenReturn(Optional.of(staleFundamentals));

        // When
        BigDecimal[] result = enrichmentService.fetchMarketData("STALE");

        // Then - Should still return stale data (better than nothing)
        assertThat(result).hasSize(4);
        assertThat(result[0]).isEqualByComparingTo("50.00");
        assertThat(result[1]).isEqualByComparingTo("2.50");
        assertThat(result[2]).isEqualByComparingTo("20.00");
        assertThat(result[3]).isEqualByComparingTo("1.15");
    }

    @Test
    @DisplayName("Should normalize ticker case")
    void shouldNormalizeTickerCase() {
        // Given
        when(fundamentalsService.getFundamentals(anyString())).thenReturn(Optional.of(completeFundamentals));

        // When
        enrichmentService.fetchMarketData("aapl");
        enrichmentService.fetchMarketData("AAPL");
        enrichmentService.fetchMarketData("AaPl");

        // Then
        verify(fundamentalsService).getFundamentals("aapl");
        verify(fundamentalsService).getFundamentals("AAPL");
        verify(fundamentalsService).getFundamentals("AaPl");
    }
}
