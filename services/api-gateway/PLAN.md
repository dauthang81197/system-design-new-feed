# 🚀 Build Plan — Go API Gateway

> Chuyển API Gateway từ **NestJS → Go**  
> 8 phase độc lập, mỗi phase **build & test được riêng lẻ**

---

## 📋 Phase Overview

| #  | Phase                              | Layer           | Key Lib                        | Status      |
|----|------------------------------------|-----------------|--------------------------------|-------------|
| 1  | Go Project Skeleton + Chi Router   | Foundation      | `go-chi/chi/v5`                | ⬜ Todo     |
| 2  | Reverse Proxy                      | Routing         | `net/http/httputil`            | ⬜ Todo     |
| 3  | Logger + CORS Middleware           | Observability   | `slog`, `go-chi/cors`          | ⬜ Todo     |
| 4  | JWT Verify Middleware              | Security        | `golang-jwt/jwt/v5`            | ⬜ Todo     |
| 5  | Rate Limiting Middleware           | Protection      | `x/time/rate`, `go-redis/v9`   | ⬜ Todo     |
| 6  | Circuit Breaker + Retry            | Resilience      | `sony/gobreaker`               | ⬜ Todo     |
| 7  | Service Discovery + Load Balancing | Scalability     | `hashicorp/consul/api`         | ⬜ Todo     |
| 8  | Dockerfile + docker-compose        | Deployment      | Docker multi-stage             | ⬜ Todo     |

---

## 🔁 Middleware Pipeline

```
Request
  │
  ▼
┌─────────────────────────────────────────────────────────────────┐
│  1. Logger       — log method, path, status, latency, IP        │
│  2. CORS         — set Access-Control-* headers                 │
│  3. Rate Limit   — 429 nếu vượt limit (chạy TRƯỚC JWT!)        │
│  4. JWT Verify   — 401 nếu token invalid (protected routes)     │
│  5. Circuit Breaker — 503 nếu upstream đang lỗi                 │
│  6. Reverse Proxy — forward tới upstream service                │
└─────────────────────────────────────────────────────────────────┘
  │
  ▼
Upstream Service (:3001 / :3002 / :3003 / :3004)
```

---

## 📦 Target File Structure

```
services/api-gateway/
├── cmd/gateway/
│   └── main.go                      ← entry point + graceful shutdown
├── internal/
│   ├── config/config.go             ← load env vars
│   ├── proxy/proxy.go               ← httputil.ReverseProxy per upstream
│   ├── middleware/
│   │   ├── logger.go                ← structured JSON log (slog)
│   │   ├── cors.go                  ← go-chi/cors
│   │   ├── auth.go                  ← JWT verify + X-User-Id inject
│   │   ├── ratelimit.go             ← in-memory (x/time/rate)
│   │   └── ratelimit_redis.go       ← Redis sliding window (Lua)
│   ├── circuit/breaker.go           ← gobreaker + retry RoundTripper
│   ├── discovery/consul.go          ← Consul health query + cache
│   └── loadbalancer/
│       ├── balancer.go              ← interface Balancer
│       ├── roundrobin.go            ← atomic counter
│       └── weighted.go              ← Consul metadata weight
├── go.mod / go.sum
├── Dockerfile
└── README.md
```

---

## 🔨 Phase Details

### Phase 1 — Go Project Skeleton + Chi Router

```
Goal: Server chạy, route table đủ 14 routes, health check OK
```

**Files:** `cmd/gateway/main.go`, `internal/config/config.go`, `go.mod`

```bash
go mod init github.com/your-org/api-gateway
go get github.com/go-chi/chi/v5
```

- [ ] `chi.NewRouter()` + đăng ký 14 routes → placeholder `200 OK`
- [ ] `GET /health` → `{"status":"ok"}`
- [ ] Load env: `PORT`, `JWT_ACCESS_SECRET`, upstream URLs
- [ ] `signal.NotifyContext` + `server.Shutdown(ctx)` (graceful shutdown)

```bash
# Test
curl http://localhost:3000/health   # → {"status":"ok"}
```

---

### Phase 2 — Reverse Proxy (`httputil.ReverseProxy`)

```
Goal: Gateway forward thực sự xuống upstream (không cần lib ngoài)
```

**Files:** `internal/proxy/proxy.go`

- [ ] `httputil.NewSingleHostReverseProxy(target)` × 4 upstream
- [ ] Custom `Director`: strip prefix, forward `X-Request-Id` / `X-Forwarded-For`
- [ ] Chi route groups:
  ```go
  r.Route("/auth",     func(r chi.Router) { r.Handle("/*", authProxy) })
  r.Route("/posts",    func(r chi.Router) { r.Handle("/*", postsProxy) })
  r.Route("/follow",   func(r chi.Router) { r.Handle("/*", followProxy) })
  r.Route("/newsfeed", func(r chi.Router) { r.Handle("/*", newsfeedProxy) })
  ```

```bash
# Test (upstream auth-service phải chạy trên :3001)
curl -X POST http://localhost:3000/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test"}'
```

---

### Phase 3 — Middleware: Logger + CORS

```
Goal: Mọi request có log JSON chuẩn + CORS headers đúng
```

**Files:** `internal/middleware/logger.go`, `internal/middleware/cors.go`

```bash
go get github.com/go-chi/cors
```

- [ ] `logger.go` — `slog` (stdlib Go 1.21+), capture status + latency:
  ```json
  {"time":"...","method":"POST","path":"/auth/login","status":200,"latency":"12ms","ip":"127.0.0.1"}
  ```
- [ ] `cors.go` — `go-chi/cors`, `AllowedOrigins` từ env `CORS_ORIGIN`
- [ ] `r.Use(loggerMiddleware, corsMiddleware)` — gắn vào router

