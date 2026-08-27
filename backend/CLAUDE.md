# Last Mission Backend — CLAUDE.md

> This document defines the **package structure and coding conventions for the backend (`backend/`)**.
> Domain scope, roles, and overall workflows follow the top-level project `CLAUDE.md`; that document takes precedence on conflict.
> Everything below is based on patterns actually observed in `backend/src` across all six implemented modules (`user`, `chat`, `event`, `reservation`, `payment`, `marketing`) — a new domain should follow these same patterns.

---

## 1. Runtime Environment

| Item             | Value                                                                                                                                                       |
| ---------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Language/Runtime | Java 25 (`languageVersion = JavaLanguageVersion.of(25)`)                                                                                                    |
| Framework        | Spring Boot 4, Spring Modulith 2.1.0, Spring Security                                                                                                       |
| Build            | Gradle Kotlin DSL (`build.gradle.kts`), private Nexus (Cloudflare Access) dependencies (`example-auth-client`, `example-vault-cert`)                    |
| Container        | Multi-stage Dockerfile (`eclipse-temurin:25-jdk` build → `25-jre` runtime), runs `./gradlew test bootJar` during the build                                  |
| Local run        | `application.yaml` imports `.env` / `../.env` / `../../.env` in that order; the `application-dev.yaml` profile runs HTTPS locally using mkcert certificates |

Before adding a new dependency, make sure not to disturb the existing configuration in `build.gradle.kts` (Modulith BOM, Lombok, Flyway, etc.).

---

## 2. Module (Domain) Boundaries — Spring Modulith

Each domain is a single top-level package `com.coderhan.lastmission.<domain>`, and its `package-info.java` declares:

```java
@org.springframework.modulith.ApplicationModule(
        displayName = "Display Name",
        allowedDependencies = {"user", "shared", "shared::error", "shared::realtime"}
)
package com.coderhan.lastmission.<domain>;
```

