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
    val targetPrice: BigDecimal? = null,  // Opcional, precio objetivo
    
    @SerializedName("targetPfcf")
    val targetPfcf: BigDecimal? = null,  // Opcional, P/FCF objetivo
    
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
    init {
        require(targetPrice != null || targetPfcf != null) {
            "Debe especificar al menos targetPrice o targetPfcf"
        }
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
    
    // Datos de mercado actuales
    @SerializedName("currentPrice")
    val currentPrice: BigDecimal?,
    
    @SerializedName("freeCashFlowPerShare")
    val freeCashFlowPerShare: BigDecimal?,
    
    @SerializedName("actualPfcf")
    val actualPfcf: BigDecimal?,
    
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

### 3. WatchlistPage

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
```

---

## Ejemplos de Uso

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

### 3. Activity/Fragment - Crear/Editar Item

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
    
    // Request válido - mínimo
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
  - [ ] Pull to refresh
  - [ ] Scroll infinito (paginación)
  - [ ] Búsqueda/filtrado
  - [ ] Ordenamiento (por fecha, ticker, precio, etc.)
  - [ ] Indicadores visuales (infravalorada/sobrevalorada)

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

## 🎯 Próximos Pasos

1. **Implementar data models** en Android
2. **Configurar Retrofit** con interceptor de autenticación
3. **Crear Repository** con manejo de errores
4. **Implementar ViewModel** con StateFlow
5. **Diseñar UI** para lista, detalle y formularios
6. **Añadir paginación** y pull-to-refresh
7. **Implementar búsqueda** y ordenamiento
8. **Añadir tests** unitarios y de integración

---

## 🆘 Troubleshooting

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
