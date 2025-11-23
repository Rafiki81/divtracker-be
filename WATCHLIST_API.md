# Watchlist API - Documentación

## Descripción General

La funcionalidad de Watchlist (Radar) permite a los usuarios autenticados gestionar una lista personalizada de empresas que desean vigilar, con la posibilidad de establecer precios objetivo y ratios PFCF (Price to Free Cash Flow) para alertas.

## Características Principales

- ✅ **CRUD completo** de items del watchlist
- ✅ **Seguridad por usuario**: Cada usuario solo puede ver y gestionar sus propios items
- ✅ **Validaciones robustas**: Ticker único por usuario, valores positivos, etc.
- ✅ **Paginación y ordenamiento** en listados
- ✅ **Actualización parcial (PATCH)** para modificar campos específicos
- ✅ **Documentación OpenAPI/Swagger** integrada
- ✅ **Tests unitarios y de integración** completos

## Modelo de Datos

### Entidad: WatchlistItem

```java
{
  "id": "UUID",
  "userId": "UUID",
  "ticker": "String (1-12 caracteres, normalizado a mayúsculas)",
  "exchange": "String (opcional, máx 50 caracteres)",
  "targetPrice": "BigDecimal (opcional, >0)",
  "targetPfcf": "BigDecimal (opcional, >0)",
  "notifyWhenBelowPrice": "Boolean (default: false)",
  "notes": "String (opcional, máx 500 caracteres)",
  "createdAt": "LocalDateTime",
  "updatedAt": "LocalDateTime"
}
```

### Restricciones

- **Ticker único por usuario** (case insensitive)
- **Al menos uno** de `targetPrice` o `targetPfcf` debe estar presente
- **Valores positivos** para precios y ratios
- **Aislamiento total** entre usuarios

## Endpoints API

### Base URL: `/api/v1/watchlist` y `/api/v1/tickers`

Todos los endpoints requieren autenticación JWT mediante header:
```
Authorization: Bearer <token>
```

### 0. Buscar Tickers

#### 0.1. Symbol Lookup (Búsqueda Exacta) - **RECOMENDADO**

```http
GET /api/v1/tickers/lookup?symbol={ticker}
```

**Descripción**: Busca símbolos exactos en exchanges US. Ideal para encontrar variaciones de un ticker específico (ej: BAM → BAM, BAM.A, BAM.B). **Este endpoint es el recomendado para el flujo inicial de selección de ticker.**

**Parámetros de Query:**
- `symbol` (requerido): Símbolo del ticker a buscar (1-12 caracteres)
  - Ejemplos: "BAM", "AAPL", "MSFT"
  - Busca símbolos que comienzan con el valor proporcionado
  - Retorna coincidencias exactas de US exchanges

**Respuesta 200 OK:**
```json
[
  {
    "symbol": "BAM",
    "description": "Brookfield Asset Management Inc",
    "type": "Common Stock",
    "exchange": "NYSE",
    "currency": "USD",
    "figi": "BBG000C46HM9"
  },
  {
    "symbol": "BAM.A",
    "description": "Brookfield Asset Management Ltd Class A",
    "type": "Common Stock",
    "exchange": "NYSE",
    "currency": "USD",
    "figi": "BBG00YWGZ6V9"
  }
]
```

**Flujo recomendado:**
1. Usuario escribe "BAM" en el campo de búsqueda
2. App llama a `/api/v1/tickers/lookup?symbol=BAM`
3. Backend devuelve todas las variaciones (BAM, BAM.A, etc.)
4. Usuario selecciona el símbolo exacto de la lista
5. App crea watchlist item con el símbolo validado

---

#### 0.2. Search by Name (Búsqueda Fuzzy)

```http
GET /api/v1/tickers/search?q={query}
```

**Descripción**: Busca tickers por nombre de empresa. Realiza búsqueda fuzzy flexible contra la API de Finnhub. Útil cuando el usuario no conoce el ticker exacto.

