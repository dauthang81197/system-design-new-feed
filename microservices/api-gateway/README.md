# API Gateway Service — Go

## Overview

The **API Gateway** is the single entry point for all external client requests in the Instagram Newsfeed system.  
Rewritten from **NestJS → Go** using `net/http` + `httputil.ReverseProxy` + **Chi Router**.

> **Build strategy:** 8 independent phases — each phase is fully runnable & testable before moving to the next.

---

## Architecture Position

```
                        ┌──────────────────────────────────────────────────┐
                        │                CLIENT (Mobile / Web)              │
                        └─────────────────────────┬────────────────────────┘
                                                   │  HTTPS :3000
                                                   ▼
                        ┌──────────────────────────────────────────────────┐
                        │                  API GATEWAY  :3000  (Go)         │
                        │                                                  │
                        │  ┌──────────┐ ┌──────────┐ ┌───────────────┐   │
                        │  │  Logger  │ │  CORS    │ │  Rate Limit   │   │
                        │  │  (slog)  │ │(chi/cors)│ │ memory|redis  │   │
                        │  └──────────┘ └──────────┘ └───────────────┘   │
                        │  ┌──────────────────────────────────────────┐   │
                        │  │        JWT Verify  (golang-jwt/jwt/v5)   │   │
                        │  └──────────────────────────────────────────┘   │
                        │  ┌──────────────────────────────────────────┐   │
                        │  │   Circuit Breaker + Retry (sony/gobreaker)│   │
                        │  └──────────────────────────────────────────┘   │
                        │  ┌──────────────────────────────────────────┐   │
                        │  │  Service Discovery (Consul) + LB          │   │
                        │  │  Round-Robin / Weighted                   │   │
                        │  └──────────────────────────────────────────┘   │
                        │  ┌──────────────────────────────────────────┐   │
                        │  │   Reverse Proxy  (httputil.ReverseProxy)  │   │
                        │  └──────────────────────────────────────────┘   │
                        └────┬──────────┬──────────┬──────────┬───────────┘
                             │          │          │          │
                ┌────────────┘  ┌───────┘   ┌─────┘   ┌──────┘
                ▼               ▼           ▼         ▼
   ┌─────────────────┐ ┌──────────────┐ ┌──────────────┐ ┌─────────────────┐
   │  Auth Service   │ │Posts Service │ │Follow Service│ │Newsfeed Reader  │
   │    :3001        │ │   :3002      │ │   :3003      │ │    :3004        │
   └─────────────────┘ └──────────────┘ └──────────────┘ └─────────────────┘
```

---

## Middleware Pipeline (Ordered)

```
Request ──▶ Logger ──▶ CORS ──▶ RateLimit ──▶ JWT ──▶ CircuitBreaker ──▶ ReverseProxy ──▶ Upstream
                                    │               │          │
                                  429 ←──────── 401 ←────── 503
```

> ⚠️ **Thứ tự quan trọng:** Rate Limit chạy **trước** JWT để chặn brute-force trước khi tốn CPU verify token.

---

## Route Table

| Method   | Path                          | Upstream           | Auth |
|----------|-------------------------------|--------------------|------|
| `POST`   | `/auth/register`              | auth-service       | ❌   |
| `POST`   | `/auth/login`                 | auth-service       | ❌   |
| `POST`   | `/auth/refresh`               | auth-service       | ❌   |
| `DELETE` | `/auth/logout`                | auth-service       | ✅   |
| `GET`    | `/users/:id`                  | auth-service       | ✅   |
| `POST`   | `/posts`                      | posts-service      | ✅   |
| `GET`    | `/posts/:id`                  | posts-service      | ✅   |
| `DELETE` | `/posts/:id`                  | posts-service      | ✅   |
| `POST`   | `/follow/:targetId`           | follow-service     | ✅   |
| `DELETE` | `/follow/:targetId`           | follow-service     | ✅   |
| `GET`    | `/follow/:userId/followers`   | follow-service     | ✅   |
| `GET`    | `/follow/:userId/following`   | follow-service     | ✅   |
| `GET`    | `/newsfeed`                   | newsfeed-reader    | ✅   |
| `GET`    | `/health`                     | gateway (self)     | ❌   |

