# WaterFun 上线评估报告

> 生成日期：2026-06-24（上次更新：2026-06-22）
> 基于项目代码全面分析，参考 `.ai/` 知识库及实际源码。
> 本次更新：配置体系重构（3层→2层）后同步，标记已修复项，更新文件路径。

---

## 总体结论：功能就绪，安全与运维加固后即可上线

项目功能完整度高、架构设计清晰，配置体系已有生产就绪设计（两层配置拆分 + 环境变量注入）。
当前是 **开发阶段**，上线前需处理剩余安全配置项和运维就绪度问题。

---

## 一、项目概况

| 维度 | 数据 |
|------|------|
| 后端 Java 源文件 | ~700+ |
| 前端 TS/Vue 源文件 | ~200+ |
| Gradle 模块 | 5 个（common-lib, service-core, service, admin-service, gateway） |
| pnpm workspace | 3 个（admin, web-client, web-core） |
| 数据库脚本 | 7 个（建表、初始化、统计等） |
| 测试文件 | 6 个（覆盖极少） |

---

## 二、功能完整度矩阵

### 已完成

| 领域 | 模块 | 状态 |
|------|------|------|
| 用户认证 | 密码/短信/邮箱登录、注册、双令牌(Access+Refresh)、RSA JWT(RS256) | ✅ |
| 用户资料 | 基本信息编辑、头像COS预签名上传、公开资料 | ✅ |
| 账号安全 | 设置/重置/修改密码、绑定/修改邮箱/手机号 | ✅ |
| 帖子系统 | CRUD、列表(分页+筛选)、详情、分类、标签、点赞/收藏 | ✅ |
| 评论系统 | 创建/删除/列表/点赞 | ✅ |
| 通知系统 | 系统通知、未读数、批量已读、SSE实时推送 | ✅（已整合在user-service） |
| 资源上传 | COS预签名上传/下载、STS临时凭证、回调确认 | ✅ |
| 后台管理 | 用户/角色/权限/帖子/分类/标签/Banner管理 | ✅ |
| 内容审核 | 帖子/图片/文字审核、批量操作、审核历史 | ✅ |
| 工单系统 | 举报/申诉/反馈/建议统一Ticket端点 | ✅ |
| 封禁系统 | 6种封禁类型(BAN_LOGIN/POST/COMMENT/UPLOAD/CHAT/CREATE)、AOP @BanCheck 拦截、自动延期逻辑、永久封禁 | ✅ |
| 仪表盘/统计 | 站点概况、趋势图、在线用户、JVM监控 | ✅ |
| 缓存体系 | Caffeine本地+Redis分布式+MySQL 三层缓存 | ✅ |
| 异步消息 | RabbitMQ审核回调、工单通知 | ✅ |
| IP地理定位 | ip2region 集成 | ✅ |
| 多语言 | 中英文i18n（web-client + admin-service） | ✅ |

### 未实现 / 延期（计划上线后迭代）

| 功能 | 计划 | 说明 |
|------|------|------|
| 全文搜索 | 上线后 | Elasticsearch / MeiliSearch |
| 第三方OAuth登录 | 上线后 | 微信/GitHub/Google（依赖已引入，未启用） |
| 站内私信/聊天 | 上线后 | 已有的 BAN_CHAT 封禁类型预留 |
| 用户关注流/时间线 | 上线后 | UserFollower表已有，缺Feed |
| 数据导出/账号注销 | 上线后 | GDPR合规 |
| 操作审计日志增强 | 上线后 | 现有AccountAuditLog可扩展 |
| CDN配置 | 上线后 | COS已有，加CDN加速 |
| APM/错误监控 | 上线后 | Sentry等 |
| 压力测试 | 上线后 | JMeter/k6脚本 |

---

## 三、上线前必须修复（高优先级）

### 1. 私钥提交到源码（极高危）

**位置**：
- `waterfun-service/src/main/resources/keys/private.key`
- `waterfun-admin-service/src/main/resources/keys/private.key`

**风险**：RSA私钥硬编码在源码目录中。任何人拿到仓库即可签发合法JWT Token，冒充任意用户。

**当前保护措施**：`.gitignore` 已通过 `*.key` 模式排除密钥文件，故密钥不会进入 Git 历史。
但源码目录中仍存在明文私钥，开发环境可接受，上线前必须替换。

