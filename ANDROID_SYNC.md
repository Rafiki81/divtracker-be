# DivTracker Android - Sincronización con Backend

Este documento describe todos los campos expuestos por el backend de DivTracker para sincronizar la app Android.

## 📋 Resumen de Cambios Recientes

Se han añadido nuevas métricas financieras enfocadas en inversión por dividendos:

### Nuevos Campos de Mercado
- `dailyChangePercent` - Variación diaria %
- `marketCapitalization` - Capitalización de mercado
- `weekHigh52` / `weekLow52` - Rango 52 semanas
- `weekRange52Position` - Posición en el rango (0-1)

### Nuevas Métricas de Dividendos
- `dividendGrowthRate5Y` - Crecimiento del dividendo a 5 años
- `dividendCoverageRatio` - Cobertura del dividendo (FCF/Dividend)
- `payoutRatioFcf` - Payout Ratio basado en FCF
- `chowderRuleValue` - Regla de Chowder (Yield + Growth)

---

## 🔄 WatchlistItemResponse - Modelo Completo

### Kotlin Data Class

```kotlin
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class WatchlistItemResponse(
    // === IDENTIFICADORES ===
    val id: UUID,
    val userId: UUID,
    
    // === DATOS BÁSICOS ===
    val ticker: String,
    val exchange: String?,
    val notes: String?,
    
    // === PRECIO Y OBJETIVOS ===
    val currentPrice: BigDecimal?,           // Precio actual de mercado
    val targetPrice: BigDecimal?,            // Precio objetivo manual
    val targetPfcf: BigDecimal?,             // P/FCF objetivo
    
    // === DATOS DE MERCADO (NUEVOS) ===
    val dailyChangePercent: BigDecimal?,     // Variación diaria (%), ej: 1.25 = +1.25%
    val marketCapitalization: BigDecimal?,   // Market cap en USD (valor completo, no millones)
    val weekHigh52: BigDecimal?,             // Máximo 52 semanas
    val weekLow52: BigDecimal?,              // Mínimo 52 semanas
    val weekRange52Position: BigDecimal?,    // Posición 0-1 (0=mínimo, 1=máximo)
    
    // === MÉTRICAS DE FCF ===
    val freeCashFlowPerShare: BigDecimal?,   // FCF por acción
    val actualPfcf: BigDecimal?,             // P/FCF actual calculado
    val fcfYield: BigDecimal?,               // FCF Yield (%)
    val focfCagr5Y: BigDecimal?,             // CAGR del FCF operativo 5 años (%)
    
    // === MÉTRICAS DE DIVIDENDOS ===
    val dividendYield: BigDecimal?,          // Yield actual (%), ej: 3.50 = 3.5%
    val dividendGrowthRate5Y: BigDecimal?,   // Crecimiento 5Y (%), ej: 8.50 = 8.5%
    val dividendCoverageRatio: BigDecimal?,  // Cobertura = FCF/Dividend, >1.5 es saludable
    val payoutRatioFcf: BigDecimal?,         // Payout como ratio (0.45 = 45%)
    val chowderRuleValue: BigDecimal?,       // Yield% + DGR5Y%, ≥12 es bueno
    
    // === OTRAS MÉTRICAS ===
    val beta: BigDecimal?,                   // Volatilidad vs mercado
    val peAnnual: BigDecimal?,               // PER anual
    
    // === VALORACIÓN DCF ===
    val dcfFairValue: BigDecimal?,           // Valor intrínseco por DCF
    val fairPriceByPfcf: BigDecimal?,        // Precio justo por P/FCF objetivo
    val marginOfSafety: BigDecimal?,         // Margen de seguridad vs DCF (%)
    val discountToFairPrice: BigDecimal?,    // Descuento vs precio justo
    val deviationFromTargetPrice: BigDecimal?, // Desviación vs target manual
    val undervalued: Boolean?,               // Precio < DCF (Golden Rule)
    
    // === PARÁMETROS DE INVERSIÓN ===
    val estimatedFcfGrowthRate: BigDecimal?, // Tasa crecimiento FCF (decimal: 0.08 = 8%)
    val investmentHorizonYears: Int?,        // Horizonte en años
    val discountRate: BigDecimal?,           // WACC/Tasa descuento (decimal: 0.10 = 10%)
    
    // === MÉTRICAS CALCULADAS ===
    val estimatedIRR: BigDecimal?,           // TIR estimada (%)
    val estimatedROI: BigDecimal?,           // ROI al horizonte (%)
    val paybackPeriod: BigDecimal?,          // Años para recuperar inversión
    
    // === NOTIFICACIONES ===
    val notifyWhenBelowPrice: Boolean?,
    
    // === TIMESTAMPS ===
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
)
```

