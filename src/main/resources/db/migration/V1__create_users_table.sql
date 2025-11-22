-- Crear tabla users
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    provider VARCHAR(50) NOT NULL,
    provider_id VARCHAR(255),
    role VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índice para búsquedas por email
CREATE UNIQUE INDEX idx_users_email ON users (email);

-- Índice para búsquedas por provider y providerId (OAuth)
CREATE INDEX idx_users_provider ON users (provider, provider_id);

-- Comentarios para documentación
COMMENT ON TABLE users IS 'Tabla de usuarios del sistema';
COMMENT ON COLUMN users.id IS 'Identificador único del usuario (UUID)';
COMMENT ON COLUMN users.email IS 'Email del usuario (único)';
COMMENT ON COLUMN users.password IS 'Contraseña encriptada (puede ser NULL para usuarios OAuth)';
COMMENT ON COLUMN users.first_name IS 'Nombre del usuario';
COMMENT ON COLUMN users.last_name IS 'Apellido del usuario';
COMMENT ON COLUMN users.provider IS 'Proveedor de autenticación (LOCAL, GOOGLE)';
COMMENT ON COLUMN users.provider_id IS 'ID del proveedor externo (para OAuth)';
COMMENT ON COLUMN users.role IS 'Rol del usuario (USER, ADMIN)';
COMMENT ON COLUMN users.enabled IS 'Indica si la cuenta está activa';
COMMENT ON COLUMN users.created_at IS 'Fecha de creación de la cuenta';
COMMENT ON COLUMN users.updated_at IS 'Fecha de última actualización';
