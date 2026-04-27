package com.daella.hospital_management_system.service.impl;

import com.daella.hospital_management_system.dto.request.AppointmentRequest;
import com.daella.hospital_management_system.dto.response.AppointmentResponse;
import com.daella.hospital_management_system.entity.Appointment;
import com.daella.hospital_management_system.entity.Doctor;
import com.daella.hospital_management_system.entity.Patient;
import com.daella.hospital_management_system.enums.AppointmentStatus;
import com.daella.hospital_management_system.exception.InvalidOperationException;
import com.daella.hospital_management_system.exception.ResourceNotFoundException;
import com.daella.hospital_management_system.repository.AppointmentRepository;
import com.daella.hospital_management_system.repository.DoctorRepository;
import com.daella.hospital_management_system.repository.PatientRepository;
import com.daella.hospital_management_system.service.AppointmentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Appointment service implementation.
 *
 * <p><b>Transaction strategy:</b>
 * <ul>
 *   <li>{@code createAppointment} — {@code READ_COMMITTED} isolation avoids dirty reads
 *       when checking slot availability. {@code rollbackFor = Exception.class} ensures
 *       any unexpected error rolls back the partially saved appointment.</li>
 *   <li>{@code cancelAppointment} — {@code REQUIRED} propagation so it always runs within
 *       a transaction; rollback on any exception keeps status consistent.</li>
 *   <li>All read methods — {@code readOnly = true} for Hibernate optimisations.</li>
 * </ul>
 *
 * <p><b>Rollback scenarios:</b>
 * <ul>
 *   <li>Doctor not found → {@link ResourceNotFoundException} → rollback.</li>
 *   <li>Patient not found → {@link ResourceNotFoundException} → rollback.</li>
 *   <li>Slot already taken → {@link InvalidOperationException} → rollback.</li>
 *   <li>Cancelling an already-completed appointment → {@link InvalidOperationException} → rollback.</li>
 * </ul>
 */
