package com.daella.hospital_management_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Hospital Management System — Spring Boot entry point.
 *
 * <p>{@code @EnableScheduling} activates the token-blacklist cleanup scheduler
 * in {@link com.daella.hospital_management_system.security.TokenBlacklistService}.
 * {@code @EnableCaching} activates the Spring Cache configuration from CacheConfig.
 */
@SpringBootApplication
@EnableScheduling
@EnableCaching
public class HospitalManagementSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(HospitalManagementSystemApplication.class, args);
    }

}
