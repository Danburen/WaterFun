# WaterFun 技术架构文档

## 项目概述

WaterFun 是一个功能完善的社区论坛平台，采用微服务架构设计。系统分为客户端（用户论坛）、管理后台（Admin）、以及后端 API 服务层。统一网关作为信任边界，所有请求经由 Gateway 鉴权后转发到下游服务。

## 目录结构

```
WaterFun/
├── waterfun-web-client/        # 用户论坛前端 (Nuxt 4 + Vue 3)
├── waterfun-admin/             # 管理后台 (Vue 3 + Vite)
├── waterfun-web-core/          # 前端共享类型/工具
├── waterfun-gateway/           # API 网关 (Spring Cloud Gateway)
├── waterfun-service/           # 主业务服务（含通知功能）
├── waterfun-admin-service/     # 后台管理服务
├── waterfun-service-core/      # 核心业务组件库
├── waterfun-common-lib/        # 公共基础库
├── sqls/                       # 数据库脚本
├── deploy/                     # 部署配置（docker/config/bin/keys/utils）
├── docs/                       # 项目文档
├── .ai/                        # AI 知识库
```

---

## 后端技术栈详解

### 1. 核心框架

| 组件 | 版本 | 用途 |
|------|------|------|
| Java | 22 | 运行时 |
| Spring Boot | 4.0.1 / 4.0.3 | 应用框架 |
| Spring Cloud | 2025.1.0 | 云原生框架 |
| Gradle | - | 构建工具 |

### 2. 数据库层

#### MySQL 8.0
- **ORM**: Spring Data JPA + Hibernate
- **连接池**: HikariCP（Spring Boot 默认）
- **会话存储**: Spring Session + Redis（分布式会话）
- **特性**：
  - 实体注解使用 `@Entity`, `@Table`, `@Column`
  - MapStruct 用于 DTO/Entity 映射
  - 自定义 SQL 查询通过 MyBatis（管理后台部分场景）
  - JPA Specification 支持动态查询（Ticket 多条件筛选）

```java
// 典型 Entity 定义
@Entity
@Table(name = "inbox_system")
public class InboxSystem {
    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "notice_type", columnDefinition = "tinyint UNSIGNED")
    private Integer noticeType;

    @Column(name = "is_read", nullable = false)
    @ColumnDefault("0")
    private Boolean isRead;
}
```

#### Redis
- **用途**：
  1. **分布式会话存储**：`spring-session-data-redis`
  2. **Token 缓存**：Access Token 黑名单/白名单
  3. **验证码缓存**：短信/邮箱验证码
  4. **限流计数器**：滑动窗口算法
  5. **分布式锁**：设备指纹清理

```java
// RedisHelper 封装 (waterfun-service-core)
public class RedisHelper implements RedisHelperHolder {
    private final StringRedisTemplate redisTemplate;

    public String get(String key);
    public void set(String key, String value);
    public String getAndDel(String key);
    public Long incr(String key);
    public Boolean setIfAbsent(String key, String value, Duration expire);
}
```

### 3. 缓存架构

#### 三层缓存设计

```
L1: 本地缓存 (Caffeine)
    Gateway 层 / 热点数据 / JWT 解码缓存
L2: Redis 缓存
    Session / Token / 验证码 / 限流计数
L3: 数据库
    持久化存储
```

#### Caffeine 配置 (Gateway)

```java
// waterfun-gateway/build.gradle
implementation 'com.github.ben-manes.caffeine:caffeine:3.2.3'
```

### 4. 消息队列

#### RabbitMQ
- **用途**：异步任务分发、内容审核通知、工单结果通知
- **交换机类型**：Direct Exchange
- **队列设计**：
  - `moderation.notification` — 审核任务队列
  - `moderation.notification.dlq` — 死信队列
  - `QUEUE_TICKET_NOTIFICATION` — 工单通知队列

```java
// RabbitConfig
public DirectExchange moderationExchange() {
    return new DirectExchange("moderation.exchange");
}

public Queue moderationNotificationQueue() {
    return QueueBuilder.durable("moderation.notification")
        .withArgument("x-dead-letter-exchange", "moderation.exchange.dlx")
        .withArgument("x-dead-letter-routing-key", "moderation.notification.dlq")
        .build();
}

// 消息发送
rabbitTemplate.convertAndSend(
    RabbitConstants.MODERATION_EXCHANGE,
    "moderation.notification",
    message
);
```

