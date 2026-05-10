package com.daella.hospital_management_system.repository;

import com.daella.hospital_management_system.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    // ── Derived Queries ───────────────────────────────────────────────────────

    Optional<Prescription> findByAppointmentId(Long appointmentId);

    boolean existsByAppointmentId(Long appointmentId);

    List<Prescription> findByAppointmentPatientId(Long patientId);

    // ── JPQL Queries ──────────────────────────────────────────────────────────

    /**
     * JPQL — full medical history for a patient: prescriptions with their items,
     * appointment, doctor, doctor's User (for name/email), and department all eagerly
     * fetched to avoid N+1.
     */
    @Query("SELECT DISTINCT p FROM Prescription p " +
           "JOIN FETCH p.appointment a " +
           "JOIN FETCH a.doctor d " +
           "JOIN FETCH d.user " +
           "JOIN FETCH d.department " +
           "LEFT JOIN FETCH p.items " +
           "WHERE a.patient.id = :patientId " +
           "ORDER BY p.issuedDate DESC")
    List<Prescription> findMedicalHistoryByPatient(@Param("patientId") Long patientId);

    // ── Native SQL Queries ────────────────────────────────────────────────────

    /**
     * Native SQL — prescription count grouped by doctor.
     * Joins users table because firstName/lastName now live there, not in doctors.
     * Returns rows of [doctor_id, doctor_name, prescription_count].
     */
    @Query(value = """
            SELECT doc.id                                        AS doctor_id,
                   CONCAT(u.first_name, ' ', u.last_name)       AS doctor_name,
                   COUNT(p.id)                                   AS prescription_count
            FROM prescriptions p
            JOIN appointments a  ON a.id   = p.appointment_id
            JOIN doctors doc     ON doc.id = a.doctor_id
            JOIN users u         ON u.id   = doc.user_id
            GROUP BY doc.id, doctor_name
            ORDER BY prescription_count DESC
            """, nativeQuery = true)
    List<Object[]> countPrescriptionsByDoctorNative();
}

