package com.rafiki18.divtracker_be.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Firebase configuration for push notifications.
 * 
 * The Firebase service account credentials can be provided in two ways:
 * 1. FIREBASE_CREDENTIALS_JSON: Base64 encoded JSON (recommended for production)
 * 2. FIREBASE_CREDENTIALS_PATH: Path to the JSON file (for local development)
 */
@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${firebase.enabled:false}")
    private boolean firebaseEnabled;

    @Value("${firebase.credentials.json:}")
    private String firebaseCredentialsJson;

    @Value("${firebase.credentials.path:}")
    private String firebaseCredentialsPath;

    @Value("${firebase.project-id:}")
    private String projectId;

    @PostConstruct
    public void initialize() {
        if (!firebaseEnabled) {
            log.info("Firebase is disabled. Push notifications will not be sent.");
            return;
        }

        if (FirebaseApp.getApps().isEmpty()) {
            try {
                FirebaseOptions options = buildFirebaseOptions();
                if (options != null) {
                    FirebaseApp.initializeApp(options);
                    log.info("Firebase initialized successfully for project: {}", projectId);
                } else {
                    log.warn("Firebase credentials not configured. Push notifications disabled.");
                }
            } catch (Exception e) {
                log.error("Failed to initialize Firebase: {}", e.getMessage(), e);
            }
        }
    }

    private FirebaseOptions buildFirebaseOptions() throws IOException {
        GoogleCredentials credentials = getCredentials();
        if (credentials == null) {
            return null;
        }

        FirebaseOptions.Builder builder = FirebaseOptions.builder()
                .setCredentials(credentials);
        
        if (projectId != null && !projectId.isBlank()) {
            builder.setProjectId(projectId);
        }

        return builder.build();
    }

    private GoogleCredentials getCredentials() throws IOException {
        // Option 1: Base64 encoded JSON (from environment variable)
        if (firebaseCredentialsJson != null && !firebaseCredentialsJson.isBlank()) {
            log.debug("Loading Firebase credentials from JSON string");
            try {
                // Try to decode as base64 first
                byte[] decoded = Base64.getDecoder().decode(firebaseCredentialsJson);
                return GoogleCredentials.fromStream(new ByteArrayInputStream(decoded));
            } catch (IllegalArgumentException e) {
                // Not base64, try as plain JSON
                return GoogleCredentials.fromStream(
                    new ByteArrayInputStream(firebaseCredentialsJson.getBytes(StandardCharsets.UTF_8)));
            }
        }

        // Option 2: Path to JSON file
        if (firebaseCredentialsPath != null && !firebaseCredentialsPath.isBlank()) {
            log.debug("Loading Firebase credentials from file: {}", firebaseCredentialsPath);
            InputStream serviceAccount = getClass().getClassLoader().getResourceAsStream(firebaseCredentialsPath);
            if (serviceAccount != null) {
                return GoogleCredentials.fromStream(serviceAccount);
            }
            // Try as absolute path
            try (InputStream fileStream = new java.io.FileInputStream(firebaseCredentialsPath)) {
                return GoogleCredentials.fromStream(fileStream);
            }
        }

        // Option 3: Default application credentials (works in GCP environments)
        try {
            log.debug("Attempting to use default application credentials");
            return GoogleCredentials.getApplicationDefault();
        } catch (IOException e) {
            log.debug("Default application credentials not available");
            return null;
        }
    }

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        if (!firebaseEnabled || FirebaseApp.getApps().isEmpty()) {
            log.debug("FirebaseMessaging bean not created - Firebase is disabled or not initialized");
            return null;
        }
        return FirebaseMessaging.getInstance();
    }
}
