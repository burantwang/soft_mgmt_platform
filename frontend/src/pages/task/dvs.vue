<template>
  <div class="dvs-task-page">
    <el-card shadow="never" class="mb-16">
      <template #header>
        <div class="page-header">
          <div class="page-title">DVS</div>
          <div class="page-header-right">
            <template v-if="isAdmin">
              <el-button @click="categoryDialogVisible = true">分类管理</el-button>
              <el-button @click="openPrefixDialog">Bug 前缀配置</el-button>
              <el-button @click="openAiConfigDialog">AI 配置</el-button>
            </template>
            <el-button type="primary" @click="onPickReport">上传测试报告</el-button>
          </div>
        </div>
      </template>

      <div class="week-stats">
        <div class="week-stats-header">
          <span class="ws-title">最近7天分析完成概况</span>
          <span class="ws-sub">点击日期查看当天执行明细</span>
        </div>
        <div class="week-stats-body">
          <div
            v-for="day in weekStats"
            :key="day.date"
            class="day-cell"
            :class="{ active: search.date === day.date, 'has-data': day.totalCount > 0 }"
            @click="selectDate(day.date)"
          >
            <div class="dc-date">
              {{ formatDayLabel(day.date) }}
              <span v-if="isToday(day.date)" class="dc-today-dot"></span>
            </div>
            <div class="dc-rate" :class="rateClass(day)">{{ rateText(day) }}</div>
            <div class="dc-bar">
              <div class="dc-bar-inner" :class="rateClass(day)" :style="{ width: barWidth(day) }"></div>
            </div>
            <div class="dc-counts">
              <span class="dc-count"><b>{{ day.totalCount }}</b> 失败</span>
              <span class="dc-count"><b>{{ day.analyzedCount }}</b> 已分析</span>
            </div>
            <div v-if="pendingCount(day) > 0" class="dc-pending">未完成 <b>{{ pendingCount(day) }}</b></div>
            <div v-else-if="day.totalCount > 0" class="dc-done">全部完成</div>
          </div>
        </div>
      </div>

      <div class="search-bar">
        <div class="form-row">
          <div class="form-item">
            <label>日期</label>
            <el-date-picker v-model="search.date" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
          </div>
          <div class="form-item">
            <label>分支</label>
            <el-input v-model="search.branch" placeholder="分支关键字" clearable />
          </div>
          <div class="form-item">
            <label>机型</label>
            <el-input v-model="search.projectName" placeholder="机型关键字" clearable />
          </div>
          <div class="form-item">
            <label>用例名称</label>
            <el-input v-model="search.caseName" placeholder="用例关键字" clearable />
          </div>
          <div class="form-item actions">
            <el-button type="primary" @click="loadData">查询</el-button>
            <el-button @click="resetSearch">重置</el-button>
            <el-button type="success" :loading="exporting" @click="handleExport">导出 Excel</el-button>
          </div>
        </div>
      </div>
    </el-card>

    <el-card v-loading="loading" shadow="never" class="groups">
      <el-empty v-if="!loading && groups.length === 0" description="该日期暂无失败用例" :image-size="100" />

      <el-collapse v-else v-model="activeNames" class="group-collapse">
        <el-collapse-item v-for="group in groups" :key="groupKey(group)" :name="groupKey(group)">
          <template #title>
            <div class="group-title">
              <div class="group-title-left">
                <span class="group-name">【{{ group.branch }} - {{ group.projectName }}】执行明细</span>
                <span class="group-stat">
                  用例总数：<b>{{ group.totalCount }}</b>
                  成功：<b class="ok">{{ group.passedCount }}</b>
                  失败：<b class="bad">{{ group.failedCount }}</b>
                  通过率：<b :class="group.passRate >= 100 ? 'ok' : 'warn'">{{ group.passRate }}%</b>
                </span>
              </div>
              <div class="group-title-right">
                <span class="status-stat">
                  待处理：<b>{{ groupStatusStats(group).pending }}</b>
                  处理中：<b>{{ groupStatusStats(group).processing }}</b>
                  已修复：<b class="ok">{{ groupStatusStats(group).fixed }}</b>
                  分析完成率：<b :class="groupStatusStats(group).rate >= 100 ? 'ok' : 'warn'">{{ groupStatusStats(group).rate }}%</b>
                </span>
              </div>
            </div>
          </template>

          <!-- 模块(HTML)独立统计（默认折叠，点击标题展开） -->
          <div v-if="group.modules && group.modules.length" class="module-stats">
            <div
              class="ms-header"
              :class="{ expanded: isModuleExpanded(groupKey(group)) }"
              @click="toggleModuleStats(groupKey(group))"
            >
              <div class="ms-header-left">
                <el-icon class="ms-arrow">
                  <component :is="isModuleExpanded(groupKey(group)) ? ArrowDown : ArrowRight" />
                </el-icon>
                <span class="ms-title">模块执行统计</span>
                <span class="ms-sub">全量测试结果分模块执行，每个 HTML 单独记录统计</span>
              </div>
              <div class="ms-header-right">
                <span class="ms-summary">共 {{ group.modules.length }} 个模块</span>
              </div>
            </div>
            <el-table
              v-show="isModuleExpanded(groupKey(group))"
              :data="group.modules"
              border
              size="small"
              class="module-table"
            >
              <el-table-column prop="moduleName" label="模块(HTML)" min-width="200" show-overflow-tooltip>
                <template #default="{ row }">
                  <span class="module-name">{{ row.moduleName }}</span>
                </template>
              </el-table-column>
              <el-table-column label="结果" width="80" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.result === 1 ? 'success' : 'danger'" size="small">{{ row.resultDesc }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="totalCount" label="用例总数" width="90" align="center" />
              <el-table-column prop="passedCount" label="通过" width="70" align="center">
                <template #default="{ row }"><span class="ok-text">{{ row.passedCount }}</span></template>
              </el-table-column>
              <el-table-column prop="failedCount" label="失败" width="70" align="center">
                <template #default="{ row }"><span class="bad-text">{{ row.failedCount }}</span></template>
              </el-table-column>
              <el-table-column prop="errorCount" label="错误" width="70" align="center">
                <template #default="{ row }"><span v-if="row.errorCount > 0" class="bad-text">{{ row.errorCount }}</span><span v-else class="text-muted">0</span></template>
              </el-table-column>
              <el-table-column prop="skippedCount" label="跳过" width="70" align="center" />
              <el-table-column label="通过率" width="90" align="center">
                <template #default="{ row }">
                  <span :class="row.passRate >= 100 ? 'ok-text' : 'warn-text'">{{ row.passRate }}%</span>
                </template>
              </el-table-column>
              <el-table-column label="报告" width="90" align="center">
                <template #default="{ row }">
                  <el-button v-if="row.reportFileId" link type="primary" @click="openModuleReport(row.reportFileId)">查看</el-button>
                  <span v-else class="text-muted">-</span>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div class="group-filter">
            <label>责任人</label>
            <el-select
              v-model="assigneeFilter[groupKey(group)]"
              placeholder="全部责任人"
              clearable
              class="filter-select"
              @clear="assigneeFilter[groupKey(group)] = undefined"
            >
              <el-option label="未指派" :value="0" />
              <el-option v-for="u in userOptions" :key="u.id" :label="u.nickname || u.username" :value="u.id" />
            </el-select>
            <label>状态</label>
            <el-select
              v-model="statusFilter[groupKey(group)]"
              placeholder="全部状态"
              clearable
              class="filter-select"
              @clear="statusFilter[groupKey(group)] = undefined"
            >
              <el-option v-for="s in statusFilterOptions" :key="s.value" :label="s.label" :value="s.value" />
            </el-select>
            <el-button size="small" text @click="resetGroupFilter(group)">重置</el-button>
          </div>

          <FailCaseEditTable
            :rows="visibleCases(group)"
            board="dvs"
            :user-options="userOptions"
            :categories="categories"
            :redmine-prefix="redminePrefix"
            :is-admin="isAdmin"
            @updated="loadData"
          />
        </el-collapse-item>
      </el-collapse>
    </el-card>

    <!-- 隐藏的多文件选择框 -->
    <input ref="fileInputRef" type="file" accept=".html,.htm" multiple style="display: none" @change="onFileChange" />

    <!-- 上传预览弹窗 -->
    <el-dialog v-model="previewVisible" title="报告解析预览" width="880px" destroy-on-close>
      <div v-loading="previewLoading" class="preview-body">
        <el-alert
          :type="previewAlertType"
          :title="previewAlertText"
          :closable="false"
          show-icon
          class="mb-16"
        />
        <el-table :data="previewItems" border size="small" max-height="240" class="mb-16">
          <el-table-column type="index" width="50" />
          <el-table-column prop="fileName" label="模块(HTML)" min-width="180" show-overflow-tooltip />
          <el-table-column label="结果" width="70" align="center">
            <template #default="{ row }">
              <el-tag :type="row.result === 1 ? 'success' : 'danger'" size="small">{{ row.resultDesc }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="totalCount" label="用例" width="70" align="center" />
          <el-table-column prop="passedCount" label="通过" width="60" align="center" />
          <el-table-column prop="failedCount" label="失败" width="60" align="center" />
          <el-table-column prop="errorCount" label="错误" width="60" align="center" />
          <el-table-column prop="skippedCount" label="跳过" width="60" align="center" />
          <el-table-column prop="version" label="版本" width="110" show-overflow-tooltip>
            <template #default="{ row }">{{ row.version || '-' }}</template>
          </el-table-column>
          <el-table-column label="失败用例" width="80" align="center">
            <template #default="{ row }">
              <span :class="row.failCases?.length ? 'bad-text' : ''">{{ row.failCases?.length || 0 }}</span>
            </template>
          </el-table-column>
        </el-table>

        <el-form ref="confirmFormRef" :model="confirmForm" :rules="confirmRules" label-width="90px">
          <el-form-item label="代码分支" prop="branch">
            <el-input v-model="confirmForm.branch" placeholder="如 master / release/v1.2" />
          </el-form-item>
          <el-form-item label="镜像版本" prop="version">
            <el-input v-model="confirmForm.version" placeholder="留空使用报告内 Environment.Version" />
          </el-form-item>
          <el-form-item label="关联机型" prop="projectIds">
            <el-select v-model="confirmForm.projectIds" multiple placeholder="选择机型（每个模块×机型单独记录）" style="width: 100%">
              <el-option v-for="p in projects" :key="p.id" :label="p.projectName" :value="p.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="confirmForm.remark" type="textarea" :rows="2" placeholder="备注（可选）" />
          </el-form-item>
        </el-form>

        <template v-if="totalFailCases.length">
          <el-divider content-position="left">失败/错误用例明细（{{ totalFailCases.length }} 条）</el-divider>
          <el-table :data="totalFailCases" border size="small" max-height="220">
            <el-table-column prop="module" label="所属模块" min-width="160" show-overflow-tooltip />
            <el-table-column prop="name" label="用例名称" min-width="220" show-overflow-tooltip />
            <el-table-column prop="log" label="运行日志" min-width="300" show-overflow-tooltip />
          </el-table>
        </template>
      </div>
      <template #footer>
        <el-button @click="previewVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="confirmReport">确认入库</el-button>
      </template>
    </el-dialog>

    <!-- 分类管理弹窗（仅管理员） -->
    <el-dialog v-model="categoryDialogVisible" title="问题分类管理" width="520px" destroy-on-close>
      <div class="category-list">
        <div v-for="c in categories" :key="c.id" class="category-row">
          <span class="category-name" :class="{ 'is-off': c.status !== 1 }">{{ c.categoryName }}</span>
          <el-button
            size="small"
            :type="c.status === 1 ? 'danger' : 'success'"
            text
            @click="toggleCategoryStatus(c)"
          >{{ c.status === 1 ? '停用' : '启用' }}</el-button>
        </div>
      </div>
      <div class="category-add">
        <el-input v-model="newCategoryName" placeholder="新分类名称" style="width: 200px" />
        <el-button type="primary" @click="addCategory">新增</el-button>
      </div>
    </el-dialog>

    <!-- Redmine 前缀配置弹窗（仅管理员） -->
    <el-dialog v-model="prefixDialogVisible" title="Bug 系统地址前缀配置" width="520px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="地址前缀">
          <el-input v-model="prefixInput" placeholder="如 http://100.60.183.51:300/issues/" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="prefixDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="savePrefix">保存</el-button>
      </template>
    </el-dialog>

    <!-- AI 配置弹窗（仅管理员） -->
    <el-dialog v-model="aiConfigDialogVisible" title="AI 服务配置" width="640px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="Base URL">
          <el-input v-model="aiConfigForm.baseUrl" placeholder="如 https://api.openai.com/v1" />
        </el-form-item>
        <el-form-item label="API Key">
          <el-input v-model="aiConfigForm.apiKey" type="password" show-password placeholder="API Key" />
        </el-form-item>
        <el-form-item label="模型">
          <el-input v-model="aiConfigForm.model" placeholder="如 gpt-4o-mini / deepseek-v4-flash" />
        </el-form-item>
        <el-form-item label="技能集">
          <div class="skill-list">
            <div v-for="s in skills" :key="s.id" class="skill-row">
              <el-switch v-model="s.enabled" :active-value="1" :inactive-value="0" @change="toggleSkill(s)" />
              <span class="skill-name" :class="{ 'is-off': s.enabled !== 1 }">{{ s.title }}</span>
              <el-button size="small" text type="primary" @click="openSkillEdit(s)">编辑</el-button>
              <el-button size="small" text type="danger" @click="removeSkill(s)">删除</el-button>
            </div>
            <el-button size="small" type="primary" plain @click="openSkillEdit()">+ 新增技能</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="aiConfigDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAiConfig">保存</el-button>
      </template>
    </el-dialog>

    <!-- skill 编辑弹窗（markdown） -->
    <el-dialog v-model="skillEditDialogVisible" :title="skillEditForm.id ? '编辑技能' : '新增技能'" width="900px" destroy-on-close>
      <el-form label-width="70px">
        <el-form-item label="文件名">
          <el-input v-model="skillEditForm.title" placeholder="如 skill1.md" />
        </el-form-item>
      </el-form>
      <div class="skill-editor">
        <div class="skill-editor-pane">
          <div class="pane-title">Markdown 编辑</div>
          <el-input v-model="skillEditForm.content" type="textarea" :rows="16" placeholder="输入 markdown 内容..." />
        </div>
        <div class="skill-editor-pane">
          <div class="pane-title">预览</div>
          <div class="skill-preview markdown-body" v-html="skillPreviewHtml"></div>
        </div>
      </div>
      <template #footer>
        <el-button @click="skillEditDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveSkill">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  confirmDvsReportApi,
  exportDvsExcelApi,
  getDvsEnabledUsersApi,
  getDvsGroupedCasesApi,
  getDvsRecentWeekStatsApi,
  getDvsReportContentApi,
  previewDvsReportApi
} from '@/api/dvs'
import {
  addIssueCategoryApi,
  createAiSkillApi,
  deleteAiSkillApi,
  getAiConfigApi,
  getAiSkillsApi,
  getFailCaseMetaApi,
  getProjectEnabledApi,
  toggleAiSkillApi,
  updateAiConfigApi,
  updateAiSkillApi,
  updateIssueCategoryStatusApi,
  updateRedminePrefixApi
} from '@/api/release'
import type {
  DvsFailCase,
  DvsFailCaseGrouped,
  DvsRecentDayStat,
  DvsReportPreviewVO
} from '@/types/dvs'
import type { AiConfig, AiSkill, IssueCategory, ReleaseProject, UserOption } from '@/types/release'
import { marked } from 'marked'
import { sanitizeHtml } from '@/utils/sanitize'
import { useUserStore } from '@/store/user'
import { ArrowDown, ArrowRight } from '@element-plus/icons-vue'
import FailCaseEditTable from '@/components/FailCaseEditTable.vue'

