package com.rafiki18.divtracker_be.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "watchlist_items",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "ticker"}),
    indexes = @Index(name = "idx_watchlist_user_id", columnList = "user_id")
)
public class WatchlistItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(nullable = false, length = 12)
    private String ticker;
    
    @Column(length = 50)
    private String exchange;
    
    @Column(precision = 19, scale = 4)
    private BigDecimal targetPrice;
    
    @Column(precision = 19, scale = 4)
    private BigDecimal targetPfcf;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean notifyWhenBelowPrice = false;
    
    @Column(length = 500)
    private String notes;
    
    // Valuation parameters
    @Column(name = "estimated_fcf_growth_rate", precision = 5, scale = 4)
    private BigDecimal estimatedFcfGrowthRate;
    
    @Column(name = "investment_horizon_years")
    private Integer investmentHorizonYears;
    
    @Column(name = "discount_rate", precision = 5, scale = 4)
    private BigDecimal discountRate;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (ticker != null) {
            ticker = ticker.trim().toUpperCase();
        }
        if (notifyWhenBelowPrice == null) {
            notifyWhenBelowPrice = false;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (ticker != null) {
            ticker = ticker.trim().toUpperCase();
        }
    }
}
