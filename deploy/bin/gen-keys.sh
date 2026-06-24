#!/bin/bash
# ============================================================
# WaterFun RSA Key Generation Script (Bash — Linux/macOS)
# Generates an RSA 2048 key pair for JWT signing (RS256).
#
# Usage:
#   bash deploy/scripts/gen-keys.sh
#
# Output:
#   deploy/keys/private.key    — Private key (签名用)
#   deploy/keys/public.key     — Public key (验证用)
# ============================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
DEPLOY_KEY_DIR="$SCRIPT_DIR/../keys"

mkdir -p "$DEPLOY_KEY_DIR"

echo "Generating RSA 2048-bit key pair..."
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 \
    -out "$DEPLOY_KEY_DIR/private.key"

openssl rsa -pubout -in "$DEPLOY_KEY_DIR/private.key" \
    -out "$DEPLOY_KEY_DIR/public.key"

echo "Key pair generated successfully!"
echo "  Private: $DEPLOY_KEY_DIR/private.key"
echo "  Public : $DEPLOY_KEY_DIR/public.key"
echo ""
echo "Next step: set JWT_PRIVATE_KEY env var"
echo '  export JWT_PRIVATE_KEY="file:./deploy/keys/private.key"'
