<template>
  <div class="weekly-task-page">
    <el-card shadow="never" class="mb-16">
      <template #header>
        <div class="page-header">
          <div class="page-title">Weekly_Sanity</div>
          <el-button type="primary" @click="onPickReport">上传测试报告</el-button>
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
            :row-class-name="rowClassName"
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
            <el-table-column label="模块 / 失败脚本" min-width="240" show-overflow-tooltip>
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
                    <ResizeTipTextarea
                      v-if="isExpanded(toCase(row).id)"
                      :key="`fr-on-${toCase(row).id}`"
                      v-model="editing[toCase(row).id].failReason"
                      :autosize="{ minRows: 1, maxRows: 200 }"
                      placeholder="填写失败原因"
                      :disabled="fieldDisabled(toCase(row), 'failReason')"
                    />
                    <ResizeTipTextarea
                      v-else
                      :key="`fr-off-${toCase(row).id}`"
                      v-model="editing[toCase(row).id].failReason"
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
                    <ResizeTipTextarea
                      v-if="isExpanded(toCase(row).id)"
                      :key="`pr-on-${toCase(row).id}`"
                      v-model="editing[toCase(row).id].progress"
                      :autosize="{ minRows: 1, maxRows: 200 }"
                      placeholder="填写分析进展与结论"
                      :disabled="fieldDisabled(toCase(row), 'progress')"
                    />
                    <ResizeTipTextarea
                      v-else
                      :key="`pr-off-${toCase(row).id}`"
                      v-model="editing[toCase(row).id].progress"
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
                    v-model="editing[toCase(row).id].assigneeId"
                    placeholder="选择"
                    clearable
                    :disabled="assignDisabled(toCase(row))"
                    :loading="saving[toCase(row).id]"
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
                  <div :class="['status-cell', statusClass(editing[(toCase(row)).id].status)]">
                    <el-select
                      v-model="editing[(toCase(row)).id].status"
                      placeholder="状态"
                      :disabled="fieldDisabled(toCase(row), 'status')"
                      class="status-select"
                    >
                      <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
                      <el-option key="closed" label="已关闭" :value="5" :disabled="!isAdmin" />
                    </el-select>
                  </div>
                </el-tooltip>
              </template>
            </el-table-column>
            <el-table-column label="AI分析描述" min-width="200">
              <template #default="{ row }">
                <el-tooltip placement="top" :show-after="150" :disabled="!row.aiAnalysis || isExpanded(toCase(row).id)" popper-class="ai-tip-popper">
                  <template #content>
                    <div class="ai-tip-content">{{ row.aiAnalysis }}</div>
                  </template>
                  <div :class="['ai-cell', { 'ai-cell-expanded': isExpanded(toCase(row).id) }]">{{ row.aiAnalysis || '暂无分析' }}</div>
                </el-tooltip>
              </template>
            </el-table-column>
            <el-table-column label="AI分析判断" width="110" align="center">
              <template #default="{ row }">
                <el-tooltip :content="fieldDisabledTip(toCase(row), 'aiAnalysisCorrect')" placement="top" :disabled="!fieldDisabled(toCase(row), 'aiAnalysisCorrect')">
                  <div>
                    <el-select v-model="editing[toCase(row).id].aiAnalysisCorrect" placeholder="" :disabled="fieldDisabled(toCase(row), 'aiAnalysisCorrect')" style="width: 70px">
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
                    <el-button type="primary" size="small" :loading="saving[toCase(row).id]" :disabled="saveDisabled(toCase(row))" @click="saveCase(toCase(row))">保存</el-button>
                  </div>
                </el-tooltip>
              </template>
            </el-table-column>
          </el-table>
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
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  assignWeeklyFailCaseApi,
  confirmWeeklyReportApi,
  getWeeklyEnabledUsersApi,
  getWeeklyGroupedCasesApi,
  getWeeklyRecentWeekStatsApi,
  getWeeklyReportContentApi,
  previewWeeklyReportApi,
  updateWeeklyFailCaseApi
} from '@/api/weekly'
import { getProjectEnabledApi } from '@/api/release'
import type {
  WeeklyFailCase,
  WeeklyFailCaseGrouped,
  WeeklyFailCaseUpdateForm,
  WeeklyRecentDayStat,
  WeeklyReportPreviewVO
} from '@/types/weekly'
import type { ReleaseProject, UserOption } from '@/types/release'
import { useUserStore } from '@/store/user'
import { ArrowDown, ArrowRight } from '@element-plus/icons-vue'
import ResizeTipTextarea from '@/components/ResizeTipTextarea.vue'

