package com.example.api;

import com.example.config.EnvConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Базовый класс для всех HTTP-обработчиков.
 */
public abstract class BaseHandler implements HttpHandler {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final ObjectMapper objectMapper = new ObjectMapper();

    // ThreadLocal для хранения времени начала запроса
    private static final ThreadLocal<Long> requestStartTime = new ThreadLocal<>();

    // Константа для CORS (читается из env)
    private static final String ALLOWED_ORIGIN = EnvConfig.getOrDefault("CORS_ALLOWED_ORIGIN", "http://localhost:3000");

    protected void sendJsonResponse(HttpExchange exchange, int statusCode, Object response) throws IOException {
        String jsonResponse = objectMapper.writeValueAsString(response);
        byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }

        logger.debug("Response: status={}, body={}", statusCode, jsonResponse);
    }

    protected void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        logger.warn("Error response: {} {} - {} (status={})",
                exchange.getRequestMethod(),
                exchange.getRequestURI().getPath(),
                message,
                statusCode);
        sendJsonResponse(exchange, statusCode, error);
        logResponse(exchange, statusCode);
    }

    protected void sendSuccessResponse(HttpExchange exchange, Object data) throws IOException {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        logger.info("Success response: {} {} - data sent",
                exchange.getRequestMethod(),
                exchange.getRequestURI().getPath());
        sendJsonResponse(exchange, 200, response);
        logResponse(exchange, 200);
    }

    protected void sendSuccessMessage(HttpExchange exchange, String message) throws IOException {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        logger.info("Success response: {} {} - {}",
                exchange.getRequestMethod(),
                exchange.getRequestURI().getPath(),
                message);
        sendJsonResponse(exchange, 200, response);
        logResponse(exchange, 200);
    }

    protected String readRequestBody(HttpExchange exchange) throws IOException {
        byte[] bodyBytes = exchange.getRequestBody().readAllBytes();
        String body = new String(bodyBytes, StandardCharsets.UTF_8);
        // ✅ Логирование тела запроса УБРАНО — пароли не попадают в логи
        // logger.info("Request body: {}", body);

        // Для отладки можно логировать только для не-чувствительных эндпоинтов
        String path = exchange.getRequestURI().getPath();
        if (!path.contains("/login") && !path.contains("/register")) {
            logger.debug("Request body (non-sensitive): {}", body);
        }
        return body;
    }

    protected <T> T readRequestBody(HttpExchange exchange, Class<T> clazz) throws IOException {
        String body = readRequestBody(exchange);
        try {
            return objectMapper.readValue(body, clazz);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            logger.error("Invalid JSON format in request body", e);
            throw new IOException("Invalid JSON format", e);
        }
    }

    protected String extractToken(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            logger.debug("Token extracted: {}...", token.substring(0, Math.min(20, token.length())));
            return token;
        }
        logger.debug("No Authorization header found");
        return null;
    }

    protected void logRequest(HttpExchange exchange) {
        long startTime = System.currentTimeMillis();
        requestStartTime.set(startTime);
        logger.info("→ {} {} - from {}",
                exchange.getRequestMethod(),
                exchange.getRequestURI().getPath(),
                exchange.getRemoteAddress());
    }

    protected void logResponse(HttpExchange exchange, int statusCode) {
        Long startTime = requestStartTime.get();
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            logger.info("← {} {} - status={}, duration={}ms",
                    exchange.getRequestMethod(),
                    exchange.getRequestURI().getPath(),
                    statusCode,
                    duration);
            requestStartTime.remove(); // Очищаем ThreadLocal
        }
    }

    protected void setCorsHeaders(HttpExchange exchange) {
        // ✅ CORS больше не "*", а читается из переменной окружения
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", ALLOWED_ORIGIN);
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    protected void handleOptions(HttpExchange exchange) throws IOException {
        setCorsHeaders(exchange);
        exchange.sendResponseHeaders(204, -1);
        logResponse(exchange, 204);
        exchange.close();
    }
}