<template>
  <el-table
    :data="rows"
    border
    stripe
    size="small"
    class="case-table"
    :expand-on-click-row="false"
    @expand-change="onExpandChange"
  >
    <!-- 展开列：AI 分析 + 用例日志 -->
    <el-table-column type="expand" width="45">
      <template #default="{ row }">
        <div class="ai-block">
          <div class="ai-title">AI 分析</div>
          <template v-if="row.aiRootCause || row.aiEvidence || row.aiSolution">
            <div class="ai-item">
              <span class="ai-label">根因</span>
              <div class="ai-text" v-html="mdToHtml(row.aiRootCause) || '暂无'"></div>
            </div>
            <div class="ai-item">
              <span class="ai-label">佐证</span>
              <div class="ai-text" v-html="mdToHtml(row.aiEvidence) || '暂无'"></div>
            </div>
            <div class="ai-item">
              <span class="ai-label">解决建议</span>
              <div class="ai-text" v-html="mdToHtml(row.aiSolution) || '暂无'"></div>
            </div>
          </template>
          <template v-else-if="row.aiAnalysis">
            <div class="ai-item">
              <span class="ai-label">分析描述</span>
              <div class="ai-text" v-html="mdToHtml(row.aiAnalysis) || '暂无'"></div>
            </div>
          </template>
          <template v-else>
            <div class="ai-empty">
              <el-button
                type="primary"
                size="small"
                :loading="aiLoading[row.id]"
                @click="triggerAiAnalyze(row)"
              >启用 AI 分析</el-button>
            </div>
          </template>
          <div class="ai-item ai-verdict">
            <span class="ai-label">分析判断</span>
            <el-select
              v-model="editing[row.id].aiAnalysisCorrect"
              placeholder=""
              :disabled="fieldDisabled(row, 'aiAnalysisCorrect')"
              style="width: 90px"
            >
              <el-option label="Y" :value="1" />
              <el-option label="N" :value="0" />
            </el-select>
          </div>
        </div>
        <div class="case-log">
          <div class="log-title">用例运行日志</div>
          <template v-if="row.caseLog">
            <div v-for="(it, i) in logLines(row.caseLog)" :key="i" class="log-line" :class="it.cls">
              <span class="log-ln">{{ i + 1 }}</span>
              <span class="log-text" v-html="highlightLogLine(it.text)" />
            </div>
          </template>
          <span v-else class="text-muted">无日志</span>
        </div>
      </template>
    </el-table-column>

    <!-- 来源（个人任务等平铺场景展示） -->
    <el-table-column v-if="showSource" label="来源" min-width="200">
      <template #default="{ row }">
        <div v-if="row.branch || row.projectName || row.moduleName || row.taskNo" class="source-cell">
          <div class="source-line">
            <span v-if="row.branch" class="branch">{{ row.branch }}</span>
            <el-tag v-if="row.version" size="small" type="info">{{ row.version }}</el-tag>
          </div>
          <div class="source-sub">
            <el-tag v-if="row.projectName" size="small">{{ row.projectName }}</el-tag>
            <el-tag v-if="row.moduleName" size="small" type="warning" class="ml-4">{{ row.moduleName }}</el-tag>
            <span v-if="row.taskNo" class="task-no ml-4">{{ row.taskNo }}</span>
          </div>
        </div>
        <span v-else class="text-muted">-</span>
      </template>
    </el-table-column>

    <!-- 失败脚本 -->
    <el-table-column label="失败脚本" min-width="220" show-overflow-tooltip>
      <template #default="{ row }">
        <div class="case-name">
          <el-tag size="small" :type="row.caseType === 'error' ? 'danger' : 'warning'">
            {{ row.caseTypeDesc || (row.caseType === 'error' ? '错误' : '失败') }}
          </el-tag>
          <span class="name-text">{{ row.caseName }}</span>
        </div>
      </template>
    </el-table-column>

    <!-- 失败原因 -->
    <el-table-column label="失败原因" min-width="200">
      <template #default="{ row }">
        <el-tooltip :content="fieldDisabledTip(row, 'failReason')" placement="top" :disabled="!fieldDisabled(row, 'failReason')">
          <div>
            <ResizeTipTextarea
              v-if="isExpanded(row.id)"
              :key="`fr-on-${row.id}`"
              v-model="editing[row.id].failReason"
              :autosize="{ minRows: 1, maxRows: 200 }"
              placeholder="填写失败原因"
              :disabled="fieldDisabled(row, 'failReason')"
            />
            <ResizeTipTextarea
              v-else
              :key="`fr-off-${row.id}`"
              v-model="editing[row.id].failReason"
              :autosize="{ minRows: 1, maxRows: 2 }"
              placeholder="填写失败原因"
              :disabled="fieldDisabled(row, 'failReason')"
            />
          </div>
        </el-tooltip>
      </template>
    </el-table-column>

    <!-- 结论进展 -->
    <el-table-column label="结论进展" min-width="200">
      <template #default="{ row }">
        <el-tooltip :content="fieldDisabledTip(row, 'progress')" placement="top" :disabled="!fieldDisabled(row, 'progress')">
          <div>
            <ResizeTipTextarea
              v-if="isExpanded(row.id)"
              :key="`pr-on-${row.id}`"
              v-model="editing[row.id].progress"
              :autosize="{ minRows: 1, maxRows: 200 }"
              placeholder="填写分析进展与结论"
              :disabled="fieldDisabled(row, 'progress')"
            />
            <ResizeTipTextarea
              v-else
              :key="`pr-off-${row.id}`"
              v-model="editing[row.id].progress"
              :autosize="{ minRows: 1, maxRows: 2 }"
              placeholder="填写分析进展与结论"
              :disabled="fieldDisabled(row, 'progress')"
            />
          </div>
        </el-tooltip>
      </template>
    </el-table-column>

    <!-- 问题分类 -->
    <el-table-column label="问题分类" width="130" align="center">
      <template #default="{ row }">
        <el-select
          v-model="editing[row.id].issueCategory"
          placeholder="选择"
          clearable
          :disabled="fieldDisabled(row, 'failReason')"
          style="width: 110px"
        >
          <el-option v-for="c in categories" :key="c.id" :label="c.categoryName" :value="c.categoryName" />
        </el-select>
      </template>
    </el-table-column>

    <!-- Bug 单号 -->
    <el-table-column label="Bug单号" width="130" align="center">
      <template #default="{ row }">
        <div class="bug-no-cell">
          <el-input
            v-model="editing[row.id].bugNo"
            placeholder=""
            :disabled="fieldDisabled(row, 'failReason')"
            style="width: 80px"
          />
          <el-button
            v-if="editing[row.id].bugNo"
            link
            type="primary"
            size="small"
            @click="jumpToBug(editing[row.id].bugNo)"
          >跳转</el-button>
        </div>
      </template>
    </el-table-column>

    <!-- 责任人 -->
    <el-table-column label="责任人" width="140" align="center">
      <template #default="{ row }">
        <el-tooltip :content="assignDisabled(row) ? '仅责任人为自己的问题单可操作' : ''" placement="top" :disabled="!assignDisabled(row)">
          <el-select
            v-model="editing[row.id].assigneeId"
            placeholder="选择"
            clearable
            :disabled="assignDisabled(row)"
            :loading="saving[row.id]"
            @change="quickAssign(row, $event)"
          >
            <el-option v-for="u in userOptions" :key="u.id" :label="u.nickname || u.username" :value="u.id" />
          </el-select>
        </el-tooltip>
      </template>
    </el-table-column>

    <!-- 状态 -->
    <el-table-column label="状态" width="120" align="center">
      <template #default="{ row }">
        <el-tooltip :content="fieldDisabledTip(row, 'status')" placement="top" :disabled="!fieldDisabled(row, 'status')">
          <div :class="['status-cell', statusClass(editing[row.id].status)]">
            <el-select
              v-model="editing[row.id].status"
              placeholder="状态"
              :disabled="fieldDisabled(row, 'status')"
              class="status-select"
            >
              <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
              <el-option key="closed" label="已关闭" :value="5" :disabled="!isAdmin" />
            </el-select>
          </div>
        </el-tooltip>
      </template>
    </el-table-column>

    <!-- 保存 -->
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
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import ResizeTipTextarea from '@/components/ResizeTipTextarea.vue'
import { aiAnalyzeDailyFailCaseApi, assignFailCaseApi, updateFailCaseApi } from '@/api/release'
import { aiAnalyzeWeeklyFailCaseApi, assignWeeklyFailCaseApi, updateWeeklyFailCaseApi } from '@/api/weekly'
import { aiAnalyzeDvsFailCaseApi, assignDvsFailCaseApi, updateDvsFailCaseApi } from '@/api/dvs'
import type { MyTaskCaseUpdateForm } from '@/types/mytask'
import type { IssueCategory, UserOption } from '@/types/release'

