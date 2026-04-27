package com.daella.hospital_management_system.dto.request;

import com.daella.hospital_management_system.enums.BloodType;
import com.daella.hospital_management_system.enums.Gender;
import com.daella.hospital_management_system.validations.RwandaPhone;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientRequest {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid format")
    private String email;

    @RwandaPhone
    private String phone;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    private Gender gender;

    private BloodType bloodType;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    private String emergencyContactName;
    private String emergencyContactPhone;
    private String medicalHistory;
}
