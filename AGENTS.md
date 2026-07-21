# AGENTS.md

Multi-package full-stack app: a Maven multi-module Spring Boot backend (`api-gateway/`, `auth-service/`, `mail-service/`, `notification-service/` under root aggregator `pom.xml`) and a React 18 + Vite frontend (`frontend-email-service/`). Root also holds `docker-compose.yml`, `Dockerfile.backend` (parameterized per-module build), `db/init/`, and `observability/`.

## Commands

### Backend (root)
- `mvn test` - run the full reactor test suite (all four modules). Requires Docker running for the Testcontainers ITs (`*IT.java`).
- `mvn -pl mail-service test` - run one module. `mvn -pl mail-service -Dtest=ClassName test` for a single class.
- `mvn -pl mail-service spring-boot:run -Dspring-boot.run.profiles=local` - run one service locally (needs its dependencies up: `docker compose --env-file .env.docker up -d db redis kafka auth-service`).

### Frontend (`frontend-email-service/`)
- `npm install` then `npm run dev` - Vite dev server on `:8080`, proxies `/api` to `http://localhost:8081`.
- `npm test` - runs Vitest once (`vitest run`), not watch. Uses jsdom, MSW, and axios-mock-adapter; no real network calls.
- `npm run lint` - ESLint 9 flat config (`eslint .`).
- `npm run build` - production build to `dist/`.

### Full stack (root)
- `docker compose --env-file .env.docker up --build` - MySQL `:3307`, backend `:8081`, frontend `:80`. First run builds; subsequent runs drop `--build`.
- `docker compose --env-file .env.docker down -v` wipes the MySQL volume.

## Spring profiles (backend)

`application.properties` is Docker-oriented (MySQL host `db`, `ddl-auto=validate`). Selecting a profile only overrides what differs:
- `local` - `application-local.properties` points at `localhost:3306` and CORS origin `:8080`. Use with `-Dspring-boot.run.profiles=local` or IntelliJ Active profiles `local`.
- `test` - `application-test.properties` switches to H2 (`ddl-auto=create-drop`), dummy Discord + Redis values, and `spring.cache.type=simple` to bypass Redis. **Do not rely on Redis/Mysql being available for tests.**
- (default) - Docker compose profile; expects all `DB_*`/`REDIS_*`/`DISCORD_*`/`JWT_*` (`JWT_PRIVATE_KEY_PATH`/`JWT_PUBLIC_KEY_PATH`) placeholders to be resolved from env.

Each service has its own `application.properties` plus sibling `application-local.properties`, `application-test.properties`, and `application-it.properties`. The `it` profile is used by the Testcontainers integration tests (`AuthFlowIT`/`MailFlowIT`/`NotificationFlowIT`) and leaves the datasource, Redis, and Kafka connection details to `@ServiceConnection`, so no hard-coded host/port is needed in `application-it.properties`.

## Env files and loading (gotcha)

