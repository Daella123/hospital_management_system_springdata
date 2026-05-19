package com.daella.hospital_management_system.security;

import com.daella.hospital_management_system.logging.SecurityEventLogger;
import org.springframework.http.HttpStatus;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT authentication filter — runs once per request.
 *
 * <p>Processing order:
 * <ol>
 *   <li>Extract the {@code Authorization: Bearer <token>} header</li>
 *   <li>Reject blacklisted tokens immediately</li>
 *   <li>Validate signature and expiry via {@link JwtService}</li>
 *   <li>Set the authentication in {@link SecurityContextHolder}</li>
 * </ol>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final TokenBlacklistService blacklistService;
    private final SecurityEventLogger securityEventLogger;
    private final SecurityErrorResponseWriter securityErrorResponseWriter;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   CustomUserDetailsService userDetailsService,
                                   TokenBlacklistService blacklistService,
                                   SecurityEventLogger securityEventLogger,
                                   SecurityErrorResponseWriter securityErrorResponseWriter) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.blacklistService = blacklistService;
        this.securityEventLogger = securityEventLogger;
        this.securityErrorResponseWriter = securityErrorResponseWriter;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // Skip if no Bearer token present
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);

        // ── Blacklist check ──────────────────────────────────────────────────
        if (blacklistService.isBlacklisted(token)) {
            securityEventLogger.logBlacklistedTokenAttempt(request.getRequestURI());
            securityErrorResponseWriter.write(response, HttpStatus.UNAUTHORIZED, "Unauthorized",
                    "This access token has been revoked (for example after logout). "
                            + "Please sign in again to obtain a new token.");
            return;
        }

        try {
            final String userEmail = jwtService.extractUsername(token);

            // Only authenticate if not already authenticated
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                if (jwtService.isTokenValid(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (ExpiredJwtException e) {
            securityEventLogger.logTokenExpired(request.getRequestURI());
            securityErrorResponseWriter.write(response, HttpStatus.UNAUTHORIZED, "Unauthorized",
                    "Your access token has expired. Please sign in again.");
            return;
        } catch (JwtException | IllegalArgumentException e) {
            securityEventLogger.logUnauthorizedAccess(request.getRequestURI(), "Invalid JWT token");
            securityErrorResponseWriter.write(response, HttpStatus.UNAUTHORIZED, "Unauthorized",
                    "The access token is missing, invalid, or corrupt. "
                            + "Use a Bearer token from /auth/login or refresh.");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
