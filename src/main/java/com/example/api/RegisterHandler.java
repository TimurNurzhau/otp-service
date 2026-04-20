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

        if (!"POST".equals(exchange.getRequestMethod())) {
            sendErrorResponse(exchange, 405, "Method not allowed");
            return;
        }

        try {
            Map<String, String> body = readRequestBody(exchange, Map.class);
            String username = body.get("username");
            String password = body.get("password");

            if (username == null || password == null) {
                sendErrorResponse(exchange, 400, "Username and password required");
                return;
            }

            User user = userService.register(username, password, User.Role.USER);

            Map<String, Object> response = Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "role", user.getRole().name()
            );

            sendSuccessResponse(exchange, response);

        } catch (IllegalArgumentException e) {
            sendErrorResponse(exchange, 409, e.getMessage());
        } catch (Exception e) {
            sendErrorResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }
}