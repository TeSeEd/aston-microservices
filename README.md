# Aston Microservices

Учебный микросервисный проект на **Java 21, Spring Boot 3.5.6 и Spring Cloud 2025.0.3**.

Проект реализует управление пользователями, обработку событий через Apache Kafka и отправку email-уведомлений. В системе используются PostgreSQL, Transactional Outbox, REST API, Springdoc OpenAPI, HATEOAS и HAL.

В рамках домашнего задания №7 добавлены четыре микросервисных паттерна:

- **API Gateway** — единая точка входа в REST API.
- **Service Discovery** — обнаружение микросервисов через Netflix Eureka.
- **Circuit Breaker** — защита от сбоев через Resilience4j.
- **External Configuration** — централизованная конфигурация через Spring Cloud Config Server.

Для примера используется название сайта **AstonHub**.

## Возможности

- Создание, получение, обновление и удаление пользователей.
- Валидация данных и централизованная обработка ошибок.
- Документирование REST API через Springdoc OpenAPI и Swagger UI.
- Поддержка HATEOAS и HAL.
- Отправка событий Kafka при создании и удалении пользователей.
- Transactional Outbox для надёжной публикации событий.
- Повторные попытки отправки событий.
- Обработка Kafka-событий и защита от дубликатов.
- Отправка email-уведомлений через SMTP.
- Повторные попытки отправки писем.
- REST API для отправки произвольного письма.
- Регистрация и обнаружение микросервисов в Eureka.
- Маршрутизация HTTP-запросов через API Gateway.
- Circuit Breaker с fallback-ответом при недоступности сервиса уведомлений.
- Загрузка внешней конфигурации через Config Server.
- Запуск инфраструктуры через Docker Compose.

## Технологии

| Категория | Технологии |
|---|---|
| Язык | Java 21 |
| Фреймворк | Spring Boot 3.5.6 |
| Микросервисы | Spring Cloud 2025.0.3 |
| Service Discovery | Netflix Eureka |
| API Gateway | Spring Cloud Gateway WebFlux |
| Балансировка | Spring Cloud LoadBalancer |
| Отказоустойчивость | Spring Cloud Circuit Breaker, Resilience4j |
| Конфигурация | Spring Cloud Config Server, Config Client |
| REST API | Spring Web |
| Базы данных | PostgreSQL 17, Spring Data JPA, Hibernate |
| Миграции | Flyway |
| Обмен событиями | Apache Kafka, Spring Kafka |
| Документация | Springdoc OpenAPI, Swagger UI |
| Гипермедиа | Spring HATEOAS, HAL |
| Почта | Spring Mail, SMTP |
| Тестирование | JUnit 5, Mockito, MockMvc, Testcontainers 2.0.5, GreenMail |
| Сборка | Maven |
| Контейнеризация | Docker Compose |
| Тестовый SMTP | Mailpit |

## Архитектура

### Общая схема

```text
                       HTTP Client
                           |
                           v
                  API Gateway :8082
                           |
                  +--------+--------+
                  |                 |
                  v                 v
           user-service      notification-service
              :8080                :8081
                  |                 |
                  v                 v
          PostgreSQL          PostgreSQL
                  |
                  v
         Transactional Outbox
                  |
                  v
             Apache Kafka
                  |
                  v
         notification-service
                  |
                  v
               Mailpit


       +--------------------------+
       | Eureka Discovery Server  |
       |          :8761           |
       +--------------------------+
                  ^
                  |
         Регистрация и поиск
             сервисов


       +--------------------------+
       | Spring Cloud Config      |
       | Server :8888             |
       +--------------------------+
                  |
                  v
             config-repo/
                  |
         Внешние конфигурации
         микросервисов
```

`user-service` и `notification-service` обмениваются событиями асинхронно через Kafka. Gateway используется для входящих HTTP-запросов.

### Структура проекта

Проект состоит из шести Maven-модулей.

```text
aston-microservices/
├── event-contracts/
├── user-service/
├── notification-service/
├── discovery-server/
├── api-gateway/
├── config-server/
├── config-repo/
│   ├── user-service.properties
│   ├── notification-service.properties
│   └── api-gateway.properties
├── docker-compose.yml
└── pom.xml
```

**event-contracts** — общие контракты событий Kafka.

