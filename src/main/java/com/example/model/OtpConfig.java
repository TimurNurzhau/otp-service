package com.example.model;

/**
 * Модель конфигурации OTP-кодов.
 * Соответствует таблице otp_config в базе данных.
 * Всегда содержит только одну запись с id = 1.
 */
public class OtpConfig {

    private Integer id = 1;
    private Integer codeLength;
    private Integer ttlSeconds;

    // Конструкторы
    public OtpConfig() {
    }

    public OtpConfig(Integer codeLength, Integer ttlSeconds) {
        this.codeLength = codeLength;
        this.ttlSeconds = ttlSeconds;
    }

    // Геттеры и сеттеры
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCodeLength() {
        return codeLength;
    }

    public void setCodeLength(Integer codeLength) {
        this.codeLength = codeLength;
    }

    public Integer getTtlSeconds() {
        return ttlSeconds;
    }

    public void setTtlSeconds(Integer ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    @Override
    public String toString() {
        return "OtpConfig{" +
                "codeLength=" + codeLength +
                ", ttlSeconds=" + ttlSeconds +
                '}';
    }
}