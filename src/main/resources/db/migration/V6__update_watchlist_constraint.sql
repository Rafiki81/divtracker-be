-- Modificar constraint para permitir crear watchlist items con solo notes
-- Esto permite añadir stocks sin especificar precios objetivos inicialmente

-- Eliminar el constraint antiguo
ALTER TABLE watchlist_items DROP CONSTRAINT check_at_least_one_target;

-- Añadir el nuevo constraint que permite notes como valor válido
ALTER TABLE watchlist_items ADD CONSTRAINT check_at_least_one_target CHECK (
    target_price IS NOT NULL OR target_pfcf IS NOT NULL OR notes IS NOT NULL
);

COMMENT ON CONSTRAINT check_at_least_one_target ON watchlist_items IS 
    'Al menos uno de los siguientes debe tener valor: target_price, target_pfcf, o notes';
