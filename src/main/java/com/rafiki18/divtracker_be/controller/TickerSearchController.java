package com.rafiki18.divtracker_be.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rafiki18.divtracker_be.dto.TickerSearchResult;
import com.rafiki18.divtracker_be.service.TickerSearchService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/tickers")
@RequiredArgsConstructor
@Tag(name = "Ticker Search", description = "Búsqueda flexible de tickers por nombre o símbolo")
@SecurityRequirement(name = "bearerAuth")
public class TickerSearchController {
    
    private final TickerSearchService tickerSearchService;
    
    @Operation(
        summary = "Buscar símbolos exactos (Symbol Lookup)",
        description = "Busca símbolos de acciones en exchanges US usando coincidencia exacta. " +
                "Ideal para encontrar variaciones de un ticker específico (ej: BAM, BAM.A, BAM.B). " +
                "Retorna símbolos que comienzan con el query proporcionado. " +
                "Ejemplo: 'BAM' retornará BAM, BAM.A, BAM.TO, etc."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lista de símbolos encontrados (puede estar vacía si no hay coincidencias)",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = TickerSearchResult.class))
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Query inválido o vacío",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "401",
            description = "No autenticado",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "503",
            description = "Servicio de búsqueda no disponible (Finnhub deshabilitado)",
            content = @Content(mediaType = "application/json")
        )
    })
    @GetMapping("/lookup")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TickerSearchResult>> lookupTicker(
            @Parameter(
                description = "Símbolo del ticker a buscar (ej: BAM, AAPL, MSFT)", 
                example = "BAM",
                required = true
            )
            @RequestParam String symbol
    ) {
        if (symbol == null || symbol.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        if (!tickerSearchService.isAvailable()) {
            return ResponseEntity.status(503).build();
        }
        
        List<TickerSearchResult> results = tickerSearchService.lookupTicker(symbol);
        return ResponseEntity.ok(results);
    }

    @Operation(
        summary = "Buscar tickers por nombre",
        description = "Busca tickers de acciones por nombre de empresa. " +
                "La búsqueda es flexible y fuzzy, ideal para buscar por nombre de compañía. " +
                "Por ejemplo: 'apple', 'microsoft', 'tesla', etc. " +
                "Retorna hasta 20 resultados ordenados por relevancia."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lista de tickers encontrados (puede estar vacía si no hay coincidencias)",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = TickerSearchResult.class))
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Query inválido o vacío",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "401",
            description = "No autenticado",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "503",
            description = "Servicio de búsqueda no disponible (Finnhub deshabilitado)",
            content = @Content(mediaType = "application/json")
        )
    })
    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TickerSearchResult>> searchTickers(
            @Parameter(
                description = "Término de búsqueda (nombre de empresa)", 
                example = "Apple",
                required = true
            )
            @RequestParam String q
    ) {
        if (q == null || q.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        if (!tickerSearchService.isAvailable()) {
            return ResponseEntity.status(503).build();
        }
        
        List<TickerSearchResult> results = tickerSearchService.searchTickers(q);
        return ResponseEntity.ok(results);
    }
}
