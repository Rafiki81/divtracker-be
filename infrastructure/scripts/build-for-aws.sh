#!/bin/bash

# Build Application for AWS Deployment
# Creates deployment package with JAR, Procfile, and .ebextensions

set -e

echo "🔨 Building DivTracker for AWS deployment..."
echo ""

# Check prerequisites
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven is not installed"
    exit 1
fi

# Navigate to project root
cd "$(dirname "$0")/../.."

# Clean previous builds
echo "🧹 Cleaning previous builds..."
./mvnw clean
echo ""

# Run tests
echo "🧪 Running tests..."
./mvnw test
if [ $? -ne 0 ]; then
    echo "❌ Tests failed. Fix tests before deploying."
    exit 1
fi
echo "✅ All tests passed"
echo ""

# Build JAR
echo "📦 Building JAR..."
./mvnw package -DskipTests
echo "✅ JAR built successfully"
echo ""

# Create deployment package
echo "📁 Creating deployment package..."
mkdir -p aws-deploy

# Copy JAR
cp target/divtracker-be-*.jar aws-deploy/divtracker-be.jar

# Copy or create Procfile
if [ -f "Procfile" ]; then
    cp Procfile aws-deploy/
else
    echo "web: java -Dserver.port=5000 -Xms256m -Xmx512m -XX:MaxRAMPercentage=75.0 -jar divtracker-be.jar" > aws-deploy/Procfile
fi

# Copy .ebextensions if exists
if [ -d ".ebextensions" ]; then
    cp -r .ebextensions aws-deploy/
fi

# Create ZIP package
cd aws-deploy
zip -r ../divtracker-aws.zip . -q
cd ..

# Cleanup
rm -rf aws-deploy

# Show package info
if [ -f "divtracker-aws.zip" ]; then
    SIZE=$(du -h divtracker-aws.zip | cut -f1)
    echo "✅ Deployment package created: divtracker-aws.zip (${SIZE})"
    echo ""
    echo "📋 Package contents:"
    unzip -l divtracker-aws.zip | head -20
    echo ""
    echo "✨ Ready to deploy! Run: make deploy-prod"
else
    echo "❌ Failed to create deployment package"
    exit 1
fi
