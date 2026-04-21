package com.example.service;

import com.example.config.DatabaseConnection;
import com.example.dao.OtpCodeDao;
import com.example.dao.OtpConfigDao;
import com.example.model.OtpCode;
import com.example.model.OtpConfig;
import com.example.notification.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Random;

/**
 * Сервис для работы с OTP-кодами.
 * Генерация, валидация, управление статусами и отправка.
 *
 * ВАЖНО: Все операции с БД атомарны и используют транзакции
 * для предотвращения race conditions.
 */
public class OtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);

    private final OtpCodeDao otpCodeDao;
    private final OtpConfigDao configDao;
    private final NotificationService notificationService;
    private final Random random;

    // Константы для валидации (вынесены из магических чисел)
    private static final int MIN_CODE_LENGTH = 4;
    private static final int MAX_CODE_LENGTH = 10;
    private static final int DEFAULT_CODE_LENGTH = 6;

    private static final int MIN_TTL_SECONDS = 30;
    private static final int MAX_TTL_SECONDS = 3600;
    private static final int DEFAULT_TTL_SECONDS = 300;

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
        if (length < 1) {
            throw new IllegalArgumentException("Code length must be at least 1");
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * Генерирует новый OTP-код для пользователя и операции.
     *
     * ✅ АТОМАРНАЯ ОПЕРАЦИЯ:
     * - Использует транзакцию с блокировкой строки
     * - Деактивирует старые коды и вставляет новый за одну транзакцию
     * - Предотвращает race condition при параллельных запросах
     *
     * @param userId ID пользователя
     * @param operationId ID операции
     * @return сгенерированный код (в виде строки)
     * @throws SQLException если ошибка БД
     */
    public String generateOtp(Long userId, String operationId) throws SQLException {
        // Валидация входных параметров
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("Invalid user ID");
        }
        if (operationId == null || operationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Operation ID cannot be empty");
        }
        if (operationId.length() > 100) {
            throw new IllegalArgumentException("Operation ID too long (max 100 chars)");
        }

        // Получаем текущую конфигурацию
        OtpConfig config = configDao.getConfig();
        int codeLength = config != null ? config.getCodeLength() : DEFAULT_CODE_LENGTH;
        int ttlSeconds = config != null ? config.getTtlSeconds() : DEFAULT_TTL_SECONDS;

        // Генерируем код и время истечения
        String code = generateCode(codeLength);
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(ttlSeconds);

        logger.debug("Generating OTP for user {}: operationId={}, expiresAt={}",
                userId, operationId, expiresAt);

        // ✅ АТОМАРНАЯ ОПЕРАЦИЯ: транзакция с блокировкой
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);  // Начинаем транзакцию

            try {
                // 1. Блокируем строку пользователя для предотвращения параллельных операций
                // Используем SELECT FOR UPDATE для пессимистической блокировки
                String lockSql = "SELECT id FROM users WHERE id = ? FOR UPDATE";
                try (PreparedStatement lockStmt = conn.prepareStatement(lockSql)) {
                    lockStmt.setLong(1, userId);
                    var rs = lockStmt.executeQuery();
                    if (!rs.next()) {
                        throw new SQLException("User not found with ID: " + userId);
                    }
                }

                // 2. Деактивируем все старые активные коды для этой операции
                String deactivateSql = "UPDATE otp_codes SET status = 'EXPIRED'::code_status " +
                        "WHERE user_id = ? AND operation_id = ? AND status = 'ACTIVE'::code_status";
                try (PreparedStatement deactivateStmt = conn.prepareStatement(deactivateSql)) {
                    deactivateStmt.setLong(1, userId);
                    deactivateStmt.setString(2, operationId);
                    int deactivated = deactivateStmt.executeUpdate();
                    if (deactivated > 0) {
                        logger.info("Deactivated {} old codes for user {}: operationId={}",
                                deactivated, userId, operationId);
                    }
                }

                // 3. Вставляем новый код
                String insertSql = "INSERT INTO otp_codes (user_id, operation_id, code, status, created_at, expires_at) " +
                        "VALUES (?, ?, ?, 'ACTIVE'::code_status, ?, ?)";
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    insertStmt.setLong(1, userId);
                    insertStmt.setString(2, operationId);
                    insertStmt.setString(3, code);
                    insertStmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                    insertStmt.setTimestamp(5, Timestamp.valueOf(expiresAt));

                    int affectedRows = insertStmt.executeUpdate();
                    if (affectedRows == 0) {
                        throw new SQLException("Failed to insert OTP code");
                    }

                    try (var generatedKeys = insertStmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            logger.debug("OTP code inserted with ID: {}", generatedKeys.getLong(1));
                        }
                    }
                }

                conn.commit();  // ✅ Фиксируем транзакцию
                logger.info("OTP generated atomically for user {}: operationId={}", userId, operationId);

            } catch (SQLException e) {
                conn.rollback();  // ❌ Откат при любой ошибке
                logger.error("Transaction rollback for user {}: {}", userId, e.getMessage());
                throw e;
            } finally {
                conn.setAutoCommit(true);  // Восстанавливаем autocommit
            }
        }

        return code;
    }

    /**
     * Альтернативная реализация через UPSERT (более эффективная).
     * Использует единый SQL запрос вместо транзакции с блокировкой.
     */
    public String generateOtpUpsert(Long userId, String operationId) throws SQLException {
        OtpConfig config = configDao.getConfig();
        int codeLength = config != null ? config.getCodeLength() : DEFAULT_CODE_LENGTH;
        int ttlSeconds = config != null ? config.getTtlSeconds() : DEFAULT_TTL_SECONDS;

        String code = generateCode(codeLength);
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(ttlSeconds);

        // Единый атомарный SQL запрос
        String sql = """
            WITH deactivated AS (
                UPDATE otp_codes 
                SET status = 'EXPIRED'::code_status 
                WHERE user_id = ? 
                AND operation_id = ? 
                AND status = 'ACTIVE'::code_status
                RETURNING id
            )
            INSERT INTO otp_codes (user_id, operation_id, code, status, created_at, expires_at)
            SELECT ?, ?, ?, 'ACTIVE'::code_status, ?, ?
            WHERE NOT EXISTS (
                SELECT 1 FROM otp_codes 
                WHERE user_id = ? 
                AND operation_id = ? 
                AND status = 'ACTIVE'::code_status
            )
            RETURNING code
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Параметры для деактивации
            stmt.setLong(1, userId);
            stmt.setString(2, operationId);

            // Параметры для вставки
            stmt.setLong(3, userId);
            stmt.setString(4, operationId);
            stmt.setString(5, code);
            stmt.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setTimestamp(7, Timestamp.valueOf(expiresAt));

            // Параметры для проверки существования
            stmt.setLong(8, userId);
            stmt.setString(9, operationId);

            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    logger.info("OTP generated via UPSERT for user {}: operationId={}", userId, operationId);
                    return rs.getString("code");
                } else {
                    // Это не должно случиться, но на всякий случай
                    throw new SQLException("Failed to generate OTP code");
                }
            }
        }
    }

    /**
     * Генерирует и отправляет OTP-код через указанный канал
     *
     * @param userId ID пользователя
     * @param operationId ID операции
     * @param channel канал отправки (email, sms, telegram, file)
     * @param destination адрес получателя
     * @throws RuntimeException если отправка не удалась
     */
    public void generateAndSendOtp(Long userId, String operationId,
                                   String channel, String destination) throws SQLException {
        // Валидация destination
        if (destination == null || destination.trim().isEmpty()) {
            throw new IllegalArgumentException("Destination cannot be empty");
        }

        // Валидация канала
        String[] availableChannels = getAvailableChannels();
        boolean channelValid = false;
        for (String ch : availableChannels) {
            if (ch.equalsIgnoreCase(channel)) {
                channelValid = true;
                break;
            }
        }
        if (!channelValid) {
            throw new IllegalArgumentException("Invalid channel: " + channel);
        }

        // Генерируем код (атомарно!)
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
     *
     * @param userId ID пользователя
     * @param operationId ID операции
     * @param code код для проверки
     * @return true если код был успешно проверен и использован
     */
    public boolean validateOtp(Long userId, String operationId, String code) throws SQLException {
        // Валидация входных параметров
        if (userId == null || userId <= 0) {
            logger.warn("Invalid user ID for validation");
            return false;
        }
        if (operationId == null || operationId.trim().isEmpty()) {
            logger.warn("Empty operation ID for validation");
            return false;
        }
        if (code == null || code.trim().isEmpty()) {
            logger.warn("Empty code for validation");
            return false;
        }

        // Атомарная операция: проверка + обновление в одном запросе
        boolean validated = otpCodeDao.validateAndUseCode(userId, operationId, code);

        if (validated) {
            logger.info("✅ OTP code successfully validated for user {}: operationId={}",
                    userId, operationId);
        } else {
            logger.warn("❌ OTP validation failed for user {}: operationId={}",
                    userId, operationId);
        }

        return validated;
    }

    /**
     * Получить текущую конфигурацию OTP
     */
    public OtpConfig getConfig() throws SQLException {
        OtpConfig config = configDao.getConfig();
        if (config == null) {
            // Возвращаем конфиг по умолчанию, если в БД еще нет
            logger.warn("No config found in DB, using defaults");
            return new OtpConfig(DEFAULT_CODE_LENGTH, DEFAULT_TTL_SECONDS);
        }
        return config;
    }

    /**
     * Обновить конфигурацию OTP (только для админа)
     *
     * @param codeLength длина кода (4-10)
     * @param ttlSeconds время жизни в секундах (30-3600)
     * @throws IllegalArgumentException если параметры не проходят валидацию
     * @throws SQLException если ошибка БД
     */
    public void updateConfig(int codeLength, int ttlSeconds) throws SQLException {
        // Валидация параметров с использованием констант
        if (codeLength < MIN_CODE_LENGTH || codeLength > MAX_CODE_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("codeLength must be between %d and %d (got: %d)",
                            MIN_CODE_LENGTH, MAX_CODE_LENGTH, codeLength)
            );
        }
        if (ttlSeconds < MIN_TTL_SECONDS || ttlSeconds > MAX_TTL_SECONDS) {
            throw new IllegalArgumentException(
                    String.format("ttlSeconds must be between %d and %d (got: %d)",
                            MIN_TTL_SECONDS, MAX_TTL_SECONDS, ttlSeconds)
            );
        }

        OtpConfig config = new OtpConfig(codeLength, ttlSeconds);
        boolean updated = configDao.update(config);

        if (updated) {
            logger.info("✅ OTP config updated: codeLength={}, ttlSeconds={}", codeLength, ttlSeconds);
        } else {
            logger.error("❌ Failed to update OTP config");
            throw new SQLException("Failed to update OTP configuration");
        }
    }

    /**
     * Возвращает доступные каналы отправки
     */
    public String[] getAvailableChannels() {
        return notificationService.getAvailableChannels();
    }

}