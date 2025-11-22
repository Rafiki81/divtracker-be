package com.rafiki18.divtracker_be.marketdata.stream;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class WatchlistTickerSubscriptionService {

    private final FinnhubStreamingClient streamingClient;
    private final Map<String, AtomicInteger> subscriptions = new ConcurrentHashMap<>();

    public void bootstrap(Collection<String> tickers) {
        if (CollectionUtils.isEmpty(tickers)) {
            return;
        }
        tickers.forEach(this::registerTicker);
    }

    public boolean isStreamingActive() {
        return streamingClient.isStreamingEnabled();
    }

    public void registerTicker(String ticker) {
        if (!streamingClient.isStreamingEnabled() || !StringUtils.hasText(ticker)) {
            return;
        }
        String normalized = normalize(ticker);
        AtomicInteger counter = subscriptions.computeIfAbsent(normalized, key -> new AtomicInteger());
        int newValue = counter.incrementAndGet();
        if (newValue == 1) {
            streamingClient.subscribeTicker(normalized);
            log.debug("Ticker {} subscribed after first registration", normalized);
        }
    }

    public void unregisterTicker(String ticker) {
        if (!streamingClient.isStreamingEnabled() || !StringUtils.hasText(ticker)) {
            return;
        }
        String normalized = normalize(ticker);
        subscriptions.computeIfPresent(normalized, (key, counter) -> {
            int remaining = counter.decrementAndGet();
            if (remaining <= 0) {
                streamingClient.unsubscribeTicker(normalized);
                log.debug("Ticker {} unsubscribed (no remaining watchlist references)", normalized);
                return null;
            }
            return counter;
        });
    }

    private String normalize(String ticker) {
        return ticker.trim().toUpperCase();
    }
}
