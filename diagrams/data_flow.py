#!/usr/bin/env python3
"""
DivTracker - Data Flow Diagram
Shows how market data flows from Finnhub to Android app
"""
from diagrams import Diagram, Cluster, Edge
from diagrams.saas.analytics import Stitch
from diagrams.onprem.database import PostgreSQL
from diagrams.programming.framework import Spring
from diagrams.firebase.grow import Messaging
from diagrams.onprem.client import Users

graph_attr = {
    "fontsize": "18",
    "bgcolor": "white",
    "pad": "0.5",
    "rankdir": "LR",
}

edge_attr = {
    "fontsize": "10",
}

with Diagram(
    "DivTracker - Data Flow",
    filename="data_flow",
    show=False,
    direction="LR",
    graph_attr=graph_attr,
):
    
    # Data Sources
    finnhub = Stitch("Finnhub API")
    
    with Cluster("Backend Processing"):
        
        with Cluster("Inbound"):
            webhook = Spring("Webhook\nController")
            rest_api = Spring("REST API\n(on-demand)")
        
        with Cluster("Processing"):
            webhook_proc = Spring("Webhook\nProcessing")
            fundamentals = Spring("Fundamentals\nService")
            valuation = Spring("Valuation\nService")
        
        with Cluster("Storage"):
            db = PostgreSQL("PostgreSQL\n(watchlist, prices)")
        
        with Cluster("Outbound"):
            push_svc = Spring("Push\nNotification")
    
    firebase = Messaging("Firebase\nFCM")
    android = Users("Android\nApp")
    
    # Real-time flow (webhooks)
    finnhub >> Edge(label="1. Trade events\n(webhook)", color="blue") >> webhook
    webhook >> Edge(label="2. Process\ntrades", color="blue") >> webhook_proc
    webhook_proc >> Edge(label="3. Update\nprices", color="blue") >> db
    webhook_proc >> Edge(label="4. Notify\nusers", color="blue") >> push_svc
    push_svc >> Edge(label="5. Push\nPRICE_UPDATE", color="blue") >> firebase
    firebase >> Edge(label="6. Silent\nnotification", color="blue") >> android
    
    # On-demand flow (user refresh)
    android >> Edge(label="A. GET /watchlist", color="green", style="dashed") >> rest_api
    rest_api >> Edge(label="B. Fetch\nfundamentals", color="green", style="dashed") >> fundamentals
    fundamentals >> Edge(label="C. Get\nmetrics", color="green", style="dashed") >> finnhub
    fundamentals >> Edge(label="D. Calculate\nDCF", color="green", style="dashed") >> valuation
    valuation >> Edge(label="E. Store", color="green", style="dashed") >> db
    db >> Edge(label="F. Return\nenriched data", color="green", style="dashed") >> android
