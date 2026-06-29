# Auth Login Flow Reference

> Device risk control, RT family management, Redis key structure, cookie lifecycle, and error code mapping.

---

## 1. Redis Key Structure

### 1.1 Device Keys

| Key | Type | TTL | Purpose |
|-----|------|-----|---------|
| `user:{uid}:devices` | Set (member→timestamp) | ∞ | All known device hashes for a user |
| `user:{uid}:device:{did}:last_active` | String (timestamp) | 30d | Last active time per device |
| `user:{uid}:device:{did}:access` | String (jti) | `token.access.expiration` (30d) | Current valid AT jti per device |

### 1.2 RT Family Keys

| Key | Type | TTL | Purpose |
|-----|------|-----|---------|
| `user:{uid}:device:{did}:rt_family` | String (family UUID) | `token.refresh.family` (30d) | Current family ID for a device |
| `user:{uid}:rt_families` | Set (member→timestamp) | ∞ | All family IDs a user has ever had |
| `user:{uid}:device:{did}:rt_family:{familyId}:ref` | String (RT value) | `token.refresh.rotate` (7d) | Current refresh token value |

### 1.3 Token Constants (from `application.yml`)

| Parameter | Value | Purpose |
|-----------|-------|---------|
| `token.access.expiration` | 2592000s (30d) | AT + jti lifetime |
| `token.refresh.rotate` | 604800s (7d) | RT single-rotation lifetime |
| `token.refresh.family` | 2592000s (30d) | RT family lifetime |

Device salt (from `device.salt` in external `common.yml`): Base64-encoded HMAC key.

---

## 2. Device Fingerprint

### 2.1 Client Side (`fingerprint.ts`)

```
collectDeviceFeatures() → JSON({
  screen: { width, height, pixelRatio, colorDepth },
  browser: { userAgent, language, cookieEnabled },
  timezone, timezoneOffset, hardwareConcurrency
})

generateFingerprint() = SHA-256(featureJSON).substring(0, 16)
```

Deterministic — same browser+machine always produces **same** `deviceFp` (no random salt).

The `getDeviceInfo()` function additionally detects `deviceType`, `os`, `browser`, `screenResolution` for audit logging.

### 2.2 Server Side (`DeviceServiceImpl`)

```java
calculaateDid(userUid, dfp) → SHA-256(deviceHashSalt + dfp + userUid) → Base64
```

- Called by `TokenService.validateRefreshToken()` to compute `calculatedHashDid` from raw `dfp`
- Called by `generateAndStoreDeviceId()` to persist `did` in the user's device set
- `isNewDeviceDid()` checks if the computed hash exists in `user:{uid}:devices` set → logs "New Device detected" if absent (TODO: add risk control)

Device TTLs (config):
- `device.temp.ttl`: 3600s (1h)
- `device.short.ttl`: 604800s (7d)
- `device.long.ttl`: 7776000s (90d)

---

## 3. Full Login Flow (Password)

### Step-by-step

