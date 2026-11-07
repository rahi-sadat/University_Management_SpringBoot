package com.example.university_management.config;

import com.example.university_management.model.Role;
import com.example.university_management.model.UserAccount;
import com.example.university_management.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Initializes the default system administrator account upon application startup.
 * Implements CommandLineRunner, which Spring Boot executes after startup.
 */
@Component
public class AdminAccountInitializer implements CommandLineRunner {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap-admin.email}")
    private String adminEmail;

    @Value("${app.bootstrap-admin.password}")
    private String adminPassword;

    public AdminAccountInitializer(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        String email = adminEmail.trim().toLowerCase(Locale.ROOT);

        // Check if an account with this email already exists
        if (userAccountRepository.existsByEmail(email)) {
            UserAccount existingAccount = userAccountRepository.findByEmail(email).orElseThrow();
            if (existingAccount.getRole() != Role.ADMIN) {
                throw new IllegalStateException("Bootstrap email belongs to a non-administrator account");
            }
            System.out.println("Administrator already exists.");
            return;
        }

        // Validate the bootstrap password
        if (adminPassword == null || adminPassword.isBlank() || adminPassword.length() < 8
                || adminPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("Set APP_BOOTSTRAP_ADMIN_PASSWORD to 8 or more characters, at most 72 UTF-8 bytes");
        }

        // Validate the bootstrap email
        if (email.length() > 254 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalStateException("Set a valid APP_BOOTSTRAP_ADMIN_EMAIL");
        }

        // Create the initial administrator
        UserAccount admin = new UserAccount();
        admin.setFullName("System Administrator");
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);

        userAccountRepository.save(admin);
        System.out.println("Initial administrator created successfully.");
    }
}
