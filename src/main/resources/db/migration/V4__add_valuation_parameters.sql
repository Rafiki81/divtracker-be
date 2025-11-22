-- V4: Add valuation parameters to watchlist_items table
-- These fields enable advanced financial metrics calculation (DCF, IRR, ROI, etc.)

-- Add estimated FCF growth rate (stored as decimal, e.g., 0.08 for 8%)
ALTER TABLE watchlist_items ADD COLUMN estimated_fcf_growth_rate DECIMAL(5, 4);

COMMENT ON COLUMN watchlist_items.estimated_fcf_growth_rate IS 'Expected annual FCF growth rate as decimal (e.g., 0.08 = 8%)';

-- Add investment horizon in years
ALTER TABLE watchlist_items ADD COLUMN investment_horizon_years INTEGER;

COMMENT ON COLUMN watchlist_items.investment_horizon_years IS 'Investment time horizon in years for valuation calculations';

-- Add discount rate / WACC (stored as decimal, e.g., 0.10 for 10%)
ALTER TABLE watchlist_items ADD COLUMN discount_rate DECIMAL(5, 4);

COMMENT ON COLUMN watchlist_items.discount_rate IS 'Discount rate or WACC as decimal (e.g., 0.10 = 10%) used in DCF calculations';

-- Set default values for existing rows (conservative assumptions)
UPDATE watchlist_items 
SET 
    estimated_fcf_growth_rate = 0.05,  -- Default 5% growth
    investment_horizon_years = 5,       -- Default 5-year horizon
    discount_rate = 0.10                -- Default 10% discount rate
WHERE estimated_fcf_growth_rate IS NULL;
