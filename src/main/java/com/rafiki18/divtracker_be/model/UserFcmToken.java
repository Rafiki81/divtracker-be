package com.rafiki18.divtracker_be.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing a Firebase Cloud Messaging token for a user's device.
 * Each user can have multiple tokens (one per device).
 */
@Entity
@Table(name = "user_fcm_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFcmToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "fcm_token", nullable = false, length = 500)
    private String fcmToken;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(name = "device_name")
    private String deviceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform")
    @Builder.Default
    private Platform platform = Platform.ANDROID;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    public enum Platform {
        ANDROID, IOS, WEB
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Mark this token as used (for tracking active devices).
     */
    public void markAsUsed() {
        this.lastUsedAt = Instant.now();
    }

    /**
     * Deactivate this token (e.g., when Firebase reports it as invalid).
     */
    public void deactivate() {
        this.isActive = false;
    }
}
