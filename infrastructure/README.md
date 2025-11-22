# 🏗️ Infraestructura DivTracker - AWS Deployment

## 📋 Descripción General

Esta carpeta contiene toda la **Infraestructura como Código (IaC)** necesaria para desplegar DivTracker en AWS utilizando **AWS CDK con Go**. La arquitectura está diseñada para ser **Free Tier eligible** durante el primer año, haciéndola ideal para proyectos educativos y de aprendizaje.

### 🎯 Arquitectura AWS

```
┌─────────────────────────────────────────────────────────────┐
│                         AWS Cloud                           │
│  ┌──────────────────────────────────────────────────────┐  │
│  │                    VPC (10.0.0.0/16)                 │  │
│  │                                                       │  │
│  │  ┌─────────────────┐      ┌─────────────────┐       │  │
│  │  │  Public Subnet  │      │  Public Subnet  │       │  │
│  │  │  (us-east-1a)   │      │  (us-east-1b)   │       │  │
│  │  │                 │      │                 │       │  │
│  │  │  ┌───────────┐  │      │                 │       │  │
│  │  │  │  Elastic  │  │      │                 │       │  │
│  │  │  │ Beanstalk │◄─┼──────┼─── Internet     │       │  │
│  │  │  │ (t2.micro)│  │      │    Gateway      │       │  │
│  │  │  └─────┬─────┘  │      │                 │       │  │
│  │  └────────┼────────┘      └─────────────────┘       │  │
│  │           │                                          │  │
│  │           │ (Security Group: Port 5432)              │  │
│  │           ▼                                          │  │
│  │  ┌─────────────────┐      ┌─────────────────┐       │  │
│  │  │ Private Subnet  │      │ Private Subnet  │       │  │
│  │  │  (us-east-1a)   │      │  (us-east-1b)   │       │  │
│  │  │                 │      │                 │       │  │
│  │  │  ┌───────────┐  │      │  ┌───────────┐  │       │  │
│  │  │  │    RDS    │  │      │  │    RDS    │  │       │  │
│  │  │ PostgreSQL│  │      │                 │       │  │
│  │  │(db.t3.micro)│  │      │ (Single-AZ)  │       │  │
│  │  │  └───────────┘  │      │                 │       │  │
│  │  └─────────────────┘      └─────────────────┘       │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌──────────────────┐  ┌──────────────────┐               │
│  │  CloudWatch Logs │  │ Secrets Manager  │               │
│  │  (7 days)        │  │ (DB + API Keys)  │               │
│  └──────────────────┘  └──────────────────┘               │
└─────────────────────────────────────────────────────────────┘
```

### ✨ Características de la Infraestructura

- **🚀 Elastic Beanstalk**: Single Instance con Java 17 (Corretto) - FREE TIER
- **🗄️ RDS PostgreSQL 15**: Single-AZ db.t3.micro con backups - FREE TIER
- **🔒 Secrets Manager**: 1 secret consolidado con todas las credenciales
- **📊 CloudWatch Logs**: Retención 7 días para troubleshooting
- **🌐 VPC**: Red privada con subnets públicas (app) y privadas aisladas (DB)
- **🛡️ Security Groups**: Mínimo privilegio - solo puertos necesarios
- **🔔 Webhooks**: Finnhub webhooks para actualizaciones de precios en tiempo real
- **💰 FREE TIER**: t2.micro + db.t3.micro = ~0€/mes primer año, ~15-22€/mes después

---

## 📁 Estructura del Proyecto

```
infrastructure/
├── README.md                          # Este archivo
├── cdk/                               # AWS CDK Infrastructure (Go)
│   ├── go.mod                         # Go module dependencies
│   ├── go.sum                         # Go checksum file
│   ├── cdk.json                       # CDK configuration
│   ├── main.go                        # CDK App entry point
│   ├── stack.go                       # Main infrastructure stack
│   ├── vpc.go                         # VPC construct (subnets, security groups)
│   ├── database.go                    # RDS PostgreSQL construct
│   ├── secrets.go                     # Secrets Manager construct
│   ├── beanstalk.go                   # Elastic Beanstalk construct
│   └── .gitignore                     # CDK specific ignores
└── scripts/                           # Scripts de automatización
    ├── init-terraform-backend.sh      # Bootstrap CDK (cdk bootstrap)
    ├── build-for-aws.sh               # Construye y empaqueta la aplicación
    └── deploy.sh                      # Despliega la aplicación a Elastic Beanstalk
```

---

## 🔧 Constructs de CDK (Go)

### 1️⃣ VPC Construct (`vpc.go`)

