#!/bin/bash

# Deploy Application to AWS Elastic Beanstalk
# This script builds, packages, and deploys the application

set -e

# Configuration
ENVIRONMENT="${1:-prod}"
AWS_REGION="us-east-1"
S3_BUCKET="divtracker-deployments"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
VERSION_LABEL="v${TIMESTAMP}"

echo "🚀 Deploying DivTracker to AWS Elastic Beanstalk..."
echo "   Environment: $ENVIRONMENT"
echo "   Version:     $VERSION_LABEL"
echo ""

# Check prerequisites
if ! command -v aws &> /dev/null; then
    echo "❌ AWS CLI is not installed"
    exit 1
fi

if ! command -v mvn &> /dev/null; then
    echo "❌ Maven is not installed"
    exit 1
fi

# Navigate to project root
cd "$(dirname "$0")/../.."

# Step 1: Build application
echo "📦 Building application..."
./mvnw clean package -DskipTests
echo "✅ Build complete"
echo ""

# Step 2: Create deployment package
echo "📁 Creating deployment package..."
mkdir -p aws-deploy
cp target/divtracker-be-*.jar aws-deploy/divtracker-be.jar
cp Procfile aws-deploy/ 2>/dev/null || echo "web: java -Dserver.port=5000 -jar divtracker-be.jar" > aws-deploy/Procfile
cp -r .ebextensions aws-deploy/ 2>/dev/null || true

cd aws-deploy
zip -r ../divtracker-aws-${VERSION_LABEL}.zip . -q
cd ..
rm -rf aws-deploy
echo "✅ Deployment package created: divtracker-aws-${VERSION_LABEL}.zip"
echo ""

# Step 3: Upload to S3
echo "☁️  Uploading to S3..."
# Create bucket if it doesn't exist
aws s3 mb "s3://${S3_BUCKET}" --region "$AWS_REGION" 2>/dev/null || true
aws s3 cp "divtracker-aws-${VERSION_LABEL}.zip" "s3://${S3_BUCKET}/" --region "$AWS_REGION"
echo "✅ Uploaded to S3: s3://${S3_BUCKET}/divtracker-aws-${VERSION_LABEL}.zip"
echo ""

# Step 4: Get Elastic Beanstalk application name and environment
cd infrastructure/terraform/environments/${ENVIRONMENT}
APP_NAME=$(terraform output -raw beanstalk_application_name 2>/dev/null || echo "divtracker-${ENVIRONMENT}")
ENV_NAME=$(terraform output -raw beanstalk_environment_name 2>/dev/null || echo "divtracker-${ENVIRONMENT}")
cd ../../../../

echo "🔧 Target:"
echo "   Application: $APP_NAME"
echo "   Environment: $ENV_NAME"
echo ""

# Step 5: Create application version in Elastic Beanstalk
echo "📋 Creating application version..."
aws elasticbeanstalk create-application-version \
    --application-name "$APP_NAME" \
    --version-label "$VERSION_LABEL" \
    --source-bundle S3Bucket="${S3_BUCKET}",S3Key="divtracker-aws-${VERSION_LABEL}.zip" \
    --region "$AWS_REGION" \
    > /dev/null
echo "✅ Application version created: $VERSION_LABEL"
echo ""

# Step 6: Deploy to environment
echo "🚀 Deploying to environment..."
aws elasticbeanstalk update-environment \
    --application-name "$APP_NAME" \
    --environment-name "$ENV_NAME" \
    --version-label "$VERSION_LABEL" \
    --region "$AWS_REGION" \
    > /dev/null
echo "✅ Deployment initiated"
echo ""

# Step 7: Wait for deployment to complete
echo "⏳ Waiting for deployment to complete (this may take 3-5 minutes)..."
aws elasticbeanstalk wait environment-updated \
    --application-name "$APP_NAME" \
    --environment-name "$ENV_NAME" \
    --region "$AWS_REGION" || true
echo ""

# Step 8: Check health
echo "🏥 Checking application health..."
APP_URL=$(cd infrastructure/terraform/environments/${ENVIRONMENT} && terraform output -raw application_url 2>/dev/null || echo "")

if [ -n "$APP_URL" ]; then
    sleep 10
    if curl -f -s "${APP_URL}/actuator/health" > /dev/null; then
        echo "✅ Application is healthy!"
        echo ""
        echo "🎉 Deployment successful!"
        echo ""
        echo "📋 Application Details:"
        echo "   URL:         $APP_URL"
        echo "   Health:      ${APP_URL}/actuator/health"
        echo "   API Docs:    ${APP_URL}/swagger-ui.html"
        echo "   Version:     $VERSION_LABEL"
        echo ""
        echo "📊 View logs with: make logs-${ENVIRONMENT}"
    else
        echo "⚠️  Health check failed. Check logs with: make logs-${ENVIRONMENT}"
        exit 1
    fi
else
    echo "⚠️  Could not determine application URL. Run 'make infra-output' to view details."
fi

# Cleanup
rm -f "divtracker-aws-${VERSION_LABEL}.zip"

echo ""
echo "✨ Deployment complete!"
