# Fanout Worker Service

## Overview

The **Fanout Worker** is an event-driven background worker that consumes `post.created` events from **Kafka** and **fans out** (pushes) new posts to the Redis-based newsfeed cache of every follower of the post author.

This service implements the **Push model (fan-out on write)** strategy for the Instagram-style newsfeed.

Built with **NestJS** + **KafkaJS** + **ioredis**.

---

## Architecture Position

```
┌──────────────────┐
│   Posts Service  │
│     :3002        │
└────────┬─────────┘
         │  produce
         │  Topic: post.created
         ▼
┌──────────────────────────────────┐
│           Apache Kafka           │
│   Topic: post.created            │
│   Partitions: N (by author_id)   │
└────────────────┬─────────────────┘
                 │  consume (group: fanout-worker-group)
                 ▼
┌──────────────────────────────────────────────────────────────────┐
│                        FANOUT WORKER  :3005                       │
│                                                                  │
│  1. Consume event { postId, authorId, createdAt, mediaUrl }      │
│  2. Fetch follower list from Follow Service (or Redis cache)     │
│  3. For each follower → LPUSH newsfeed:{userId} {postId}         │
│  4. LTRIM newsfeed:{userId} 0 999  (keep last 1000 items)        │
│  5. Publish push notification event → notifications-service      │
│                                                                  │
│  ┌──────────────┐   ┌──────────────┐   ┌─────────────────────┐  │
│  │  Kafka       │   │  Follow      │   │  Redis (Newsfeed    │  │
│  │  Consumer    │──▶│  Service     │──▶│  Cache) LPUSH       │  │
│  │  (KafkaJS)   │   │  HTTP/gRPC   │   │  newsfeed:{userId}  │  │
│  └──────────────┘   └──────────────┘   └─────────────────────┘  │
│                                                |                 │
│                                                ▼                 │
│                              ┌──────────────────────────────┐   │
│                              │  Kafka: notification.created │   │
│                              │  → Notifications Service     │   │
│                              └──────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────┘
```

---

## Fanout Strategy

| Strategy        | Description                                          | Used Here |
|-----------------|------------------------------------------------------|-----------|
| **Push (Write)** | On post creation, push to every follower's feed     | ✅        |
| **Pull (Read)**  | Fetch and merge posts at read time                  | ❌        |
| **Hybrid**       | Push for normal users, pull for celebrities (>10k) | 🔜 Planned |

> **Celebrity problem:** Users with >10,000 followers bypass push. Their posts are fetched at read time by `newsfeed-reader` and merged with the pre-built cache. Threshold is configurable via `CELEBRITY_THRESHOLD` env var.

---

## Event Contract

### Consumed — `post.created`

```json
{
  "postId": "uuid-v4",
  "authorId": "uuid-v4",
  "caption": "string",
  "mediaUrl": "string | null",
  "createdAt": "ISO-8601 timestamp"
}
```

**Kafka topic:** `post.created`  
**Consumer group:** `fanout-worker-group`  
**Partition key:** `authorId` (ensures ordering per author)

---

### Produced — `notification.push`

```json
{
  "type": "NEW_POST",
  "actorId": "uuid-v4",
  "recipientId": "uuid-v4",
  "postId": "uuid-v4",
  "createdAt": "ISO-8601 timestamp"
}
```

**Kafka topic:** `notification.push`

---

## Redis Data Model

```
Key:   newsfeed:{userId}
Type:  Redis List
Value: [ "postId-newest", "postId-2", ..., "postId-1000th" ]

LPUSH  newsfeed:{userId}  {postId}    ← prepend new post
LTRIM  newsfeed:{userId}  0 999       ← keep latest 1000
TTL    newsfeed:{userId}  604800      ← expire after 7 days
```

---

## Retry & Error Handling

| Scenario                   | Strategy                                              |
|----------------------------|-------------------------------------------------------|
| Redis write fails          | Retry 3× with exponential backoff (100ms, 500ms, 2s) |
| Follow service unreachable | Retry 3× then send to Dead Letter Topic (DLT)         |
| Kafka consumer lag         | Auto-scale worker replicas (HPA in k8s)               |
| Celebrity fan-out skip     | Log `SKIP_FANOUT` metric, handled by newsfeed-reader  |
| Poison pill message        | After 3 failures → publish to `post.created.DLT`     |

---

## Environment Variables

```env
# Server
PORT=3005
NODE_ENV=development

# Kafka
KAFKA_BROKERS=kafka:9092
KAFKA_GROUP_ID=fanout-worker-group
KAFKA_CONSUMER_TOPIC=post.created
KAFKA_NOTIFICATION_TOPIC=notification.push

# Redis (Newsfeed Cache)
REDIS_HOST=redis
REDIS_PORT=6379
NEWSFEED_MAX_LENGTH=1000
NEWSFEED_TTL_SECONDS=604800

# Follow Service
FOLLOW_SERVICE_URL=http://follow-service:3003

# Fanout Config
CELEBRITY_THRESHOLD=10000       # users above this skip push fanout
FANOUT_BATCH_SIZE=100           # followers processed per batch
FANOUT_CONCURRENCY=5            # concurrent Redis write batches

# Retry
MAX_RETRY_ATTEMPTS=3
RETRY_BACKOFF_MS=100
```

---

## Project Structure

```
src/
├── app.module.ts
├── main.ts
├── fanout/
│   ├── fanout.module.ts
│   ├── fanout.service.ts           # Core fan-out logic
│   └── fanout.processor.ts         # Kafka consumer handler
├── kafka/
│   ├── kafka.module.ts
│   ├── kafka.consumer.ts
│   └── kafka.producer.ts
├── redis/
│   ├── redis.module.ts
│   └── redis.service.ts            # LPUSH / LTRIM
├── follow/
│   └── follow.client.ts            # HTTP client for follow-service
├── common/
│   ├── filters/
│   └── interceptors/
```

---

## Running Locally

```bash
# Install dependencies
yarn install

# Start Kafka + Redis (from root)
docker-compose -f ../../deploy/docker-compose.yml up kafka redis -d

# Development
yarn start:dev

# Production
yarn build && yarn start:prod
```

---

## Docker

```bash
docker build -t fanout-worker .
docker run --env-file .env fanout-worker
```

---

## Monitoring

| Metric                        | Description                              |
|-------------------------------|------------------------------------------|
| `fanout_events_consumed_total` | Total Kafka messages consumed           |
| `fanout_duration_ms`          | Time taken for a single fan-out         |
| `fanout_followers_pushed`     | Number of feeds updated per event       |
| `fanout_celebrity_skipped`    | Events skipped due to celebrity rule    |
| `fanout_dlq_total`            | Messages sent to Dead Letter Queue      |

---

## Tech Stack

- **NestJS** – framework
- **KafkaJS** – Kafka consumer/producer
- **ioredis** – Redis client for feed cache
- **axios / @nestjs/axios** – HTTP client for follow-service

