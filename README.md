![Frontend](https://img.shields.io/badge/Frontend-React.js-blue.svg)
![Backend](https://img.shields.io/badge/Backend-Spring%20Boot-green.svg)
![Database](https://img.shields.io/badge/Database-MySQL-white.svg)
![Cache](https://img.shields.io/badge/Cache-Redis-red.svg)
![Container](https://img.shields.io/badge/Container-Docker-blue.svg)
![Deployment](https://img.shields.io/badge/Deployment-AWS-orange.svg)
![License](https://img.shields.io/badge/License-GPL--3.0-yellow.svg)

<div align="center">
  <img src="https://github.com/user-attachments/assets/3438953d-9596-41fd-9570-2c0ec3713657" alt="The Website Logo" width="200" />
</div>

# Seamail: An Email Service
Seamail is a full-stack email service designed around the `@seamail.com` domain. It provides secure, efficient, and user-friendly functionalities for managing emails through an intuitive interface, backed by JWT-based authentication with automatic token refresh, Redis for token storage and inbox caching, and a fully versioned REST API.
It is officially deployed on **Amazon Web Services (AWS)** using a custom domain.

# Features
- **User Registration & Sign-in:** Secure registration and login with server-side BCrypt password hashing.
- **HTTPS Encryption & Deployment:** Deployed on AWS with a valid SSL certificate issued by Let's Encrypt, ensuring all data is securely encrypted and protected from interception.
- **OAuth2 Authentication:** Allows users to optionally sign in with their Discord account via a custom server-side OAuth2 callback.
- **JWT Authentication:** Stateless Bearer token authentication with 30-minute access tokens and automatic silent refresh via rotating refresh tokens.
- **Automatic Token Refresh:** A centralised axios interceptor detects expired tokens, silently exchanges the refresh token for a new pair, and retries the original request without interrupting the user.
- **Multi-language Support:** Enhances accessibility by making the platform available in English, German, and French via i18next.
- **Email Management:** Allows users to view and manage inbox, outbox, and trashbox for efficient email organisation.
- **Email Actions:** Send, move to trash, and permanently delete emails directly from any mailbox.
- **Email Sorting & Filtering:** Sort emails by priority or date, and filter them by subject or sender, all via a single unified query endpoint.
- **Password Management:** Allows users to securely change their password to maintain account security.
- **Account Management:** Allows users to permanently delete their accounts and change their default profile picture (PNG or JPEG, max 5 MB).
- **Redis Caching:** Caches inbox emails per user using Redis Cloud with a 15-minute TTL to reduce database load and improve response times. Cache is automatically invalidated when emails are received or moved to trash. Redis also stores refresh tokens with a 7-day TTL and automatic rotation on every use.

---

# Architecture Overview

```
┌─────────────────────────────────────────┐
│              React (Vite)               │
│   TailwindCSS · React Router · i18next  │
│   apiClient.js · parseApiError.js       │
└─────────────────┬───────────────────────┘
                  │ HTTPS / REST (JSON)
┌─────────────────▼───────────────────────┐
│          Spring Boot  /api/v1           │
│  Controller → Service → Repository      │
│  JwtFilter · GlobalExceptionHandler     │
│  Spring Boot Actuator (health probes)   │
└────────┬───────────────────┬────────────┘
         │                   │
┌────────▼──────┐   ┌────────▼───────────────────┐
│     MySQL     │   │        Redis Cloud         │
│  Spring Data  │   │  Inbox cache  TTL 15 min   │
│     JPA       │   │  Refresh tokens TTL 7 days │
└───────────────┘   └────────────────────────────┘
```

**Key design decisions:**
- **Versioned REST API:** all endpoints live under `/api/v1`, making future versioning straightforward.
- **DTO layer:** request/response objects are fully decoupled from JPA entities; no entity is ever serialised directly over the wire.
- **Centralised exception handling:** a single `@RestControllerAdvice` maps every custom domain exception to a consistent JSON error shape (`ErrorResponse` / `ValidationErrorResponse`) with HTTP status, machine-readable error code, message, path, and timestamp.
- **Centralised HTTP client:** a single `apiClient.js` axios instance handles Bearer header injection, 401 detection, silent token refresh with request queuing, and redirect to sign-in on refresh failure.
- **Refresh token rotation:** every call to `POST /api/v1/auth/refresh` deletes the old Redis refresh token key and issues a new access + refresh pair, limiting the window of token reuse.
- **Focused caching:** only the inbox (the highest-traffic read) is cached in Redis with a 15-minute TTL; cache is evicted automatically on send or trash actions.
- **Stateless security:** a custom `JwtFilter` (extending `OncePerRequestFilter`) validates Bearer tokens and populates the `SecurityContext` with the user's email and authorities before every protected request; no session state is held server-side.
- **Service abstractions:** `UserService` and `EmailService` implement `IUserService` and `IEmailService` interfaces, keeping controllers thin and the service layer fully testable in isolation.
- **Health monitoring:** Spring Boot Actuator exposes `/actuator/health` with liveness and readiness probe groups (including DB and Redis checks) for AWS load-balancer integration.
- **SSL Termination:** Nginx handles HTTPS requests using **Let's Encrypt** certificates, ensuring all traffic between the client and the server is encrypted.

---

# Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React 18, Vite, TailwindCSS, React Router v7, Axios |
| Internationalisation | i18next / react-i18next (EN, DE, FR) |
| Backend | Spring Boot 3.2.3, Java 17, Maven |
| Security | Spring Security, JWT HS256 (JJWT 0.11.5), BCrypt, Discord OAuth2 |
| Database | MySQL 8.0 |
| Caching & Token Store | Redis Cloud via Spring Cache + Spring Data Redis (`@Cacheable` / `@CacheEvict` / `StringRedisTemplate`) |
| Monitoring | Spring Boot Actuator (health, liveness, readiness) |
| Infrastructure | Docker, Nginx |
| Backend Testing | JUnit 5, `@WebMvcTest`, `@DataJpaTest`, Mockito, AssertJ, H2 |
| Frontend Testing | Vitest, React Testing Library, MSW (Mock Service Worker), axios-mock-adapter |

---

# API Reference

All protected endpoints require `Authorization: Bearer <accessToken>`.  
The token is a 30-minute HS256 JWT. Use `POST /api/v1/auth/refresh` to renew it silently.

## Auth `/api/v1`

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/sign-in` | Public | Authenticates a user; returns `{ accessToken, refreshToken }` |
| POST | `/sign-up` | Public | Registers a new `@seamail.com` account; returns `{ accessToken, refreshToken }` (201) |
| POST | `/auth/refresh` | Public | Exchanges a refresh token for a new access + refresh pair (rotation) |
| GET | `/auth/discord` | Public | Discord OAuth2 callback; 302 redirects to frontend `/home` with tokens in query params |

## Emails `/api/v1`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/inbox` | Bearer | Returns all active inbox emails for the authenticated user |
| GET | `/outbox` | Bearer | Returns all sent emails for the authenticated user |
| GET | `/trashbox` | Bearer | Returns all trashed emails for the authenticated user |
| GET | `/emails` | Bearer | Query endpoint; supports `?sort=priority\|date`, `?filterBy=subject\|sender&filterValue=` |
| POST | `/send-email` | Bearer | Sends a new email to a specified recipient (201 empty body) |
| POST | `/move-to-trash` | Bearer | Moves a specified email to trash (204) |
| DELETE | `/delete-email` | Bearer | Permanently deletes a specified email (204) |

## Users `/api/v1`

| Method | Path | Auth | Description |
|---|---|---|---|
| PUT | `/change-password` | Bearer | Updates the authenticated user's password |
| PUT | `/update-language` | Bearer | Updates the authenticated user's language preference |
| DELETE | `/delete-account` | Bearer | Permanently deletes the authenticated user's account |
| POST | `/{email}/profile-picture` | Bearer | Uploads a PNG or JPEG profile picture (raw bytes, max 5 MB) |
| GET | `/{email}/profile-picture` | Bearer | Retrieves the profile picture as `image/jpeg` |

## Actuator

| Method | Path | Description |
|---|---|---|
| GET | `/actuator/health` | Overall health status |
| GET | `/actuator/health/liveness` | Liveness probe (AWS ALB) |
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
| `.env.production` | Docker Compose | AWS production |

> The `.env` files inside `frontend-email-service/` and `backendemailservice/` are only read during IDE development. Docker Compose always reads from the root directory env file.

---

# 🐳 Docker Setup

Docker runs the entire stack (MySQL + Spring Boot + React/Nginx) with a single command. No need to install Java, Node.js, or MySQL locally.

## Prerequisites
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running

## Steps

**1. Clone the repository**
```bash
git clone https://github.com/your-username/Email-Service.git
cd Email-Service
```

**2. Create the `.env.docker` file**

Create a `.env.docker` file in the root `Email-Service` directory. Use `.env.docker.example` as a template. Then fill in your values.

Discord credentials can be obtained from the [Discord Developer Portal](https://discord.com/developers/applications).  
Redis credentials can be obtained from [Redis Cloud](https://redis.io/cloud/) (free tier available).

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
| Backend API | http://localhost:8081 |

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

# Rebuild a specific service
docker compose --env-file .env.docker up --build backend
```

> Note: MySQL data is stored in a Docker volume and persists across restarts. It is only deleted when you run `docker compose down -v`.

---

# 🛠️ Manual Setup (Local Development)

## Prerequisites
- Java 17+
- Maven 3.9+
- MySQL 8
- Node.js 20
- Redis Cloud account

## Database Setup
Run the `Tables.sql` script in the "SQL Scripts" folder to set up your database tables.

## Redis Cache Setup
1. Create a free account on [Redis Cloud](https://redis.io/cloud/)
2. Create a new database and note down your connection details (host, port, username, and password)

## Backend Setup

**1. Navigate to the backend directory**
```bash
cd backendemailservice
```

**2. Create `.env`**

Create a `.env` file in the root `backendemailservice` directory. Use `.env.example` as a template. Then fill in your values.

> Note: `DB_USER=root` here (local MySQL), vs `DB_USER=seamail_user` in Docker.

**3. Configure IntelliJ run configuration**

Install the [EnvFile plugin](https://plugins.jetbrains.com/plugin/7861-envfile) in IntelliJ, then in your run configuration:
- **EnvFile tab** → enable and point to `backendemailservice/.env`
- **Active profiles** → set to `local`

![image](https://github.com/user-attachments/assets/acb22cd2-5303-4379-9e37-eeccaa230264)

This activates `application-local.properties` which connects to your local MySQL instead of the Docker `db` host.

**4. Run the backend**

Either run directly from IntelliJ, or:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

The backend will start on http://localhost:8081

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

The frontend will start on http://localhost:8080

---

# Testing

## Backend

Tests use JUnit 5, Spring `@WebMvcTest`, `@DataJpaTest`, Mockito, and AssertJ. The `test` profile uses an in-memory H2 database (`ddl-auto=create-drop`) and simple in-memory cache instead of Redis, so no external services are needed to run the suite.

```bash
cd backendemailservice
mvn test
```

| Layer | Tests |
|---|---|
| **Services** | `UserServiceTest`: registration, credential validation, domain enforcement |
| | `EmailServiceTest`: send, sort, filter, trash, delete logic |
| **Repositories** | `UserRepositoryTest`: custom query methods |
| | `EmailRepositoryTest`: `moveToTrashBox` bulk UPDATE, inbox/outbox/trash queries |
| **Controllers** | `AccessControllerTest`: sign-in / sign-up, validation, error shapes |
| | `EmailsControllerTest`: authorised and unauthorised email endpoints |
| | `UsersControllerTest`: account management and error paths |
| **Integration** | `FullFlowIntegrationTest`: end-to-end sign-up → send → inbox → trash → delete |

## Frontend

Tests use Vitest, React Testing Library, MSW (Mock Service Worker), and axios-mock-adapter. MSW intercepts all HTTP calls at the network level; no real requests are made.

```bash
cd frontend-email-service
npm test
```

| Test file | What it covers |
|---|---|
| `parseApiError.test.js` | All 4 error shapes: `ErrorResponse`, `ValidationErrorResponse`, Spring Security 401, network error |
| `apiClient.test.js` | Bearer header injection, 401 → refresh → retry flow, redirect on refresh failure |
| `AppContext.test.jsx` | `refreshToken` storage, `clearSession`, `sharedEmailToFullyView` serialisation |
| `SignInPage.test.jsx` | Plain password sent, `accessToken` + `refreshToken` stored, error display |
| `SignUpPage.test.jsx` | Domain validation, conflict error, `fieldErrors` display |
| `SigninWithDiscord.test.jsx` | Redirect URL constructed with correct `VITE_DISCORD_REDIRECT_URI` |
| `HomePage.test.jsx` | Discord callback reads and stores `refreshToken`, strips query params |
| `EmailsSnippetView.test.jsx` | `GET /emails?sort=` and `GET /emails?filterBy=`, `emailID` list keys |
| `NewMessageComposer.test.jsx` | Send form, `fieldErrors` on validation failure |
| `SettingsMainContent.test.jsx` | Plain password on change, JPEG upload accepted |
| `Sidebar.test.jsx` | `clearSession` clears context and localStorage on sign-out |
| `Navbar.test.jsx` | Profile picture blob uses response `Content-Type` |
| `EmailFullView.test.jsx` | `emailID` field used throughout, move-to-trash and delete calls |

---

# Project Structure

```
Email-Service/
├── backendemailservice/               # Spring Boot API
│   ├── pom.xml                        # Maven deps, Java 17, Boot 3.2.3
│   ├── Dockerfile                     # Multi-stage build, exposes 8081
│   ├── .env.example                   # Backend env template
│   └── src/main/java/.../
│       ├── BackendemailserviceApplication.java   # Entry point
│       ├── config/                    # SecurityConfig, CorsConfig, RedisConfig,
│       │                              #   DiscordOAuthProperties, CachingConfig
│       ├── controller/                # AccessController, EmailsController,
│       │                              #   UsersController, OAuth2Controller
│       ├── dto/                       # Request / response records and beans
│       ├── entity/                    # User, Email (JPA entities)
│       ├── exception/                 # ApplicationException, ErrorResponse,
│       │                              #   ValidationErrorResponse, GlobalExceptionHandler
│       ├── filter/                    # JwtFilter (OncePerRequestFilter)
│       ├── health/                    # CustomRedisHealthIndicator
│       ├── repository/                # UserRepository, EmailRepository
│       ├── service/                   # IUserService, IEmailService (interfaces)
│       │                              #   UserService, EmailService (implementations)
│       │                              #   CustomUserDetailsService
│       └── util/                      # JwtUtil
├── frontend-email-service/            # React SPA
│   ├── package.json                   # Scripts: dev, build, test (vitest run)
│   ├── vite.config.js                 # Port 8080, /api proxy, vitest config
│   ├── Dockerfile                     # Node 20 build + nginx serve
│   ├── nginx.conf                     # SPA fallback + /api/v1/ proxy to backend
│   ├── .env.example                   # VITE_* variables
│   └── src/
│       ├── main.jsx                   # Router: /, /sign-in, /home, /settings
│       ├── context/AppContext.jsx     # Auth + mailbox global state, localStorage
│       ├── api/apiClient.js           # Axios instance, Bearer header, refresh interceptor
│       ├── utils/parseApiError.js     # Normalises all backend error shapes
│       ├── pages/                     # SignUp, SignIn, Home, Settings screens
│       ├── components/                # Layout, composer, email views, Discord button
│       ├── i18n.js                    # English / German / French strings
│       └── tests/                     # Vitest + MSW test suite
├── SQL Scripts/
│   └── Tables.sql                     # MySQL schema init for Docker and manual setup
├── docker-compose.yml                 # db + backend + frontend services
└── .env.docker.example                # Compose env template
```

---

# Screenshot
![image](https://github.com/user-attachments/assets/6ac251dd-e2e2-49ee-9d3f-c1fbe756d6e0)

# Logo Idea
The Seamail logo combines an envelope with dynamic wave patterns, symbolizing seamless communication. It reflects the efficient, modern, and user-friendly email service.

![Seamail - Website Logo](https://github.com/user-attachments/assets/c791622c-62a6-4ac0-95da-13996c60020f)

## Author
**Mohamed Metwalli** - Software Engineer & Technical Writer  
🌐 [mohamedmetwalli.com](https://www.mohamedmetwalli.com) · [LinkedIn](https://www.linkedin.com/in/mohamed-metwalli5)
