package com.daella.hospital_management_system.controller;

import com.daella.hospital_management_system.dto.response.ApiResponse;
import com.daella.hospital_management_system.logging.SecurityAuditService;
import com.daella.hospital_management_system.security.TokenBlacklistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Admin-only endpoints for system health, security reports, and token management.
 * All endpoints require the ADMIN role.
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "System administration endpoints — ADMIN role required")
public class AdminController {

    private final SecurityAuditService auditService;
    private final TokenBlacklistService blacklistService;

    public AdminController(SecurityAuditService auditService,
                           TokenBlacklistService blacklistService) {
        this.auditService = auditService;
        this.blacklistService = blacklistService;
    }

    /**
     * Returns a real-time security report including login stats, token usage,
     * and per-endpoint access frequency.
     */
    @GetMapping("/security-report")
    @Operation(
            summary = "Get live security audit report",
            description = "Returns login attempts, failures, token stats, and endpoint frequency. ADMIN only.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<SecurityAuditService.SecurityReport>> securityReport() {
        return ResponseEntity.ok(ApiResponse.success(auditService.getReport()));
    }

    /**
     * Returns the current size of the JWT blacklist — useful for monitoring memory usage.
     */
    @GetMapping("/blacklist-size")
    @Operation(
            summary = "Get number of blacklisted tokens",
            description = "Shows how many tokens are currently in the revocation blacklist. ADMIN only.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Map<String, Integer>>> blacklistSize() {
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("blacklistedTokenCount", blacklistService.blacklistSize())));
    }
}
