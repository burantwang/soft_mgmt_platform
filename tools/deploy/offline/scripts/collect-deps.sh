#!/usr/bin/env bash
# ============================================================
# 在【联网的 Ubuntu 24.04 x86_64 构建机】上执行：
#   1) 下载 Docker 引擎及其依赖的离线 deb 包集合（供无外网服务器安装）
#   2) 导出镜像 tar 包
# 产出：deps/docker-debs/*.deb 、images/*.tar
# ============================================================
set -euo pipefail

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEB_DIR="$BASE_DIR/deps/docker-debs"
IMG_DIR="$BASE_DIR/images"
APP_VERSION="${1:-1.0.0}"

echo "==> 收集 Docker 离线安装包到 $DEB_DIR"
mkdir -p "$DEB_DIR/partial"
apt-get update -qq
apt-get install -y --download-only --reinstall \
  -o Dir::Cache::archives="$DEB_DIR" \
  docker.io docker-compose-v2 containerd runc
rm -rf "$DEB_DIR/partial"
echo "==> deb 包数量：$(ls -1 "$DEB_DIR" | wc -l)"

echo "==> 导出应用镜像 tar"
mkdir -p "$IMG_DIR"
docker save -o "$IMG_DIR/backend-${APP_VERSION}.tar"  "devplatform/backend:${APP_VERSION}"
docker save -o "$IMG_DIR/frontend-${APP_VERSION}.tar" "devplatform/frontend:${APP_VERSION}"
docker save -o "$IMG_DIR/mysql-8.0.tar"               devplatform/mysql:8.0
ls -lh "$IMG_DIR"

echo "==> 完成。将整个 offline 目录打包即可迁移到无外网服务器"
