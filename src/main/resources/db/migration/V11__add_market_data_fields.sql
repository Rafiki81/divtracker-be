-- Add market data fields from Finnhub that we already receive but don't store
ALTER TABLE instrument_fundamentals
ADD COLUMN market_capitalization DECIMAL(19, 2),
ADD COLUMN week_high_52 DECIMAL(19, 4),
ADD COLUMN week_low_52 DECIMAL(19, 4),
ADD COLUMN daily_change_percent DECIMAL(10, 4);
