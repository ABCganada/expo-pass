# Last Mission Backend — CLAUDE.md

> This document defines the **package structure and coding conventions for the backend (`backend/`)**.
> Domain scope, roles, and overall workflows follow the top-level project `CLAUDE.md`; that document takes precedence on conflict.
> Everything below is based on patterns actually observed in `backend/src` (the `user` and `chat` modules) — new domains (Event/Reservation/Payment/Marketing) should follow these same patterns.

---

## 1. Runtime Environment

| Item             | Value                                                                                                                                                       |
| ---------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Language/Runtime | Java 25 (`languageVersion = JavaLanguageVersion.of(25)`)                                                                                                    |
| Framework        | Spring Boot 4, Spring Modulith 2.0.5, Spring Security                                                                                                       |
| Build            | Gradle Kotlin DSL (`build.gradle.kts`), private Nexus (Cloudflare Access) dependencies (`coder-han-auth-client`, `coder-han-vault-cert`)                    |
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
- Modules that currently exist: `user` (Identity), `chat` (messenger), `shared` (common, with `error`/`realtime` named interfaces), `config` (WebMvc/CORS, not a module), `kafka` (demo/test-only controller).
- The **Event / Reservation / Payment / Marketing** domains have no modules yet. Based on section 5 (Domains & Owned Tables) of the top-level `CLAUDE.md`, create new `event`, `reservation`, `payment`, and `marketing` packages. Domains never reference each other's tables directly — they collaborate only through each other's `application`-layer services/interfaces (exposed as named interfaces where needed).

---

## 3. Layered Structure (Hexagonal)

Inside a domain package, split into 4 layers (using the `chat` module as the example):

```
<domain>/
  domain/            Entities/value objects (records, immutable, validated in the compact constructor)
  application/        Services (@Service) + port interfaces (repositories, broadcasters, etc.)
  infrastructure/
    persistence/       JdbcTemplate-based repository implementations (package-private)
    websocket/(security/…)  Technology-specific adapter implementations
  presentation/        @RestController — kept thin; DTO records are nested inside the controller
  package-info.java    @ApplicationModule declaration
```

- `domain` records validate their invariants in the compact constructor (see `UserAccount`, `ChatRoom`). No framework annotations go here — pure Java models.
- `application` services depend only on port interfaces (`ChatRepository`, `ChatBroadcaster`, etc.); implementation classes live under `infrastructure`, encapsulated with `@Repository`/`@Component` and package-private visibility.
- Controllers only parse requests and map responses — no business logic. Constructor injection via Lombok's `@RequiredArgsConstructor`.

---

## 4. Persistence Conventions

- **JdbcTemplate + hand-written SQL** is the actual pattern in use (see `JdbcUserAccountRepository`, `JdbcChatRepository`). Declare `RowMapper` as a `private static final RowMapper<T>` field or a static method reference.
- The JPA dependency (`spring-boot-starter-data-jpa`) is present, but **no domain currently uses entities/Repositories.** Unless a new domain genuinely needs complex relationships, follow the existing pattern (JdbcTemplate) — get team agreement before mixing in JPA entities (the existing `README.md` / top-level `CLAUDE.md` state that MyBatis is unused and JPA+JDBC are used together).
- Schema changes happen **only through Flyway migrations.** `JPA_DDL_AUTO=none` is fixed, and `spring.flyway.enabled` is currently `false` (turn it on only after the initial schema baseline is finalized — see the comment in `application.yaml`). No migration files exist yet under `src/main/resources/db/migration/`, so when adding tables for a new domain (e.g. `events`, `orders`, `payments`, `banner_ads`), you'll be creating this directory and its naming convention (`V{n}__description.sql`) for the first time.
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

