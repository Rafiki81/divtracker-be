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

    @Column(name = "exchange", length = 50)
    private String exchange;

    @Column(name = "currency", length = 10)
    private String currency;

    @Column(name = "sector", length = 100)
    private String sector;

    @Column(name = "industry", length = 100)
    private String industry;

    // Price data
    @Column(name = "current_price", precision = 19, scale = 4)
    private BigDecimal currentPrice;

    @Column(name = "price_change", precision = 19, scale = 4)
    private BigDecimal priceChange;

    @Column(name = "price_change_percent", precision = 10, scale = 2)
    private BigDecimal priceChangePercent;

    @Column(name = "previous_close", precision = 19, scale = 4)
    private BigDecimal previousClose;

    @Column(name = "high_52_week", precision = 19, scale = 4)
    private BigDecimal high52Week;

    @Column(name = "low_52_week", precision = 19, scale = 4)
    private BigDecimal low52Week;

    // Valuation metrics
    @Column(name = "pe_ttm", precision = 19, scale = 4)
    private BigDecimal peTTM;

    @Column(name = "price_to_book", precision = 19, scale = 4)
    private BigDecimal priceToBook;

    @Column(name = "price_to_sales", precision = 19, scale = 4)
    private BigDecimal priceToSales;

    // Risk metrics
    @Column(name = "beta", precision = 10, scale = 4)
    private BigDecimal beta;

    // Cash Flow data
    @Column(name = "fcf_per_share_ttm", precision = 19, scale = 4)
    private BigDecimal fcfPerShareTTM;

    @Column(name = "fcf_per_share_annual", precision = 19, scale = 4)
    private BigDecimal fcfPerShareAnnual;

    @Column(name = "operating_cash_flow", precision = 19, scale = 2)
    private BigDecimal operatingCashFlow;

    @Column(name = "capital_expenditures", precision = 19, scale = 2)
    private BigDecimal capitalExpenditures;

    @Column(name = "free_cash_flow", precision = 19, scale = 2)
    private BigDecimal freeCashFlow;

    // Profitability
    @Column(name = "eps_ttm", precision = 19, scale = 4)
    private BigDecimal epsTTM;

    @Column(name = "revenue_ttm", precision = 19, scale = 2)
    private BigDecimal revenueTTM;

    @Column(name = "net_income_ttm", precision = 19, scale = 2)
    private BigDecimal netIncomeTTM;

    @Column(name = "operating_margin", precision = 10, scale = 4)
    private BigDecimal operatingMargin;

    @Column(name = "profit_margin", precision = 10, scale = 4)
    private BigDecimal profitMargin;

    @Column(name = "roe", precision = 10, scale = 4)
    private BigDecimal roe;

    @Column(name = "roa", precision = 10, scale = 4)
    private BigDecimal roa;

    // Share info
    @Column(name = "shares_outstanding")
    private Long sharesOutstanding;

    @Column(name = "market_capitalization", precision = 19, scale = 2)
    private BigDecimal marketCapitalization;

    // Dividends
    @Column(name = "dividend_yield", precision = 10, scale = 4)
    private BigDecimal dividendYield;

    @Column(name = "dividend_per_share", precision = 19, scale = 4)
    private BigDecimal dividendPerShare;

    @Column(name = "payout_ratio", precision = 10, scale = 4)
    private BigDecimal payoutRatio;

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
        return currentPrice != null && 
               (fcfPerShareTTM != null || fcfPerShareAnnual != null);
    }

    /**
     * Get the best available FCF per share value.
     */
    public BigDecimal getBestFcfPerShare() {
        if (fcfPerShareTTM != null) {
            return fcfPerShareTTM;
        }
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
