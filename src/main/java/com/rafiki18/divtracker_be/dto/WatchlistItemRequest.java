package com.rafiki18.divtracker_be.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud para crear o actualizar un item del watchlist")
public class WatchlistItemRequest {
    
    @NotBlank(message = "El ticker es obligatorio")
    @Size(min = 1, max = 12, message = "El ticker debe tener entre 1 y 12 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9.\\-]+$", message = "El ticker solo puede contener letras, números, puntos y guiones")
    @Schema(description = "Símbolo del ticker de la empresa", example = "AAPL", required = true)
    private String ticker;
    
    @Size(max = 50, message = "El exchange no puede superar 50 caracteres")
    @Schema(description = "Mercado o exchange donde cotiza", example = "NASDAQ")
    private String exchange;
    
    @DecimalMin(value = "0.01", message = "El precio objetivo debe ser mayor que 0")
    @Digits(integer = 15, fraction = 4, message = "El precio objetivo tiene un formato inválido")
    @Schema(description = "Precio objetivo para la acción", example = "150.50")
    private BigDecimal targetPrice;
    
    @DecimalMin(value = "0.01", message = "El PFCF objetivo debe ser mayor que 0")
    @Digits(integer = 15, fraction = 4, message = "El PFCF objetivo tiene un formato inválido")
    @Schema(description = "Price to Free Cash Flow objetivo", example = "15.5")
    private BigDecimal targetPfcf;
    
    @Schema(description = "Notificar cuando el precio esté por debajo del objetivo", example = "false")
    private Boolean notifyWhenBelowPrice;
    
    @Size(max = 500, message = "Las notas no pueden superar 500 caracteres")
    @Schema(description = "Notas adicionales sobre la empresa", example = "Empresa tecnológica líder en innovación")
    private String notes;
    
    @DecimalMin(value = "0.0", message = "La tasa de crecimiento del FCF debe ser mayor o igual a 0")
    @DecimalMax(value = "1.0", message = "La tasa de crecimiento del FCF no puede superar 100%")
    @Digits(integer = 1, fraction = 4, message = "La tasa de crecimiento tiene un formato inválido")
    @Schema(description = "Tasa de crecimiento anual esperada del FCF (decimal, ej: 0.08 para 8%)", example = "0.08")
    private BigDecimal estimatedFcfGrowthRate;
    
    @Min(value = 1, message = "El horizonte de inversión debe ser al menos 1 año")
    @Max(value = 30, message = "El horizonte de inversión no puede superar 30 años")
    @Schema(description = "Horizonte de inversión en años para cálculos de valoración", example = "5")
    private Integer investmentHorizonYears;
    
    @DecimalMin(value = "0.01", message = "La tasa de descuento debe ser mayor que 0")
    @DecimalMax(value = "1.0", message = "La tasa de descuento no puede superar 100%")
    @Digits(integer = 1, fraction = 4, message = "La tasa de descuento tiene un formato inválido")
    @Schema(description = "Tasa de descuento / costo de capital (WACC) como decimal (ej: 0.10 para 10%)", example = "0.10")
    private BigDecimal discountRate;
    
    // Removed validation - will be handled in service layer
    // to allow creation with just ticker if Finnhub is enabled
}
