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

    @Test
    void testEnrichWithMarketData_MapsNewFields() {
        // Arrange
        fundamentals.setPayoutRatioFcf(new BigDecimal("0.45"));
        fundamentals.setChowderRuleValue(new BigDecimal("12.5"));
        
        // Act
        mapper.enrichWithMarketData(response, fundamentals);

        // Assert
        assertThat(response.getPayoutRatioFcf()).isEqualTo(new BigDecimal("0.45"));
        assertThat(response.getChowderRuleValue()).isEqualTo(new BigDecimal("12.5"));
    }

    @Test
    void testEnrichWithMarketData_MapsMarketDataFields() {
        // Arrange
        fundamentals.setDailyChangePercent(new BigDecimal("1.25"));
        fundamentals.setMarketCapitalization(new BigDecimal("2850000"));
        fundamentals.setWeekHigh52(new BigDecimal("199.62"));
        fundamentals.setWeekLow52(new BigDecimal("124.17"));
        
        // Act
        mapper.enrichWithMarketData(response, fundamentals);

        // Assert
        assertThat(response.getDailyChangePercent()).isEqualTo(new BigDecimal("1.25"));
        assertThat(response.getMarketCapitalization()).isEqualTo(new BigDecimal("2850000"));
        assertThat(response.getWeekHigh52()).isEqualTo(new BigDecimal("199.62"));
        assertThat(response.getWeekLow52()).isEqualTo(new BigDecimal("124.17"));
    }

    @Test
    void testEnrichWithMarketData_Calculates52WeekRangePosition() {
        // Arrange: price=100, low=50, high=150 -> position = (100-50)/(150-50) = 0.5
        fundamentals.setCurrentPrice(new BigDecimal("100.00"));
        fundamentals.setWeekLow52(new BigDecimal("50.00"));
        fundamentals.setWeekHigh52(new BigDecimal("150.00"));
        
        // Act
        mapper.enrichWithMarketData(response, fundamentals);

        // Assert
        assertThat(response.getWeekRange52Position()).isEqualByComparingTo(new BigDecimal("0.5"));
    }
}
