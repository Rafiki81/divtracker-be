#!/usr/bin/env python3
"""
DivTracker - Backend Components Diagram
"""
from diagrams import Diagram, Cluster, Edge
from diagrams.programming.framework import Spring
from diagrams.programming.language import Java
from diagrams.onprem.database import PostgreSQL
from diagrams.onprem.client import Users
from diagrams.saas.analytics import Stitch
from diagrams.firebase.grow import Messaging

graph_attr = {
    "fontsize": "18",
    "bgcolor": "white",
    "pad": "0.5",
}

node_attr = {
    "fontsize": "12",
}

with Diagram(
    "DivTracker - Backend Components",
    filename="backend_components",
    show=False,
    direction="LR",
    graph_attr=graph_attr,
    node_attr=node_attr
):
    
    # External
    android = Users("Android App")
    finnhub_api = Stitch("Finnhub API")
    firebase = Messaging("Firebase FCM")
    
    with Cluster("Spring Boot Application"):
        
        with Cluster("Controllers"):
            auth_ctrl = Java("AuthController")
            watchlist_ctrl = Java("WatchlistController")
            webhook_ctrl = Java("FinnhubWebhookController")
            device_ctrl = Java("DeviceController")
            ticker_ctrl = Java("TickerSearchController")
        
        with Cluster("Services"):
            auth_svc = Spring("AuthService")
            watchlist_svc = Spring("WatchlistService")
            valuation_svc = Spring("WatchlistValuationService")
            webhook_svc = Spring("WebhookProcessingService")
            push_svc = Spring("PushNotificationService")
            fcm_svc = Spring("FcmTokenService")
        
        with Cluster("Market Data"):
            finnhub_client = Java("FinnhubClient")
            fundamentals_svc = Spring("FundamentalsService")
        
        with Cluster("Repositories"):
            user_repo = Java("UserRepository")
            watchlist_repo = Java("WatchlistItemRepository")
            fcm_repo = Java("UserFcmTokenRepository")
        
        with Cluster("Security"):
            jwt_filter = Spring("JwtAuthFilter")
            security_config = Spring("SecurityConfig")
    
    db = PostgreSQL("PostgreSQL")
    
    # Controller connections
    android >> auth_ctrl >> auth_svc >> user_repo
    android >> watchlist_ctrl >> watchlist_svc >> watchlist_repo
    android >> device_ctrl >> fcm_svc >> fcm_repo
    android >> ticker_ctrl >> finnhub_client
    
    finnhub_api >> webhook_ctrl >> webhook_svc
    
    # Service connections
    watchlist_svc >> valuation_svc
    valuation_svc >> fundamentals_svc >> finnhub_client >> finnhub_api
    
    webhook_svc >> push_svc >> firebase
    push_svc >> fcm_svc
    
    # Database
    user_repo >> db
    watchlist_repo >> db
    fcm_repo >> db
