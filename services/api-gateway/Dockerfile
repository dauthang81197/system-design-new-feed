# ===== STAGE 1: Build =====
FROM golang:1.22 AS builder

WORKDIR /app

COPY go.mod go.sum ./
RUN go mod download

COPY . .

RUN CGO_ENABLED=0 GOOS=linux go build -o api-gateway .

# ===== STAGE 2: Runtime =====
FROM alpine:3.19

RUN apk add --no-cache ca-certificates

WORKDIR /app

COPY --from=builder /app/api-gateway .

EXPOSE 4000

CMD ["./api-gateway"]