package com.example.api;

import com.example.config.EnvConfig;
import com.example.dao.OtpCodeDao;
import com.example.model.User;
import com.example.service.UserService;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.security.SecureRandom;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Главный класс HTTP-сервера.
 * Запускает сервер на порту 8080 и регистрирует обработчики.
 */
public class HttpServerApp {

    private static HttpServer server;
    private static ScheduledExecutorService scheduler;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static void main(String[] args) throws Exception {
        int desiredPort = 8080;

        // Порт можно указать как аргумент командной строки
        if (args.length > 0) {
            try {
                desiredPort = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number, using default 8080");
            }
        }

        // Находим свободный порт
        int port = findFreePort(desiredPort);

        if (port != desiredPort) {
            System.out.println("Port " + desiredPort + " is busy, using port " + port + " instead");
        }

        server = HttpServer.create(new InetSocketAddress(port), 0);

        // Регистрируем обработчики
        server.createContext("/api/register", new RegisterHandler());
        server.createContext("/api/login", new LoginHandler());
        server.createContext("/api/otp/generate", new OtpGenerateHandler());
        server.createContext("/api/otp/validate", new OtpValidateHandler());
        server.createContext("/api/admin/config", new AdminConfigHandler());
        server.createContext("/api/admin/users", new AdminUsersHandler());

        server.setExecutor(null);
        server.start();

        System.out.println("========================================");
        System.out.println("OTP Service started on port " + port);
        System.out.println("========================================");
        System.out.println("Available endpoints:");
        System.out.println("  POST http://localhost:" + port + "/api/register");
        System.out.println("  POST http://localhost:" + port + "/api/login");
        System.out.println("  POST http://localhost:" + port + "/api/otp/generate");
        System.out.println("  POST http://localhost:" + port + "/api/otp/validate");
        System.out.println("  GET/PUT http://localhost:" + port + "/api/admin/config");
        System.out.println("  GET/DELETE http://localhost:" + port + "/api/admin/users");
        System.out.println("========================================");

        // Запускаем фоновую очистку просроченных кодов
        startExpiredCodeCleaner();

        // ✅ Автоматическое создание админа с безопасным паролем
        createAdminIfNotExists();

        // Добавляем Graceful Shutdown
        setupShutdownHook();
    }

    /**
     * Генерирует случайный безопасный пароль
     */
    private static String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder password = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            password.append(chars.charAt(SECURE_RANDOM.nextInt(chars.length())));
        }
        return password.toString();
    }

    /**
     * Создает администратора при первом запуске приложения
     * ✅ БЕЗОПАСНАЯ ВЕРСИЯ: пароль читается из env или генерируется
     */
    private static void createAdminIfNotExists() {
        try {
            UserService userService = new UserService();
            var allUsers = userService.findAll();
            boolean adminExists = allUsers.stream()
                    .anyMatch(u -> u.getRole() == User.Role.ADMIN);

            if (!adminExists) {
                // Читаем из переменных окружения
                String adminUsername = EnvConfig.getOrDefault("OTP_ADMIN_USERNAME", "admin");
                String adminPassword = EnvConfig.get("OTP_ADMIN_PASSWORD");

                boolean isGenerated = false;
                if (adminPassword == null || adminPassword.isEmpty()) {
                    // Если пароль не задан в env - генерируем случайный
                    adminPassword = generateRandomPassword();
                    isGenerated = true;
                }

                User admin = userService.register(adminUsername, adminPassword, User.Role.ADMIN);

                System.out.println("\n╔════════════════════════════════════════════════════════════════╗");
                System.out.println("║                    🎯 ADMIN USER CREATED                       ║");
                System.out.println("╠════════════════════════════════════════════════════════════════╣");
                System.out.println("║  Username: " + padRight(adminUsername, 44) + "║");
                System.out.println("║  Password: " + padRight(adminPassword, 44) + "║");
                System.out.println("║  Role:     ADMIN" + padRight("", 41) + "║");
                if (isGenerated) {
                    System.out.println("╠════════════════════════════════════════════════════════════════╣");
                    System.out.println("║  ⚠️  PASSWORD WAS AUTO-GENERATED!                              ║");
                    System.out.println("║  Save this password immediately! It won't be shown again.     ║");
                    System.out.println("║  To set a fixed password, use env variable:                   ║");
                    System.out.println("║  OTP_ADMIN_PASSWORD=your_secure_password                      ║");
                }
                System.out.println("╚════════════════════════════════════════════════════════════════╝\n");

            } else {
                System.out.println("[INFO] Admin user already exists, skipping creation.");

                // Дополнительная безопасность: проверяем, не используется ли дефолтный пароль
                var adminUser = allUsers.stream()
                        .filter(u -> u.getRole() == User.Role.ADMIN)
                        .findFirst();

                if (adminUser.isPresent()) {
                    String envPassword = EnvConfig.get("OTP_ADMIN_PASSWORD");
                    if (envPassword != null && !envPassword.isEmpty()) {
                        System.out.println("[INFO] Admin password is set via environment variable");
                    } else {
                        System.out.println("[WARN] Admin password is NOT set via environment variable!");
                        System.out.println("[WARN] Consider setting OTP_ADMIN_PASSWORD for security");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to create/check admin: " + e.getMessage());
        }
    }

    /**
     * Вспомогательный метод для выравнивания строк
     */
    private static String padRight(String s, int length) {
        if (s.length() >= length) return s.substring(0, length);
        return s + " ".repeat(length - s.length());
    }

    /**
     * Находит свободный порт, начиная с desiredPort
     */
    private static int findFreePort(int desiredPort) {
        int port = desiredPort;
        int maxAttempts = 10;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            if (isPortAvailable(port)) {
                return port;
            }
            port++;
        }

        // Если не нашли свободный порт, используем случайный
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException("Could not find any free port", e);
        }
    }

    /**
     * Проверяет, доступен ли порт
     */
    private static boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket()) {
            socket.bind(new InetSocketAddress("localhost", port));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Запускает фоновый процесс, который каждую минуту отмечает
     * просроченные OTP-коды как EXPIRED.
     */
    private static void startExpiredCodeCleaner() {
        OtpCodeDao otpCodeDao = new OtpCodeDao();

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                int expired = otpCodeDao.markExpiredCodes();
                if (expired > 0) {
                    System.out.println("[CLEANER] Marked " + expired + " codes as EXPIRED");
                }
            } catch (Exception e) {
                System.err.println("[CLEANER ERROR] " + e.getMessage());
            }
        }, 1, 1, TimeUnit.MINUTES);

        System.out.println("[CLEANER] Expired code cleaner started (runs every minute)");
    }

    /**
     * Настраивает корректное завершение работы приложения
     */
    private static void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n========================================");
            System.out.println("Shutting down OTP Service...");
            System.out.println("========================================");

            if (server != null) {
                server.stop(5);
                System.out.println("[SHUTDOWN] HTTP server stopped");
            }

            if (scheduler != null && !scheduler.isShutdown()) {
                scheduler.shutdown();
                try {
                    if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                        scheduler.shutdownNow();
                    }
                    System.out.println("[SHUTDOWN] Cleaner scheduler stopped");
                } catch (InterruptedException e) {
                    scheduler.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }

            System.out.println("[SHUTDOWN] OTP Service stopped successfully");
            System.out.println("========================================");
        }));
    }
}