package com.daella.hospital_management_system.auth;

import com.daella.hospital_management_system.auth.dto.AuthResponse;
import com.daella.hospital_management_system.auth.dto.LoginRequest;
import com.daella.hospital_management_system.auth.dto.RegisterRequest;
import com.daella.hospital_management_system.dto.response.ApiResponse;
import com.daella.hospital_management_system.logging.SecurityEventLogger;
import com.daella.hospital_management_system.security.JwtService;
import com.daella.hospital_management_system.security.TokenBlacklistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * Authentication endpoints — all public except {@code /auth/me} and {@code /auth/logout}.
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Register, login, profile, and logout")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final TokenBlacklistService blacklistService;
    private final SecurityEventLogger eventLogger;

    public AuthController(AuthService authService,
                          JwtService jwtService,
                          TokenBlacklistService blacklistService,
                          SecurityEventLogger eventLogger) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.blacklistService = blacklistService;
        this.eventLogger = eventLogger;
    }

    // ── POST /auth/register ───────────────────────────────────────────────────

    @PostMapping("/register")
    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account. Password is hashed with BCrypt. " +
                          "Role defaults to RECEPTIONIST if not specified."
    )
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse body = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", body));
    }

    // ── POST /auth/login ──────────────────────────────────────────────────────

    @PostMapping("/login")
    @Operation(
            summary = "Login with email and password",
            description = "Returns a signed JWT token valid for 24 hours. " +
                          "Include it in subsequent requests as: Authorization: Bearer <token>"
    )
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse body = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", body));
    }

    // ── GET /auth/me ──────────────────────────────────────────────────────────

    @GetMapping("/me")
    @Operation(
            summary = "Get currently authenticated user",
            description = "Returns profile info of the user identified by the JWT token.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<AuthResponse>> me(
            @AuthenticationPrincipal UserDetails userDetails) {
        AuthResponse body = authService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(body));
    }

    // ── POST /auth/logout ─────────────────────────────────────────────────────

    @PostMapping("/logout")
    @Operation(
            summary = "Logout — blacklist the current token",
            description = "Adds the provided JWT to the blacklist so it cannot be reused. " +
                          "The token must be sent in the Authorization header.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            Date expiry = jwtService.extractExpiration(token);
            LocalDateTime expiresAt = expiry.toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDateTime();
            blacklistService.blacklist(token, expiresAt);
            eventLogger.logLogout(userDetails.getUsername());
        }

        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }
}
