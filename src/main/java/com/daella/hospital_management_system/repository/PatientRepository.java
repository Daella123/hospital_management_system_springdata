package com.daella.hospital_management_system.repository;

import com.daella.hospital_management_system.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    // ── Derived Queries ───────────────────────────────────────────────────────

    Optional<Patient> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<Patient> findByPhone(String phone);

    Page<Patient> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            String firstName, String lastName, Pageable pageable);

    // ── Native SQL Queries ────────────────────────────────────────────────────

    /**
     * Native SQL — patient registration count grouped by month for a given year.
     * Returns rows of [month_number, registration_count].
     */
    @Query(value = """
            SELECT EXTRACT(MONTH FROM created_at) AS month,
                   COUNT(*) AS registration_count
            FROM patients
            WHERE EXTRACT(YEAR FROM created_at) = :year
            GROUP BY month
            ORDER BY month
            """, nativeQuery = true)
    List<Object[]> countRegistrationsByMonthNative(@Param("year") int year);
}

