-- Fix precision for dividend_growth_rate_5y to match Entity (10, 4)
-- V7 created it as (10, 2) but we need more precision for percentages

ALTER TABLE instrument_fundamentals
    ALTER COLUMN dividend_growth_rate_5y TYPE DECIMAL(10, 4);
