# 04 - Jenkins 测试报告推送联调指南

> 适用场景：Jenkins 执行完 Daily Sanity（或任何 pytest-html 自动化测试）后，自动把报告推送到本平台，由平台解析入库，联动「失败任务追踪 / Daily_Sanity」等模块。
> 文档定位：联调前准备清单 + 平台接口说明 + Jenkins 配置步骤 + 验证与排错。

---

## 1. 总体流程

```
┌────────────┐  ① 测试完成，生成 pytest-html 报告   ┌──────────────┐
│  Jenkins    │ ─────────────────────────────────▶  │  测试报告     │
│  (Pipeline) │                                     │ (html 文件)   │
└────────────┘                                     └──────────────┘
       │ ② curl multipart POST /api/open/jenkins/push
       │    Header: X-Api-Token
       ▼
┌───────────────────────────────────────────────────────────┐
│  平台后端 (Spring Boot :8080)                              │
│   1. Token 鉴权                                           │
│   2. HtmlReportParser 解析报告（统计 + 失败用例）            │
│   3. 报告文件归档入库（reportFile）                         │
│   4. 创建发布记录（source=JENKINS_PUSH），失败用例入失败追踪  │
└───────────────────────────────────────────────────────────┘
       │ ③ 前端查看
       ▼
   发布管理 / Daily_Sanity / 失败任务追踪
```

**推荐方式：Jenkins 主动上传报告文件本体（multipart）。** 不要只推送文件 URL，原因见第 5 节。

---

## 2. 联调前准备清单

### 2.1 平台侧

- [ ] 后端已启动且可从 **Jenkins 节点**访问到地址，例如 `http://<平台IP>:8080`（确认防火墙放行）。
- [ ] 已设置环境变量 `API_PUSH_TOKEN`，或确认使用默认值。

| 配置项 | 位置 | 默认值 | 说明 |
|---|---|---|---|
| `api.push.token` | `backend/src/main/resources/application.yml` | `dev-platform-push-token` | 通过环境变量 `API_PUSH_TOKEN` 注入，线上务必改掉默认值 |

### 2.2 Jenkins 侧

- [ ] Daily Sanity 任务产物为标准 **pytest-html** 报告（平台解析器按 pytest-html 结构解析）。
- [ ] 确定本次推送关联的 **机型编码（projectCodes）**，且这些编码在平台上「机型管理」中已存在（不存在的编码会被拒绝）。
- [ ] Jenkins 节点装有 `curl`（或可用 HTTP 客户端）。

### 2.3 联调用信息

- 平台地址：`http://<平台IP>:8080`
- 推送 Token：与平台 `API_PUSH_TOKEN` 一致
- 分支名：如 `develop`
- 机型编码：如 `RM6991,RM6119`（以平台实际编码为准）
- 报告产物相对路径：如 `reports/daily_sanity.html`

---

## 3. 平台开放接口说明

### 3.1 接口一览

| 方法 | 路径 | Content-Type | 说明 |
|---|---|---|---|
| POST | `/api/open/jenkins/push` | `multipart/form-data` | 上传报告文件 + 表单参数，解析后入库（推荐） |
| POST | `/api/open/jenkins/push` | `application/json` | 直接推送统计数字入库（不传文件） |

鉴权：请求头 `X-Api-Token`，与平台配置的 Token 一致，不匹配返回 Token 无效。

### 3.2 multipart 模式参数（推荐）

| 参数 | 必填 | 类型 | 说明 |
|---|---|---|---|
| `branch` | ✅ | String | 代码分支，最长 128 |
| `projectCodes` | ✅ | String | 关联机型编码，逗号分隔，如 `RM6991,RM6119` |
| `file` | ✅ | 文件 | `.html` 测试报告（≤ 报告存储限制） |
| `version` | | String | 镜像/构建版本号，最长 128 |
| `imageUrl` | | String | 镜像地址或报告在线链接，最长 500 |
| `remark` | | String | 备注，最长 255 |

成功返回：

```json
{ "code": 0, "msg": "ok", "data": 123 }   // data = 发布记录 id
```

失败返回示例：

```json
{ "code": 40001, "msg": "推送鉴权失败", "data": null }
```

### 3.3 JSON 模式参数（已自行解析好统计数字时）

```json
{
  "branch": "develop",
  "projectCodes": ["RM6991"],
  "version": "v1.2.3",
  "imageUrl": "registry.xxx/demo:1.2.3",
  "remark": "Daily Sanity 自动推送",
  "totalCount": 120,
  "passedCount": 115,
  "failedCount": 3,
  "errorCount": 1,
  "skippedCount": 1,
  "durationSec": 3598.5,
  "reportTime": "2026-09-07 02:30:00"
}
```

> JSON 模式不会带出失败用例明细（不传报告文件），失败追踪需另行处理。**日常建议用 multipart 模式。**

---

## 4. Jenkins 侧配置步骤

### 4.1 凭据管理（Credentials）

1. Jenkins → Manage Jenkins → Credentials → 添加：
   - Kind：**Secret text**（Secret text 类型）
   - ID：`platform-push-token`
   - Secret：与平台 `API_PUSH_TOKEN` 一致

### 4.2 Pipeline 中推送报告

在 Daily Sanity 的 `Jenkinsfile` 末尾追加 stage：

