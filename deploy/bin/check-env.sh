#!/bin/bash
# ============================================================
# WaterFun 环境配置检查 (Bash — Linux/macOS)
# 检查所有 ${VAR} 是否有来源（环境变量 or common-dev-secrets.yml）
# 无 :default 的三方变量必须配置，否则启动会失败
#
# Usage:
#   bash deploy/bin/check-env.sh
# ============================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SECRETS_FILE="$SCRIPT_DIR/../config/common-dev-secrets.yml"

# ---------- 颜色 ----------
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
GRAY='\033[0;90m'
WHITE='\033[1;37m'
NC='\033[0m'

# ---------- 1. 加载 common-dev-secrets.yml ----------
declare -A secrets
if [ -f "$SECRETS_FILE" ]; then
    while IFS=: read -r key value; do
        key="$(echo "$key" | tr -d '[:space:]')"
        value="$(echo "$value" | sed 's/^[[:space:]]*//')"
        if echo "$key" | grep -qE '^[A-Z][A-Z_0-9]+$'; then
            secrets["$key"]="$value"
        fi
    done < <(grep -E '^[A-Z][A-Z_0-9]+:' "$SECRETS_FILE" 2>/dev/null || true)
    echo -e "${CYAN}[OK]${NC} Loaded common-dev-secrets.yml"
else
    echo -e "${YELLOW}[..]${NC} common-dev-secrets.yml not found (third-party services may be affected)"
fi
echo ""

# ---------- 2. 定义所有变量 ----------
# category: required / default / optional
VARS_DEF=(
    # Database (infra, has :default)
    "DB_URL:default:MySQL connection URL"
    "DB_USERNAME:default:MySQL username"
    "DB_PASSWORD:default:MySQL password"

    # Redis (infra, has :default)
    "REDIS_HOST:default:Redis host"
    "REDIS_PASSWORD:default:Redis password"

    # RabbitMQ (infra, has :default)
    "RABBITMQ_HOST:default:RabbitMQ host"
    "RABBITMQ_USERNAME:default:RabbitMQ username"
    "RABBITMQ_PASSWORD:default:RabbitMQ password"

    # JWT
    "JWT_PRIVATE_KEY:default:RSA private key path"

    # Device (required, no :default)
    "DEVICE_SALT:required:Device fingerprint salt"

    # Spring Security
    "SECURITY_USER_PASSWORD:default:Actuator security password"

    # Mail - SMTP (required, no :default)
    "MAIL_HOST:required:SMTP server"
    "MAIL_PORT:required:SMTP port"
    "MAIL_USERNAME:required:SMTP account"
    "MAIL_PASSWORD:required:SMTP password / auth code"

    # Support email (required, no :default)
    "SUPPORT_EMAIL:required:Support email address"

    # COS (required, no :default)
    "TENCENT_COS_BUCKET:required:COS bucket name"

    # Aliyun SMS (required, no :default)
    "ALIYUN_SMS_SIGN_NAME:required:SMS sign name"
    "ALIYUN_SMS_VERIFY_CODE_TEMPLATE:required:SMS template code"

    # Resend (required, no :default)
    "RESEND_API_KEY:required:Resend API Key"

    # Gateway/Service ports (application.yml has :default)
    "WATERFUN_GATEWAY_PORT:default:Gateway port"
    "WATERFUN_SERVICE_PORT:default:User service port"
    "WATERFUN_ADMIN_SERVICE_PORT:default:Admin service port"
    "WATERFUN_USER_SERVICE_URI:default:User service URI (gateway proxy)"
    "WATERFUN_ADMIN_SERVICE_URI:default:Admin service URI (gateway proxy)"

    # Optional (read via System.getenv())
    "TENCENTCLOUD_SECRET_ID:optional:Tencent Cloud API secret ID"
    "TENCENTCLOUD_SECRET_KEY:optional:Tencent Cloud API secret key"
    "WATERFUN_KEK:optional:Key encryption key"
)

# ---------- 3. 检查 ----------
pass_count=0
warn_count=0
fail_count=0
fail_list=()

echo -e "${WHITE}Result:${NC}"
echo "============================================================"

IFS_SAVE="$IFS"
for entry in "${VARS_DEF[@]}"; do
    IFS=':' read -r name category desc <<< "$entry"
    IFS="$IFS_SAVE"

    env_val="${!name:-}"
    secrets_val="${secrets[$name]:-}"

    if [ -n "$env_val" ]; then
        echo -e "  ${GREEN}[PASS]${NC} $name (from env var)"
        pass_count=$((pass_count + 1))
    elif [ -n "$secrets_val" ]; then
        echo -e "  ${YELLOW}[WARN]${NC} $name (from common-dev-secrets.yml)"
        warn_count=$((warn_count + 1))
    elif [ "$category" = "default" ]; then
        echo -e "  ${GRAY}[PASS]${NC} $name (uses common.yml :default)"
        pass_count=$((pass_count + 1))
    elif [ "$category" = "optional" ]; then
        echo -e "  ${GRAY}[PASS]${NC} $name (optional, not required)"
        pass_count=$((pass_count + 1))
    else
        echo -e "  ${RED}[FAIL]${NC} $name -- $desc"
        fail_count=$((fail_count + 1))
        fail_list+=("$name -- $desc")
    fi
done

echo "============================================================"

# ---------- 4. 总结 ----------
echo ""
echo -e "${WHITE}=== Summary ===${NC}"
echo -e "  Pass: $pass_count | From secrets file: $warn_count | Missing: $fail_count"

if [ "$fail_count" -eq 0 ]; then
    echo ""
    echo -e "  ${GREEN}[OK] All required environment variables are configured.${NC}"
    if [ "$warn_count" -gt 0 ]; then
        echo -e "  ${YELLOW}[WARN] $warn_count variable(s) come from common-dev-secrets.yml.${NC}"
        echo -e "  ${YELLOW}        For production, set them as system environment variables instead.${NC}"
    fi
else
    echo ""
    echo -e "  ${RED}[FAIL] The following $fail_count variable(s) have no source:${NC}"
    for item in "${fail_list[@]}"; do
        echo -e "    ${RED}  - $item${NC}"
    done
    echo ""
    echo -e "  ${CYAN}[DEV] Add them to deploy/config/common-dev-secrets.yml${NC}"
    echo -e "  ${CYAN}[PROD] Inject them as system environment variables${NC}"
    echo ""
    echo -e "  ${CYAN}See deploy/env-vars-reference.md for details.${NC}"
    exit 1
fi

# ---------- 5. Check ip2region data ----------
echo ""
echo -e "${WHITE}=== ip2region ===${NC}"
IP2REGION_FILE="$SCRIPT_DIR/../utils/ip2region_v4.xdb"
if [ -f "$IP2REGION_FILE" ]; then
    SIZE=$(stat -c%s "$IP2REGION_FILE" 2>/dev/null || stat -f%z "$IP2REGION_FILE" 2>/dev/null)
    SIZE_HR=$(numfmt --to=iec "$SIZE" 2>/dev/null || echo "$SIZE bytes")
    echo -e "  ${GREEN}✅ ip2region_v4.xdb ($SIZE_HR)${NC}"
else
    echo -e "  ${YELLOW}❌ ip2region_v4.xdb not found${NC}"
    echo -e "     ${CYAN}→ Run: bash deploy/bin/setup-ip2region.sh${NC}"
    echo -e "     ${CYAN}Or manually download to deploy/utils/ip2region_v4.xdb${NC}"
fi