---

## Target Project Structure

```
services/api-gateway/
├── cmd/
│   └── gateway/
│       └── main.go                  # Entry point, wiring, graceful shutdown
├── internal/
│   ├── config/
│   │   └── config.go                # Load env vars (os.Getenv / envconfig)
│   ├── proxy/
│   │   └── proxy.go                 # httputil.ReverseProxy per upstream
│   ├── middleware/
│   │   ├── logger.go                # Structured JSON log (slog)
│   │   ├── cors.go                  # go-chi/cors, AllowedOrigins from env
│   │   ├── auth.go                  # JWT verify, inject X-User-Id header
│   │   ├── ratelimit.go             # In-memory: golang.org/x/time/rate
│   │   └── ratelimit_redis.go       # Redis sliding window (go-redis/v9 + Lua)
│   ├── circuit/
│   │   └── breaker.go               # sony/gobreaker + retry RoundTripper
│   ├── discovery/
│   │   └── consul.go                # hashicorp/consul/api, cache + watch
│   └── loadbalancer/
│       ├── balancer.go              # interface Balancer
│       ├── roundrobin.go            # atomic counter round-robin
│       └── weighted.go              # weight from Consul metadata
├── go.mod
├── go.sum
├── Dockerfile
└── README.md
```

---

## Implementation Plan — 8 Phases

### ✅ Phase 1 — Go Project Skeleton + Chi Router

**Goal:** Server chạy được, route table đăng ký đủ, health check hoạt động.

**Files to create:**
```
cmd/gateway/main.go
internal/config/config.go
go.mod
```

**Dependencies:**
```bash
go mod init github.com/your-org/api-gateway
go get github.com/go-chi/chi/v5
```

**Tasks:**
- [ ] Khởi tạo `chi.NewRouter()`
- [ ] Đăng ký 14 routes (public + protected) → placeholder `200 OK`
- [ ] `GET /health` → `{"status":"ok"}`
- [ ] Load config từ env: `PORT`, `JWT_ACCESS_SECRET`, upstream URLs
- [ ] `signal.NotifyContext` + `server.Shutdown(ctx)` — graceful shutdown

**Test:**
```bash
curl http://localhost:3000/health
# → {"status":"ok"}
```

---

### ✅ Phase 2 — Reverse Proxy (`httputil.ReverseProxy`)

**Goal:** Gateway thực sự forward request xuống upstream service.

**Files to create:**
```
internal/proxy/proxy.go
```

**Dependencies:** stdlib `net/http/httputil` (no extra deps)

**Tasks:**
- [ ] `httputil.NewSingleHostReverseProxy(target)` cho mỗi upstream
- [ ] Custom `Director` function:
  - Strip prefix (`/auth` → `/`) trước khi gửi upstream
  - Forward `X-Request-Id`, `X-Forwarded-For`, `X-Real-IP`
  - Set `X-User-Id` từ context (Phase 4)
- [ ] Đăng ký vào Chi route groups:
  ```go
  r.Route("/auth",    func(r chi.Router) { r.Handle("/*", authProxy) })
  r.Route("/posts",   func(r chi.Router) { r.Handle("/*", postsProxy) })
  r.Route("/follow",  func(r chi.Router) { r.Handle("/*", followProxy) })
  r.Route("/newsfeed",func(r chi.Router) { r.Handle("/*", newsfeedProxy) })
  ```

**Test:**
```bash
# Upstream auth-service chạy trên :3001
curl -X POST http://localhost:3000/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test"}'
```

---

### ✅ Phase 3 — Middleware: Request Logger + CORS

