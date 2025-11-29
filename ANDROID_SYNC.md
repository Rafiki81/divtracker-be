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

### 🔥 Firebase Cloud Messaging (FCM)
- Push notifications en tiempo real
- Actualizaciones de precios silenciosas (data-only)
- Alertas de precio objetivo alcanzado
- Alertas de margen de seguridad
- Resumen diario de watchlist

---

## 🔔 Firebase Cloud Messaging - Push Notifications

### Arquitectura

```
┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
│  Finnhub API    │──────▶│  Backend        │──────▶│  Firebase FCM   │
│  (Webhooks)     │       │  (Spring Boot)  │       │  (Google)       │
└─────────────────┘       └────────┬────────┘       └────────┬────────┘
                                   │                         │
                                   │ REST API                │ Push
                                   ▼                         ▼
                          ┌─────────────────┐       ┌─────────────────┐
                          │  PostgreSQL     │       │  Android App    │
                          │  (FCM Tokens)   │       │  (Cliente)      │
                          └─────────────────┘       └─────────────────┘
```

### Tipos de Notificaciones

| Tipo | Descripción | Comportamiento |
|------|-------------|----------------|
| `PRICE_UPDATE` | Actualización de precio | **Silenciosa** (data-only) - Actualiza UI sin mostrar notificación |
| `PRICE_ALERT` | Precio objetivo alcanzado | **Visible** - Muestra notificación al usuario |
| `MARGIN_ALERT` | Margen de seguridad alcanzado | **Visible** - Muestra notificación al usuario |
| `DAILY_SUMMARY` | Resumen diario de watchlist | **Visible** - Una vez al día |

---

## 📱 Configuración Firebase en Android

### 1. Dependencias (build.gradle.kts)

```kotlin
plugins {
    id("com.google.gms.google-services")
}

dependencies {
    // Firebase BOM
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
}
```

### 2. google-services.json

Descarga el archivo `google-services.json` desde Firebase Console y colócalo en `app/`.

### 3. FirebaseMessagingService

```kotlin
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DivTrackerMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Enviar token al backend
        scope.launch {
            registerTokenWithBackend(token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        val data = remoteMessage.data
        val type = data["type"] ?: return
        
        when (type) {
            "PRICE_UPDATE" -> handlePriceUpdate(data)
            "PRICE_ALERT" -> handlePriceAlert(data)
            "MARGIN_ALERT" -> handleMarginAlert(data)
            "DAILY_SUMMARY" -> handleDailySummary(data)
        }
    }

    private fun handlePriceUpdate(data: Map<String, String>) {
        // Actualización silenciosa - solo actualizar datos locales
        val ticker = data["ticker"] ?: return
        val price = data["price"]?.toBigDecimalOrNull() ?: return
        val dailyChange = data["dailyChangePercent"]?.toBigDecimalOrNull()
        
        // Actualizar Room database o StateFlow
        scope.launch {
            watchlistRepository.updatePrice(ticker, price, dailyChange)
        }
    }

    private fun handlePriceAlert(data: Map<String, String>) {
        val ticker = data["ticker"] ?: return
        val price = data["price"] ?: return
        val targetPrice = data["targetPrice"] ?: return
        val title = data["title"] ?: "Precio Objetivo Alcanzado"
        val body = data["body"] ?: "$ticker ha alcanzado \$$price"
        
        showNotification(title, body, NotificationChannel.PRICE_ALERTS)
    }

    private fun handleMarginAlert(data: Map<String, String>) {
        val ticker = data["ticker"] ?: return
        val margin = data["marginOfSafety"] ?: return
        val title = data["title"] ?: "Margen de Seguridad"
        val body = data["body"] ?: "$ticker tiene un margen de seguridad de $margin%"
        
        showNotification(title, body, NotificationChannel.MARGIN_ALERTS)
    }

    private fun handleDailySummary(data: Map<String, String>) {
        val title = data["title"] ?: "Resumen Diario"
        val body = data["body"] ?: "Tu watchlist ha sido actualizada"
        val itemCount = data["itemCount"]?.toIntOrNull() ?: 0
        val undervaluedCount = data["undervaluedCount"]?.toIntOrNull() ?: 0
        
        showNotification(title, body, NotificationChannel.DAILY_SUMMARY)
    }

    private suspend fun registerTokenWithBackend(token: String) {
        try {
            val deviceId = getDeviceId()
            val request = DeviceRegistrationRequest(
                fcmToken = token,
                deviceId = deviceId,
                platform = "ANDROID",
                deviceName = android.os.Build.MODEL
            )
            apiService.registerDevice(request)
        } catch (e: Exception) {
            // Reintentar más tarde
        }
    }

    private fun showNotification(title: String, body: String, channel: NotificationChannel) {
        // Implementar con NotificationCompat.Builder
    }
}
```

---

## 🔌 API de Registro de Dispositivos

### Endpoints

#### Registrar dispositivo

```http
POST /api/v1/devices/register
Authorization: Bearer {token}
Content-Type: application/json

{
  "fcmToken": "fK1234567890abcdef...",
  "deviceId": "unique-device-id-123",
  "platform": "ANDROID",
  "deviceName": "Pixel 8 Pro"
}
```