**user-service** — REST API пользователей, бизнес-логика, HATEOAS, Transactional Outbox и публикация Kafka-событий.

**notification-service** — обработка Kafka-событий, сохранение уведомлений и отправка email. Также предоставляет HTTP API для отправки произвольных писем.

**discovery-server** — сервер Netflix Eureka для регистрации и обнаружения микросервисов.

**api-gateway** — единая точка входа для HTTP-клиентов, маршрутизация через Eureka, балансировка и Circuit Breaker.

**config-server** — сервер централизованной конфигурации на базе Spring Cloud Config.

**config-repo** — внешние файлы настроек микросервисов.

---

## Микросервисные паттерны

### 1. Service Discovery — Netflix Eureka

Eureka Server работает на порту `8761`.

Каждый микросервис регистрируется в Eureka под своим `spring.application.name`.

Зарегистрированные приложения:

- `USER-SERVICE`
- `NOTIFICATION-SERVICE`
- `API-GATEWAY`

Адрес панели управления:

http://localhost:8761

Gateway использует Eureka для поиска экземпляров сервисов по логическим именам, а не по жёстко заданным IP-адресам.

Для контейнерного запуска используется адрес:

```properties
eureka.client.service-url.defaultZone=http://discovery-server:8761/eureka/
```

Имя `discovery-server` разрешается внутри сети Docker Compose.

### 2. API Gateway — Spring Cloud Gateway

Gateway работает на порту `8082` и предоставляет единую точку входа.

| Путь | Целевой сервис |
|---|---|
| `/api/users/**` | `lb://user-service` |
| `/api/notifications/**` | `lb://notification-service` |

Префикс `lb://` означает использование Spring Cloud LoadBalancer совместно с Service Discovery.

Пример получения пользователей:

```http
GET http://localhost:8082/api/users
```

Пример получения пользователя:

```http
GET http://localhost:8082/api/users/1
```

Пример отправки письма:

```http
POST http://localhost:8082/api/notifications/email
```

Прямые адреса `localhost:8080` и `localhost:8081` оставлены доступными для локальной разработки и диагностики.

#### HATEOAS через Gateway

Gateway передаёт информацию об исходном HTTP-запросе с помощью forwarded headers.

В `user-service` используется настройка:

```properties
server.forward-headers-strategy=framework
```

Это позволяет формировать публичные HATEOAS-ссылки через Gateway:

```json
{
  "_links": {
    "self": {
      "href": "http://localhost:8082/api/users/1"
    },
    "users": {
      "href": "http://localhost:8082/api/users"
    }
  }
}
```

Таким образом, клиенту не требуется знать внутренние адреса Docker-контейнеров.

Настройка доверенных прокси в Gateway предназначена для локальной Docker-среды. При публикации системы во внешней сети её необходимо пересмотреть.

### 3. Circuit Breaker — Resilience4j

Circuit Breaker подключён к маршруту `notification-service` на уровне API Gateway.

Используются:

- Spring Cloud Circuit Breaker.
- Resilience4j.
- Реактивная интеграция с WebFlux.
- Fallback-обработчик `FallbackController`.

Основные параметры:

| Параметр | Значение |
|---|---|
| slidingWindowSize | 5 |
| minimumNumberOfCalls | 3 |
| failureRateThreshold | 50% |
| waitDurationInOpenState | 10 секунд |
| permittedNumberOfCallsInHalfOpenState | 2 |
| timeoutDuration | 3 секунды |

#### Состояния Circuit Breaker

**CLOSED** — обычная работа, запросы передаются в `notification-service`.

**OPEN** — после достижения порога ошибок новые обращения временно блокируются.

**HALF_OPEN** — после периода ожидания разрешаются пробные запросы для проверки восстановления.

При сбое используется fallback:

```http
HTTP/1.1 503 Service Unavailable
```

```json
{
  "message": "Notification service temporarily unavailable"
}
```

#### Проверка отказоустойчивости

Сначала проверяется успешная отправка письма через Gateway с ответом `202 Accepted`.

Затем останавливается сервис уведомлений:

```powershell
docker compose stop notification-service
```

Выполняются повторные POST-запросы через Gateway.

При отказе возвращается HTTP `503`, а в логах Gateway было подтверждено состояние `OPEN`:

```text
CallNotPermittedException:
CircuitBreaker 'notificationCircuitBreaker' is OPEN
and does not permit further calls
```

