# WaterFun MySQL Auto Backup Script
# Save as backup_mysql.ps1

# Config
$MYSQL_BIN = "E:\Program Files\MySQL\MySQL Server 8.0\bin"
$BACKUP_DIR = "D:\backup\waterfun"
$DB_HOST = "localhost"
$DB_PORT = "3306"
$DB_USER = "root"
$DB_PASS = "123456"
$DB_NAME = "waterfun"
$RETAIN_DAYS = 30

# Generate date suffix
$DATE_SUFFIX = Get-Date -Format "yyyyMMdd_HHmmss"
$BACKUP_FILE = "$BACKUP_DIR\${DB_NAME}_${DATE_SUFFIX}.sql"
$LOG_FILE = "$BACKUP_DIR\backup_log.txt"

# Create backup directory
if (!(Test-Path $BACKUP_DIR)) {
    New-Item -ItemType Directory -Path $BACKUP_DIR -Force | Out-Null
}

# Check mysqldump exists
$mysqldump = "$MYSQL_BIN\mysqldump.exe"
if (!(Test-Path $mysqldump)) {
    $msg = "mysqldump not found: $mysqldump"
    Write-Host $msg -ForegroundColor Red
    $msg | Out-File -Append -FilePath $LOG_FILE
    pause
    exit 1
}

# Execute backup
$timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
$startMsg = "[$timestamp] Start backup $DB_NAME..."
Write-Host $startMsg
$startMsg | Out-File -Append -FilePath $LOG_FILE

# Build arguments (password without space after -p)
$argList = @(
    "-h$DB_HOST",
    "-P$DB_PORT",
    "-u$DB_USER",
    "-p$DB_PASS",
    "--single-transaction",
    "--routines",
    "--triggers",
    "--default-character-set=utf8mb4",
    $DB_NAME
)

try {
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = $mysqldump
    $psi.Arguments = $argList -join " "
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow = $true

    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $psi
    $process.Start() | Out-Null

    $output = $process.StandardOutput.ReadToEnd()
    $errorOutput = $process.StandardError.ReadToEnd()
    $process.WaitForExit()

    if ($process.ExitCode -ne 0) {
        throw "mysqldump exit code: $($process.ExitCode), Error: $errorOutput"
    }

    # Save output
    [System.IO.File]::WriteAllText($BACKUP_FILE, $output, [System.Text.UTF8Encoding]::new($false))

    # Compress
    $gzipFile = "$BACKUP_FILE.gz"
    $inputStream = [System.IO.File]::OpenRead($BACKUP_FILE)
    $outputStream = [System.IO.File]::Create($gzipFile)
    $gzipStream = New-Object System.IO.Compression.GzipStream($outputStream, [System.IO.Compression.CompressionMode]::Compress)
    $inputStream.CopyTo($gzipStream)
    $gzipStream.Close()
    $outputStream.Close()
    $inputStream.Close()
    Remove-Item $BACKUP_FILE
    $BACKUP_FILE = $gzipFile

    # Delete old backups
    $cutoffDate = (Get-Date).AddDays(-$RETAIN_DAYS)
    Get-ChildItem -Path $BACKUP_DIR -Filter "*.sql*" | Where-Object { $_.LastWriteTime -lt $cutoffDate } | Remove-Item -Force

    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $successMsg = "[$timestamp] Backup success: $BACKUP_FILE"
    Write-Host $successMsg -ForegroundColor Green
    $successMsg | Out-File -Append -FilePath $LOG_FILE

} catch {
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $failMsg = "[$timestamp] Backup failed: $_"
    Write-Host $failMsg -ForegroundColor Red
    $failMsg | Out-File -Append -FilePath $LOG_FILE
    Write-Host "Check log: $LOG_FILE" -ForegroundColor Yellow
}

pause
