package main

import (
	"os"

	"github.com/aws/aws-cdk-go/awscdk/v2"
	"github.com/aws/jsii-runtime-go"
)

// Main CDK Application
func main() {
	defer jsii.Close()

	app := awscdk.NewApp(nil)

	// Get environment from AWS credentials
	env := awscdk.Environment{
		Account: jsii.String(os.Getenv("CDK_DEFAULT_ACCOUNT")),
		Region:  jsii.String(getRegion()),
	}

	// Create production stack
	NewDivTrackerStack(app, "DivTrackerProdStack", &DivTrackerStackProps{
		StackProps: awscdk.StackProps{
			Env:         &env,
			StackName:   jsii.String("divtracker-prod"),
			Description: jsii.String("DivTracker Backend Infrastructure - Production Environment"),
			Tags: &map[string]*string{
				"Project":   jsii.String("DivTracker"),
				"ManagedBy": jsii.String("CDK"),
			},
		},
	})

	app.Synth(nil)
}

func getRegion() string {
	region := os.Getenv("CDK_DEFAULT_REGION")
	if region == "" {
		region = "us-east-1"
	}
	return region
}
