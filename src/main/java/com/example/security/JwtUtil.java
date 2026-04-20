package com.example.security;

import com.example.config.EnvConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Утилитарный класс для работы с JWT токенами.
 * Использует переменные окружения для секрета.
 */
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);
    private static final SecretKey SECRET_KEY;
    private static final long EXPIRATION_TIME;

    static {
        // Загружаем секрет из переменных окружения
        String secretString = EnvConfig.getRequired("jwt.secret");

        // Проверяем длину секрета (должен быть минимум 32 байта для HS256)
        if (secretString.length() < 32) {
            logger.warn("JWT secret is too short ({} chars). Minimum recommended: 32 characters",
                    secretString.length());
        }

        String expirationStr = EnvConfig.getOrDefault("jwt.expiration.ms", "86400000");

        SECRET_KEY = Keys.hmacShaKeyFor(secretString.getBytes(StandardCharsets.UTF_8));
        EXPIRATION_TIME = Long.parseLong(expirationStr);

        logger.info("JWT Configuration loaded from environment variables");
        logger.debug("JWT expiration time: {} ms ({} hours)",
                EXPIRATION_TIME, EXPIRATION_TIME / 3600000);
    }

    public static String generateToken(Long userId, String username, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + EXPIRATION_TIME);

        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(SECRET_KEY, Jwts.SIG.HS256)
                .compact();
    }

    public static Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(SECRET_KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public static Long getUserIdFromToken(String token) {
        Claims claims = validateToken(token);
        return claims.get("userId", Long.class);
    }

    public static String getUsernameFromToken(String token) {
        Claims claims = validateToken(token);
        return claims.getSubject();
    }

    public static String getRoleFromToken(String token) {
        Claims claims = validateToken(token);
        return claims.get("role", String.class);
    }

    public static boolean isValidToken(String token) {
        try {
            validateToken(token);
            return true;
        } catch (Exception e) {
            logger.debug("Invalid token: {}", e.getMessage());
            return false;
        }
    }

    public static boolean isAdmin(String token) {
        try {
            String role = getRoleFromToken(token);
            return "ADMIN".equals(role);
        } catch (Exception e) {
            return false;
        }
    }
}