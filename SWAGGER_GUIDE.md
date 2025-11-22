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
