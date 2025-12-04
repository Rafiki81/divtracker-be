#!/usr/bin/env python3
"""
DivTracker - AWS Infrastructure Architecture Diagram
"""
from diagrams import Diagram, Cluster, Edge
from diagrams.aws.compute import ElasticBeanstalk, EC2
from diagrams.aws.database import RDS, ElastiCache
from diagrams.aws.network import VPC, PublicSubnet, PrivateSubnet, InternetGateway, NATGateway
from diagrams.aws.security import SecretsManager, IAMRole
from diagrams.aws.management import Cloudwatch
from diagrams.aws.general import Client
from diagrams.onprem.client import Users
from diagrams.saas.analytics import Stitch

graph_attr = {
    "fontsize": "20",
    "bgcolor": "white",
    "pad": "0.5",
    "splines": "spline",
}

with Diagram(
    "DivTracker - AWS Architecture",
    filename="aws_architecture",
    show=False,
    direction="TB",
    graph_attr=graph_attr
):
    
    # External actors
    android_app = Users("Android App")
    finnhub = Stitch("Finnhub API")
    
    with Cluster("AWS Cloud"):
        
        with Cluster("VPC"):
            igw = InternetGateway("Internet\nGateway")
            
            with Cluster("Public Subnet"):
                nat = NATGateway("NAT Gateway")
                
                with Cluster("Elastic Beanstalk"):
                    eb = ElasticBeanstalk("DivTracker\nEnvironment")
                    ec2 = EC2("EC2 Instance\n(Corretto 17)")
            
            with Cluster("Private Subnet"):
                rds = RDS("PostgreSQL 15\n(db.t3.micro)")
        
        # Security & Monitoring
        secrets = SecretsManager("Secrets Manager\n(JWT, API Keys)")
        iam = IAMRole("IAM Role\n(EB Service)")
        logs = Cloudwatch("CloudWatch\nLogs")
    
    # Connections
    android_app >> Edge(label="HTTPS REST API") >> igw >> eb
    finnhub >> Edge(label="Webhooks") >> igw >> eb
    
    eb >> ec2
    ec2 >> Edge(label="JDBC") >> rds
    ec2 >> Edge(label="Get Secrets") >> secrets
    ec2 >> Edge(label="Logs") >> logs
    
    eb >> iam
