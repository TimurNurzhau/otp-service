package com.example.api;

import com.example.model.User;
import com.example.security.JwtUtil;
import com.example.service.UserService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AdminUsersHandler extends BaseHandler {

    private final UserService userService;

    public AdminUsersHandler() {
        this.userService = new UserService();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        logRequest(exchange);

        String token = extractToken(exchange);
        if (token == null || !JwtUtil.isValidToken(token)) {
            sendErrorResponse(exchange, 401, "Unauthorized");
            return;
        }

        if (!JwtUtil.isAdmin(token)) {
            sendErrorResponse(exchange, 403, "Forbidden: Admin access required");
            return;
        }

        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if ("GET".equals(method)) {
                List<User> users = userService.findAllNonAdmin();
                List<Map<String, Object>> userList = users.stream()
                        .map(u -> Map.<String, Object>of(
                                "id", u.getId(),
                                "username", u.getUsername(),
                                "role", u.getRole().name()
                        ))
                        .collect(Collectors.toList());

                sendSuccessResponse(exchange, userList);

            } else if ("DELETE".equals(method)) {
                String[] parts = path.split("/");
                if (parts.length < 5) {
                    sendErrorResponse(exchange, 400, "User ID required");
                    return;
                }

                Long userId = Long.parseLong(parts[4]);
                boolean deleted = userService.deleteUser(userId);

                if (deleted) {
                    sendSuccessMessage(exchange, "User deleted");
                } else {
                    sendErrorResponse(exchange, 404, "User not found");
                }

            } else {
                sendErrorResponse(exchange, 405, "Method not allowed");
            }

        } catch (NumberFormatException e) {
            sendErrorResponse(exchange, 400, "Invalid user ID");
        } catch (Exception e) {
            sendErrorResponse(exchange, 500, "Internal server error");
        }
    }
}