- Any other domain package not listed in `allowedDependencies` cannot be imported — it will still compile, but `ModularityTest` (`ApplicationModules.of(LastMissionApplication.class).verify()`) catches the violation at build time. **After adding a new domain module, always run this test locally and make sure it passes.**
- A sub-package that multiple modules need to share is exposed as a named interface — e.g. `shared::error`, `shared::realtime` — by annotating it with `@org.springframework.modulith.NamedInterface("name")`. Sub-packages that don't need to be shared are not exposed this way.
- Modules that currently exist: `user` (Identity), `chat` (messenger), `event`, `reservation`, `payment`, `marketing`, `shared` (common, with `error`/`realtime` named interfaces), `config` (WebMvc/CORS, not a module), `kafka` (demo/test-only controller).
- Domains never reference each other's tables directly — they collaborate only through each other's `application`-layer services/interfaces (exposed as named interfaces where needed, e.g. `event`'s `ReservationQueryPort`/`PaymentEventQueryPort`) or through Spring Modulith application events (see section 8) — not Kafka.

---

## 3. Layered Structure (Hexagonal)

Inside a domain package, split into 4 layers (using the `chat` module as the example):

```
<domain>/
  domain/            Entities/value objects (records, immutable, validated in the compact constructor)
  application/        Services (@Service) + port interfaces (repositories, broadcasters, etc.)
  infrastructure/
    persistence/       Repository implementations — JdbcTemplate for chat/payment/marketing, JPA (@Entity + Spring Data) for event/reservation/user (package-private)
    websocket/(security/…)  Technology-specific adapter implementations
  presentation/        @RestController — kept thin; DTO records are nested inside the controller
  package-info.java    @ApplicationModule declaration
```

- `domain` records validate their invariants in the compact constructor (see `UserAccount`, `ChatRoom`). No framework annotations go here — pure Java models.
- `application` services depend only on port interfaces (`ChatRepository`, `ChatBroadcaster`, etc.); implementation classes live under `infrastructure`, encapsulated with `@Repository`/`@Component` and package-private visibility.
- Controllers only parse requests and map responses — no business logic. Constructor injection via Lombok's `@RequiredArgsConstructor`.

---

## 4. Persistence Conventions

- **This is a genuinely mixed JPA + JDBC codebase — pick per domain, not a single rule for everything:**
  - `event`, `reservation`, and `user` use JPA `@Entity` classes (e.g. `Event`, `Ticket`, `ReservationOrderEntity`, `UserAccountEntity`) with Spring Data JPA repositories for domains with real relational structure.
  - `chat`, `payment`, and `marketing` use **JdbcTemplate + hand-written SQL** (see `JdbcChatRepository`, and the `payment`/`marketing` `infrastructure/persistence` packages). Declare `RowMapper` as a `private static final RowMapper<T>` field or a static method reference.
  - MyBatis is still not used either way.
- Schema is **not** managed through Flyway migrations in practice: `JPA_DDL_AUTO=none` is fixed, and `spring.flyway.enabled` defaults to `false` and has not been turned on. Tables are created by hand-written scripts under `backend/src/main/resources/db/*_ddl.sql` (currently `user_ddl.sql`, `payment_ddl.sql`, `marketing_ddl.sql` — `event` and `reservation` have no ddl file checked in despite having live tables, which is a gap, not a pattern to copy). If you do turn Flyway on, use the `V{n}__description.sql` naming convention under `db/migration/`, which doesn't exist yet.
- Because CockroachDB `bigserial` ids can exceed 2^53 (the precision limit of a JS `Number`), **serialize every ID in API responses as a string** (see `Long.toString(id)` in `ChatController.RoomResponse`, etc.). New domain DTOs must do the same.

---

## 5. API Conventions

- Every REST response is wrapped in `shared.ApiResponse<T>` (`success`, `message`, `data`, `@JsonInclude(NON_NULL)`). On success use `ApiResponse.success(data)` or `ApiResponse.success(message, data)`.
- Controllers are `@RestController` + `@RequestMapping("/api/v1/<domain>")`; request/response DTOs are declared as `record`s inside the controller, converted from domain objects via a static `from(...)` factory.
- The current user is obtained via `@AuthenticationPrincipal LastMissionPrincipal principal` (id/email/name). If a role check (e.g. is-admin) is needed, also accept an `Authentication authentication` parameter and check its `GrantedAuthority` (see the `isAdmin(...)` helper).
- Path prefixes and permission mapping follow `SecurityConfig` — see section 6 below. Decide which permission group a new controller's path belongs to first, then place it under that prefix.

---

## 6. Error Handling

- Throw domain exceptions as `shared.error.BusinessException(ErrorCode, message)`.
- `shared.error.ErrorCode` is a single enum shared across all domains, with a domain-specific prefix (e.g. `CHAT_ROOM_NOT_FOUND`). When adding error codes for a new domain, add them to this enum in the form `EVENT_*`, `RESERVATION_*`, `PAYMENT_*`, `MARKETING_*`.
- The `switch (exception.errorCode())` in `shared.web.ApiExceptionHandler` is an **exhaustive switch** (no default), so adding a value to `ErrorCode` requires adding a matching case (HTTP status mapping) to this switch as well — otherwise it won't compile. Don't forget this when adding a new error code, or the build will fail.
- `IllegalArgumentException` is mapped to 400 by a separate handler (for immediate validation failures that don't carry their own status).

---

## 7. Authorization / Security

- There are only 4 roles: `user.UserRole` (`ADMIN`, `MANAGER`, `USER`, `DEVELOPER`). See the top-level `CLAUDE.md` for the Super Admin/Event Admin/End User mapping. There is no separate "marketer"/"accounting" role — ad management runs under `MANAGER`, settlement/revenue reporting under `ADMIN`.
- Per-path permissions are managed as a whitelist in `SecurityConfig`:
  - `/api/v1/admin/**` → `ADMIN`
  - `/api/v1/manager/**` → `MANAGER` or `ADMIN` (`hasAnyRole`, not `ADMIN`-only — a manager must be able to reach their own management screens)
  - `/api/kafka/test/**` → `DEVELOPER`
  - A small set of read/tracking endpoints are `permitAll()` regardless of the prefix above: public banner listing + impression/click tracking, and the Toss payment webhook (`/webhooks/payments/toss`, also CSRF-exempt since Toss can't present a CSRF token).
  - `/api/**` (everything else) → `ADMIN`, `MANAGER`, `USER`, `DEVELOPER`
  - `/ws/**` → all 4 authenticated roles (the WebSocket handshake isn't subject to CSRF, so `StompConfig`'s Origin check is the separate line of defense)
  - Every other request is `denyAll()` — a new endpoint must be explicitly included in this rule set to be reachable (if omitted, it's rejected by this rule itself rather than returning a 403 elsewhere).
  - A path prefix only grants *role* access — several manager/admin endpoints additionally check *ownership* in the service layer (e.g. `EventOwnershipValidator`, the reservation/settlement manager endpoints re-checking "does this manager actually own this event") so one manager can't reach another manager's data through a role-only gate.
- Authentication is **STATELESS** (no session) and integrated with in-house auth-core (`LastMissionAuthenticationFilter` + `AuthClient`; `UserProvisioningService` provisions the local `user_accounts` row on first login). CSRF is maintained separately via a cookie (`LASTMISSION-XSRF-TOKEN` / header `X-LASTMISSION-XSRF-TOKEN`).
- CORS, configured in `config.WebConfig`, allows only the frontend domains (`*.example.com`, local `localhost:3000`) with `allowCredentials(true)` — since the auth-core session cookie is delivered same-site, update this too whenever a new frontend domain is added.

---

## 8. Realtime / Messaging / Cross-Domain Events

- WebSocket (STOMP, no SockJS) maintains **one connection per user**, managed by `shared.realtime`; chat, presence, etc. are split purely by subscription destination (`PresenceRegistry`, `StompConfig`). The reservation waiting room (`WaitingRoomController`) instead uses a Redis-backed SSE stream, not STOMP. If a new domain needs realtime updates, extend the shared STOMP connection with a new destination rather than creating a separate WebSocket endpoint.
- **Kafka is not used for real cross-domain messaging today**, despite being wired into the build and toggleable via `LASTMISSION_KAFKA_ENABLED`. `KafkaTestController`/`KafkaTestReceiver` are `DEVELOPER`-only demo code — don't treat their presence as evidence that any domain actually publishes/consumes Kafka events.
- All real domain-to-domain integration (payment confirming a reservation, an ad rejection triggering a refund, an event ending triggering settlement, etc.) is done via **Spring Modulith application events**: `@ApplicationModuleListener` for durable, transactional-outbox-backed listeners (persisted to the `event_publication` table, survive a restart — used for the payment→reservation/marketing flows) and plain `@TransactionalEventListener(phase = AFTER_COMMIT)` for non-durable ones (used for payment's own listeners on marketing/event events). Match the durability guarantee to how bad it would be to silently drop that event on a crash — don't default to the non-durable one just because it's shorter to write.
- Two schedulers (`ReservationReconcileScheduler`, `BannerAdReconcileScheduler`) exist specifically to self-heal orders/ads stuck in a pending state if an event was ever missed — new event-driven flows that move money or stock should consider whether they need the same safety net.

---

## 9. Testing Conventions

- JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`) + AssertJ. Service unit tests mock ports with `@Mock` and inject them into the service with `@InjectMocks` (see `ChatServiceTest`).
- Time-dependent logic injects `Clock` as a field, fixed via `@Spy Clock clock = Clock.fixed(...)` — any service using `OffsetDateTime.now(clock)` should always be tested this way.
- `architecture.ModularityTest` verifies all module boundaries/cyclic dependencies across the whole codebase. After adding a new domain module, always run this test locally and confirm it passes.
- Infrastructure-layer components — security filters, WebSocket guards, etc. — also get unit tests (see `LastMissionAuthenticationFilterTest`, `ChatSubscriptionGuardTest`, `StompInboundGuardTest`).

---

## 10. Checklist for Adding a New Domain

This is the pattern `event`/`reservation`/`payment`/`marketing` were actually built with — follow it for the next one.

1. Create the `com.coderhan.lastmission.<domain>` package + declare `@ApplicationModule` in `package-info.java` (keep allowed dependencies minimal).
2. Build layers in order: `domain` (JPA `@Entity` if the domain has real relational structure, otherwise plain immutable records + validation) → `application` (services + port interfaces) → `infrastructure.persistence` (JdbcTemplate or Spring Data JPA, matching the choice above) → `presentation` (`@RestController`).
3. Add domain-prefixed error codes to `shared.error.ErrorCode` + add the matching status-code case to the `ApiExceptionHandler` switch.
4. Add the new endpoint prefix and allowed roles to `SecurityConfig`'s path rules — remember role alone may not be enough; add an ownership check in the service layer if one manager/user must not see another's data.
5. Write a hand-written DDL script under `db/<domain>_ddl.sql` for any needed tables (Flyway is present but disabled — don't assume `SPRING_FLYWAY_ENABLED=true` migrations are how schema actually ships today).
6. Write service unit tests + confirm `ModularityTest` passes.
7. If data from another domain is needed, design collaboration through that domain's `application` port (exposed as a named interface if necessary) instead of querying its tables directly. If the domains need to react to each other's state changes (not just query), prefer a Spring Modulith application event (section 8) over a direct synchronous call.

---

## 11. Build / Deployment Notes

- `./gradlew test bootJar` runs inside the Docker build stage, so a test failure fails the image build itself.
- CI (`.github/workflows/build-backend-ghcr.yml`) reacts only to changes under `backend/**`, pushes the image to GHCR, and auto-commits the updated image tag into `apps/lastmission/backend/deployment.yaml` in a **separate central GitOps repository** (checked out at build time) — the Kubernetes manifests are not part of this monorepo (ArgoCD deploys from that other repo afterward). In this public snapshot the workflow's automatic `push` trigger is disabled (`workflow_dispatch` only) since the private Nexus/Vault/GitOps access it needs isn't available outside the original infra.
- The full list of local environment variables and prerequisites (WARP, Vault bootstrap, mkcert) follows the repository root `.env.example`.
