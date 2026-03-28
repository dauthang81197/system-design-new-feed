# API Gateway Service

## Overview

The **API Gateway** is the single entry point for all external client requests in the Instagram Newsfeed system. It is responsible for request routing, authentication enforcement, rate limiting, and aggregating responses from downstream microservices.

Built with **NestJS** + **HTTP Proxy / gRPC** on top of Node.js.

---

## Architecture Position

```
                          ┌─────────────────────────────────────────────┐
                          │               CLIENT (Mobile/Web)            │
                          └─────────────────────┬───────────────────────┘
                                                │  HTTPS
                                                ▼
                          ┌─────────────────────────────────────────────┐
                          │               API GATEWAY  :3000             │
                          │                                             │
                          │  ┌──────────┐  ┌──────────┐  ┌──────────┐ │
                          │  │   Auth   │  │  Rate    │  │  Route   │ │
                          │  │ Enforce  │  │  Limit   │  │  Table   │ │
                          │  └──────────┘  └──────────┘  └──────────┘ │
                          └───┬─────┬──────────┬──────────┬────────────┘
                              │     │          │          │
                   HTTP/gRPC  │     │          │          │
              ┌───────────────┘     │          │          └───────────────┐
              ▼                     ▼          ▼                          ▼
   ┌──────────────────┐  ┌──────────────┐  ┌──────────────────┐  ┌─────────────────┐
   │   Auth Service   │  │ Posts Service│  │  Follow Service  │  │ Newsfeed Reader │
   │     :3001        │  │    :3002     │  │     :3003        │  │     :3004       │
   └──────────────────┘  └──────┬───────┘  └──────────────────┘  └─────────────────┘
                                │
                          publish event
                                │
                                ▼
                   ┌────────────────────────┐
                   │       Kafka Topic      │
                   │    post.created        │
                   └────────────┬───────────┘
                                │
                                ▼
                   ┌────────────────────────┐
                   │    Fanout Worker       │
                   │       :3005            │
                   └────────────────────────┘
```

---

## Responsibilities

| Concern              | Detail                                                          |
|----------------------|-----------------------------------------------------------------|
| **Routing**          | Reverse-proxy each path prefix to the correct downstream service |
| **Auth Enforcement** | Validate JWT `Authorization: Bearer <token>` on protected routes |
| **Rate Limiting**    | 60 req/min per IP (global), stricter limits on write routes     |
| **Request Logging**  | Structured JSON logs for every inbound request                  |
| **CORS**             | Allow configured origins for mobile/web clients                 |
| **Health Check**     | `GET /health` aggregates liveness from all downstream services  |

---

## Route Table

| Method | Path                          | Upstream Service   | Auth Required |
|--------|-------------------------------|--------------------|---------------|
| POST   | `/auth/register`              | auth-service       | ❌            |
| POST   | `/auth/login`                 | auth-service       | ❌            |
| POST   | `/auth/refresh`               | auth-service       | ❌            |
| DELETE | `/auth/logout`                | auth-service       | ✅            |
| GET    | `/users/:id`                  | auth-service       | ✅            |
| POST   | `/posts`                      | posts-service      | ✅            |
| GET    | `/posts/:id`                  | posts-service      | ✅            |
| DELETE | `/posts/:id`                  | posts-service      | ✅            |
| POST   | `/follow/:targetId`           | follow-service     | ✅            |
| DELETE | `/follow/:targetId`           | follow-service     | ✅            |
| GET    | `/follow/:userId/followers`   | follow-service     | ✅            |
| GET    | `/follow/:userId/following`   | follow-service     | ✅            |
| GET    | `/newsfeed`                   | newsfeed-reader    | ✅            |
| GET    | `/health`                     | (gateway itself)   | ❌            |

---

## Environment Variables

```env
# Server
PORT=3000
NODE_ENV=development

# JWT (for validation only – no signing here)
JWT_ACCESS_SECRET=change_me_in_production_access

# Upstream Services
AUTH_SERVICE_URL=http://auth-service:3001
POSTS_SERVICE_URL=http://posts-service:3002
FOLLOW_SERVICE_URL=http://follow-service:3003
NEWSFEED_SERVICE_URL=http://newsfeed-reader:3004
NOTIFICATIONS_SERVICE_URL=http://notifications-service:3006

# Rate Limiting
THROTTLE_TTL=60000
THROTTLE_LIMIT=60

# CORS
CORS_ORIGIN=http://localhost:3000,https://your-app.com
```

---

## Project Structure

```
src/
├── app.module.ts
├── main.ts
├── proxy/
│   ├── proxy.module.ts
│   └── proxy.service.ts          # HTTP proxy logic
├── auth/
│   ├── auth.guard.ts             # JWT validation guard
│   └── auth.strategy.ts
├── health/
│   └── health.controller.ts      # GET /health
├── common/
│   ├── filters/
│   │   └── http-exception.filter.ts
│   ├── interceptors/
│   │   └── logging.interceptor.ts
│   └── middleware/
│       └── request-logger.middleware.ts
```

---

## Running Locally

```bash
# Install dependencies
yarn install

# Development (hot reload)
yarn start:dev

# Production build
yarn build && yarn start:prod
```

---

## Docker

```bash
# Build image
docker build -t api-gateway .

# Run with env file
docker run --env-file .env -p 3000:3000 api-gateway
```

---

## Health Check

```bash
curl http://localhost:3000/health
# → { "status": "ok", "services": { "auth": "up", "posts": "up", ... } }
```

---

## Tech Stack

- **NestJS** – framework
- **@nestjs/throttler** – rate limiting
- **http-proxy-middleware** – reverse proxy
- **passport-jwt** – token validation
- **Helmet** – security headers

