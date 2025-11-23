package com.rafiki18.divtracker_be.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for calculating advanced financial metrics and valuations.
 * Implements DCF, IRR, FCF Yield, Margin of Safety, Payback Period, and ROI calculations.
 */
@Service
@Slf4j
public class FinancialMetricsService {

    private static final int SCALE = 4;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final int MAX_IRR_ITERATIONS = 100;
    private static final BigDecimal IRR_TOLERANCE = new BigDecimal("0.0001");

    /**
     * Calculate Free Cash Flow Yield as a percentage.
     * FCF Yield = (FCF per Share / Current Price) × 100
     *
     * @param fcfPerShare Free cash flow per share
     * @param currentPrice Current market price
     * @return FCF Yield percentage, or null if inputs are invalid
     */
    public BigDecimal calculateFcfYield(BigDecimal fcfPerShare, BigDecimal currentPrice) {
        if (fcfPerShare == null || currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("Invalid inputs for FCF Yield calculation");
            return null;
        }

        try {
            return fcfPerShare.divide(currentPrice, SCALE, ROUNDING)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, ROUNDING);
        } catch (ArithmeticException e) {
            log.warn("Error calculating FCF Yield: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Calculate intrinsic value using Discounted Cash Flow (DCF) model.
     * Uses Gordon Growth Model for terminal value.
     *
     * @param currentFcf Current free cash flow per share
     * @param growthRate Expected annual FCF growth rate (as decimal, e.g., 0.08 for 8%)
     * @param discountRate Required rate of return / WACC (as decimal)
     * @param projectionYears Number of years to project
     * @return Intrinsic value per share, or null if calculation fails
     */
    public BigDecimal calculateDCF(BigDecimal currentFcf, BigDecimal growthRate, 
                                   BigDecimal discountRate, Integer projectionYears) {
        if (currentFcf == null || growthRate == null || discountRate == null || projectionYears == null) {
            log.debug("Missing required inputs for DCF calculation");
            return null;
        }

        if (currentFcf.compareTo(BigDecimal.ZERO) <= 0 || projectionYears <= 0) {
            log.debug("Invalid FCF or projection years for DCF");
            return null;
        }

        // Relaxed check: Discount rate only needs to be greater than perpetual growth rate
        // We assume perpetual growth is half of the projection growth rate
        BigDecimal perpetualGrowth = growthRate.divide(BigDecimal.valueOf(2), SCALE, ROUNDING);
        
        if (discountRate.compareTo(perpetualGrowth) <= 0) {
            log.debug("Discount rate ({}) must be greater than perpetual growth rate ({})", discountRate, perpetualGrowth);
            return null;
        }

        try {
            BigDecimal presentValue = BigDecimal.ZERO;
            BigDecimal projectedFcf = currentFcf;

            // Calculate present value of projected cash flows
            for (int year = 1; year <= projectionYears; year++) {
                projectedFcf = projectedFcf.multiply(BigDecimal.ONE.add(growthRate));
                BigDecimal discountFactor = BigDecimal.ONE.add(discountRate)
                        .pow(year);
                BigDecimal yearPV = projectedFcf.divide(discountFactor, SCALE, ROUNDING);
                presentValue = presentValue.add(yearPV);
            }

            // Calculate terminal value using Gordon Growth Model
            // Terminal Value = Final FCF × (1 + perpetual growth) / (discount rate - perpetual growth)
            // Perpetual growth is already calculated above
            BigDecimal terminalFcf = projectedFcf.multiply(BigDecimal.ONE.add(perpetualGrowth));
            BigDecimal terminalValue = terminalFcf.divide(
                    discountRate.subtract(perpetualGrowth), SCALE, ROUNDING);
            
            // Discount terminal value to present
            BigDecimal terminalDiscountFactor = BigDecimal.ONE.add(discountRate)
                    .pow(projectionYears);
            BigDecimal terminalPV = terminalValue.divide(terminalDiscountFactor, SCALE, ROUNDING);

            BigDecimal intrinsicValue = presentValue.add(terminalPV);
            log.debug("DCF calculated: PV of cash flows={}, Terminal PV={}, Total={}",
                    presentValue, terminalPV, intrinsicValue);

            return intrinsicValue.setScale(2, ROUNDING);
        } catch (ArithmeticException e) {
            log.warn("Error calculating DCF: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Calculate margin of safety as a percentage.
     * Margin of Safety = ((Intrinsic Value - Current Price) / Intrinsic Value) × 100
     *
     * @param intrinsicValue Calculated intrinsic/fair value
     * @param currentPrice Current market price
     * @return Margin of safety percentage (positive = undervalued), or null if invalid
     */
    public BigDecimal calculateMarginOfSafety(BigDecimal intrinsicValue, BigDecimal currentPrice) {
        if (intrinsicValue == null || currentPrice == null || intrinsicValue.compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("Invalid inputs for margin of safety calculation");
            return null;
        }

        try {
            return intrinsicValue.subtract(currentPrice)
                    .divide(intrinsicValue, SCALE, ROUNDING)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, ROUNDING);
        } catch (ArithmeticException e) {
            log.warn("Error calculating margin of safety: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Calculate payback period in years.
     * Years needed to recover investment with generated FCF.
     *
     * @param currentPrice Investment amount (current price)
     * @param fcfPerShare Annual free cash flow per share
     * @param growthRate Expected annual FCF growth rate (as decimal)
     * @return Payback period in years, or null if calculation fails
     */
    public BigDecimal calculatePaybackPeriod(BigDecimal currentPrice, BigDecimal fcfPerShare, 
                                            BigDecimal growthRate) {
        if (currentPrice == null || fcfPerShare == null || growthRate == null) {
            log.debug("Missing inputs for payback period calculation");
            return null;
        }

        if (fcfPerShare.compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("FCF must be positive for payback calculation");
            return null;
        }

        try {
            BigDecimal cumulativeFcf = BigDecimal.ZERO;
            BigDecimal yearlyFcf = fcfPerShare;
            int years = 0;
            int maxYears = 50; // Reasonable limit

            while (cumulativeFcf.compareTo(currentPrice) < 0 && years < maxYears) {
                years++;
                yearlyFcf = yearlyFcf.multiply(BigDecimal.ONE.add(growthRate));
                cumulativeFcf = cumulativeFcf.add(yearlyFcf);
            }

            if (years >= maxYears) {
                log.debug("Payback period exceeds {} years", maxYears);
                return null;
            }

            // Interpolate for fractional year
            BigDecimal remainingAmount = currentPrice.subtract(
                    cumulativeFcf.subtract(yearlyFcf));
            BigDecimal fractionalYear = remainingAmount.divide(yearlyFcf, SCALE, ROUNDING);
            
            return BigDecimal.valueOf(years - 1).add(fractionalYear)
                    .setScale(1, ROUNDING);
        } catch (Exception e) {
            log.warn("Error calculating payback period: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Calculate estimated ROI over investment horizon.
     * Considers both capital appreciation and accumulated FCF.
     *
     * @param currentPrice Current market price
     * @param targetPrice Expected future price (e.g., intrinsic value)
     * @param fcfPerShare Current FCF per share
     * @param growthRate Expected annual FCF growth rate (as decimal)
     * @param years Investment horizon in years
     * @return Total ROI percentage, or null if calculation fails
     */
    public BigDecimal calculateEstimatedROI(BigDecimal currentPrice, BigDecimal targetPrice,
                                           BigDecimal fcfPerShare, BigDecimal growthRate, 
                                           Integer years) {
        if (currentPrice == null || targetPrice == null || fcfPerShare == null || 
            growthRate == null || years == null) {
            log.debug("Missing inputs for ROI calculation");
            return null;
        }

        if (currentPrice.compareTo(BigDecimal.ZERO) <= 0 || years <= 0) {
            log.debug("Invalid price or years for ROI calculation");
            return null;
        }

        try {
            // Capital appreciation
            BigDecimal capitalGain = targetPrice.subtract(currentPrice);

            // Accumulated FCF over the period
            BigDecimal accumulatedFcf = BigDecimal.ZERO;
            BigDecimal yearlyFcf = fcfPerShare;
            
            for (int year = 1; year <= years; year++) {
                yearlyFcf = yearlyFcf.multiply(BigDecimal.ONE.add(growthRate));
                accumulatedFcf = accumulatedFcf.add(yearlyFcf);
            }

            // Total return = capital gain + accumulated FCF
            BigDecimal totalReturn = capitalGain.add(accumulatedFcf);
            
            // ROI % = (Total Return / Initial Investment) × 100
            BigDecimal roi = totalReturn.divide(currentPrice, SCALE, ROUNDING)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, ROUNDING);

            log.debug("ROI calculated: Capital gain={}, Accumulated FCF={}, Total ROI={}%",
                    capitalGain, accumulatedFcf, roi);

            return roi;
        } catch (ArithmeticException e) {
            log.warn("Error calculating ROI: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Calculate Internal Rate of Return (IRR) using Newton-Raphson method.
     * IRR is the discount rate that makes NPV = 0.
     *
     * @param initialInvestment Initial investment (negative value expected)
     * @param cashFlows List of expected cash flows (including terminal value)
     * @return IRR as percentage, or null if calculation fails
     */
    public BigDecimal calculateIRR(BigDecimal initialInvestment, List<BigDecimal> cashFlows) {
        if (initialInvestment == null || cashFlows == null || cashFlows.isEmpty()) {
            log.debug("Invalid inputs for IRR calculation");
            return null;
        }

        if (initialInvestment.compareTo(BigDecimal.ZERO) >= 0) {
            log.debug("Initial investment should be negative for IRR calculation");
            return null;
        }

        try {
            // Build complete cash flow series
            List<BigDecimal> allCashFlows = new ArrayList<>();
            allCashFlows.add(initialInvestment);
            allCashFlows.addAll(cashFlows);

            // Initial guess: 10%
            BigDecimal rate = new BigDecimal("0.10");
            
            for (int iteration = 0; iteration < MAX_IRR_ITERATIONS; iteration++) {
                BigDecimal npv = calculateNPV(allCashFlows, rate);
                BigDecimal npvDerivative = calculateNPVDerivative(allCashFlows, rate);

                if (npvDerivative.abs().compareTo(BigDecimal.valueOf(0.00001)) < 0) {
                    log.debug("NPV derivative too small, cannot continue IRR iteration");
                    return null;
                }

                BigDecimal newRate = rate.subtract(
                        npv.divide(npvDerivative, SCALE, ROUNDING));

                if (rate.subtract(newRate).abs().compareTo(IRR_TOLERANCE) < 0) {
                    BigDecimal irr = newRate.multiply(BigDecimal.valueOf(100))
                            .setScale(2, ROUNDING);
                    log.debug("IRR converged after {} iterations: {}%", iteration + 1, irr);
                    return irr;
                }

                rate = newRate;
            }

            log.debug("IRR did not converge after {} iterations", MAX_IRR_ITERATIONS);
            return null;
        } catch (Exception e) {
            log.warn("Error calculating IRR: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Generate projected cash flows for IRR calculation.
     *
     * @param currentFcf Current FCF per share
     * @param growthRate Annual growth rate (as decimal)
     * @param years Projection period
     * @param terminalMultiple P/FCF multiple for terminal value
     * @return List of projected cash flows
     */
    public List<BigDecimal> generateProjectedCashFlows(BigDecimal currentFcf, BigDecimal growthRate,
                                                       int years, BigDecimal terminalMultiple) {
        List<BigDecimal> cashFlows = new ArrayList<>();
        BigDecimal fcf = currentFcf;

        for (int year = 1; year <= years; year++) {
            fcf = fcf.multiply(BigDecimal.ONE.add(growthRate));
            cashFlows.add(fcf);
        }

        // Add terminal value in final year
        BigDecimal terminalValue = fcf.multiply(terminalMultiple);
        BigDecimal lastYearTotal = cashFlows.get(cashFlows.size() - 1).add(terminalValue);
        cashFlows.set(cashFlows.size() - 1, lastYearTotal);

        return cashFlows;
    }

    /**
     * Calculate Net Present Value for given cash flows and discount rate.
     */
    private BigDecimal calculateNPV(List<BigDecimal> cashFlows, BigDecimal rate) {
        BigDecimal npv = BigDecimal.ZERO;
        for (int i = 0; i < cashFlows.size(); i++) {
            BigDecimal discountFactor = BigDecimal.ONE.add(rate).pow(i);
            npv = npv.add(cashFlows.get(i).divide(discountFactor, SCALE, ROUNDING));
        }
        return npv;
    }

    /**
     * Calculate derivative of NPV with respect to discount rate.
     * Used in Newton-Raphson method for IRR.
     */
    private BigDecimal calculateNPVDerivative(List<BigDecimal> cashFlows, BigDecimal rate) {
        BigDecimal derivative = BigDecimal.ZERO;
        for (int i = 1; i < cashFlows.size(); i++) {
            BigDecimal discountFactor = BigDecimal.ONE.add(rate).pow(i + 1);
            BigDecimal term = cashFlows.get(i).multiply(BigDecimal.valueOf(-i))
                    .divide(discountFactor, SCALE, ROUNDING);
            derivative = derivative.add(term);
        }
        return derivative;
    }
}