**Goal:** Mọi request đều có log JSON + CORS headers đúng.

**Files to create:**
```
internal/middleware/logger.go
internal/middleware/cors.go
```

**Dependencies:**
```bash
go get github.com/go-chi/cors
```

**Tasks:**
- [ ] `logger.go` — wrap `http.ResponseWriter` để capture status code, dùng `slog` (stdlib Go 1.21+):
  ```json
  {"time":"...","method":"POST","path":"/auth/login","status":200,"latency":"12ms","ip":"127.0.0.1"}
  ```
- [ ] `cors.go` — dùng `go-chi/cors`:
  ```go
  cors.Handler(cors.Options{
      AllowedOrigins: strings.Split(cfg.CORSOrigin, ","),
      AllowedMethods: []string{"GET","POST","PUT","DELETE","OPTIONS"},
      AllowedHeaders: []string{"Authorization","Content-Type","X-Request-Id"},
  })
  ```
- [ ] Gắn vào router:
  ```go
  r.Use(middleware.Logger)
  r.Use(corsMiddleware)
  ```

**Test:**
```bash
curl -v -X OPTIONS http://localhost:3000/posts \
  -H "Origin: http://localhost:5173"
# → Access-Control-Allow-Origin: http://localhost:5173
```

---

### ✅ Phase 4 — Middleware: JWT Verify

**Goal:** Protected routes yêu cầu JWT hợp lệ; inject `X-User-Id` cho upstream.

**Files to create:**
```
internal/middleware/auth.go
```

**Dependencies:**
```bash
go get github.com/golang-jwt/jwt/v5
```

**Tasks:**
- [ ] Parse `Authorization: Bearer <token>`
- [ ] Verify HMAC-SHA256 bằng `JWT_ACCESS_SECRET` (khớp với auth-service)
- [ ] Extract `userId` từ claims → inject vào:
  - `context.WithValue(r.Context(), "userId", userId)`
  - Request header `X-User-Id: <userId>` trước khi proxy
- [ ] **Skip** cho public routes: `/auth/register`, `/auth/login`, `/auth/refresh`
- [ ] Trả `401`:
  ```json
  {"error":"unauthorized","message":"invalid or expired token"}
  ```

**Apply chỉ cho protected route group:**
```go
r.Group(func(r chi.Router) {
    r.Use(middleware.JWTAuth(cfg.JWTSecret))
    r.Handle("/posts/*", postsProxy)
    r.Handle("/follow/*", followProxy)
    r.Handle("/newsfeed*", newsfeedProxy)
})
```

**Test:**
```bash
# Không có token → 401
curl http://localhost:3000/newsfeed

# Có token hợp lệ → 200
curl http://localhost:3000/newsfeed \
  -H "Authorization: Bearer <valid_jwt>"
```

---

### ✅ Phase 5 — Middleware: Rate Limiting

**Goal:** Giới hạn request per IP, chống brute-force & DDoS.

**Files to create:**
```
internal/middleware/ratelimit.go         # Strategy A: in-memory
internal/middleware/ratelimit_redis.go   # Strategy B: Redis sliding window
```

**Dependencies:**
```bash
go get golang.org/x/time/rate
go get github.com/redis/go-redis/v9
```

#### Strategy A — In-Memory (`golang.org/x/time/rate`)

```
Global:  60 req/min per IP
Write:   10 req/min per IP (POST, DELETE)
Store:   sync.Map[ip → *rate.Limiter]
Cleanup: goroutine purge idle entries every 5 min
```

#### Strategy B — Redis Sliding Window

```
Key:    ratelimit:{ip}
Script: ZADD + ZREMRANGEBYSCORE + ZCARD  (Lua atomic)
Window: 60s rolling
Limit:  60 global / 10 write
```

**Select via env:**
```env
RATE_LIMIT_BACKEND=memory   # hoặc redis
RATE_LIMIT_GLOBAL=60
RATE_LIMIT_WRITE=10              # req/min per IP for POST/DELETE
```