```
Frontend                          Gateway                       User Service                    Redis
   │                                │                                │                            │
   │  POST /api/auth/login-by-password│                               │                            │
   │  { username, password, captcha,  │                               │                            │
   │    deviceFp, deviceInfo }        │                               │                            │
   ├──────────────────────────────►  │                                │                            │
   │                                │  JWT auth? No (whitelisted)     │                            │
   │                                │  → forward                     │                            │
   │                                ├──────────────────────────────► │                            │
   │                                │                                │ 1. verifyCaptcha(CAPTCHA_KEY)│
   │                                │                                │ 2. BCrypt.matches(password)  │
   │                                │                                │ 3. checkLoginBan()          │
   │                                │                                │ 4. onlineUserService.update  │
   │                                │                                │ 5. auditLog: LOGIN          │
   │                                │                                │                            │
   │                                │                                │ BuildLoginResponse(user, dfp)│
   │                                │                                │                            │
   │                                │                                │ createNewTokens(userUid, dfp)│
   │                                │                                │                            │
   │                                │                                │ deviceService.generateAndStore│
   │                                │                                │ calculaateDid(userUid, dfp) │
   │                                │                                ├─────────────────────────► │
   │                                │                                │  SADD user:{uid}:devices   │
   │                                │                                │◄────────────────────────── │
   │                                │                                │                            │
   │                                │                                │ tokenService.genCacheNewAcc │
   │                                │                                │  - generate JWT (claims:    │
   │                                │                                │    sub=uid, jti=uuid,       │
   │                                │                                │    did=deviceId)            │
   │                                │                                │  - SET user:{uid}:device:   │
   │                                │                                │    {did}:access → jti      │
   │                                │                                │  - updateUserDeviceActive   │
   │                                │                                ├─────────────────────────► │
   │                                │                                │◄────────────────────────── │
   │                                │                                │                            │
   │                                │                                │ tokenService.genAndCacheRef │
   │                                │                                │  - GET rt_family for did    │
   │                                │                                │  → null (first time)        │
   │                                │                                │    → create family UUID     │
   │                                │                                │  - SADD rt_families         │
   │                                │                                │  - SET rt_family (30d)      │
   │                                │                                │  - SET ref (7d, RT value)   │
   │                                │                                ├─────────────────────────► │
   │                                │                                │◄────────────────────────── │
   │                                │                                │                            │
   │                                │                                │ CookieUtil.setTokenCookie   │
   │                                │                                │  Set-Cookie: REFRESH_TOKEN  │
   │                                │                                │  (httpOnly, Strict, /)      │
   │                                │                                │  + expire old /api/auth     │
   │                                │                                │                            │
   │                                │◄──────────────────────────────│                            │
   │                                │                                │                            │
   │  ← 200 { accessToken, exp }    │                                │                            │
   │◄───────────────────────────────│                                │                            │
```

### Response Body (LoginClientData)

```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiJ9...",
  "exp": 2592000
}
```

### Response Cookie

```
Set-Cookie: REFRESH_TOKEN=<jwt>; Path=/; HttpOnly; SameSite=Strict; Max-Age=604800
Set-Cookie: REFRESH_TOKEN=; Path=/api/auth; Max-Age=0  (expire old path)
```

---

## 4. RT Family Rotation (`TokenService.genAndCacheRefToken`)

### State Machine

```
            ┌──────────────────────────────────────────────┐
            │  GET user:{uid}:device:{did}:rt_family        │
            │                                              │
            ▼                                              │
        ┌──────┐                                           │
 null ─►│CREATE│  New family UUID, SADD rt_families,       │
        │FAMILY│  SET rt_family (30d)                      │
        └──┬───┘                                           │
           │                                               │
           ▼                                               │
        ┌──────┐                                           │
 exists►│READ  │  GET ref in current family                │
        │FAMILY│                                           │
        └──┬───┘                                           │
           │                                               │
     ┌─────┴─────┐                                         │
     ▼           ▼                                         │
  ┌──────┐   ┌──────┐                                      │
  │NULL  │   │EXISTS│  Old ref found → DEL old ref (revoke)│
  │(TTL  │   └──┬───┘                                      │
  │exp.) │      │                                          │
  └──┬───┘      │                                          │
     │          │                                          │
     │          │                                          │
     ├──────────┤                                          │
     ▼          ▼                                          │
  Rebuild family ──────────────────────────────────────────┘
  (remove old family from set,
   create new family UUID,
   SET rt_family 30d)
           │
           ▼
      ┌────────┐
      │CREATE RT│  SET new ref (7d)
      │value=UUID│  Return TokenResult(RT, 604800)
      └────────┘
```

### Refresh Token Value

RT value = `StringUtil.noDashUUIDString(UUID.randomUUID())` — a plain UUID, NOT a JWT. The AT is an RSA-signed JWT.

---

## 5. Refresh Flow

```
Frontend                         Gateway                        User Service
   │                                │                                │
   │  AT expires, 401               │                                │
   │  intercept in axiosRequest.ts  │                                │
   │                                │                                │
   │  GET /api/auth/refresh         │                                │
   │  ?deviceFp=<fp>                │                                │
   │  Cookie: REFRESH_TOKEN=<rt>    │                                │
   │  (no Authorization header)     │                                │
   ├─────────────────────────────► │  JWT auth? No (whitelisted)     │
   │                                ├──────────────────────────────► │
   │                                │                                │
   │                                │                                │ refreshAccessToken(rt, dfp)
   │                                │                                │  1. StringUtil.isBlankThen(rt) → throw
   │                                │                                │  2. UserCtxHolder.safeGetUserId()
   │                                │                                │     (populated by AuthGlobalFilter
   │                                │                                │      from Cookie REFRESH_TOKEN)
   │                                │                                │  3. tokenService.validateRefreshToken
   │                                │                                │     a. calculaateDid(userUid, dfp)
   │                                │                                │     b. GET rt_family → null → REAUTHENTICATE_REQUIRED
   │                                │                                │     c. GET ref → null → REAUTHENTICATE_REQUIRED
   │                                │                                │     d. return RefreshTokenPayload(uid, did)
   │                                │                                │  4. genAndCacheRefToken (rotate)
   │                                │                                │  5. genCacheNewAccTokenRevokeOlds
   │                                │                                │  6. Set-Cookie: new REFRESH_TOKEN
   │                                │                                │
   │  ← { accessToken, exp }        │                                │
   │◄───────────────────────────────│                                │
   │                                │                                │
   │  Retry original request        │                                │
```