### 5. 云存储服务

#### 腾讯云 COS
- **SDK**：`com.qcloud:cos-java-client:5.6.133`
- **特性**：
  - 临时预签名 URL 上传（5 分钟过期）
  - 预签名 URL 下载（1 小时过期）
  - STS 临时凭证 (Security Token Service)
  - 文件类型检测（通过 Range 请求获取文件头）

```java
// TencentCosService 核心实现
public class TencentCosService implements CloudFileService {
    private final COSClient cosClient;

    // 生成上传预签名 URL
    public String generateUploadPresignedUrl(String key, MediaResourceType type) {
        // 1. 生成临时上传 token (STS)
        // 2. 生成预签名 URL (默认 5 分钟过期)
        // 3. 缓存 token 到 Redis
    }

    // 生成下载预签名 URL (默认 1 小时过期)
    public String generatePresignedUrl(String key, Duration expires) {
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, key);
        return cosClient.generatePresignedUrl(request).toString();
    }

    // 文件类型检测
    public String detectFileType(String key, long start, long end);
}
```

### 6. 短信服务

#### 阿里云短信
- **SDK**：`com.aliyun:dysmsapi20170525:4.1.1`
- **用途**：手机验证码

```java
// AliyunSmsService
public class AliyunSmsService {
    private final Client smsClient;

    @Value("${aliyun.sms.sign-name}")
    private String signName;

    @Value("${aliyun.sms.verify-code.template-name}")
    private String templateName;

    public void sendVerifyCode(String phoneNumber, String code) {
        SendSmsRequest request = new SendSmsRequest();
        request.setSignName(signName);
        request.setTemplateCode(templateName);
        request.setPhoneNumbers(phoneNumber);
        request.setTemplateParam("{\"code\":\"" + code + "\"}");
        smsClient.sendSms(request);
    }
}
```

### 7. 认证与授权

#### 双令牌 JWT + RSA
- **Access Token**：短期（30 天），包含用户身份信息
- **Refresh Token**：长期（7 天可续期，家族最大 30 天）
- **签名算法**：RS256 (RSA + SHA-256)
- **密钥管理**：RSA 私钥签名（Gateway 验证）

```java
// RsaJwtUtil (waterfun-service-core)
public class RsaJwtUtil {
    private final KeyPair keyPair;

    public TokenResult generateToken(Map<String, String> claims, Duration dur) {
        return Jwts.builder()
            .claims(claims)
            .issuedAt(new Date())
            .expiration(new Date(clock.millis() + dur.toMillis()))
            .signWith(keyPair.getPrivate(), Jwts.SIG.RS256)
            .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
            .verifyWith(keyPair.getPublic())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
```

#### Token 管理流程

```
登录流程
1. 用户登录（用户名/密码/验证码）
2. 生成 Device Fingerprint（设备指纹）
3. 生成 Access Token（短期）+ Refresh Token（长期）
4. 将 Refresh Token 写入 HttpOnly Cookie
5. 将 Access Token 返回（前端存储）

续期流程
1. Access Token 过期
2. 使用 Refresh Token 请求续期
3. 验证 Refresh Token（Redis + JWT）
4. 生成新的 Token 对（Rotate 机制）
5. 旧 Refresh Token 作废

登出流程
1. 删除 Redis 中 Refresh Token
2. 将 Access Token 加入黑名单
3. 清除 Cookie
```

#### OAuth 2.0 Resource Server
- Gateway 层使用 `spring-boot-starter-oauth2-resource-server` 验证 JWT
- 自定义 `RsaJwtDecoder` 实现 RS256 解码 + Redis JTI 校验
- 下游服务通过 Gateway 注入的 `X-User-*` 请求头获取用户上下文
- `spring-boot-starter-oauth2-client` / `authorization-server` 依赖存在但未启用（为第三方登录预留）

#### CSRF 防护
- **后端策略**：所有服务 `csrf.disable()`（防护依赖 Gateway JWT + CORS 白名单）
- **前端代码**：保留 CSRF Token 获取逻辑，向后兼容

### 8. API 网关

