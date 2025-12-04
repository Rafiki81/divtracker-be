package com.rafiki18.divtracker_be.marketdata;

import org.springframework.context.ApplicationEvent;

/**
 * Event fired when a ticker is removed from all watchlists.
 * Used to trigger WebSocket unsubscription.
 */
public class TickerRemovedEvent extends ApplicationEvent {

    private final String ticker;

    public TickerRemovedEvent(Object source, String ticker) {
        super(source);
        this.ticker = ticker;
    }

    public String getTicker() {
        return ticker;
    }
}
