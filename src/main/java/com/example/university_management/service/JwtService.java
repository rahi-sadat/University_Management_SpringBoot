package com.example.university_management.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Service for generating, signing, and validating JSON Web Tokens (JWT).
 * Uses the HMAC-SHA256 (HS256) cryptographic algorithm.
 */
@Service
public class JwtService {

    private static final String ISSUER = "university-management";
    private final SecretKey signingKey;
    private final long expiresIn;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expires-in}") long expiresIn) {

        // Validate and decode the Base64 signing secret
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secret);
            if (keyBytes.length < 32) {
                throw new IllegalArgumentException("JWT secret key must be at least 32 bytes (256 bits)");
            }
            this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("JWT_SECRET must be Base64 encoding of at least 32 random bytes");
        }

        // Token lifetime must be between 1 second and 24 hours (86400 seconds)
        if (expiresIn < 1 || expiresIn > 86400) {
            throw new IllegalArgumentException("JWT_EXPIRES_IN must be between 1 and 86400 seconds");
        }
        this.expiresIn = expiresIn;
    }

    /**
     * Creates and signs a new JWT token for an authenticated user.
     *
     * @param email     The user's email address (stored in the 'sub' claim)
     * @param accountId The user's database ID (stored in the 'accountId' claim)
     * @return A signed JWT string
     */
    public String generateToken(String email, Long accountId) {
        Instant now = Instant.now();
        Date issuedAt = Date.from(now);
        Date expiration = Date.from(now.plusSeconds(expiresIn));

        return Jwts.builder()
                .issuer(ISSUER)
                .subject(email)
                .id(UUID.randomUUID().toString()) // Unique token ID (jti) for revocation tracking
                .claim("accountId", accountId)    // Binds token to this specific user account
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Verifies the signature of the token and validates all required claims.
     *
     * @param token The JWT string sent in the Authorization header
     * @return The verified Claims payload
     * @throws JwtException if signature verification or claims validation fails
     */
    public Claims validateToken(String token) {
        // 1. Verify token signature and check issuer
        var signed = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(ISSUER)
                .build()
                .parseSignedClaims(token);

        Claims claims = signed.getPayload();

        // 2. Verify signing algorithm is HS256
        if (!"HS256".equals(signed.getHeader().getAlgorithm())) {
            throw new JwtException("Invalid token algorithm");
        }

        // 3. Verify subject (email) is present
        String subject = claims.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new JwtException("Token subject is missing");
        }

        // 4. Verify unique token ID (UUID format, 36 characters)
        String tokenId = claims.getId();
        if (tokenId == null || tokenId.isBlank() || tokenId.length() != 36) {
            throw new JwtException("Token ID is invalid");
        }

        // 5. Verify accountId claim is present and positive
        Long accountId = claims.get("accountId", Long.class);
        if (accountId == null || accountId <= 0) {
            throw new JwtException("Account ID claim is missing or invalid");
        }

        // 6. Verify issuance and expiration dates
        Date issuedAt = claims.getIssuedAt();
        Date expiration = claims.getExpiration();
        if (issuedAt == null || expiration == null) {
            throw new JwtException("Token timestamps are missing");
        }
        if (issuedAt.after(new Date())) {
            throw new JwtException("Token issuance date cannot be in the future");
        }
        if (!expiration.after(issuedAt)) {
            throw new JwtException("Token expiration date must be after issuance date");
        }

        return claims;
    }

    public long getExpiresIn() {
        return expiresIn;
    }
}
