package com.daella.hospital_management_system.service.impl;

import com.daella.hospital_management_system.dto.request.PrescriptionRequest;
import com.daella.hospital_management_system.dto.response.PrescriptionItemResponse;
import com.daella.hospital_management_system.dto.response.PrescriptionResponse;
import com.daella.hospital_management_system.entity.Appointment;
import com.daella.hospital_management_system.entity.Prescription;
import com.daella.hospital_management_system.entity.PrescriptionItem;
import com.daella.hospital_management_system.enums.AppointmentStatus;
import com.daella.hospital_management_system.exception.DuplicateResourceException;
import com.daella.hospital_management_system.exception.InvalidOperationException;
import com.daella.hospital_management_system.exception.ResourceNotFoundException;
import com.daella.hospital_management_system.repository.AppointmentRepository;
import com.daella.hospital_management_system.repository.PrescriptionRepository;
import com.daella.hospital_management_system.service.PrescriptionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Prescription service implementation.
 *
 * <p><b>Transaction strategy:</b>
 * <ul>
 *   <li>{@code createPrescription} — {@code READ_COMMITTED} isolation prevents dirty reads
 *       when checking appointment status and duplicate prescriptions.
 *       {@code rollbackFor = Exception.class} rolls back the entire prescription
 *       (header + all items persisted via cascade) if any item fails to save.</li>
 *   <li>Read methods — {@code readOnly = true}.</li>
 * </ul>
 *
 * <p><b>Rollback scenarios:</b>
 * <ul>
 *   <li>Appointment not CONFIRMED or COMPLETED → {@link InvalidOperationException} → rollback.</li>
 *   <li>Prescription already exists for appointment → {@link DuplicateResourceException} → rollback.</li>
 *   <li>Any item processing error (e.g. DB constraint) → rollback of entire prescription.</li>
 * </ul>
 */
@Service
@Transactional
public class PrescriptionServiceImpl implements PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final AppointmentRepository  appointmentRepository;

    public PrescriptionServiceImpl(PrescriptionRepository prescriptionRepository,
                                    AppointmentRepository appointmentRepository) {
        this.prescriptionRepository = prescriptionRepository;
        this.appointmentRepository  = appointmentRepository;
    }

    // ── Create ────────────────────────────────────────────────────────────────

    /**
     * Creates a prescription with all its items atomically.
     *
     * <p>If any item build or save fails, the whole transaction rolls back —
     * preventing orphaned prescription headers with no items.
     */
    @Override
    @Transactional(
            propagation = Propagation.REQUIRED,
            isolation   = Isolation.READ_COMMITTED,
            rollbackFor = Exception.class
    )
    public PrescriptionResponse createPrescription(PrescriptionRequest request) {
        Appointment appointment = findAppointmentOrThrow(request.getAppointmentId());

        if (appointment.getStatus() != AppointmentStatus.COMPLETED
                && appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new InvalidOperationException(
                    "Prescription can only be issued for CONFIRMED or COMPLETED appointments");
        }
        if (prescriptionRepository.existsByAppointmentId(request.getAppointmentId())) {
            throw new DuplicateResourceException(
                    "A prescription already exists for appointment " + request.getAppointmentId());
        }

        Prescription prescription = buildPrescription(request, appointment);
        return toResponse(prescriptionRepository.save(prescription));
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PrescriptionResponse getPrescriptionById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PrescriptionResponse getPrescriptionByAppointment(Long appointmentId) {
        return toResponse(prescriptionRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Prescription for appointment", "appointmentId", appointmentId)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrescriptionResponse> getPrescriptionsByPatient(Long patientId) {
        return prescriptionRepository.findByAppointmentPatientId(patientId)
                .stream().map(this::toResponse).toList();
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Override
    public PrescriptionResponse updatePrescription(Long id, PrescriptionRequest request) {
        Prescription prescription = findOrThrow(id);
        Appointment  appointment  = findAppointmentOrThrow(request.getAppointmentId());

        prescription.setAppointment(appointment);
        prescription.setIssuedDate(request.getIssuedDate());
        prescription.setExpiryDate(request.getExpiryDate());
        prescription.setDiagnosis(request.getDiagnosis());
        prescription.setNotes(request.getNotes());
        prescription.getItems().clear();
        request.getItems().stream()
                .map(ir -> PrescriptionItem.builder()
                        .prescription(prescription)
                        .medicineName(ir.getMedicineName())
                        .dosage(ir.getDosage())
                        .frequency(ir.getFrequency())
                        .duration(ir.getDuration())
                        .instructions(ir.getInstructions())
                        .build())
                .forEach(prescription.getItems()::add);

        return toResponse(prescriptionRepository.save(prescription));
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Override
    public void deletePrescription(Long id) {
        if (!prescriptionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Prescription", "id", id);
        }
        prescriptionRepository.deleteById(id);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Prescription findOrThrow(Long id) {
        return prescriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription", "id", id));
    }

    private Appointment findAppointmentOrThrow(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", id));
    }

    private Prescription buildPrescription(PrescriptionRequest r, Appointment appointment) {
        Prescription p = Prescription.builder()
                .appointment(appointment)
                .issuedDate(r.getIssuedDate())
                .expiryDate(r.getExpiryDate())
                .diagnosis(r.getDiagnosis())
                .notes(r.getNotes())
                .build();

        List<PrescriptionItem> items = r.getItems().stream()
                .map(ir -> PrescriptionItem.builder()
                        .prescription(p)
                        .medicineName(ir.getMedicineName())
                        .dosage(ir.getDosage())
                        .frequency(ir.getFrequency())
                        .duration(ir.getDuration())
                        .instructions(ir.getInstructions())
                        .build())
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        p.setItems(items);
        return p;
    }

    public PrescriptionResponse toResponse(Prescription p) {
        Appointment appt       = p.getAppointment();
        String      patientName = appt.getPatient().getFirstName() + " " + appt.getPatient().getLastName();
        String      doctorName  = "Dr. " + appt.getDoctor().getFirstName() + " " + appt.getDoctor().getLastName();

        List<PrescriptionItemResponse> itemResponses = p.getItems().stream()
                .map(i -> PrescriptionItemResponse.builder()
                        .id(i.getId())
                        .medicineName(i.getMedicineName())
                        .dosage(i.getDosage())
                        .frequency(i.getFrequency())
                        .duration(i.getDuration())
                        .instructions(i.getInstructions())
                        .createdAt(i.getCreatedAt())
                        .build())
                .toList();

        return PrescriptionResponse.builder()
                .id(p.getId())
                .appointmentId(appt.getId())
                .patientId(appt.getPatient().getId())
                .patientFullName(patientName)
                .doctorFullName(doctorName)
                .issuedDate(p.getIssuedDate())
                .expiryDate(p.getExpiryDate())
                .diagnosis(p.getDiagnosis())
                .notes(p.getNotes())
                .items(itemResponses)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
