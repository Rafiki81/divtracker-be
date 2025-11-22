package com.rafiki18.divtracker_be.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rafiki18.divtracker_be.model.MarketPriceTick;
import com.rafiki18.divtracker_be.repository.MarketPriceTickRepository;
import com.rafiki18.divtracker_be.service.FinnhubWebhookService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/webhooks/finnhub")
@RequiredArgsConstructor
@Slf4j
public class FinnhubWebhookController {

    private final FinnhubWebhookService webhookService;
    private final MarketPriceTickRepository marketPriceTickRepository;

    @PostMapping
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader(value = "X-Finnhub-Secret", required = false) String secret,
            @RequestBody Map<String, Object> payload) {
        
        log.debug("Received Finnhub webhook: {}", payload);

        // Verificar secret
        if (!webhookService.verifySecret(secret)) {
            log.warn("Invalid Finnhub webhook secret received");
            return ResponseEntity.status(401).build();
        }

        try {
            // Procesar el webhook
            processWebhookPayload(payload);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error processing Finnhub webhook", e);
            return ResponseEntity.status(500).build();
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
