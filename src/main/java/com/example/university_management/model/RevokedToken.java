package com.example.university_management.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Entity representing a revoked JWT token.
 * When a user logs out, the unique ID (jti) and expiration time of their token
 * are stored here so the token cannot be reused even if the server restarts.
 */
@Entity
@Table(name = "revoked_tokens")
public class RevokedToken {

    @Id
    @Column(length = 36)
    private String id; // The unique UUID of the token (JWT ID)

    @Column(nullable = false)
    private Instant expiresAt; // When this token expires naturally

    // Default constructor required by JPA
    protected RevokedToken() {
    }

    public RevokedToken(String id, Instant expiresAt) {
        this.id = id;
        this.expiresAt = expiresAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
