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
 * Controller for managing Teacher accounts (/api/teachers).
 * Accessible by ADMIN and TEACHER roles.
 */
@RestController
@RequestMapping("/api/teachers")
public class TeacherController {

    private final AccountService accountService;

    public TeacherController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * GET /api/teachers: List all teacher accounts.
     */
    @GetMapping
    public List<AccountResponse> getAllTeachers() {
        List<UserAccount> accounts = accountService.findAccountsByRole(Role.TEACHER);
        List<AccountResponse> responseList = new ArrayList<>();
        for (UserAccount account : accounts) {
            responseList.add(AccountResponse.from(account));
        }
        return responseList;
    }

    /**
     * GET /api/teachers/{id}: Get a specific teacher account by ID.
     */
    @GetMapping("/{id}")
    public AccountResponse getTeacherById(@PathVariable Long id) {
        UserAccount account = accountService.findAccountByIdAndRole(id, Role.TEACHER);
        return AccountResponse.from(account);
    }

    /**
     * POST /api/teachers: Create a new teacher account (ADMIN only).
     */
    @PostMapping
    public ResponseEntity<AccountResponse> createTeacher(@Valid @RequestBody CreateAccountRequest request) {
        UserAccount created = accountService.createAccount(
                request.getFullName(),
                request.getEmail(),
                request.getPassword(),
                Role.TEACHER);

        AccountResponse response = AccountResponse.from(created);
        URI location = URI.create("/api/teachers/" + created.getId());

        return ResponseEntity.created(location).body(response);
    }

    /**
     * PUT /api/teachers/{id}: Update a teacher account.
     * An ADMIN can update any teacher. A TEACHER can only update their own account.
     */
    @PutMapping("/{id}")
    public AccountResponse updateTeacher(
            @PathVariable Long id,
            @Valid @RequestBody CreateAccountRequest request,
            Authentication authentication) {

        AccountUserDetails currentUser = (AccountUserDetails) authentication.getPrincipal();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        // Non-admin teachers are only allowed to update their own profile
        if (!isAdmin && !currentUser.getAccountId().equals(id)) {
            throw new AccessDeniedException("You do not have permission to update another teacher's account");
        }

        UserAccount updated = accountService.updateAccount(
                id,
                Role.TEACHER,
                request.getFullName(),
                request.getEmail(),
                request.getPassword());

        return AccountResponse.from(updated);
    }

    /**
     * DELETE /api/teachers/{id}: Delete a teacher account (ADMIN only).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTeacher(@PathVariable Long id) {
        accountService.deleteAccount(id, Role.TEACHER);
        return ResponseEntity.noContent().build();
    }
}