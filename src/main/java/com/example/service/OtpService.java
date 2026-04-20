package com.example.service;

import com.example.dao.OtpCodeDao;
import com.example.dao.OtpConfigDao;
import com.example.model.OtpCode;
import com.example.model.OtpConfig;
import com.example.notification.NotificationService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Random;

/**
 * Сервис для работы с OTP-кодами.
 * Генерация, валидация, управление статусами и отправка.
 */
public class OtpService {

    private final OtpCodeDao otpCodeDao;
    private final OtpConfigDao configDao;
    private final NotificationService notificationService;
    private final Random random;

    public OtpService() {
        this.otpCodeDao = new OtpCodeDao();
        this.configDao = new OtpConfigDao();
        this.notificationService = new NotificationService();
        this.random = new Random();
    }

    /**
     * Генерирует случайный цифровой код заданной длины
     */
    private String generateCode(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * Генерирует новый OTP-код для пользователя и операции
     * @param userId ID пользователя
     * @param operationId ID операции
     * @return сгенерированный код (в виде строки)
     */
    public String generateOtp(Long userId, String operationId) throws SQLException {
        // Получаем текущую конфигурацию
        OtpConfig config = configDao.getConfig();
        int codeLength = config.getCodeLength();
        int ttlSeconds = config.getTtlSeconds();

        // Генерируем код
        String code = generateCode(codeLength);

        // Вычисляем время истечения
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(ttlSeconds);

        // Создаём и сохраняем запись
        OtpCode otpCode = new OtpCode(userId, operationId, code, expiresAt);
        otpCodeDao.save(otpCode);

        return code;
    }

    /**
     * Генерирует и отправляет OTP-код через указанный канал
     * @param userId ID пользователя
     * @param operationId ID операции
     * @param channel канал отправки (email, sms, telegram, file)
     * @param destination адрес получателя
     * @return сгенерированный код
     */
    public String generateAndSendOtp(Long userId, String operationId,
                                     String channel, String destination) throws SQLException {
        String code = generateOtp(userId, operationId);
        notificationService.send(channel, destination, code);
        return code;
    }

    /**
     * Проверяет OTP-код
     * @param userId ID пользователя
     * @param operationId ID операции
     * @param code код для проверки
     * @return true если код верный и активный
     */
    public boolean validateOtp(Long userId, String operationId, String code) throws SQLException {
        var otpCodeOpt = otpCodeDao.findByUserOperationAndCode(userId, operationId, code);

        if (otpCodeOpt.isEmpty()) {
            return false;
        }

        OtpCode otpCode = otpCodeOpt.get();

        // Проверяем статус
        if (!otpCode.isActive()) {
            return false;
        }

        // Проверяем срок действия
        if (otpCode.isExpired()) {
            otpCodeDao.updateStatus(otpCode.getId(), OtpCode.CodeStatus.EXPIRED);
            return false;
        }

        // Отмечаем как использованный
        otpCodeDao.updateStatus(otpCode.getId(), OtpCode.CodeStatus.USED);

        return true;
    }

    /**
     * Получить текущую конфигурацию OTP
     */
    public OtpConfig getConfig() throws SQLException {
        return configDao.getConfig();
    }

    /**
     * Обновить конфигурацию OTP (только для админа)
     */
    public void updateConfig(int codeLength, int ttlSeconds) throws SQLException {
        OtpConfig config = new OtpConfig(codeLength, ttlSeconds);
        configDao.update(config);
    }

    /**
     * Возвращает доступные каналы отправки
     */
    public String[] getAvailableChannels() {
        return notificationService.getAvailableChannels();
    }
}