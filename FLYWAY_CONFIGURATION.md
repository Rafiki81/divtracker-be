# Resumen de Configuración de Flyway y Cambios

## Cambios Realizados

### 1. Migración de User de Long a UUID

**Problema identificado**: La entidad `User` usaba `Long` como ID pero `WatchlistItem` usaba `UUID`, causando incompatibilidad.

**Solución implementada**:
- ✅ Actualizado modelo `User` para usar `UUID` en lugar de `Long`
- ✅ Actualizado `AuthResponse` DTO para usar `UUID`
- ✅ Eliminado método auxiliar `getUserIdAsUUID()` del `WatchlistController`
- ✅ Actualizados todos los tests para usar `UUID`

### 2. Migraciones de Flyway

Se crearon dos migraciones en orden:

#### V1__create_users_table.sql
```sql
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
```

**Características**:
- ID tipo UUID con generación automática
- Email único con índice
- Soporte para autenticación LOCAL y OAuth (GOOGLE)
- Campos de auditoría (created_at, updated_at)
- Índices para optimización de consultas

#### V2__create_watchlist_items.sql
```sql
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
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

**Características**:
- ID tipo UUID con generación automática
- `user_id` tipo UUID (compatible con tabla users)
- Constraint: al menos uno de `target_price` o `target_pfcf` debe estar presente
- Constraint: valores target deben ser positivos
- Índice único para (user_id, ticker) case-insensitive
- Índices para optimización de consultas

### 3. Configuración de Flyway

#### application.properties (producción)
```properties
# Flyway
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true
```

#### application-test.properties (tests)
```properties
# Flyway deshabilitado en tests
spring.flyway.enabled=false

# JPA crea las tablas automáticamente
spring.jpa.hibernate.ddl-auto=create-drop
```

**Razón**: En tests usamos H2 con creación automática de esquema para velocidad y simplicidad.

### 4. Estructura de Base de Datos

```
users (UUID)
  ↓ (user_id)
watchlist_items (UUID)
```

**Relación**: `watchlist_items.user_id` referencia a `users.id` (ambos UUID)

## Verificación

### Para verificar que Flyway está funcionando correctamente:

1. **Iniciar la aplicación** (debe conectarse a PostgreSQL)
2. **Verificar las migraciones ejecutadas**:
```sql
SELECT * FROM flyway_schema_history;
```

Deberías ver:
- V1__create_users_table.sql - SUCCESS
- V2__create_watchlist_items.sql - SUCCESS

3. **Verificar las tablas creadas**:
```sql
\dt
-- Deberías ver: users, watchlist_items, flyway_schema_history
```

4. **Verificar los tipos de columnas**:
```sql
\d users
\d watchlist_items
-- Ambas tablas deben tener id como UUID
```

### Para ejecutar los tests:

```bash
# Todos los tests
mvn test

# Solo tests de autenticación
mvn test -Dtest=AuthControllerIntegrationTest

# Solo tests de watchlist
mvn test -Dtest=WatchlistControllerIntegrationTest

# Solo tests unitarios
mvn test -Dtest=AuthServiceTest,WatchlistServiceTest
```

## Ventajas de usar UUID

1. **Seguridad**: No expone el número total de registros
2. **Distribución**: Permite generar IDs en múltiples instancias sin conflictos
3. **Escalabilidad**: Facilita sharding y replicación
4. **Consistencia**: Mismo tipo de ID en toda la aplicación

## Desventajas a considerar

1. **Tamaño**: UUID (16 bytes) vs Long (8 bytes)
2. **Performance**: Índices ligeramente más lentos con UUID
3. **Legibilidad**: UUIDs son menos legibles que números secuenciales

## Próximos Pasos

1. ✅ Configurar PostgreSQL en desarrollo
2. ✅ Ejecutar la aplicación y verificar migraciones
3. ✅ Ejecutar todos los tests
4. ⏳ Configurar foreign key entre watchlist_items y users (opcional)
5. ⏳ Agregar más validaciones a nivel de base de datos

## Notas Importantes

- **No eliminar migraciones ejecutadas**: Flyway mantiene un registro de todas las migraciones
- **Versionar migraciones**: Seguir el patrón V{número}__{descripción}.sql
- **Naming**: user_id (snake_case en DB), userId (camelCase en Java)
- **Tests**: Usan H2 sin Flyway para velocidad

## Referencias

- [Flyway Documentation](https://flywaydb.org/documentation/)
- [Spring Boot Flyway](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.data-initialization.migration-tool.flyway)
- [PostgreSQL UUID](https://www.postgresql.org/docs/current/datatype-uuid.html)
