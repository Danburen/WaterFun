# ============================================================
# WaterFun environment config checker (PowerShell)
# Check all ${VAR} have a source (env var or common-dev-secrets.yml)
# Variables without :default MUST be configured, or startup fails.
#
# Usage:
#   .\deploy\bin\check-env.ps1
# ============================================================

$ErrorActionPreference = 'Stop'

# ---------- 0. Load .env file (Docker Compose deployment) ----------
$searchDirs = @(
    (Get-Location).Path,
    $PSScriptRoot,
    (Join-Path $PSScriptRoot "..\..")
)

$envLoaded = $false
foreach ($dir in $searchDirs) {
    $candidate = Join-Path (Resolve-Path $dir -ErrorAction SilentlyContinue) ".env"
    if ($candidate -and (Test-Path -LiteralPath $candidate -PathType Leaf)) {
        Get-Content -LiteralPath $candidate | ForEach-Object {
            if ($_ -match '^\s*([A-Z][A-Z_0-9]+)\s*=\s*(.*?)\s*$') {
                $key = $matches[1]
                $value = $matches[2] -replace '^["'']|["'']$', ''   # strip surrounding quotes
                [Environment]::SetEnvironmentVariable($key, $value, "Process")
            }
        }
        Write-Host "[OK] Loaded $candidate" -ForegroundColor Cyan
        $envLoaded = $true
        break
    }
}
if (-not $envLoaded) {
    Write-Host "[..] No .env found (using system env vars only)" -ForegroundColor DarkYellow
}
Write-Host ""

# ---------- 1. Load common-dev-secrets.yml ----------
$secretsPath = Join-Path $PSScriptRoot "..\config\common-dev-secrets.yml"
$secrets = @{}
if (Test-Path -LiteralPath $secretsPath) {
    Get-Content -LiteralPath $secretsPath | ForEach-Object {
        if ($_ -match '^([A-Z][A-Z_0-9]+)\s*:\s*(.*)$') {
            $secrets[$matches[1]] = $matches[2].Trim()
        }
    }
    Write-Host "[OK] Loaded common-dev-secrets.yml" -ForegroundColor Cyan
} else {
    Write-Host "[..] common-dev-secrets.yml not found (third-party services may be affected)" -ForegroundColor DarkYellow
}
Write-Host ""

# ---------- 2. Define all variables ----------
# category: required = no :default (fails if missing)
#           default = has :default (production only concern)
#           optional = read by System.getenv() (not mandatory)
$vars = @(
    # ---- Database (infra, has :default) ----
    @{Name="DB_URL";               Category="default";    Desc="MySQL connection URL"}
    @{Name="DB_USERNAME";          Category="default";    Desc="MySQL username"}
    @{Name="DB_PASSWORD";          Category="default";    Desc="MySQL password"}

    # ---- Redis (infra, has :default) ----
    @{Name="REDIS_HOST";           Category="default";    Desc="Redis host"}
    @{Name="REDIS_PASSWORD";       Category="default";    Desc="Redis password"}

    # ---- RabbitMQ (infra, has :default) ----
    @{Name="RABBITMQ_HOST";        Category="default";    Desc="RabbitMQ host"}
    @{Name="RABBITMQ_USERNAME";    Category="default";    Desc="RabbitMQ username"}
    @{Name="RABBITMQ_PASSWORD";    Category="default";    Desc="RabbitMQ password"}

    # ---- JWT ----
    @{Name="JWT_PRIVATE_KEY";      Category="default";    Desc="RSA private key path"}

    # ---- Device fingerprint (required, no :default) ----
    @{Name="DEVICE_SALT";          Category="required";    Desc="Device fingerprint salt"}

    # ---- Spring Security ----
    @{Name="SECURITY_USER_PASSWORD"; Category="default";  Desc="Actuator security password"}

    # ---- Mail - SMTP (required, no :default) ----
    @{Name="MAIL_HOST";            Category="required";   Desc="SMTP server"}
    @{Name="MAIL_PORT";            Category="required";   Desc="SMTP port"}
    @{Name="MAIL_USERNAME";        Category="required";   Desc="SMTP account"}
    @{Name="MAIL_PASSWORD";        Category="required";   Desc="SMTP password / auth code"}

    # ---- Support email (required, no :default) ----
    @{Name="SUPPORT_EMAIL";        Category="required";   Desc="Support email address"}

    # ---- Object storage - COS (required, no :default) ----
    @{Name="TENCENT_COS_BUCKET";   Category="required";   Desc="COS bucket name"}

    # ---- SMS - Aliyun (required, no :default) ----
    @{Name="ALIYUN_SMS_SIGN_NAME";             Category="required"; Desc="SMS sign name"}
    @{Name="ALIYUN_SMS_VERIFY_CODE_TEMPLATE";  Category="required"; Desc="SMS template code"}

    # ---- Notification - Resend (required, no :default) ----
    @{Name="RESEND_API_KEY";       Category="required";   Desc="Resend API Key"}

    # ---- Gateway/service ports (application.yml has :default) ----
    @{Name="WATERFUN_GATEWAY_PORT";       Category="default"; Desc="Gateway port"}
    @{Name="WATERFUN_SERVICE_PORT";       Category="default"; Desc="User service port"}
    @{Name="WATERFUN_ADMIN_SERVICE_PORT"; Category="default"; Desc="Admin service port"}
    @{Name="WATERFUN_USER_SERVICE_URI";   Category="default"; Desc="User service URI (gateway proxy)"}
    @{Name="WATERFUN_ADMIN_SERVICE_URI";  Category="default"; Desc="Admin service URI (gateway proxy)"}

    # ---- Optional (read by System.getenv()) ----
    @{Name="TENCENTCLOUD_SECRET_ID";    Category="optional"; Desc="Tencent Cloud API secret ID"}
    @{Name="TENCENTCLOUD_SECRET_KEY";    Category="optional"; Desc="Tencent Cloud API secret key"}
    @{Name="WATERFUN_KEK";              Category="optional"; Desc="Key encryption key"}
)

