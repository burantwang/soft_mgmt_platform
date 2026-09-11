#!/usr/bin/env bash
# ============================================================
# 版本升级：替换 build 目录下的 jar/dist → 重建镜像 → 滚动重启
# 数据目录不受影响；升级前自动备份
# 用法：sudo bash scripts/upgrade.sh [新版本号]
# ============================================================
set -euo pipefail

INSTALL_DIR="${INSTALL_DIR:-/opt/devplatform}"
NEW_VERSION="${1:-$(date +%Y%m%d%H%M)}"
ENV_FILE="$INSTALL_DIR/config/.env"
COMPOSE_FILE="$INSTALL_DIR/compose/docker-compose.yml"

echo "==> 升级前备份（防止意外）"
bash "$INSTALL_DIR/scripts/backup.sh"

echo "==> 构建新版本镜像：$NEW_VERSION"
docker build -t "devplatform/backend:${NEW_VERSION}"  "$INSTALL_DIR/build/backend"
docker build -t "devplatform/frontend:${NEW_VERSION}" "$INSTALL_DIR/build/frontend"

echo "==> 更新编排文件中的镜像版本"
sed -i "s|devplatform/backend:[^ ]*|devplatform/backend:${NEW_VERSION}|g; s|devplatform/frontend:[^ ]*|devplatform/frontend:${NEW_VERSION}|g" "$COMPOSE_FILE"

echo "==> 重启应用容器（数据库容器不动）"
cd "$INSTALL_DIR"
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d backend frontend

echo "==> 升级完成，当前状态"
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" ps
