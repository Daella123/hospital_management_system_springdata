package com.daella.hospital_management_system.service.impl;

import com.daella.hospital_management_system.dto.request.DoctorRequest;
import com.daella.hospital_management_system.dto.response.DoctorResponse;
import com.daella.hospital_management_system.entity.Department;
import com.daella.hospital_management_system.entity.Doctor;
import com.daella.hospital_management_system.exception.DuplicateResourceException;
import com.daella.hospital_management_system.exception.ResourceNotFoundException;
import com.daella.hospital_management_system.repository.DepartmentRepository;
import com.daella.hospital_management_system.repository.DoctorRepository;
import com.daella.hospital_management_system.service.DoctorService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;

    public DoctorServiceImpl(DoctorRepository doctorRepository,
                              DepartmentRepository departmentRepository) {
        this.doctorRepository = doctorRepository;
        this.departmentRepository = departmentRepository;
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Override
    public DoctorResponse createDoctor(DoctorRequest request) {
        if (doctorRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                    "Doctor with email '" + request.getEmail() + "' already exists");
        }
        if (doctorRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new DuplicateResourceException(
                    "License number '" + request.getLicenseNumber() + "' is already registered");
        }
        Department department = findDepartmentOrThrow(request.getDepartmentId());
        Doctor saved = doctorRepository.save(toEntity(request, department));
        return toResponse(saved);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "doctors", key = "#id")
    public DoctorResponse getDoctorById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DoctorResponse> getAllDoctors(Pageable pageable) {
        return doctorRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorResponse> getDoctorsByDepartment(Long departmentId) {
        findDepartmentOrThrow(departmentId);
        return doctorRepository.findByDepartmentId(departmentId).stream()
                .map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DoctorResponse> getDoctorsByDepartmentPaged(Long departmentId, Pageable pageable) {
        findDepartmentOrThrow(departmentId);
        return doctorRepository.findByDepartmentId(departmentId, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DoctorResponse> searchDoctors(String query, Pageable pageable) {
        return doctorRepository
                .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(query, query, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DoctorResponse> getDoctorsBySpecialization(String specialization, Pageable pageable) {
        return doctorRepository.findBySpecializationContainingIgnoreCase(specialization, pageable)
                .map(this::toResponse);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Override
    @CachePut(value = "doctors", key = "#id")
    public DoctorResponse updateDoctor(Long id, DoctorRequest request) {
        Doctor doctor = findOrThrow(id);

        if (!doctor.getEmail().equalsIgnoreCase(request.getEmail())
                && doctorRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                    "Email '" + request.getEmail() + "' is already taken");
        }
        if (!doctor.getLicenseNumber().equalsIgnoreCase(request.getLicenseNumber())
                && doctorRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new DuplicateResourceException(
                    "License number '" + request.getLicenseNumber() + "' is already registered");
        }

        Department department = findDepartmentOrThrow(request.getDepartmentId());
        applyFields(doctor, request, department);
        return toResponse(doctorRepository.save(doctor));
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Override
    @CacheEvict(value = "doctors", key = "#id")
    public void deleteDoctor(Long id) {
        if (!doctorRepository.existsById(id)) {
            throw new ResourceNotFoundException("Doctor", "id", id);
        }
        doctorRepository.deleteById(id);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Doctor findOrThrow(Long id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", id));
    }

    private Department findDepartmentOrThrow(Long departmentId) {
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", departmentId));
    }

    private Doctor toEntity(DoctorRequest r, Department department) {
        return Doctor.builder()
                .firstName(r.getFirstName())
                .lastName(r.getLastName())
                .email(r.getEmail())
                .phone(r.getPhone())
                .gender(r.getGender())
                .specialization(r.getSpecialization())
                .licenseNumber(r.getLicenseNumber())
                .yearsOfExperience(r.getYearsOfExperience())
                .dateOfBirth(r.getDateOfBirth())
                .department(department)
                .build();
    }

    private void applyFields(Doctor d, DoctorRequest r, Department department) {
        d.setFirstName(r.getFirstName());
        d.setLastName(r.getLastName());
        d.setEmail(r.getEmail());
        d.setPhone(r.getPhone());
        d.setGender(r.getGender());
        d.setSpecialization(r.getSpecialization());
        d.setLicenseNumber(r.getLicenseNumber());
        d.setYearsOfExperience(r.getYearsOfExperience());
        d.setDateOfBirth(r.getDateOfBirth());
        d.setDepartment(department);
    }

    public DoctorResponse toResponse(Doctor d) {
        return DoctorResponse.builder()
                .id(d.getId())
                .firstName(d.getFirstName())
                .lastName(d.getLastName())
                .email(d.getEmail())
                .phone(d.getPhone())
                .gender(d.getGender())
                .specialization(d.getSpecialization())
                .licenseNumber(d.getLicenseNumber())
                .yearsOfExperience(d.getYearsOfExperience())
                .dateOfBirth(d.getDateOfBirth())
                .departmentId(d.getDepartment().getId())
                .departmentName(d.getDepartment().getName())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }
}
