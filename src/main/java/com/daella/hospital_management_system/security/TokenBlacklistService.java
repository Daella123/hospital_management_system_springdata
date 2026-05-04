package com.daella.hospital_management_system.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token blacklist service — stores revoked JWT tokens so they cannot be reused
 * after logout or forced invalidation.
 *
 * <p><b>DSA concept:</b> Uses a {@link ConcurrentHashMap} (hash table, O(1) average
 * lookup) to store {@code token → expiryTime} pairs. A scheduled task removes entries
 * whose associated token has already expired, keeping memory bounded.</p>
 */
@Service
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);

    /**
     * Map from raw JWT string → time the token expires.
     * Thread-safe: multiple requests may call blacklist/isBlacklisted concurrently.
     */
    private final ConcurrentHashMap<String, LocalDateTime> blacklistedTokens = new ConcurrentHashMap<>();

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Adds a token to the blacklist. Call this on logout or forced revocation.
     *
     * @param token     the raw JWT string
     * @param expiresAt the token's original expiry (used for cleanup)
     */
    public void blacklist(String token, LocalDateTime expiresAt) {
        blacklistedTokens.put(token, expiresAt);
        log.info("Token blacklisted — total blacklisted: {}", blacklistedTokens.size());
    }

    /**
     * Returns {@code true} if the token is present in the blacklist.
     * Called on every authenticated request before the token is trusted.
     */
    public boolean isBlacklisted(String token) {
        return blacklistedTokens.containsKey(token);
    }

    /** Returns the current number of blacklisted tokens (for audit/reporting). */
    public int blacklistSize() {
        return blacklistedTokens.size();
    }

    // ── Scheduled cleanup ─────────────────────────────────────────────────────

    /**
     * Removes expired tokens from the blacklist every 30 minutes.
     * Once a token has expired it is useless to a potential attacker,
     * so we can safely evict it and free memory.
     */
    @Scheduled(fixedDelay = 1_800_000) // 30 minutes
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        int sizeBefore = blacklistedTokens.size();
        blacklistedTokens.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
        int removed = sizeBefore - blacklistedTokens.size();
        if (removed > 0) {
            log.info("Blacklist cleanup: removed {} expired token(s), {} remaining",
                    removed, blacklistedTokens.size());
        }
    }
}