---

## 📊 Interpretación de Métricas

### Métricas de Dividendos

| Métrica | Descripción | Interpretación |
|---------|-------------|----------------|
| `dividendYield` | Rentabilidad anual por dividendo | > 3% es interesante para income |
| `dividendGrowthRate5Y` | Crecimiento histórico 5 años | > 7% indica buen crecimiento |
| `dividendCoverageRatio` | FCF / Dividendo | > 1.5 = sostenible, < 1.0 = en riesgo |
| `payoutRatioFcf` | % del FCF pagado como dividendo | < 0.70 (70%) deja margen para crecer |
| `chowderRuleValue` | Yield + Growth Rate 5Y | ≥ 12 = buena oportunidad |

### Métricas de Valoración

| Métrica | Descripción | Interpretación |
|---------|-------------|----------------|
| `weekRange52Position` | Posición en rango anual | < 0.3 = cerca de mínimos (potencial) |
| `marginOfSafety` | Descuento vs DCF | > 20% = buen margen |
| `undervalued` | Precio < DCF | `true` = posible oportunidad |
| `fcfYield` | FCF / Precio | > 5% es atractivo |

### Métricas de Riesgo

| Métrica | Descripción | Interpretación |
|---------|-------------|----------------|
| `beta` | Volatilidad vs S&P 500 | < 1 = menos volátil que mercado |
| `paybackPeriod` | Años para recuperar inversión | < 10 años es razonable |

---

## 🎨 Sugerencias de UI para Android

### Card de Watchlist Item

```
┌─────────────────────────────────────────────────┐
│  AAPL                           ▲ +1.25%        │  <- ticker + dailyChangePercent
│  Apple Inc.                                     │
│  $172.15                        52W: ████░░ 65% │  <- currentPrice + weekRange52Position
├─────────────────────────────────────────────────┤
│  Dividend Metrics                               │
│  ┌─────────┬─────────┬─────────┬─────────┐     │
│  │ Yield   │ Growth  │Coverage │ Chowder │     │
│  │  3.5%   │  8.5%   │  2.15x  │  12.0   │     │
│  │         │         │   ✓     │   ✓     │     │
│  └─────────┴─────────┴─────────┴─────────┘     │
├─────────────────────────────────────────────────┤
│  Valuation                                      │
│  DCF: $195.50  │  Margin: +13.5%  │ UNDERVALUED│
└─────────────────────────────────────────────────┘
```

### Colores Sugeridos

```kotlin
// Para dailyChangePercent
fun getDailyChangeColor(percent: BigDecimal?): Color {
    return when {
        percent == null -> Color.Gray
        percent > BigDecimal.ZERO -> Color.Green
        percent < BigDecimal.ZERO -> Color.Red
        else -> Color.Gray
    }
}

// Para dividendCoverageRatio
fun getCoverageColor(ratio: BigDecimal?): Color {
    return when {
        ratio == null -> Color.Gray
        ratio >= BigDecimal("1.5") -> Color.Green      // Saludable
        ratio >= BigDecimal("1.0") -> Color.Yellow     // Ajustado
        else -> Color.Red                               // En riesgo
    }
}

// Para chowderRuleValue
fun getChowderColor(value: BigDecimal?): Color {
    return when {
        value == null -> Color.Gray
        value >= BigDecimal("12") -> Color.Green       // Buena oportunidad
        value >= BigDecimal("8") -> Color.Yellow       // Aceptable
        else -> Color.Red                               // No cumple
    }
}

// Para weekRange52Position
fun get52WeekPositionColor(position: BigDecimal?): Color {
    return when {
        position == null -> Color.Gray
        position <= BigDecimal("0.3") -> Color.Green   // Cerca de mínimos
        position >= BigDecimal("0.8") -> Color.Red     // Cerca de máximos
        else -> Color.Yellow
    }
}
```

---

## 🔢 Formateo de Valores