const userStore = useUserStore()

// 管理员（超管/普通管理员）不受前置条件限制；普通员工遵循限制
const isAdmin = computed(() => {
  const codes = userStore.userInfo?.roleCodes || []
  return codes.includes('super_admin') || codes.includes('admin')
})

const loading = ref(false)
const exporting = ref(false)
const groups = ref<DvsFailCaseGrouped[]>([])
const activeNames = ref<string[]>([])
const userOptions = ref<UserOption[]>([])
const weekStats = ref<DvsRecentDayStat[]>([])

// 失败用例元数据：问题分类 + Redmine 前缀
const categories = ref<IssueCategory[]>([])
const redminePrefix = ref('')
// 分类管理弹窗（仅管理员）
const categoryDialogVisible = ref(false)
const newCategoryName = ref('')
// Redmine 前缀配置弹窗（仅管理员）
const prefixDialogVisible = ref(false)
const prefixInput = ref('')
// AI 配置弹窗（仅管理员）
const aiConfigDialogVisible = ref(false)
const aiConfigForm = reactive<AiConfig>({ baseUrl: '', apiKey: '', model: '' })
// 技能集
const SKILL_MODULE = 'dvs_sanity'
const skills = ref<AiSkill[]>([])
const skillEditDialogVisible = ref(false)
const skillEditForm = reactive<{ id: number | null; title: string; content: string }>({ id: null, title: '', content: '' })

