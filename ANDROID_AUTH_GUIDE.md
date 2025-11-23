# 📱 Guía de Implementación Auth - Android

Esta guía contiene todos los modelos de datos y endpoints necesarios para implementar autenticación en tu app Android de DivTracker.

---

## 📦 1. Data Models

### SignupRequest.kt
```kotlin
// filepath: app/src/main/java/com/tuapp/divtracker/data/models/auth/SignupRequest.kt
package com.tuapp.divtracker.data.models.auth

import com.google.gson.annotations.SerializedName

data class SignupRequest(
    @SerializedName("email")
    val email: String,
    
    @SerializedName("password")
    val password: String,
    
    @SerializedName("firstName")
    val firstName: String,
    
    @SerializedName("lastName")
    val lastName: String
)

/**
 * Ejemplo de uso:
 * 
 * val request = SignupRequest(
 *     email = "john.doe@example.com",
 *     password = "SecurePass123!",
 *     firstName = "John",
 *     lastName = "Doe"
 * )
 */
```

### LoginRequest.kt
```kotlin
// filepath: app/src/main/java/com/tuapp/divtracker/data/models/auth/LoginRequest.kt
package com.tuapp.divtracker.data.models.auth

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("email")
    val email: String,
    
    @SerializedName("password")
    val password: String
)

/**
 * Ejemplo de uso:
 * 
 * val request = LoginRequest(
 *     email = "john.doe@example.com",
 *     password = "SecurePass123!"
 * )
 */
```

### AuthResponse.kt
```kotlin
// filepath: app/src/main/java/com/tuapp/divtracker/data/models/auth/AuthResponse.kt
package com.tuapp.divtracker.data.models.auth

import com.google.gson.annotations.SerializedName

data class AuthResponse(
    @SerializedName("token")
    val token: String,
    
    @SerializedName("type")
    val type: String, // Siempre "Bearer"
    
    @SerializedName("id")
    val id: String, // UUID del usuario
    
    @SerializedName("email")
    val email: String,
    
    @SerializedName("firstName")
    val firstName: String,
    
    @SerializedName("lastName")
    val lastName: String
)

/**
 * Ejemplo de respuesta exitosa del servidor:
 * 
 * {
 *   "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2huLmRvZUBleGFtcGxlLmNvbSIsImlhdCI6MTY5ODQzMjAwMCwiZXhwIjoxNjk4NTE4NDAwfQ.xxx",
 *   "type": "Bearer",
 *   "id": "123e4567-e89b-12d3-a456-426614174000",
 *   "email": "john.doe@example.com",
 *   "firstName": "John",
 *   "lastName": "Doe"
 * }
 */
```

### ErrorResponse.kt
```kotlin
// filepath: app/src/main/java/com/tuapp/divtracker/data/models/auth/ErrorResponse.kt
package com.tuapp.divtracker.data.models.auth

import com.google.gson.annotations.SerializedName

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

/**
 * Ejemplo de respuesta de error (400 Bad Request):
 * 
 * {
 *   "timestamp": "2025-11-23T10:30:00.000+00:00",
 *   "status": 400,
 *   "error": "Bad Request",
 *   "message": "Email already exists",
 *   "path": "/api/auth/signup"
 * }
 */
```

---

## 🌐 2. API Service (Retrofit)

