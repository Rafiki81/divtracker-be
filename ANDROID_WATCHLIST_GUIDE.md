# Guía de Integración Android - Watchlist API

## 📋 Tabla de Contenidos

1. [Descripción General](#descripción-general)
2. [Data Models](#data-models)
3. [API Service](#api-service)
4. [Repository](#repository)
5. [ViewModel](#viewmodel)
6. [UI States](#ui-states)
7. [Ejemplos de Uso en Activities/Fragments](#ejemplos-de-uso)
8. [Paginación](#paginación)
9. [Manejo de Errores](#manejo-de-errores)
10. [Testing](#testing)
11. [Dependencias](#dependencias)
12. [URLs de Entorno](#urls-de-entorno)
13. [Checklist de Implementación](#checklist-de-implementación)

---

## Descripción General

La **Watchlist API** permite a los usuarios autenticados gestionar una lista de empresas que desean vigilar, con análisis financiero automático que incluye:

- ✅ **DCF (Discounted Cash Flow)**: Valoración intrínseca
- ✅ **TIR (Internal Rate of Return)**: Rentabilidad esperada
- ✅ **FCF Yield**: Rendimiento del flujo de caja libre
- ✅ **Margen de Seguridad**: Diferencia entre precio y valor intrínseco
- ✅ **Payback Period**: Años para recuperar inversión
- ✅ **ROI Estimado**: Retorno de inversión proyectado

**Base URL**: `/api/v1/watchlist`

**Autenticación**: Todos los endpoints requieren JWT token en el header `Authorization: Bearer <token>`

### 🆕 Creación Automática con Finnhub

**Nuevo en v1.1**: Puedes crear items solo con el ticker, sin necesidad de especificar `targetPrice` o `targetPfcf`. El backend:

1. 🔍 Obtiene el precio actual de Finnhub
2. 📊 Obtiene el FCF por acción de Finnhub
3. 🧮 Calcula el P/FCF actual automáticamente
4. ✨ Lo establece como `targetPfcf` inicial
5. 📈 Calcula todas las métricas financieras (DCF, TIR, ROI, etc.)

**Requisito**: Finnhub API debe estar configurada en el backend (ver `FINNHUB_SETUP.md`)

---

## Data Models

### 1. WatchlistItemRequest

**Request para crear o actualizar un item**

```kotlin
package com.yourcompany.divtracker.data.model

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class WatchlistItemRequest(
    @SerializedName("ticker")
    val ticker: String,  // Requerido, 1-12 caracteres, ej: "AAPL"
    
    @SerializedName("exchange")
    val exchange: String? = null,  // Opcional, ej: "NASDAQ"
    
    @SerializedName("targetPrice")
    val targetPrice: BigDecimal? = null,  // Opcional* - ver nota abajo
    
    @SerializedName("targetPfcf")
    val targetPfcf: BigDecimal? = null,  // Opcional* - ver nota abajo
    
    @SerializedName("notifyWhenBelowPrice")
    val notifyWhenBelowPrice: Boolean? = false,
    
    @SerializedName("notes")
    val notes: String? = null,  // Opcional, máx 500 caracteres
    
    @SerializedName("estimatedFcfGrowthRate")
    val estimatedFcfGrowthRate: BigDecimal? = null,  // Opcional, 0.0-1.0 (ej: 0.08 = 8%)
    
    @SerializedName("investmentHorizonYears")
    val investmentHorizonYears: Int? = null,  // Opcional, 1-30 años
    
    @SerializedName("discountRate")
    val discountRate: BigDecimal? = null  // Opcional, 0.01-1.0 (ej: 0.10 = 10%)
) {
    // NOTA: targetPrice y targetPfcf son opcionales si Finnhub está habilitado
    // El backend calculará targetPfcf automáticamente basándose en datos de mercado
    // Si Finnhub no está disponible, al menos uno es requerido
    
    init {
        ticker.let {
            require(it.isNotBlank() && it.length in 1..12) {
                "El ticker debe tener entre 1 y 12 caracteres"
            }
            require(it.matches(Regex("^[A-Za-z0-9.\\-]+$"))) {
                "El ticker solo puede contener letras, números, puntos y guiones"
            }
        }
    }
}
```

### 2. WatchlistItemResponse

**Response con todos los datos y métricas calculadas**

```kotlin
package com.yourcompany.divtracker.data.model

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class WatchlistItemResponse(
    @SerializedName("id")
    val id: UUID,
    
    @SerializedName("userId")
    val userId: UUID,
    
    @SerializedName("ticker")
    val ticker: String,
    
    @SerializedName("exchange")
    val exchange: String?,
    
    // Parámetros configurados por el usuario
    @SerializedName("targetPrice")
    val targetPrice: BigDecimal?,
    
    @SerializedName("targetPfcf")
    val targetPfcf: BigDecimal?,
    
    @SerializedName("notifyWhenBelowPrice")
    val notifyWhenBelowPrice: Boolean,
    
    @SerializedName("notes")
    val notes: String?,
    
    // Datos de mercado actuales (desde cache de fundamentales)
    @SerializedName("currentPrice")
    val currentPrice: BigDecimal?,
    
    @SerializedName("freeCashFlowPerShare")
    val freeCashFlowPerShare: BigDecimal?,
    
    @SerializedName("actualPfcf")
    val actualPfcf: BigDecimal?,
    
    // 🆕 NUEVOS: Ratios de valoración desde cache
    @SerializedName("peTTM")
    val peTTM: BigDecimal?,  // Price-to-Earnings TTM
    
    @SerializedName("beta")
    val beta: BigDecimal?,  // Volatilidad vs mercado (1.0 = mismo que mercado)
    
    // Análisis de valoración básico
    @SerializedName("fairPriceByPfcf")
    val fairPriceByPfcf: BigDecimal?,
    
    @SerializedName("discountToFairPrice")
    val discountToFairPrice: BigDecimal?,  // Positivo = descuento, negativo = prima
    
    @SerializedName("deviationFromTargetPrice")
    val deviationFromTargetPrice: BigDecimal?,
    
    @SerializedName("undervalued")
    val undervalued: Boolean?,
    
    // Parámetros de valoración avanzada
    @SerializedName("estimatedFcfGrowthRate")
    val estimatedFcfGrowthRate: BigDecimal?,
    
    @SerializedName("investmentHorizonYears")
    val investmentHorizonYears: Int?,
    
    @SerializedName("discountRate")
    val discountRate: BigDecimal?,
    
    // Métricas calculadas avanzadas
    @SerializedName("dcfFairValue")
    val dcfFairValue: BigDecimal?,  // Valor intrínseco por DCF
    
    @SerializedName("fcfYield")
    val fcfYield: BigDecimal?,  // FCF Yield en porcentaje
    
    @SerializedName("marginOfSafety")
    val marginOfSafety: BigDecimal?,  // Margen de seguridad en %
    
    @SerializedName("paybackPeriod")
    val paybackPeriod: BigDecimal?,  // Años para recuperar inversión
    
    @SerializedName("estimatedROI")
    val estimatedROI: BigDecimal?,  // ROI proyectado en %
    
    @SerializedName("estimatedIRR")
    val estimatedIRR: BigDecimal?,  // TIR en %
    
    @SerializedName("createdAt")
    val createdAt: LocalDateTime,
    
    @SerializedName("updatedAt")
    val updatedAt: LocalDateTime
)
```

### 3. TickerSearchResult

**Resultado de búsqueda de ticker**

```kotlin
package com.yourcompany.divtracker.data.model

import com.google.gson.annotations.SerializedName

data class TickerSearchResult(
    @SerializedName("symbol")
    val symbol: String,  // Ej: "AAPL"
    
    @SerializedName("description")
    val description: String,  // Ej: "Apple Inc"
    
    @SerializedName("type")
    val type: String?,  // Ej: "Common Stock"
    
    @SerializedName("exchange")
    val exchange: String?,  // Ej: "NASDAQ"
    
    @SerializedName("currency")
    val currency: String?,  // Ej: "USD"
    
    @SerializedName("figi")
    val figi: String?  // Código FIGI
)
```

### 4. WatchlistPage

**Response paginado**

```kotlin
package com.yourcompany.divtracker.data.model

import com.google.gson.annotations.SerializedName

data class WatchlistPage(
    @SerializedName("content")
    val content: List<WatchlistItemResponse>,
    
    @SerializedName("totalElements")
    val totalElements: Long,
    
    @SerializedName("totalPages")
    val totalPages: Int,
    
    @SerializedName("size")
    val size: Int,
    
    @SerializedName("number")
    val number: Int,  // Página actual (0-indexed)
    
    @SerializedName("numberOfElements")
    val numberOfElements: Int,
    
    @SerializedName("first")
    val first: Boolean,
    
    @SerializedName("last")
    val last: Boolean,
    
    @SerializedName("empty")
    val empty: Boolean
)
```

---

## API Service

```kotlin
package com.yourcompany.divtracker.data.api

import com.yourcompany.divtracker.data.model.WatchlistItemRequest
import com.yourcompany.divtracker.data.model.WatchlistItemResponse
import com.yourcompany.divtracker.data.model.WatchlistPage
import retrofit2.Response
import retrofit2.http.*
import java.util.UUID

interface WatchlistApiService {
    
    /**
     * 🆕 Symbol Lookup - Búsqueda exacta de símbolos (RECOMENDADO PARA FLUJO PRINCIPAL)
     * 
     * Nuevo en v1.2: Búsqueda exacta de símbolos en US exchanges.
     * Busca símbolos exactos en US exchanges. Retorna todas las variaciones
     * de un ticker específico (ej: BAM → BAM, BAM.A, BAM.B).
     * 
     * Este es el endpoint recomendado para el flujo inicial de selección de ticker:
     * 1. Usuario escribe "BAM"
     * 2. App llama a lookupSymbol("BAM")
     * 3. Backend devuelve todas las variaciones (BAM, BAM.A, etc.)
     * 4. Usuario selecciona el símbolo exacto
     * 5. App crea watchlist item con el símbolo validado
     * 
     * @param symbol Símbolo del ticker (ej: "BAM", "AAPL", "MSFT")
     * @return Lista de símbolos que comienzan con el query (hasta 20)
     */
    @GET("api/v1/tickers/lookup")
    suspend fun lookupSymbol(
        @Query("symbol") symbol: String
    ): Response<List<TickerSearchResult>>
    
    /**
     * 🆕 Search by Name - Búsqueda fuzzy por nombre de compañía
     * 
     * Nuevo en v1.2: Búsqueda flexible para cuando el usuario no conoce el ticker exacto.
     * Busca por nombre de empresa:
     * - "Apple" → AAPL
     * - "Microsoft" → MSFT
     * - "Tesla" → TSLA
     * 
     * @param query Término de búsqueda (nombre de empresa)
     * @return Lista de hasta 20 resultados coincidentes
     */
    @GET("api/v1/tickers/search")
    suspend fun searchTickers(
        @Query("q") query: String
    ): Response<List<TickerSearchResult>>
    
    /**
     * Listar items del watchlist con paginación y ordenamiento
     * 
     * @param page Número de página (0-indexed)
     * @param size Tamaño de página (default: 20)
     * @param sortBy Campo de ordenamiento (default: "createdAt")
     * @param direction Dirección de ordenamiento: "ASC" o "DESC" (default: "DESC")
     * @return Página con lista de items y métricas calculadas
     */
    @GET("api/v1/watchlist")
    suspend fun listWatchlistItems(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sortBy") sortBy: String = "createdAt",
        @Query("direction") direction: String = "DESC"
    ): Response<WatchlistPage>
    
    /**
     * Obtener detalles completos de un item por ID
     * 
     * @param id UUID del item
     * @return Item con todos sus datos y métricas calculadas
     */
    @GET("api/v1/watchlist/{id}")
    suspend fun getWatchlistItemById(
        @Path("id") id: UUID
    ): Response<WatchlistItemResponse>
    
    /**
     * Crear nuevo item en el watchlist
     * 
     * El backend calculará automáticamente:
     * - Precio actual de mercado
     * - FCF por acción
     * - P/FCF actual
     * - Precio justo por P/FCF
     * - Descuento/prima
     * - DCF fair value
     * - Margen de seguridad
     * - TIR, ROI, payback period, etc.
     * 
     * @param request Datos del item a crear
     * @return Item creado con todas las métricas calculadas
     */
    @POST("api/v1/watchlist")
    suspend fun createWatchlistItem(
        @Body request: WatchlistItemRequest
    ): Response<WatchlistItemResponse>
    
    /**
     * Actualizar item existente (PATCH - actualización parcial)
     * 
     * Solo se actualizan los campos enviados en el request.
     * Recalcula automáticamente todas las métricas si cambian
     * los parámetros de entrada.
     * 
     * @param id UUID del item a actualizar
     * @param request Datos a actualizar (campos opcionales)
     * @return Item actualizado con métricas recalculadas
     */
    @PATCH("api/v1/watchlist/{id}")
    suspend fun updateWatchlistItem(
        @Path("id") id: UUID,
        @Body request: WatchlistItemRequest
    ): Response<WatchlistItemResponse>
    
    /**
     * Eliminar item del watchlist
     * 
     * @param id UUID del item a eliminar
     * @return Response vacío (204 No Content)
     */
    @DELETE("api/v1/watchlist/{id}")
    suspend fun deleteWatchlistItem(
        @Path("id") id: UUID
    ): Response<Unit>
}
```

---

## Repository

```kotlin
package com.yourcompany.divtracker.data.repository

import com.yourcompany.divtracker.data.api.WatchlistApiService
import com.yourcompany.divtracker.data.model.WatchlistItemRequest
import com.yourcompany.divtracker.data.model.WatchlistItemResponse
import com.yourcompany.divtracker.data.model.WatchlistPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.util.UUID

class WatchlistRepository(
    private val apiService: WatchlistApiService
) {
    
    /**
     * 🆕 Symbol Lookup - Búsqueda exacta de símbolos (RECOMENDADO)
     * Nuevo en v1.2
     */
    suspend fun lookupSymbol(symbol: String): Result<List<TickerSearchResult>> = 
        withContext(Dispatchers.IO) {
            try {
                if (symbol.isBlank()) {
                    return@withContext Result.success(emptyList())
                }
                val response = apiService.lookupSymbol(symbol)
                handleResponse(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    /**
     * 🆕 Search by Name - Búsqueda fuzzy por nombre
     * Nuevo en v1.2
     */
    suspend fun searchTickers(query: String): Result<List<TickerSearchResult>> = 
        withContext(Dispatchers.IO) {
            try {
                if (query.isBlank()) {
                    return@withContext Result.success(emptyList())
                }
                val response = apiService.searchTickers(query)
                handleResponse(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    /**
     * Listar items con paginación
     */
    suspend fun listItems(
        page: Int = 0,
        size: Int = 20,
        sortBy: String = "createdAt",
        direction: String = "DESC"
    ): Result<WatchlistPage> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.listWatchlistItems(page, size, sortBy, direction)
            handleResponse(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Obtener item por ID
     */
    suspend fun getItemById(id: UUID): Result<WatchlistItemResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getWatchlistItemById(id)
            handleResponse(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Crear nuevo item
     */
    suspend fun createItem(request: WatchlistItemRequest): Result<WatchlistItemResponse> = 
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.createWatchlistItem(request)
                handleResponse(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    /**
     * Actualizar item existente
     */
    suspend fun updateItem(
        id: UUID,
        request: WatchlistItemRequest
    ): Result<WatchlistItemResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.updateWatchlistItem(id, request)
            handleResponse(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Eliminar item
     */
    suspend fun deleteItem(id: UUID): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.deleteWatchlistItem(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Helper para manejar respuestas de Retrofit
     */
    private fun <T> handleResponse(response: Response<T>): Result<T> {
        return if (response.isSuccessful) {
            response.body()?.let {
                Result.success(it)
            } ?: Result.failure(Exception("Respuesta vacía del servidor"))
        } else {
            val errorMsg = when (response.code()) {
                401 -> "No autenticado. Inicia sesión nuevamente"
                404 -> "Item no encontrado"
                409 -> "El ticker ya existe en tu watchlist"
                400 -> "Datos inválidos. Verifica los campos"
                else -> "Error ${response.code()}: ${response.message()}"
            }
            Result.failure(Exception(errorMsg))
        }
    }
}
```

---

## ViewModel

```kotlin
package com.yourcompany.divtracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourcompany.divtracker.data.model.WatchlistItemRequest
import com.yourcompany.divtracker.data.model.WatchlistItemResponse
import com.yourcompany.divtracker.data.model.WatchlistPage
import com.yourcompany.divtracker.data.repository.WatchlistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class WatchlistViewModel(
    private val repository: WatchlistRepository
) : ViewModel() {
    
    // State para lista de items
    private val _listState = MutableStateFlow<WatchlistListState>(WatchlistListState.Idle)
    val listState: StateFlow<WatchlistListState> = _listState.asStateFlow()
    
    // State para detalle de un item
    private val _detailState = MutableStateFlow<WatchlistDetailState>(WatchlistDetailState.Idle)
    val detailState: StateFlow<WatchlistDetailState> = _detailState.asStateFlow()
    
    // State para operaciones de creación/actualización/eliminación
    private val _operationState = MutableStateFlow<WatchlistOperationState>(WatchlistOperationState.Idle)
    val operationState: StateFlow<WatchlistOperationState> = _operationState.asStateFlow()
    
    // State para búsqueda de tickers
    private val _searchState = MutableStateFlow<TickerSearchState>(TickerSearchState.Idle)
    val searchState: StateFlow<TickerSearchState> = _searchState.asStateFlow()
    
    /**
     * Buscar tickers por nombre o símbolo
     */
    fun searchTickers(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                _searchState.value = TickerSearchState.Idle
                return@launch
            }
            
            _searchState.value = TickerSearchState.Loading
            
            repository.searchTickers(query)
                .onSuccess { results ->
                    _searchState.value = TickerSearchState.Success(results)
                }
                .onFailure { error ->
                    _searchState.value = TickerSearchState.Error(
                        error.message ?: "Error al buscar tickers"
                    )
                }
        }
    }
    
    /**
     * Limpiar resultados de búsqueda
     */
    fun clearSearch() {
        _searchState.value = TickerSearchState.Idle
    }
    
    /**
     * Cargar lista de items con paginación
     */
    fun loadWatchlist(
        page: Int = 0,
        size: Int = 20,
        sortBy: String = "createdAt",
        direction: String = "DESC"
    ) {
        viewModelScope.launch {
            _listState.value = WatchlistListState.Loading
            
            repository.listItems(page, size, sortBy, direction)
                .onSuccess { watchlistPage ->
                    _listState.value = WatchlistListState.Success(watchlistPage)
                }
                .onFailure { error ->
                    _listState.value = WatchlistListState.Error(
                        error.message ?: "Error al cargar watchlist"
                    )
                }
        }
    }
    
    /**
     * Cargar detalle de un item
     */
    fun loadItemDetail(id: UUID) {
        viewModelScope.launch {
            _detailState.value = WatchlistDetailState.Loading
            
            repository.getItemById(id)
                .onSuccess { item ->
                    _detailState.value = WatchlistDetailState.Success(item)
                }
                .onFailure { error ->
                    _detailState.value = WatchlistDetailState.Error(
                        error.message ?: "Error al cargar item"
                    )
                }
        }
    }
    
    /**
     * Crear nuevo item
     */
    fun createItem(request: WatchlistItemRequest) {
        viewModelScope.launch {
            _operationState.value = WatchlistOperationState.Loading
            
            repository.createItem(request)
                .onSuccess { item ->
                    _operationState.value = WatchlistOperationState.Created(item)
                    // Recargar lista después de crear
                    loadWatchlist()
                }
                .onFailure { error ->
                    _operationState.value = WatchlistOperationState.Error(
                        error.message ?: "Error al crear item"
                    )
                }
        }
    }
    
    /**
     * Actualizar item existente
     */
    fun updateItem(id: UUID, request: WatchlistItemRequest) {
        viewModelScope.launch {
            _operationState.value = WatchlistOperationState.Loading
            
            repository.updateItem(id, request)
                .onSuccess { item ->
                    _operationState.value = WatchlistOperationState.Updated(item)
                    // Recargar lista después de actualizar
                    loadWatchlist()
                }
                .onFailure { error ->
                    _operationState.value = WatchlistOperationState.Error(
                        error.message ?: "Error al actualizar item"
                    )
                }
        }
    }
    
    /**
     * Eliminar item
     */
    fun deleteItem(id: UUID) {
        viewModelScope.launch {
            _operationState.value = WatchlistOperationState.Loading
            
            repository.deleteItem(id)
                .onSuccess {
                    _operationState.value = WatchlistOperationState.Deleted
                    // Recargar lista después de eliminar
                    loadWatchlist()
                }
                .onFailure { error ->
                    _operationState.value = WatchlistOperationState.Error(
                        error.message ?: "Error al eliminar item"
                    )
                }
        }
    }
    
    /**
     * Reset de estados
     */
    fun resetListState() {
        _listState.value = WatchlistListState.Idle
    }
    
    fun resetDetailState() {
        _detailState.value = WatchlistDetailState.Idle
    }
    
    fun resetOperationState() {
        _operationState.value = WatchlistOperationState.Idle
    }
}
```

---

## UI States

```kotlin
package com.yourcompany.divtracker.ui.viewmodel

import com.yourcompany.divtracker.data.model.WatchlistItemResponse
import com.yourcompany.divtracker.data.model.WatchlistPage

/**
 * Estados para la lista de watchlist
 */
sealed class WatchlistListState {
    object Idle : WatchlistListState()
    object Loading : WatchlistListState()
    data class Success(val watchlistPage: WatchlistPage) : WatchlistListState()
    data class Error(val message: String) : WatchlistListState()
}

/**
 * Estados para el detalle de un item
 */
sealed class WatchlistDetailState {
    object Idle : WatchlistDetailState()
    object Loading : WatchlistDetailState()
    data class Success(val item: WatchlistItemResponse) : WatchlistDetailState()
    data class Error(val message: String) : WatchlistDetailState()
}

/**
 * Estados para operaciones CRUD
 */
sealed class WatchlistOperationState {
    object Idle : WatchlistOperationState()
    object Loading : WatchlistOperationState()
    data class Created(val item: WatchlistItemResponse) : WatchlistOperationState()
    data class Updated(val item: WatchlistItemResponse) : WatchlistOperationState()
    object Deleted : WatchlistOperationState()
    data class Error(val message: String) : WatchlistOperationState()
}

/**
 * Estados para búsqueda de tickers
 */
sealed class TickerSearchState {
    object Idle : TickerSearchState()
    object Loading : TickerSearchState()
    data class Success(val results: List<TickerSearchResult>) : TickerSearchState()
    data class Error(val message: String) : TickerSearchState()
}
```

---

## Ejemplos de Uso

### 0. Activity/Fragment - Buscar y Seleccionar Ticker

```kotlin
package com.yourcompany.divtracker.ui.fragment

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.yourcompany.divtracker.R
import com.yourcompany.divtracker.databinding.FragmentTickerSearchBinding
import com.yourcompany.divtracker.ui.adapter.TickerSearchAdapter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TickerSearchFragment : Fragment(R.layout.fragment_ticker_search) {
    
    private var _binding: FragmentTickerSearchBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: TickerSearchViewModel by viewModels()
    private lateinit var adapter: TickerSearchAdapter
    
    private var searchJob: Job? = null
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTickerSearchBinding.bind(view)
        
        setupRecyclerView()
        setupSearchInput()
        observeSearchState()
    }
    
    private fun setupRecyclerView() {
        adapter = TickerSearchAdapter { ticker ->
            // Usuario seleccionó un ticker
            val action = TickerSearchFragmentDirections
                .actionSearchToCreateWatchlist(ticker.symbol)
            findNavController().navigate(action)
        }
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@TickerSearchFragment.adapter
        }
    }
    
    private fun setupSearchInput() {
        binding.searchInput.doAfterTextChanged { text ->
            val query = text?.toString()?.trim() ?: ""
            
            // Cancelar búsqueda anterior
            searchJob?.cancel()
            
            if (query.isEmpty()) {
                adapter.submitList(emptyList())
                return@doAfterTextChanged
            }
            
            // Debounce: esperar 300ms antes de buscar
            searchJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(300)
                
                // Symbol Lookup (recomendado) - Búsqueda exacta
                viewModel.lookupSymbol(query)
                
                // Alternativa: Search by Name - Búsqueda fuzzy
                // viewModel.searchTickers(query)
            }
        }
    }
    
    private fun observeSearchState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.searchState.collect { state ->
                when (state) {
                    is TickerSearchState.Idle -> {
                        binding.progressBar.visibility = View.GONE
                        binding.emptyView.visibility = View.VISIBLE
                        binding.emptyView.text = "Escribe un ticker para buscar"
                    }
                    
                    is TickerSearchState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.emptyView.visibility = View.GONE
                    }
                    
                    is TickerSearchState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        
                        if (state.results.isEmpty()) {
                            binding.emptyView.visibility = View.VISIBLE
                            binding.emptyView.text = "No se encontraron resultados"
                            binding.recyclerView.visibility = View.GONE
                        } else {
                            binding.emptyView.visibility = View.GONE
                            binding.recyclerView.visibility = View.VISIBLE
                            adapter.submitList(state.results)
                        }
                    }
                    
                    is TickerSearchState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.emptyView.visibility = View.VISIBLE
                        binding.emptyView.text = "Error: ${state.message}"
                        
                        Toast.makeText(
                            requireContext(),
                            "Error al buscar: ${state.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        searchJob?.cancel()
    }
}
```

**TickerSearchViewModel.kt**:
```kotlin
package com.yourcompany.divtracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourcompany.divtracker.data.model.TickerSearchResult
import com.yourcompany.divtracker.data.repository.WatchlistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class TickerSearchState {
    object Idle : TickerSearchState()
    object Loading : TickerSearchState()
    data class Success(val results: List<TickerSearchResult>) : TickerSearchState()
    data class Error(val message: String) : TickerSearchState()
}

class TickerSearchViewModel(
    private val repository: WatchlistRepository
) : ViewModel() {
    
    private val _searchState = MutableStateFlow<TickerSearchState>(TickerSearchState.Idle)
    val searchState: StateFlow<TickerSearchState> = _searchState
    
    /**
     * Symbol Lookup - Búsqueda exacta (recomendado)
     * Busca símbolos que comienzan con el query (BAM -> BAM, BAM.A, BAM.B)
     */
    fun lookupSymbol(symbol: String) {
        if (symbol.isBlank()) {
            _searchState.value = TickerSearchState.Idle
            return
        }
        
        viewModelScope.launch {
            _searchState.value = TickerSearchState.Loading
            
            val result = repository.lookupSymbol(symbol)
            
            _searchState.value = when {
                result.isSuccess -> {
                    TickerSearchState.Success(result.getOrDefault(emptyList()))
                }
                else -> {
                    val error = result.exceptionOrNull()?.message ?: "Error desconocido"
                    TickerSearchState.Error(error)
                }
            }
        }
    }
    
    /**
     * Search by Name - Búsqueda fuzzy
     * Busca por nombre de compañía (Apple -> AAPL)
     */
    fun searchTickers(query: String) {
        if (query.isBlank()) {
            _searchState.value = TickerSearchState.Idle
            return
        }
        
        viewModelScope.launch {
            _searchState.value = TickerSearchState.Loading
            
            val result = repository.searchTickers(query)
            
            _searchState.value = when {
                result.isSuccess -> {
                    TickerSearchState.Success(result.getOrDefault(emptyList()))
                }
                else -> {
                    val error = result.exceptionOrNull()?.message ?: "Error desconocido"
                    TickerSearchState.Error(error)
                }
            }
        }
    }
}
```

---

### 1. Activity/Fragment - Listar Watchlist

```kotlin
package com.yourcompany.divtracker.ui.fragment

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.yourcompany.divtracker.R
import com.yourcompany.divtracker.databinding.FragmentWatchlistBinding
import com.yourcompany.divtracker.ui.adapter.WatchlistAdapter
import com.yourcompany.divtracker.ui.viewmodel.WatchlistListState
import com.yourcompany.divtracker.ui.viewmodel.WatchlistViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WatchlistFragment : Fragment(R.layout.fragment_watchlist) {
    
    private var _binding: FragmentWatchlistBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: WatchlistViewModel by viewModels()
    private lateinit var adapter: WatchlistAdapter
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentWatchlistBinding.bind(view)
        
        setupRecyclerView()
        observeListState()
        
        // Cargar primera página
        viewModel.loadWatchlist(page = 0, size = 20, sortBy = "createdAt", direction = "DESC")
        
        // Pull to refresh
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadWatchlist()
        }
    }
    
    private fun setupRecyclerView() {
        adapter = WatchlistAdapter(
            onItemClick = { item ->
                // Navegar a detalle
                val action = WatchlistFragmentDirections
                    .actionWatchlistFragmentToDetailFragment(item.id.toString())
                findNavController().navigate(action)
            },
            onDeleteClick = { item ->
                // Confirmar y eliminar
                showDeleteConfirmation(item.id)
            }
        )
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@WatchlistFragment.adapter
        }
    }
    
    private fun observeListState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.listState.collectLatest { state ->
                when (state) {
                    is WatchlistListState.Idle -> {
                        binding.progressBar.visibility = View.GONE
                        binding.swipeRefresh.isRefreshing = false
                    }
                    
                    is WatchlistListState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    
                    is WatchlistListState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        binding.swipeRefresh.isRefreshing = false
                        
                        val watchlistPage = state.watchlistPage
                        adapter.submitList(watchlistPage.content)
                        
                        // Mostrar empty state si no hay items
                        if (watchlistPage.content.isEmpty()) {
                            binding.emptyView.visibility = View.VISIBLE
                            binding.recyclerView.visibility = View.GONE
                        } else {
                            binding.emptyView.visibility = View.GONE
                            binding.recyclerView.visibility = View.VISIBLE
                        }
                        
                        // Info de paginación
                        binding.pageInfo.text = "Página ${watchlistPage.number + 1} de ${watchlistPage.totalPages}"
                    }
                    
                    is WatchlistListState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.swipeRefresh.isRefreshing = false
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
    
    private fun showDeleteConfirmation(id: UUID) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar item")
            .setMessage("¿Estás seguro de eliminar este item del watchlist?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.deleteItem(id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
```

### 2. Activity/Fragment - Detalle de Item

```kotlin
package com.yourcompany.divtracker.ui.fragment

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.yourcompany.divtracker.R
import com.yourcompany.divtracker.databinding.FragmentWatchlistDetailBinding
import com.yourcompany.divtracker.ui.viewmodel.WatchlistDetailState
import com.yourcompany.divtracker.ui.viewmodel.WatchlistViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID

class WatchlistDetailFragment : Fragment(R.layout.fragment_watchlist_detail) {
    
    private var _binding: FragmentWatchlistDetailBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: WatchlistViewModel by viewModels()
    private val args: WatchlistDetailFragmentArgs by navArgs()
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentWatchlistDetailBinding.bind(view)
        
        observeDetailState()
        
        // Cargar detalle
        val itemId = UUID.fromString(args.itemId)
        viewModel.loadItemDetail(itemId)
        
        // Botón editar
        binding.btnEdit.setOnClickListener {
            // Navegar a pantalla de edición
            val action = WatchlistDetailFragmentDirections
                .actionDetailFragmentToEditFragment(itemId.toString())
            findNavController().navigate(action)
        }
    }
    
    private fun observeDetailState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.detailState.collectLatest { state ->
                when (state) {
                    is WatchlistDetailState.Idle -> {
                        binding.progressBar.visibility = View.GONE
                    }
                    
                    is WatchlistDetailState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.contentLayout.visibility = View.GONE
                    }
                    
                    is WatchlistDetailState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        binding.contentLayout.visibility = View.VISIBLE
                        
                        val item = state.item
                        
                        // Datos básicos
                        binding.tvTicker.text = item.ticker
                        binding.tvExchange.text = item.exchange ?: "N/A"
                        binding.tvCurrentPrice.text = "$${item.currentPrice ?: "N/A"}"
                        binding.tvNotes.text = item.notes ?: "Sin notas"
                        
                        // Parámetros objetivo
                        binding.tvTargetPrice.text = item.targetPrice?.let { "$$it" } ?: "N/A"
                        binding.tvTargetPfcf.text = item.targetPfcf?.toString() ?: "N/A"
                        
                        // Métricas calculadas
                        binding.tvActualPfcf.text = item.actualPfcf?.toString() ?: "N/A"
                        binding.tvFcfYield.text = item.fcfYield?.let { "${it}%" } ?: "N/A"
                        binding.tvDcfFairValue.text = item.dcfFairValue?.let { "$$it" } ?: "N/A"
                        binding.tvMarginOfSafety.text = item.marginOfSafety?.let { "${it}%" } ?: "N/A"
                        binding.tvEstimatedIRR.text = item.estimatedIRR?.let { "${it}%" } ?: "N/A"
                        binding.tvEstimatedROI.text = item.estimatedROI?.let { "${it}%" } ?: "N/A"
                        binding.tvPaybackPeriod.text = item.paybackPeriod?.let { "$it años" } ?: "N/A"
                        
                        // Indicador de valoración
                        if (item.undervalued == true) {
                            binding.tvUndervalued.text = "✅ INFRAVALORADA"
                            binding.tvUndervalued.setTextColor(Color.GREEN)
                        } else {
                            binding.tvUndervalued.text = "⚠️ SOBREVALORADA"
                            binding.tvUndervalued.setTextColor(Color.RED)
                        }
                    }
                    
                    is WatchlistDetailState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
```

### 3A. Crear Item - MODO SIMPLE con Búsqueda (Recomendado)

**Nuevo**: Crear item con búsqueda flexible de tickers y carga automática de datos

```kotlin
package com.yourcompany.divtracker.ui.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.yourcompany.divtracker.R
import com.yourcompany.divtracker.data.model.TickerSearchResult
import com.yourcompany.divtracker.data.model.WatchlistItemRequest
import com.yourcompany.divtracker.databinding.FragmentQuickAddBinding
import com.yourcompany.divtracker.ui.adapter.TickerSearchAdapter
import com.yourcompany.divtracker.ui.viewmodel.TickerSearchState
import com.yourcompany.divtracker.ui.viewmodel.WatchlistOperationState
import com.yourcompany.divtracker.ui.viewmodel.WatchlistViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class QuickAddWatchlistFragment : Fragment(R.layout.fragment_quick_add) {
    
    private var _binding: FragmentQuickAddBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: WatchlistViewModel by viewModels()
    private lateinit var searchAdapter: TickerSearchAdapter
    private var searchJob: Job? = null
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentQuickAddBinding.bind(view)
        
        setupSearchRecyclerView()
        setupSearchInput()
        observeSearchState()
        observeOperationState()
    }
    
    private fun setupSearchRecyclerView() {
        searchAdapter = TickerSearchAdapter { tickerResult ->
            onTickerSelected(tickerResult)
        }
        
        binding.recyclerSearch.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = searchAdapter
        }
    }
    
    private fun setupSearchInput() {
        binding.etTicker.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                
                // Debounce: esperar 300ms después de que el usuario deje de escribir
                searchJob?.cancel()
                
                if (query.length >= 2) {
                    searchJob = lifecycleScope.launch {
                        delay(300) // Debounce time
                        viewModel.searchTickers(query)
                    }
                } else {
                    viewModel.clearSearch()
                }
            }
        })
    }
    
    private fun observeSearchState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.searchState.collectLatest { state ->
                when (state) {
                    is TickerSearchState.Idle -> {
                        binding.recyclerSearch.isVisible = false
                        binding.searchProgress.isVisible = false
                        binding.tvSearchEmpty.isVisible = false
                    }
                    
                    is TickerSearchState.Loading -> {
                        binding.searchProgress.isVisible = true
                        binding.recyclerSearch.isVisible = false
                        binding.tvSearchEmpty.isVisible = false
                    }
                    
                    is TickerSearchState.Success -> {
                        binding.searchProgress.isVisible = false
                        
                        if (state.results.isEmpty()) {
                            binding.recyclerSearch.isVisible = false
                            binding.tvSearchEmpty.isVisible = true
                            binding.tvSearchEmpty.text = "No se encontraron resultados"
                        } else {
                            binding.recyclerSearch.isVisible = true
                            binding.tvSearchEmpty.isVisible = false
                            searchAdapter.submitList(state.results)
                        }
                    }
                    
                    is TickerSearchState.Error -> {
                        binding.searchProgress.isVisible = false
                        binding.recyclerSearch.isVisible = false
                        binding.tvSearchEmpty.isVisible = true
                        binding.tvSearchEmpty.text = "Error: ${state.message}"
                    }
                }
            }
        }
    }
    
    private fun onTickerSelected(tickerResult: TickerSearchResult) {
        // Autocompletar el campo con el ticker seleccionado
        binding.etTicker.setText(tickerResult.symbol)
        binding.etTicker.setSelection(tickerResult.symbol.length)
        
        // Mostrar info del ticker seleccionado
        binding.tvTickerInfo.isVisible = true
        binding.tvTickerInfo.text = "${tickerResult.description}\n${tickerResult.exchange} • ${tickerResult.type}"
        
        // Ocultar resultados de búsqueda
        viewModel.clearSearch()
        
        // Mostrar botón de añadir
        binding.btnQuickAdd.isEnabled = true
    }
    
    private fun quickAddItem() {
        val ticker = binding.etTicker.text.toString().trim()
        
        if (ticker.isBlank()) {
            Toast.makeText(requireContext(), "Selecciona un ticker", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Crear item SOLO con ticker
        // El backend cargará precio, FCF y calculará métricas automáticamente
        val request = WatchlistItemRequest(
            ticker = ticker
            // Sin targetPrice ni targetPfcf
            // Finnhub los calculará automáticamente
        )
        
        viewModel.createItem(request)
    }
    
    private fun observeOperationState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.operationState.collectLatest { state ->
                when (state) {
                    is WatchlistOperationState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.btnQuickAdd.isEnabled = false
                    }
                    
                    is WatchlistOperationState.Created -> {
                        binding.progressBar.visibility = View.GONE
                        val item = state.item
                        Toast.makeText(
                            requireContext(),
                            "✅ ${item.ticker} añadido\n" +
                            "Precio: $${item.currentPrice}\n" +
                            "P/FCF: ${item.actualPfcf}\n" +
                            "Estado: ${if (item.undervalued == true) "Infravalorada" else "Sobrevalorada"}",
                            Toast.LENGTH_LONG
                        ).show()
                        findNavController().popBackStack()
                    }
                    
                    is WatchlistOperationState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnQuickAdd.isEnabled = true
                        
                        // Manejar error de datos no disponibles
                        val errorMsg = if (state.message.contains("datos de mercado")) {
                            "No se encontraron datos para ${binding.etTicker.text}.\n" +
                            "Intenta con otro ticker o añade valores manualmente."
                        } else {
                            state.message
                        }
                        
                        Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
                    }
                    
                    else -> {}
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
```

### 3B. Activity/Fragment - Crear/Editar Item COMPLETO (Modo Manual)

```kotlin
package com.yourcompany.divtracker.ui.fragment

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.yourcompany.divtracker.R
import com.yourcompany.divtracker.data.model.WatchlistItemRequest
import com.yourcompany.divtracker.databinding.FragmentCreateWatchlistBinding
import com.yourcompany.divtracker.ui.viewmodel.WatchlistOperationState
import com.yourcompany.divtracker.ui.viewmodel.WatchlistViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.math.BigDecimal

class CreateWatchlistFragment : Fragment(R.layout.fragment_create_watchlist) {
    
    private var _binding: FragmentCreateWatchlistBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: WatchlistViewModel by viewModels()
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCreateWatchlistBinding.bind(view)
        
        observeOperationState()
        
        binding.btnCreate.setOnClickListener {
            createItem()
        }
    }
    
    private fun createItem() {
        val ticker = binding.etTicker.text.toString().trim()
        val exchange = binding.etExchange.text.toString().trim().takeIf { it.isNotEmpty() }
        val targetPriceStr = binding.etTargetPrice.text.toString()
        val targetPfcfStr = binding.etTargetPfcf.text.toString()
        val notes = binding.etNotes.text.toString().trim().takeIf { it.isNotEmpty() }
        
        // Validaciones básicas
        if (ticker.isBlank()) {
            Toast.makeText(requireContext(), "El ticker es obligatorio", Toast.LENGTH_SHORT).show()
            return
        }
        
        val targetPrice = targetPriceStr.toBigDecimalOrNull()
        val targetPfcf = targetPfcfStr.toBigDecimalOrNull()
        
        if (targetPrice == null && targetPfcf == null) {
            Toast.makeText(
                requireContext(),
                "Debes especificar al menos precio objetivo o P/FCF objetivo",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        // Parámetros avanzados (opcionales)
        val growthRateStr = binding.etGrowthRate.text.toString()
        val horizonStr = binding.etHorizon.text.toString()
        val discountRateStr = binding.etDiscountRate.text.toString()
        
        val request = WatchlistItemRequest(
            ticker = ticker,
            exchange = exchange,
            targetPrice = targetPrice,
            targetPfcf = targetPfcf,
            notifyWhenBelowPrice = binding.switchNotify.isChecked,
            notes = notes,
            estimatedFcfGrowthRate = growthRateStr.toBigDecimalOrNull(),
            investmentHorizonYears = horizonStr.toIntOrNull(),
            discountRate = discountRateStr.toBigDecimalOrNull()
        )
        
        viewModel.createItem(request)
    }
    
    private fun observeOperationState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.operationState.collectLatest { state ->
                when (state) {
                    is WatchlistOperationState.Idle -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnCreate.isEnabled = true
                    }
                    
                    is WatchlistOperationState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.btnCreate.isEnabled = false
                    }
                    
                    is WatchlistOperationState.Created -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(
                            requireContext(),
                            "✅ Item creado: ${state.item.ticker}",
                            Toast.LENGTH_SHORT
                        ).show()
                        // Volver atrás
                        findNavController().popBackStack()
                    }
                    
                    is WatchlistOperationState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnCreate.isEnabled = true
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    }
                    
                    else -> { /* No hacer nada para Updated y Deleted */ }
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
```

---

## Paginación

### Implementación de Scroll Infinito

```kotlin
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class WatchlistFragment : Fragment() {
    
    private var currentPage = 0
    private var isLoading = false
    private var isLastPage = false
    
    private fun setupPagination() {
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                
                if (!isLoading && !isLastPage) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                        && firstVisibleItemPosition >= 0) {
                        // Cargar siguiente página
                        loadNextPage()
                    }
                }
            }
        })
    }
    
    private fun loadNextPage() {
        currentPage++
        viewModel.loadWatchlist(page = currentPage, size = 20)
    }
    
    private fun observeListState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.listState.collectLatest { state ->
                when (state) {
                    is WatchlistListState.Loading -> {
                        isLoading = true
                    }
                    
                    is WatchlistListState.Success -> {
                        isLoading = false
                        isLastPage = state.watchlistPage.last
                        
                        // Agregar items a la lista existente (no reemplazar)
                        val currentList = adapter.currentList.toMutableList()
                        currentList.addAll(state.watchlistPage.content)
                        adapter.submitList(currentList)
                    }
                    
                    is WatchlistListState.Error -> {
                        isLoading = false
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    }
                    
                    else -> {
                        isLoading = false
                    }
                }
            }
        }
    }
}
```

---

## Manejo de Errores

### Errores HTTP Comunes

```kotlin
// 401 Unauthorized
"No autenticado. Inicia sesión nuevamente"
→ Redirigir a pantalla de login

// 404 Not Found
"Item no encontrado"
→ El item fue eliminado o no existe

// 409 Conflict
"El ticker ya existe en tu watchlist"
→ Ya tienes ese ticker, no puedes duplicarlo

// 400 Bad Request
"Datos inválidos. Verifica los campos"
→ Validaciones fallidas (ticker vacío, valores negativos, etc.)

// 500 Internal Server Error
"Error en el servidor. Intenta más tarde"
→ Error del backend
```

### ErrorResponse Model

```kotlin
data class ErrorResponse(
    @SerializedName("timestamp")
    val timestamp: String,
    
    @SerializedName("status")
    val status: Int,
    
    @SerializedName("error")
    val error: String,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("path")
    val path: String
)
```

---

## Testing

### Datos de Prueba

```kotlin
object TestData {
    
    // Ejemplos de búsqueda de tickers
    val searchQueryExamples = listOf(
        "Apple",      // Búsqueda por nombre
        "AAPL",       // Búsqueda por símbolo
        "micro",      // Búsqueda parcial
        "tesla",      // Búsqueda insensible a mayúsculas
        "MSFT"        // Símbolo exacto
    )
    
    // Resultado de búsqueda de ejemplo
    val sampleSearchResult = TickerSearchResult(
        symbol = "AAPL",
        description = "Apple Inc",
        type = "Common Stock",
        exchange = "NASDAQ",
        currency = "USD",
        figi = "BBG000B9XRY4"
    )
    
    // Request válido - SOLO TICKER (requiere Finnhub habilitado)
    val validRequestAutomatic = WatchlistItemRequest(
        ticker = "AAPL"
        // Sin targetPrice ni targetPfcf
        // Backend los calculará automáticamente
    )
    
    // Request válido - mínimo con valores manuales
    val validRequestMinimal = WatchlistItemRequest(
        ticker = "AAPL",
        targetPrice = BigDecimal("150.00")
    )
    
    // Request válido - completo
    val validRequestComplete = WatchlistItemRequest(
        ticker = "MSFT",
        exchange = "NASDAQ",
        targetPrice = BigDecimal("350.00"),
        targetPfcf = BigDecimal("20.0"),
        notifyWhenBelowPrice = true,
        notes = "Microsoft - Software líder",
        estimatedFcfGrowthRate = BigDecimal("0.08"),
        investmentHorizonYears = 5,
        discountRate = BigDecimal("0.10")
    )
    
    // Request inválido - sin targets
    val invalidRequestNoTargets = WatchlistItemRequest(
        ticker = "GOOGL",
        exchange = "NASDAQ"
        // No tiene targetPrice ni targetPfcf → ERROR
    )
    
    // Request inválido - ticker vacío
    val invalidRequestEmptyTicker = WatchlistItemRequest(
        ticker = "",
        targetPrice = BigDecimal("100.00")
        // ticker vacío → ERROR
    )
    
    // Response de ejemplo
    val sampleResponse = WatchlistItemResponse(
        id = UUID.randomUUID(),
        userId = UUID.randomUUID(),
        ticker = "AAPL",
        exchange = "NASDAQ",
        targetPrice = BigDecimal("150.00"),
        targetPfcf = BigDecimal("15.0"),
        notifyWhenBelowPrice = false,
        notes = "Apple Inc.",
        currentPrice = BigDecimal("172.15"),
        freeCashFlowPerShare = BigDecimal("11.45"),
        actualPfcf = BigDecimal("15.03"),
        fairPriceByPfcf = BigDecimal("180.00"),
        discountToFairPrice = BigDecimal("0.12"),
        deviationFromTargetPrice = BigDecimal("-0.05"),
        undervalued = false,
        estimatedFcfGrowthRate = BigDecimal("0.08"),
        investmentHorizonYears = 5,
        discountRate = BigDecimal("0.10"),
        dcfFairValue = BigDecimal("195.50"),
        fcfYield = BigDecimal("6.65"),
        marginOfSafety = BigDecimal("25.00"),
        paybackPeriod = BigDecimal("7.2"),
        estimatedROI = BigDecimal("85.50"),
        estimatedIRR = BigDecimal("12.50"),
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )
}
```

### Unit Test de ViewModel

```kotlin
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@ExperimentalCoroutinesApi
class WatchlistViewModelTest {
    
    @Test
    fun `loadWatchlist emite Loading y luego Success`() = runTest {
        // Arrange
        val mockRepository = mockk<WatchlistRepository>()
        val viewModel = WatchlistViewModel(mockRepository)
        val mockPage = WatchlistPage(/* ... */)
        
        coEvery { mockRepository.listItems(any(), any(), any(), any()) } returns Result.success(mockPage)
        
        // Act
        viewModel.loadWatchlist()
        
        // Assert
        assertTrue(viewModel.listState.value is WatchlistListState.Loading)
        
        advanceUntilIdle() // Espera a que termine la coroutine
        
        assertTrue(viewModel.listState.value is WatchlistListState.Success)
    }
    
    @Test
    fun `createItem con datos válidos emite Created`() = runTest {
        val mockRepository = mockk<WatchlistRepository>()
        val viewModel = WatchlistViewModel(mockRepository)
        val request = TestData.validRequestMinimal
        val mockResponse = TestData.sampleResponse
        
        coEvery { mockRepository.createItem(request) } returns Result.success(mockResponse)
        
        viewModel.createItem(request)
        advanceUntilIdle()
        
        assertTrue(viewModel.operationState.value is WatchlistOperationState.Created)
    }
}
```

---

## Dependencias

### build.gradle (Module: app)

```gradle
dependencies {
    // Retrofit
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    
    // OkHttp (para interceptors de autenticación)
    implementation 'com.squareup.okhttp3:okhttp:4.11.0'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.11.0'
    
    // Gson
    implementation 'com.google.code.gson:gson:2.10.1'
    
    // Coroutines
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3'
    
    // ViewModel y LiveData
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2'
    implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.6.2'
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.6.2'
    
    // Fragment KTX
    implementation 'androidx.fragment:fragment-ktx:1.6.1'
    
    // RecyclerView
    implementation 'androidx.recyclerview:recyclerview:1.3.1'
    
    // SwipeRefreshLayout
    implementation 'androidx.swiperefreshlayout:swiperefreshlayout:1.1.0'
    
    // Navigation (si usas Navigation Component)
    implementation 'androidx.navigation:navigation-fragment-ktx:2.7.3'
    implementation 'androidx.navigation:navigation-ui-ktx:2.7.3'
    
    // Testing
    testImplementation 'junit:junit:4.13.2'
    testImplementation 'org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3'
    testImplementation 'io.mockk:mockk:1.13.7'
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
}
```

---

## URLs de Entorno

### Configuración en RetrofitClient

```kotlin
object ApiConfig {
    
    // Producción AWS
    const val BASE_URL_PRODUCTION = "http://divtracker-prod.eba-rghuxgtw.eu-west-1.elasticbeanstalk.com/"
    
    // Local (emulador Android)
    const val BASE_URL_LOCAL_EMULATOR = "http://10.0.2.2:8080/"
    
    // Local (dispositivo físico - cambia por tu IP)
    const val BASE_URL_LOCAL_DEVICE = "http://192.168.1.XXX:8080/"
    
    // Selección automática según BuildConfig
    val BASE_URL: String
        get() = if (BuildConfig.DEBUG) {
            BASE_URL_LOCAL_EMULATOR
        } else {
            BASE_URL_PRODUCTION
        }
}
```

### Actualizar RetrofitClient

```kotlin
object RetrofitClient {
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(TokenManager))
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(ApiConfig.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val watchlistApiService: WatchlistApiService by lazy {
        retrofit.create(WatchlistApiService::class.java)
    }
}
```

---

## Checklist de Implementación

### ✅ Backend - Ya implementado

- [x] WatchlistController con todos los endpoints
- [x] WatchlistService con lógica de negocio
- [x] WatchlistItemRepository con queries
- [x] FinancialMetricsService para cálculos DCF/TIR/etc
- [x] Autenticación JWT funcionando
- [x] Base de datos PostgreSQL configurada
- [x] Despliegue en AWS Elastic Beanstalk

### 📱 Android - Por implementar

- [ ] **Data Models**
  - [ ] WatchlistItemRequest
  - [ ] WatchlistItemResponse
  - [ ] WatchlistPage
  - [ ] ErrorResponse

- [ ] **API Layer**
  - [ ] WatchlistApiService (Retrofit interface)
  - [ ] AuthInterceptor (añadir token JWT)
  - [ ] RetrofitClient configurado

- [ ] **Repository Layer**
  - [ ] WatchlistRepository con métodos CRUD

- [ ] **ViewModel Layer**
  - [ ] WatchlistViewModel
  - [ ] UI States (ListState, DetailState, OperationState)

- [ ] **UI Layer**
  - [ ] WatchlistFragment (lista con paginación)
  - [ ] WatchlistDetailFragment (detalle completo)
  - [ ] CreateWatchlistFragment (crear/editar)
  - [ ] WatchlistAdapter (RecyclerView)
  - [ ] Layouts XML

- [ ] **Features**
  - [ ] ⚡ Quick Add: Crear item solo con ticker (modo automático)
  - [ ] 📝 Formulario completo: Crear item con valores manuales
  - [ ] Pull to refresh
  - [ ] Scroll infinito (paginación)
  - [ ] Búsqueda/filtrado
  - [ ] Ordenamiento (por fecha, ticker, precio, etc.)
  - [ ] Indicadores visuales (infravalorada/sobrevalorada)
  - [ ] Badge/chip mostrando "Auto" vs "Manual" según cómo se creó

- [ ] **Testing**
  - [ ] Unit tests para ViewModel
  - [ ] Unit tests para Repository
  - [ ] UI tests para flujos críticos

---

## 📚 Recursos Adicionales

### Documentación del Backend

- **Swagger UI (Local)**: http://localhost:8080/swagger-ui.html
- **Swagger UI (Producción)**: http://divtracker-prod.eba-rghuxgtw.eu-west-1.elasticbeanstalk.com/swagger-ui.html
- **Guía de Autenticación**: Ver `ANDROID_AUTH_GUIDE.md`
- **Documentación de API**: Ver `WATCHLIST_API.md`

### Base de Datos (Referencia)

**RDS PostgreSQL**:
- Host: `divtracker-prod-db.ctkc6ukqmzpt.eu-west-1.rds.amazonaws.com`
- Puerto: `5432`
- Base de datos: `divtracker`
- Usuario: `divtracker`
- Password: `oUHzpG6_o,z0v8G,C5bhcdbzqhOqYQ`

### Métricas Financieras

El backend calcula automáticamente:

1. **DCF Fair Value**: Valor intrínseco usando flujo de caja descontado
   - Formula: Suma del valor presente de FCF proyectados + valor terminal
   
2. **TIR (IRR)**: Tasa interna de retorno
   - Formula: Tasa que iguala VPN a cero
   
3. **FCF Yield**: Rendimiento del flujo de caja libre
   - Formula: (FCF por acción / Precio actual) × 100
   
4. **Margen de Seguridad**: Diferencia entre precio y valor
   - Formula: ((DCF Fair Value - Precio actual) / DCF Fair Value) × 100
   
5. **Payback Period**: Años para recuperar inversión
   - Formula: Inversión inicial / FCF anual promedio
   
6. **ROI Estimado**: Retorno de inversión proyectado
   - Formula: ((Valor futuro - Inversión) / Inversión) × 100

---

## 💡 Recomendaciones de UX

### Pantalla de Creación Simple vs Completa

**Opción 1: Dos botones en la lista**
```
[+ Añadir Rápido]  [⚙️ Añadir Avanzado]
```

**Opción 2: Modal con opción**
```
Click en [+] → Diálogo:
"¿Cómo quieres añadir?"
[ Rápido (solo ticker) ]
[ Avanzado (configuración completa) ]
```

**Opción 3: Formulario progresivo**
```
Paso 1: Ticker (obligatorio)
[Continuar] → Carga datos automáticamente
Paso 2: ¿Quieres personalizar? [Sí] [No, usar automático]
```

### Adapter para Búsqueda de Tickers

```kotlin
package com.yourcompany.divtracker.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yourcompany.divtracker.data.model.TickerSearchResult
import com.yourcompany.divtracker.databinding.ItemTickerSearchBinding

class TickerSearchAdapter(
    private val onTickerClick: (TickerSearchResult) -> Unit
) : ListAdapter<TickerSearchResult, TickerSearchAdapter.ViewHolder>(DiffCallback) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTickerSearchBinding.inflate(
            LayoutInflater.from(parent.context), 
            parent, 
            false
        )
        return ViewHolder(binding, onTickerClick)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class ViewHolder(
        private val binding: ItemTickerSearchBinding,
        private val onTickerClick: (TickerSearchResult) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(result: TickerSearchResult) {
            binding.tvSymbol.text = result.symbol
            binding.tvDescription.text = result.description
            binding.tvExchange.text = buildString {
                append(result.exchange ?: "")
                if (result.type != null) {
                    if (isNotEmpty()) append(" • ")
                    append(result.type)
                }
            }
            
            binding.root.setOnClickListener {
                onTickerClick(result)
            }
        }
    }
    
    private object DiffCallback : DiffUtil.ItemCallback<TickerSearchResult>() {
        override fun areItemsTheSame(
            oldItem: TickerSearchResult, 
            newItem: TickerSearchResult
        ): Boolean = oldItem.symbol == newItem.symbol
        
        override fun areContentsTheSame(
            oldItem: TickerSearchResult, 
            newItem: TickerSearchResult
        ): Boolean = oldItem == newItem
    }
}
```

### Layout XML para Item de Búsqueda

**item_ticker_search.xml**:
```xml
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.material.card.MaterialCardView 
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_margin="4dp"
    app:cardElevation="2dp"
    app:cardCornerRadius="8dp">
    
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="12dp">
        
        <TextView
            android:id="@+id/tvSymbol"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textSize="16sp"
            android:textStyle="bold"
            android:textColor="?attr/colorPrimary"
            tools:text="AAPL" />
        
        <TextView
            android:id="@+id/tvDescription"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="4dp"
            android:textSize="14sp"
            tools:text="Apple Inc" />
        
        <TextView
            android:id="@+id/tvExchange"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="2dp"
            android:textSize="12sp"
            android:textColor="?android:attr/textColorSecondary"
            tools:text="NASDAQ • Common Stock" />
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

### Indicadores Visuales

```kotlin
// Badge en cada item de la lista
if (item.targetPfcf != null && item.targetPrice == null) {
    Badge("AUTO", color = Color.Blue)
} else {
    Badge("MANUAL", color = Color.Gray)
}
```

### Feedback al Usuario

```kotlin
// Mostrar shimmer/loading mientras se obtienen datos
if (state is Loading && isAutoMode) {
    Text("Obteniendo datos de mercado...")
    LinearProgressIndicator()
}

// Mostrar resumen al crear
if (state is Created) {
    SuccessDialog(
        ticker = item.ticker,
        price = item.currentPrice,
        pfcf = item.actualPfcf,
        status = if (item.undervalued) "Infravalorada" else "Sobrevalorada"
    )
}
```

---

## 🎯 Próximos Pasos

1. **Implementar data models** en Android
2. **Configurar Retrofit** con interceptor de autenticación
3. **Crear Repository** con manejo de errores
4. **Implementar ViewModel** con StateFlow
5. **Diseñar UI** para lista, detalle y formularios
6. **🆕 Implementar Quick Add** (solo ticker) como opción principal
7. **🆕 Implementar formulario avanzado** como opción secundaria
8. **Añadir paginación** y pull-to-refresh
9. **Implementar búsqueda** y ordenamiento
10. **Añadir tests** unitarios y de integración

---

## 🔀 Modos de Creación de Items

### Modo 1: Automático (Solo Ticker) ⚡ RECOMENDADO

**Ventajas:**
- ✅ Más rápido para el usuario
- ✅ Datos reales del mercado
- ✅ Cálculos automáticos de métricas
- ✅ Menos campos en el formulario

**Requisito:** Finnhub API configurada en backend

```kotlin
val request = WatchlistItemRequest(
    ticker = "AAPL",
    notes = "Apple Inc."  // Opcional
)
```

**Response esperado:**
```json
{
  "id": "...",
  "ticker": "AAPL",
  "targetPfcf": 25.5,  // ← Calculado automáticamente
  "currentPrice": 172.15,  // ← Desde Finnhub
  "freeCashFlowPerShare": 6.75,  // ← Desde Finnhub
  "actualPfcf": 25.5,
  "dcfFairValue": 195.50,
  "undervalued": false,
  ...
}
```

### Modo 2: Manual (Con Valores Específicos)

**Ventajas:**
- ✅ Control total sobre los parámetros
- ✅ Funciona sin Finnhub
- ✅ Análisis personalizado

```kotlin
val request = WatchlistItemRequest(
    ticker = "MSFT",
    targetPrice = BigDecimal("350.00"),
    targetPfcf = BigDecimal("20.0"),
    estimatedFcfGrowthRate = BigDecimal("0.08"),
    investmentHorizonYears = 5,
    discountRate = BigDecimal("0.10"),
    notes = "Microsoft - Strong fundamentals"
)
```

### Modo 3: Híbrido (Ticker + Algunos Valores)

```kotlin
val request = WatchlistItemRequest(
    ticker = "GOOGL",
    targetPrice = BigDecimal("140.00"),  // Manual
    estimatedFcfGrowthRate = BigDecimal("0.10"),  // Manual
    investmentHorizonYears = 5  // Manual
    // El resto se calcula automáticamente
)
```

---

## 🆘 Troubleshooting

### Error: "No se pudieron obtener datos de mercado para [TICKER]"

**Causa**: Finnhub no tiene datos para ese ticker o la API key no está configurada.

**Solución**:
1. Verificar que el ticker es correcto (usa símbolos de Yahoo Finance / Finnhub)
2. Probar con otro ticker popular (AAPL, MSFT, GOOGL)
3. Usar modo manual: especificar `targetPrice` o `targetPfcf` manualmente

**Ejemplo de manejo en UI:**
```kotlin
if (error.message.contains("datos de mercado")) {
    // Mostrar diálogo para ingresar valores manualmente
    showManualInputDialog(ticker)
} else {
    Toast.makeText(context, error.message, Toast.LENGTH_LONG).show()
}
```

### Error: "No autenticado"

**Solución**: Verificar que el token JWT se envía en el header `Authorization: Bearer <token>`

```kotlin
// Verificar en AuthInterceptor
class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenManager.getToken()
        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
```

### Error: "El ticker ya existe"

**Solución**: No puedes tener el mismo ticker dos veces. Edita el existente en lugar de crear uno nuevo.

### Error: Network timeout

**Solución**: Aumentar timeout en OkHttpClient

```kotlin
.connectTimeout(30, TimeUnit.SECONDS)
.readTimeout(30, TimeUnit.SECONDS)
.writeTimeout(30, TimeUnit.SECONDS)
```

### Paginación no carga más páginas

**Solución**: Verificar que `isLastPage` se actualiza correctamente

```kotlin
isLastPage = state.watchlistPage.last
```

### Métricas muestran "N/A"

**Causa**: El backend necesita datos financieros de la empresa (FCF, price, etc.)

**Solución**: Verificar que el ticker existe y tiene datos en la API de Yahoo Finance

---

## 📝 Notas Finales

- **Token JWT**: Válido por 24 horas, renovar automáticamente
- **Ticker único**: No puedes tener el mismo ticker repetido
- **Parámetros opcionales**: `targetPrice`, `targetPfcf`, `growthRate`, etc. son opcionales, pero al menos uno de `targetPrice` o `targetPfcf` es requerido
- **Cálculos automáticos**: El backend calcula todas las métricas, solo envía los parámetros de entrada
- **Paginación**: Por defecto 20 items por página, máximo recomendado 100
- **Ordenamiento**: Soporta ordenamiento por cualquier campo (createdAt, ticker, currentPrice, etc.)

---

**¡Éxito con la implementación!** 🚀

Para más información, consulta:
- `ANDROID_AUTH_GUIDE.md` - Autenticación
- `WATCHLIST_API.md` - Documentación completa de API
- `SWAGGER_GUIDE.md` - Swagger UI
- Swagger UI interactivo en producción