/** skill markdown 预览 */
const skillPreviewHtml = computed(() => {
  if (!skillEditForm.content) return ''
  try {
    return sanitizeHtml(marked.parse(skillEditForm.content) as string)
  } catch {
    return ''
  }
})

const search = reactive({
  date: new Date().toISOString().slice(0, 10),
  branch: '',
  projectName: '',
  caseName: ''
})

/** 每个分组（分支×机型）模块执行统计的展开状态（默认折叠） */
const moduleExpanded = reactive<Record<string, boolean>>({})

function isModuleExpanded(key: string): boolean {
  return !!moduleExpanded[key]
}

function toggleModuleStats(key: string) {
  moduleExpanded[key] = !moduleExpanded[key]
}

// 每个分组（分支×机型）的按责任人筛选：value 为 undefined 表示全部，0 表示未指派
const assigneeFilter = reactive<Record<string, number | undefined>>({})

// 每个分组的按状态筛选：value 为 undefined 表示全部；99 表示已修复(含非缺陷)
const statusFilter = reactive<Record<string, number | undefined>>({})

// 状态筛选选项（非缺陷、已修复均归入"已修复(含非缺陷)"，用 99 表示）
const statusFilterOptions = [
  { value: 1, label: '待处理' },
  { value: 2, label: '处理中' },
  { value: 99, label: '已修复(含非缺陷)' },
  { value: 4, label: '非缺陷' }
]

