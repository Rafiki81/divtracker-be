package com.rafiki18.divtracker_be.mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.rafiki18.divtracker_be.dto.WatchlistItemRequest;
import com.rafiki18.divtracker_be.dto.WatchlistItemResponse;
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
                                     BigDecimal currentPrice, 
                                     BigDecimal fcfPerShare) {
        if (response == null) {
            return;
        }
        
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
        
        // Usar valores por defecto si no están configurados
        BigDecimal growthRate = response.getEstimatedFcfGrowthRate() != null ? 
                response.getEstimatedFcfGrowthRate() : new BigDecimal("0.05");
        Integer horizon = response.getInvestmentHorizonYears() != null ? 
                response.getInvestmentHorizonYears() : 5;
        BigDecimal discountRate = response.getDiscountRate() != null ? 
                response.getDiscountRate() : new BigDecimal("0.10");
        
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
