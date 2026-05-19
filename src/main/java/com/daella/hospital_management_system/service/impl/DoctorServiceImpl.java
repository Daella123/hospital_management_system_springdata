package com.daella.hospital_management_system.service.impl;

import com.daella.hospital_management_system.dto.request.DoctorRequest;
import com.daella.hospital_management_system.dto.response.DoctorResponse;
import com.daella.hospital_management_system.entity.Department;
import com.daella.hospital_management_system.entity.Doctor;
import com.daella.hospital_management_system.entity.Role;
import com.daella.hospital_management_system.entity.User;
import com.daella.hospital_management_system.enums.RoleName;
import com.daella.hospital_management_system.exception.DuplicateResourceException;
import com.daella.hospital_management_system.exception.InvalidOperationException;
import com.daella.hospital_management_system.exception.ResourceNotFoundException;
import com.daella.hospital_management_system.repository.DepartmentRepository;
import com.daella.hospital_management_system.repository.DoctorRepository;
import com.daella.hospital_management_system.repository.RoleRepository;
import com.daella.hospital_management_system.repository.UserRepository;
import com.daella.hospital_management_system.service.DoctorService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Transactional
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository     doctorRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository       userRepository;
    private final RoleRepository       roleRepository;
    private final PasswordEncoder      passwordEncoder;

    public DoctorServiceImpl(DoctorRepository doctorRepository,
                              DepartmentRepository departmentRepository,
                              UserRepository userRepository,
                              RoleRepository roleRepository,
                              PasswordEncoder passwordEncoder) {
        this.doctorRepository     = doctorRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository       = userRepository;
        this.roleRepository       = roleRepository;
        this.passwordEncoder      = passwordEncoder;
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Override
    public DoctorResponse createDoctor(DoctorRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                    "A user with email '" + request.getEmail() + "' already exists");
        }
        if (doctorRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new DuplicateResourceException(
                    "License number '" + request.getLicenseNumber() + "' is already registered");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new InvalidOperationException("Password is required when creating a new doctor");
        }

        Role doctorRole = roleRepository.findByName(RoleName.DOCTOR)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: DOCTOR"));

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .provider("LOCAL")
                .enabled(true)
                .roles(Set.of(doctorRole))
                .build();

        Department department = findDepartmentOrThrow(request.getDepartmentId());

        // CascadeType.PERSIST on Doctor.user will persist the User when Doctor is saved.
        Doctor saved = doctorRepository.save(Doctor.builder()
                .user(user)
                .phone(request.getPhone())
                .gender(request.getGender())
                .specialization(request.getSpecialization())
                .licenseNumber(request.getLicenseNumber())
                .yearsOfExperience(request.getYearsOfExperience())
                .dateOfBirth(request.getDateOfBirth())
                .department(department)
                .build());

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
                .findByUser_FirstNameContainingIgnoreCaseOrUser_LastNameContainingIgnoreCase(query, query, pageable)
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
        User   user   = doctor.getUser();

        if (!user.getEmail().equalsIgnoreCase(request.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                    "Email '" + request.getEmail() + "' is already taken");
        }
        if (!doctor.getLicenseNumber().equalsIgnoreCase(request.getLicenseNumber())
                && doctorRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new DuplicateResourceException(
                    "License number '" + request.getLicenseNumber() + "' is already registered");
        }

        // Update identity fields on the linked User
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());

        // Update doctor-specific fields
        Department department = findDepartmentOrThrow(request.getDepartmentId());
        doctor.setPhone(request.getPhone());
        doctor.setGender(request.getGender());
        doctor.setSpecialization(request.getSpecialization());
        doctor.setLicenseNumber(request.getLicenseNumber());
        doctor.setYearsOfExperience(request.getYearsOfExperience());
        doctor.setDateOfBirth(request.getDateOfBirth());
        doctor.setDepartment(department);

        // CascadeType.MERGE propagates User changes when Doctor is saved.
        return toResponse(doctorRepository.save(doctor));
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Override
    @CacheEvict(value = "doctors", key = "#id")
    public void deleteDoctor(Long id) {
        Doctor doctor = findOrThrow(id);
        Long userId = doctor.getUser().getId();
        // Delete Doctor first (FK holder), then the linked User account.
        doctorRepository.deleteById(id);
        userRepository.deleteById(userId);
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

    public DoctorResponse toResponse(Doctor d) {
        User user = d.getUser();
        return DoctorResponse.builder()
                .id(d.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(d.getPhone())
                .gender(d.getGender())
                .specialization(d.getSpecialization())
                .licenseNumber(d.getLicenseNumber())
                .yearsOfExperience(d.getYearsOfExperience())
                .dateOfBirth(d.getDateOfBirth())
                .departmentId(d.getDepartment() != null ? d.getDepartment().getId() : null)
                .departmentName(d.getDepartment() != null ? d.getDepartment().getName() : null)
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }
}
