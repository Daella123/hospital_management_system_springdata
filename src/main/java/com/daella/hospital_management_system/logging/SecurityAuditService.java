package com.daella.hospital_management_system.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory security audit tracker.
 *
 * <p>Maintains lightweight counters for key security events so an ADMIN can
 * pull a quick report via {@code GET /api/v1/admin/security-report} without
 * needing a dedicated metrics system.</p>
 *
 * <p><b>DSA note:</b> Uses {@link AtomicLong} for lock-free counter updates
 * and a {@link ConcurrentHashMap} for per-endpoint frequency tracking.</p>
 */
@Service
public class SecurityAuditService {

    private static final Logger log = LoggerFactory.getLogger(SecurityAuditService.class);

    private final AtomicLong totalLoginAttempts  = new AtomicLong();
    private final AtomicLong failedLoginAttempts = new AtomicLong();
    private final AtomicLong tokenIssued         = new AtomicLong();
    private final AtomicLong tokenRevoked        = new AtomicLong();

    /** Per-endpoint access frequency — endpoint path → hit count. */
    private final ConcurrentHashMap<String, AtomicLong> endpointFrequency = new ConcurrentHashMap<>();

    // ── Event recorders ───────────────────────────────────────────────────────

    public void recordLoginAttempt()  { totalLoginAttempts.incrementAndGet(); }
    public void recordLoginFailure()  { failedLoginAttempts.incrementAndGet(); }
    public void recordTokenIssued()   { tokenIssued.incrementAndGet(); }
    public void recordTokenRevoked()  { tokenRevoked.incrementAndGet(); }

    public void recordEndpointAccess(String endpoint) {
        endpointFrequency
                .computeIfAbsent(endpoint, k -> new AtomicLong())
                .incrementAndGet();
    }

    // ── Report ────────────────────────────────────────────────────────────────

    /**
     * Returns a human-readable snapshot of the current security statistics.
     */
    public SecurityReport getReport() {
        return new SecurityReport(
                totalLoginAttempts.get(),
                failedLoginAttempts.get(),
                tokenIssued.get(),
                tokenRevoked.get(),
                new ConcurrentHashMap<>(endpointFrequency)
                        .entrySet().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                java.util.Map.Entry::getKey,
                                e -> e.getValue().get()))
        );
    }

    // ── Nested report record ──────────────────────────────────────────────────

    public record SecurityReport(
            long totalLoginAttempts,
            long failedLoginAttempts,
            long tokensIssued,
            long tokensRevoked,
            java.util.Map<String, Long> endpointFrequency
    ) {}
}
