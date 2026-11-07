package com.example.university_management.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object (DTO) for creating or updating an account.
 * Used in POST and PUT requests for Admins, Teachers, and Students.
 */
public class CreateAccountRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 255, message = "Full name must be at most 255 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Size(max = 254, message = "Email must be at most 254 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72, message = "Password must contain 8 to 72 characters and at most 72 UTF-8 bytes")
    private String password;

    public CreateAccountRequest() {
    }

    public CreateAccountRequest(String fullName, String email, String password) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