После запуска сервиса:

```powershell
docker compose start notification-service
```

и обновления сведений Eureka повторные HTTP-запросы успешно возвращали `202 Accepted`.

Таким образом, вручную проверены обработка отказа, переход в `OPEN` и восстановление успешной обработки запросов. Автоматизированная проверка всех переходов состояний Circuit Breaker пока не добавлена.

### 4. External Configuration — Spring Cloud Config Server

Config Server работает на порту `8888`.

Для учебного проекта используется **native profile** — внешние файлы конфигурации хранятся в локальном каталоге `config-repo`, а не в отдельном Git-репозитории.

```text
config-repo/
├── user-service.properties
├── notification-service.properties
└── api-gateway.properties
```

Config Server читает этот каталог через Docker volume в режиме только для чтения:

```yaml
volumes:
  - ./config-repo:/app/config-repo:ro
```

Адреса проверки:

- http://localhost:8888/user-service/default
- http://localhost:8888/notification-service/default
- http://localhost:8888/api-gateway/default

Например, файл `user-service.properties` содержит:

```properties
app.config-source=external-config-server

eureka.client.service-url.defaultZone=${EUREKA_SERVER_URL:http://localhost:8761/eureka/}
```

Микросервисы используют Spring Cloud Config Client для загрузки внешних параметров при запуске.

Пример настройки клиента:

```properties
spring.config.import=optional:configserver:${CONFIG_SERVER_URL:http://localhost:8888}
```

В Docker Compose адрес Config Server задаётся через переменную окружения:

```yaml
CONFIG_SERVER_URL: http://config-server:8888
```

Для Config Server настроен HTTP healthcheck через Spring Boot Actuator.

Зависимые контейнеры используют:

```yaml
depends_on:
  config-server:
    condition: service_healthy
```

Это позволяет дождаться готовности Config Server перед запуском клиентов при обычном запуске через Docker Compose.

#### Проверка загрузки конфигурации

Пример успешной загрузки:

```text
Fetching config from server at : http://config-server:8888
Located environment: name=user-service, profiles=[default]
```

Подключение проверено для `user-service`, `notification-service` и `api-gateway`.

На текущем этапе во внешнюю конфигурацию вынесены параметры Eureka и тестовый параметр `app.config-source`. Остальные настройки, включая параметры БД, Kafka и SMTP, продолжают использовать существующую конфигурацию и переменные окружения.

Благодаря `optional:configserver:` клиент может продолжить запуск с локальными настройками, если Config Server недоступен. Это поведение предназначено для текущей учебной конфигурации.

---

## Обмен событиями через Kafka

### Как работают уведомления

```text
REST API
   |
   v
user-service
   |
   v
PostgreSQL (Outbox)
   |
   v
Apache Kafka
   |
   v
notification-service
   |
   v
PostgreSQL (Notifications)
   |
   v
SMTP (Mailpit)
```

При создании пользователя формируется событие `USER_CREATED`, при удалении — `USER_DELETED`.

События сначала сохраняются в базе данных. Если Kafka недоступна, публикация повторяется.

`notification-service` сохраняет уведомления и выполняет повторные попытки отправки при ошибках SMTP.

### Содержание писем

При создании пользователя отправляется приветственное письмо:

**Тема:** Добро пожаловать в AstonHub!

**Текст:**

> Здравствуйте! Ваш аккаунт на сайте AstonHub был успешно создан.

При удалении пользователя:

**Тема:** Удаление аккаунта в AstonHub

**Текст:**

> Здравствуйте! Ваш аккаунт на сайте AstonHub был удалён.

---

## Запуск через Docker Compose

### Требования

Для запуска необходимы:

- Docker Desktop.
- Docker Compose.

Для локальной Maven-сборки также нужны Java 21 и Maven.

### Запуск

Из корневой папки проекта:

```powershell
docker compose up -d --build
```

Docker Compose запускает девять контейнеров.

| Сервис | Назначение | Порт на localhost |
|---|---|---|
| api-gateway | Единая точка входа REST API | 8082 |
| discovery-server | Eureka Server | 8761 |
| config-server | Централизованная конфигурация | 8888 |
| user-service | Управление пользователями | 8080 |
| notification-service | Уведомления | 8081 |
| user-postgres | База пользователей | 5432 |
| notification-postgres | База уведомлений | 5433 |
| kafka | Брокер сообщений | 9092 |
| mailpit | Тестовый SMTP-сервер | 1025, 8025 |

