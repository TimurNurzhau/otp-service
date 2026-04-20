package com.example.notification;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Канал отправки OTP-кодов в файл.
 * Сохраняет коды в файл otp_codes.txt в корне проекта.
 */
public class FileNotificationChannel implements NotificationChannel {

    private static final String FILE_PATH = "otp_codes.txt";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public boolean send(String destination, String code) {
        try {
            Path path = Paths.get(FILE_PATH);
            String timestamp = LocalDateTime.now().format(FORMATTER);
            String line = String.format("[%s] Destination: %s, Code: %s%n", timestamp, destination, code);

            Files.writeString(
                    path,
                    line,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );

            System.out.println("[FILE] Code saved to " + FILE_PATH);
            return true;

        } catch (IOException e) {
            System.err.println("[FILE ERROR] Failed to save code: " + e.getMessage());
            return false;
        }
    }

    @Override
    public String getChannelName() {
        return "FILE";
    }
}