function groupKey(group: DvsFailCaseGrouped) {
  return `${group.branch}@@${group.projectName}`
}

function resetGroupFilter(group: DvsFailCaseGrouped) {
  const key = groupKey(group)
  assigneeFilter[key] = undefined
  statusFilter[key] = undefined
}

function visibleCases(group: DvsFailCaseGrouped): DvsFailCase[] {
  const key = groupKey(group)
  const af = assigneeFilter[key]
  const sf = statusFilter[key]
  const filtered = group.cases.filter((c) => {
    if (af !== undefined) {
      if (af === 0 ? c.assigneeId != null : c.assigneeId !== af) return false
    }
    if (sf !== undefined) {
      if (sf === 99 ? (c.status !== 3 && c.status !== 4 && c.status !== 5) : c.status !== sf) return false
    }
    return true
  })
  // 已关闭(5) 排到最后；其余保持原顺序
  return [...filtered].sort((a, b) => {
    if (a.status === 5 && b.status !== 5) return 1
    if (a.status !== 5 && b.status === 5) return -1
    return 0
  })
}

/** 分组状态统计：待处理/处理中/已修复(含非缺陷)/分析完成率 */
function groupStatusStats(group: DvsFailCaseGrouped) {
  const cases = group.cases
  const pending = cases.filter((c) => c.status === 1).length
  const processing = cases.filter((c) => c.status === 2).length
  const fixed = cases.filter((c) => c.status === 3 || c.status === 4).length
  const closed = cases.filter((c) => c.status === 5).length
  const total = cases.length
  // 已修复 + 已关闭 均计入分析完成
  const done = fixed + closed
  const rate = total > 0 ? Math.round((done / total) * 100) : 100
  return { pending, processing, fixed, closed, rate }
}

/** 打开模块的原始 HTML 报告（新窗口） */
async function openModuleReport(fileId: number) {
  try {
    const blob = await getDvsReportContentApi(fileId)
    const url = URL.createObjectURL(blob)
    window.open(url, '_blank')
    setTimeout(() => URL.revokeObjectURL(url), 60_000)
  } catch {
    // 错误提示已在请求拦截器统一处理
  }
}

async function loadUsers() {
  try {
    const res = await getDvsEnabledUsersApi()
    userOptions.value = res.data || []
  } catch (e) {
    // ignore
  }
}

/** 加载失败用例元数据（问题分类 + Redmine 前缀） */
async function loadMeta() {
  try {
    const res = await getFailCaseMetaApi()
    categories.value = res.data?.categories || []
    redminePrefix.value = res.data?.redminePrefix || ''
  } catch (e) {
    // ignore
  }
}

