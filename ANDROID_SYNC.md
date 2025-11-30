# DivTracker Android - Sincronización con Backend

Este documento describe todos los campos expuestos por el backend de DivTracker para sincronizar la app Android.

> **Última actualización**: 30 de noviembre de 2025

## 📋 Estado del Backend

### ✅ Implementado y Listo
- API REST completa para watchlist con métricas de dividendos
- Firebase Cloud Messaging (FCM) para push notifications
- Autenticación JWT
- Endpoints de dispositivos para FCM

### 🔥 Firebase Cloud Messaging (FCM)
| Tipo | Estado Backend | Comportamiento |
|------|----------------|----------------|
| `PRICE_UPDATE` | ✅ Implementado | **Silenciosa** (data-only) - Solo datos, sin notificación visible |
| `PRICE_ALERT` | ✅ Implementado | **Visible** - Cuando precio alcanza `targetPrice` |
| `MARGIN_ALERT` | ✅ Implementado | **Visible** - Margen de seguridad alto |
| `DAILY_SUMMARY` | ✅ Implementado | **Visible** - Cron 22:00 CET días laborables |

### Campos Disponibles
- **Mercado**: `currentPrice`, `dailyChangePercent`, `marketCapitalization`, `weekHigh52`, `weekLow52`, `weekRange52Position`
- **Dividendos**: `dividendYield`, `dividendGrowthRate5Y`, `dividendCoverageRatio`, `payoutRatioFcf`, `chowderRuleValue`
- **FCF**: `freeCashFlowPerShare`, `actualPfcf`, `fcfYield`, `focfCagr5Y`
- **Valoración**: `dcfFairValue`, `fairPriceByPfcf`, `marginOfSafety`, `undervalued`

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

### Payloads de Notificaciones (lo que envía el Backend)

#### PRICE_UPDATE (Silenciosa - Data Only)
```json
{
  "data": {
    "type": "PRICE_UPDATE",
    "ticker": "AAPL",
    "price": "189.50",
    "changePercent": "2.35",
    "timestamp": "1701234567890"
  }
}
```
> ⚠️ **NO tiene campo `notification`** - Android debe actualizar datos locales sin mostrar notificación.

#### PRICE_ALERT (Visible)
```json
{
  "notification": {
    "title": "🎯 Alerta de Precio: AAPL",
    "body": "AAPL ha alcanzado tu precio objetivo ($150.00 → $148.50)"
  },
  "data": {
    "type": "PRICE_ALERT",
    "ticker": "AAPL",
    "currentPrice": "148.50",
    "targetPrice": "150.00",
    "timestamp": "1701234567890"
  }
}
```

#### MARGIN_ALERT (Visible)
```json
{
  "notification": {
    "title": "📈 Oportunidad: AAPL",
    "body": "AAPL tiene un margen de seguridad del 25.5% - ¡Posible oportunidad de compra!"
  },
  "data": {
    "type": "MARGIN_ALERT",
    "ticker": "AAPL",
    "marginOfSafety": "25.5",
    "currentPrice": "148.50",
    "timestamp": "1701234567890"
  }
}
```

#### DAILY_SUMMARY (Visible)
```json
{
  "notification": {
    "title": "📊 Resumen Diario - DivTracker",
    "body": "Tu watchlist (15 acciones): 📈 8 subiendo, 7 bajando"
  },
  "data": {
    "type": "DAILY_SUMMARY",
    "tickerCount": "15",
    "gainersCount": "8",
    "losersCount": "7",
    "timestamp": "1701234567890"
  }
}
```

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

### 3. AndroidManifest.xml

