OTP Service

Сервис для генерации и проверки одноразовых кодов подтверждения.

Функциональность

- Регистрация и аутентификация пользователей через JWT токены
- Генерация OTP кодов с настраиваемой длиной и временем жизни
- Проверка OTP кодов
- Отправка кодов через FILE, EMAIL, SMS и TELEGRAM
- Админ панель для управления пользователями и настройками
- Автоматическая очистка просроченных кодов каждую минуту

Требования

- Java 17 или новее
- Maven 3.6 или новее
- PostgreSQL 17 или новее

Установка и запуск

1. Клонирование репозитория

   git clone https://github.com/TimurNurzhau/otp-service.git
   cd otp-service

2. Настройка базы данных

   Установите PostgreSQL и создайте базу данных otp_service.
   Затем выполните скрипт миграции из папки src/main/resources/db/migration/V1__create_tables.sql

3. Настройка подключения к базе данных

   Отредактируйте файл src/main/resources/db.properties
   Укажите url, username и password для вашей базы данных.

4. Запуск приложения

   mvn clean compile exec:java -Dexec.mainClass="com.example.api.HttpServerApp"

   Сервер запустится на порту 8080.

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
Возвращает operationId, channel, code и сообщение.

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
EMAIL - отправка на почту через SMTP. Требует настройки email.properties.
SMS - отправка через SMPP протокол. Требует запущенный эмулятор SMPPSim и настройки sms.properties.
TELEGRAM - отправка через Telegram бота. Требует токен бота и chat id в telegram.properties.

Если канал не настроен, приложение работает в режиме эмуляции и выводит код в консоль.

Конфигурационные файлы

Все конфигурационные файлы находятся в папке src/main/resources/

db.properties - настройки подключения к PostgreSQL
email.properties - настройки SMTP сервера для отправки почты
sms.properties - настройки SMPP для отправки SMS
telegram.properties - токен бота и chat id для Telegram
logback.xml - настройки логирования

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

Автор

Timur Nurzhau