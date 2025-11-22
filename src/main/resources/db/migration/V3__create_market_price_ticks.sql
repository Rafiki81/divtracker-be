CREATE TABLE market_price_ticks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticker VARCHAR(12) NOT NULL,
    price DECIMAL(19, 6) NOT NULL,
    volume DECIMAL(19, 4),
    trade_timestamp TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    source VARCHAR(32) NOT NULL DEFAULT 'FINNHUB'
);

CREATE INDEX idx_market_price_ticks_ticker_time
    ON market_price_ticks (ticker, trade_timestamp DESC);

COMMENT ON TABLE market_price_ticks IS 'Precio en tiempo real proveniente del stream de Finnhub';
COMMENT ON COLUMN market_price_ticks.ticker IS 'Ticker normalizado a mayúsculas';
COMMENT ON COLUMN market_price_ticks.price IS 'Precio del trade recibido';
COMMENT ON COLUMN market_price_ticks.volume IS 'Volumen reportado por Finnhub';
COMMENT ON COLUMN market_price_ticks.trade_timestamp IS 'Instante del trade según Finnhub';
COMMENT ON COLUMN market_price_ticks.received_at IS 'Instante en el que la app persistió el trade';
COMMENT ON COLUMN market_price_ticks.source IS 'Origen del dato (Finnhub, etc.)';
