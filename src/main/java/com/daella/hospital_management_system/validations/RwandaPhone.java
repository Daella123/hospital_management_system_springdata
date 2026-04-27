package com.daella.hospital_management_system.validations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = RwandaPhoneValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RwandaPhone {
    String message() default "Phone number must be a valid Rwandan number (e.g. 0781234567)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
