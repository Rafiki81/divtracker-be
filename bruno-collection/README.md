# 🧪 DivTracker API - Colección de Bruno

Colección completa de requests para testear todos los endpoints de DivTracker API.

## 📦 Instalación de Bruno

Bruno es un cliente API open-source similar a Postman pero más ligero y basado en archivos.

### macOS
```bash
brew install bruno
```

### Descargar manualmente
https://www.usebruno.com/downloads

## 🚀 Cómo usar esta colección

### 1. Abrir Bruno
Inicia la aplicación Bruno en tu sistema.

### 2. Abrir Colección
1. Click en **"Open Collection"**
2. Navega a: `/Users/rafaelperezbeato/IdeaProjects/divtracker-be/bruno-collection`
3. Selecciona la carpeta completa

### 3. Configurar Entorno

La colección incluye dos entornos:

#### **Local** (desarrollo)
```
baseUrl: http://localhost:8080
authToken: (se guarda automáticamente después del login)
```

#### **AWS** (producción)
```
baseUrl: https://your-app.elasticbeanstalk.com
authToken: (se guarda automáticamente después del login)
```

Selecciona el entorno en el dropdown superior de Bruno.

### 4. Flujo de Trabajo Recomendado

#### Paso 1: Verificar servidor
```
Health → Health Check
```

#### Paso 2: Crear cuenta o iniciar sesión
```
Auth → Signup  (primera vez)
  o
Auth → Login   (cuenta existente)
```

El token JWT se guarda automáticamente en `{{authToken}}` 🎉

#### Paso 3: Probar autenticación
```
Test → Protected Endpoint
```

#### Paso 4: Gestionar Watchlist
```
Watchlist → Create Item - Simple    (crear empresa básica)
Watchlist → Create Item - Advanced  (crear con métricas avanzadas)
Watchlist → List Items              (ver todas las empresas)
Watchlist → Get Item by ID          (ver detalles de una empresa)
Watchlist → Update Item             (actualizar empresa)
Watchlist → Delete Item             (eliminar empresa)
```

#### Paso 5: Configurar Push Notifications (opcional)
```
Devices → Register Device   (registrar dispositivo para FCM)
Devices → List Devices      (ver dispositivos registrados)
Devices → Delete Device     (eliminar dispositivo)
```

## 📂 Estructura de la Colección

```
bruno-collection/
├── bruno.json                     # Configuración de la colección
├── environments/
│   ├── Local.bru                 # Entorno local
│   └── AWS.bru                   # Entorno AWS
├── Auth/
│   ├── Signup.bru                # Registro de usuario
│   └── Login.bru                 # Login
├── Test/
│   ├── Public Endpoint.bru       # Test público
│   └── Protected Endpoint.bru    # Test con JWT
├── Watchlist/
│   ├── List Items.bru            # Listar empresas (paginado)
│   ├── Get Item by ID.bru        # Ver detalles
│   ├── Create Item - Simple.bru  # Crear básico
│   ├── Create Item - Advanced.bru # Crear con análisis avanzado
│   ├── Update Item.bru           # Actualizar
│   └── Delete Item.bru           # Eliminar
├── Devices/                       # 🔔 Push Notifications (FCM)
│   ├── Register Device.bru       # Registrar dispositivo
│   ├── List Devices.bru          # Listar dispositivos
│   └── Delete Device.bru         # Eliminar dispositivo
├── Tickers/
│   ├── Lookup Symbol.bru         # Búsqueda exacta de símbolo
│   └── Search by Name.bru        # Búsqueda fuzzy por nombre
├── Fundamentals/
│   └── Refresh Fundamentals.bru  # Actualizar datos desde Finnhub
├── Admin/
│   ├── Refresh Stale Fundamentals.bru # Job manual de actualización
│   └── Cleanup Old Fundamentals.bru   # Job manual de limpieza
├── Health/
│   ├── Health Check.bru          # Estado del servidor
│   └── Info.bru                  # Info de la app
└── README.md                      # Esta guía
```

## 🎯 Ejemplos de Uso

### Crear empresa con análisis básico

```json
POST /api/v1/watchlist
{
  "ticker": "AAPL",
  "exchange": "NASDAQ",
  "targetPrice": 150.50,
  "targetPfcf": 15.5,
  "notes": "Empresa tecnológica líder"
}
```