**Parámetros de Query:**
- `q` (requerido): Término de búsqueda (mínimo 1 carácter)
  - Ejemplos: "apple", "microsoft", "tesla"
  - No distingue mayúsculas/minúsculas
  - Busca coincidencias parciales por nombre de compañía

**Respuesta 200 OK:**
```json
[
  {
    "symbol": "AAPL",
    "description": "Apple Inc",
    "type": "Common Stock",
    "exchange": "NASDAQ",
    "currency": "USD",
    "figi": "BBG000B9XRY4"
  },
  {
    "symbol": "AAPL.SW",
    "description": "Apple Inc",
    "type": "Common Stock",
    "exchange": "SIX",
    "currency": "CHF",
    "figi": "BBG000B9Y5X2"
  }
]
```

---

#### Respuestas Comunes (Ambos Endpoints)

**Respuesta 400 Bad Request:**
```json
{
  "timestamp": "2025-11-23T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Query parameter is required",
  "path": "/api/v1/tickers/lookup"
}
```

**Respuesta 503 Service Unavailable:**
```json
{
  "timestamp": "2025-11-23T10:30:00",
  "status": 503,
  "error": "Service Unavailable",
  "message": "Ticker search service is currently unavailable",
  "path": "/api/v1/tickers/lookup"
}
```

**Notas:**
- Ambos endpoints retornan hasta 20 resultados
- Requieren que Finnhub esté configurado (`FINNHUB_API_KEY`)
- **Lookup** es más rápido y preciso para tickers conocidos
- **Search** es mejor para buscar por nombre de compañía
- Útiles para implementar autocompletado en UI

### 1. Listar Items del Watchlist

```http
GET /api/v1/watchlist?page=0&size=20&sortBy=createdAt&direction=DESC
```

**Parámetros de Query:**
- `page` (default: 0): Número de página (0-indexed)
- `size` (default: 20): Tamaño de página
- `sortBy` (default: "createdAt"): Campo de ordenamiento
- `direction` (default: "DESC"): Dirección (ASC/DESC)

