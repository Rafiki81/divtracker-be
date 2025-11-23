package com.rafiki18.divtracker_be.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing cached fundamental data from Finnhub.
 * Allows working offline and reduces API calls.
 */
@Entity
@Table(name = "instrument_fundamentals")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstrumentFundamentals {

    @Id
    @Column(name = "ticker", nullable = false, length = 12)
    private String ticker;

    // Basic info
    @Column(name = "company_name", length = 255)
    private String companyName;

    @Column(name = "currency", length = 10)
    private String currency;

    @Column(name = "sector", length = 100)
    private String sector;

    // Price data
    @Column(name = "current_price", precision = 19, scale = 4)
    private BigDecimal currentPrice;

    // Valuation metrics
    @Column(name = "pe_annual", precision = 19, scale = 4)
    private BigDecimal peAnnual;

    // Risk metrics
    @Column(name = "beta", precision = 10, scale = 4)
    private BigDecimal beta;

    @Column(name = "debt_to_equity_ratio", precision = 10, scale = 4)
    private BigDecimal debtToEquityRatio;

    // Cash Flow data (annual)
    @Column(name = "fcf_annual", precision = 19, scale = 2)
    private BigDecimal fcfAnnual;

    @Column(name = "fcf_per_share_annual", precision = 19, scale = 4)
    private BigDecimal fcfPerShareAnnual;

    // Share info
    @Column(name = "share_outstanding", precision = 19, scale = 2)
    private BigDecimal shareOutstanding;

    // Dividend data
    @Column(name = "dividend_per_share_annual", precision = 19, scale = 4)
    private BigDecimal dividendPerShareAnnual;

    @Column(name = "dividend_yield", precision = 10, scale = 4)
    private BigDecimal dividendYield;

    @Column(name = "dividend_growth_rate_5y", precision = 10, scale = 4)
    private BigDecimal dividendGrowthRate5Y;

    // Growth metrics
    @Column(name = "eps_growth_5y", precision = 10, scale = 4)
    private BigDecimal epsGrowth5Y;

    @Column(name = "revenue_growth_5y", precision = 10, scale = 4)
    private BigDecimal revenueGrowth5Y;

    @Column(name = "focf_cagr_5y", precision = 10, scale = 4)
    private BigDecimal focfCagr5Y;

    // Metadata
    @Enumerated(EnumType.STRING)
    @Column(name = "data_quality", length = 20, nullable = false)
    @Builder.Default
    private DataQuality dataQuality = DataQuality.COMPLETE;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", length = 50, nullable = false)
    @Builder.Default
    private DataSource source = DataSource.FINNHUB;

    @Column(name = "last_updated_at", nullable = false)
    private LocalDateTime lastUpdatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        lastUpdatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdatedAt = LocalDateTime.now();
    }

    /**
     * Check if data is stale (older than 24 hours).
     */
    public boolean isStale() {
        return lastUpdatedAt.isBefore(LocalDateTime.now().minusHours(24));
    }

    /**
     * Check if we have minimum required data for valuation.
     */
    public boolean hasMinimumData() {
        return currentPrice != null && fcfPerShareAnnual != null;
    }

    /**
     * Get FCF per share value.
     */
    public BigDecimal getFcfPerShare() {
        return fcfPerShareAnnual;
    }

    public enum DataQuality {
        COMPLETE,  // All critical fields populated
        PARTIAL,   // Some fields missing but usable
        STALE      // Data older than 24h
    }

    public enum DataSource {
        FINNHUB,   // Fetched from Finnhub API
        MANUAL,    // Manually entered
        CALCULATED // Derived from other data
    }
}
