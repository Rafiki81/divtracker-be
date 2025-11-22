package main

import (
	"github.com/aws/aws-cdk-go/awscdk/v2"
	"github.com/aws/constructs-go/constructs/v10"
)

type DivTrackerStackProps struct {
	awscdk.StackProps
}

// Main Infrastructure Stack
func NewDivTrackerStack(scope constructs.Construct, id string, props *DivTrackerStackProps) awscdk.Stack {
	var sprops awscdk.StackProps
	if props != nil {
		sprops = props.StackProps
	}
	stack := awscdk.NewStack(scope, &id, &sprops)

	// Create VPC with public and private subnets
	vpc := NewVpcConstruct(stack, "VPC")

	// Create RDS PostgreSQL database
	database := NewDatabaseConstruct(stack, "Database", &DatabaseConstructProps{
		Vpc:           vpc.Vpc,
		SecurityGroup: vpc.RdsSecurityGroup,
	})

	// Create Secrets for application configuration
	secrets := NewSecretsConstruct(stack, "Secrets", &SecretsConstructProps{
		DatabaseSecret: database.DatabaseSecret,
	})

	// Create Elastic Beanstalk application
	NewElasticBeanstalkConstruct(stack, "Beanstalk", &ElasticBeanstalkConstructProps{
		Vpc:                vpc.Vpc,
		PublicSubnets:      vpc.PublicSubnets,
		SecurityGroup:      vpc.AppSecurityGroup,
		Database:           database.DbInstance,
		DatabaseSecret:     database.DatabaseSecret,
		AppSecretsArn:      secrets.AppSecretsArn,
		DbSecretArn:        database.DatabaseSecret.SecretArn(),
		JwtSecret:          secrets.JwtSecret,
		FinnhubApiKey:      secrets.FinnhubApiKey,
		GoogleClientId:     secrets.GoogleClientId,
		GoogleClientSecret: secrets.GoogleClientSecret,
	})

	return stack
}