**Response:**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "deviceId": "unique-device-id-123",
  "platform": "ANDROID",
  "deviceName": "Pixel 8 Pro",
  "isActive": true,
  "createdAt": "2024-11-29T10:30:00",
  "lastUsedAt": "2024-11-29T10:30:00"
}
```

#### Listar dispositivos

```http
GET /api/v1/devices
Authorization: Bearer {token}
```

**Response:**
```json
[
  {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "deviceId": "unique-device-id-123",
    "platform": "ANDROID",
    "deviceName": "Pixel 8 Pro",
    "isActive": true,
    "createdAt": "2024-11-29T10:30:00",
    "lastUsedAt": "2024-11-29T15:45:00"
  }
]
```

#### Eliminar dispositivo

```http
DELETE /api/v1/devices/{deviceId}
Authorization: Bearer {token}
```

---

## 📦 DTOs de Kotlin

### DeviceRegistrationRequest

```kotlin
data class DeviceRegistrationRequest(
    val fcmToken: String,
    val deviceId: String,
    val platform: String = "ANDROID",  // ANDROID, IOS, WEB
    val deviceName: String? = null
)
```

### DeviceResponse

```kotlin
data class DeviceResponse(
    val id: UUID,
    val deviceId: String,
    val platform: String,
    val deviceName: String?,
    val isActive: Boolean,
    val createdAt: LocalDateTime,
    val lastUsedAt: LocalDateTime?
)
```

### PushNotificationPayload (Data Message)

```kotlin
// Estructura del data payload que envía el backend
data class PushNotificationPayload(
    val type: String,           // PRICE_UPDATE, PRICE_ALERT, MARGIN_ALERT, DAILY_SUMMARY
    val ticker: String?,        // Símbolo del ticker
    val title: String?,         // Título de la notificación
    val body: String?,          // Cuerpo de la notificación
    
    // Para PRICE_UPDATE y PRICE_ALERT
    val price: String?,         // Precio actual como String
    val targetPrice: String?,   // Precio objetivo
    val dailyChangePercent: String?,
    
    // Para MARGIN_ALERT
    val marginOfSafety: String?,
    val dcfFairValue: String?,
    
    // Para DAILY_SUMMARY
    val itemCount: String?,
    val undervaluedCount: String?,
    val totalGainers: String?,
    val totalLosers: String?,
    
    // Metadata
    val timestamp: String?      // ISO 8601
)
```

---

## 🔧 Implementación Completa Android

### 1. Repository para gestión de tokens

```kotlin
class FcmTokenRepository(
    private val apiService: DivTrackerApiService,
    private val preferences: SharedPreferences
) {
    private val KEY_FCM_TOKEN = "fcm_token"
    private val KEY_DEVICE_ID = "device_id"

    suspend fun registerToken(token: String): Result<DeviceResponse> {
        return try {
            val deviceId = getOrCreateDeviceId()
            val request = DeviceRegistrationRequest(
                fcmToken = token,
                deviceId = deviceId,
                platform = "ANDROID",
                deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
            )
            val response = apiService.registerDevice(request)
            preferences.edit().putString(KEY_FCM_TOKEN, token).apply()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unregisterCurrentDevice(): Result<Unit> {
        return try {
            val deviceId = preferences.getString(KEY_DEVICE_ID, null)
                ?: return Result.failure(Exception("No device registered"))
            apiService.unregisterDevice(deviceId)
            preferences.edit().remove(KEY_FCM_TOKEN).apply()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getOrCreateDeviceId(): String {
        return preferences.getString(KEY_DEVICE_ID, null) ?: run {
            val newId = UUID.randomUUID().toString()
            preferences.edit().putString(KEY_DEVICE_ID, newId).apply()
            newId
        }
    }
}
```

### 2. Notification Channels

```kotlin
object NotificationChannels {
    const val PRICE_ALERTS = "price_alerts"
    const val MARGIN_ALERTS = "margin_alerts"
    const val DAILY_SUMMARY = "daily_summary"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)

            // Price Alerts - Alta importancia
            val priceChannel = NotificationChannel(
                PRICE_ALERTS,
                "Alertas de Precio",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones cuando un ticker alcanza tu precio objetivo"
                enableVibration(true)
            }

            // Margin Alerts - Alta importancia
            val marginChannel = NotificationChannel(
                MARGIN_ALERTS,
                "Alertas de Margen de Seguridad",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones cuando un ticker tiene buen margen de seguridad"
                enableVibration(true)
            }

            // Daily Summary - Baja importancia
            val summaryChannel = NotificationChannel(
                DAILY_SUMMARY,
                "Resumen Diario",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Resumen diario de tu watchlist"
            }

            notificationManager.createNotificationChannels(
                listOf(priceChannel, marginChannel, summaryChannel)
            )
        }
    }
}
```

### 3. Retrofit API Service

```kotlin
interface DivTrackerApiService {
    
    @POST("api/v1/devices/register")
    suspend fun registerDevice(
        @Body request: DeviceRegistrationRequest
    ): DeviceResponse

    @GET("api/v1/devices")
    suspend fun getDevices(): List<DeviceResponse>

    @DELETE("api/v1/devices/{deviceId}")
    suspend fun unregisterDevice(
        @Path("deviceId") deviceId: String
    )
}
```

### 4. Manejo de actualizaciones silenciosas

```kotlin
class WatchlistPriceUpdater(
    private val watchlistDao: WatchlistDao,
    private val _priceUpdates: MutableSharedFlow<PriceUpdate>
) {
    
    data class PriceUpdate(
        val ticker: String,
        val price: BigDecimal,
        val dailyChangePercent: BigDecimal?,
        val timestamp: LocalDateTime
    )

    suspend fun handlePriceUpdate(data: Map<String, String>) {
        val ticker = data["ticker"] ?: return
        val price = data["price"]?.toBigDecimalOrNull() ?: return
        val dailyChange = data["dailyChangePercent"]?.toBigDecimalOrNull()
        val timestamp = data["timestamp"]?.let { 
            LocalDateTime.parse(it) 
        } ?: LocalDateTime.now()

        // Actualizar base de datos local
        watchlistDao.updatePrice(ticker, price, dailyChange, timestamp)

        // Emitir para actualizar UI en tiempo real
        _priceUpdates.emit(PriceUpdate(ticker, price, dailyChange, timestamp))
    }
}
```

---

## 🎨 UI: Indicador de conexión en tiempo real

```kotlin
@Composable
fun RealtimeConnectionIndicator(
    isConnected: Boolean,
    lastUpdate: LocalDateTime?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(8.dp)
    ) {
        // Punto de estado
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = if (isConnected) Color.Green else Color.Red,
                    shape = CircleShape
                )
        )
        
        Spacer(modifier = Modifier.width(4.dp))
        
        // Texto de estado
        Text(
            text = if (isConnected) {
                lastUpdate?.let { 
                    "Actualizado ${formatRelativeTime(it)}" 
                } ?: "Conectado"
            } else {
                "Sin conexión"
            },
            style = MaterialTheme.typography.caption,
            color = if (isConnected) Color.Green else Color.Red
        )
    }
}

