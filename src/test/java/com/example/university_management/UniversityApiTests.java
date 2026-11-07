package com.example.university_management;

import com.example.university_management.config.AdminAccountInitializer;
import com.example.university_management.model.Role;
import com.example.university_management.model.UserAccount;
import com.example.university_management.repository.RevokedTokenRepository;
import com.example.university_management.repository.UserAccountRepository;
import com.example.university_management.service.AccountService;
import com.example.university_management.service.JwtService;
import com.example.university_management.service.TokenRevocationService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:isolated-test.properties")
class UniversityApiTests {
    private static final String PASSWORD = "Coursework!123";
    private static final String TEST_KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    @Value("${local.server.port}") int port;
    @Autowired AccountService accounts;
    @Autowired UserAccountRepository repository;
    @Autowired RevokedTokenRepository revokedTokens;
    @Autowired JwtService jwt;
    @Autowired PasswordEncoder encoder;
    @Autowired AdminAccountInitializer initializer;
    private final JsonMapper json = new JsonMapper();
    private final CookieManager cookies = new CookieManager();
    private final HttpClient client = HttpClient.newBuilder().cookieHandler(cookies).build();

    @BeforeEach
    void seedIsolatedDatabase() {
        revokedTokens.deleteAll();
        repository.deleteAll();
        for (Role role : Role.values()) {
            accounts.createAccount(role.name(), email(role), PASSWORD, role);
        }
    }

    @Test
    void jsonLoginUsesBcryptAndIssuesUniqueSafeTokens() throws Exception {
        var response = loginResponse(Role.ADMIN);
        check(response, 200);
        JsonNode body = json.readTree(response.body());
        assertThat(body.get("tokenType").asText()).isEqualTo("Bearer");
        assertThat(body.get("expiresIn").asLong()).isEqualTo(3600);
        assertThat(body.get("role").asText()).isEqualTo("ADMIN");
        assertThat(response.headers().firstValue("Cache-Control").orElse("")).contains("no-store");
        String token = body.get("accessToken").asText();
        var claims = jwt.validateToken(token);
        assertThat(claims.getSubject()).isEqualTo(email(Role.ADMIN));
        assertThat(claims.getId()).isNotBlank();
        assertThat(claims).doesNotContainKeys("password", "role");
        assertThat(jwt.validateToken(login(Role.ADMIN)).getId()).isNotEqualTo(claims.getId());
        assertThat(encoder.matches(PASSWORD, repository.findByEmail(email(Role.ADMIN)).orElseThrow().getPassword())).isTrue();
    }

    @Test
    void incorrectAndUnknownCredentialsReturnSafe401() throws Exception {
        for (String email : new String[] {email(Role.ADMIN), "unknown@test.local"}) {
            var response = send("POST", "/api/auth/login", null,
                    json.writeValueAsString(Map.of("email", email, "password", "Incorrect!123")));
            check(response, 401);
            assertThat(response.body()).isEqualTo("{\"error\":\"Invalid email or password\"}");
        }
        check(send("POST", "/api/auth/login", null, "{\"email\":\"invalid\",\"password\":\"\"}"), 400);
        check(send("POST", "/api/auth/login", null, "{"), 400);
    }

    @Test
    void noSessionCookieOrBasicCredentialsCanAuthenticate() throws Exception {
        String token = login(Role.ADMIN);
        check(send("GET", "/api/students", token, null), 200);
        check(send("GET", "/api/students", null, null), 401);
        assertThat(cookies.getCookieStore().getCookies()).isEmpty();
        var cookieRequest = HttpRequest.newBuilder(url("/api/students"))
                .header("Cookie", "JSESSIONID=old-session; accessToken=" + token).GET().build();
        check(client.send(cookieRequest, HttpResponse.BodyHandlers.ofString()), 401);
        check(withHeader("Basic YWRtaW46cGFzc3dvcmQ="), 401);
        check(send("POST", "/api/auth/logout", null, null), 401);
        check(send("GET", "/api/auth/csrf", token, null), 404);
    }

