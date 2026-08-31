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

          <FailCaseEditTable
            :rows="visibleCases(group)"
            board="daily"
            :user-options="userOptions"
            :categories="categories"
            :redmine-prefix="redminePrefix"
            :is-admin="isAdmin"
            @updated="loadData"
          />
        </el-collapse-item>
      </el-collapse>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { getEnabledUsersApi, getFailCaseMetaApi, getGroupedFailCasesApi, getRecentWeekStatsApi, getReportContentApi } from '@/api/release'
import type { FailCaseGrouped, GroupedFailCase, IssueCategory, RecentDayStat, UserOption } from '@/types/release'
import { useUserStore } from '@/store/user'
import FailCaseEditTable from '@/components/FailCaseEditTable.vue'

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
const categories = ref<IssueCategory[]>([])
const redminePrefix = ref('')

const search = reactive({
  date: new Date().toISOString().slice(0, 10),
  branch: '',
  projectName: '',
  caseName: ''
})

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

/** 分组状态统计：待处理/处理中/已修复(含非缺陷)/已关闭/分析完成率 */
function groupStatusStats(group: FailCaseGrouped) {
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

async function loadUsers() {
  try {
    const res = await getEnabledUsersApi()
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
  loadMeta()
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

/* 已关闭用例整行置灰（管理员关闭后置底） */
:deep(.row-closed) {
  color: #c0c4cc;
}
:deep(.row-closed td) {
  background: #f5f7fa !important;
  color: #c0c4cc !important;
}
:deep(.row-closed:hover > td),
:deep(.row-closed.el-table__row--hover > td) {
  background: #ebeef5 !important;
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
