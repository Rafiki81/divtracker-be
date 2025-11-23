package com.rafiki18.divtracker_be.service;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.rafiki18.divtracker_be.dto.TickerSearchResult;
import com.rafiki18.divtracker_be.marketdata.FinnhubClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for searching stock tickers and symbols.
 * Provides flexible search by company name or ticker symbol.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TickerSearchService {
    
    private final FinnhubClient finnhubClient;
    
    /**
     * Look up ticker symbols using exact symbol matching.
     * Best for validating and finding variations of a specific ticker (e.g., BAM, BAM.A).
     *
     * @param query Ticker symbol to look up (e.g., "BAM", "AAPL")
     * @return List of matching ticker symbols from US exchanges
     */
    public List<TickerSearchResult> lookupTicker(String query) {
        if (query == null || query.trim().isEmpty()) {
            log.debug("Empty lookup query provided");
            return Collections.emptyList();
        }
        
        if (!finnhubClient.isEnabled()) {
            log.warn("Finnhub is not enabled, ticker lookup unavailable");
            return Collections.emptyList();
        }
        
        String normalizedQuery = query.trim();
        log.debug("Looking up ticker symbols for: {}", normalizedQuery);
        
        List<TickerSearchResult> results = finnhubClient.lookupSymbol(normalizedQuery);
        log.info("Found {} ticker symbols for lookup: {}", results.size(), normalizedQuery);
        
        return results;
    }

    /**
     * Search for tickers by query (company name or symbol).
     * Best for finding companies by name (e.g., "Apple" -> AAPL).
     * Uses intelligent search: tries symbol lookup first (faster), 
     * falls back to fuzzy search if no results (better coverage).
     *
     * @param query Search term (e.g., "Apple", "Microsoft", "TAP")
     * @return List of matching ticker results, limited to 20
     */
    public List<TickerSearchResult> searchTickers(String query) {
        if (query == null || query.trim().isEmpty()) {
            log.debug("Empty search query provided");
            return Collections.emptyList();
        }
        
        if (!finnhubClient.isEnabled()) {
            log.warn("Finnhub is not enabled, ticker search unavailable");
            return Collections.emptyList();
        }
        
        String normalizedQuery = query.trim();
        log.debug("Searching tickers for query: {}", normalizedQuery);
        
        // Try exact symbol lookup first (faster for tickers)
        List<TickerSearchResult> results = finnhubClient.lookupSymbol(normalizedQuery);
        
        // If no results with exact lookup, try fuzzy search (for company names)
        if (results.isEmpty()) {
            log.debug("No results from symbol lookup, trying fuzzy search");
            results = finnhubClient.searchSymbols(normalizedQuery);
        }
        
        log.info("Found {} ticker results for query: {}", results.size(), normalizedQuery);
        
        return results;
    }
    
    /**
     * Check if ticker search is available.
     *
     * @return true if Finnhub is enabled
     */
    public boolean isAvailable() {
        return finnhubClient.isEnabled();
    }
}
