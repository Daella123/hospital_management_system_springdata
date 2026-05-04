package com.daella.hospital_management_system.auth.dto;

import com.daella.hospital_management_system.enums.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for {@code POST /auth/register}.
 * If {@code role} is not provided, the server assigns RECEPTIONIST by default.
 */
@Data
public class RegisterRequest {

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
     * ADMIN can assign any role; regular self-registration uses the default.
     */
    private RoleName role;
}
