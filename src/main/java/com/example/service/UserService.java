package com.example.service;

import com.example.dao.UserDao;
import com.example.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Сервис для работы с пользователями.
 * Содержит бизнес-логику регистрации, аутентификации и управления пользователями.
 */
public class UserService {

    private final UserDao userDao;

    public UserService() {
        this.userDao = new UserDao();
    }

    /**
     * Регистрация нового пользователя
     * @param username имя пользователя
     * @param password пароль (в открытом виде)
     * @param role роль пользователя
     * @return зарегистрированный пользователь
     * @throws IllegalArgumentException если пользователь уже существует
     * @throws IllegalStateException если пытаются создать второго админа
     */
    public User register(String username, String password, User.Role role) throws SQLException {
        // Проверяем, не существует ли уже такой пользователь
        Optional<User> existing = userDao.findByUsername(username);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("User with username '" + username + "' already exists");
        }

        // Проверяем, не пытаются ли создать второго админа
        if (role == User.Role.ADMIN && userDao.adminExists()) {
            throw new IllegalStateException("Admin already exists. Cannot create second admin.");
        }

        // Хешируем пароль
        String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());

        // Создаём и сохраняем пользователя
        User user = new User(username, passwordHash, role);
        return userDao.save(user);
    }

    /**
     * Аутентификация пользователя
     * @param username имя пользователя
     * @param password пароль
     * @return пользователь, если аутентификация успешна
     * @throws IllegalArgumentException если неверный логин или пароль
     */
    public User login(String username, String password) throws SQLException {
        Optional<User> userOpt = userDao.findByUsername(username);

        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        User user = userOpt.get();

        if (!BCrypt.checkpw(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        return user;
    }

    /**
     * Получить пользователя по ID
     */
    public Optional<User> findById(Long id) throws SQLException {
        return userDao.findById(id);
    }

    /**
     * Получить пользователя по имени
     */
    public Optional<User> findByUsername(String username) throws SQLException {
        return userDao.findByUsername(username);
    }

    /**
     * Получить всех пользователей
     */
    public List<User> findAll() throws SQLException {
        return userDao.findAll();
    }

    /**
     * Получить всех пользователей (кроме админов) — для админского API
     */
    public List<User> findAllNonAdmin() throws SQLException {
        return userDao.findByRole(User.Role.USER);
    }

    /**
     * Удалить пользователя по ID
     */
    public boolean deleteUser(Long id) throws SQLException {
        return userDao.deleteById(id);
    }

    /**
     * Проверить, является ли пользователь админом
     */
    public boolean isAdmin(Long userId) throws SQLException {
        Optional<User> user = userDao.findById(userId);
        return user.isPresent() && user.get().isAdmin();
    }
}