    @ParameterizedTest
    @CsvSource({"ADMIN,admins", "ADMIN,teachers", "ADMIN,students"})
    void adminHasFullCrudOnAllResources(Role actor, String resource) throws Exception {
        String token = login(actor);
        String path = "/api/" + resource;
        var seeded = repository.findByRole(resourceRole(resource)).getFirst();
        check(send("GET", path, token, null), 200);
        var created = send("POST", path, token, accountBody("New Person", "new@test.local"));
        check(created, 201);
        long id = json.readTree(created.body()).get("id").asLong();
        String location = path + "/" + id;
        assertThat(created.headers().firstValue("Location")).contains(location);
        assertThat(json.readTree(created.body()).get("role").asText()).isEqualTo(resourceRole(resource).name());
        check(send("GET", location, token, null), 200);
        var updated = send("PUT", location, token, accountBody("Updated Person", "updated@test.local"));
        check(updated, 200);
        assertThat(json.readTree(updated.body()).get("fullName").asText()).isEqualTo("Updated Person");
        check(send("DELETE", location, token, null), 204);
        check(send("GET", location, token, null), 404);
    }

    @Test
    void teachersAndStudentsCanViewCategoryAndEditOwnProfileOnly() throws Exception {
        String teacherToken = login(Role.TEACHER);
        String studentToken = login(Role.STUDENT);
        var teacher = repository.findByRole(Role.TEACHER).getFirst();
        var student = repository.findByRole(Role.STUDENT).getFirst();

        // Seed a second teacher and second student to test cross-account unauthorized editing
        var otherTeacher = accounts.createAccount("Other Teacher", "other-teacher@test.local", PASSWORD, Role.TEACHER);
        var otherStudent = accounts.createAccount("Other Student", "other-student@test.local", PASSWORD, Role.STUDENT);

        // TEACHER can view teachers, can update OWN profile, but CANNOT update other teachers, create, delete, or access admins/students
        check(send("GET", "/api/teachers", teacherToken, null), 200);
        check(send("GET", "/api/teachers/" + teacher.getId(), teacherToken, null), 200);
        check(send("PUT", "/api/teachers/" + teacher.getId(), teacherToken, accountBody("Updated Teacher", email(Role.TEACHER))), 200);
        check(send("PUT", "/api/teachers/" + otherTeacher.getId(), teacherToken, accountBody("Hacked Teacher", "hacked-teacher@test.local")), 403);
        check(send("POST", "/api/teachers", teacherToken, accountBody("Blocked", "blocked@test.local")), 403);
        check(send("DELETE", "/api/teachers/" + teacher.getId(), teacherToken, null), 403);
        check(send("GET", "/api/admins", teacherToken, null), 403);
        check(send("GET", "/api/students", teacherToken, null), 403);

        // STUDENT can view students, can update OWN profile, but CANNOT update other students, create, delete, or access admins/teachers
        check(send("GET", "/api/students", studentToken, null), 200);
        check(send("GET", "/api/students/" + student.getId(), studentToken, null), 200);
        check(send("PUT", "/api/students/" + student.getId(), studentToken, accountBody("Updated Student", email(Role.STUDENT))), 200);
        check(send("PUT", "/api/students/" + otherStudent.getId(), studentToken, accountBody("Hacked Student", "hacked-student@test.local")), 403);
        check(send("POST", "/api/students", studentToken, accountBody("Blocked", "blocked@test.local")), 403);
        check(send("DELETE", "/api/students/" + student.getId(), studentToken, null), 403);
        check(send("GET", "/api/admins", studentToken, null), 403);
        check(send("GET", "/api/teachers", studentToken, null), 403);
    }

    @Test
    void malformedTamperedExpiredAndIncompleteTokensFail() throws Exception {
        for (String header : new String[] {"Bearer", "Bearer ", "Bearer invalid", "Token abc", "Bearer a b"}) {
            check(withHeader(header), 401);
        }
        String token = login(Role.ADMIN);
        int signatureStart = token.lastIndexOf('.') + 1;
        char replacement = token.charAt(signatureStart) == 'a' ? 'b' : 'a';
        String tampered = token.substring(0, signatureStart) + replacement + token.substring(signatureStart + 1);
        check(send("GET", "/api/students", tampered, null), 401);
        check(send("GET", "/api/students", signed(email(Role.ADMIN), Instant.now().minusSeconds(5), true), null), 401);
        check(send("GET", "/api/students", signed(email(Role.ADMIN), Instant.now().plusSeconds(100), false), null), 401);
        String wrongKey = Jwts.builder().subject(email(Role.ADMIN)).signWith(Jwts.SIG.HS256.key().build()).compact();
        check(send("GET", "/api/students", wrongKey, null), 401);
    }

