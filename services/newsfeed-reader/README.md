# Newsfeed Reader Service

## Overview

The **Newsfeed Reader** serves each user's personalized home feed. It reads pre-computed post IDs from the **Redis** newsfeed cache (written by the Fanout Worker), then hydrates the full post objects from the **Posts Service**.

For **celebrity accounts** (followers > threshold), it fetches posts directly from the Posts Service at read time and merges them into the cached feed (**hybrid model**).

Built with **NestJS** + **Redis** + **HTTP client**.

---

## Architecture Position

```
API Gateway :3000
      │  GET /newsfeed
      ▼
┌──────────────────────────────────────────────────────────────────┐
│                   NEWSFEED READER  :3004                         │
│                                                                  │
│  1. Read newsfeed:{userId} from Redis → [ postId, postId, ... ]  │
│  2. Fetch post details from Posts Service (batch)                │
│  3. Merge celebrity posts (pull for users > threshold)           │
│  4. Sort by createdAt DESC + paginate                            │
│  5. Return enriched feed to client                               │
│                                                                  │
│  ┌──────────────────┐    ┌────────────────────────────────────┐  │
│  │  NewsfeedCtrl    │    │         NewsfeedService            │  │
│  │  GET /newsfeed   │───▶│  readFeed(userId, page, limit)     │  │
│  └──────────────────┘    └──────────────┬─────────────────────┘  │
│                                         │                        │
│            ┌────────────────────────────┼────────────────────┐   │
│            ▼                            ▼                    ▼   │
│   ┌─────────────────┐   ┌───────────────────┐  ┌──────────────┐  │
│   │  Redis Cache    │   │  Posts Service    │  │Follow Service│  │
│   │ newsfeed:{uid}  │   │  (batch hydrate)  │  │(celebrity??) │  │
│   └─────────────────┘   └───────────────────┘  └──────────────┘  │
└──────────────────────────────────────────────────────────────────┘
```

---

## API Endpoints

| Method | Path         | Description                              | Auth |
|--------|--------------|------------------------------------------|------|
| GET    | `/newsfeed`  | Get current user's paginated newsfeed    | ✅   |

### GET `/newsfeed` — Query Params

| Param   | Type   | Default | Description              |
|---------|--------|---------|--------------------------|
| `page`  | number | 1       | Page number              |
| `limit` | number | 20      | Items per page (max 100) |

### GET `/newsfeed` — Response

```json
{
  "data": [
    {
      "id": "uuid",
      "authorId": "uuid",
      "caption": "string",
      "mediaUrl": "string | null",
      "createdAt": "ISO-8601"
    }
  ],
  "page": 1,
  "limit": 20,
  "hasMore": true
}
```

---

## Feed Resolution Logic

```
readFeed(userId):
  1. LRANGE newsfeed:{userId} 0 -1   → cached post IDs (from push fanout)
  2. GET following:{userId}           → list of accounts user follows
  3. Filter out celebrity accounts   → call Follow Service for follower counts
  4. For each celebrity following:
       GET /posts?authorId={id}&limit=20   → pull latest posts
  5. Merge + deduplicate + sort by createdAt DESC
  6. Return paginated slice
```

---

## Redis Keys Read

```
Key:   newsfeed:{userId}          → Redis List (written by fanout-worker)
```

---

## Environment Variables

```env
PORT=3004
NODE_ENV=development

# Redis (Newsfeed Cache)
REDIS_HOST=redis
REDIS_PORT=6379

# Upstream Services
POSTS_SERVICE_URL=http://posts-service:3002
FOLLOW_SERVICE_URL=http://follow-service:3003

# Celebrity threshold (must match fanout-worker setting)
CELEBRITY_THRESHOLD=10000

# JWT
JWT_ACCESS_SECRET=change_me_in_production_access

# Pagination
NEWSFEED_DEFAULT_LIMIT=20
NEWSFEED_MAX_LIMIT=100
```

---

## Project Structure

```
src/
├── app.module.ts
├── main.ts
├── newsfeed/
│   ├── newsfeed.module.ts
│   ├── newsfeed.controller.ts
│   ├── newsfeed.service.ts          # core read + merge logic
│   └── dto/
│       └── newsfeed-response.dto.ts
├── redis/
│   ├── redis.module.ts
│   └── redis.service.ts
├── posts/
│   └── posts.client.ts              # HTTP client to posts-service
├── follow/
│   └── follow.client.ts             # HTTP client to follow-service
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

- **NestJS** – framework
- **ioredis** – read newsfeed cache
- **@nestjs/axios** – hydrate from posts/follow services

