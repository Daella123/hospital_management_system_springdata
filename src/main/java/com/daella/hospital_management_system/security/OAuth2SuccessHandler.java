package com.daella.hospital_management_system.security;

import com.daella.hospital_management_system.entity.Role;
import com.daella.hospital_management_system.entity.User;
import com.daella.hospital_management_system.enums.RoleName;
import com.daella.hospital_management_system.repository.RoleRepository;
import com.daella.hospital_management_system.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Handles successful Google OAuth2 logins.
 *
 * <p>After Google redirects back with user info, this handler:
 * <ol>
 *   <li>Extracts the Google user's email and name</li>
 *   <li>Creates a new {@link User} if they don't exist yet, or reuses the existing one</li>
 *   <li>Issues a JWT token</li>
 *   <li>Redirects to the frontend with the token as a query param</li>
 * </ol>
 */
@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2SuccessHandler.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public OAuth2SuccessHandler(UserRepository userRepository,
                                RoleRepository roleRepository,
                                JwtService jwtService,
                                CustomUserDetailsService userDetailsService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email      = oAuth2User.getAttribute("email");
        String name       = oAuth2User.getAttribute("name");
        String providerId = oAuth2User.getAttribute("sub"); // Google subject ID

        if (email == null) {
            log.error("OAuth2 login failed: email not returned by Google");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email not available from Google");
            return;
        }

        // ── Find or create user ──────────────────────────────────────────────
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> createOAuth2User(email, name, providerId));

        // ── Issue JWT ────────────────────────────────────────────────────────
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        String jwt = jwtService.generateToken(userDetails);

        log.info("OAuth2 login successful for user '{}'", email);

        // Redirect frontend with token (adjust URL to match your frontend)
        String redirectUrl = "http://localhost:3000/oauth2/callback?token=" + jwt;
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private User createOAuth2User(String email, String fullName, String providerId) {
        String firstName = fullName != null && fullName.contains(" ")
                ? fullName.substring(0, fullName.lastIndexOf(' '))
                : (fullName != null ? fullName : "OAuth2");
        String lastName = fullName != null && fullName.contains(" ")
                ? fullName.substring(fullName.lastIndexOf(' ') + 1)
                : "User";

        Role receptionistRole = roleRepository.findByName(RoleName.RECEPTIONIST)
                .orElseThrow(() -> new IllegalStateException("RECEPTIONIST role not found in DB"));

        Set<Role> roles = new HashSet<>();
        roles.add(receptionistRole);

        User newUser = User.builder()
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .provider("GOOGLE")
                .providerId(providerId)
                .enabled(true)
                .roles(roles)
                .build();

        log.info("Creating new OAuth2 user for email '{}'", email);
        return userRepository.save(newUser);
    }
}