- There are only 4 roles: `user.domain.RoleCode` (`ADMIN`, `MANAGER`, `USER`, `DEVELOPER`). See the top-level `CLAUDE.md` for the Super Admin/Event Admin/End User mapping.
- Per-path permissions are managed as a whitelist in `SecurityConfig`:
  - `/api/v1/admin/**` → `ADMIN`
  - `/api/v1/manager/**` → `ADMIN`, `MANAGER`
  - `/api/kafka/test/**` → `ADMIN`, `DEVELOPER`
  - `/api/**` (everything else) → `ADMIN`, `MANAGER`, `USER`, `DEVELOPER`
  - `/ws/**` → all 4 authenticated roles (the WebSocket handshake isn't subject to CSRF, so `StompConfig`'s Origin check is the separate line of defense)
  - Every other request is `denyAll()` — a new endpoint must be explicitly included in this rule set to be reachable (if omitted, it's rejected by this rule itself rather than returning a 403 elsewhere).
- Authentication is **STATELESS** (no session) and integrated with in-house auth-core (`LastMissionAuthenticationFilter` + `AuthClient`; `UserProvisioningService` provisions the local `user_accounts` row on first login). CSRF is maintained separately via a cookie (`LASTMISSION-XSRF-TOKEN` / header `X-LASTMISSION-XSRF-TOKEN`).
- CORS, configured in `config.WebConfig`, allows only the frontend domains (`*.coder-han.com`, local `localhost:3000`) with `allowCredentials(true)` — since the auth-core session cookie is delivered same-site, update this too whenever a new frontend domain is added.

---

## 8. Realtime / Messaging

- WebSocket (STOMP, no SockJS) maintains **one connection per user**, managed by `shared.realtime`; chat, presence, etc. are split purely by subscription destination (`PresenceRegistry`, `StompConfig`). If a new domain needs realtime updates (e.g. check-in status, settlement notifications), extend this by adding a new destination on top of the shared connection — do not create a separate new WebSocket endpoint.
- Kafka can be toggled via a feature flag (`LASTMISSION_KAFKA_ENABLED`). `KafkaTestController` is a demo/developer-only example — use it only as a reference, not in production domain logic.

---

## 9. Testing Conventions

- JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`) + AssertJ. Service unit tests mock ports with `@Mock` and inject them into the service with `@InjectMocks` (see `ChatServiceTest`).
- Time-dependent logic injects `Clock` as a field, fixed via `@Spy Clock clock = Clock.fixed(...)` — any service using `OffsetDateTime.now(clock)` should always be tested this way.
- `architecture.ModularityTest` verifies all module boundaries/cyclic dependencies across the whole codebase. After adding a new domain module, always run this test locally and confirm it passes.
- Infrastructure-layer components — security filters, WebSocket guards, etc. — also get unit tests (see `LastMissionAuthenticationFilterTest`, `ChatSubscriptionGuardTest`, `StompInboundGuardTest`).

---

## 10. Checklist for Adding a New Domain (Event/Reservation/Payment/Marketing)

1. Create the `com.coderhan.lastmission.<domain>` package + declare `@ApplicationModule` in `package-info.java` (keep allowed dependencies minimal).
2. Build layers in order: `domain` (immutable records + validation) → `application` (services + port interfaces) → `infrastructure.persistence` (JdbcTemplate implementations) → `presentation` (`@RestController`).
3. Add domain-prefixed error codes to `shared.error.ErrorCode` + add the matching status-code case to the `ApiExceptionHandler` switch.
4. Add the new endpoint prefix and allowed roles to `SecurityConfig`'s path rules.
5. Write Flyway migrations (`db/migration/V{n}__...sql`) for any needed tables, and verify locally with `SPRING_FLYWAY_ENABLED=true`.
6. Write service unit tests + confirm `ModularityTest` passes.
7. If data from another domain is needed, design collaboration through that domain's `application` port (exposed as a named interface if necessary) instead of querying its tables directly.

---

## 11. Build / Deployment Notes

- `./gradlew test bootJar` runs inside the Docker build stage, so a test failure fails the image build itself.
- CI (`.github/workflows/build-backend-ghcr.yml`) reacts only to changes under `backend/**`, pushes the image to GHCR, and auto-commits the updated image tag into `k8s/backend/deployment.yaml` (ArgoCD deploys it afterward).
- The full list of local environment variables and prerequisites (WARP, Vault bootstrap, mkcert) follows the repository root `.env.example`.
