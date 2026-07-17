# WaterFun 部署前本地测试流程

> 在 push 到 GitHub、触发 CI/CD **之前**，在本地开发机跑一遍这个流程。
> **大部分问题可以在 2 分钟内发现，不用等 CI 排队 5 分钟才报错。**

---

## 目录

1. [测试什么](#一测试什么)
2. [前置检查](#二前置检查)
3. [后端构建测试](#三后端构建测试)
4. [前端构建测试](#四前端构建测试)
5. [Docker 镜像构建测试](#五docker-镜像构建测试)
6. [全栈本地运行（可选）](#六全栈本地运行可选)
7. [修改 Dockerfile 后专项检查](#七修改-dockerfile-后专项检查)
8. [常见问题](#八常见问题)

---

## 一、测试什么

本地测试覆盖 CI/CD 中 **前 4 步**，确保 push 后 CI 不会在构建阶段失败：

| CI/CD 步骤 | 本地对应 | 发现问题 |
|-----------|---------|---------|
| Setup Node.js + pnpm | `pnpm install --frozen-lockfile` | lockfile 过期、workspace 配置错误 |
| Build Admin 前端 | `pnpm --filter @waterfun/admin build` | TypeScript 类型错、Vite 配置错、import 路径错 |
| Build Web Client | `pnpm --filter @waterfun/web-client build` | Nuxt 构建错误、SSR 兼容问题 |
| Build 后端 JAR | `gradlew.bat build -x test` | Java 编译错、依赖冲突、MapStruct 未生成代码 |
| Build & Push Docker 镜像 | `docker build`（不 push） | Dockerfile 语法错、文件缺失、COPY 路径错 |

> **原则**：能在本地提前发现的问题，绝不等到 CI 才暴露。

---

## 二、前置检查

```powershell
# 2.1 检查必备工具
java --version              # 需要 Java 22+
node --version              # 需要 >= 20.19.0
pnpm --version              # 需要 10.7.1
docker --version            # 可选（测 Docker 构建时需要）
```

**如果缺失**：

| 工具 | 安装方式 |
|------|---------|
| Java 22 | 项目根 `gradlew.bat` 会自动下载 Gradle 分发的 JDK，或手动安装 Adoptium Temurin 22 |
| pnpm | `corepack enable && corepack prepare pnpm@10.7.1 --activate`（Node 20+ 自带 corepack） |
| Docker | Docker Desktop for Windows |

### 2.2 检查密钥文件

Docker 构建后端镜像时需要 RSA 密钥对，确认存在：

```powershell
Test-Path deploy\keys\private.key    # 需要 True
Test-Path deploy\keys\public.key     # 需要 True
```

如果缺失，生成：

```powershell
mkdir -p deploy\keys
# 需要 OpenSSL（Git Bash 或 WSL 中执行）
openssl genpkey -algorithm RSA -out deploy/keys/private.key -pkeyopt rsa_keygen_bits:2048
openssl rsa -pubout -in deploy/keys/private.key -out deploy/keys/public.key
```

### 2.3 检查 Git 工作区

```powershell
git status
```

确保没有未提交的、不应该存在的文件混入构建。

---

## 三、后端构建测试

**CI 执行的是 `./gradlew build -x test`，本地完全一致：**

```powershell
# 3.1 编译 + 打包 JAR（跳过 test，因为几乎没有测试）
gradlew.bat build -x test
```

**预期结果：**

```
BUILD SUCCESSFUL in 14s
5 actionable tasks: 5 executed
```

编译会依次构建 `waterfun-common-lib` → `waterfun-service-core` → 各 boot 模块。

**验证产出物：**

```powershell
# 检查生成的 JAR 文件
Get-ChildItem -Path waterfun-*\build\libs\*.jar
```

应该看到：

- `waterfun-gateway/build/libs/waterfun-gateway-*.jar`
- `waterfun-service/build/libs/waterfun-service-*.jar`
- `waterfun-admin-service/build/libs/waterfun-admin-service-*.jar`

### 只测试单个模块

如果只改了某个模块，可以只构建它，更快：

```powershell
gradlew.bat :waterfun-gateway:build -x test
gradlew.bat :waterfun-service:build -x test
gradlew.bat :waterfun-admin-service:build -x test
```

### 编译错误常见原因

| 错误 | 原因 | 修复 |
|------|------|------|
| `MapStruct` 未生成实现类 | 未加 `annotationProcessor` 依赖或 `-parameters` 编译参数 | 检查 `build.gradle` |
| `找不到符号` | import 漏了、或依赖模块未先构建 | 跑一次完整 `gradlew.bat build` |
| `程序包 xxx 不存在` | 本地 Gradle 缓存损坏 | `gradlew.bat clean` 再试 |
| 枚举 `fromValue` 抛异常 | 新加枚举值忘加 `fromValue` 映射 | 检查枚举类 |

---

## 四、前端构建测试

### 4.1 安装依赖

```powershell
pnpm install --frozen-lockfile
```

> 如果失败，说明 `pnpm-lock.yaml` 与 `package.json` 不一致。提交前必须：
> ```powershell
> pnpm install  # 生成新 lockfile
> # 然后提交 pnpm-lock.yaml
> ```

### 4.2 构建 Admin 后台

```powershell
pnpm --filter @waterfun/admin build
```

**预期：** Vite 构建成功，产出在 `waterfun-admin/dist/`。

### 4.3 构建 Web Client 论坛

```powershell
pnpm --filter @waterfun/web-client build
```

**预期：** Nuxt 构建成功，产出在 `waterfun-web-client/.output/`。

### 前端构建错误常见原因

| 错误 | 原因 | 修复 |
|------|------|------|
| `Module not found: Can't resolve` | import 路径错或依赖未装 | 检查路径大小写、`pnpm install` |
| `Type 'X' has no property 'Y'` | TypeScript 类型定义不匹配 | 检查 `waterfun-web-core/src/types/` |
| `VITE_AUTH_SKIP_LIST` 未定义 | `.env.production` 缺变量 | 检查环境变量文件 |

---

## 五、Docker 镜像构建测试

> ⚠ **需要 Docker Desktop 正在运行**（右下角绿图标）。
>
> 如果报错 `failed to connect to the docker API`，先启动 Docker Desktop。

### 5.1 构建后端镜像（统一 Dockerfile，multi-target）

```powershell
# 在项目根目录执行
docker build --target gateway      -t waterfun-gateway:test      .
docker build --target user-service -t waterfun-service:test      .
docker build --target admin-service -t waterfun-admin-service:test .
```

每个 target 共用同一个 build stage，一次编译全部模块，运行时只提取对应 JAR：

```
=> [build 7/7] RUN ./gradlew build -x test --no-daemon
=> [gateway 1/4] COPY --from=build .../waterfun-gateway/build/libs/*.jar app.jar
=> exporting to image
=> => naming to docker.io/library/waterfun-gateway:test
```

> 首次构建需要下载 Gradle 9.2.1 和项目依赖（约 2-5 分钟），后续只重编有变动的模块。

### 5.2 构建前端

```powershell
docker build -f deploy\docker\Dockerfile.web-client -t waterfun-web-client:test  .
```

### 5.3 或用 Compose 构建（更接近生产）

```powershell
docker compose -f deploy\docker\docker-compose.app.yml build
```

这会按 `docker-compose.app.yml` 的定义构建 `gateway`、`user-service`、`admin-service`、`web-client` 四个镜像。

### 5.3 清理测试镜像

```powershell
docker rmi waterfun-gateway:test waterfun-service:test waterfun-admin-service:test waterfun-web-client:test
```

### Docker build 失败常见原因

| 错误 | 原因 | 修复 |
|------|------|------|
| `Unable to access jarfile gradle-wrapper.jar` | Dockerfile 用 `./gradlew` 但 jar 不在 context 中 | 改用 `gradle:jdk22` 镜像 + `gradle` 命令（已修复） |
| `COPY failed: file not found` | Dockerfile 中 COPY 路径不对，或 .dockerignore 拦了 | 检查 COPY 源路径是否在 context 中 |
| `Could not resolve dependencies` | Gradle 依赖下载超时（网络问题） | 重试，或配置国内 Gradle mirror |
| `Cannot run program "node"` | Web Client Dockerfile 中 pnpm 找不到 node | 检查 base image 是否包含 Node |

---

## 六、全栈本地运行（可选）

> 适合验证**多个服务的交互**（如 Gateway → UserService 的 JWT 传参链路）。
> 需要 Docker Desktop 运行中。

### 6.1 启动基础设施

```powershell
# 创建 Docker 网络（只需一次）
docker network create waterfun-net

# 启动 MySQL + Redis + RabbitMQ
docker compose -f deploy\docker\docker-compose.dev.yml up -d

# 验证容器健康
docker ps
# 应看到: waterfun-mysql (healthy), waterfun-redis (healthy), waterfun-rabbitmq
```

### 6.2 生成 RSA 密钥（如果还没有）

```powershell
# Git Bash 或 WSL 中执行
mkdir -p deploy/keys
openssl genpkey -algorithm RSA -out deploy/keys/private.key -pkeyopt rsa_keygen_bits:2048
openssl rsa -pubout -in deploy/keys/private.key -out deploy/keys/public.key
```

### 6.3 构建并启动所有服务

```powershell
docker compose -f deploy\docker\docker-compose.app.yml build
docker compose -f deploy\docker\docker-compose.app.yml up -d
```

### 6.4 健康检查

```powershell
# Gateway 健康检查
curl -s http://localhost:8080/actuator/health

# User Service 健康检查
curl -s http://localhost:8081/actuator/health

# Public API（不需要认证）
curl -s http://localhost:8080/api/public/banners

# 验证 Gateway 返回 JSON 而非 404
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/auth/login-by-password
# 预期: 405（Method Not Allowed，说明路由正常）
```

### 6.5 查看日志

```powershell
docker compose -f deploy\docker\docker-compose.app.yml logs -f --tail=50
```

### 6.6 停止

```powershell
docker compose -f deploy\docker\docker-compose.app.yml down
docker compose -f deploy\docker\docker-compose.dev.yml down
```

---

## 七、修改 Dockerfile 后专项检查

修改了 `Dockerfile` 或 `deploy/docker/Dockerfile.web-client` 后，除了跑一次构建，还需要检查：

### 7.1 文件上下文检查

后端统一 `Dockerfile` 的 build context 是项目根目录（`.`），确认 COPY 的源文件都存在：

```
settings.gradle / build.gradle                 (√)
gradle/wrapper/gradle-wrapper.jar              (√)
waterfun-common-lib/                           (√)
waterfun-service-core/build.gradle              (√)
waterfun-gateway/build.gradle + waterfun-gateway/src/    (√)
waterfun-service/build.gradle + waterfun-service/src/    (√)
waterfun-admin-service/build.gradle + waterfun-admin-service/src/  (√)
```

**Dockerfile.web-client** 需要本地存在：
```
pnpm-lock.yaml / pnpm-workspace.yaml / package.json (√)
waterfun-web-core/ (√)
waterfun-web-client/ (√)
```

### 7.2 分层缓存合理性

检查是否有不必要的 COPY 导致缓存失效：

```
# 好的顺序（已有，不要改）
COPY gradlew settings.gradle build.gradle ./    ← 很少变
COPY gradle/ ./gradle/                          ← 很少变
COPY *build.gradle ...                          ← 依赖声明显式拷贝，命中缓存
RUN ./gradlew dependencies                      ← 依赖声明不变就命中缓存
COPY waterfun-*/ waterfun-*/                    ← 源码变动才会重编
RUN ./gradlew build -x test                     ← 重编改动的模块
```

### 7.3 Runtime 镜像基础

确认 runtime stage 没有用 `-slim`、`-alpine` 以外的过大镜像：

| 当前 | 评价 |
|------|------|
| `eclipse-temurin:22-jre` | ✅ JRE 镜像，200MB，不含编译器 |
| `node:22-alpine` | ✅ 45MB，合理 |
| `nginx:1.27-alpine` | ✅ 合理 |

---

## 八、常见问题

### Docker 无法连接

```
ERROR: failed to connect to the docker API at npipe:////./pipe/dockerDesktopLinuxEngine
```

**原因：** Docker Desktop 未运行。

**解决：** 开始菜单 → Docker Desktop 启动，等右下角变绿。

### 端口冲突

```
Error response from daemon: Ports are not available: exposing port TCP 0.0.0.0:3306
```

**原因：** 本地已有 MySQL/Redis 占用了端口（可能是 WSL 或系统服务）。

**解决方法（任选其一）：**

1. 停掉本地服务
2. 或改 `docker-compose.dev.yml` 的端口映射，比如 `3307:3306`
3. 或只在 WSL2 里跑 Docker（绕过 Windows 端口占用）

### 磁盘空间不足

Docker 构建会缓存多层镜像，长期累积可能占几十 GB。定期清理：

```powershell
# 清理未使用的镜像、容器、构建缓存
docker system prune -a --volumes
```

### 跨平台换行符

Dockerfile 如果 git 换行符设为了 `CRLF`，Linux 容器可能报错：

```
exec: no such command: ...
```

**解决：** 确保 `deploy/docker/Dockerfile*` 在 git 中为 `LF`：

```powershell
git config core.autocrlf input
# 或提交前手动确认 Dockerfile 不是 CRLF
```

---

## 附录：提交前快速检查清单

```powershell
# 1. 后端编译
gradlew.bat build -x test

# 2. 前端依赖 + 构建
pnpm install --frozen-lockfile
pnpm --filter @waterfun/admin build
pnpm --filter @waterfun/web-client build

# 3. Docker 构建（需要 Docker Desktop）
docker build --target gateway -t waterfun-gateway:test .
docker build -f deploy/docker/Dockerfile.web-client -t waterfun-web-client:test .

# 全部通过 → git push
```
