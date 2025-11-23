# Configuración de Finnhub API

## 🎯 Objetivo

Habilitar la API de Finnhub para obtener datos financieros en tiempo real (precio actual, FCF por acción) para los tickers del watchlist.

---

## 📋 Pasos para Configurar

### 1. Obtener API Key de Finnhub

1. Ir a [Finnhub.io](https://finnhub.io/)
2. Registrarte con tu email
3. Ir a **Dashboard** → **API Keys**
4. Copiar tu **API Key** (formato: `xxxxxxxxxxxxxxxxxx`)

**Nota**: El plan gratuito incluye 60 llamadas/minuto, suficiente para desarrollo.

---

### 2. Configurar en AWS Elastic Beanstalk

#### Opción A: Desde la consola AWS

1. Ir a **Elastic Beanstalk** → **Environments**
2. Seleccionar `divtracker-prod`
3. Ir a **Configuration** → **Software**
4. Click en **Edit**
5. En **Environment properties**, añadir:
   ```
   Name: FINNHUB_API_KEY
   Value: <tu-api-key>
   ```
6. **Save** y esperar a que el entorno se actualice

#### Opción B: Desde CLI (si tienes eb instalado)

```bash
cd /Users/rafaelperezbeato/IdeaProjects/divtracker-be

# Configurar variable de entorno
eb setenv FINNHUB_API_KEY=<tu-api-key>

# Verificar
eb printenv | grep FINNHUB
```

---

### 3. Verificar Configuración

Una vez configurada la API key, el backend automáticamente:

✅ **Cargará datos al crear items**: Si creas un item solo con el ticker (sin `targetPrice` ni `targetPfcf`), el backend:
   - Obtendrá el precio actual de Finnhub
   - Obtendrá el FCF por acción de Finnhub
   - Calculará el P/FCF actual
   - Lo usará como `targetPfcf` inicial

✅ **Enriquecerá responses**: Todas las respuestas incluirán:
   - `currentPrice`: Precio actual de mercado
   - `freeCashFlowPerShare`: FCF por acción
   - `actualPfcf`: P/FCF actual calculado
   - `fairPriceByPfcf`: Precio justo según tu P/FCF objetivo
   - `undervalued`: Si está infravalorada o no
   - Y todas las métricas avanzadas (DCF, TIR, ROI, etc.)

---

## 🧪 Probar desde Android

### Crear item solo con ticker (modo automático)

```json
POST /api/v1/watchlist
Authorization: Bearer <token>

{
  "ticker": "AAPL"
}
```

**Respuesta esperada**:
```json
{
  "id": "...",
  "ticker": "AAPL",
  "targetPfcf": 25.5,  // ← Calculado automáticamente
  "currentPrice": 172.15,  // ← Desde Finnhub
  "freeCashFlowPerShare": 6.75,  // ← Desde Finnhub
  "actualPfcf": 25.5,
  "undervalued": false,
  ...
}
```

### Crear item con valores manuales (modo manual)

```json
POST /api/v1/watchlist
Authorization: Bearer <token>

{
  "ticker": "AAPL",
  "targetPrice": 150.00,
  "targetPfcf": 20.0,
  "notes": "Target valuation"
}
```

---

## 🔍 Troubleshooting

### Error: "No se pudieron obtener datos de mercado"

**Causa**: Finnhub no tiene datos para ese ticker o la API key no está configurada.

**Solución**: 
1. Verificar que `FINNHUB_API_KEY` está configurada en AWS
2. Verificar que el ticker es válido (usar tickers de Yahoo Finance / Finnhub)
3. Proporcionar manualmente `targetPrice` o `targetPfcf`

### Error: "Debe especificar al menos targetPrice o targetPfcf"

**Causa**: Finnhub está deshabilitado y no se proporcionaron valores manuales.

**Solución**: Configurar `FINNHUB_API_KEY` O proporcionar valores manualmente.

### Los datos financieros aparecen como null

**Causa**: 
- Finnhub API key no configurada
- Ticker no encontrado en Finnhub
- Límite de llamadas excedido (60/min en plan free)

**Solución**:
1. Verificar logs: `eb logs -a` → buscar "Finnhub"
2. Verificar API key: `eb printenv | grep FINNHUB`
3. Probar ticker en [Finnhub Symbol Lookup](https://finnhub.io/docs/api/symbol-search)

---

## 📊 Límites del Plan Gratuito

- **60 llamadas/minuto**
- **30 llamadas/segundo** (ráfagas)
- Datos de mercado en tiempo real
- Datos financieros históricos
- Sin tarjeta de crédito requerida

**Suficiente para**:
- Desarrollo y testing
- Hasta ~100 usuarios activos simultáneos
- ~3000 cargas de watchlist/hora

---

## 🚀 Despliegue

Después de configurar `FINNHUB_API_KEY`:

```bash
cd /Users/rafaelperezbeato/IdeaProjects/divtracker-be

# Construir
./mvnw clean package -DskipTests

# Desplegar a AWS
eb deploy

# Verificar logs
eb logs -a | grep -i finnhub
```

**Logs esperados**:
```
INFO - MarketDataEnrichmentService: Fetching market data for AAPL
INFO - FinnhubClient: Successfully fetched quote for AAPL: 172.15
INFO - FinnhubClient: Successfully fetched FCF for AAPL: 6.75
```

---

## 📱 Actualizar Guía Android

Agregar a `ANDROID_WATCHLIST_GUIDE.md`:

### Crear Item - Solo con Ticker (Simple)

```kotlin
val simpleRequest = WatchlistItemRequest(
    ticker = "AAPL"
    // Sin targetPrice ni targetPfcf
    // El backend los calculará automáticamente
)

viewModel.createItem(simpleRequest)
```

### Crear Item - Con Valores Manuales (Completo)

```kotlin
val completeRequest = WatchlistItemRequest(
    ticker = "MSFT",
    targetPrice = BigDecimal("350.00"),
    targetPfcf = BigDecimal("20.0"),
    notes = "Microsoft - Strong fundamentals",
    estimatedFcfGrowthRate = BigDecimal("0.08"),
    investmentHorizonYears = 5,
    discountRate = BigDecimal("0.10")
)

viewModel.createItem(completeRequest)
```

---

## ✅ Checklist Post-Configuración

- [ ] API key de Finnhub obtenida
- [ ] Variable `FINNHUB_API_KEY` configurada en AWS
- [ ] Entorno AWS actualizado (restart automático)
- [ ] Logs verificados (sin errores de Finnhub)
- [ ] Prueba desde Android: crear item solo con ticker
- [ ] Verificar que los datos financieros se muestran
- [ ] Documentar en Android que ahora es opcional especificar targets

---

## 🔗 Referencias

- [Finnhub API Docs](https://finnhub.io/docs/api)
- [Finnhub Dashboard](https://finnhub.io/dashboard)
- [AWS EB Environment Variables](https://docs.aws.amazon.com/elasticbeanstalk/latest/dg/environments-cfg-softwaresettings.html)
- Backend: `FinnhubClient.java`, `MarketDataEnrichmentService.java`

---

**Última actualización**: 23 noviembre 2025