**Propósito**: Crea la red privada virtual con segmentación pública/privada.

**Recursos creados**:
- VPC con CIDR `10.0.0.0/16`
- 2 subnets públicas (para Elastic Beanstalk)
- 2 subnets privadas aisladas (para RDS)
- Internet Gateway automático
- Security Groups (app y RDS)

**Struct retornado**:
```go
type VpcConstruct struct {
    Vpc              awsec2.Vpc
    AppSecurityGroup awsec2.SecurityGroup
    RdsSecurityGroup awsec2.SecurityGroup
    PublicSubnets    *[]awsec2.ISubnet
    PrivateSubnets   *[]awsec2.ISubnet
}
```

**Configuración**:
- MaxAzs: 2 (alta disponibilidad)
- NatGateways: 0 (Free Tier)
- EnableDnsHostnames: true
- EnableDnsSupport: true

### 2️⃣ Database Construct (`database.go`)

**Propósito**: Despliega base de datos PostgreSQL gestionada.

**Recursos creados**:
- RDS PostgreSQL 15.4
- Secrets Manager automático
- Automated backups (7 días)
- CloudWatch Logs

**Props requeridas**:
```go
type DatabaseConstructProps struct {
    Vpc           awsec2.Vpc
    SecurityGroup awsec2.SecurityGroup
}
```

**Configuración**:
- Engine: PostgreSQL 15 (última versión)
- InstanceType: db.t3.micro (Free Tier)
- AllocatedStorage: 20 GB GP3
- MultiAz: false (Single-AZ para Free Tier)
- BackupRetention: 7 días
- Username: divtracker (configurado en credentials)
- Password: auto-generado y guardado en Secrets Manager
- DeletionProtection: false (desarrollo)

**Struct retornado**:
```go
type DatabaseConstruct struct {
    DbInstance     awsrds.DatabaseInstance
    DatabaseSecret awssecretsmanager.ISecret
}
```

### 3️⃣ Elastic Beanstalk Construct (`beanstalk.go`)

**Propósito**: Despliega la aplicación Java con gestión automática.

**Recursos creados**:
- Elastic Beanstalk Application
- Elastic Beanstalk Environment (SingleInstance)
- IAM Roles (EC2 + Service Role)
- CloudWatch Logs configuration
- Health checks configurados

**Props requeridas**:
```go
type ElasticBeanstalkConstructProps struct {
    Vpc                  awsec2.Vpc
    PublicSubnets        *[]awsec2.ISubnet
    SecurityGroup        awsec2.SecurityGroup
    Database             awsrds.DatabaseInstance
    DatabaseSecret       awssecretsmanager.ISecret
    AppSecretsArn        *string
    DbSecretArn          *string
    JwtSecret            *string
    FinnhubApiKey        string
    FinnhubWebhookSecret string
    GoogleClientId       string
    GoogleClientSecret   string
}
```

**Configuración**:
- InstanceType: t2.micro (Free Tier)
- EnvironmentType: SingleInstance (no load balancer)
- SolutionStack: Amazon Linux 2023 v4.8.0 + Corretto 17
- Health Check: `/actuator/health`
- JVM Settings: Xms=128m, Xmx=384m (configurado en Procfile)
- HikariCP: max-pool-size=5, min-idle=1

**Outputs**:
- CfnOutput con Application URL

---

## 🚀 Prerequisitos

### 1. Herramientas Requeridas

```bash
# Go (>= 1.21)
go version

# AWS CDK CLI (>= 2.150.0)
npm install -g aws-cdk
cdk --version

# AWS CLI (>= 2.0)
aws --version

# Maven (>= 3.9)
mvn --version

# Java 17
java --version
```

### 2. Configuración de AWS

```bash
# Configurar credenciales de AWS
aws configure

# Verificar credenciales
aws sts get-caller-identity
```

**Credenciales necesarias**:
- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- Región: `us-east-1` (recomendada para Free Tier)

### 3. GitHub Secrets

Configura los siguientes secrets en tu repositorio de GitHub (Settings → Secrets and variables → Actions):

**Requeridos para infraestructura:**
- `AWS_ACCESS_KEY_ID` - Access key de AWS
- `AWS_SECRET_ACCESS_KEY` - Secret key de AWS  
- `AWS_ACCOUNT_ID` - ID de tu cuenta AWS

