package com.rafiki18.divtracker_be.service;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Financial Metrics Service Tests")
class FinancialMetricsServiceTest {

    private final FinancialMetricsService service = new FinancialMetricsService();

    @Test
    @DisplayName("Calculate FCF Yield - Valid inputs")
    void testCalculateFcfYield_ValidInputs() {
        BigDecimal fcfPerShare = new BigDecimal("10.00");
        BigDecimal currentPrice = new BigDecimal("150.00");

        BigDecimal yield = service.calculateFcfYield(fcfPerShare, currentPrice);

        assertNotNull(yield);
        assertEquals(new BigDecimal("6.67"), yield);
    }

    @Test
    @DisplayName("Calculate FCF Yield - Null inputs should return null")
    void testCalculateFcfYield_NullInputs() {
        assertNull(service.calculateFcfYield(null, new BigDecimal("150")));
        assertNull(service.calculateFcfYield(new BigDecimal("10"), null));
    }

    @Test
    @DisplayName("Calculate FCF Yield - Zero price should return null")
    void testCalculateFcfYield_ZeroPrice() {
        assertNull(service.calculateFcfYield(new BigDecimal("10"), BigDecimal.ZERO));
    }

    @Test
    @DisplayName("Calculate DCF - Valid inputs")
    void testCalculateDCF_ValidInputs() {
        BigDecimal currentFcf = new BigDecimal("10.00");
        BigDecimal growthRate = new BigDecimal("0.08");  // 8%
        BigDecimal discountRate = new BigDecimal("0.10"); // 10%
        Integer years = 5;

        BigDecimal intrinsicValue = service.calculateDCF(currentFcf, growthRate, discountRate, years);

        assertNotNull(intrinsicValue);
        assertTrue(intrinsicValue.compareTo(BigDecimal.ZERO) > 0);
        // DCF should be higher than simple sum due to terminal value
        assertTrue(intrinsicValue.compareTo(new BigDecimal("50")) > 0);
    }

