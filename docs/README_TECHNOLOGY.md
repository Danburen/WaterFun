# WaterFun 技术架构文档

## 项目概述

WaterFun 是一个功能完善的社区论坛平台，采用微服务架构设计。系统分为客户端（用户论坛）、管理后台（Admin）、以及后端 API 服务层。

## 目录结构

```
WaterFun/
├── waterfun-web-client/     # 用户论坛前端 (Nuxt 3)
├── waterfun-admin/          # 管理后台 (Vue 3 + Vite)
├── waterfun-gateway/       # API 网关 (Spring Cloud Gateway)
├── waterfun-service/        # 主业务服务
├── waterfun-admin-service/  # 管理后台服务
├── waterfun-service-core/   # 核心业务组件库
├── waterfun-common-lib/     # 公共基础库
├── sqls/                  # 数据库脚本
└── docs/                  # 文档
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
- **连接池**: HikariCP (Spring Boot 默认)
- **会话存储**: Spring Session + JDBC (支持分布式会话)
- **特性**:
  - 实体注解使用 `@Entity`, `@Table`, `@Column`
  - MapStruct 用于 DTO/Entity 映射
  - 自定义 SQL 查询通过 MyBatis (部分场景)

```java
// 典型 Entity 定义示例
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
- **版本**: 集成于 Spring Boot Starter
- **用途**:
  1. **分布式会话存储**: `spring-session-data-redis`
  2. **Token 缓存**: Access Token 黑名单/白名单
  3. **验证码缓存**: 短信/邮箱验证码
  4. **限流计数器**: 滑动窗口算法
  5. **分布式锁**: 设备指纹清理

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

#### 三级缓存设计

```
┌─────────────────────────────────────────────┐
│           L1: 本地缓存 (Caffeine)            │
│    Gateway 层 / 热点数据 / JWT 解码缓存       │
├─────────────────────────────────────────────┤
│           L2: Redis 缓存                     │
│   Session / Token / 验证码 / 限流计数      │
├─────────────────────────────────────────────┤
│           L3: 数据库                        │
│          持久化存储                         │
└─────────────────────────────────────────────┘
```

#### Caffeine 配置 (Gateway)

```java
// waterfun-gateway/build.gradle
implementation 'com.github.ben-manes.caffeine:caffeine:3.2.3'
```

### 4. 消息队列

#### RabbitMQ
- **用途**: 异步任务分发、内容审核通知
- **交换机类型**: Direct Exchange
- **队列设计**:
  - `moderation.notification` - 审核任务队列
  - `moderation.notification.dlq` - 死信队列

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
- **SDK**: `com.qcloud:cos-java-client`
- **特性**:
  - 临时预签名 URL 上传
  - 预签名 URL 下载 (带过期时间)
  - STS 临时凭证 (Security Token Service)
  - 分块上传支持
  - 文件类型检测

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
    
    // 文件类型检测 (通过 Range 请求获取文件头)
    public String detectFileType(String key, long start, long end);
}
```

**配置示例**:
```yaml
cloud:
  tencent:
    cos:
      bucket-name: waterfun-XXXXXXXX
      secret-id: xxxxxx
      secret-key: xxxxxx
      region: ap-guangzhou
      default-expires-seconds: 3600
      upload-expires-seconds: 300
      safety-marge-seconds: 300
```

### 6. 短信服务

#### 阿里云短信
- **SDK**: `com.aliyun:dysmsapi20170525`
- **用途**: 手机验证码、通知消息
- **配置**: 签名 + 模板 ID

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
- **Access Token**: 短期 (默认配置)，包含用户身份信息
- **Refresh Token**: 长期 (7天)，用于续期
- **签名算法**: RS256 (RSA + SHA-256)
- **密钥管理**: RSA 私钥签名，公钥验证

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
┌─────────────────────────────────────────────────────┐
│                    登录流程                          │
├─────────────────────────────────────────────────────┤
│  1. 用户登录 (用户名/密码/验证码)                     │
│  2. 生成 Device Fingerprint (设备指纹)              │
│  3. 生成 Access Token (短期) + Refresh Token (长期) │
│  4. 将 Refresh Token 写入 HttpOnly Cookie        │
│  5. 将 Access Token 存入 Redis 黑名单              │
│  6. 返回 Access Token (前端存储)                   │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│                  续期流程                           │
├─────────────────────────────────────────────────────┤
│  1. Access Token 过期                              │
│  2. 使用 Refresh Token 请求续期                     │
│  3. 验证 Refresh Token (Redis + JWT)              │
│  4. 生成新的 Token 对                               │
│  5. 旧 Refresh Token 作废                          │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│                  登出流程                           │
├─────────────────────────────────────────────────────┤
│  1. 删除 Redis 中 Refresh Token                      │
│  2. 将 Access Token 加入黑名单                      │
│  3. 清除 Cookie                                   │
└─────────────────────────────────────────────────────┘
```