### AuthApiService.kt
```kotlin
// filepath: app/src/main/java/com/tuapp/divtracker/data/api/AuthApiService.kt
package com.tuapp.divtracker.data.api

import com.tuapp.divtracker.data.models.auth.AuthResponse
import com.tuapp.divtracker.data.models.auth.LoginRequest
import com.tuapp.divtracker.data.models.auth.SignupRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    
    /**
     * Registro de nuevo usuario
     * 
     * Endpoint: POST /api/auth/signup
     * 
     * @param request Datos del usuario a registrar
     * @return AuthResponse con token JWT y datos del usuario
     * 
     * Códigos de respuesta:
     * - 201: Usuario creado exitosamente
     * - 400: Datos inválidos (email mal formado, password débil, campos vacíos)
     * - 409: Email ya existe en el sistema
     * 
     * Ejemplo de uso:
     * val response = authApiService.signup(
     *     SignupRequest(
     *         email = "john.doe@example.com",
     *         password = "SecurePass123!",
     *         firstName = "John",
     *         lastName = "Doe"
     *     )
     * )
     */
    @POST("api/auth/signup")
    suspend fun signup(@Body request: SignupRequest): Response<AuthResponse>
    
    /**
     * Login de usuario existente
     * 
     * Endpoint: POST /api/auth/login
     * 
     * @param request Credenciales del usuario (email y password)
     * @return AuthResponse con token JWT y datos del usuario
     * 
     * Códigos de respuesta:
     * - 200: Login exitoso
     * - 401: Credenciales inválidas (email o password incorrectos)
     * - 400: Datos mal formados
     * 
     * Ejemplo de uso:
     * val response = authApiService.login(
     *     LoginRequest(
     *         email = "john.doe@example.com",
     *         password = "SecurePass123!"
     *     )
     * )
     */
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>
}
```

---

## 🔧 3. Retrofit Configuration

### RetrofitClient.kt
```kotlin
// filepath: app/src/main/java/com/tuapp/divtracker/data/api/RetrofitClient.kt
package com.tuapp.divtracker.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    
    /**
     * URLs según entorno (ver TESTING_GUIDE.md)
     */
    
    // Para emulador Android (localhost se mapea a 10.0.2.2)
    private const val BASE_URL_LOCAL_EMULATOR = "http://10.0.2.2:8080/"
    
    // Para dispositivo físico conectado a la misma red Wi-Fi
    // Reemplaza con la IP de tu máquina: ifconfig | grep "inet "
    private const val BASE_URL_LOCAL_DEVICE = "http://192.168.1.XXX:8080/"
    
    // Servidor de producción en AWS
    private const val BASE_URL_PRODUCTION = "http://divtracker-prod.eba-rghuxgtw.eu-west-1.elasticbeanstalk.com/"
    
    // 🔧 Cambia esto según tu entorno actual
    private const val BASE_URL = BASE_URL_PRODUCTION // o BASE_URL_LOCAL_EMULATOR
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        // Cambia a Level.NONE en producción
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val authApiService: AuthApiService by lazy {
        retrofit.create(AuthApiService::class.java)
    }
}
```

---

## 💾 4. Token Manager (SharedPreferences)

### TokenManager.kt
```kotlin
// filepath: app/src/main/java/com/tuapp/divtracker/data/local/TokenManager.kt
package com.tuapp.divtracker.data.local

import android.content.Context
import android.content.SharedPreferences

class TokenManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val PREFS_NAME = "divtracker_prefs"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_FIRST_NAME = "user_first_name"
        private const val KEY_LAST_NAME = "user_last_name"
    }
    
    /**
     * Guarda la información de autenticación después de login/signup
     */
    fun saveAuthData(
        token: String,
        userId: String,
        email: String,
        firstName: String,
        lastName: String
    ) {
        prefs.edit().apply {
            putString(KEY_TOKEN, token)
            putString(KEY_USER_ID, userId)
            putString(KEY_EMAIL, email)
            putString(KEY_FIRST_NAME, firstName)
            putString(KEY_LAST_NAME, lastName)
            apply()
        }
    }
    
    /**
     * Obtiene el token JWT guardado
     * @return Token JWT o null si no existe
     */
    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }
    
    /**
     * Obtiene el ID del usuario
     */
    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }
    
    /**
     * Obtiene el email del usuario
     */
    fun getEmail(): String? {
        return prefs.getString(KEY_EMAIL, null)
    }
    
    /**
     * Obtiene el nombre del usuario
     */
    fun getFirstName(): String? {
        return prefs.getString(KEY_FIRST_NAME, null)
    }
    
    /**
     * Obtiene el apellido del usuario
     */
    fun getLastName(): String? {
        return prefs.getString(KEY_LAST_NAME, null)
    }
    
    /**
     * Verifica si el usuario está autenticado (tiene token)
     */
    fun isLoggedIn(): Boolean {
        return getToken() != null
    }
    
    /**
     * Cierra sesión eliminando todos los datos guardados
     */
    fun logout() {
        prefs.edit().clear().apply()
    }
    
    /**
     * Obtiene el header de autorización formateado
     * @return "Bearer {token}" o null si no hay token
     */
    fun getAuthorizationHeader(): String? {
        val token = getToken() ?: return null
        return "Bearer $token"
    }
}

/**
 * Ejemplo de uso:
 * 
 * // Después de login/signup exitoso:
 * val tokenManager = TokenManager(context)
 * tokenManager.saveAuthData(
 *     token = authResponse.token,
 *     userId = authResponse.id,
 *     email = authResponse.email,
 *     firstName = authResponse.firstName,
 *     lastName = authResponse.lastName
 * )
 * 
 * // Para verificar si está logueado:
 * if (tokenManager.isLoggedIn()) {
 *     // Usuario autenticado
 * }
 * 
 * // Para usar en requests protegidos:
 * val authHeader = tokenManager.getAuthorizationHeader()
 * // authHeader = "Bearer eyJhbGciOiJIUzI1NiJ9..."
 * 
 * // Para cerrar sesión:
 * tokenManager.logout()
 */
```

