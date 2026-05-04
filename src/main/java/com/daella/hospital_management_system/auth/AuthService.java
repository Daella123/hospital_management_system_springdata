package com.daella.hospital_management_system.auth;

import com.daella.hospital_management_system.auth.dto.AuthResponse;
import com.daella.hospital_management_system.auth.dto.LoginRequest;
import com.daella.hospital_management_system.auth.dto.RegisterRequest;
import com.daella.hospital_management_system.entity.Role;
import com.daella.hospital_management_system.entity.User;
import com.daella.hospital_management_system.enums.RoleName;
import com.daella.hospital_management_system.exception.DuplicateResourceException;
import com.daella.hospital_management_system.exception.ResourceNotFoundException;
import com.daella.hospital_management_system.logging.SecurityEventLogger;
import com.daella.hospital_management_system.repository.RoleRepository;
import com.daella.hospital_management_system.repository.UserRepository;
import com.daella.hospital_management_system.security.CustomUserDetailsService;
import com.daella.hospital_management_system.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service handling user registration, login, and profile retrieval.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final SecurityEventLogger eventLogger;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager,
                       CustomUserDetailsService userDetailsService,
                       SecurityEventLogger eventLogger) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.eventLogger = eventLogger;
    }

    // ── Register ──────────────────────────────────────────────────────────────

    /**
     * Registers a new local user.
     * Password is hashed with BCrypt before persisting — never stored plain.
     *
     * @throws DuplicateResourceException if the email is already taken
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                    "User with email '" + request.getEmail() + "' already exists");
        }

        // Resolve role — default to RECEPTIONIST for self-registration
        RoleName roleName = request.getRole() != null ? request.getRole() : RoleName.RECEPTIONIST;
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));

        Set<Role> roles = new HashSet<>();
        roles.add(role);

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword())) // BCrypt hash
                .provider("LOCAL")
                .enabled(true)
                .roles(roles)
                .build();

        userRepository.save(user);
        log.info("New user registered: '{}'", user.getEmail());

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);

        return buildAuthResponse(token, user, roles);
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    /**
     * Authenticates a user with email + password and returns a JWT.
     *
     * @throws BadCredentialsException on invalid credentials (handled globally)
     */
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(), request.getPassword()));
        } catch (BadCredentialsException e) {
            eventLogger.logLoginFailed(request.getEmail());
            throw e;
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + request.getEmail()));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);

        eventLogger.logLoginSuccess(user.getEmail());
        return buildAuthResponse(token, user, user.getRoles());
    }

    // ── Current user ──────────────────────────────────────────────────────────

    /**
     * Retrieves the currently authenticated user's profile.
     *
     * @param email extracted from the JWT by the controller
     */
    public AuthResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
        return buildAuthResponse(null, user, user.getRoles());
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private AuthResponse buildAuthResponse(String token, User user, Set<Role> roles) {
        List<String> roleNames = roles.stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toList());

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .roles(roleNames)
                .build();
    }
}
