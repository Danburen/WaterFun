# RSA 密钥对

本项目使用 RS256（RSA 2048）进行 JWT 签名和验证。

## 文件说明

| 文件 | 上 git | 说明 |
|------|--------|------|
| `private.key` | ❌ | JWT 签名私钥（不进入版本控制） |
| `public.key` | ❌ | JWT 验证公钥（不上传，需自行生成） |

## 生成密钥

```powershell
# Windows
.\deploy\bin\gen-keys.ps1
```

```bash
# Linux / macOS
bash deploy/bin/gen-keys.sh
```

## 环境变量配置

生成密钥后，需要设置环境变量：

```powershell
# PowerShell
$env:JWT_PRIVATE_KEY = "file:./deploy/keys/private.key"
```

```bash
# Linux / macOS
export JWT_PRIVATE_KEY="file:./deploy/keys/private.key"
```

> `common.yml` 中的 `jwt.public-key` 也需要设置环境变量 `JWT_PUBLIC_KEY`，或使用默认路径 `file:./deploy/keys/public.key`。
