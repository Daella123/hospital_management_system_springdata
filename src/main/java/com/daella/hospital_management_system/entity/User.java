package com.daella.hospital_management_system.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Application user entity — stores both local (username/password) and
 * OAuth2 (Google) users in the same table.
 *
 * <p>Passwords are <strong>always</strong> stored as BCrypt hashes.
 * Plain-text passwords are never persisted.</p>
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_email", columnList = "email")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /** BCrypt-hashed password. Null for OAuth2-only users. */
    @Column(length = 255)
    private String password;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    /**
     * Authentication provider: "LOCAL" for username/password users,
     * "GOOGLE" for OAuth2 users.
     */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String provider = "LOCAL";

    /** Google subject ID — used to link OAuth2 accounts. */
    @Column(name = "provider_id", length = 100)
    private String providerId;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
