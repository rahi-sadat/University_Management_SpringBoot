package com.example.university_management.dto;

/**
 * Data Transfer Object (DTO) returned upon successful authentication.
 * Contains the signed JWT token, token type ("Bearer"), expiry seconds, and user role.
 */
public class LoginResponse {

    private final String accessToken;
    private final String tokenType = "Bearer";
    private final long expiresIn;
    private final String role;
    private final Long accountId;
    private final String email;

    public LoginResponse(String accessToken, long expiresIn, String role) {
        this(accessToken, expiresIn, role, null, null);
    }

    public LoginResponse(String accessToken, long expiresIn, String role, Long accountId, String email) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.role = role;
        this.accountId = accountId;
        this.email = email;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public String getRole() {
        return role;
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getEmail() {
        return email;
    }
}
