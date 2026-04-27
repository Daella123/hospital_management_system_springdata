package com.daella.hospital_management_system.service;

import com.daella.hospital_management_system.dto.request.DepartmentRequest;
import com.daella.hospital_management_system.dto.response.DepartmentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DepartmentService {

    DepartmentResponse createDepartment(DepartmentRequest request);

    DepartmentResponse getDepartmentById(Long id);

    DepartmentResponse updateDepartment(Long id, DepartmentRequest request);

    void deleteDepartment(Long id);

    List<DepartmentResponse> getAllDepartments();

    Page<DepartmentResponse> getAllDepartmentsPaged(Pageable pageable);

    List<DepartmentResponse> searchDepartments(String name);
}
