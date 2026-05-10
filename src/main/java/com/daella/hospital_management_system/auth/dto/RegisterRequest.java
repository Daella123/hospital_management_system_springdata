package com.daella.hospital_management_system.auth.dto;

import com.daella.hospital_management_system.enums.Gender;
import com.daella.hospital_management_system.enums.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * Request body for {@code POST /auth/register}.
 *
 * <p>If {@code role} is not provided, the server assigns RECEPTIONIST by default.
 *
 * <p>When {@code role = DOCTOR}, the optional doctor-profile fields below are used
 * to create the linked Doctor entity automatically. A departmentId is strongly
 * recommended; department can be assigned later by an admin if omitted.
 */
@Data
public class RegisterRequest {

    // ── Required for all roles ────────────────────────────────────────────────

    @NotBlank(message = "First name is required")
    @Size(max = 50)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 50)
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    /**
     * Optional role. Defaults to RECEPTIONIST when null.
     */
    private RoleName role;

    // ── Doctor profile fields (only used when role = DOCTOR) ─────────────────

    private String specialization;
    private String licenseNumber;
    private Long   departmentId;
    private String phone;
    private Gender gender;
    private Integer yearsOfExperience;
    private LocalDate dateOfBirth;
}
