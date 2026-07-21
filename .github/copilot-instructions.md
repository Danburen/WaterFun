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
- **❌ Never return `ApiResponse<Map<String, String>>`.** Every API response MUST use a concrete VO/DTO class (e.g., `ReAuthTokenVo`, `ReAuthKeyVo`, `ReAuthInfo`). This ensures type safety, self-documenting API, and easier frontend consumption.
- Request body: prefer concrete DTO classes over `@RequestBody Map<String, String>`. Use `Map` only for truly dynamic key-value data.

## Re-auth (re-authentication) flow
Sensitive operations (change phone/email, reset/set password, forgot-password) require the user to re-prove identity via SMS:

```
Step 1: GET  /api/auth/account/re-auth/info       → { maskedPhone: "138****0000" }
Step 2: POST /api/auth/account/re-auth              { phone, scene }        → sends SMS
Step 3: POST /api/auth/account/re-auth/verify       { scene, code }         → { reAuthToken }
Step 4: POST /api/auth/account/password/reset       { reAuthToken, ... }    → executes operation
```

- For unauthenticated flows (forgot-password), use `POST /api/auth/forgot-password/re-auth` with identifier + captcha.
- Redis key: `op:re-auth:{scene}:{uuid}` → userUid, TTL 5min, consumed via `getAndDel` (one-time).
- Scene-scoped: a `CHANGE_PHONE` token cannot be used for `CHANGE_EMAIL`.

## Redis key conventions
- Root namespace prefixes live in `RootKeyConstants` (e.g., `THRESHOLD`, `VERIFY`, `USER`, `CLOUD`). These are the top-level scope for `SCAN MATCH user:*` etc.
- Each service class must encapsulate its Redis key construction in **private (static) methods**.
- Sub-key segments (after the root namespace) are **hardcoded as inline strings** in the private method — do not extract them into constants.
- Method name describes the purpose (e.g., `targetMinKey`, `captchaKey`, `loginTempFailKey`), not the key structure.
- Only extract a shared builder class (`XxxKeyBuilder`) when **the same key pattern is used across 2+ service classes**; otherwise a private method suffices.
- ❌ No global `RedisKeyConstants`-style file listing key segments. Each service owns its key structure completely.

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

## DTO naming convention (user-facing API)

All POST request body DTOs MUST use `Req` suffix:
- ✅ `PwdLoginReq`, `SendCodeReq`, `PasswordChangeReq`, `ChangeEmailReq`, `ReAuthReq`
- ❌ `LoginRequest`, `PasswordChangeRequest`, `SendCodeDto`, `ReAuthRequestBody`
- Exception: GET request params may use `@RequestParam` directly (no DTO needed).

All response DTOs (returned inside `ApiResponse<T>`) MUST use `Resp` or `Vo` suffix:
- ✅ `AccountResp`, `ReAuthInfoResp`, `ReAuthTokenVo`, `LoginClientData`
- ❌ `ReAuthInfo`, `AccountData`, `PlainDTO`
- Exception: `CodeResult` and other internal service-core types that are service returns (not API-contract) may keep their existing naming.

## Enum convention
All enum fields MUST use `tinyint UNSIGNED` in the database with a JPA `@Converter(autoApply = true)`.

**Forbidden:**
- ❌ `@Enumerated(EnumType.STRING)` — stores string names, brittle on rename
- ❌ `@Enumerated(EnumType.ORDINAL)` — fragile when enum order changes
- ❌ `@Convert(disableConversion = true)` — defeats the converter

**Required pattern:**
```java
// 1. Enum with explicit values
@Getter
public enum AccountStatus {
    ACTIVE(0),
    SUSPENDED(1),
    DEACTIVATED(2);

    private final short value;
    AccountStatus(final int value) { this.value = (short) value; }

    public static AccountStatus fromValue(final int value) {
        for (AccountStatus s : values()) {
            if (s.value == value) return s;
        }
        throw new IllegalArgumentException("Unknown: " + value);
    }
}

// 2. Converter with autoApply
@Converter(autoApply = true)
public class AccountStatusConverter implements AttributeConverter<AccountStatus, Short> {
    @Override
    public Short convertToDatabaseColumn(AccountStatus attribute) {
        return attribute == null ? null : attribute.getValue();
    }
    @Override
    public AccountStatus convertToEntityAttribute(Short dbData) {
        return dbData == null ? null : AccountStatus.fromValue(dbData);
    }
}

// 3. Entity field — no @Enumerated, converter auto-applies
@ColumnDefault("'0'")
@Column(name = "account_status", columnDefinition = "tinyint UNSIGNED")
private AccountStatus accountStatus = AccountStatus.ACTIVE;
```

**Always add a COMMENT to the column** documenting each value:
```sql
account_status tinyint UNSIGNED DEFAULT 0 COMMENT '0=ACTIVE, 1=SUSPENDED, 2=DEACTIVATED'
```

Existing converters reside in the same package as their enum. All follow the pattern above.
- Before changing auth behavior, inspect all 3 layers: gateway filter/security, downstream `GatewayUserContextFilter`, and front-end axios auth handling.
- If adding new endpoint families, keep prefixing consistent (`/api/admin/...` vs `/api/...`) to avoid accidental route overlap.
- There are very few meaningful tests currently; when fixing regressions, validate by running the affected module and smoke-testing endpoints/UI path.
