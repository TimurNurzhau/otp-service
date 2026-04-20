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

        // Handle CORS preflight
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            handleOptions(exchange);
            return;
        }

        String token = extractToken(exchange);
        if (token == null || !JwtUtil.isValidToken(token)) {
            logger.warn("Unauthorized admin users access attempt");
            sendErrorResponse(exchange, 401, "Unauthorized");
            return;
        }

        if (!JwtUtil.isAdmin(token)) {
            logger.warn("Non-admin user attempted to access admin users");
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

                logger.info("Admin retrieved {} non-admin users", userList.size());
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
                    logger.info("Admin deleted user with ID: {}", userId);
                    sendSuccessMessage(exchange, "User deleted");
                } else {
                    logger.warn("Admin attempted to delete non-existent user ID: {}", userId);
                    sendErrorResponse(exchange, 404, "User not found");
                }

            } else {
                sendErrorResponse(exchange, 405, "Method not allowed");
            }

        } catch (NumberFormatException e) {
            logger.warn("Invalid user ID format: {}", e.getMessage());
            sendErrorResponse(exchange, 400, "Invalid user ID");
        } catch (Exception e) {
            logger.error("Unexpected error in admin users handler", e);
            sendErrorResponse(exchange, 500, "Internal server error");
        }
    }
}