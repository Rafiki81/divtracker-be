# 🚀 Setup GitHub Actions - Guía Rápida

## 📋 Configurar Secrets y Variables

### 1️⃣ Secrets (Settings → Secrets and variables → Actions → Secrets)

| Nombre | Valor | Descripción |
|--------|-------|-------------|
| `AWS_ACCESS_KEY_ID` | `AKIA...` | AWS Access Key |
| `AWS_SECRET_ACCESS_KEY` | `wJal...` | AWS Secret Key |
| `FINNHUB_API_KEY` | `tu_key` | API de Finnhub |
| `FINNHUB_WEBHOOK_SECRET` | `random_string` | Secret para verificar webhooks |
| `JWT_SECRET` | `64+_caracteres` | Secret para firmar tokens JWT |
| `GOOGLE_CLIENT_SECRET` | `GOCSPX-...` | Google OAuth Secret (opcional) |
| `FIREBASE_CREDENTIALS_JSON` | `base64_json` | Firebase Service Account JSON (base64 encoded) |
| `FIREBASE_PROJECT_ID` | `tu-proyecto` | ID del proyecto Firebase |

### 2️⃣ Variables (Settings → Secrets and variables → Actions → Variables)

| Nombre | Valor | Descripción |
|--------|-------|-------------|
| `AWS_ACCOUNT_ID` | `123456789012` | Tu AWS Account ID |
| `GOOGLE_CLIENT_ID` | `123-abc.apps...` | Google OAuth ID (opcional) |

---

## 🔥 Configurar Firebase (Push Notifications)

### Paso 1: Crear proyecto en Firebase
```
Firebase Console → Add project → Nombre: divtracker
```

### Paso 2: Generar Service Account Key
```
Project Settings → Service accounts → Generate new private key
```

### Paso 3: Codificar JSON en base64
```bash
# macOS/Linux
cat firebase-service-account.json | base64 -w0
# o
cat firebase-service-account.json | base64 | tr -d '\n'

# El resultado será algo como:
# eyJ0eXBlIjoic2VydmljZV9hY2NvdW50IiwicHJvamVjdF9pZCI6Ii...
```

### Paso 4: Guardar como Secret
```
GitHub → Settings → Secrets → New repository secret
Name: FIREBASE_CREDENTIALS_JSON
Value: (pegar el base64 generado)

Name: FIREBASE_PROJECT_ID  
Value: tu-proyecto-firebase
```

> ⚠️ **Importante**: Si no configuras Firebase, FCM estará deshabilitado (`fcm.enabled=false`) y la app funcionará sin push notifications.

---

## 🎯 Workflows Disponibles

### 🏗️ **1. Crear Infraestructura** (Manual)
```
Actions → 🏗️ Crear Infraestructura → Run workflow
```
**Crea:** VPC, RDS, Elastic Beanstalk, Secrets Manager

---

### 🚀 **2. Desplegar Aplicación** (Automático)
```
git push origin main  ← Se ejecuta automáticamente
```
**O manual:**
```
Actions → 🚀 Desplegar Aplicación → Run workflow
```

---

### 💥 **3. Destruir Infraestructura** (Manual con confirmación)
```
Actions → 💥 Destruir Infraestructura → Run workflow
Input: DESTRUIR
```
⚠️ **Elimina TODO**

---

## 🔐 Obtener AWS Credentials

### Paso 1: Crear usuario IAM
```bash
# AWS Console → IAM → Users → Create user
Nombre: github-actions-divtracker
```

### Paso 2: Adjuntar políticas
- `AdministratorAccess-AWSElasticBeanstalk`
- `AmazonRDSFullAccess`
- `AmazonVPCFullAccess`
- `IAMFullAccess`
- `AWSCloudFormationFullAccess`

### Paso 3: Crear Access Key
```
Security credentials → Create access key → Application outside AWS
```

### Paso 4: Copiar credenciales
```
Access Key ID: AKIA...
Secret Access Key: wJal...
```

---

## 📊 AWS Account ID

```bash
# Opción 1: AWS Console
Click en tu nombre (arriba derecha) → Aparece el Account ID

# Opción 2: AWS CLI
aws sts get-caller-identity --query Account --output text
```

---

## 🎬 Orden de Ejecución (Primera vez)

### 1. Configurar Secrets y Variables en GitHub
✅ Ver sección de arriba

### 2. Bootstrap CDK (desde tu local, solo primera vez)
```bash
cd infrastructure/cdk
export AWS_ACCOUNT_ID=123456789012
export AWS_REGION=us-east-1
export FINNHUB_API_KEY=tu_key
cdk bootstrap aws://$AWS_ACCOUNT_ID/$AWS_REGION
```

### 3. Crear Infraestructura
```
GitHub Actions → 🏗️ Crear Infraestructura → Run workflow
```
⏱️ **15 minutos**

### 4. Desplegar Aplicación
```bash
git add .
git commit -m "initial deployment"
git push origin main
```
⏱️ **5 minutos**

### 5. Verificar
```
Ver el summary del workflow para obtener la URL
http://divtracker-prod.xxxxx.elasticbeanstalk.com
```

---

## 🔄 Workflow de Desarrollo

### Desarrollo normal:
```bash
# Hacer cambios en el código
git add .
git commit -m "feat: nueva funcionalidad"
git push origin main

# ← GitHub Actions despliega automáticamente
```

### Ver logs:
```bash
make logs-prod
# O en GitHub: Actions → último workflow → Deploy a AWS
```

---

## 🐛 Troubleshooting

### Error: "Unable to locate credentials"
✅ Verifica que los Secrets estén configurados correctamente

### Error: "Need to perform AWS calls for account XXX"
✅ Ejecuta `cdk bootstrap` desde tu local primero

### Error: "Application divtracker-prod does not exist"
✅ Ejecuta primero el workflow "Crear Infraestructura"

### Error: "Health check failed"
✅ Ver logs: `make logs-prod` o en AWS Console

---

## 💰 Costos

| Servicio | Free Tier | Post Free Tier |
|----------|-----------|----------------|
| EC2 t2.micro | 750h/mes ✅ | ~$8/mes |
| RDS db.t3.micro | 750h/mes ✅ | ~$15/mes |
| GitHub Actions | 2000 min/mes ✅ | $0.008/min |

**Total año 1:** ~$0-2/mes  
**Después:** ~$25/mes

---

## 📞 Comandos Útiles

```bash
# Ver outputs de infraestructura
make infra-output

# Ver logs
make logs-prod

# Health check
make verify-health

# Simular CI localmente
make ci
```

---

## ✅ Checklist

- [ ] Secrets configurados en GitHub
  - [ ] AWS_ACCESS_KEY_ID
  - [ ] AWS_SECRET_ACCESS_KEY
  - [ ] FINNHUB_API_KEY
  - [ ] FINNHUB_WEBHOOK_SECRET
  - [ ] JWT_SECRET
  - [ ] FIREBASE_CREDENTIALS_JSON (opcional)
  - [ ] FIREBASE_PROJECT_ID (opcional)
- [ ] Variables configuradas en GitHub
  - [ ] AWS_ACCOUNT_ID
  - [ ] GOOGLE_CLIENT_ID (opcional)
- [ ] CDK bootstrap ejecutado
- [ ] Infraestructura creada
- [ ] Aplicación desplegada
- [ ] Health check ✅
- [ ] URL funcionando

🎉 **¡Listo!**
