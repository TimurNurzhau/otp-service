package com.example.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Класс для управления подключением к базе данных.
 * Загружает настройки из db.properties и создаёт соединения.
 */
public class DatabaseConnection {

    private static final String PROPERTIES_FILE = "db.properties";
    private static String url;
    private static String username;
    private static String password;

    // Статический блок - выполняется при загрузке класса
    static {
        loadProperties();
    }

    /**
     * Загружает настройки подключения из файла db.properties
     */
    private static void loadProperties() {
        try (InputStream input = DatabaseConnection.class.getClassLoader()
                .getResourceAsStream(PROPERTIES_FILE)) {

            if (input == null) {
                throw new RuntimeException("Не найден файл " + PROPERTIES_FILE);
            }

            Properties props = new Properties();
            props.load(input);

            url = props.getProperty("db.url");
            username = props.getProperty("db.username");
            password = props.getProperty("db.password");

            // Регистрируем драйвер PostgreSQL
            Class.forName("org.postgresql.Driver");

            System.out.println("Настройки БД загружены успешно");

        } catch (IOException e) {
            throw new RuntimeException("Ошибка при чтении файла " + PROPERTIES_FILE, e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Драйвер PostgreSQL не найден", e);
        }
    }

    /**
     * Создаёт новое соединение с базой данных
     * @return Connection объект
     * @throws SQLException если не удалось подключиться
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    /**
     * Проверяет, доступна ли база данных
     * @return true если подключение успешно
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("Ошибка подключения к БД: " + e.getMessage());
            return false;
        }
    }
}