# RateLimiter Service

A production-ready, plug-and-play distributed rate limiting microservice built with Spring Boot. Drop it alongside any backend app and protect your routes from abuse.

---

## How It Works

Any backend service makes a single HTTP call before processing a request:

```
Your App  →  POST /api/rate-limit/check?clientKey=ip:route&maxReq=5  →  RateLimiter
                                                                              │
                                                                    allowed / denied
```

- Returns `200 OK` if the client is within their limit
- Returns `429 Too Many Requests` if limit exceeded
- Logs every request asynchronously to MySQL for audit trail
- Exposes `/actuator/health` for Docker/K8s orchestration

---

## Architecture

```
Client Request
      │
      ▼
┌─────────────────────────┐
│   RateLimitController   │  ← REST layer
└────────────┬────────────┘
             │
     ┌───────┴────────┐
     ▼                ▼
┌──────────────┐   ┌──────────────┐
│ RateLimiter  │   │  LogService  │
│   Service    │   │  (@Async)    │
│  Sliding     │   │              │
│  Window      │   │              │
└──────┬───────┘   └──────┬───────┘
       │                  │
       ▼                  ▼
┌──────────────┐   ┌──────────────┐
│    Redis     │   │    MySQL     │
│ (counters)   │   │ (audit logs) │
└──────────────┘   └──────────────┘
```

### Why Redis for counting?
Redis `INCREMENT` is atomic — safe under concurrent requests without locks. Keys auto-expire via TTL so no cleanup code needed. Sub-millisecond latency means zero impact on your API response time.

### Why MySQL for logs?
Persistent, queryable audit trail. Written on a separate thread via `@Async` so the log write never blocks your response.

### Why separate service?
One rate limiter, many apps. Your auth service, your payment API — all plug into the same instance. Each route gets its own limit. Zero duplication.

---

## Tech Stack

| Layer | Tool | Reason |
|---|---|---|
| Framework | Spring Boot 3 | Production-grade, minimal config |
| Rate counting | Redis | Atomic ops, TTL, sub-millisecond |
| Audit logs | MySQL + JPA | Persistent, queryable |
| Async writes | `@Async` + Spring thread pool | Non-blocking log writes |
| Health check | Spring Actuator | Docker/K8s readiness |
| Templating | Thymeleaf | Interactive docs UI |
| Containerization | Docker + Docker Compose | Run anywhere, one command |

---

## API Reference

### API Documentation UI

```
GET /api/rate-limit
```

Serves the interactive documentation dashboard for the service.

Open in browser:

```text
http://localhost:8080/api/rate-limit
```

Features:
- Endpoint reference
- Request/response examples
- Per-route rate limit examples
- Integration guide
- Health check reference
- Interactive styled docs UI

---

### Check Rate Limit

```
POST /api/rate-limit/check
```

| Parameter | Type | Required | Default | Description |
|---|---|---|---|---|
| `clientKey` | string | ✅ | — | Unique identifier (e.g. `ip:route(Client IP + URI)`) |
| `maxReq` | int | ❌ | `10` | Max requests allowed in window |
| `resetInSeconds` | long | ❌ | `60` | Window size in seconds |

**Response `200 OK` (allowed):**
```json
{
  "allowed": true,
  "remainingRequests": 7,
  "resetInSeconds": 60
}
```

**Response `429 Too Many Requests` (denied):**
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

**Response `200 OK`:**
```json
[
  {
    "id": 1,
    "clientKey": "192.168.1.1:/{URI}",
    "ipAddress": "192.168.1.1",
    "allowed": true,
    "remainingReq": 4,
    "timestamp": "2026-05-07T11:21:51"
  }
]
```

---

### Health Check

```
GET /actuator/health
```