# ---------- 3. Check ----------
$passCount = 0
$warnCount = 0
$failCount = 0
$failList = @()

Write-Host "Result:" -ForegroundColor White
Write-Host ("=" * 60) -ForegroundColor DarkGray

foreach ($v in $vars) {
    $name = $v.Name
    $envVal = [Environment]::GetEnvironmentVariable($name, "Process")
    $secretsVal = $secrets[$name]

    if ($envVal) {
        Write-Host "  [PASS] $name (from env var)" -ForegroundColor Green
        $passCount++
    } elseif ($secretsVal) {
        Write-Host "  [WARN] $name (from common-dev-secrets.yml)" -ForegroundColor Yellow
        $warnCount++
    } elseif ($v.Category -eq "default") {
        Write-Host "  [PASS] $name (uses common.yml :default)" -ForegroundColor DarkGray
        $passCount++
    } elseif ($v.Category -eq "optional") {
        Write-Host "  [PASS] $name (optional, not required)" -ForegroundColor DarkGray
        $passCount++
    } else {
        Write-Host "  [FAIL] $name -- $($v.Desc)" -ForegroundColor Red
        $failCount++
        $failList += "$name -- $($v.Desc)"
    }
}

Write-Host ("=" * 60) -ForegroundColor DarkGray

# ---------- 4. Summary ----------
Write-Host ""
Write-Host "=== Summary ===" -ForegroundColor White
Write-Host "  Pass: $passCount | From secrets file: $warnCount | Missing: $failCount" -ForegroundColor White

if ($failCount -eq 0) {
    Write-Host ""
    Write-Host "  [OK] All required environment variables are configured." -ForegroundColor Green
    if ($warnCount -gt 0) {
        Write-Host "  [WARN] $warnCount variable(s) come from common-dev-secrets.yml." -ForegroundColor Yellow
        Write-Host "        For production, set them as system environment variables instead." -ForegroundColor Yellow
    }
} else {
    Write-Host ""
    Write-Host "  [FAIL] The following $failCount variable(s) have no source:" -ForegroundColor Red
    foreach ($item in $failList) {
        Write-Host "         - $item" -ForegroundColor Red
    }
    Write-Host ""
    Write-Host "  [DEV] Add them to deploy/config/common-dev-secrets.yml" -ForegroundColor Cyan
    Write-Host "  [PROD] Inject them as system environment variables" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "  See deploy/env-vars-reference.md for details." -ForegroundColor Cyan
    exit 1
}

# ---------- 5. Check ip2region data ----------
Write-Host ""
Write-Host "=== ip2region ===" -ForegroundColor White
$ip2regionPath = Resolve-Path "$PSScriptRoot/../utils/ip2region_v4.xdb" -ErrorAction SilentlyContinue
if ($ip2regionPath -and (Test-Path $ip2regionPath)) {
    $size = (Get-Item $ip2regionPath).Length
    Write-Host "  ✅ ip2region_v4.xdb ($($size.ToString('N0')) bytes)" -ForegroundColor Green
} else {
    Write-Host "  ❌ ip2region_v4.xdb not found" -ForegroundColor Yellow
    Write-Host "     → Run: .\deploy\bin\setup-ip2region.ps1 for windows or setup-ip2region.sh for linux" -ForegroundColor Cyan
    Write-Host "     Or manually download to deploy/utils/ip2region_v4.xdb" -ForegroundColor Cyan
}

if ($host.Name -eq "ConsoleHost") {
    Write-Host ""
    Read-Host "Press Enter to exit"
}