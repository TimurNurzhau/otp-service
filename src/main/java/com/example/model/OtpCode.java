package com.example.model;

import java.time.LocalDateTime;

/**
 * Модель OTP-кода.
 * Соответствует таблице otp_codes в базе данных.
 */
public class OtpCode {

    private Long id;
    private Long userId;
    private String operationId;
    private String code;
    private CodeStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    // Статусы OTP-кода
    public enum CodeStatus {
        ACTIVE, EXPIRED, USED
    }

    // Конструкторы
    public OtpCode() {
    }

    public OtpCode(Long userId, String operationId, String code, LocalDateTime expiresAt) {
        this.userId = userId;
        this.operationId = operationId;
        this.code = code;
        this.expiresAt = expiresAt;
        this.status = CodeStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
    }

    // Геттеры и сеттеры
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public CodeStatus getStatus() {
        return status;
    }

    public void setStatus(CodeStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    // Вспомогательные методы
    public boolean isActive() {
        return status == CodeStatus.ACTIVE;
    }

    public boolean isExpired() {
        return status == CodeStatus.EXPIRED ||
                (expiresAt != null && LocalDateTime.now().isAfter(expiresAt));
    }

    public boolean isUsed() {
        return status == CodeStatus.USED;
    }

    @Override
    public String toString() {
        return "OtpCode{" +
                "id=" + id +
                ", userId=" + userId +
                ", operationId='" + operationId + '\'' +
                ", status=" + status +
                ", expiresAt=" + expiresAt +
                '}';
    }
}