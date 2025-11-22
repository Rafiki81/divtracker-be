package com.rafiki18.divtracker_be.dto;

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
@Schema(description = "Respuesta de autenticación exitosa")
public class AuthResponse {
    
    @Schema(description = "Token JWT para autenticación", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;
    
    @Schema(description = "Tipo de token", example = "Bearer", defaultValue = "Bearer")
    @Builder.Default
    private String type = "Bearer";
    
    @Schema(description = "ID del usuario", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;
    
    @Schema(description = "Email del usuario", example = "usuario@ejemplo.com")
    private String email;
    
    @Schema(description = "Nombre del usuario", example = "Juan")
    private String firstName;
    
    @Schema(description = "Apellido del usuario", example = "Pérez")
    private String lastName;
}
