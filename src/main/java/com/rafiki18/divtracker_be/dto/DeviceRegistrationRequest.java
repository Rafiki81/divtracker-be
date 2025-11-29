package com.rafiki18.divtracker_be.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for registering a device's FCM token.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceRegistrationRequest {

    @NotBlank(message = "FCM token is required")
    @Size(max = 500, message = "FCM token cannot exceed 500 characters")
    private String fcmToken;

    @NotBlank(message = "Device ID is required")
    @Size(max = 255, message = "Device ID cannot exceed 255 characters")
    private String deviceId;

    @Size(max = 255, message = "Device name cannot exceed 255 characters")
    private String deviceName;

    @Builder.Default
    private String platform = "ANDROID";
}
