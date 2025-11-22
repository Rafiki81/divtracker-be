# 📚 Guía de Swagger/OpenAPI

## Acceso a la Documentación

Una vez que la aplicación esté ejecutándose, puedes acceder a la documentación interactiva de la API en:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

## Características

### 🔐 Autenticación JWT

La API utiliza autenticación JWT (JSON Web Tokens). Para probar endpoints protegidos:

1. **Registrar un usuario** o **Iniciar sesión** usando los endpoints:
   - `POST /api/auth/signup`
   - `POST /api/auth/login`

2. **Copiar el token** de la respuesta

3. **Hacer clic en el botón "Authorize"** (🔓) en la parte superior derecha de Swagger UI

4. **Pegar el token** en el campo de valor (solo el token, sin "Bearer")

5. **Hacer clic en "Authorize"** y luego en "Close"

Ahora puedes probar todos los endpoints protegidos.

### 📋 Endpoints Disponibles

#### Autenticación

- **POST /api/auth/signup** - Registrar nuevo usuario
  ```json
  {
    "email": "usuario@ejemplo.com",
    "password": "password123",
    "firstName": "Juan",
    "lastName": "Pérez"
  }
  ```

- **POST /api/auth/login** - Iniciar sesión
  ```json
  {
    "email": "usuario@ejemplo.com",
    "password": "password123"
  }
  ```

#### Watchlist

- **GET /api/v1/watchlist** - Listar empresas en el watchlist (paginado)
  - Parámetros: `page`, `size`, `sortBy`, `direction`
  
- **GET /api/v1/watchlist/{id}** - Obtener detalles de una empresa
  
- **POST /api/v1/watchlist** - Añadir empresa al watchlist
  ```json
  {
    "ticker": "AAPL",
    "exchange": "NASDAQ",
    "targetPrice": 150.50,
    "targetPfcf": 15.5,
    "notifyWhenBelowPrice": false,
    "notes": "Empresa tecnológica líder",
    "estimatedFcfGrowthRate": 0.08,
    "investmentHorizonYears": 5,
    "discountRate": 0.10
  }
  ```
  
- **PATCH /api/v1/watchlist/{id}** - Actualizar empresa (parcial)
  
- **DELETE /api/v1/watchlist/{id}** - Eliminar empresa del watchlist

#### Métricas Calculadas Automáticamente

La respuesta de cada item del watchlist incluye:

```json
{
  "id": "uuid",
  "ticker": "AAPL",
  "currentPrice": 172.15,
  "targetPrice": 150.50,
  "targetPfcf": 15.5,
  "freeCashFlowPerShare": 11.45,
  "actualPfcf": 15.03,
  "fairPriceByPfcf": 180.00,
  "discountToFairPrice": 0.12,
  "deviationFromTargetPrice": -0.05,
  "undervalued": true,
  
  // Parámetros de valoración
  "estimatedFcfGrowthRate": 0.08,
  "investmentHorizonYears": 5,
  "discountRate": 0.10,
  
  // Métricas avanzadas
  "dcfFairValue": 195.50,
  "fcfYield": 6.65,
  "marginOfSafety": 25.00,
  "paybackPeriod": 7.2,
  "estimatedROI": 85.50,
  "estimatedIRR": 12.50,
  
  "createdAt": "2023-11-22T10:30:00",
  "updatedAt": "2023-11-22T15:45:00"
}
```

**Explicación de las métricas:**
- **dcfFairValue**: Valor intrínseco calculado por Discounted Cash Flow
- **fcfYield**: Rendimiento del Free Cash Flow (FCF/Precio × 100)
- **marginOfSafety**: % de descuento del precio actual vs valor DCF (positivo = infravalorado)
- **paybackPeriod**: Años estimados para recuperar la inversión
- **estimatedROI**: Retorno de inversión esperado al horizonte configurado
- **estimatedIRR**: Tasa Interna de Retorno anual esperada

### 🔔 Webhooks de Finnhub

El endpoint de webhooks recibe actualizaciones de precios en tiempo real:

```json
POST /api/webhooks/finnhub
X-Finnhub-Secret: d4gubhhr01qgvvc57cf0

{
  "event": "trade",
  "data": [
    {
      "s": "AAPL",
      "p": 172.15,
      "t": 1732285432000,
      "v": 1000
    }
  ]
}
```

**Configuración:**
1. Dashboard de Finnhub → Webhooks
2. URL: `https://tu-app.elasticbeanstalk.com/api/webhooks/finnhub`
3. Secret: El configurado en `FINNHUB_WEBHOOK_SECRET`

**Funcionamiento:**
- Finnhub envía POST con eventos de trades
- Se verifica el header `X-Finnhub-Secret`
- Los precios se guardan en `market_price_ticks`
- Responde 200 OK para confirmar recepción

#### Webhooks

- **POST /api/webhooks/finnhub** - Recibir notificaciones de trades desde Finnhub
  - Header: `X-Finnhub-Secret` (autenticación)
  - Body: Eventos de trades en tiempo real
  - **Nota**: Normalmente llamado por Finnhub, no por clientes

#### Testing

- **GET /api/test/public** - Endpoint público (no requiere autenticación)
- **GET /api/test/protected** - Endpoint protegido (requiere JWT)

### 🎨 Respuesta de Autenticación

Ambos endpoints de autenticación devuelven:

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "id": 1,
  "email": "usuario@ejemplo.com",
  "firstName": "Juan",
  "lastName": "Pérez"
}
```

### 🛠️ Configuración

La configuración de Swagger está en:
- `src/main/java/com/rafiki18/divtracker_be/config/OpenApiConfig.java`
- `src/main/resources/application.properties` (sección Swagger/OpenAPI)

### 📝 Personalización

Para agregar documentación a nuevos endpoints:

1. **Agregar anotaciones en el Controller**:
```java
@Tag(name = "Nombre", description = "Descripción del grupo")
public class MiController {
    
    @Operation(
        summary = "Breve descripción",
        description = "Descripción detallada"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Éxito",
            content = @Content(schema = @Schema(implementation = MiDTO.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Error de validación"
        )
    })
    @GetMapping("/endpoint")
    public ResponseEntity<MiDTO> miEndpoint() {
        // ...
    }
}
```

2. **Agregar anotaciones en los DTOs**:
```java
@Schema(description = "Descripción del modelo")
public class MiDTO {
    
    @Schema(description = "Campo X", example = "ejemplo", required = true)
    private String campo;
}
```

### 🔒 Endpoints Protegidos

Para marcar un endpoint como protegido en la documentación:

```java
@Operation(
    summary = "Endpoint protegido",
    security = @SecurityRequirement(name = "bearerAuth")
)
```

## Notas Adicionales

- La documentación se genera automáticamente a partir de las anotaciones
- Los esquemas de validación (`@NotBlank`, `@Email`, etc.) se reflejan en Swagger
- Puedes probar todos los endpoints directamente desde Swagger UI
- Los ejemplos en los `@Schema` ayudan a entender el formato esperado

## Enlaces Útiles

- [Documentación Springdoc OpenAPI](https://springdoc.org/)
- [Swagger UI](https://swagger.io/tools/swagger-ui/)
- [OpenAPI Specification](https://swagger.io/specification/)