**Response on exceed:**
```json
{"error":"too_many_requests","message":"rate limit exceeded, retry after 1s"}
```
```
HTTP/1.1 429 Too Many Requests
Retry-After: 1
```

---

### ✅ Phase 6 — Circuit Breaker + Retry

**Goal:** Tự động cô lập upstream bị lỗi, tránh cascade failure.

**Files to create:**
```
internal/circuit/breaker.go
```

**Dependencies:**
```bash
go get github.com/sony/gobreaker
```

**Circuit Breaker Settings (per upstream):**

| Setting        | Value                    |
|----------------|--------------------------|
| MaxRequests    | 5 (half-open probe)      |
| Interval       | 30s (count reset)        |
| Timeout        | 10s (open → half-open)   |
| ReadyToTrip    | ≥ 5 consecutive failures |

**Retry Logic (GET only — idempotent):**
```
Max retries:  2
Backoff:      100ms (fixed)
Retry on:     5xx, network error
No retry on:  4xx (client error)
```

**Custom RoundTripper:**
```go
type breakerTransport struct {
    breaker   *gobreaker.CircuitBreaker
    transport http.RoundTripper
}

func (t *breakerTransport) RoundTrip(req *http.Request) (*http.Response, error) {
    resp, err := t.breaker.Execute(func() (interface{}, error) {
        return t.transport.RoundTrip(req)
    })
    // ...
}
```

**Response when circuit open:**
```json
{"error":"service_unavailable","message":"upstream posts-service is temporarily unavailable"}
```

---

### ✅ Phase 7 — Service Discovery (Consul) + Load Balancing

**Goal:** Dynamic upstream resolution thay vì static URL, hỗ trợ multiple instances.

**Files to create:**
```
internal/discovery/consul.go
internal/loadbalancer/balancer.go
internal/loadbalancer/roundrobin.go
internal/loadbalancer/weighted.go
```

**Dependencies:**
```bash
go get github.com/hashicorp/consul/api
```

**Consul Discovery Flow:**
```
Gateway startup
    │
    ├─▶ consul.Health().Service("posts-service", "", true, nil)
    │       → [instance1:3002, instance2:3002]
    │
    ├─▶ Cache local (TTL 10s)
    │
    └─▶ Background refresh goroutine (polling / Consul Watch)
```

**Load Balancer Interface:**
```go
type Balancer interface {
    Next(instances []ServiceInstance) ServiceInstance
}

// Implementations:
// - RoundRobin: atomic uint64 counter % len(instances)
// - Weighted:   weight from Consul service.Meta["weight"]
```

**Fallback Static Config:**
```env
DISCOVERY_MODE=static    # dùng static URLs bên dưới
# DISCOVERY_MODE=consul  # dùng Consul

AUTH_SERVICE_URL=http://auth-service:3001
POSTS_SERVICE_URL=http://posts-service:3002
FOLLOW_SERVICE_URL=http://follow-service:3003
NEWSFEED_SERVICE_URL=http://newsfeed-reader:3004

CONSUL_ADDR=consul:8500
LB_STRATEGY=round-robin   # hoặc weighted
```

---

### ✅ Phase 8 — Dockerfile + docker-compose Integration

**Goal:** Containerize gateway, tích hợp vào hệ thống đầy đủ.

**Files to create/update:**
```
services/api-gateway/Dockerfile
deploy/docker-compose.yml   (thêm api-gateway + consul services)
```

**Dockerfile (multi-stage):**
```dockerfile
# ── Stage 1: Build ────────────────────────────────────────────
FROM golang:1.22-alpine AS builder
WORKDIR /app
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 GOOS=linux go build -o gateway ./cmd/gateway

# ── Stage 2: Run ─────────────────────────────────────────────
FROM alpine:3.19
RUN apk --no-cache add ca-certificates tzdata
WORKDIR /app
COPY --from=builder /app/gateway .
EXPOSE 3000
ENTRYPOINT ["./gateway"]
```

