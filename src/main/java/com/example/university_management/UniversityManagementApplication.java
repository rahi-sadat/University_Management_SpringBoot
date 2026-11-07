package com.example.university_management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

@SpringBootApplication
public class UniversityManagementApplication {

    public static void main(String[] args) {
        // Automatically load .env file for local IDE execution (e.g. IntelliJ Run button)
        loadDotEnvIfPresent();

        SpringApplication.run(UniversityManagementApplication.class, args);
    }

    /**
     * Reads local .env file (if present) and sets system properties.
     * This allows clicking "Run" in IntelliJ IDEA without needing to manually
     * configure environment variables inside the IDE Run Configuration.
     */
    private static void loadDotEnvIfPresent() {
        File envFile = new File(".env");
        if (!envFile.exists()) {
            return;
        }

        try {
            List<String> lines = Files.readAllLines(envFile.toPath());
            for (String line : lines) {
                line = line.trim();
                // Skip empty lines or comment lines
                if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                    continue;
                }

                int separatorIndex = line.indexOf('=');
                String key = line.substring(0, separatorIndex).trim();
                String value = line.substring(separatorIndex + 1).trim();

                // Remove surrounding quotes if present
                if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                    value = value.substring(1, value.length() - 1);
                } else if (value.startsWith("'") && value.endsWith("'") && value.length() >= 2) {
                    value = value.substring(1, value.length() - 1);
                }

                // Set as system property if not already configured in environment
                if (System.getProperty(key) == null && System.getenv(key) == null) {
                    System.setProperty(key, value);
                }

                // Also map common .env property aliases to Spring Boot property names
                if ("DB_PASSWORD".equals(key)
                        && System.getProperty("SPRING_DATASOURCE_PASSWORD") == null
                        && System.getenv("SPRING_DATASOURCE_PASSWORD") == null) {
                    System.setProperty("SPRING_DATASOURCE_PASSWORD", value);
                }
                if ("ADMIN_EMAIL".equals(key)
                        && System.getProperty("APP_BOOTSTRAP_ADMIN_EMAIL") == null
                        && System.getenv("APP_BOOTSTRAP_ADMIN_EMAIL") == null) {
                    System.setProperty("APP_BOOTSTRAP_ADMIN_EMAIL", value);
                }
                if ("ADMIN_PASSWORD".equals(key)
                        && System.getProperty("APP_BOOTSTRAP_ADMIN_PASSWORD") == null
                        && System.getenv("APP_BOOTSTRAP_ADMIN_PASSWORD") == null) {
                    System.setProperty("APP_BOOTSTRAP_ADMIN_PASSWORD", value);
                }
            }
        } catch (Exception ignored) {
            // If .env cannot be read, proceed with standard environment variables
        }
    }
}
