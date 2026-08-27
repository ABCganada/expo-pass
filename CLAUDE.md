# Last Mission — Top-Level Project Guide

> This document defines the **overview, domains, and conventions for the entire monorepo** (frontend + backend + infrastructure).
> Screen/component-level frontend rules live in `frontend/CLAUDE.md`; backend-specific rules live in `backend/CLAUDE.md`.
> On conflict: if a sub-area `CLAUDE.md` exists, it takes precedence for that area; this document is the source of truth for domain scope, roles, and global conventions.

---

## 1. Project Overview

**Exhibition (Event) Reservation Management Platform** — a SaaS-style service that unifies management, promotion, reservation, payment, and entry verification for multiple exhibitions/events.

- Handles everything from event registration to QR e-ticket issuance, on-site check-in, and settlement/statistics, via web/mobile.
- Solves the manual sign-up problems of small/mid-size events, and provides QR-based e-tickets plus automated settlement/statistics.
- Supports an additional revenue model via **paid banner ad** slots on the platform (fixed-rate booking by slot × day, not an auction/bidding system).
- This repository builds the above SaaS capabilities on top of existing in-house **central authentication (auth-core)** and **real-time messenger (STOMP)** infrastructure.

### Current Implementation Scope (Important)

All six domains below are implemented end-to-end (backend REST API + DB tables + frontend pages), not placeholders.

| Domain                          | Status                                                                                                                    |
| -------------------------------- | -------------------------------------------------------------------------------------------------------------------------- |
| **Identity** (auth/account)      | Implemented — integrated with in-house auth-core, role-based (`UserRole`) permissions, admin member/role management       |
| **Chat** (messenger)              | Implemented — real-time chat over STOMP                                                                                   |
| **Event**                        | Implemented — event/ticket CRUD, content/image management, manager ownership checks, scheduled end-of-event detection    |
| **Reservation**                  | Implemented — order + per-ticket QR issuance, Redis-backed virtual waiting room, manager QR check-in scanner, self-healing reconciliation |
| **Payment**                       | Implemented — real Toss Payments confirm/cancel integration, tiered auto-refund policy, per-event settlement (5% flat commission) |
| **Marketing** (banner ads)        | Implemented — fixed-rate slot booking, capacity limits, impression/click tracking + CTR stats, ad settlement            |

Cross-domain integration between these modules is done via **Spring Modulith application events** (`@ApplicationModuleListener` / `@TransactionalEventListener`), not Kafka — Kafka is wired into the build but only exercised by a `DEVELOPER`-only demo controller (`kafka.KafkaTestController`), not by any real domain flow.

Always verify claims in this document against the actual code (`backend/src`, `frontend/features`) before relying on them — this file is maintained by hand and can drift from the implementation.

---

## 2. Tech Stack (Actual Configuration — the Source of Truth for This Repository)

| Category               | Technology                                                                                                                  | Notes                                                                                    |
| ---------------------- | --------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------- |
| **Frontend**           | Next.js 16 (App Router), React 19, TypeScript, Redux Toolkit + RTK Query, STOMP(`@stomp/stompjs`)                           | See `frontend/CLAUDE.md` for details                                                     |
| **Backend**            | Java 25, Spring Boot 4, Spring Modulith (domain module boundary enforcement), Spring Security                               | See `backend/CLAUDE.md` for details                                                      |
| **Persistence**        | JPA + JDBC(`JdbcTemplate`) used together, hand-written DDL, CockroachDB (PostgreSQL-compatible)                             | **MyBatis is not used.** `event`/`reservation`/`user` use JPA `@Entity`; `chat`/`payment`/`marketing` use hand-written JDBC. Flyway is a dependency but **disabled** (`spring.flyway.enabled=false`, `ddl-auto=none`) — schema is applied manually via `backend/src/main/resources/db/*_ddl.sql` scripts, not versioned Flyway migrations |
| **Messaging/Realtime** | Kafka, STOMP over WebSocket (no SockJS)                                                                                     |                                                                                          |
| **Auth**               | Integrated with in-house central auth (auth-core), mTLS, HashiCorp Vault (dynamic issuance of DB/Kafka client certificates) |                                                                                          |
| **CI/CD**              | GitHub Actions → GHCR (GitHub Container Registry) → ArgoCD → Kubernetes                                                     | **Jenkins/DockerHub are not used**                                                       |
| **Network**            | Cloudflare WARP (Zero Trust) + Private Hostname Route, Cloudflare Access                                                    | Required to reach the private Nexus, Vault, DB, and Kafka                                |

> The original planning document included `JPA/MyBatis used together`, `Jenkins`, and `DockerHub`, but the actual implementation follows the table above. Whenever the planning document and the actual configuration differ, always trust this table (and each sub-area `CLAUDE.md`).

