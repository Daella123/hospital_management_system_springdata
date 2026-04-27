package com.daella.hospital_management_system.validations;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RwandaPhoneValidator implements ConstraintValidator<RwandaPhone, String> {
    // Rwanda valid prefixes for all major carriers
    private static final String RWANDA_PHONE_REGEX = "^07(2|3|8|9)\\d{7}$";



    @Override
    public boolean isValid(String phone, ConstraintValidatorContext constraintValidatorContext) {
        if (phone == null || phone.isBlank()) {
            return true; // let @NotBlank handle null/blank if needed
        }
        return phone.matches(RWANDA_PHONE_REGEX);

    }
}
