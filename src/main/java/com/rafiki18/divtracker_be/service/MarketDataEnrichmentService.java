package com.rafiki18.divtracker_be.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.rafiki18.divtracker_be.model.InstrumentFundamentals;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for enriching watchlist items with market data.
 * Uses cached fundamentals with fallback to live Finnhub data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MarketDataEnrichmentService {
    
    private final InstrumentFundamentalsService fundamentalsService;
    
    /**
     * Enriches a watchlist item response with current market data.
     * Uses cached fundamentals (< 24h) or fetches from Finnhub if stale.
     * Falls back to stale data if Finnhub is unavailable.
     *
     * @param ticker Stock ticker symbol
     * @return Array with [currentPrice, fcfPerShareAnnual, peAnnual, beta, epsGrowth5Y, revenueGrowth5Y], or nulls if data unavailable
     */
    public BigDecimal[] fetchMarketData(String ticker) {
        log.debug("Fetching market data for {}", ticker);
        
        return fundamentalsService.getFundamentals(ticker)
                .map(f -> new BigDecimal[]{
                    f.getCurrentPrice(),
                    f.getFcfPerShare(),
                    f.getPeAnnual(),
                    f.getBeta(),
                    f.getEpsGrowth5Y(),
                    f.getRevenueGrowth5Y()
                })
                .orElseGet(() -> {
                    log.debug("No market data available for {}", ticker);
                    return new BigDecimal[]{null, null, null, null, null, null};
                });
    }
    
    /**
     * Get complete fundamentals snapshot for a ticker.
     *
     * @param ticker Stock ticker symbol
     * @return Fundamentals object, or null if unavailable
     */
    public InstrumentFundamentals getFundamentals(String ticker) {
        return fundamentalsService.getFundamentals(ticker).orElse(null);
    }
    
    /**
     * Checks if market data enrichment is available.
     * Returns true even if using cached data.
     *
     * @return true if we have any data (cached or fresh)
     */
    public boolean isAvailable() {
        return true; // Always available with caching
    }
    
    /**
     * Force refresh market data from Finnhub.
     *
     * @param ticker Stock ticker symbol
     */
    public void refreshMarketData(String ticker) {
        log.info("Force refreshing market data for {}", ticker);
        fundamentalsService.refreshFundamentals(ticker);
    }
}
