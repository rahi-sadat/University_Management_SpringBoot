package com.example.university_management.controller;

import com.example.university_management.dto.LoginRequest;
import com.example.university_management.dto.LoginResponse;
import com.example.university_management.security.AccountUserDetails;
import com.example.university_management.security.JwtAuthenticationFilter;
import com.example.university_management.service.JwtService;
import com.example.university_management.service.TokenRevocationService;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final TokenRevocationService tokenRevocationService;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            TokenRevocationService tokenRevocationService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.tokenRevocationService = tokenRevocationService;
    }


    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {

        var unauthenticatedToken = UsernamePasswordAuthenticationToken.unauthenticated(
                request.getEmail(), request.getPassword());
        var authentication = authenticationManager.authenticate(unauthenticatedToken);


        AccountUserDetails userDetails = (AccountUserDetails) authentication.getPrincipal();
        Long accountId = userDetails.getAccountId();


        String authority = authentication.getAuthorities().iterator().next().getAuthority();
        String role = authority.replace("ROLE_", "");


        String accessToken = jwtService.generateToken(authentication.getName(), accountId);
        long expiresIn = jwtService.getExpiresIn();


        LoginResponse responseBody = new LoginResponse(
                accessToken, expiresIn, role, accountId, authentication.getName());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(responseBody);
    }


    @PostMapping("/logout")
    public Map<String, String> logout(@RequestAttribute(JwtAuthenticationFilter.TOKEN_CLAIMS) Claims claims) {
        tokenRevocationService.revoke(claims);
        return Map.of("message", "Logged out successfully");
    }
}
