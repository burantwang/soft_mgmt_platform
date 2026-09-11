#!/usr/bin/env bash
# ============================================================
# 数据备份：数据库 + 上传文件 + 配置（三件套）
# 用法：sudo bash scripts/backup.sh
# 输出：/opt/devplatform/data/backup/<时间戳>/
# ============================================================
set -euo pipefail

INSTALL_DIR="${INSTALL_DIR:-/opt/devplatform}"
ENV_FILE="$INSTALL_DIR/config/.env"
[[ -f "$ENV_FILE" ]] || { echo "找不到配置文件 $ENV_FILE" >&2; exit 1; }

MYSQL_ROOT_PASSWORD="$(grep '^MYSQL_ROOT_PASSWORD=' "$ENV_FILE" | cut -d= -f2-)"
TS="$(date +%Y%m%d_%H%M%S)"
OUT="$INSTALL_DIR/data/backup/$TS"
mkdir -p "$OUT"

echo "==> 导出数据库"
docker exec dev-platform-mysql sh -c \
  "exec mysqldump -uroot -p'$MYSQL_ROOT_PASSWORD' --single-transaction --routines --triggers --default-character-set=utf8mb4 dev_platform" \
  > "$OUT/db_dev_platform.sql"

echo "==> 打包上传文件"
tar -czf "$OUT/files.tar.gz" -C "$INSTALL_DIR/data" files 2>/dev/null || true

echo "==> 备份配置"
cp "$ENV_FILE" "$OUT/env.bak"
chmod 600 "$OUT/env.bak"

echo "==> 备份完成：$OUT"
ls -lh "$OUT"
