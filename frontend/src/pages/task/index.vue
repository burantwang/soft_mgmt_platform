<template>
  <div class="fail-task-page">
    <el-card shadow="never" class="mb-16">
      <template #header>
        <div class="page-title">Daily_Sanity</div>
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

          <el-table
            :data="visibleCases(group)"
            border
            stripe
            size="small"
            class="case-table"
            :expand-on-click-row="false"
            @expand-change="onExpandChange"
          >
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
                <el-tooltip :content="fieldDisabledTip(toCase(row), 'failReason')" placement="top" :disabled="!fieldDisabled(toCase(row), 'failReason')">
                  <div>
                    <el-input
                      v-if="isExpanded(row.id)"
                      :key="`fr-on-${row.id}`"
                      v-model="editing[row.id].failReason"
                      type="textarea"
                      :autosize="{ minRows: 1, maxRows: 200 }"
                      placeholder="填写失败原因"
                      :disabled="fieldDisabled(toCase(row), 'failReason')"
                    />
                    <el-input
                      v-else
                      :key="`fr-off-${row.id}`"
                      v-model="editing[row.id].failReason"
                      type="textarea"
                      :autosize="{ minRows: 1, maxRows: 2 }"
                      placeholder="填写失败原因"
                      :disabled="fieldDisabled(toCase(row), 'failReason')"
                    />
                  </div>
                </el-tooltip>
              </template>
            </el-table-column>
            <el-table-column label="结论进展" min-width="200">
              <template #default="{ row }">
                <el-tooltip :content="fieldDisabledTip(toCase(row), 'progress')" placement="top" :disabled="!fieldDisabled(toCase(row), 'progress')">
                  <div>
                    <el-input
                      v-if="isExpanded(row.id)"
                      :key="`pr-on-${row.id}`"
                      v-model="editing[row.id].progress"
                      type="textarea"
                      :autosize="{ minRows: 1, maxRows: 200 }"
                      placeholder="填写分析进展与结论"
                      :disabled="fieldDisabled(toCase(row), 'progress')"
                    />
                    <el-input
                      v-else
                      :key="`pr-off-${row.id}`"
                      v-model="editing[row.id].progress"
                      type="textarea"
                      :autosize="{ minRows: 1, maxRows: 2 }"
                      placeholder="填写分析进展与结论"
                      :disabled="fieldDisabled(toCase(row), 'progress')"
                    />
                  </div>
                </el-tooltip>
              </template>
            </el-table-column>
            <el-table-column label="责任人" width="140" align="center">
              <template #default="{ row }">
                <el-tooltip :content="assignDisabled(toCase(row)) ? '仅责任人为自己的问题单可操作' : ''" placement="top" :disabled="!assignDisabled(toCase(row))">
                  <el-select
                    v-model="editing[row.id].assigneeId"
                    placeholder="选择"
                    clearable
                    :disabled="assignDisabled(toCase(row))"
                    :loading="saving[row.id]"
                    @change="quickAssign(toCase(row), $event)"
                  >
                    <el-option v-for="u in userOptions" :key="u.id" :label="u.nickname || u.username" :value="u.id" />
                  </el-select>
                </el-tooltip>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="120" align="center">
              <template #default="{ row }">
                <el-tooltip :content="fieldDisabledTip(toCase(row), 'status')" placement="top" :disabled="!fieldDisabled(toCase(row), 'status')">
                  <div :class="['status-cell', statusClass(editing[row.id].status)]">
                    <el-select
                      v-model="editing[row.id].status"
                      placeholder="状态"
                      :disabled="fieldDisabled(toCase(row), 'status')"
                      class="status-select"
                    >
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
                <el-tooltip :content="fieldDisabledTip(toCase(row), 'aiAnalysisCorrect')" placement="top" :disabled="!fieldDisabled(toCase(row), 'aiAnalysisCorrect')">
                  <div>
                    <el-select v-model="editing[row.id].aiAnalysisCorrect" placeholder="" :disabled="fieldDisabled(toCase(row), 'aiAnalysisCorrect')" style="width: 70px">
                      <el-option label="Y" :value="1" />
                      <el-option label="N" :value="0" />
                    </el-select>
                  </div>
                </el-tooltip>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="90" align="center" fixed="right">
              <template #default="{ row }">
                <el-tooltip :content="saveDisabledTip(toCase(row))" placement="top" :disabled="!saveDisabled(toCase(row))">
                  <div>
                    <el-button type="primary" size="small" :loading="saving[row.id]" :disabled="saveDisabled(toCase(row))" @click="saveCase(toCase(row))">保存</el-button>
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
import { assignFailCaseApi, getEnabledUsersApi, getGroupedFailCasesApi, getRecentWeekStatsApi, getReportContentApi, updateFailCaseApi } from '@/api/release'
import type { FailCaseGrouped, GroupedFailCase, RecentDayStat, UserOption, FailCaseUpdateForm } from '@/types/release'
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
const weekStats = ref<RecentDayStat[]>([])
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

/** 当前行责任人是当前登录用户 */
function isMyCase(row: GroupedFailCase): boolean {
  return row.assigneeId != null && row.assigneeId === userStore.userInfo?.id
}

/** 普通用户是否有权操作该行（责任人是自己；管理员不受限） */
function canOperate(row: GroupedFailCase): boolean {
  return isAdmin.value || isMyCase(row)
}

