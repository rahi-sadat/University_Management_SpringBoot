package com.example.university_management;

import com.example.university_management.model.Role;
import com.example.university_management.repository.UserAccountRepository;
import com.example.university_management.service.AccountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfSystemProperty(named = "postman", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:isolated-test.properties")
class PostmanCollectionTests {
    @Value("${local.server.port}") int port;
    @Autowired AccountService accounts;
    @Autowired UserAccountRepository repository;

    @Test
    void runCollectionAgainstRealHttpServer() throws Exception {
        String email = "postman-runner@test.local";
        if (!repository.existsByEmail(email)) accounts.createAccount("Postman Runner", email, "RunnerPassword!123", Role.ADMIN);
        Path runner = Path.of("target/postman-tools/node_modules/newman/bin/newman.js").toAbsolutePath();
        Process process = new ProcessBuilder("node", runner.toString(), "run",
                "postman/University-Management-System.postman_collection.json",
                "--env-var", "baseUrl=http://localhost:" + port,
                "--env-var", "adminEmail=" + email,
                "--env-var", "adminPassword=RunnerPassword!123",
                "--reporters", "cli,json", "--reporter-json-export", "target/newman-report.json")
                .redirectErrorStream(true).redirectOutput(Path.of("target/newman.log").toFile()).start();
        boolean completed = process.waitFor(180, TimeUnit.SECONDS);
        if (!completed) process.destroyForcibly();
        assertThat(completed).as("Newman completed within 180 seconds; see target/newman.log").isTrue();
        assertThat(process.exitValue()).as("Newman result; see target/newman.log").isZero();
    }
}
