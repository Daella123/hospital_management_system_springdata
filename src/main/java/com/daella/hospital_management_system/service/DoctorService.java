package com.daella.hospital_management_system.service;

import com.daella.hospital_management_system.dto.request.DoctorRequest;
import com.daella.hospital_management_system.dto.response.DoctorResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DoctorService {

    DoctorResponse createDoctor(DoctorRequest request);

    DoctorResponse getDoctorById(Long id);

    DoctorResponse updateDoctor(Long id, DoctorRequest request);

    void deleteDoctor(Long id);

    Page<DoctorResponse> getAllDoctors(Pageable pageable);

    List<DoctorResponse> getDoctorsByDepartment(Long departmentId);

    Page<DoctorResponse> getDoctorsByDepartmentPaged(Long departmentId, Pageable pageable);

    Page<DoctorResponse> searchDoctors(String query, Pageable pageable);

    Page<DoctorResponse> getDoctorsBySpecialization(String specialization, Pageable pageable);
}
