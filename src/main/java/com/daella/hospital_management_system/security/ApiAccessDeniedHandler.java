package com.daella.hospital_management_system.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Runs when an authenticated user lacks authority for an endpoint (wrong role / method rule /
 * {@code @PreAuthorize} failure surfaced as {@link AccessDeniedException} in some paths).
 */
@Component
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

    private static final String MESSAGE = ""
            + "You are signed in, but your role does not allow this operation. "
            + "Each area of the API is restricted by role (for example ADMIN, DOCTOR, NURSE, "
            + "RECEPTIONIST). Ask an administrator to grant the correct role if you need access.";

    private final SecurityErrorResponseWriter responseWriter;

    public ApiAccessDeniedHandler(SecurityErrorResponseWriter responseWriter) {
        this.responseWriter = responseWriter;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        responseWriter.write(
                response,
                HttpStatus.FORBIDDEN,
                "Forbidden",
                MESSAGE);
    }
}