```groovy
pipeline {
    environment {
        PLATFORM_URL = 'http://192.168.1.100:8080'   // 平台地址，按实际修改
        DEVICE_CODES = 'RM6991,RM6119'                // 本次测试机型编码，按实际修改
    }
    stages {
        // ... 前面的测试、产物归档 stage ...

        stage('Push Report to Platform') {
            when { success() }                       // 仅在测试执行成功时推送（失败产物可选调整）
            steps {
                script {
                    // ① 定位报告文件，glob 按实际产物路径调整
                    def reports = findFiles(glob: '**/daily_sanity.html')
                    if (reports.isEmpty()) {
                        error '未找到 Daily Sanity 测试报告（**/daily_sanity.html）'
                    }
                    def report = reports.first()

                    // ② 读取本次构建分支
                    def branch = env.GIT_BRANCH ?: env.BRANCH_NAME ?: 'unknown'

                    // ③ 推送文件
                    withCredentials([string(credentialsId: 'platform-push-token', variable: 'PUSH_TOKEN')]) {
                        sh """
                            curl -sS -X POST '${PLATFORM_URL}/api/open/jenkins/push' \
                              -H 'X-Api-Token: ${PUSH_TOKEN}' \
                              -F 'branch=${branch}' \
                              -F 'projectCodes=${DEVICE_CODES}' \
                              -F 'version=${env.BUILD_TAG}' \
                              -F 'remark=Daily Sanity 自动推送' \
                              -F 'file=@${report.path}'
                        """
                    }
                }
            }
        }
    }
}
```

### 4.3 手动验证命令（联调最常用）

在 **Jenkins 节点**（或任意能访问平台的机器）上先手工执行一次，确认通了再写进 Pipeline：

```bash
# 把 http://192.168.1.100:8080 与路径替换为实际值
curl -sS -X POST 'http://192.168.1.100:8080/api/open/jenkins/push' \
  -H 'X-Api-Token: dev-platform-push-token' \
  -F 'branch=develop' \
  -F 'version=v1.2.3' \
  -F 'projectCodes=RM6991,RM6119' \
  -F 'remark=Daily Sanity 手动联调' \
  -F 'file=@/path/to/daily_sanity.html'
```

返回 `code: 0` 且带 `recordId` 即为成功。

---

## 5. 为什么传文件而不是文件地址

| 方案 | 平台能否解析入库 | 是否可靠 | 结论 |
|---|---|---|---|
| **上传 HTML 文件本体** | ✅ 后端内置 `HtmlReportParser`，解析统计 + 失败用例并联动失败追踪 | ✅ 无跨机访问问题 | ✅ 推荐 |
| 只推送报告 URL | ❌ 平台无法解析用例明细 | ⚠️ 平台需能访问该 URL（Jenkins 节点文件系统路径平台访问不到） | 仅作辅助字段 `imageUrl` |

> 报告文件上传后平台会自动归档，前端「失败任务追踪」里可直接查看原始 HTML 报告，无需 Jenkins 另开静态下载地址。

---

## 6. 常见问题与排查

| 现象 | 可能原因 | 处理 |
|---|---|---|
| 返回 Token 无效 | `X-Api-Token` 与平台配置不一致 | 核对平台 `API_PUSH_TOKEN` 环境变量；重启用后生效 |
| 返回「请至少推送一个机型编码」 | `projectCodes` 为空或分隔符不对 | 使用英文逗号分隔；不能为空 |
| 返回机型相关错误 | 推送的机型编码在平台不存在 | 先在平台机型管理确认编码，或用已存在编码测试 |
| 返回「推送解析失败，请检查报告格式」 | 报告不是标准 pytest-html | 用平台「手动上传」同一文件若能解析，则报告格式没问题，检查文件是否损坏 |
| 返回「仅支持 .html 格式」 | 文件名后缀不是 html/htm | 确认上传文件扩展名 |
| Jenkins 节点 curl 超时/连不上 | 平台防火墙未放行、地址端口错误 | 在 Jenkins 节点 `curl http://<平台IP>:8080` 探活 |
| 推送成功但前端看不到 | 机型/页面筛选条件不对 | 按推送的 branch/机型/日期刷新「失败任务追踪 / Daily_Sanity」 |

### 联调自检顺序

1. 手动 curl（见 4.3）→ 确认返回 `code:0` ✅
2. 前端登录 → 失败任务追踪 / Daily_Sanity，按日期/机型/分支筛选 → 能看到新记录 ✅
3. 点击该记录 → 能查看**原始 HTML 报告** ✅
4. 确认失败用例是否被指派为「待处理」状态，进入日常追踪流 ✅
5. 再把上述命令封装进 Jenkinsfile，跑一次真实任务验证自动推送 ✅

---

## 7. 相关代码位置（后端联调参考）

| 内容 | 位置 |
|---|---|
| Jenkins 开放接口 | `backend/src/main/java/com/company/devplatform/module/release/controller/JenkinsPushController.java` |
| 报告解析器 | `HtmlReportParser`（release 模块通用）/ `DvsHtmlReportParser`（DVS 专用） |
| 报告文件存储 | `backend/src/main/java/com/company/devplatform/module/release/service/FileStorageService.java` |
| 推送入参 DTO | `backend/src/main/java/com/company/devplatform/module/release/dto/JenkinsPushDTO.java` |
| Token 配置 | `backend/src/main/resources/application.yml` → `api.push.token` |
