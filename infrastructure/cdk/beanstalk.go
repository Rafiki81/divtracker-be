package main

import (
	"fmt"

	"github.com/aws/aws-cdk-go/awscdk/v2"
	"github.com/aws/aws-cdk-go/awscdk/v2/awsec2"
	"github.com/aws/aws-cdk-go/awscdk/v2/awselasticbeanstalk"
	"github.com/aws/aws-cdk-go/awscdk/v2/awsiam"
	"github.com/aws/aws-cdk-go/awscdk/v2/awsrds"
	"github.com/aws/constructs-go/constructs/v10"
	"github.com/aws/jsii-runtime-go"
)

type ElasticBeanstalkConstructProps struct {
	Vpc                awsec2.Vpc
	PublicSubnets      *[]awsec2.ISubnet
	SecurityGroup      awsec2.SecurityGroup
	Database           awsrds.DatabaseInstance
	AppSecretsArn      *string
	DbSecretArn        *string
	JwtSecret          *string
	FinnhubApiKey      string
	GoogleClientId     string
	GoogleClientSecret string
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

	// Build option settings
	optionSettings := []interface{}{
		buildOptionSetting("aws:elasticbeanstalk:environment", "EnvironmentType", "SingleInstance"),
		buildOptionSetting("aws:elasticbeanstalk:environment", "ServiceRole", *serviceRole.RoleArn()),
		buildOptionSetting("aws:autoscaling:launchconfiguration", "InstanceType", "t2.micro"),
		buildOptionSetting("aws:autoscaling:launchconfiguration", "IamInstanceProfile", *instanceProfile.Ref()),
		buildOptionSetting("aws:autoscaling:launchconfiguration", "SecurityGroups", *props.SecurityGroup.SecurityGroupId()),
		buildOptionSetting("aws:ec2:vpc", "VPCId", *props.Vpc.VpcId()),
		buildOptionSetting("aws:ec2:vpc", "Subnets", subnetIds),
		buildOptionSetting("aws:ec2:vpc", "AssociatePublicIpAddress", "true"),
		buildOptionSetting("aws:elasticbeanstalk:container:java:corretto", "Xmx", "512m"),
		buildOptionSetting("aws:elasticbeanstalk:container:java:corretto", "Xms", "256m"),
		buildOptionSetting("aws:elasticbeanstalk:application", "Application Healthcheck URL", "/actuator/health"),
		buildOptionSetting("aws:elasticbeanstalk:healthreporting:system", "SystemType", "enhanced"),
		buildOptionSetting("aws:elasticbeanstalk:cloudwatch:logs", "StreamLogs", "true"),
		buildOptionSetting("aws:elasticbeanstalk:cloudwatch:logs", "DeleteOnTerminate", "false"),
		buildOptionSetting("aws:elasticbeanstalk:cloudwatch:logs", "RetentionInDays", "7"),
		buildOptionSetting("aws:elasticbeanstalk:application:environment", "SPRING_PROFILES_ACTIVE", "aws"),
		buildOptionSetting("aws:elasticbeanstalk:application:environment", "RDS_HOSTNAME", *props.Database.DbInstanceEndpointAddress()),
		buildOptionSetting("aws:elasticbeanstalk:application:environment", "RDS_PORT", *props.Database.DbInstanceEndpointPort()),
		buildOptionSetting("aws:elasticbeanstalk:application:environment", "RDS_DB_NAME", "divtracker"),
		buildOptionSetting("aws:elasticbeanstalk:application:environment", "RDS_USERNAME", "divtracker"),
		buildOptionSetting("aws:elasticbeanstalk:application:environment", "DB_SECRET_ARN", *props.DbSecretArn),
		buildOptionSetting("aws:elasticbeanstalk:application:environment", "JWT_SECRET", *props.JwtSecret),
		buildOptionSetting("aws:elasticbeanstalk:application:environment", "FINNHUB_API_KEY", props.FinnhubApiKey),
		buildOptionSetting("aws:elasticbeanstalk:application:environment", "GOOGLE_CLIENT_ID", props.GoogleClientId),
		buildOptionSetting("aws:elasticbeanstalk:application:environment", "GOOGLE_CLIENT_SECRET", props.GoogleClientSecret),
		buildOptionSetting("aws:elasticbeanstalk:application:environment", "APP_SECRETS_ARN", *props.AppSecretsArn),
	}

	// Create Elastic Beanstalk environment
	environment := awselasticbeanstalk.NewCfnEnvironment(scope, jsii.String(id+"Environment"), &awselasticbeanstalk.CfnEnvironmentProps{
		ApplicationName:   application.ApplicationName(),
		EnvironmentName:   jsii.String("divtracker-prod"),
		SolutionStackName: jsii.String("64bit Amazon Linux 2023 v4.3.4 running Corretto 17"),
		OptionSettings:    &optionSettings,
	})

	environment.AddDependency(application)

	// Output the application URL
	awscdk.NewCfnOutput(scope, jsii.String(id+"ApplicationURL"), &awscdk.CfnOutputProps{
		Value:       jsii.String(fmt.Sprintf("http://%s", *environment.AttrEndpointUrl())),
		Description: jsii.String("Application URL"),
		ExportName:  jsii.String("DivTrackerApplicationURL"),
	})
}

func buildOptionSetting(namespace, optionName, value string) interface{} {
	return map[string]*string{
		"Namespace":  jsii.String(namespace),
		"OptionName": jsii.String(optionName),
		"Value":      jsii.String(value),
	}
}
