package main

import (
	"fmt"

	"github.com/aws/aws-cdk-go/awscdk/v2"
	"github.com/aws/aws-cdk-go/awscdk/v2/awsec2"
	"github.com/aws/aws-cdk-go/awscdk/v2/awselasticbeanstalk"
	"github.com/aws/aws-cdk-go/awscdk/v2/awsiam"
	"github.com/aws/aws-cdk-go/awscdk/v2/awsrds"
	"github.com/aws/aws-cdk-go/awscdk/v2/awssecretsmanager"
	"github.com/aws/constructs-go/constructs/v10"
	"github.com/aws/jsii-runtime-go"
)

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

// Creates EB application and environment with proper IAM roles
func NewElasticBeanstalkConstruct(scope constructs.Construct, id string, props *ElasticBeanstalkConstructProps) {

	// Create IAM role for EC2 instances
	ec2Role := awsiam.NewRole(scope, jsii.String(id+"EC2Role"), &awsiam.RoleProps{
		AssumedBy: awsiam.NewServicePrincipal(jsii.String("ec2.amazonaws.com"), nil),
		ManagedPolicies: &[]awsiam.IManagedPolicy{
			awsiam.ManagedPolicy_FromAwsManagedPolicyName(jsii.String("AWSElasticBeanstalkWebTier")),
			awsiam.ManagedPolicy_FromAwsManagedPolicyName(jsii.String("AWSElasticBeanstalkWorkerTier")),
			awsiam.ManagedPolicy_FromAwsManagedPolicyName(jsii.String("AWSElasticBeanstalkMulticontainerDocker")),
		},
		RoleName: jsii.String("divtracker-eb-ec2-role"),
	})

	// Add Secrets Manager access policy
	ec2Role.AddToPolicy(awsiam.NewPolicyStatement(&awsiam.PolicyStatementProps{
		Effect: awsiam.Effect_ALLOW,
		Actions: &[]*string{
			jsii.String("secretsmanager:GetSecretValue"),
			jsii.String("secretsmanager:DescribeSecret"),
		},
		Resources: &[]*string{
			props.AppSecretsArn,
			props.DbSecretArn,
		},
	}))

	// Create instance profile
	instanceProfile := awsiam.NewCfnInstanceProfile(scope, jsii.String(id+"InstanceProfile"), &awsiam.CfnInstanceProfileProps{
		Roles:               &[]*string{ec2Role.RoleName()},
		InstanceProfileName: jsii.String("divtracker-eb-instance-profile"),
	})

	// Create IAM role for Elastic Beanstalk service
	serviceRole := awsiam.NewRole(scope, jsii.String(id+"ServiceRole"), &awsiam.RoleProps{
		AssumedBy: awsiam.NewServicePrincipal(jsii.String("elasticbeanstalk.amazonaws.com"), nil),
		ManagedPolicies: &[]awsiam.IManagedPolicy{
			awsiam.ManagedPolicy_FromAwsManagedPolicyName(jsii.String("service-role/AWSElasticBeanstalkEnhancedHealth")),
			awsiam.ManagedPolicy_FromAwsManagedPolicyName(jsii.String("service-role/AWSElasticBeanstalkService")),
		},
		RoleName: jsii.String("divtracker-eb-service-role"),
	})

	// Create Elastic Beanstalk application
	application := awselasticbeanstalk.NewCfnApplication(scope, jsii.String(id+"Application"), &awselasticbeanstalk.CfnApplicationProps{
		ApplicationName: jsii.String("divtracker-prod"),
		Description:     jsii.String("DivTracker Backend Application"),
	})

	// Build subnet IDs string
	subnetIds := ""
	for i, subnet := range *props.PublicSubnets {
		if i > 0 {
			subnetIds += ","
		}
		subnetIds += *subnet.SubnetId()
	}

	// Build option settings using proper CDK structure
	optionSettings := &[]interface{}{
		&awselasticbeanstalk.CfnEnvironment_OptionSettingProperty{
			Namespace:  jsii.String("aws:elasticbeanstalk:environment"),
			OptionName: jsii.String("EnvironmentType"),
			Value:      jsii.String("SingleInstance"),
		},
		&awselasticbeanstalk.CfnEnvironment_OptionSettingProperty{
			Namespace:  jsii.String("aws:elasticbeanstalk:environment"),
			OptionName: jsii.String("ServiceRole"),
			Value:      serviceRole.RoleArn(),
		},
		&awselasticbeanstalk.CfnEnvironment_OptionSettingProperty{
			Namespace:  jsii.String("aws:autoscaling:launchconfiguration"),
			OptionName: jsii.String("InstanceType"),
			Value:      jsii.String("t2.micro"),
		},
		&awselasticbeanstalk.CfnEnvironment_OptionSettingProperty{
			Namespace:  jsii.String("aws:autoscaling:launchconfiguration"),
			OptionName: jsii.String("IamInstanceProfile"),
			Value:      instanceProfile.Ref(),
		},
		&awselasticbeanstalk.CfnEnvironment_OptionSettingProperty{
			Namespace:  jsii.String("aws:autoscaling:launchconfiguration"),
			OptionName: jsii.String("SecurityGroups"),
			Value:      props.SecurityGroup.SecurityGroupId(),
		},
		&awselasticbeanstalk.CfnEnvironment_OptionSettingProperty{
			Namespace:  jsii.String("aws:ec2:vpc"),
			OptionName: jsii.String("VPCId"),
			Value:      props.Vpc.VpcId(),
		},
		&awselasticbeanstalk.CfnEnvironment_OptionSettingProperty{
			Namespace:  jsii.String("aws:ec2:vpc"),
			OptionName: jsii.String("Subnets"),
			Value:      jsii.String(subnetIds),
		},
		&awselasticbeanstalk.CfnEnvironment_OptionSettingProperty{
			Namespace:  jsii.String("aws:ec2:vpc"),
			OptionName: jsii.String("AssociatePublicIpAddress"),
			Value:      jsii.String("true"),
		},
		&awselasticbeanstalk.CfnEnvironment_OptionSettingProperty{
			Namespace:  jsii.String("aws:elasticbeanstalk:application"),
			OptionName: jsii.String("Application Healthcheck URL"),
			Value:      jsii.String("/actuator/health"),
		},
		&awselasticbeanstalk.CfnEnvironment_OptionSettingProperty{
			Namespace:  jsii.String("aws:elasticbeanstalk:healthreporting:system"),
			OptionName: jsii.String("SystemType"),
			Value:      jsii.String("enhanced"),
		},
		&awselasticbeanstalk.CfnEnvironment_OptionSettingProperty{
			Namespace:  jsii.String("aws:elasticbeanstalk:cloudwatch:logs"),
			OptionName: jsii.String("StreamLogs"),
			Value:      jsii.String("true"),
		},
		&awselasticbeanstalk.CfnEnvironment_OptionSettingProperty{
			Namespace:  jsii.String("aws:elasticbeanstalk:cloudwatch:logs"),
			OptionName: jsii.String("DeleteOnTerminate"),
			Value:      jsii.String("false"),
		},
		&awselasticbeanstalk.CfnEnvironment_OptionSettingProperty{
			Namespace:  jsii.String("aws:elasticbeanstalk:cloudwatch:logs"),
			OptionName: jsii.String("RetentionInDays"),
			Value:      jsii.String("7"),
		},
		&awselasticbeanstalk.CfnEnvironment_OptionSettingProperty{
			Namespace:  jsii.String("aws:elasticbeanstalk:application:environment"),
			OptionName: jsii.String("SPRING_PROFILES_ACTIVE"),
			Value:      jsii.String("aws"),
		},
		&awselasticbeanstalk.CfnEnvironment_OptionSettingProperty{
			Namespace:  jsii.String("aws:elasticbeanstalk:application:environment"),
			OptionName: jsii.String("RDS_HOSTNAME"),
			Value:      props.Database.DbInstanceEndpointAddress(),
		},
		&awselasticbeanstalk.CfnEnvironment_OptionSettingProperty{
			Namespace:  jsii.String("aws:elasticbeanstalk:application:environment"),
			OptionName: jsii.String("RDS_PORT"),
			Value:      props.Database.DbInstanceEndpointPort(),
		},
		&awselasticbeanstalk.CfnEnvironment_OptionSettingProperty{
			Namespace:  jsii.String("aws:elasticbeanstalk:application:environment"),
			OptionName: jsii.String("RDS_DB_NAME"),
			Value:      jsii.String("divtracker"),
		},
		&awselasticbeanstalk.CfnEnvironment_OptionSettingProperty{
			Namespace:  jsii.String("aws:elasticbeanstalk:application:environment"),
			OptionName: jsii.String("RDS_USERNAME"),
			Value:      jsii.String("divtracker"),
		},
		&awselasticbeanstalk.CfnEnvironment_OptionSettingProperty{
			Namespace:  jsii.String("aws:elasticbeanstalk:application:environment"),
			OptionName: jsii.String("DB_PASSWORD"),
			Value:      props.DatabaseSecret.SecretValueFromJson(jsii.String("password")).UnsafeUnwrap(),
		},
		&awselasticbeanstalk.CfnEnvironment_OptionSettingProperty{
			Namespace:  jsii.String("aws:elasticbeanstalk:application:environment"),
			OptionName: jsii.String("JWT_SECRET"),
			Value:      props.JwtSecret,
		},
		&awselasticbeanstalk.CfnEnvironment_OptionSettingProperty{
			Namespace:  jsii.String("aws:elasticbeanstalk:application:environment"),
			OptionName: jsii.String("FINNHUB_API_KEY"),
			Value:      jsii.String(props.FinnhubApiKey),
		},
		&awselasticbeanstalk.CfnEnvironment_OptionSettingProperty{
			Namespace:  jsii.String("aws:elasticbeanstalk:application:environment"),
			OptionName: jsii.String("FINNHUB_WEBHOOK_SECRET"),
			Value:      jsii.String(props.FinnhubWebhookSecret),
		},
		&awselasticbeanstalk.CfnEnvironment_OptionSettingProperty{
			Namespace:  jsii.String("aws:elasticbeanstalk:application:environment"),
			OptionName: jsii.String("GOOGLE_CLIENT_ID"),
			Value:      jsii.String(props.GoogleClientId),
		},
		&awselasticbeanstalk.CfnEnvironment_OptionSettingProperty{
			Namespace:  jsii.String("aws:elasticbeanstalk:application:environment"),
			OptionName: jsii.String("GOOGLE_CLIENT_SECRET"),
			Value:      jsii.String(props.GoogleClientSecret),
		},
		&awselasticbeanstalk.CfnEnvironment_OptionSettingProperty{
			Namespace:  jsii.String("aws:elasticbeanstalk:application:environment"),
			OptionName: jsii.String("APP_SECRETS_ARN"),
			Value:      props.AppSecretsArn,
		},
		&awselasticbeanstalk.CfnEnvironment_OptionSettingProperty{
			Namespace:  jsii.String("aws:elasticbeanstalk:application:environment"),
			OptionName: jsii.String("DB_SECRET_ARN"),
			Value:      props.DbSecretArn,
		},
	}

	// Create Elastic Beanstalk environment
	environment := awselasticbeanstalk.NewCfnEnvironment(scope, jsii.String(id+"Environment"), &awselasticbeanstalk.CfnEnvironmentProps{
		ApplicationName:   application.ApplicationName(),
		EnvironmentName:   jsii.String("divtracker-prod"),
		SolutionStackName: jsii.String("64bit Amazon Linux 2023 v4.8.0 running Corretto 17"),
		OptionSettings:    optionSettings,
	})

	environment.AddDependency(application)

	// Output the application URL
	awscdk.NewCfnOutput(scope, jsii.String(id+"ApplicationURL"), &awscdk.CfnOutputProps{
		Value:       jsii.String(fmt.Sprintf("http://%s", *environment.AttrEndpointUrl())),
		Description: jsii.String("Application URL"),
		ExportName:  jsii.String("DivTrackerApplicationURL"),
	})
}
