package com.example.api;

import com.example.security.JwtUtil;
import com.example.service.OtpService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.Map;

public class OtpValidateHandler extends BaseHandler {

    private final OtpService otpService;

    public OtpValidateHandler() {
        this.otpService = new OtpService();
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

        String token = extractToken(exchange);
        if (token == null || !JwtUtil.isValidToken(token)) {
            logger.warn("Unauthorized OTP validation attempt");
            sendErrorResponse(exchange, 401, "Unauthorized");
            return;
        }

        try {
            Long userId = JwtUtil.getUserIdFromToken(token);
            Map<String, String> body = readRequestBody(exchange, Map.class);

            String operationId = body.get("operationId");
            String code = body.get("code");

            if (operationId == null || code == null) {
                sendErrorResponse(exchange, 400, "operationId and code required");
                logger.warn("Missing required fields: operationId={}, code={}", operationId, code);
                return;
            }

            if (operationId.trim().isEmpty() || code.trim().isEmpty()) {
                sendErrorResponse(exchange, 400, "operationId and code cannot be empty");
                return;
            }

            if (!code.matches("\\d+")) {
                sendErrorResponse(exchange, 400, "Code must contain only digits");
                return;
            }

            boolean valid = otpService.validateOtp(userId, operationId, code);

            Map<String, Object> response = Map.of(
                    "operationId", operationId,
                    "valid", valid,
                    "message", valid ? "Code is valid" : "Invalid or expired code"
            );

            logger.info("OTP validation for user {}: operationId={}, valid={}",
                    userId, operationId, valid);
            sendSuccessResponse(exchange, response);

        } catch (IOException e) {
            logger.error("Invalid JSON in request", e);
            sendErrorResponse(exchange, 400, "Invalid JSON format");
        } catch (Exception e) {
            logger.error("Unexpected error during OTP validation", e);
            sendErrorResponse(exchange, 500, "Internal server error");
        }
    }
}