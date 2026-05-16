# WaterFun - A new era diversified interactive forum

WaterFun 是一个论坛类网站项目，采用**前后端分离 + 网关**的混合型单体仓库（Gradle 多模块 + pnpm workspace）。当前包含用户侧论坛、后台管理、统一网关、通知服务等模块。

---

## ✨ 已实现功能（基于现有 API / 文档）

> 以下功能来自 `docs/api_docs/openapi.json` 与 `reference/*.openapi.json`，代表当前已具备的接口能力。

### 用户侧（Forum / User）

- 认证与登录
  - 账号密码登录
  - 短信验证码登录
  - 邮箱验证码登录
  - 注册
  - 刷新 Access Token
  - 登出
  - 验证码 / CSRF Token
- 账号安全
  - 设置密码 / 重置密码
  - 绑定 / 激活 / 修改邮箱
  - 绑定 / 激活 / 修改手机号
- 用户资料
  - 获取用户基础信息
  - 获取/更新个人资料（昵称、性别、生日、居住地、简介等）
  - 头像上传（预签名 URL + 回调确认流程）
- 帖子与标签
  - 发布帖子
  - 获取帖子列表（分页 + 分类 / 标签过滤）
  - 标签增删改查
- 系统通知
  - 系统通知列表（游标分页）
  - 未读数量
  - 批量标记已读 / 全部标记已读

### 后台管理（Admin）

> 参考 `reference/Admin.*.openapi.json`

- 管理员认证
- 用户管理
- 权限 / 角色管理
- 标签管理
- 分类管理
- 帖子管理
- 内容审核（moderation）

### 统一网关能力（Gateway）

- 统一 API 入口
- JWT 认证校验（非公共接口）
- 登录 / 注册 / 刷新 / 健康检查白名单
- 基础限流（内存实现）
- `X-Request-Id` 追踪

---

## 🧱 项目架构

### Monorepo 结构

```
/waterfun-gateway          # 统一网关
/waterfun-service          # 用户侧核心业务 API
/waterfun-admin-service    # 后台管理 API
/waterfun-notify-service   # 通知服务
/waterfun-service-core     # 共享业务 / 领域能力
/waterfun-common-lib       # 通用响应 / 错误 / 工具
/waterfun-admin            # 管理后台前端 (Vue3 + Vite)
/waterfun-web-client       # 用户侧前端 (Nuxt3)
/waterfun-web-core         # 前端共享类型/工具
```

### 关键认证链路

- 网关负责 JWT 校验 + Redis JTI 校验
- 网关注入 `X-User-*` 头部，业务服务依赖用户上下文过滤器
- 下游服务本身 `permitAll()`，由网关作为信任边界

---

## 🧰 技术栈（必须标注）

### 后端

- Java 22
- Spring Boot 4.0.1
- Spring Security / OAuth2 Resource Server / Authorization Server
- Spring Data JPA + JDBC
- MyBatis
- Redis + Spring Session
- WebSocket
- JWT (jjwt)
- MapStruct
- Lombok
- Hutool Captcha
- Swagger / OpenAPI
- MySQL
- RabbitMQ (AMQP)

### 前端

- 用户端：Nuxt 3 + Vue 3 + Vite
- 管理端：Vue 3 + Vite
- 状态管理：Pinia
- UI：Element Plus
- i18n：Vue I18n / @nuxtjs/i18n
- HTTP：Axios

### 其他集成

- COS（腾讯云对象存储）
- Aliyun SMS
- Resend 邮件发送
- 本地运行依赖：MySQL + Redis

---

## 🗂️ 接口与文档

- API 文档：`docs/api_docs/*.json`
- 参考 OpenAPI：`reference/*.openapi.json`

---

## 🚀 本地运行

### 后端（Windows）

```bash
# 构建全部
gradlew.bat build

# 运行单模块
gradlew.bat :waterfun-service:bootRun
gradlew.bat :waterfun-gateway:bootRun
gradlew.bat :waterfun-admin-service:bootRun
gradlew.bat :waterfun-notify-service:bootRun
```

### 前端

```bash
pnpm install

# 管理端
pnpm adev

# 用户端
pnpm cdev
```

---

## 🔧 配置与环境

- 共享配置模板：`deploy/shared/config/common.yml`
- DB/Redis 启动脚本：`deploy/bin/database-service.bat`
- DB 初始脚本：`sqls/`

---

## 🧭 其他说明

- API 返回统一封装：`ApiResponse<T>`（code/message/data）
- 错误使用 `ErrorResponse` + i18n 键值
- 控制器保持轻薄，核心逻辑下沉至 `waterfun-service-core`

---

## 📎 License

MIT
