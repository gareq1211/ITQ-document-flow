# Document Flow Service

[![Java](https://img.shields.io/badge/Java-17-blue.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-blue.svg)](https://www.docker.com/)
[![Gradle](https://img.shields.io/badge/Gradle-8.x-lightblue.svg)](https://gradle.org/)

Сервис для управления документами с историей статусов и реестром утверждений.

## Стек технологий

- **Java 17**
- **Spring Boot 3.1.5** (Web, Data JPA, Validation)
- **PostgreSQL 15**
- **Gradle 8.x**
- **Liquibase** (миграции БД)
- **Docker Compose** (контейнеризация)
- **JUnit 5 / Testcontainers** (тестирование)

## Архитектура
┌─────────────┐ ┌──────────────┐ ┌─────────────┐
│ Клиент │────▶│ Контроллер │────▶│ Сервис │
└─────────────┘ └──────────────┘ └──────┬──────┘
│
▼
┌─────────────┐ ┌──────────────┐ ┌─────────────┐
│ PostgreSQL │◀────│ Репозиторий │◀────│ Модели │
└─────────────┘ └──────────────┘ └─────────────┘

## Функциональность

- Создание документов (статус DRAFT)
- Отправка на согласование (DRAFT → SUBMITTED)
- Утверждение документов (SUBMITTED → APPROVED)
- История изменений по каждому документу
- Реестр утверждённых документов
- Пакетная обработка (до 1000 документов за запрос)
- Поиск с фильтрацией (статус, автор, дата)
- Фоновые воркеры для автоматической обработки
- Конкурентное тестирование утверждения
- Утилита для массовой генерации документов

## Быстрый старт

### Предварительные требования

- [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- [Java 17+](https://adoptium.net/)
- [Gradle](https://gradle.org/install/) (или используйте wrapper)

### Запуск с Docker Compose

```bash
# Клонировать репозиторий
git clone https://github.com/gareq1211/ITQ-document-flow.git
cd ITQ-document-flow

# Запустить PostgreSQL и pgAdmin
docker-compose up -d

# Запустить приложение (в отдельном терминале)
cd service
./gradlew bootRun

Приложение будет доступно по адресу: http://localhost:8080
API Endpoints
Документы
Метод	URL	Описание
POST	/api/documents	Создать новый документ
GET	/api/documents/{id}	Получить документ с историей
GET	/api/documents?ids=1,2,3&page=0&size=20	Получить документы по списку ID
POST	/api/documents/submit	Отправить на согласование
POST	/api/documents/approve	Утвердить документы
GET	/api/documents/search	Поиск документов
Тестирование
Метод	URL	Описание
POST	/api/test/concurrent-approve/{id}	Тест конкурентного утверждения
Примеры запросов
Создание документа
bash
curl -X POST http://localhost:8080/api/documents \
  -H "Content-Type: application/json" \
  -d '{"author":"Иван Петров","title":"Отчет по проекту"}'
Ответ:

json
{
  "id": 1,
  "uniqueNumber": "DOC-12345678",
  "author": "Иван Петров",
  "title": "Отчет по проекту",
  "status": "DRAFT",
  "createdAt": "2024-01-01T12:00:00",
  "updatedAt": "2024-01-01T12:00:00",
  "history": []
}
Отправка на согласование
bash
curl -X POST http://localhost:8080/api/documents/submit \
  -H "Content-Type: application/json" \
  -d '{"ids":[1,2,3],"userId":"user123","comment":"На проверку"}'
Ответ:

json
[
  {
    "documentId": 1,
    "status": "SUCCESS",
    "message": "Document submitted successfully"
  },
  {
    "documentId": 2,
    "status": "SUCCESS",
    "message": "Document submitted successfully"
  }
]
Поиск документов
bash
curl "http://localhost:8080/api/documents/search?status=DRAFT&author=Иван&dateFrom=2024-01-01T00:00:00&dateTo=2024-12-31T23:59:59&page=0&size=10"
Утилита для генерации документов
Сборка
bash
cd generator-utility
./gradlew build
Конфигурация (config.properties)
properties
service.url=http://localhost:8080
total.documents=1000
batch.size=10
threads=5
authors.file=authors.txt
Запуск
bash
java -jar build/libs/generator-utility-1.0.0.jar config.properties
Фоновые воркеры
Сервис автоматически обрабатывает документы:

SubmitWorker — каждые 10 секунд отправляет DRAFT документы на согласование

ApproveWorker — каждые 10 секунд утверждает SUBMITTED документы

Настройка в application.yml:

yaml
worker:
  batch-size: 100
  submit:
    interval: 10000
  approve:
    interval: 10000
Мониторинг
Swagger UI: http://localhost:8080/swagger-ui.html

Health check: http://localhost:8080/actuator/health

Метрики: http://localhost:8080/actuator/prometheus

pgAdmin: http://localhost:5050 (admin@admin.com / admin)

Тестирование
bash
cd service
./gradlew test
Тесты используют H2 in-memory БД и не требуют внешней PostgreSQL.

Структура проекта
text
document-flow/
├── docker-compose.yml
├── settings.gradle
├── build.gradle
├── service/                 # Основной сервис
│   ├── build.gradle
│   └── src/
│       ├── main/java/...   # Код приложения
│       └── test/java/...   # Тесты
├── generator-utility/       # Утилита для генерации
│   ├── build.gradle
│   └── src/
└── config/                  # Конфигурация линтеров
    └── checkstyle/
Возможные проблемы
На Windows Docker не подключается к localhost
Используйте IP контейнера:

bash
docker inspect document-flow-db | findstr "IPAddress"
Обновите application.yml:

yaml
spring.datasource.url: jdbc:postgresql://<IP>:5432/document_flow
На Linux / MacOS всё работает из коробки
Разработчик
GitHub: @gareq1211

Проект: ITQ-document-flow