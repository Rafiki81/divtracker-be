package com.rafiki18.divtracker_be.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rafiki18.divtracker_be.service.PushNotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduled job to send daily summary push notifications.
 * Sends a summary of watchlist performance to all registered devices.
 */
@Component
@ConditionalOnProperty(name = "fcm.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DailySummaryScheduler {
    
    private final PushNotificationService pushNotificationService;
    
    /**
     * Send daily summary notifications after US market close.
     * Runs daily at 10 PM CET (4 PM EST / 1 PM PST).
     * 
     * This ensures all market data is finalized for the day.
     */
    @Scheduled(cron = "${fcm.daily-summary.cron:0 0 22 * * MON-FRI}")
    public void sendDailySummaryNotifications() {
        log.info("Starting daily summary notifications");
        
        try {
            pushNotificationService.sendDailySummaries();
            log.info("Daily summary notifications completed");
        } catch (Exception e) {
            log.error("Error sending daily summary notifications", e);
        }
    }
}
