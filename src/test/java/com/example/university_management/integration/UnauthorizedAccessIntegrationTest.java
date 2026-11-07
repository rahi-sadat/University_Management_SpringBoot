package com.example.university_management.integration;

import com.example.university_management.model.Role;
import com.example.university_management.repository.RevokedTokenRepository;
import com.example.university_management.repository.UserAccountRepository;
import com.example.university_management.service.AccountService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:isolated-test.properties")
class UnauthorizedAccessIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@test.local";
    private static final String PASSWORD = "Coursework!123";

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private AccountService accounts;

    @Autowired
    private UserAccountRepository repository;

    @Autowired
    private RevokedTokenRepository revokedTokens;

    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void setUp() {
        revokedTokens.deleteAll();
        repository.deleteAll();

        accounts.createAccount(
                "Test Admin",
                ADMIN_EMAIL,
                PASSWORD,
                Role.ADMIN
        );
    }

    @Test
    void accessWithoutToken_shouldReturnUnauthorized() throws Exception {
        var response = send(
                "GET",
                "/api/students",
                null,
                null
        );

        assertEquals(401, response.statusCode());
    }

    private HttpResponse<String> send(
            String method,
            String path,
            String token,
            String body
    ) throws Exception {
        var request = HttpRequest.newBuilder(url(path))
                .header("Content-Type", "application/json");

        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }

        request.method(
                method,
                body == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body)
        );

        return client.send(
                request.build(),
                HttpResponse.BodyHandlers.ofString()
        );
    }

    private URI url(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}
