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

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:isolated-test.properties")
class AdminCreateStudentIntegrationTest {

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
    private final JsonMapper json = new JsonMapper();

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
    void adminShouldCreateStudentSuccessfully() throws Exception {
        String token = loginAsAdmin();

        String studentBody = json.writeValueAsString(
                Map.of(
                        "fullName", "Test Student",
                        "email", "student@test.local",
                        "password", "Student123!"
                )
        );

        var response = send(
                "POST",
                "/api/students",
                token,
                studentBody
        );

        assertEquals(201, response.statusCode());

        JsonNode result = json.readTree(response.body());

        assertEquals(
                "STUDENT",
                result.get("role").asText()
        );

        long studentId = result.get("id").asLong();

        var savedStudent = repository.findById(studentId);

        assertTrue(savedStudent.isPresent());

        assertEquals(
                "student@test.local",
                savedStudent.get().getEmail()
        );
    }

    private String loginAsAdmin() throws Exception {
        String body = json.writeValueAsString(
                Map.of(
                        "email", ADMIN_EMAIL,
                        "password", PASSWORD
                )
        );

        var response = send(
                "POST",
                "/api/auth/login",
                null,
                body
        );

        assertEquals(200, response.statusCode());

        return json.readTree(response.body())
                .get("accessToken")
                .asText();
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
