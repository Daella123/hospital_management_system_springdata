package com.daella.hospital_management_system.service.impl;

import com.daella.hospital_management_system.dto.request.PatientFeedbackRequest;
import com.daella.hospital_management_system.dto.response.PatientFeedbackResponse;
import com.daella.hospital_management_system.entity.Doctor;
import com.daella.hospital_management_system.entity.Patient;
import com.daella.hospital_management_system.entity.PatientFeedback;
import com.daella.hospital_management_system.exception.ResourceNotFoundException;
import com.daella.hospital_management_system.repository.DoctorRepository;
import com.daella.hospital_management_system.repository.PatientFeedbackRepository;
import com.daella.hospital_management_system.repository.PatientRepository;
import com.daella.hospital_management_system.service.PatientFeedbackService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PatientFeedbackServiceImpl implements PatientFeedbackService {

    private final PatientFeedbackRepository feedbackRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    public PatientFeedbackServiceImpl(PatientFeedbackRepository feedbackRepository,
                                       PatientRepository patientRepository,
                                       DoctorRepository doctorRepository) {
        this.feedbackRepository = feedbackRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Override
    public PatientFeedbackResponse createFeedback(PatientFeedbackRequest request) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", request.getPatientId()));

        Doctor doctor = null;
        if (request.getDoctorId() != null) {
            doctor = doctorRepository.findById(request.getDoctorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", request.getDoctorId()));
        }

        PatientFeedback saved = feedbackRepository.save(PatientFeedback.builder()
                .patient(patient)
                .doctor(doctor)
                .rating(request.getRating())
                .comment(request.getComment())
                .feedbackDate(request.getFeedbackDate())
                .build());
        return toResponse(saved);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PatientFeedbackResponse getFeedbackById(Long id) {
        return toResponse(feedbackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback", "id", id)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PatientFeedbackResponse> getAllFeedbacks(Pageable pageable) {
        return feedbackRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PatientFeedbackResponse> getFeedbackByPatient(Long patientId, Pageable pageable) {
        return feedbackRepository.findByPatientId(patientId, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PatientFeedbackResponse> getFeedbackByDoctor(Long doctorId, Pageable pageable) {
        return feedbackRepository.findByDoctorId(doctorId, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PatientFeedbackResponse> getFeedbackByMinRating(Integer minRating, Pageable pageable) {
        return feedbackRepository.findByRatingGreaterThanEqual(minRating, pageable).map(this::toResponse);
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Override
    public void deleteFeedback(Long id) {
        if (!feedbackRepository.existsById(id)) {
            throw new ResourceNotFoundException("Feedback", "id", id);
        }
        feedbackRepository.deleteById(id);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public PatientFeedbackResponse toResponse(PatientFeedback f) {
        String patientName = f.getPatient().getFirstName() + " " + f.getPatient().getLastName();
        Long doctorId = f.getDoctor() != null ? f.getDoctor().getId() : null;
        String doctorName = f.getDoctor() != null
                ? "Dr. " + f.getDoctor().getFirstName() + " " + f.getDoctor().getLastName() : null;

        return PatientFeedbackResponse.builder()
                .id(f.getId())
                .patientId(f.getPatient().getId())
                .patientFullName(patientName)
                .doctorId(doctorId)
                .doctorFullName(doctorName)
                .rating(f.getRating())
                .comment(f.getComment())
                .feedbackDate(f.getFeedbackDate())
                .createdAt(f.getCreatedAt())
                .build();
    }
}
