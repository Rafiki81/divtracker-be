package com.rafiki18.divtracker_be.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta con los detalles de un item del watchlist")
public class WatchlistItemResponse {
    
    @Schema(description = "ID único del item", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;
    
    @Schema(description = "ID del usuario propietario", example = "123e4567-e89b-12d3-a456-426614174001")
    private UUID userId;
    
    @Schema(description = "Símbolo del ticker de la empresa", example = "AAPL")
    private String ticker;
    
    @Schema(description = "Mercado o exchange donde cotiza", example = "NASDAQ")
    private String exchange;
    
    @Schema(description = "Precio objetivo para la acción", example = "150.50")
    private BigDecimal targetPrice;
    
    @Schema(description = "Price to Free Cash Flow objetivo", example = "15.5")
    private BigDecimal targetPfcf;
    
    @Schema(description = "Notificar cuando el precio esté por debajo del objetivo", example = "false")
    private Boolean notifyWhenBelowPrice;
    
    @Schema(description = "Notas adicionales sobre la empresa")
    private String notes;

    @Schema(description = "Precio actual de mercado", example = "172.15")
    private BigDecimal currentPrice;

    @Schema(description = "FCF por acción reportado", example = "11.45")
    private BigDecimal freeCashFlowPerShare;

    // Note: PE Annual and Beta are available via fetchMarketData() but not included in response
    // to keep the API response focused on valuation metrics. Can be added if needed.

    @Schema(description = "P/FCF actual calculado", example = "15.03")
    private BigDecimal actualPfcf;

    @Schema(description = "Precio justo calculado usando el P/FCF objetivo", example = "180.00")
    private BigDecimal fairPriceByPfcf;

    @Schema(description = "Descuento (positivo) o prima (negativa) frente al precio justo calculado", example = "0.12")
    private BigDecimal discountToFairPrice;

    @Schema(description = "Desviación porcentual respecto al precio objetivo manual", example = "-0.05")
    private BigDecimal deviationFromTargetPrice;

    @Schema(description = "Indica si la acción cotiza por debajo del precio justo calculado")
    private Boolean undervalued;

    // Parámetros de valoración configurables
    @Schema(description = "Tasa de crecimiento anual esperada del FCF (decimal)", example = "0.08")
    private BigDecimal estimatedFcfGrowthRate;

    @Schema(description = "Horizonte de inversión en años", example = "5")
    private Integer investmentHorizonYears;

    @Schema(description = "Tasa de descuento / WACC (decimal)", example = "0.10")
    private BigDecimal discountRate;

    // Métricas avanzadas calculadas
    @Schema(description = "Valor intrínseco calculado por DCF", example = "195.50")
    private BigDecimal dcfFairValue;

    @Schema(description = "FCF Yield actual (porcentaje)", example = "6.65")
    private BigDecimal fcfYield;

    @Schema(description = "Margen de seguridad respecto al valor DCF (porcentaje, positivo = infravalorado)", example = "25.00")
    private BigDecimal marginOfSafety;

    @Schema(description = "Periodo de recuperación de la inversión en años", example = "7.2")
    private BigDecimal paybackPeriod;

    @Schema(description = "ROI estimado al horizonte de inversión (porcentaje)", example = "85.50")
    private BigDecimal estimatedROI;

    @Schema(description = "TIR estimada (porcentaje)", example = "12.50")
    private BigDecimal estimatedIRR;
    
    @Schema(description = "Fecha de creación del item")
    private LocalDateTime createdAt;
    
    @Schema(description = "Fecha de última actualización del item")
    private LocalDateTime updatedAt;
}
