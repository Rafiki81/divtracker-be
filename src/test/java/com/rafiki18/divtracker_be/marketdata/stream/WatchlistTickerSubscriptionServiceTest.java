package com.rafiki18.divtracker_be.marketdata.stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WatchlistTickerSubscriptionServiceTest {

    @Mock
    private FinnhubStreamingClient streamingClient;

    private WatchlistTickerSubscriptionService service;

    @BeforeEach
    void setUp() {
        service = new WatchlistTickerSubscriptionService(streamingClient);
    }

    @Test
    @DisplayName("registerTicker solo suscribe una vez por ticker")
    void registerTicker_SubscribeOnce() {
        when(streamingClient.isStreamingEnabled()).thenReturn(true);

        service.registerTicker("aapl");
        service.registerTicker("AAPL");

        verify(streamingClient, times(1)).subscribeTicker(eq("AAPL"));
    }

    @Test
    @DisplayName("unregisterTicker cancela suscripción cuando contador llega a cero")
    void unregisterTicker_UnsubscribeWhenZero() {
        when(streamingClient.isStreamingEnabled()).thenReturn(true);

        service.registerTicker("MSFT");
        service.registerTicker("MSFT");
        service.unregisterTicker("MSFT");
        service.unregisterTicker("msft");

        verify(streamingClient, times(1)).subscribeTicker("MSFT");
        verify(streamingClient, times(1)).unsubscribeTicker("MSFT");
    }

    @Test
    @DisplayName("No interactúa con streaming cuando está deshabilitado")
    void registerTicker_NoOpWhenDisabled() {
        when(streamingClient.isStreamingEnabled()).thenReturn(false);

        service.registerTicker("NFLX");
        service.unregisterTicker("NFLX");

        verify(streamingClient, never()).subscribeTicker("NFLX");
        verify(streamingClient, never()).unsubscribeTicker("NFLX");
    }
}