#### Spring Cloud Gateway
- **特性**：
  - 路由转发（动态路由）
  - 请求限流（认证端点限流 + 全局限流）
  - OAuth2 Resource Server JWT 校验 + Redis JTI
  - 全局异常处理（JSON 格式错误响应）

```yaml
# 路由配置 (application.yml)
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: admin-service
              uri: ${WATERFUN_ADMIN_SERVICE_URI:http://localhost:8082}
              predicates:
                - Path=/api/admin/**
            - id: user-service
              uri: ${WATERFUN_USER_SERVICE_URI:http://localhost:8081}
              predicates:
                - Path=/api/**
```

#### 限流实现

**认证端点限流（Caffeine 本地缓存）**：
```yaml
waterfun:
  rate-limit:
    auth:
      enabled: true
      paths:
        - /api/auth/login
        - /api/auth/register
        - /api/auth/refresh
      requests: 10
      window-seconds: 60
```

**全局限流（IP + 方法级别）**：
```yaml
waterfun:
  rate-limit:
    global:
      enabled: true
      get-limit: 1000
      write-limit: 300
      window-seconds: 60
```

### 9. 分布式会话

- **实现**：Spring Session + Redis
- **Session ID**：`SESSION` cookie
- **过期时间**：30 分钟
- **命名空间**：`session:`

### 10. 定时任务

```java
// ScheduleService
public class ScheduleService {

    @Scheduled(cron = "0 0 3 * *")
    public void cleanupExpiredDevices() {
        // 清理 90 天未活跃的设备记录
    }

    @Scheduled(cron = "0 0 3 * *")
    public void cleanupBlacklist() {
        // 清理过期的 Token 黑名单
    }
}
```

### 11. 监控与诊断

#### Spring Boot Actuator
- **端点**：`/actuator/health`, `/actuator/info`
- **健康检查**：Redis, MySQL, RabbitMQ
- **安全配置**：
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

---

## 前端技术栈

### 客户端 (waterfun-web-client)

| 技术 | 版本 | 用途 |
|------|------|------|
| Nuxt | ^4.4.8 | SSR 框架 |
| Vue | ^3.5.13 | UI 框架 |
| Pinia | - | 状态管理 |
| Element Plus | ^2.9.9 | UI 组件库 |
| @nuxt/i18n | ^9.5.3 | 国际化 |
| Sass | ^1.89.2 | 样式预处理 |
| dayjs | ^1.11.18 | 日期处理 |
| vee-validate | ^4.15.0 | 表单验证 |

### 管理后台 (waterfun-admin)

| 技术 | 版本 | 用途 |
|------|------|------|
| Vite | ^7.1.10 | 构建工具 |
| Vue | ^3.5.18 | UI 框架 |
| Pinia | ^3.0.3 | 状态管理 |
| Element Plus | ^2.11.4 | UI 组件库 |
| ECharts | ^6.0.0 | 数据可视化 |
| vue-echarts | ^8.0.1 | Vue ECharts 封装 |

### 公共模块 (waterfun-web-core)
- API 类型定义
- 错误处理：APIError
- 请求封装：Axios 实例（支持 BigInt JSON 解析）
- 响应类型：ApiRes, PromiseResBody

---

## 已实现功能

### 用户端功能

| 模块 | 功能 | 状态 |
|------|------|------|
| 认证 | 用户名密码/手机号/邮箱登录、注册、验证码、Token 刷新/续期、登出 | ✅ |
| 用户 | 用户资料查看/编辑、头像上传（预签名）、关注/粉丝、用户隐私设置 | ✅ |
| 内容 | 发帖（分类/标签）、帖子列表/详情、帖子编辑/删除、分类管理、标签管理、收藏/点赞 | ✅ |
| 评论 | 创建、删除、列表、点赞 | ✅ |
| 通知 | 系统通知、已读/批量已读、SSE 实时推送 | ✅ |
| 工单 | 内容举报、账号申诉、功能反馈、建议 | ✅ |
| 资源 | 腾讯云 COS 上传、预签名 URL 下载 | ✅ |

### 管理后台功能

