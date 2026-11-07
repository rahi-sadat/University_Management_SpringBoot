package com.example.university_management.config;

import com.example.university_management.security.ApiErrorWriter;
import com.example.university_management.security.JwtAuthenticationFilter;
import com.example.university_management.service.CustomUserDetailsService;
import com.example.university_management.service.JwtService;
import com.example.university_management.service.TokenRevocationService;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.savedrequest.NullRequestCache;

/**
 * Spring Security configuration class.
 * Configures JWT-based stateless authentication, endpoint permissions,
 * and custom error responses.
 */
@Configuration
public class SecurityConfig {

    /**
     * Configures the AuthenticationManager using DaoAuthenticationProvider
     * to check user credentials against the database with BCrypt password hashing.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            CustomUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    /**
     * Defines security filter chain and authorization rules.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtService jwtService,
            TokenRevocationService revocations,
            CustomUserDetailsService userDetailsService,
            ApiErrorWriter errorWriter) throws Exception {

        // Instantiate our custom JWT filter
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(
                jwtService, revocations, userDetailsService, errorWriter);

        http
                // Disable CSRF because we use stateless Bearer tokens in headers, not session cookies
                .csrf(csrf -> csrf.disable())

                // Disable default form login and basic authentication
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())

                // Stateless session management: do not create or use HTTP sessions
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .securityContext(context -> context.securityContextRepository(new NullSecurityContextRepository()))
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))

                // URL authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Allow Spring Boot error dispatching
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()

                        // Public endpoints
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers("/", "/index.html", "/styles.css", "/app.js", "/favicon.ico").permitAll()

                        // Role-based access control rules
                        .requestMatchers("/api/admins", "/api/admins/**").hasRole("ADMIN")

                        // Teachers: View and Update allowed for ADMIN and TEACHER; Create and Delete are ADMIN only
                        .requestMatchers(HttpMethod.GET, "/api/teachers", "/api/teachers/**").hasAnyRole("ADMIN", "TEACHER")
                        .requestMatchers(HttpMethod.PUT, "/api/teachers/**").hasAnyRole("ADMIN", "TEACHER")
                        .requestMatchers(HttpMethod.POST, "/api/teachers", "/api/teachers/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/teachers/**").hasRole("ADMIN")

                        // Students: View and Update allowed for ADMIN and STUDENT; Create and Delete are ADMIN only
                        .requestMatchers(HttpMethod.GET, "/api/students", "/api/students/**").hasAnyRole("ADMIN", "STUDENT")
                        .requestMatchers(HttpMethod.PUT, "/api/students/**").hasAnyRole("ADMIN", "STUDENT")
                        .requestMatchers(HttpMethod.POST, "/api/students", "/api/students/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/students/**").hasRole("ADMIN")

                        // All other requests require authentication
                        .anyRequest().authenticated())

                // Custom exception handlers for 401 Unauthorized and 403 Forbidden
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                errorWriter.write(response, 401, "Authentication required"))
                        .accessDeniedHandler((request, response, exception) ->
                                errorWriter.write(response, 403, "You do not have permission to access this resource")))

                // Register our custom JWT filter before the standard authentication filter
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