---

## 3. Monorepo Structure

```
frontend/         Next.js app (see frontend/CLAUDE.md)
backend/          Spring Boot app (see backend/CLAUDE.md)
.github/workflows/  Path-based CI (only builds the relevant image when backend/** or frontend/** changes)
.env.example      Local dev environment variable template (includes WARP/Vault/Kafka/DB prerequisites)
```

- Even though it's a monorepo, the frontend and backend are **deployed independently** (`.github/workflows/build-*-ghcr.yml` each react only to changes under `frontend/**` or `backend/**` respectively).
- **Kubernetes manifests are not part of this repo.** After an image is built, CI checks out a separate central GitOps repository and commits the updated image tag into that repo's `apps/lastmission/<service>/deployment.yaml` (`git push origin HEAD:main`), and ArgoCD (watching that other repo) detects the change and deploys. In this public snapshot the `push` trigger on both build workflows is disabled (`workflow_dispatch` only), since the private Nexus/Vault/GitOps infra they depend on isn't reachable outside the original environment.

---

## 4. User Permission Levels (Roles & Permissions)

```
[Super Admin] ──▶ [Event Admin] ──▶ [End User]
   (platform operator / super privilege)   (event organizer / per-event privilege)   (event attendee / mobile web)
```

The actual implementation expresses this as the 4 values of `backend/.../user/UserRole.java`:

| UserRole    | Planned Role          | Description                                                                                                                                             |
| ----------- | --------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `ADMIN`     | Super Admin           | Platform-wide settings, event/manager management, banner ad approval, access to payment/settlement/ad-settlement dashboards                             |
| `MANAGER`   | Event Admin           | Managing their own events' tickets/content/reservations, QR check-in scanner, own-event settlements, banner ad registration (`marketer` actions also run under this role) |
| `USER`      | End User              | Browsing events, reserving/paying, viewing QR mobile tickets                                                                                            |
| `DEVELOPER` | (infrastructure only) | Internal developer account outside the platform's SaaS domain permission scheme (Kafka demo endpoints only)                                             |

> **Accounting team** and **Marketer/Sponsor** are still not separate roles — there is no 5th/6th `UserRole` value. Ad management ("marketer" work) runs under `MANAGER` (same `/api/v1/manager/**` prefix as event management), and settlement/revenue reporting ("accounting" work) runs under `ADMIN`. If these ever need to be split into their own roles, decide whether to extend `UserRole` or design a separate permission axis (e.g., per-domain permissions) — do not add role strings arbitrarily.

---

## 5. Domains & Owned Tables (Domain Ownership)

Each domain maps to exactly one backend Spring Modulith module (`@ApplicationModule` in `backend/.../<domain>/package-info.java`) and one frontend feature (`frontend/features/<domain>`). Domains never access another domain's tables/entities directly — they must collaborate only through that domain's service/API (`ModularityTest` verifies module-boundary violations at build time).

| Domain                        | Owner   | Owned Tables                                                                                                  | Status                        |
| ----------------------------- | ------- | -------------------------------------------------------------------------------------------------------------- | ------------------------------ |
| Identity (auth/account)       | User    | `user_accounts`, `user_role_codes`                                                                            | Implemented (`user` module)   |
| Event                         | Event   | `events`, `event_categories`, `event_contents`, `event_images`, `event_bookmarks`, `tickets`                  | Implemented (`event` module)  |
| Reservation                   | Order   | `reservation_orders`, `reservation_order_items` (check-in state lives on the order-item row, no separate checkins table) | Implemented (`reservation` module) |
| Payment                       | Payment | `payments`, `payment_refunds`, `payment_settlements`, `payment_ad_settlements`, `payment_logs`                | Implemented (`payment` module) |
| Marketing (ads)               | Banner  | `marketing_banner_slots`, `marketing_banner_ads`, `marketing_banner_ad_slots`, `marketing_banner_impressions`, `marketing_banner_clicks` | Implemented (`marketing` module) |
| (foundation) Chat (messenger) | Chat    | Chat rooms/messages (in-house infra feature, outside the SaaS domain)                                          | Implemented (`chat` module)   |

> `event` and `reservation` have no dedicated `db/*_ddl.sql` file (unlike `payment_ddl.sql`/`marketing_ddl.sql`/`user_ddl.sql`) — their tables are only inferable from JPA `@Table` annotations. Treat this as a real schema-tracking gap, not an oversight to copy for new domains.

### Data Relationship Overview