    @Test
    @DisplayName("Calculate DCF - Conservative case (low growth, high discount)")
    void testCalculateDCF_ConservativeCase() {
        BigDecimal currentFcf = new BigDecimal("5.00");
        BigDecimal growthRate = new BigDecimal("0.03");  // 3%
        BigDecimal discountRate = new BigDecimal("0.12"); // 12%
        Integer years = 10;

        BigDecimal intrinsicValue = service.calculateDCF(currentFcf, growthRate, discountRate, years);

        assertNotNull(intrinsicValue);
        assertTrue(intrinsicValue.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("Calculate DCF - Null inputs should return null")
    void testCalculateDCF_NullInputs() {
        assertNull(service.calculateDCF(null, new BigDecimal("0.08"), new BigDecimal("0.10"), 5));
        assertNull(service.calculateDCF(new BigDecimal("10"), null, new BigDecimal("0.10"), 5));
    }

    @Test
    @DisplayName("Calculate DCF - Discount rate must be greater than perpetual growth rate")
    void testCalculateDCF_InvalidRates() {
        BigDecimal currentFcf = new BigDecimal("10.00");
        BigDecimal growthRate = new BigDecimal("0.12"); // Perpetual growth = 0.06
        BigDecimal discountRate = new BigDecimal("0.05"); // Lower than perpetual growth

        assertNull(service.calculateDCF(currentFcf, growthRate, discountRate, 5));
    }

    @Test
    @DisplayName("Calculate DCF - High growth case (Discount rate < Growth rate but > Perpetual growth)")
    void testCalculateDCF_HighGrowthCase() {
        BigDecimal currentFcf = new BigDecimal("10.00");
        BigDecimal growthRate = new BigDecimal("0.12"); // Perpetual growth = 0.06
        BigDecimal discountRate = new BigDecimal("0.10"); // Lower than growth, but higher than perpetual

        BigDecimal intrinsicValue = service.calculateDCF(currentFcf, growthRate, discountRate, 5);
        
        assertNotNull(intrinsicValue);
        assertTrue(intrinsicValue.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("Calculate Margin of Safety - Undervalued case")
    void testCalculateMarginOfSafety_Undervalued() {
        BigDecimal intrinsicValue = new BigDecimal("200.00");
        BigDecimal currentPrice = new BigDecimal("150.00");

        BigDecimal margin = service.calculateMarginOfSafety(intrinsicValue, currentPrice);

        assertNotNull(margin);
        assertEquals(new BigDecimal("25.00"), margin); // 25% margin of safety
    }

    @Test
    @DisplayName("Calculate Margin of Safety - Overvalued case")
    void testCalculateMarginOfSafety_Overvalued() {
        BigDecimal intrinsicValue = new BigDecimal("150.00");
        BigDecimal currentPrice = new BigDecimal("200.00");

        BigDecimal margin = service.calculateMarginOfSafety(intrinsicValue, currentPrice);

        assertNotNull(margin);
        assertTrue(margin.compareTo(BigDecimal.ZERO) < 0); // Negative margin
        assertEquals(new BigDecimal("-33.33"), margin);
    }

    @Test
    @DisplayName("Calculate Payback Period - Valid inputs")
    void testCalculatePaybackPeriod_ValidInputs() {
        BigDecimal currentPrice = new BigDecimal("100.00");
        BigDecimal fcfPerShare = new BigDecimal("10.00");
        BigDecimal growthRate = new BigDecimal("0.05"); // 5%

        BigDecimal payback = service.calculatePaybackPeriod(currentPrice, fcfPerShare, growthRate);

        assertNotNull(payback);
        assertTrue(payback.compareTo(BigDecimal.ZERO) > 0);
        // Should be around 8-9 years with 5% growth
        assertTrue(payback.compareTo(new BigDecimal("15")) < 0);
    }

    @Test
    @DisplayName("Calculate Payback Period - Zero growth")
    void testCalculatePaybackPeriod_ZeroGrowth() {
        BigDecimal currentPrice = new BigDecimal("100.00");
        BigDecimal fcfPerShare = new BigDecimal("10.00");
        BigDecimal growthRate = BigDecimal.ZERO;

        BigDecimal payback = service.calculatePaybackPeriod(currentPrice, fcfPerShare, growthRate);

        assertNotNull(payback);
        // Should be exactly 10 years (100 / 10)
        assertEquals(new BigDecimal("10.0"), payback);
    }

    @Test
    @DisplayName("Calculate Payback Period - Negative FCF should return null")
    void testCalculatePaybackPeriod_NegativeFCF() {
        assertNull(service.calculatePaybackPeriod(
                new BigDecimal("100"), 
                new BigDecimal("-5"), 
                new BigDecimal("0.05")));
    }

    @Test
    @DisplayName("Calculate Estimated ROI - Valid inputs")
    void testCalculateEstimatedROI_ValidInputs() {
        BigDecimal currentPrice = new BigDecimal("100.00");
        BigDecimal targetPrice = new BigDecimal("150.00");
        BigDecimal fcfPerShare = new BigDecimal("8.00");
        BigDecimal growthRate = new BigDecimal("0.07"); // 7%
        Integer years = 5;

        BigDecimal roi = service.calculateEstimatedROI(
                currentPrice, targetPrice, fcfPerShare, growthRate, years);

        assertNotNull(roi);
        assertTrue(roi.compareTo(BigDecimal.ZERO) > 0);
        // Should include both capital appreciation (50%) and accumulated FCF
        assertTrue(roi.compareTo(new BigDecimal("50")) > 0);
    }

    @Test
    @DisplayName("Calculate Estimated ROI - Only capital appreciation")
    void testCalculateEstimatedROI_OnlyCapitalAppreciation() {
        BigDecimal currentPrice = new BigDecimal("100.00");
        BigDecimal targetPrice = new BigDecimal("120.00");
        BigDecimal fcfPerShare = BigDecimal.ZERO; // No FCF
        BigDecimal growthRate = BigDecimal.ZERO;
        Integer years = 3;

        BigDecimal roi = service.calculateEstimatedROI(
                currentPrice, targetPrice, fcfPerShare, growthRate, years);

        assertNotNull(roi);
        assertEquals(new BigDecimal("20.00"), roi); // 20% return from price alone
    }

    @Test
    @DisplayName("Calculate IRR - Valid cash flows")
    void testCalculateIRR_ValidCashFlows() {
        BigDecimal initialInvestment = new BigDecimal("-100.00");
        List<BigDecimal> cashFlows = List.of(
                new BigDecimal("10.00"),
                new BigDecimal("12.00"),
                new BigDecimal("14.00"),
                new BigDecimal("16.00"),
                new BigDecimal("120.00") // Final year includes terminal value
        );

        BigDecimal irr = service.calculateIRR(initialInvestment, cashFlows);

        assertNotNull(irr);
        assertTrue(irr.compareTo(BigDecimal.ZERO) > 0);
        // Should be a reasonable IRR between 10-20%
        assertTrue(irr.compareTo(new BigDecimal("5")) > 0);
        assertTrue(irr.compareTo(new BigDecimal("30")) < 0);
    }

    @Test
    @DisplayName("Calculate IRR - Positive initial investment should return null")
    void testCalculateIRR_PositiveInitialInvestment() {
        BigDecimal initialInvestment = new BigDecimal("100.00"); // Positive
        List<BigDecimal> cashFlows = List.of(
                new BigDecimal("10.00"),
                new BigDecimal("110.00")
        );

        assertNull(service.calculateIRR(initialInvestment, cashFlows));
    }

    @Test
    @DisplayName("Calculate IRR - Empty cash flows should return null")
    void testCalculateIRR_EmptyCashFlows() {
        assertNull(service.calculateIRR(new BigDecimal("-100"), List.of()));
    }

    @Test
    @DisplayName("Generate Projected Cash Flows - Valid inputs")
    void testGenerateProjectedCashFlows() {
        BigDecimal currentFcf = new BigDecimal("10.00");
        BigDecimal growthRate = new BigDecimal("0.08");
        int years = 5;
        BigDecimal terminalMultiple = new BigDecimal("15.0");

        List<BigDecimal> cashFlows = service.generateProjectedCashFlows(
                currentFcf, growthRate, years, terminalMultiple);

        assertNotNull(cashFlows);
        assertEquals(5, cashFlows.size());
        
        // First year should be higher than current FCF
        assertTrue(cashFlows.get(0).compareTo(currentFcf) > 0);
        
        // Each year should grow
        for (int i = 1; i < cashFlows.size() - 1; i++) {
            assertTrue(cashFlows.get(i).compareTo(cashFlows.get(i-1)) > 0);
        }
        
        // Last year should include terminal value (much larger)
        assertTrue(cashFlows.get(4).compareTo(cashFlows.get(3).multiply(new BigDecimal("2"))) > 0);
    }

    @Test
    @DisplayName("Real world example - Apple-like valuation")
    void testRealWorldExample_AppleLike() {
        // Simulate Apple: $172 price, $11.45 FCF/share, 8% growth, 10% discount, 5 years
        BigDecimal currentPrice = new BigDecimal("172.00");
        BigDecimal fcfPerShare = new BigDecimal("11.45");
        BigDecimal growthRate = new BigDecimal("0.08");
        BigDecimal discountRate = new BigDecimal("0.10");
        Integer years = 5;

        // FCF Yield
        BigDecimal fcfYield = service.calculateFcfYield(fcfPerShare, currentPrice);
        assertNotNull(fcfYield);
        assertTrue(fcfYield.compareTo(new BigDecimal("6")) > 0); // Should be around 6.65%

        // DCF Intrinsic Value
        BigDecimal dcfValue = service.calculateDCF(fcfPerShare, growthRate, discountRate, years);
        assertNotNull(dcfValue);
        assertTrue(dcfValue.compareTo(BigDecimal.ZERO) > 0);

        // Margin of Safety
        BigDecimal margin = service.calculateMarginOfSafety(dcfValue, currentPrice);
        assertNotNull(margin);

        // Payback Period
        BigDecimal payback = service.calculatePaybackPeriod(currentPrice, fcfPerShare, growthRate);
        assertNotNull(payback);
        assertTrue(payback.compareTo(new BigDecimal("20")) < 0); // Should be under 20 years

        // ROI
        BigDecimal roi = service.calculateEstimatedROI(
                currentPrice, dcfValue, fcfPerShare, growthRate, years);
        assertNotNull(roi);
    }
}
