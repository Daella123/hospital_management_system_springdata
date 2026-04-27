package com.daella.hospital_management_system.service.impl;

import com.daella.hospital_management_system.dto.request.PatientRequest;
import com.daella.hospital_management_system.dto.response.PatientResponse;
import com.daella.hospital_management_system.entity.Patient;
import com.daella.hospital_management_system.exception.DuplicateResourceException;
import com.daella.hospital_management_system.exception.ResourceNotFoundException;
import com.daella.hospital_management_system.repository.PatientRepository;
import com.daella.hospital_management_system.service.PatientService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;

    public PatientServiceImpl(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Override
    public PatientResponse createPatient(PatientRequest request) {
        if (patientRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                    "Patient with email '" + request.getEmail() + "' already exists");
        }
        Patient saved = patientRepository.save(toEntity(request));
        return toResponse(saved);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "patients", key = "#id")
    public PatientResponse getPatientById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PatientResponse> getAllPatients(Pageable pageable) {
        return patientRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PatientResponse> searchPatients(String query, Pageable pageable) {
        return patientRepository
                .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(query, query, pageable)
                .map(this::toResponse);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Override
    @CachePut(value = "patients", key = "#id")
    public PatientResponse updatePatient(Long id, PatientRequest request) {
        Patient patient = findOrThrow(id);
        if (!patient.getEmail().equalsIgnoreCase(request.getEmail())
                && patientRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                    "Email '" + request.getEmail() + "' is already taken by another patient");
        }
        applyFields(patient, request);
        return toResponse(patientRepository.save(patient));
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Override
    @CacheEvict(value = "patients", key = "#id")
    public void deletePatient(Long id) {
        if (!patientRepository.existsById(id)) {
            throw new ResourceNotFoundException("Patient", "id", id);
        }
        patientRepository.deleteById(id);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Patient findOrThrow(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", id));
    }

    private Patient toEntity(PatientRequest r) {
        return Patient.builder()
                .firstName(r.getFirstName())
                .lastName(r.getLastName())
                .email(r.getEmail())
                .phone(r.getPhone())
                .dateOfBirth(r.getDateOfBirth())
                .gender(r.getGender())
                .bloodType(r.getBloodType())
                .address(r.getAddress())
                .emergencyContactName(r.getEmergencyContactName())
                .emergencyContactPhone(r.getEmergencyContactPhone())
                .medicalHistory(r.getMedicalHistory())
                .build();
    }

    private void applyFields(Patient p, PatientRequest r) {
        p.setFirstName(r.getFirstName());
        p.setLastName(r.getLastName());
        p.setEmail(r.getEmail());
        p.setPhone(r.getPhone());
        p.setDateOfBirth(r.getDateOfBirth());
        p.setGender(r.getGender());
        p.setBloodType(r.getBloodType());
        p.setAddress(r.getAddress());
        p.setEmergencyContactName(r.getEmergencyContactName());
        p.setEmergencyContactPhone(r.getEmergencyContactPhone());
        p.setMedicalHistory(r.getMedicalHistory());
    }

    public PatientResponse toResponse(Patient p) {
        return PatientResponse.builder()
                .id(p.getId())
                .firstName(p.getFirstName())
                .lastName(p.getLastName())
                .email(p.getEmail())
                .phone(p.getPhone())
                .dateOfBirth(p.getDateOfBirth())
                .gender(p.getGender())
                .bloodType(p.getBloodType())
                .address(p.getAddress())
                .emergencyContactName(p.getEmergencyContactName())
                .emergencyContactPhone(p.getEmergencyContactPhone())
                .medicalHistory(p.getMedicalHistory())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
