# WaterFun - A new era diversified interactive forum

WaterFun 是一个论坛 / 社区平台项目，采用 **前后端分离 + 网关** 的混合型单体仓库（**Gradle 多模块** + **pnpm workspace**）。仓库内包含用户侧论坛（Web Client）、后台管理（Admin）以及后端 API 服务（Gateway + Services）。

---

## ✨ Features

- **统一网关作为信任边界**：鉴权在 Gateway 完成，下游服务专注业务（网关注入 `X-User-*` 上下文）。
- **安全链路完整**：双令牌（Access/Refresh）+ **RSA JWT (RS256)**，并结合 Redis 做 token/jti 维护。
- **缓存分层**：本地缓存（Caffeine）+ Redis + MySQL 的常见三层思路，适合热点数据与会话/验证码等场景。
- **异步化审核/通知**：RabbitMQ 队列化处理 moderation 等任务。
- **对象存储能力完善**：Tencent COS 预签名上传/下载、STS 临时凭证等。
- **工程化统一规范**：统一响应封装（`ApiResponse<T>` / `ErrorResponse`），核心逻辑下沉到 `waterfun-service-core`。

---

## ✅ Implemented Features

### 用户侧（Forum / User）
- 认证：账号密码 / 短信 / 邮箱验证码登录、注册、刷新 Token、登出、CSRF Token
- 账号安全：设置/重置密码；绑定/激活/修改邮箱/手机号
- 用户资料：基础信息、资料编辑、头像上传（预签名 URL + 回调确认）
- 内容：发帖、帖子列表/详情（分页 + 分类/标签过滤）、标签管理
- 通知：系统通知列表、未读数、批量已读/全部已读
- 资源：COS 上传、预签名下载

### 后台管理（Admin）
- 管理员认证
- 用户管理、角色/权限/菜单
- 分类/标签、帖子管理与审核（moderation）
- Banner 管理、仪表盘统计与图表

### 网关（Gateway）
- 统一 API 入口、JWT 校验、公共白名单
- 基础限流、`X-Request-Id` 追踪

> 以 `docs/api_docs/*.json` 与 `reference/*.openapi.json` 为准。

---

## 🧱 Monorepo Structure

```
/waterfun-gateway          # 统一网关 (Spring Cloud Gateway)
/waterfun-service          # 用户侧核心业务 API
/waterfun-admin-service    # 后台管理 API
/waterfun-notify-service   # 通知服务
/waterfun-service-core     # 共享业务 / 领域能力
/waterfun-common-lib       # 通用响应 / 错误 / 工具
/waterfun-admin            # 管理后台前端 (Vue 3 + Vite)
/waterfun-web-client       # 用户侧前端 (Nuxt 3)
/waterfun-web-core         # 前端共享类型/工具
/sqls                      # 数据库脚本
/docs                      # 文档
/reference                 # OpenAPI 参考
```

---

## 🧰 Tech Stack (Short)

- Backend: **Java 22**, **Spring Boot 4.x**, Spring Cloud Gateway, Spring Security/OAuth2, JPA/Hibernate + MyBatis, Redis + Spring Session, MySQL, RabbitMQ, Caffeine
- Frontend: **Nuxt 3 (Web Client)**, **Vue 3 + Vite (Admin)**, Pinia, Element Plus, Axios
- Integrations: Tencent COS, Aliyun SMS, Resend Email

---

## 🚀 Local Development (Quick Start)

### Prerequisites
- Java 22
- Node.js `>= 20.19.0` + pnpm `10.7.1`
- MySQL + Redis（必需）
- RabbitMQ（需要审核/通知链路时）

### Backend (Windows)
```bash
# build all
gradlew.bat build

# run modules
gradlew.bat :waterfun-gateway:bootRun
gradlew.bat :waterfun-service:bootRun
gradlew.bat :waterfun-admin-service:bootRun
gradlew.bat :waterfun-notify-service:bootRun
```

### Frontend
```bash
pnpm install

# admin
pnpm adev

# web client
pnpm cdev
```

---

## 📚 Docs / Config

- 技术架构与细节：`docs/README_TECHNOLOGY.md`
- API 文档：`docs/api_docs/*.json`
- 参考 OpenAPI：`reference/*.openapi.json`

---

## 📄 License

This project is licensed under **MPL-2.0**. See `LICENSE` for details.