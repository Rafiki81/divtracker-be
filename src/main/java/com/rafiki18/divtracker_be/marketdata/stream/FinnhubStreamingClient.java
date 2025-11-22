package com.rafiki18.divtracker_be.marketdata.stream;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rafiki18.divtracker_be.config.properties.FinnhubProperties;
import com.rafiki18.divtracker_be.model.MarketPriceTick;
import com.rafiki18.divtracker_be.repository.MarketPriceTickRepository;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

@Component
@RequiredArgsConstructor
@Slf4j
public class FinnhubStreamingClient {

    private static final String SOURCE = "FINNHUB";

    private final FinnhubProperties properties;
    private final MarketPriceTickRepository marketPriceTickRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock = Clock.systemUTC();

    private final ReactorNettyWebSocketClient client = new ReactorNettyWebSocketClient();
    private final Sinks.Many<String> outbound = Sinks.many().multicast().onBackpressureBuffer();
    private final Set<String> activeTickers = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService reconnectExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "finnhub-stream-reconnect");
        thread.setDaemon(true);
        return thread;
    });

    private volatile Disposable connection;
    private volatile boolean reconnectScheduled;

    @PostConstruct
    void start() {
        if (!properties.isStreamingEnabled()) {
            log.info("Finnhub streaming disabled (missing API key or configuration)");
            return;
        }
        connect();
    }

    @PreDestroy
    void shutdown() {
        if (connection != null) {
            connection.dispose();
        }
        reconnectExecutor.shutdownNow();
    }

    public boolean isStreamingEnabled() {
        return properties.isStreamingEnabled();
    }

    public void subscribeTicker(@Nullable String rawTicker) {
        if (!isStreamingEnabled() || !StringUtils.hasText(rawTicker)) {
            return;
        }
        String ticker = normalize(rawTicker);
        if (activeTickers.add(ticker)) {
            emit(message("subscribe", ticker));
            log.debug("Subscribed ticker {} to Finnhub stream", ticker);
        }
    }

    public void unsubscribeTicker(@Nullable String rawTicker) {
        if (!isStreamingEnabled() || !StringUtils.hasText(rawTicker)) {
            return;
        }
        String ticker = normalize(rawTicker);
        if (activeTickers.remove(ticker)) {
            emit(message("unsubscribe", ticker));
            log.debug("Unsubscribed ticker {} from Finnhub stream", ticker);
        }
    }

    private void connect() {
        URI uri = URI.create(properties.getWebsocketUrl() + "?token=" + properties.getApiKey());
        log.info("Connecting to Finnhub WebSocket at {}", uri);

        Mono<Void> sessionMono = client.execute(uri, session -> {
            Flux<String> initialSubscriptions = Flux.defer(() ->
                Flux.fromIterable(activeTickers)
                    .map(ticker -> message("subscribe", ticker)));
                Mono<Void> receive = session.receive()
                    .map(WebSocketMessage::getPayloadAsText)
                    .flatMap(this::handleMessage)
                    .onErrorResume(ex -> {
                        log.warn("Finnhub stream receive error: {}", ex.getMessage());
                        return Mono.empty();
                    })
                    .then();

            Mono<Void> send = session.send(initialSubscriptions
                    .concatWith(outbound.asFlux())
                    .map(session::textMessage))
                    .doOnError(ex -> log.warn("Finnhub stream send error: {}", ex.getMessage()))
                    .onErrorResume(ex -> Mono.empty());

            return Mono.when(receive, send)
                    .doOnTerminate(() -> log.warn("Finnhub WebSocket session terminated"));
        });

        this.connection = sessionMono
                .doOnError(ex -> scheduleReconnect("error", ex))
                .doOnTerminate(() -> scheduleReconnect("completed", null))
                .subscribe();
    }

    private void emit(String payload) {
        Sinks.EmitResult result = outbound.tryEmitNext(payload);
        if (result.isFailure()) {
            log.trace("Finnhub stream emit dropped: {}", result);
        }
    }

    private Mono<Void> handleMessage(String payload) {
        return Mono.fromCallable(() -> objectMapper.readValue(payload, FinnhubTradeEnvelope.class))
                .flatMap(envelope -> {
                    if (!"trade".equalsIgnoreCase(envelope.type()) || CollectionUtils.isEmpty(envelope.data())) {
                        return Mono.empty();
                    }
                    List<MarketPriceTick> entities = envelope.data().stream()
                            .map(this::toEntity)
                            .filter(Objects::nonNull)
                            .toList();
                    if (entities.isEmpty()) {
                        return Mono.empty();
                    }
                    return Mono.fromCallable(() -> marketPriceTickRepository.saveAll(entities))
                            .subscribeOn(Schedulers.boundedElastic())
                            .then();
                })
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.warn("Finnhub stream payload error: status {}", ex.getStatusCode());
                    return Mono.empty();
                })
                .onErrorResume(ex -> {
                    log.debug("Finnhub stream parsing error for payload {}: {}", payload, ex.getMessage());
                    return Mono.empty();
                });
    }

    private MarketPriceTick toEntity(FinnhubTradeData data) {
        if (!StringUtils.hasText(data.symbol()) || data.price() == null || data.timestamp() == null) {
            return null;
        }
        MarketPriceTick tick = new MarketPriceTick();
        tick.setTicker(normalize(data.symbol()));
        tick.setPrice(data.price());
        tick.setVolume(data.volume());
        Instant tradeTime = Instant.ofEpochMilli(data.timestamp());
        tick.setTradeTimestamp(tradeTime);
        tick.setSource(SOURCE);
        tick.setReceivedAt(clock.instant());
        return tick;
    }

    private void scheduleReconnect(String reason, @Nullable Throwable cause) {
        if (!isStreamingEnabled()) {
            return;
        }
        if (reconnectScheduled) {
            return;
        }
        reconnectScheduled = true;
        Duration delay = properties.getStreamReconnectDelay() == null
                ? Duration.ofSeconds(5)
                : properties.getStreamReconnectDelay();
        log.info("Scheduling Finnhub stream reconnect in {} (reason: {})", delay, reason);
        reconnectExecutor.schedule(() -> {
            reconnectScheduled = false;
            connect();
        }, delay.toMillis(), TimeUnit.MILLISECONDS);
        if (cause != null) {
            log.warn("Last Finnhub stream error", cause);
        }
    }

    private String message(String type, String ticker) {
        return String.format("{\"type\":\"%s\",\"symbol\":\"%s\"}", type, ticker);
    }

    private String normalize(String ticker) {
        return ticker.trim().toUpperCase();
    }
}
