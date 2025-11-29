package com.rafiki18.divtracker_be.controller;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rafiki18.divtracker_be.dto.DeviceRegistrationRequest;
import com.rafiki18.divtracker_be.dto.DeviceResponse;
import com.rafiki18.divtracker_be.model.User;
import com.rafiki18.divtracker_be.service.FcmTokenService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for managing device registrations for push notifications.
 * Only active when fcm.enabled=true
 */
@RestController
@RequestMapping("/api/v1/devices")
@ConditionalOnProperty(name = "fcm.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Devices", description = "Device registration for push notifications")
public class DeviceController {

    private final FcmTokenService fcmTokenService;

    /**
     * Register a device for push notifications.
     * If the device is already registered, updates the token.
     */
    @PostMapping("/register")
    @Operation(summary = "Register device for push notifications",
               description = "Registers or updates a device's FCM token for receiving push notifications")
    public ResponseEntity<DeviceResponse> registerDevice(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody DeviceRegistrationRequest request) {
        
        log.info("Registering device {} for user {}", request.getDeviceId(), user.getId());
        
        DeviceResponse response = fcmTokenService.registerDevice(user.getId(), request);
        
        return ResponseEntity.ok(response);
    }

    /**
     * List all registered devices for the current user.
     */
    @GetMapping
    @Operation(summary = "List registered devices",
               description = "Returns all devices registered for push notifications")
    public ResponseEntity<List<DeviceResponse>> listDevices(@AuthenticationPrincipal User user) {
        log.debug("Listing devices for user {}", user.getId());
        
        List<DeviceResponse> devices = fcmTokenService.getUserDevices(user.getId());
        
        return ResponseEntity.ok(devices);
    }

    /**
     * Unregister a device from push notifications.
     */
    @DeleteMapping("/{deviceId}")
    @Operation(summary = "Unregister device",
               description = "Removes a device from receiving push notifications")
    public ResponseEntity<Void> unregisterDevice(
            @AuthenticationPrincipal User user,
            @PathVariable String deviceId) {
        
        log.info("Unregistering device {} for user {}", deviceId, user.getId());
        
        fcmTokenService.unregisterDevice(user.getId(), deviceId);
        
        return ResponseEntity.noContent().build();
    }
}