### Crear empresa con análisis avanzado (DCF, TIR)

```json
POST /api/v1/watchlist
{
  "ticker": "MSFT",
  "exchange": "NASDAQ",
  "targetPrice": 350.00,
  "targetPfcf": 18.0,
  "estimatedFcfGrowthRate": 0.12,    // 12% crecimiento anual
  "investmentHorizonYears": 5,        // 5 años
  "discountRate": 0.10                // 10% WACC
}
```

**Métricas calculadas automáticamente:**
- ✅ DCF Fair Value (valor intrínseco)
- ✅ FCF Yield (rendimiento)
- ✅ Margin of Safety (margen de seguridad)
- ✅ Payback Period (periodo de recuperación)
- ✅ Estimated ROI (retorno esperado)
- ✅ Estimated IRR (TIR)

### Actualizar empresa

```json
PATCH /api/v1/watchlist/{id}
{
  "targetPrice": 160.00,
  "notes": "Actualizado después de earnings"
}
```

Solo incluye los campos que quieres cambiar (PATCH parcial).

### Registrar dispositivo para Push Notifications

```json
POST /api/v1/devices/register
{
  "fcmToken": "fK1234567890abcdef...",
  "deviceId": "android-unique-id",
  "platform": "ANDROID",
  "deviceName": "Pixel 8 Pro"
}
```

**Tipos de notificaciones que recibirás:**
- 🔔 **PRICE_ALERT**: Cuando un ticker alcanza tu precio objetivo
- 📊 **MARGIN_ALERT**: Cuando el margen de seguridad supera tu umbral
- 📈 **PRICE_UPDATE**: Actualizaciones silenciosas de precios (data-only)
- 📅 **DAILY_SUMMARY**: Resumen diario de tu watchlist (22:00 CET)

## 🧪 Tests Automáticos

Cada request incluye tests automáticos que verifican:
- ✅ Código de estado HTTP correcto
- ✅ Estructura de la respuesta
- ✅ Presencia de campos obligatorios
- ✅ Tipos de datos correctos

Los tests se ejecutan automáticamente después de cada request.

## 🔑 Variables de Entorno

### Variables predefinidas:
- `{{baseUrl}}` - URL base del API
- `{{authToken}}` - Token JWT (se guarda automáticamente)

### Variables dinámicas:
- `{{itemId}}` - UUID del item (reemplázalo con ID real)

## 📝 Notas Importantes

### Autenticación
El token JWT se guarda automáticamente después de hacer **Signup** o **Login**.
No necesitas copiarlo manualmente.

### IDs de Items
Para requests que requieren `{{itemId}}`:
1. Primero ejecuta **List Items** o **Create Item**
2. Copia el `id` de la respuesta
3. Reemplaza `{{itemId}}` en el request

### Paginación
Los endpoints de listado soportan paginación:
- `page`: Número de página (0-indexed)
- `size`: Tamaño de página (default: 20)
- `sortBy`: Campo para ordenar (createdAt, ticker, currentPrice, etc.)
- `direction`: ASC o DESC

## 🐛 Troubleshooting

### Error 401 Unauthorized
- Verifica que hiciste login (`Auth → Login`)
- El token puede haber expirado (24h) - haz login de nuevo

### Error 404 Not Found
- Verifica que el servidor está corriendo
- Verifica la URL base en el entorno activo

### Error 409 Conflict
- El ticker ya existe en tu watchlist
- Usa un ticker diferente o actualiza el existente

## 📚 Documentación Adicional

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs
- **Guía Swagger**: Ver `SWAGGER_GUIDE.md` en el proyecto

## 🚀 Comandos Útiles

### Iniciar servidor local
```bash
make run-local
```

### Ver logs de BD
```bash
make logs-db
```

### Ejecutar tests
```bash
make test
```

## 💡 Tips

1. **Usa el entorno correcto**: Cambia entre Local/AWS según necesites
2. **Revisa los tests**: Te ayudan a entender qué espera cada endpoint
3. **Lee la documentación**: Cada request tiene una sección "Docs" con info útil
4. **Guarda ejemplos**: Bruno permite guardar múltiples ejemplos por request
5. **Usa colecciones**: Ejecuta todos los tests de una carpeta de una vez

---

¿Preguntas? Revisa la documentación completa en `SWAGGER_GUIDE.md` o abre Swagger UI.
