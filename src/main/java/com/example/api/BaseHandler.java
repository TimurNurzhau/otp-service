package com.example.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Базовый класс для всех HTTP-обработчиков.
 */
public abstract class BaseHandler implements HttpHandler {

    protected final ObjectMapper objectMapper = new ObjectMapper();

    protected void sendJsonResponse(HttpExchange exchange, int statusCode, Object response) throws IOException {
        String jsonResponse = objectMapper.writeValueAsString(response);
        byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    protected void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        sendJsonResponse(exchange, statusCode, error);
    }

    protected void sendSuccessResponse(HttpExchange exchange, Object data) throws IOException {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        sendJsonResponse(exchange, 200, response);
    }

    protected void sendSuccessMessage(HttpExchange exchange, String message) throws IOException {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        sendJsonResponse(exchange, 200, response);
    }

    protected String readRequestBody(HttpExchange exchange) throws IOException {
        byte[] bodyBytes = exchange.getRequestBody().readAllBytes();
        return new String(bodyBytes, StandardCharsets.UTF_8);
    }

    protected <T> T readRequestBody(HttpExchange exchange, Class<T> clazz) throws IOException {
        String body = readRequestBody(exchange);
        return objectMapper.readValue(body, clazz);
    }

    protected String extractToken(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    protected void logRequest(HttpExchange exchange) {
        System.out.println("[" + java.time.LocalDateTime.now() + "] " +
                exchange.getRequestMethod() + " " +
                exchange.getRequestURI().getPath());
    }
}