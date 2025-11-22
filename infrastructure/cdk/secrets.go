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

	// Generate JWT secret
	jwtSecretGen := awssecretsmanager.NewSecret(scope, jsii.String(id+"JwtSecret"), &awssecretsmanager.SecretProps{
		GenerateSecretString: &awssecretsmanager.SecretStringGenerator{
			SecretStringTemplate: jsii.String("{}"),
			GenerateStringKey:    jsii.String("secret"),
			ExcludePunctuation:   jsii.Bool(true),
			PasswordLength:       jsii.Number(64),
		},
	})

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
		// If not provided, use the auto-generated one
		jwtSecret = *jwtSecretGen.SecretValueFromJson(jsii.String("secret")).UnsafeUnwrap()
	}

	// Build secrets JSON
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

	// Create application secrets
	appSecrets := awssecretsmanager.NewSecret(scope, jsii.String(id+"AppSecrets"), &awssecretsmanager.SecretProps{
		SecretName:        jsii.String("divtracker-prod-app-secrets"),
		Description:       jsii.String("Application secrets for DivTracker"),
		SecretStringValue: awscdk.SecretValue_UnsafePlainText(jsii.String(secretsJson)),
	})

	return &SecretsConstruct{
		AppSecretsArn:        appSecrets.SecretArn(),
		JwtSecret:            jwtSecretGen.SecretValueFromJson(jsii.String("secret")).UnsafeUnwrap(),
		FinnhubApiKey:        finnhubApiKey,
		FinnhubWebhookSecret: finnhubWebhookSecret,
		GoogleClientId:       googleClientId,
		GoogleClientSecret:   googleClientSecret,
	}
}
