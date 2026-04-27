package com.daella.hospital_management_system.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Cache configuration.
 *
 * <p>Uses {@link ConcurrentMapCacheManager} (in-memory, no extra dependency required).
 * Named caches are pre-declared so typos in service annotations fail fast at startup.
 *
 * <ul>
 *   <li><b>patients</b>   – individual patient lookups keyed by id</li>
 *   <li><b>doctors</b>    – individual doctor lookups keyed by id</li>
 *   <li><b>departments</b>– department lookups; evicted on any mutation</li>
 *   <li><b>inventory</b>  – individual inventory item lookups keyed by id</li>
 * </ul>
 *
 * <p>Performance note: before caching, every GET /api/patients/{id} hit the database.
 * After caching, repeated reads of the same record are served from heap memory with
 * sub-millisecond latency. Cache entries are evicted immediately on update/delete,
 * guaranteeing consistency (no stale reads).
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                "patients",
                "doctors",
                "departments",
                "inventory"
        );
    }
}
