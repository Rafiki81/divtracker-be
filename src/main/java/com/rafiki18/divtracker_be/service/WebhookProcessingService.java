package com.rafiki18.divtracker_be.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rafiki18.divtracker_be.model.InstrumentFundamentals;
import com.rafiki18.divtracker_be.model.MarketPriceTick;
import com.rafiki18.divtracker_be.repository.InstrumentFundamentalsRepository;
import com.rafiki18.divtracker_be.repository.MarketPriceTickRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for asynchronous processing of webhook payloads.
 * Separated from controller to ensure Spring AOP @Async works correctly.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookProcessingService {

    private final MarketPriceTickRepository marketPriceTickRepository;
    private final InstrumentFundamentalsRepository instrumentFundamentalsRepository;

    /**
     * Processes the webhook payload asynchronously.
     * This method is called from the controller after responding with 200 OK.
     * Being in a separate bean ensures Spring AOP intercepts the @Async annotation.
     */
    @Async
    public void processWebhookPayloadAsync(Map<String, Object> payload) {
        try {
            processWebhookPayload(payload);
            log.info("Finnhub webhook processed successfully");
        } catch (Exception e) {
            log.error("Error processing Finnhub webhook asynchronously: {}", e.getMessage(), e);
        }
    }

    /**
     * Processes trade events from Finnhub webhook.
     * For each trade:
     * 1. Saves to market_price_ticks table (historical record)
     * 2. Updates currentPrice in instrument_fundamentals (for real-time display)
     */
    @SuppressWarnings("unchecked")
    @Transactional
    public void processWebhookPayload(Map<String, Object> payload) {
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
            processSingleTrade(trade);
        }
    }

    /**
     * Process a single trade event.
     */
    private void processSingleTrade(Map<String, Object> trade) {
        try {
            String symbol = (String) trade.get("s");
            Object priceObj = trade.get("p");
            Object timestampObj = trade.get("t");
            Object volumeObj = trade.get("v");

            if (symbol == null || priceObj == null || timestampObj == null) {
                log.warn("Incomplete trade data: {}", trade);
                return;
            }

            String ticker = symbol.toUpperCase();
            BigDecimal price = new BigDecimal(priceObj.toString());
            BigDecimal volume = volumeObj != null ? new BigDecimal(volumeObj.toString()) : null;
            Instant tradeTimestamp = Instant.ofEpochMilli(Long.parseLong(timestampObj.toString()));

            // 1. Save to market_price_ticks for historical record
            MarketPriceTick tick = new MarketPriceTick();
            tick.setTicker(ticker);
            tick.setPrice(price);
            tick.setVolume(volume);
            tick.setTradeTimestamp(tradeTimestamp);
            tick.setSource("FINNHUB_WEBHOOK");
            tick.setReceivedAt(Instant.now());
            
            marketPriceTickRepository.save(tick);
            log.debug("Saved price tick for {}: ${}", ticker, price);

            // 2. Update currentPrice in instrument_fundamentals (if exists)
            updateInstrumentFundamentalsPrice(ticker, price);
            
        } catch (Exception e) {
            log.error("Error processing trade data: {}", trade, e);
        }
    }

    /**
     * Updates the current price in InstrumentFundamentals if the ticker exists.
     * This ensures the frontend sees real-time prices from webhooks.
     */
    private void updateInstrumentFundamentalsPrice(String ticker, BigDecimal newPrice) {
        instrumentFundamentalsRepository.findByTickerIgnoreCase(ticker)
            .ifPresent(fundamentals -> {
                BigDecimal oldPrice = fundamentals.getCurrentPrice();
                fundamentals.setCurrentPrice(newPrice);
                
                // Calculate daily change if we have old price
                if (oldPrice != null && oldPrice.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal change = newPrice.subtract(oldPrice)
                        .divide(oldPrice, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
                    fundamentals.setDailyChangePercent(change);
                }
                
                // Don't update lastUpdatedAt - that's for full data refresh
                // We only want to track the price update
                instrumentFundamentalsRepository.save(fundamentals);
                log.debug("Updated instrument_fundamentals price for {}: {} -> {}", 
                    ticker, oldPrice, newPrice);
            });
    }
}
