package com.example.university_management.controller;

import com.example.university_management.dto.AccountResponse;
import com.example.university_management.dto.CreateAccountRequest;
import com.example.university_management.model.Role;
import com.example.university_management.model.UserAccount;
import com.example.university_management.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;


@RestController
@RequestMapping("/api/admins")
public class AdminController {

    private final AccountService accountService;

    public AdminController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * GET /api/admins: List all administrator accounts.
     */
    @GetMapping
    public List<AccountResponse> getAllAdmins() {
        List<UserAccount> accounts = accountService.findAccountsByRole(Role.ADMIN);
        List<AccountResponse> responseList = new ArrayList<>();
        for (UserAccount account : accounts) {
            responseList.add(AccountResponse.from(account));
        }
        return responseList;
    }

    /**
     * GET /api/admins/{id}: Get a specific administrator account by ID.
     */
    @GetMapping("/{id}")
    public AccountResponse getAdminById(@PathVariable Long id) {
        UserAccount account = accountService.findAccountByIdAndRole(id, Role.ADMIN);
        return AccountResponse.from(account);
    }

    /**
     * POST /api/admins: Create a new administrator account.
     */
    @PostMapping
    public ResponseEntity<AccountResponse> createAdmin(@Valid @RequestBody CreateAccountRequest request) {
        UserAccount created = accountService.createAccount(
                request.getFullName(),
                request.getEmail(),
                request.getPassword(),
                Role.ADMIN);

        AccountResponse response = AccountResponse.from(created);
        URI location = URI.create("/api/admins/" + created.getId());

        return ResponseEntity.created(location).body(response);
    }

    /**
     * PUT /api/admins/{id}: Update an existing administrator account.
     */
    @PutMapping("/{id}")
    public AccountResponse updateAdmin(
            @PathVariable Long id,
            @Valid @RequestBody CreateAccountRequest request) {
        UserAccount updated = accountService.updateAccount(
                id,
                Role.ADMIN,
                request.getFullName(),
                request.getEmail(),
                request.getPassword());

        return AccountResponse.from(updated);
    }

    /**
     * DELETE /api/admins/{id}: Delete an administrator account.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAdmin(@PathVariable Long id) {
        accountService.deleteAccount(id, Role.ADMIN);
        return ResponseEntity.noContent().build();
    }
}