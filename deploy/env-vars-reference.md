# WaterFun 环境变量配置参考

---

## 一、配置文件加载链

```
application.yml  ─── 始终加载（各服务自己的配置，路径/端口等）
    │
    ├── deploy/config/common.yml               ← 统一配置模板
    │   ├── 非三方/infra 配置: 自带 :default 值，零配置启动
    │   └── 三方/敏感配置: 纯 ${VAR} 引用，无默认值
    │
    └── deploy/config/common-dev-secrets.yml   ← ❌ 不上 git
        └── 所有三方接口/敏感凭据的开发默认值
```

**开发环境**：clone 项目后，找同事复制 `common-dev-secrets.yml`，然后直接启动，无需设置任何系统环境变量。
**生产环境**：所有 `${VAR}` 通过系统环境变量注入，覆盖 `common.yml` 中的 `:default` 值。

---

## 二、开发环境

### 2.1 启动条件

开发环境**不需要设置任何系统环境变量**。`common.yml` 已经为所有基础设施配置（DB/Redis/RabbitMQ/JWT/Device）提供了 `:default` 默认值。

需要云功能（发邮件/短信/上传文件）时，确保 `deploy/config/common-dev-secrets.yml` 存在且包含有效的三方凭据。

### 2.2 `common-dev-secrets.yml` 文件内容参考

```yaml
# 邮件服务器 (QQ SMTP)
MAIL_HOST: smtp.qq.com
MAIL_PORT: 465
MAIL_USERNAME: your-email@qq.com
MAIL_PASSWORD: your-smtp-auth-code

# 客服邮箱
SUPPORT_EMAIL: support@your-domain.com

# 对象存储 (Tencent COS)
TENCENT_COS_BUCKET: your-bucket-1300000000

# SMS 阿里云
ALIYUN_SMS_SIGN_NAME: 你的短信签名
ALIYUN_SMS_VERIFY_CODE_TEMPLATE: SMS_000000000

# 通知 (Resend)
RESEND_API_KEY: re_xxxxxxxxxxxx
```

### 2.3 Docker 开发环境

直接 `docker compose -f deploy/docker/docker-compose.dev.yml up`，所有值已硬编码，无需额外配置。

---

## 三、生产环境

### 3.1 配置文件加载链

```
application.yml  ─── 始终加载
    │
    ├── deploy/config/common.yml      ← 所有 ${VAR:default} 被环境变量覆盖
    └── deploy/config/common-dev-secrets.yml  ← 文件不存在时跳过（optional）
```

生产环境**不依赖** `common-dev-secrets.yml`。所有 `${VAR}`（尤其是无 `:default` 的三方配置）必须通过系统环境变量注入。

### 3.2 生产部署操作步骤

1. 运行 `deploy/bin/gen-keys.ps1`（Windows）或 `deploy/bin/gen-keys.sh`（Unix）生成新的 RSA 密钥对
2. 将密钥对的**私钥**安全保存，路径通过 `JWT_PRIVATE_KEY` 指定
3. 设置以下所有生产环境变量
4. 启动服务

### 3.3 生产必需环境变量

| # | 变量 | 说明 | 示例值 |
|---|------|------|--------|
| 1 | `DB_URL` | MySQL 连接地址 | `jdbc:mysql://10.0.0.1:3306/waterfun` |
| 2 | `DB_USERNAME` | MySQL 用户名 | `waterfun_prod` |
| 3 | `DB_PASSWORD` | MySQL 密码 | — |
| 4 | `REDIS_HOST` | Redis 主机 | `10.0.0.1` |
| 5 | `REDIS_PORT` | Redis 端口 | `6379` |
| 6 | `REDIS_DATABASE` | Redis 数据库索引 | `0` |
| 7 | `REDIS_USERNAME` | Redis 用户名 | `default` |
| 8 | `REDIS_PASSWORD` | Redis 密码 | — |
| 9 | `RABBITMQ_HOST` | RabbitMQ 主机 | `10.0.0.1` |
| 10 | `RABBITMQ_PORT` | RabbitMQ 端口 | `5672` |
| 11 | `RABBITMQ_USERNAME` | RabbitMQ 用户名 | `waterfun_mq` |
| 12 | `RABBITMQ_PASSWORD` | RabbitMQ 密码 | — |
| 13 | `WATERFUN_GATEWAY_PORT` | 网关监听端口 | `8080` |
| 14 | `WATERFUN_SERVICE_PORT` | 用户服务端口 | `8081` |
| 15 | `WATERFUN_ADMIN_SERVICE_PORT` | 管理服务端口 | `8082` |
| 16 | `WATERFUN_USER_SERVICE_URI` | 用户服务地址（网关转发） | `http://user-service:8081` |
| 17 | `WATERFUN_ADMIN_SERVICE_URI` | 管理服务地址（网关转发） | `http://admin-service:8082` |
| 18 | `JWT_PRIVATE_KEY` | RSA 私钥文件路径 | `file:/etc/waterfun/keys/private.key` |
| 19 | `DEVICE_SALT` | 设备指纹盐值（≥16字符） | — |
| 20 | `MAIL_HOST` | SMTP 服务器 | `smtp.sendgrid.net` |
| 21 | `MAIL_PORT` | SMTP 端口 | `465` |
| 22 | `MAIL_USERNAME` | SMTP 账号 | — |
| 23 | `MAIL_PASSWORD` | SMTP 密码/授权码 | — |
| 24 | `SUPPORT_EMAIL` | 客服邮箱 | `support@waterfun.top` |
| 25 | `SECURITY_USER_NAME` | Actuator 安全用户 | `admin` |
| 26 | `SECURITY_USER_PASSWORD` | Actuator 安全密码 | — |
| 27 | `TENCENT_COS_BUCKET` | COS Bucket 名称 | `waterfun-prod-1300178389` |
| 28 | `RESEND_API_KEY` | Resend API Key | — |
| 29 | `ALIYUN_SMS_SIGN_NAME` | 阿里云短信签名名称 | `水趣科技` |
| 30 | `ALIYUN_SMS_VERIFY_CODE_TEMPLATE` | 阿里云短信验证码模板 Code | `SMS_487295072` |

### 3.4 生产可选环境变量

| # | 变量 | 说明 | 读取方式 |
|---|------|------|----------|
| 31 | `TENCENTCLOUD_SECRET_ID` | 腾讯云 API 密钥 ID | Java `System.getenv()` |
| 32 | `TENCENTCLOUD_SECRET_KEY` | 腾讯云 API 密钥 Key | Java `System.getenv()` |
| 33 | `WATERFUN_KEK` | 密钥加密密钥 | Java `System.getenv()` |

---

## 四、部署验证

设置完环境变量后运行：

```bash
# PowerShell 验证必需变量
@(
    "DB_URL","DB_USERNAME","DB_PASSWORD",
    "REDIS_HOST","REDIS_PORT","REDIS_PASSWORD",
    "JWT_PRIVATE_KEY",
    "MAIL_USERNAME","MAIL_PASSWORD"
) | ForEach-Object {
    if (-not (Get-Item "env:$_" -ErrorAction SilentlyContinue)) {
        Write-Warning "⚠ 缺少环境变量: $_"
    } else {
        Write-Host "✓ $_ 已设置" -ForegroundColor Green
    }
}
```
