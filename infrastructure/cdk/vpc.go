package main

import (
	"github.com/aws/aws-cdk-go/awscdk/v2/awsec2"
	"github.com/aws/constructs-go/constructs/v10"
	"github.com/aws/jsii-runtime-go"
)

type VpcConstruct struct {
	Vpc              awsec2.Vpc
	AppSecurityGroup awsec2.SecurityGroup
	RdsSecurityGroup awsec2.SecurityGroup
	PublicSubnets    *[]awsec2.ISubnet
	PrivateSubnets   *[]awsec2.ISubnet
}

// Creates VPC with public and private subnets, security groups
func NewVpcConstruct(scope constructs.Construct, id string) *VpcConstruct {
	vpc := awsec2.NewVpc(scope, jsii.String(id+"VPC"), &awsec2.VpcProps{
		IpAddresses:        awsec2.IpAddresses_Cidr(jsii.String("10.0.0.0/16")),
		MaxAzs:             jsii.Number(2),
		NatGateways:        jsii.Number(0), // No NAT Gateway for Free Tier
		EnableDnsHostnames: jsii.Bool(true),
		EnableDnsSupport:   jsii.Bool(true),
		SubnetConfiguration: &[]*awsec2.SubnetConfiguration{
			{
				Name:       jsii.String("Public"),
				SubnetType: awsec2.SubnetType_PUBLIC,
				CidrMask:   jsii.Number(24),
			},
			{
				Name:       jsii.String("Private"),
				SubnetType: awsec2.SubnetType_PRIVATE_ISOLATED,
				CidrMask:   jsii.Number(24),
			},
		},
	})

	// Security Group for Application (Elastic Beanstalk)
	appSg := awsec2.NewSecurityGroup(scope, jsii.String(id+"AppSG"), &awsec2.SecurityGroupProps{
		Vpc:               vpc,
		Description:       jsii.String("Security group for DivTracker application"),
		AllowAllOutbound:  jsii.Bool(true),
		SecurityGroupName: jsii.String("divtracker-app-sg"),
	})

	appSg.AddIngressRule(
		awsec2.Peer_AnyIpv4(),
		awsec2.Port_Tcp(jsii.Number(80)),
		jsii.String("Allow HTTP traffic"),
		jsii.Bool(false),
	)

	appSg.AddIngressRule(
		awsec2.Peer_AnyIpv4(),
		awsec2.Port_Tcp(jsii.Number(443)),
		jsii.String("Allow HTTPS traffic"),
		jsii.Bool(false),
	)

	// Security Group for RDS
	rdsSg := awsec2.NewSecurityGroup(scope, jsii.String(id+"RdsSG"), &awsec2.SecurityGroupProps{
		Vpc:               vpc,
		Description:       jsii.String("Security group for RDS PostgreSQL"),
		AllowAllOutbound:  jsii.Bool(true),
		SecurityGroupName: jsii.String("divtracker-rds-sg"),
	})

	rdsSg.AddIngressRule(
		appSg,
		awsec2.Port_Tcp(jsii.Number(5432)),
		jsii.String("Allow PostgreSQL from application"),
		jsii.Bool(false),
	)

	// Get subnets
	publicSubnets := vpc.PublicSubnets()
	privateSubnets := vpc.IsolatedSubnets()

	return &VpcConstruct{
		Vpc:              vpc,
		AppSecurityGroup: appSg,
		RdsSecurityGroup: rdsSg,
		PublicSubnets:    publicSubnets,
		PrivateSubnets:   privateSubnets,
	}
}