**上线前操作**：
1. **运行 `deploy/bin/gen-keys.ps1` 生成新的生产环境 RSA 密钥对**
2. 将私钥部署到生产服务器 `deploy/keys/private.key`
3. 公钥可随部署包分发（无风险）
4. 生产环境通过 `JWT_PRIVATE_KEY` 环境变量指定密钥路径，如 `JWT_PRIVATE_KEY=file:/etc/waterfun/keys/private.key`
5. 源码中保留 `private.key` 仅用于本地开发，不对外造成影响

### 2. 凭据保护（中高危）

**位置**：`deploy/config/common-dev-secrets.yml`

**内容**：QQ邮箱SMTP授权码、COS Bucket名、阿里云短信签名和模板、Resend API Key等。

**当前保护**：`.gitignore` 已排除 `common-dev-secrets.yml`，不会提交到 Git。

**上线前操作**：
1. 生产服务器**不部署** `common-dev-secrets.yml`
2. 所有 `${VAR}` 通过系统环境变量注入
3. 参考 `deploy/env-vars-reference.md` 在生产服务器设置约 30 个环境变量

### 3. CORS 域名白名单（低危）

**当前状态**：GatewaySecurityConfig 从 `waterfun.cors.allowed-origins` 读取白名单。

```yaml
waterfun:
  cors:
    allowed-origins: https://waterfun.top,http://localhost:3000,http://localhost:5173
```

**结论**：代码层面已完成白名单配置，非 `*`，有具体域名。上线前确认生产域名是否在列表中即可。

### 4. 下游服务安全加固（低危）

**当前状态**：两个下游服务的 SecurityConfig 使用 IP 白名单：
- 开发模式（`trusted-ips` 空值）：放行所有 IP
- 生产模式（设置 `gateway.trusted-ips`）：仅允许网关 IP 访问

**结论**：架构已完成，代码已就位。上线前只需在生产环境设置 `gateway.trusted-ips` + 网络层安全组限制。

### 5. 前端 `.env.production` 为占位符（阻塞）

**当前状态**：已使用 `YOUR_DOMAIN` 占位符。

```env
# admin/.env.production
VITE_API_BASE=//api.YOUR_DOMAIN/api/admin

# web-client/.env.production
VITE_API_BASE=//api.YOUR_DOMAIN/api
NUXT_PUBLIC_API_BASE=//api.YOUR_DOMAIN/
```

**上线前操作**：CI/CD 构建时替换 `YOUR_DOMAIN` 为真实域名。

### 6. 网关白名单路径零散（中危）

**当前状态**：GatewaySecurityConfig 中：
```java
.pathMatchers("/api/auth/**").permitAll()
.pathMatchers("/api/admin/auth/**").permitAll()
.pathMatchers("/api/posts/list", "/api/posts/list/detail", "/api/banners").permitAll()
```

**建议**：统一使用 `/api/public/**` 前缀，所有公开端点放在该路径下。

### 7. 网关全局限流宽松（低中危）

```yaml
get-limit: 1000
write-limit: 300
window-seconds: 60
```

5 req/s 对写操作来说已不算低。建议在上线后逐步调整，当前可接受。

---

## 四、已修复 / 已处理项

### 1. DEBUG 日志关闭（已修复）

`org.springframework.security` 从 `DEBUG` → `WARN`，认证细节不再打印到日志。

### 2. 配置文件合并（已处理）

原先 `application.yml` 通过 `spring.profiles.include` 引用多个 profile 文件（auth、notification、oss、security、quota），这些文件全部被 `.gitignore` 排除。已合并到主 `application.yml` 中，移除 `spring.profiles.include`，删除冗余 profile 文件。

### 3. JWT_PRIVATE_KEY 无默认回退（已修复）

`${JWT_PRIVATE_KEY:classpath:keys/private.key}` → `${JWT_PRIVATE_KEY}`，生产环境忘记设置变量将直接启动失败（双重保障：`@Value` 解析失败 + `@PostConstruct validateKeyConfig()`）。

### 4. 端口环境变量化（已修复）

| 服务 | 旧值 | 新值 |
|------|------|------|
| User Service | `server.port: 8081` | `${WATERFUN_SERVICE_PORT:8081}` |
| Admin Service | `server.port: 8082` | `${WATERFUN_ADMIN_SERVICE_PORT:8082}` |

### 5. Admin 前端 CSRF 激活（已修复）

`waterfun-admin/src/utils/axiosRequest.ts` 中 CSRF 逻辑已取消注释，与 web-client 保持一致。

### 6. Actuator 显式配置（已修复）

