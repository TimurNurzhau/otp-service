package com.example.service;

import com.example.dao.UserDao;
import com.example.model.User;
import at.favre.lib.crypto.bcrypt.BCrypt;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Сервис для работы с пользователями.
 * Содержит бизнес-логику регистрации, аутентификации и управления пользователями.
 */
public class UserService {

    private final UserDao userDao;
    private static final BCrypt.Hasher BCRYPT_HASHER = BCrypt.withDefaults();
    private static final BCrypt.Verifyer BCRYPT_VERIFIER = BCrypt.verifyer();

    public UserService() {
        this.userDao = new UserDao();
    }

    /**
     * Регистрация нового пользователя
     */
    public User register(String username, String password, User.Role role) throws SQLException {
        Optional<User> existing = userDao.findByUsername(username);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("User with username '" + username + "' already exists");
        }

        if (role == User.Role.ADMIN && userDao.adminExists()) {
            throw new IllegalStateException("Admin already exists. Cannot create second admin.");
        }

        // Хешируем пароль (новая библиотека)
        String passwordHash = BCRYPT_HASHER.hashToString(12, password.toCharArray());

        User user = new User(username, passwordHash, role);
        return userDao.save(user);
    }

    /**
     * Аутентификация пользователя
     */
    public User login(String username, String password) throws SQLException {
        Optional<User> userOpt = userDao.findByUsername(username);

        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        User user = userOpt.get();

        // Проверяем пароль (новая библиотека)
        BCrypt.Result result = BCRYPT_VERIFIER.verify(password.toCharArray(), user.getPasswordHash());

        if (!result.verified) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        return user;
    }

    // Остальные методы остаются без изменений
    public Optional<User> findById(Long id) throws SQLException {
        return userDao.findById(id);
    }

    public Optional<User> findByUsername(String username) throws SQLException {
        return userDao.findByUsername(username);
    }

    public List<User> findAll() throws SQLException {
        return userDao.findAll();
    }

    public List<User> findAllNonAdmin() throws SQLException {
        return userDao.findByRole(User.Role.USER);
    }

    public boolean deleteUser(Long id) throws SQLException {
        return userDao.deleteById(id);
    }

    public boolean isAdmin(Long userId) throws SQLException {
        Optional<User> user = userDao.findById(userId);
        return user.isPresent() && user.get().isAdmin();
    }
}