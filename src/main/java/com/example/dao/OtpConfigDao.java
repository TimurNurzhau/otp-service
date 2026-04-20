package com.example.dao;

import com.example.config.DatabaseConnection;
import com.example.model.OtpConfig;

import java.sql.*;

/**
 * DAO для работы с таблицей otp_config.
 * Таблица всегда содержит только одну запись с id = 1.
 */
public class OtpConfigDao {

    private static final int CONFIG_ID = 1;

    /**
     * Получает текущую конфигурацию OTP-кодов
     * @return объект OtpConfig или null если конфигурация не найдена
     */
    public OtpConfig getConfig() throws SQLException {
        String sql = "SELECT id, code_length, ttl_seconds FROM otp_config WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, CONFIG_ID);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new OtpConfig(
                            rs.getInt("code_length"),
                            rs.getInt("ttl_seconds")
                    );
                }
            }
        }

        return null;
    }

    /**
     * Обновляет конфигурацию OTP-кодов
     * @param config новая конфигурация
     * @return true если обновление прошло успешно
     */
    public boolean update(OtpConfig config) throws SQLException {
        String sql = "UPDATE otp_config SET code_length = ?, ttl_seconds = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, config.getCodeLength());
            stmt.setInt(2, config.getTtlSeconds());
            stmt.setInt(3, CONFIG_ID);

            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        }
    }

    /**
     * Получает длину кода (удобный метод)
     */
    public int getCodeLength() throws SQLException {
        OtpConfig config = getConfig();
        return config != null ? config.getCodeLength() : 6; // по умолчанию 6
    }

    /**
     * Получает время жизни кода в секундах (удобный метод)
     */
    public int getTtlSeconds() throws SQLException {
        OtpConfig config = getConfig();
        return config != null ? config.getTtlSeconds() : 300; // по умолчанию 5 минут
    }
}