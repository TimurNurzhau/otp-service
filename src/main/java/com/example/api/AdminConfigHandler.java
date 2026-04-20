package com.example.api;

import com.example.model.OtpConfig;
import com.example.security.JwtUtil;
import com.example.service.OtpService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.Map;

public class AdminConfigHandler extends BaseHandler {

    private final OtpService otpService;

    public AdminConfigHandler() {
        this.otpService = new OtpService();
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

            if ("GET".equals(method)) {
                OtpConfig config = otpService.getConfig();
                Map<String, Object> response = Map.of(
                        "codeLength", config.getCodeLength(),
                        "ttlSeconds", config.getTtlSeconds()
                );
                sendSuccessResponse(exchange, response);

            } else if ("PUT".equals(method)) {
                Map<String, Integer> body = readRequestBody(exchange, Map.class);
                Integer codeLength = body.get("codeLength");
                Integer ttlSeconds = body.get("ttlSeconds");

                if (codeLength == null || ttlSeconds == null) {
                    sendErrorResponse(exchange, 400, "codeLength and ttlSeconds required");
                    return;
                }

                otpService.updateConfig(codeLength, ttlSeconds);
                sendSuccessMessage(exchange, "Configuration updated");

            } else {
                sendErrorResponse(exchange, 405, "Method not allowed");
            }

        } catch (Exception e) {
            sendErrorResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }
}