```
Events ──▶ Tickets ──▶ Reservation_Orders ──┬──▶ Reservation_Order_Items (QR + check-in state)
  │                                          └──▶ Payments ──┬──▶ Payment_Refunds
  │                                                            └──▶ Payment_Settlements
  └──▶ Banner_Ads ──▶ Payments (order_type=ADVERTISEMENT) ──▶ Payment_Ad_Settlements
```

---

## 6. Menu Structure (Summary)

Route groups under `frontend/app/`: `(user)/`, `manager/`, `admin/`, `developer/` (internal observability, outside the SaaS domain), `login/`.

| Role (route group)     | Key Screens                                                                                                                                                    |
| ----------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| End User (`(user)`)    | `exhibitions` list/detail, `reservations` + `reservations/payment` (+ success/fail), `tickets` / `tickets/qr`, `my/bookmarks`, `payments` (history), `vip-ads` (banner display), `support` |
| Event Admin (`manager`) | `events` (+ `new`, `[eventId]`), `reservations/[eventId]` (attendees), `check-in/[eventId]` (QR scanner) + `check-in/status`, `settlements` (own events), `banner-ads` (+ payment success/fail) |
| Super Admin (`admin`)  | `events` (+ `categories`), `reservations-summary` (cross-manager), `members` (role management), `payments` (settlement/ad-settlement dashboards, payment logs), `banners` (approve/reject) |

There is no separate "Accounting team" or "Marketer/Sponsor" role or route group — settlement/revenue reporting lives under the `admin`/`manager` payment screens, and ad management lives under `manager/banner-ads` + `admin/banners`.

---

## 7. Key Process Flows

1. **Event setup**: Event Admin (`MANAGER`) creates the event, tickets, and content/images under their own account; Super Admin can reassign an event's manager or publish/cancel any event.
2. **Ad registration**: Event Admin books a banner slot at its fixed `pricePerDay × days` rate (capacity-limited per slot, no bidding); the ad stays `PENDING` until paid, then `PAID` until Super Admin approves it (or rejects it, which triggers an automatic refund).
3. **Reservation & payment**: Customer browses/reserves on mobile web → `ReservationService` decrements ticket stock and creates one order item (with its own QR hash) per ticket → Toss Payments Widget checkout → `PaymentService.confirm` re-validates the amount server-side before confirming → a `PaymentConfirmedEvent` flips the order to `CONFIRMED` (skipped entirely for free/0-won tickets).
4. **On-site verification**: On event day, the Event Admin scans the ticket QR in the check-in screen (camera via `jsQR`, or manual entry) → `ReservationService.checkin` validates ownership/status and records `checkedInAt`.
5. **Settlement & statistics**: A daily scheduler (`EventEndScheduler`) detects events past their end date and publishes `EventEndedEvent` → `SettlementService` creates a per-event settlement (flat 5% commission) from completed/refunded payments; banner ads are settled the same way on expiry (`BannerAdScheduler` → `AdExpiredEvent`, no commission). Refunds follow a tiered auto-approval policy based on days-until-event-start; there is no manual approval step today.

---

## 8. Cross-Cutting Backend Conventions (Shared Across Domains)

- **Module boundaries**: At the top of each domain package, declare `@ApplicationModule(displayName = "...", allowedDependencies = {...})` in `package-info.java`. When adding a new domain module, declare its allowed dependencies explicitly, and reference only `shared` (and named interfaces such as `shared::error`, `shared::realtime`) as common ground.
- **Common response/error handling**: API responses use `shared.ApiResponse<T>` (`success`/`message`/`data`); domain exceptions use `shared.error.BusinessException` + `shared.error.ErrorCode` (domain-prefixed, e.g. `CHAT_*`). New domains follow this same pattern.
- **Auth context**: The current-request user is identified only via the `user` module's `AuthenticatedUser`/`LastMissionPrincipal`. Domain modules never call the auth-core client directly.
- **Schema changes**: `JPA_DDL_AUTO=none` and Flyway is disabled — schema is applied manually via hand-written scripts under `backend/src/main/resources/db/*_ddl.sql` (not all domains have one yet; see the note in section 5).

Detailed backend coding conventions (package layout, testing strategy, etc.) are covered in `backend/CLAUDE.md`.

---

## 9. Documentation Map

| Document                    | Scope                                                                       |
| --------------------------- | --------------------------------------------------------------------------- |
| `CLAUDE.md` (this document) | Project-wide overview, domains/roles/workflows, cross-cutting conventions   |
| `frontend/CLAUDE.md`        | Frontend design system, components, folder structure, coding rules          |
| `backend/CLAUDE.md`         | Backend package structure, module conventions, testing strategy             |
| `README.md`                 | Top-level repository introduction                                           |
| `.env.example`              | Local dev environment variables and prerequisites (WARP/Vault/mkcert) guide |
