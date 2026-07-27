![Frontend](https://img.shields.io/badge/Frontend-React.js-blue.svg)
![Backend](https://img.shields.io/badge/Backend-Spring%20Boot-green.svg)
![Database](https://img.shields.io/badge/Database-MySQL-white.svg)
![Cache](https://img.shields.io/badge/Cache-Redis-red.svg)
![Container](https://img.shields.io/badge/Container-Docker-blue.svg)
![License](https://img.shields.io/badge/License-GPL--3.0-yellow.svg)

<div align="center">
  <img src="https://github.com/user-attachments/assets/3438953d-9596-41fd-9570-2c0ec3713657" alt="The Website Logo" width="200" />
</div>

# Seamail: An Email Service
Seamail is a full-stack email service built around the `@seamail.com` domain. It provides secure, efficient, and user-friendly email management through an intuitive interface, backed by a Spring Boot microservices architecture: JWT-based authentication with automatic token refresh, Redis for token storage and inbox caching, Kafka for event-driven notifications, and a fully versioned REST API behind a single API gateway.

# Features
- **User Registration & Sign-in:** Secure registration and login with server-side BCrypt password hashing.
- **OAuth2 Authentication:** Allows users to optionally sign in with their Discord account via a custom server-side OAuth2 callback with CSRF state validation and ticket-based token exchange.
- **JWT Authentication:** Stateless Bearer token authentication with 30-minute access tokens and automatic silent refresh via rotating refresh tokens. Refresh tokens are revoked on account deletion and password change.
- **Automatic Token Refresh:** A centralised axios interceptor detects expired tokens, silently exchanges the refresh token for a new pair, and retries the original request without interrupting the user.
- **Multi-language Support:** Enhances accessibility by making the platform available in English, German, and French via i18next.
- **Email Management:** Allows users to view and manage inbox, outbox, and trashbox for efficient email organisation.
- **Email Actions:** Send, move to trash, and permanently delete emails directly from any mailbox.
- **Email Sorting & Filtering:** Sort emails by priority or date, and filter them by subject or sender, all via a single unified query endpoint.
- **Password Management:** Allows users to securely change their password (requiring current password verification) to maintain account security.
- **Account Management:** Allows users to permanently delete their accounts and change their default profile picture (PNG or JPEG, max 5 MB).
- **Event-Driven Notifications:** When an email is sent, mail-service publishes an `EmailSentEvent` to Kafka; notification-service consumes it idempotently and exposes a personal notification feed (per-recipient unread counts, mark-as-read) over REST.
- **Redis Caching:** Caches inbox emails per user using Redis to reduce database load and improve response times. Cache is keyed by user + page + size and automatically invalidated (all entries) when emails are received, moved to trash, or deleted. Redis also stores refresh tokens with a 7-day TTL and automatic rotation on every use, plus Discord OAuth tickets (60s) and CSRF state nonces (5min).

---

# Architecture Overview

```
                       ┌────────────────────────────────────────────┐
                       │              React SPA / nginx             │
                       │   TailwindCSS · React Router · i18next      │
                       │   apiClient.js · parseApiError.js           │
                       └─────────────────────┬──────────────────────┘
                                             │ /api/v1 (HTTPS)
                       ┌─────────────────────▼──────────────────────┐
                       │            api-gateway  :8081              │
                       │  Spring Cloud Gateway · CORS · Swagger UI  │
                       │  single entry point; routes /api/v1/**     │
                       └──────┬──────────┬───────────────┬───────────┘
                              │          │               │
                ┌──────────────▼┐  ┌──────▼───────┐  ┌────▼─────────────┐
                │ auth-service   │  │ mail-service │  │  notification-  │
                │   :8082        │◀─┤   :8083      │  │   service :8084  │
                │ RS256 + JWKS   │  │ resource     │  │ resource server │
                │ refresh rotate │  │ server +     │  │ + idempotent    │
                │ Discord OAuth  │  │ Feign check  │  │ Kafka consumer  │
                └───────┬────────┘  └──────┬───────┘  └────────┬────────┘
                        │ JWKS             │ EmailSentEvent    │
                        │ pub keys ────────┤ (AFTER_COMMIT)    │
                        │                  ▼                   │ consume
                   ┌────▼───┐         ┌──────────────┐         │
                   │ Redis  │         │  Kafka KRaft │─────────┘
                   │ tokens │         │  email.sent   │
                   │ inbox  │         │  + email.sent │
                   │ cache  │         │    .DLT       │
                   └────────┘         └──────────────┘

     MySQL 8.0 (one server, three Flyway-managed schemas):
     ┌─────────────┬─────────────┬──────────────────────────┐
     │ seamail_auth│ seamail_mail│ seamail_notifications     │
     └─────────────┴─────────────┴──────────────────────────┘
```

**Key design decisions:**
- **Versioned REST API:** all endpoints live under `/api/v1`, making future versioning straightforward.
- **Asymmetric JWT signing with JWKS:** auth-service signs access tokens with an RS256 private key and publishes only the public keys at `/.well-known/jwks.json`; mail-service and notification-service validate them statelessly as OAuth2 resource servers, so no shared secret is ever distributed and key rotation just means publishing a new `kid`.
- **Stateless security via OAuth2 resource servers:** no HTTP sessions are held server-side; controllers read the caller email from `@AuthenticationPrincipal(expression = "subject")`. An expired or malformed token yields a clean 401 from the resource-server authentication entry point, not a 500.
- **After-commit Kafka publishing:** `EmailSentEvent` is published via `@TransactionalEventListener(AFTER_COMMIT)`, so consumers never observe rolled-back writes. Combined with the idempotent consumer on the other side of the topic, this gives safe at-least-once delivery on a per-event basis without introducing a transactional outbox table.
- **Idempotent consumer with a dead-letter topic:** Kafka delivers at-least-once, so notification-service deduplicates on a unique `event_id` (check-then-insert guarded by a unique constraint) and routes poison pills to `email.sent.DLT` after bounded exponential backoff instead of blocking the partition.
- **Atomic refresh-token rotation:** every `POST /api/v1/auth/refresh` atomically claims the old Redis refresh key via `delete()` (only one concurrent refresh wins; the loser is rejected with 401) and issues a new access + refresh pair. Tokens are also revoked on account deletion and password change.
- **Discord OAuth ticket flow:** `GET /auth/discord/state` generates a CSRF nonce (5-min TTL in Redis), the frontend fetches it before redirecting to Discord. On callback, `GET /auth/discord` validates the state, exchanges the code with Discord, stores an opaque 60-second ticket in Redis, and 302-redirects to `/home?code=<ticket>`. The SPA then `POST /auth/exchange`s the ticket for tokens. JWTs never appear in URLs.
- **Feign receiver check before persist:** before saving an email, mail-service calls auth-service's `/internal/users/{email}/exists` via Spring Cloud OpenFeign; a 404 surfaces as `ReceiverNotFoundException`, and a timeout or 5xx returns 503 with a `SERVICE_UNAVAILABLE` error code so the sender gets a clear "auth service unavailable" message.
- **Focused caching:** only the inbox (the highest-traffic read) is cached in Redis, keyed by user + page + size; cache is evicted automatically (all entries) on send, trash, or delete actions, trading fine-grained invalidation for guaranteed consistency.
- **Per-service schemas with Flyway + `ddl-auto=validate`:** one MySQL 8.0 server hosts three schemas (`seamail_auth`, `seamail_mail`, `seamail_notifications`), each owned by one service with its own Flyway migrations and a database user scoped to that schema. Hibernate validation is kept on as a drift detector that fails startup if an entity diverges from the migrated schema.
- **Spring Cloud Gateway as the only public entry point:** all `/api/v1/**` traffic routes through `:8081`; CORS is enforced only here (downstream services trust the gateway origin), and `/internal/**` endpoints are not exposed through the gateway. Swagger UI is aggregated at `/swagger-ui.html` so all three services appear in one place.
- **Centralised exception handling:** a `@RestControllerAdvice` in every service maps every custom domain exception to a consistent JSON error shape (`ErrorResponse` / `ValidationErrorResponse`) with HTTP status, machine-readable error code, message, path, and timestamp. It also maps malformed body, type mismatch, missing parameter, and data integrity violation exceptions to appropriate 400/409 responses. A custom OAuth2 `AuthenticationEntryPoint` keeps the 401 body in the same shape so the frontend `parseApiError.js` keeps working.
- **Integration tests on real infrastructure:** Testcontainers ITs run against real MySQL, Redis, and Kafka containers using `@ServiceConnection`, so Flyway migrations, JSON Kafka serialisation, and Redis rotation logic are verified against the same engines used in deployment rather than in-memory stand-ins. The surefire/failsafe split keeps `mvn test` Docker-free and routes `*IT` classes to `mvn verify`.
- **End-to-end distributed tracing:** a single action (sending an email) produces one Zipkin trace crossing api-gateway -> mail-service -> Kafka -> notification-service, with trace/span IDs in every log line for cross-service correlation. Sampling is `1.0` in dev.

---

# Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React 18, Vite, TailwindCSS, React Router v7, Axios |
| Internationalisation | i18next / react-i18next (EN, DE, FR) |
| Backend | Spring Boot 3.2.3, Spring Cloud (Gateway, OpenFeign), spring-kafka, Java 21, Maven |
| Security | Spring Security, RS256 JWT (Nimbus JOSE) with JWKS, OAuth2 resource servers, BCrypt, Discord OAuth2, Spring Cloud Gateway |
| Database | MySQL 8.0 (per-service schemas, versioned with Flyway migrations) |
| Messaging | Apache Kafka (KRaft) |
| Caching & Token Store | Redis via Spring Cache + Spring Data Redis (`@Cacheable` / `@CacheEvict` / `StringRedisTemplate`); local container in dev |
| Observability | Micrometer, Prometheus, Zipkin (distributed tracing), Grafana |
| API Docs | springdoc-openapi (Swagger UI aggregated at the gateway) |
| Monitoring | Spring Boot Actuator (health, liveness, readiness, Prometheus endpoint) |
| Infrastructure | Docker, Nginx |
| Backend Testing | JUnit 5, `@WebMvcTest`, `@DataJpaTest`, Mockito, H2 (slices), Testcontainers (integration) |
| Frontend Testing | Vitest, React Testing Library, MSW (Mock Service Worker), axios-mock-adapter |

---

# API Reference

All protected endpoints require `Authorization: Bearer <accessToken>`.  
The token is a 30-minute RS256 JWT signed by auth-service; resource servers validate it via the public keys at `GET /.well-known/jwks.json`. Use `POST /api/v1/auth/refresh` to renew it silently.

## Auth `/api/v1`

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/sign-in` | Public | Authenticates a user; returns `{ accessToken, refreshToken }` |
| POST | `/sign-up` | Public | Registers a new `@seamail.com` account; returns `{ accessToken, refreshToken }` (201) |
| POST | `/auth/refresh` | Public | Exchanges a refresh token for a new access + refresh pair (rotation) |
| GET | `/auth/discord/state` | Public | Returns a CSRF state nonce `{ state: "..." }` (5-min TTL) |
| GET | `/auth/discord` | Public | Discord OAuth2 callback; 302 redirects to frontend `/home?code=<opaque ticket>` |
| POST | `/auth/exchange` | Public | Exchanges a Discord ticket for `{ accessToken, refreshToken, email }` (60s single-use) |

## Emails `/api/v1`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/inbox` | Bearer | Returns active inbox emails as `Page<EmailResponseDto>` (`?page=0&size=20`) |
| GET | `/outbox` | Bearer | Returns sent emails as `Page<EmailResponseDto>` (`?page=0&size=20`) |
| GET | `/trashbox` | Bearer | Returns trashed emails as `Page<EmailResponseDto>` (`?page=0&size=20`) |
| GET | `/emails` | Bearer | Query endpoint; supports `?sort=priority\|date`, `?filterBy=subject\|sender&filterValue=`, `?mailbox=Inbox\|Outbox\|Trashbox`, `?page=0&size=20` |
| POST | `/send-email` | Bearer | Sends a new email to a specified recipient (201 empty body) |
| POST | `/move-to-trash` | Bearer | Moves a specified email to trash (204) |
| DELETE | `/delete-email` | Bearer | Permanently deletes a specified email (204) |

## Users `/api/v1`

| Method | Path | Auth | Description |
|---|---|---|---|
| PUT | `/change-password` | Bearer | Updates the authenticated user's password (requires `currentPassword` in body) |
| PUT | `/update-language` | Bearer | Updates the authenticated user's language preference |
| DELETE | `/delete-account` | Bearer | Permanently deletes the authenticated user's account |
| POST | `/{email}/profile-picture` | Bearer | Uploads a PNG or JPEG profile picture (raw bytes, max 5 MB) |
| GET | `/{email}/profile-picture` | Bearer | Retrieves the profile picture as `image/png` or `image/jpeg`; 404 if none |

## Notifications `/api/v1`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/notifications` | Bearer | Returns the authenticated user's notifications as a paginated feed (`?page=0&size=20`) |
| GET | `/notifications/unread-count` | Bearer | Returns `{ count: N }` unread notifications for the authenticated user |
| POST | `/notifications/{id}/read` | Bearer | Marks a single notification as read (204); idempotent |

> JWKS: `GET /.well-known/jwks.json` is public (no Bearer) and exposes the auth-service signing keys resource servers use to validate access tokens.

## Actuator

| Method | Path | Description |
|---|---|---|
| GET | `/actuator/health` | Overall health status |
| GET | `/actuator/health/liveness` | Liveness probe |
| GET | `/actuator/health/readiness` | Readiness probe; includes DB and Redis checks |

---

# Error Response Contract

Every error from the backend has one of two consistent JSON shapes:

**Domain / server errors** (`ErrorResponse`):
```json
{
  "status": 404,
  "error": "USER_NOT_FOUND",
  "message": "User not found",
  "path": "/api/v1/sign-in",
  "timestamp": "2026-05-23T14:30:00"
}
```

**Validation errors** (`ValidationErrorResponse`):
```json
{
  "status": 400,
  "error": "VALIDATION_FAILED",
  "message": "Request validation failed.",
  "path": "/api/v1/sign-up",
  "timestamp": "2026-05-23T14:30:00",
  "fieldErrors": ["email: must not be blank", "password: size must be between 8 and 255"]
}
```

The frontend `parseApiError.js` utility normalises both shapes (and the Spring Security `{"message":"Unauthorized"}` shape) into a single consistent object for UI error display.

---

# Setup Options

There are two ways to run Seamail:

| Method | Best For |
|--------|----------|
| 🐳 **Docker** | Quick setup, no local dependencies needed |
| 🛠️ **Manual** | Local development with IntelliJ |

## Environment Files Overview

Seamail uses separate environment files depending on the context. Each file lives in the **root `Email-Service` directory** and is never committed to version control.

| File | Used By | When |
|------|---------|------|
| `.env` | IDE / `npm run dev` | Local development without Docker |
| `.env.docker` | Docker Compose | Local Docker |

> The `.env` files inside `frontend-email-service/` and the backend module directories (`api-gateway/`, `auth-service/`, `mail-service/`, `notification-service/`) are only read during IDE/Maven local development. Docker Compose always reads from the root directory env file.

---

# 🐳 Docker Setup

Docker runs the entire stack (MySQL, Redis, Kafka, the four backend services, the observability stack, and the React/Nginx frontend) with a single command. No need to install Java, Node.js, or MySQL locally.

## Prerequisites
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running

## Steps

**1. Clone the repository**
```bash
git clone https://github.com/MohamedMetwalli5/Email-Service.git
cd Email-Service
```

**2. Create the `.env.docker` file**

Create a `.env.docker` file in the root `Email-Service` directory. Use `.env.docker.example` as a template. Then fill in your values.

Discord credentials can be obtained from the [Discord Developer Portal](https://discord.com/developers/applications).  
Redis needs no external account: Docker Compose runs a local Redis container.

> Make sure `http://localhost:8081/api/v1/auth/discord` is added as a redirect URI in your Discord Developer Portal under **OAuth2 → Redirects**. Set the same value as `DISCORD_REDIRECT_URI` in your env file and `VITE_DISCORD_REDIRECT_URI` for the frontend.

**3. Follow the rules in the `nginx.conf` file in the `frontend-email-service` directory**

**4. Build and run**
```bash
docker compose --env-file .env.docker up --build
```

**5. Access the app**

| Service | URL |
|---------|-----|
| Frontend | http://localhost |
| Backend API (api-gateway entry point) | http://localhost:8081 |
| Swagger UI (aggregated API docs) | http://localhost:8081/swagger-ui.html |
| Grafana (admin/admin) | http://localhost:3000 |
| Prometheus | http://localhost:9090 |
| Zipkin | http://localhost:9411 |

**6. Subsequent runs** (after the first build)
```bash
docker compose --env-file .env.docker up
```

## Useful Docker Commands

```bash
# Stop containers (data is preserved)
docker compose --env-file .env.docker down

# Stop containers and delete all data
docker compose --env-file .env.docker down -v

# View logs
docker compose logs -f

# Rebuild a specific service (services: api-gateway, auth-service,
# mail-service, notification-service, frontend)
docker compose --env-file .env.docker up --build mail-service
```

> Note: MySQL data is stored in a Docker volume and persists across restarts. It is only deleted when you run `docker compose down -v`.

---

# 🛠️ Manual Setup (Local Development)

## Prerequisites
- Java 21+
- Maven 3.9+
- Node.js 20
- Docker Desktop (provides the local MySQL, Redis, and Kafka containers, so no external database or cache accounts are needed)

## Database Setup
No manual schema setup is required. Each service manages its own schema through Flyway migrations under `<service>/src/main/resources/db/migration`, applied automatically on startup. Hibernate runs with `ddl-auto=validate`, so a service fails fast at startup if an entity drifts from the migrated schema.

## Backend Setup

The backend is a Maven multi-module reactor (`api-gateway`, `auth-service`, `mail-service`, `notification-service`). All Maven commands run from the root `Email-Service` directory.

**1. Start the infrastructure**

MySQL, Redis, and Kafka are easiest to run as Docker containers:
```bash
docker compose --env-file .env.docker up -d db redis kafka
```

**2. Create `.env`**

Create a `.env` file in the root `Email-Service` directory (see `.env.docker.example` for the variable names). Each backend service reads `DB_NAME`, `DB_USER`, `DB_PASSWORD`, plus Redis and Discord values; the `local` Spring profile retargets the datasource and Redis at `localhost`.

> When running several services side by side, point each run configuration at that module's own `.env` (e.g. `mail-service/.env`) so each service gets its own schema credentials.

**3. Configure IntelliJ run configuration**

Install the [EnvFile plugin](https://plugins.jetbrains.com/plugin/7861-envfile) in IntelliJ, then in your run configuration:
- **EnvFile tab** → enable and point to the root `.env`
- **Active profiles** → set to `local`

<img width="1917" height="892" alt="Screenshot" src="https://github.com/user-attachments/assets/1c3b5319-9f1b-451a-9e52-77bab0d8848c" />


This activates `application-local.properties`, which connects to `localhost` instead of the Docker container hostnames.

**4. Run a service**

Either run its `*Application` class directly from IntelliJ, or from the root:
```bash
mvn -pl mail-service spring-boot:run -Dspring-boot.run.profiles=local
```

Replace `mail-service` with `api-gateway`, `auth-service`, or `notification-service` to run a different module. Note that mail-service needs auth-service running as well (Feign receiver validation and JWKS), so start it first.

The services listen on:
| Service | URL |
|---------|-----|
| api-gateway | http://localhost:8081 |
| auth-service | http://localhost:8082 |
| mail-service | http://localhost:8083 |
| notification-service | http://localhost:8084 |

The gateway on `:8081` is the only entry point the frontend talks to. Alternatively, run the whole stack via Docker (see Docker Setup above).

## Frontend Setup

**1. Navigate to the frontend directory**
```bash
cd frontend-email-service
```

**2. Install dependencies**
```bash
npm install
```

**3. Create `.env`**
Create a `.env` file in the root `frontend-email-service` directory. Use `.env.example` as a template. Then fill in your values.

> Set `VITE_DISCORD_REDIRECT_URI=http://localhost:8081/api/v1/auth/discord` and make sure the same value is registered in the Discord Developer Portal.

**4. Follow the rules in the `nginx.conf` file in the `frontend-email-service` directory**

**5. Start the frontend**
```bash
npm run dev
```

The frontend will start on http://localhost:8080, proxying `/api` to http://localhost:8081 (the api-gateway).

---

# Testing

## Backend

Unit and slice tests use JUnit 5, Spring `@WebMvcTest` and `@DataJpaTest` slices, and Mockito. The `test` profile uses an in-memory H2 database (`ddl-auto=create-drop`) and a simple in-memory cache instead of Redis, and controller slices use a test security configuration with the `jwt()` MockMvc post-processor, matching the OAuth2 resource-server setup without real JWKS or token signing. The suite is Docker-free:

```bash
mvn test
```

Integration tests run via Testcontainers against real MySQL, Redis, and Kafka containers (requires Docker):

```bash
mvn verify
```

| Layer | Tests |
|---|---|
| **Services** | `UserServiceTest`: registration, credential validation, domain enforcement |
| | `EmailServiceTest`: send, sort, filter, trash, delete logic |
| | `NotificationServiceTest`: idempotent event recording, duplicate/race skipping, feed, unread count, mark-as-read ownership |
| **Repositories** | `UserRepositoryTest`: custom query methods |
| | `EmailRepositoryTest`: `moveToTrashBox` bulk UPDATE, inbox/outbox/trash queries |
| | `NotificationRepositoryTest`: unique `event_id` constraint, per-recipient unread counts, paged feed |
| **Controllers** | `AccessControllerTest`: sign-in / sign-up, validation, error shapes, malformed body 400 |
| | `EmailsControllerTest`: authorised and unauthorised email endpoints, paginated responses |
| | `UsersControllerTest`: account management, current-password verification, error paths |
| | `NotificationControllerTest`: paged feed, unread count, mark-as-read, 404 on missing notification |
| **Integration** | `AuthFlowIT`: sign-up → sign-in → refresh rotation → JWKS against real MySQL and Redis |
| | `MailFlowIT`: full mailbox lifecycle (send → inbox + outbox → trash → delete) and exactly one Kafka event on real MySQL/Redis/Kafka |
| | `NotificationFlowIT`: consumes `email.sent` idempotently (duplicate `event_id` stays one row) and exposes the REST feed on real Kafka/MySQL |

## Frontend

Tests use Vitest, React Testing Library, MSW (Mock Service Worker), and axios-mock-adapter. MSW intercepts all HTTP calls at the network level; no real requests are made.

```bash
cd frontend-email-service
npm test
```

| Test file | What it covers |
|---|---|
| `parseApiError.test.js` | All 4 error shapes: `ErrorResponse`, `ValidationErrorResponse`, Spring Security 401, network error |
| `apiClient.test.js` | Bearer header injection, 401 → refresh → retry flow, refresh-fails-mid-queue rejection, redirect on refresh failure |
| `AppContext.test.jsx` | `refreshToken` storage, `clearSession`, `sharedEmailToFullyView` serialisation, `app:logout` event, `storage` event |
| `SignInPage.test.jsx` | Plain password sent, `accessToken` + `refreshToken` stored, error display |
| `SignUpPage.test.jsx` | Domain validation, conflict error, `fieldErrors` display |
| `SigninWithDiscord.test.jsx` | Fetches CSRF state from backend, redirect URL with correct `VITE_DISCORD_REDIRECT_URI` |
| `HomePage.test.jsx` | Discord callback exchanges `?code=` ticket for tokens, stores them, strips URL |
| `EmailsSnippetView.test.jsx` | `GET /emails?sort=` and `GET /emails?filterBy=`, `emailID` list keys |
| `NewMessageComposer.test.jsx` | Send form, `fieldErrors` on validation failure |
| `SettingsMainContent.test.jsx` | Plain password on change, current password sent, JPEG upload accepted, ISO language codes |
| `Sidebar.test.jsx` | `clearSession` clears context and localStorage on sign-out |
| `Navbar.test.jsx` | Profile picture blob uses response `Content-Type` |
| `EmailFullView.test.jsx` | `emailID` field used throughout, move-to-trash and delete calls |
| `ProtectedRoute.test.jsx` | Redirects to `/sign-in` when no token; renders content when authenticated |

---

# Project Structure

```
Email-Service/
├── pom.xml                          # Aggregator parent (com.seamail:seamail-parent)
├── Dockerfile.backend               # Parameterized per-module build (ARG MODULE)
├── docker-compose.yml               # db, redis, kafka, 4 app services, zipkin, prometheus, grafana, frontend
├── db/init/                         # 01-schemas.sh: per-service MySQL schemas + scoped users
├── observability/
│   ├── prometheus.yml               # Scrape config for all services
│   └── grafana/                     # Datasource + dashboard provisioning
├── api-gateway/                     # Spring Cloud Gateway (:8081)
│   └── src/main/java/.../gateway/   # Routes, global CORS, Swagger aggregation (application.yml)
├── auth-service/                    # Identity, RS256 + JWKS, refresh rotation, Discord OAuth (:8082)
│   └── src/main/java/.../auth/      # config, controller, dto, entity, exception,
│                                    #   health, repository, service
├── mail-service/                    # Mail domain, resource server, Feign, Kafka producer (:8083)
│   └── src/main/java/.../mail/      # client (Feign), config, controller, dto, entity, event,
│                                    #   exception, health, messaging (producer), repository, service
├── notification-service/            # Kafka consumer, idempotent feed (:8084)
│   └── src/main/java/.../notification/  # config, controller, dto, entity, event,
│                                    #   exception, messaging (consumer + DLT), repository, service
├── frontend-email-service/          # React SPA
│   ├── package.json                 # Scripts: dev, build, test (vitest run)
│   ├── vite.config.js               # Port 8080, /api proxy, vitest config
│   ├── Dockerfile                   # Node 20 build + nginx serve
│   ├── nginx.conf                   # SPA fallback + /api/v1/ proxy to api-gateway
│   ├── .env.example                 # VITE_* variables
│   └── src/
│       ├── main.jsx                 # Router: /, /sign-in, /home, /settings
│       ├── AppContext.jsx           # Auth + mailbox global state, localStorage
│       ├── api/apiClient.js         # Axios instance, Bearer header, refresh interceptor
│       ├── utils/parseApiError.js   # Normalises all backend error shapes
│       ├── pages/                   # SignUp, SignIn, Home, Settings screens
│       ├── components/              # Layout, composer, email views, Discord button, ProtectedRoute
│       ├── i18n.js                  # English / German / French strings (ISO codes: en/fr/de)
│       └── tests/                   # Vitest + MSW test suite
├── SQL Scripts/
│   └── Tables.sql                   # Retired - schema now managed by Flyway per service
└── .env.docker.example              # Compose env template
```

---

# Screenshot
![image](https://github.com/user-attachments/assets/6ac251dd-e2e2-49ee-9d3f-c1fbe756d6e0)

# Logo Idea
The Seamail logo combines an envelope with dynamic wave patterns, symbolizing seamless communication. It reflects the efficient, modern, and user-friendly email service.

![Seamail - Website Logo](https://github.com/user-attachments/assets/c791622c-62a6-4ac0-95da-13996c60020f)

## Author
**Mohamed Metwalli** - Software Engineer & Technical Writer  
🌐 [mohamedmetwalli.com](https://mohamedmetwalli.com) · [LinkedIn](https://www.linkedin.com/in/mohamed-metwalli5)
