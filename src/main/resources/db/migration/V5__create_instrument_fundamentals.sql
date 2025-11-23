-- Create instrument_fundamentals table for caching Finnhub data
CREATE TABLE instrument_fundamentals (
    ticker VARCHAR(12) PRIMARY KEY,
    
    -- Basic info
    company_name VARCHAR(255),
    exchange VARCHAR(50),
    currency VARCHAR(10),
    sector VARCHAR(100),
    industry VARCHAR(100),
    
    -- Price data
    current_price DECIMAL(19, 4),
    price_change DECIMAL(19, 4),
    price_change_percent DECIMAL(10, 2),
    previous_close DECIMAL(19, 4),
    high_52_week DECIMAL(19, 4),
    low_52_week DECIMAL(19, 4),
    
    -- Valuation metrics
    pe_ttm DECIMAL(19, 4),
    price_to_book DECIMAL(19, 4),
    price_to_sales DECIMAL(19, 4),
    
    -- Risk metrics
    beta DECIMAL(10, 4),
    
    -- Cash Flow data
    fcf_per_share_ttm DECIMAL(19, 4),
    fcf_per_share_annual DECIMAL(19, 4),
    operating_cash_flow DECIMAL(19, 2),
    capital_expenditures DECIMAL(19, 2),
    free_cash_flow DECIMAL(19, 2),
    
    -- Profitability
    eps_ttm DECIMAL(19, 4),
    revenue_ttm DECIMAL(19, 2),
    net_income_ttm DECIMAL(19, 2),
    operating_margin DECIMAL(10, 4),
    profit_margin DECIMAL(10, 4),
    roe DECIMAL(10, 4),
    roa DECIMAL(10, 4),
    
    -- Share info
    shares_outstanding BIGINT,
    market_capitalization DECIMAL(19, 2),
    
    -- Dividends
    dividend_yield DECIMAL(10, 4),
    dividend_per_share DECIMAL(19, 4),
    payout_ratio DECIMAL(10, 4),
    
    -- Metadata
    data_quality VARCHAR(20) DEFAULT 'COMPLETE', -- COMPLETE, PARTIAL, STALE
    source VARCHAR(50) DEFAULT 'FINNHUB',
    last_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT chk_data_quality CHECK (data_quality IN ('COMPLETE', 'PARTIAL', 'STALE')),
    CONSTRAINT chk_source CHECK (source IN ('FINNHUB', 'MANUAL', 'CALCULATED'))
);

-- Index for performance
CREATE INDEX idx_fundamentals_updated_at ON instrument_fundamentals(last_updated_at);
CREATE INDEX idx_fundamentals_ticker_updated ON instrument_fundamentals(ticker, last_updated_at);
CREATE INDEX idx_fundamentals_data_quality ON instrument_fundamentals(data_quality);

-- Comments
COMMENT ON TABLE instrument_fundamentals IS 'Cached snapshots of fundamental data from Finnhub API';
COMMENT ON COLUMN instrument_fundamentals.data_quality IS 'COMPLETE: all fields populated, PARTIAL: some missing, STALE: older than 24h';
COMMENT ON COLUMN instrument_fundamentals.last_updated_at IS 'Timestamp of last successful data fetch from Finnhub';
