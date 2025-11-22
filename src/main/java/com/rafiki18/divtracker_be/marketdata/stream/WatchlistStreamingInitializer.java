package com.rafiki18.divtracker_be.marketdata.stream;

import java.util.List;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.rafiki18.divtracker_be.repository.WatchlistItemRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class WatchlistStreamingInitializer {

    private final WatchlistItemRepository watchlistItemRepository;
    private final WatchlistTickerSubscriptionService subscriptionService;

    @EventListener(ApplicationReadyEvent.class)
    public void preloadWatchlistTickers() {
        if (!subscriptionService.isStreamingActive()) {
            log.info("Skipping watchlist streaming bootstrap because Finnhub streaming is disabled");
            return;
        }
        List<String> tickers = watchlistItemRepository.findDistinctTickers();
        if (tickers.isEmpty()) {
            log.info("No watchlist tickers to preload for streaming");
            return;
        }
        subscriptionService.bootstrap(tickers);
        log.info("Preloaded {} watchlist tickers for Finnhub streaming", tickers.size());
    }
}
