package com.rafiki18.divtracker_be.marketdata;

import org.springframework.context.ApplicationEvent;

/**
 * Event fired when a new ticker is added to any user's watchlist.
 * Used to trigger WebSocket subscription for real-time updates.
 */
public class TickerAddedEvent extends ApplicationEvent {

    private final String ticker;

    public TickerAddedEvent(Object source, String ticker) {
        super(source);
        this.ticker = ticker;
    }

    public String getTicker() {
        return ticker;
    }
}