/** 新增问题分类（仅管理员） */
async function addCategory() {
  const name = newCategoryName.value.trim()
  if (!name) {
    ElMessage.warning('请输入分类名称')
    return
  }
  try {
    await addIssueCategoryApi({ categoryName: name })
    ElMessage.success('新增成功')
    newCategoryName.value = ''
    categoryDialogVisible.value = false
    await loadMeta()
  } catch {
    // ignore
  }
}

/** 停用/启用分类（仅管理员） */
async function toggleCategoryStatus(cat: IssueCategory) {
  try {
    await updateIssueCategoryStatusApi(cat.id, cat.status === 1 ? 0 : 1)
    ElMessage.success(cat.status === 1 ? '已停用' : '已启用')
    await loadMeta()
  } catch {
    // ignore
  }
}

/** 打开 Redmine 前缀配置弹窗 */
function openPrefixDialog() {
  prefixInput.value = redminePrefix.value
  prefixDialogVisible.value = true
}

/** 保存 Redmine 前缀（仅管理员） */
async function savePrefix() {
  const prefix = prefixInput.value.trim()
  if (!prefix) {
    ElMessage.warning('请输入地址前缀')
    return
  }
  try {
    await updateRedminePrefixApi(prefix)
    ElMessage.success('保存成功')
    redminePrefix.value = prefix
    prefixDialogVisible.value = false
  } catch {
    // ignore
  }
}

/** 打开 AI 配置弹窗（仅管理员） */
async function openAiConfigDialog() {
  try {
    const res = await getAiConfigApi()
    aiConfigForm.baseUrl = res.data?.baseUrl || ''
    aiConfigForm.apiKey = res.data?.apiKey || ''
    aiConfigForm.model = res.data?.model || ''
  } catch {
    // ignore
  }
  aiConfigDialogVisible.value = true
  loadSkills()
}

/** 保存 AI 配置（仅管理员） */
async function saveAiConfig() {
  try {
    await updateAiConfigApi({ ...aiConfigForm })
    ElMessage.success('保存成功')
    aiConfigDialogVisible.value = false
  } catch {
    // ignore
  }
}

/** 加载技能集列表 */
async function loadSkills() {
  try {
    const res = await getAiSkillsApi(SKILL_MODULE)
    skills.value = res.data || []
  } catch {
    // ignore
  }
}

/** 打开 skill 编辑弹窗（新增或编辑） */
function openSkillEdit(skill?: AiSkill) {
  if (skill) {
    skillEditForm.id = skill.id
    skillEditForm.title = skill.title
    skillEditForm.content = skill.content || ''
  } else {
    skillEditForm.id = null
    skillEditForm.title = ''
    skillEditForm.content = ''
  }
  skillEditDialogVisible.value = true
}

/** 保存 skill */
async function saveSkill() {
  const title = skillEditForm.title.trim()
  if (!title) {
    ElMessage.warning('请输入文件名')
    return
  }
  try {
    if (skillEditForm.id) {
      await updateAiSkillApi(skillEditForm.id, { title, content: skillEditForm.content })
    } else {
      await createAiSkillApi(SKILL_MODULE, { title, content: skillEditForm.content })
    }
    ElMessage.success('保存成功')
    skillEditDialogVisible.value = false
    await loadSkills()
  } catch {
    // ignore
  }
}

/** 启用/停用 skill */
async function toggleSkill(skill: AiSkill) {
  try {
    await toggleAiSkillApi(skill.id, skill.enabled === 1 ? 0 : 1)
    await loadSkills()
  } catch {
    // ignore
  }
}

/** 删除 skill */
async function removeSkill(skill: AiSkill) {
  try {
    await deleteAiSkillApi(skill.id)
    ElMessage.success('已删除')
    await loadSkills()
  } catch {
    // ignore
  }
}

async function loadWeekStats() {
  try {
    const res = await getDvsRecentWeekStatsApi()
    weekStats.value = res.data || []
  } catch (e) {
    // ignore
  }
}

function selectDate(date: string) {
  search.date = date
  loadData()
}

function isToday(dateStr: string): boolean {
  return dateStr === new Date().toISOString().slice(0, 10)
}

function formatDayLabel(dateStr: string): string {
  const d = new Date(dateStr)
  const today = new Date()
  const yesterday = new Date(today)
  yesterday.setDate(yesterday.getDate() - 1)

  const fmt = (dt: Date) => dt.toISOString().slice(0, 10)
  if (dateStr === fmt(today)) return '今天'
  if (dateStr === fmt(yesterday)) return '昨天'
  return `${d.getMonth() + 1}/${d.getDate()}`
}

/** 无执行明细时展示 None */
function rateText(day: DvsRecentDayStat): string {
  if (day.totalCount === 0 || day.rate === null || day.rate === undefined) return 'None'
  return `${day.rate}%`
}

/** 完成率样式：100%绿 / 未完成橙 / 无数据灰 */
function rateClass(day: DvsRecentDayStat): string {
  if (day.totalCount === 0 || day.rate === null || day.rate === undefined) return 'none'
  return day.rate >= 100 ? 'ok' : 'warn'
}

function barWidth(day: DvsRecentDayStat): string {
  if (day.totalCount === 0 || day.rate === null || day.rate === undefined) return '0%'
  return `${day.rate}%`
}

function pendingCount(day: DvsRecentDayStat): number {
  return Math.max(day.totalCount - day.analyzedCount, 0)
}

async function loadData() {
  loading.value = true
  try {
    const res = await getDvsGroupedCasesApi({
      date: search.date,
      branch: search.branch || undefined,
      projectName: search.projectName || undefined,
      caseName: search.caseName || undefined
    })
    groups.value = res.data || []
    activeNames.value = groups.value.map(groupKey)
  } finally {
    loading.value = false
  }
}

