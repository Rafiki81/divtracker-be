package com.rafiki18.divtracker_be.service;

import java.util.List;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rafiki18.divtracker_be.dto.DeviceRegistrationRequest;
import com.rafiki18.divtracker_be.dto.DeviceResponse;
import com.rafiki18.divtracker_be.model.User;
import com.rafiki18.divtracker_be.model.UserFcmToken;
import com.rafiki18.divtracker_be.repository.UserFcmTokenRepository;
import com.rafiki18.divtracker_be.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing FCM tokens for push notifications.
 */
@Service
@ConditionalOnProperty(name = "fcm.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class FcmTokenService {

    private final UserFcmTokenRepository fcmTokenRepository;
    private final UserRepository userRepository;

    /**
     * Register or update a device's FCM token for a user.
     * If the device already exists, updates the token.
     */
    @Transactional
    public DeviceResponse registerDevice(UUID userId, DeviceRegistrationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        UserFcmToken token = fcmTokenRepository.findByUserIdAndDeviceId(userId, request.getDeviceId())
                .map(existing -> {
                    log.debug("Updating existing FCM token for user {} device {}", userId, request.getDeviceId());
                    existing.setFcmToken(request.getFcmToken());
                    existing.setDeviceName(request.getDeviceName());
                    existing.setIsActive(true); // Reactivate if was deactivated
                    return existing;
                })
                .orElseGet(() -> {
                    log.debug("Creating new FCM token for user {} device {}", userId, request.getDeviceId());
                    return UserFcmToken.builder()
                            .user(user)
                            .fcmToken(request.getFcmToken())
                            .deviceId(request.getDeviceId())
                            .deviceName(request.getDeviceName())
                            .platform(UserFcmToken.Platform.valueOf(request.getPlatform().toUpperCase()))
                            .isActive(true)
                            .build();
                });

        token = fcmTokenRepository.save(token);
        log.info("Registered FCM token for user {} on device {}", userId, request.getDeviceId());

        return toResponse(token);
    }

    /**
     * Get all registered devices for a user.
     */
    @Transactional(readOnly = true)
    public List<DeviceResponse> getUserDevices(UUID userId) {
        return fcmTokenRepository.findByUserIdAndIsActiveTrue(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Remove a device registration.
     */
    @Transactional
    public void unregisterDevice(UUID userId, String deviceId) {
        fcmTokenRepository.findByUserIdAndDeviceId(userId, deviceId)
                .ifPresent(token -> {
                    fcmTokenRepository.delete(token);
                    log.info("Unregistered device {} for user {}", deviceId, userId);
                });
    }

    /**
     * Get all active tokens for users watching a specific ticker.
     */
    @Transactional(readOnly = true)
    public List<UserFcmToken> getTokensForTicker(String ticker) {
        return fcmTokenRepository.findActiveTokensByWatchlistTicker(ticker.toUpperCase());
    }

    /**
     * Get all active tokens for users with any watchlist items.
     * Used for daily summary notifications.
     */
    @Transactional(readOnly = true)
    public List<UserFcmToken> getTokensForDailySummary() {
        return fcmTokenRepository.findActiveTokensForUsersWithWatchlist();
    }

    /**
     * Deactivate a token (called when Firebase reports the token as invalid).
     */
    @Transactional
    public void deactivateToken(String fcmToken) {
        fcmTokenRepository.deactivateByFcmToken(fcmToken);
        log.debug("Deactivated invalid FCM token");
    }

    /**
     * Mark a token as used (for tracking active devices).
     */
    @Transactional
    public void markTokenAsUsed(String fcmToken) {
        fcmTokenRepository.findByFcmToken(fcmToken)
                .ifPresent(token -> {
                    token.markAsUsed();
                    fcmTokenRepository.save(token);
                });
    }

    private DeviceResponse toResponse(UserFcmToken token) {
        return DeviceResponse.builder()
                .id(token.getId())
                .deviceId(token.getDeviceId())
                .deviceName(token.getDeviceName())
                .platform(token.getPlatform().name())
                .isActive(token.getIsActive())
                .createdAt(token.getCreatedAt())
                .lastUsedAt(token.getLastUsedAt())
                .build();
    }
}
