-- V12: Add FCM tokens table for Firebase Cloud Messaging push notifications
-- This table stores device tokens for each user to enable real-time push notifications

CREATE TABLE user_fcm_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    fcm_token VARCHAR(500) NOT NULL,
    device_id VARCHAR(255) NOT NULL,
    device_name VARCHAR(255),
    platform VARCHAR(50) DEFAULT 'ANDROID',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP,
    
    CONSTRAINT uk_user_device UNIQUE(user_id, device_id)
);

-- Index for efficient lookups by user
CREATE INDEX idx_fcm_tokens_user_id ON user_fcm_tokens(user_id);

-- Index for active tokens (most common query)
CREATE INDEX idx_fcm_tokens_active ON user_fcm_tokens(is_active) WHERE is_active = true;

-- Index for token lookup (for token refresh/invalidation)
CREATE INDEX idx_fcm_tokens_token ON user_fcm_tokens(fcm_token);

COMMENT ON TABLE user_fcm_tokens IS 'Stores FCM tokens for push notifications to user devices';
COMMENT ON COLUMN user_fcm_tokens.fcm_token IS 'Firebase Cloud Messaging device token';
COMMENT ON COLUMN user_fcm_tokens.device_id IS 'Unique device identifier (stays constant across app reinstalls)';
COMMENT ON COLUMN user_fcm_tokens.device_name IS 'Human-readable device name (e.g., Samsung Galaxy S24)';
COMMENT ON COLUMN user_fcm_tokens.platform IS 'Device platform: ANDROID, IOS, or WEB';
COMMENT ON COLUMN user_fcm_tokens.is_active IS 'Whether this token is still valid for notifications';
COMMENT ON COLUMN user_fcm_tokens.last_used_at IS 'Last time a notification was successfully sent to this device';
