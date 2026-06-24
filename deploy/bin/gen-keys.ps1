# ============================================================
# WaterFun RSA Key Generation Script (PowerShell)
# Generates an RSA 2048 key pair for JWT signing (RS256).
#
# Usage:
#   .\deploy\scripts\gen-keys.ps1
#
# Output:
#   deploy/keys/private.key    — Private key (签名用)
#   deploy/keys/public.key     — Public key (验证用)
# ============================================================

$DeployKeyDir = Resolve-Path "$PSScriptRoot\..\keys"

# Ensure target directory exists
if (-not (Test-Path -LiteralPath $DeployKeyDir)) {
    New-Item -ItemType Directory -Path $DeployKeyDir -Force | Out-Null
}

# Generate 2048-bit RSA private key
Write-Host "Generating RSA 2048-bit key pair..."
& openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 `
    -out "$DeployKeyDir\private.key" 2>&1 | Out-Null

# Extract public key from private key
& openssl rsa -pubout -in "$DeployKeyDir\private.key" `
    -out "$DeployKeyDir\public.key" 2>&1 | Out-Null

Write-Host "Key pair generated successfully!"
Write-Host "  Private: $DeployKeyDir\private.key"
Write-Host "  Public : $DeployKeyDir\public.key"
Write-Host ""
Write-Host "Next step: set JWT_PRIVATE_KEY env var"
Write-Host '  $env:JWT_PRIVATE_KEY = "file:./deploy/keys/private.key"'
