package com.rafiki18.divtracker_be.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for device registration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceResponse {
    
    private UUID id;
    private String deviceId;
    private String deviceName;
    private String platform;
    private Boolean isActive;
    private Instant createdAt;
    private Instant lastUsedAt;
}
