# Frontend Development Guide

## Scope

This package is the Last Mission Next.js frontend. Keep it free of exam, registration, learning-management, and wrong-note domains.

The retained application scope is:

- shared authentication and current-user state
- user and admin application shells
- realtime presence and messenger
- theme support
- an empty admin member-management placeholder until its API and database contract are approved

## Structure

- `app/`: route entry points and layouts
- `features/auth`: authentication and current-user state
- `features/chat`: messenger UI, API, and models
- `features/shell`: user navigation shell
- `features/admin`: admin navigation shell
- `features/shared`: cross-feature API and UI
- `features/socket`: STOMP connection support
- `features/store`: Redux store setup
- `features/theme`: theme state and UI

Keep domain logic inside its feature. Page files should compose feature components and avoid duplicating API or state logic.

## Conventions

- Use TypeScript and the `@/` import alias.
- Use Redux Toolkit and RTK Query for shared server state.
- Keep component styles in colocated CSS Modules.
- Do not introduce mock member data or member-management APIs until the backend and schema are approved.
- Do not commit secrets, local environment files, generated certificates, build output, or dependencies.
- Keep infrastructure identifiers lowercase and hyphen-free when they are used as the cert-core project id: `lastmission`.

## Verification

Use Node.js 24 on Linux, matching the production container.

```bash
npm ci
npm run lint
npm run build
```

Before committing, confirm that the production build exposes only the intended routes and that no deleted exam-domain imports remain.
