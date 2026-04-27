package com.daella.hospital_management_system.service.impl;

import com.daella.hospital_management_system.dto.request.DepartmentRequest;
import com.daella.hospital_management_system.dto.response.DepartmentResponse;
import com.daella.hospital_management_system.entity.Department;
import com.daella.hospital_management_system.exception.DuplicateResourceException;
import com.daella.hospital_management_system.exception.InvalidOperationException;
import com.daella.hospital_management_system.exception.ResourceNotFoundException;
import com.daella.hospital_management_system.repository.DepartmentRepository;
import com.daella.hospital_management_system.service.DepartmentService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentServiceImpl(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Override
    @CacheEvict(value = "departments", allEntries = true)
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        if (departmentRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException(
                    "Department '" + request.getName() + "' already exists");
        }
        Department saved = departmentRepository.save(toEntity(request));
        return toResponse(saved);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "departments", key = "#id")
    public DepartmentResponse getDepartmentById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "departments", key = "'all'")
    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DepartmentResponse> getAllDepartmentsPaged(Pageable pageable) {
        return departmentRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponse> searchDepartments(String name) {
        return departmentRepository.findByNameContainingIgnoreCase(name)
                .stream().map(this::toResponse).toList();
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Override
    @Caching(evict = {
            @CacheEvict(value = "departments", key = "#id"),
            @CacheEvict(value = "departments", key = "'all'")
    })
    public DepartmentResponse updateDepartment(Long id, DepartmentRequest request) {
        Department dept = findOrThrow(id);
        if (!dept.getName().equalsIgnoreCase(request.getName())
                && departmentRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException(
                    "Department name '" + request.getName() + "' is already in use");
        }
        dept.setName(request.getName());
        dept.setDescription(request.getDescription());
        dept.setHeadOfDepartment(request.getHeadOfDepartment());
        return toResponse(departmentRepository.save(dept));
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Override
    @Caching(evict = {
            @CacheEvict(value = "departments", key = "#id"),
            @CacheEvict(value = "departments", key = "'all'")
    })
    public void deleteDepartment(Long id) {
        Department dept = findOrThrow(id);
        if (!dept.getDoctors().isEmpty()) {
            throw new InvalidOperationException(
                    "Cannot delete department '" + dept.getName() +
                    "' because it still has " + dept.getDoctors().size() + " doctor(s) assigned");
        }
        departmentRepository.deleteById(id);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Department findOrThrow(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));
    }

    private Department toEntity(DepartmentRequest r) {
        return Department.builder()
                .name(r.getName())
                .description(r.getDescription())
                .headOfDepartment(r.getHeadOfDepartment())
                .build();
    }

    public DepartmentResponse toResponse(Department d) {
        return DepartmentResponse.builder()
                .id(d.getId())
                .name(d.getName())
                .description(d.getDescription())
                .headOfDepartment(d.getHeadOfDepartment())
                .doctorCount(d.getDoctors() != null ? d.getDoctors().size() : 0)
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }
}
