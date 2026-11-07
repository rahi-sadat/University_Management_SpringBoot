package com.example.university_management.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Helper component to write standard JSON error responses directly to HttpServletResponse.
 * Used by security filters and entry points before requests reach Spring MVC controllers.
 */
@Component
public class ApiErrorWriter {

    /**
     * Writes a JSON error object: {"error": "<message>"} with the given HTTP status code.
     */
    public void write(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // HTTP 401 Unauthorized responses should include the WWW-Authenticate header
        if (status == 401) {
            response.setHeader("WWW-Authenticate", "Bearer");
        }

        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
