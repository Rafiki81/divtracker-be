# 📊 DivTracker Backend

> **Plataforma de análisis y valoración de acciones con dividendos**  
> Proyecto Final de Ciclo Formativo de Grado Superior en Desarrollo de Aplicaciones Web

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue)
![AWS](https://img.shields.io/badge/AWS-Elastic%20Beanstalk-orange)
![License](https://img.shields.io/badge/License-MIT-green)

---

## 📖 Descripción

DivTracker es una aplicación backend REST API para análisis financiero avanzado de acciones, enfocada en inversores que buscan ingresos por dividendos. Permite crear watchlists personalizadas, obtener datos en tiempo real del mercado y calcular métricas financieras avanzadas como TIR, DCF, FCF Yield, y más.

### ✨ Características Principales

- 🔐 **Autenticación JWT** con OAuth2 (Google)
- 📈 **Datos de mercado en tiempo real** vía Finnhub API
- 🔍 **Búsqueda flexible de tickers** - por nombre o símbolo con autocompletado
- 🤖 **Cálculos automáticos inteligentes** - 4 modos de creación:
  - ⚡ **Modo 1**: Solo ticker → calcula targetPrice + targetPfcf automáticamente
  - 🎯 **Modo 2**: ticker + targetPfcf → calcula targetPrice
  - 💰 **Modo 3**: ticker + targetPrice → calcula targetPfcf
  - ✏️ **Modo 4**: ticker + ambos valores → usa datos manuales
- 💰 **Métricas financieras avanzadas**:
  - TIR (Tasa Interna de Retorno)
  - DCF (Discounted Cash Flow) con Gordon Growth Model
  - FCF Yield (Free Cash Flow Yield)
  - Margen de Seguridad
  - Periodo de Payback
  - ROI Estimado
- 🔔 **Webhooks de Finnhub** para actualizaciones en tiempo real de precios
- 🗄️ **PostgreSQL** con migraciones Flyway
- 📝 **OpenAPI/Swagger** para documentación interactiva
- 🐳 **Docker** y **AWS Elastic Beanstalk** ready
- 🏗️ **AWS CDK (Go)** para infraestructura como código
- 🚀 **GitHub Actions** para CI/CD automatizado

---

## 🏗️ Arquitectura

```
┌─────────────────────────────────────────────────────────────┐
│                        Cliente Web/Mobile                    │
└──────────────────────┬──────────────────────────────────────┘
                       │ HTTPS/WSS
                       ▼
          ┌────────────────────────┐
          │   Spring Boot Backend  │
          │   (REST API + WebSocket)│
          └────────┬───────┬───────┘
                   │       │
        ┌──────────┘       └──────────┐
        ▼                              ▼
┌───────────────┐            ┌──────────────────┐
│  PostgreSQL   │            │  Finnhub API     │
│  (Datos)      │            │  (Market Data)   │
└───────────────┘            └──────────────────┘
```

---

## 📁 Estructura del Proyecto

```
divtracker-be/
├── src/
│   ├── main/
│   │   ├── java/com/rafiki18/divtracker_be/
│   │   │   ├── config/              # Configuración (Security, WebSocket, CORS)
│   │   │   ├── controller/          # Controladores REST
│   │   │   │   ├── AuthController.java
│   │   │   │   └── WatchlistController.java
│   │   │   ├── dto/                 # Data Transfer Objects
│   │   │   │   ├── AuthResponse.java
│   │   │   │   ├── LoginRequest.java
│   │   │   │   ├── WatchlistItemRequest.java
│   │   │   │   └── WatchlistItemResponse.java
│   │   │   ├── entity/              # Entidades JPA
│   │   │   │   ├── User.java
│   │   │   │   └── WatchlistItem.java
│   │   │   ├── exception/           # Manejo de excepciones
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   ├── marketdata/          # Integración Finnhub
│   │   │   │   ├── FinnhubClient.java
│   │   │   │   └── stream/
│   │   │   │       ├── FinnhubStreamingClient.java
│   │   │   │       └── WatchlistTickerSubscriptionService.java
│   │   │   ├── mapper/              # MapStruct mappers
│   │   │   │   └── WatchlistMapper.java
│   │   │   ├── repository/          # Spring Data JPA
│   │   │   │   ├── UserRepository.java
│   │   │   │   └── WatchlistItemRepository.java
│   │   │   ├── security/            # JWT, OAuth2
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   ├── JwtTokenProvider.java
│   │   │   │   └── SecurityConfig.java
│   │   │   ├── service/             # Lógica de negocio
│   │   │   │   ├── AuthService.java
│   │   │   │   ├── WatchlistService.java
│   │   │   │   ├── FinancialMetricsService.java
│   │   │   │   └── MarketDataEnrichmentService.java
│   │   │   └── DivtrackerBeApplication.java
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-local.properties
│   │       ├── application-test.properties
│   │       ├── application-aws.properties
│   │       └── db/migration/        # Migraciones Flyway
│   │           ├── V1__create_users_table.sql
│   │           ├── V2__create_watchlist_items.sql
│   │           ├── V3__create_market_price_ticks.sql
│   │           └── V4__add_valuation_parameters.sql
│   └── test/
│       └── java/com/rafiki18/divtracker_be/
│           ├── controller/           # Tests de integración
│           ├── service/              # Tests unitarios
│           └── e2e/                  # Tests end-to-end (pendiente)
├── infrastructure/                   # Infraestructura como código
│   ├── cdk/                          # AWS CDK en Go
│   │   ├── main.go                   # CDK App entry point
│   │   ├── stack.go                  # Main infrastructure stack
│   │   ├── vpc.go                    # VPC construct
│   │   ├── database.go               # RDS PostgreSQL
│   │   ├── secrets.go                # Secrets Manager
│   │   └── beanstalk.go              # Elastic Beanstalk
│   └── scripts/                      # Scripts de deployment
├── .github/workflows/                # GitHub Actions CI/CD
│   ├── infra-create.yml              # Crear infraestructura
│   ├── deploy-app.yml                # Desplegar aplicación
│   └── infra-destroy.yml             # Destruir infraestructura
├── .ebextensions/                    # Config Elastic Beanstalk
├── docker-compose.yml                # PostgreSQL local
├── Makefile                          # Comandos automatizados
├── Procfile                          # Config para AWS EB
└── pom.xml                           # Dependencias Maven
```

---

## 🚀 Quick Start

### Prerequisitos

- **Java 17+**
- **Docker** y **Docker Compose**
- **Maven** (incluido en el proyecto con `mvnw`)

### 1. Clonar repositorio

```bash
git clone https://github.com/tu-usuario/divtracker-be.git
cd divtracker-be
```

### 2. Configurar variables de entorno

Crea `.env` en la raíz (opcional, hay valores por defecto):

```bash
# Finnhub API (opcional para testing)
FINNHUB_API_KEY=tu_api_key_aqui

# JWT Secret (se genera automáticamente si no se provee)
JWT_SECRET=tu_secret_super_seguro

# Google OAuth (opcional)
GOOGLE_CLIENT_ID=tu_client_id
GOOGLE_CLIENT_SECRET=tu_client_secret
```

### 3. Iniciar base de datos

```bash
make docker-up
```

Esto inicia PostgreSQL en Docker en el puerto 5432.

### 4. Ejecutar migraciones

```bash
make db-migrate
```

### 5. Ejecutar aplicación

```bash
make run-local
```

La aplicación estará disponible en:
- **API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Health Check**: http://localhost:8080/actuator/health

---

## 🧪 Testing

```bash
# Ejecutar todos los tests
make test

# Solo tests unitarios
make test-unit

# Solo tests de integración
make test-integration

# Ver cobertura
./mvnw test jacoco:report
# Reporte en: target/site/jacoco/index.html
```

### Resultados actuales
- ✅ **68 tests pasando**
- 🧪 Tests unitarios de servicios
- 🔗 Tests de integración de controladores
- 📊 Tests de métricas financieras

---

## 📡 API Endpoints

### Autenticación

```bash
# Registro
POST /api/auth/signup
Content-Type: application/json
{
  "email": "usuario@ejemplo.com",
  "password": "SecurePass123!",
  "firstName": "John",
  "lastName": "Doe"
}

# Login
POST /api/auth/login
Content-Type: application/json
{
  "email": "usuario@ejemplo.com",
  "password": "SecurePass123!"
}

# Response
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "email": "usuario@ejemplo.com",
  "firstName": "John",
  "lastName": "Doe"
}
```

### Watchlist

```bash
# Buscar tickers (requiere JWT)
GET /api/v1/tickers/search?q=apple
Authorization: Bearer {token}

# Response
[
  {
    "symbol": "AAPL",
    "description": "Apple Inc",
    "type": "Common Stock",
    "exchange": "NASDAQ",
    "currency": "USD"
  }
]

# Crear item - Modo Automático (solo ticker)
POST /api/v1/watchlist
Authorization: Bearer {token}
Content-Type: application/json
{
  "ticker": "AAPL"
}

# Crear item - Modo Manual (con datos)
POST /api/v1/watchlist
Authorization: Bearer {token}
Content-Type: application/json
{
  "ticker": "AAPL",
  "exchange": "NASDAQ",
  "targetPrice": 180.00,
  "targetPfcf": 15.0,
  "estimatedFcfGrowthRate": 8.5,
  "investmentHorizonYears": 5,
  "discountRate": 10.0,
  "notes": "Apple - Strong fundamentals"
}

# Response (con métricas calculadas)
{
  "id": "uuid-here",
  "ticker": "AAPL",
  "currentPrice": 175.43,
  "freeCashFlowPerShare": 6.32,
  "targetPrice": 180.00,
  "targetPfcf": 15.0,
  "actualPfcf": 27.75,
  "fcfYield": 3.60,
  "marginOfSafety": 2.61,
  "dcfFairValue": 205.48,
  "estimatedIRR": 13.79,
  "paybackPeriod": 7.25,
  "estimatedROI": 78.96,
  "createdAt": "2024-11-22T10:30:00Z"
}

# Listar items
GET /api/v1/watchlist?page=0&size=10&sort=createdAt,desc
Authorization: Bearer {token}

# Actualizar item
PATCH /api/v1/watchlist/{id}
Authorization: Bearer {token}
{
  "targetPrice": 200.00,
  "notes": "Updated target"
}

# Eliminar item
DELETE /api/v1/watchlist/{id}
Authorization: Bearer {token}
```

### WebSocket

```javascript
// Conectar con JWT
const socket = new SockJS('http://localhost:8080/ws/market-data');
const stompClient = Stomp.over(socket);

stompClient.connect(
  { Authorization: `Bearer ${token}` },
  () => {
    // Suscribirse a actualizaciones
    stompClient.subscribe('/user/queue/market-data', (message) => {
      const data = JSON.parse(message.body);
      console.log('Market update:', data);
    });

    // Solicitar suscripción a tickers
    stompClient.send('/app/market-data/subscribe', {}, JSON.stringify({
      action: 'subscribe',
      tickers: ['AAPL', 'MSFT']
    }));
  }
);
```

---

## 💡 Métricas Financieras

### FCF Yield (Free Cash Flow Yield)
```
FCF Yield = (FCF por acción / Precio actual) × 100
```
Indica el porcentaje de retorno en flujo de caja libre.

### DCF (Discounted Cash Flow)
```
DCF = Σ(FCF_n / (1 + r)^n) + Terminal Value
Terminal Value = FCF_final × (1 + g) / (r - g)
```
Valoración basada en flujos de caja futuros descontados.

### Margen de Seguridad
```
Margen = ((Valor Intrínseco - Precio) / Precio) × 100
```
Porcentaje de descuento del precio actual vs. valor calculado.

### TIR (IRR - Internal Rate of Return)
```
0 = Inversión_inicial + Σ(FCF_n / (1 + TIR)^n)
```
Tasa de retorno anualizada del proyecto de inversión.

### Periodo de Payback
```
Payback = Inversión Inicial / FCF Anual
```
Años necesarios para recuperar la inversión.

### ROI Estimado
```
ROI = (Ganancia Capital + FCF Acumulado) / Inversión × 100
```
Retorno total esperado en el horizonte temporal.

---

## 🔧 Configuración

### Perfiles de Spring

- **`local`**: Desarrollo local con Docker Compose
- **`test`**: Tests con H2 in-memory
- **`aws`**: Producción en AWS Elastic Beanstalk

### Variables de entorno

| Variable | Descripción | Default | Requerido |
|----------|-------------|---------|-----------|
| `SPRING_PROFILES_ACTIVE` | Perfil activo | local | No |
| `RDS_HOSTNAME` | Hostname de PostgreSQL | localhost | No |
| `RDS_PORT` | Puerto de PostgreSQL | 5432 | No |
| `RDS_DB_NAME` | Nombre de la base de datos | divtracker | No |
| `RDS_USERNAME` | Usuario de la base de datos | divtracker | No |
| `DB_PASSWORD` | Contraseña de la base de datos | - | Sí (AWS) |
| `FINNHUB_API_KEY` | API key de Finnhub | - | Sí* |
| `FINNHUB_WEBHOOK_SECRET` | Secret para webhooks de Finnhub | - | Sí (AWS) |
| `JWT_SECRET` | Secret para JWT | (generado) | No |
| `GOOGLE_CLIENT_ID` | OAuth Google Client ID | - | No |
| `GOOGLE_CLIENT_SECRET` | OAuth Google Secret | - | No |
| `APP_SECRETS_ARN` | ARN del secret consolidado en AWS | - | No (AWS) |

_*Opcional para testing sin datos reales_

---

## 🚢 Deployment en AWS

### Despliegue automatizado con GitHub Actions

1. **Configurar secrets en GitHub**:
   - `AWS_ACCESS_KEY_ID`
   - `AWS_SECRET_ACCESS_KEY`
   - `AWS_ACCOUNT_ID`
   - `FINNHUB_API_KEY`
   - `FINNHUB_WEBHOOK_SECRET`
   - `JWT_SECRET`
   - `GOOGLE_CLIENT_ID` (opcional)
   - `GOOGLE_CLIENT_SECRET` (opcional)

2. **Ejecutar workflow "🏗️ Crear Infraestructura"**:
   - Crea VPC, RDS PostgreSQL, Secrets Manager, Elastic Beanstalk
   - Tiempo estimado: ~15-20 minutos
   - Configurado para FREE TIER (t2.micro, db.t3.micro)

3. **Ejecutar workflow "🚀 Desplegar Aplicación"**:
   - Tests → Build → Deploy → Health Check
   - Se ejecuta automáticamente en push a `main`
   - Tiempo estimado: ~5-7 minutos

Ver [infrastructure/README.md](infrastructure/README.md) para detalles completos sobre CDK y arquitectura AWS.

### Quick Deploy a AWS

```bash
# 1. Inicializar infraestructura (solo primera vez)
make infra-init-backend
make infra-init

# 2. Configurar variables
cd infrastructure/terraform/environments/prod
cp terraform.tfvars.example terraform.tfvars
# Editar terraform.tfvars con tus valores

# 3. Deploy completo
make deploy-full
```

---

## 📊 Monitorización

### Health Check
```bash
curl http://localhost:8080/actuator/health
```

### Métricas
```bash
curl http://localhost:8080/actuator/metrics
```

### Logs
```bash
# Local
docker-compose logs -f

# AWS
make logs-prod
```

---

## 🛠️ Makefile - Comandos Disponibles

```bash
make help              # Ver todos los comandos
make build             # Compilar proyecto
make test              # Ejecutar tests
make run               # Ejecutar aplicación
make docker-up         # Iniciar PostgreSQL
make docker-down       # Detener PostgreSQL
make db-migrate        # Aplicar migraciones
make clean             # Limpiar builds
make format            # Formatear código

# AWS
make infra-init        # Inicializar Terraform
make infra-plan        # Ver plan de infraestructura
make infra-apply       # Aplicar infraestructura
make deploy-prod       # Deploy a producción
make logs-prod         # Ver logs de producción
make infra-destroy     # Destruir infraestructura
```

---

## 🏛️ Stack Tecnológico

### Backend
- **Java 17** - Lenguaje
- **Spring Boot 3.5.6** - Framework
- **Spring Security** - Autenticación/Autorización
- **Spring Data JPA** - ORM
- **Spring WebSocket** - Comunicación en tiempo real

### Base de Datos
- **PostgreSQL 15** - Base de datos principal
- **Flyway** - Migraciones de BD
- **H2** - Base de datos para tests

### Integraciones
- **Finnhub API** - Datos de mercado en tiempo real
- **OAuth2** - Autenticación con Google

### DevOps
- **Docker** - Contenedores
- **Terraform** - Infrastructure as Code
- **AWS Elastic Beanstalk** - Hosting
- **AWS RDS** - Base de datos gestionada
- **GitHub Actions** - CI/CD

### Documentación
- **Swagger/OpenAPI 3** - Documentación API interactiva
- **Spring Boot Actuator** - Métricas y health checks

### Testing
- **JUnit 5** - Framework de testing
- **Mockito** - Mocking
- **AssertJ** - Aserciones fluidas
- **TestRestTemplate** - Tests de integración

---

## 📝 Roadmap

### ✅ Completado
- [x] Autenticación JWT
- [x] CRUD de Watchlist
- [x] Integración con Finnhub
- [x] Métricas financieras avanzadas
- [x] WebSocket para datos en tiempo real
- [x] Tests unitarios y de integración
- [x] Infraestructura AWS con Terraform
- [x] CI/CD con scripts automatizados

### 🚧 En desarrollo
- [ ] Tests E2E completos
- [ ] Cache con Redis
- [ ] Rate limiting
- [ ] Notificaciones por email

### 🔮 Futuro
- [ ] Frontend React/Vue
- [ ] App móvil
- [ ] Alertas de precio
- [ ] Backtesting de estrategias
- [ ] Dashboard de portfolio
- [ ] API pública con rate limiting

---

## 🤝 Contribuir

Este es un proyecto de fin de ciclo formativo. Sugerencias y feedback son bienvenidos:

1. Fork el proyecto
2. Crea tu feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la branch (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

---

## 📄 Licencia

Este proyecto está bajo la Licencia MIT. Ver archivo [LICENSE](LICENSE) para más detalles.

---

## 👤 Autor

**Rafael Perez-Beato Santamaria**

- 🎓 Proyecto Final - CFGS Desarrollo de Aplicaciones multiplataforma
- 📧 Email: rperezbeato@gmail.com
- 🔗 LinkedIn: [Rafael Perez-Beato Santamaria](www.linkedin.com/in/rafael-p-a528031ab)
- 💻 GitHub: [@rafiki81](https://github.com/Rafiki81)

---

## 🙏 Agradecimientos

- [Finnhub](https://finnhub.io/) - API de datos de mercado
- [Spring Boot](https://spring.io/projects/spring-boot) - Framework excepcional
- [AWS](https://aws.amazon.com/) - Infraestructura cloud

---

## 📚 Documentación Adicional

- [Infraestructura AWS](infrastructure/README.md)
- [API Documentation (Swagger)](http://localhost:8080/swagger-ui.html)
- [Guía de Deployment](infrastructure/DEPLOYMENT.md)

---

**⭐ Si este proyecto te resulta útil, dale una estrella en GitHub!**
