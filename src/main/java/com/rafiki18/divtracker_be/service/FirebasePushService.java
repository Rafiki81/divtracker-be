package com.rafiki18.divtracker_be.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import com.rafiki18.divtracker_be.dto.PushNotificationDto;
import com.rafiki18.divtracker_be.model.UserFcmToken;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for sending push notifications via Firebase Cloud Messaging.
 */
@Service
@ConditionalOnProperty(name = "fcm.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class FirebasePushService {

    private final FirebaseMessaging firebaseMessaging;
    private final FcmTokenService fcmTokenService;

    @Value("${firebase.enabled:false}")
    private boolean firebaseEnabled;

    /**
     * Send a push notification to a single token.
     */
    public boolean sendNotification(String fcmToken, PushNotificationDto notification) {
        if (!isEnabled()) {
            log.debug("Firebase disabled, skipping notification to {}", fcmToken);
            return false;
        }

        try {
            Message message = buildMessage(fcmToken, notification);
            firebaseMessaging.send(message);
            fcmTokenService.markTokenAsUsed(fcmToken);
            return true;
        } catch (FirebaseMessagingException e) {
            log.warn("FCM failed: {}", e.getMessage());
            handleFirebaseError(fcmToken, e);
            return false;
        }
    }

    /**
     * Send a push notification to multiple tokens (up to 500).
     * Returns the number of successful sends.
     */
    public int sendNotificationToMultiple(List<UserFcmToken> tokens, PushNotificationDto notification) {
        if (!isEnabled() || tokens.isEmpty()) {
            log.debug("Skipping multicast: enabled={}, tokens={}", isEnabled(), tokens.size());
            return 0;
        }

        log.info("📤 FCM: Enviando {} a {} dispositivos", notification.getType(), tokens.size());

        List<String> fcmTokens = tokens.stream()
                .map(UserFcmToken::getFcmToken)
                .toList();

        // Firebase allows max 500 tokens per multicast
        int totalSuccess = 0;
        for (int i = 0; i < fcmTokens.size(); i += 500) {
            List<String> batch = fcmTokens.subList(i, Math.min(i + 500, fcmTokens.size()));
            totalSuccess += sendBatch(batch, notification);
        }

        return totalSuccess;
    }

    /**
     * Send a push notification asynchronously to all tokens watching a ticker.
     */
    @Async
    public void sendPriceUpdateAsync(String ticker, PushNotificationDto notification) {
        List<UserFcmToken> tokens = fcmTokenService.getTokensForTicker(ticker);
        if (tokens.isEmpty()) {
            log.trace("No tokens watching ticker {}", ticker);
            return;
        }

        int sent = sendNotificationToMultiple(tokens, notification);
        log.debug("Sent {} price updates for {} to {} devices", notification.getType(), ticker, sent);
    }

    /**
     * Send notification to specific user's devices.
     */
    @Async
    public void sendToUserAsync(List<UserFcmToken> userTokens, PushNotificationDto notification) {
        if (userTokens.isEmpty()) {
            return;
        }
        
        int sent = sendNotificationToMultiple(userTokens, notification);
        log.debug("Sent {} notification to {} devices", notification.getType(), sent);
    }

    private int sendBatch(List<String> fcmTokens, PushNotificationDto notification) {
        try {
            MulticastMessage message = buildMulticastMessage(fcmTokens, notification);
            BatchResponse response = firebaseMessaging.sendEachForMulticast(message);
            
            // Handle failures silently
            if (response.getFailureCount() > 0) {
                handleBatchFailures(fcmTokens, response.getResponses());
            }

            // Mark successful tokens as used
            for (int i = 0; i < response.getResponses().size(); i++) {
                if (response.getResponses().get(i).isSuccessful()) {
                    fcmTokenService.markTokenAsUsed(fcmTokens.get(i));
                }
            }
            log.info("✅ FCM: {} enviados, {} fallidos", response.getSuccessCount(), response.getFailureCount());
            return response.getSuccessCount();
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send batch notification: {}", e.getMessage());
            return 0;
        }
    }

    private Message buildMessage(String fcmToken, PushNotificationDto notification) {
        Message.Builder builder = Message.builder()
                .setToken(fcmToken)
                .putAllData(notification.getData());

        // Add notification payload for visible notifications
        if (!notification.isDataOnly() && notification.getTitle() != null) {
            builder.setNotification(Notification.builder()
                    .setTitle(notification.getTitle())
                    .setBody(notification.getBody())
                    .build());
            
            // Android-specific configuration
            builder.setAndroidConfig(AndroidConfig.builder()
                    .setNotification(AndroidNotification.builder()
                            .setClickAction("FLUTTER_NOTIFICATION_CLICK")
                            .setDefaultSound(true)
                            .build())
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .build());
        } else {
            // Data-only message (silent, for price updates)
            builder.setAndroidConfig(AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.NORMAL)
                    .build());
        }

        return builder.build();
    }

    private MulticastMessage buildMulticastMessage(List<String> fcmTokens, PushNotificationDto notification) {
        MulticastMessage.Builder builder = MulticastMessage.builder()
                .addAllTokens(fcmTokens)
                .putAllData(notification.getData());

        // Add notification payload for visible notifications
        if (!notification.isDataOnly() && notification.getTitle() != null) {
            builder.setNotification(Notification.builder()
                    .setTitle(notification.getTitle())
                    .setBody(notification.getBody())
                    .build());

            builder.setAndroidConfig(AndroidConfig.builder()
                    .setNotification(AndroidNotification.builder()
                            .setClickAction("FLUTTER_NOTIFICATION_CLICK")
                            .setDefaultSound(true)
                            .build())
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .build());
        } else {
            builder.setAndroidConfig(AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.NORMAL)
                    .build());
        }

        return builder.build();
    }

    private void handleFirebaseError(String fcmToken, FirebaseMessagingException e) {
        String errorCode = e.getMessagingErrorCode() != null ? 
                e.getMessagingErrorCode().name() : "UNKNOWN";

        // Deactivate invalid tokens silently
        if (isInvalidTokenError(errorCode)) {
            fcmTokenService.deactivateToken(fcmToken);
        }
    }

    private void handleBatchFailures(List<String> fcmTokens, List<SendResponse> responses) {
        List<String> invalidTokens = new ArrayList<>();
        
        for (int i = 0; i < responses.size(); i++) {
            SendResponse response = responses.get(i);
            if (!response.isSuccessful()) {
                FirebaseMessagingException ex = response.getException();
                String errorCode = (ex != null && ex.getMessagingErrorCode() != null) ?
                        ex.getMessagingErrorCode().name() : "UNKNOWN";
                
                if (isInvalidTokenError(errorCode)) {
                    invalidTokens.add(fcmTokens.get(i));
                }
            }
        }

        // Deactivate all invalid tokens silently
        for (String token : invalidTokens) {
            fcmTokenService.deactivateToken(token);
        }
    }

    private boolean isInvalidTokenError(String errorCode) {
        return "UNREGISTERED".equals(errorCode) || 
               "INVALID_ARGUMENT".equals(errorCode) ||
               "NOT_FOUND".equals(errorCode);
    }

    private boolean isEnabled() {
        return firebaseEnabled && firebaseMessaging != null;
    }
}