function resetSearch() {
  search.date = new Date().toISOString().slice(0, 10)
  search.branch = ''
  search.projectName = ''
  search.caseName = ''
  loadData()
}

/** 导出当天全部失败用例 Excel */
async function handleExport() {
  exporting.value = true
  try {
    await exportDvsExcelApi(search.date)
    ElMessage.success('导出成功')
  } catch {
    // 错误提示已在请求拦截器统一处理
  } finally {
    exporting.value = false
  }
}

/* ==================== 上传测试报告 ==================== */

const fileInputRef = ref<HTMLInputElement>()
const previewVisible = ref(false)
const previewLoading = ref(false)
const submitting = ref(false)
const previewData = ref<DvsReportPreviewVO | null>(null)
const projects = ref<ReleaseProject[]>([])

const previewItems = computed(() => previewData.value?.items || [])

/** 合并所有模块的失败用例（预览展示） */
const totalFailCases = computed(() => {
  return previewItems.value.flatMap((item) =>
    (item.failCases || []).map((fc) => ({
      module: item.fileName,
      name: fc.name,
      log: fc.log
    }))
  )
})

const previewAlertType = computed(() => {
  const failed = previewItems.value.filter((i) => i.result === 2).length
  return failed > 0 ? 'error' : 'success'
})

const previewAlertText = computed(() => {
  const items = previewItems.value
  const failed = items.filter((i) => i.result === 2).length
  const total = totalFailCases.value.length
  return `共 ${items.length} 个模块，失败模块 ${failed} 个` + (total ? `，失败/错误用例 ${total} 条` : '')
})

const confirmFormRef = ref<FormInstance>()
const confirmForm = reactive({
  previewToken: '',
  branch: '',
  version: '',
  projectIds: [] as number[],
  remark: ''
})
const confirmRules: FormRules = {
  branch: [{ required: true, message: '请输入代码分支', trigger: 'blur' }],
  projectIds: [{ required: true, type: 'array', min: 1, message: '请至少选择一个机型', trigger: 'change' }]
}

function onPickReport() {
  fileInputRef.value?.click()
}

/** DVS 报告扩展名筛查：仅接受 pytest-html 生成的 .html/.htm 文件 */
const REPORT_EXT_RE = /\.html?$/i

async function onFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const all = Array.from(input.files || [])
  input.value = ''
  if (!all.length) return
  const files = all.filter((f) => REPORT_EXT_RE.test(f.name))
  const ignored = all.filter((f) => !REPORT_EXT_RE.test(f.name))
  if (ignored.length) {
    ElMessage.warning(
      `已忽略 ${ignored.length} 个非 HTML 文件（仅支持 .html/.htm）：${ignored.map((f) => f.name).join('、')}`
    )
  }
  if (!files.length) return
  previewLoading.value = true
  try {
    const res = await previewDvsReportApi(files)
    previewData.value = res.data || null
    confirmForm.previewToken = previewData.value?.previewToken || ''
    confirmForm.branch = ''
    confirmForm.version = ''
    confirmForm.projectIds = []
    confirmForm.remark = ''
    previewVisible.value = true
  } catch {
    // 错误提示已在请求拦截器统一处理
  } finally {
    previewLoading.value = false
  }
}

async function confirmReport() {
  try {
    await confirmFormRef.value?.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    await confirmDvsReportApi({ ...confirmForm })
    ElMessage.success('确认入库成功')
    previewVisible.value = false
    await Promise.all([loadData(), loadWeekStats()])
  } catch {
    // 错误提示已在请求拦截器统一处理
  } finally {
    submitting.value = false
  }
}

async function loadProjects() {
  try {
    const res = await getProjectEnabledApi()
    projects.value = res.data || []
  } catch (e) {
    // ignore
  }
}

onMounted(async () => {
  // 刷新页面后用户信息为空，先拉取以获取角色编码（判断管理员豁免）
  if (!userStore.userInfo) {
    try {
      await userStore.fetchInfo()
    } catch {
      // token 失效等错误由接口统一处理，此处忽略
    }
  }
  loadUsers()
  loadWeekStats()
  loadData()
  loadProjects()
  loadMeta()
})
</script>

<style scoped>
.dvs-task-page {
  padding: 16px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 分类管理弹窗 */
.category-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}
.category-row {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  background: #fafbfd;
}
.category-name {
  font-size: 13px;
  color: #303133;
}
.category-name.is-off {
  color: #c0c4cc;
  text-decoration: line-through;
}
.category-add {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 技能集列表 */
.skill-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
}
.skill-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  background: #fafbfd;
}
.skill-name {
  flex: 1;
  font-size: 13px;
  color: #303133;
  font-family: "JetBrains Mono", Consolas, monospace;
}
.skill-name.is-off {
  color: #c0c4cc;
  text-decoration: line-through;
}

