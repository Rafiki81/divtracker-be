# 🧪 Guía de Pruebas - DivTracker API

Esta guía te ayudará a probar todos los endpoints de la API usando Swagger UI.

## 🚀 Iniciar la Aplicación

```bash
# Opción 1: Usando Make (recomendado)
make run-local

# Opción 2: Usando Maven directamente
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## 📝 Acceder a Swagger UI

Una vez iniciada la aplicación, abre tu navegador en:

**Swagger UI**: http://localhost:8080/swagger-ui.html

**OpenAPI JSON**: http://localhost:8080/api-docs

---

## 🔐 Flujo de Pruebas

### 1️⃣ Registro de Usuario (Signup)

1. Ve a la sección **"Autenticación"** en Swagger
2. Expande `POST /api/auth/signup`
3. Click en **"Try it out"**
4. Ingresa el JSON:

```json
{
  "email": "test@example.com",
  "password": "password123",
  "firstName": "John",
  "lastName": "Doe"
}
```

5. Click en **"Execute"**
6. **Copia el `token` de la respuesta** (lo necesitarás para los siguientes pasos)

**Respuesta esperada (200 OK)**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "email": "test@example.com",
  "firstName": "John",
  "lastName": "Doe"
}
```

---

### 2️⃣ Autenticar (Login)

1. Expande `POST /api/auth/login`
2. Click en **"Try it out"**
3. Ingresa:

```json
{
  "email": "test@example.com",
  "password": "password123"
}
```

4. Click en **"Execute"**
5. **Copia el nuevo `token`** si lo necesitas

---

### 3️⃣ Autorizar en Swagger

Para probar los endpoints protegidos, necesitas autenticarte en Swagger:

1. **Click en el botón "Authorize" 🔓** (arriba a la derecha en Swagger UI)
2. En el modal que aparece, ingresa:
   ```
   Bearer eyJhbGciOiJIUzI1NiJ9... (tu token completo)
   ```
3. Click en **"Authorize"**
4. Click en **"Close"**

✅ Ahora el candado debería aparecer cerrado 🔒

---

### 4️⃣ Crear Items en el Watchlist

1. Ve a la sección **"Watchlist"**
2. Expande `POST /api/v1/watchlist`
3. Click en **"Try it out"**
4. Ingresa:

```json
{
  "ticker": "AAPL",
  "exchange": "NASDAQ",
  "targetPrice": 150.50,
  "targetPfcf": 15.5,
  "notifyWhenBelowPrice": false,
  "notes": "Apple Inc. - Empresa tecnológica líder"
}
```

5. Click en **"Execute"**

