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

        // Handle CORS preflight
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            handleOptions(exchange);
            return;
        }

        String token = extractToken(exchange);
        if (token == null || !JwtUtil.isValidToken(token)) {
            logger.warn("Unauthorized admin config access attempt");
            sendErrorResponse(exchange, 401, "Unauthorized");
            return;
        }

        if (!JwtUtil.isAdmin(token)) {
            logger.warn("Non-admin user attempted to access admin config");
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
                logger.info("Admin retrieved OTP config: codeLength={}, ttlSeconds={}",
                        config.getCodeLength(), config.getTtlSeconds());
                sendSuccessResponse(exchange, response);

            } else if ("PUT".equals(method)) {
                Map<String, Integer> body = readRequestBody(exchange, Map.class);
                Integer codeLength = body.get("codeLength");
                Integer ttlSeconds = body.get("ttlSeconds");

                if (codeLength == null || ttlSeconds == null) {
                    sendErrorResponse(exchange, 400, "codeLength and ttlSeconds required");
                    return;
                }

                // Валидация
                if (codeLength < 4 || codeLength > 10) {
                    sendErrorResponse(exchange, 400, "codeLength must be between 4 and 10");
                    return;
                }

                if (ttlSeconds < 30 || ttlSeconds > 3600) {
                    sendErrorResponse(exchange, 400, "ttlSeconds must be between 30 and 3600");
                    return;
                }

                otpService.updateConfig(codeLength, ttlSeconds);
                logger.info("Admin updated OTP config: codeLength={}, ttlSeconds={}",
                        codeLength, ttlSeconds);
                sendSuccessMessage(exchange, "Configuration updated");

            } else {
                sendErrorResponse(exchange, 405, "Method not allowed");
            }

        } catch (IOException e) {
            logger.error("Invalid JSON in request", e);
            sendErrorResponse(exchange, 400, "Invalid JSON format");
        } catch (Exception e) {
            logger.error("Unexpected error in admin config handler", e);
            sendErrorResponse(exchange, 500, "Internal server error");
        }
    }
}