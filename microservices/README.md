# Instagram Newsfeed — System Design

A production-grade **Instagram-style newsfeed** system built with microservices.  
Stack: **NestJS · PostgreSQL · Redis · Kafka · S3/MinIO · Docker · Kubernetes**

---

## Full System Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           CLIENT  (Mobile / Web)                                │
└───────────────────────────────────┬─────────────────────────────────────────────┘
                                    │  HTTPS
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                          API GATEWAY  :3000                                     │
│         Auth Enforce · Rate Limit · Routing · CORS · Logging                   │
└──┬──────────┬──────────────┬────────────────┬──────────────────────────────────┘
   │          │              │                │
   ▼          ▼              ▼                ▼
:3001      :3002           :3003            :3004
Auth       Posts          Follow          Newsfeed
Service    Service        Service         Reader
  │          │              │                │
  │          │ publish       │ cache          │ read cache
  │          ▼              │                │
  │   ┌────────────┐        │                │
  │   │   Kafka    │        │                │
  │   │post.created│        │                │
  │   └─────┬──────┘        │                │
  │         │               │                │
  │         ▼               │                │
  │   ┌────────────────────────────────┐     │
  │   │         Fanout Worker  :3005   │     │
  │   │  1. Consume post.created       │     │
  │   │  2. GET followers (→ :3003)    │─────┘
  │   │  3. LPUSH newsfeed:{uid} Redis │
  │   │  4. Publish notification.push  │
  │   └────────┬───────────────────────┘
  │            │
  │            ▼ Kafka: notification.push
  │   ┌─────────────────────┐
  │   │  Notifications  :3006│
  │   │  WebSocket + DB     │
  │   └─────────────────────┘
  │
  ▼
┌─────────────────────────────────────────────────────┐
│                  Infrastructure                      │
│  PostgreSQL · Redis · Kafka · S3/MinIO               │
└─────────────────────────────────────────────────────┘
```

---

## Full Request Flows

### Flow 1 — User Creates a Post

```
Client
  │── POST /posts ──▶ API Gateway
                          │── validate JWT
                          │── forward ──▶ Posts Service
                                              │── INSERT posts table (PostgreSQL)
                                              │── upload media ──▶ S3/MinIO
                                              │── Kafka PRODUCE post.created
                                              │── 201 response ──▶ Client
                                              │
                                       Kafka: post.created
                                              │
                                              ▼
                                       Fanout Worker
                                              │── GET /follow/{authorId}/followers ──▶ Follow Service
                                              │── for each follower:
                                              │       LPUSH newsfeed:{followerId} {postId}  (Redis)
                                              │       LTRIM newsfeed:{followerId} 0 999
                                              │── Kafka PRODUCE notification.push
                                              │
                                       Kafka: notification.push
                                              │
                                              ▼
                                       Notifications Service
                                              │── INSERT notifications table
                                              │── WebSocket EMIT → online followers
```

### Flow 2 — User Reads Newsfeed

```
Client
  │── GET /newsfeed ──▶ API Gateway
                            │── validate JWT
                            │── forward ──▶ Newsfeed Reader
                                                │── LRANGE newsfeed:{userId} (Redis)
                                                │── batch GET /posts?ids=[...] ──▶ Posts Service
                                                │── GET celebrity following ──▶ Follow Service
                                                │── pull celebrity posts ──▶ Posts Service
                                                │── merge + sort + paginate
                                                │── 200 response ──▶ Client
```

### Flow 3 — User Follows Another User

```
Client
  │── POST /follow/:targetId ──▶ API Gateway
                                     │── validate JWT
                                     │── forward ──▶ Follow Service
                                                         │── INSERT follows table (PostgreSQL)
                                                         │── SADD followers:{targetId} {userId} (Redis)
                                                         │── Kafka PRODUCE follow.created
                                                         │── 201 response ──▶ Client
                                                         │
                                                  Kafka: follow.created
                                                         │
                                                         ▼
                                                  Notifications Service
                                                         │── INSERT notification (NEW_FOLLOWER)
                                                         │── WebSocket EMIT → targetId
```

---

## Services Overview

| Service                                      | Port | Responsibility                                     |
|----------------------------------------------|------|----------------------------------------------------|
| [api-gateway](./services/api-gateway)         | 3000 | Routing, Auth, Rate Limiting                       |
| [auth](./services/auth)                       | 3001 | Register, Login, JWT, Refresh Token                |
| [posts](./services/posts)                     | 3002 | Create/Read/Delete posts, S3 upload, Kafka publish |
| [follow](./services/follow)                   | 3003 | Follow/Unfollow, social graph, follower cache      |
| [newsfeed-reader](./services/newsfeed-reader) | 3004 | Serve personalized feed from Redis cache           |
| [fanout-worker](./services/fanout-worker)     | 3005 | Consume post.created, push to follower feeds       |
| [notifications](./services/notifications)     | 3006 | WebSocket alerts, push notifications               |

---

## Infrastructure

| Component  | Usage                                           |
|------------|-------------------------------------------------|
| PostgreSQL | Persistent storage for each service             |
| Redis      | Newsfeed cache, follower cache, token blacklist |
| Kafka      | Event bus: post.created, notification.push, follow.created |
| S3/MinIO   | Post media storage (images/videos)              |

---

## Getting Started

```bash
# Start all infrastructure
docker-compose -f deploy/docker-compose.yml up -d

# Run a specific service in dev mode
cd services/auth && yarn install && yarn start:dev
cd services/posts && yarn install && yarn start:dev
# ... etc
```

---

## Project Structure

```
instagram-newsfeed/
├── deploy/
│   ├── docker-compose.yml          # Full stack local deploy
│   └── k8s-manifests/              # Kubernetes YAML
├── infra/
│   ├── kafka/                      # Kafka config
│   ├── postgres/                   # DB init scripts
│   ├── redis/                      # Redis config
│   └── s3/                         # MinIO/S3 config
└── services/
    ├── api-gateway/   → README.md  ✅
    ├── auth/          → README.md  ✅
    ├── posts/         → README.md  ✅
    ├── follow/        → README.md  ✅
    ├── newsfeed-reader/ → README.md ✅
    ├── fanout-worker/ → README.md  ✅
    └── notifications/ → README.md  ✅
```