const userStore = useUserStore()

// 管理员（超管/普通管理员）不受前置条件限制；普通员工遵循限制
const isAdmin = computed(() => {
  const codes = userStore.userInfo?.roleCodes || []
  return codes.includes('super_admin') || codes.includes('admin')
})

const loading = ref(false)
const groups = ref<WeeklyFailCaseGrouped[]>([])
const activeNames = ref<string[]>([])
const userOptions = ref<UserOption[]>([])
const weekStats = ref<WeeklyRecentDayStat[]>([])
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

const editing = reactive<Record<number, WeeklyFailCaseUpdateForm>>({})

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

/** 将 el-table 插槽的 DefaultRow 转换为强类型用例对象 */
function toCase(row: any): WeeklyFailCase {
  return row as WeeklyFailCase
}

/** 行展开状态：用于让行内长文本栏位（失败原因/结论进展/AI 分析）按内容自适应撑高 */
const expandedRowIds = ref<Set<number>>(new Set())

/** 判断某行是否处于展开状态 */
function isExpanded(id: number): boolean {
  return expandedRowIds.value.has(id)
}

/** el-table expand 事件：把当前所有展开行同步到 expandedRowIds */
function onExpandChange(_row: WeeklyFailCase, expandedRows: WeeklyFailCase[] | boolean) {
  if (Array.isArray(expandedRows)) {
    expandedRowIds.value = new Set(expandedRows.map((r) => r.id))
  } else {
    expandedRowIds.value = new Set()
  }
}

// 每个分组当前选中的原始报告文件ID（仅用于展示选中项）
const reportSel = reactive<Record<string, number | undefined>>({})

function groupKey(group: WeeklyFailCaseGrouped) {
  return `${group.branch}@@${group.projectName}`
}

function resetGroupFilter(group: WeeklyFailCaseGrouped) {
  const key = groupKey(group)
  assigneeFilter[key] = undefined
  statusFilter[key] = undefined
}