---

## 🗂️ 5. Repository Pattern

### AuthRepository.kt
```kotlin
// filepath: app/src/main/java/com/tuapp/divtracker/data/repository/AuthRepository.kt
package com.tuapp.divtracker.data.repository

import com.tuapp.divtracker.data.api.AuthApiService
import com.tuapp.divtracker.data.local.TokenManager
import com.tuapp.divtracker.data.models.auth.AuthResponse
import com.tuapp.divtracker.data.models.auth.LoginRequest
import com.tuapp.divtracker.data.models.auth.SignupRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class AuthRepository(
    private val authApiService: AuthApiService,
    private val tokenManager: TokenManager
) {
    
    /**
     * Registra un nuevo usuario
     * 
     * @return Result<AuthResponse> con el resultado de la operación
     */
    suspend fun signup(
        email: String,
        password: String,
        firstName: String,
        lastName: String
    ): Result<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            val response = authApiService.signup(
                SignupRequest(
                    email = email,
                    password = password,
                    firstName = firstName,
                    lastName = lastName
                )
            )
            
            handleAuthResponse(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Inicia sesión con email y password
     * 
     * @return Result<AuthResponse> con el resultado de la operación
     */
    suspend fun login(
        email: String,
        password: String
    ): Result<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            val response = authApiService.login(
                LoginRequest(
                    email = email,
                    password = password
                )
            )
            
            handleAuthResponse(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Cierra sesión eliminando el token guardado
     */
    fun logout() {
        tokenManager.logout()
    }
    
    /**
     * Verifica si el usuario está autenticado
     */
    fun isLoggedIn(): Boolean {
        return tokenManager.isLoggedIn()
    }
    
    /**
     * Maneja la respuesta de autenticación común para login/signup
     */
    private fun handleAuthResponse(response: Response<AuthResponse>): Result<AuthResponse> {
        return when {
            response.isSuccessful && response.body() != null -> {
                val authResponse = response.body()!!
                
                // Guardar los datos de autenticación
                tokenManager.saveAuthData(
                    token = authResponse.token,
                    userId = authResponse.id,
                    email = authResponse.email,
                    firstName = authResponse.firstName,
                    lastName = authResponse.lastName
                )
                
                Result.success(authResponse)
            }
            response.code() == 400 -> {
                Result.failure(Exception("Datos inválidos. Verifica los campos."))
            }
            response.code() == 401 -> {
                Result.failure(Exception("Email o contraseña incorrectos"))
            }
            response.code() == 409 -> {
                Result.failure(Exception("El email ya está registrado"))
            }
            else -> {
                Result.failure(Exception("Error del servidor: ${response.code()}"))
            }
        }
    }
}
```

