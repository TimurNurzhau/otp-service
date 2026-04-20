package com.example.dao;

import com.example.config.DatabaseConnection;
import com.example.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO для работы с таблицей users.
 * Содержит методы для создания, поиска, удаления пользователей.
 */
public class UserDao {

    /**
     * Сохраняет нового пользователя в базе данных
     * @param user пользователь для сохранения (id будет присвоен автоматически)
     * @return сохранённый пользователь с установленным id
     */
    public User save(User user) throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPasswordHash());
            stmt.setString(3, user.getRole().name());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating user failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setId(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Creating user failed, no ID obtained.");
                }
            }
        }

        return user;
    }

    /**
     * Находит пользователя по имени (username)
     * @param username имя пользователя
     * @return Optional с пользователем или пустой Optional
     */
    public Optional<User> findByUsername(String username) throws SQLException {
        String sql = "SELECT id, username, password_hash, role FROM users WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    User user = mapRowToUser(rs);
                    return Optional.of(user);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Находит пользователя по id
     * @param id идентификатор пользователя
     * @return Optional с пользователем или пустой Optional
     */
    public Optional<User> findById(Long id) throws SQLException {
        String sql = "SELECT id, username, password_hash, role FROM users WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    User user = mapRowToUser(rs);
                    return Optional.of(user);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Возвращает список всех пользователей
     * @return список пользователей
     */
    public List<User> findAll() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, username, password_hash, role FROM users ORDER BY id";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                users.add(mapRowToUser(rs));
            }
        }

        return users;
    }

    /**
     * Возвращает список всех пользователей с определённой ролью
     * @param role роль для фильтрации
     * @return список пользователей
     */
    public List<User> findByRole(User.Role role) throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, username, password_hash, role FROM users WHERE role = ? ORDER BY id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, role.name());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapRowToUser(rs));
                }
            }
        }

        return users;
    }

    /**
     * Проверяет, существует ли пользователь с ролью ADMIN
     * @return true если администратор уже существует
     */
    public boolean adminExists() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE role = 'ADMIN'";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }

        return false;
    }

    /**
     * Удаляет пользователя по id
     * @param id идентификатор пользователя
     * @return true если пользователь был удалён
     */
    public boolean deleteById(Long id) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        }
    }

    /**
     * Обновляет роль пользователя
     * @param id идентификатор пользователя
     * @param newRole новая роль
     * @return true если обновление прошло успешно
     */
    public boolean updateRole(Long id, User.Role newRole) throws SQLException {
        String sql = "UPDATE users SET role = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newRole.name());
            stmt.setLong(2, id);

            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        }
    }

    /**
     * Вспомогательный метод для преобразования строки ResultSet в объект User
     */
    private User mapRowToUser(ResultSet rs) throws SQLException {
        return new User(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                User.Role.valueOf(rs.getString("role"))
        );
    }
}