/** 轻量 Markdown → HTML：用于 AI 分析结果渲染，支持标题/列表/加粗/行内代码/代码块/换行 */
function mdToHtml(text?: string): string {
  if (!text) return ''
  const escapeHtml = (s: string) =>
    s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')
  const inline = (s: string) =>
    escapeHtml(s)
      .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
      .replace(/`([^`]+)`/g, '<code>$1</code>')
  const lines = text.split(/\r?\n/)
  const html: string[] = []
  let inUl = false
  let inOl = false
  let inCode = false
  for (const raw of lines) {
    const line = raw.replace(/\s+$/, '')
    if (line.trim().startsWith('```')) {
      if (inCode) {
        html.push('</code></pre>')
        inCode = false
      } else {
        if (inUl) {
          html.push('</ul>')
          inUl = false
        }
        if (inOl) {
          html.push('</ol>')
          inOl = false
        }
        html.push('<pre><code>')
        inCode = true
      }
      continue
    }
    if (inCode) {
      html.push(escapeHtml(line))
      continue
    }
    const h = line.match(/^(#{1,4})\s+(.*)$/)
    if (h) {
      if (inUl) {
        html.push('</ul>')
        inUl = false
      }
      if (inOl) {
        html.push('</ol>')
        inOl = false
      }
      const level = Math.min(h[1].length + 1, 6)
      html.push(`<h${level}>${inline(h[2])}</h${level}>`)
      continue
    }
    const ul = line.match(/^\s*[-*]\s+(.*)$/)
    if (ul) {
      if (inOl) {
        html.push('</ol>')
        inOl = false
      }
      if (!inUl) {
        html.push('<ul>')
        inUl = true
      }
      html.push(`<li>${inline(ul[1])}</li>`)
      continue
    }
    const ol = line.match(/^\s*\d+[.)]\s+(.*)$/)
    if (ol) {
      if (inUl) {
        html.push('</ul>')
        inUl = false
      }
      if (!inOl) {
        html.push('<ol>')
        inOl = true
      }
      html.push(`<li>${inline(ol[1])}</li>`)
      continue
    }
    if (inUl) {
      html.push('</ul>')
      inUl = false
    }
    if (inOl) {
      html.push('</ol>')
      inOl = false
    }
    if (line.trim() === '') continue
    html.push(`<p>${inline(line)}</p>`)
  }
  if (inUl) html.push('</ul>')
  if (inOl) html.push('</ol>')
  if (inCode) html.push('</code></pre>')
  return html.join('\n')
}