---

## 🎨 6. ViewModel

### AuthViewModel.kt
```kotlin
// filepath: app/src/main/java/com/tuapp/divtracker/ui/auth/AuthViewModel.kt
package com.tuapp.divtracker.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuapp.divtracker.data.models.auth.AuthResponse
import com.tuapp.divtracker.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    /**
     * Registra un nuevo usuario
     */
    fun signup(
        email: String,
        password: String,
        firstName: String,
        lastName: String
    ) {
        // Validación básica
        if (email.isBlank() || password.isBlank() || 
            firstName.isBlank() || lastName.isBlank()) {
            _authState.value = AuthState.Error("Todos los campos son obligatorios")
            return
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _authState.value = AuthState.Error("Email inválido")
            return
        }
        
        if (password.length < 6) {
            _authState.value = AuthState.Error("La contraseña debe tener al menos 6 caracteres")
            return
        }
        
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            authRepository.signup(email, password, firstName, lastName)
                .onSuccess { authResponse ->
                    _authState.value = AuthState.Success(authResponse)
                }
                .onFailure { exception ->
                    _authState.value = AuthState.Error(
                        exception.message ?: "Error desconocido"
                    )
                }
        }
    }
    
    /**
     * Inicia sesión con email y password
     */
    fun login(email: String, password: String) {
        // Validación básica
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Email y contraseña son obligatorios")
            return
        }
        
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            authRepository.login(email, password)
                .onSuccess { authResponse ->
                    _authState.value = AuthState.Success(authResponse)
                }
                .onFailure { exception ->
                    _authState.value = AuthState.Error(
                        exception.message ?: "Error desconocido"
                    )
                }
        }
    }
    
    /**
     * Cierra sesión
     */
    fun logout() {
        authRepository.logout()
        _authState.value = AuthState.Idle
    }
    
    /**
     * Resetea el estado a Idle (útil para limpiar errores)
     */
    fun resetState() {
        _authState.value = AuthState.Idle
    }
    
    /**
     * Verifica si el usuario está autenticado
     */
    fun isLoggedIn(): Boolean {
        return authRepository.isLoggedIn()
    }
}

/**
 * Estados posibles de autenticación
 */
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val authResponse: AuthResponse) : AuthState()
    data class Error(val message: String) : AuthState()
}
```

---

## 🧪 7. Datos de Prueba

### TestData.kt
```kotlin
// filepath: app/src/main/java/com/tuapp/divtracker/data/models/auth/TestData.kt
package com.tuapp.divtracker.data.models.auth

/**
 * Datos de prueba para testing y debugging
 */
object TestData {
    
    // ✅ Usuario válido para signup
    val validSignupRequest = SignupRequest(
        email = "test@example.com",
        password = "password123",
        firstName = "John",
        lastName = "Doe"
    )
    
    // ✅ Usuario válido para login
    val validLoginRequest = LoginRequest(
        email = "test@example.com",
        password = "password123"
    )
    
    // ❌ Email duplicado (debería dar 409 Conflict)
    val duplicateEmailSignup = SignupRequest(
        email = "test@example.com", // Ya existe
        password = "password456",
        firstName = "Jane",
        lastName = "Smith"
    )
    
    // ❌ Credenciales inválidas (debería dar 401 Unauthorized)
    val invalidLogin = LoginRequest(
        email = "test@example.com",
        password = "wrongpassword"
    )
    
    // ❌ Email mal formado (debería dar 400 Bad Request)
    val invalidEmailSignup = SignupRequest(
        email = "notanemail",
        password = "password123",
        firstName = "John",
        lastName = "Doe"
    )
    
    // ❌ Password muy corto
    val shortPasswordSignup = SignupRequest(
        email = "test2@example.com",
        password = "123",
        firstName = "John",
        lastName = "Doe"
    )
    
    // ❌ Campos vacíos
    val emptyFieldsSignup = SignupRequest(
        email = "",
        password = "",
        firstName = "",
        lastName = ""
    )
}
```

