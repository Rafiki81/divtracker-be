package com.rafiki18.divtracker_be.dto;

/**
 * Types of push notifications supported by the application.
 */
public enum PushNotificationType {
    
    /**
     * Silent data-only notification for price updates.
     * Android app receives this and updates UI without showing a notification.
     */
    PRICE_UPDATE,
    
    /**
     * Visible notification when price reaches user's target price.
     */
    PRICE_ALERT,
    
    /**
     * Visible notification when margin of safety exceeds threshold.
     */
    MARGIN_ALERT,
    
    /**
     * Daily summary notification with watchlist overview.
     */
    DAILY_SUMMARY
}
