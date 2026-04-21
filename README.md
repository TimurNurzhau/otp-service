https://github.com/TimurNurzhau/otp-service/actions/workflows/ci.yml/badge.svg
https://img.shields.io/badge/Java-17-blue.svg
https://img.shields.io/badge/Maven-3.6+-red.svg
https://img.shields.io/badge/PostgreSQL-17-blue.svg

OTP Service

Сервис для генерации и проверки одноразовых кодов подтверждения.

Функциональность

Регистрация и аутентификация пользователей через JWT токены

Генерация OTP кодов с настраиваемой длиной и временем жизни

Проверка OTP кодов

Отправка кодов через FILE, EMAIL, SMS и TELEGRAM

Админ панель для управления пользователями и настройками

Автоматическая очистка просроченных кодов каждую минуту

Атомарные транзакции для предотвращения race conditions

Graceful shutdown

Требования

Java 17 или новее

Maven 3.6 или новее

PostgreSQL 17 или новее

Установка и запуск

Клонирование репозитория

git clone https://github.com/TimurNurzhau/otp-service.git
cd otp-service

Настройка базы данных

Установите PostgreSQL и создайте базу данных otp_service.
Затем выполните скрипт миграции:
psql -U postgres -d otp_service -f src/main/resources/db/migration/V1__create_tables.sql

Настройка переменных окружения

Создайте файл .env в папке src/main/resources/:

DB_URL=jdbc:postgresql://localhost:5432/otp_service
DB_USERNAME=postgres
DB_PASSWORD=your_password

JWT_SECRET=your_super_secret_key_at_least_32_characters

OTP_ADMIN_USERNAME=admin (опционально)
OTP_ADMIN_PASSWORD=your_secure_password (опционально)

CORS_ALLOWED_ORIGIN=http://localhost:3000 (опционально)

EMAIL_USERNAME=your_email@gmail.com (опционально)
EMAIL_PASSWORD=your_app_password (опционально)
EMAIL_FROM=your_email@gmail.com (опционально)

TELEGRAM_BOT_TOKEN=your_bot_token (опционально)
TELEGRAM_CHAT_ID=your_chat_id (опционально)

Запуск приложения

mvn clean compile exec:java -Dexec.mainClass="com.example.api.HttpServerApp"

Сервер запустится на порту 8080.

При первом запуске автоматически создаётся администратор. Если OTP_ADMIN_PASSWORD не задан, генерируется случайный пароль и выводится в консоль. Сохраните его — он будет показан только один раз.

API Endpoints

Публичные эндпоинты

POST /api/register
Регистрация нового пользователя.
Тело: username, password.
Ответ: id, username, role.

POST /api/login
Вход в систему.
Тело: username, password.
Ответ: token, username, role.

Пользовательские эндпоинты (требуют токен в заголовке Authorization: Bearer ТОКЕН)

POST /api/otp/generate
Генерация и отправка OTP кода.
Тело: operationId (обязательно), channel (email/sms/telegram/file), destination (адрес получателя).
Ответ: operationId, channel, message.

POST /api/otp/validate
Проверка OTP кода.
Тело: operationId, code.
Ответ: operationId, valid (true/false), message.

Админские эндпоинты (требуют токен админа)

GET /api/admin/config
Получить текущие настройки OTP.

PUT /api/admin/config
Изменить настройки OTP.
Тело: codeLength (4-10), ttlSeconds (30-3600).

GET /api/admin/users
Получить список всех обычных пользователей (без админов).

DELETE /api/admin/users/{id}
Удалить пользователя по ID.

Каналы отправки кодов

FILE - сохранение кода в файл otp_codes.txt в корне проекта.
EMAIL - отправка на почту через SMTP. Требует переменные EMAIL_USERNAME, EMAIL_PASSWORD, EMAIL_FROM.
SMS - отправка через SMPP протокол. Требует запущенный эмулятор SMPPSim. Если эмулятор не доступен — автоматическая эмуляция.
TELEGRAM - отправка через Telegram бота. Требует TELEGRAM_BOT_TOKEN и TELEGRAM_CHAT_ID.

Если канал не настроен или недоступен, приложение автоматически переключается в режим эмуляции.

Тестирование каналов

Email эмулятор:

Скачайте FakeSMTP: https://nilhcem.com/FakeSMTP/download
Запустите на порту 2525. Переменные EMAIL оставьте пустыми — приложение переключится в режим эмуляции.

SMS эмулятор:

Скачайте SMPPSim: https://sourceforge.net/projects/smppsim/
Запустите startsmppsim.bat (Windows) или startsmppsim.sh (Linux/Mac).
Если эмулятор не запущен, приложение автоматически эмулирует отправку.

Telegram:

Создайте бота у @BotFather в Telegram.
Получите токен бота.
Отправьте любое сообщение боту.
Выполните запрос: https://api.telegram.org/bot{TOKEN}/getUpdates
Получите chat_id из ответа.
Заполните переменные TELEGRAM_BOT_TOKEN и TELEGRAM_CHAT_ID.

Структура проекта

com.example.api - HTTP обработчики запросов
com.example.config - конфигурация (DatabaseConnection, EnvConfig)
com.example.dao - слой доступа к данным
com.example.model - модели данных (User, OtpCode, OtpConfig)
com.example.notification - каналы отправки уведомлений
com.example.security - JWT утилиты
com.example.service - бизнес логика

Логирование

Логи пишутся в консоль и в файл logs/otp-service.log.
Формат: дата, время, уровень, класс, сообщение.

Автоматическая очистка кодов

При запуске сервера запускается фоновый процесс, который каждую минуту проверяет базу данных и отмечает просроченные коды статусом EXPIRED.

Запуск тестов

mvn test

Отчёт о покрытии кода:

mvn jacoco:report
Отчёт откроется в target/site/jacoco/index.html

CI/CD

Проект использует GitHub Actions:

Автоматическая сборка и тестирование при каждом push

Проверка на JDK 17

Запуск тестов с PostgreSQL в Docker

Генерация отчёта о покрытии кода

Автор

Timur Nurzhau