/** 组件内联编辑行结构（GroupedFailCase / WeeklyFailCase / MyTaskCase 均结构兼容） */
interface EditRow {
  id: number
  caseType?: string
  caseTypeDesc?: string
  caseName: string
  caseLog?: string
  status: number
  statusDesc?: string
  assigneeId?: number
  assigneeName?: string
  failReason?: string
  fixPlan?: string
  isBug?: number
  progress?: string
  conclusion?: string
  aiAnalysis?: string
  aiAnalysisCorrect?: number
  aiRootCause?: string
  aiEvidence?: string
  aiSolution?: string
  bugNo?: string
  issueCategory?: string
  taskNo?: string
  branch?: string
  version?: string
  projectName?: string
  moduleName?: string
  publishTime?: string
}

const props = withDefaults(
  defineProps<{
    rows: EditRow[]
    board: 'daily' | 'weekly' | 'dvs'
    userOptions: UserOption[]
    categories: IssueCategory[]
    redminePrefix: string
    isAdmin: boolean
    showSource?: boolean
  }>(),
  { showSource: false }
)

const emit = defineEmits<{
  (e: 'updated'): void
}>()

const userStore = useUserStore()

const statusOptions = [
  { value: 1, label: '待处理' },
  { value: 2, label: '处理中' },
  { value: 3, label: '已修复' },
  { value: 4, label: '非缺陷' }
]

