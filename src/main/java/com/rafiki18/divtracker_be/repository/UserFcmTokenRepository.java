package com.rafiki18.divtracker_be.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.rafiki18.divtracker_be.model.UserFcmToken;

/**
 * Repository for managing FCM tokens.
 */
@Repository
public interface UserFcmTokenRepository extends JpaRepository<UserFcmToken, UUID> {

    /**
     * Find all active tokens for a user.
     */
    List<UserFcmToken> findByUserIdAndIsActiveTrue(UUID userId);

    /**
     * Find a specific token by user and device.
     */
    Optional<UserFcmToken> findByUserIdAndDeviceId(UUID userId, String deviceId);

    /**
     * Find token by FCM token string (for invalidation).
     */
    Optional<UserFcmToken> findByFcmToken(String fcmToken);

    /**
     * Delete all tokens for a user (e.g., on account deletion).
     */
    void deleteByUserId(UUID userId);

    /**
     * Find all active tokens for users who have a specific ticker in their watchlist.
     * This is the key query for sending price updates to relevant users.
     */
    @Query("""
        SELECT DISTINCT t FROM UserFcmToken t
        WHERE t.isActive = true
        AND EXISTS (
            SELECT 1 FROM WatchlistItem w 
            WHERE w.userId = t.user.id 
            AND UPPER(w.ticker) = UPPER(:ticker)
        )
        """)
    List<UserFcmToken> findActiveTokensByWatchlistTicker(@Param("ticker") String ticker);

    /**
     * Find all active tokens for users who have any items in their watchlist.
     * Used for daily summary notifications.
     */
    @Query("""
        SELECT DISTINCT t FROM UserFcmToken t
        WHERE t.isActive = true
        AND EXISTS (SELECT 1 FROM WatchlistItem w WHERE w.userId = t.user.id)
        """)
    List<UserFcmToken> findActiveTokensForUsersWithWatchlist();

    /**
     * Deactivate a token by FCM token string.
     */
    @Modifying
    @Query("UPDATE UserFcmToken t SET t.isActive = false WHERE t.fcmToken = :fcmToken")
    void deactivateByFcmToken(@Param("fcmToken") String fcmToken);

    /**
     * Count active tokens for a user.
     */
    long countByUserIdAndIsActiveTrue(UUID userId);
}
