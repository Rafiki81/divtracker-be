# Tests de Integración - Auth Controller

Este archivo contiene los tests de integración para el controlador de autenticación.

## Estructura de Tests

### AuthControllerIntegrationTest

Tests de integración completos que prueban los endpoints de autenticación:

#### Tests de Signup (Registro)
- ✅ `shouldRegisterNewUser` - Registro exitoso de un nuevo usuario
- ✅ `shouldFailToRegisterWithExistingEmail` - Fallo al registrar con email existente
- ✅ `shouldFailToRegisterWithInvalidEmail` - Fallo con email inválido
- ✅ `shouldFailToRegisterWithShortPassword` - Fallo con contraseña corta (< 6 caracteres)
- ✅ `shouldFailToRegisterWithMissingFields` - Fallo con campos obligatorios faltantes
- ✅ `shouldCreateUserWithCorrectRoleAndProvider` - Verificación de Role y Provider correctos
- ✅ `shouldEncryptPasswordOnSignup` - Verificación de encriptación de contraseña
- ✅ `shouldReturnValidJwtTokenOnSignup` - Validación del formato JWT del token

#### Tests de Login (Inicio de sesión)
- ✅ `shouldLoginWithValidCredentials` - Login exitoso con credenciales válidas
- ✅ `shouldFailToLoginWithWrongPassword` - Fallo con contraseña incorrecta
- ✅ `shouldFailToLoginWithNonExistentUser` - Fallo con usuario inexistente
- ✅ `shouldFailToLoginWithInvalidEmail` - Fallo con formato de email inválido
- ✅ `shouldFailToLoginWithEmptyCredentials` - Fallo con credenciales vacías
- ✅ `shouldReturnValidJwtTokenOnLogin` - Validación del formato JWT del token

### AuthServiceTest

Tests unitarios para el servicio de autenticación:

#### Tests de Signup
- ✅ `shouldRegisterNewUser` - Lógica de registro correcta
- ✅ `shouldThrowExceptionWhenEmailExists` - Manejo de email duplicado
- ✅ `shouldCreateUserWithCorrectAttributes` - Atributos del usuario correctos
- ✅ `shouldEncodePasswordBeforeSaving` - Encriptación antes de guardar

#### Tests de Login
- ✅ `shouldLoginWithValidCredentials` - Login exitoso
- ✅ `shouldThrowExceptionWhenCredentialsInvalid` - Manejo de credenciales inválidas
- ✅ `shouldThrowExceptionWhenUserNotFoundAfterAuth` - Manejo de usuario no encontrado
- ✅ `shouldAuthenticateWithCorrectCredentials` - Autenticación correcta

## Ejecutar los Tests

### Todos los tests
```bash
./mvnw test
```

### Solo tests de integración
```bash
./mvnw test -Dtest=AuthControllerIntegrationTest
```

### Solo tests unitarios
```bash
./mvnw test -Dtest=AuthServiceTest
```

### Con reporte de cobertura
```bash
./mvnw test jacoco:report
```

## Configuración

Los tests utilizan:
- **Base de datos H2 en memoria** para evitar dependencias de PostgreSQL
- **Perfil de test** (`application-test.properties`)
- **MockMvc** para simular peticiones HTTP
- **Mockito** para tests unitarios
- **Transactional** para rollback automático después de cada test

## Endpoints Probados

### POST `/api/auth/signup`
```json
{
  "email": "test@example.com",
  "password": "password123",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Respuesta exitosa (200 OK):**
```json
{
  "token": "eyJhbGc...",
  "type": "Bearer",
  "id": 1,
  "email": "test@example.com",
  "firstName": "John",
  "lastName": "Doe"
}
```

### POST `/api/auth/login`
```json
{
  "email": "test@example.com",
  "password": "password123"
}
```

**Respuesta exitosa (200 OK):**
```json
{
  "token": "eyJhbGc...",
  "type": "Bearer",
  "id": 1,
  "email": "test@example.com",
  "firstName": "John",
  "lastName": "Doe"
}
```

## Validaciones Probadas

### Email
- ✅ Formato válido de email
- ✅ Email único (no duplicados)
- ✅ Campo obligatorio

### Password
- ✅ Mínimo 6 caracteres
- ✅ Máximo 40 caracteres
- ✅ Encriptación con BCrypt
- ✅ Campo obligatorio

### FirstName y LastName
- ✅ Campos obligatorios
- ✅ No vacíos

## Códigos de Estado HTTP

- `200 OK` - Operación exitosa
- `400 Bad Request` - Validación fallida
- `401 Unauthorized` - Credenciales inválidas
- `4xx Client Error` - Email duplicado u otro error del cliente

## Tecnologías Utilizadas

- **JUnit 5** - Framework de testing
- **MockMvc** - Testing de controladores
- **Mockito** - Mocking para tests unitarios
- **AssertJ** - Assertions fluidas
- **H2 Database** - Base de datos en memoria para tests
- **Spring Boot Test** - Soporte de testing de Spring

## Notas

- Los tests se ejecutan con `@Transactional` para garantizar que cada test sea independiente
- Se usa `@ActiveProfiles("test")` para cargar la configuración de test
- La base de datos se limpia antes de cada test con `userRepository.deleteAll()`
- Los tokens JWT se validan con expresiones regulares para verificar su formato