**Respuesta esperada (201 Created)**:
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174001",
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "ticker": "AAPL",
  "exchange": "NASDAQ",
  "targetPrice": 150.50,
  "targetPfcf": 15.5,
  "notifyWhenBelowPrice": false,
  "notes": "Apple Inc. - Empresa tecnológica líder",
  "createdAt": "2025-10-19T19:30:00",
  "updatedAt": "2025-10-19T19:30:00"
}
```

#### Crea más items de ejemplo:

**Microsoft**:
```json
{
  "ticker": "MSFT",
  "exchange": "NASDAQ",
  "targetPrice": 330.00,
  "targetPfcf": 20.0,
  "notifyWhenBelowPrice": true,
  "notes": "Microsoft Corporation"
}
```

**Google (Alphabet)**:
```json
{
  "ticker": "GOOGL",
  "exchange": "NASDAQ",
  "targetPrice": 140.00,
  "targetPfcf": 18.0,
  "notifyWhenBelowPrice": false,
  "notes": "Alphabet Inc. - Google"
}
```

**Tesla**:
```json
{
  "ticker": "TSLA",
  "exchange": "NASDAQ",
  "targetPrice": 250.00,
  "targetPfcf": null,
  "notes": "Tesla Inc. - Vehículos eléctricos"
}
```

---

### 5️⃣ Listar Items del Watchlist

1. Expande `GET /api/v1/watchlist`
2. Click en **"Try it out"**
3. Ajusta los parámetros de paginación:
   - `page`: 0
   - `size`: 20
   - `sortBy`: createdAt
   - `direction`: DESC
4. Click en **"Execute"**

**Respuesta esperada (200 OK)**:
```json
{
  "content": [
    {
      "id": "...",
      "ticker": "TSLA",
      ...
    },
    {
      "id": "...",
      "ticker": "GOOGL",
      ...
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 4,
  "totalPages": 1
}
```

---

### 6️⃣ Obtener un Item Específico

1. Expande `GET /api/v1/watchlist/{id}`
2. Click en **"Try it out"**
3. Ingresa el `id` de un item (copia uno de la respuesta anterior)
4. Click en **"Execute"**

**Respuesta esperada (200 OK)**: Detalles del item

---

### 7️⃣ Actualizar un Item (PATCH)

1. Expande `PATCH /api/v1/watchlist/{id}`
2. Click en **"Try it out"**
3. Ingresa el `id` del item a actualizar
4. Modifica solo los campos que quieras cambiar:

```json
{
  "targetPrice": 160.00,
  "notes": "Apple Inc. - Precio actualizado"
}
```

5. Click en **"Execute"**

**Respuesta esperada (200 OK)**: Item actualizado

---

### 8️⃣ Eliminar un Item

1. Expande `DELETE /api/v1/watchlist/{id}`
2. Click en **"Try it out"**
3. Ingresa el `id` del item a eliminar
4. Click en **"Execute"**

**Respuesta esperada (204 No Content)**: Sin contenido

---

## 🧪 Casos de Prueba

### ✅ Validaciones

**1. Ticker duplicado** (debería fallar con 409 Conflict):
```json
{
  "ticker": "AAPL",
  "targetPrice": 150.00
}
```

**2. Sin targetPrice ni targetPfcf** (debería fallar con 400 Bad Request):
```json
{
  "ticker": "NFLX",
  "exchange": "NASDAQ"
}
```

**3. Email duplicado en signup** (debería fallar con 409 Conflict):
```json
{
  "email": "test@example.com",
  "password": "password123",
  "firstName": "Jane",
  "lastName": "Smith"
}
```

**4. Credenciales inválidas en login** (debería fallar con 401 Unauthorized):
```json
{
  "email": "test@example.com",
  "password": "wrongpassword"
}
```

---

### 🔒 Seguridad

**1. Acceder sin token** (debería fallar con 401):
- Cierra sesión en Swagger (click en "Authorize" y luego "Logout")
- Intenta hacer un GET a `/api/v1/watchlist`
- Debería devolver 401 Unauthorized

**2. Aislamiento por usuario**:
- Registra un segundo usuario
- Intenta acceder al ID de un item del primer usuario
- Debería devolver 404 Not Found

---

## 📊 Endpoints Públicos

Estos endpoints NO requieren autenticación:

- `GET /api/test/public` - Endpoint de prueba público

---

## 🛠️ Comandos útiles

```bash
# Ver logs en tiempo real
make docker-logs

# Parar la aplicación
Ctrl + C

# Parar Docker
make docker-down

# Limpiar todo y empezar de nuevo
make clean
docker compose down -v
make run-local
```

---

## 📝 Notas

- **JWT Expiration**: Los tokens expiran en 24 horas (86400000 ms)
- **Base de Datos**: Los datos persisten en el volumen Docker
- **Reset Database**: Elimina el volumen con `docker volume rm divtracker-be_postgres_data`
- **Flyway**: Las migraciones se ejecutan automáticamente al iniciar

---

## 🐛 Troubleshooting

### Error: "401 Unauthorized"
- Verifica que has autorizado correctamente en Swagger
- Asegúrate de incluir "Bearer " antes del token
- Verifica que el token no haya expirado

### Error: "409 Conflict - Ticker already exists"
- Ya existe un item con ese ticker para tu usuario
- Usa otro ticker o elimina el existente primero

### Error: "Cannot connect to database"
- Verifica que PostgreSQL esté corriendo: `docker ps`
- Reinicia Docker: `make docker-down && make docker-up`

### Error: "Port 8080 already in use"
- Mata el proceso: `lsof -ti:8080 | xargs kill -9`
- O usa otro puerto en `application-local.properties`

---

## 🎯 Próximos Pasos

Una vez que hayas probado todos los endpoints, puedes:

1. **Integrar con el Frontend**: Usa los endpoints desde React/Vue/Angular
2. **Agregar más validaciones**: Personaliza las reglas de negocio
3. **Implementar notificaciones**: Usa el campo `notifyWhenBelowPrice`
4. **Agregar análisis financiero**: Calcula P/FCF automáticamente
5. **OAuth2 con Google**: Configura las credenciales de Google

---

¡Feliz testing! 🚀