```kotlin
object ValueFormatter {
    
    // Market Cap: $2.85T, $285B, $28.5M
    fun formatMarketCap(value: BigDecimal?): String {
        if (value == null) return "N/A"
        return when {
            value >= BigDecimal("1000000000000") -> 
                "$${(value / BigDecimal("1000000000000")).setScale(2, RoundingMode.HALF_UP)}T"
            value >= BigDecimal("1000000000") -> 
                "$${(value / BigDecimal("1000000000")).setScale(2, RoundingMode.HALF_UP)}B"
            value >= BigDecimal("1000000") -> 
                "$${(value / BigDecimal("1000000")).setScale(2, RoundingMode.HALF_UP)}M"
            else -> "$${value.setScale(0, RoundingMode.HALF_UP)}"
        }
    }
    
    // Porcentajes: +1.25%, -0.50%
    fun formatPercent(value: BigDecimal?, showSign: Boolean = true): String {
        if (value == null) return "N/A"
        val sign = if (showSign && value > BigDecimal.ZERO) "+" else ""
        return "$sign${value.setScale(2, RoundingMode.HALF_UP)}%"
    }
    
    // Ratio: 2.15x
    fun formatRatio(value: BigDecimal?): String {
        if (value == null) return "N/A"
        return "${value.setScale(2, RoundingMode.HALF_UP)}x"
    }
    
    // Precio: $172.15
    fun formatPrice(value: BigDecimal?): String {
        if (value == null) return "N/A"
        return "$${value.setScale(2, RoundingMode.HALF_UP)}"
    }
    
    // Payout como porcentaje: 45% (desde 0.45)
    fun formatPayoutRatio(value: BigDecimal?): String {
        if (value == null) return "N/A"
        val percent = value.multiply(BigDecimal("100"))
        return "${percent.setScale(0, RoundingMode.HALF_UP)}%"
    }
    
    // 52-week position como barra visual
    fun format52WeekBar(position: BigDecimal?): String {
        if (position == null) return "░░░░░░░░░░"
        val filled = (position.toDouble() * 10).toInt().coerceIn(0, 10)
        return "█".repeat(filled) + "░".repeat(10 - filled)
    }
}
```

---

## 📱 Ejemplo JSON de Respuesta

```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "userId": "123e4567-e89b-12d3-a456-426614174001",
  "ticker": "AAPL",
  "exchange": "NASDAQ",
  "currentPrice": 172.15,
  "targetPrice": 180.00,
  "targetPfcf": 15.0,
  
  "dailyChangePercent": 1.25,
  "marketCapitalization": 2850000000000,
  "weekHigh52": 199.62,
  "weekLow52": 124.17,
  "weekRange52Position": 0.65,
  
  "freeCashFlowPerShare": 11.45,
  "actualPfcf": 15.03,
  "fcfYield": 6.65,
  "focfCagr5Y": 12.50,
  
  "dividendYield": 3.50,
  "dividendGrowthRate5Y": 8.50,
  "dividendCoverageRatio": 2.15,
  "payoutRatioFcf": 0.45,
  "chowderRuleValue": 12.00,
  
  "beta": 1.28,
  "peAnnual": 25.4,
  
  "dcfFairValue": 195.50,
  "fairPriceByPfcf": 171.75,
  "marginOfSafety": 13.56,
  "discountToFairPrice": 0.048,
  "deviationFromTargetPrice": -0.044,
  "undervalued": true,
  
  "estimatedFcfGrowthRate": 0.08,
  "investmentHorizonYears": 5,
  "discountRate": 0.10,
  
  "estimatedIRR": 12.50,
  "estimatedROI": 85.50,
  "paybackPeriod": 7.2,
  
  "notifyWhenBelowPrice": false,
  "notes": "Apple - Strong fundamentals",
  
  "createdAt": "2024-11-22T10:30:00",
  "updatedAt": "2024-11-22T15:45:00"
}
```

---

## ✅ Checklist de Implementación Android

### Modelo de Datos
- [ ] Actualizar `WatchlistItemResponse` data class con todos los campos
- [ ] Añadir campos nullable para compatibilidad

### Repository/API
- [ ] Verificar que Retrofit deserializa todos los campos
- [ ] Manejar campos null gracefully

### UI Components
- [ ] Crear componente `DividendMetricsCard`
- [ ] Crear componente `WeekRangeIndicator` (barra 52 semanas)
- [ ] Crear componente `ChowderBadge`
- [ ] Actualizar `WatchlistItemCard` con nuevos datos

### Formatters
- [ ] Implementar `ValueFormatter` object
- [ ] Implementar funciones de color según valores

### Pantallas
- [ ] Actualizar lista de watchlist con indicadores visuales
- [ ] Actualizar detalle de item con todas las métricas
- [ ] Añadir sección de "Dividend Analysis"

---

## 📝 Notas Importantes

1. **Todos los campos nuevos son nullable** - El backend puede no tener datos para todas las acciones
2. **`payoutRatioFcf` es ratio, no porcentaje** - Multiplicar por 100 para mostrar como %
3. **`marketCapitalization` es valor completo en USD** - No está en millones
4. **`weekRange52Position`** se calcula: `(precio - min) / (max - min)`
5. **`undervalued`** usa la "Golden Rule": `precio < DCF`
