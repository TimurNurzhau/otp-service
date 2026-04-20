package com.example.api;

import com.example.model.User;
import com.example.security.JwtUtil;
import com.example.service.UserService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.Map;

public class LoginHandler extends BaseHandler {

    private final UserService userService;

    public LoginHandler() {
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

            // Валидация
            if (username == null || password == null) {
                sendErrorResponse(exchange, 400, "Username and password required");
                return;
            }

            User user = userService.login(username, password);
            String token = JwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole().name());

            Map<String, Object> response = Map.of(
                    "token", token,
                    "username", user.getUsername(),
                    "role", user.getRole().name()
            );

            logger.info("User logged in successfully: {}", username);
            sendSuccessResponse(exchange, response);

        } catch (IllegalArgumentException e) {
            logger.warn("Login failed for user: {}", e.getMessage());
            sendErrorResponse(exchange, 401, e.getMessage());
        } catch (IOException e) {
            logger.error("Invalid JSON in request", e);
            sendErrorResponse(exchange, 400, "Invalid JSON format");
        } catch (Exception e) {
            logger.error("Unexpected error during login", e);
            sendErrorResponse(exchange, 500, "Internal server error");
        }
    }
}