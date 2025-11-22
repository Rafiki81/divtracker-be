# 🐳 Desarrollo Local con Testcontainers

Esta aplicación está configurada para usar **Testcontainers** en desarrollo local, lo que significa que **no necesitas instalar PostgreSQL** en tu máquina. La aplicación levantará automáticamente un contenedor Docker de PostgreSQL.

## 📋 Requisitos Previos

1. **Docker Desktop** instalado y en ejecución
   - [Descargar Docker Desktop](https://www.docker.com/products/docker-desktop)
   - Asegúrate de que Docker esté corriendo antes de iniciar la aplicación

2. **Java 21** instalado
   - Verifica con: `java -version`

3. **Maven** (opcional, puedes usar el wrapper incluido)
   - Verifica con: `mvn -version`

## 🚀 Cómo Ejecutar la Aplicación

> **💡 FORMA MÁS FÁCIL**: Usa el Makefile incluido. Ver [MAKEFILE_GUIDE.md](MAKEFILE_GUIDE.md) para todos los comandos disponibles.

### Método Recomendado: Makefile

```bash
# Ejecutar con Testcontainers (recomendado)
make run

# O con Docker Compose
make run-dev

# Ver todos los comandos disponibles
make help
```

### Opción 1: Desde tu IDE (Recomendado)

1. Abre el proyecto en tu IDE favorito (IntelliJ IDEA, Eclipse, VS Code)
2. Localiza la clase `TestDivtrackerBeApplication.java` en `src/test/java/com/rafiki18/divtracker_be/`
3. Haz clic derecho → Run 'TestDivtrackerBeApplication'
4. ¡Listo! La aplicación iniciará y levantará automáticamente PostgreSQL en Docker

### Opción 2: Desde la Terminal con Maven

```bash
# Compilar el proyecto
./mvnw clean compile

# Ejecutar la aplicación con Testcontainers
./mvnw spring-boot:test-run
```

### Opción 3: Con exec:java

```bash
./mvnw compile exec:java \
  -Dexec.mainClass="com.rafiki18.divtracker_be.TestDivtrackerBeApplication" \
  -Dexec.classpathScope=test
```

## 🎯 Verificar que Funciona

Una vez iniciada la aplicación, deberías ver en los logs:

```
Creating container for image: postgres:16-alpine
Container postgres:16-alpine is starting...
Container is started
```

La aplicación estará disponible en:

- **API Base**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Docs**: http://localhost:8080/api-docs

## 🧪 Ejecutar Tests

Los tests también usan Testcontainers automáticamente:

```bash
# Ejecutar todos los tests
./mvnw test

# Ejecutar un test específico
./mvnw test -Dtest=AuthControllerIntegrationTest
```

## 🔧 Configuración

### Perfil Local

La aplicación usa el perfil `local` automáticamente cuando ejecutas `TestDivtrackerBeApplication`.
Este perfil está configurado en `src/main/resources/application-local.properties`.

### Base de Datos

- **Imagen**: PostgreSQL 16 Alpine
- **Base de datos**: `divtracker`
- **Usuario**: `divtracker_user`
- **Contraseña**: `divtracker_pass`
- **Puerto**: Asignado automáticamente por Testcontainers
- **Reutilización**: Habilitada (el contenedor se reutiliza entre ejecuciones)

### Flyway

Las migraciones de Flyway se ejecutan automáticamente al iniciar la aplicación:
- `V1__create_users_table.sql` - Crea la tabla de usuarios
- `V2__create_watchlist_items.sql` - Crea la tabla del watchlist
- `V3__create_market_price_ticks.sql` - Almacena los trades en tiempo real recibidos desde Finnhub via WebSocket

### Streaming en Tiempo Real (Finnhub)

- Exporta `FINNHUB_API_KEY` para habilitar las llamadas REST y el stream de precios.
- El WebSocket de Finnhub se conecta a `wss://ws.finnhub.io` y se reconecta automáticamente si la conexión se pierde.
- Cada ticker en tu watchlist se suscribe automáticamente y sus trades quedan registrados en la tabla `market_price_ticks`.
- Puedes deshabilitar el stream estableciendo `finnhub.stream-enabled=false` si sólo quieres los endpoints REST.

## 🐛 Troubleshooting

### Docker no está corriendo
```
Error: Could not find a valid Docker environment
```
**Solución**: Inicia Docker Desktop y espera a que esté completamente arrancado.

### Puerto 8080 ocupado
```
Port 8080 is already in use
```
**Solución**: Cambia el puerto en `application-local.properties`:
```properties
server.port=8081
```

### Permisos de Docker en Linux
```
Permission denied while trying to connect to Docker daemon
```
**Solución**: Añade tu usuario al grupo docker:
```bash
sudo usermod -aG docker $USER
# Cierra sesión y vuelve a iniciar
```

### Limpiar contenedores de Testcontainers
```bash
# Ver contenedores de Testcontainers
docker ps -a | grep testcontainers

# Detener y eliminar todos
docker rm -f $(docker ps -a -q --filter "label=org.testcontainers")
```

## 📊 Logs Útiles

La configuración local incluye logs detallados:

- **SQL Queries**: Ver las consultas SQL ejecutadas
- **Hibernate**: Ver operaciones de Hibernate
- **Testcontainers**: Ver el ciclo de vida del contenedor

Para ajustar el nivel de logs, edita `application-local.properties`:

```properties
logging.level.com.rafiki18.divtracker_be=DEBUG
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.testcontainers=INFO
```

## 🌐 Variables de Entorno (Opcional)

Si quieres configurar OAuth2 o JWT personalizados:

```bash
export JWT_SECRET="tu-secret-key-super-segura-de-al-menos-256-bits"
export GOOGLE_CLIENT_ID="tu-google-client-id"
export GOOGLE_CLIENT_SECRET="tu-google-client-secret"
```

## 🏭 Ejecutar en Producción

Para producción, **NO uses** `TestDivtrackerBeApplication`. En su lugar:

1. Usa la clase principal `DivtrackerBeApplication`
2. Configura una base de datos PostgreSQL real
3. Configura las variables de entorno apropiadas
4. Usa el perfil de producción

```bash
java -jar divtracker-be.jar --spring.profiles.active=prod
```

## 📚 Recursos Adicionales

- [Documentación de Testcontainers](https://www.testcontainers.org/)
- [Spring Boot Testcontainers](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing.testcontainers)
- [PostgreSQL Docker Image](https://hub.docker.com/_/postgres)

## 💡 Ventajas de Este Enfoque

✅ No necesitas instalar PostgreSQL localmente  
✅ Base de datos limpia y consistente  
✅ Mismo entorno para todos los desarrolladores  
✅ Tests más realistas (base de datos real, no H2)  
✅ Configuración mínima requerida  
✅ Flyway ejecuta migraciones automáticamente  
✅ Reutilización de contenedores para arranques más rápidos  

---

**¿Preguntas o problemas?** Revisa la sección de Troubleshooting o contacta al equipo de desarrollo.
