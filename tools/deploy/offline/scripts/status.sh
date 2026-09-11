#!/usr/bin/env bash
# ============================================================
# 状态与健康检查（容器、资源、接口、磁盘、错误日志）
# 用法：sudo bash scripts/status.sh
# ============================================================
set -uo pipefail

INSTALL_DIR="${INSTALL_DIR:-/opt/devplatform}"
ENV_FILE="$INSTALL_DIR/config/.env"
COMPOSE_FILE="$INSTALL_DIR/compose/docker-compose.yml"
cd "$INSTALL_DIR" || exit 1

echo "===== 容器状态 ====="
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" ps
echo
echo "===== 资源占用 ====="
docker stats --no-stream --format 'table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}' 2>/dev/null || true
echo
echo "===== 接口探测 ====="
printf '前端页面   : '; curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1/ || echo 'fail'
printf '后端登录接口: '; curl -s -o /dev/null -w '%{http_code}\n' -X POST http://127.0.0.1/api/auth/login -H 'Content-Type: application/json' -d '{}' || echo 'fail'
printf '数据库健康 : '; docker inspect -f '{{.State.Health.Status}}' dev-platform-mysql 2>/dev/null || echo 'unknown'
echo
echo "===== 数据目录占用 ====="
du -sh "$INSTALL_DIR"/data/* 2>/dev/null || true
echo
echo "===== 后端最近错误 ====="
docker logs --tail 50 dev-platform-backend 2>&1 | grep -iE 'error|exception' | tail -5 || echo '无'
