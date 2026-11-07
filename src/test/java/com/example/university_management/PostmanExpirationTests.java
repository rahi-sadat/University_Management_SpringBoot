package com.example.university_management;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfSystemProperty(named = "postman", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:isolated-test.properties", properties = {
        "app.jwt.expires-in=2",
        "spring.datasource.url=jdbc:h2:mem:expiration_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"})
class PostmanExpirationTests {
    @Value("${local.server.port}") int port;

    @Test
    void expiredSignedTokenIsRejectedOverHttp() throws Exception {
        Path runner = Path.of("target/postman-tools/node_modules/newman/bin/newman.js").toAbsolutePath();
        Process process = new ProcessBuilder("node", runner.toString(), "run",
                "postman/University-Management-System.postman_collection.json", "--folder", "Expiration",
                "--env-var", "baseUrl=http://localhost:" + port,
                "--env-var", "adminEmail=bootstrap@test.local",
                "--env-var", "adminPassword=TestBootstrap!123",
                "--reporters", "cli,json", "--reporter-json-export", "target/newman-expiration-report.json")
                .redirectErrorStream(true).redirectOutput(Path.of("target/newman-expiration.log").toFile()).start();
        boolean completed = process.waitFor(30, TimeUnit.SECONDS);
        if (!completed) process.destroyForcibly();
        assertThat(completed).as("Newman expiration check completed; see target/newman-expiration.log").isTrue();
        assertThat(process.exitValue()).as("Newman expiration result; see target/newman-expiration.log").isZero();
    }
}
