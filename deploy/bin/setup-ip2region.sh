#!/usr/bin/env bash
# shellcheck disable=SC2016
#
# setup-ip2region.sh — Download ip2region IP geolocation database if not present.
#
# Usage:
#   ./deploy/bin/setup-ip2region.sh
#
# Downloads ip2region_v4.xdb to deploy/utils/ from the official repo.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
OUTPUT_DIR="${OUTPUT_DIR:-"$SCRIPT_DIR/../utils"}"
URL="${URL:-https://raw.githubusercontent.com/lionsoul2014/ip2region/master/data/ip2region.xdb}"
FILENAME="${FILENAME:-ip2region_v4.xdb}"
FORCE="${FORCE:-false}"

OUTPUT_FILE="$OUTPUT_DIR/$FILENAME"

# --- Check if already exists ---
if [ -f "$OUTPUT_FILE" ] && [ "${FORCE}" != "true" ]; then
    SIZE=$(stat -c%s "$OUTPUT_FILE" 2>/dev/null || stat -f%z "$OUTPUT_FILE" 2>/dev/null)
    echo "✅ ip2region data already exists at $OUTPUT_FILE ($(numfmt --to=iec "$SIZE" 2>/dev/null || echo "$SIZE bytes"))"
    echo "   Set FORCE=true to re-download."
    exit 0
fi

echo "⬇️  Downloading ip2region data from:"
echo "   $URL"
echo "   → $OUTPUT_FILE"

# Ensure target directory exists
mkdir -p "$OUTPUT_DIR"

# Download with curl or wget
if command -v curl &>/dev/null; then
    curl -fsSL -o "$OUTPUT_FILE" "$URL" --connect-timeout 10 --max-time 60
elif command -v wget &>/dev/null; then
    wget -q -O "$OUTPUT_FILE" "$URL" --timeout=30
else
    echo "❌ Neither curl nor wget found. Install one of them and retry." >&2
    exit 1
fi

# Verify
if [ -f "$OUTPUT_FILE" ] && [ -s "$OUTPUT_FILE" ]; then
    SIZE=$(stat -c%s "$OUTPUT_FILE" 2>/dev/null || stat -f%z "$OUTPUT_FILE" 2>/dev/null)
    echo "✅ Downloaded $(numfmt --to=iec "$SIZE" 2>/dev/null || echo "$SIZE bytes")"
else
    echo "❌ Download failed or empty file" >&2
    rm -f "$OUTPUT_FILE"
    exit 1
fi
