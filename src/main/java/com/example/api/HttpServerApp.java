package com.example.api;

import com.example.dao.OtpCodeDao;
import com.example.model.User;
import com.example.service.UserService;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
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

        // Автоматическое создание админа при первом запуске
        createAdminIfNotExists();

        // Добавляем Graceful Shutdown
        setupShutdownHook();
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

    /**
     * Создает администратора при первом запуске приложения
     */
    private static void createAdminIfNotExists() {
        try {
            UserService userService = new UserService();
            var allUsers = userService.findAll();
            boolean adminExists = allUsers.stream()
                    .anyMatch(u -> u.getRole() == User.Role.ADMIN);

            if (!adminExists) {
                User admin = userService.register("admin", "admin123", User.Role.ADMIN);
                System.out.println("\n========================================");
                System.out.println("🎯 ADMIN USER CREATED AUTOMATICALLY:");
                System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                System.out.println("  Username: admin");
                System.out.println("  Password: admin123");
                System.out.println("  Role: ADMIN");
                System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                System.out.println("========================================\n");
            } else {
                System.out.println("[INFO] Admin user already exists, skipping creation.");
            }
        } catch (Exception e) {
            System.out.println("[INFO] Admin check: " + e.getMessage());
        }
    }
}