**docker-compose additions:**
```yaml
api-gateway:
  build:
    context: ../services/api-gateway
  container_name: api-gateway
  ports:
    - '3000:3000'
  environment:
    PORT: 3000
    JWT_ACCESS_SECRET: change_me_in_production_access
    DISCOVERY_MODE: static
    AUTH_SERVICE_URL: http://auth-service:3001
    POSTS_SERVICE_URL: http://posts-service:3002
    FOLLOW_SERVICE_URL: http://follow-service:3003
    NEWSFEED_SERVICE_URL: http://newsfeed-reader:3004
    RATE_LIMIT_BACKEND: redis
    REDIS_ADDR: redis:6379
    CORS_ORIGIN: http://localhost:5173
  depends_on:
    - auth
    - redis
  restart: unless-stopped
  networks:
    - newsfeed-net

consul:
  image: hashicorp/consul:1.18
  container_name: consul
  ports:
    - '8500:8500'
  command: agent -dev -client=0.0.0.0
  networks:
    - newsfeed-net
```

---

## Environment Variables

```env
# ── Server ───────────────────────────────────────────────────
PORT=3000

# ── JWT ──────────────────────────────────────────────────────
JWT_ACCESS_SECRET=change_me_in_production_access

# ── Upstream (static mode) ───────────────────────────────────
AUTH_SERVICE_URL=http://auth-service:3001
POSTS_SERVICE_URL=http://posts-service:3002
FOLLOW_SERVICE_URL=http://follow-service:3003
NEWSFEED_SERVICE_URL=http://newsfeed-reader:3004
NOTIFICATIONS_SERVICE_URL=http://notifications-service:3006

# ── Rate Limiting ─────────────────────────────────────────────
RATE_LIMIT_BACKEND=memory        # memory | redis
RATE_LIMIT_GLOBAL=60             # req/min per IP
RATE_LIMIT_WRITE=10              # req/min per IP for POST/DELETE

# ── Redis (for rate limit Strategy B) ────────────────────────
REDIS_ADDR=localhost:6379

# ── Service Discovery ─────────────────────────────────────────
DISCOVERY_MODE=static            # static | consul
CONSUL_ADDR=consul:8500
LB_STRATEGY=round-robin          # round-robin | weighted

# ── CORS ─────────────────────────────────────────────────────
CORS_ORIGIN=http://localhost:5173,https://your-app.com
```

---

## Tech Stack

| Component          | Library / Tool                         |
|--------------------|----------------------------------------|
| Language           | Go 1.22+                               |
| Router             | `go-chi/chi/v5`                        |
| Reverse Proxy      | `net/http/httputil` (stdlib)           |
| JWT Verify         | `golang-jwt/jwt/v5`                    |
| Rate Limit (A)     | `golang.org/x/time/rate`               |
| Rate Limit (B)     | `redis/go-redis/v9` + Lua script       |
| Circuit Breaker    | `sony/gobreaker`                       |
| Service Discovery  | `hashicorp/consul/api`                 |
| Logger             | `log/slog` (stdlib Go 1.21+)           |
| CORS               | `go-chi/cors`                          |
| Container          | Docker multi-stage (golang + alpine)   |

---

## Further Considerations

> **gRPC Proxy:** Plan hiện tại chỉ cover HTTP reverse proxy. Nếu downstream service dùng gRPC, cần thêm `google.golang.org/grpc` + `mwitkow/grpc-proxy` — để Phase 9 riêng.

> **Graceful Shutdown:** Mỗi phase phải có `signal.NotifyContext` + `server.Shutdown(ctx)` từ Phase 1 để tránh mất in-flight requests khi deploy/restart.

> **Observability:** Sau Phase 8, có thể thêm Phase 9: Prometheus metrics (`/metrics`) + distributed tracing (OpenTelemetry).
