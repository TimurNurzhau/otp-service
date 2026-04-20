package com.example.dao;

import com.example.config.DatabaseConnection;
import com.example.model.OtpCode;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO для работы с таблицей otp_codes.
 * Содержит методы для создания, поиска и обновления OTP-кодов.
 */
public class OtpCodeDao {

    /**
     * Сохраняет новый OTP-код в базе данных
     * @param otpCode код для сохранения
     * @return сохранённый код с установленным id
     */
    public OtpCode save(OtpCode otpCode) throws SQLException {
        String sql = "INSERT INTO otp_codes (user_id, operation_id, code, status, created_at, expires_at) " +
                "VALUES (?, ?, ?, ?::code_status, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setLong(1, otpCode.getUserId());
            stmt.setString(2, otpCode.getOperationId());
            stmt.setString(3, otpCode.getCode());
            stmt.setString(4, otpCode.getStatus().name());
            stmt.setTimestamp(5, Timestamp.valueOf(otpCode.getCreatedAt()));
            stmt.setTimestamp(6, Timestamp.valueOf(otpCode.getExpiresAt()));

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating OTP code failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    otpCode.setId(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Creating OTP code failed, no ID obtained.");
                }
            }
        }

        return otpCode;
    }

    /**
     * Находит активный код по ID пользователя и ID операции
     * @param userId ID пользователя
     * @param operationId ID операции
     * @return Optional с кодом или пустой Optional
     */
    public Optional<OtpCode> findActiveByUserAndOperation(Long userId, String operationId) throws SQLException {
        String sql = "SELECT id, user_id, operation_id, code, status, created_at, expires_at " +
                "FROM otp_codes " +
                "WHERE user_id = ? AND operation_id = ? AND status = 'ACTIVE' " +
                "ORDER BY created_at DESC LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            stmt.setString(2, operationId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToOtpCode(rs));
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Находит код по ID пользователя, ID операции и значению кода
     * @param userId ID пользователя
     * @param operationId ID операции
     * @param code значение кода
     * @return Optional с кодом или пустой Optional
     */
    public Optional<OtpCode> findByUserOperationAndCode(Long userId, String operationId, String code) throws SQLException {
        String sql = "SELECT id, user_id, operation_id, code, status, created_at, expires_at " +
                "FROM otp_codes " +
                "WHERE user_id = ? AND operation_id = ? AND code = ? " +
                "ORDER BY created_at DESC LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            stmt.setString(2, operationId);
            stmt.setString(3, code);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToOtpCode(rs));
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Обновляет статус кода
     * @param id ID кода
     * @param newStatus новый статус
     * @return true если обновление прошло успешно
     */
    public boolean updateStatus(Long id, OtpCode.CodeStatus newStatus) throws SQLException {
        String sql = "UPDATE otp_codes SET status = ?::code_status WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newStatus.name());
            stmt.setLong(2, id);

            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        }
    }

    /**
     * Отмечает все просроченные коды как EXPIRED
     * @return количество обновлённых записей
     */
    public int markExpiredCodes() throws SQLException {
        String sql = "UPDATE otp_codes SET status = 'EXPIRED' " +
                "WHERE status = 'ACTIVE' AND expires_at < ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));

            return stmt.executeUpdate();
        }
    }

    /**
     * Удаляет все коды, связанные с пользователем
     * @param userId ID пользователя
     * @return количество удалённых записей
     */
    public int deleteByUserId(Long userId) throws SQLException {
        String sql = "DELETE FROM otp_codes WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            return stmt.executeUpdate();
        }
    }

    /**
     * Возвращает список всех активных кодов пользователя
     * @param userId ID пользователя
     * @return список кодов
     */
    public List<OtpCode> findActiveByUserId(Long userId) throws SQLException {
        List<OtpCode> codes = new ArrayList<>();
        String sql = "SELECT id, user_id, operation_id, code, status, created_at, expires_at " +
                "FROM otp_codes " +
                "WHERE user_id = ? AND status = 'ACTIVE' " +
                "ORDER BY created_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    codes.add(mapRowToOtpCode(rs));
                }
            }
        }

        return codes;
    }

    /**
     * Вспомогательный метод для преобразования строки ResultSet в объект OtpCode
     */
    private OtpCode mapRowToOtpCode(ResultSet rs) throws SQLException {
        OtpCode code = new OtpCode();
        code.setId(rs.getLong("id"));
        code.setUserId(rs.getLong("user_id"));
        code.setOperationId(rs.getString("operation_id"));
        code.setCode(rs.getString("code"));
        code.setStatus(OtpCode.CodeStatus.valueOf(rs.getString("status")));
        code.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        code.setExpiresAt(rs.getTimestamp("expires_at").toLocalDateTime());
        return code;
    }
}