package com.example.api;

import com.example.model.User;
import com.example.service.UserService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.Map;

public class RegisterHandler extends BaseHandler {

    private final UserService userService;

    public RegisterHandler() {
        this.userService = new UserService();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        logRequest(exchange);

        // Handle CORS preflight
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            handleOptions(exchange);
            return;
        }

        if (!"POST".equals(exchange.getRequestMethod())) {
            sendErrorResponse(exchange, 405, "Method not allowed");
            return;
        }

        try {
            Map<String, String> body = readRequestBody(exchange, Map.class);
            String username = body.get("username");
            String password = body.get("password");

            // Валидация входных данных
            if (username == null || password == null) {
                sendErrorResponse(exchange, 400, "Username and password required");
                return;
            }

            if (username.length() < 3 || username.length() > 50) {
                sendErrorResponse(exchange, 400, "Username must be between 3 and 50 characters");
                return;
            }

            if (!username.matches("^[a-zA-Z0-9_]+$")) {
                sendErrorResponse(exchange, 400, "Username can only contain letters, numbers and underscore");
                return;
            }

            if (password.length() < 6) {
                sendErrorResponse(exchange, 400, "Password must be at least 6 characters");
                return;
            }

            User user = userService.register(username, password, User.Role.USER);

            Map<String, Object> response = Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "role", user.getRole().name()
            );

            logger.info("User registered successfully: {}", username);
            sendSuccessResponse(exchange, response);

        } catch (IllegalArgumentException e) {
            logger.warn("Registration failed: {}", e.getMessage());
            sendErrorResponse(exchange, 409, e.getMessage());
        } catch (IllegalStateException e) {
            logger.warn("Registration failed: {}", e.getMessage());
            sendErrorResponse(exchange, 403, e.getMessage());
        } catch (IOException e) {
            logger.error("Invalid JSON in request", e);
            sendErrorResponse(exchange, 400, "Invalid JSON format");
        } catch (Exception e) {
            logger.error("Unexpected error during registration", e);
            sendErrorResponse(exchange, 500, "Internal server error");
        }
    }
}