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
Затем выполните скрипт миграции из папки src/main/resources/db/migration/V1__create_tables.sql

Настройка переменных окружения

Создайте файл .env в папке src/main/resources/ со следующими переменными:

DB_URL=jdbc:postgresql://localhost:5432/otp_service
DB_USERNAME=postgres
DB_PASSWORD=your_password

JWT_SECRET=your_super_secret_key_at_least_32_characters_long

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

При первом запуске автоматически создаётся администратор. Если пароль не задан в переменных окружения, будет сгенерирован случайный и выведен в консоль. Сохраните его — он будет показан только один раз.

API Endpoints

Публичные эндпоинты

POST /api/register
Регистрация нового пользователя.
Тело запроса: username и password.
Возвращает id, username и role созданного пользователя.

POST /api/login
Вход в систему.
Тело запроса: username и password.
Возвращает JWT токен, username и role.

Пользовательские эндпоинты (требуют токен в заголовке Authorization: Bearer ТОКЕН)

POST /api/otp/generate
Генерация и отправка OTP кода.
Тело запроса: operationId (обязательно), channel (email, sms, telegram, file), destination (адрес получателя).
Возвращает operationId, channel и сообщение. Код не возвращается в ответе.

POST /api/otp/validate
Проверка OTP кода.
Тело запроса: operationId и code.
Возвращает operationId, valid (true или false) и сообщение.

Админские эндпоинты (требуют токен админа)

GET /api/admin/config
Получить текущие настройки OTP (длина кода и время жизни в секундах).

PUT /api/admin/config
Изменить настройки OTP.
Тело запроса: codeLength и ttlSeconds.
Возвращает сообщение об успехе.

GET /api/admin/users
Получить список всех обычных пользователей (без админов).

DELETE /api/admin/users/ID
Удалить пользователя по его ID.

Каналы отправки кодов

FILE - сохранение кода в файл otp_codes.txt в корне проекта.
EMAIL - отправка на почту через SMTP. Требует настройки переменных EMAIL_USERNAME, EMAIL_PASSWORD, EMAIL_FROM.
SMS - отправка через SMPP протокол. Требует запущенный эмулятор SMPPSim.
TELEGRAM - отправка через Telegram бота. Требует токен бота и chat id.

Если канал не настроен, приложение работает в режиме эмуляции и выводит код в консоль.

Тестирование Email (Эмулятор)

Для тестирования отправки email без реального SMTP сервера:

Скачайте FakeSMTP с официального сайта https://nilhcem.com/FakeSMTP/download

Или через curl:
curl -L -o fakeSMTP.jar https://github.com/Nilhcem/FakeSMTP/releases/download/v2.2/fakeSMTP-2.2.jar

Запустите эмулятор на порту 2525. В переменных окружения EMAIL оставьте пустыми — приложение автоматически переключится в режим эмуляции.

SMS (SMPP эмулятор)

Скачайте SMPPSim с sourceforge.net. Распакуйте архив и запустите startsmppsim.bat для Windows или startsmppsim.sh для Linux/Mac. Если эмулятор не запущен, приложение автоматически переключается в режим эмуляции.

Telegram

Создайте бота у BotFather в Telegram. Получите токен бота. Отправьте любое сообщение вашему боту, затем выполните запрос к API для получения chat_id. Заполните переменные TELEGRAM_BOT_TOKEN и TELEGRAM_CHAT_ID.

Структура проекта

com.example.api - HTTP обработчики запросов
com.example.config - конфигурация подключения к базе данных
com.example.dao - слой доступа к данным (Data Access Object)
com.example.model - модели данных (User, OtpCode, OtpConfig)
com.example.notification - каналы отправки уведомлений
com.example.security - утилиты для работы с JWT
com.example.service - бизнес логика приложения

Логирование

Логи пишутся в консоль и в файл logs/otp-service.log.
Формат логов: дата, время, уровень, класс, сообщение.

Автоматическая очистка кодов

При запуске сервера запускается фоновый процесс, который каждую минуту проверяет базу данных и отмечает просроченные коды статусом EXPIRED.

Запуск тестов

Запустите все тесты командой mvn test. Для генерации отчета о покрытии кода выполните mvn jacoco:report. Отчет откроется в папке target/site/jacoco.

Автор

Timur Nurzhau