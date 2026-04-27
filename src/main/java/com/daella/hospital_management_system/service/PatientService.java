package com.daella.hospital_management_system.service;

import com.daella.hospital_management_system.dto.request.PatientRequest;
import com.daella.hospital_management_system.dto.response.PatientResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PatientService {

    PatientResponse createPatient(PatientRequest request);

    PatientResponse getPatientById(Long id);

    PatientResponse updatePatient(Long id, PatientRequest request);

    void deletePatient(Long id);

    Page<PatientResponse> getAllPatients(Pageable pageable);

    Page<PatientResponse> searchPatients(String query, Pageable pageable);
}