/* skill markdown 编辑器 */
.skill-editor {
  display: flex;
  gap: 12px;
  height: 420px;
}
.skill-editor-pane {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.pane-title {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 6px;
}
.skill-editor-pane :deep(.el-textarea) {
  flex: 1;
}
.skill-editor-pane :deep(.el-textarea__inner) {
  height: 100% !important;
  min-height: 360px;
}
.skill-preview {
  flex: 1;
  overflow: auto;
  padding: 10px 12px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  background: #fafbfd;
  font-size: 13px;
  line-height: 1.7;
  color: #303133;
  word-break: break-word;
}
.skill-preview :deep(h1),
.skill-preview :deep(h2),
.skill-preview :deep(h3) {
  margin: 10px 0 6px;
  font-weight: 600;
}
.skill-preview :deep(h1) { font-size: 18px; }
.skill-preview :deep(h2) { font-size: 16px; }
.skill-preview :deep(h3) { font-size: 14px; }
.skill-preview :deep(p) { margin: 6px 0; }
.skill-preview :deep(code) {
  background: #f1f5f9;
  padding: 1px 6px;
  border-radius: 3px;
  font-family: "JetBrains Mono", Consolas, monospace;
  font-size: 12.5px;
}
.skill-preview :deep(pre) {
  background: #1e293b;
  color: #e2e8f0;
  padding: 10px;
  border-radius: 6px;
  overflow: auto;
}
.skill-preview :deep(ul),
.skill-preview :deep(ol) { padding-left: 20px; margin: 6px 0; }

.page-title {
  font-size: 18px;
  font-weight: 600;
}

.week-stats {
  border: 1px solid #ebeef5;
  border-radius: 10px;
  background: linear-gradient(180deg, #fafbfd 0%, #ffffff 100%);
  margin-bottom: 16px;
  overflow: hidden;
}

.week-stats-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px 8px;
  border-bottom: 1px dashed #ebeef5;
}

.ws-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.ws-title::before {
  content: '';
  display: inline-block;
  width: 4px;
  height: 14px;
  border-radius: 2px;
  background: linear-gradient(180deg, #409eff, #79bbff);
  margin-right: 8px;
  vertical-align: -2px;
}

.ws-sub {
  font-size: 12px;
  color: #a8abb2;
}

.week-stats-body {
  display: flex;
}

.day-cell {
  flex: 1;
  min-width: 0;
  text-align: center;
  padding: 12px 6px 10px;
  cursor: pointer;
  position: relative;
  transition: background 0.2s;
  user-select: none;
}

.day-cell + .day-cell {
  border-left: 1px solid #f0f2f5;
}

.day-cell:hover {
  background: #f5f7fa;
}

.day-cell.active {
  background: linear-gradient(180deg, #eaf3ff 0%, #f0f7ff 100%);
  box-shadow: inset 0 -2px 0 #409eff;
}

.day-cell.has-data {
  background: linear-gradient(180deg, #bfe9d3 0%, #a3dcbf 100%);
}

.day-cell.has-data:hover {
  background: linear-gradient(180deg, #aee2c9 0%, #93d4b4 100%);
}

.day-cell.has-data.active {
  background: linear-gradient(180deg, #cfe4ff 0%, #b0d1ff 100%);
  box-shadow: inset 0 -2px 0 #409eff;
}

.dc-date {
  font-size: 13px;
  color: #606266;
  margin-bottom: 6px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.dc-today-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #409eff;
  display: inline-block;
}

.dc-rate {
  font-size: 22px;
  font-weight: 700;
  line-height: 1.2;
  margin-bottom: 8px;
}

.dc-rate.ok {
  color: #16a34a;
}

.dc-rate.warn {
  color: #d97706;
}

.dc-rate.none {
  color: #c0c4cc;
  font-size: 18px;
}

.dc-bar {
  height: 5px;
  background: #f0f2f5;
  border-radius: 3px;
  overflow: hidden;
  margin: 0 12px 8px;
}

.dc-bar-inner {
  height: 100%;
  border-radius: 3px;
  transition: width 0.3s;
}

.dc-bar-inner.ok {
  background: linear-gradient(90deg, #34d399, #10b981);
}

.dc-bar-inner.warn {
  background: linear-gradient(90deg, #fbbf24, #f59e0b);
}

.dc-counts {
  display: flex;
  justify-content: center;
  gap: 8px;
  font-size: 12px;
  color: #909399;
}

.dc-count b {
  color: #606266;
  font-weight: 600;
}

.dc-pending {
  font-size: 12px;
  color: #d97706;
  margin-top: 4px;
}

.dc-pending b {
  font-weight: 700;
}

.dc-done {
  font-size: 12px;
  color: #16a34a;
  margin-top: 4px;
}

.mb-16 {
  margin-bottom: 16px;
}

.search-bar {
  margin-bottom: 16px;
}

.form-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px;
}

.form-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.form-item label {
  color: #606266;
  font-size: 14px;
  white-space: nowrap;
}

.form-item.actions {
  margin-left: auto;
}

.group-collapse {
  border: none;
}

.group-title {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px 24px;
  padding-right: 12px;
  font-size: 14px;
}

.group-title-left {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px 24px;
  flex: 1;
}

.group-title-right {
  display: flex;
  align-items: center;
  margin-left: auto;
}

.group-name {
  font-weight: 600;
  color: #303133;
}

.group-stat {
  color: #606266;
}

.group-stat b {
  margin: 0 4px;
  font-weight: 600;
}

.group-stat .ok {
  color: #16a34a;
}

.group-stat .bad {
  color: #dc2626;
}

.group-stat .warn {
  color: #d97706;
}

.status-stat {
  color: #4a5568;
  background: linear-gradient(135deg, #e0f2fe 0%, #dbeafe 50%, #e0e7ff 100%);
  border: 1px solid #bfdbfe;
  border-radius: 20px;
  padding: 4px 16px;
  font-size: 13px;
  box-shadow: 0 1px 4px rgba(59, 130, 246, 0.12);
  white-space: nowrap;
}

.status-stat b {
  margin: 0 4px;
  font-weight: 600;
}

.status-stat .ok {
  color: #16a34a;
}

.status-stat .warn {
  color: #d97706;
}

/* 模块(HTML)独立统计 */
.module-stats {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  margin: 0 8px 12px;
  padding: 10px 12px;
  background: #fafbfd;
}

.ms-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 8px;
  margin: -4px -8px 8px;
  border-radius: 6px;
  cursor: pointer;
  user-select: none;
  transition: background 0.2s;
}
.ms-header:hover {
  background: #eef2f7;
}
.ms-header-left {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}
.ms-header-right {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #909399;
}
.ms-arrow {
  color: #909399;
  font-size: 14px;
  flex-shrink: 0;
  transition: transform 0.2s;
}
.ms-header.expanded .ms-arrow {
  color: #409eff;
}
.ms-summary {
  padding: 2px 10px;
  background: #ecf5ff;
  color: #409eff;
  border-radius: 10px;
  font-weight: 500;
}

.ms-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.ms-title::before {
  content: '';
  display: inline-block;
  width: 4px;
  height: 12px;
  border-radius: 2px;
  background: linear-gradient(180deg, #10b981, #34d399);
  margin-right: 6px;
  vertical-align: -1px;
}

.ms-sub {
  font-size: 12px;
  color: #a8abb2;
}

.module-name {
  font-family: "JetBrains Mono", Consolas, monospace;
}

.group-filter {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 8px 4px;
}

.group-filter label {
  color: #606266;
  font-size: 13px;
  white-space: nowrap;
}

.filter-divider {
  width: 1px;
  height: 20px;
  background: #e4e7ed;
  margin: 0 4px;
}

.filter-select {
  width: 180px;
}

.report-select {
  width: 220px;
}

.case-table {
  margin: 8px;
}

.case-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.name-text {
  font-family: "JetBrains Mono", Consolas, monospace;
}

/* AI 处理框 */
.ai-block {
  padding: 12px 16px;
  background: #f0f7ff;
  border-left: 3px solid #409eff;
  margin-bottom: 8px;
}
.ai-title {
  font-weight: 600;
  margin-bottom: 8px;
  color: #303133;
}
.ai-item {
  display: flex;
  gap: 8px;
  margin-bottom: 6px;
  font-size: 13px;
  line-height: 1.6;
}
.ai-item:last-child {
  margin-bottom: 0;
}
.ai-label {
  flex-shrink: 0;
  width: 60px;
  color: #606266;
  font-weight: 500;
}
.ai-text {
  flex: 1;
  color: #303133;
  white-space: pre-wrap;
  word-break: break-word;
}
.ai-verdict {
  align-items: center;
}
.ai-empty {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* Bug 单号 */
.bug-no-cell {
  display: flex;
  align-items: center;
  gap: 4px;
}

.case-log {
  padding: 12px 16px;
  background: #f8fafc;
}

.log-title {
  font-weight: 600;
  margin-bottom: 8px;
  color: #303133;
}

.log-content {
  max-height: 300px;
  overflow: auto;
  background: #1e293b;
  color: #e2e8f0;
  padding: 12px;
  border-radius: 6px;
  font-size: 12px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.text-muted {
  color: #909399;
}

.ok-text {
  color: #16a34a;
}

.bad-text {
  color: #dc2626;
}

.warn-text {
  color: #d97706;
}

.ai-cell {
  max-height: 44px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  white-space: pre-wrap;
  word-break: break-all;
  color: #606266;
  font-size: 13px;
  line-height: 1.5;
}

/* 行展开时 AI 分析描述按内容撑高 */
.ai-cell.ai-cell-expanded {
  display: block;
  -webkit-line-clamp: unset;
  -webkit-box-orient: unset;
  overflow: visible;
  max-height: none;
  white-space: pre-wrap;
}

/* 状态栏位底色（Element Plus 2.4+ 的 el-select 内部结构为 .el-select__wrapper） */
.status-cell .status-select :deep(.el-select__wrapper) {
  background-color: transparent;
  box-shadow: 0 0 0 1px #dcdfe6 inset;
  transition: background-color 0.2s, box-shadow 0.2s;
}
.status-cell.status-processing .status-select :deep(.el-select__wrapper) {
  background-color: #fff8e1 !important;
  box-shadow: 0 0 0 1px #ffc107 inset !important;
}
.status-cell.status-fixed .status-select :deep(.el-select__wrapper) {
  background-color: #e8f5e9 !important;
  box-shadow: 0 0 0 1px #4caf50 inset !important;
}

/* 已关闭用例整行置灰（管理员关闭后置底） */
::deep(.row-closed) {
  color: #c0c4cc;
}
::deep(.row-closed td) {
  background: #f5f7fa !important;
  color: #c0c4cc !important;
}
::deep(.row-closed:hover > td),
::deep(.row-closed.el-table__row--hover > td) {
  background: #ebeef5 !important;
}

.preview-body {
  max-height: 70vh;
  overflow: auto;
  padding-right: 4px;
}
</style>

<style>
/* AI 分析弹窗内容（渲染在 body 下，需全局样式） */
.ai-tip-popper {
  max-width: 660px;
}

.ai-tip-content {
  max-height: 360px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 13px;
  line-height: 1.7;
  color: #303133;
}
</style>
