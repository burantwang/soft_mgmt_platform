<template>
  <div class="fail-task-page">
    <el-card shadow="never" class="mb-16">
      <template #header>
        <div class="page-title">DailySanity任务</div>
      </template>

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
            <span class="filter-divider"></span>
            <label>原始报告</label>
            <el-select
              :model-value="reportSel[groupKey(group)]"
              placeholder="选择报告文件"
              clearable
              class="filter-select report-select"
              :disabled="!(group.reportFiles && group.reportFiles.length)"
              @change="openReport(group, $event)"
            >
              <el-option
                v-for="f in group.reportFiles || []"
                :key="f.fileId"
                :label="f.fileName"
                :value="f.fileId"
              />
            </el-select>
          </div>

          <el-table :data="visibleCases(group)" border stripe size="small" class="case-table">
            <el-table-column type="expand" width="45">
              <template #default="{ row }">
                <div class="case-log">
                  <div class="log-title">用例运行日志</div>
                  <pre v-if="row.caseLog" class="log-content">{{ row.caseLog }}</pre>
                  <span v-else class="text-muted">无日志</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="失败脚本" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">
                <div class="case-name">
                  <el-tag size="small" :type="row.caseType === 'error' ? 'danger' : 'warning'">{{ row.caseTypeDesc }}</el-tag>
                  <span class="name-text">{{ row.caseName }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="失败原因" min-width="200">
              <template #default="{ row }">
                <el-tooltip :content="fieldDisabledTip(row, 'failReason')" placement="top" :disabled="!fieldDisabled(row, 'failReason')">
                  <div>
                    <el-input v-model="editing[row.id].failReason" type="textarea" :rows="2" placeholder="填写失败原因" :disabled="fieldDisabled(row, 'failReason')" />
                  </div>
                </el-tooltip>
              </template>
            </el-table-column>
            <el-table-column label="结论进展" min-width="200">
              <template #default="{ row }">
                <el-tooltip :content="fieldDisabledTip(row, 'progress')" placement="top" :disabled="!fieldDisabled(row, 'progress')">
                  <div>
                    <el-input v-model="editing[row.id].progress" type="textarea" :rows="2" placeholder="填写分析进展与结论" :disabled="fieldDisabled(row, 'progress')" />
                  </div>
                </el-tooltip>
              </template>
            </el-table-column>
            <el-table-column label="责任人" width="140" align="center">
              <template #default="{ row }">
                <el-select v-model="editing[row.id].assigneeId" placeholder="选择" clearable>
                  <el-option v-for="u in userOptions" :key="u.id" :label="u.nickname || u.username" :value="u.id" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="120" align="center">
              <template #default="{ row }">
                <el-tooltip :content="fieldDisabledTip(row, 'status')" placement="top" :disabled="!fieldDisabled(row, 'status')">
                  <div>
                    <el-select v-model="editing[row.id].status" placeholder="状态" :disabled="fieldDisabled(row, 'status')">
                      <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
                    </el-select>
                  </div>
                </el-tooltip>
              </template>
            </el-table-column>
            <el-table-column label="AI分析描述" min-width="200">
              <template #default="{ row }">
                <el-tooltip placement="top" :show-after="150" :disabled="!row.aiAnalysis" popper-class="ai-tip-popper">
                  <template #content>
                    <div class="ai-tip-content">{{ row.aiAnalysis }}</div>
                  </template>
                  <div class="ai-cell">{{ row.aiAnalysis || '暂无分析' }}</div>
                </el-tooltip>
              </template>
            </el-table-column>
            <el-table-column label="AI分析判断" width="110" align="center">
              <template #default="{ row }">
                <el-tooltip :content="fieldDisabledTip(row, 'aiAnalysisCorrect')" placement="top" :disabled="!fieldDisabled(row, 'aiAnalysisCorrect')">
                  <div>
                    <el-select v-model="editing[row.id].aiAnalysisCorrect" placeholder="" :disabled="fieldDisabled(row, 'aiAnalysisCorrect')" style="width: 70px">
                      <el-option label="Y" :value="1" />
                      <el-option label="N" :value="0" />
                    </el-select>
                  </div>
                </el-tooltip>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="90" align="center" fixed="right">
              <template #default="{ row }">
                <el-tooltip :content="saveDisabledTip(row)" placement="top" :disabled="!saveDisabled(row)">
                  <div>
                    <el-button type="primary" size="small" :loading="saving[row.id]" :disabled="saveDisabled(row)" @click="saveCase(row)">保存</el-button>
                  </div>
                </el-tooltip>
              </template>
            </el-table-column>
          </el-table>
        </el-collapse-item>
      </el-collapse>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getEnabledUsersApi, getGroupedFailCasesApi, getReportContentApi, updateFailCaseApi } from '@/api/release'
import type { FailCaseGrouped, GroupedFailCase, UserOption, FailCaseUpdateForm } from '@/types/release'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

// 管理员（超管/普通管理员）不受前置条件限制；普通员工遵循限制
const isAdmin = computed(() => {
  const codes = userStore.userInfo?.roleCodes || []
  return codes.includes('super_admin') || codes.includes('admin')
})

const loading = ref(false)
const groups = ref<FailCaseGrouped[]>([])
const activeNames = ref<string[]>([])
const userOptions = ref<UserOption[]>([])
const saving = reactive<Record<number, boolean>>({})

const search = reactive({
  date: new Date().toISOString().slice(0, 10),
  branch: '',
  projectName: '',
  caseName: ''
})