```bash
# Test CORS
curl -v -X OPTIONS http://localhost:3000/posts \
  -H "Origin: http://localhost:5173"
# → Access-Control-Allow-Origin: http://localhost:5173
```

---

### Phase 4 — Middleware: JWT Verify

```
Goal: Protected routes yêu cầu JWT hợp lệ; inject X-User-Id cho upstream
```

**Files:** `internal/middleware/auth.go`

```bash
go get github.com/golang-jwt/jwt/v5
```

- [ ] Parse `Authorization: Bearer <token>`
- [ ] Verify HMAC-SHA256 bằng `JWT_ACCESS_SECRET`
- [ ] Extract `userId` → `context` + header `X-User-Id`
- [ ] Skip public: `/auth/register`, `/auth/login`, `/auth/refresh`
- [ ] `401` response:
  ```json
  {"error":"unauthorized","message":"invalid or expired token"}
  ```

```go
// Protected group
r.Group(func(r chi.Router) {
    r.Use(middleware.JWTAuth(cfg.JWTSecret))
    r.Handle("/posts/*",    postsProxy)
    r.Handle("/follow/*",   followProxy)
    r.Handle("/newsfeed*",  newsfeedProxy)
})
```

---

### Phase 5 — Middleware: Rate Limiting

```
Goal: Giới hạn req/IP, chống brute-force & DDoS
```

**Files:** `internal/middleware/ratelimit.go`, `internal/middleware/ratelimit_redis.go`

```bash
go get golang.org/x/time/rate
go get github.com/redis/go-redis/v9
```

#### Strategy A — In-Memory

| Scope  | Limit       | Storage                  |
|--------|-------------|--------------------------|
| Global | 60 req/min  | `sync.Map[ip→*Limiter]`  |
| Write  | 10 req/min  | (POST, DELETE only)      |

#### Strategy B — Redis Sliding Window

| Item   | Detail                                     |
|--------|--------------------------------------------|
| Key    | `ratelimit:{ip}`                           |
| Script | Lua: `ZADD + ZREMRANGEBYSCORE + ZCARD`     |
| Window | 60s rolling                                |

```env
RATE_LIMIT_BACKEND=memory   # memory | redis
RATE_LIMIT_GLOBAL=60
RATE_LIMIT_WRITE=10
```

```
HTTP 429 Too Many Requests
Retry-After: 1
{"error":"too_many_requests","message":"rate limit exceeded, retry after 1s"}
```

---

### Phase 6 — Circuit Breaker + Retry

```
Goal: Tự cô lập upstream lỗi, tránh cascade failure
```

**Files:** `internal/circuit/breaker.go`

```bash
go get github.com/sony/gobreaker
```

| Setting     | Value                       |
|-------------|-----------------------------|
| MaxRequests | 5 (half-open probe)         |
| Interval    | 30s                         |
| Timeout     | 10s (open → half-open)      |
| ReadyToTrip | ≥ 5 consecutive failures    |

**Retry (GET only):**

| Setting    | Value               |
|------------|---------------------|
| Max retry  | 2                   |
| Backoff    | 100ms fixed         |
| Retry on   | 5xx, network error  |
| No retry   | 4xx                 |

```
HTTP 503 Service Unavailable
{"error":"service_unavailable","message":"upstream posts-service is temporarily unavailable"}
```

---

### Phase 7 — Service Discovery (Consul) + Load Balancing

```
Goal: Dynamic upstream resolution, hỗ trợ multiple instances
```

**Files:** `internal/discovery/consul.go`, `internal/loadbalancer/*.go`

```bash
go get github.com/hashicorp/consul/api
```

**Discovery Flow:**
```
Startup → consul.Health().Service("posts-service")
        → cache local TTL 10s
        → background refresh goroutine
```

**Balancer Interface:**
```go
type Balancer interface {
    Next(instances []ServiceInstance) ServiceInstance
}
// RoundRobin: atomic uint64 % len(instances)
// Weighted:   Consul service.Meta["weight"]
```

```env
DISCOVERY_MODE=static   # static | consul
CONSUL_ADDR=consul:8500
LB_STRATEGY=round-robin # round-robin | weighted
```

> Fallback: nếu `DISCOVERY_MODE=static` → dùng `*_SERVICE_URL` tĩnh

---

### Phase 8 — Dockerfile + docker-compose

```
Goal: Containerize, tích hợp đầy đủ vào hệ thống
```

**Files:** `Dockerfile`, `deploy/docker-compose.yml`

**Dockerfile (multi-stage):**
```dockerfile
FROM golang:1.22-alpine AS builder
WORKDIR /app
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 GOOS=linux go build -o gateway ./cmd/gateway

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
  build: { context: ../services/api-gateway }
  ports: ['3000:3000']
  depends_on: [auth, redis]

consul:
  image: hashicorp/consul:1.18
  ports: ['8500:8500']
  command: agent -dev -client=0.0.0.0
```

---

## ⚠️ Key Notes

| #  | Note                                                                                              |
|----|---------------------------------------------------------------------------------------------------|
| 1  | **Middleware order:** Logger → CORS → RateLimit → JWT → CircuitBreaker → Proxy                   |
| 2  | **Rate limit trước JWT** để chặn brute-force trước khi tốn CPU verify token                     |
| 3  | **Graceful shutdown** phải có từ Phase 1: `signal.NotifyContext` + `server.Shutdown(ctx)`        |
| 4  | **gRPC proxy** không nằm trong 8 phases này — thêm Phase 9 nếu cần (`mwitkow/grpc-proxy`)       |
| 5  | **Observability** (Phase 9+): Prometheus `/metrics` + OpenTelemetry distributed tracing          |

