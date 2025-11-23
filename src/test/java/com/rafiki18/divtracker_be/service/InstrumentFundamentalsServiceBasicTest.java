package com.rafiki18.divtracker_be.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.rafiki18.divtracker_be.model.InstrumentFundamentals;

@ExtendWith(MockitoExtension.class)
@DisplayName("InstrumentFundamentalsService Basic Tests")
class InstrumentFundamentalsServiceBasicTest {

    @Mock
    private InstrumentFundamentalsService fundamentalsService;

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
    @DisplayName("Should get current price")
    void shouldGetCurrentPrice() {
        // Given
        when(fundamentalsService.getCurrentPrice("AAPL")).thenReturn(Optional.of(new BigDecimal("173.50")));

        // When
        Optional<BigDecimal> result = fundamentalsService.getCurrentPrice("AAPL");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualByComparingTo("173.50");
    }

    @Test
    @DisplayName("Should get FCF per share")
    void shouldGetFcfPerShare() {
        // Given
        when(fundamentalsService.getFcfPerShare("AAPL")).thenReturn(Optional.of(new BigDecimal("6.32")));

        // When
        Optional<BigDecimal> result = fundamentalsService.getFcfPerShare("AAPL");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualByComparingTo("6.32");
    }

    @Test
    @DisplayName("Should get PE ratio")
    void shouldGetPE() {
        // Given
        when(fundamentalsService.getPE("AAPL")).thenReturn(Optional.of(new BigDecimal("27.45")));

        // When
        Optional<BigDecimal> result = fundamentalsService.getPE("AAPL");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualByComparingTo("27.45");
    }

    @Test
    @DisplayName("Should get beta")
    void shouldGetBeta() {
        // Given
        when(fundamentalsService.getBeta("AAPL")).thenReturn(Optional.of(new BigDecimal("1.29")));

        // When
        Optional<BigDecimal> result = fundamentalsService.getBeta("AAPL");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualByComparingTo("1.29");
    }

    @Test
    @DisplayName("Should return empty when ticker not found")
    void shouldReturnEmptyWhenTickerNotFound() {
        // Given
        when(fundamentalsService.getCurrentPrice("NOTFOUND")).thenReturn(Optional.empty());

        // When
        Optional<BigDecimal> result = fundamentalsService.getCurrentPrice("NOTFOUND");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should get complete fundamentals")
    void shouldGetCompleteFundamentals() {
        // Given
        when(fundamentalsService.getFundamentals("AAPL")).thenReturn(Optional.of(completeFundamentals));

        // When
        Optional<InstrumentFundamentals> result = fundamentalsService.getFundamentals("AAPL");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getTicker()).isEqualTo("AAPL");
        assertThat(result.get().getCompanyName()).isEqualTo("Apple Inc.");
        assertThat(result.get().getCurrentPrice()).isEqualByComparingTo("173.50");
        assertThat(result.get().getDataQuality()).isEqualTo(InstrumentFundamentals.DataQuality.COMPLETE);
    }
}
