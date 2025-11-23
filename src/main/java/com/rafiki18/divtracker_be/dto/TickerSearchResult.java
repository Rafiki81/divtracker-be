package com.rafiki18.divtracker_be.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resultado de búsqueda de ticker")
public class TickerSearchResult {
    
    @Schema(description = "Símbolo del ticker", example = "AAPL")
    private String symbol;
    
    @Schema(description = "Nombre completo de la empresa", example = "Apple Inc")
    private String description;
    
    @Schema(description = "Tipo de instrumento", example = "Common Stock")
    private String type;
    
    @Schema(description = "Exchange donde cotiza", example = "NASDAQ")
    private String exchange;
    
    @Schema(description = "Moneda", example = "USD")
    private String currency;
    
    @Schema(description = "Código FIGI", example = "BBG000B9XRY4")
    private String figi;
}
