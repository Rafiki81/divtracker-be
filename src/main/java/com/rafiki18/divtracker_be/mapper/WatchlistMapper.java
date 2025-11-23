package com.rafiki18.divtracker_be.mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.rafiki18.divtracker_be.dto.WatchlistItemRequest;
import com.rafiki18.divtracker_be.dto.WatchlistItemResponse;
import com.rafiki18.divtracker_be.model.InstrumentFundamentals;
import com.rafiki18.divtracker_be.model.WatchlistItem;
import com.rafiki18.divtracker_be.service.FinancialMetricsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class WatchlistMapper {
    
    private final FinancialMetricsService financialMetricsService;
    
    /**
     * Convierte un Request DTO a Entity
     */
    public WatchlistItem toEntity(WatchlistItemRequest request, UUID userId) {
        if (request == null) {
            return null;
        }
        
        return WatchlistItem.builder()
                .userId(userId)
                .ticker(normalizeTicker(request.getTicker()))
                .exchange(request.getExchange())
                .targetPrice(request.getTargetPrice())
                .targetPfcf(request.getTargetPfcf())
                .notifyWhenBelowPrice(Boolean.TRUE.equals(request.getNotifyWhenBelowPrice()))
                .notes(request.getNotes())
                .estimatedFcfGrowthRate(request.getEstimatedFcfGrowthRate())
                .investmentHorizonYears(request.getInvestmentHorizonYears())
                .discountRate(request.getDiscountRate())
                .build();
    }
    
    /**
     * Actualiza una entidad existente con datos del Request (merge parcial)
     */
    public void updateEntityFromRequest(WatchlistItem entity, WatchlistItemRequest request) {
        if (request == null || entity == null) {
            return;
        }
        
        if (request.getTicker() != null) {
            entity.setTicker(normalizeTicker(request.getTicker()));
        }
        
        if (request.getExchange() != null) {
            entity.setExchange(request.getExchange());
        }
        
        if (request.getTargetPrice() != null) {
            entity.setTargetPrice(request.getTargetPrice());
        }
        
        if (request.getTargetPfcf() != null) {
            entity.setTargetPfcf(request.getTargetPfcf());
        }
        
        if (request.getNotifyWhenBelowPrice() != null) {
            entity.setNotifyWhenBelowPrice(request.getNotifyWhenBelowPrice());
        }
        
        if (request.getNotes() != null) {
            entity.setNotes(request.getNotes());
        }
        
        if (request.getEstimatedFcfGrowthRate() != null) {
            entity.setEstimatedFcfGrowthRate(request.getEstimatedFcfGrowthRate());
        }
        
        if (request.getInvestmentHorizonYears() != null) {
            entity.setInvestmentHorizonYears(request.getInvestmentHorizonYears());
        }
        
        if (request.getDiscountRate() != null) {
            entity.setDiscountRate(request.getDiscountRate());
        }
    }
    
    /**
     * Convierte una Entity a Response DTO (sin enriquecimiento de mercado)
     */
    public WatchlistItemResponse toResponse(WatchlistItem entity) {
        if (entity == null) {
            return null;
        }
        
        WatchlistItemResponse.WatchlistItemResponseBuilder builder = WatchlistItemResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .ticker(entity.getTicker())
                .exchange(entity.getExchange())
                .targetPrice(entity.getTargetPrice())
                .targetPfcf(entity.getTargetPfcf())
                .notifyWhenBelowPrice(entity.getNotifyWhenBelowPrice())
                .notes(entity.getNotes())
                .estimatedFcfGrowthRate(entity.getEstimatedFcfGrowthRate())
                .investmentHorizonYears(entity.getInvestmentHorizonYears())
                .discountRate(entity.getDiscountRate())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt());
        
        return builder.build();
    }
    
    /**
     * Enriquece la respuesta con datos de mercado y métricas financieras avanzadas
     */
    public void enrichWithMarketData(WatchlistItemResponse response, 
                                     InstrumentFundamentals fundamentals) {
        if (response == null || fundamentals == null) {
            return;
        }
        
        BigDecimal currentPrice = fundamentals.getCurrentPrice();
        BigDecimal fcfPerShare = fundamentals.getFcfPerShare();
        
        if (currentPrice != null) {
            response.setCurrentPrice(currentPrice);
            
            // Desviación del precio objetivo manual
            if (response.getTargetPrice() != null) {
                BigDecimal deviation = response.getTargetPrice().subtract(currentPrice)
                        .divide(response.getTargetPrice(), 4, java.math.RoundingMode.HALF_UP);
                response.setDeviationFromTargetPrice(deviation);
            }
        }
        
        if (fcfPerShare != null) {
            response.setFreeCashFlowPerShare(fcfPerShare);
        }
        
        // Si falta alguno de los dos datos críticos, no podemos calcular métricas de valoración
        if (currentPrice == null || fcfPerShare == null) {
            return;
        }
        
        // 1. Determine Growth Rate
        BigDecimal growthRate = response.getEstimatedFcfGrowthRate();
        if (growthRate == null) {
            // Try FOCF CAGR first (most relevant)
            if (fundamentals.getFocfCagr5Y() != null) {
                growthRate = fundamentals.getFocfCagr5Y().divide(new BigDecimal("100"), 4, java.math.RoundingMode.HALF_UP);
            } 
            // Fallback to average of other growth metrics
            else {
                BigDecimal sum = BigDecimal.ZERO;
                int count = 0;
                
                if (fundamentals.getEpsGrowth5Y() != null) {
                    sum = sum.add(fundamentals.getEpsGrowth5Y());
                    count++;
                }
                if (fundamentals.getRevenueGrowth5Y() != null) {
                    sum = sum.add(fundamentals.getRevenueGrowth5Y());
                    count++;
                }
                if (fundamentals.getDividendGrowthRate5Y() != null) {
                    sum = sum.add(fundamentals.getDividendGrowthRate5Y());
                    count++;
                }
                
                if (count > 0) {
                    growthRate = sum.divide(BigDecimal.valueOf(count), 4, java.math.RoundingMode.HALF_UP)
                            .divide(new BigDecimal("100"), 4, java.math.RoundingMode.HALF_UP);
                } else {
                    growthRate = new BigDecimal("0.05"); // Default 5%
                }
            }
            
            // Cap growth rate for safety (e.g., max 15%)
            if (growthRate.compareTo(new BigDecimal("0.15")) > 0) {
                growthRate = new BigDecimal("0.15");
            }
        }
        
        // 2. Determine Discount Rate
        BigDecimal discountRate = response.getDiscountRate();
        if (discountRate == null) {
            // Use CAPM if Beta is available
            // Risk Free Rate (10Y Treasury) ~ 4.4%
            // Equity Risk Premium ~ 5.5%
            if (fundamentals.getBeta() != null) {
                BigDecimal riskFree = new BigDecimal("0.044");
                BigDecimal marketPremium = new BigDecimal("0.055");
                discountRate = riskFree.add(fundamentals.getBeta().multiply(marketPremium))
                        .setScale(4, java.math.RoundingMode.HALF_UP);
                
                // Clamp discount rate between 6% and 15%
                if (discountRate.compareTo(new BigDecimal("0.06")) < 0) {
                    discountRate = new BigDecimal("0.06");
                } else if (discountRate.compareTo(new BigDecimal("0.15")) > 0) {
                    discountRate = new BigDecimal("0.15");
                }
            } else {
                discountRate = new BigDecimal("0.10"); // Default 10%
            }
        }
        
        Integer horizon = response.getInvestmentHorizonYears() != null ? 
                response.getInvestmentHorizonYears() : 5;
        
        // Calcular P/FCF actual
        if (fcfPerShare.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal actualPfcf = currentPrice.divide(fcfPerShare, 4, java.math.RoundingMode.HALF_UP);
            response.setActualPfcf(actualPfcf);
        }
        
        // Calcular precio justo por P/FCF objetivo
        if (response.getTargetPfcf() != null && fcfPerShare.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal fairPrice = response.getTargetPfcf().multiply(fcfPerShare);
            response.setFairPriceByPfcf(fairPrice);
            
            BigDecimal discount = fairPrice.subtract(currentPrice)
                    .divide(fairPrice, 4, java.math.RoundingMode.HALF_UP);
            response.setDiscountToFairPrice(discount);
            response.setUndervalued(discount.compareTo(BigDecimal.ZERO) > 0);
        }
        
        // Desviación del precio objetivo manual
        if (response.getTargetPrice() != null) {
            BigDecimal deviation = response.getTargetPrice().subtract(currentPrice)
                    .divide(response.getTargetPrice(), 4, java.math.RoundingMode.HALF_UP);
            response.setDeviationFromTargetPrice(deviation);
        }
        
        // ===== MÉTRICAS AVANZADAS =====
        
        // FCF Yield
        BigDecimal fcfYield = financialMetricsService.calculateFcfYield(fcfPerShare, currentPrice);
        response.setFcfYield(fcfYield);
        
        // DCF (Discounted Cash Flow)
        log.debug("Calculating DCF for {}: fcf={}, growth={}, discount={}, horizon={}", 
                response.getTicker(), fcfPerShare, growthRate, discountRate, horizon);
        
        BigDecimal dcfValue = financialMetricsService.calculateDCF(
                fcfPerShare, growthRate, discountRate, horizon);
        response.setDcfFairValue(dcfValue);
        
        // Margen de seguridad (respecto al DCF)
        if (dcfValue != null) {
            BigDecimal marginOfSafety = financialMetricsService.calculateMarginOfSafety(
                    dcfValue, currentPrice);
            response.setMarginOfSafety(marginOfSafety);
        }
        
        // Payback Period
        BigDecimal paybackPeriod = financialMetricsService.calculatePaybackPeriod(
                currentPrice, fcfPerShare, growthRate);
        response.setPaybackPeriod(paybackPeriod);
        
        // ROI estimado
        BigDecimal targetForRoi = dcfValue != null ? dcfValue : 
                (response.getTargetPrice() != null ? response.getTargetPrice() : currentPrice);
        BigDecimal estimatedRoi = financialMetricsService.calculateEstimatedROI(
                currentPrice, targetForRoi, fcfPerShare, growthRate, horizon);
        response.setEstimatedROI(estimatedRoi);
        
        // TIR (IRR)
        try {
            List<BigDecimal> cashFlows = financialMetricsService.generateProjectedCashFlows(
                    fcfPerShare, growthRate, horizon, response.getTargetPfcf() != null ? 
                            response.getTargetPfcf() : new BigDecimal("15"));
            BigDecimal irr = financialMetricsService.calculateIRR(
                    currentPrice.negate(), cashFlows);
            response.setEstimatedIRR(irr);
        } catch (Exception e) {
            log.debug("Could not calculate IRR for {}: {}", response.getTicker(), e.getMessage());
        }
    }
    
    /**
     * Normaliza el ticker: trim y uppercase
     */
    private String normalizeTicker(String ticker) {
        return ticker != null ? ticker.trim().toUpperCase() : null;
    }
}
