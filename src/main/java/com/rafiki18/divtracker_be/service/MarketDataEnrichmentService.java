package com.rafiki18.divtracker_be.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.rafiki18.divtracker_be.marketdata.FinnhubClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for enriching watchlist items with real-time market data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MarketDataEnrichmentService {
    
    private final FinnhubClient finnhubClient;
    
    /**
     * Enriches a watchlist item response with current market data from Finnhub.
     * Fetches current price and FCF per share, then returns both values.
     *
     * @param ticker Stock ticker symbol
     * @return Array with [currentPrice, fcfPerShare], or nulls if data unavailable
     */
    public BigDecimal[] fetchMarketData(String ticker) {
        if (!finnhubClient.isEnabled()) {
            log.debug("Finnhub client is disabled, skipping market data fetch for {}", ticker);
            return new BigDecimal[]{null, null};
        }
        
        BigDecimal currentPrice = finnhubClient.fetchCurrentPrice(ticker).orElse(null);
        BigDecimal fcfPerShare = finnhubClient.fetchFreeCashFlowPerShare(ticker).orElse(null);
        
        if (currentPrice == null || fcfPerShare == null) {
            log.debug("Incomplete market data for {}: price={}, fcf={}", 
                    ticker, currentPrice, fcfPerShare);
        }
        
        return new BigDecimal[]{currentPrice, fcfPerShare};
    }
    
    /**
     * Checks if market data enrichment is available.
     *
     * @return true if Finnhub client is enabled
     */
    public boolean isAvailable() {
        return finnhubClient.isEnabled();
    }
}
