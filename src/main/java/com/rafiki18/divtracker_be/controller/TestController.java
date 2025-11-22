package com.rafiki18.divtracker_be.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/test")
@Tag(name = "Testing", description = "Endpoints de prueba para verificar autenticación")
public class TestController {
    
    @Operation(
        summary = "Endpoint público",
        description = "Endpoint accesible sin autenticación para verificar que la API está funcionando"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Respuesta exitosa",
            content = @Content(mediaType = "application/json")
        )
    })
    @GetMapping("/public")
    public ResponseEntity<Map<String, String>> publicEndpoint() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "This is a public endpoint");
        return ResponseEntity.ok(response);
    }
    
    @Operation(
        summary = "Endpoint protegido",
        description = "Endpoint que requiere autenticación JWT para verificar que el sistema de seguridad funciona",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Acceso autorizado",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "401",
            description = "No autorizado - Token JWT inválido o ausente",
            content = @Content(mediaType = "application/json")
        )
    })
    @GetMapping("/protected")
    public ResponseEntity<Map<String, String>> protectedEndpoint(Authentication authentication) {
        Map<String, String> response = new HashMap<>();
        response.put("message", "This is a protected endpoint");
        response.put("user", authentication.getName());
        return ResponseEntity.ok(response);
    }
}
