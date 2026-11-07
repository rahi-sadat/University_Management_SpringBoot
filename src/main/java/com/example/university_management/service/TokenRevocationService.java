package com.example.university_management.service;

import com.example.university_management.model.RevokedToken;
import com.example.university_management.repository.RevokedTokenRepository;
import io.jsonwebtoken.Claims;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Service to manage the revocation of JWT tokens upon logout.
 * Revoked token IDs are saved in the database so that logout persists
 * across application restarts.
 */
@Service
public class TokenRevocationService {

    private final RevokedTokenRepository repository;

    public TokenRevocationService(RevokedTokenRepository repository) {
        this.repository = repository;
    }

    /**
     * Checks whether a token ID (jti) is listed as revoked in the database.
     */
    public boolean isRevoked(String id) {
        return repository.existsById(id);
    }

    /**
     * Revokes a token by saving its unique token ID (jti) and expiration time.
     * Also removes older expired tokens from the database to keep the table clean.
     */
    public void revoke(Claims claims) {
        // 1. Clean up old tokens that have already expired naturally
        repository.deleteByExpiresAtBefore(Instant.now());

        String tokenId = claims.getId();

        // 2. If already revoked, nothing more to do
        if (repository.existsById(tokenId)) {
            return;
        }

        // 3. Store the revoked token ID and natural expiration timestamp
        try {
            Instant expiresAt = claims.getExpiration().toInstant();
            repository.saveAndFlush(new RevokedToken(tokenId, expiresAt));
        } catch (DataIntegrityViolationException exception) {
            // If a concurrent logout request already saved this token ID, confirm it is present
            if (!repository.existsById(tokenId)) {
                throw exception;
            }
        }
    }
}
