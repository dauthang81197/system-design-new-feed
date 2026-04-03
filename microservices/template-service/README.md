# Template Service

A **production-ready Spring Boot 3.x microservice template**. Copy and rename this to bootstrap any new service in seconds.

---

## 📦 Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.4 / Java 21 |
| Database (relational) | PostgreSQL 16 + Spring Data JPA + Liquibase |
| Database (document) | MongoDB 7 + Spring Data MongoDB |
| Cache | Redis 7 + Lettuce |
| Messaging | Apache Kafka (Confluent) |
| Object Storage | AWS S3 / MinIO |
| API Docs | SpringDoc OpenAPI 3 (Swagger UI) |
| Security | JWT (HS256, jjwt 0.12) |
| Mapping | MapStruct |
| Logging | Logback + Logstash JSON encoder |
| Testing | JUnit 5, Spring Security Test, Spring Kafka Test |

---

## 🗂️ Project Structure

```
src/main/java/com/pv/template/
├── TemplateApplication.java       # Entry point
├── config/
│   ├── SecurityConfig.java        # Spring Security + JWT filter chain
│   ├── RedisConfig.java           # Lettuce pool configuration
│   ├── KafkaConfig.java           # Topic definitions
│   ├── S3Config.java              # AWS S3 / MinIO client
│   └── SwaggerConfig.java         # OpenAPI 3 + Bearer auth
├── controller/
│   └── TemplateController.java    # REST endpoints
├── service/
│   ├── TemplateService.java       # Interface
│   └── TemplateServiceImpl.java   # Business logic
├── repository/
│   ├── jpa/TemplateRepository.java      # Spring Data JPA
│   └── mongo/TemplateMongoRepository.java # Spring Data MongoDB
├── entity/
│   └── TemplateEntity.java        # JPA @Entity → PostgreSQL
├── document/
│   └── TemplateDocument.java      # MongoDB @Document (audit log)
├── dto/
│   ├── TemplateRequest.java       # Input DTO (validated)
│   ├── TemplateResponse.java      # Output DTO
│   ├── PageResponse.java          # Generic paginated response
│   └── ErrorResponse.java         # Unified error body
├── mapper/
│   └── TemplateMapper.java        # MapStruct entity ↔ DTO
├── security/
│   ├── JwtUtils.java              # JWT parse & claims extraction
│   ├── JwtAuthFilter.java         # OncePerRequestFilter (stateless)
│   └── UserPrincipal.java         # @AuthenticationPrincipal holder
├── exception/
│   ├── GlobalExceptionHandler.java # @RestControllerAdvice
│   ├── ResourceNotFoundException.java
│   └── ForbiddenException.java
├── kafka/
│   ├── TemplateEventProducer.java # Idempotent Kafka producer
│   └── TemplateEventConsumer.java # Manual-ack Kafka consumer
└── s3/
    └── S3StorageService.java      # Upload / Download / Delete
```

---

## 🚀 Quick Start

### Run with Docker Compose (all dependencies)

```bash
docker compose up --build
```

Service: http://localhost:8080/api/v1  
Swagger UI: http://localhost:8080/api/v1/swagger-ui.html  
MinIO Console: http://localhost:9001 (minioadmin / minioadmin)

### Run locally (external dependencies required)

```bash
# Copy and fill in your env vars
cp .env.example .env

mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## 🔐 Authentication

All endpoints (except `/swagger-ui/**`, `/api-docs/**`, `/actuator/health`) require a **Bearer JWT** issued by the `auth-service`.

```
Authorization: Bearer <access_token>
```

Claims expected in the token:
- `sub` — user ID
- `roles` — list of role strings, e.g. `["USER", "ADMIN"]`

---

## 🗄️ Database Migrations (Liquibase)

Migration files live in `src/main/resources/db/changelog/changes/`.  
Naming convention: `NNN-description.sql`

```
001-create-template-table.sql  ✅ (initial schema)
002-add-tags-column.sql        ← add next migration here
```

---

## ✏️ Copy & Rename Checklist

When creating a new service from this template:

- [ ] Rename folder: `template-service` → `your-service`
- [ ] Update `pom.xml`: `artifactId`, `name`
- [ ] Update `application.yml`: `spring.application.name`
- [ ] Rename all Java packages: `com.pv.template` → `com.pv.yourservice`
- [ ] Rename entity, document, DTOs, mapper, controller, service, repository classes
- [ ] Update Kafka topic names in `KafkaConfig.java` and `application.yml`
- [ ] Update `docker-compose.yml` container names and ports
- [ ] Add your first Liquibase migration in `db/changelog/changes/`
- [ ] Update this `README.md`

---

## 📋 API Endpoints

| Method | Path | Description | Auth |
|---|---|---|---|
| `POST` | `/templates` | Create template | ✅ |
| `GET` | `/templates/{id}` | Get by ID | ✅ |
| `GET` | `/templates` | List (paginated) | ✅ |
| `PUT` | `/templates/{id}` | Update | ✅ owner |
| `DELETE` | `/templates/{id}` | Delete | ✅ owner |
| `GET` | `/templates/admin/all` | Admin list | ✅ ADMIN role |

