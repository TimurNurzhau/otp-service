package com.example.notification;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Канал отправки OTP-кодов через Telegram бота.
 */
public class TelegramNotificationChannel implements NotificationChannel {

    private final String botToken;
    private final String chatId;
    private final HttpClient httpClient;

    public TelegramNotificationChannel() {
        Properties config = loadConfig();
        this.botToken = config.getProperty("telegram.bot.token", "");
        this.chatId = config.getProperty("telegram.chat.id", "");
        this.httpClient = HttpClient.newHttpClient();
    }

    private Properties loadConfig() {
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("telegram.properties")) {

            Properties props = new Properties();
            if (input != null) {
                props.load(input);
            }
            return props;

        } catch (IOException e) {
            return new Properties();
        }
    }

    @Override
    public boolean send(String destination, String code) {
        // Используем chatId из конфига или переданный destination
        String targetChatId = (destination != null && !destination.isEmpty())
                ? destination : chatId;

        if (botToken.isEmpty() || targetChatId.isEmpty()) {
            System.out.println("[TELEGRAM EMULATION] Sending code '" + code + "' to chat " + targetChatId);
            return true;
        }

        try {
            String message = String.format("Your OTP code is: %s", code);
            String url = String.format(
                    "https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s",
                    botToken,
                    targetChatId,
                    URLEncoder.encode(message, StandardCharsets.UTF_8)
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() == 200) {
                System.out.println("[TELEGRAM] Code sent to chat " + targetChatId);
                return true;
            } else {
                System.err.println("[TELEGRAM ERROR] Status: " + response.statusCode());
                return false;
            }

        } catch (Exception e) {
            System.err.println("[TELEGRAM ERROR] " + e.getMessage());
            return false;
        }
    }

    @Override
    public String getChannelName() {
        return "TELEGRAM";
    }
}