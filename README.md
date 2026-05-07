# RateLimiter Service

A production-ready distributed rate limiting microservice built with Spring Boot. Uses Redis for atomic sliding window counting and MySQL for persistent async audit logging. Fully containerized with Docker Compose.

---

## What It Does

Any service can call this API to check if a client has exceeded their request quota.

- ✅ Returns `200 OK` with remaining requests if allowed
- ❌ Returns `429 Too Many Requests` if limit exceeded
- 📋 Logs every request asynchronously to MySQL
- ❤️ Exposes health check endpoint for orchestration

---

## Architecture

```
Client Request
      │
      ▼
┌─────────────────────┐
│  RateLimitController│  ← REST layer, sets X-RateLimit-* headers
└────────┬────────────┘
         │
         ├──────────────────────────────────┐
         ▼                                  ▼
┌─────────────────────┐          ┌─────────────────────┐
│  RateLimiterService │          │     LogService       │
│  (Sliding Window)   │          │  (@Async writes)     │
└────────┬────────────┘          └──────────┬──────────┘
         │                                  │
         ▼                                  ▼
┌─────────────────────┐          ┌─────────────────────┐
│       Redis         │          │       MySQL          │
│  (atomic counters)  │          │   (audit logs)       │
└─────────────────────┘          └─────────────────────┘
```

### Why Redis for counting?
Redis `INCREMENT` is atomic — safe under concurrent requests without locks. Counters auto-expire with TTL so no cleanup needed.

### Why MySQL for logs?
Persistent, queryable audit trail. Written asynchronously so it never slows down the response.

---

## Tech Stack

| Layer | Tool | Reason |
|---|---|---|
| Framework | Spring Boot 3 | Production-grade, fast setup |
| Rate counting | Redis | Atomic ops, TTL, sub-millisecond |
| Audit logs | MySQL + JPA | Persistent, queryable |
| Async writes | `@Async` + Spring thread pool | Non-blocking log writes |
| Health check | Spring Actuator | Docker/K8s readiness |
| Containerization | Docker + Docker Compose | Run anywhere |

---

## API Endpoints

### Check Rate Limit
```
POST /api/rate-limit/check?clientKey={key}
```

**Response Body (allowed):** `200 OK`
```json
{
  "allowed": true,
  "remainingRequests": 7,
  "resetInSeconds": 60
}
```

**Response Body (denied):** `429 Too Many Requests`
```json
{
  "allowed": false,
  "remainingRequests": 0,
  "resetInSeconds": 60
}
```

---

### Get Logs for a Client
```
GET /api/rate-limit/logs/{clientKey}
```

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "clientKey": "user123",
    "ipAddress": "172.18.0.1",
    "allowed": true,
    "remainingReq": 3,
    "timestamp": "2026-01-15T10:30:00"
  }
]
```

---

### Health Check
```
GET /actuator/health
```

**Response:** `200 OK`
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "redis": { "status": "UP" }
  }
}
```

---

## Running Locally

### Prerequisites
- Docker + Docker Compose
- That's it!

### Start Everything
```bash
git clone https://github.com/jainish/RateLimiterService.git
cd RateLimiterService
docker-compose up --build
```

App runs at `http://localhost:8080`

### Stop
```bash
docker-compose down
```

### Stop and wipe data
```bash
docker-compose down -v
```

---

## Testing

**PowerShell:**
```powershell
# Check rate limit
curl.exe -X POST "http://localhost:8080/api/rate-limit/check?clientKey=user123"

# Get logs
curl.exe -X GET "http://localhost:8080/api/rate-limit/logs/user123"

# Health check
curl.exe -X GET "http://localhost:8080/actuator/health"
```

**Command Prompt / Linux / Mac:**
```bash
# Check rate limit
curl -X POST "http://localhost:8080/api/rate-limit/check?clientKey=user123"

# Get logs
curl -X GET "http://localhost:8080/api/rate-limit/logs/user123"

# Health check
curl -X GET "http://localhost:8080/actuator/health"
```

Hit the check endpoint 11 times — first 10 return `200`, 11th returns `429`.

---

## Configuration

Edit `docker-compose.yml` or pass as environment variables:

| Property | Default | Description |
|---|---|---|
| `rate.limiter.max-requests` | `10` | Max requests per window |
| `rate.limiter.window-seconds` | `60` | Window size in seconds |
| `spring.redis.host` | `docker container` | Redis host |
| `spring.datasource.url` | `docker container` | MySQL JDBC URL |

---

## Project Structure

```
src/main/java/com/jainish/ratelimiter/
├── controller/
│   └── RateLimitController.java     # REST endpoints, response headers
├── service/
│   ├── RateLimiterService.java      # Sliding window logic with Redis
│   └── LogService.java              # Async log writes to MySQL
├── model/
│   ├── RateLimitResponse.java       # API response model
│   └── RequestLog.java              # JPA entity for audit logs
├── repository/
│   └── RequestLogRepository.java    # Spring Data JPA repo
└── config/
    └── RedisConfig.java             # StringRedisTemplate bean
```

---