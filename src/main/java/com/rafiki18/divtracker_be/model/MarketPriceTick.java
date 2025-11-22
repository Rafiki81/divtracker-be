package com.rafiki18.divtracker_be.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "market_price_ticks")
@Getter
@Setter
@NoArgsConstructor
public class MarketPriceTick {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, length = 12)
    private String ticker;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal price;

    @Column(precision = 19, scale = 4)
    private BigDecimal volume;

    @Column(name = "trade_timestamp", nullable = false)
    private Instant tradeTimestamp;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    @Column(nullable = false, length = 32)
    private String source = "FINNHUB";
}