**Requeridos para la aplicación:**
- `FINNHUB_API_KEY` - Obtener en [finnhub.io/register](https://finnhub.io/register)
- `FINNHUB_WEBHOOK_SECRET` - Secret para verificar webhooks (generar aleatorio)
- `JWT_SECRET` - Secret para firmar tokens JWT (64+ caracteres aleatorios)

**Opcionales (OAuth2 Google):**
- `GOOGLE_CLIENT_ID` - Client ID de [console.cloud.google.com](https://console.cloud.google.com)
- `GOOGLE_CLIENT_SECRET` - Client Secret de Google OAuth

Ver [.github/SETUP.md](../.github/SETUP.md) para instrucciones detalladas.

---

## 🎯 Guía de Despliegue Rápido

### Paso 1: Bootstrap CDK (solo primera vez)

```bash
# Ejecutar bootstrap de CDK
make infra-init-backend

# O manualmente:
cd infrastructure/scripts
./init-terraform-backend.sh  # (renombrado pero funciona para CDK)
```

Este comando ejecuta:
- `cdk bootstrap` en tu cuenta de AWS
- Crea recursos necesarios para CDK (S3 bucket, IAM roles, etc.)

### Paso 2: Configurar Variables de Entorno

```bash
# Exportar secrets ANTES de desplegar
export FINNHUB_API_KEY="tu-api-key-de-finnhub"
export GOOGLE_CLIENT_ID="tu-google-client-id"  # Opcional
export GOOGLE_CLIENT_SECRET="tu-google-client-secret"  # Opcional

# Verificar que estén configuradas
echo $FINNHUB_API_KEY
```

> ⚠️ **IMPORTANTE**: CDK lee las variables de entorno durante el deploy

### Paso 3: Instalar Dependencias de Go

```bash
# Navegar al proyecto CDK
cd infrastructure/cdk

# Descargar dependencias
go mod tidy
go mod download
```

### Paso 4: Sintetizar CloudFormation (Preview)

```bash
# Desde la raíz del proyecto
make infra-synth

# O manualmente:
cd infrastructure/cdk
cdk synth
```

Esto genera los templates de CloudFormation sin desplegar.

### Paso 5: Desplegar Infraestructura

```bash
# Desplegar todo el stack
make infra-deploy

# O manualmente:
cd infrastructure/cdk
cdk deploy --all --require-approval never
```

⏱️ **Tiempo estimado**: 10-15 minutos (RDS tarda más)

CDK mostrará los cambios y pedirá confirmación (usa `--require-approval never` para CI/CD).

### Paso 6: Desplegar Aplicación

```bash
# Construir y desplegar aplicación
make deploy-prod

# O paso a paso:
make aws-build          # Crea divtracker-aws.zip
cd infrastructure/scripts
./deploy.sh prod        # Sube y despliega
```

### Paso 7: Verificar Despliegue

```bash
# Obtener URL de la aplicación
make infra-output

# Verificar health endpoint
make verify-health

# Ver logs en tiempo real
make logs-prod
```

**Ejemplo de output esperado**:
```
Application URL: http://divtracker-prod.us-east-1.elasticbeanstalk.com
Health Status: UP
Database Status: Connected
```

---

## 💰 Desglose de Costos

### 🎁 Primer Año (Free Tier)

| Servicio | Free Tier | Configuración Actual | Costo |
|----------|-----------|---------------------|-------|
| **Elastic Beanstalk** | Incluido | Single instance t2.micro | **$0** |
| **EC2 (t2.micro)** | 750h/mes (12 meses) | 730h/mes | **$0** |
| **RDS (db.t3.micro)** | 750h/mes (12 meses) | 730h/mes | **$0** |
| **EBS Storage** | 20 GB (12 meses) | 20 GB | **$0** |
| **Data Transfer** | 15 GB/mes salida | ~5 GB/mes | **$0** |
| **CloudWatch** | 5 GB logs + métricas básicas | ~2 GB/mes | **$0** |
| **Secrets Manager** | 30 días trial | 3 secrets | **$0** (primeros 30 días) |

**Total Año 1**: **~$1.20/mes** (solo Secrets Manager después de 30 días)

### 💵 Después del Free Tier

| Servicio | Costo Mensual |
|----------|---------------|
| EC2 t2.micro | ~$8.50 |
| RDS db.t3.micro (single-AZ) | ~$15.00 |
| EBS Storage (20 GB) | ~$2.00 |
| Secrets Manager (3 secrets) | ~$1.20 |
| **Total** | **~$26.70/mes** |

> 💡 **Optimización**: Mantener single-AZ, t2.micro instance, y 20GB storage minimiza costos.

---

## 📊 Comandos del Makefile

### Infraestructura

```bash
# Backend de Terraform
make infra-init-backend    # Crear S3 bucket y DynamoDB table

# Gestión de Terraform
make infra-init            # Inicializar Terraform
make infra-plan            # Ver plan de ejecución
make infra-apply           # Aplicar cambios
make infra-destroy         # DESTRUIR toda la infraestructura
make infra-output          # Ver outputs (URLs, endpoints)
make infra-format          # Formatear archivos .tf

# Despliegue de Aplicación
make aws-build             # Construir paquete para AWS
make deploy-prod           # Desplegar a producción
make deploy-dev            # Desplegar a desarrollo

# Monitorización
make logs-prod             # Ver logs de producción
make logs-dev              # Ver logs de desarrollo
make verify-health         # Verificar health endpoint

# Stack Completo
make deploy-full           # infra-apply + deploy-prod
```

### Desarrollo Local

```bash
make setup                 # docker-up + db-migrate
make build                 # Compilar proyecto
make test                  # Ejecutar todos los tests
make run-local             # Ejecutar con perfil local
make docker-up             # Levantar PostgreSQL en Docker
make docker-down           # Detener contenedores
```

---

## 🔍 Monitorización y Logs

### CloudWatch Logs

Los logs de la aplicación se envían automáticamente a CloudWatch:

```bash
# Ver logs desde la CLI
aws logs tail /aws/elasticbeanstalk/divtracker-prod/var/log/eb-engine.log --follow

# O usar el Makefile
make logs-prod
```

### Grupos de Logs Disponibles

- `/aws/elasticbeanstalk/divtracker-prod/var/log/eb-engine.log`: Logs de Elastic Beanstalk
- `/aws/elasticbeanstalk/divtracker-prod/var/log/web.stdout.log`: Logs de la aplicación
- `/aws/elasticbeanstalk/divtracker-prod/healthd/daemon.log`: Health daemon

### Métricas en CloudWatch

Métricas exportadas automáticamente:
- `DivTracker.jvm.memory.used`
- `DivTracker.jvm.threads.live`
- `DivTracker.http.server.requests`
- `DivTracker.watchlist.operations`
- `DivTracker.market.data.ticks`

**Ver métricas**:
```bash
# Consola de AWS
aws cloudwatch list-metrics --namespace DivTracker
```

### Health Checks

Elastic Beanstalk monitoriza automáticamente:
- Path: `/actuator/health`
- Intervalo: 30 segundos
- Timeout: 5 segundos
- Unhealthy threshold: 3 fallos consecutivos

---

## 🛠️ Troubleshooting

### Problema: RDS no conecta desde Elastic Beanstalk

**Síntoma**: Logs muestran `Connection refused` o `timeout`

**Solución**:
```bash
# 1. Verificar Security Group de RDS permite tráfico desde app SG
aws ec2 describe-security-groups --group-ids <RDS_SG_ID>

# 2. Verificar variables de entorno en EB
aws elasticbeanstalk describe-configuration-settings \
  --environment-name divtracker-prod \
  --application-name divtracker

# 3. Verificar que RDS esté en subnets privadas correctas
make infra-output
```

### Problema: Terraform state lock

**Síntoma**: `Error locking state: ConditionalCheckFailedException`

**Solución**:
```bash
# Ver locks activos
aws dynamodb scan --table-name divtracker-terraform-locks

# Forzar unlock (solo si estás seguro de que no hay otra operación)
terraform force-unlock <LOCK_ID>
```

### Problema: Despliegue falla con health check

**Síntoma**: EB marca la app como "Degraded" después de despliegue

**Solución**:
```bash
# 1. Ver logs en tiempo real
make logs-prod

# 2. Verificar health endpoint manualmente
curl https://tu-app.elasticbeanstalk.com/actuator/health

# 3. Verificar variables de entorno
aws elasticbeanstalk describe-environments --environment-names divtracker-prod

# 4. Rollback a versión anterior
aws elasticbeanstalk update-environment \
  --environment-name divtracker-prod \
  --version-label <VERSION_ANTERIOR>
```

### Problema: Secrets Manager no accesible

**Síntoma**: `AccessDeniedException` al leer secrets

**Solución**:
```bash
# Verificar IAM policy del instance profile
aws iam get-role-policy --role-name divtracker-beanstalk-ec2-role \
  --policy-name SecretsManagerAccess

# Añadir política si falta
aws iam put-role-policy --role-name divtracker-beanstalk-ec2-role \
  --policy-name SecretsManagerAccess \
  --policy-document file://secrets-policy.json
```

### Problema: Costos inesperados

**Solución**:
```bash
# Ver costos estimados
aws ce get-cost-and-usage \
  --time-period Start=2024-01-01,End=2024-01-31 \
  --granularity MONTHLY \
  --metrics BlendedCost \
  --group-by Type=SERVICE

# Verificar que RDS esté en single-AZ
aws rds describe-db-instances --db-instance-identifier divtracker-prod

# Verificar que EB sea single instance (sin ALB)
aws elasticbeanstalk describe-configuration-settings \
  --environment-name divtracker-prod \
  --application-name divtracker \
  | grep EnvironmentType
```

---

## 🧹 Limpieza y Destrucción

### Destruir Infraestructura Completa

⚠️ **ADVERTENCIA**: Esto eliminará TODOS los recursos de AWS.

```bash
# Opción 1: Makefile
make infra-destroy

# Opción 2: Terraform directo
cd infrastructure/terraform/environments/prod
terraform destroy
```

### Destrucción Parcial

```bash
# Solo destruir Elastic Beanstalk (mantener RDS)
terraform destroy -target=module.beanstalk

# Solo destruir RDS (mantener app)
terraform destroy -target=module.rds
```

### Backup Antes de Destruir

```bash
# Crear snapshot manual de RDS
aws rds create-db-snapshot \
  --db-instance-identifier divtracker-prod \
  --db-snapshot-identifier divtracker-manual-snapshot-$(date +%Y%m%d)

# Exportar datos de Secrets Manager
aws secretsmanager get-secret-value \
  --secret-id divtracker-prod-db-password \
  --query SecretString --output text > db-password-backup.txt
```

---

## 🔒 Mejores Prácticas de Seguridad

### 1. Gestión de Secrets

✅ **Hacer**:
- Usar AWS Secrets Manager para credenciales
- Rotar passwords regularmente
- No hardcodear secrets en código
- Usar `.gitignore` para `terraform.tfvars`

❌ **No hacer**:
- Commitear `terraform.tfvars` con secrets
- Usar passwords débiles
- Compartir credenciales por email/Slack

### 2. Acceso a RDS

✅ **Configuración actual**:
- RDS en subnets privadas
- Security Group solo permite puerto 5432
- Solo tráfico desde app Security Group
- No acceso público desde Internet

### 3. IAM Roles

✅ **Principio de mínimo privilegio**:
- Instance profile solo tiene permisos necesarios
- Secrets Manager: solo lectura de secrets específicos
- CloudWatch: solo escritura de logs y métricas

### 4. Encriptación

✅ **Datos en reposo**:
- RDS: storage encriptado con AES-256
- Secrets Manager: encriptación por defecto
- S3 (Terraform state): SSE-S3

✅ **Datos en tránsito**:
- HTTPS para tráfico web (configurar certificado SSL)
- PostgreSQL: requiere SSL para conexiones

---

## 📚 Recursos Adicionales

### Documentación Oficial

- [Terraform AWS Provider](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)
- [AWS Elastic Beanstalk](https://docs.aws.amazon.com/elasticbeanstalk/)
- [AWS RDS PostgreSQL](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/CHAP_PostgreSQL.html)
- [AWS Free Tier](https://aws.amazon.com/free/)

### Tutoriales

- [Getting Started with Terraform](https://learn.hashicorp.com/terraform)
- [Elastic Beanstalk Java Tutorial](https://docs.aws.amazon.com/elasticbeanstalk/latest/dg/java-getstarted.html)
- [RDS Best Practices](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/CHAP_BestPractices.html)

---

## 🤝 Contribución

Para modificar la infraestructura:

1. Crear branch para cambios: `git checkout -b infra/descripcion-cambio`
2. Modificar archivos `.tf` necesarios
3. Formatear código: `make infra-format`
4. Validar cambios: `terraform validate`
5. Probar con `terraform plan`
6. Crear PR con descripción detallada
7. Aplicar después de revisión: `make infra-apply`

---

## 📝 Changelog

### v1.0.0 (2024)
- ✅ Arquitectura inicial con VPC, RDS, Elastic Beanstalk
- ✅ Terraform modular con 3 módulos reutilizables
- ✅ Optimización para AWS Free Tier
- ✅ Integración con Secrets Manager
- ✅ CloudWatch Logs y métricas
- ✅ Scripts de automatización
- ✅ Makefile completo

---

## 📞 Soporte

Para problemas o preguntas sobre la infraestructura:

1. Revisar sección de **Troubleshooting** arriba
2. Consultar logs de CloudWatch
3. Verificar configuración con `make infra-output`
4. Crear issue en GitHub con logs y descripción

---

**Desarrollado con ❤️ para proyecto de FP**