Проверка состояния контейнеров:

```powershell
docker compose ps
```

Просмотр логов:

```powershell
docker compose logs -f api-gateway
docker compose logs -f config-server
docker compose logs -f discovery-server
docker compose logs -f user-service
docker compose logs -f notification-service
```

### Доступные адреса

| Сервис | Адрес |
|---|---|
| API Gateway — пользователи | http://localhost:8082/api/users |
| API Gateway — уведомления | http://localhost:8082/api/notifications/email |
| Eureka Dashboard | http://localhost:8761 |
| Config Server | http://localhost:8888/user-service/default |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| Mailpit | http://localhost:8025 |

Swagger UI и OpenAPI JSON доступны напрямую через `user-service`. Отдельные маршруты Gateway для них не настраивались.

Mailpit принимает тестовые письма локально и не отправляет их на реальные почтовые адреса.

---

## REST API пользователей

Основной адрес через Gateway:

`http://localhost:8082/api/users`

| Метод | Endpoint | Описание | Успешный статус |
|---|---|---|---|
| POST | `/api/users` | Создание пользователя | 201 Created |
| GET | `/api/users` | Список пользователей | 200 OK |
| GET | `/api/users/{id}` | Получение по ID | 200 OK |
| PUT | `/api/users/{id}` | Обновление пользователя | 200 OK |
| DELETE | `/api/users/{id}` | Удаление пользователя | 204 No Content |

Для некорректных данных предусмотрены ответы `400 Bad Request`, для отсутствующего пользователя — `404 Not Found`.

Пример структуры ошибки:

```json
{
  "status": 404,
  "message": "Пользователь с id: 999 не найден",
  "timestamp": "2026-10-26T12:00:00"
}
```

### Создание пользователя

Пример JSON:

```json
{
  "name": "Ivan",
  "email": "ivan@example.com",
  "age": 25
}
```

Пример запроса через PowerShell:

```powershell
$body = @{
    name = "Ivan"
    email = "ivan@example.com"
    age = 25
} | ConvertTo-Json

Invoke-RestMethod `
    -Uri "http://localhost:8082/api/users" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body
```

При успешном создании возвращается `201 Created`, заголовок `Location` со ссылкой на созданного пользователя и JSON с HATEOAS-ссылками.

После создания пользователя формируется событие `USER_CREATED`, которое передаётся через Kafka в `notification-service`.

При удалении пользователя аналогично формируется `USER_DELETED`.

---

## Swagger и OpenAPI

Документирование REST API `user-service` выполняется через Springdoc OpenAPI.

Swagger UI:

http://localhost:8080/swagger-ui.html

OpenAPI JSON:

http://localhost:8080/v3/api-docs

Документация содержит:

- Описания CRUD-операций.
- Параметры и тела запросов.
- HTTP-статусы успешных ответов и ошибок.
- Схемы `UserRequestDto` и `ErrorResponseDto`.
- Модели HATEOAS-ответов отдельного пользователя и коллекции.

Для описания HATEOAS используются документационные модели:

- `UserHateoasResponseDoc`.
- `UserCollectionHateoasResponseDoc`.

Они предназначены для генерации документации и не заменяют реальные DTO или Spring HATEOAS-модели.

---

## HATEOAS и HAL

REST API пользователей поддерживает HATEOAS (*Hypermedia as the Engine of Application State*) с использованием Spring HATEOAS.

Применяются:

- `EntityModel<UserResponseDto>` — отдельный пользователь со ссылками.
- `CollectionModel<EntityModel<UserResponseDto>>` — коллекция пользователей.
- `WebMvcLinkBuilder` — формирование ссылок на методы контроллера.

### Получение пользователя

**GET** `/api/users/1`

Пример ответа через Gateway:

```json
{
  "id": 1,
  "name": "Ivan",
  "email": "ivan@example.com",
  "age": 25,
  "createdAt": "2026-10-26T12:00:00",
  "_links": {
    "self": {
      "href": "http://localhost:8082/api/users/1"
    },
    "users": {
      "href": "http://localhost:8082/api/users"
    }
  }
}
```

Ссылка `self` указывает на текущего пользователя, а `users` — на коллекцию пользователей.

### Получение списка пользователей

**GET** `/api/users`

Пример HAL-ответа:

```json
{
  "_embedded": {
    "userResponseDtoList": [
      {
        "id": 1,
        "name": "Ivan",
        "email": "ivan@example.com",
        "age": 25,
        "createdAt": "2026-10-26T12:00:00",
        "_links": {
          "self": {
            "href": "http://localhost:8082/api/users/1"
          },
          "users": {
            "href": "http://localhost:8082/api/users"
          }
        }
      }
    ]
  },
  "_links": {
    "self": {
      "href": "http://localhost:8082/api/users"
    }
  }
}
```

Поле `_embedded` содержит пользователей, каждый из которых имеет собственные HATEOAS-ссылки.

Поле `_links` на верхнем уровне описывает ссылки самой коллекции.

Если пользователей нет, коллекция возвращается с HTTP `200 OK` и ссылкой `self`; блок `_embedded` может отсутствовать.

**Важно:** `GET /api/users` возвращает HAL-объект, а не корневой JSON-массив.

### HATEOAS в остальных операциях

- `POST /api/users` — `201 Created`, данные и ссылки `self`, `users`.
- `PUT /api/users/{id}` — `200 OK`, обновлённые данные и ссылки.
- `DELETE /api/users/{id}` — `204 No Content`, без тела ответа.

Бизнес-логика сервисного слоя при внедрении HATEOAS не изменялась.

---

## REST API уведомлений

`notification-service` позволяет отправить письмо напрямую через REST API, без Kafka.

**POST** `/api/notifications/email`

Основной адрес:

`http://localhost:8082/api/notifications/email`

