# WaterFun 部署指南

> 适用于：**裸 Ubuntu 22.04+**，部署方式：**Docker 容器化后端 + 宿主机 Nginx + Certbot 自动 SSL**

---

## 目录

1. [架构概览](#一架构概览)
2. [前置条件](#二前置条件)
3. [服务器初始化](#三服务器初始化)
4. [基础设施（MySQL + Redis + RabbitMQ）](#四基础设施mysql--redis--rabbitmq)
5. [密钥与配置](#五密钥与配置)
6. [部署后端服务](#六部署后端服务)
7. [Nginx 反向代理](#七nginx-反向代理)
8. [SSL 证书（Certbot 自动）](#八ssl-证书certbot-自动)
9. [GitHub Actions CI/CD](#九github-actions-cicd)
10. [验证清单](#十验证清单)
11. [日常运维](#十一日常运维)

---

## 一、架构概览

```
                        Internet
                           │
                      [DNS: waterfun.top]
                           │
                     ┌─────┴─────┐
                     │   Nginx    │  ← 宿主机，SSL 终结
                     │  :443/80   │
                     └─────┬─────┘
                           │
                    ┌──────┴──────┐
                    │              │
               /api/**          /admin/*
                    │              │
            ┌───────┴───────┐     Nginx 直接
            │ 127.0.0.1     │     提供静态文件
            │  Docker       │
            │  gateway:8080 │
            └───┬───┬───┬───┘
                │   │   │
         ┌──────┘   │   └──────┐
         ▼          ▼          ▼
   user-service  admin-service  ← Docker 容器
      :8081         :8082          (127.0.0.1 绑定)
         │
    MySQL / Redis / RabbitMQ      ← Docker 或独立部署
```

### 端口说明

| 端口 | 绑定 | 用途 | 外部访问 |
|------|------|------|---------|
| 80/443 | `0.0.0.0` | Nginx HTTP/HTTPS | ✅ 是 |
| 8080 | `127.0.0.1` | Gateway（API 入口） | ❌ 仅宿主机 Nginx |
| 8081 | `127.0.0.1` | User Service | ❌ 仅网关转发 |
| 8082 | `127.0.0.1` | Admin Service | ❌ 仅网关转发 |
| 3306 | `127.0.0.1` | MySQL | ❌ 仅内部 |
| 6379 | `127.0.0.1` | Redis | ❌ 仅内部 |
| 5672 | `127.0.0.1` | RabbitMQ | ❌ 仅内部 |

> **安全原则**：所有 Java 服务和中间件端口全部绑定 `127.0.0.1`，只有 Nginx 监听 `0.0.0.0`。

---

## 二、前置条件

- 一台 Ubuntu 22.04+ 服务器（2C4G 以上）
- 一个域名（如 `waterfun.top`）已解析到服务器 IP
- GitHub 仓库已配置好 Actions Secrets（参考第九节）

---

## 三、服务器初始化

```bash
# 3.1 系统更新
sudo apt update && sudo apt upgrade -y

# 3.2 安装 Docker
curl -fsSL https://get.docker.com | sudo bash
sudo usermod -aG docker $USER
# 退出重登录使组生效

# 3.3 验证
docker --version && docker compose version

# 3.4 安装 Nginx + Certbot
sudo apt install -y nginx certbot python3-certbot-nginx
```

---

## 四、基础设施（MySQL + Redis + RabbitMQ）

### 4.1 创建 Docker 网络（只需一次）

```bash
docker network create waterfun-net
```

### 4.2 启动中间件

用项目提供的 `docker-compose.dev.yml`（它只包含中间件）：

```bash
# 复制到服务器
scp deploy/docker/docker-compose.dev.yml ubuntu@your-server:/opt/waterfun/

# 在服务器上执行
cd /opt/waterfun
docker compose -f docker-compose.dev.yml up -d

# 验证
docker ps
# 应该看到 mysql、redis、rabbitmq 三个容器
```

> 生产环境建议替换默认密码，编辑 `docker-compose.dev.yml` 中的环境变量。
> 或者自行准备外部 MySQL/Redis/RabbitMQ 服务。

### 4.3 初始化数据库

```bash
# 复制 SQL 脚本到服务器
scp sqls/CREATE_TABLES.sql ubuntu@your-server:/tmp/
scp sqls/CREATE_TABLES_2.sql ubuntu@your-server:/tmp/
scp sqls/CREATE_DATABASE_3.sql ubuntu@your-server:/tmp/

# 在服务器上执行
docker exec -i waterfun-mysql mysql -uroot -p123456 waterfun < /tmp/CREATE_TABLES.sql
docker exec -i waterfun-mysql mysql -uroot -p123456 waterfun < /tmp/CREATE_TABLES_2.sql
docker exec -i waterfun-mysql mysql -uroot -p123456 waterfun < /tmp/CREATE_DATABASE_3.sql
```

---

## 五、密钥与配置

### 5.1 生成 RSA 密钥对

```bash
# 在服务器上
mkdir -p /opt/waterfun/keys
# 复制 gen-keys.sh 到服务器并执行
deploy/bin/gen-keys.sh
# 或在服务器直接生成:
openssl genpkey -algorithm RSA -out /opt/waterfun/keys/private.key -pkeyopt rsa_keygen_bits:2048
openssl rsa -pubout -in /opt/waterfun/keys/private.key -out /opt/waterfun/keys/public.key
chmod 600 /opt/waterfun/keys/private.key
```

### 5.2 复制配置模板

```bash
cp deploy/config/common.yml /opt/waterfun/
```

### 5.3 创建环境变量文件

```bash
cat > /opt/waterfun/.env << 'EOF'
# ============================================================
# WaterFun 生产环境变量
# 安全提示: chmod 600 .env，不要提交到 Git
# ============================================================

# ---- 数据库 ----
DB_URL=jdbc:mysql://localhost:3306/waterfun
DB_USERNAME=root
DB_PASSWORD=你的MySQL密码

# ---- Redis ----
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=你的Redis密码

# ---- RabbitMQ ----
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=admin
RABBITMQ_PASSWORD=你的MQ密码

# ---- JWT（RSA 私钥路径）----
JWT_PRIVATE_KEY=file:/opt/waterfun/keys/private.key

# ---- 邮件（SMTP，用于发送验证码）----
MAIL_HOST=smtp.sendgrid.net
MAIL_PORT=465
MAIL_USERNAME=apikey
MAIL_PASSWORD=你的SMTP密码
SUPPORT_EMAIL=support@waterfun.top

# ---- 对象存储（腾讯 COS，用于图片/附件上传）----
TENCENT_COS_BUCKET=waterfun-prod-1300000000

# ---- 腾讯云 API（COS STS 临时密钥）----
TENCENTCLOUD_SECRET_ID=你的腾讯云SecretId
TENCENTCLOUD_SECRET_KEY=你的腾讯云SecretKey
# TENCENTCLOUD_REGION=ap-shanghai   # 可选，默认 ap-shanghai

# ---- 阿里云短信（验证码发送）----
ALIBABA_CLOUD_ACCESS_KEY_ID=你的阿里云AccessKey
ALIBABA_CLOUD_ACCESS_KEY_SECRET=你的阿里云Secret
ALIYUN_SMS_SIGN_NAME=水趣科技
ALIYUN_SMS_VERIFY_CODE_TEMPLATE=SMS_487295072

# ---- 通知邮件（Resend）----
RESEND_API_KEY=re_你的ResendKey

# ---- 安全配置 ----
DEVICE_SALT=$(openssl rand -hex 32)   # 设备指纹盐值，生成后固定
SECURITY_USER_NAME=admin              # Actuator 认证用户名
SECURITY_USER_PASSWORD=你的Actuator密码

# ---- 密钥加密密钥（AES-GCM，数据加密用）----
WATERFUN_KEK=$(openssl rand -base64 32)
EOF

chmod 600 /opt/waterfun/.env
```

> **`DEVICE_SALT` 和 `WATERFUN_KEK`** 首次生成后请固定下来，不要每次部署都重新生成，否则已有数据无法解密。

### 5.4 所有环境变量速查

| # | 变量 | 必须 | 说明 | 默认值 |
|---|------|------|------|--------|
| 1 | `DB_URL` | ✅ | MySQL JDBC 连接 | `jdbc:mysql://localhost:3306/waterfun` |
| 2 | `DB_USERNAME` | ✅ | MySQL 用户名 | `root` |
| 3 | `DB_PASSWORD` | ✅ | MySQL 密码 | `123456` |
| 4 | `REDIS_HOST` | ✅ | Redis 地址 | `localhost` |
| 5 | `REDIS_PORT` | | Redis 端口 | `6379` |
| 6 | `REDIS_PASSWORD` | | Redis 密码 | _(空)_ |
| 7 | `JWT_PRIVATE_KEY` | ✅ | RSA 私钥文件路径 | `file:./deploy/keys/private.key` |
| 8 | `MAIL_HOST` | ✅ | SMTP 服务器 | 无 |
| 9 | `MAIL_PORT` | ✅ | SMTP 端口 | 无 |
| 10 | `MAIL_USERNAME` | ✅ | SMTP 账号 | 无 |
| 11 | `MAIL_PASSWORD` | ✅ | SMTP 密码 | 无 |
| 12 | `SUPPORT_EMAIL` | ✅ | 客服邮箱 | 无 |
| 13 | `TENCENT_COS_BUCKET` | ✅ | COS 存储桶 | 无 |
| 14 | `TENCENTCLOUD_SECRET_ID` | ✅ | 腾讯云 API 密钥 | 无 |
| 15 | `TENCENTCLOUD_SECRET_KEY` | ✅ | 腾讯云 API 密钥 | 无 |
| 16 | `ALIBABA_CLOUD_ACCESS_KEY_ID` | ✅ | 阿里云 AccessKey | 无 |
| 17 | `ALIBABA_CLOUD_ACCESS_KEY_SECRET` | ✅ | 阿里云 Secret | 无 |
| 18 | `ALIYUN_SMS_SIGN_NAME` | ✅ | 短信签名 | 无 |
| 19 | `ALIYUN_SMS_VERIFY_CODE_TEMPLATE` | ✅ | 短信模板 Code | 无 |
| 20 | `RESEND_API_KEY` | ✅ | Resend API Key | 无 |
| 21 | `DEVICE_SALT` | ✅ | 设备指纹盐值 | 无（已移除默认值） |
| 22 | `WATERFUN_KEK` | ✅ | AES 密钥加密密钥 | 无 |

**可选变量**（有合理默认值）：

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `REDIS_PORT` | `6379` | |
| `REDIS_DATABASE` | `0` | |
| `REDIS_USERNAME` | `default` | |
| `RABBITMQ_HOST` | `localhost` | |
| `RABBITMQ_PORT` | `5672` | |
| `RABBITMQ_USERNAME` | `admin` | |
| `RABBITMQ_PASSWORD` | `admin123` | |
| `RABBITMQ_VHOST` | `/` | |
| `WATERFUN_GATEWAY_PORT` | `8080` | |
| `WATERFUN_SERVICE_PORT` | `8081` | |
| `WATERFUN_ADMIN_SERVICE_PORT` | `8082` | |
| `WATERFUN_USER_SERVICE_URI` | `http://localhost:8081` | |
| `WATERFUN_ADMIN_SERVICE_URI` | `http://localhost:8082` | |
| `SECURITY_USER_NAME` | `admin` | Actuator 认证 |
| `SECURITY_USER_PASSWORD` | `admin` | Actuator 认证 |
| `TENCENTCLOUD_REGION` | `ap-shanghai` | |

---

## 六、部署后端服务

### 6.1 使用 `docker-compose.deploy.yml`

项目提供了部署专用编排文件，它引用 GHCR 上的预构建镜像：

```bash
cd /opt/waterfun

# 首次部署前设置环境变量
export GITHUB_REPOSITORY=你的GitHub用户名/waterfun

# 登录 GHCR（需要 GitHub PAT，权限 read:packages）
echo $CR_PAT | docker login ghcr.io -u 你的GitHub用户名 --password-stdin

# 拉取镜像
docker compose -f docker-compose.deploy.yml pull

# 启动服务
docker compose -f docker-compose.deploy.yml up -d

# 验证
curl http://localhost:8080/actuator/health
# 预期: {"status":"UP"}
```

### 6.2 验证后端

```bash
# 查看日志
docker compose -f docker-compose.deploy.yml logs -f --tail=100

# 健康检查
curl -s http://localhost:8080/actuator/health | jq .

# 测试 API（需要有 Nginx 或直接改 hosts）
curl -s http://localhost:8080/api/public/banners
```

---

## 七、Nginx 反向代理

### 7.1 创建 Nginx 配置

```bash
sudo tee /etc/nginx/sites-available/waterfun << 'NGINX'
# WaterFun Production Nginx Configuration
# SSL termination + API reverse proxy + Admin SPA

upstream gateway {
    server 127.0.0.1:8080;
}

server {
    listen 80;
    server_name waterfun.top api.waterfun.top;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name waterfun.top api.waterfun.top;

    # SSL 证书由 Certbot 自动管理
    ssl_certificate     /etc/letsencrypt/live/waterfun.top/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/waterfun.top/privkey.pem;
    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_ciphers         HIGH:!aNULL:!MD5;

    # 安全头
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;

    # Admin 管理后台（静态文件）
    location /admin/ {
        alias /usr/share/nginx/html/admin/;
        try_files $uri $uri/ /admin/index.html;
        add_header Cache-Control "public, max-age=3600";
    }

    # API 请求 → Gateway
    location /api/ {
        proxy_pass http://gateway;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # SSE 通知支持
        proxy_buffering off;
        proxy_cache off;
        proxy_read_timeout 86400s;
    }

    # 健康检查
    location /health {
        proxy_pass http://gateway/actuator/health;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
    }

    # 社区前端（Nuxt SSR，预留）
    location / {
        # 等 Dockerfile.web-client 就绪后取消注释
        # proxy_pass http://127.0.0.1:3000;
        return 502;
    }

    # 禁止访问敏感文件
    location ~ /\.(env|git|svn) {
        deny all;
        return 404;
    }
}
NGINX
```

### 7.2 启用站点

```bash
sudo ln -sf /etc/nginx/sites-available/waterfun /etc/nginx/sites-enabled/
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t && sudo systemctl reload nginx
```

---

## 八、SSL 证书（Certbot 自动）

```bash
# 申请证书（会自动修改 Nginx 配置）
sudo certbot --nginx -d waterfun.top -d api.waterfun.top

# 验证自动续签
sudo certbot renew --dry-run
```

Certbot 会自动创建 systemd timer，证书到期前会自动续签并 reload Nginx。
不需要额外配置定时任务。

---

## 九、GitHub Actions CI/CD

### 9.1 首次运行前准备工作

1. 确保 `.github/workflows/deploy.yml` 已推送到仓库
2. 创建 GitHub Personal Access Token（`read:packages` 权限），以便服务器拉取镜像
3. 在服务器上用 PAT 登录 GHCR：
   ```bash
   export CR_PAT=你的_github_pat
   echo $CR_PAT | docker login ghcr.io -u 你的GitHub用户名 --password-stdin
   ```

### 9.2 配置 GitHub Secrets

仓库 → Settings → Secrets and variables → Actions → New repository secret：

| Secret | 说明 |
|--------|------|
| `SSH_HOST` | 服务器 IP 地址 |
| `SSH_USER` | SSH 用户名（如 `ubuntu`） |
| `SSH_KEY` | SSH 私钥内容（`cat ~/.ssh/id_rsa`） |
| `DEPLOY_DOMAIN` | 域名，如 `waterfun.top`（用于替换前端 .env.production 占位符） |

### 9.3 触发部署

**手动触发：**
```
GitHub → Actions → Deploy → Run workflow → 输入 tag（默认 latest）→ Run
```

**自动触发（暂未启用）：**
当 `main` 分支有推送时自动触发。如需启用，编辑 `.github/workflows/deploy.yml` 注释掉的 `push` 部分。

### 9.4 CI/CD 流程

```
1. Checkout 代码
2. Build Admin 前端（替换 YOUR_DOMAIN → 真实域名）
3. Build 后端 JAR（gradlew build -x test）
4. Build & Push Docker 镜像到 ghcr.io（gateway / user-service / admin-service）
5. SSH 登录服务器
   → docker login ghcr.io
   → docker pull 新镜像
   → docker compose up -d 重启服务
6. SCP 上传前端静态文件
   → 解压到 /usr/share/nginx/html/admin/
```

---

## 十、验证清单

部署完成后逐项确认：

- [ ] `curl http://localhost:8080/actuator/health` → `{"status":"UP"}`
- [ ] `curl https://waterfun.top/health` → 能访问
- [ ] `curl https://waterfun.top/api/public/banners` → 返回 JSON
- [ ] `curl https://waterfun.top/admin/` → 返回管理后台页面
- [ ] 浏览器打开 `https://waterfun.top` → 不走代理的话看到 502（Nuxt 预留）
- [ ] `sudo certbot renew --dry-run` → 续签测试通过
- [ ] Docker 容器日志无报错：`docker compose logs --tail=50`

---

## 十一、日常运维

### 日志查看

```bash
# 所有服务日志
docker compose -f docker-compose.deploy.yml logs -f

# 单个服务
docker compose -f docker-compose.deploy.yml logs -f gateway
docker compose -f docker-compose.deploy.yml logs -f user-service
```

### 更新版本

```bash
# 更新 .env 中的 IMAGE_TAG
# 然后:
docker compose -f docker-compose.deploy.yml pull
docker compose -f docker-compose.deploy.yml up -d
```

### 备份数据库

```bash
# 使用项目提供的备份脚本
deploy/bin/backup_mysql.sh
```

### 查看 JVM 监控

```
https://waterfun.top/api/admin/monitor/jvm  （需登录）
```

---

## 附录：关键文件索引

| 文件 | 用途 |
|------|------|
| `.github/workflows/deploy.yml` | CI/CD 自动部署 workflow |
| `deploy/docker/docker-compose.deploy.yml` | 服务器部署编排（引用 GHCR 镜像） |
| `deploy/docker/docker-compose.prod.yml` | 全量生产编排（含 Nginx + 中间件） |
| `deploy/docker/docker-compose.dev.yml` | 开发环境中间件编排 |
| `deploy/config/common.yml` | 统一配置模板（环境变量注入） |
| `deploy/env-vars-reference.md` | 环境变量完整参考 |
| `deploy/bin/gen-keys.sh` / `gen-keys.ps1` | RSA 密钥对生成脚本 |
| `deploy/bin/backup_mysql.sh` / `backup_mysql.ps1` | 数据库备份脚本 |
| `deploy/bin/check-env.sh` / `check-env.ps1` | 环境变量检查脚本 |
| `deploy/bin/setup-ip2region.sh` / `setup-ip2region.ps1` | IP 地理库下载脚本 |
| `deploy/docker/Dockerfile.gateway` | Gateway Dockerfile |
| `deploy/docker/Dockerfile.service` | User Service Dockerfile |
| `deploy/docker/Dockerfile.admin` | Admin Service Dockerfile |
| `deploy/docker/nginx/nginx.conf` | 全容器化方案的 Nginx 配置参考 |