---

## 📋 8. Configuración en build.gradle

### build.gradle (Module: app)
```gradle
dependencies {
    // Retrofit para networking
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    
    // OkHttp para logging
    implementation 'com.squareup.okhttp3:logging-interceptor:4.11.0'
    
    // Coroutines para operaciones asíncronas
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
    
    // ViewModel y LiveData
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2'
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.6.2'
    
    // Optional: Para usar DataStore en lugar de SharedPreferences
    // implementation 'androidx.datastore:datastore-preferences:1.0.0'
}
```

---

## 📊 9. Estructura de Respuestas

### ✅ Signup Exitoso (201 Created)
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwiaWF0IjoxNzAwMDAwMDAwLCJleHAiOjE3MDAwODY0MDB9.xxx",
  "type": "Bearer",
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "email": "test@example.com",
  "firstName": "John",
  "lastName": "Doe"
}
```

### ✅ Login Exitoso (200 OK)
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwiaWF0IjoxNzAwMDAwMDAwLCJleHAiOjE3MDAwODY0MDB9.xxx",
  "type": "Bearer",
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "email": "test@example.com",
  "firstName": "John",
  "lastName": "Doe"
}
```

### ❌ Email Duplicado (409 Conflict)
```json
{
  "timestamp": "2025-11-23T10:30:00.000+00:00",
  "status": 409,
  "error": "Conflict",
  "message": "Email already exists",
  "path": "/api/auth/signup"
}
```

### ❌ Credenciales Inválidas (401 Unauthorized)
```json
{
  "timestamp": "2025-11-23T10:30:00.000+00:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid credentials",
  "path": "/api/auth/login"
}
```

### ❌ Datos Inválidos (400 Bad Request)
```json
{
  "timestamp": "2025-11-23T10:30:00.000+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Email must be valid",
  "path": "/api/auth/signup"
}
```

---

## 🌍 10. URLs de los Endpoints

### Entorno Local (Desarrollo)

**Para Emulador Android:**
```
Base URL: http://10.0.2.2:8080/
Signup: http://10.0.2.2:8080/api/auth/signup
Login: http://10.0.2.2:8080/api/auth/login
```

**Para Dispositivo Físico (misma red Wi-Fi):**
```
Base URL: http://[TU_IP]:8080/
Signup: http://[TU_IP]:8080/api/auth/signup
Login: http://[TU_IP]:8080/api/auth/login

# Para obtener tu IP:
# macOS/Linux: ifconfig | grep "inet "
# Windows: ipconfig
```

### Entorno Producción (AWS)

```
Base URL: http://divtracker-prod.eba-rghuxgtw.eu-west-1.elasticbeanstalk.com/
Signup: http://divtracker-prod.eba-rghuxgtw.eu-west-1.elasticbeanstalk.com/api/auth/signup
Login: http://divtracker-prod.eba-rghuxgtw.eu-west-1.elasticbeanstalk.com/api/auth/login
```

---

## 🔐 11. Manejo del Token JWT

### ¿Qué es el Token?
El token JWT (JSON Web Token) es una cadena encriptada que identifica al usuario autenticado. Tiene una duración de **24 horas**.

### Formato del Token
```
Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwiaWF0IjoxNzAwMDAwMDAwLCJleHAiOjE3MDAwODY0MDB9.signature
```

### Cómo Usarlo en Requests Protegidos

```kotlin
// Agregar el token en el header Authorization
val authHeader = tokenManager.getAuthorizationHeader()
// authHeader = "Bearer eyJhbGciOiJIUzI1NiJ9..."

// Ejemplo con Retrofit:
@GET("api/v1/watchlist")
suspend fun getWatchlist(
    @Header("Authorization") authorization: String
): Response<List<WatchlistItem>>

// Uso:
val response = apiService.getWatchlist(authHeader!!)
```

### Expiración del Token