const editing = reactive<Record<number, MyTaskCaseUpdateForm>>({})
const saving = reactive<Record<number, boolean>>({})
const aiLoading = reactive<Record<number, boolean>>({})
const expandedRowIds = ref<Set<number>>(new Set())

function toEditingForm(item: EditRow): MyTaskCaseUpdateForm {
  // 结论进展合并展示，以 progress 为主、conclusion 兜底（兼容历史数据），避免重复保存
  const merged = item.progress || item.conclusion || ''
  return {
    status: item.status,
    assigneeId: item.assigneeId,
    failReason: item.failReason || '',
    fixPlan: item.fixPlan || '',
    isBug: item.isBug ?? 0,
    aiAnalysisCorrect: item.aiAnalysisCorrect,
    progress: merged,
    conclusion: merged,
    bugNo: item.bugNo || '',
    issueCategory: item.issueCategory || ''
  }
}

// rows 变化时重建编辑态
watch(
  () => props.rows,
  (rows) => {
    Object.keys(editing).forEach((k) => delete editing[Number(k)])
    rows.forEach((c) => {
      editing[c.id] = toEditingForm(c)
    })
  },
  { immediate: true, deep: true }
)

/* ---------- 展开状态 ---------- */
function isExpanded(id: number): boolean {
  return expandedRowIds.value.has(id)
}

function onExpandChange(_row: any, expandedRows: any[] | boolean) {
  if (Array.isArray(expandedRows)) {
    expandedRowIds.value = new Set(expandedRows.map((r) => r.id))
  } else {
    expandedRowIds.value = new Set()
  }
}

/* ---------- 权限 ---------- */
function isMyCase(row: any): boolean {
  return row.assigneeId != null && row.assigneeId === userStore.userInfo?.id
}

function canOperate(row: any): boolean {
  if (props.isAdmin) return true
  if (row.status === 5) return false
  return isMyCase(row)
}

function assignDisabled(row: any): boolean {
  if (props.isAdmin) return false
  if (row.status === 5) return true
  return row.assigneeId != null && row.assigneeId !== userStore.userInfo?.id
}

type GuardField = 'failReason' | 'progress' | 'status' | 'aiAnalysisCorrect'

function statusReady(row: any): boolean {
  const f = editing[row.id]
  if (!f) return false
  const reason = f.failReason?.trim()
  const progress = f.progress?.trim()
  const aiOk = f.aiAnalysisCorrect === 1 || f.aiAnalysisCorrect === 0
  return !!(reason && progress && aiOk)
}

function fieldDisabled(row: any, field: GuardField): boolean {
  if (props.isAdmin) return false
  if (!canOperate(row)) return true
  if (field === 'status') return !statusReady(row)
  return false
}

function fieldDisabledTip(row: any, field: GuardField): string {
  if (props.isAdmin) return ''
  if (row.status === 5) return '已关闭用例仅可查看'
  if (!canOperate(row)) return '仅责任人为自己的问题单可操作'
  if (field === 'status') return '请完成所有信息后再更新'
  return ''
}

function saveDisabled(row: any): boolean {
  return !props.isAdmin && !canOperate(row)
}

function saveDisabledTip(row: any): string {
  if (props.isAdmin) return ''
  if (row.status === 5) return '已关闭用例仅可查看'
  return saveDisabled(row) ? '仅责任人为自己的问题单可操作' : ''
}

function statusClass(status: number | undefined): string {
  if (status === 2) return 'status-processing'
  if (status === 3 || status === 4) return 'status-fixed'
  return ''
}

/* ---------- 保存 / 指派 / AI ---------- */
async function saveCase(row: any) {
  const form = editing[row.id]
  form.conclusion = form.progress
  saving[row.id] = true
  try {
    if (props.board === 'daily') {
      await updateFailCaseApi(row.id, form)
    } else if (props.board === 'weekly') {
      await updateWeeklyFailCaseApi(row.id, form)
    } else {
      await updateDvsFailCaseApi(row.id, form)
    }
    ElMessage.success('保存成功')
    emit('updated')
  } finally {
    saving[row.id] = false
  }
}

