# Last Mission — Top-Level Project Guide

> This document defines the **overview, domains, and conventions for the entire monorepo** (frontend + backend + infrastructure).
> Screen/component-level frontend rules live in `frontend/CLAUDE.md`; backend-specific rules live in `backend/CLAUDE.md`.
> On conflict: if a sub-area `CLAUDE.md` exists, it takes precedence for that area; this document is the source of truth for domain scope, roles, and global conventions.

---

## 1. Project Overview

**Exhibition (Event) Reservation Management Platform** — a SaaS-style service that unifies management, promotion, reservation, payment, and entry verification for multiple exhibitions/events.

- Handles everything from event registration to QR e-ticket issuance, on-site check-in, and settlement/statistics, via web/mobile.
- Solves the manual sign-up problems of small/mid-size events, and provides QR-based e-tickets plus automated settlement/statistics.
- Supports an additional revenue model via **paid VIP banner ad** slots on the platform.
- This repository builds the above SaaS capabilities on top of existing in-house **central authentication (auth-core)** and **real-time messenger (STOMP)** infrastructure. Auth and messenger are already implemented; the Event/Reservation/Payment/Marketing domains are new development based on this spec.

### Current Implementation Scope (Important)

| Domain                                        | Status                                                                                                                                                           |
| --------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Identity** (auth/account)                   | Implemented — integrated with in-house auth-core, role-based (`RoleCode`) permissions                                                                            |
| **Chat** (messenger)                          | Implemented — real-time chat over STOMP                                                                                                                          |
| **Event / Reservation / Payment / Marketing** | Not implemented — new design/development target based on this document's spec. On the frontend these exist only as placeholders until the API/schema is approved |

Do not assume any feature not in the table above (reservations, payments, banners, etc.) already exists. Always check the actual code (`backend/src`, `frontend/features`) first.

---

## 2. Tech Stack (Actual Configuration — the Source of Truth for This Repository)

