package com.example.university_management.repository;

import com.example.university_management.model.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

/**
 * Repository for storing and checking revoked JWT tokens.
 */
public interface RevokedTokenRepository extends JpaRepository<RevokedToken, String> {

    /**
     * Deletes all token entries that have already expired.
     * Spring Data JPA automatically derives the SQL delete statement.
     */
    @Transactional
    long deleteByExpiresAtBefore(Instant now);
}