**Response `200 OK`:**
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "redis": { "status": "UP" },
    "diskSpace": { "status": "UP" }
  }
}
```

---

## Integrating Into Your App

### Step 1 — Get the client IP

```java
public String getClientIP(HttpServletRequest request) {
    String ip = request.getHeader("X-Forwarded-For");
    return (ip != null) ? ip : request.getRemoteAddr();
}
```

### Step 2 — Call the rate limiter before processing

```java
@PostMapping("/apiRoute")
public ResponseEntity<?> funcName(HttpServletRequest request) {

    // clientKey = [Client IP + URI]
    String clientKey = getClientIP(request) + request.getRequestURI();

    String url = "http://ratelimiter:8080/api/rate-limit/check"
               + "?clientKey=" + clientKey
               + "&maxReq=5"
               + "&resetInSeconds=60";

    ResponseEntity<RateLimitResponse> check =
        restTemplate.postForEntity(
            url,
            null,
            RateLimitResponse.class
        );

    if (!check.getBody().isAllowed()) {
        return ResponseEntity
                .status(429)
                .body("Too many requests. Slow down.");
    }

    // your actual logic here
}
```

### Step 3 — Set per-route limits independently

```java
// Strict — link creation
"?clientKey=" + ip + ":/{URI}&maxReq=5&resetInSeconds=60"

// Relaxed — reading stats
"?clientKey=" + ip + ":/{URI}&maxReq=30&resetInSeconds=60"

// Very strict — auth endpoints
"?clientKey=" + ip + ":/{URI}&maxReq=3&resetInSeconds=300"
```

`clientKey = [Client IP + URI]`

Each route gets its own independent Redis counter because clientKeys are unique for each URI.

The `clientKey` is the Redis counter key. How you construct it determines the scope of the limit.

| Strategy | clientKey format | Effect |
|---|---|---|
| IP only | `192.168.1.1` | One shared limit across all routes — not recommended |
| IP + Route ✅ | `192.168.1.1:/URI` | Independent counter per route per IP — recommended |
| API Key + Route | `apikey_abc:/URI` | For authenticated apps with API keys |
| User ID + Route | `user_123:/URI` | For logged-in users — most precise |

Note:
Replace / in the URI with - before generating the clientKey.

Example:
```bash
/api/users  →  -api-users
/auth/login →  -auth-login
```
This prevents conflicts and keeps Redis keys clean and consistent. 
---

## Running Locally

### Prerequisites
- Docker + Docker Compose

### Start

```bash
git clone https://github.com/jainish/RateLimiterService.git
cd RateLimiterService
docker compose up --build
```

App runs at:
```text
http://localhost:8080
```

Interactive docs UI:
```text
http://localhost:8080/api/rate-limit
```

### Stop

```bash
docker compose down
```

### Stop and wipe all data

```bash
docker compose down -v
```

---

## Testing

### PowerShell

```powershell
# Basic check (default limits)
curl.exe -X POST "http://localhost:8080/api/rate-limit/check?clientKey=myip:/{URI}"

# Custom limits
curl.exe -X POST "http://localhost:8080/api/rate-limit/check?clientKey=myip:/{URI}&maxReq=5&resetInSeconds=60"

# Get logs
curl.exe -X GET "http://localhost:8080/api/rate-limit/logs/myip:/{URI}"

# Health
curl.exe -X GET "http://localhost:8080/actuator/health"
```

### curl (Linux / Mac / CMD)

```bash
curl -X POST "http://localhost:8080/api/rate-limit/check?clientKey=myip:/{URI}&maxReq=5&resetInSeconds=60"

curl -X GET "http://localhost:8080/api/rate-limit/logs/myip:/{URI}"

curl -X GET "http://localhost:8080/actuator/health"
```

Hit the check endpoint 6 times with `maxReq=5` — first 5 return `200`, 6th returns `429`.

---

## Configuration

All config is passed via environment variables in `docker-compose.yml`:

| Variable | Default | Description |
|---|---|---|
| `SPRING_REDIS_HOST` | `redis` | Redis hostname |
| `SPRING_DATASOURCE_URL` | MySQL container | Full JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `root` | MySQL user |
| `SPRING_DATASOURCE_PASSWORD` | `passwd` | MySQL password |

Per-request limits (`maxReq`, `resetInSeconds`) are passed by the calling app — the rate limiter has no hardcoded business logic.

---

## Project Structure

```
src/main/java/com/jainish/ratelimiter/
├── controller/
│   └── RateLimitController.java
├── service/
│   ├── RateLimiterService.java
│   └── LogService.java
├── entity/
│   ├── RateLimitResponse.java
│   └── RequestLog.java
├── repository/
│   └── RequestLogRepository.java
└── config/
    └── RedisConfig.java
```

---

## License

MIT
