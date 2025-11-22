package main

import (
	"github.com/aws/aws-cdk-go/awscdk/v2"
	"github.com/aws/aws-cdk-go/awscdk/v2/awsec2"
	"github.com/aws/aws-cdk-go/awscdk/v2/awsrds"
	"github.com/aws/aws-cdk-go/awscdk/v2/awssecretsmanager"
	"github.com/aws/constructs-go/constructs/v10"
	"github.com/aws/jsii-runtime-go"
)

type DatabaseConstructProps struct {
	Vpc           awsec2.Vpc
	SecurityGroup awsec2.SecurityGroup
}

type DatabaseConstruct struct {
	DbInstance     awsrds.DatabaseInstance
	DatabaseSecret awssecretsmanager.ISecret
}

// Creates RDS PostgreSQL instance with automated backups and encryption
func NewDatabaseConstruct(scope constructs.Construct, id string, props *DatabaseConstructProps) *DatabaseConstruct {

	// Create credentials with specific username
	credentials := awsrds.Credentials_FromGeneratedSecret(jsii.String("divtracker"), &awsrds.CredentialsBaseOptions{})

	// Create PostgreSQL database instance
	dbInstance := awsrds.NewDatabaseInstance(scope, jsii.String(id+"Instance"), &awsrds.DatabaseInstanceProps{
		Engine: awsrds.DatabaseInstanceEngine_Postgres(&awsrds.PostgresInstanceEngineProps{
			Version: awsrds.PostgresEngineVersion_VER_15(),
		}),
		Credentials:  credentials,
		InstanceType: awsec2.InstanceType_Of(awsec2.InstanceClass_BURSTABLE3, awsec2.InstanceSize_MICRO),
		Vpc:          props.Vpc,
		VpcSubnets: &awsec2.SubnetSelection{
			SubnetType: awsec2.SubnetType_PRIVATE_ISOLATED,
		},
		SecurityGroups:            &[]awsec2.ISecurityGroup{props.SecurityGroup},
		AllocatedStorage:          jsii.Number(20),
		StorageType:               awsrds.StorageType_GP3,
		StorageEncrypted:          jsii.Bool(true),
		MultiAz:                   jsii.Bool(false), // Single-AZ for Free Tier
		PubliclyAccessible:        jsii.Bool(false),
		DatabaseName:              jsii.String("divtracker"),
		BackupRetention:           awscdk.Duration_Days(jsii.Number(7)),
		DeleteAutomatedBackups:    jsii.Bool(true),
		RemovalPolicy:             awscdk.RemovalPolicy_SNAPSHOT,
		DeletionProtection:        jsii.Bool(false),
		EnablePerformanceInsights: jsii.Bool(false), // Disable for Free Tier
		CloudwatchLogsExports: &[]*string{
			jsii.String("postgresql"),
			jsii.String("upgrade"),
		},
		MonitoringInterval: awscdk.Duration_Seconds(jsii.Number(60)),
		InstanceIdentifier: jsii.String("divtracker-prod-db"),
	})

	return &DatabaseConstruct{
		DbInstance:     dbInstance,
		DatabaseSecret: dbInstance.Secret(),
	}
}
