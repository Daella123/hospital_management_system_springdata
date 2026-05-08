package com.daella.hospital_management_system.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Runs when an unauthenticated user hits a secured endpoint — returns structured JSON instead
 * of an empty/plain 401 body from Spring Security defaults.
 */
@Component
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String MESSAGE = ""
            + "Authentication is required to access this endpoint. "
            + "Send a valid Bearer JWT in the Authorization header (for example "
            + "\"Authorization: Bearer <your-token>\") after signing in via /auth/login.";

    private final SecurityErrorResponseWriter responseWriter;

    public ApiAuthenticationEntryPoint(SecurityErrorResponseWriter responseWriter) {
        this.responseWriter = responseWriter;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        responseWriter.write(
                response,
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                MESSAGE);
    }
}
