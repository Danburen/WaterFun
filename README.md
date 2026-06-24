# WaterFun — 多元化互动论坛社区

<div align="center">

**A feature-rich community forum platform with gateway-centric microservice architecture.**

[![Java 22](https://img.shields.io/badge/Java_22-ED8B00?logo=openjdk&logoColor=white)]()
[![Spring Boot 4.0](https://img.shields.io/badge/Spring_Boot_4.0-6DB33F?logo=springboot&logoColor=white)]()
[![Spring Cloud 2025](https://img.shields.io/badge/Spring_Cloud_2025-6DB33F?logo=spring&logoColor=white)]()
[![Vue 3](https://img.shields.io/badge/Vue_3-4FC08D?logo=vuedotjs&logoColor=white)]()
[![Nuxt 4](https://img.shields.io/badge/Nuxt_4-00DC82?logo=nuxt&logoColor=white)]()
[![MySQL](https://img.shields.io/badge/MySQL-4479A1?logo=mysql&logoColor=white)]()
[![Redis](https://img.shields.io/badge/Redis-FF4438?logo=redis&logoColor=white)]()

</div>

---

## Architecture Advantages

| Design Decision | Benefit |
|----------------|---------|
| **Gateway as trust boundary** | Only Gateway is public-facing. Downstream services (user-service, admin-service) never expose ports externally. Authentication, rate limiting, CORS all centralized in Gateway. User context propagated via `X-User-*` headers. |
| **2-layer config system** | Development requires zero OS environment variables. Infra defaults embedded in `common.yml` (`:default`), third-party secrets in gitignored `common-dev-secrets.yml`. Production uses env var injection only. |
| **Dual-token JWT (RS256) + Redis rotation** | Short-lived Access Token + long-lived Refresh Token with rotation family. RS256 asymmetric signing, Redis JTI lookup for revocation. Device fingerprinting prevents token theft. |
| **3-tier cache** | Caffeine (L1, hot data in Gateway) → Redis (L2, session/token/captcha) → MySQL (L3, persistence). Optimized for read-heavy community workloads. |
| **AOP-based ban enforcement** | `@BanCheck` annotation intercepts any service method. 6 ban types (login/post/comment/upload/chat/create), automatic extension logic, permanent-override semantics. |
| **Unified ticket system + async MQ** | Single `POST /api/tickets` endpoint for report/appeal/feedback/suggestion. RabbitMQ decouples submission from review; admin review triggers penalty application and real-time notification via SSE. |
| **Multi-module monorepo** | 5 Gradle modules + 3 pnpm workspaces. Shared core (`waterfun-service-core`) reduces duplication; common lib (`waterfun-common-lib`) has zero Spring dependency for cross-module reuse. |

---

## Features

| Area | Capabilities |
|------|-------------|
| **Authentication** | Password / SMS / Email login & register, dual-token (Access+Refresh), RS256 JWT, device fingerprint, CSRF Token |
| **Account Security** | Set/reset password, bind/modify email & phone, captcha verification |
| **User Profiles** | Profile edit, avatar upload (COS presigned URL + callback confirm), follower system, privacy settings |
| **Content** | Post CRUD, paginated lists with category/tag filters, tags, comments, likes, bookmarks |
| **Notifications** | System inbox, unread count, batch read, SSE real-time push, RabbitMQ async delivery |
| **Ticket System** | Content report, account appeal, feedback, suggestion — unified endpoint, MQ decoupled |
| **Content Moderation** | Post/image/text audit workflow, batch operations, audit history, RabbitMQ callback strategies |
| **Ban System** | 6 penalty types, AOP `@BanCheck` enforcement, auto-extension, permanent override, lift management |
| **Admin Dashboard** | User/role/permission management, site statistics, JVM monitoring, online users, ECharts visualization |
| **Object Storage** | Tencent COS presigned upload/download, STS temporary credentials, file type detection, callback confirmation |
| **Rate Limiting** | Auth endpoint rate limit (10 req/60s), global IP-based rate limit (1000 GET / 300 write per 60s) |

---

## Tech Stack

| Category | Technology |
|----------|------------|
| Framework | Spring Boot 4.0.x, Spring Cloud Gateway 2025.1.0 |
| Language | Java 22, TypeScript 5.9 |
| Auth | Spring Security, OAuth2 Resource Server, RS256 JWT (jjwt 0.12.x), BCrypt |
| ORM | JPA / Hibernate + MyBatis 3.0.3 |
| Cache | Caffeine 3.2.3 (L1) + Redis (L2) + MySQL (L3) |
| Queue | RabbitMQ (moderation + ticket notifications) |
| Storage | Tencent COS (presigned URLs) |
| SMS | Aliyun SMS (dysmsapi) |
| Email | Resend (resend-java), Spring Mail (SMTP) |
| Frontend | Nuxt 4 + Vue 3 (Web Client), Vue 3 + Vite 7 (Admin) |
| UI Library | Element Plus, ECharts 6 |
| Mapping | MapStruct 1.6.3 (Java), Axios 1.13.5 (HTTP) |

---

## Getting Started

### Prerequisites

- Java 22, Node.js >= 20.19.0, pnpm 10.7.1
- MySQL + Redis (required), RabbitMQ (optional, for moderation)

### Backend

```bash
gradlew.bat build
gradlew.bat :waterfun-gateway:bootRun
gradlew.bat :waterfun-service:bootRun
gradlew.bat :waterfun-admin-service:bootRun
```

### Frontend

```bash
pnpm install
pnpm adev        # Admin (port 5173)
pnpm cdev        # Web Client (port 3000)
```

### Configuration

The project uses a 2-layer config system. **No OS environment variables needed for development.**

```
deploy/config/common.yml                 # Infra defaults embedded (:default)
deploy/config/common-dev-secrets.yml     # Third-party secrets (gitignored, request from teammate)
```

---

## Project Structure

```
waterfun-gateway          API Gateway (Spring Cloud Gateway, port 8080)
waterfun-service          User-Facing Business API (port 8081, incl. notification)
waterfun-admin-service    Admin API (port 8082)
waterfun-service-core     Shared Business Logic (JPA, services, AOP aspects)
waterfun-common-lib       Shared Utilities (zero Spring dependency)
waterfun-admin            Admin Dashboard (Vue 3 + Vite + Element Plus)
waterfun-web-client       User Forum (Nuxt 4 + Vue 3 + Element Plus)
waterfun-web-core         Shared Frontend Library (TS types, utilities)
sqls/                     Database scripts
deploy/                   Docker, config, scripts, keys
```

---

## License

MPL-2.0. See `LICENSE` for details.
