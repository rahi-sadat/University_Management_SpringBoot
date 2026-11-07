package com.example.university_management.service;

import com.example.university_management.model.Role;
import com.example.university_management.model.UserAccount;
import com.example.university_management.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Service class for managing university accounts (Admin, Teacher, Student).
 * Handles business validation, email normalization, password hashing with BCrypt,
 * and database operations via UserAccountRepository.
 */
@Service
public class AccountService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    // Dependency injection via constructor
    public AccountService(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Creates a new user account with the specified role.
     */
    public UserAccount createAccount(
            String fullName,
            String email,
            String rawPassword,
            Role role) {

        validateAccountData(fullName, email, rawPassword, role);

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        // Check if an account with this email already exists
        if (userAccountRepository.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An account with this email already exists.");
        }

        UserAccount account = new UserAccount();
        account.setFullName(fullName.trim());
        account.setEmail(normalizedEmail);

        // Hash password with BCrypt before storing. Never store plain-text passwords!
        account.setPassword(passwordEncoder.encode(rawPassword));
        account.setRole(role);

        return userAccountRepository.saveAndFlush(account);
    }

    /**
     * Retrieves all accounts belonging to the given role.
     */
    public List<UserAccount> findAccountsByRole(Role role) {
        return userAccountRepository.findByRole(role);
    }

    /**
     * Retrieves an account by its ID and verifies it has the expected role.
     */
    public UserAccount findAccountByIdAndRole(Long id, Role role) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Account ID must be positive.");
        }

        UserAccount account = userAccountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Account not found."));

        if (account.getRole() != role) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Account not found for this resource type.");
        }

        return account;
    }

    /**
     * Updates an existing account's name, email, and password.
     */
    public UserAccount updateAccount(
            Long id,
            Role role,
            String fullName,
            String email,
            String rawPassword) {

        validateAccountData(fullName, email, rawPassword, role);

        UserAccount account = findAccountByIdAndRole(id, role);

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        Optional<UserAccount> existing = userAccountRepository.findByEmail(normalizedEmail);
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Another account already uses this email.");
        }

        account.setFullName(fullName.trim());
        account.setEmail(normalizedEmail);

        // Hash the new password with BCrypt
        account.setPassword(passwordEncoder.encode(rawPassword));

        return userAccountRepository.saveAndFlush(account);
    }

    /**
     * Deletes an account after verifying its role.
     */
    public void deleteAccount(Long id, Role role) {
        UserAccount account = findAccountByIdAndRole(id, role);
        userAccountRepository.delete(account);
    }

    /**
     * Validates input fields before creating or updating an account.
     */
    private void validateAccountData(
            String fullName,
            String email,
            String rawPassword,
            Role role) {

        if (fullName == null || fullName.isBlank() || fullName.length() > 255) {
            throw new IllegalArgumentException(
                    "Full name is required and must be at most 255 characters.");
        }

        if (email == null || email.length() > 254
                || !email.trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException(
                    "A valid email address of at most 254 characters is required.");
        }

        // BCrypt can only safely hash passwords up to 72 bytes. Passwords longer than 72 bytes are truncated by BCrypt.
        if (rawPassword == null || rawPassword.isBlank() || rawPassword.length() < 8
                || rawPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException(
                    "Password must contain at least 8 characters and at most 72 UTF-8 bytes.");
        }

        if (role == null) {
            throw new IllegalArgumentException("Role is required.");
        }
    }
}
