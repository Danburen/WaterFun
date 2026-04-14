# WaterFun AI Coding Instructions

## Architecture map (monorepo)
- This is a mixed monorepo: Gradle multi-module backend + pnpm workspace frontend.
- Backend modules in [settings.gradle](../settings.gradle):
  - `waterfun-gateway`: API entry, JWT verify, auth rate-limit, user context header injection.
  - `waterfun-service`: user-facing business APIs (`/api/auth`, `/api/user`, `/api/post`, `/api/public`).
  - `waterfun-admin-service`: admin APIs (`/api/admin/**`).
  - `waterfun-service-core`: shared domain/services (auth, user, storage, persistence).
  - `waterfun-common-lib`: shared response/error/util classes.
- Frontend workspaces in [pnpm-workspace.yaml](../pnpm-workspace.yaml):
  - `waterfun-admin` (Vue 3 + Vite)
  - `waterfun-web-client` (Nuxt 3)
  - `waterfun-web-core` (shared TS types/utilities)

## Critical data/security flow
- Trust boundary is the gateway. `GatewaySecurityConfig` allows `/api/auth/**` and `/api/admin/auth/**`, protects others.
- Gateway validates RSA JWT + Redis JTI in `RsaJwtDecoder`, then injects `X-User-*` headers in `AuthGlobalFilter`.
- Downstream services currently `permitAll()` and rely on `GatewayUserContextFilter` + `UserCtxHolder` for identity.
- Login flow example: `/api/auth/login-by-password` -> `AuthCoreServiceImpl.BuildLoginResponse()` sets refresh cookie + returns access token in `ApiResponse`.

## API and error contract
- Success wrapper is `ApiResponse<T>` (`code/message/data`), not raw DTOs.
- Errors use `ErrorResponse` + i18n keys from `BaseResponseCode` and `messages.properties`.
- Keep controller return style consistent: `return ApiResponse.success(...)`.

## Project-specific coding patterns
- Keep controllers thin; put business logic into `waterfun-service-core/services/**`.
- Preserve existing package naming even if misspelled (`confirguation`, `constratin`) unless doing a full refactor.
- Reuse shared types/utilities from `waterfun-web-core/src/types/**` in both frontends.
- Axios wrappers already handle auth/csrf skip lists (`VITE_AUTH_SKIP_LIST`, `VITE_CSRF_SKIP_LIST`); do not duplicate token logic in each API file.

## Build / run / debug workflows
- Backend (Windows): use `gradlew.bat` at repo root.
  - Build all: `gradlew.bat build`
  - Run one service: `gradlew.bat :waterfun-service:bootRun` (same pattern for `gateway`, `admin-service`, `notify-service`)
  - Run tests for one module: `gradlew.bat :waterfun-gateway:test`
- Frontend:
  - Install: `pnpm install`
  - Admin dev: `pnpm adev` (root script -> `@waterfun/admin`)
  - Web client dev: `pnpm cdev` (root script -> `@waterfun/web-client`)

## Environment / integration points
- Local infra: MySQL + Redis are required. DB schema/seed scripts live in `sqls/`.
- Shared backend config template is `deploy/shared/config/common.yml` (datasource, redis, jwt public key path).
- Windows helper `deploy/bin/database-service.bat` starts MySQL + Redis, but uses machine-specific Redis path.
- Cloud integrations exist in core services (Tencent COS, Aliyun SMS, Resend email); prefer extending existing service abstractions (`CloudFileService`, auth code senders).

## Contracts and references
- API artifacts are checked in under `docs/api_docs/*.json` and `reference/*.openapi.json`; update when changing external API shapes.
- Admin frontend API files map directly to admin-service routes (e.g., `waterfun-admin/src/api/user.ts` <-> `/api/admin/users/**`).

## Practical guardrails for agents
- Before changing auth behavior, inspect all 3 layers: gateway filter/security, downstream `GatewayUserContextFilter`, and front-end axios auth handling.
- If adding new endpoint families, keep prefixing consistent (`/api/admin/...` vs `/api/...`) to avoid accidental route overlap.
- There are very few meaningful tests currently; when fixing regressions, validate by running the affected module and smoke-testing endpoints/UI path.
