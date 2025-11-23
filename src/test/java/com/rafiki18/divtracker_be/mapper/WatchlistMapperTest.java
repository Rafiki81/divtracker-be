package com.rafiki18.divtracker_be.mapper;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.rafiki18.divtracker_be.dto.WatchlistItemResponse;
import com.rafiki18.divtracker_be.model.InstrumentFundamentals;
import com.rafiki18.divtracker_be.service.FinancialMetricsService;

@ExtendWith(MockitoExtension.class)
class WatchlistMapperTest {

    @Mock
    private FinancialMetricsService financialMetricsService;

    @InjectMocks
    private WatchlistMapper mapper;

    private WatchlistItemResponse response;
    private InstrumentFundamentals fundamentals;

    @BeforeEach
    void setUp() {
        response = WatchlistItemResponse.builder()
                .ticker("AAPL")
                .targetPfcf(new BigDecimal("15.0"))
                .build();

        fundamentals = InstrumentFundamentals.builder()
                .ticker("AAPL")
                .currentPrice(new BigDecimal("100.00"))
                .fcfPerShareAnnual(new BigDecimal("10.00"))
                .beta(new BigDecimal("1.2"))
                .build();
    }

    @Test
    void testEnrichWithMarketData_Undervalued_WhenPriceBelowDCF() {
        // Arrange
        BigDecimal dcfValue = new BigDecimal("120.00"); // Higher than current price (100)
        
        when(financialMetricsService.calculateDCF(any(), any(), any(), any()))
                .thenReturn(dcfValue);
        
        // Act
        mapper.enrichWithMarketData(response, fundamentals);

        // Assert
        assertThat(response.getDcfFairValue()).isEqualTo(dcfValue);
        assertThat(response.getUndervalued()).isTrue();
    }

    @Test
    void testEnrichWithMarketData_Overvalued_WhenPriceAboveDCF() {
        // Arrange
        BigDecimal dcfValue = new BigDecimal("80.00"); // Lower than current price (100)
        
        when(financialMetricsService.calculateDCF(any(), any(), any(), any()))
                .thenReturn(dcfValue);
        
        // Act
        mapper.enrichWithMarketData(response, fundamentals);

        // Assert
        assertThat(response.getDcfFairValue()).isEqualTo(dcfValue);
        assertThat(response.getUndervalued()).isFalse();
    }
}
