package com.example.api;

import com.example.security.JwtUtil;
import com.example.service.OtpService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.Map;

public class OtpGenerateHandler extends BaseHandler {

    private final OtpService otpService;

    public OtpGenerateHandler() {
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
            logger.warn("Unauthorized OTP generation attempt");
            sendErrorResponse(exchange, 401, "Unauthorized");
            return;
        }

        try {
            Long userId = JwtUtil.getUserIdFromToken(token);
            Map<String, String> body = readRequestBody(exchange, Map.class);

            String operationId = body.get("operationId");
            String channel = body.getOrDefault("channel", "file");
            String destination = body.get("destination");

            if (operationId == null || operationId.trim().isEmpty()) {
                sendErrorResponse(exchange, 400, "operationId required");
                return;
            }

            if (operationId.length() > 100) {
                sendErrorResponse(exchange, 400, "operationId too long (max 100 characters)");
                return;
            }

            // Валидация канала
            String[] availableChannels = otpService.getAvailableChannels();
            boolean channelValid = false;
            for (String ch : availableChannels) {
                if (ch.equalsIgnoreCase(channel)) {
                    channelValid = true;
                    break;
                }
            }

            if (!channelValid) {
                sendErrorResponse(exchange, 400, "Invalid channel. Available: " + String.join(", ", availableChannels));
                return;
            }

            if (destination == null || destination.trim().isEmpty()) {
                sendErrorResponse(exchange, 400, "destination required");
                return;
            }

            // Генерируем и отправляем код (метод void, ничего не возвращает)
            otpService.generateAndSendOtp(userId, operationId, channel, destination);

            Map<String, Object> response = Map.of(
                    "operationId", operationId,
                    "channel", channel,
                    "message", "OTP code generated and sent via " + channel
            );

            logger.info("OTP generated for user {}: operationId={}, channel={}, destination={}",
                    userId, operationId, channel, destination);
            sendSuccessResponse(exchange, response);

        } catch (RuntimeException e) {
            logger.error("Failed to send OTP: {}", e.getMessage());
            sendErrorResponse(exchange, 500, e.getMessage());
        } catch (IOException e) {
            logger.error("Invalid JSON in request", e);
            sendErrorResponse(exchange, 400, "Invalid JSON format");
        } catch (Exception e) {
            logger.error("Unexpected error during OTP generation", e);
            sendErrorResponse(exchange, 500, "Internal server error");
        }
    }
}