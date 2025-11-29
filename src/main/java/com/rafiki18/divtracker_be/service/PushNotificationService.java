package com.rafiki18.divtracker_be.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rafiki18.divtracker_be.dto.PushNotificationDto;
import com.rafiki18.divtracker_be.model.UserFcmToken;
import com.rafiki18.divtracker_be.model.WatchlistItem;
import com.rafiki18.divtracker_be.repository.WatchlistItemRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for sending push notifications based on price updates.
 * Handles determining which users to notify and when.
 */
@Service
@ConditionalOnProperty(name = "fcm.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {

    private final FcmTokenService fcmTokenService;
    private final FirebasePushService firebasePushService;
    private final WatchlistItemRepository watchlistItemRepository;

    /**
     * Send price update notifications for a ticker.
     * This is called asynchronously after processing webhook data.
     */
    @Async
    @Transactional(readOnly = true)
    public void sendPriceUpdateNotifications(String ticker, BigDecimal currentPrice, 
            BigDecimal previousPrice, BigDecimal changePercent) {
        
        List<UserFcmToken> tokens = fcmTokenService.getTokensForTicker(ticker);
        
        if (tokens.isEmpty()) {
            log.debug("No devices registered for ticker {}", ticker);
            return;
        }

        log.info("Sending price updates for {} to {} devices", ticker, tokens.size());

        // Group tokens by user to check for alerts per user
        Map<UUID, List<UserFcmToken>> tokensByUser = tokens.stream()
                .collect(Collectors.groupingBy(t -> t.getUser().getId()));

        for (var entry : tokensByUser.entrySet()) {
            UUID userId = entry.getKey();
            List<UserFcmToken> userTokens = entry.getValue();

            // Check if user has price alerts for this ticker
            watchlistItemRepository.findByUserIdAndTickerIgnoreCase(userId, ticker)
                .ifPresent(item -> checkPriceTargetAlert(item, currentPrice, userTokens));

            // Always send silent price update for background sync
            PushNotificationDto notification = PushNotificationDto.priceUpdate(
                    ticker, currentPrice, changePercent);
            firebasePushService.sendNotificationToMultiple(userTokens, notification);
        }
    }

    /**
     * Check if current price has reached the target price and send alert.
     */
    private void checkPriceTargetAlert(WatchlistItem item, BigDecimal currentPrice, List<UserFcmToken> tokens) {
        if (item.getTargetPrice() == null || !Boolean.TRUE.equals(item.getNotifyWhenBelowPrice())) {
            return;
        }

        BigDecimal targetPrice = item.getTargetPrice();

        // Alert when current price falls at or below target price
        if (currentPrice.compareTo(targetPrice) <= 0) {
            log.info("Price target reached for {} - current: {}, target: {}", 
                    item.getTicker(), currentPrice, targetPrice);
            
            PushNotificationDto notification = PushNotificationDto.priceAlert(
                    item.getTicker(),
                    currentPrice,
                    targetPrice
            );
            
            firebasePushService.sendNotificationToMultiple(tokens, notification);
        }
    }

    /**
     * Send daily summary notifications to all users with watchlist items.
     * This should be scheduled to run once per day (e.g., after market close).
     */
    @Async
    @Transactional(readOnly = true)
    public void sendDailySummaries() {
        List<UserFcmToken> tokens = fcmTokenService.getTokensForDailySummary();
        
        if (tokens.isEmpty()) {
            log.debug("No devices registered for daily summary");
            return;
        }

        // Group by user
        Map<UUID, List<UserFcmToken>> tokensByUser = tokens.stream()
                .collect(Collectors.groupingBy(t -> t.getUser().getId()));

        log.info("Sending daily summaries to {} users", tokensByUser.size());

        for (var entry : tokensByUser.entrySet()) {
            UUID userId = entry.getKey();
            List<UserFcmToken> userTokens = entry.getValue();

            // Get user's watchlist tickers
            List<String> tickers = watchlistItemRepository.findDistinctTickersByUserId(userId);
            
            if (tickers.isEmpty()) {
                continue;
            }

            // For simplicity, we send a summary with count only
            // In a real implementation, you'd calculate gainers/losers from fundamentals
            PushNotificationDto notification = PushNotificationDto.dailySummary(
                    tickers,
                    0, // gainers - would need to calculate from fundamentals
                    0  // losers - would need to calculate from fundamentals
            );
            
            firebasePushService.sendNotificationToMultiple(userTokens, notification);
        }
    }
}
