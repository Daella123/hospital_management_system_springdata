package com.daella.hospital_management_system.repository;

import com.daella.hospital_management_system.entity.Doctor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    // ── Derived Queries ───────────────────────────────────────────────────────

    Optional<Doctor> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByLicenseNumber(String licenseNumber);

    List<Doctor> findByDepartmentId(Long departmentId);

    Page<Doctor> findByDepartmentId(Long departmentId, Pageable pageable);

    Page<Doctor> findBySpecializationContainingIgnoreCase(String specialization, Pageable pageable);

    Page<Doctor> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            String firstName, String lastName, Pageable pageable);

    // ── JPQL Queries ──────────────────────────────────────────────────────────

    /**
     * JPQL — doctors in the given department who have NO active appointment
     * at the requested date/time (i.e. they are available).
     */
    @Query("SELECT d FROM Doctor d " +
           "WHERE d.department.id = :departmentId " +
           "AND d.id NOT IN (" +
           "    SELECT a.doctor.id FROM Appointment a " +
           "    WHERE a.appointmentDateTime = :dateTime " +
           "    AND a.status NOT IN ('CANCELLED', 'NO_SHOW')" +
           ")")
    List<Doctor> findAvailableDoctorsByDepartment(
            @Param("departmentId") Long departmentId,
            @Param("dateTime")     LocalDateTime dateTime);
}

