# 软件研发管理平台（SoftDev Platform）

多机协作的开发管理平台，包含任务/版本发布/周报/DVS 测试报告/门户/知识库(Wiki)等模块。

## 技术栈与结构

| 目录 | 说明 |
|---|---|
| `backend/` | Spring Boot 3 + Java 17 + MyBatis-Plus，模块化（auth/task/release/weekly/dvs/portal/wiki/home） |
| `frontend/` | Vue 3 + TypeScript + Element Plus + Vite |
| `sql/` | 数据库结构脚本 `00X-*.sql`（版本化增量）+ `dev_platform_backup.sql`（全量快照） |
| `tools/` | 本机开发工具脚本（环境搭建、启动、数据库备份） |
| `docs/` | 需求/设计/任务文档 |

## 环境要求

- JDK 17、Node.js 18+、MySQL 8.0（见 `tools/setup-mysql.ps1` 无管理员 portable 安装方案）

## 快速启动（本机）

1. 安装 MySQL 并启动：`powershell -File tools/setup-mysql.ps1`
2. 建库导数据：导入 `sql/init.sql`（建库+全量结构）后执行 `sql/00X-*.sql` 增量脚本；或直接导入最新快照 `sql/dev_platform_backup.sql`
3. 启动后端：`powershell -File tools/start-backend.ps1`（默认端口 8080，dev profile）
4. 启动前端：`cd frontend && npm install && npm run dev`

## 多机协作工作流（GitHub 同步）

> 本仓库同时托管代码与数据库快照，用于在不同电脑之间无缝切换开发。

### 代码同步

- 标准流程：`git pull --rebase` → 开发 → `git commit` → `git push`
- 建议在 `main` 分支小步提交，提交信息参考规范（`feat/fix/chore/refactor` 前缀）

### 数据库备份（开发节点结束时执行）

```powershell
powershell -ExecutionPolicy Bypass -File tools/dump-db.ps1   # 默认 root/root，可用 -Password 覆盖
git add sql/dev_platform_backup.sql
git commit -m "chore(db): refresh database snapshot"
git push
```

> 注意：`sql/dev_platform_backup.sql` 是全量业务数据快照（含 release 失败用例等），当前约几十 MB。
> 建议仅在**功能节点结束或换机前**手动刷新提交，不要高频提交，避免仓库膨胀。

### 新电脑恢复

1. `git clone <repo-url> && cd`（同目录结构）
2. 安装 JDK17 / Node18 / MySQL8（或跑 `tools/setup-mysql.ps1`）
3. 导入数据库：`mysql -uroot -proot < sql/dev_platform_backup.sql`（或 init.sql + 增量脚本）
4. 按「快速启动」跑前后端

## 常见问题

- MySQL 未启动：执行 `powershell -File tools/setup-mysql.ps1`（幂等，不会重置数据）
- 后端连库失败：检查 `DB_HOST/DB_PORT/DB_USERNAME/DB_PASSWORD` 环境变量（默认 root/root@localhost:3306）

详细背景见 `docs/` 目录。