#### OAuth 2.0
- **Resource Server**: Spring Security OAuth2 Resource Server
- **Client**: 支持第三方登录配置
- **Authorization Server**: Spring Authorization Server (已集成)

```java
// 安全配置
implementation 'org.springframework.boot:spring-boot-starter-oauth2-resource-server'
implementation 'org.springframework.boot:spring-boot-starter-oauth2-client'
implementation 'org.springframework.boot:spring-boot-starter-oauth2-authorization-server'
```

#### CSRF 防护
- **机制**: Spring Security CSRF Token
- **Cookie**: `XSRF-TOKEN`
- **Header**: `X-XSRF-TOKEN`

### 8. API 网关

#### Spring Cloud Gateway
- **特性**:
  - 路由转发 (动态路由)
  - 请求限流
  - 认证过滤器
  - 全局异常处理

```java
// 路由配置 (application.yml)
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: http://localhost:8081
          predicates:
            - Path=/api/user/**, /api/auth/**
        - id: admin-service
          uri: http://localhost:8082
          predicates:
            - Path=/api/admin/**
```

#### 限流实现

**Gateway 层限流**:
```java
// AuthRateLimitFilter
// 使用 Redis 实现分布式限流
// 滑动窗口算��
// 默认: 20 次/60秒 (登录接口)
```

**Service 层限流**:
```java
// @RateLimit 自定义注解
@RateLimit(key = "avatarUpload", permits = 5, window = 60)
public void uploadAvatar(MultipartFile file);
```

### 9. 分布式会话

#### Spring Session
```java
// SessionConfig
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800)
public class SessionConfig {
}
```

- **存储**: Redis (数据库 0)
- **Session ID**: `SESSION` cookie
- **过期时间**: 30 分钟

### 10. 定时任务

#### Spring Scheduler
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
```java
implementation 'org.springframework.boot:spring-boot-starter-actuator'
```

- **端点**: `/actuator/health`, `/actuator/info`
- **健康检查**: Redis, MySQL, RabbitMQ

#### Spring Launch Monitor
```java
implementation 'io.github.danburen:spring-launch-monitor:0.0.1'
```

- **启动报告**: HTML + JSON 格式
- **火焰图**: 启动时间分析

---

## 前端技术栈

### 客户端 (waterfun-web-client)

| 技术 | 版本 | 用途 |
|------|------|------|
| Nuxt | 3.16.2 | SSR 框架 |
| Vue | 3.5.13 | UI 框架 |
| Pinia | 3.0.3 | 状态管理 |
| Element Plus | 2.11.5 | UI 组件库 |
| @nuxt/i18n | 9.5.3 | 国际化 |
| Sass | 1.89.2 | 样式预处理 |
| dayjs | 1.11.18 | 日期处理 |
| vee-validate | 4.15.0 | 表单验证 |

### 管理后台 (waterfun-admin)

| 技术 | 版本 | 用途 |
|------|------|------|
| Vite | 7.1.10 | 构建工具 |
| Vue | 3.5.18 | UI 框架 |
| Pinia | 3.0.3 | 状态管理 |
| Element Plus | 2.11.4 | UI 组件库 |
| ECharts | 6.0.0 | 数据可视化 |
| vue-echarts | 8.0.1 | Vue ECharts 封装 |

### 公共模块 (waterfun-web-core)

- **API 类型定义**
- **错误处理**: APIError
- **请求封装**: Axios 实例
- **响应类型**: ApiRes, PromiseResBody

---

## 已实现功能

### 用户端功能

| 模块 | 功能 | 状态 |
|------|------|------|
| **认证** | 用户名密码登录 | ✅ |
| | 手机号登录 | ✅ |
| | 注册 | ✅ |
| | 验证码 (短信/邮箱) | ✅ |
| | Token 刷新/续期 | ✅ |
| | 登出 | ✅ |
| **用户** | 用户资料查看/编辑 | ✅ |
| | 头像上传 | ✅ |
| | 关注/粉丝 | ✅ |
| | 用户隐私设置 | ✅ |
| **内容** | 发帖 (分类/标签) | ✅ |
| | 帖子列表/详情 | ✅ |
| | 帖子编辑/删除 | ✅ |
| | 分类管理 | ✅ |
| | 标签管理 | ✅ |
| **通知** | 系统通知 | ✅ |
| | 通知已读/批量已读 | ✅ |
| **资源** | 腾讯云 COS 上传 | ✅ |
| | 预签名 URL 下载 | ✅ |

