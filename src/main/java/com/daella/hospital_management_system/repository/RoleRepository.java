package com.daella.hospital_management_system.repository;

import com.daella.hospital_management_system.entity.Role;
import com.daella.hospital_management_system.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for {@link Role} entities.
 */
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);
}