/** 责任人下拉是否禁用：普通用户不能操作他人责任行 */
function assignDisabled(row: GroupedFailCase): boolean {
  if (isAdmin.value) return false
  return row.assigneeId != null && row.assigneeId !== userStore.userInfo?.id
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

/** 是否禁用指定栏位（普通用户仅可编辑责任人是自己的行，管理员豁免） */
function fieldDisabled(row: GroupedFailCase, field: GuardField): boolean {
  if (isAdmin.value) return false
  if (!canOperate(row)) return true
  if (field === 'status') return !statusReady(row)
  return false
}

function fieldDisabledTip(row: GroupedFailCase, field: GuardField): string {
  if (isAdmin.value) return ''
  if (!canOperate(row)) return '仅责任人为自己的问题单可操作'
  if (field === 'status') return '请完成所有信息后再更新'
  return ''
}

/** 保存按钮：普通用户仅责任人是自己的行可保存 */
function saveDisabled(row: GroupedFailCase): boolean {
  return !isAdmin.value && !canOperate(row)
}

function saveDisabledTip(row: GroupedFailCase): string {
  return saveDisabled(toCase(row)) ? '仅责任人为自己的问题单可操作' : ''
}

/** 将 el-table 插槽的 DefaultRow 转换为强类型用例对象 */
function toCase(row: any): GroupedFailCase {
  return row as GroupedFailCase
}

/** 状态栏位样式类名 */
function statusClass(status: number | undefined): string {
  if (status === 2) return 'status-processing'
  if (status === 3 || status === 4) return 'status-fixed'
  return ''
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
  // 结论进展栏位合并展示，以 progress 为主、conclusion 兜底（兼容历史数据），
  // 不再拼接两个字段，避免重复保存导致内容翻倍
  const merged = item.progress || item.conclusion || ''
  return {
    status: item.status,
    assigneeId: item.assigneeId,
    failReason: item.failReason || '',
    fixPlan: item.fixPlan || '',
    isBug: item.isBug ?? 0,
    aiAnalysisCorrect: item.aiAnalysisCorrect,
    progress: merged,
    conclusion: merged
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

async function loadWeekStats() {
  try {
    const res = await getRecentWeekStatsApi()
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
function rateText(day: RecentDayStat): string {
  if (day.totalCount === 0 || day.rate === null || day.rate === undefined) return 'None'
  return `${day.rate}%`
}

/** 完成率样式：100%绿 / 未完成橙 / 无数据灰 */
function rateClass(day: RecentDayStat): string {
  if (day.totalCount === 0 || day.rate === null || day.rate === undefined) return 'none'
  return day.rate >= 100 ? 'ok' : 'warn'
}

function barWidth(day: RecentDayStat): string {
  if (day.totalCount === 0 || day.rate === null || day.rate === undefined) return '0%'
  return `${day.rate}%`
}

function pendingCount(day: RecentDayStat): number {
  return Math.max(day.totalCount - day.analyzedCount, 0)
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

/** 快速指派责任人：下拉选择即保存生效，无需点击保存按钮 */
async function quickAssign(row: GroupedFailCase, val: number | string | undefined) {
  const assigneeId = val == null || val === '' ? null : Number(val)
  if (assigneeId === row.assigneeId) return
  // 普通用户预校验：未指派仅可认领给自己；不能取消指派（管理员不受限）
  if (!isAdmin.value) {
    if (row.assigneeId == null && assigneeId !== userStore.userInfo?.id) {
      editing[row.id].assigneeId = row.assigneeId
      ElMessage.warning('未指派用例仅可认领给自己')
      return
    }
    if (assigneeId == null) {
      editing[row.id].assigneeId = row.assigneeId
      ElMessage.warning('不能取消指派，请转派给其他责任人')
      return
    }
  }
  saving[row.id] = true
  try {
    await assignFailCaseApi(row.id, assigneeId)
    row.assigneeId = assigneeId ?? undefined
    ElMessage.success(assigneeId == null ? '已取消指派' : '指派成功')
  } catch {
    // 接口失败时回滚下拉显示值，错误信息由拦截器统一提示
    editing[row.id].assigneeId = row.assigneeId
  } finally {
    saving[row.id] = false
  }
}

async function saveCase(row: any) {
  const item = row as GroupedFailCase
  const form = editing[item.id]
  // 结论进展栏位合并展示 progress+conclusion，保存时两字段保持一致，
  // 避免删除栏位内容后，残留的 conclusion 旧值在下一次加载时又被合并回流
  form.conclusion = form.progress
  saving[item.id] = true
  try {
    await updateFailCaseApi(item.id, form)
    ElMessage.success('保存成功')
    await loadData()
  } finally {
    saving[item.id] = false
  }
}

/** 行展开状态：用于让行内长文本栏位（失败原因/分析进展/AI 分析）按内容自适应撑高 */
const expandedRowIds = ref<Set<number>>(new Set())

/** 判断某行是否处于展开状态 */
function isExpanded(id: number): boolean {
  return expandedRowIds.value.has(id)
}

/** 长文本 textarea 的 autosize 配置：展开时按内容撑高，未展开时 2 行紧凑 */
function textAreaAutoSize(id: number) {
  return isExpanded(id) ? { minRows: 1, maxRows: 200 } : { minRows: 1, maxRows: 2 }
}

/** el-table expand 事件：把当前所有展开行同步到 expandedRowIds */
function onExpandChange(_row: any, expandedRows: any[]) {
  expandedRowIds.value = new Set(expandedRows.map((r) => r.id))
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

/* 行展开时 textarea 自动按内容撑高，无需限制最大高度 */

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
