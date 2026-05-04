package com.daella.hospital_management_system.entity;

import com.daella.hospital_management_system.enums.RoleName;
import jakarta.persistence.*;
import lombok.*;

/**
 * Role entity — maps to the `roles` table.
 * Values correspond to {@link RoleName}: ADMIN, DOCTOR, NURSE, RECEPTIONIST.
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "name", nullable = false, unique = true, length = 30)
    private RoleName name;
}