async function quickAssign(row: any, val: number | string | undefined) {
  const assigneeId = val == null || val === '' ? null : Number(val)
  if (assigneeId === row.assigneeId) return
  if (!props.isAdmin) {
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
    if (props.board === 'daily') {
      await assignFailCaseApi(row.id, assigneeId)
    } else if (props.board === 'weekly') {
      await assignWeeklyFailCaseApi(row.id, assigneeId)
    } else {
      await assignDvsFailCaseApi(row.id, assigneeId)
    }
    row.assigneeId = assigneeId ?? undefined
    ElMessage.success(assigneeId == null ? '已取消指派' : '指派成功')
    emit('updated')
  } catch {
    editing[row.id].assigneeId = row.assigneeId
  } finally {
    saving[row.id] = false
  }
}

async function triggerAiAnalyze(row: any) {
  aiLoading[row.id] = true
  try {
    const res = props.board === 'daily'
      ? await aiAnalyzeDailyFailCaseApi(row.id)
      : props.board === 'weekly'
        ? await aiAnalyzeWeeklyFailCaseApi(row.id)
        : await aiAnalyzeDvsFailCaseApi(row.id)
    const r = res.data
    if (r) {
      row.aiRootCause = r.rootCause
      row.aiEvidence = r.evidence
      row.aiSolution = r.solution
      // 回填到编辑栏位：AI 结果优先，无结果时保留原值
      const form = editing[row.id]
      if (form) {
        form.failReason = r.rootCause || form.failReason
        form.fixPlan = r.solution || form.fixPlan
        form.progress = r.evidence || form.progress
      }
    }
    ElMessage.success('AI 分析结果已填入失败原因/修改方案/结论进展，请确认后保存')
  } catch {
    // 错误提示已在请求拦截器统一处理
  } finally {
    aiLoading[row.id] = false
  }
}