三个模块（gateway/user-service/admin-service）的 `application.yml` 均已显式配置：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: never
```

### 7. `.gitignore` 密钥保护验证（已确认）

| 模式 | 保护内容 | 是否有效 |
|------|---------|---------|
| `*.key` + `!*.key.example` | 密钥文件（private.key） | ✅ 排除 |
| `deploy/` | 部署配置目录 | ✅ 排除 |
| `*.env` + `!*.env.example` | 环境变量文件 | ✅ 排除 |
| `common-dev-secrets.yml` | 三方接口凭据 | ✅ 排除 |

---

## 五、上线前强烈建议修复（中优先级）

### 1. Admin SecurityConfig 标注 @Deprecated

`waterfun-admin-service` 的 SecurityConfig 标注了 `@Deprecated`，且类上无 `@EnableWebSecurity`（与 user-service 的 SecurityConfig 不一致）。建议确认是否有替换方案。

### 2. 无完整 Docker 化部署方案

`deploy/docker/` 已有 dev/prod 分离的编排文件：
- `docker-compose.yml`（开发，仅中间件）  
- `docker-compose.prod.yml`（生产模板，含 Java 服务容器）

但生产编排需按实际环境调整（Nginx、SSL、日志挂载等）。

### 3. 测试覆盖率极低

仅 6 个测试文件：
- `EnumMappingTest.java` / `ApiResponseTest.java` / `BanPermissionMappingTest.java`
- `ReportServiceImplTest.java` / `PenaltyServiceImplTest.java` / `TicketModerationServiceImplTest.java`
- `CreateUserReportReqValidationTest.java`

建议至少覆盖：用户注册→登录→发帖→审核→封禁核心流程。

### 4. 私钥路径环境变量启动校验（已修复）

已添加 `@PostConstruct validateKeyConfig()`，启动时校验私钥文件是否存在，不存在则抛出清晰错误。

---

## 六、可上线后迭代的功能

| 功能 | 预计复杂度 | 说明 |
|------|-----------|------|
| 全文搜索 | 高 | 集成 Elasticsearch / MeiliSearch |
| 第三方OAuth登录 | 中 | 微信 / GitHub / Google（依赖已引入） |
| 站内私信/聊天 | 高 | 已有BAN_CHAT预留，需WebSocket |
| 用户关注流/Feed | 中 | 已有UserFollower表，需聚合时间线 |
| 数据导出/账号注销 | 中 | GDPR合规 |
| 操作审计日志增强 | 低 | 现有审计表可扩展 |
| CDN加速 | 低 | COS源站 + CDN |
| APM集成 | 低 | Sentry / 阿里云ARMS |
| 压力测试 | 低 | JMeter / k6 脚本 |
| 细粒度限流（每用户/每端点） | 中 | 当前仅全局IP限流 |
| HSTS / CSP安全头 | 低 | 增强前端安全 |
| 邮箱模板美化 | 低 | Thymeleaf模板已集成 |

---

## 七、上线操作 Checklist

### Phase 1 — 上线前必须完成（阻塞）

- [ ] **替换 RSA 密钥对**：运行 `deploy/bin/gen-keys.ps1` 生成生产密钥
  - 当前 `waterfun-service` 和 `waterfun-admin-service` 的 `resources/keys/` 下仍有 dev 私钥
  - `.gitignore` 已排除 `*.key`，不会进入 Git
- [ ] **注入约 30 个生产环境变量**：参考 `deploy/env-vars-reference.md`
- [ ] **前端构建域名替换**：`.env.production` 的 `YOUR_DOMAIN` → 真实域名
- [ ] **网络层限制**：安全组仅允许外网访问网关 8080 端口，service 端口仅允许网关 IP
- [ ] **网关白名单路径统一**：当前白名单硬编码在 `GatewaySecurityConfig.java` 的 `pathMatchers()` 中，建议收束到 `/api/public/**` 前缀

#### 代码层面已完成（确认可工作）

- [x] **CORS 域名白名单**：`waterfun.cors.allowed-origins` 已配置具体域名（非 `*`），代码已从配置读取
- [x] **下游服务 IP 白名单架构**：`gatewayIpAuthorizationManager()` + `gateway.trusted-ips` 已实现
- [x] **MySQL 自动备份脚本**：`deploy/bin/backup_mysql.ps1` + `backup_mysql.sh` + `README.md` 已就位
- [x] **Actuator 端点**：已显式配置只暴露 `health,info`，`show-details: never`
- [x] **JWT_PRIVATE_KEY 无默认值**：已移除 `:default`，缺了直接启动失败
- [x] **端口环境变量化**：user-service(8081) 和 admin-service(8082) 已环境变量化
- [x] **Admin CSRF**：已激活，与 web-client 一致

### Phase 2 — 强烈建议完成

- [ ] **Admin SecurityConfig 清理**：去掉 `@Deprecated` + 确认 `@EnableWebSecurity`
- [ ] **完整 Docker Compose 编排**：生产编排需按实际环境调整
- [ ] **网关白名单统一前缀**：收束到 `/api/public/**`
- [ ] **核心流程冒烟测试**：覆盖注册→登录→发帖→审核→封禁流程

### Phase 3 — 上线后迭代

- [ ] 全文搜索（ES / MeiliSearch）
- [ ] 第三方 OAuth 登录（微信 / GitHub / Google）
- [ ] 站内私信（WebSocket，已预留 BAN_CHAT）
- [ ] 用户关注流/Feed（已有 UserFollower 表）
- [ ] 数据导出/账号注销（GDPR 合规）
- [ ] CDN + HSTS/CSP 安全头 + APM（Sentry）
- [ ] 压力测试（JMeter / k6）

---

## 八、代码核实发现（额外问题）

### 1. `admin-service` SecurityConfig 标注 @Deprecated

标注了 `@Deprecated`，且类上无 `@EnableWebSecurity`（与 user-service 的 SecurityConfig 不一致，user-service 有 `@EnableWebSecurity`）。建议确认是否有替换方案。

### 2. Gateway 路由配置格式

`spring.cloud.gateway.server.webflux.routes` 在 WebFlux 环境下正常工作，非 Bug（标准 Spring Cloud Gateway 使用 `spring.cloud.gateway.routes`，但在 WebFlux 环境下 `server.webflux` 嵌套也被支持）。

### 3. 服务间端口统一（已修复）

后端服务端口：
- 网关：`${WATERFUN_GATEWAY_PORT:8080}` ✅
- User Service：`${WATERFUN_SERVICE_PORT:8081}` ✅
- Admin Service：`${WATERFUN_ADMIN_SERVICE_PORT:8082}` ✅

---

## 九、配置体系说明

### 两层配置拆分

项目使用 `spring.config.import` 实现两层配置：

```
application.yml                   ← 版本控制，各服务特有配置（端口/路由等）
    ├─ deploy/config/common.yml   ← 统一配置模板
    │   ├─ 基础设施（DB/Redis/RabbitMQ）: 非三方带 :default 值
    │   └─ 三方接口/敏感配置: 纯 ${VAR} 引用，无默认值
    └─ deploy/config/common-dev-secrets.yml  ← ❌ .gitignore（三方凭据开发默认值）
```

- `application.yml`（各模块）：各服务特有配置，独立于共享模板
- `common.yml`：统一配置模板，基础设施配置带 `:default` 默认值，三方配置纯 `${VAR}`
- `common-dev-secrets.yml`：三方接口凭据的开发默认值（**.gitignore**，向同事获取）

**开发模式**：clone 项目 → 复制 `common-dev-secrets.yml` → 直接启动，无需设任何环境变量。
**生产模式**：所有 `${VAR}` 通过系统环境变量注入，`common-dev-secrets.yml` 不上线。

### 环境变量切换

```bash
# 开发环境（默认，零配置）
java -jar waterfun-service.jar

# 生产环境
DB_URL=jdbc:mysql://prod-host:3306/waterfun \
DB_USERNAME=prod_user \
DB_PASSWORD=prod_pass \
... \
java -jar waterfun-service.jar
```

---

## 十、架构决策记录

### 通知服务不独立拆分

通知功能整合在 `waterfun-service` 中，使用：
- RabbitMQ 处理异步通知消息（审核结果、工单结果）
- SSE 连接实现客户端实时推送
- Inbox 实体存储系统通知

决定：暂不独立拆分。当前耦合度低、消息队列解耦已足够。后续如果通知逻辑复杂度增加，再考虑拆分。

### 配置文件合并回 application.yml

原先 profile 文件分散多地且被 `.gitignore` 排除，新成员 clone 后无法获取配置。已合并且进入版本控制，开发体验一致，无需额外复制配置步骤。

### 配置体系从 3 层缩减为 2 层

原先三层（`common.yml` 纯模板 + `common-dev.yml` 开发值 + `common-dev-secrets.yml` 三方密钥）简化为两层：`common.yml` 统一带默认值 + `common-dev-secrets.yml` 仅存三方密钥。开发者无需设任何环境变量即可启动。

---

> 本报告基于项目代码和 `.ai/` 知识库分析生成。当前为 **2026-06-24** 开发阶段状态。
> 建议上线前逐项验证并更新状态。已完成的 Checklist 项可标记为 ✅。
