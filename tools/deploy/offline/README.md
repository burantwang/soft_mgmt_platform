# Sonic 版本发布业务平台 —— 离线部署手册

面向**无外网的生产服务器**（Ubuntu 24.04 x86_64）的全离线部署方案。
所有运行时依赖（Docker 引擎、MySQL、JDK 运行时、Nginx、应用）均随包提供，安装过程不访问互联网。

---

## 一、交付包结构

```
offline/
├── install.sh                      # 一键安装（幂等，可重复执行）
├── README.md                       # 本文档（部署手册）
├── build/
│   ├── backend/
│   │   ├── Dockerfile              # 后端运行时镜像（COPY jar，不编译）
│   │   └── dev-platform.jar        # 后端产物
│   └── frontend/
│       ├── Dockerfile              # 前端运行时镜像（nginx 托管）
│       ├── nginx.conf              # 静态托管 + /api 反代 + SPA 回退
│       └── dist/                   # 前端构建产物
├── compose/
│   └── docker-compose.yml          # 编排（使用本地镜像，不 build）
├── config/
│   ├── .env.template              # 环境变量模板
│   └── daemon.json                # Docker 守护进程配置（镜像加速/日志轮转）
├── sql/
│   └── init/                      # 数据库初始化脚本（按文件名顺序自动执行）
│       ├── 001-init.sql           # 表结构与基础数据
│       ├── 002~010-*.sql          # 各模块增量脚本
│       └── 011-data-snapshot.sql  # 全量业务数据快照（迁移用）
├── deps/
│   └── docker-debs/               # Docker 引擎离线安装包（由构建机收集）
├── images/                        # docker save 导出的镜像 tar
│   ├── mysql-8.0.tar
│   ├── backend-<版本>.tar
│   └── frontend-<版本>.tar
└── scripts/
    ├── build-images.sh            # 构建机：构建镜像
    ├── collect-deps.sh            # 构建机：收集 Docker deb + 导出镜像
    ├── backup.sh                  # 备份数据库/文件/配置
    ├── upgrade.sh                 # 版本升级（数据不动）
    └── status.sh                  # 状态与健康检查
```

---

## 二、构建机操作（联网 Ubuntu 24.04）

只在**制作离线包**时执行一次：

```bash
# 1. 安装 Docker（构建机需要联网）
sudo apt-get update && sudo apt-get install -y docker.io docker-compose-v2

# 2. 准备产物：backend/target/dev-platform.jar 与 frontend/dist
#    （在开发机执行 mvn package 与 npm run build 后拷入 build/ 对应目录）

# 3. 构建应用镜像
sudo bash scripts/build-images.sh 1.0.0

# 4. 收集 Docker 离线安装包 + 导出镜像 tar
sudo bash scripts/collect-deps.sh 1.0.0
```

完成后把整个 `offline/` 目录打包（`tar -czf devplatform-offline-1.0.0.tar.gz -C offline .`），
通过 U 盘/内网传输到目标服务器。

---

## 三、目标服务器部署（无外网）

```bash
# 1. 解压交付包
sudo mkdir -p /opt/devplatform && sudo tar -xzf devplatform-offline-1.0.0.tar.gz -C /opt/devplatform

# 2. 一键安装（自动完成：装 Docker → 加载镜像 → 生成配置 → 启动 → 自检）
cd /opt/devplatform && sudo bash install.sh
```

安装脚本会输出访问地址与初始账号，首次启动会自动完成数据库初始化（含全量数据快照导入，约 1-3 分钟）。

---

## 四、部署后验证

```bash
# 容器状态（应全部 Up / healthy）
docker compose --env-file /opt/devplatform/config/.env -f /opt/devplatform/compose/docker-compose.yml ps

# 前端页面
curl -I http://127.0.0.1/

# 后端接口（未登录应返回业务错误码而非连接失败）
curl -s -X POST http://127.0.0.1/api/auth/login -H 'Content-Type: application/json' -d '{"username":"admin","password":"Admin@123"}'
```

浏览器访问 `http://<服务器IP>/`，用 `admin` 登录（首次登录强制修改密码），
按业务链路验证：系统管理（用户/角色/用户组）→ 版本发布 → 报告上传 → 看板。

---

## 五、日常运维

| 操作 | 命令 |
|---|---|
| 查看状态 | `sudo bash /opt/devplatform/scripts/status.sh` |
| 查看日志 | `docker logs -f dev-platform-backend` |
| 备份 | `sudo bash /opt/devplatform/scripts/backup.sh` |
| 升级 | 替换 `build/backend/dev-platform.jar`、`build/frontend/dist` 后 `sudo bash scripts/upgrade.sh 1.0.1` |
| 重启 | `cd /opt/devplatform && docker compose --env-file config/.env -f compose/docker-compose.yml restart` |
| 停止 | `... down`（`down -v` 会删除数据卷，勿用） |

数据位置：
- 数据库文件：`/opt/devplatform/data/mysql`
- 业务上传文件：`/opt/devplatform/data/files`
- 备份输出：`/opt/devplatform/data/backup/<时间戳>`

---

## 六、迁移到新服务器（整体搬迁）

1. 在旧服务器执行 `scripts/backup.sh`，取走 `data/backup/<时间戳>` 整个目录；
2. 新服务器安装同一离线包（`install.sh`）；
3. 恢复数据：
   ```bash
   docker exec -i dev-platform-mysql mysql -uroot -p<密码> dev_platform < db_dev_platform.sql
   tar -xzf files.tar.gz -C /opt/devplatform/data/
   ```
4. 若配置需要保持（API_PUSH_TOKEN 等），用 `env.bak` 覆盖 `/opt/devplatform/config/.env` 后重启。

---

## 七、常见问题

**Q1：`docker pull` 报 connection refused / DNS 解析异常**
目标服务器无外网属正常现象，本方案通过 `docker load` 离线镜像规避。构建机如遇拉取失败，可配置 `config/daemon.json` 中的镜像加速地址。

**Q2：80 端口被占用**
修改 `compose/docker-compose.yml` 中 frontend 的端口映射（如 `"8081:80"`），重启服务。

**Q3：首次启动数据库初始化失败**
删除 `/opt/devplatform/data/mysql` 下内容后重启 mysql 容器会重新初始化（**仅限首次部署，生产环境慎用**）：
```bash
docker compose ... down && sudo rm -rf /opt/devplatform/data/mysql/* && docker compose ... up -d
```

**Q4：后端连不上数据库**
查看 `docker logs dev-platform-backend`，确认 `config/.env` 中 `MYSQL_ROOT_PASSWORD` 与数据库实际密码一致（该值同时用于后端连接）。

**Q5：忘记 admin 密码**
在 `config/.env` 中修改 `ADMIN_INIT_PASSWORD` 只对"账号不存在"时生效；已有账号请用数据库方式重置或联系维护人员。
