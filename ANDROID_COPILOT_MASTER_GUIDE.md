# 📱 Android Copilot Master Guide - DivTracker

Este archivo sirve como "Contexto Maestro" para GitHub Copilot en Android Studio. Úsalo para generar ViewModels, Repositorios, Modelos y UI siguiendo la arquitectura exacta del backend.

---

## 🌐 Configuración Base

- **Base URL (Prod):** `http://divtracker-prod.eba-rghuxgtw.eu-west-1.elasticbeanstalk.com/`
- **Base URL (Local Emulador):** `http://10.0.2.2:8080/`
- **Auth Header:** `Authorization: Bearer <token>`
- **Formato Fecha:** ISO-8601 (ej: `2025-11-23T10:30:00`)

---

## 1. Módulo de Autenticación (Auth)

### A. Login
- **Endpoint:** `POST /api/auth/login`
- **Campos Usuario (Input):**
  - `email` (String, validación de email)
  - `password` (String)
- **Respuesta Sistema (Output):** `AuthResponse` (token, id, email, firstName, lastName)
- **Flujo:**
  1. Usuario ingresa credenciales.
  2. App envía request.
  3. Si 200 OK -> Guardar `token` en `SharedPreferences`/`EncryptedSharedPreferences`.
  4. Navegar a `MainActivity`.

### B. Registro (Signup)
- **Endpoint:** `POST /api/auth/signup`
- **Campos Usuario (Input):**
  - `firstName` (String)
  - `lastName` (String)
  - `email` (String)
  - `password` (String, min 6 caracteres)
- **Respuesta Sistema (Output):** `AuthResponse` (mismo que login)
- **Flujo:**
  1. Validar campos en UI.
  2. Enviar request.
  3. Si 200 OK -> Guardar token y navegar.

---

## 2. Módulo de Descubrimiento (Ticker Search)

### A. Búsqueda Inteligente (Universal) - *Recomendado*
- **Endpoint:** `GET /api/v1/tickers/search?q={texto}`
- **Campos Usuario (Input):** Texto en barra de búsqueda (ej: "Apple" o "AAPL").
- **Respuesta Sistema (Output):** Lista de `TickerSearchResult`.
- **Lógica Backend:**
  1. Intenta buscar por **Símbolo exacto** primero (rápido).
  2. Si no hay resultados, busca por **Nombre de compañía** (fuzzy).
- **Uso:** Ideal para una barra de búsqueda única "Buscar empresa o ticker".

### B. Búsqueda Estricta por Símbolo (Lookup)
- **Endpoint:** `GET /api/v1/tickers/lookup?symbol={texto}`
- **Uso:** Optimización si solo quieres autocompletar tickers (ej: campo específico "Ticker").
- **Flujo:** Retorna variaciones del símbolo (ej: "BAM" -> BAM, BAM.A, BAM.B).

---

## 3. Módulo Watchlist (Core)

### A. Listar Items
- **Endpoint:** `GET /api/v1/watchlist`
- **Parámetros:** `page=0`, `size=20`, `sortBy=createdAt`, `direction=DESC`
- **Campos Usuario:** Ninguno (carga automática).
- **Respuesta Sistema:** `WatchlistPage` (lista de items + metadata paginación).
- **Datos Clave a Mostrar en Lista:**
  - `ticker`
  - `currentPrice` (Precio actual)
  - `dcfFairValue` (Valor justo calculado)
  - `undervalued` (Boolean -> Mostrar icono verde/rojo)
  - `marginOfSafety` (% de descuento)

### B. Crear Item (Agregar Acción)
- **Endpoint:** `POST /api/v1/watchlist`
- **Campos Usuario (Input):**
  - `ticker` (**Requerido**, viene de la búsqueda)
  - `targetPrice` (Opcional, precio objetivo de compra)
  - `targetPfcf` (Opcional, ratio P/FCF objetivo)
  - `notes` (Opcional, notas personales)
  - `notifyWhenBelowPrice` (Boolean, switch para alertas)
