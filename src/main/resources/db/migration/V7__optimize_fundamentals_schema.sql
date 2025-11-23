-- Optimize instrument_fundamentals table
-- Simplified from 30+ fields to 14 essential fields
-- Strategy: Use ANNUAL data instead of quarterly/TTM for more stability
-- All fields verified available in production logs (TAP.A, GIS)

-- Step 1: Drop unnecessary columns that were removed from the model
ALTER TABLE instrument_fundamentals
    DROP COLUMN IF EXISTS exchange,
    DROP COLUMN IF EXISTS industry,
    DROP COLUMN IF EXISTS price_change,
    DROP COLUMN IF EXISTS price_change_percent,
    DROP COLUMN IF EXISTS previous_close,
    DROP COLUMN IF EXISTS high_52_week,
    DROP COLUMN IF EXISTS low_52_week,
    DROP COLUMN IF EXISTS pe_ttm,
    DROP COLUMN IF EXISTS price_to_book,
    DROP COLUMN IF EXISTS price_to_sales,
    DROP COLUMN IF EXISTS fcf_per_share_ttm,
    DROP COLUMN IF EXISTS operating_cash_flow,
    DROP COLUMN IF EXISTS capital_expenditures,
    DROP COLUMN IF EXISTS free_cash_flow,
    DROP COLUMN IF EXISTS eps_ttm,
    DROP COLUMN IF EXISTS revenue_ttm,
    DROP COLUMN IF EXISTS net_income_ttm,
    DROP COLUMN IF EXISTS operating_margin,
    DROP COLUMN IF EXISTS profit_margin,
    DROP COLUMN IF EXISTS roe,
    DROP COLUMN IF EXISTS roa,
    DROP COLUMN IF EXISTS shares_outstanding,
    DROP COLUMN IF EXISTS market_capitalization,
    DROP COLUMN IF EXISTS dividend_yield,
    DROP COLUMN IF EXISTS dividend_per_share,
    DROP COLUMN IF EXISTS payout_ratio;

-- Step 2: Rename fcf_per_share_annual to match new naming (kept this one, removed TTM)
-- Note: fcf_ttm was never added to the table, so no need to drop it

-- Step 3: Add missing columns from the new model
ALTER TABLE instrument_fundamentals
    ADD COLUMN IF NOT EXISTS pe_annual DECIMAL(19, 4),
    ADD COLUMN IF NOT EXISTS debt_to_equity_ratio DECIMAL(19, 4),
    ADD COLUMN IF NOT EXISTS fcf_annual DECIMAL(19, 2),
    ADD COLUMN IF NOT EXISTS share_outstanding DECIMAL(19, 2),
    ADD COLUMN IF NOT EXISTS dividend_per_share_annual DECIMAL(19, 4),
    ADD COLUMN IF NOT EXISTS dividend_yield DECIMAL(10, 4),
    ADD COLUMN IF NOT EXISTS dividend_growth_rate_5y DECIMAL(10, 2),
    ADD COLUMN IF NOT EXISTS eps_growth_5y DECIMAL(10, 4),
    ADD COLUMN IF NOT EXISTS revenue_growth_5y DECIMAL(10, 4);

-- Step 4: Comments for new columns
COMMENT ON COLUMN instrument_fundamentals.pe_annual IS 'Price-to-Earnings ratio (Annual) - more reliable than TTM';
COMMENT ON COLUMN instrument_fundamentals.debt_to_equity_ratio IS 'Total Debt / Total Equity (Quarterly) from Finnhub metrics';
COMMENT ON COLUMN instrument_fundamentals.fcf_annual IS 'Free Cash Flow (Annual) = Operating Cash Flow + Capital Expenditure';
COMMENT ON COLUMN instrument_fundamentals.fcf_per_share_annual IS 'Free Cash Flow per Share (Annual) = FCF / shares outstanding';
COMMENT ON COLUMN instrument_fundamentals.share_outstanding IS 'Number of shares outstanding (in millions)';
COMMENT ON COLUMN instrument_fundamentals.dividend_per_share_annual IS 'Annual dividend per share';
COMMENT ON COLUMN instrument_fundamentals.dividend_yield IS 'Dividend yield percentage (TTM)';
COMMENT ON COLUMN instrument_fundamentals.dividend_growth_rate_5y IS '5-year dividend growth rate';
COMMENT ON COLUMN instrument_fundamentals.eps_growth_5y IS '5-year EPS growth rate';
COMMENT ON COLUMN instrument_fundamentals.revenue_growth_5y IS '5-year Revenue growth rate';

-- Step 5: Update table comment to reflect new simplified structure
COMMENT ON TABLE instrument_fundamentals IS 'Cached snapshots of essential fundamental data from Finnhub API (14 fields, annual data strategy)';