- Repo-root env files (`.env`, `.env.docker`, `.env.production` per README) are the source of truth. The `.env` files inside each subdirectory are only read during IDE/Maven/npm local dev.
- Spring does **not** read `.env` itself. Local backend runs require the IntelliJ [EnvFile plugin](https://plugins.jetbrains.com/plugin/7861-envfile) pointed at the root `.env`, or env vars exported in the shell. Vite reads `frontend-email-service/.env` natively.
- `.env` files are gitignored. Never edit them; ask the user. Templates are `.env.example` / `.env.docker.example`.
- `DB_USER=root` in local `.env` vs `DB_USER=seamail_user` in `.env.docker` - keep them distinct.
- Vite `VITE_*` vars are baked in at build time. The frontend Dockerfile forwards them as build args; changing them requires a frontend rebuild.
- Per-service DB vars (`AUTH_DB_*`, `MAIL_DB_*`, `NOTIFICATION_DB_*`) point each service at its own MySQL schema.
- `JWT_PRIVATE_KEY_PATH` / `JWT_PUBLIC_KEY_PATH` empty means auth-service generates an ephemeral dev keypair at startup (not safe for shared environments).
- `KAFKA_BOOTSTRAP_SERVERS` (e.g. `kafka:9092`), `JWKS_URI` (e.g. `http://auth-service:8082/.well-known/jwks.json`), and per-service URLs (`AUTH_SERVICE_URL`, `MAIL_SERVICE_URL`, `NOTIFICATION_SERVICE_URL`) wire inter-service calls in Docker.

## Schema and JPA

- Each service's schema source of truth is its own Flyway migrations under `src/main/resources/db/migration`. `SQL Scripts/Tables.sql` is retired (kept in git history); do not edit it.
- `ddl-auto=validate` in prod/local means Hibernate will fail startup if entities drift from the Flyway schema - keep `entity/` and migrations in sync. Only the `test` profile uses `create-drop`, and H2 slices disable Flyway entirely.
- MySQL exposed on host port `:3307` (container `:3306`) under Docker.

## Backend architecture notes

- Each service module has its own `@SpringBootApplication` entry point (`*Application.java`). Layering is strictly Controller -> Service (`IUserService`/`IEmailService` interfaces) -> Repository.
- **Entities are never serialized over the wire.** Requests/responses use DTOs in `dto/` (records and beans). Add new fields there, not on entities.
- Auth is stateless JWT. auth-service signs **RS256** tokens (Nimbus JWT/JWK) and publishes its public keys at `/.well-known/jwks.json`; mail-service and notification-service are resource servers that validate access tokens statelessly through Spring Security OAuth2 resource server + JWKS, exposing the caller email via `@AuthenticationPrincipal(expression = "subject")`. `filter/JwtFilter` extends `OncePerRequestFilter` and populates `SecurityContext` from the Bearer token before each protected request - no HTTP sessions. `JwtFilter` catches all parse exceptions and continues the filter chain (so an expired/malformed token yields a clean 401, not a 500). All `/api/v1/**` endpoints except `sign-in`, `sign-up`, `auth/refresh`, `auth/discord`, `auth/discord/state`, `auth/exchange` require `Authorization: Bearer <accessToken>`.
- Refresh tokens are stored in Redis (7-day TTL) and **rotated on every refresh**; rotation uses `redisTemplate.delete(key)` as an atomic claim (the boolean return ensures only one concurrent refresh wins; the loser is rejected with 401). Refresh tokens are also revoked on account deletion and password change. Access tokens last 30 minutes.
- Discord OAuth uses a ticket-based flow: `GET /auth/discord/state` generates a CSRF nonce (5-min TTL in Redis), the frontend fetches it before redirecting to Discord. On callback, `GET /auth/discord` validates the state, exchanges the code with Discord, stores an opaque 60-second ticket in Redis, and 302-redirects to `/home?code=<ticket>`. The SPA then `POST /auth/exchange`s the ticket for `{accessToken, refreshToken, email}`. JWTs never appear in URLs.
- Errors follow a fixed contract: `ErrorResponse` for domain errors, `ValidationErrorResponse` for bean-validation failures - both produced by `GlobalExceptionHandler`. The handler also maps `HttpMessageNotReadableException` (malformed body), `MethodArgumentTypeMismatchException`, `MissingServletRequestParameterException`, and `DataIntegrityViolationException` to appropriate 400/409 responses. Do not return ad-hoc error JSON.
- Inbox caching: `GET /inbox` is cached in Redis keyed by `userEmail + page + size`; evicted on send/trash/delete via `@CacheEvict(allEntries = true)`. Don't add `@Cacheable` to other reads without reason.
- Email queries are paginated: all inbox/outbox/trashbox/emails endpoints return `Page<EmailResponseDto>` (`{content, totalElements, totalPages, ...}`) and accept `?page=0&size=20` params. Default page size is 20.
- `deleteEmail` requires the email to already be trashed (`trash=true`); permanent delete is a separate step from soft-delete (move-to-trash).
- `Mailbox` enum in `entity/` replaces magic strings for inbox/outbox/trashbox switching in `queryEmails`.
- mail-service validates the receiver through `AuthUserClient` (Spring Cloud OpenFeign, calls auth-service `InternalUsersController.existsByEmail`) before persisting an email, and publishes an `EmailSentEvent` via `@TransactionalEventListener(AFTER_COMMIT)` so consumers never see rolled-back writes.
- notification-service consumes `email.sent` idempotently: each event carries a unique `event_id`, the consumer check-then-inserts against a unique constraint to win races, and poison pills are routed to `email.sent.DLT` after bounded exponential backoff.
- The api-gateway (Spring Cloud Gateway, `:8081`) is the only public entry point: it routes `/api/v1/**` to the downstream services, terminates CORS, and aggregates Swagger UI. `/internal/**` is routed nowhere (not exposed through the gateway). Downstream services listen on `:8082` (auth), `:8083` (mail), `:8084` (notification).
- CORS is enforced only at the gateway; downstream services trust the gateway origin.
- Actuator health: `/actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness` (readiness includes DB + Redis checks via `health/CustomRedisHealthIndicator`).

## Observability

- Prometheus scrapes `/actuator/prometheus` on each backend service at `:9090` (Prometheus itself).
- Grafana at `:3000` (admin/admin in dev) ships a provisioned dashboard under `observability/grafana/`.
- Zipkin at `:9411` collects traces; a send-email request produces a single trace crossing gateway -> mail-service -> Kafka -> notification-service, with trace/span IDs in every log line.
- Trace sampling is `1.0` in dev (`management.tracing.sampling.probability`).

## Frontend architecture notes

- `src/main.jsx` defines routes: `/`, `/sign-in`, `/home`, `/settings`. `/home` and `/settings` are wrapped in `ProtectedRoute` which redirects to `/sign-in` when no `authToken` is in context.
- `src/api/apiClient.js` is the single axios instance: injects Bearer header, detects 401, silently refreshes the token (queues concurrent requests), redirects to sign-in on refresh failure. On refresh failure it dispatches an `app:logout` custom event so `AppContext` clears its in-memory state (not just localStorage). Do not create other axios instances.
- `src/utils/parseApiError.js` normalizes all backend error shapes (including Spring Security `{"message":"Unauthorized"}` and network errors) into one object - reuse it instead of branching on shapes.
- `src/AppContext.jsx` holds auth + mailbox global state backed by `localStorage` (`refreshToken`, `accessToken`, language, profile picture). It listens for `app:logout` (same-tab logout) and `storage` (cross-tab sync) events to keep context state in sync. `profilePictureVersion` is a counter that triggers `Navbar` to re-fetch the profile picture after an upload (no page reload).
- Discord OAuth: the redirect URI must be identical in the Discord Developer Portal, backend `DISCORD_REDIRECT_URI`, and frontend `VITE_DISCORD_REDIRECT_URI`. The frontend fetches a CSRF state from `GET /auth/discord/state` before redirecting to Discord. After the backend callback, the SPA reads `?code=<ticket>` from the URL and `POST /auth/exchange`s it for tokens.
- i18next bundles EN/DE/FR in `src/i18n.js`, keyed by ISO codes (`en`/`fr`/`de`); user language preference is persisted server-side via `PUT /api/v1/users/update-language`. `document.documentElement.lang` is updated when the language changes.

## Testing notes

- Backend tests: `@WebMvcTest` (controllers, sliced, with Mockito), `@DataJpaTest` (repository slices on H2), `JwtFilterTest` (malformed/expired token handling, valid-token auth population), and `FullFlowIntegrationTest` (end-to-end sign-up -> send -> inbox -> trash -> delete). Slice tests use H2 + `TestSecurityConfig` and the `jwt()` MockMvc post-processor to populate a principal without real JWKS. Slices do not load the full context, so populate collaborators via `@MockBean`.
- Testcontainers integration tests `AuthFlowIT` / `MailFlowIT` / `NotificationFlowIT` run against real MySQL, Redis, and Kafka using the `it` profile (datasource/Redis/Kafka via `@ServiceConnection`). They require Docker to be running.
- Frontend tests live in `src/tests/`; MSW handlers and axios-mock-adapter intercept HTTP, so tests never touch `apiClient`'s real interceptors unless configured to.
- Vitest setup file is `src/tests/setup.js` (referenced from `vite.config.js` `test.setupFiles`).

## Reference docs

Detailed feature list, API table, and architecture diagram live in `README.md` at the repo root - consult it for endpoint shapes and the error contract before editing controllers or `parseApiError.js`.