- **Campos Autocompletados por Backend (Output):**
  - El backend consulta Finnhub automáticamente y rellena:
    - `currentPrice`
    - `freeCashFlowPerShare`
    - `dcfFairValue` (Valor intrínseco calculado)
    - `marginOfSafety`
- **Flujo:**
  1. Usuario confirma ticker.
  2. (Opcional) Usuario ingresa precio objetivo.
  3. Enviar POST.
  4. Backend retorna el objeto completo con cálculos financieros ya hechos.

### C. Ver Detalle
- **Endpoint:** `GET /api/v1/watchlist/{id}`
- **Respuesta:** Objeto `WatchlistItemResponse` completo.

### D. Actualizar Item
- **Endpoint:** `PATCH /api/v1/watchlist/{id}`
- **Campos Usuario (Input):** Cualquiera de los campos de creación (todos opcionales).
- **Flujo:** Enviar solo los campos modificados. El backend recalcula las métricas automáticamente.

### E. Eliminar Item
- **Endpoint:** `DELETE /api/v1/watchlist/{id}`

---

## 4. Módulo de Datos (Fundamentals)

### A. Refrescar Datos Manualmente
- **Endpoint:** `POST /api/v1/fundamentals/{ticker}/refresh`
- **Uso:** Botón "Actualizar Datos" en la vista de detalle.
- **Flujo:** Fuerza al backend a buscar datos frescos en Finnhub (bypasseando el cache de 6h) y devuelve los fundamentales actualizados.
- **Respuesta Sistema (Output):** `FundamentalsResponse`

### Response: Fundamentals (Detalle Técnico)
```kotlin
data class FundamentalsResponse(
    val ticker: String,
    val companyName: String?,
    val currency: String?,
    val sector: String?,
    val currentPrice: BigDecimal?,
    val peAnnual: BigDecimal?,      // Price-to-Earnings
    val beta: BigDecimal?,          // Volatilidad
    val debtToEquityRatio: BigDecimal?,
    val fcfAnnual: BigDecimal?,
    val fcfPerShareAnnual: BigDecimal?,
    val dividendYield: BigDecimal?,
    val dividendGrowthRate5Y: BigDecimal?, // Crecimiento Dividendos 5Y
    val epsGrowth5Y: BigDecimal?,          // Crecimiento EPS 5Y
    val revenueGrowth5Y: BigDecimal?,      // Crecimiento Ventas 5Y
    val focfCagr5Y: BigDecimal?,           // Crecimiento FCF Operativo 5Y
    val dataQuality: String?,       // "COMPLETE", "PARTIAL", "STALE"
    val lastUpdatedAt: String?      // ISO-8601
)
```

---

## 📦 Modelos de Datos (Kotlin Templates)

Copia estos modelos para que Copilot entienda la estructura exacta.

### Request: Crear/Editar Item
```kotlin
data class WatchlistItemRequest(
    val ticker: String, // Solo requerido en creación
    val exchange: String? = null,
    val targetPrice: BigDecimal? = null, // Opcional
    val targetPfcf: BigDecimal? = null,  // Opcional
    val notifyWhenBelowPrice: Boolean? = false,
    val notes: String? = null,
    // Parámetros avanzados DCF (Opcionales)
    // Si no se envía estimatedFcfGrowthRate, el backend lo auto-calcula basado en EPS Growth histórico (topeado al 15%)
    val estimatedFcfGrowthRate: BigDecimal? = null, 
    val investmentHorizonYears: Int? = null,
    val discountRate: BigDecimal? = null
)
```

