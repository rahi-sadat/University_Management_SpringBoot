package com.example.university_management.controller;

import com.example.university_management.dto.AccountResponse;
import com.example.university_management.dto.CreateAccountRequest;
import com.example.university_management.model.Role;
import com.example.university_management.model.UserAccount;
import com.example.university_management.security.AccountUserDetails;
import com.example.university_management.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for managing Student accounts (/api/students).
 * Accessible by ADMIN and STUDENT roles.
 */
@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final AccountService accountService;

    public StudentController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * GET /api/students: List all student accounts.
     */
    @GetMapping
    public List<AccountResponse> getAllStudents() {
        List<UserAccount> accounts = accountService.findAccountsByRole(Role.STUDENT);
        List<AccountResponse> responseList = new ArrayList<>();
        for (UserAccount account : accounts) {
            responseList.add(AccountResponse.from(account));
        }
        return responseList;
    }

    /**
     * GET /api/students/{id}: Get a specific student account by ID.
     */
    @GetMapping("/{id}")
    public AccountResponse getStudentById(@PathVariable Long id) {
        UserAccount account = accountService.findAccountByIdAndRole(id, Role.STUDENT);
        return AccountResponse.from(account);
    }

    /**
     * POST /api/students: Create a new student account (ADMIN only).
     */
    @PostMapping
    public ResponseEntity<AccountResponse> createStudent(@Valid @RequestBody CreateAccountRequest request) {
        UserAccount created = accountService.createAccount(
                request.getFullName(),
                request.getEmail(),
                request.getPassword(),
                Role.STUDENT);

        AccountResponse response = AccountResponse.from(created);
        URI location = URI.create("/api/students/" + created.getId());

        return ResponseEntity.created(location).body(response);
    }

    /**
     * PUT /api/students/{id}: Update a student account.
     * An ADMIN can update any student. A STUDENT can only update their own account.
     */
    @PutMapping("/{id}")
    public AccountResponse updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody CreateAccountRequest request,
            Authentication authentication) {

        AccountUserDetails currentUser = (AccountUserDetails) authentication.getPrincipal();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        // Non-admin students are only allowed to update their own profile
        if (!isAdmin && !currentUser.getAccountId().equals(id)) {
            throw new AccessDeniedException("You do not have permission to update another student's account");
        }

        UserAccount updated = accountService.updateAccount(
                id,
                Role.STUDENT,
                request.getFullName(),
                request.getEmail(),
                request.getPassword());

        return AccountResponse.from(updated);
    }

    /**
     * DELETE /api/students/{id}: Delete a student account (ADMIN only).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        accountService.deleteAccount(id, Role.STUDENT);
        return ResponseEntity.noContent().build();
    }
}