### Frontend 401 Recovery (`axiosRequest.ts`)

```typescript
// Only one refresh at a time (isRefreshing flag)
const deviceFp = await generateFingerprint()
const res = await fetch(`/auth/refresh?deviceFp=${deviceFp}`, {
  method: 'POST',
  credentials: 'include'  // sends REFRESH_TOKEN cookie
})
// On success: update authStore + accountPoolStore
// On failure: removeToken + redirect to /login
```

---

## 6. Logout Flow

### User & Admin Logout

Both endpoints use the same DTO `LogoutRequestBody` (`{ "deviceFp": "<fp>" }`) with `Content-Type: application/json`.

```
Frontend                         Gateway                        User Service
   │                                │                                │
   │ POST /api/user/security/logout │                                │
   │ body: { "deviceFp": "<fp>" }   │                                │
   │ Cookie: REFRESH_TOKEN=<rt>     │                                │
   ├─────────────────────────────► │                                │
   │                                │ JWT auth? Yes → verify AT      │
   │                                │ Set X-User-Uid header          │
   │                                ├──────────────────────────────► │
   │                                │                                │
   │                                │                                │ loginService.logout(rt, dfp)
   │                                │                                │  1. if blank(rt) → REAUTHORIZATION_REQUIRED
   │                                │                                │  2. validateRefreshToken(uid, rt, dfp)
   │                                │                                │  3. removeAccessToken(uid, did)
   │                                │                                │     → DEL user:{uid}:device:{did}:access
   │                                │                                │  4. removeRefreshToken(uid, dfp, rt)
   │                                │                                │     → calculaateDid → get rt_family
   │                                │                                │     → DEL ref key
   │                                │                                │
   │                                │                                │ CookieUtil.cleanTokenCookie()
   │                                │                                │   Set-Cookie: REFRESH_TOKEN=; Max-Age=0; Path=/
   │                                │                                │   Set-Cookie: REFRESH_TOKEN=; Max-Age=0; Path=/api/auth
   │                                │                                │
   │  ← 200                         │                                │
```

> **⚠️ History**: Previously the user endpoint received a raw string body (`<deviceFp>`) without a wrapping JSON object. This caused Spring Boot to parse it with the wrong `Content-Type` (default axios POST emits `application/x-www-form-urlencoded`), producing a `deviceFp` value different from login's. Fixed by using `{ "deviceFp": "<fp>" }` JSON body with `LogoutRequestBody` DTO — same as admin always did.

---

## 7. Cookie Management

### Cookie Attributes

| Property | Value |
|----------|-------|
| Name | `REFRESH_TOKEN` |
| HttpOnly | true |
| Secure | false (dev; `COOKIE_SECURE=false`) |
| SameSite | `Strict` |
| Path | `/` |
| Max-Age | 604800s (7d) |

### Migration Detail

Previously the cookie path was `/api/auth`. To avoid `getCookieValue()` reading a stale cookie, **both** `setRefreshTokenCookie()` and `cleanTokenCookie()` emit an additional `Set-Cookie` with `Path=/api/auth; Max-Age=0` to expire the old-path cookie.

### Cookie Reading

```java
// CookieUtil.getCookieValue() uses .findFirst()
// When duplicate cookies exist (old path + new path),
// the first one is returned — can be the stale one
// Migration fix: expire old /api/auth cookie on every set/clean
```

---

## 8. Error Codes

