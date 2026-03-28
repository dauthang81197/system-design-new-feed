# Notifications Service

## Overview

The **Notifications Service** is responsible for delivering real-time and push notifications to users (e.g. "X started following you", "X posted a new photo"). It consumes events from **Kafka** and delivers via **WebSocket** (real-time) and/or push notification providers (FCM / APNs).

Built with **NestJS** + **KafkaJS** + **WebSocket Gateway**.

---

## Architecture Position

```
┌──────────────────────────────────┐
│        Fanout Worker             │
│  Produces: notification.push     │
└─────────────────┬────────────────┘

┌──────────────────────────────────┐
│        Follow Service            │
│  Produces: follow.created        │
└─────────────────┬────────────────┘
                  │
                  ▼ Kafka Topics
┌──────────────────────────────────────────────────────────────┐
│               NOTIFICATIONS SERVICE  :3006                    │
│                                                              │
│  ┌───────────────┐   ┌───────────────────────────────────┐  │
│  │  Kafka        │   │     NotificationsService          │  │
│  │  Consumer     │──▶│  handleNewPost / handleFollow     │  │
│  └───────────────┘   └────────────────┬──────────────────┘  │
│                                       │                     │
│              ┌────────────────────────┼─────────────────┐   │
│              ▼                        ▼                 ▼   │
│  ┌──────────────────┐  ┌───────────────────┐  ┌─────────────┐ │
│  │  WebSocket GW    │  │  PostgreSQL       │  │  FCM/APNs   │ │
│  │  (socket.io)     │  │  notifications    │  │  (mobile    │ │
│  │  real-time push  │  │  table (history)  │  │   push)     │ │
│  └──────────────────┘  └───────────────────┘  └─────────────┘ │
└──────────────────────────────────────────────────────────────┘
              ▲
              │  WS connect
┌─────────────┴──────────┐
│   Client (Mobile/Web)  │
└────────────────────────┘
```

---

## Consumed Kafka Events

### `notification.push` (from Fanout Worker)

```json
{
  "type": "NEW_POST",
  "actorId": "uuid",
  "recipientId": "uuid",
  "postId": "uuid",
  "createdAt": "ISO-8601"
}
```

### `follow.created` (from Follow Service)

```json
{
  "type": "NEW_FOLLOWER",
  "actorId": "uuid",
  "recipientId": "uuid",
  "createdAt": "ISO-8601"
}
```

---

## API Endpoints

| Method | Path                        | Description                          | Auth |
|--------|-----------------------------|--------------------------------------|------|
| GET    | `/notifications`            | Get notification history (paginated) | ✅   |
| PATCH  | `/notifications/:id/read`   | Mark a notification as read          | ✅   |
| PATCH  | `/notifications/read-all`   | Mark all notifications as read       | ✅   |

### WebSocket Events

| Event           | Direction       | Payload                              |
|-----------------|-----------------|--------------------------------------|
| `notification`  | Server → Client | `{ type, actorId, postId, message }` |
| `join`          | Client → Server | `{ userId }` (on connect)            |

---

## Database Schema

```sql
CREATE TABLE notifications (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  recipient_id UUID        NOT NULL,
  actor_id     UUID        NOT NULL,
  type         VARCHAR(50) NOT NULL,   -- NEW_POST | NEW_FOLLOWER | LIKE
  post_id      UUID,
  is_read      BOOLEAN     NOT NULL DEFAULT FALSE,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_recipient ON notifications (recipient_id, created_at DESC);
```

---

## Environment Variables

```env
PORT=3006
NODE_ENV=development

# Kafka
KAFKA_BROKERS=kafka:9092
KAFKA_GROUP_ID=notifications-service-group
KAFKA_TOPICS=notification.push,follow.created

# Database
DATABASE_URL=postgresql://postgres:postgres@postgres:5432/notifications_db

# JWT
JWT_ACCESS_SECRET=change_me_in_production_access

# Push (optional)
FCM_SERVER_KEY=
```

---

## Project Structure

```
src/
├── app.module.ts
├── main.ts
├── notifications/
│   ├── notifications.module.ts
│   ├── notifications.controller.ts
│   ├── notifications.service.ts
│   ├── notification.entity.ts
│   └── dto/
├── kafka/
│   ├── kafka.module.ts
│   └── kafka.consumer.ts
├── gateway/
│   └── notifications.gateway.ts   # WebSocket (socket.io)
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

- **NestJS** + **@nestjs/websockets** + **socket.io**
- **KafkaJS** – event consumption
- **TypeORM** + **PostgreSQL** – notification history

