<template>
  <div class="dashboard">
    <!-- 单日发布情况（深色面板，与下方白色图表区形成清晰分层） -->
    <section class="day-section">
      <header class="day-header">
        <div class="day-title">
          <h2>单日发布情况</h2>
          <p>选择日期，查看该日版本发布的统计总结与明细</p>
        </div>
        <div class="day-picker">
          <span class="today-badge" v-if="isToday">今天</span>
          <el-date-picker
            v-model="selectedDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            :disabled-date="disabledFuture"
            :clearable="false"
            style="width: 150px"
          />
        </div>
      </header>

      <div class="day-stats">
        <div class="day-stat">
          <div class="ds-label">发布次数</div>
          <div class="ds-value">{{ day.totalRecords }}</div>
          <div class="ds-sub">
            成功 <span class="ok-text">{{ day.successCount }}</span>
            · 失败 <span class="bad-text">{{ day.failedCount }}</span>
          </div>
        </div>
        <div class="day-stat">
          <div class="ds-label">发布成功率</div>
          <div class="ds-value">{{ day.successRate }}%</div>
          <div class="ds-sub">成功 / 当日发布</div>
        </div>
        <div class="day-stat">
          <div class="ds-label">用例通过率</div>
          <div class="ds-value">{{ day.overallPassRate }}%</div>
          <div class="ds-sub">{{ day.passedCases }} / {{ day.totalCases }} 用例通过</div>
        </div>
        <div class="day-stat">
          <div class="ds-label">覆盖机型</div>
          <div class="ds-value">{{ day.projectCount }}</div>
          <div class="ds-sub">当日发布涉及机型</div>
        </div>
      </div>

      <div class="day-records">
        <div class="dr-header">
          <span class="dr-title">{{ selectedDate }} 分支 × 机型发布情况</span>
          <span class="text-muted">
            累计发布 {{ overview.totalRecords }} 次 · 成功率 {{ overview.successRate }}% ·
            用例通过率 {{ overview.overallPassRate }}% · 覆盖 {{ overview.projectCount }} 机型
          </span>
        </div>
        <el-table
          v-loading="loading"
          :data="dayStats"
          border
          stripe
          show-summary
          :summary-method="getDaySummary"
          max-height="420"
        >
          <el-table-column prop="branch" label="分支" min-width="130" show-overflow-tooltip />
          <el-table-column prop="projectName" label="机型" min-width="120" show-overflow-tooltip />
          <el-table-column prop="version" label="版本号" min-width="120">
            <template #default="{ row }">
              <el-tag size="small" type="info">{{ row.version || '-' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="totalCount" label="脚本总数" width="95" align="right" />
          <el-table-column prop="passedCount" label="成功数" width="85" align="right">
            <template #default="{ row }">
              <span class="ok-text">{{ row.passedCount }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="failedCount" label="失败数" width="85" align="right">
            <template #default="{ row }">
              <span :class="row.failedCount > 0 ? 'bad-text' : ''">{{ row.failedCount }}</span>
            </template>
          </el-table-column>
          <el-table-column label="通过率" width="90" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.totalCount > 0" :type="row.passRate >= 100 ? 'success' : 'warning'" size="small">
                {{ row.passRate }}%
              </el-tag>
              <span v-else class="text-muted">-</span>
            </template>
          </el-table-column>
          <el-table-column label="发布状态" width="95" align="center">
            <template #default="{ row }">
              <el-tag :type="row.result === 1 ? 'success' : 'danger'" size="small">
                {{ row.resultDesc }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="imageUrl" label="镜像地址" min-width="230" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-if="row.imageUrl" class="image-cell">{{ row.imageUrl }}</span>
              <span v-else class="text-muted">-</span>
            </template>
          </el-table-column>
          <el-table-column prop="sourceDesc" label="来源" width="100" />
          <el-table-column prop="publishTime" label="发布时间" width="170" />
        </el-table>
        <el-empty v-if="!loading && dayStats.length === 0" description="该日期暂无发布记录" :image-size="80" />
      </div>
    </section>

    <!-- 近15天趋势 -->
    <el-card shadow="never" class="chart-card">
      <template #header>
        <div class="chart-header">
          <span>近 15 天发布趋势</span>
          <span class="text-muted" style="font-size: 12px">每 60 秒自动刷新</span>
        </div>
      </template>
      <div ref="trendRef" class="chart-box"></div>
    </el-card>

    <div class="chart-grid">
      <el-card shadow="never" class="chart-card">
        <template #header>按分支发布统计</template>
        <div ref="branchRef" class="chart-box"></div>
      </el-card>
      <el-card shadow="never" class="chart-card">
        <template #header>按机型发布次数</template>
        <div ref="projectRef" class="chart-box"></div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import * as echarts from 'echarts'
import { getDashboardSummaryApi } from '@/api/release'
import type { DashboardSummary } from '@/types/release'

const EMPTY_OVERVIEW = {
  totalRecords: 0,
  successCount: 0,
  failedCount: 0,
  successRate: 0,
  totalCases: 0,
  passedCases: 0,
  overallPassRate: 0,
  projectCount: 0
}

function todayStr(): string {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

const loading = ref(false)
const summary = ref<DashboardSummary>()
const overview = computed(() => summary.value?.overview || EMPTY_OVERVIEW)
const day = computed(() => summary.value?.dayOverview || EMPTY_OVERVIEW)
const dayStats = computed(() => summary.value?.dayStats || [])
const isToday = computed(() => selectedDate.value === todayStr())

/* 日期选择：默认今天，禁止选择未来日期 */
const selectedDate = ref(todayStr())

function disabledFuture(date: Date): boolean {
  const today = new Date()
  today.setHours(23, 59, 59, 999)
  return date.getTime() > today.getTime()
}

function getDaySummary(param: { columns: any[]; data: any[] }): string[] {
  const { columns, data } = param
  const sums: string[] = []
  let total = 0
  let passed = 0
  let failed = 0
  data.forEach((item) => {
    total += item.totalCount || 0
    passed += item.passedCount || 0
    failed += item.failedCount || 0
  })
  columns.forEach((column, index) => {
    if (index === 0) {
      sums[index] = '全局合计'
    } else if (column.property === 'totalCount') {
      sums[index] = String(total)
    } else if (column.property === 'passedCount') {
      sums[index] = String(passed)
    } else if (column.property === 'failedCount') {
      sums[index] = String(failed)
    } else if (column.property === 'passRate') {
      sums[index] = total > 0 ? `${((passed * 100) / total).toFixed(2)}%` : '-'
    } else {
      sums[index] = ''
    }
  })
  return sums
}

async function loadData() {
  loading.value = true
  try {
    const res = await getDashboardSummaryApi(selectedDate.value)
    summary.value = res.data
  } finally {
    loading.value = false
    await nextTick()
    renderTrend()
    renderBranch()
    renderProject()
  }
}

/* 切换日期时刷新，但 60s 自动刷新仍保持所选日期 */
function onDateChange() {
  loadData()
}

const trendRef = ref<HTMLDivElement>()
const branchRef = ref<HTMLDivElement>()
const projectRef = ref<HTMLDivElement>()

let trendChart: echarts.ECharts | null = null
let branchChart: echarts.ECharts | null = null
let projectChart: echarts.ECharts | null = null
let timer: ReturnType<typeof setInterval> | null = null

function renderTrend() {
  const el = trendRef.value
  if (!el || !summary.value) return
  trendChart ??= echarts.init(el)
  const trend = summary.value.trend
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['发布次数', '成功', '失败'] },
    grid: { left: 40, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: trend.map((t) => t.date.slice(5)) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        name: '发布次数',
        type: 'bar',
        data: trend.map((t) => t.total),
        barWidth: 12,
        itemStyle: { color: '#2b5be3' }
      },
      {
        name: '成功',
        type: 'line',
        smooth: true,
        data: trend.map((t) => t.success),
        itemStyle: { color: '#16a34a' }
      },
      {
        name: '失败',
        type: 'line',
        smooth: true,
        data: trend.map((t) => t.failed),
        itemStyle: { color: '#dc2626' }
      }
    ]
  })
}

function renderBranch() {
  const el = branchRef.value
  if (!el || !summary.value) return
  branchChart ??= echarts.init(el)
  const stats = summary.value.branchStats
  branchChart.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: (params: unknown) => {
        const arr = params as Array<{ seriesName: string; value: number; name: string }>
        const stat = stats.find((s) => s.branch === arr[0]?.name)
        const lines = arr
          .map((p) => `${p.seriesName}：${p.value}`)
          .join('<br/>')
        return `${arr[0]?.name}<br/>${lines}<br/>成功率：${stat?.successRate ?? 0}%`
      }
    },
    legend: { data: ['成功', '失败'] },
    grid: { left: 40, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: stats.map((s) => s.branch) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        name: '成功',
        type: 'bar',
        stack: 'total',
        data: stats.map((s) => s.success),
        itemStyle: { color: '#16a34a' }
      },
      {
        name: '失败',
        type: 'bar',
        stack: 'total',
        data: stats.map((s) => s.failed),
        itemStyle: { color: '#dc2626' }
      }
    ]
  })
}

