# 开发期部署与重新出包指南

> 适用场景：**开发尚未完成**，`192.168.13.129` 这台 VM 作为联调 / 验证环境；
> 等版本冻结、需要交付到无外网生产机时，再出正式离线包。
>
> 面向无外网生产机的正式交付说明见 [`offline/README.md`](offline/README.md)。

---

## 0. 环境速查

| 项 | 值 |
|---|---|
| 验证服务器 | `wwz@192.168.13.129` |
| 部署目录 | `/opt/devplatform` |
| 访问地址 | `http://192.168.13.129/`（初始账号 `admin` / 见 `config/.env` 的 `ADMIN_INIT_PASSWORD`） |
| 配置文件 | `/opt/devplatform/config/.env`（`chmod 600`，仅 root 可读） |
| 基线镜像 | `devplatform/backend:baseline-20260910`、`devplatform/frontend:baseline-20260910`、`devplatform/mysql:baseline-20260910` |
| 密钥 | 部署私钥 `C:\Users\wwz\.ssh\devplatform_vm`（已从仓库移除并加入 `.gitignore`） |

产物路径约定（与 Dockerfile 一致）：

- 后端 `mvn package` → `backend/target/dev-platform.jar`
- 前端 `npm run build` → `frontend/dist/`

---

## 1. 日常开发：改完代码刷新验证环境（推荐路径）

**不要每次都出离线包。** 开发期只需要刷新镜像重启容器。

### 1.1 在开发机构建产物并推送

```powershell
# 后端
mvn -f backend/pom.xml clean package -DskipTests
scp -i C:\Users\wwz\.ssh\devplatform_vm backend/target/dev-platform.jar `
    wwz@192.168.13.129:/opt/devplatform/build/backend/dev-platform.jar

# 前端（注意用 dist/. 而不是 dist/*，否则会漏掉隐藏文件）
npm --prefix frontend run build
scp -i C:\Users\wwz\.ssh\devplatform_vm -r frontend/dist/. `
    wwz@192.168.13.129:/opt/devplatform/build/frontend/dist/
```

### 1.2 在服务器重建镜像并滚动重启

```bash
sudo bash /opt/devplatform/scripts/upgrade.sh dev-$(date +%Y%m%d%H%M)
```

`upgrade.sh` 会依次：自动备份 → 用 `build/` 下的 jar/dist 重建两个镜像 → 更新 compose 里的镜像 tag →
`up -d backend frontend` 滚动重启（**数据库容器不动，数据不受影响**）。

### 1.3 只改了单端时，可只重建对应镜像

```bash
cd /opt/devplatform
sudo docker build -t devplatform/backend:dev build/backend
sudo docker compose --env-file config/.env -f compose/docker-compose.yml up -d backend
```

### 1.4 自检

```bash
sudo bash /opt/devplatform/scripts/status.sh     # 容器状态 + 资源 + 接口探测 + 最近错误日志
```

---

## 2. 需要交付时：正式出离线包（三步）

在服务器 `/opt/devplatform` 下执行（该机已装 Docker 且可访问 apt）：

```bash
cd /opt/devplatform
sudo bash scripts/build-images.sh 1.0.0     # 用 build/ 下的 jar + dist 重建三个镜像
sudo bash scripts/collect-deps.sh 1.0.0     # 收集 Docker 离线 deb + docker save 导出 images/*.tar
sudo bash scripts/package.sh 1.0.0          # 产出 /opt/devplatform-offline-1.0.0.tar.gz
```

产物：`/opt/devplatform-offline-1.0.0.tar.gz`（归档顶层目录固定为 `devplatform`，解压到 `/opt` 即为 `/opt/devplatform`）。

`package.sh` 的两个关键行为：

1. **编码自检**：若 `sql/init/011-data-snapshot.sql` 是 UTF-16（BOM `ff fe`）则**直接报错阻断打包**；
2. **排除运行期数据**：不带入 `data/mysql`、`data/logs`、`config/.env`、`*.bak`，避免把本机密钥和实时库带出去。

### 仅需刷新镜像 tar（不重收 deb、不出整包）

```bash
cd /opt/devplatform
sudo systemd-run --unit=dpimages --collect bash -c '
  cd /opt/devplatform &&
  docker save -o images/mysql-8.0.tar     devplatform/mysql:8.0 &&
  docker save -o images/backend-1.0.0.tar devplatform/backend:1.0.0 &&
  docker save -o images/frontend-1.0.0.tar devplatform/frontend:1.0.0'
```

> 用 `systemd-run` 而不是 `nohup ... &`：导出耗时较长，后台单元不会因 SSH 断开而中断。

---

## 3. 更新数据库快照（`011-data-snapshot.sql`）

包内 `sql/init/` 按文件名顺序在 MySQL **首次初始化**时自动执行，`011-data-snapshot.sql` 是迁移用的全量业务数据快照。

刷新步骤（在开发机）：

```powershell
powershell -ExecutionPolicy Bypass -File tools/dump-db.ps1
```

默认输出到 `sql/dev_platform_backup.sql`，再把它作为初始化快照使用：

```powershell
Copy-Item sql/dev_platform_backup.sql sql/init/011-data-snapshot.sql -Force
scp -i C:\Users\wwz\.ssh\devplatform_vm sql/init/011-data-snapshot.sql `
    wwz@192.168.13.129:/opt/devplatform/sql/init/011-data-snapshot.sql
```

`tools/dump-db.ps1` 已改为使用 `--result-file` 写出（而非 `>` 重定向），以保证输出为 **UTF-8 无 BOM**。

> **必须校验编码**，这是踩过的坑：PowerShell 5.1 的 `>` 重定向会输出 UTF-16LE，
> 新机器首次初始化 MySQL 时会直接失败：
> `ERROR: ASCII '\0' appeared in the statement ... Set --binary-mode`
>
> 校验命令：
> ```bash
> head -c 2 sql/init/011-data-snapshot.sql | od -An -tx1   # 不能是 ff fe
> ```
> 正常情况下应为 `2d 2d`（即 `--`）。`package.sh` 也会做这一步自检。

上传到服务器覆盖后，新装的机器即带最新数据；**已初始化过的库不会重跑**，需手工导入。

---

## 4. 回滚到基线

当前基线镜像 tag 为 `baseline-20260910`（后端 9/9 构建的 jar）。改动翻车时：

```bash
docker tag devplatform/backend:baseline-20260910  devplatform/backend:1.0.0
docker tag devplatform/frontend:baseline-20260910 devplatform/frontend:1.0.0
cd /opt/devplatform
sudo docker compose --env-file config/.env -f compose/docker-compose.yml up -d backend frontend
```

---

## 5. 注意事项

- **不要用 `docker compose down -v`**：`-v` 会删除数据卷。
- `sql/init/*.sql` 只在**首次初始化**（`data/mysql` 为空）时执行；已有数据的库改表结构要用迁移脚本或手工 SQL。
- 前端产物传输用 `scp -r dist/.`，`dist/*` 会漏掉隐藏文件。
- 版本 tag 建议语义化：开发期用 `dev-<时间戳>`，冻结版本用 `1.0.0` / `1.0.1`。
- Windows 上远程执行含 `$()`、单双引号的复杂命令易被 PowerShell 吞掉，建议把脚本 base64 后
  `echo <b64> | base64 -d | sudo bash -s` 传入；长任务一律走 `systemd-run`。
- 当前服务器上 `images/*.tar` 与 `deps/docker-debs/*.deb` 已生成但**尚未打成整包**；
  需要交付时直接跑第 2 节的 `package.sh` 即可。
