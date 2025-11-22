package com.rafiki18.divtracker_be.controller;

import java.net.URI;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.rafiki18.divtracker_be.dto.WatchlistItemRequest;
import com.rafiki18.divtracker_be.dto.WatchlistItemResponse;
import com.rafiki18.divtracker_be.model.User;
import com.rafiki18.divtracker_be.service.WatchlistService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/watchlist")
@RequiredArgsConstructor
@Tag(name = "Watchlist", description = "Gestión del radar de empresas vigiladas")
@SecurityRequirement(name = "bearerAuth")
public class WatchlistController {
    
    private final WatchlistService watchlistService;
    
    @Operation(
        summary = "Listar items del watchlist",
        description = "Obtiene una lista paginada de todas las empresas en el watchlist del usuario autenticado. " +
                "Incluye precios actualizados y todas las métricas de valoración calculadas (DCF, TIR, FCF Yield, etc.)."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lista obtenida exitosamente",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Page.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "No autenticado",
            content = @Content(mediaType = "application/json")
        )
    })
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<WatchlistItemResponse>> list(
            @AuthenticationPrincipal User user,
            @Parameter(description = "Número de página (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Campo de ordenamiento") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Dirección de ordenamiento") @RequestParam(defaultValue = "DESC") Sort.Direction direction
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<WatchlistItemResponse> items = watchlistService.list(user.getId(), pageable);
        return ResponseEntity.ok(items);
    }
    
    @Operation(
        summary = "Obtener un item del watchlist",
        description = "Obtiene los detalles completos de un item específico del watchlist, " +
                "incluyendo precio actual, métricas de valoración (DCF, TIR, margen de seguridad, etc.) " +
                "y parámetros de análisis configurados."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Item encontrado",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = WatchlistItemResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Item no encontrado",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "401",
            description = "No autenticado",
            content = @Content(mediaType = "application/json")
        )
    })
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WatchlistItemResponse> getById(
            @AuthenticationPrincipal User user,
            @Parameter(description = "ID del item") @PathVariable UUID id
    ) {
        WatchlistItemResponse item = watchlistService.getById(user.getId(), id);
        return ResponseEntity.ok(item);
    }
    
    @Operation(
        summary = "Crear item en el watchlist",
        description = "Añade una nueva empresa al watchlist del usuario autenticado. " +
                "Calcula automáticamente métricas de valoración como DCF, TIR, FCF Yield, margen de seguridad, " +
                "payback period y ROI estimado. Los parámetros de valoración (growthRate, horizon, discountRate) " +
                "son opcionales y se usan para cálculos avanzados."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Item creado exitosamente",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = WatchlistItemResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos inválidos",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "409",
            description = "El ticker ya existe en el watchlist",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "401",
            description = "No autenticado",
            content = @Content(mediaType = "application/json")
        )
    })
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WatchlistItemResponse> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody WatchlistItemRequest request
    ) {
        WatchlistItemResponse created = watchlistService.create(user.getId(), request);
        
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        
        return ResponseEntity.created(location).body(created);
    }
    
    @Operation(
        summary = "Actualizar item del watchlist",
        description = "Actualiza parcialmente un item existente en el watchlist (PATCH/merge). " +
                "Recalcula automáticamente todas las métricas de valoración cuando se actualizan " +
                "los parámetros de entrada (targetPrice, targetPfcf, growthRate, horizon, discountRate)."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Item actualizado exitosamente",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = WatchlistItemResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Item no encontrado",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "409",
            description = "El nuevo ticker ya existe en el watchlist",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "401",
            description = "No autenticado",
            content = @Content(mediaType = "application/json")
        )
    })
    @PatchMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WatchlistItemResponse> update(
            @AuthenticationPrincipal User user,
            @Parameter(description = "ID del item") @PathVariable UUID id,
            @RequestBody WatchlistItemRequest request
    ) {
        WatchlistItemResponse updated = watchlistService.update(user.getId(), id, request);
        return ResponseEntity.ok(updated);
    }
    
    @Operation(
        summary = "Eliminar item del watchlist",
        description = "Elimina un item del watchlist del usuario autenticado"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Item eliminado exitosamente"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Item no encontrado",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "401",
            description = "No autenticado",
            content = @Content(mediaType = "application/json")
        )
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal User user,
            @Parameter(description = "ID del item") @PathVariable UUID id
    ) {
        watchlistService.delete(user.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
