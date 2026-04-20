OTP SERVICE

Сервис для генерации и проверки одноразовых кодов подтверждения (OTP).

ФУНКЦИОНАЛЬНОСТЬ

Регистрация и аутентификация пользователей через JWT токены

Генерация OTP кодов с настраиваемой длиной и временем жизни

Проверка OTP кодов

Отправка кодов через FILE, EMAIL, SMS и TELEGRAM

Админ панель для управления пользователями и настройками

Автоматическая очистка просроченных кодов каждую минуту

Разграничение ролей (USER и ADMIN)

ТЕХНОЛОГИИ

Java 17

PostgreSQL 17

Maven

JWT для аутентификации

JSMTP для SMS (SMPP протокол)

Jakarta Mail для Email

Telegram Bot API

Logback для логирования

ТРЕБОВАНИЯ

Java 17 или новее

Maven 3.6 или новее

PostgreSQL 17 или новее

УСТАНОВКА И ЗАПУСК

Клонирование репозитория
git clone https://github.com/TimurNurzhau/otp-service.git
cd otp-service

Настройка базы данных
Установите PostgreSQL и создайте базу данных otp_service.
Выполните скрипт миграции из папки src/main/resources/db/migration/V1__create_tables.sql

Настройка подключения к базе данных
Отредактируйте файл src/main/resources/db.properties
Укажите url, username и password для вашей базы данных.

Запуск приложения
mvn clean compile exec:java -Dexec.mainClass="com.example.api.HttpServerApp"
Сервер запустится на порту 8080 (или 8081, если порт занят)

АДМИН ПО УМОЛЧАНИЮ

При первом запуске автоматически создается администратор:

Username: admin

Password: admin123

API ENDPOINTS

Публичные эндпоинты (без токена)

POST /api/register
Регистрация нового пользователя.
Тело запроса: username и password.

POST /api/login
Вход в систему.
Тело запроса: username и password.
Возвращает JWT токен.

Пользовательские эндпоинты (требуют токен)

POST /api/otp/generate
Генерация и отправка OTP кода.
Тело запроса: operationId, channel (email/sms/telegram/file), destination.

POST /api/otp/validate
Проверка OTP кода.
Тело запроса: operationId и code.

Админские эндпоинты (требуют токен админа)

GET /api/admin/config
Получить текущие настройки OTP.

PUT /api/admin/config
Изменить настройки OTP.
Тело запроса: codeLength и ttlSeconds.

GET /api/admin/users
Получить список всех обычных пользователей.

DELETE /api/admin/users/ID
Удалить пользователя по его ID.

ТЕСТИРОВАНИЕ В POWERSHELL

Логин администратора

$response = Invoke-WebRequest -Uri "http://localhost:8081/api/login" -Method POST -ContentType "application/json" -Body '{"username":"admin","password":"admin123"}'
$token = ($response.Content | ConvertFrom-Json).data.token

Получение конфигурации OTP

Invoke-WebRequest -Uri "http://localhost:8081/api/admin/config" -Method GET -Headers @{Authorization = "Bearer $token"}

Обновление конфигурации OTP (8 цифр, 10 минут)

Invoke-WebRequest -Uri "http://localhost:8081/api/admin/config" -Method PUT -ContentType "application/json" -Headers @{Authorization = "Bearer $token"} -Body '{"codeLength":8,"ttlSeconds":600}'

Регистрация обычного пользователя

Invoke-WebRequest -Uri "http://localhost:8081/api/register" -Method POST -ContentType "application/json" -Body '{"username":"user","password":"pass123"}'

Логин обычного пользователя

$userResponse = Invoke-WebRequest -Uri "http://localhost:8081/api/login" -Method POST -ContentType "application/json" -Body '{"username":"user","password":"pass123"}'
$userToken = ($userResponse.Content | ConvertFrom-Json).data.token

Генерация OTP кода

$otpResponse = Invoke-WebRequest -Uri "http://localhost:8081/api/otp/generate" -Method POST -ContentType "application/json" -Headers @{Authorization = "Bearer $userToken"} -Body '{"operationId":"test","channel":"file","destination":"user"}'
$code = ($otpResponse.Content | ConvertFrom-Json).data.code

Проверка OTP кода

Invoke-WebRequest -Uri "http://localhost:8081/api/otp/validate" -Method POST -ContentType "application/json" -Headers @{Authorization = "Bearer $userToken"} -Body "{"operationId":"test","code":"$code"}"

Список пользователей (только для админа)

Invoke-WebRequest -Uri "http://localhost:8081/api/admin/users" -Method GET -Headers @{Authorization = "Bearer $token"}

Попытка доступа обычного пользователя к админ-API (должна вернуть 403)

Invoke-WebRequest -Uri "http://localhost:8081/api/admin/config" -Method GET -Headers @{Authorization = "Bearer $userToken"}

КАНАЛЫ ОТПРАВКИ КОДОВ

FILE - сохранение кода в файл otp_codes.txt в корне проекта. Работает без настройки.

EMAIL - отправка на почту через SMTP. Требует настройки email.properties.

SMS - отправка через SMPP протокол. Требует запущенный эмулятор SMPPSim и настройки sms.properties.

TELEGRAM - отправка через Telegram бота. Требует токен бота и chat id в telegram.properties.

Если канал не настроен, приложение работает в режиме эмуляции и выводит код в консоль.

НАСТРОЙКА КАНАЛОВ

Для Email:
Отредактируйте src/main/resources/email.properties, укажите свои данные SMTP.

Для SMS:
Скачайте и запустите эмулятор SMPPSim.
Отредактируйте src/main/resources/sms.properties.

Для Telegram:
Создайте бота через @BotFather, получите токен.
Напишите боту любое сообщение.
Выполните запрос https://api.telegram.org/bot/ВАШ_ТОКЕН/getUpdates
Найдите chat id и укажите в telegram.properties.

СТРУКТУРА ПРОЕКТА

com.example.api - HTTP обработчики запросов
com.example.config - конфигурация подключения к базе данных
com.example.dao - слой доступа к данным (Data Access Object)
com.example.model - модели данных (User, OtpCode, OtpConfig)
com.example.notification - каналы отправки уведомлений
com.example.security - утилиты для работы с JWT
com.example.service - бизнес логика приложения

ЛОГИРОВАНИЕ

Логи пишутся в консоль и в файл logs/otp-service.log.
Формат логов: дата, время, уровень, класс, сообщение.

АВТОМАТИЧЕСКАЯ ОЧИСТКА КОДОВ

При запуске сервера запускается фоновый процесс, который каждую минуту проверяет базу данных и отмечает просроченные коды статусом EXPIRED.

АВТОР

Timur Nurzhau