| 模块 | 功能 | 状态 |
|------|------|------|
| 认证 | 管理员登录、Token 管理 | ✅ |
| 用户管理 | 用户列表、禁用/启用、资料管理 | ✅ |
| 内容管理 | 帖子管理、帖子审核、分类管理、标签管理 | ✅ |
| Banner | Banner 管理 | ✅ |
| 角色权限 | 角色 CRUD、权限 CRUD、菜单管理 | ✅ |
| 内容审核 | 审核任务、RabbitMQ 异步队列 | ✅ |
| 工单审核 | 举报/申诉/反馈/建议审核（含封禁操作） | ✅ |
| 封禁管理 | 创建封禁、解除封禁、状态查询 | ✅ |
| 仪表盘 | 数据统计、图表展示、JVM 监控、在线用户 | ✅ |

### 后端服务

| 功能 | 实现方式 |
|------|----------|
| REST API | Spring MVC |
| 分布式 Session | Spring Session + Redis |
| 分布式限流 | Redis 滑动窗口 |
| 本地限流 | Caffeine |
| 用户封禁 | AOP @BanCheck 注解 + BanCheckAspect |
| 内容审核 | RabbitMQ 异步队列 + ModerationCallbackStrategy |
| 工单系统 | 统一端点 + RabbitMQ 通知 |
| 验证码 | Redis TTL |
| 定时清理 | @Scheduled |
| IP 地理定位 | ip2region |

---

## 安全特性

1. **密码加密**：BCrypt（jbcrypt 0.4）
2. **JWT RSA 签名**：RS256，jjwt 0.12.x
3. **网关信任边界**：下游服务不暴露公网，仅信任 X-User-* 请求头
4. **限流保护**：认证端点限流（Caffeine）+ 全局限流（Redis 滑动窗口）
5. **CORS 白名单**：配置化域名白名单，非通配符
6. **Actuator 端点限制**：仅暴露 health/info，不显示详情
7. **设备指纹**：防止 Token 盗用
8. **AOP 封禁拦截**：@BanCheck 注解自动阻断被封禁用户操作

---

## 部署架构

```
                                    Nginx / CDN
                                        |
                                    Gateway :8080
                                   /              \
                          User Service :8081    Admin Service :8082
                              |        |              |        |
                           MySQL    Redis         RabbitMQ  Tencent COS
```

Gateway 是唯一公网入口，下游服务仅接受来自 Gateway 的请求（IP 白名单 + X-User-* 请求头验证）。

### 配置体系

采用两层配置结构，通过 `spring.config.import` 加载：

```
application.yml              ← 版本控制，各服务特有配置（端口/路由等）
    ├─ deploy/config/common.yml               ← 两层配置模板
    │   ├─ 基础设施（DB/Redis/RabbitMQ）: 带 :default 值
    │   └─ 三方/敏感（邮件/COS/SMS）: 纯 ${VAR}，无默认值
    └─ deploy/config/common-dev-secrets.yml   ← ❌ .gitignore
        三方接口/敏感凭据的开发默认值
```

**开发环境**：clone 项目 → 向同事获取 `common-dev-secrets.yml` → 直接启动，无需设置任何系统环境变量。

**生产环境**：所有 `${VAR}` 通过系统环境变量注入，`common-dev-secrets.yml` 不上线。

### 环境变量切换

```bash
# 开发环境（默认，零配置）
java -jar waterfun-service.jar

# 生产环境
DB_URL=jdbc:mysql://... DB_USERNAME=... ... java -jar waterfun-service.jar
```

完整环境变量清单参见 `deploy/env-vars-reference.md`。

---

## 依赖版本

- Java: 22
- Spring Boot: 4.0.1 / 4.0.3
- Spring Cloud: 2025.1.0
- Node.js: >= 20.19.0
- Nuxt: ^4.4.8
- Vue: ^3.5.x
- pnpm: 10.7.1

---

## 架构决策记录

### 通知服务不独立拆分

通知功能整合在 `waterfun-service` 中，使用：
- RabbitMQ 处理异步通知消息（审核结果、工单结果）
- SSE 连接实现客户端实时推送
- Inbox 实体存储系统通知

决策：暂不独立拆分。当前耦合度低、消息队列解耦已足够。

### 配置合并回 application.yml

原先 profile 文件（application-auth/notification/oss/security/quota.yml）分散多地且被 .gitignore 排除，新成员 clone 后无法获取配置。已合并到各模块主 `application.yml`，移除 `spring.profiles.include`。

---

*Last Updated: 2026-06-24*