@Service
@Transactional
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    public AppointmentServiceImpl(AppointmentRepository appointmentRepository,
                                   PatientRepository patientRepository,
                                   DoctorRepository doctorRepository) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
    }

    // ── Create ────────────────────────────────────────────────────────────────

    /**
     * Books an appointment.
     *
     * <p>Uses READ_COMMITTED isolation: slot-check reads committed data only,
     * preventing dirty-read false positives. Rolls back on any exception so
     * no half-created appointment is persisted.
     *
     * <p><b>Rollback scenarios:</b> invalid patient ID, invalid doctor ID, slot already taken.
     */
    @Override
    @Transactional(
            propagation = Propagation.REQUIRED,
            isolation   = Isolation.READ_COMMITTED,
            rollbackFor = Exception.class
    )
    public AppointmentResponse createAppointment(AppointmentRequest request) {
        Patient patient = findPatientOrThrow(request.getPatientId());
        Doctor  doctor  = findDoctorOrThrow(request.getDoctorId());
        validateSlot(doctor.getId(), request.getAppointmentDateTime(), null);

        Appointment saved = appointmentRepository.save(Appointment.builder()
                .patient(patient)
                .doctor(doctor)
                .appointmentDateTime(request.getAppointmentDateTime())
                .status(request.getStatus() != null ? request.getStatus() : AppointmentStatus.SCHEDULED)
                .reason(request.getReason())
                .notes(request.getNotes())
                .build());
        return toResponse(saved);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponse getAppointmentById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AppointmentResponse> getAllAppointments(Pageable pageable) {
        return appointmentRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AppointmentResponse> getAppointmentsByPatient(Long patientId, Pageable pageable) {
        findPatientOrThrow(patientId);
        return appointmentRepository.findByPatientId(patientId, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AppointmentResponse> getAppointmentsByDoctor(Long doctorId, Pageable pageable) {
        findDoctorOrThrow(doctorId);
        return appointmentRepository.findByDoctorId(doctorId, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AppointmentResponse> getAppointmentsByStatus(AppointmentStatus status, Pageable pageable) {
        return appointmentRepository.findByStatus(status, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AppointmentResponse> getAppointmentsByDateRange(LocalDateTime start, LocalDateTime end,
                                                                 Pageable pageable) {
        if (start.isAfter(end)) {
            throw new InvalidOperationException("Start date must be before end date");
        }
        return appointmentRepository.findByAppointmentDateTimeBetween(start, end, pageable)
                .map(this::toResponse);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Override
    public AppointmentResponse updateAppointment(Long id, AppointmentRequest request) {
        Appointment appt   = findOrThrow(id);
        Patient     patient = findPatientOrThrow(request.getPatientId());
        Doctor      doctor  = findDoctorOrThrow(request.getDoctorId());

        boolean slotChanged = !appt.getAppointmentDateTime().equals(request.getAppointmentDateTime())
                || !appt.getDoctor().getId().equals(request.getDoctorId());
        if (slotChanged) {
            validateSlot(doctor.getId(), request.getAppointmentDateTime(), id);
        }

        appt.setPatient(patient);
        appt.setDoctor(doctor);
        appt.setAppointmentDateTime(request.getAppointmentDateTime());
        appt.setReason(request.getReason());
        appt.setNotes(request.getNotes());
        if (request.getStatus() != null) {
            appt.setStatus(request.getStatus());
        }
        return toResponse(appointmentRepository.save(appt));
    }

    @Override
    public AppointmentResponse updateStatus(Long id, AppointmentStatus status) {
        Appointment appt = findOrThrow(id);
        if (appt.getStatus() == AppointmentStatus.CANCELLED) {
            throw new InvalidOperationException("Cannot change status of a cancelled appointment");
        }
        appt.setStatus(status);
        return toResponse(appointmentRepository.save(appt));
    }

    /**
     * Cancels an appointment.
     *
     * <p>Explicit propagation + rollbackFor: if the status update fails for any reason
     * (e.g. DB constraint, runtime error), the status stays unchanged.
     *
     * <p><b>Rollback scenarios:</b> appointment already COMPLETED.
     */
    @Override
    @Transactional(
            propagation = Propagation.REQUIRED,
            rollbackFor = Exception.class
    )
    public void cancelAppointment(Long id) {
        Appointment appt = findOrThrow(id);
        if (appt.getStatus() == AppointmentStatus.COMPLETED) {
            throw new InvalidOperationException("Cannot cancel an already completed appointment");
        }
        appt.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appt);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Appointment findOrThrow(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", id));
    }

    private Patient findPatientOrThrow(Long patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", patientId));
    }

    private Doctor findDoctorOrThrow(Long doctorId) {
        return doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", doctorId));
    }

    /**
     * Ensures the doctor has no active appointment at the requested time.
     * Pass excludeId to skip the current appointment when updating.
     */
    private void validateSlot(Long doctorId, LocalDateTime dateTime, Long excludeId) {
        if (appointmentRepository.isDoctorSlotTaken(doctorId, dateTime)) {
            throw new InvalidOperationException(
                    "Doctor already has an active appointment at " + dateTime);
        }
    }

    public AppointmentResponse toResponse(Appointment a) {
        String patientName = a.getPatient().getFirstName() + " " + a.getPatient().getLastName();
        String doctorName  = "Dr. " + a.getDoctor().getFirstName() + " " + a.getDoctor().getLastName();
        String deptName    = a.getDoctor().getDepartment() != null
                ? a.getDoctor().getDepartment().getName() : null;

        return AppointmentResponse.builder()
                .id(a.getId())
                .patientId(a.getPatient().getId())
                .patientFullName(patientName)
                .doctorId(a.getDoctor().getId())
                .doctorFullName(doctorName)
                .departmentName(deptName)
                .appointmentDateTime(a.getAppointmentDateTime())
                .status(a.getStatus())
                .reason(a.getReason())
                .notes(a.getNotes())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
