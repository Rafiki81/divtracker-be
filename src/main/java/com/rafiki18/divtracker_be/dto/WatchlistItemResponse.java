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

    @Schema(description = "Variación porcentual diaria", example = "1.25")
    private BigDecimal dailyChangePercent;

    @Schema(description = "Capitalización de mercado en USD", example = "2850000000000")
    private BigDecimal marketCapitalization;

    @Schema(description = "Máximo de 52 semanas", example = "199.62")
    private BigDecimal weekHigh52;

    @Schema(description = "Mínimo de 52 semanas", example = "124.17")
    private BigDecimal weekLow52;

    @Schema(description = "Posición en rango 52 semanas (0=mínimo, 1=máximo). <0.3 potencialmente infravalorado", example = "0.65")
    private BigDecimal weekRange52Position;

    @Schema(description = "FCF por acción reportado", example = "11.45")
    private BigDecimal freeCashFlowPerShare;

    @Schema(description = "Beta de la acción (volatilidad)", example = "0.85")
    private BigDecimal beta;

    @Schema(description = "CAGR del FCF Operativo a 5 años (Finnhub)", example = "12.50")
    private BigDecimal focfCagr5Y;

    @Schema(description = "PER Anual", example = "25.4")
    private BigDecimal peAnnual;

    @Schema(description = "Rentabilidad por dividendo anual (%)", example = "3.50")
    private BigDecimal dividendYield;

    @Schema(description = "Crecimiento del dividendo a 5 años (%)", example = "8.50")
    private BigDecimal dividendGrowthRate5Y;

    @Schema(description = "Cobertura del dividendo (FCF/Dividendo). >1.5 es saludable", example = "2.15")
    private BigDecimal dividendCoverageRatio;

    @Schema(description = "Payout Ratio sobre FCF como ratio (0.45 = 45%). <0.70 es sostenible", example = "0.45")
    private BigDecimal payoutRatioFcf;

    @Schema(description = "Regla de Chowder = Yield% + DGR5Y%. ≥12 es buena oportunidad", example = "12.50")
    private BigDecimal chowderRuleValue;

    @Schema(description = "P/FCF actual calculado", example = "15.03")
    private BigDecimal actualPfcf;

    @Schema(description = "Precio justo calculado usando el P/FCF objetivo", example = "180.00")
    private BigDecimal fairPriceByPfcf;

    @Schema(description = "Descuento (positivo) o prima (negativa) frente al precio justo calculado", example = "0.12")
    private BigDecimal discountToFairPrice;

    @Schema(description = "Desviación porcentual respecto al precio objetivo manual", example = "-0.05")
    private BigDecimal deviationFromTargetPrice;

    @Schema(description = "Indica si la acción cotiza por debajo del DCF (Golden Rule: Precio < DCF)", example = "true")
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
    
    @Schema(description = "Fecha de creación del item", example = "2024-11-22T10:30:00")
    private LocalDateTime createdAt;
    
    @Schema(description = "Fecha de última actualización del item", example = "2024-11-22T15:45:00")
    private LocalDateTime updatedAt;
}
