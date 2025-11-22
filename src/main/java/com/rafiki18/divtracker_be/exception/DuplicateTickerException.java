package com.rafiki18.divtracker_be.exception;

public class DuplicateTickerException extends RuntimeException {
    
    public DuplicateTickerException(String ticker) {
        super("Ticker '" + ticker + "' already exists in your watchlist");
    }
    
    public DuplicateTickerException(String message, Throwable cause) {
        super(message, cause);
    }
}
