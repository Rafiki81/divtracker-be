-- Crear tabla watchlist_items
CREATE TABLE watchlist_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    ticker VARCHAR(12) NOT NULL,
    exchange VARCHAR(50),
    target_price DECIMAL(19, 4),
    target_pfcf DECIMAL(19, 4),
    notify_when_below_price BOOLEAN NOT NULL DEFAULT false,
    notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraint: Al menos uno de target_price o target_pfcf debe estar presente
    CONSTRAINT check_at_least_one_target CHECK (
        target_price IS NOT NULL OR target_pfcf IS NOT NULL
    ),
    
    -- Constraint: Los valores target deben ser positivos
    CONSTRAINT check_positive_target_price CHECK (
        target_price IS NULL OR target_price > 0
    ),
    CONSTRAINT check_positive_target_pfcf CHECK (
        target_pfcf IS NULL OR target_pfcf > 0
    )
);

-- Índice único para user_id y ticker (case insensitive)
CREATE UNIQUE INDEX idx_watchlist_user_ticker ON watchlist_items (user_id, UPPER(ticker));

-- Índice para búsquedas por user_id
CREATE INDEX idx_watchlist_user_id ON watchlist_items (user_id);

-- Índice para ordenamiento por createdAt
CREATE INDEX idx_watchlist_created_at ON watchlist_items (created_at DESC);

-- Comentarios para documentación
COMMENT ON TABLE watchlist_items IS 'Items del watchlist de usuarios para seguimiento de empresas y alertas';
COMMENT ON COLUMN watchlist_items.id IS 'Identificador único del item';
COMMENT ON COLUMN watchlist_items.user_id IS 'ID del usuario propietario del item';
COMMENT ON COLUMN watchlist_items.ticker IS 'Símbolo ticker de la empresa (normalizado a mayúsculas)';
COMMENT ON COLUMN watchlist_items.exchange IS 'Mercado o exchange donde cotiza la empresa';
COMMENT ON COLUMN watchlist_items.target_price IS 'Precio objetivo para alerta';
COMMENT ON COLUMN watchlist_items.target_pfcf IS 'PFCF (Price to Free Cash Flow) objetivo';
COMMENT ON COLUMN watchlist_items.notify_when_below_price IS 'Activar notificación cuando precio esté por debajo del objetivo';
COMMENT ON COLUMN watchlist_items.notes IS 'Notas adicionales del usuario sobre la empresa';
