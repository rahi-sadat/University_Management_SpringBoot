package com.example.university_management.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object (DTO) for receiving user login credentials.
 */
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Size(max = 254, message = "Email must be at most 254 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(max = 72, message = "Password must be at most 72 characters")
    private String password;

    public LoginRequest() {
    }

    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