Пример JSON:

```json
{
  "to": "test@example.com",
  "subject": "Тестовое письмо",
  "body": "Привет! Это проверка отправки письма."
}
```

Пример запроса через PowerShell:

```powershell
$body = @{
    to = "test@example.com"
    subject = "Test email"
    body = "Hello from AstonHub"
} | ConvertTo-Json

Invoke-WebRequest `
    -Uri "http://localhost:8082/api/notifications/email" `
    -Method Post `
    -ContentType "application/json; charset=utf-8" `
    -Body ([System.Text.Encoding]::UTF8.GetBytes($body))
```

При успешном принятии запроса возвращается HTTP `202 Accepted`.

Письмо можно проверить в Mailpit:

http://localhost:8025

---

## Базы данных

Для каждого бизнес-микросервиса используется отдельная база PostgreSQL.

### user-service

- `users` — пользователи.
- `outbox_events` — события для публикации в Kafka.

### notification-service

- `notifications` — уведомления и их статусы.
- `processed_user_events` — обработанные события для защиты от повторной обработки.

Для управления схемами используется Flyway. Миграции применяются при запуске приложений.

### Подключение к PostgreSQL

База пользователей:

```powershell
docker compose exec user-postgres psql -U user_service_user -d user_service
```

База уведомлений:

```powershell
docker compose exec notification-postgres psql -U notification_service_user -d notification_service
```

Примеры SQL:

```sql
SELECT * FROM users;
SELECT * FROM outbox_events;
SELECT * FROM notifications;
```

Каждый запрос необходимо выполнять в соответствующей базе данных.

---

## Тестирование

В проекте используются JUnit 5, Mockito, MockMvc, Testcontainers и GreenMail.

Проверяются:

- Бизнес-логика управления пользователями.
- REST API и валидация данных.
- HTTP-статусы и обработка ошибок.
- HATEOAS-ссылки и HAL-ответы.
- Работа с PostgreSQL.
- Сохранение событий в Transactional Outbox.
- Публикация Kafka-событий.
- Обработка событий и защита от дубликатов.
- Транзакционность обработки событий.
- Работа репозитория уведомлений.
- Отправка электронных писем.

### Тестирование HATEOAS

В `UserControllerTest` используются MockMvc и Mockito.

Проверяются:

- `POST /api/users` — статус `201`, `Location`, поля пользователя и ссылки.
- `GET /api/users/{id}` — данные пользователя и ссылки.
- `GET /api/users` — структура `_embedded.userResponseDtoList`.
- Пустой список — `200 OK`, отсутствие `_embedded`, ссылка `self`.
- `PUT /api/users/{id}` — обновление данных и ссылок.
- `DELETE /api/users/{id}` — `204 No Content`.
- Обработка некорректных запросов и отсутствующих пользователей.

