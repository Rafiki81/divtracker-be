.PHONY: help clean compile test run run-local run-dev run-prod docker-up docker-down docker-logs flyway-migrate flyway-info flyway-clean swagger install
.PHONY: infra-init-backend infra-init infra-plan infra-apply infra-destroy infra-output infra-format
.PHONY: aws-build deploy-prod deploy-dev logs-prod logs-dev verify-health deploy-full setup-aws

# Variables
MAVEN := ./mvnw
DOCKER_COMPOSE := docker compose
SPRING_PROFILE ?= local

# Colores para output
GREEN := \033[0;32m
YELLOW := \033[0;33m
RED := \033[0;31m
NC := \033[0m # No Color

## help: Muestra esta ayuda
help:
	@echo "$(GREEN)Comandos disponibles:$(NC)"
	@echo ""
	@echo "  $(YELLOW)make install$(NC)        - Instala dependencias del proyecto"
	@echo "  $(YELLOW)make clean$(NC)          - Limpia el proyecto"
	@echo "  $(YELLOW)make compile$(NC)        - Compila el proyecto"
	@echo "  $(YELLOW)make test$(NC)           - Ejecuta todos los tests"
	@echo "  $(YELLOW)make test-unit$(NC)      - Ejecuta solo tests unitarios"
	@echo "  $(YELLOW)make test-integration$(NC) - Ejecuta solo tests de integración"
	@echo "  $(YELLOW)make run$(NC)            - Ejecuta la aplicación (con Testcontainers)"
	@echo "  $(YELLOW)make run-local$(NC)      - Ejecuta en modo local (con Testcontainers)"
	@echo "  $(YELLOW)make run-dev$(NC)        - Ejecuta con PostgreSQL en Docker Compose"
	@echo "  $(YELLOW)make run-prod$(NC)       - Ejecuta en modo producción"
	@echo "  $(YELLOW)make docker-up$(NC)      - Levanta PostgreSQL con Docker Compose"
	@echo "  $(YELLOW)make docker-down$(NC)    - Detiene PostgreSQL"
	@echo "  $(YELLOW)make docker-logs$(NC)    - Muestra logs de PostgreSQL"
	@echo "  $(YELLOW)make flyway-migrate$(NC) - Ejecuta migraciones de Flyway"
	@echo "  $(YELLOW)make flyway-info$(NC)    - Muestra estado de migraciones"
	@echo "  $(YELLOW)make flyway-clean$(NC)   - Limpia base de datos (¡CUIDADO!)"
	@echo "  $(YELLOW)make swagger$(NC)        - Abre Swagger UI en el navegador"
	@echo "  $(YELLOW)make format$(NC)         - Formatea el código"
	@echo "  $(YELLOW)make package$(NC)        - Empaqueta la aplicación (JAR)"
	@echo ""
	@echo "$(GREEN)Infraestructura AWS (Terraform):$(NC)"
	@echo "  $(YELLOW)make infra-init-backend$(NC) - Bootstrap AWS CDK (primera vez)"
	@echo "  $(YELLOW)make infra-deps$(NC)      - Instala dependencias de Go"
	@echo "  $(YELLOW)make infra-synth$(NC)     - Genera templates CloudFormation"
	@echo "  $(YELLOW)make infra-diff$(NC)      - Compara con stack actual"
	@echo "  $(YELLOW)make infra-deploy$(NC)    - Despliega infraestructura"
	@echo "  $(YELLOW)make infra-destroy$(NC)   - Destruye infraestructura (¡CUIDADO!)"
	@echo "  $(YELLOW)make infra-ls$(NC)        - Lista stacks de CDK"
	@echo ""
	@echo "$(GREEN)Despliegue AWS:$(NC)"
	@echo "  $(YELLOW)make aws-build$(NC)       - Construye paquete para AWS"
	@echo "  $(YELLOW)make deploy-prod$(NC)     - Despliega a producción"
	@echo "  $(YELLOW)make deploy-dev$(NC)      - Despliega a desarrollo"
	@echo "  $(YELLOW)make logs-prod$(NC)       - Ver logs de producción"
	@echo "  $(YELLOW)make verify-health$(NC)   - Verifica salud de la aplicación"
	@echo "  $(YELLOW)make deploy-full$(NC)     - Despliega infraestructura + aplicación"
	@echo ""

## install: Instala dependencias
install:
	@echo "$(GREEN)📦 Instalando dependencias...$(NC)"
	$(MAVEN) clean install -DskipTests

## clean: Limpia el proyecto
clean:
	@echo "$(GREEN)🧹 Limpiando proyecto...$(NC)"
	$(MAVEN) clean

## compile: Compila el proyecto
compile:
	@echo "$(GREEN)🔨 Compilando proyecto...$(NC)"
	$(MAVEN) compile

## test: Ejecuta todos los tests
test:
	@echo "$(GREEN)🧪 Ejecutando tests...$(NC)"
	$(MAVEN) test

## test-unit: Ejecuta solo tests unitarios
test-unit:
	@echo "$(GREEN)🧪 Ejecutando tests unitarios...$(NC)"
	$(MAVEN) test -Dtest="**/*Test"

## test-integration: Ejecuta solo tests de integración
test-integration:
	@echo "$(GREEN)🧪 Ejecutando tests de integración...$(NC)"
	$(MAVEN) test -Dtest="**/*IntegrationTest"

## run: Ejecuta la aplicación con Testcontainers (perfil local)
run:
	@echo "$(GREEN)🚀 Ejecutando aplicación con Testcontainers...$(NC)"
	@echo "$(YELLOW)⚠️  Docker debe estar corriendo$(NC)"
	$(MAVEN) spring-boot:run -Dspring-boot.run.profiles=local

## run-local: Ejecuta en modo local con Docker Compose
run-local: run-dev

## run-dev: Ejecuta con PostgreSQL en Docker Compose
run-dev: docker-up
	@echo "$(GREEN)🚀 Ejecutando aplicación en modo desarrollo...$(NC)"
	@sleep 3
	$(MAVEN) spring-boot:run -Dspring-boot.run.profiles=local

## run-prod: Ejecuta en modo producción
run-prod:
	@echo "$(GREEN)🚀 Ejecutando aplicación en modo producción...$(NC)"
	@echo "$(RED)⚠️  Asegúrate de tener PostgreSQL corriendo en localhost:5432$(NC)"
	$(MAVEN) spring-boot:run

## docker-up: Levanta PostgreSQL con Docker Compose
docker-up:
	@echo "$(GREEN)🐳 Levantando PostgreSQL con Docker Compose...$(NC)"
	@if [ ! -f docker-compose.yml ]; then \
		echo "$(RED)❌ docker-compose.yml no encontrado$(NC)"; \
		exit 1; \
	fi
	$(DOCKER_COMPOSE) up -d
	@echo "$(GREEN)✅ PostgreSQL corriendo en localhost:5432$(NC)"
	@echo "   Usuario: divtracker"
	@echo "   Password: divtracker123"
	@echo "   Base de datos: divtracker_db"

## docker-down: Detiene PostgreSQL
docker-down:
	@echo "$(GREEN)🛑 Deteniendo PostgreSQL...$(NC)"
	$(DOCKER_COMPOSE) down

## docker-logs: Muestra logs de PostgreSQL
docker-logs:
	@echo "$(GREEN)📋 Logs de PostgreSQL:$(NC)"
	$(DOCKER_COMPOSE) logs -f postgres

## flyway-migrate: Ejecuta migraciones de Flyway
flyway-migrate:
	@echo "$(GREEN)🔄 Ejecutando migraciones de Flyway...$(NC)"
	$(MAVEN) flyway:migrate

## flyway-info: Muestra estado de migraciones
flyway-info:
	@echo "$(GREEN)ℹ️  Estado de migraciones:$(NC)"
	$(MAVEN) flyway:info

## flyway-clean: Limpia base de datos (¡CUIDADO!)
flyway-clean:
	@echo "$(RED)⚠️  ¿Estás seguro de limpiar la base de datos? [y/N]$(NC)" && read ans && [ $${ans:-N} = y ]
	@echo "$(GREEN)🧹 Limpiando base de datos...$(NC)"
	$(MAVEN) flyway:clean

## swagger: Abre Swagger UI
swagger:
	@echo "$(GREEN)📚 Abriendo Swagger UI...$(NC)"
	@sleep 2
	@open http://localhost:8080/swagger-ui.html || xdg-open http://localhost:8080/swagger-ui.html || echo "$(YELLOW)Abre manualmente: http://localhost:8080/swagger-ui.html$(NC)"

## format: Formatea el código
format:
	@echo "$(GREEN)✨ Formateando código...$(NC)"
	$(MAVEN) spotless:apply 2>/dev/null || echo "$(YELLOW)⚠️  Spotless no configurado$(NC)"

## package: Empaqueta la aplicación
package:
	@echo "$(GREEN)📦 Empaquetando aplicación...$(NC)"
	$(MAVEN) clean package -DskipTests
	@echo "$(GREEN)✅ JAR generado en: target/divtracker-be-0.0.1-SNAPSHOT.jar$(NC)"

## dev: Entorno completo de desarrollo (Docker + App)
dev: clean compile docker-up
	@echo "$(GREEN)🚀 Iniciando entorno de desarrollo completo...$(NC)"
	@sleep 3
	@echo "$(GREEN)✅ Entorno listo. Ejecutando aplicación...$(NC)"
	$(MAVEN) spring-boot:run -Dspring-boot.run.profiles=local

## stop: Detiene todo
stop: docker-down
	@echo "$(GREEN)🛑 Deteniendo servicios...$(NC)"
	@pkill -f "spring-boot:run" 2>/dev/null || true
	@echo "$(GREEN)✅ Servicios detenidos$(NC)"

## logs: Muestra logs de la aplicación en tiempo real
logs:
	@echo "$(GREEN)📋 Logs de la aplicación:$(NC)"
	@tail -f logs/divtracker-be.log 2>/dev/null || echo "$(YELLOW)No hay archivo de logs todavía$(NC)"

## status: Muestra el estado de los servicios
status:
	@echo "$(GREEN)📊 Estado de servicios:$(NC)"
	@echo ""
	@echo "$(YELLOW)Docker Compose:$(NC)"
	@$(DOCKER_COMPOSE) ps 2>/dev/null || echo "  No corriendo"
	@echo ""
	@echo "$(YELLOW)Aplicación Spring Boot:$(NC)"
	@curl -s http://localhost:8080/actuator/health 2>/dev/null | grep -q "UP" && echo "  ✅ Corriendo" || echo "  ❌ No corriendo"
	@echo ""
	@echo "$(YELLOW)PostgreSQL:$(NC)"
	@docker ps | grep -q postgres && echo "  ✅ Corriendo" || echo "  ❌ No corriendo"

## quick-test: Test rápido (solo compilación y un test)
quick-test:
	@echo "$(GREEN)⚡ Test rápido...$(NC)"
	$(MAVEN) clean compile test -Dtest=DivtrackerBeApplicationTests

## build: Build completo (limpia, compila, tests y empaqueta)
build:
	@echo "$(GREEN)🏗️  Build completo...$(NC)"
	$(MAVEN) clean package

## watch: Ejecuta en modo watch (recarga automática)
watch:
	@echo "$(GREEN)👀 Ejecutando en modo watch...$(NC)"
	$(MAVEN) spring-boot:run -Dspring-boot.run.profiles=local -Dspring-boot.run.jvmArguments="-Dspring.devtools.restart.enabled=true"

# ============================================
# Infrastructure Commands (AWS CDK with Go)
# ============================================

## infra-deps: Instala dependencias Go
infra-deps:
	@echo "$(GREEN)📦 Instalando dependencias Go...$(NC)"
	cd $(CDK_DIR) && go mod download

## infra-synth: Preview de CloudFormation
infra-synth:
	@echo "$(GREEN)🔍 Generando templates...$(NC)"
	cd $(CDK_DIR) && cdk synth

## infra-diff: Ver diferencias con stack actual
infra-diff:
	@echo "$(GREEN)📊 Comparando cambios...$(NC)"
	cd $(CDK_DIR) && cdk diff

## infra-deploy: Desplegar infraestructura (manual)
infra-deploy:
	@echo "$(GREEN)🚀 Desplegando infraestructura...$(NC)"
	@echo "$(YELLOW)⚠️  Usa GitHub Actions para deployments automáticos$(NC)"
	cd $(CDK_DIR) && cdk deploy

## infra-destroy: Destruir infraestructura (manual)
infra-destroy:
	@echo "$(RED)💥 Destruyendo infraestructura...$(NC)"
	@echo "$(YELLOW)⚠️  Usa GitHub Actions para mayor seguridad$(NC)"
	cd $(CDK_DIR) && cdk destroy

## infra-output: Ver outputs de infraestructura
infra-output:
	@echo "$(GREEN)📊 Outputs:$(NC)"
	@aws cloudformation describe-stacks \
		--stack-name DivtrackerStack \
		--query 'Stacks[0].Outputs' \
		--output table

#
# AWS Elastic Beanstalk
#

# ============================================
# AWS Deployment Commands
# ============================================

## aws-build: Construye paquete de despliegue para AWS
aws-build:
	@echo "$(GREEN)📦 Construyendo paquete de despliegue para AWS...$(NC)"
	@cd infrastructure/scripts && ./build-for-aws.sh

## deploy-prod: Despliega a entorno de producción
deploy-prod:
	@echo "$(GREEN)🚀 Desplegando a producción...$(NC)"
	@cd infrastructure/scripts && ./deploy.sh prod

## deploy-dev: Despliega a entorno de desarrollo
deploy-dev:
	@echo "$(GREEN)🚀 Desplegando a desarrollo...$(NC)"
	@cd infrastructure/scripts && ./deploy.sh dev

## logs-prod: Ver logs de producción
logs-prod:
	@echo "$(GREEN)📋 Obteniendo logs de producción...$(NC)"
	@aws elasticbeanstalk logs retrieve \
		--environment-name divtracker-prod \
		--region us-east-1

## logs-dev: Ver logs de desarrollo
logs-dev:
	@echo "$(GREEN)📋 Obteniendo logs de desarrollo...$(NC)"
	@aws elasticbeanstalk logs retrieve \
		--environment-name divtracker-dev \
		--region us-east-1

## verify-health: Verifica salud de la aplicación
verify-health:
	@echo "$(GREEN)🏥 Verificando salud de la aplicación...$(NC)"
	@URL=$$(cd infrastructure/terraform/environments/prod && terraform output -raw application_url 2>/dev/null) && \
	if [ -n "$$URL" ]; then \
		curl -f "$$URL/actuator/health" && echo "" && echo "$(GREEN)✓ Aplicación saludable$(NC)" || echo "$(RED)✗ Health check falló$(NC)"; \
	else \
		echo "$(RED)No se pudo determinar la URL de la aplicación$(NC)"; \
	fi

# ============================================
# Combined Commands
# ============================================

## setup-aws: Configura toda la infraestructura AWS
setup-aws: infra-init-backend infra-deps
	@echo "$(GREEN)✓ AWS CDK configurado!$(NC)"
	@echo ""
	@echo "Siguiente paso:"
	@echo "  1. Exportar variables: export FINNHUB_API_KEY=tu-key"
	@echo "  2. Ejecutar 'make infra-synth' para preview"
	@echo "  3. Ejecutar 'make infra-deploy' para crear infraestructura"
	@echo "  4. Ejecutar 'make deploy-prod' para desplegar aplicación"

## deploy-full: Despliega stack completo (infraestructura + aplicación)
deploy-full: infra-deploy deploy-prod
	@echo "$(GREEN)✓ Despliegue completo finalizado!$(NC)"
	@cd infrastructure/cdk && cdk list

.DEFAULT_GOAL := help
