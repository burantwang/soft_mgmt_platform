#!/usr/bin/env bash
# ============================================================
# 构建离线部署镜像（在【联网的构建机】上执行，只需执行一次）
# 说明：镜像内不含任何配置与密钥，全部运行期由环境变量注入
# 产物：devplatform/mysql:8.0 / devplatform/backend:<ver> / devplatform/frontend:<ver>
# 用法：sudo bash scripts/build-images.sh [版本号]
# ============================================================
set -euo pipefail

APP_VERSION="${1:-${APP_VERSION:-1.0.0}}"
BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_DIR="$BASE_DIR/build"

echo "==> 工作目录: $BASE_DIR"
echo "==> 版本号  : $APP_VERSION"

if ! command -v docker >/dev/null 2>&1; then
  echo "!! 未检测到 docker，请先安装 Docker 引擎" >&2
  exit 1
fi

echo "==> [1/4] 拉取 MySQL 官方镜像并标记为本地镜像"
docker pull mysql:8.0
docker tag mysql:8.0 devplatform/mysql:8.0

echo "==> [2/4] 构建后端镜像（COPY 已构建好的 jar）"
docker build -t "devplatform/backend:${APP_VERSION}" "$BUILD_DIR/backend"

echo "==> [3/4] 构建前端镜像（COPY 已构建好的 dist）"
docker build -t "devplatform/frontend:${APP_VERSION}" "$BUILD_DIR/frontend"

echo "==> [4/4] 本机构建结果"
docker images --format '{{.Repository}}:{{.Tag}}  {{.Size}}' | grep -E 'devplatform|^mysql' || true
echo "==> 镜像构建完成"
