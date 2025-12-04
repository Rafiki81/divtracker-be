package com.rafiki18.divtracker_be.marketdata;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rafiki18.divtracker_be.config.properties.FinnhubProperties;
import com.rafiki18.divtracker_be.model.MarketPriceTick;
import com.rafiki18.divtracker_be.repository.InstrumentFundamentalsRepository;
import com.rafiki18.divtracker_be.repository.MarketPriceTickRepository;
import com.rafiki18.divtracker_be.repository.WatchlistItemRepository;
import com.rafiki18.divtracker_be.service.PushNotificationService;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket client for Finnhub real-time trade data.
 * 
 * Connects to wss://ws.finnhub.io and subscribes to tickers in user watchlists.
 * Processes incoming trade messages and:
 * 1. Saves price ticks to database
 * 2. Updates instrument fundamentals
 * 3. Sends push notifications for price alerts
 * 
 * Features:
 * - Auto-reconnect on connection loss
 * - Dynamic subscription management (add/remove tickers)
 * - Graceful shutdown
 * 
 * IMPORTANT: Free Finnhub plan limits WebSocket to 50 symbols maximum.
 */
@Component
@Slf4j
public class FinnhubWebSocketClient extends TextWebSocketHandler {

    /**
     * Maximum symbols allowed on Finnhub free plan.
     * Upgrade to paid plan for unlimited symbols.
     */
    private static final int MAX_SYMBOLS_FREE_PLAN = 50;
    
    /**
     * Minimum interval between push notifications per ticker (in milliseconds).
     * Prevents spamming users with high-frequency trades.
     */
    private static final long NOTIFICATION_THROTTLE_MS = 60_000; // 60 seconds

    private final FinnhubProperties properties;
    private final WatchlistItemRepository watchlistItemRepository;
    private final MarketPriceTickRepository marketPriceTickRepository;
    private final InstrumentFundamentalsRepository instrumentFundamentalsRepository;
    private final ObjectMapper objectMapper;
    
    // Optional - may be null if FCM not configured
    private PushNotificationService pushNotificationService;

    private WebSocketSession session;
    private final Set<String> subscribedTickers = ConcurrentHashMap.newKeySet();
    private final Map<String, Long> lastNotificationTime = new ConcurrentHashMap<>();
    private final AtomicBoolean isConnecting = new AtomicBoolean(false);
    private final AtomicBoolean shouldReconnect = new AtomicBoolean(true);
    private ScheduledExecutorService reconnectExecutor;

    public FinnhubWebSocketClient(
            FinnhubProperties properties,
            WatchlistItemRepository watchlistItemRepository,
            MarketPriceTickRepository marketPriceTickRepository,
            InstrumentFundamentalsRepository instrumentFundamentalsRepository,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.watchlistItemRepository = watchlistItemRepository;
        this.marketPriceTickRepository = marketPriceTickRepository;
        this.instrumentFundamentalsRepository = instrumentFundamentalsRepository;
        this.objectMapper = objectMapper;
    }

    @Autowired(required = false)
    public void setPushNotificationService(PushNotificationService pushNotificationService) {
        this.pushNotificationService = pushNotificationService;
        log.info("Push notification service injected into WebSocket client");
    }

