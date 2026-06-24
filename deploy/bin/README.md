# WaterFun 可执行脚本

## 脚本清单

| 脚本 | 适用环境 | 用途 |
|------|---------|------|
| `check-env.ps1` | Windows | 检查所有 `${VAR}` 环境变量是否已配置 |
| `check-env.sh` | Linux / macOS | 同上 |
| `setup-ip2region.ps1` | Windows | 自动下载 ip2region IP 地理定位数据库 |
| `setup-ip2region.sh` | Linux / macOS | 同上 |
| `gen-keys.ps1` | Windows | 生成 RSA 2048 密钥对（JWT RS256 签名用） |
| `gen-keys.sh` | Linux / macOS | 同上 |
| `backup_mysql.ps1` | Windows | MySQL 全量备份 + gzip 压缩 + 自动清理旧备份 |
| `backup_mysql.sh` | Linux / macOS | 同上 |

所有脚本位于 `deploy/bin/`，从项目根目录执行。

---

## check-env — 环境配置检查

检查所有 `${VAR}` 是否有来源（系统环境变量 或 `common-dev-secrets.yml` 文件）。

### 用法

```powershell
# Windows
.\deploy\bin\check-env.ps1
```

```bash
# Linux / macOS
bash deploy/bin/check-env.sh
```

### 输出解读

| 状态 | 含义 |
|------|------|
| `[PASS]` 绿色 | 来自系统环境变量 |
| `[PASS]` 灰色 | 来自 `:default` 默认值，或为 optional（非必须） |
| `[WARN]` 黄色 | 来自 `common-dev-secrets.yml` 文件，环境变量未设 |
| `[FAIL]` 红色 | 无任何来源，启动将报错 |

---

## setup-ip2region — ip2region 数据库下载

自动下载 ip2region IP 地理定位数据库 xdb 文件到 `deploy/utils/`。

### 用法

```powershell
# Windows
.\deploy\bin\setup-ip2region.ps1
```

```bash
# Linux / macOS
bash deploy/bin/setup-ip2region.sh
```

文件已存在于 `deploy/utils/` 时自动跳过，避免重复下载。使用 `-Force` 参数强制重新下载。

### 下载源

默认从 `lionsoul2014/ip2region` 官方仓库下载。可通过环境变量 `URL` 覆盖：

```powershell
# Windows
$env:URL = "https://mirror.example.com/ip2region.xdb"
.\deploy\bin\setup-ip2region.ps1
```

```bash
# Linux / macOS
URL="https://mirror.example.com/ip2region.xdb" bash deploy/bin/setup-ip2region.sh
```

> xdb 文件被 `.gitignore` 排除，不会提交到 Git。新环境需运行此脚本。

---

## gen-keys — RSA 密钥对生成

为 JWT RS256 签名生成 RSA 2048 密钥对。

### 用法

```powershell
# Windows
.\deploy\bin\gen-keys.ps1
```

```bash
# Linux / macOS
bash deploy/bin/gen-keys.sh
```

### 输出

- `deploy/keys/private.key` — 私钥（签名用）
- `deploy/keys/public.key` — 公钥（验证用）

生成后需设置 `JWT_PRIVATE_KEY` 环境变量指向私钥文件。

---

## backup_mysql — MySQL 数据库备份

全量备份指定 MySQL 数据库，自动 gzip 压缩，保留最近 30 天备份。

### 用法

```powershell
# Windows
.\deploy\bin\backup_mysql.ps1
```

```bash
# Linux / macOS
chmod +x deploy/bin/backup_mysql.sh
./deploy/bin/backup_mysql.sh
```

### 配置

编辑脚本顶部配置区：

| 参数 | Windows 默认 | Linux 默认 |
|------|-------------|-----------|
| `MYSQL_BIN` | `E:\Program Files\MySQL\MySQL Server 8.0\bin` | `/usr/bin` |
| `BACKUP_DIR` | `D:\backup\waterfun` | `/data/backup/waterfun` |
| `DB_HOST` | `localhost` | `localhost` |
| `DB_USER` | `root` | `root` |
| `DB_PASS` | `123456` | `123456` |
| `RETAIN_DAYS` | `30` | `30` |

### 自动定时备份

**Windows（任务计划程序）：**
1. 打开 `taskschd.msc`
2. 创建基本任务 → 触发器选每天（凌晨 3:00）
3. 操作 → 启动程序 → `D:\Project\waterfun\deploy\bin\backup_mysql.ps1`

**Linux（crontab）：**
```bash
crontab -e
0 3 * * * /path/to/waterfun/deploy/bin/backup_mysql.sh >/dev/null 2>&1
```

### 恢复数据

```bash
# Windows
mysql -h localhost -u root -p123456 waterfun < D:\backup\waterfun\waterfun_20260619.sql

# Linux（压缩备份需先解压）
gunzip -c /data/backup/waterfun/waterfun_20260619.sql.gz | mysql -h localhost -u root -p123456 waterfun
```

---

## 注意事项

1. **首次使用前**检查脚本内的 MySQL 路径、用户名、密码是否匹配你的环境
2. **生产环境**建议用 `--defaults-extra-file` 管理数据库凭据，不要明文写密码
3. **前提条件**：
   - `check-env`：运行前确保已安装 Java + Spring Boot 项目已构建
   - `gen-keys`：需安装 OpenSSL（Windows 可用 Git Bash 自带的 `openssl`）
   - `backup_mysql`：需安装 MySQL `mysqldump` 工具
