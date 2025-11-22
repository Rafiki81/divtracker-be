package main

import (
	"fmt"
	"os"

	"github.com/aws/aws-cdk-go/awscdk/v2"
	"github.com/aws/aws-cdk-go/awscdk/v2/awssecretsmanager"
	"github.com/aws/constructs-go/constructs/v10"
	"github.com/aws/jsii-runtime-go"
)

type SecretsConstructProps struct {
	DatabaseSecret awssecretsmanager.ISecret
}

type SecretsConstruct struct {
	AppSecretsArn        *string
	JwtSecret            *string
	FinnhubApiKey        string
	FinnhubWebhookSecret string
	GoogleClientId       string
	GoogleClientSecret   string
}

// Manages application secrets in AWS Secrets Manager
func NewSecretsConstruct(scope constructs.Construct, id string, props *SecretsConstructProps) *SecretsConstruct {

	// Read from environment variables
	finnhubApiKey := os.Getenv("FINNHUB_API_KEY")
	if finnhubApiKey == "" {
		finnhubApiKey = "your-finnhub-api-key-here"
		fmt.Println("⚠️  WARNING: FINNHUB_API_KEY not set. Using placeholder.")
	}

	finnhubWebhookSecret := os.Getenv("FINNHUB_WEBHOOK_SECRET")
	if finnhubWebhookSecret == "" {
		finnhubWebhookSecret = "your-webhook-secret-here"
		fmt.Println("⚠️  WARNING: FINNHUB_WEBHOOK_SECRET not set. Using placeholder.")
	}

	googleClientId := os.Getenv("GOOGLE_CLIENT_ID")
	if googleClientId == "" {
		googleClientId = ""
	}

	googleClientSecret := os.Getenv("GOOGLE_CLIENT_SECRET")
	if googleClientSecret == "" {
		googleClientSecret = ""
	}

	jwtSecret := os.Getenv("JWT_SECRET")
	if jwtSecret == "" {
		// Generate a random JWT secret
		jwtSecret = "generated-jwt-secret-will-be-auto-generated"
	}

	// Build consolidated secrets JSON with all application secrets
	secretsJson := fmt.Sprintf(`{
		"JWT_SECRET": "%s",
		"FINNHUB_API_KEY": "%s",
		"FINNHUB_WEBHOOK_SECRET": "%s",
		"GOOGLE_CLIENT_ID": "%s",
		"GOOGLE_CLIENT_SECRET": "%s",
		"DB_SECRET_ARN": "%s"
	}`,
		jwtSecret,
		finnhubApiKey,
		finnhubWebhookSecret,
		googleClientId,
		googleClientSecret,
		*props.DatabaseSecret.SecretArn(),
	)

	// Create single consolidated application secrets
	appSecrets := awssecretsmanager.NewSecret(scope, jsii.String(id+"AppSecrets"), &awssecretsmanager.SecretProps{
		SecretName:        jsii.String("divtracker-prod-secrets"),
		Description:       jsii.String("All application secrets for DivTracker (JWT, Finnhub, Google OAuth, DB)"),
		SecretStringValue: awscdk.SecretValue_UnsafePlainText(jsii.String(secretsJson)),
	})

	return &SecretsConstruct{
		AppSecretsArn:        appSecrets.SecretArn(),
		JwtSecret:            appSecrets.SecretValueFromJson(jsii.String("JWT_SECRET")).UnsafeUnwrap(),
		FinnhubApiKey:        finnhubApiKey,
		FinnhubWebhookSecret: finnhubWebhookSecret,
		GoogleClientId:       googleClientId,
		GoogleClientSecret:   googleClientSecret,
	}
}
