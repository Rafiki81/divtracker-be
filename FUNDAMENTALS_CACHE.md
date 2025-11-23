# Sistema de Cache de Fundamentales

## Descripción General

El sistema de cache de fundamentales permite trabajar sin conexión a Finnhub, almacenando snapshots de datos fundamentales de instrumentos financieros. Reduce las llamadas a la API y mejora el rendimiento.

## Arquitectura

### Componentes

1. **InstrumentFundamentals** (Entity)
   - Tabla: `instrument_fundamentals`
   - Almacena 30+ métricas financieras
   - Estados de calidad: COMPLETE, PARTIAL, STALE
   - Fuentes de datos: FINNHUB, MANUAL, CALCULATED

2. **InstrumentFundamentalsRepository** (Repository)
   - Consultas para detectar datos stale
   - Búsqueda por ticker, fecha, calidad

3. **InstrumentFundamentalsService** (Service)
   - Lógica de caching inteligente
   - Actualización automática de datos stale
   - Fallback a datos antiguos si Finnhub no disponible

4. **FundamentalsRefreshScheduler** (Scheduled Job)
   - Refresca datos stale cada 6 horas
   - Limpia datos muy antiguos (>30 días)
   - Respeta rate limits de Finnhub (60 calls/min)

5. **FundamentalsController** (REST API)
   - Endpoint para refresh manual
   - Requiere autenticación

## Flujo de Datos

### 1. Obtención de Fundamentales (getFundamentals)

```
┌─────────────────────────────────────────────────────────┐
│ MarketDataEnrichmentService.fetchMarketData(ticker)     │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│ InstrumentFundamentalsService.getFundamentals(ticker)   │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
         ┌───────────────────────┐
         │ ¿Existe en cache?     │
         └───────┬───────────────┘
                 │
        ┌────────┴─────────┐
        │                  │
       NO                 SÍ
        │                  │
        │          ┌───────▼────────┐
        │          │ ¿Está fresco?  │
        │          │ (<24h)         │
        │          └───────┬────────┘
        │                  │
        │         ┌────────┴────────┐
        │         │                 │
        │        SÍ                NO
        │         │                 │
        │         │         ┌───────▼───────────┐
        │         │         │ Fetch desde       │
        │         │         │ Finnhub           │
        │         │         └───────┬───────────┘
        │         │                 │
        │         │         ┌───────▼───────────┐
        │         │         │ ¿Éxito?           │
        │         │         └───────┬───────────┘
        │         │                 │
        │         │         ┌───────┴───────┐
        │         │         │               │
        │         │        SÍ              NO
        │         │         │               │
        │         │         │       ┌───────▼────────┐
        │         │         │       │ Usar stale     │
        │         │         │       │ (fallback)     │
        │         │         │       └────────────────┘
        │         │         │
        └─────────┴─────────┴──────────────────►
                            │
                            ▼
                  ┌──────────────────┐
                  │ Return Data      │
                  │ [price, fcf,     │
                  │  pe, beta]       │
                  └──────────────────┘
```

### 2. Actualización de Cache

```
┌────────────────────────────────────────────────────┐
│ FundamentalsRefreshScheduler (cada 6h)            │
└────────────────────┬───────────────────────────────┘
                     │
                     ▼
         ┌───────────────────────┐
         │ Buscar datos stale    │
         │ (>24h)                │
         └───────┬───────────────┘
                 │
                 ▼
         ┌───────────────────────┐
         │ Refrescar hasta 50    │
         │ tickers por batch     │
         └───────┬───────────────┘
                 │
                 ▼
    ┌────────────────────────────────┐
    │ Fetch desde Finnhub            │
    │ - Profile (company info)       │
    │ - Metrics (PE, beta, margins)  │
    │ - Quote (price)                │
    └────────────┬───────────────────┘
                 │
                 ▼
         ┌───────────────────────┐
         │ Actualizar cache      │
         │ + timestamp           │
         │ + data_quality        │
         └───────────────────────┘
```

## Datos Almacenados

### Información Básica
- `ticker` (PK)
- `company_name`
- `exchange`
- `currency`
- `sector`
- `industry`

### Precio
- `current_price`
- `price_change`
- `price_change_percent`
- `previous_close`
- `high_52_week`
- `low_52_week`