    @Test
    void logoutPersistsRevocationAndOnlyRevokesThePresentedToken() throws Exception {
        String first = login(Role.ADMIN);
        String second = login(Role.ADMIN);
        check(send("POST", "/api/auth/logout", first, null), 200);
        String id = jwt.validateToken(first).getId();
        assertThat(revokedTokens.findById(id)).isPresent();
        // A new service instance still consults the persisted database entry.
        assertThat(new TokenRevocationService(revokedTokens).isRevoked(id)).isTrue();
        check(send("GET", "/api/admins", first, null), 401);
        check(send("POST", "/api/auth/logout", first, null), 401);
        assertThat(revokedTokens.count()).isEqualTo(1);
        check(send("GET", "/api/admins", second, null), 200);
    }

    @Test
    void databaseRoleOverridesTokenClaimsAndDeletedAccountsCannotAuthenticate() throws Exception {
        String forgedRole = signed(email(Role.TEACHER), Instant.now().plusSeconds(100), true);
        check(send("GET", "/api/admins", forgedRole, null), 403);
        check(send("GET", "/api/teachers", forgedRole, null), 200);
        UserAccount teacher = repository.findByEmail(email(Role.TEACHER)).orElseThrow();
        teacher.setRole(Role.STUDENT);
        repository.saveAndFlush(teacher);
        check(send("GET", "/api/teachers", forgedRole, null), 403);
        check(send("GET", "/api/students", forgedRole, null), 200);
        repository.deleteById(teacher.getId());
        check(send("GET", "/api/students", forgedRole, null), 401);
        accounts.createAccount("Reused Email", email(Role.TEACHER), PASSWORD, Role.ADMIN);
        check(send("GET", "/api/admins", forgedRole, null), 401);
    }

    @Test
    void duplicateValidationIdsAndWrongResourceTypesHaveCorrectStatuses() throws Exception {
        String admin = login(Role.ADMIN);
        long teacherId = repository.findByRole(Role.TEACHER).getFirst().getId();
        check(send("POST", "/api/students", admin, accountBody("Duplicate", "ADMIN@TEST.LOCAL")), 409);
        check(send("PUT", "/api/teachers/" + teacherId, admin, accountBody("Duplicate", email(Role.ADMIN))), 409);
        for (String id : new String[] {"0", "-1", "abc", "99999999999999999999999"}) {
            check(send("GET", "/api/students/" + id, admin, null), 400);
        }
        for (String method : new String[] {"GET", "PUT", "DELETE"}) {
            String body = "PUT".equals(method) ? accountBody("Wrong Type", "wrong@test.local") : null;
            check(send(method, "/api/students/" + teacherId, admin, body), 404);
            check(send(method, "/api/students/9223372036854775807", admin, body), 404);
        }
        for (String body : new String[] {"{", "{}", accountBody("", "ok@test.local"),
                accountBody("Name", "invalid"), accountBody("x".repeat(256), "ok@test.local"),
                json.writeValueAsString(Map.of("fullName", "Name", "email", "ok@test.local", "password", "short")),
                json.writeValueAsString(Map.of("fullName", "Name", "email", "ok@test.local", "password", "あ".repeat(30)))}) {
            check(send("POST", "/api/students", admin, body), 400);
        }
        String withRole = accountBody("Escalation", "escalate@test.local").replace("}", ",\"role\":\"ADMIN\"}");
        check(send("POST", "/api/teachers", login(Role.ADMIN), withRole), 400);
    }