**Respuesta 200 OK:**
```json
{
  "content": [
    {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "userId": "123e4567-e89b-12d3-a456-426614174001",
      "ticker": "AAPL",
      "exchange": "NASDAQ",
      "targetPrice": 150.50,
      "targetPfcf": 15.5,
      "notifyWhenBelowPrice": false,
      "notes": "Apple Inc.",
      "createdAt": "2025-10-19T10:30:00",
      "updatedAt": "2025-10-19T10:30:00"
    }
  ],
  "pageable": {...},
  "totalElements": 1,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

### 2. Obtener Item por ID

```http
GET /api/v1/watchlist/{id}
```

**Respuesta 200 OK:** Item encontrado  
**Respuesta 404 Not Found:** Item no existe o pertenece a otro usuario

### 3. Crear Nuevo Item

```http
POST /api/v1/watchlist
Content-Type: application/json
```

El sistema soporta **4 modos diferentes** de crear items, con cálculos automáticos inteligentes:

---

#### **Modo 1: Carga Completamente Automática (Solo Ticker)** ⚡

Si solo proporcionas el ticker, el sistema carga **TODO** automáticamente desde Finnhub:

```json
{
  "ticker": "AAPL"
}
```

**El sistema automáticamente:**
1. Obtiene `currentPrice` desde Finnhub (ej: $172.15)
2. Obtiene `freeCashFlowPerShare` desde Finnhub (ej: $6.75)
3. Calcula `targetPfcf = currentPrice / FCF` (ej: 25.50)
4. Calcula `targetPrice = FCF × targetPfcf` (ej: $172.13)
5. Enriquece la respuesta con todas las métricas financieras

**Requisitos:**
- Finnhub debe estar configurado (`FINNHUB_API_KEY`)
- El ticker debe existir en Finnhub
- Finnhub debe tener datos de precio y FCF para el ticker

**Respuesta 201 Created:**
```json
{
  "id": "uuid-here",
  "ticker": "AAPL",
  "targetPrice": 172.13,
  "targetPfcf": 25.50,
  "currentPrice": 172.15,
  "freeCashFlowPerShare": 6.75,
  "actualPfcf": 25.50,
  "dcfFairValue": 172.13,
  "marginOfSafety": -0.01,
  "fcfYield": 3.92,
  "paybackPeriod": 25.50,
  "estimatedIRR": 3.92,
  "estimatedROI": 19.60,
  "createdAt": "2025-11-23T10:30:00",
  "updatedAt": "2025-11-23T10:30:00"
}
```

---

#### **Modo 2: Solo Target P/FCF (Calcula Target Price)** 🎯

Proporciona solo el múltiplo P/FCF deseado:

```json
{
  "ticker": "AAPL",
  "targetPfcf": 20.0
}
```

**El sistema automáticamente:**
1. Obtiene `freeCashFlowPerShare` desde Finnhub (ej: $6.75)
2. Calcula `targetPrice = FCF × targetPfcf` (ej: $6.75 × 20 = **$135.00**)
3. Guarda ambos valores
4. Enriquece con métricas actuales

**Requisitos:**
- Finnhub debe estar configurado
- Finnhub debe tener datos de FCF para el ticker

**Respuesta 201 Created:**
```json
{
  "id": "uuid-here",
  "ticker": "AAPL",
  "targetPrice": 135.00,
  "targetPfcf": 20.0,
  "currentPrice": 172.15,
  "freeCashFlowPerShare": 6.75,
  "actualPfcf": 25.50,
  "dcfFairValue": 135.00,
  "marginOfSafety": 21.58,
  "undervalued": false,
  "createdAt": "2025-11-23T10:30:00"
}
```

---

#### **Modo 3: Solo Target Price (Calcula Target P/FCF)** 💰

Proporciona solo el precio objetivo:

```json
{
  "ticker": "AAPL",
  "targetPrice": 150.00
}
```

**El sistema automáticamente:**
1. Obtiene `freeCashFlowPerShare` desde Finnhub (ej: $6.75)
2. Calcula `targetPfcf = targetPrice / FCF` (ej: $150 / $6.75 = **22.22**)
3. Guarda ambos valores
4. Enriquece con métricas actuales

**Requisitos:**
- Finnhub debe estar configurado
- Finnhub debe tener datos de FCF para el ticker

**Respuesta 201 Created:**
```json
{
  "id": "uuid-here",
  "ticker": "AAPL",
  "targetPrice": 150.00,
  "targetPfcf": 22.22,
  "currentPrice": 172.15,
  "freeCashFlowPerShare": 6.75,
  "actualPfcf": 25.50,
  "dcfFairValue": 150.00,
  "marginOfSafety": 12.86,
  "undervalued": false,
  "createdAt": "2025-11-23T10:30:00"
}
```

---

#### **Modo 4: Valores Manuales Completos (Sin Cálculos)** ✏️

Proporciona ambos valores manualmente:

```json
{
  "ticker": "AAPL",
  "exchange": "NASDAQ",
  "targetPrice": 150.50,
  "targetPfcf": 15.5,
  "notifyWhenBelowPrice": false,
  "notes": "Análisis conservador basado en FCF histórico"
}
```

**El sistema:**
1. **No calcula nada**, usa exactamente tus valores
2. Solo enriquece con datos actuales de mercado (precio, FCF actual)
3. Calcula métricas comparativas (margen de seguridad, etc.)

**Respuesta 201 Created:**
- Header `Location`: URL del recurso creado
- Body: Item creado con métricas calculadas

---

### Tabla Comparativa de Modos

| Modo | Proporcionas | Sistema Calcula | Requiere Finnhub |
|------|-------------|-----------------|------------------|
| **1. Automático** | Solo ticker | targetPrice + targetPfcf | ✅ Sí (precio + FCF) |
| **2. Target P/FCF** | ticker + targetPfcf | targetPrice | ✅ Sí (FCF) |
| **3. Target Price** | ticker + targetPrice | targetPfcf | ✅ Sí (FCF) |
| **4. Manual** | ticker + ambos valores | Nada | ❌ No (opcional para enriquecimiento) |

---

### Validaciones

- `ticker`: Requerido, 1-12 caracteres, alfanumérico con puntos y guiones
- `targetPrice`: Opcional, pero si presente debe ser > 0
- `targetPfcf`: Opcional, pero si presente debe ser > 0
- `notes`: Máximo 500 caracteres
- Si **no** proporcionas ningún valor, Finnhub **debe estar disponible**
- Si proporcionas **solo uno**, Finnhub **debe tener datos de FCF**

---

### Errores Comunes

**Sin datos y Finnhub no disponible:**
```json
{
  "timestamp": "2025-11-23T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Debe especificar al menos targetPrice o targetPfcf cuando Finnhub no está disponible.",
  "path": "/api/v1/watchlist"
}
```

**Ticker sin datos en Finnhub:**
```json
{
  "timestamp": "2025-11-23T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "No se pudieron obtener datos de mercado para INVALID. Debes especificar al menos targetPrice o targetPfcf manualmente.",
  "path": "/api/v1/watchlist"
}
```

**Solo targetPrice pero sin FCF disponible:**
```json
{
  "timestamp": "2025-11-23T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "No se pudo obtener FCF para TICKER. Debes especificar targetPfcf manualmente.",
  "path": "/api/v1/watchlist"
}
```

**Respuesta 409 Conflict:** Ticker duplicado para el usuario  
**Respuesta 400 Bad Request:** Datos inválidos (valores negativos, formato incorrecto, etc.)

### 4. Actualizar Item (Parcial)

```http
PATCH /api/v1/watchlist/{id}
Content-Type: application/json
```

**Body (todos los campos son opcionales):**
```json
{
  "ticker": "AAPL",
  "exchange": "NASDAQ",
  "targetPrice": 160.00,
  "targetPfcf": 16.0,
  "notifyWhenBelowPrice": true,
  "notes": "Updated notes"
}
```

**Respuesta 200 OK:** Item actualizado  
**Respuesta 404 Not Found:** Item no existe o pertenece a otro usuario  
**Respuesta 409 Conflict:** Nuevo ticker duplicado

### 5. Eliminar Item

```http
DELETE /api/v1/watchlist/{id}
```

**Respuesta 204 No Content:** Eliminado exitosamente  
**Respuesta 404 Not Found:** Item no existe o pertenece a otro usuario

## Ejemplos de Uso

### Usando cURL

```bash
# Symbol Lookup (recomendado - búsqueda exacta)
curl -X GET "http://localhost:8080/api/v1/tickers/lookup?symbol=BAM" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Search by name (búsqueda fuzzy)
curl -X GET "http://localhost:8080/api/v1/tickers/search?q=apple" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Listar items
curl -X GET "http://localhost:8080/api/v1/watchlist" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Crear item (modo automático - solo ticker)
curl -X POST "http://localhost:8080/api/v1/watchlist" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "ticker": "AAPL"
  }'

