package com.example.api;

import com.example.dao.OtpCodeDao;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Главный класс HTTP-сервера.
 * Запускает сервер на порту 8080 и регистрирует обработчики.
 */
public class HttpServerApp {

    public static void main(String[] args) throws Exception {
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

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
        System.out.println("  POST /api/register");
        System.out.println("  POST /api/login");
        System.out.println("  POST /api/otp/generate");
        System.out.println("  POST /api/otp/validate");
        System.out.println("  GET/PUT /api/admin/config");
        System.out.println("  GET/DELETE /api/admin/users");
        System.out.println("========================================");

        // Запускаем фоновую очистку просроченных кодов
        startExpiredCodeCleaner();
    }

    /**
     * Запускает фоновый процесс, который каждую минуту отмечает
     * просроченные OTP-коды как EXPIRED.
     */
    private static void startExpiredCodeCleaner() {
        OtpCodeDao otpCodeDao = new OtpCodeDao();

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                int expired = otpCodeDao.markExpiredCodes();
                if (expired > 0) {
                    System.out.println("[CLEANER] Marked " + expired + " codes as EXPIRED");
                }
            } catch (Exception e) {
                System.err.println("[CLEANER ERROR] " + e.getMessage());
            }
        }, 1, 1, TimeUnit.MINUTES); // Проверка каждую минуту

        System.out.println("[CLEANER] Expired code cleaner started (runs every minute)");
    }
}