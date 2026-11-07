package com.example.university_management.dto;

import com.example.university_management.model.Role;
import com.example.university_management.model.UserAccount;

/**
 * Data Transfer Object (DTO) returned to the client when viewing accounts.
 * Crucially, this class NEVER includes the password hash for security reasons.
 */
public class AccountResponse {

    private Long id;
    private String fullName;
    private String email;
    private Role role;

    public AccountResponse() {
    }

    public AccountResponse(Long id, String fullName, String email, Role role) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
    }

    /**
     * Helper method to map a UserAccount entity to this response DTO.
     */
    public static AccountResponse from(UserAccount account) {
        return new AccountResponse(
                account.getId(),
                account.getFullName(),
                account.getEmail(),
                account.getRole()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}