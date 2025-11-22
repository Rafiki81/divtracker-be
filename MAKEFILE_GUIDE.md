# 🛠️ Guía de uso del Makefile

Este proyecto incluye un Makefile completo para facilitar el desarrollo. A continuación se describen los comandos más útiles.

## 📋 Requisitos previos

- **Docker Desktop** instalado y corriendo (para Testcontainers o Docker Compose)
- **Java 21** instalado
- **Maven** (incluido en el proyecto vía `mvnw`)

## 🚀 Comandos principales

### Desarrollo rápido

```bash
# Ver todos los comandos disponibles
make help

# Ejecutar la aplicación con Testcontainers (RECOMENDADO)
make run

# O de forma más explícita
make run-local
```

Esto levantará automáticamente un contenedor PostgreSQL temporal y ejecutará la aplicación.

### Desarrollo con Docker Compose

```bash
# Levantar PostgreSQL con Docker Compose y ejecutar la app
make run-dev

# Solo levantar PostgreSQL (sin ejecutar la app)
make docker-up

# Ver logs de PostgreSQL
make docker-logs

# Detener PostgreSQL
make docker-down
```

## 🧪 Tests

```bash
# Ejecutar todos los tests
make test

# Solo tests unitarios
make test-unit

# Solo tests de integración
make test-integration

# Test rápido (solo compilación básica)
make quick-test
```

## 🏗️ Construcción

```bash
# Limpiar el proyecto
make clean

# Compilar sin tests
make compile

# Instalar dependencias
make install

# Build completo (clean + compile + test + package)
make build

# Empaquetar JAR (sin tests)
make package
```

## 🗄️ Migraciones de base de datos (Flyway)

```bash
# Ejecutar migraciones pendientes
make flyway-migrate

# Ver estado de migraciones
make flyway-info

# Limpiar base de datos (¡CUIDADO! Borra todos los datos)
make flyway-clean
```

## 📚 Documentación

```bash
# Abrir Swagger UI en el navegador
make swagger
```

Swagger estará disponible en: http://localhost:8080/swagger-ui.html

## 🔍 Monitoreo

```bash
# Ver estado de todos los servicios
make status

# Ver logs en tiempo real
make logs
```

## 🎯 Flujos de trabajo comunes

### Primera vez ejecutando el proyecto

```bash
# 1. Instalar dependencias
make install

# 2. Ejecutar la aplicación
make run
```

### Desarrollo diario

```bash
# Opción A: Con Testcontainers (más simple, contenedor temporal)
make run

# Opción B: Con Docker Compose (contenedor persistente)
make run-dev
```

### Antes de hacer commit

```bash
# Ejecutar todos los tests
make test

# Build completo para verificar que todo funciona
make build
```

### Detener todo

```bash
make stop
```

## 📦 Variables de entorno

Puedes personalizar el comportamiento del Makefile con variables:

```bash
# Cambiar perfil de Spring
make run SPRING_PROFILE=prod

# Usar otro Docker Compose
make docker-up DOCKER_COMPOSE="docker compose"
```

## 🐳 Contenedores Docker

### Con Testcontainers (make run)

- ✅ Automático, no requiere configuración
- ✅ Se limpia automáticamente al terminar
- ✅ Ideal para desarrollo y tests
- ❌ Se pierde la data al reiniciar

### Con Docker Compose (make run-dev)

- ✅ Data persistente entre reinicios
- ✅ Puedes acceder a la BD desde herramientas externas
- ✅ Incluye pgAdmin opcional
- ❌ Requiere gestión manual del contenedor

#### Credenciales de PostgreSQL (Docker Compose)

```
Host: localhost
Port: 5432
Database: divtracker_db
User: divtracker
Password: divtracker123
```

#### pgAdmin (opcional)

Para habilitar pgAdmin:

```bash
docker-compose --profile admin up -d
```

Acceder en: http://localhost:5050
- Email: admin@divtracker.com
- Password: admin123

## 🔥 Comandos útiles adicionales

```bash
# Entorno completo de desarrollo (limpia, compila, levanta Docker y ejecuta)
make dev

# Ejecutar en modo watch (recarga automática)
make watch

# Formatear código
make format
```

## 📝 Notas

- El comando `make run` (con Testcontainers) es el **método recomendado** para desarrollo local
- Asegúrate de que Docker Desktop esté corriendo antes de ejecutar comandos que requieren contenedores
- Los contenedores de Testcontainers se reutilizan entre ejecuciones para mejor performance
- Usa `make docker-down` cuando termines de trabajar para liberar recursos

## 🆘 Solución de problemas

### "Docker no está corriendo"

Inicia Docker Desktop antes de ejecutar `make run` o `make docker-up`.

### "Puerto 5432 ya está en uso"

```bash
# Detener contenedores existentes
make docker-down

# O buscar qué está usando el puerto
lsof -i :5432
```

### "Tests fallan por timeout de base de datos"

```bash
# Los tests usan H2 en memoria, no deberían depender de Docker
# Si fallan, verifica que el perfil "test" esté activo en los tests
```

### "Flyway falla al migrar"

```bash
# Ver estado de migraciones
make flyway-info

# Si es necesario, limpiar y volver a migrar
make flyway-clean
make flyway-migrate
```

## 📚 Más información

- [Documentación de Flyway](FLYWAY_CONFIGURATION.md)
- [Guía de Swagger](SWAGGER_GUIDE.md)
- [Documentación de Tests](TEST_DOCUMENTATION.md)
- [API de Watchlist](WATCHLIST_API.md)
