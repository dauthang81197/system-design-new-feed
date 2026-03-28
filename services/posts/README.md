# Posts Service

## Overview

The **Posts Service** handles all post lifecycle operations: create, read, delete. When a post is created, it publishes a `post.created` event to **Kafka**, which triggers the **Fanout Worker** to distribute the post to followers' newsfeeds.

Built with **NestJS** + **PostgreSQL** (TypeORM) + **S3** (media) + **Kafka**.

---

## Architecture Position

```
API Gateway :3000
      │  POST /posts
      ▼
┌─────────────────────────────────────────────────┐
│              POSTS SERVICE  :3002               │
│                                                 │
│  ┌─────────────────┐    ┌─────────────────────┐ │
│  │  PostsController│    │     PostsService    │ │
│  │  POST   /posts  │───▶│  create / get /     │ │
│  │  GET    /posts  │    │  delete             │ │
│  │  DELETE /posts  │    └────────┬────────────┘ │
│  └─────────────────┘             │              │
│                        ┌─────────┴──────────┐   │
│                        │   PostgreSQL (DB)  │   │
│                        │   Table: posts     │   │
│                        └─────────┬──────────┘   │
│                                  │ write         │
│                        ┌─────────┴──────────┐   │
│                        │   S3 / MinIO       │   │
│                        │   (media upload)   │   │
│                        └────────────────────┘   │
│                                  │ publish       │
│                        ┌─────────┴──────────┐   │
│                        │  Kafka Producer    │   │
│                        │  post.created      │   │
│                        └────────────────────┘   │
└─────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │   Fanout Worker  │
                    └──────────────────┘
```

---

## API Endpoints

| Method | Path          | Description                   | Auth |
|--------|---------------|-------------------------------|------|
| POST   | `/posts`      | Create a new post             | ✅   |
| GET    | `/posts/:id`  | Get a single post by ID       | ✅   |
| GET    | `/posts`      | List posts by author (paged)  | ✅   |
| DELETE | `/posts/:id`  | Delete own post               | ✅   |

### POST `/posts` — Request Body

```json
{
  "caption": "string (max 2200 chars)",
  "mediaUrl": "string | null"
}
```

### POST `/posts` — Response

```json
{
  "id": "uuid-v4",
  "authorId": "uuid-v4",
  "caption": "string",
  "mediaUrl": "string | null",
  "createdAt": "ISO-8601"
}
```

---

## Database Schema

```sql
CREATE TABLE posts (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  author_id   UUID        NOT NULL,
  caption     TEXT,
  media_url   TEXT,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  deleted_at  TIMESTAMPTZ              -- soft delete
);

CREATE INDEX idx_posts_author_id_created ON posts (author_id, created_at DESC);
```

---

## Kafka Event Published

**Topic:** `post.created`

```json
{
  "postId": "uuid-v4",
  "authorId": "uuid-v4",
  "caption": "string",
  "mediaUrl": "string | null",
  "createdAt": "ISO-8601"
}
```

---

## Environment Variables

```env
PORT=3002
NODE_ENV=development

# Database
DATABASE_URL=postgresql://postgres:postgres@postgres:5432/posts_db

# Kafka
KAFKA_BROKERS=kafka:9092
KAFKA_TOPIC_POST_CREATED=post.created

# S3 / MinIO
S3_ENDPOINT=http://minio:9000
S3_BUCKET=posts-media
S3_ACCESS_KEY=minioadmin
S3_SECRET_KEY=minioadmin

# JWT (validate token passed from API Gateway)
JWT_ACCESS_SECRET=change_me_in_production_access
```

---

## Project Structure

```
src/
├── app.module.ts
├── main.ts
├── posts/
│   ├── posts.module.ts
│   ├── posts.controller.ts
│   ├── posts.service.ts
│   ├── post.entity.ts
│   └── dto/
│       ├── create-post.dto.ts
│       └── post-response.dto.ts
├── kafka/
│   ├── kafka.module.ts
│   └── kafka.producer.ts
├── storage/
│   ├── storage.module.ts
│   └── storage.service.ts       # S3 upload
├── database/
│   └── database.module.ts
└── common/
```

---

## Running Locally

```bash
yarn install
yarn start:dev
```

---

## Tech Stack

- **NestJS**, **TypeORM**, **PostgreSQL**
- **KafkaJS** – event publishing
- **@aws-sdk/client-s3** – media upload to S3/MinIO