```xml
<service
    android:name=".fcm.DivTrackerMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

### 4. FirebaseMessagingService - Implementación Completa

```kotlin
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@AndroidEntryPoint
class DivTrackerMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var fcmTokenRepository: FcmTokenRepository
    
    @Inject
    lateinit var watchlistDao: WatchlistDao

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        serviceScope.launch {
            fcmTokenRepository.registerToken(token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        val data = remoteMessage.data
        val type = data["type"] ?: return
        
        when (type) {
            "PRICE_UPDATE" -> handlePriceUpdate(data)
            "PRICE_ALERT" -> handleVisibleNotification(data, remoteMessage.notification)
            "MARGIN_ALERT" -> handleVisibleNotification(data, remoteMessage.notification)
            "DAILY_SUMMARY" -> handleVisibleNotification(data, remoteMessage.notification)
        }
    }

    /**
     * PRICE_UPDATE: Actualización silenciosa - actualizar Room DB sin mostrar notificación
     */
    private fun handlePriceUpdate(data: Map<String, String>) {
        val ticker = data["ticker"] ?: return
        val price = data["price"]?.toBigDecimalOrNull() ?: return
        val changePercent = data["changePercent"]?.toBigDecimalOrNull()
        
        serviceScope.launch {
            // Actualizar precio en la base de datos local
            watchlistDao.updatePriceByTicker(
                ticker = ticker,
                currentPrice = price,
                dailyChangePercent = changePercent
            )
        }
    }

    /**
     * Notificaciones visibles - el sistema las muestra automáticamente si app está en background.
     * Solo necesitamos manejar si la app está en foreground.
     */
    private fun handleVisibleNotification(
        data: Map<String, String>, 
        notification: RemoteMessage.Notification?
    ) {
        val title = notification?.title ?: data["title"] ?: return
        val body = notification?.body ?: data["body"] ?: return
        val type = data["type"] ?: "GENERAL"
        
        val channelId = when (type) {
            "PRICE_ALERT" -> NotificationChannels.PRICE_ALERTS
            "MARGIN_ALERT" -> NotificationChannels.MARGIN_ALERTS
            "DAILY_SUMMARY" -> NotificationChannels.DAILY_SUMMARY
            else -> NotificationChannels.GENERAL
        }
        
        showNotification(title, body, channelId, data)
    }

    private fun showNotification(
        title: String, 
        body: String, 
        channelId: String,
        data: Map<String, String>
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            data["ticker"]?.let { putExtra("ticker", it) }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
```

---

## 🔌 API de Registro de Dispositivos

### Endpoints

| Método | Endpoint | Descripción | Auth |
|--------|----------|-------------|------|
| `POST` | `/api/v1/devices/register` | Registrar/actualizar dispositivo | Bearer |
| `GET` | `/api/v1/devices` | Listar dispositivos del usuario | Bearer |
| `DELETE` | `/api/v1/devices/{deviceId}` | Eliminar dispositivo (logout) | Bearer |

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

**Response (200 OK):**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "deviceId": "unique-device-id-123",
  "platform": "ANDROID",
  "deviceName": "Pixel 8 Pro",
  "isActive": true,
  "createdAt": "2024-11-29T10:30:00Z",
  "lastUsedAt": "2024-11-29T10:30:00Z"
}
```

#### Listar dispositivos

```http
GET /api/v1/devices
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
[
  {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "deviceId": "unique-device-id-123",
    "platform": "ANDROID",
    "deviceName": "Pixel 8 Pro",
    "isActive": true,
    "createdAt": "2024-11-29T10:30:00Z",
    "lastUsedAt": "2024-11-29T15:45:00Z"
  }
]
```

#### Eliminar dispositivo (para Logout)

```http
DELETE /api/v1/devices/{deviceId}
Authorization: Bearer {token}
```

**Response:** `204 No Content`

---

## 📦 DTOs de Kotlin

### DeviceRegistrationRequest

```kotlin
data class DeviceRegistrationRequest(
    val fcmToken: String,           // Required, max 500 chars
    val deviceId: String,           // Required, max 255 chars
    val platform: String = "ANDROID",  // ANDROID, IOS, WEB
    val deviceName: String? = null  // Optional, max 255 chars
)
```

### DeviceResponse

```kotlin
import java.time.Instant
import java.util.UUID

data class DeviceResponse(
    val id: UUID,
    val deviceId: String,
    val platform: String,
    val deviceName: String?,
    val isActive: Boolean,
    val createdAt: Instant,     // ISO 8601 format
    val lastUsedAt: Instant?    // ISO 8601 format
)
```

### PushNotificationPayload (Data Message)

```kotlin
/**
 * Estructura del payload `data` que envía el backend.
 * Nota: Las notificaciones visibles también incluyen `notification` con title/body.
 */
data class PushNotificationPayload(
    val type: String,               // PRICE_UPDATE, PRICE_ALERT, MARGIN_ALERT, DAILY_SUMMARY
    
    // Para PRICE_UPDATE (campos que envía el backend)
    val ticker: String?,            // Símbolo del ticker
    val price: String?,             // Precio actual como String
    val changePercent: String?,     // Variación % (solo en PRICE_UPDATE)
    
    // Para PRICE_ALERT
    val currentPrice: String?,      // Precio actual
    val targetPrice: String?,       // Precio objetivo del usuario
    
    // Para MARGIN_ALERT
    val marginOfSafety: String?,    // Margen de seguridad %
    
    // Para DAILY_SUMMARY
    val tickerCount: String?,       // Total de tickers en watchlist
    val gainersCount: String?,      // Cantidad subiendo
    val losersCount: String?,       // Cantidad bajando
    
    // Metadata (todos los tipos)
    val timestamp: String?          // Unix timestamp en milisegundos
)
```

---

## 🔧 Implementación Completa Android

### 1. FcmTokenRepository - Gestión de Tokens

```kotlin
import android.content.SharedPreferences
import android.os.Build
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmTokenRepository @Inject constructor(
    private val apiService: DivTrackerApiService,
    private val preferences: SharedPreferences,
    private val authRepository: AuthRepository  // Para obtener el JWT token
) {
    companion object {
        private const val KEY_FCM_TOKEN = "fcm_token"
        private const val KEY_DEVICE_ID = "device_id"
    }

    suspend fun registerToken(token: String): Result<DeviceResponse> {
        // Solo registrar si el usuario está autenticado
        if (!authRepository.isLoggedIn()) {
            // Guardar token localmente para registrar después del login
            preferences.edit().putString(KEY_FCM_TOKEN, token).apply()
            return Result.failure(Exception("User not logged in"))
        }
        
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

    /**
     * Llamar después de login exitoso para registrar token pendiente
     */
    suspend fun registerPendingToken() {
        preferences.getString(KEY_FCM_TOKEN, null)?.let { token ->
            registerToken(token)
        }
    }

    /**
     * Llamar en logout para eliminar el dispositivo del backend
     */
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

    fun getDeviceId(): String = getOrCreateDeviceId()

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
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {
    const val PRICE_ALERTS = "price_alerts"
    const val MARGIN_ALERTS = "margin_alerts"
    const val DAILY_SUMMARY = "daily_summary"
    const val GENERAL = "general"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)

            val channels = listOf(
                NotificationChannel(
                    PRICE_ALERTS,
                    "Alertas de Precio",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notificaciones cuando un ticker alcanza tu precio objetivo"
                    enableVibration(true)
                },
                NotificationChannel(
                    MARGIN_ALERTS,
                    "Alertas de Margen de Seguridad",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notificaciones de oportunidades de compra"
                    enableVibration(true)
                },
                NotificationChannel(
                    DAILY_SUMMARY,
                    "Resumen Diario",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Resumen diario de tu watchlist"
                    enableVibration(false)
                },
                NotificationChannel(
                    GENERAL,
                    "General",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )

            notificationManager.createNotificationChannels(channels)
        }
    }
}
```

### 3. WatchlistDao - Query para actualizar precio

```kotlin
@Dao
interface WatchlistDao {
    
    @Query("""
        UPDATE watchlist_items 
        SET current_price = :currentPrice, 
            daily_change_percent = :dailyChangePercent,
            updated_at = :updatedAt
        WHERE UPPER(ticker) = UPPER(:ticker)
    """)
    suspend fun updatePriceByTicker(
        ticker: String,
        currentPrice: BigDecimal,
        dailyChangePercent: BigDecimal?,
        updatedAt: Long = System.currentTimeMillis()
    )
    
    // ... otros métodos
}
```

### 4. Retrofit API Service

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

### 5. Flujo de Logout

```kotlin
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val fcmTokenRepository: FcmTokenRepository
) : ViewModel() {

    fun logout() {
        viewModelScope.launch {
            // 1. Eliminar dispositivo del backend (stop push notifications)
            fcmTokenRepository.unregisterCurrentDevice()
            
            // 2. Limpiar sesión local
            authRepository.logout()
            
            // 3. Navegar a login
            _navigationEvent.emit(NavigationEvent.GoToLogin)
        }
    }
}
```

### 6. Permisos de Notificación (Android 13+)

```kotlin
// En tu Activity o Fragment
private val notificationPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
) { isGranted ->
    if (isGranted) {
        // Permiso concedido, registrar token
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            viewModel.registerFcmToken(token)
        }
    }
}

private fun requestNotificationPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Ya tenemos permiso
            }
            shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                // Mostrar explicación al usuario
                showNotificationRationale()
            }
            else -> {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
```

---

## 🎨 UI: Indicador de conexión en tiempo real

```kotlin
@Composable
fun RealtimeConnectionIndicator(
    lastUpdateTime: Long?,  // timestamp en millis
    modifier: Modifier = Modifier
) {
    val isRecent = lastUpdateTime?.let { 
        System.currentTimeMillis() - it < 60_000 // menos de 1 minuto
    } ?: false
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = if (isRecent) Color(0xFF4CAF50) else Color.Gray,
                    shape = CircleShape
                )
        )
        
        Spacer(modifier = Modifier.width(4.dp))
        
        Text(
            text = lastUpdateTime?.let { formatRelativeTime(it) } ?: "Sin datos",
            style = MaterialTheme.typography.labelSmall,
            color = if (isRecent) Color(0xFF4CAF50) else Color.Gray
        )
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    val seconds = (System.currentTimeMillis() - timestamp) / 1000
    return when {
        seconds < 60 -> "hace ${seconds}s"
        seconds < 3600 -> "hace ${seconds / 60}m"
        seconds < 86400 -> "hace ${seconds / 3600}h"
        else -> "hace ${seconds / 86400}d"
    }
}
```

---

## 🔄 WatchlistItemResponse - Modelo Completo

### Kotlin Data Class (actualizada con todos los campos del backend)

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
    val targetPrice: BigDecimal?,            // Precio objetivo manual del usuario
    val targetPfcf: BigDecimal?,             // P/FCF objetivo
    
    // === DATOS DE MERCADO ===
    val dailyChangePercent: BigDecimal?,     // Variación diaria %, ej: 1.25 = +1.25%
    val marketCapitalization: BigDecimal?,   // Market cap en USD (valor completo)
    val weekHigh52: BigDecimal?,             // Máximo 52 semanas
    val weekLow52: BigDecimal?,              // Mínimo 52 semanas
    val weekRange52Position: BigDecimal?,    // Posición 0-1 (0=mínimo, 1=máximo)
    
    // === MÉTRICAS DE FCF ===
    val freeCashFlowPerShare: BigDecimal?,   // FCF por acción
    val actualPfcf: BigDecimal?,             // P/FCF actual calculado
    val fcfYield: BigDecimal?,               // FCF Yield (%)
    val focfCagr5Y: BigDecimal?,             // CAGR del FCF operativo 5 años (%)
    
    // === MÉTRICAS DE DIVIDENDOS ===
    val dividendYield: BigDecimal?,          // Yield actual (%), ej: 3.50
    val dividendGrowthRate5Y: BigDecimal?,   // Crecimiento 5Y (%), ej: 8.50
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
    
    // === PARÁMETROS DE INVERSIÓN (configurables por usuario) ===
    val estimatedFcfGrowthRate: BigDecimal?, // Tasa crecimiento FCF (decimal: 0.08 = 8%)
    val investmentHorizonYears: Int?,        // Horizonte en años
    val discountRate: BigDecimal?,           // WACC/Tasa descuento (decimal: 0.10 = 10%)
    
    // === MÉTRICAS CALCULADAS ===
    val estimatedIRR: BigDecimal?,           // TIR estimada (%)
    val estimatedROI: BigDecimal?,           // ROI al horizonte (%)
    val paybackPeriod: BigDecimal?,          // Años para recuperar inversión
    
    // === NOTIFICACIONES ===
    val notifyWhenBelowPrice: Boolean?,      // Si enviar PRICE_ALERT cuando precio < targetPrice
    
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

### 🔥 Firebase Cloud Messaging (PRIORITARIO)

#### Configuración Inicial
- [ ] Añadir dependencias de Firebase en `build.gradle.kts`
- [ ] Configurar `google-services.json` desde Firebase Console
- [ ] Registrar `DivTrackerMessagingService` en `AndroidManifest.xml`
- [ ] Crear `NotificationChannels` en `Application.onCreate()`

#### Implementación de Servicios
- [ ] Implementar `DivTrackerMessagingService` con `onNewToken()` y `onMessageReceived()`
- [ ] Implementar `FcmTokenRepository` para gestión de tokens
- [ ] **Implementar `handlePriceUpdate()` para actualizar `WatchlistDao`** ← TODO pendiente
- [ ] Añadir endpoints de dispositivos a Retrofit API Service

#### Permisos y UI
- [ ] Manejar permisos `POST_NOTIFICATIONS` (Android 13+)
- [ ] Crear UI `RealtimeConnectionIndicator` para mostrar estado

#### Flujos de Usuario
- [ ] Registrar token FCM después de login exitoso (`registerPendingToken()`)
- [ ] **Implementar botón de Logout** que llame a `DELETE /api/v1/devices/{deviceId}` ← TODO pendiente
- [ ] Implementar lógica de reintento para registro de tokens fallidos

### 📱 Modelo de Datos
- [ ] Actualizar `WatchlistItemResponse` data class con todos los campos
- [ ] Verificar que Retrofit deserializa todos los campos (nullable)
- [ ] Añadir `updatePriceByTicker()` en `WatchlistDao`

### 🎨 UI Components
- [ ] Crear componente `DividendMetricsCard` (o integrar en `WatchlistDetailScreen`)
- [ ] Crear componente `WeekRangeIndicator` (barra 52 semanas)
- [ ] Crear componente `ChowderBadge`
- [ ] Actualizar `WatchlistItemCard` con nuevos datos

### 🔢 Formatters
- [ ] Implementar `ValueFormatter` object
- [ ] Implementar funciones de color según valores

---

## 📝 Notas Importantes

### Datos y Formateo
1. **Todos los campos son nullable** - El backend puede no tener datos para todas las acciones
2. **`payoutRatioFcf` es ratio, no porcentaje** - Multiplicar por 100 para mostrar como %
3. **`marketCapitalization` es valor completo en USD** - No está en millones
4. **`weekRange52Position`** se calcula: `(precio - min) / (max - min)`
5. **`undervalued`** usa la "Golden Rule": `precio < DCF`

### Firebase Cloud Messaging
6. **`PRICE_UPDATE` es data-only** - NO tiene campo `notification`, solo `data`
7. **Tokens FCM pueden cambiar** - Siempre manejar `onNewToken()` y re-registrar
8. **`deviceId` debe ser único y persistente** - Usar UUID guardado en SharedPreferences
9. **Notificaciones requieren permiso en Android 13+** - Solicitar `POST_NOTIFICATIONS`
10. **Las alertas solo se envían para tickers en watchlist** - El backend filtra por usuario

### Timestamps del Backend
11. **`DeviceResponse.createdAt/lastUsedAt`** son `Instant` (ISO 8601 con Z)
12. **`WatchlistItemResponse.createdAt/updatedAt`** son `LocalDateTime`
13. **`timestamp` en notificaciones** es Unix millis como String

### Canales de Notificación (Android 8+)
| Channel ID | Importancia | Descripción |
|------------|-------------|-------------|
| `price_alerts` | HIGH | Alertas de precio objetivo - vibración |
| `margin_alerts` | HIGH | Alertas de margen de seguridad - vibración |
| `daily_summary` | LOW | Resumen diario - sin vibración |

### Flujo de Registro de Token
```
App Start → Check Auth → If logged in → Get FCM Token → POST /api/v1/devices/register
                       → If not logged in → Store token locally
                       
Login Success → Call registerPendingToken() → POST /api/v1/devices/register

onNewToken() → POST /api/v1/devices/register (actualiza el existente)

Logout → DELETE /api/v1/devices/{deviceId} → Clear local session
```

### Flujo de PRICE_UPDATE (Silenciosa)
```
Finnhub Webhook → Backend procesa → Firebase envía data-only message
                                            ↓
Android DivTrackerMessagingService.onMessageReceived()
                                            ↓
                        handlePriceUpdate(data) → WatchlistDao.updatePriceByTicker()
                                            ↓
                        UI se actualiza automáticamente (Room + Flow)
```