| Code | Message Key | Thrown When |
|------|-------------|-------------|
| `AuthCode.REAUTHORIZATION_REQUIRED` | `auth.reauthorization.required` | Refresh token cookie missing/blank; login ban |
| `BaseResponseCode.REAUTHENTICATE_REQUIRED` | `auth.reauthenticate.required` | RT family not found in Redis (expired or device mismatch); RT value not found in Redis |
| `AuthCode.CAPTCHA_INVALID` | `auth.captcha.incorrect` | Captcha verification failed |
| `AuthCode.USERNAME_OR_PASSWORD_INCORRECT` | `auth.credentials_incorrect` | Wrong username/password (or not found) |
| `AuthCode.TOKEN_EXPIRED` | `auth.token_expired` | Access token JWT expired |
| `AuthCode.TOKEN_INVALID` | `auth.token_invalid` | Access token JWT signature invalid |

### Error Mapping Summary

| Scenario | Error |
|----------|-------|
| RT cookie missing at refresh/logout | `REAUTHORIZATION_REQUIRED` |
| RT family expired/no family in Redis | `REAUTHENTICATE_REQUIRED` |
| RT value not in Redis (but family exists) | `REAUTHENTICATE_REQUIRED` |
| AT expired at Gateway | `TOKEN_EXPIRED` |
| AT jti in Redis doesn't match claim | `TOKEN_INVALID` (JwtException → 401) |
| Login ban active | `REAUTHORIZATION_REQUIRED` (in `checkLoginBan`) |

---

## 9. Security Architecture Notes

- **Access Token**: RSA-signed JWT, returned in response body only (never in cookie). Claims: `sub`=uid, `jti`=UUID, `did`=deviceId.
- **Refresh Token**: Plain UUID, stored as HTTP-only cookie + Redis. One rotation per 7 days, family lifetime 30 days.
- **Device Binding**: AT's `did` claim + RT family key both use the computed device hash. Logout requires matching `dfp`.
- **Gateway JWT Filter**: All `/api/user/**` and `/api/admin/**` POST endpoints require valid AT in `Authorization: Bearer` header. The `AuthGlobalFilter` also extracts uid from AT claims and sets `X-User-Uid` for downstream services.
- **Whitelisted Endpoints** (no JWT required): `/api/auth/**`, `/api/admin/auth/captcha`.
- **Refresh uses Cookie auth only**: The `/api/auth/refresh` endpoint reads `REFRESH_TOKEN` from cookie; the Gateway's `AuthGlobalFilter` also extracts uid from the cookie for `X-User-Uid`.

---

## 10. Sequence Diagrams

### Login (Password) — Full Trace

```
Frontend                          User Service                         Redis
   │                                  │                                  │
   │  POST /login-by-password          │                                  │
   │  {username, password, captcha,    │                                  │
   │   deviceFp, deviceInfo}           │                                  │
   │─────────────────────────────────►│                                  │
   │                                  │  GET CAPTCHA_KEY cookie          │
   │                                  │  verifyCaptcha()                 │
   │                                  │  BCrypt.matches()                │
   │                                  │  checkLoginBan()                 │
   │                                  │  onlineUserService.update()      │
   │                                  │  auditLog: LOGIN                 │
   │                                  │                                  │
   │                                  │  createNewTokens(uid, dfp)       │
   │                                  │                                  │
   │                                  │  calculaateDid(uid, dfp) = did   │
   │                                  ├─────────────────────────────────►│
   │                                  │  SADD user:{uid}:devices did     │
   │                                  │◄─────────────────────────────────│
   │                                  │                                  │
   │                                  │  genCacheNewAccTokenRevokeOlds   │
   │                                  │  → JWT {sub, jti, did}          │
   │                                  ├─────────────────────────────────►│
   │                                  │  SET user:{uid}:device:{did}:    │
   │                                  │    access → jti (30d)           │
   │                                  │◄─────────────────────────────────│
   │                                  │                                  │
   │                                  │  genAndCacheRefToken             │
   │                                  ├─────────────────────────────────►│
   │                                  │  GET rt_family → null           │
   │                                  │  → family = UUID                │
   │                                  │  SADD user:{uid}:rt_families     │
   │                                  │  SET rt_family (30d)            │
   │                                  │  SET ref (7d) = UUID            │
   │                                  │◄─────────────────────────────────│
   │                                  │                                  │
   │  ← 200 {accessToken, exp}        │                                  │
   │  Set-Cookie: REFRESH_TOKEN=...   │                                  │
   │◄─────────────────────────────────│                                  │
```