    /**
     * Start WebSocket connection when application is ready.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!properties.isStreamingEnabled()) {
            log.info("Finnhub WebSocket streaming is disabled");
            return;
        }
        
        reconnectExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "finnhub-ws-reconnect");
            t.setDaemon(true);
            return t;
        });
        
        connect();
    }

    /**
     * Connect to Finnhub WebSocket.
     */
    public void connect() {
        if (!properties.isStreamingEnabled()) {
            log.debug("Streaming not enabled, skipping connect");
            return;
        }
        
        if (isConnecting.getAndSet(true)) {
            log.debug("Connection already in progress");
            return;
        }
        
        if (isConnected()) {
            log.debug("Already connected");
            isConnecting.set(false);
            return;
        }

        try {
            String wsUrl = properties.getWebsocketUrl() + "?token=" + properties.getApiKey();
            log.info("Connecting to Finnhub WebSocket: {}", properties.getWebsocketUrl());
            
            StandardWebSocketClient client = new StandardWebSocketClient();
            client.execute(this, wsUrl).get(10, TimeUnit.SECONDS);
            
        } catch (Exception e) {
            log.error("Failed to connect to Finnhub WebSocket: {}", e.getMessage());
            isConnecting.set(false);
            scheduleReconnect();
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        this.session = session;
        isConnecting.set(false);
        log.info("Connected to Finnhub WebSocket");
        
        // Subscribe to all tickers in watchlists
        subscribeToWatchlistTickers();
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.warn("Finnhub WebSocket connection closed: {}", status);
        this.session = null;
        subscribedTickers.clear();
        
        if (shouldReconnect.get()) {
            scheduleReconnect();
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("Finnhub WebSocket transport error: {}", exception.getMessage());
        if (session.isOpen()) {
            session.close();
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            processMessage(message.getPayload());
        } catch (Exception e) {
            log.error("Error processing WebSocket message: {}", e.getMessage(), e);
        }
    }

    /**
     * Process incoming WebSocket message from Finnhub.
     * 
     * Message format:
     * {
     *   "type": "trade",
     *   "data": [
     *     {"s": "AAPL", "p": 150.25, "t": 1234567890123, "v": 100, "c": ["1","12"]}
     *   ]
     * }
     */
    private void processMessage(String payload) throws JsonProcessingException {
        Map<String, Object> message = objectMapper.readValue(payload, 
            new TypeReference<Map<String, Object>>() {});
        
        String type = (String) message.get("type");
        
        if ("ping".equals(type)) {
            // Finnhub sends ping messages to keep connection alive
            log.trace("Received ping from Finnhub");
            return;
        }
        
        if (!"trade".equals(type)) {
            log.debug("Ignoring message type: {}", type);
            return;
        }
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> trades = (List<Map<String, Object>>) message.get("data");
        
        if (trades == null || trades.isEmpty()) {
            return;
        }
        
        for (Map<String, Object> trade : trades) {
            processTrade(trade);
        }
    }

    /**
     * Process a single trade event.
     */
    private void processTrade(Map<String, Object> trade) {
        try {
            String symbol = (String) trade.get("s");
            Object priceObj = trade.get("p");
            Object timestampObj = trade.get("t");
            Object volumeObj = trade.get("v");

            if (symbol == null || priceObj == null || timestampObj == null) {
                log.trace("Incomplete trade data: {}", trade);
                return;
            }

            String ticker = symbol.toUpperCase();
            
            // Only process if we track this ticker
            var fundamentalsOpt = instrumentFundamentalsRepository.findByTickerIgnoreCase(ticker);
            if (fundamentalsOpt.isEmpty()) {
                log.trace("Ignoring tick for untracked ticker: {}", ticker);
                return;
            }

            BigDecimal price = new BigDecimal(priceObj.toString());
            BigDecimal volume = volumeObj != null ? new BigDecimal(volumeObj.toString()) : null;
            long timestampMillis = ((Number) timestampObj).longValue();
            Instant tradeTimestamp = Instant.ofEpochMilli(timestampMillis);

            // 1. Save to market_price_ticks
            MarketPriceTick tick = new MarketPriceTick();
            tick.setTicker(ticker);
            tick.setPrice(price);
            tick.setVolume(volume);
            tick.setTradeTimestamp(tradeTimestamp);
            tick.setSource("FINNHUB_WEBSOCKET");
            tick.setReceivedAt(Instant.now());
            
            marketPriceTickRepository.save(tick);
            log.debug("Saved price tick for {}: ${} via WebSocket", ticker, price);

            // 2. Update instrument_fundamentals
            var fundamentals = fundamentalsOpt.get();
            BigDecimal oldPrice = fundamentals.getCurrentPrice();
            fundamentals.setCurrentPrice(price);
            
            // Calculate daily change
            if (oldPrice != null && oldPrice.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal change = price.subtract(oldPrice)
                    .divide(oldPrice, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
                fundamentals.setDailyChangePercent(change);
            }
            
            instrumentFundamentalsRepository.save(fundamentals);

            // 3. Send push notifications (throttled to avoid spamming)
            if (pushNotificationService != null && shouldSendNotification(ticker)) {
                BigDecimal changePercent = fundamentals.getDailyChangePercent();
                pushNotificationService.sendPriceUpdateNotifications(ticker, price, oldPrice, changePercent);
                log.debug("Sent push notification for {}", ticker);
            }
            
        } catch (Exception e) {
            log.error("Error processing trade: {}", trade, e);
        }
    }

    /**
     * Check if enough time has passed since the last notification for this ticker.
     * This prevents spamming users with high-frequency trade updates.
     * 
     * @param ticker the ticker symbol
     * @return true if a notification should be sent
     */
    private boolean shouldSendNotification(String ticker) {
        long now = System.currentTimeMillis();
        Long lastTime = lastNotificationTime.get(ticker);
        
        if (lastTime == null || (now - lastTime) >= NOTIFICATION_THROTTLE_MS) {
            lastNotificationTime.put(ticker, now);
            return true;
        }
        
        return false;
    }

    /**
     * Subscribe to all tickers currently in any user's watchlist.
     */
    public void subscribeToWatchlistTickers() {
        try {
            List<String> tickers = watchlistItemRepository.findDistinctTickers();
            log.info("Found {} distinct tickers in watchlists to subscribe", tickers.size());
            
            for (String ticker : tickers) {
                subscribe(ticker);
            }
        } catch (Exception e) {
            log.error("Error subscribing to watchlist tickers: {}", e.getMessage(), e);
        }
    }

    /**
     * Subscribe to a single ticker.
     */
    public void subscribe(String ticker) {
        if (!isConnected()) {
            log.debug("Cannot subscribe to {}: not connected", ticker);
            return;
        }
        
        String normalizedTicker = ticker.toUpperCase();
        
        if (subscribedTickers.contains(normalizedTicker)) {
            log.trace("Already subscribed to {}", normalizedTicker);
            return;
        }
        
        // Check free plan limit (50 symbols max)
        if (subscribedTickers.size() >= MAX_SYMBOLS_FREE_PLAN) {
            log.warn("Cannot subscribe to {}: reached free plan limit of {} symbols. " +
                     "Upgrade Finnhub plan for unlimited symbols.", 
                     normalizedTicker, MAX_SYMBOLS_FREE_PLAN);
            return;
        }
        
        try {
            String subscribeMessage = String.format(
                "{\"type\":\"subscribe\",\"symbol\":\"%s\"}", 
                normalizedTicker
            );
            
            session.sendMessage(new TextMessage(subscribeMessage));
            subscribedTickers.add(normalizedTicker);
            log.info("Subscribed to ticker: {} ({}/{})", 
                     normalizedTicker, subscribedTickers.size(), MAX_SYMBOLS_FREE_PLAN);
            
        } catch (Exception e) {
            log.error("Error subscribing to {}: {}", normalizedTicker, e.getMessage());
        }
    }

    /**
     * Unsubscribe from a ticker.
     */
    public void unsubscribe(String ticker) {
        if (!isConnected()) {
            return;
        }
        
        String normalizedTicker = ticker.toUpperCase();
        
        if (!subscribedTickers.contains(normalizedTicker)) {
            return;
        }
        
        try {
            String unsubscribeMessage = String.format(
                "{\"type\":\"unsubscribe\",\"symbol\":\"%s\"}", 
                normalizedTicker
            );
            
            session.sendMessage(new TextMessage(unsubscribeMessage));
            subscribedTickers.remove(normalizedTicker);
            log.info("Unsubscribed from ticker: {}", normalizedTicker);
            
        } catch (Exception e) {
            log.error("Error unsubscribing from {}: {}", normalizedTicker, e.getMessage());
        }
    }

    /**
     * Refresh subscriptions based on current watchlist state.
     * Call this when watchlist items are added/removed.
     */
    public void refreshSubscriptions() {
        if (!isConnected()) {
            return;
        }
        
        try {
            Set<String> currentTickers = new HashSet<>(watchlistItemRepository.findDistinctTickers());
            
            // Unsubscribe from tickers no longer in any watchlist
            Set<String> toUnsubscribe = new HashSet<>(subscribedTickers);
            toUnsubscribe.removeAll(currentTickers);
            for (String ticker : toUnsubscribe) {
                unsubscribe(ticker);
            }
            
            // Subscribe to new tickers
            for (String ticker : currentTickers) {
                if (!subscribedTickers.contains(ticker.toUpperCase())) {
                    subscribe(ticker);
                }
            }
            
            log.info("Subscription refresh complete. Subscribed to {} tickers", subscribedTickers.size());
            
        } catch (Exception e) {
            log.error("Error refreshing subscriptions: {}", e.getMessage(), e);
        }
    }

    /**
     * Check if WebSocket is connected.
     */
    public boolean isConnected() {
        return session != null && session.isOpen();
    }

    /**
     * Get the set of currently subscribed tickers.
     */
    public Set<String> getSubscribedTickers() {
        return Set.copyOf(subscribedTickers);
    }

    /**
     * Schedule a reconnection attempt.
     */
    private void scheduleReconnect() {
        if (!shouldReconnect.get() || reconnectExecutor == null || reconnectExecutor.isShutdown()) {
            return;
        }
        
        long delaySeconds = properties.getStreamReconnectDelay().getSeconds();
        log.info("Scheduling reconnect in {} seconds", delaySeconds);
        
        reconnectExecutor.schedule(this::connect, delaySeconds, TimeUnit.SECONDS);
    }

    // ==================== Event Listeners ====================

    /**
     * Handle when a new ticker is added to any watchlist.
     */
    @EventListener
    public void onTickerAdded(TickerAddedEvent event) {
        log.info("Received TickerAddedEvent for: {}", event.getTicker());
        subscribe(event.getTicker());
    }

    /**
     * Handle when a ticker is removed from all watchlists.
     */
    @EventListener
    public void onTickerRemoved(TickerRemovedEvent event) {
        log.info("Received TickerRemovedEvent for: {}", event.getTicker());
        unsubscribe(event.getTicker());
    }

    // ==================== Status Methods ====================

    /**
     * Get connection status info for monitoring.
     */
    public Map<String, Object> getStatus() {
        return Map.of(
            "connected", isConnected(),
            "streamingEnabled", properties.isStreamingEnabled(),
            "subscribedTickers", subscribedTickers.size(),
            "maxSymbols", MAX_SYMBOLS_FREE_PLAN,
            "remainingSlots", MAX_SYMBOLS_FREE_PLAN - subscribedTickers.size(),
            "tickers", List.copyOf(subscribedTickers)
        );
    }

    /**
     * Graceful shutdown.
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down Finnhub WebSocket client");
        shouldReconnect.set(false);
        
        if (reconnectExecutor != null) {
            reconnectExecutor.shutdownNow();
        }
        
        if (session != null && session.isOpen()) {
            try {
                session.close();
            } catch (Exception e) {
                log.warn("Error closing WebSocket session: {}", e.getMessage());
            }
        }
    }
}
