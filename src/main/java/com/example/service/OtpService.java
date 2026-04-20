package com.example.service;

import com.example.dao.OtpCodeDao;
import com.example.dao.OtpConfigDao;
import com.example.model.OtpCode;
import com.example.model.OtpConfig;
import com.example.notification.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Random;

/**
 * Сервис для работы с OTP-кодами.
 * Генерация, валидация, управление статусами и отправка.
 */
public class OtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);

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

        deactivateOldCodes(userId, operationId);

        // Генерируем код
        String code = generateCode(codeLength);

        // Вычисляем время истечения
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(ttlSeconds);

        // Создаём и сохраняем запись
        OtpCode otpCode = new OtpCode(userId, operationId, code, expiresAt);
        otpCodeDao.save(otpCode);

        logger.debug("OTP code generated for user {}: operationId={}, expiresAt={}",
                userId, operationId, expiresAt);

        return code;
    }

    /**
     * Генерирует и отправляет OTP-код через указанный канал
     * @param userId ID пользователя
     * @param operationId ID операции
     * @param channel канал отправки (email, sms, telegram, file)
     * @param destination адрес получателя
     * @throws RuntimeException если отправка не удалась
     */
    public void generateAndSendOtp(Long userId, String operationId,
                                   String channel, String destination) throws SQLException {
        // Генерируем код
        String code = generateOtp(userId, operationId);

        // Отправляем через указанный канал
        boolean sent = notificationService.send(channel, destination, code);

        if (!sent) {
            logger.error("Failed to send OTP via {} to {} for user {}",
                    channel, destination, userId);
            throw new RuntimeException("Failed to send OTP code via " + channel);
        }

        logger.info("OTP sent successfully via {} to {} for user {}: operationId={}",
                channel, destination, userId, operationId);
    }

    /**
     * Проверяет OTP-код (атомарно, безопасно для многопоточности)
     * @param userId ID пользователя
     * @param operationId ID операции
     * @param code код для проверки
     * @return true если код был успешно проверен и использован
     */
    public boolean validateOtp(Long userId, String operationId, String code) throws SQLException {
        // Атомарная операция: проверка + обновление в одном запросе
        boolean validated = otpCodeDao.validateAndUseCode(userId, operationId, code);

        if (validated) {
            logger.info("OTP code successfully validated for user {}: operationId={}",
                    userId, operationId);
        } else {
            logger.warn("OTP validation failed for user {}: operationId={}",
                    userId, operationId);
        }

        return validated;
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
        // Валидация параметров
        if (codeLength < 4 || codeLength > 10) {
            throw new IllegalArgumentException("codeLength must be between 4 and 10");
        }
        if (ttlSeconds < 30 || ttlSeconds > 3600) {
            throw new IllegalArgumentException("ttlSeconds must be between 30 and 3600");
        }

        OtpConfig config = new OtpConfig(codeLength, ttlSeconds);
        boolean updated = configDao.update(config);

        if (updated) {
            logger.info("OTP config updated: codeLength={}, ttlSeconds={}", codeLength, ttlSeconds);
        } else {
            logger.error("Failed to update OTP config");
            throw new SQLException("Failed to update OTP configuration");
        }
    }

    /**
     * Возвращает доступные каналы отправки
     */
    public String[] getAvailableChannels() {
        return notificationService.getAvailableChannels();
    }

    /**
     * Деактивирует все активные коды для операции (полезно при повторной генерации)
     * @param userId ID пользователя
     * @param operationId ID операции
     */
    public void deactivateOldCodes(Long userId, String operationId) throws SQLException {
        String sql = "UPDATE otp_codes SET status = 'EXPIRED'::code_status " +
                "WHERE user_id = ? AND operation_id = ? AND status = 'ACTIVE'::code_status";

        try (var conn = com.example.config.DatabaseConnection.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setString(2, operationId);
            int deactivated = stmt.executeUpdate();
            if (deactivated > 0) {
                logger.info("Deactivated {} old codes for user {}: operationId={}",
                        deactivated, userId, operationId);
            }
        }
    }
}