### Token Refresh — Full Trace

```
Frontend                          User Service                         Redis
   │                                  │                                  │
   │  POST /auth/refresh?deviceFp=...  │                                  │
   │  Cookie: REFRESH_TOKEN=<rt>       │                                  │
   │─────────────────────────────────►│                                  │
   │                                  │  CookieUtil.getCookieValue()     │
   │                                  │  → rt                           │
   │                                  │                                  │
   │                                  │  UserCtxHolder.getUserId()       │
   │                                  │  (pulled from cookie by          │
   │                                  │   Gateway AuthGlobalFilter)      │
   │                                  │                                  │
   │                                  │  validateRefreshToken(uid,rt,dfp)│
   │                                  │  → calculaateDid(uid, dfp)       │
   │                                  ├─────────────────────────────────►│
   │                                  │  GET rt_family → familyId       │
   │                                  │◄─────────────────────────────────│
   │                                  │  if null → REAUTHENTICATE_REQUIRED│
   │                                  │                                  │
   │                                  │  isNewDeviceDid(uid, did)        │
   │                                  ├─────────────────────────────────►│
   │                                  │  SISMEMBER user:{uid}:devices    │
   │                                  │◄─────────────────────────────────│
   │                                  │  if new → log (TODO: risk ctrl) │
   │                                  │                                  │
   │                                  │  GET ref by family → rt_value   │
   │                                  ├─────────────────────────────────►│
   │                                  │◄─────────────────────────────────│
   │                                  │  if null → REAUTHENTICATE_REQUIRED│
   │                                  │                                  │
   │                                  │  genAndCacheRefToken (rotate)    │
   │                                  │  → DEL old ref                  │
   │                                  │  → SET new ref (7d)             │
   │                                  ├─────────────────────────────────►│
   │                                  │◄─────────────────────────────────│
   │                                  │                                  │
   │                                  │  genCacheNewAccTokenRevokeOlds   │
   │                                  │  → SET new access jti (30d)     │
   │                                  ├─────────────────────────────────►│
   │                                  │◄─────────────────────────────────│
   │                                  │                                  │
   │  ← 200 {accessToken, exp}        │                                  │
   │  Set-Cookie: REFRESH_TOKEN=<new> │                                  │
   │◄─────────────────────────────────│                                  │
```

### Logout — Full Trace

```
Frontend                          User Service                         Redis
   │                                  │                                  │
   │  POST /user/security/logout       │                                  │
   │  body: { "deviceFp": "<fp>" }    │                                  │
   │  Cookie: REFRESH_TOKEN=<rt>       │                                  │
   │─────────────────────────────────►│                                  │
   │                                  │  CookieUtil.getCookieValue()     │
   │                                  │  → rt                           │
   │                                  │                                  │
   │                                  │  StringUtil.isBlank(rt)          │
   │                                  │  → if true: REAUTHORIZATION_REQUIRED│
   │                                  │                                  │
   │                                  │  UserCtxHolder.getUserUid()      │
   │                                  │                                  │
   │                                  │  validateRefreshToken(uid,rt,dfp)│
   │                                  │  → calculaateDid → get familyId  │
   │                                  ├─────────────────────────────────►│
   │                                  │  GET rt_family, GET ref         │
   │                                  │◄─────────────────────────────────│
   │                                  │                                  │
   │                                  │  removeAccessToken(uid, did)     │
   │                                  ├─────────────────────────────────►│
   │                                  │  DEL user:{uid}:device:{did}:    │
   │                                  │    access                       │
   │                                  │◄─────────────────────────────────│
   │                                  │                                  │
   │                                  │  removeRefreshToken(uid, dfp, rt)│
   │                                  │  → DEL ref key                  │
   │                                  ├─────────────────────────────────►│
   │                                  │◄─────────────────────────────────│
   │                                  │                                  │
   │                                  │  CookieUtil.cleanTokenCookie()   │
   │                                  │  → Set-Cookie: REFRESH_TOKEN=;  │
   │                                  │    Max-Age=0; Path=/            │
   │                                  │  → Set-Cookie: REFRESH_TOKEN=;  │
   │                                  │    Max-Age=0; Path=/api/auth    │
   │                                  │                                  │
   │  ← 200                           │                                  │
   │◄─────────────────────────────────│                                  │
```