### Response: Item Completo
```kotlin
data class WatchlistItemResponse(
    val id: UUID,
    val ticker: String,
    // Datos Usuario
    val targetPrice: BigDecimal?,
    val notes: String?,
    // Datos Mercado (Automáticos)
    val currentPrice: BigDecimal?,
    val freeCashFlowPerShare: BigDecimal?,
    val peAnnual: BigDecimal?,         // Price-to-Earnings Anual
    val beta: BigDecimal?,             // Volatilidad
    val focfCagr5Y: BigDecimal?,       // CAGR FCF 5 años
    val dividendYield: BigDecimal?,    // Yield
    // Cálculos Backend (Automáticos)
    val dcfFairValue: BigDecimal?,     // Valor Justo
    val marginOfSafety: BigDecimal?,   // Margen Seguridad %
    val undervalued: Boolean?,         // ¿Está barata?
    val estimatedIRR: BigDecimal?,     // Retorno anual esperado
    val paybackPeriod: BigDecimal?     // Años para recuperar inversión
)
```

### Response: Búsqueda Ticker
```kotlin
data class TickerSearchResult(
    val symbol: String,      // Ej: "AAPL"
    val description: String, // Ej: "Apple Inc"
    val type: String?,       // Ej: "Common Stock"
    val exchange: String?    // Ej: "NASDAQ"
)
```

---

## 5. Ejemplo de Flujo Completo (Frontend)

### Escenario: Agregar Acción al Watchlist

Este ejemplo muestra cómo implementar el flujo completo usando **MVVM + Coroutines + StateFlow**.

#### 1. Repository (`WatchlistRepository.kt`)
Encapsula las llamadas a la API.

```kotlin
class WatchlistRepository(private val api: DivTrackerApi) {

    // Búsqueda de Ticker (Universal)
    suspend fun searchTicker(query: String): Result<List<TickerSearchResult>> = runCatching {
        // Usa el endpoint de búsqueda inteligente (/search) que soporta nombre y símbolo
        api.searchTickers(query)
    }

    // Crear Item
    suspend fun createItem(ticker: String, targetPrice: BigDecimal?): Result<WatchlistItemResponse> = runCatching {
        val request = WatchlistItemRequest(
            ticker = ticker,
            targetPrice = targetPrice
            // Otros campos opcionales...
        )
        api.createWatchlistItem(request)
    }
}
```

#### 2. ViewModel (`AddTickerViewModel.kt`)
Maneja el estado de la UI.

```kotlin
class AddTickerViewModel(private val repository: WatchlistRepository) : ViewModel() {

    // Estado de Búsqueda
    private val _searchResults = MutableStateFlow<List<TickerSearchResult>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    // Estado de Operación (Loading/Success/Error)
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun onSearchQueryChanged(query: String) {
        viewModelScope.launch {
            if (query.length > 2) {
                repository.searchTicker(query)
                    .onSuccess { _searchResults.value = it }
                    .onFailure { /* Manejar error silencioso */ }
            }
        }
    }

    fun onAddTickerClicked(ticker: String, targetPrice: String?) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            
            val price = targetPrice?.toBigDecimalOrNull()
            
            repository.createItem(ticker, price)
                .onSuccess { 
                    _uiState.value = UiState.Success("Ticker agregado: ${it.ticker}")
                }
                .onFailure { e ->
                    _uiState.value = UiState.Error(e.message ?: "Error desconocido")
                }
        }
    }
}

sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class Success(val message: String) : UiState()
    data class Error(val error: String) : UiState()
}
```

#### 3. Fragment (`AddTickerFragment.kt`)
Observa el estado y actualiza la UI.

```kotlin
// En onViewCreated...

// 1. Observar resultados de búsqueda
viewLifecycleOwner.lifecycleScope.launch {
    viewModel.searchResults.collect { results ->
        adapter.submitList(results)
    }
}

// 2. Observar estado de la operación
viewLifecycleOwner.lifecycleScope.launch {
    viewModel.uiState.collect { state ->
        when(state) {
            is UiState.Loading -> binding.progressBar.isVisible = true
            is UiState.Success -> {
                binding.progressBar.isVisible = false
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                findNavController().popBackStack() // Volver a la lista
            }
            is UiState.Error -> {
                binding.progressBar.isVisible = false
                showErrorDialog(state.error)
            }
            else -> binding.progressBar.isVisible = false
        }
    }
}
```