    @Test
    void accountServiceHashesWithDistinctSaltsAndScopesUpdatesAndDeletes() {
        var first = accounts.createAccount("First", " FIRST@test.local ", PASSWORD, Role.STUDENT);
        var second = accounts.createAccount("Second", "second@test.local", PASSWORD, Role.STUDENT);
        assertThat(first.getEmail()).isEqualTo("first@test.local");
        assertThat(first.getPassword()).isNotEqualTo(PASSWORD).isNotEqualTo(second.getPassword());
        assertThat(encoder.matches(PASSWORD, first.getPassword())).isTrue();
        var updated = accounts.updateAccount(first.getId(), Role.STUDENT, "Updated", first.getEmail(), "NewPassword!123");
        assertThat(encoder.matches("NewPassword!123", updated.getPassword())).isTrue();
        assertThatThrownBy(() -> accounts.createAccount("Duplicate", first.getEmail(), PASSWORD, Role.ADMIN))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        assertThatThrownBy(() -> accounts.deleteAccount(first.getId(), Role.TEACHER)).isInstanceOf(ResponseStatusException.class);
        assertThat(repository.existsById(first.getId())).isTrue();
        accounts.deleteAccount(first.getId(), Role.STUDENT);
        assertThat(repository.existsById(first.getId())).isFalse();
        assertThatThrownBy(() -> accounts.createAccount("Name", "bad-email", PASSWORD, Role.STUDENT)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void bootstrapIsCreatedOnceAndExistingPasswordIsPreserved() throws Exception {
        initializer.run();
        String hash = repository.findByEmail("bootstrap@test.local").orElseThrow().getPassword();
        initializer.run();
        assertThat(repository.findByEmail("bootstrap@test.local").orElseThrow().getPassword()).isEqualTo(hash);
        assertThat(encoder.matches("TestBootstrap!123", hash)).isTrue();
    }

    @Test
    void signingConfigurationFailsSafelyForWeakSecretsOrInvalidLifetime() {
        assertThatThrownBy(() -> new JwtService("invalid", 3600)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JwtService(TEST_KEY, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JwtService(TEST_KEY, 86401)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void staticFrontendIsPubliclyAccessible() throws Exception {
        var response = client.send(
                HttpRequest.newBuilder(url("/index.html")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("University Management System");
    }

    private String signed(String subject, Instant expiration, boolean requiredClaims) {
        var builder = Jwts.builder().issuer("university-management").subject(subject).claim("role", "ADMIN")
                .expiration(Date.from(expiration));
        if (requiredClaims) {
            builder.id(UUID.randomUUID().toString()).issuedAt(Date.from(Instant.now().minusSeconds(60)));
            builder.claim("accountId", repository.findByEmail(subject).orElseThrow().getId());
        }
        return builder.signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_KEY)), Jwts.SIG.HS256).compact();
    }

    private HttpResponse<String> loginResponse(Role role) throws Exception {
        return send("POST", "/api/auth/login", null,
                json.writeValueAsString(Map.of("email", email(role), "password", PASSWORD)));
    }
    private String login(Role role) throws Exception {
        var response = loginResponse(role);
        check(response, 200);
        return json.readTree(response.body()).get("accessToken").asText();
    }
    private String accountBody(String name, String email) {
        return json.writeValueAsString(Map.of("fullName", name, "email", email, "password", PASSWORD));
    }
    private HttpResponse<String> send(String method, String path, String token, String body) throws Exception {
        var request = HttpRequest.newBuilder(url(path)).header("Content-Type", "application/json");
        if (token != null) request.header("Authorization", "Bearer " + token);
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
    private HttpResponse<String> withHeader(String header) throws Exception {
        return client.send(HttpRequest.newBuilder(url("/api/students")).header("Authorization", header).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }
    private URI url(String path) { return URI.create("http://localhost:" + port + path); }
    private String email(Role role) { return role.name().toLowerCase(java.util.Locale.ROOT) + "@test.local"; }
    private Role resourceRole(String resource) {
        return switch (resource) { case "admins" -> Role.ADMIN; case "teachers" -> Role.TEACHER; default -> Role.STUDENT; };
    }
    private void check(HttpResponse<String> response, int expected) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(expected);
        assertThat(response.headers().allValues("Set-Cookie")).isEmpty();
        assertThat(response.body()).doesNotContain("\"password\"", "$2a$", "$2b$", "stackTrace", "jdbc:");
        if (expected >= 400) {
            assertThat(response.headers().firstValue("Content-Type").orElse("")).contains("application/json");
            assertThat(json.readTree(response.body()).has("error")).isTrue();
        }
        if (expected == 204) assertThat(response.body()).isEmpty();
    }
}
