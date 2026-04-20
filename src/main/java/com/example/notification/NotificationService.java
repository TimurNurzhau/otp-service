package com.example.notification;

import java.util.HashMap;
import java.util.Map;

/**
 * Сервис для отправки уведомлений через разные каналы.
 */
public class NotificationService {

    private final Map<String, NotificationChannel> channels;

    public NotificationService() {
        channels = new HashMap<>();
        channels.put("email", new EmailNotificationChannel());
        channels.put("sms", new SmsNotificationChannel());
        channels.put("telegram", new TelegramNotificationChannel());
        channels.put("file", new FileNotificationChannel());
    }

    /**
     * Отправляет OTP-код через указанный канал
     * @param channelName название канала (email, sms, telegram, file)
     * @param destination адрес получателя
     * @param code код для отправки
     * @return true если отправка успешна
     */
    public boolean send(String channelName, String destination, String code) {
        NotificationChannel channel = channels.get(channelName.toLowerCase());

        if (channel == null) {
            System.err.println("[NOTIFICATION] Unknown channel: " + channelName);
            return false;
        }

        return channel.send(destination, code);
    }

    /**
     * Возвращает список доступных каналов
     */
    public String[] getAvailableChannels() {
        return channels.keySet().toArray(new String[0]);
    }
}