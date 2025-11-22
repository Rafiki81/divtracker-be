#!/bin/bash

# Bootstrap AWS CDK Environment
# This script bootstraps the AWS CDK in your account for CloudFormation deployments
# Run this ONCE before deploying CDK stacks

set -e

# Configuration
AWS_REGION="${CDK_DEFAULT_REGION:-us-east-1}"

echo "🚀 Bootstrapping AWS CDK..."
echo ""

# Check if AWS CLI is installed
if ! command -v aws &> /dev/null; then
    echo "❌ AWS CLI is not installed. Please install it first."
    exit 1
fi

# Check if CDK CLI is installed
if ! command -v cdk &> /dev/null; then
    echo "❌ AWS CDK CLI is not installed."
    echo "   Install with: npm install -g aws-cdk"
    exit 1
fi

# Check if Go is installed
if ! command -v go &> /dev/null; then
    echo "❌ Go is not installed. Please install Go 1.21+ first."
    echo "   Download from: https://go.dev/dl/"
    exit 1
fi

# Check if AWS credentials are configured
if ! aws sts get-caller-identity &> /dev/null; then
    echo "❌ AWS credentials are not configured. Run 'aws configure' first."
    exit 1
fi

# Get AWS Account ID
AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

echo "✅ Prerequisites check passed"
echo ""
echo "📋 Configuration:"
echo "   AWS Account:  $AWS_ACCOUNT_ID"
echo "   AWS Region:   $AWS_REGION"
echo "   CDK Version:  $(cdk --version)"
echo "   Go Version:   $(go version | awk '{print $3}')"
echo ""

# Check required environment variables
echo "🔍 Checking required environment variables..."
if [ -z "$FINNHUB_API_KEY" ]; then
    echo "❌ FINNHUB_API_KEY is not set"
    echo "   Please export: export FINNHUB_API_KEY=your-api-key"
    exit 1
fi
echo "✅ FINNHUB_API_KEY configured"

if [ -z "$GOOGLE_CLIENT_ID" ]; then
    echo "⚠️  GOOGLE_CLIENT_ID not set (optional for Google OAuth)"
fi

if [ -z "$GOOGLE_CLIENT_SECRET" ]; then
    echo "⚠️  GOOGLE_CLIENT_SECRET not set (optional for Google OAuth)"
fi
echo ""

# Install Go dependencies
echo "📦 Installing Go dependencies..."
cd "$(dirname "$0")/../cdk"
go mod tidy
go mod download
echo "✅ Go dependencies installed"
echo ""

# Bootstrap CDK
echo "🔧 Bootstrapping AWS CDK environment..."
cdk bootstrap aws://$AWS_ACCOUNT_ID/$AWS_REGION

echo ""
echo "🎉 AWS CDK bootstrapped successfully!"
echo ""
echo "📋 Next steps:"
echo "   1. make infra-synth   # Preview CloudFormation templates"
echo "   2. make infra-diff    # See differences with deployed stack"
echo "   3. make infra-deploy  # Deploy infrastructure to AWS"
echo ""
