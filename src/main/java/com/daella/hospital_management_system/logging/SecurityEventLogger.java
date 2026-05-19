package com.daella.hospital_management_system.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Centralised logger for security-related events.
 *
 * <p>All security-sensitive actions (login, logout, token expiry, unauthorized access)
 * are funnelled through this component so they can be audited in one place
 * and easily redirected to an external SIEM if needed.</p>
 */
@Component
public class SecurityEventLogger {

    private static final Logger log = LoggerFactory.getLogger(SecurityEventLogger.class);

    public void logLoginSuccess(String email) {
        log.info("[SECURITY] LOGIN SUCCESS — user='{}'", email);
    }

    public void logLoginFailed(String email) {
        log.warn("[SECURITY] LOGIN FAILED — user='{}'", email);
    }

    public void logLogout(String email) {
        log.info("[SECURITY] LOGOUT — user='{}' token blacklisted", email);
    }

    public void logUnauthorizedAccess(String uri, String reason) {
        log.warn("[SECURITY] UNAUTHORIZED ACCESS — uri='{}' reason='{}'", uri, reason);
    }

    public void logTokenExpired(String uri) {
        log.warn("[SECURITY] TOKEN EXPIRED — uri='{}'", uri);
    }

    public void logBlacklistedTokenAttempt(String uri) {
        log.warn("[SECURITY] BLACKLISTED TOKEN USED — uri='{}'", uri);
    }

    public void logEndpointAccess(String email, String method, String uri) {
        log.debug("[SECURITY] ENDPOINT ACCESS — user='{}' method={} uri='{}'", email, method, uri);
    }
}
