-- Add focf_cagr_5y column to instrument_fundamentals
-- This field is critical for calculating the Growth Rate in DCF models
-- It represents the 5-year Compound Annual Growth Rate of Free Operating Cash Flow

ALTER TABLE instrument_fundamentals
    ADD COLUMN IF NOT EXISTS focf_cagr_5y DECIMAL(10, 4);

COMMENT ON COLUMN instrument_fundamentals.focf_cagr_5y IS '5-year Free Operating Cash Flow CAGR';
