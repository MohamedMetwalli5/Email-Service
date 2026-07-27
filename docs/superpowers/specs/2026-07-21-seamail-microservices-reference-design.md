# Seamail Microservices Reference Architecture - Design

Date: 2026-07-21
Status: Approved by user

## Purpose

Evolve the Seamail backend from a single Spring Boot monolith into a small microservices
landscape that serves as a reference and proof of work for mid-senior Java/Spring Boot
interviews.
Every added capability must be general-purpose (transferable to any future project), not
domain-specific.
Changes should stay moderate in size; the frontend remains functionally unchanged.

## Approved decisions

| Decision | Choice | Rationale |
|---|---|---|
| Extraction scope | Extract auth-service and notification-service; mail-service keeps the rest | Covers the two most transferable microservice stories: distributed security and event-driven design |
| Token strategy | RS256 with JWKS; mail/notification become OAuth2 resource servers | Industry standard; no shared secrets; replaces JJWT HS256 + custom JwtFilter |
| API gateway | Spring Cloud Gateway (WebFlux) as single entry point on :8081 | Expected edge component; centralizes routing and CORS; no Eureka (DNS-based discovery like Docker/K8s) |
| Notification behavior | In-app REST notification feed, own DB, no frontend wiring yet | Fully backend, fully testable |
| Messaging | Kafka (single broker, KRaft mode) | Most commonly requested messaging stack |
| Migrations | Flyway per service; `ddl-auto=validate` retained | Universal practice; entities cannot drift from schema |
| Testing | Keep existing slice tests; add Testcontainers `@ServiceConnection` integration tests | Real MySQL/Redis/Kafka in tests; strong mid-senior differentiator |
| API docs | springdoc-openapi per service, aggregated at the gateway | Expected everywhere, cheap to add |
| Observability | Micrometer + Prometheus + Grafana; Micrometer Tracing (Brave) to Zipkin | Standard stack; one trace across gateway -> mail -> Kafka -> notification |

Explicitly out of scope: Resilience4j/rate limiting, Kubernetes manifests, WebSocket push,
welcome-notification on signup, frontend feature changes (only the proxy target).

## Repository layout

Maven multi-module build; root `pom.xml` is an aggregator parent
(`com.seamail:seamail-parent`, packaging `pom`) so `mvn test` at root runs all modules.
Spring Boot remains 3.2.3, Java 21; Spring Cloud 2023.0.x for the gateway.

```
Email-Service/
  pom.xml                     # aggregator parent
  api-gateway/                # com.seamail.gateway
  auth-service/               # com.seamail.auth
  mail-service/               # renamed from backendemailservice/; com.seamail.mail
  notification-service/       # com.seamail.notification
  frontend-email-service/     # unchanged
  observability/              # prometheus.yml, grafana provisioning
  docs/
  docker-compose.yml          # rewritten
```

The existing `backendemailservice/` directory is git-renamed to `mail-service/` and its
Java packages renamed from `com.backendemailservice.backendemailservice` to
`com.seamail.mail`.

## Ports (host)

| Component | Port |
|---|---|
| api-gateway | 8081 (frontend proxy target, unchanged from today) |
| auth-service | 8082 |
| mail-service | 8083 |
| notification-service | 8084 |
| MySQL | 3307 |
| Kafka | 9092 |
| Zipkin | 9411 |
| Prometheus | 9090 |
| Grafana | 3000 |

