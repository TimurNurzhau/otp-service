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

        if (!"POST".equals(exchange.getRequestMethod())) {
            sendErrorResponse(exchange, 405, "Method not allowed");
            return;
        }

        String token = extractToken(exchange);
        if (token == null || !JwtUtil.isValidToken(token)) {
            sendErrorResponse(exchange, 401, "Unauthorized");
            return;
        }

        try {
            Long userId = JwtUtil.getUserIdFromToken(token);
            Map<String, String> body = readRequestBody(exchange, Map.class);

            String operationId = body.get("operationId");
            String channel = body.getOrDefault("channel", "file");
            String destination = body.get("destination");

            if (operationId == null) {
                sendErrorResponse(exchange, 400, "operationId required");
                return;
            }

            String code = otpService.generateAndSendOtp(userId, operationId, channel, destination);

            Map<String, Object> response = Map.of(
                    "operationId", operationId,
                    "channel", channel,
                    "code", code,
                    "message", "OTP code generated and sent via " + channel
            );

            sendSuccessResponse(exchange, response);

        } catch (Exception e) {
            sendErrorResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }
}