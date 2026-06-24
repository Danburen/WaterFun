#!/usr/bin/env bash
# ============================================================
# WaterFun MySQL 自动备份脚本（Linux 环境）
# 使用方法：
#   手动运行： bash backup_mysql.sh
#   自动运行：配合 crontab 使用
# ============================================================

set -euo pipefail

# ----- 配置区（按需修改）-----
MYSQL_BIN="/usr/bin"
BACKUP_DIR="/data/backup/waterfun"
DB_HOST="localhost"
DB_PORT=3306
DB_USER="root"
DB_PASS="123456"
DB_NAME="waterfun"
RETAIN_DAYS=30
LOG_FILE="${BACKUP_DIR}/backup_log.txt"
# -----------------------------

# 生成日期后缀: 20260619
DATE_SUFFIX=$(date +%Y%m%d)
BACKUP_FILE="${BACKUP_DIR}/${DB_NAME}_${DATE_SUFFIX}.sql"

# 创建备份目录
mkdir -p "$BACKUP_DIR"

# 记录开始
echo "[$(date '+%Y-%m-%d %H:%M:%S')] 开始备份 ${DB_NAME}..." >> "$LOG_FILE"

# 执行备份
"${MYSQL_BIN}/mysqldump" \
    -h "$DB_HOST" \
    -P "$DB_PORT" \
    -u "$DB_USER" \
    -p"$DB_PASS" \
    "$DB_NAME" \
    --single-transaction \
    --routines \
    --triggers \
    --default-character-set=utf8mb4 \
    > "$BACKUP_FILE" 2>> "$LOG_FILE"

# 检查是否成功
if [ $? -ne 0 ]; then
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] ✗ 备份失败" >> "$LOG_FILE"
    echo "备份失败，请检查日志：${LOG_FILE}"
    exit 1
fi

# 压缩备份（默认启用 gzip，节省空间）
gzip -f "$BACKUP_FILE"
BACKUP_FILE="${BACKUP_FILE}.gz"

# 删除旧备份
find "$BACKUP_DIR" -name "${DB_NAME}_*.sql.gz" -type f -mtime +${RETAIN_DAYS} -delete

echo "[$(date '+%Y-%m-%d %H:%M:%S')] ✓ 备份完成：${BACKUP_FILE}" >> "$LOG_FILE"
echo "备份完成：${BACKUP_FILE}"
