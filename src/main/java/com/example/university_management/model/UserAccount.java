package com.example.university_management.model;

import jakarta.persistence.*;

/**
 * Entity representing a university user account.
 * All roles (Admin, Teacher, Student) share this single table.
 */
@Entity
@Table(name = "user_accounts")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    // Stored as a BCrypt hash, never plain text
    @Column(nullable = false)
    private String password;

    // Stored as String (ADMIN, TEACHER, or STUDENT)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // Required by JPA
    public UserAccount() {
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}