### Ratios de Valoración
- `pe_ttm` (Price-to-Earnings TTM)
- `price_to_book`
- `price_to_sales`

### Riesgo
- `beta` (volatilidad vs mercado)

### Free Cash Flow
- `fcf_per_share_ttm` (TTM)
- `fcf_per_share_annual` (Annual)
- `operating_cash_flow`
- `capital_expenditures`
- `free_cash_flow`

### Earnings y Revenue
- `eps_ttm` (Earnings Per Share TTM)
- `revenue_ttm`
- `net_income_ttm`

### Márgenes
- `operating_margin`
- `profit_margin`
- `roe` (Return on Equity)

### Dividendos
- `dividend_yield`
- `dividend_per_share`
- `payout_ratio`

### Metadatos
- `shares_outstanding`
- `market_cap` (calculado)
- `data_quality` (COMPLETE, PARTIAL, STALE)
- `source` (FINNHUB, MANUAL, CALCULATED)
- `last_updated_at`
- `created_at`

## Estrategia de Caching

### Freshness Window
- **Fresco**: < 24 horas
- **Stale**: > 24 horas
- **Muy antiguo**: > 30 días (candidato a limpieza)

### Data Quality States

1. **COMPLETE**
   - Tiene datos mínimos requeridos (price, FCF, PE, beta)
   - Menor de 24h

2. **PARTIAL**
   - Falta algún dato mínimo
   - Puede seguir siendo útil

3. **STALE**
   - Mayor de 24h
   - Marcado para refresh
   - Usado como fallback si Finnhub falla

### Fallback Strategy

```
1. Check cache
   ├─ Fresh data → Return immediately
   └─ Stale data → Try fetch from Finnhub
      ├─ Success → Update cache, return fresh data
      └─ Failure → Return stale data (better than nothing)
```

## API Endpoints

### Refresh Manual

```http
POST /api/v1/fundamentals/{ticker}/refresh
Authorization: Bearer <token>
```

**Respuesta:**
```json
{
  "ticker": "AAPL",
  "companyName": "Apple Inc.",
  "currentPrice": 173.50,
  "fcfPerShareTTM": 6.32,
  "fcfPerShareAnnual": 6.15,
  "peTTM": 27.45,
  "beta": 1.29,
  "dividendYield": 0.0052,
  "epsTTM": 6.32,
  "revenueTTM": 383285000000,
  "sharesOutstanding": 15552752000,
  "operatingMargin": 0.2987,
  "profitMargin": 0.2531,
  "exchange": "NASDAQ",
  "sector": "Technology",
  "dataQuality": "COMPLETE",
  "dataSource": "FINNHUB",
  "lastUpdatedAt": "2025-11-23T12:30:00"
}
```

## Scheduled Jobs

### 1. Refresh Stale Data (cada 6 horas)

```java
@Scheduled(cron = "0 0 */6 * * *")
public void refreshStaleFundamentals()
```

- Busca fundamentales con `last_updated_at > 24h`
- Refresca hasta 50 tickers por ejecución
- Rate limit: 1 call/segundo (60/min)
- Prioriza por antigüedad

### 2. Cleanup Old Data (diario a las 3 AM)

```java
@Scheduled(cron = "0 0 3 * * *")
public void cleanupOldFundamentals()
```

- Busca fundamentales con `last_updated_at > 30 days`
- Elimina solo registros con `data_quality = STALE`
- Preserva datos COMPLETE aunque sean antiguos

## Integración con WatchlistService

### Antes (sin cache)
```java
BigDecimal[] marketData = marketDataEnrichmentService.fetchMarketData(ticker);
BigDecimal price = marketData[0];
BigDecimal fcf = marketData[1];
```

### Ahora (con cache)
```java
BigDecimal[] marketData = marketDataEnrichmentService.fetchMarketData(ticker);
BigDecimal price = marketData[0];     // from cache
BigDecimal fcf = marketData[1];       // from cache
BigDecimal pe = marketData[2];        // NEW: available
BigDecimal beta = marketData[3];      // NEW: available
```

## Rate Limiting

### Finnhub API Limits
- **Free tier**: 60 calls/minute
- **Paid tier**: 300 calls/minute (default)

### Scheduler Strategy
- Max 50 refreshes por ejecución (cada 6h)
- 1 segundo entre llamadas
- Máximo 60 calls/min respetado

