package com.example.university_management.security;

import com.example.university_management.service.CustomUserDetailsService;
import com.example.university_management.service.JwtService;
import com.example.university_management.service.TokenRevocationService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Filter that intercepts incoming HTTP requests once per request.
 * It reads the "Authorization: Bearer <token>" header, validates the JWT,
 * checks database revocation, reloads the current user permissions from the database,
 * and sets up the Spring Security context.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String TOKEN_CLAIMS = "university.jwt.claims";

    private final JwtService jwtService;
    private final TokenRevocationService revocations;
    private final CustomUserDetailsService users;
    private final ApiErrorWriter errors;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            TokenRevocationService revocations,
            CustomUserDetailsService users,
            ApiErrorWriter errors) {
        this.jwtService = jwtService;
        this.revocations = revocations;
        this.users = users;
        this.errors = errors;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getServletPath();

        // 1. Skip filter for public login endpoint
        if ("POST".equals(method) && "/api/auth/login".equals(path)) {
            return true;
        }

        // 2. Skip filter for public static frontend resources
        if ("GET".equals(method)) {
            if ("/".equals(path) || "/index.html".equals(path) || "/styles.css".equals(path)
                    || "/app.js".equals(path) || "/favicon.ico".equals(path)) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        // If no Authorization header is provided, continue to the next filter.
        // Protected endpoints will be caught by Spring Security authorization rules.
        if (header == null) {
            chain.doFilter(request, response);
            return;
        }

        try {
            // Validate header: must have exactly 1 Authorization header, starting with "Bearer "
            boolean hasSingleHeader = Collections.list(request.getHeaders("Authorization")).size() == 1;
            boolean startsWithBearer = header.regionMatches(true, 0, "Bearer ", 0, 7);
            boolean hasTokenContent = header.length() > 7;
            boolean tokenHasNoSpaces = hasTokenContent && !header.substring(7).contains(" ");

            if (!hasSingleHeader || !startsWithBearer || !hasTokenContent || !tokenHasNoSpaces) {
                throw new JwtException("Invalid authorization header format");
            }

            // Extract token string after "Bearer " prefix
            String token = header.substring(7);

            // Step 1: Validate token signature, timestamps, and claims
            Claims claims = jwtService.validateToken(token);

            // Step 2: Check if this token was revoked during logout
            if (revocations.isRevoked(claims.getId())) {
                throw new JwtException("Token has been revoked upon logout");
            }

            // Step 3: Load the user from the database by email
            // Crucial: Authorities/roles are loaded from the database, never trusted from client claims
            AccountUserDetails user = users.loadUserByUsername(claims.getSubject());

            // Step 4: Verify the database account ID matches the token claim
            // Prevents an old token from authenticating a newly created account if an email is reused
            Long tokenAccountId = claims.get("accountId", Long.class);
            if (!user.getAccountId().equals(tokenAccountId)) {
                throw new JwtException("Token account no longer matches current database account");
            }

            // Step 5: Create Spring Security authentication token and store in SecurityContext
            var authentication = UsernamePasswordAuthenticationToken.authenticated(
                    user, null, user.getAuthorities());

            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            // Save claims as a request attribute so logout endpoint can read the token ID
            request.setAttribute(TOKEN_CLAIMS, claims);

        } catch (JwtException | IllegalArgumentException | AuthenticationException exception) {
            SecurityContextHolder.clearContext();
            errors.write(response, 401, "Invalid or expired access token");
            return;
        } catch (DataAccessException exception) {
            SecurityContextHolder.clearContext();
            errors.write(response, 503, "Database temporarily unavailable");
            return;
        }

        // Pass request to the next filter in the chain
        chain.doFilter(request, response);
    }
}
