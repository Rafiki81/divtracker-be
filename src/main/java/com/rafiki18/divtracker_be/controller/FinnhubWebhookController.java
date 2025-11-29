package com.rafiki18.divtracker_be.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rafiki18.divtracker_be.service.FinnhubWebhookService;
import com.rafiki18.divtracker_be.service.WebhookProcessingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/webhooks/finnhub")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Webhooks", description = "Endpoints para recibir notificaciones externas")
public class FinnhubWebhookController {

    private final FinnhubWebhookService webhookService;
    private final WebhookProcessingService webhookProcessingService;
    private final ObjectMapper objectMapper;

    @Operation(
        summary = "Webhook de Finnhub para actualizaciones de precios",
        description = "Endpoint público para recibir notificaciones de trades en tiempo real desde Finnhub. " +
                "Finnhub envía eventos de tipo 'trade' con los últimos precios de las acciones suscritas. " +
                "Requiere el header X-Finnhub-Secret para autenticación. " +
                "Los precios recibidos se guardan en la tabla market_price_ticks para histórico y análisis."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Webhook procesado exitosamente",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Secret inválido o ausente",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Error procesando el webhook",
            content = @Content(mediaType = "application/json")
        )
    })
    @PostMapping
    public ResponseEntity<Void> handleWebhook(
            @Parameter(
                description = "Secret de autenticación de Finnhub (configurado en su dashboard)",
                example = "d4gubhhr01qgvvc57cf0",
                required = true
            )
            @RequestHeader(value = "X-Finnhub-Secret", required = false) String secret,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Payload del webhook de Finnhub con eventos de trades",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Map.class),
                    examples = @ExampleObject(
                        name = "Trade Event",
                        value = """
                        {
                          "event": "trade",
                          "data": [
                            {
                              "s": "AAPL",
                              "p": 172.15,
                              "t": 1732285432000,
                              "v": 1000
                            },
                            {
                              "s": "MSFT",
                              "p": 378.50,
                              "t": 1732285433000,
                              "v": 500
                            }
                          ]
                        }
                        """
                    )
                )
            )
            @RequestBody String rawPayload) {
        
        // Verificar secret
        if (!webhookService.verifySecret(secret)) {
            log.warn("Invalid Finnhub webhook secret");
            return ResponseEntity.status(401).build();
        }

        try {
            // Parsear el payload JSON manualmente (Finnhub no envía Content-Type header)
            Map<String, Object> payload = objectMapper.readValue(rawPayload, new TypeReference<Map<String, Object>>() {});
            
            // Responder inmediatamente con 200 para evitar timeouts de Finnhub
            // Procesar el webhook de forma asíncrona (delegado a servicio separado para que @Async funcione)
            webhookProcessingService.processWebhookPayloadAsync(payload);
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error parsing Finnhub webhook payload: {}", e.getMessage(), e);
            return ResponseEntity.status(400).build();
        }
    }
}
