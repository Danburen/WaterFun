<#
.SYNOPSIS
    Download ip2region IP geolocation database if not present.
.DESCRIPTION
    Checks for ip2region_v4.xdb in deploy/utils/ and downloads it
    from the official ip2region repository if missing.
.PARAMETER OutputDir
    Target directory (default: ../utils relative to script location).
.PARAMETER Url
    Download URL for the ip2region xdb file.
.PARAMETER Filename
    Output filename (default: ip2region_v4.xdb).
.EXAMPLE
    .\deploy\bin\setup-ip2region.ps1
#>

param(
    [string]$OutputDir = "$PSScriptRoot/../utils",
    [string]$Url = "https://raw.githubusercontent.com/lionsoul2014/ip2region/master/data/ip2region_v4.xdb",
    [string]$Filename = "ip2region_v4.xdb",
    [switch]$Force
)

# Resolve to absolute path
$OutputFile = Resolve-Path (Join-Path $OutputDir $Filename) -ErrorAction SilentlyContinue
if (-not $OutputFile) {
    $dir = Resolve-Path $OutputDir -ErrorAction SilentlyContinue
    if (-not $dir) {
        $dir = New-Item -ItemType Directory -Path $OutputDir -Force
    }
    $OutputFile = Join-Path $dir $Filename
}

# Check if already exists
if ((Test-Path $OutputFile) -and -not $Force) {
    $size = (Get-Item $OutputFile).Length
    Write-Host "✅ ip2region data already exists at $OutputFile ($($size.ToString('N0')) bytes)"
    Write-Host "   Use -Force to re-download."
    exit 0
}

Write-Host "⬇️  Downloading ip2region data from:" -NoNewline
Write-Host " $Url" -ForegroundColor Cyan
Write-Host "   → $OutputFile"

try {
    # Ensure parent directory exists
    $parent = Split-Path $OutputFile -Parent
    if (-not (Test-Path $parent)) {
        New-Item -ItemType Directory -Path $parent -Force | Out-Null
    }

    # Download with progress bar
    $response = Invoke-WebRequest -Uri $Url -OutFile $OutputFile -UseBasicParsing -PassThru
    $size = (Get-Item $OutputFile).Length

    if ($size -gt 100KB) {
        Write-Host "✅ Downloaded $($size.ToString('N0')) bytes" -ForegroundColor Green
    } else {
        Write-Host "⚠️  File seems too small ($($size.ToString('N0')) bytes), URL may be incorrect." -ForegroundColor Yellow
    }
} catch {
    Write-Host "❌ Download failed: $_" -ForegroundColor Red
    if (Test-Path $OutputFile) { Remove-Item $OutputFile -Force }
    exit 1
}
