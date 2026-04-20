package com.example.notification;

/**
 * Интерфейс для каналов отправки уведомлений.
 * Все каналы (Email, SMS, Telegram, File) должны реализовывать этот интерфейс.
 */
public interface NotificationChannel {

    /**
     * Отправляет OTP-код получателю
     * @param destination адрес получателя (email, номер телефона, chatId)
     * @param code OTP-код
     * @return true если отправка успешна
     */
    boolean send(String destination, String code);

    /**
     * Возвращает название канала
     */
    String getChannelName();
}