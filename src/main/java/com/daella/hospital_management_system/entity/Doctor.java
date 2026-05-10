package com.daella.hospital_management_system.entity;

import com.daella.hospital_management_system.enums.Gender;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Doctor entity — hospital-specific profile information.
 *
 * <p>Identity fields (firstName, lastName, email) live in the linked {@link User}.
 * This entity holds only what is unique to a doctor's professional profile.
 */
@Entity
@Table(name = "doctors", indexes = {
        @Index(name = "idx_doctor_user_id",       columnList = "user_id"),
        @Index(name = "idx_doctor_department_id", columnList = "department_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The linked User account — single source of truth for name and email.
     * CascadeType.PERSIST: saving a new Doctor also saves a new User.
     * CascadeType.MERGE:   updating a Doctor also merges User field changes.
     * No REMOVE cascade: deletion is handled explicitly in the service.
     */
    @OneToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Column(length = 100)
    private String specialization;

    @Column(name = "license_number", unique = true, length = 50)
    private String licenseNumber;

    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    // Nullable: a doctor may be unassigned to a department initially (e.g. during self-registration).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @OneToMany(mappedBy = "doctor", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Appointment> appointments = new ArrayList<>();

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