### 管理后台功能

| 模块 | 功能 | 状态 |
|------|------|------|
| **认证** | 管理员登录 | ✅ |
| | Token 管理 | ✅ |
| **用户** | 用户列表 | ✅ |
| | 用户禁用/启用 | ✅ |
| | 用户资料管理 | ✅ |
| **内容** | 帖子管理 | ✅ |
| | 帖子审核 | ✅ |
| | 分类管理 | ✅ |
| | 标签管理 | ✅ |
| **Banner** | Banner 管理 | ✅ |
| **角色权限** | 角色管理 | ✅ |
| | 权限管理 | ✅ |
| | 菜单管理 | ✅ |
| **审核** | 内容审核任务 | ✅ |
| | 审核队列 (RabbitMQ) | ✅ |
| **仪表盘** | 数据统计 | ✅ |
| | 图表展示 | ✅ |

### 后端服务

| 功能 | 实现方式 |
|------|----------|
| REST API | Spring MVC |
| 分布式Session | Spring Session + Redis |
| 分布式限流 | Redis 滑动窗口 |
| 本地限流 | Caffeine |
| 内容审核 | RabbitMQ 异步队列 |
| 验证码 | Redis TTL |
| 定时清理 | @Scheduled |

---

## 安全特性

1. **密码加密**: BCrypt
2. **JWT RSA 签名**: RS256
3. **CSRF 防护**: Spring Security Cookie
4. **限流保护**: 服务端 + 网关端双重限流
5. **敏感操作验证码**: 关键操作需验证码
6. **设备指纹**: 防止 token 盗用

---

## 部署架构

```
                                    ┌──────────────────┐
                                    │   Nginx / CDN    │
                                    └────────┬─────────┘
                                             │
                              ┌──────────────┴───────────┐
                              │                            │
                     ┌────────▼─────────┐       ┌────────────▼──────┐
                     │  Gateway :8080 │       │  Admin Gateway  │
                     └────────┬────────┘       └────────┬───────┘
                              │                          │
              ┌───────────────┼─────────────���─���──────────┼───────────────┐
              │               │                          │               │
     ┌────────▼────────┐ ┌────▼──────────┐    ┌─────────▼────────┐ ┌──▼──────────┐
     │ User Service   │ │Admin Service  │    │  Admin Service   │ │ Admin App   │
     │   :8081        │ │    :8082      │    │     :8082       │ │  (SPA)     │
     └───────┬────────┘ └────┬─────────┘    └─────────┬────────┘ └──┬─────────┘
             │                │                        │               │
             └────────────────┼────────────────────────┘               │
                            └────────┬─────────────────────────┘
                                     │
                    ┌────────────────┼────────────────────┐
                    │                │                     │
              ┌─────▼─────┐  ┌─────▼─────┐  ┌─────────▼──────┐
              │  MySQL    │  │  Redis    │  │   RabbitMQ     │
              │ :3306    │  │  :6379    │  │    :5672       │
              └──────────┘  └──────────┘  └────────────────┘
                                              │
                                        ┌─────▼─────┐
                                        │  Tencent   │
                                        │    COS    │
                                        └───────────┘
```

---

## 配置文件

### 主服务 (application.yml)
- 数据库: MySQL
- 缓存: Redis
- 消息队列: RabbitMQ
- 邮件: QQ邮箱 SMTP
- 对象存储: 腾讯云 COS
- 短信: 阿里云 SMS

### 安全配置 (application-security.yml)
- JWT 配置
- OAuth2 配置
- 安全策略

### 认证配置 (application-auth.yml)
- Token 过期时间
- 设备指纹配置

---

## 技术亮点

1. **微服务架构**: Gateway + 多个 Service
2. **双令牌认证**: JWT + RSA + 设备指纹
3. **三级缓存**: Caffeine + Redis + MySQL
4. **消息队列异步审核**: RabbitMQ
5. **云存储预签名**: 腾讯云 COS STS
6. **分布式限流**: Redis + 滑动窗口
7. **分布式 Session**: Spring Session
8. **验证码策略模式**: 支持多种验证码渠道

---

## 依赖版本

- Java: 22
- Spring Boot: 4.0.1 / 4.0.3
- Spring Cloud: 2025.1.0
- Node.js: ≥20.19.0
- Nuxt: 3.16.2
- Vue: 3.5.x
- pnpm: 10.7.1

---

*Last Updated: 2026-05-07*