# 🧪 DivTracker API - HTTP Collection

Colección de requests HTTP para probar la API de DivTracker usando **REST Client** o **Thunder Client** en VS Code.

## 📦 Extensiones Compatibles

### REST Client (Recomendado)
```
ID: humao.rest-client
```
Instala desde VS Code: `Ctrl+Shift+X` → buscar "REST Client"

### Thunder Client
```
ID: rangav.vscode-thunder-client
```

## 📁 Estructura

```
http/
├── env.http          # Variables de entorno (baseUrl, tokens)
├── auth.http         # Autenticación (signup, login)
├── watchlist.http    # CRUD de watchlist
├── tickers.http      # Búsqueda de símbolos
├── devices.http      # FCM push notifications
├── admin.http        # Endpoints de administración
├── health.http       # Health checks y tests
├── webhooks.http     # Webhooks de Finnhub
└── README.md         # Esta guía
```

## 🚀 Cómo usar

### 1. Iniciar el servidor
```bash
make run-local
```

### 2. Verificar que funciona
Abre `health.http` y ejecuta **Health Check** (click en "Send Request")

### 3. Crear cuenta o login
Abre `auth.http`:
- Ejecuta **Signup** para crear cuenta
- O ejecuta **Login** si ya tienes cuenta

### 4. Copiar el token
De la respuesta del login, copia el valor de `token` y pégalo en:
- `env.http` → variable `@authToken`
- O directamente en cada archivo donde dice `YOUR_JWT_TOKEN_HERE`

### 5. ¡Listo para probar!
Ya puedes ejecutar cualquier request autenticado.

## 📝 Archivos

### `auth.http`
- **Signup**: Registro de nuevo usuario
- **Login**: Iniciar sesión (guarda token automáticamente)

### `watchlist.http`
- **List Items**: Listar watchlist paginada
- **Get Item by ID**: Ver detalles de un item
- **Create Item - Simple**: Solo ticker
- **Create Item - Advanced**: Con métricas DCF
- **Update Item**: Actualización parcial
- **Delete Item**: Eliminar

### `tickers.http`
- **Lookup Symbol**: Búsqueda exacta (autocompletado)
- **Search by Name**: Búsqueda fuzzy

### `devices.http`
- **Register Device**: Registrar para push notifications
- **List Devices**: Ver dispositivos del usuario
- **Delete Device**: Eliminar dispositivo

### `admin.http`
- **Refresh Fundamentals**: Actualizar datos de un ticker
- **Refresh Stale**: Actualizar datos antiguos (>24h)
- **Cleanup Old**: Limpiar datos viejos (>30 días)

### `health.http`
- **Health Check**: Estado del servidor
- **Info**: Información de la app
- **Metrics**: Métricas
- **Public/Protected**: Tests de autenticación

### `webhooks.http`
- **Finnhub Webhook**: Simular actualizaciones de precios

## 💡 Tips

### Variables dinámicas
REST Client soporta variables que se guardan de respuestas anteriores:
```http
# @name login
POST {{baseUrl}}/api/auth/login
...

### Usar token de la respuesta anterior
@authToken = {{login.response.body.token}}
```

### Comentarios
- `###` separa requests
- `#` es un comentario
- `// @name` nombra un request para referenciarlo

### Atajos en VS Code
- `Ctrl+Alt+R` (o `Cmd+Alt+R` en Mac): Ejecutar request
- `Ctrl+Alt+E`: Seleccionar entorno

## 🔗 Links

- [REST Client Docs](https://marketplace.visualstudio.com/items?itemName=humao.rest-client)
- [Thunder Client Docs](https://www.thunderclient.com/docs)
- [Swagger UI](http://localhost:8080/swagger-ui.html)

---

**Nota**: Esta colección es equivalente a `bruno-collection/` pero en formato `.http` estándar.