function renderProject() {
  const el = projectRef.value
  if (!el || !summary.value) return
  projectChart ??= echarts.init(el)
  const stats = summary.value.projectStats
  projectChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}<br/>{c} 次（{d}%）' },
    legend: { type: 'scroll', orient: 'vertical', right: 0, top: 'center' },
    series: [
      {
        name: '发布次数',
        type: 'pie',
        radius: ['40%', '65%'],
        center: ['40%', '50%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
        label: { formatter: '{b}\n{c} 次' },
        data: stats.map((s) => ({ name: s.projectName, value: s.total }))
      }
    ]
  })
}

function handleResize() {
  trendChart?.resize()
  branchChart?.resize()
  projectChart?.resize()
}

onMounted(() => {
  loadData()
  timer = setInterval(loadData, 60_000)
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  branchChart?.dispose()
  projectChart?.dispose()
})
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* ---------- 单日发布情况：深色渐变面板，与下方白色图表区强分割 ---------- */
.day-section {
  background: linear-gradient(135deg, #0f2557 0%, #1e3a8a 45%, #2b5be3 100%);
  border-radius: 14px;
  padding: 22px 24px 24px;
  box-shadow: 0 10px 26px rgba(15, 37, 87, 0.3);
}

.day-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 18px;
}

.day-title h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #fff;
}

.day-title p {
  margin: 6px 0 0;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}

.day-picker {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.today-badge {
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.4);
  color: #fff;
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 999px;
}

.day-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.day-stat {
  background: #fff;
  border-radius: 10px;
  padding: 16px 18px;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.12);
}

.ds-label {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 8px;
}

.ds-value {
  font-size: 30px;
  font-weight: 700;
  line-height: 1;
  margin-bottom: 10px;
  color: #1e3a8a;
}

.ds-sub {
  font-size: 12px;
  color: #6b7280;
}

.day-records {
  margin-top: 16px;
  background: #fff;
  border-radius: 10px;
  padding: 14px 16px;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.12);
}

.dr-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.dr-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2329;
}

/* ---------- 图表区 ---------- */
.chart-card {
  border-radius: 10px;
}

.chart-card :deep(.el-card__body) {
  padding: 8px 12px 12px;
}

.chart-box {
  height: 320px;
}

.chart-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.ok-text {
  color: #16a34a;
}

.image-cell {
  font-family: "JetBrains Mono", Consolas, monospace;
  color: var(--el-color-primary);
}

.bad-text {
  color: #dc2626;
}

.text-muted {
  color: #9ca3af;
}

.mr-4 {
  margin-right: 4px;
}
</style>
