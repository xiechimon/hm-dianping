# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A Dianping-like (点评) local life services platform with shop reviews, user authentication, vouchers, and seckill functionality. Built with Spring Boot 3.x + MyBatis Plus + Redis + MySQL, with a Vue.js frontend served via Nginx.

## Build & Run Commands

```bash
# Build the project
mvn clean package -DskipTests

# Run locally (requires MySQL and Redis on localhost)
mvn spring-boot:run

# Run tests
mvn test

# Run a single test class
mvn test -Dtest=ClassName

# Docker Compose (full stack: MySQL, Redis, Backend, Nginx)
docker-compose up -d
```

## Architecture

### Layered Structure
- **Controller**: REST endpoints in `com.xmon.controller`
- **Service**: Business logic in `com.xmon.service.impl` (MyBatis Plus `ServiceImpl`)
- **Mapper**: Data access in `com.xmon.mapper` (MyBatis Plus `Mapper`)

### Authentication Flow
Two-interceptor chain in `MvcConfig`:
1. `RefreshTokenInterceptor` (order=0): Extracts token from `Authorization` header, looks up user in Redis (`login:token:*`), stores `UserDTO` in `UserHolder` (ThreadLocal), refreshes token TTL
2. `LoginInterceptor` (order=1): Checks if `UserHolder` has a user; returns 401 if not

Public paths (excluded from login check): `/user/code`, `/user/login`, `/blog/hot`, `/shop-type/**`, `/upload/**`, `/voucher/**`, `/voucher-order/**`, `/shop/**`

### Redis Caching Patterns
Located in `com.xmon.utils.CacheClient`:
- `queryWithPassThrough`: Cache-aside with null value caching to prevent cache penetration
- `queryWithLogicalExpire`: Logical expiration for hot data, with mutex lock + background rebuild to prevent cache breakdown
- `setWithLogicalExpire`: Writes data with separate `expireTime` field in `RedisData`

Redis key prefixes in `RedisConstants`: `cache:shop:*`, `cache:shop-type:list`, `lock:shop:*`, `login:token:*`, `login:code:*`, `seckill:stock:*`, `blog:liked:*`, `feed:*`, `shop:geo:*`, `sign:*`

### Global Unique IDs
`RedisIdWorker`: Timestamp (seconds since `BEGIN_TIMESTAMP=1767225600`) << 32 bits + daily incrementing counter. Key format: `icr:{prefix}:{yyyy:MM:dd}`

### User Context
`UserHolder`: ThreadLocal wrapper for `UserDTO`. Always call `removeUser()` in `afterCompletion` (both interceptors do this).

### Response Format
All controllers return `Result.ok(data)` or `Result.fail(message)`. `Result` contains `success` boolean and `code`/`data`/`message` fields.

## Key Files
- `init-sql/hmdp.sql`: Database schema
- `docker-compose.yml`: Full local infrastructure (MySQL 8.0, Redis 7.0, Nginx 1.25, Spring Boot backend)
- `nginx/nginx.conf`: Reverse proxy to backend at `localhost:8081`
- `nginx/html/hmdp/`: Vue.js frontend static assets
- `application.yml`: Local config (port 8081, MySQL/Redis on localhost)

## Technology Stack
- Spring Boot 3.5.9, Java 21
- MyBatis Plus 3.5.15 (base mapper/service annotations)
- Redis (Spring Data Redis, Lettuce client)
- MySQL 8.0
- Hutool 5.8.42 (utility library)
- Lombok
