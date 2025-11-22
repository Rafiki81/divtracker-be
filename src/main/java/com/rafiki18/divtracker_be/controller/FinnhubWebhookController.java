package com.rafiki18.divtracker_be.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rafiki18.divtracker_be.model.MarketPriceTick;
import com.rafiki18.divtracker_be.repository.MarketPriceTickRepository;
import com.rafiki18.divtracker_be.service.FinnhubWebhookService;

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
    private final MarketPriceTickRepository marketPriceTickRepository;

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
            @RequestBody Map<String, Object> payload) {
        
        log.info("Received Finnhub webhook - Secret present: {}, Payload size: {}", secret != null, payload.size());

        // Verificar secret
        if (!webhookService.verifySecret(secret)) {
            log.warn("Invalid Finnhub webhook secret received - Secret: {}", secret != null ? "present" : "missing");
            return ResponseEntity.status(401).build();
        }

        // Responder inmediatamente con 200 para evitar timeouts de Finnhub
        // Procesar el webhook de forma asíncrona
        processWebhookPayloadAsync(payload);
        
        return ResponseEntity.ok().build();
    }

    /**
     * Procesa el webhook de forma asíncrona para responder rápidamente a Finnhub
     * y evitar timeouts según su documentación.
     */
    @Async
    @SuppressWarnings("unchecked")
    public void processWebhookPayloadAsync(Map<String, Object> payload) {
        try {
            processWebhookPayload(payload);
            log.info("Finnhub webhook processed successfully");
        } catch (Exception e) {
            log.error("Error processing Finnhub webhook asynchronously: {}", e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private void processWebhookPayload(Map<String, Object> payload) {
        String event = (String) payload.get("event");
        
        if (!"trade".equals(event)) {
            log.debug("Ignoring non-trade event: {}", event);
            return;
        }

        List<Map<String, Object>> data = (List<Map<String, Object>>) payload.get("data");
        if (data == null || data.isEmpty()) {
            log.debug("No trade data in webhook payload");
            return;
        }

        for (Map<String, Object> trade : data) {
            try {
                MarketPriceTick tick = new MarketPriceTick();
                
                String symbol = (String) trade.get("s");
                Object priceObj = trade.get("p");
                Object timestampObj = trade.get("t");
                Object volumeObj = trade.get("v");

                if (symbol == null || priceObj == null || timestampObj == null) {
                    log.warn("Incomplete trade data: {}", trade);
                    continue;
                }

                tick.setTicker(symbol.toUpperCase());
                tick.setPrice(new BigDecimal(priceObj.toString()));
                tick.setVolume(volumeObj != null ? new BigDecimal(volumeObj.toString()) : null);
                tick.setTradeTimestamp(Instant.ofEpochMilli(Long.parseLong(timestampObj.toString())));
                tick.setSource("FINNHUB_WEBHOOK");
                tick.setReceivedAt(Instant.now());

                marketPriceTickRepository.save(tick);
                log.debug("Saved price tick for {}: ${}", symbol, tick.getPrice());
                
            } catch (Exception e) {
                log.error("Error processing trade data: {}", trade, e);
            }
        }
    }
}