function visibleCases(group: WeeklyFailCaseGrouped): WeeklyFailCase[] {
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

/** 当前行责任人是当前登录用户 */
function isMyCase(row: WeeklyFailCase): boolean {
  return row.assigneeId != null && row.assigneeId === userStore.userInfo?.id
}

/** 普通用户是否有权操作该行（责任人是自己；管理员不受限；已关闭仅管理员可操作） */
function canOperate(row: WeeklyFailCase): boolean {
  if (isAdmin.value) return true
  if (row.status === 5) return false
  return isMyCase(row)
}

/** 责任人下拉是否禁用：普通用户不能操作他人责任行；已关闭仅管理员可操作 */
function assignDisabled(row: WeeklyFailCase): boolean {
  if (isAdmin.value) return false
  if (row.status === 5) return true
  return row.assigneeId != null && row.assigneeId !== userStore.userInfo?.id
}

/** 状态栏位前置条件：失败原因、结论进展、AI分析判断(Y/N)均已填写 */
function statusReady(row: WeeklyFailCase): boolean {
  const f = editing[row.id]
  if (!f) return false
  const reason = f.failReason?.trim()
  const progress = f.progress?.trim()
  const aiOk = f.aiAnalysisCorrect === 1 || f.aiAnalysisCorrect === 0
  return !!(reason && progress && aiOk)
}

type GuardField = 'failReason' | 'progress' | 'status' | 'aiAnalysisCorrect'

/** 是否禁用指定栏位（普通用户仅可编辑责任人是自己的行，管理员豁免） */
function fieldDisabled(row: WeeklyFailCase, field: GuardField): boolean {
  if (isAdmin.value) return false
  if (!canOperate(row)) return true
  if (field === 'status') return !statusReady(row)
  return false
}

function fieldDisabledTip(row: WeeklyFailCase, field: GuardField): string {
  if (isAdmin.value) return ''
  if (row.status === 5) return '已关闭用例仅可查看'
  if (!canOperate(row)) return '仅责任人为自己的问题单可操作'
  if (field === 'status') return '请完成所有信息后再更新'
  return ''
}

/** 保存按钮：普通用户仅责任人是自己的行可保存 */
function saveDisabled(row: WeeklyFailCase): boolean {
  return !isAdmin.value && !canOperate(row)
}

function saveDisabledTip(row: WeeklyFailCase): string {
  if (isAdmin.value) return ''
  if (row.status === 5) return '已关闭用例仅可查看'
  return saveDisabled(row) ? '仅责任人为自己的问题单可操作' : ''
}

/** 状态栏位样式类名 */
function statusClass(status: number | undefined): string {
  if (status === 2) return 'status-processing'
  if (status === 3 || status === 4) return 'status-fixed'
  return ''
}

/** 分组状态统计：待处理/处理中/已修复(含非缺陷)/分析完成率 */
function groupStatusStats(group: WeeklyFailCaseGrouped) {
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
async function openReport(group: WeeklyFailCaseGrouped, fileId?: number) {
  if (!fileId) return
  try {
    const blob = await getWeeklyReportContentApi(fileId)
    const url = URL.createObjectURL(blob)
    window.open(url, '_blank')
    // 新窗口加载后释放 Blob URL，避免内存占用
    setTimeout(() => URL.revokeObjectURL(url), 60_000)
  } catch {
    // 错误提示已在请求拦截器统一处理
  }
}

/** 打开模块的原始 HTML 报告（新窗口） */
async function openModuleReport(fileId: number) {
  try {
    const blob = await getWeeklyReportContentApi(fileId)
    const url = URL.createObjectURL(blob)
    window.open(url, '_blank')
    setTimeout(() => URL.revokeObjectURL(url), 60_000)
  } catch {
    // 错误提示已在请求拦截器统一处理
  }
}

function toEditingForm(item: WeeklyFailCase): WeeklyFailCaseUpdateForm {
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
    const res = await getWeeklyEnabledUsersApi()
    userOptions.value = res.data || []
  } catch (e) {
    // ignore
  }
}

async function loadWeekStats() {
  try {
    const res = await getWeeklyRecentWeekStatsApi()
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
function rateText(day: WeeklyRecentDayStat): string {
  if (day.totalCount === 0 || day.rate === null || day.rate === undefined) return 'None'
  return `${day.rate}%`
}

/** 完成率样式：100%绿 / 未完成橙 / 无数据灰 */
function rateClass(day: WeeklyRecentDayStat): string {
  if (day.totalCount === 0 || day.rate === null || day.rate === undefined) return 'none'
  return day.rate >= 100 ? 'ok' : 'warn'
}

function barWidth(day: WeeklyRecentDayStat): string {
  if (day.totalCount === 0 || day.rate === null || day.rate === undefined) return '0%'
  return `${day.rate}%`
}

function pendingCount(day: WeeklyRecentDayStat): number {
  return Math.max(day.totalCount - day.analyzedCount, 0)
}

async function loadData() {
  loading.value = true
  try {
    const res = await getWeeklyGroupedCasesApi({
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
async function quickAssign(row: WeeklyFailCase, val: number | string | undefined) {
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
    await assignWeeklyFailCaseApi(row.id, assigneeId)
    row.assigneeId = assigneeId ?? undefined
    ElMessage.success(assigneeId == null ? '已取消指派' : '指派成功')
  } catch {
    // 接口失败时回滚下拉显示值，错误信息由拦截器统一提示
    editing[row.id].assigneeId = row.assigneeId
  } finally {
    saving[row.id] = false
  }
}

async function saveCase(row: WeeklyFailCase) {
  const form = editing[row.id]
  // 结论进展栏位合并展示 progress+conclusion，保存时两字段保持一致，
  // 避免删除栏位内容后，残留的 conclusion 旧值在下一次加载时又被合并回流
  form.conclusion = form.progress
  saving[row.id] = true
  try {
    await updateWeeklyFailCaseApi(row.id, form)
    ElMessage.success('保存成功')
    await loadData()
  } finally {
    saving[row.id] = false
  }
}

/* ==================== 上传测试报告 ==================== */

const fileInputRef = ref<HTMLInputElement>()
const previewVisible = ref(false)
const previewLoading = ref(false)
const submitting = ref(false)
const previewData = ref<WeeklyReportPreviewVO | null>(null)
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

async function onFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const files = Array.from(input.files || [])
  input.value = ''
  if (!files.length) return
  previewLoading.value = true
  try {
    const res = await previewWeeklyReportApi(files)
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
    await confirmWeeklyReportApi({ ...confirmForm })
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
})
</script>

<style scoped>
.weekly-task-page {
  padding: 16px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
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
