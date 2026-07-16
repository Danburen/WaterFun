# WaterFun 部署指南

> 适用于：**Ubuntu 22.04+**，全容器化部署，**无需在宿主机安装 Nginx/Certbot/Java/Node.js**

### 什么是全容器化管理？

部署后所有组件运行在 Docker 容器中，宿主机只需要三样东西：**Docker + 网络 + 防火墙放行 80/443**。

| 以往的做法（宿主机依赖） | 现在的做法（全容器化） |
|------------------------|---------------------|
| 装 certbot + crontab 续签 SSL | acme-companion 容器自动申请/续签/reload |
| 手动写 nginx 配置 + reload | nginx-proxy 容器根据环境变量自动生成配置 |
| 装 Java 22 + 编译运行 JAR | 构建时打包为 Docker 镜像，直接 run |
| 进程管理用 systemd 维护 | Docker 内置健康检查 + 自动重启 |

**日常运维只需 `docker compose up -d` 和 `docker compose logs -f`，不需要 SSH 进去改任何系统配置。**

---

## 目录

1. [架构概览](#一架构概览)
2. [前置条件](#二前置条件)
3. [DNS 配置](#三dns-配置)
4. [服务器初始化](#四服务器初始化)
5. [基础设施部署（MySQL + Redis + RabbitMQ）](#五基础设施部署mysql--redis--rabbitmq)
6. [初始化数据库](#六初始化数据库)
7. [密钥与配置](#七密钥与配置)
8. [服务部署（后端 + Nginx 代理 + SSL）](#八服务部署后端--nginx-代理--ssl)
9. [验证清单](#九验证清单)
10. [GitHub Actions CI/CD](#十github-actions-cicd)
11. [日常运维](#十一日常运维)

---

## 一、架构概览

```
                         Internet
                            │
                     80/443 │
                            ▼
               ┌──────────────────────┐
               │  nginx-proxy 容器     │  ← 自动反向代理 + SSL 终结
               │  + acme-companion    │  ← 自动申请/续签 Let's Encrypt
               └────────┬─────────────┘
                        │
                ┌────────┴──────────┐
                │                   │
        api.waterfun.top     waterfun.top
                │                   │
           Gateway 容器      Web Client 容器
          (JWT 认证)        (Nuxt 4 SSR)
          ↙         ↘              │
    user-service   admin-service    │
    (容器)          (容器)          │
          │              │         │
          └─── admin.waterfun.top ──┘
                            │
                      Nginx 容器
                 (Admin SPA 静态文件)
                            │
          ┌─────────────────┼─────────────────┐
           MySQL    Redis    RabbitMQ
           (容器)   (容器)    (容器)

  Web Client 通过 nginx-proxy 反向代理 → waterfun.top（VIRTUAL_HOST）
  Admin SPA 通过 nginx-proxy 反向代理 → admin.waterfun.top
  API 通过 nginx-proxy 反向代理 → api.waterfun.top
```

### 为什么用子域名，而不是单域名 + 路径（api.waterfun.top/api/）？

| 对比项 | 子域名（当前方案） | 单域名 + 路径 |
|--------|------------------|--------------|
| DNS 记录 | 多配 2 条 A 记录（一次 30 秒） | 只需 1 条 |
| SSL 证书 | **nginx-proxy + acme-companion 自动申请 + 自动续签，零维护** | 手动 certbot + crontab 续签，忘了用户看到安全警告 |
| 增加新服务 | 容器加 `VIRTUAL_HOST` 环境变量即可 | 改 nginx 配置再 reload |

为什么推荐子域名：**一次性的 30 秒 DNS 配置，换来 SSL 证书永远自动续签、永不失效。** 而单域名方案每次续签还得确保 crontab 不能炸。

### 域名规划

| 域名 | 用途 | 指向 |
|------|------|------|
| `api.waterfun.top` | 后端 API | Gateway 容器 |
| `admin.waterfun.top` | 管理后台 | Admin SPA Nginx 容器 |
| `waterfun.top` | 社区论坛（Nuxt 4 SSR） | Web Client 容器 |

### Compose 文件拆分

| 文件 | 内容 | 更新频率 |
|------|------|---------|
| `docker-compose.infra.yml` | MySQL, Redis, RabbitMQ | 几乎不更新 |
| `docker-compose.app.yml` | nginx-proxy, acme-companion, admin-nginx, web-client, 后端服务 | 随版本更新 |
| `docker-compose.deploy.yml` | 后端服务 + web-client（CI/CD 用，引用 GHCR 镜像） | CI/CD 自动更新 |

---

## 二、前置条件

- 一台 Ubuntu 22.04+ 服务器（**2C2G 足够**，已内置低配调优）
- 域名已解析到服务器 IP（见下节）
- 以下端口未被占用：80, 443

---

## 三、DNS 配置

在域名管理后台添加 A 记录：便于Certbot自动申请证书。而不采用路径解析模式

| 类型 | 名称 | 值 |
|------|------|----|
| A | `api` | 服务器 IP |
| A | `admin` | 服务器 IP |
| A | `@` | 服务器 IP（Nuxt 论坛） |

验证：
```bash
dig api.waterfun.top +short
dig admin.waterfun.top +short
# 都应返回服务器 IP
```

---

## 四、服务器初始化

```bash
# 4.1 系统更新 + 基础工具
sudo apt update && sudo apt upgrade -y
sudo apt install -y git curl
```
# 4.2 安装 Docker
```bash
curl -fsSL https://get.docker.com | sudo bash
sudo usermod -aG docker $USER
```
# 退出重登录使组生效

# 4.3 验证
```bash
git --version                # 预期 git 2.34+
docker --version             # 预期 Docker 27+
docker compose version
```

# 4.4 克隆项目
这里推荐优先配置代理，国内有些服务器访问git会很慢：
```bash
Host github.com
    HostName github.com
    User git
    Port 22
    IdentityFile ~/.ssh/id_rsa # 你的ssh key 路径
    # 使用 nc 通过 SOCKS5 代理(如果有的话）
    # ProxyCommand nc -v -x 127.0.0.1:7890 %h %p
```

```bash
git clone https://github.com/你的/waterfun.git /opt/waterfun
```

# 4.5 创建密钥与前端目录
```bash
mkdir -p /opt/waterfun/deploy/docker/{admin-dist,keys}
```

---

## 五、基础设施部署（MySQL + Redis + RabbitMQ）

### 5.1 创建 Docker 网络（只需一次）

```bash
docker network create waterfun-net
```

### 5.2 确认配置文件就位

§4.4 已通过 `git clone` 将完整仓库克隆到 `/opt/waterfun/`，所有部署配置文件已在仓库中，**无需额外上传**：

```bash
ls -la /opt/waterfun/deploy/docker/docker-compose.infra.yml
ls -la /opt/waterfun/deploy/config/common.yml
ls -la /opt/waterfun/deploy/docker/nginx/admin-nginx.conf
```

如果上述文件都存在，继续下一步。如果不存在，说明 clone 不完整，重新执行 §4.4。

### 5.3 创建环境变量文件

```bash
cat > /opt/waterfun/.env << 'EOF'
# ============================================================
# WaterFun 生产环境变量
# 安全提示: chmod 600 .env，不要提交到 Git
# ============================================================

# ---- 数据库 ----
DB_PASSWORD=你的MySQL密码

# ---- Redis ----
REDIS_PASSWORD=你的Redis密码

# ---- RabbitMQ ----
RABBITMQ_USERNAME=admin
RABBITMQ_PASSWORD=你的MQ密码

# ---- 邮件 ----
MAIL_HOST=smtp.sendgrid.net
MAIL_PORT=465
MAIL_USERNAME=apikey
MAIL_PASSWORD=你的SMTP密码
SUPPORT_EMAIL=support@waterfun.top

# ---- 对象存储（腾讯 COS）----
TENCENT_COS_BUCKET=waterfun-prod-1300000000

# ---- 腾讯云 API（COS STS 临时密钥）----
TENCENTCLOUD_SECRET_ID=你的腾讯云SecretId
TENCENTCLOUD_SECRET_KEY=你的腾讯云SecretKey
TENCENTCLOUD_REGION=ap-shanghai                              # 可选，默认 ap-shanghai（上海），华南用 ap-guangzhou

# ---- 阿里云短信 ----
ALIBABA_CLOUD_ACCESS_KEY_ID=你的阿里云AccessKey
ALIBABA_CLOUD_ACCESS_KEY_SECRET=你的阿里云Secret
ALIYUN_SMS_SIGN_NAME=水趣科技
ALIYUN_SMS_VERIFY_CODE_TEMPLATE=SMS_487295072

# ---- 通知邮件（Resend）----
RESEND_API_KEY=re_你的ResendKey

# ---- 安全配置 ----
DEVICE_SALT=你的设备指纹盐值                    # 部署后固定，不要改
SECURITY_USER_PASSWORD=你的Actuator密码
WATERFUN_KEK=你的KEK值                          # 部署后固定，不要改

# ---- SSL 证书邮箱（用于 Let's Encrypt）----
SSL_EMAIL=admin@waterfun.top

# ---- 子域名 ----
API_DOMAIN=api.waterfun.top
ADMIN_DOMAIN=admin.waterfun.top
EOF

chmod 600 /opt/waterfun/.env
```

> **`DEVICE_SALT` 和 `WATERFUN_KEK`**：首次部署时在服务器上生成，写入后固定，不要每次部署都重新生成：
> ```bash
> openssl rand -hex 32      # 生成 DEVICE_SALT（64 字符十六进制）
> openssl rand -base64 32   # 生成 WATERFUN_KEK（44 字符 base64）
> ```

### 5.4 环境变量检查（必做）

每次 `docker compose up` 之前都必须跑，确认所有 `required` 变量已配置：

```bash
cd /opt/waterfun
bash deploy/bin/check-env.sh
```

输出示例：

```
  [OK] Loaded /opt/waterfun/.env
  [OK] Loaded common-dev-secrets.yml

Result:
  [PASS] DB_URL (from env var)
  [PASS] DEVICE_SALT (from env var)
  ...
  [FAIL] MAIL_HOST -- SMTP server
  [FAIL] MAIL_PORT -- SMTP port

=== Summary ===
  Pass: 15 | From secrets file: 0 | Missing: 2

  [FAIL] The following 2 variable(s) have no source:
         - MAIL_HOST -- SMTP server
         - MAIL_PORT -- SMTP port

  [DEV] Add them to deploy/config/common-dev-secrets.yml
  [PROD] Inject them as system environment variables
```

**检查失败（有 `[FAIL]`）时不要继续部署**，补齐缺失变量再跑一次，直到全部通过。

### 5.5 启动基础设施

```bash
cd /opt/waterfun
docker compose -f deploy/docker/docker-compose.infra.yml up -d

# 验证三个容器都健康
docker ps
# 看到: waterfun-mysql, waterfun-redis, waterfun-rabbitmq
```

---

## 六、初始化数据库

无需手动执行 SQL。Flyway 在 `user-service` 首次启动时自动执行 `V1__baseline.sql` 创建全部表结构。

确保 `application-prod.yml`（在 JAR 内）和 `common.yml`（挂载为 Volume）就位即可，启动后自动建表：

```bash
# 启动后查看 Flyway 迁移日志
docker logs waterfun-user-service --tail=30 | grep flyway
# 预期输出: Successfully applied 1 migration to schema 'waterfun', now at version v1
```

---

## 七、密钥与配置

### 7.1 生成 RSA 密钥对

```bash
# 在服务器上执行（路径与 docker volume 挂载一致）
mkdir -p /opt/waterfun/deploy/keys
openssl genpkey -algorithm RSA -out /opt/waterfun/deploy/keys/private.key -pkeyopt rsa_keygen_bits:2048
openssl rsa -pubout -in /opt/waterfun/deploy/keys/private.key -out /opt/waterfun/deploy/keys/public.key
chmod 600 /opt/waterfun/deploy/keys/private.key
```

### 7.2 确认配置模板就位

```bash
# 仓库 clone 后已在 deploy/config/ 下，无需上传
ls -la /opt/waterfun/deploy/config/common.yml
```

### 7.3 上传 Admin 前端（首次部署）

> 前端构建在本地（有 Node.js 的开发机）完成，**服务器不需要安装 Node.js**。  
> 服务器通过 `admin-nginx` 容器挂载 `deploy/docker/admin-dist/` 目录提供静态文件。

```bash
# 本地开发机执行：构建
pnpm --filter @waterfun/admin build

# 上传到服务器（路径与 compose volume 挂载一致）
scp -r waterfun-admin/dist/* ubuntu@your-server:/opt/waterfun/deploy/docker/admin-dist/
```

---

## 八、服务部署（后端 + Nginx 代理 + SSL）

### 8.1 确认编排文件就位

```bash
# 仓库 clone 后已在 deploy/docker/ 下，无需上传
ls -la /opt/waterfun/deploy/docker/docker-compose.app.yml
ls -la /opt/waterfun/deploy/docker/nginx/admin-nginx.conf
```

### 8.2 环境检查（必做）

```bash
cd /opt/waterfun
bash deploy/bin/check-env.sh
```

确认全部 `[PASS]` 后再继续。

### 8.3 构建并启动

```bash
cd /opt/waterfun

# 构建后端镜像（首次大约 5-10 分钟，后续有缓存）
docker compose -f deploy/docker/docker-compose.app.yml build

# 启动所有服务（compose 已内置 SPRING_PROFILES_ACTIVE=prod，自动加载 application-prod.yml）
docker compose -f deploy/docker/docker-compose.app.yml up -d

# 查看启动日志
docker compose -f deploy/docker/docker-compose.app.yml logs -f --tail=50
```

> `SPRING_PROFILES_ACTIVE=prod` 已在 `docker-compose.app.yml` / `docker-compose.prod.yml` / `docker-compose.deploy.yml` 三个编排文件中设置，生产环境使用 `prod` profile，自动关闭 Swagger 等开发功能。

### 8.4 关于 SSL 证书（全自动，宿主机零操作）

**nginx-proxy + acme-companion 会自动完成以下工作：**

1. 检测 `LETSENCRYPT_HOST` 环境变量
2. 为配置的域名自动申请 Let's Encrypt 证书
3. **证书到期前自动续签（无需 cron、无需 systemd timer）**
4. **续签后自动 reload nginx（无需手动操作）**
5. 配置 301 HTTP → HTTPS 重定向

**宿主机不需要安装 certbot，不需要写定时任务，不需要手动续签。** 证书永不过期。

首次申请可能需要 **1-2 分钟**，证书就绪后 nginx-proxy 自动 reload：

```bash
# 查看证书申请进度
docker logs waterfun-acme -f
```

### 8.5 首次启动后的验证

```bash
# 查看所有运行中的容器
docker ps

# 应该看到:
#   waterfun-nginx-proxy      (nginx-proxy)
#   waterfun-acme             (acme-companion)
#   waterfun-admin            (admin SPA)
#   waterfun-web-client       (Nuxt 4 SSR)
#   waterfun-gateway          (Gateway)
#   waterfun-user-service     (User Service)
#   waterfun-admin-service    (Admin Service)

# 健康检查（通过 Docker 内网）
curl -s http://localhost:8080/actuator/health
# 预期: {"status":"UP"}

# 等待 SSL 就绪后（约 1-2 分钟）:
curl -sI https://api.waterfun.top/api/public/banners
# 预期: HTTP/2 200

# Web Client 验证
curl -sI https://waterfun.top/
# 预期: HTTP/2 200（Nuxt SSR 返回 HTML）
```

---

## 九、验证清单

- [ ] `bash deploy/bin/check-env.sh` → 全部 `[PASS]`，无 `[FAIL]`
- [ ] `curl -s http://localhost:8080/actuator/health` → `{"status":"UP"}`
- [ ] `curl -s https://api.waterfun.top/api/public/banners` → 返回 JSON
- [ ] `curl -sI https://admin.waterfun.top/` → HTTP/2 200
- [ ] `curl -sI https://admin.waterfun.top/index.html` → 返回 HTML
- [ ] 浏览器打开 `https://admin.waterfun.top` → 显示管理后台登录页
- [ ] `curl -sI https://waterfun.top/` → HTTP/2 200（Nuxt SSR 返回 HTML）
- [ ] 浏览器打开 `https://waterfun.top` → 显示论坛首页
- [ ] `docker logs waterfun-acme` → 无 SSL 错误日志
- [ ] 各容器日志无异常：`docker compose -f deploy/docker/docker-compose.app.yml logs --tail=30`

---

## 十、GitHub Actions CI/CD

### 10.1 第一次 CI/CD 部署前准备

确保服务器上已成功完成一次**手动部署**（步骤五～八），然后：

#### 1. 登录 GHCR

```bash
# 创建 GitHub PAT（Settings → Developer settings → Personal access tokens → Fine-grained tokens）
# 权限: read:packages（部署用）
export CR_PAT=你的_github_pat
echo $CR_PAT | docker login ghcr.io -u 你的GitHub用户名 --password-stdin
```

> CI/CD workflow 运行时使用 `GITHUB_TOKEN` 登录（workflow 运行期间有效）。
> 但如果 workflow 执行间隔较长，`GITHUB_TOKEN` 可能过期。
> 建议创建一个 PAT 设为服务器环境变量 `CR_PAT`，workflow 脚本读取它登录。

#### 2. 配置 GitHub Secrets

仓库 → Settings → Secrets and variables → Actions → New repository secret：

| Secret | 说明 |
|--------|------|
| `SSH_HOST` | 服务器 IP 地址 |
| `SSH_USER` | SSH 用户名（如 `ubuntu`） |
| `SSH_KEY` | SSH 私钥内容（`cat ~/.ssh/id_rsa`） |
| `DEPLOY_DOMAIN` | 部署域名，如 `waterfun.top`（替换前端 `VITE_API_BASE` 和 `.env.production` 中的 `YOUR_DOMAIN` 占位符） |

### 10.2 触发部署

```
GitHub → Actions → Deploy → Run workflow → Branch: main → 输入 tag（默认 latest）→ Run workflow
```

### 10.3 CI/CD 流程

```
1. Checkout 代码
2. Build Admin 前端（替换 YOUR_DOMAIN → 真实域名）
3. Build Web Client 前端（Nuxt 4 SSR，替换 YOUR_DOMAIN → 真实域名）
4. Build 后端 JAR（gradlew build -x test）
5. Build & Push Docker 镜像到 ghcr.io（gateway / user-service / admin-service / web-client）
6. SSH 登录服务器
   → docker login ghcr.io
   → cd /opt/waterfun
   → export GITHUB_REPOSITORY 和 IMAGE_TAG（传递给 compose）
   → docker compose -f deploy/docker/docker-compose.deploy.yml pull（拉取新镜像）
   → docker compose -f deploy/docker/docker-compose.deploy.yml up -d --no-deps 重启所有服务（含 web-client）
   → docker image prune 清理旧镜像
7. SCP 上传 admin 前端静态文件 /tmp/admin-dist.tar.gz
   → 解压到 /opt/waterfun/deploy/docker/admin-dist/（admin-nginx 容器自动加载）
```

### 10.4 触发方式说明

三种触发方式，按需使用：

| 触发方式 | 场景 | 镜像标签 |
|---------|------|---------|
| `workflow_dispatch` | 手动部署（紧急修复 / 回滚） | 自定义（默认 `latest`） |
| `push` main 分支 | 日常 PR 合并到 main | `latest` |
| `create` tag (v*) | 正式发版，可回滚 | 版本号（如 `v1.0.0`） |

> **发版示例**：
> ```bash
> git checkout main
> git tag v1.0.0
> git push origin v1.0.0
> ```
> 会自动触发 CI/CD，构建并推送 `ghcr.io/你的/waterfun/gateway:v1.0.0` 到服务器。
> 如需回滚，手动触发 workflow 输入旧版本号即可。

---

## 十一、日常运维

### 全部服务状态

```bash
cd /opt/waterfun

# 基础设施状态
docker compose -f deploy/docker/docker-compose.infra.yml ps

# 应用服务状态
docker compose -f deploy/docker/docker-compose.app.yml ps
```

### 查看日志

```bash
cd /opt/waterfun

# 某个服务日志
docker compose -f deploy/docker/docker-compose.app.yml logs -f gateway
docker compose -f deploy/docker/docker-compose.app.yml logs -f user-service

# 反向代理日志
docker logs waterfun-nginx-proxy --tail=50

# SSL 证书日志
docker logs waterfun-acme -f
```

### 更新后端服务（CI/CD）

提交代码 → 触发 CI/CD → 自动完成。手动等效操作：

```bash
cd /opt/waterfun
export GITHUB_REPOSITORY=your-org/waterfun   # 替换为实际 org
export IMAGE_TAG=latest
docker compose -f deploy/docker/docker-compose.deploy.yml pull
docker compose -f deploy/docker/docker-compose.deploy.yml up -d --no-deps \
  gateway user-service admin-service web-client
```

### 更新 Admin 前端（手动）

```bash
# 本地开发机执行：构建
pnpm --filter @waterfun/admin build

# 上传到服务器（路径与 compose volume 挂载一致）
scp -r waterfun-admin/dist/* ubuntu@your-server:/opt/waterfun/deploy/docker/admin-dist/

# 无需重启容器，nginx 立即加载新文件
```

### 备份数据库

```bash
cd /opt/waterfun
# 仓库 clone 后脚本已在 deploy/bin/ 下，直接运行
bash deploy/bin/backup_mysql.sh
```

或手动备份：

```bash
docker exec waterfun-mysql mysqldump -uroot -p${DB_PASSWORD} waterfun | gzip > /data/backup/waterfun_$(date +%Y%m%d).sql.gz
```

### SSL 证书

acme-companion 会自动续签（Let's Encrypt 90 天有效期，提前 30 天续签）。

查看证书信息：

```bash
docker exec waterfun-acme acme.sh --list
```

### 停止/重启

```bash
cd /opt/waterfun

# 只重启后端服务（不碰 infra）
docker compose -f deploy/docker/docker-compose.app.yml restart gateway user-service admin-service web-client

# 完整重启应用栈
docker compose -f deploy/docker/docker-compose.app.yml down
docker compose -f deploy/docker/docker-compose.app.yml up -d

# 重启全部（包括 infra，谨慎操作）
docker compose -f deploy/docker/docker-compose.infra.yml restart
docker compose -f deploy/docker/docker-compose.app.yml restart
```

---

## 附录：关键文件索引

| 文件 | 用途 |
|------|------|
| `.github/workflows/deploy.yml` | CI/CD 自动部署 workflow（含 web-client 构建和推送） |
| `deploy/docker/docker-compose.infra.yml` | 基础设施编排（MySQL/Redis/RabbitMQ）|
| `deploy/docker/docker-compose.app.yml` | 服务编排（反向代理 + 后端 + Admin + Web Client）|
| `deploy/docker/docker-compose.deploy.yml` | CI/CD 部署用（后端 3 服务 + web-client，引用 GHCR 镜像，由 CI/CD 调用）|
| `deploy/docker/docker-compose.prod.yml` | 全量参考编排（旧方案，已拆分为 infra + app）|
| `deploy/docker/nginx/admin-nginx.conf` | Admin SPA Nginx 配置 |
| `deploy/config/common.yml` | 统一配置模板（生产安全，日志 INFO 级别） |
| `waterfun-*/src/main/resources/application-prod.yml` | 生产 profile 配置（关闭 Swagger，内置在 JAR 中） |
| `deploy/bin/gen-keys.sh` | RSA 密钥对生成脚本 |
| `deploy/bin/check-env.sh` | 环境变量检查脚本 |
| `deploy/bin/setup-ip2region.sh` | IP 地理库下载脚本 |
| `deploy/docker/Dockerfile.gateway` | Gateway Dockerfile |
| `deploy/docker/Dockerfile.service` | User Service Dockerfile |
| `deploy/docker/Dockerfile.admin` | Admin Service Dockerfile |
| `deploy/docker/Dockerfile.web-client` | Web Client（Nuxt 4 SSR）Dockerfile |
