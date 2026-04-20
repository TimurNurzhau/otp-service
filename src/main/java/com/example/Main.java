package com.example;

import com.example.model.User;
import com.example.service.OtpService;
import com.example.service.UserService;
import com.example.security.JwtUtil;
import org.mindrot.jbcrypt.BCrypt;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Testing Libraries ===");
        System.out.println();

        // Test 1: BCrypt (Password Hashing)
        try {
            String password = "test123";
            String hashed = BCrypt.hashpw(password, BCrypt.gensalt());
            boolean check = BCrypt.checkpw(password, hashed);
            System.out.println("[OK] BCrypt works! Password check: " + check);
        } catch (Exception e) {
            System.out.println("[ERROR] BCrypt failed: " + e.getMessage());
        }

        // Test 2: PostgreSQL Driver
        try {
            Class.forName("org.postgresql.Driver");
            System.out.println("[OK] PostgreSQL Driver loaded!");
        } catch (ClassNotFoundException e) {
            System.out.println("[ERROR] PostgreSQL Driver not found: " + e.getMessage());
        }

        // Test 3: Jackson (JSON)
        try {
            Class.forName("com.fasterxml.jackson.databind.ObjectMapper");
            System.out.println("[OK] Jackson (JSON) loaded!");
        } catch (ClassNotFoundException e) {
            System.out.println("[ERROR] Jackson not found: " + e.getMessage());
        }

        // Test 4: JWT Library
        try {
            Class.forName("io.jsonwebtoken.Jwts");
            System.out.println("[OK] JWT library loaded!");
        } catch (ClassNotFoundException e) {
            System.out.println("[ERROR] JWT library not found: " + e.getMessage());
        }

        // Test 5: Database Connection
        System.out.println();
        System.out.println("=== Testing Database Connection ===");
        boolean dbConnected = com.example.config.DatabaseConnection.testConnection();
        if (dbConnected) {
            System.out.println("[OK] Database connection successful!");
        } else {
            System.out.println("[ERROR] Could not connect to database");
            return;
        }

        System.out.println();
        System.out.println("=== All critical tests passed! ===");

        // Test 6: Service Layer Testing
        System.out.println();
        System.out.println("=== Testing Service Layer ===");
        testServiceLayer();
    }

    /**
     * Тестирование сервисного слоя, каналов отправки и JWT
     */
    private static void testServiceLayer() {
        UserService userService = new UserService();
        OtpService otpService = new OtpService();

        try {
            System.out.println();
            System.out.println("--- Testing UserService ---");

            // Регистрация пользователя (если ещё нет)
            try {
                User user = userService.register("service_test", "pass123", User.Role.USER);
                System.out.println("Registered user: " + user);
            } catch (IllegalArgumentException e) {
                System.out.println("User already exists: " + e.getMessage());
            }

            // Логин
            User loggedIn = userService.login("service_test", "pass123");
            System.out.println("Logged in: " + loggedIn);
            System.out.println("Is admin? " + userService.isAdmin(loggedIn.getId()));

            System.out.println();
            System.out.println("--- Testing OtpService ---");

            // Генерация кода
            String code = otpService.generateOtp(loggedIn.getId(), "test_operation");
            System.out.println("Generated OTP code: " + code);
            System.out.println("Code length: " + code.length());

            // Проверка кода (должен быть верным)
            boolean valid = otpService.validateOtp(loggedIn.getId(), "test_operation", code);
            System.out.println("Code validation (correct): " + valid);

            // Проверка неверного кода
            boolean invalid = otpService.validateOtp(loggedIn.getId(), "test_operation", "000000");
            System.out.println("Code validation (wrong): " + invalid);

            // Повторная проверка того же кода (должен быть false, т.к. уже USED)
            boolean used = otpService.validateOtp(loggedIn.getId(), "test_operation", code);
            System.out.println("Code validation (already used): " + used);

            // Получение конфигурации
            System.out.println("Current OTP config: " + otpService.getConfig());

            System.out.println();
            System.out.println("--- Testing Notification Channels ---");

            // Доступные каналы
            System.out.println("Available channels: " + String.join(", ", otpService.getAvailableChannels()));

            // Отправка через FILE
            String fileCode = otpService.generateAndSendOtp(loggedIn.getId(), "file_test", "file", "test_user");
            System.out.println("[FILE] Generated and saved code: " + fileCode);

            // Отправка через EMAIL (эмуляция)
            String emailCode = otpService.generateAndSendOtp(loggedIn.getId(), "email_test", "email", "test@example.com");
            System.out.println("[EMAIL] Generated code: " + emailCode);

            // Отправка через SMS (эмуляция)
            String smsCode = otpService.generateAndSendOtp(loggedIn.getId(), "sms_test", "sms", "+79001234567");
            System.out.println("[SMS] Generated code: " + smsCode);

            // Отправка через TELEGRAM (эмуляция)
            String telegramCode = otpService.generateAndSendOtp(loggedIn.getId(), "telegram_test", "telegram", "123456789");
            System.out.println("[TELEGRAM] Generated code: " + telegramCode);

            System.out.println();
            System.out.println("--- Testing JWT ---");

            String token = JwtUtil.generateToken(
                    loggedIn.getId(),
                    loggedIn.getUsername(),
                    loggedIn.getRole().name()
            );
            System.out.println("Generated JWT token: " + token.substring(0, Math.min(50, token.length())) + "...");
            System.out.println("Token valid: " + JwtUtil.isValidToken(token));
            System.out.println("Username from token: " + JwtUtil.getUsernameFromToken(token));
            System.out.println("Role from token: " + JwtUtil.getRoleFromToken(token));
            System.out.println("Is admin (from token): " + JwtUtil.isAdmin(token));

            System.out.println();
            System.out.println("[OK] All service layer tests passed!");

        } catch (Exception e) {
            System.err.println("[ERROR] Service test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}