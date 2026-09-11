#!/usr/bin/env bash
# ============================================================
# Sonic 版本发布业务平台 —— 一键离线部署脚本
# 适用环境：Ubuntu 24.04 x86_64（目标服务器无外网）
# 用法：sudo bash install.sh
# 特性：幂等可重复执行；不覆盖已有数据；配置与密钥自动生成
# ============================================================
set -euo pipefail

APP_VERSION="1.0.0"
INSTALL_DIR="/opt/devplatform"
DEPLOY_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

log()  { echo -e "\033[32m[部署]\033[0m $*"; }
warn() { echo -e "\033[33m[警告]\033[0m $*"; }
err()  { echo -e "\033[31m[错误]\033[0m $*" >&2; exit 1; }

[[ $EUID -eq 0 ]] || err "请使用 sudo 执行：sudo bash install.sh"

# ---------- 0. 环境检查 ----------
if [[ -f /etc/os-release ]]; then . /etc/os-release; log "系统：${PRETTY_NAME:-未知} / $(uname -m)"; fi
[[ "$(uname -m)" == "x86_64" ]] || warn "当前架构非 x86_64，离线镜像可能无法运行"
command -v tar >/dev/null || err "缺少 tar 命令"

# ---------- 1. Docker 引擎（离线优先）----------
if command -v docker >/dev/null 2>&1; then
  log "已检测到 Docker：$(docker --version)"
else
  if compgen -G "$DEPLOY_ROOT/deps/docker-debs/*.deb" >/dev/null; then
    log "离线安装 Docker 引擎（deb 包集合）"
    dpkg -i "$DEPLOY_ROOT/deps/docker-debs/"*.deb || apt-get -f install -y
  elif [[ -f "$DEPLOY_ROOT/deps/docker-static.tgz" ]]; then
    log "离线安装 Docker 引擎（静态二进制）"
    tar -xzf "$DEPLOY_ROOT/deps/docker-static.tgz" -C /usr/local/bin --strip-components=1 \
      docker/dockerd docker/docker docker/containerd docker/containerd-shim-runc-v2 docker/runc docker/ctr
    cat > /etc/systemd/system/docker.service <<'UNIT'
[Unit]
Description=Docker Engine
After=network-online.target
[Service]
ExecStart=/usr/local/bin/dockerd
Restart=always
[Install]
WantedBy=multi-user.target
UNIT
    systemctl daemon-reload
  else
    err "未找到 Docker 离线安装包（deps/docker-debs 或 deps/docker-static.tgz）"
  fi
fi
command -v docker >/dev/null || err "Docker 安装失败"
systemctl enable --now docker >/dev/null 2>&1 || warn "docker 服务启动命令执行异常，请检查"
docker info >/dev/null 2>&1 && log "Docker 引擎运行正常"

# ---------- 2. 目录与文件 ----------
log "准备安装目录 $INSTALL_DIR"
mkdir -p "$INSTALL_DIR"/{app,build,compose,config,sql,scripts,images,deps} \
         "$INSTALL_DIR"/data/{mysql,files,logs,backup}

if [[ "$DEPLOY_ROOT" != "$INSTALL_DIR" ]]; then
  cp -rf "$DEPLOY_ROOT"/build/*   "$INSTALL_DIR/build/"   2>/dev/null || true
  cp -rf "$DEPLOY_ROOT"/compose/* "$INSTALL_DIR/compose/" 2>/dev/null || true
  cp -rf "$DEPLOY_ROOT"/sql/*     "$INSTALL_DIR/sql/"     2>/dev/null || true
  cp -rf "$DEPLOY_ROOT"/scripts/* "$INSTALL_DIR/scripts/" 2>/dev/null || true
  cp -rf "$DEPLOY_ROOT"/images/*  "$INSTALL_DIR/images/"  2>/dev/null || true
  cp -f  "$DEPLOY_ROOT"/install.sh "$INSTALL_DIR/"        2>/dev/null || true
fi
chmod +x "$INSTALL_DIR"/scripts/*.sh 2>/dev/null || true

# ---------- 3. 环境变量 ----------
ENV_FILE="$INSTALL_DIR/config/.env"
if [[ -f "$ENV_FILE" ]]; then
  log "沿用已有配置文件 $ENV_FILE（不覆盖密钥）"
else
  log "生成配置文件 $ENV_FILE（含随机密钥）"
  rand() { tr -dc 'A-Za-z0-9' </dev/urandom | head -c "${1:-24}"; }
  cat > "$ENV_FILE" <<EOF
MYSQL_ROOT_PASSWORD=$(rand 24)
API_PUSH_TOKEN=$(rand 32)
ADMIN_INIT_PASSWORD=Admin@123
EOF
fi
chmod 600 "$ENV_FILE"

# ---------- 4. 加载镜像 ----------
shopt -s nullglob
IMAGES=("$INSTALL_DIR"/images/*.tar)
shopt -u nullglob
if (( ${#IMAGES[@]} == 0 )); then
  warn "未找到镜像 tar 包，将尝试使用本机已有镜像继续"
else
  for f in "${IMAGES[@]}"; do
    log "加载镜像 $(basename "$f")"
    docker load -i "$f"
  done
fi
docker image inspect devplatform/backend:"$APP_VERSION" >/dev/null 2>&1 || err "缺少后端镜像 devplatform/backend:$APP_VERSION"
docker image inspect devplatform/frontend:"$APP_VERSION" >/dev/null 2>&1 || err "缺少前端镜像 devplatform/frontend:$APP_VERSION"
docker image inspect devplatform/mysql:8.0 >/dev/null 2>&1 || err "缺少数据库镜像 devplatform/mysql:8.0"

# ---------- 5. 启动服务 ----------
log "启动服务（首次启动会自动初始化数据库，含全量数据快照导入，耗时约 1-3 分钟）"
cd "$INSTALL_DIR"
docker compose --env-file "$ENV_FILE" -f compose/docker-compose.yml up -d

# ---------- 6. 等待并就绪检查 ----------
log "等待数据库就绪..."
for i in $(seq 1 60); do
  state=$(docker inspect -f '{{.State.Health.Status}}' dev-platform-mysql 2>/dev/null || echo unknown)
  [[ "$state" == "healthy" ]] && break
  sleep 5
done
[[ "$state" == "healthy" ]] && log "数据库健康检查通过" || warn "数据库健康状态：$state（请查看 docker logs dev-platform-mysql）"

log "等待后端就绪..."
for i in $(seq 1 40); do
  if curl -sf -o /dev/null "http://127.0.0.1/api/auth/login" 2>/dev/null || \
     curl -s -o /dev/null -w '%{http_code}' "http://127.0.0.1/api/auth/login" 2>/dev/null | grep -qE '40[0-9]|200'; then
    break
  fi
  sleep 3
done

# ---------- 7. 结果输出 ----------
IP=$(hostname -I | awk '{print $1}')
echo
echo "==================== 部署结果 ===================="
docker compose --env-file "$ENV_FILE" -f compose/docker-compose.yml ps
echo "-------------------------------------------------"
echo " 访问地址 : http://${IP}/"
echo " 初始账号 : admin"
echo " 初始密码 : $(grep '^ADMIN_INIT_PASSWORD=' "$ENV_FILE" | cut -d= -f2)  （首次登录强制修改）"
echo " 配置文件 : $ENV_FILE"
echo " 数据目录 : $INSTALL_DIR/data"
echo " 常用命令 : docker compose --env-file $ENV_FILE -f $INSTALL_DIR/compose/docker-compose.yml ps|logs|restart"
echo "================================================="
log "部署完成"
