package com.example.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Утилитарный класс для работы с JWT токенами.
 * Генерация, проверка, извлечение данных.
 */
public class JwtUtil {

    // Секретный ключ для подписи токенов (в реальном проекте хранится в настройках)
    private static final String SECRET_STRING = "mySuperSecretKeyForOtpServiceProject2024WithEnoughLength";
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(SECRET_STRING.getBytes(StandardCharsets.UTF_8));

    // Время жизни токена — 24 часа (в миллисекундах)
    private static final long EXPIRATION_TIME = 24 * 60 * 60 * 1000;

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