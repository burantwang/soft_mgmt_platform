#!/usr/bin/env bash
# ============================================================
# 打包离线部署包（在构建机上执行）
# 用法：bash scripts/package.sh [版本号]
# 产物：上层目录下的 devplatform-offline-<版本>.tar.gz
# 说明：自动排除运行期数据卷、密钥与临时文件，归档顶层目录固定为 devplatform
# ============================================================
set -euo pipefail

VERSION="${1:-1.0.0}"
BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PARENT="$(dirname "$BASE_DIR")"
NAME="devplatform"
OUT="$PARENT/devplatform-offline-${VERSION}.tar.gz"

echo "==> 打包前检查"
[[ -f "$BASE_DIR/build/backend/dev-platform.jar" ]] || { echo "!! 缺少后端产物 build/backend/dev-platform.jar" >&2; exit 1; }
[[ -d "$BASE_DIR/build/frontend/dist" ]]           || { echo "!! 缺少前端产物 build/frontend/dist" >&2; exit 1; }
ls -1 "$BASE_DIR"/images/*.tar >/dev/null 2>&1     || echo "!! 警告：images/ 下没有镜像 tar（目标机将无法离线 load）"
ls -1 "$BASE_DIR"/deps/docker-debs/*.deb >/dev/null 2>&1 || echo "!! 警告：deps/docker-debs 为空（目标机需已装 Docker）"

# 关键：数据快照 SQL 必须是 UTF-8（无 BOM）。UTF-16 会导致 mysql 导入报 "ASCII '\0'" 错误
SNAPSHOT="$BASE_DIR/sql/init/011-data-snapshot.sql"
if [[ -f "$SNAPSHOT" ]]; then
  BOM="$(head -c 2 "$SNAPSHOT" | od -An -tx1 | tr -d ' \n')"
  if [[ "$BOM" == "fffe" || "$BOM" == "feff" ]]; then
    echo "!! 错误：011-data-snapshot.sql 含 UTF-16 BOM，请先转为 UTF-8 无 BOM" >&2
    exit 1
  fi
  echo "    快照 SQL 编码检查通过（$(du -h "$SNAPSHOT" | cut -f1)）"
fi

echo "==> 打包中（排除 data/mysql、data/logs、config/.env、*.bak 等运行期数据）"
# 以 BASE_DIR 内容为根打包，统一加 devplatform/ 前缀，保证解压到 /opt 后即为 /opt/devplatform
tar -czf "$OUT" -C "$BASE_DIR" \
  --transform='s,^\.,devplatform,' \
  --exclude=./data/mysql \
  --exclude=./data/logs \
  --exclude=./config/.env \
  --exclude='*.bak' \
  --exclude='*~' \
  --exclude=./deps/docker-debs/partial \
  .

echo "==> 完成"
ls -lh "$OUT"
echo "==> 传输到目标服务器后执行：sudo tar -xzf $(basename "$OUT") -C /opt && cd /opt/devplatform && sudo bash install.sh"
