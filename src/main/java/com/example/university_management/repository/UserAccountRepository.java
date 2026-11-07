package com.example.university_management.repository;

import com.example.university_management.model.Role;
import com.example.university_management.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for UserAccount entities.
 * Provides built-in CRUD operations and custom finder methods.
 */
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    // Find account by email for authentication and lookups
    Optional<UserAccount> findByEmail(String email);

    // Check if an email is already registered (used before creating new accounts)
    boolean existsByEmail(String email);

    // Get all accounts belonging to a specific role (ADMIN, TEACHER, or STUDENT)
    List<UserAccount> findByRole(Role role);
}