# Crear item (modo manual - con datos)
curl -X POST "http://localhost:8080/api/v1/watchlist" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "ticker": "AAPL",
    "targetPrice": 150.50,
    "targetPfcf": 15.5,
    "notes": "Apple Inc.",
    "estimatedFcfGrowthRate": 0.08,
    "investmentHorizonYears": 5,
    "discountRate": 0.10
  }'

# Actualizar item
curl -X PATCH "http://localhost:8080/api/v1/watchlist/{id}" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "targetPrice": 160.00
  }'

# Eliminar item
curl -X DELETE "http://localhost:8080/api/v1/watchlist/{id}" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## Base de Datos

### Tabla: watchlist_items

```sql
CREATE TABLE watchlist_items (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    ticker VARCHAR(12) NOT NULL,
    exchange VARCHAR(50),
    target_price DECIMAL(19, 4),
    target_pfcf DECIMAL(19, 4),
    notify_when_below_price BOOLEAN NOT NULL DEFAULT false,
    notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

**Índices:**
- `idx_watchlist_user_ticker`: UNIQUE (user_id, UPPER(ticker))
- `idx_watchlist_user_id`: (user_id)
- `idx_watchlist_created_at`: (created_at DESC)

**Constraints:**
- Al menos uno de target_price o target_pfcf debe estar presente
- Valores de precios deben ser positivos

## Arquitectura

### Capas

1. **Controller** (`WatchlistController`)
   - Endpoints REST
   - Validación de entrada
   - Autenticación y autorización
   - Documentación OpenAPI

2. **Service** (`WatchlistService`)
   - Lógica de negocio
   - Validación de duplicados
   - Aislamiento por usuario
   - Logging

3. **Repository** (`WatchlistItemRepository`)
   - Acceso a datos
   - Queries personalizadas
   - Paginación

4. **Mapper** (`WatchlistMapper`)
   - Conversión DTO ↔ Entity
   - Normalización de datos

### Manejo de Errores

Todas las respuestas de error siguen el estándar RFC7807:

```json
{
  "timestamp": "2025-10-19T10:30:00",
  "status": 409,
  "error": "Conflict",
  "message": "Ticker 'AAPL' already exists in your watchlist",
  "path": "/api/v1/watchlist"
}
```

## Tests

### Tests Unitarios

Ubicación: `src/test/java/.../service/WatchlistServiceTest.java`

Cobertura:
- ✅ Crear item exitosamente
- ✅ Validar ticker duplicado
- ✅ Actualizar item
- ✅ Eliminar item
- ✅ Listar con paginación
- ✅ Manejo de errores

### Tests de Integración

Ubicación: `src/test/java/.../controller/WatchlistControllerIntegrationTest.java`

Cobertura:
- ✅ CRUD completo
- ✅ Autenticación y autorización
- ✅ Aislamiento entre usuarios
- ✅ Validaciones de datos
- ✅ Paginación y ordenamiento

**Ejecutar tests:**
```bash
mvn test
```

## Swagger/OpenAPI

La documentación interactiva está disponible en:

```
http://localhost:8080/swagger-ui.html
```

Busca el tag **"Watchlist"** para ver todos los endpoints documentados.

## Migraciones

Las migraciones se gestionan con Flyway.

**Script de creación:** `src/main/resources/db/migration/V2__create_watchlist_items.sql`

**Ejecutar migraciones:**
```bash
mvn flyway:migrate
```

## Seguridad

- ✅ **Autenticación JWT** requerida en todos los endpoints
- ✅ **Aislamiento por usuario**: Cada usuario solo accede a sus datos
- ✅ **Validación de ownership**: Verificación en cada operación
- ✅ **Normalización de inputs**: Ticker convertido a mayúsculas
- ✅ **Protección contra SQL Injection**: Uso de JPA/Hibernate

## Mejoras Futuras

- [ ] Alertas en tiempo real cuando se alcancen precios objetivo
- [ ] Integración con APIs de cotizaciones en vivo
- [ ] Cálculo automático de PFCF basado en datos financieros
- [ ] Exportación de watchlist (CSV, JSON)
- [ ] Compartir watchlists entre usuarios
- [ ] Gráficos de evolución de precios
- [ ] Notificaciones push/email

## Soporte

Para preguntas o problemas, consulta la documentación completa en Swagger o contacta al equipo de desarrollo.
