package com.daella.hospital_management_system.repository;

import com.daella.hospital_management_system.entity.Appointment;
import com.daella.hospital_management_system.enums.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // ── Derived Queries ───────────────────────────────────────────────────────

    Page<Appointment> findByPatientId(Long patientId, Pageable pageable);

    Page<Appointment> findByDoctorId(Long doctorId, Pageable pageable);

    Page<Appointment> findByStatus(AppointmentStatus status, Pageable pageable);

    Page<Appointment> findByAppointmentDateTimeBetween(
            LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<Appointment> findByDoctorIdAndAppointmentDateTimeBetween(
            Long doctorId, LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<Appointment> findByPatientIdAndStatus(Long patientId, AppointmentStatus status, Pageable pageable);

    // ── JPQL Queries ──────────────────────────────────────────────────────────

    /** Check whether a doctor already has a non-cancelled appointment at the given time. */
    @Query("SELECT COUNT(a) > 0 FROM Appointment a " +
           "WHERE a.doctor.id = :doctorId " +
           "AND a.appointmentDateTime = :dateTime " +
           "AND a.status NOT IN ('CANCELLED', 'NO_SHOW')")
    boolean isDoctorSlotTaken(@Param("doctorId") Long doctorId,
                               @Param("dateTime") LocalDateTime dateTime);

    /**
     * JPQL — all appointments belonging to a specific department,
     * navigating through the doctor → department relationship.
     */
    @Query("SELECT a FROM Appointment a " +
           "JOIN a.doctor d " +
           "WHERE d.department.id = :departmentId " +
           "ORDER BY a.appointmentDateTime DESC")
    Page<Appointment> findByDepartmentId(@Param("departmentId") Long departmentId, Pageable pageable);

    /**
     * JPQL — a patient's full appointment history ordered newest first.
     * Fetches doctor eagerly to avoid N+1 when building responses.
     */
    @Query("SELECT a FROM Appointment a " +
           "JOIN FETCH a.doctor d " +
           "WHERE a.patient.id = :patientId " +
           "ORDER BY a.appointmentDateTime DESC")
    List<Appointment> findPatientHistory(@Param("patientId") Long patientId);

    // ── Native SQL Queries ────────────────────────────────────────────────────

    /**
     * Native SQL — total appointment count grouped by department.
     * Returns rows of [department_name, appointment_count].
     */
    @Query(value = """
            SELECT d.name AS department_name,
                   COUNT(a.id) AS appointment_count
            FROM appointments a
            JOIN doctors doc ON doc.id = a.doctor_id
            JOIN departments d ON d.id  = doc.department_id
            GROUP BY d.name
            ORDER BY appointment_count DESC
            """, nativeQuery = true)
    List<Object[]> countAppointmentsByDepartmentNative();

    /**
     * Native SQL — appointment count grouped by calendar month for a given year.
     * Returns rows of [month_number, appointment_count].
     */
    @Query(value = """
            SELECT EXTRACT(MONTH FROM appointment_date_time) AS month,
                   COUNT(*) AS appointment_count
            FROM appointments
            WHERE EXTRACT(YEAR FROM appointment_date_time) = :year
            GROUP BY month
            ORDER BY month
            """, nativeQuery = true)
    List<Object[]> countAppointmentsByMonthNative(@Param("year") int year);
}

