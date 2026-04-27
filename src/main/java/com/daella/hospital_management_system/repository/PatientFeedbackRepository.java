package com.daella.hospital_management_system.repository;

import com.daella.hospital_management_system.entity.PatientFeedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PatientFeedbackRepository extends JpaRepository<PatientFeedback, Long> {

    // ── Derived Queries ───────────────────────────────────────────────────────

    Page<PatientFeedback> findByPatientId(Long patientId, Pageable pageable);

    Page<PatientFeedback> findByDoctorId(Long doctorId, Pageable pageable);

    Page<PatientFeedback> findByRating(Integer rating, Pageable pageable);

    Page<PatientFeedback> findByRatingGreaterThanEqual(Integer minRating, Pageable pageable);

    // ── JPQL Queries ──────────────────────────────────────────────────────────

    /**
     * JPQL — all feedback submitted for doctors belonging to a specific department.
     * Navigates PatientFeedback → doctor → department.
     */
    @Query("SELECT f FROM PatientFeedback f " +
           "JOIN f.doctor d " +
           "WHERE d.department.id = :departmentId " +
           "ORDER BY f.feedbackDate DESC")
    Page<PatientFeedback> findByDepartmentId(@Param("departmentId") Long departmentId, Pageable pageable);
}

