# Shortline — Distributed URL Shortener

A production-style URL shortener built to explore how real systems handle scale: caching, load balancing, rate limiting, and horizontal scaling — not just a CRUD wrapper around a database.

**Live demo:** https://url-shortener-frontend-9ej4.onrender.com
**API base:** https://url-shortener-1-3nh5.onrender.com

> Hosted on Render's free tier — the first request after a period of inactivity may take 30–60 seconds while the instance wakes up.
>
> The live demo runs a single backend instance for simplicity. The load balancer and multi-instance routing described below are fully implemented and tested locally (round-robin across 3 instances, redirect passthrough, IP-forwarded rate limiting) — see [Running locally](#running-locally) to run the full multi-instance setup.

## Why this project

Most "URL shortener" projects stop at generating a short code and saving it to a database. This one is built around the actual engineering problems a service like this hits at scale:

- How do you generate short codes without collisions, at speed?
- How do you keep redirects fast when there are millions of them?
- How do you protect the service from abuse without blocking real users?
- How do you scale beyond one server, and route traffic across many?

Each of those maps to a specific design decision below.

## Architecture

```
Client
  │
  ▼
Load Balancer (round-robin)
  │
  ├──► App Server 1 ──┐
  ├──► App Server 2 ──┼──► Redis (cache-aside) ──► Postgres (sharding-ready)
  └──► App Server 3 ──┘
```

This is the full design, implemented and tested with 3 local instances. The live demo above runs a single instance of the app server directly (no load balancer in front) to keep the free-tier footprint small — the load balancer module is in this repo and runs the same way locally.

1. A request hits the load balancer, which round-robins across app server instances.
2. Each app server checks Redis first for the short code. On a hit, it redirects immediately.
3. On a cache miss, it queries Postgres, redirects, and repopulates the cache for next time.
4. Every app server applies Redis-backed rate limiting per client IP, using the real client IP forwarded through the load balancer via `X-Forwarded-For`.

## Design decisions

**Base62 encoding, not UUIDs.** Short codes are generated from an auto-incrementing sequence, encoded into base62 (`0-9a-zA-Z`). This keeps codes short and collision-free without needing a lookup-and-retry loop.

**Cache-aside, not write-through.** Reads check Redis first and fall back to Postgres on a miss, repopulating the cache afterward. This keeps the cache simple and lets Postgres remain the source of truth.

**302, not 301, redirects.** A 301 (permanent redirect) would let browsers cache the redirect locally and stop hitting the server — which also means losing click analytics. A 302 keeps every click flowing through the server, at a small performance cost, in exchange for accurate analytics.

**Rate limiting by real client IP.** Behind a load balancer, `request.getRemoteAddr()` returns the load balancer's IP, not the client's — which would make rate limiting useless (it would throttle "the load balancer" as a single user). The load balancer forwards the original IP via `X-Forwarded-For`, and the backend reads that header first.

**Explicit redirect handling in the load balancer.** Spring's `RestClient` can be configured to auto-follow redirects, which would break this system silently — the load balancer would follow the 302 itself and return a 200 to the browser instead of passing the redirect through. The load balancer's HTTP client is explicitly configured with `Redirect.NEVER` so the real 302 and `Location` header always reach the browser.

## Tech stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot |
| Database | PostgreSQL (Neon) |
| Cache / rate limiting | Redis |
| Load balancer | Spring Boot (custom, round-robin) |
| Frontend | React, Vite |
| Deployment | Docker, Render |

## Running locally

**Backend**
```bash
cd url-shortener
export DB_URL=jdbc:postgresql://localhost:5432/url_shortener
export DB_USERNAME=postgres
export DB_PASSWORD=your_password
export REDIS_HOST=localhost
./mvnw spring-boot:run
```

**Frontend**
```bash
cd url-shortener-frontend
npm install
npm run dev
```

**Load balancer** (optional — only needed to test multi-instance routing)
```bash
cd url-shortener-load-balancer
./mvnw spring-boot:run
```

## API

**Create a short URL**
```
POST /api/urls/shorten
Content-Type: application/json

{ "originalUrl": "https://example.com/some/long/path" }
```

**Follow a short URL**
```
GET /{shortCode}
```
Returns a 302 redirect to the original URL.

## What I'd change for a larger scale

- Move rate limiting to a token-bucket implementation with configurable burst limits, rather than a fixed window.
- Add real database sharding (this project simulates the routing logic; a production version would shard actual Postgres instances).
- Replace round-robin with least-connections routing once traffic is uneven across instances.
