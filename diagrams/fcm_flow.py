#!/usr/bin/env python3
"""
DivTracker - Firebase Cloud Messaging Flow
Shows push notification architecture
"""
from diagrams import Diagram, Cluster, Edge
from diagrams.firebase.grow import Messaging
from diagrams.onprem.database import PostgreSQL
from diagrams.programming.framework import Spring
from diagrams.programming.language import Kotlin
from diagrams.onprem.client import Users
from diagrams.saas.analytics import Stitch

graph_attr = {
    "fontsize": "18",
    "bgcolor": "white",
    "pad": "0.5",
}

with Diagram(
    "DivTracker - Push Notifications Flow",
    filename="fcm_flow",
    show=False,
    direction="TB",
    graph_attr=graph_attr,
):
    
    finnhub = Stitch("Finnhub\nWebhooks")
    
    with Cluster("Backend (Spring Boot)"):
        
        with Cluster("Token Management"):
            device_ctrl = Spring("DeviceController\nPOST /devices/register")
            fcm_token_svc = Spring("FcmTokenService")
        
        with Cluster("Notification Triggers"):
            webhook_proc = Spring("WebhookProcessing\nService")
            daily_scheduler = Spring("DailySummary\nScheduler")
        
        with Cluster("Notification Sending"):
            push_svc = Spring("PushNotification\nService")
            firebase_svc = Spring("FirebasePush\nService")
        
        db = PostgreSQL("user_fcm_tokens\ntable")
    
    firebase = Messaging("Firebase\nCloud Messaging")
    
    with Cluster("Android App"):
        messaging_svc = Kotlin("DivTrackerMessaging\nService")
        
        with Cluster("Handlers"):
            price_handler = Kotlin("handlePriceUpdate()\n[Silent - updates Room DB]")
            alert_handler = Kotlin("handlePriceAlert()\n[Shows notification]")
            summary_handler = Kotlin("handleDailySummary()\n[Shows notification]")
        
        watchlist_dao = Kotlin("WatchlistDao")
    
    user = Users("User")
    
    # Token registration flow
    user >> Edge(label="1. App start", color="purple") >> messaging_svc
    messaging_svc >> Edge(label="2. onNewToken()", color="purple") >> device_ctrl
    device_ctrl >> Edge(label="3. Save token", color="purple") >> fcm_token_svc >> db
    
    # Webhook notification flow
    finnhub >> Edge(label="4. Trade event", color="blue") >> webhook_proc
    webhook_proc >> Edge(label="5. Get tokens\nfor ticker", color="blue") >> db
    webhook_proc >> Edge(label="6. Send\nnotifications", color="blue") >> push_svc
    push_svc >> firebase_svc >> Edge(label="7. FCM API", color="blue") >> firebase
    
    # Daily summary flow
    daily_scheduler >> Edge(label="22:00 CET", color="orange", style="dashed") >> push_svc
    
    # Delivery to Android
    firebase >> Edge(label="8. Push message", color="green") >> messaging_svc
    messaging_svc >> Edge(label="PRICE_UPDATE", color="green") >> price_handler
    messaging_svc >> Edge(label="PRICE_ALERT", color="red") >> alert_handler
    messaging_svc >> Edge(label="DAILY_SUMMARY", color="orange") >> summary_handler
    
    price_handler >> Edge(label="Update local DB", color="green") >> watchlist_dao
