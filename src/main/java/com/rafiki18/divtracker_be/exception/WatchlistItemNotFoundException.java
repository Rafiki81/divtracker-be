package com.rafiki18.divtracker_be.exception;

import java.util.UUID;

public class WatchlistItemNotFoundException extends RuntimeException {
    
    public WatchlistItemNotFoundException(UUID id) {
        super("Watchlist item not found with id: " + id);
    }
    
    public WatchlistItemNotFoundException(UUID userId, UUID id) {
        super("Watchlist item not found with id: " + id + " for user: " + userId);
    }
    
    public WatchlistItemNotFoundException(String message) {
        super(message);
    }
}