Отдельный запуск:

```powershell
mvn -pl user-service -am "-Dtest=UserControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

### Интеграционные тесты SMTP

Для тестирования электронной почты используется GreenMail.

Проверяются три сценария:

1. Отправка приветственного письма при `USER_CREATED`.
2. Отправка письма при `USER_DELETED`.
3. Отправка произвольного письма через REST API.

Тесты проверяют получателя, тему и содержимое сообщения.

Отдельный запуск:

```powershell
mvn -pl notification-service -am "-Dtest=EmailIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

### Полная Maven-проверка

Из корня проекта:

```powershell
mvn clean verify
```

Для интеграционных тестов с Testcontainers необходим работающий Docker.

**Последний подтверждённый успешный прогон: 1 ноября 2026 года.**

```text
[INFO] Reactor Summary:
[INFO]
[INFO] event-contracts 1.0.0 ................ SUCCESS
[INFO] user-service 1.0-SNAPSHOT ............ SUCCESS
[INFO] notification-service 0.0.1-SNAPSHOT .. SUCCESS
[INFO] discovery-server 1.0.0 ............... SUCCESS
[INFO] api-gateway 1.0.0 .................... SUCCESS
[INFO] config-server 1.0.0 .................. SUCCESS
[INFO] aston-microservices 1.0.0 ............ SUCCESS
[INFO]
[INFO] BUILD SUCCESS
[INFO] Total time: 01:38 min
```

Все шесть Maven-модулей успешно прошли полный цикл `mvn clean verify`.

### Ручные проверки домашнего задания №7

Дополнительно проверены:

| Проверка | Результат |
|---|---|
| Регистрация сервисов в Eureka | Успешно |
| Маршрутизация `/api/users` через Gateway | HTTP 200 |
| HATEOAS-ссылки через Gateway | Корректный адрес `localhost:8082` |
| Отправка письма через Gateway | HTTP 202 |
| Fallback при отказе notification-service | HTTP 503 |
| Переход Circuit Breaker в OPEN | Подтверждён логами |
| Восстановление после запуска сервиса | HTTP 202 |
| Работа Config Server в Docker | Подтверждена |
| Получение внешней конфигурации клиентами | Подтверждено |
| Полный запуск Docker Compose | Успешно |

Автоматизированные тесты, специально проверяющие Eureka, Gateway, Circuit Breaker и Config Server, пока не добавлены; эти сценарии проверялись вручную.

---

## Остановка проекта

Остановка контейнеров:

```powershell
docker compose down
```

Данные PostgreSQL и Kafka сохраняются в Docker volumes.

Остановка с удалением volumes:

```powershell
docker compose down -v
```

**Внимание:** параметр `-v` удаляет данные в Docker volumes, используемых проектом. Использовать его следует только тогда, когда необходимо удалить сохранённые данные.

## Локальный запуск

Для локальной разработки необходимы Java 21, Maven, PostgreSQL, Kafka и SMTP-сервер.

Микросервисы можно запускать отдельно через IntelliJ IDEA или Maven с соответствующими настройками подключения к инфраструктуре.

Для локального запуска Spring Cloud-компонентов используются порты:

```text
discovery-server      8761
config-server         8888
api-gateway           8082
user-service          8080
notification-service  8081
```

При запуске Config Server через Maven из каталога модуля файловый путь для native profile:

```properties
spring.cloud.config.server.native.search-locations=file:../config-repo/
```

В Docker используется путь `/app/config-repo/`, подключённый через volume.

Основной и рекомендуемый способ запуска всего учебного проекта — **Docker Compose**.

---

## Результат

В проекте реализованы четыре микросервисных паттерна Spring Cloud:

1. **Service Discovery** — регистрация и обнаружение сервисов через Eureka.
2. **API Gateway** — маршрутизация HTTP-запросов через единый вход.
3. **Circuit Breaker** — обработка отказов и восстановление через Resilience4j.
4. **External Configuration** — централизованное предоставление настроек через Config Server.

Существующие механизмы Kafka, Transactional Outbox, REST API, Swagger, HATEOAS, PostgreSQL и SMTP сохранены.

Проект успешно собирается командой `mvn clean verify` и запускается через Docker Compose.