### Cálculos
```
50 tickers × 3 endpoints (profile, metrics, quote) = 150 calls
150 calls / 60 calls/min = 2.5 minutos por batch
```

## Beneficios

1. **Reducción de API calls**
   - 90% menos llamadas a Finnhub
   - Cache hit rate estimado: 85-95%

2. **Offline capability**
   - Funciona sin Finnhub si hay datos en cache
   - Fallback a datos stale (mejor que nada)

3. **Performance**
   - Respuesta instantánea desde DB
   - No espera a API externa

4. **Reliability**
   - No falla si Finnhub está caído
   - Datos históricos disponibles

5. **Cost savings**
   - Reduce uso de plan Finnhub
   - Puede downgrade a free tier

## Monitoreo

### Logs a revisar
```bash
# Refresh exitoso
INFO  - Successfully refreshed fundamentals for AAPL

# Refresh fallido (pero usando stale)
WARN  - Failed to refresh AAPL, using stale data from 2025-11-22

# Cache hit
DEBUG - Using cached fundamentals for TSLA (fresh: 12h ago)

# Cache miss
DEBUG - No cached fundamentals for NVDA, fetching from Finnhub
```

### Métricas recomendadas
- Cache hit rate
- Average data age
- API call count (before/after cache)
- Failed refresh count

## Migraciones

### V5: Crear tabla instrument_fundamentals

```sql
CREATE TABLE instrument_fundamentals (
    ticker VARCHAR(12) PRIMARY KEY,
    company_name VARCHAR(255),
    -- ... 30+ campos
    data_quality VARCHAR(20) CHECK (data_quality IN ('COMPLETE', 'PARTIAL', 'STALE')),
    source VARCHAR(20) CHECK (source IN ('FINNHUB', 'MANUAL', 'CALCULATED')),
    last_updated_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_last_updated ON instrument_fundamentals(last_updated_at);
CREATE INDEX idx_ticker_updated ON instrument_fundamentals(ticker, last_updated_at);
CREATE INDEX idx_data_quality ON instrument_fundamentals(data_quality);
```

## Testing

### Tests unitarios
- `InstrumentFundamentalsServiceTest`
  - Cache hit/miss scenarios
  - Stale detection
  - Fallback behavior
  - Data quality assessment

### Tests de integración
- `FundamentalsControllerIntegrationTest`
  - Refresh endpoint
  - Authentication
  - Response format

- `WatchlistServiceIntegrationTest`
  - Crear item con cache
  - Enriquecer con datos cached
  - Fallback a stale

## Troubleshooting

### Problema: Datos siempre stale
**Causa**: Scheduler no ejecutándose
**Solución**: Verificar `@EnableScheduling` en Application.java

### Problema: Rate limit exceeded
**Causa**: Demasiados refreshes simultáneos
**Solución**: Aumentar sleep entre llamadas en scheduler

### Problema: Cache nunca se actualiza
**Causa**: Threshold de 24h muy largo
**Solución**: Ajustar `FRESHNESS_THRESHOLD_HOURS` en service

## Configuración

### application.properties
```properties
# Finnhub (usado por cache)
finnhub.api-key=${FINNHUB_API_KEY}
finnhub.api-url=https://finnhub.io/api/v1
finnhub.stream-enabled=true

# Scheduling
spring.task.scheduling.pool.size=2

# Database (necesario para cache)
spring.datasource.url=jdbc:postgresql://localhost:5432/divtracker
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
```

## Roadmap Futuro

### Fase 1 (Actual)
- ✅ Cache básico de fundamentales
- ✅ Scheduled refresh
- ✅ Fallback a stale
- ✅ Manual refresh endpoint

### Fase 2 (Próximo)
- [ ] Histórico de fundamentales (time series)
- [ ] Trending/alertas de cambios significativos
- [ ] Dashboard de calidad de datos
- [ ] Refresh on-demand por usuario

### Fase 3 (Futuro)
- [ ] Machine learning para predicción de freshness
- [ ] Priorización inteligente de refreshes
- [ ] Agregación multi-source (Finnhub + Alpha Vantage)
- [ ] Export de datos históricos

## Referencias

- [Finnhub API Docs](https://finnhub.io/docs/api)
- [Spring Scheduling](https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#scheduling)
- [Flyway Migrations](https://flywaydb.org/documentation/)