/** HTML 转义（防 XSS，避免日志内容中的 < > & 破坏渲染） */
function escapeHtml(s: string): string {
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

interface LogLineItem {
  text: string
  cls: string
}

/** 将日志拆成行并标记行类型（traceback 标题 / 文件行 / 代码行 / 异常行 / 普通行） */
function logLines(log: string): LogLineItem[] {
  const lines = log.split('\n')
  const items: LogLineItem[] = []
  let afterFile = false
  for (const line of lines) {
    const t = line.trim()
    let cls = 'log-normal'
    if (/^Traceback\b/.test(t)) {
      cls = 'log-tb-header'
    } else if (/^\s*File\s+"/.test(line)) {
      cls = 'log-tb-file'
      afterFile = true
    } else if (afterFile && /^\s{2,}\S/.test(line)) {
      cls = 'log-tb-code'
    } else if (/^[A-Za-z_]\w*(?:Error|Exception):/.test(t)) {
      cls = 'log-tb-error'
      afterFile = false
    } else {
      cls = 'log-normal'
      afterFile = false
    }
    items.push({ text: line, cls })
  }
  return items
}

/**
 * 单行关键字高亮（VS Code 报错面板风格）
 * 用一次正则扫描 + 回调替换，避免二次匹配产生嵌套 span：
 * - File "path"：蓝色；line N：浅蓝；in func：紫色
 * - xxxError / xxxException（异常名）：红色粗体
 * - warning / warn：黄色；error / exception / failed / traceback：红色
 */
function highlightLogLine(line: string): string {
  return escapeHtml(line).replace(
    /(File\s+&quot;[^&]*?&quot;)|(line\s+\d+)|(in\s+[A-Za-z_]\w*)|([A-Za-z_]\w*(?:Error|Exception))|(warning|Warning|WARN)|(\berror\b|\bError\b|\bERROR\b|\bexception\b|\bException\b|\btraceback\b|\bTraceback\b|\bfailed\b|\bFailed\b|\bFAIL\b)/g,
    (_m, file, ln, func, errName, warn, genericErr) => {
      if (file) return `<span class="kw-path">${file}</span>`
      if (ln) return `<span class="kw-line">${ln}</span>`
      if (func) return `<span class="kw-func">${func}</span>`
      if (errName) return `<span class="kw-err">${errName}</span>`
      if (warn) return `<span class="kw-warn">${warn}</span>`
      if (genericErr) return `<span class="kw-err">${genericErr}</span>`
      return _m
    }
  )
}

function jumpToBug(bugNo?: string) {
  if (!bugNo) return
  window.open(`${props.redminePrefix || ''}${bugNo}`, '_blank')
}
</script>

<style scoped>
.case-table {
  width: 100%;
}

.case-name {
  display: flex;
  align-items: center;
  gap: 6px;
}

.name-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.source-cell {
  line-height: 1.5;
}

.source-line {
  display: flex;
  align-items: center;
  gap: 6px;
}

.source-sub {
  margin-top: 4px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}

.branch {
  font-weight: 600;
  color: #303133;
}

.task-no {
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 12px;
  color: #909399;
}

.bug-no-cell {
  display: flex;
  align-items: center;
  gap: 4px;
  justify-content: center;
}

.status-cell {
  display: flex;
  justify-content: center;
}

.status-select {
  width: 110px;
}

.status-processing :deep(.el-select__wrapper) {
  background: #e6f4ff;
}

.status-fixed :deep(.el-select__wrapper) {
  background: #f0f9eb;
}

.text-muted {
  color: #909399;
}

.ml-4 {
  margin-left: 4px;
}

/* 展开行：AI 分析 */
.ai-block {
  padding: 8px 12px;
  background: #f8fafc;
  border-radius: 8px;
  margin-bottom: 10px;
}

.ai-title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.ai-item {
  display: flex;
  margin-bottom: 6px;
}

.ai-label {
  flex-shrink: 0;
  width: 64px;
  color: #909399;
  font-size: 12px;
  line-height: 24px;
}

.ai-text {
  flex: 1;
  color: #303133;
  font-size: 13px;
  line-height: 1.6;
  word-break: break-all;
}

/* Markdown 渲染出的内部元素样式 */
.ai-text :deep(p) {
  margin: 4px 0;
}

.ai-text :deep(h4),
.ai-text :deep(h5) {
  margin: 8px 0 4px;
  font-size: 13px;
  font-weight: 600;
  color: #1f2937;
}

.ai-text :deep(ul),
.ai-text :deep(ol) {
  margin: 4px 0;
  padding-left: 18px;
}

.ai-text :deep(li) {
  margin: 2px 0;
}

.ai-text :deep(strong) {
  font-weight: 600;
}

.ai-text :deep(code) {
  background: #f0f2f5;
  border-radius: 4px;
  padding: 1px 4px;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 12px;
  color: #c7254e;
}

.ai-text :deep(pre) {
  background: #1e1e1e;
  color: #d4d4d4;
  border-radius: 6px;
  padding: 8px 10px;
  margin: 6px 0;
  overflow-x: auto;
  font-size: 12px;
  line-height: 1.5;
}

.ai-text :deep(pre code) {
  background: transparent;
  color: inherit;
  padding: 0;
  white-space: pre;
}

.ai-empty {
  padding: 4px 0;
}

.ai-verdict {
  align-items: center;
  margin-top: 4px;
}

.case-log {
  padding: 10px 14px;
  background: #1e1e1e;
  border-radius: 8px;
  max-height: 320px;
  overflow: auto;
}

.log-title {
  font-weight: 600;
  color: #d4d4d4;
  margin-bottom: 8px;
  font-size: 13px;
}

.log-line {
  display: flex;
  font-family: 'JetBrains Mono', Consolas, 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.7;
  color: #d4d4d4;
  padding: 0 6px;
  border-radius: 4px;
}

.log-ln {
  flex-shrink: 0;
  width: 3em;
  text-align: right;
  margin-right: 12px;
  color: #6e7681;
  user-select: none;
}

.log-text {
  flex: 1;
  white-space: pre-wrap;
  word-break: break-all;
}

/* traceback 标题行 */
.log-tb-header {
  background: #2d333b;
  color: #9da7b3;
  margin: 4px 0;
}

/* File 文件行 */
.log-tb-file {
  background: #1f2430;
  color: #c9d1d9;
}

/* 代码行 */
.log-tb-code {
  color: #b3c7d6;
}

/* 异常行 */
.log-tb-error {
  background: #3d1f24;
  color: #ff7b72;
}

/* 关键字颜色（v-html 生成的 span，scoped 需 :deep） */
.log-text :deep(.kw-err) {
  color: #ff7b72;
  font-weight: 700;
}

.log-text :deep(.kw-warn) {
  color: #d29922;
  font-weight: 600;
}

.log-text :deep(.kw-path) {
  color: #79c0ff;
}

.log-text :deep(.kw-line) {
  color: #79c0ff;
}

.log-text :deep(.kw-func) {
  color: #d2a8ff;
}
</style>