| Category               | Technology                                                                                                                  | Notes                                                                                    |
| ---------------------- | --------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------- |
| **Frontend**           | Next.js 16 (App Router), React 19, TypeScript, Redux Toolkit + RTK Query, STOMP(`@stomp/stompjs`)                           | See `frontend/CLAUDE.md` for details                                                     |
| **Backend**            | Java 25, Spring Boot 4, Spring Modulith (domain module boundary enforcement), Spring Security                               | See `backend/CLAUDE.md` for details                                                      |
| **Persistence**        | JPA + JDBC(`JdbcTemplate`) used together, Flyway migrations, CockroachDB (PostgreSQL-compatible)                            | **MyBatis is not used** — ORM/SQL mapping is unified as a JPA + JdbcTemplate combination |
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
k8s/              Kustomize manifests (frontend/backend/argocd)
.github/workflows/  Path-based CI (only builds the relevant image when backend/** or frontend/** changes)
.env.example      Local dev environment variable template (includes WARP/Vault/Kafka/DB prerequisites)
```

- Even though it's a monorepo, the frontend and backend are **deployed independently** (`.github/workflows/build-*-ghcr.yml` each react only to changes under `frontend/**` or `backend/**` respectively).
- After an image is built, CI commits the updated image tag directly into `k8s/*/deployment.yaml` (`git push origin HEAD:main`), and ArgoCD detects it and deploys (GitOps).

---

## 4. User Permission Levels (Roles & Permissions)

```
[Super Admin] ──▶ [Event Admin] ──▶ [End User]
   (platform operator / super privilege)   (event organizer / per-event privilege)   (event attendee / mobile web)
```

The actual implementation expresses this as the 4 values of `backend/.../user/domain/RoleCode.java`:

| RoleCode    | Planned Role          | Description                                                                                                                                             |
| ----------- | --------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `ADMIN`     | Super Admin           | Platform-wide settings, creating/deleting multiple exhibitions, assigning event admin accounts, VIP ad scheduling, access to payment/settlement reports |
| `MANAGER`   | Event Admin           | Managing reservations/payments/entry for the assigned exhibition, on-site QR scanner mode, replacing event content/banners                              |
| `USER`      | End User              | Browsing events, reserving/paying, viewing QR mobile tickets                                                                                            |
| `DEVELOPER` | (infrastructure only) | Internal developer account outside the platform's SaaS domain permission scheme                                                                         |

> **Accounting team** and **Marketer/Sponsor** are extended roles in the plan but do not yet exist in `RoleCode`. Before implementing, first decide whether to extend the existing 4 values or design a separate permission axis (e.g., per-domain permissions) — do not add role strings arbitrarily.

---

## 5. Domains & Owned Tables (Domain Ownership)

Each domain maps to exactly one backend Spring Modulith module (`@ApplicationModule` in `backend/.../<domain>/package-info.java`) and one frontend feature (`frontend/features/<domain>`). Domains never access another domain's tables/entities directly — they must collaborate only through that domain's service/API (`ModularityTest` verifies module-boundary violations at build time).

| Domain                        | Owner   | Owned Tables                                                          | Status                      |
| ----------------------------- | ------- | --------------------------------------------------------------------- | --------------------------- |
| Identity (auth/account)       | User    | `users`, `roles`, `event_admins`                                      | Implemented (`user` module) |
| Event                         | Event   | `events`, `tickets`, `event_contents`, `event_images`                 | Not implemented             |
| Reservation                   | Order   | `orders`, `order_items`, `checkins`                                   | Not implemented             |
| Payment                       | Payment | `payments`, `refunds`, `settlements`, `payment_logs`                  | Not implemented             |
| Marketing (ads)               | Banner  | `banner_ads`, `banner_slots`, `banner_clicks`, `banner_impressions`   | Not implemented             |
| (foundation) Chat (messenger) | Chat    | Chat rooms/messages (in-house infra feature, outside the SaaS domain) | Implemented (`chat` module) |

### Data Relationship Overview

```
Events
  │
  ├──▶ Tickets ──▶ Orders ──┬──▶ Order_Items
  │                          └──▶ Payments
  │                                 │
  └──▶ Banner_Ads            Checkins (check-ins based on Order)
```

---

## 6. Menu Structure (Summary)

| User Type        | Key Screens                                                                                                                           |
| ---------------- | ------------------------------------------------------------------------------------------------------------------------------------- |
| End User         | Home (event list + top VIP banner), event detail, reservation/payment, My Reservations (my page · QR entry ticket)                    |
| Event Admin      | Dashboard, attendee list management (XLSX download), content management, on-site entry scanner mode, statistics reports               |
| Super Admin      | Unified dashboard, exhibition/account management, VIP banner ad management, payment/settlement management, system settings & security |
| Accounting team  | Revenue/settlement dashboard, settlement management, settlement reports (PDF/XLSX), refund/payment history lookup                     |
| Marketer/Sponsor | Ad management, ad performance analysis (impressions, clicks, CTR), campaign management, performance reports (XLSX)                    |

Frontend screen ownership (reference): A = Identity (home/login/member & permission management), B = Event (event list/detail/management/ticket management), C = Reservation (reserve/QR/reservation list/check-in), D = Payment (payment/refund/settlement/revenue), E = Marketing (ads/banners/ad analytics).

---

## 7. Key Process Flows

1. **Platform & event setup**: Super Admin creates the event, then issues an Event Admin account.
2. **Content/ad registration**: Event Admin registers event content; marketer bids on/registers a VIP ad slot.
3. **Reservation & payment**: Customer reaches the event via a VIP banner/search on mobile web → after payment completes, a QR ticket is issued.
4. **On-site verification**: On event day, the Event Admin scans the QR in mobile scanner mode → immediate entry check-in.
5. **Settlement & statistics**: After the event ends, the platform fee is deducted automatically → a settlement report is issued to the organizer.

---

## 8. Cross-Cutting Backend Conventions (Shared Across Domains)

- **Module boundaries**: At the top of each domain package, declare `@ApplicationModule(displayName = "...", allowedDependencies = {...})` in `package-info.java`. When adding a new domain module, declare its allowed dependencies explicitly, and reference only `shared` (and named interfaces such as `shared::error`, `shared::realtime`) as common ground.
- **Common response/error handling**: API responses use `shared.ApiResponse<T>` (`success`/`message`/`data`); domain exceptions use `shared.error.BusinessException` + `shared.error.ErrorCode` (domain-prefixed, e.g. `CHAT_*`). New domains follow this same pattern.
- **Auth context**: The current-request user is identified only via the `user` module's `AuthenticatedUser`/`LastMissionPrincipal`. Domain modules never call the auth-core client directly.
- **Schema changes**: `JPA_DDL_AUTO=none` — schema is managed manually, only through Flyway migrations.

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
