package com.example.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Properties;

/**
 * Утилитарный класс для работы с JWT токенами.
 * Генерация, проверка, извлечение данных.
 */
public class JwtUtil {

    private static final SecretKey SECRET_KEY;
    private static final long EXPIRATION_TIME;

    static {
        try (InputStream input = JwtUtil.class.getClassLoader()
                .getResourceAsStream("jwt.properties")) {

            if (input == null) {
                throw new RuntimeException("jwt.properties not found in classpath");
            }

            Properties props = new Properties();
            props.load(input);

            String secretString = props.getProperty("jwt.secret");
            if (secretString == null || secretString.trim().isEmpty()) {
                throw new RuntimeException("jwt.secret is not configured");
            }

            String expirationStr = props.getProperty("jwt.expiration.ms");
            if (expirationStr == null || expirationStr.trim().isEmpty()) {
                throw new RuntimeException("jwt.expiration.ms is not configured");
            }

            SECRET_KEY = Keys.hmacShaKeyFor(secretString.getBytes(StandardCharsets.UTF_8));
            EXPIRATION_TIME = Long.parseLong(expirationStr);

            System.out.println("[JWT] Configuration loaded successfully");

        } catch (Exception e) {
            System.err.println("[JWT ERROR] Failed to load configuration: " + e.getMessage());
            throw new RuntimeException("Failed to initialize JWT configuration", e);
        }
    }

    /**
     * Генерирует JWT токен для пользователя
     * @param userId ID пользователя
     * @param username имя пользователя
     * @param role роль пользователя
     * @return строка с JWT токеном
     */
    public static String generateToken(Long userId, String username, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + EXPIRATION_TIME);

        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(SECRET_KEY)
                .compact();
    }

    /**
     * Проверяет токен и возвращает Claims (данные внутри токена)
     * @param token JWT токен
     * @return Claims с данными пользователя
     * @throws io.jsonwebtoken.JwtException если токен невалидный или просроченный
     */
    public static Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(SECRET_KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Извлекает ID пользователя из токена
     */
    public static Long getUserIdFromToken(String token) {
        Claims claims = validateToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * Извлекает имя пользователя из токена
     */
    public static String getUsernameFromToken(String token) {
        Claims claims = validateToken(token);
        return claims.getSubject();
    }

    /**
     * Извлекает роль пользователя из токена
     */
    public static String getRoleFromToken(String token) {
        Claims claims = validateToken(token);
        return claims.get("role", String.class);
    }

    /**
     * Проверяет, является ли токен валидным
     */
    public static boolean isValidToken(String token) {
        try {
            validateToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Проверяет, имеет ли пользователь роль ADMIN
     */
    public static boolean isAdmin(String token) {
        try {
            String role = getRoleFromToken(token);
            return "ADMIN".equals(role);
        } catch (Exception e) {
            return false;
        }
    }
}