- **Duración**: 24 horas (86400000 ms)
- **Qué hacer cuando expira**: El servidor devolverá 401 Unauthorized
- **Solución**: Pedir al usuario que haga login nuevamente

---

## 📱 12. Ejemplo Completo de Uso en Activity/Fragment

### LoginActivity.kt (ejemplo simple)
```kotlin
class LoginActivity : AppCompatActivity() {
    
    private lateinit var viewModel: AuthViewModel
    private lateinit var tokenManager: TokenManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Inicializar TokenManager
        tokenManager = TokenManager(this)
        
        // Inicializar ViewModel
        val authRepository = AuthRepository(
            RetrofitClient.authApiService,
            tokenManager
        )
        viewModel = ViewModelProvider(
            this,
            AuthViewModelFactory(authRepository)
        )[AuthViewModel::class.java]
        
        // Observar estados de autenticación
        lifecycleScope.launch {
            viewModel.authState.collect { state ->
                when (state) {
                    is AuthState.Idle -> {
                        // Estado inicial
                    }
                    is AuthState.Loading -> {
                        // Mostrar loading
                        showLoading()
                    }
                    is AuthState.Success -> {
                        // Login exitoso, ir a MainActivity
                        hideLoading()
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    }
                    is AuthState.Error -> {
                        // Mostrar error
                        hideLoading()
                        showError(state.message)
                    }
                }
            }
        }
        
        // Click en botón de login
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()
            viewModel.login(email, password)
        }
        
        // Click en "Crear cuenta"
        tvSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }
}
```

---

## ✅ 13. Checklist de Implementación

- [ ] Agregar dependencias en `build.gradle`
- [ ] Crear modelos de datos (`SignupRequest`, `LoginRequest`, `AuthResponse`, `ErrorResponse`)
- [ ] Crear `AuthApiService` con Retrofit
- [ ] Configurar `RetrofitClient` con la URL correcta
- [ ] Implementar `TokenManager` para guardar/recuperar token
- [ ] Crear `AuthRepository`
- [ ] Crear `AuthViewModel` con estados
- [ ] Implementar UI de Login
- [ ] Implementar UI de Signup
- [ ] Manejar estados (Loading, Success, Error)
- [ ] Probar con datos de prueba (`TestData`)
- [ ] Verificar manejo de errores (401, 409, 400)
- [ ] Implementar logout
- [ ] Agregar interceptor para añadir token automáticamente en requests protegidos

---

## 🐛 14. Troubleshooting

### Error: "Unable to resolve host"
**Causa**: No puedes conectarte al servidor  
**Solución**:
- Verifica que el servidor esté corriendo
- Si usas emulador, usa `10.0.2.2` en lugar de `localhost`
- Si usas dispositivo físico, verifica que esté en la misma red Wi-Fi

### Error: "401 Unauthorized" en requests protegidos
**Causa**: Token inválido, expirado o no enviado  
**Solución**:
- Verifica que el token esté guardado: `tokenManager.getToken()`
- Verifica que estés enviando el header: `Authorization: Bearer {token}`
- Si expiró (24h), pide login nuevamente

### Error: "409 Conflict - Email already exists"
**Causa**: Intentas registrar un email que ya existe  
**Solución**: Usa otro email o haz login con el existente

### Error: "400 Bad Request"
**Causa**: Datos inválidos (email mal formado, password muy corto, campos vacíos)  
**Solución**: Valida los campos antes de enviar

---

## 🎯 15. Próximos Pasos

Una vez implementado el sistema de autenticación:

1. **Implementar Watchlist**: Usar el token para crear/leer/actualizar/eliminar items
2. **Refresh Token**: Implementar renovación automática del token
3. **Biometría**: Agregar autenticación con huella/Face ID
4. **OAuth2 Google**: Permitir login con cuenta de Google
5. **Remember Me**: Opción para mantener sesión iniciada
6. **Forgot Password**: Recuperación de contraseña (si implementas endpoint en backend)

---

¡Listo para implementar! 🚀
