package com.daella.hospital_management_system.config;

import com.daella.hospital_management_system.security.CustomUserDetailsService;
import com.daella.hospital_management_system.security.JwtAuthenticationFilter;
import com.daella.hospital_management_system.security.OAuth2SuccessHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Central Spring Security configuration.
 *
 * <h3>Session strategy</h3>
 * <p>Stateless — no HTTP session is created or used. Every request must carry a valid JWT.</p>
 *
 * <h3>CSRF</h3>
 * <p>Disabled for this stateless JWT API. CSRF attacks exploit the browser's automatic
 * cookie-sending behaviour; since our clients send tokens manually in the
 * {@code Authorization} header, CSRF is not a threat here.
 * CSRF <em>should</em> be enabled when using server-rendered pages with cookie sessions.</p>
 *
 * <h3>CORS</h3>
 * <p>Configured to allow specific trusted origins only (see {@code app.cors.allowed-origins}).</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity          // enables @PreAuthorize / @Secured on methods
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    @Value("${app.cors.allowed-origins}")
    private String allowedOriginsRaw;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter,
                          CustomUserDetailsService userDetailsService,
                          OAuth2SuccessHandler oAuth2SuccessHandler) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
    }

    // ── Security filter chain ─────────────────────────────────────────────────

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF — stateless JWT API (see class Javadoc for rationale)
            .csrf(AbstractHttpConfigurer::disable)

            // CORS — use the corsConfigurationSource bean below
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Session — stateless; no session is ever created
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Authorization rules
            .authorizeHttpRequests(auth -> auth

                // ── Public endpoints ────────────────────────────────────────
                .requestMatchers(
                        "/auth/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/api-docs/**",
                        "/graphql/**",
                        "/graphiql/**"
                ).permitAll()

                // ── ADMIN-only management ───────────────────────────────────
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                // ── Departments — read: all authenticated; write: ADMIN ─────
                .requestMatchers(HttpMethod.GET,    "/api/v1/departments/**").authenticated()
                .requestMatchers(HttpMethod.POST,   "/api/v1/departments/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/v1/departments/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/departments/**").hasRole("ADMIN")

                // ── Doctors — write: ADMIN; read: ADMIN + DOCTOR ────────────
                .requestMatchers(HttpMethod.GET,    "/api/v1/doctors/**")
                        .hasAnyRole("ADMIN", "DOCTOR", "NURSE", "RECEPTIONIST")
                .requestMatchers(HttpMethod.POST,   "/api/v1/doctors/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/v1/doctors/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/doctors/**").hasRole("ADMIN")

                // ── Patients — write: ADMIN + RECEPTIONIST; read: all ───────
                .requestMatchers(HttpMethod.GET,    "/api/v1/patients/**").authenticated()
                .requestMatchers(HttpMethod.POST,   "/api/v1/patients/**")
                        .hasAnyRole("ADMIN", "RECEPTIONIST")
                .requestMatchers(HttpMethod.PUT,    "/api/v1/patients/**")
                        .hasAnyRole("ADMIN", "RECEPTIONIST")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/patients/**").hasRole("ADMIN")

                // ── Appointments — schedule: RECEPTIONIST + ADMIN; view: all
                .requestMatchers(HttpMethod.GET,    "/api/v1/appointments/**").authenticated()
                .requestMatchers(HttpMethod.POST,   "/api/v1/appointments/**")
                        .hasAnyRole("ADMIN", "RECEPTIONIST")
                .requestMatchers(HttpMethod.PUT,    "/api/v1/appointments/**")
                        .hasAnyRole("ADMIN", "RECEPTIONIST")
                .requestMatchers(HttpMethod.PATCH,  "/api/v1/appointments/**")
                        .hasAnyRole("ADMIN", "RECEPTIONIST", "DOCTOR")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/appointments/**").hasRole("ADMIN")

                // ── Prescriptions — write: DOCTOR + ADMIN; read: all ────────
                .requestMatchers(HttpMethod.GET,    "/api/v1/prescriptions/**").authenticated()
                .requestMatchers(HttpMethod.POST,   "/api/v1/prescriptions/**")
                        .hasAnyRole("ADMIN", "DOCTOR")
                .requestMatchers(HttpMethod.PUT,    "/api/v1/prescriptions/**")
                        .hasAnyRole("ADMIN", "DOCTOR")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/prescriptions/**").hasRole("ADMIN")

                // ── Medical Inventory — write: ADMIN + NURSE; read: all ─────
                .requestMatchers(HttpMethod.GET,    "/api/v1/inventory/**").authenticated()
                .requestMatchers(HttpMethod.POST,   "/api/v1/inventory/**")
                        .hasAnyRole("ADMIN", "NURSE")
                .requestMatchers(HttpMethod.PUT,    "/api/v1/inventory/**")
                        .hasAnyRole("ADMIN", "NURSE")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/inventory/**").hasRole("ADMIN")

                // ── Patient Feedback — all authenticated ─────────────────────
                .requestMatchers("/api/v1/patient-feedbacks/**").authenticated()

                // ── Reports — ADMIN + DOCTOR ─────────────────────────────────
                .requestMatchers("/api/v1/reports/**").hasAnyRole("ADMIN", "DOCTOR")

                // Any other request must be authenticated
                .anyRequest().authenticated()
            )

            // OAuth2 login (Google)
            .oauth2Login(oauth2 -> oauth2
                    .successHandler(oAuth2SuccessHandler))

            // JWT filter before Spring's username/password filter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

            // Auth provider
            .authenticationProvider(authenticationProvider());

        return http.build();
    }

    // ── CORS ─────────────────────────────────────────────────────────────────

    /**
     * CORS explanation:
     * CORS (Cross-Origin Resource Sharing) restricts which external origins can
     * send requests to our API. It is a browser-enforced mechanism — not a server
     * security feature per se. Postman and server-to-server calls ignore CORS.
     *
     * <p>We allow only the origins listed in {@code app.cors.allowed-origins}.</p>
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        List<String> origins = Arrays.asList(allowedOriginsRaw.split(","));
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // ── Beans ─────────────────────────────────────────────────────────────────

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
