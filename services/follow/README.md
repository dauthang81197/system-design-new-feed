# Follow Service

## Overview

The **Follow Service** manages the social graph: who follows whom. It stores follower/following relationships in **PostgreSQL** and caches hot follower lists in **Redis** to serve the Fanout Worker efficiently.

Built with **NestJS** + **PostgreSQL** (TypeORM) + **Redis**.

---

## Architecture Position

```
API Gateway :3000
      │  POST /follow/:targetId
      ▼
┌──────────────────────────────────────────────┐
│            FOLLOW SERVICE  :3003             │
│                                              │
│  ┌──────────────────┐  ┌──────────────────┐ │
│  │ FollowController │  │  FollowService   │ │
│  │ POST   /follow   │─▶│  follow()        │ │
│  │ DELETE /follow   │  │  unfollow()      │ │
│  │ GET followers    │  │  getFollowers()  │ │
│  │ GET following    │  │  getFollowing()  │ │
│  └──────────────────┘  └───────┬──────────┘ │
│                                │            │
│                   ┌────────────┴──────────┐ │
│                   │  PostgreSQL           │ │
│                   │  Table: follows       │ │
│                   └────────────┬──────────┘ │
│                                │ cache      │
│                   ┌────────────┴──────────┐ │
│                   │  Redis                │ │
│                   │  followers:{userId}   │ │
│                   │  following:{userId}   │ │
│                   └───────────────────────┘ │
└──────────────────────────────────────────────┘
         ▲
         │ GET /follow/:authorId/followers
         │ (called by Fanout Worker to resolve follower list)
┌──────────────────┐
│   Fanout Worker  │
└──────────────────┘
```

---

## API Endpoints

| Method | Path                              | Description                      | Auth |
|--------|-----------------------------------|----------------------------------|------|
| POST   | `/follow/:targetId`               | Follow a user                    | ✅   |
| DELETE | `/follow/:targetId`               | Unfollow a user                  | ✅   |
| GET    | `/follow/:userId/followers`       | Get follower list (paginated)    | ✅   |
| GET    | `/follow/:userId/following`       | Get following list (paginated)   | ✅   |
| GET    | `/follow/:userId/followers/count` | Get follower count               | ✅   |

### GET `/follow/:userId/followers` — Response

```json
{
  "data": ["uuid-1", "uuid-2", "..."],
  "total": 1500,
  "page": 1,
  "limit": 100
}
```

---

## Database Schema

```sql
CREATE TABLE follows (
  follower_id  UUID        NOT NULL,
  followee_id  UUID        NOT NULL,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (follower_id, followee_id)
);

CREATE INDEX idx_follows_followee ON follows (followee_id);  -- "who follows me?"
CREATE INDEX idx_follows_follower ON follows (follower_id);  -- "who do I follow?"
```

---

## Redis Cache

```
Key:   followers:{userId}         → Redis Set of follower IDs
Key:   following:{userId}         → Redis Set of following IDs
Key:   follower_count:{userId}    → Redis String (integer)

TTL: 3600s (1 hour), invalidated on follow/unfollow
```

---

## Environment Variables

```env
PORT=3003
NODE_ENV=development

# Database
DATABASE_URL=postgresql://postgres:postgres@postgres:5432/follow_db

# Redis
REDIS_HOST=redis
REDIS_PORT=6379
FOLLOW_CACHE_TTL=3600

# JWT
JWT_ACCESS_SECRET=change_me_in_production_access
```

---

## Project Structure

```
src/
├── app.module.ts
├── main.ts
├── follow/
│   ├── follow.module.ts
│   ├── follow.controller.ts
│   ├── follow.service.ts
│   ├── follow.entity.ts
│   └── dto/
│       └── follow-response.dto.ts
├── cache/
│   ├── cache.module.ts
│   └── cache.service.ts
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
- **ioredis** – follower list cache