fun formatRelativeTime(dateTime: LocalDateTime): String {
    val now = LocalDateTime.now()
    val seconds = ChronoUnit.SECONDS.between(dateTime, now)
    
    return when {
        seconds < 60 -> "hace ${seconds}s"
        seconds < 3600 -> "hace ${seconds / 60}m"
        else -> "hace ${seconds / 3600}h"
    }
}
```

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

### 🔥 Firebase Cloud Messaging
- [ ] Añadir dependencias de Firebase en `build.gradle.kts`
- [ ] Configurar `google-services.json` desde Firebase Console
- [ ] Crear `DivTrackerMessagingService` extendiendo `FirebaseMessagingService`
- [ ] Registrar el servicio en `AndroidManifest.xml`
- [ ] Crear `NotificationChannels` (price_alerts, margin_alerts, daily_summary)
- [ ] Implementar `FcmTokenRepository` para gestión de tokens
- [ ] Añadir endpoints de dispositivos a Retrofit API Service
- [ ] Implementar `WatchlistPriceUpdater` para actualizaciones silenciosas
- [ ] Crear UI `RealtimeConnectionIndicator` para mostrar estado de conexión
- [ ] Manejar permisos de notificaciones (Android 13+)
- [ ] Implementar lógica de reintento para registro de tokens

---

## 📝 Notas Importantes

### Datos y Formateo
1. **Todos los campos nuevos son nullable** - El backend puede no tener datos para todas las acciones
2. **`payoutRatioFcf` es ratio, no porcentaje** - Multiplicar por 100 para mostrar como %
3. **`marketCapitalization` es valor completo en USD** - No está en millones
4. **`weekRange52Position`** se calcula: `(precio - min) / (max - min)`
5. **`undervalued`** usa la "Golden Rule": `precio < DCF`

### Firebase Cloud Messaging
6. **`PRICE_UPDATE` es data-only** - No muestra notificación visible, solo actualiza datos
7. **Tokens FCM pueden cambiar** - Siempre manejar `onNewToken()` y re-registrar
8. **deviceId debe ser único y persistente** - Usar UUID guardado en SharedPreferences
9. **Notificaciones requieren permiso en Android 13+** - Solicitar `POST_NOTIFICATIONS`
10. **Las alertas solo se envían para tickers en watchlist** - El backend filtra por usuario

### Canales de Notificación (Android 8+)
- `price_alerts` - **IMPORTANCE_HIGH** - Alertas de precio objetivo
- `margin_alerts` - **IMPORTANCE_HIGH** - Alertas de margen de seguridad  
- `daily_summary` - **IMPORTANCE_LOW** - Resumen diario (sin vibración)

### Flujo de Registro de Token
```
App Start → Get FCM Token → POST /api/v1/devices/register → Store locally
         ↓
onNewToken() → POST /api/v1/devices/register (update existing)
         ↓
Logout → DELETE /api/v1/devices/{deviceId}
```