const statusOptions = [
  { value: 1, label: '待处理' },
  { value: 2, label: '处理中' },
  { value: 3, label: '已修复' },
  { value: 4, label: '非缺陷' }
]

const editing = reactive<Record<number, FailCaseUpdateForm>>({})

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

// 每个分组当前选中的原始报告文件ID（仅用于展示选中项）
const reportSel = reactive<Record<string, number | undefined>>({})

function groupKey(group: FailCaseGrouped) {
  return `${group.branch}@@${group.projectName}`
}

function resetGroupFilter(group: FailCaseGrouped) {
  const key = groupKey(group)
  assigneeFilter[key] = undefined
  statusFilter[key] = undefined
}

function visibleCases(group: FailCaseGrouped): GroupedFailCase[] {
  const key = groupKey(group)
  const af = assigneeFilter[key]
  const sf = statusFilter[key]
  return group.cases.filter((c) => {
    if (af !== undefined) {
      if (af === 0 ? c.assigneeId != null : c.assigneeId !== af) return false
    }
    if (sf !== undefined) {
      if (sf === 99 ? (c.status !== 3 && c.status !== 4) : c.status !== sf) return false
    }
    return true
  })
}

/** 是否已指派责任人（基于编辑中的值） */
function hasAssignee(row: GroupedFailCase): boolean {
  const aid = editing[row.id]?.assigneeId
  return aid != null && aid !== 0
}

/** 状态栏位前置条件：失败原因、结论进展、AI分析判断(Y/N)均已填写 */
function statusReady(row: GroupedFailCase): boolean {
  const f = editing[row.id]
  if (!f) return false
  const reason = f.failReason?.trim()
  const progress = f.progress?.trim()
  const aiOk = f.aiAnalysisCorrect === 1 || f.aiAnalysisCorrect === 0
  return !!(reason && progress && aiOk)
}

type GuardField = 'failReason' | 'progress' | 'status' | 'aiAnalysisCorrect'

/** 是否禁用指定栏位（普通用户遵循前置条件，管理员豁免） */
function fieldDisabled(row: GroupedFailCase, field: GuardField): boolean {
  if (isAdmin.value) return false
  if (!hasAssignee(row)) return true
  if (field === 'status') return !statusReady(row)
  return false
}

function fieldDisabledTip(row: GroupedFailCase, field: GuardField): string {
  if (isAdmin.value) return ''
  if (!hasAssignee(row)) return '请先指派责任人后再编辑'
  if (field === 'status') return '请完成所有信息后再更新'
  return ''
}

/** 保存按钮：普通用户未指派责任人时禁止保存 */
function saveDisabled(row: GroupedFailCase): boolean {
  return !isAdmin.value && !hasAssignee(row)
}

function saveDisabledTip(row: GroupedFailCase): string {
  return saveDisabled(row) ? '请先指派责任人后再保存' : ''
}

/** 分组状态统计：待处理/处理中/已修复(含非缺陷)/分析完成率 */
function groupStatusStats(group: FailCaseGrouped) {
  const cases = group.cases
  const pending = cases.filter((c) => c.status === 1).length
  const processing = cases.filter((c) => c.status === 2).length
  const fixed = cases.filter((c) => c.status === 3 || c.status === 4).length
  const total = cases.length
  const rate = total > 0 ? Math.round((fixed / total) * 100) : 100
  return { pending, processing, fixed, rate }
}

/** 打开该分组的原始 HTML 测试报告（新窗口） */
async function openReport(group: FailCaseGrouped, fileId?: number) {
  if (!fileId) return
  try {
    const blob = await getReportContentApi(fileId)
    const url = URL.createObjectURL(blob)
    window.open(url, '_blank')
    // 新窗口加载后释放 Blob URL，避免内存占用
    setTimeout(() => URL.revokeObjectURL(url), 60_000)
  } catch {
    // 错误提示已在请求拦截器统一处理
  }
}

function toEditingForm(item: GroupedFailCase): FailCaseUpdateForm {
  // 分析进展与结论合并为一列：初始化时拼接两字段历史内容，保存时同步写入两个字段
  const combined = [item.progress, item.conclusion]
    .filter((s) => s && s.trim())
    .join('\n')
  return {
    status: item.status,
    assigneeId: item.assigneeId,
    failReason: item.failReason || '',
    fixPlan: item.fixPlan || '',
    isBug: item.isBug ?? 0,
    aiAnalysisCorrect: item.aiAnalysisCorrect,
    progress: combined,
    conclusion: combined
  }
}

async function loadUsers() {
  try {
    const res = await getEnabledUsersApi()
    userOptions.value = res.data || []
  } catch (e) {
    // ignore
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await getGroupedFailCasesApi({
      date: search.date,
      branch: search.branch || undefined,
      projectName: search.projectName || undefined,
      caseName: search.caseName || undefined
    })
    groups.value = res.data || []
    activeNames.value = groups.value.map(groupKey)

    // 初始化编辑态
    Object.keys(editing).forEach((k) => delete editing[Number(k)])
    groups.value.forEach((g) => {
      g.cases.forEach((c) => {
        editing[c.id] = toEditingForm(c)
      })
    })
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

async function saveCase(row: any) {
  const item = row as GroupedFailCase
  const form = editing[item.id]
  saving[item.id] = true
  try {
    await updateFailCaseApi(item.id, form)
    ElMessage.success('保存成功')
    await loadData()
  } finally {
    saving[item.id] = false
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
  loadData()
})
</script>

<style scoped>
.fail-task-page {
  padding: 16px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
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
