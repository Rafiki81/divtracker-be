package com.rafiki18.divtracker_be.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rafiki18.divtracker_be.model.User;
import com.rafiki18.divtracker_be.scheduler.FundamentalsRefreshScheduler;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Admin endpoints for manual operations.
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin", description = "Administrative operations")
public class AdminController {
    
    private final FundamentalsRefreshScheduler scheduler;
    
    /**
     * Manually trigger the scheduled fundamentals refresh job.
     * Refreshes stale fundamentals (> 24h old), up to 50 per run.
     */
    @PostMapping("/refresh-fundamentals")
    @Operation(
        summary = "Manually refresh stale fundamentals",
        description = "Triggers the scheduled job to refresh fundamentals older than 24 hours (max 50 per run)",
        security = @SecurityRequirement(name = "bearer-auth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "Refresh job started successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
        }
    )
    public ResponseEntity<String> triggerFundamentalsRefresh(@AuthenticationPrincipal User user) {
        log.info("User {} manually triggered fundamentals refresh", user.getEmail());
        
        // Run asynchronously to avoid blocking
        new Thread(() -> {
            try {
                scheduler.refreshStaleFundamentals();
            } catch (Exception e) {
                log.error("Error in manual fundamentals refresh", e);
            }
        }).start();
        
        return ResponseEntity.ok("Fundamentals refresh job started");
    }
    
    /**
     * Manually trigger the cleanup of old fundamentals job.
     * Deletes STALE fundamentals older than 30 days.
     */
    @PostMapping("/cleanup-old-fundamentals")
    @Operation(
        summary = "Cleanup old fundamentals",
        description = "Triggers the scheduled job to delete STALE fundamentals older than 30 days",
        security = @SecurityRequirement(name = "bearer-auth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "Cleanup job started successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
        }
    )
    public ResponseEntity<String> triggerFundamentalsCleanup(@AuthenticationPrincipal User user) {
        log.info("User {} manually triggered fundamentals cleanup", user.getEmail());
        
        // Run asynchronously to avoid blocking
        new Thread(() -> {
            try {
                scheduler.cleanupOldFundamentals();
            } catch (Exception e) {
                log.error("Error in manual fundamentals cleanup", e);
            }
        }).start();
        
        return ResponseEntity.ok("Fundamentals cleanup job started");
    }
}