Containers use the same port inside the compose network (no host/container offset except
MySQL 3307->3306 and Kafka's advertised listener).

## auth-service

Owns identity. Owns the `users` table in schema `seamail_auth`
(`email` PK, `password`, `language`, `profile_picture LONGBLOB`).

Endpoints moved here unchanged in shape:
- `POST /api/v1/sign-in`, `POST /api/v1/sign-up`
- `POST /api/v1/auth/refresh` (Redis-stored rotating refresh tokens, 7-day TTL, atomic
  `delete`-as-claim; revoked on password change and account deletion)
- `POST /api/v1/auth/exchange`, `GET /api/v1/auth/discord/state`, `GET /api/v1/auth/discord`
  (Discord ticket flow moves wholesale)
- `PUT /api/v1/change-password`, `PUT /api/v1/update-language`,
  `DELETE /api/v1/delete-account`, `POST|GET /api/v1/{email}/profile-picture`

New/changed behavior:
- Access tokens signed RS256 via Nimbus (`spring-security-oauth2-jose`, `NimbusJwtEncoder`).
- RSA keypair loaded from PEM files configured by env (`JWT_PRIVATE_KEY_PATH`,
  `JWT_PUBLIC_KEY_PATH`); if absent, an ephemeral pair is generated at startup with a WARN
  log (local dev only; restarts invalidate sessions, documented).
- `GET /.well-known/jwks.json` exposes the public JWK set (permitAll).
- `GET /internal/users/{email}/exists` returns 200 or 404; consumed only by mail-service
  via Feign; the gateway never routes `/internal/**`.
- Flyway `V1__create_users_table.sql`; `ddl-auto=validate` retained.
- Redis keys prefixed `auth:` (refresh tokens, Discord tickets, CSRF nonces).

## mail-service

Keeps: inbox/outbox/trashbox/emails endpoints with pagination, `Mailbox` enum, send /
move-to-trash / delete flows, Redis inbox cache (`userEmail+page+size`, evict-all on
mutation), DTO layering, `ErrorResponse`/`ValidationErrorResponse` contract,
`GlobalExceptionHandler`.

Removed: `User` entity, `UserRepository`, all auth/user controllers, `JwtFilter`,
`JwtUtil`, `CustomUserDetailsService`, JJWT dependencies.

New/changed behavior:
- `spring-boot-starter-oauth2-resource-server`; `spring.security.oauth2.resourceserver.jwt.jwk-set-uri`
  points at auth-service. Controllers read the caller email from the `Jwt` principal
  subject instead of the old `Authentication` handling.
- Custom `AuthenticationEntryPoint` / `AccessDeniedHandler` emit the existing
  `ErrorResponse` JSON shape so the frontend `parseApiError.js` keeps working.
- `sendEmail` flow: Feign call `AuthUserClient.existsByEmail(receiver)` ->
  404 becomes `ReceiverNotFoundException`; Feign timeout/5xx becomes 503 via
  `GlobalExceptionHandler` (`SERVICE_UNAVAILABLE`, "Authentication service unavailable").
  Feign timeouts configured explicitly (connect 2s, read 3s).
- After commit, publish `EmailSentEvent` to Kafka topic `email.sent` via
  `@TransactionalEventListener(phase = AFTER_COMMIT)`. Deliberately not a full outbox
  table; the trade-off (at-most-once between commit and broker ack) is documented as a
  talking point.
- Redis keys prefixed `mail:` (inbox cache).
- Flyway `V1__create_emails_table.sql` including the `email_id_seq` sequence table.

## notification-service

New service. Schema `seamail_notifications`.

Table `notifications`:
`id BIGINT AUTO_INCREMENT PK`, `event_id VARCHAR(64) NOT NULL UNIQUE`,
`recipient_email VARCHAR(255) NOT NULL`, `type VARCHAR(32) NOT NULL`,
`source_email_id BIGINT NOT NULL`, `subject_snapshot VARCHAR(255) NOT NULL`,
`sender_snapshot VARCHAR(255) NOT NULL`, `is_read TINYINT(1) NOT NULL DEFAULT 0`,
`created_at DATETIME NOT NULL`. Index on `(recipient_email, is_read)`.

- Kafka consumer on `email.sent`, group `notification-service`, manual offset commit
  after successful DB insert. Idempotent: duplicate `event_id` is caught via the unique
  constraint and skipped (logs at INFO).
- `DefaultErrorHandler` with 3 exponential backoffs, then
  `DeadLetterPublishingRecoverer` to `email.sent.DLT`.
- Resource-server secured REST (caller email from `Jwt` subject):
  - `GET /api/v1/notifications?page=0&size=20` -> `Page<NotificationResponseDto>`
  - `GET /api/v1/notifications/unread-count` -> `{ "count": n }`
  - `POST /api/v1/notifications/{id}/read` -> marks one notification read; 404 if not
    found or owned by another user
- Flyway `V1__create_notifications_table.sql`.
- Same error contract classes (duplicated per service on purpose; no shared kernel
  library - the trade-off is a talking point).

## Kafka event contract

Topic `email.sent` (auto-created in dev, 1 partition, RF 1; documented as dev topology).

```json
{
  "eventId": "uuid",
  "eventType": "EMAIL_SENT",
  "occurredAt": "ISO-8601",
  "emailId": 123,
  "sender": "a@seamail.com",
  "receiver": "b@seamail.com",
  "subject": "..."
}
```

Producer: mail-service (`StringKafkaTemplate`, JSON via `JsonSerializer`).
Consumer: notification-service (`JsonDeserializer`, trusted packages pinned to the event
package, type headers mapped to a local `EmailSentEvent` record).

## api-gateway

Spring Cloud Gateway (WebFlux), Spring Cloud 2023.0.x. Static routes from env-config
(no discovery server):

- `/api/v1/sign-in`, `/api/v1/sign-up`, `/api/v1/auth/**`, `/api/v1/change-password`,
  `/api/v1/update-language`, `/api/v1/delete-account`, `/api/v1/*/profile-picture`,
  `/.well-known/**` -> `http://auth-service:8082`
- `/api/v1/notifications/**` -> `http://notification-service:8084`
- remaining `/api/v1/**` -> `http://mail-service:8083`

- CORS configured here only; CORS config removed from the services.
- Gateway does not validate tokens; each resource server validates its own (defense in
  depth, dumb edge - documented talking point).
- springdoc-openapi webflux integration aggregates downstream `/v3/api-docs`; Swagger UI
  at `http://localhost:8081/swagger-ui.html` lists all three services.
- Actuator health exposed for compose healthchecks.

## Observability

- All four Java modules: `micrometer-registry-prometheus`,
  `micrometer-tracing-bridge-brave`, `zipkin-reporter-brave`; sampling 1.0 in dev.
- Trace propagation works through WebFlux gateway routes, MVC controllers, Feign, and
  spring-kafka producer/consumer instrumentation, giving one trace per user action.
- Logback pattern includes `traceId`/`spanId` from MDC in every service.
- Prometheus (`observability/prometheus.yml`) scrapes all services' `/actuator/prometheus`.
- Grafana provisioned with the Prometheus datasource and one starter dashboard
  (HTTP request rate/latency per service + JVM basics).
- `management.endpoints.web.exposure.include` becomes `health,info,prometheus`;
  existing liveness/readiness groups retained per service.

## Testing strategy

- Existing slice tests travel with their owning module:
  - mail-service: `EmailsControllerTest`, `EmailServiceTest`, `EmailRepositoryTest`
    (security slices switch from the deleted `JwtFilter` to
    `SecurityMockMvcRequestPostProcessors.jwt()`).
  - auth-service: `AccessControllerTest`, `UsersControllerTest`, `UserServiceTest`,
    `UserRepositoryTest`, refresh-rotation and Discord-flow tests.
  - `JwtFilterTest` is deleted with the filter; resource-server behavior is covered by
    controller slices using `jwt()` post-processors and by integration tests.
- New Testcontainers tests (`@ServiceConnection`, containers reused where legal).
  Auth in these ITs uses the `jwt()` MockMvc post-processor (the same synthetic
  `Jwt` principal used by the controller slices) rather than launching auth-service and
  signing real RS256 tokens. This keeps each IT scoped to its own module's container
  lifecycle and avoids coupling two services' startup in a single test. Resource-server
  filter behavior (abort on missing/invalid token, subject extraction) is already proven
  by the controller-level `@WebMvcTest` slices; the ITs focus on the things only real
  MySQL/Redis/Kafka can prove (Flyway migrations, JSON Kafka serialization, refresh
  rotation against Redis, idempotent Kafka consumption against a real broker). An
  end-to-end IT exercising real JWKS fetch from a live auth-service container is a
  documented upgrade path, deliberately not built to keep these tests fast and decoupled.
  - auth-service `AuthFlowIT`: MySQL + Redis; sign-up -> sign-in -> refresh rotation
    (second use of old token -> 401) -> JWKS endpoint returns the public key.
  - mail-service `MailFlowIT`: MySQL + Redis + Kafka; migrated `FullFlowIntegrationTest`
    plus the full mailbox lifecycle (send -> inbox + outbox -> move-to-trash -> trashbox
    -> permanent delete) and a test consumer asserting exactly one `EmailSentEvent` on
    `email.sent` with matching payload; Feign client is `@MockBean`-ed (WireMock noted as
    an alternative).
  - notification-service `NotificationFlowIT`: MySQL + Kafka; publish a real
    `EmailSentEvent` to the topic -> feed returns it, unread-count is 1, mark-read works,
    publishing the same `eventId` twice still yields one row (asserted via a stability
    window so a slow duplicate insert cannot slip through after the test ends).
- H2 remains for lightweight slices; the `test` profile keeps `spring.cache.type=simple`
  where Redis semantics are irrelevant.
- Root `mvn test` runs all modules; frontend `npm test` unchanged.

## Frontend impact

- Dev proxy and Docker build already target :8081; no code change expected.
- `DISCORD_REDIRECT_URI` / `VITE_DISCORD_REDIRECT_URI` keep pointing at
  `http://localhost:8081/api/v1/auth/discord` - now served by auth-service through the
  gateway, so the Discord Developer Portal entry stays valid.
- If the resource-server 401 body differs from today's shape, `parseApiError.js` already
  normalizes the Spring Security `{"message":"Unauthorized"}` shape; the custom entry
  point keeps that contract.

## Infrastructure (docker-compose)

One MySQL 8.0 container with an init script creating three schemas
(`seamail_auth`, `seamail_mail`, `seamail_notifications`) and three users with grants
scoped to their own schema (database-per-service story on one server).
`SQL Scripts/Tables.sql` is retired in favor of Flyway migrations (git history preserves
it).
Redis single instance, logical separation by key prefix.
Kafka single broker KRaft (no Zookeeper).
Zipkin, Prometheus, Grafana added.
Each app service gets `depends_on` with healthcheck conditions; gateway is the only app
service with a host-published port besides the frontend.

## Documentation updates

- README: new architecture diagram, updated API table (unchanged endpoint shapes, new
  notifications endpoints), new "Interview talking points" section mapping each mechanism
  to its likely interview questions:
  JWKS vs shared-secret JWT; AFTER_COMMIT events vs transactional outbox; idempotent
  consumer + DLT; refresh-token rotation race (`delete` as atomic claim); inbox cache
  eviction strategy; Flyway vs `ddl-auto`; Testcontainers vs H2; trace propagation demo
  (one action -> one trace across four processes).
- AGENTS.md: new module layout, commands, ports, env vars.
- `.env.docker.example`: new keys (`JWT_PRIVATE_KEY_PATH`, `JWT_PUBLIC_KEY_PATH`,
  Kafka bootstrap, Zipkin endpoint) with dummy values.

## Implementation phases (for the plan)

1. Repo restructure: aggregator parent pom, rename `backendemailservice/` ->
   `mail-service/`, rename packages to `com.seamail.mail`; existing tests stay green.
2. Flyway in mail-service (single-schema migration first); compose MySQL init update.
3. Extract auth-service (code move, RS256/JWKS issuance, internal exists-endpoint,
   Flyway users migration); mail-service becomes a resource server with Feign
   receiver-check.
4. api-gateway + compose wiring + CORS centralization; full-stack smoke via gateway.
5. Kafka + notification-service (event contract, idempotent consumer, DLT, REST feed).
6. Observability (Prometheus, Zipkin tracing, log correlation, Grafana provisioning).
7. OpenAPI per service + gateway aggregation.
8. Testcontainers integration tests per module.
9. Docs: README talking points, AGENTS.md, env templates.

Each phase ends with: `mvn test` green in touched modules, and from phase 4 onward a
docker-compose smoke check (sign-up -> sign-in -> send -> inbox -> notifications).
