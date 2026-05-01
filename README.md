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
Seamail is a full-stack email service designed to enhance user interactions with their email system. It provides secure, efficient, and user-friendly functionalities for managing emails through an intuitive interface, backed by Redis caching for improved performance.
It is officially deployed on **Amazon Web Services (AWS)** using a custom domain.

# Features
- **User Registration & Sign-in:** Secure registration and login process for users.
- **HTTPS Encryption & Deployment:** Deployed on AWS with a valid SSL certificate issued by Let's Encrypt, ensuring all data is securely encrypted and protected from interception.
- **OAuth2 Authentication:** Allows users to optionally sign in with their Discord account.
- **JWT Authentication:** Ensures secure access to the platform with token-based authentication.
- **Multi-language Support:** Enhances accessibility by making the platform available in English, German, and French via i18next.
- **Email Management:** Allows users to view and manage inbox, outbox, and trashbox for efficient email organization.
- **Email Actions:** Send, move to trash, and delete emails directly from any mailbox.
- **Email Sorting & Filtering:** Sort emails by priority or date, and filter them by subject or sender.
- **Password Management:** Allows users to securely change their password to maintain account security.
- **Account Management:** Allows users to permanently delete their accounts and change their default profile picture.
- **Redis Caching:** Caches inbox emails per user using Redis Cloud with a 15-minute TTL to reduce database load and improve response times. Cache is automatically invalidated when new emails are received or moved to trash.

---

# Architecture Overview

```
┌─────────────────────────────────────────┐
│              React (Vite)               │
│   TailwindCSS · React Router · i18next  │
└─────────────────┬───────────────────────┘
                  │ HTTPS / REST (JSON)
┌─────────────────▼───────────────────────┐
│          Spring Boot  /api/v1           │
│  Controller → Service → Repository      │
│  JWT Filter · GlobalExceptionHandler    │
└────────┬───────────────────┬────────────┘
         │                   │
┌────────▼──────┐   ┌────────▼──────────┐
│     MySQL     │   │   Redis Cloud     │
│  Spring Data  │   │  Inbox cache      │
│     JPA       │   │  TTL 15 min       │
└───────────────┘   └───────────────────┘
```

**Key design decisions:**
- **Versioned REST API:** all endpoints live under `/api/v1`, making future versioning straightforward.
- **DTO layer:** request/response objects are fully decoupled from JPA entities; no entity is ever serialised directly over the wire.
- **Centralised exception handling:** a single `@RestControllerAdvice` maps every custom domain exception to a consistent JSON error shape and correct HTTP status.
- **Focused caching:** only the inbox (the highest-traffic read) is cached in Redis with a 15-minute TTL; cache is evicted automatically on send or trash actions.
- **Stateless security:** a custom `JwtFilter` (extending `OncePerRequestFilter`) validates Bearer tokens before every protected request; no session state is held server-side.
- **SSL Termination:** Nginx handles HTTPS requests using **Let's Encrypt** certificates, ensuring all traffic between the client and the server is encrypted.
---

# Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React 18, Vite, TailwindCSS, React Router v7, Axios |
| Internationalisation | i18next / react-i18next (EN, FR, DE) |
| Backend | Spring Boot 3, Java 21, Maven |
| Security | Spring Security, JWT (JJWT HS256), Discord OAuth2 |
| Database | MySQL |
| Caching | Redis Cloud via Spring Cache (`@Cacheable` / `@CacheEvict`) |
| Infrastructure | Docker, Nginx |
| Testing | JUnit 5, Spring MockMvc, Mockito, AssertJ |

---

# API Reference

All protected endpoints require `Authorization: Bearer <token>`.

## Auth `/api/v1`

| Method | Path | Description |
|---|---|---|
| POST | `/sign-in` | Authenticates a user with email and password, returns a signed JWT |
| POST | `/sign-up` | Registers a new user account and returns a signed JWT |
| GET | `/DiscordSignin` | Initiates the Discord OAuth2 flow and returns a JWT on success |

## Emails `/api/v1`

| Method | Path | Description |
|---|---|---|
| GET | `/inbox` | Returns all emails received by the authenticated user |
| GET | `/outbox` | Returns all emails sent by the authenticated user |
| GET | `/trashbox` | Returns all emails moved to trash by the authenticated user |
| POST | `/send-email` | Sends a new email to a specified recipient |
| POST | `/move-to-trash` | Moves a specified email to the trash |
| DELETE | `/delete-email` | Permanently deletes a specified email |
| POST | `/sort-emails` | Returns the user's emails sorted by date or priority |
| POST | `/filter-emails` | Returns the user's emails filtered by subject or sender |

## Users `/api/v1`

| Method | Path | Description |
|---|---|---|
| PUT | `/change-password` | Updates the authenticated user's password |
| PUT | `/update-language` | Updates the authenticated user's language preference |
| DELETE | `/delete-account` | Permanently deletes the authenticated user's account |
| POST | `/{email}/profile-picture` | Uploads a profile picture for the specified user |
| GET | `/{email}/profile-picture` | Retrieves the profile picture for the specified user |

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

> Also make sure `http://localhost:8081/DiscordSignin` is added as a redirect URI in your Discord Developer Portal under **OAuth2 → Redirects**.

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
- Java JDK 21
- Maven
- MySQL
- Node.js & npm
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
mvn clean install
cd target
java -jar backendemailservice-0.0.1-SNAPSHOT.jar
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

**4. Follow the rules in the `nginx.conf` file in the `frontend-email-service` directory**

**5. Start the frontend**
```bash
npm run dev
```

The frontend will start on http://localhost:8080

---

# Testing

Tests use JUnit 5, Spring MockMvc, Mockito, and AssertJ, covering the service, controller, and caching layers end-to-end.

```bash
cd backendemailservice
mvn test
```

## Test Coverage

| Layer | Tests |
|---|---|
| **Services** | `EmailServiceTest`: CRUD, sorting, filtering, trash logic |
| | `EmailServiceCacheTest`: cache population and eviction |
| | `UserServiceTest`: registration, credential validation |
| **Controllers** | `AccessControllerTest`: sign-in / sign-up edge cases |
| | `EmailsControllerTest`: authorised and unauthorised email endpoints |
| | `UsersControllerTest`: account management and error paths |

---

# Screenshot
![image](https://github.com/user-attachments/assets/6ac251dd-e2e2-49ee-9d3f-c1fbe756d6e0)


# Logo Idea
The Seamail logo combines an envelope with dynamic wave patterns, symbolizing seamless communication. It reflects the efficient, modern, and user-friendly email service.

![Seamail - Website Logo](https://github.com/user-attachments/assets/c791622c-62a6-4ac0-95da-13996c60020f)

## Author
**Mohamed Metwalli** - Software Engineer & Technical Writer  
🌐 [mohamedmetwalli.com](https://www.mohamedmetwalli.com) · [LinkedIn](https://www.linkedin.com/in/mohamed-metwalli5)
