package com.example.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

/**
 * Загрузка конфигурации из переменных окружения или файла .env
 * Приоритет: переменные окружения > файл .env > файлы .properties
 */
public class EnvConfig {

    private static final Logger logger = LoggerFactory.getLogger(EnvConfig.class);
    private static final Properties props = new Properties();

    static {
        loadEnvFile();
        loadPropertiesFiles();
    }

    /**
     * Загружает переменные из .env файла (если есть)
     */
    private static void loadEnvFile() {
        try (InputStream input = EnvConfig.class.getClassLoader()
                .getResourceAsStream(".env")) {
            if (input != null) {
                Properties envProps = new Properties();
                envProps.load(input);

                // Устанавливаем как системные свойства
                for (String key : envProps.stringPropertyNames()) {
                    String value = envProps.getProperty(key);
                    if (System.getProperty(key) == null) {
                        System.setProperty(key, value);
                        logger.debug("Loaded .env property: {}", key);
                    }
                }
                logger.info(".env file loaded successfully");
            }
        } catch (Exception e) {
            logger.debug(".env file not found, using environment variables");
        }
    }

    /**
     * Загружает .properties файлы как fallback (только для разработки)
     */
    private static void loadPropertiesFiles() {
        // Пробуем загрузить jwt.properties, но уже не критично
        try (InputStream input = EnvConfig.class.getClassLoader()
                .getResourceAsStream("jwt.properties")) {
            if (input != null) {
                props.load(input);
                logger.warn("jwt.properties loaded - NOT RECOMMENDED FOR PRODUCTION");
            }
        } catch (Exception e) {
            logger.debug("jwt.properties not found, using env variables only");
        }
    }

    /**
     * Получить значение конфигурации
     * Приоритет: System properties (от .env) > System.getenv() > .properties файлы
     */
    public static String get(String key) {
        // 1. Проверяем системные свойства (из .env)
        String value = System.getProperty(key);
        if (value != null && !value.isEmpty()) {
            return value;
        }

        // 2. Проверяем переменные окружения (переводим в верхний регистр и заменяем точки на _)
        String envKey = key.toUpperCase().replace(".", "_");
        value = System.getenv(envKey);
        if (value != null && !value.isEmpty()) {
            return value;
        }

        // 3. Проверяем другие варианты написания
        envKey = key.toUpperCase().replace(".", "__");
        value = System.getenv(envKey);
        if (value != null && !value.isEmpty()) {
            return value;
        }

        // 4. Fallback на .properties
        value = props.getProperty(key);
        if (value != null && !value.isEmpty()) {
            logger.warn("Using {} from jwt.properties - move to environment variables!", key);
            return value;
        }

        return null;
    }

    /**
     * Получить обязательное значение, если нет - exception
     */
    public static String getRequired(String key) {
        String value = get(key);
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException(
                    "Required configuration not found: " + key +
                            "\nPlease set environment variable: " + key.toUpperCase().replace(".", "_") +
                            "\nOr add to .env file in resources folder"
            );
        }
        return value;
    }

    /**
     * Получить значение с дефолтом
     */
    public static String getOrDefault(String key, String defaultValue) {
        String value = get(key);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    /**
     * Получить число
     */
    public static int getInt(String key, int defaultValue) {
        String value = get(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            logger.warn("Invalid number for {}: {}, using default {}", key, value, defaultValue);
            return defaultValue;
        }
    }
}