package com.daella.hospital_management_system.security;

import com.daella.hospital_management_system.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * Writes {@link ErrorResponse} JSON for security filter failures so clients get the same
 * shape as {@code GlobalExceptionHandler}.
 */
@Component
public class SecurityErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public SecurityErrorResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletResponse response, HttpStatus status, String error, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ErrorResponse body = ErrorResponse.builder()
                .httpStatus(status.value())
                .error(error)
                .message(message)
                .timestamp(LocalDateTime.now())
                .validationErrors(null)
                .build();

        objectMapper.writeValue(response.getWriter(), body);
    }
}
