-- Создание таблицы пользователей
CREATE TABLE IF NOT EXISTS users (
                                     id SERIAL PRIMARY KEY,
                                     username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('USER', 'ADMIN'))
    );

-- Создание ENUM для статусов OTP-кодов
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'code_status') THEN
CREATE TYPE code_status AS ENUM ('ACTIVE', 'EXPIRED', 'USED');
END IF;
END $$;

-- Создание таблицы конфигурации OTP
CREATE TABLE IF NOT EXISTS otp_config (
                                          id INTEGER PRIMARY KEY DEFAULT 1,
                                          code_length INTEGER NOT NULL DEFAULT 6,
                                          ttl_seconds INTEGER NOT NULL DEFAULT 300,
                                          CONSTRAINT single_row CHECK (id = 1)
    );

-- Вставка дефолтной конфигурации (если таблица пуста)
INSERT INTO otp_config (id, code_length, ttl_seconds)
VALUES (1, 6, 300)
    ON CONFLICT (id) DO NOTHING;

-- Создание таблицы OTP-кодов
CREATE TABLE IF NOT EXISTS otp_codes (
                                         id SERIAL PRIMARY KEY,
                                         user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    operation_id VARCHAR(100) NOT NULL,
    code VARCHAR(20) NOT NULL,
    status code_status NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL
    );

-- Создание индексов для быстрого поиска
CREATE INDEX IF NOT EXISTS idx_otp_codes_user_operation ON otp_codes(user_id, operation_id);
CREATE INDEX IF NOT EXISTS idx_otp_codes_status ON otp_codes(status);