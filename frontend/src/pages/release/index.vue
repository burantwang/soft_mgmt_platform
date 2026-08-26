<template>
  <div>
    <!-- 搜索栏 -->
    <el-card shadow="never" class="mb-16">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="分支">
          <el-select v-model="query.branch" placeholder="全部" clearable filterable style="width: 180px">
            <el-option v-for="b in branches" :key="b" :label="b" :value="b" />
          </el-select>
        </el-form-item>
        <el-form-item label="结果">
          <el-select v-model="query.result" placeholder="全部" clearable style="width: 110px">
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="来源">
          <el-select v-model="query.source" placeholder="全部" clearable style="width: 130px">
            <el-option label="人工上传" :value="1" />
            <el-option label="Jenkins推送" :value="2" />
            <el-option label="手动创建" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="机型">
          <el-select v-model="query.projectId" placeholder="全部" clearable filterable style="width: 170px">
            <el-option v-for="p in projects" :key="p.id" :label="p.projectName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="发布时间">
          <el-date-picker
            v-model="query.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            :shortcuts="timeShortcuts"
            style="width: 380px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">查询</el-button>
          <el-button @click="onReset">重置</el-button>
          <el-button type="success" :loading="exporting" @click="onExport">导出 Excel</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表 -->
    <el-card shadow="never">
      <div class="table-toolbar">
        <el-button type="primary" @click="onPickReport">上传测试报告</el-button>
        <el-button @click="openCreate">手动创建</el-button>
        <el-button @click="openHistory">查看历史</el-button>
        <span class="text-muted" style="margin-left: 8px">支持上传 pytest-html 报告，解析后自动生成发布记录；失败报告自动生成失败任务</span>
        <span class="toolbar-right">
          <el-switch v-model="autoRefresh" active-text="自动刷新" @change="onAutoRefreshChange" />
          <span class="text-muted auto-refresh-tip">上次刷新 {{ lastRefreshTime }}</span>
        </span>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="branch" label="分支" min-width="130" show-overflow-tooltip />
        <el-table-column prop="version" label="版本" min-width="110">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.version || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="机型" min-width="160">
          <template #default="{ row }">
            <template v-if="row.projectNames?.length">
              <el-tag v-for="p in row.projectNames" :key="p" size="small" class="mr-4">{{ p }}</el-tag>
            </template>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="结果" width="80">
          <template #default="{ row }">
            <el-tag :type="row.result === 1 ? 'success' : 'danger'" size="small">
              {{ row.resultDesc }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="通过率" width="90">
          <template #default="{ row }">
            <span v-if="row.totalCount > 0">{{ row.passRate }}%</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="用例统计" min-width="140">
          <template #default="{ row }">
            <span v-if="row.totalCount > 0">
              {{ row.totalCount }} 用例 ·
              <span class="ok-text">{{ row.passedCount }}通过</span>
              <template v-if="row.failedCount > 0"> · <span class="bad-text">{{ row.failedCount }}失败</span></template>
              <template v-if="row.errorCount > 0"> · <span class="bad-text">{{ row.errorCount }}错误</span></template>
              <template v-if="row.skippedCount > 0"> · {{ row.skippedCount }}跳过</template>
            </span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="sourceDesc" label="来源" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.source === 2 ? 'warning' : row.source === 1 ? 'primary' : 'info'">
              {{ row.sourceDesc }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="publisherName" label="发布人" width="100">
          <template #default="{ row }">
            {{ row.publisherName || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="publishTime" label="发布时间" width="170" />
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row as ReleaseRecordVO)">查看</el-button>
            <el-button link type="primary" @click="openEdit(row as ReleaseRecordVO)">编辑</el-button>
            <el-button link type="danger" @click="onDelete(row as ReleaseRecordVO)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-pagination">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          layout="total, sizes, prev, pager, next"
          :page-sizes="[10, 20, 50]"
          @change="onPageChange"
        />
      </div>
    </el-card>

    <!-- 隐藏的文件选择框 -->
    <input ref="fileInputRef" type="file" accept=".html,.htm" style="display: none" @change="onFileChange" />

    <!-- 上传预览弹窗 -->
    <el-dialog v-model="previewVisible" title="报告解析预览" width="720px" destroy-on-close>
      <div v-loading="previewLoading" class="preview-body">
        <el-alert
          :type="preview.result === 1 ? 'success' : 'error'"
          :title="`解析结果：${preview.resultDesc}${preview.failCases?.length ? `（失败/错误用例 ${preview.failCases.length} 条）` : ''}`"
          :closable="false"
          show-icon
          class="mb-16"
        />
        <el-descriptions :column="3" border size="small" class="mb-16">
          <el-descriptions-item label="报告文件">{{ preview.fileName }}</el-descriptions-item>
          <el-descriptions-item label="文件大小">{{ formatSize(preview.fileSize) }}</el-descriptions-item>
          <el-descriptions-item label="版本">{{ preview.version || '-' }}</el-descriptions-item>
          <el-descriptions-item label="用例总数">{{ preview.totalCount }}</el-descriptions-item>
          <el-descriptions-item label="通过/失败/错误/跳过">
            {{ preview.passedCount }} / {{ preview.failedCount }} / {{ preview.errorCount }} / {{ preview.skippedCount }}
          </el-descriptions-item>
          <el-descriptions-item label="总耗时">{{ preview.durationSec != null ? preview.durationSec + 's' : '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-form ref="confirmFormRef" :model="confirmForm" :rules="confirmRules" label-width="90px">
          <el-form-item label="代码分支" prop="branch">
            <el-input v-model="confirmForm.branch" placeholder="如 master / release/v1.2" />
          </el-form-item>
          <el-form-item label="镜像版本" prop="version">
            <el-input v-model="confirmForm.version" placeholder="留空使用报告内 Environment.Version" />
          </el-form-item>
          <el-form-item label="关联机型" prop="projectIds">
            <el-select v-model="confirmForm.projectIds" multiple placeholder="选择本次发布覆盖的机型" style="width: 100%">
              <el-option v-for="p in projects" :key="p.id" :label="p.projectName" :value="p.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="confirmForm.remark" type="textarea" :rows="2" placeholder="备注（可选）" />
          </el-form-item>
        </el-form>

        <template v-if="preview.failCases?.length">
          <el-divider content-position="left">失败/错误用例明细（{{ preview.failCases.length }} 条）</el-divider>
          <el-table :data="preview.failCases" border size="small" max-height="260">
            <el-table-column type="index" width="50" />
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

    <!-- 手动创建弹窗 -->
    <el-dialog v-model="createVisible" title="手动创建发布记录" width="520px" destroy-on-close>
      <el-form ref="createFormRef" :model="createForm" :rules="recordRules" label-width="90px">
        <el-form-item label="代码分支" prop="branch">
          <el-input v-model="createForm.branch" placeholder="如 master / release/v1.2" />
        </el-form-item>
        <el-form-item label="镜像版本">
          <el-input v-model="createForm.version" placeholder="版本号（可选）" />
        </el-form-item>
        <el-form-item label="发布结果" prop="result">
          <el-radio-group v-model="createForm.result">
            <el-radio-button :value="1">成功</el-radio-button>
            <el-radio-button :value="2">失败</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="关联机型" prop="projectIds">
          <el-select v-model="createForm.projectIds" multiple placeholder="选择机型" style="width: 100%">
            <el-option v-for="p in projects" :key="p.id" :label="p.projectName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createForm.remark" type="textarea" :rows="2" placeholder="备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCreate">确定</el-button>
      </template>
    </el-dialog>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editVisible" title="编辑发布记录" width="520px" destroy-on-close>
      <el-form ref="editFormRef" :model="editForm" :rules="recordRules" label-width="90px">
        <el-form-item label="代码分支" prop="branch">
          <el-input v-model="editForm.branch" placeholder="代码分支" />
        </el-form-item>
        <el-form-item label="镜像版本">
          <el-input v-model="editForm.version" placeholder="版本号（可选）" />
        </el-form-item>
        <el-form-item label="关联机型" prop="projectIds">
          <el-select v-model="editForm.projectIds" multiple placeholder="选择机型" style="width: 100%">
            <el-option v-for="p in projects" :key="p.id" :label="p.projectName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editForm.remark" type="textarea" :rows="2" placeholder="备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="发布记录详情" width="680px" destroy-on-close>
      <template v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="代码分支">{{ detail.branch }}</el-descriptions-item>
          <el-descriptions-item label="镜像版本">{{ detail.version || '-' }}</el-descriptions-item>
          <el-descriptions-item label="发布结果">
            <el-tag :type="detail.result === 1 ? 'success' : 'danger'" size="small">{{ detail.resultDesc }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="来源">
            <el-tag size="small" type="info">{{ detail.sourceDesc }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="关联机型">
            <el-tag v-for="p in detail.projectNames" :key="p" size="small" class="mr-4">{{ p }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="报告文件">{{ detail.reportFileName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="用例统计">
            <span v-if="detail.totalCount > 0">
              {{ detail.totalCount }} 用例 · 通过 {{ detail.passedCount }} · 失败 {{ detail.failedCount }} ·
              错误 {{ detail.errorCount }} · 跳过 {{ detail.skippedCount }} · 通过率 {{ detail.passRate }}%
            </span>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="总耗时">{{ detail.durationSec != null ? detail.durationSec + 's' : '-' }}</el-descriptions-item>
          <el-descriptions-item label="报告时间">{{ detail.reportTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="发布时间">{{ detail.publishTime }}</el-descriptions-item>
          <el-descriptions-item label="发布人">{{ detail.publisherName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
        <el-alert
          v-if="detail.failTaskId"
          title="该发布存在失败用例，已自动生成失败聚合任务，可在「失败任务追踪」中查看处理。"
          type="warning"
          :closable="false"
          show-icon
          class="mt-16"
        />
      </template>
    </el-dialog>

    <!-- 历史发布弹窗 -->
    <el-dialog v-model="historyVisible" title="历史版本发布" width="1100px" top="5vh" destroy-on-close>
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="分支">
          <el-select v-model="historyQuery.branch" placeholder="全部" clearable filterable style="width: 160px">
            <el-option v-for="b in branches" :key="b" :label="b" :value="b" />
          </el-select>
        </el-form-item>
        <el-form-item label="结果">
          <el-select v-model="historyQuery.result" placeholder="全部" clearable style="width: 100px">
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="来源">
          <el-select v-model="historyQuery.source" placeholder="全部" clearable style="width: 120px">
            <el-option label="人工上传" :value="1" />
            <el-option label="Jenkins推送" :value="2" />
            <el-option label="手动创建" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="机型">
          <el-select v-model="historyQuery.projectId" placeholder="全部" clearable filterable style="width: 150px">
            <el-option v-for="p in projects" :key="p.id" :label="p.projectName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="发布时间">
          <el-date-picker
            v-model="historyQuery.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            :shortcuts="timeShortcuts"
            style="width: 340px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onHistorySearch">查询</el-button>
          <el-button @click="onHistoryReset">重置</el-button>
          <el-button type="success" :loading="historyExporting" @click="onHistoryExport">导出 Excel</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="historyLoading" :data="historyList" border stripe max-height="480">
        <el-table-column prop="branch" label="分支" min-width="130" show-overflow-tooltip />
        <el-table-column prop="version" label="版本" min-width="100">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.version || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="机型" min-width="140">
          <template #default="{ row }">
            <template v-if="row.projectNames?.length">
              <el-tag v-for="p in row.projectNames" :key="p" size="small" class="mr-4">{{ p }}</el-tag>
            </template>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="结果" width="80">
          <template #default="{ row }">
            <el-tag :type="row.result === 1 ? 'success' : 'danger'" size="small">{{ row.resultDesc }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="通过率" width="90">
          <template #default="{ row }">
            <span v-if="row.totalCount > 0">{{ row.passRate }}%</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="用例统计" min-width="140">
          <template #default="{ row }">
            <span v-if="row.totalCount > 0">
              {{ row.totalCount }} 用例 ·
              <span class="ok-text">{{ row.passedCount }}通过</span>
              <template v-if="row.failedCount > 0"> · <span class="bad-text">{{ row.failedCount }}失败</span></template>
              <template v-if="row.errorCount > 0"> · <span class="bad-text">{{ row.errorCount }}错误</span></template>
            </span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="sourceDesc" label="来源" width="100" />
        <el-table-column prop="publisherName" label="发布人" width="90" />
        <el-table-column prop="publishTime" label="发布时间" width="170" />
        <el-table-column label="操作" width="70" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row as ReleaseRecordVO)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-pagination">
        <el-pagination
          v-model:current-page="historyQuery.page"
          v-model:page-size="historyQuery.size"
          :total="historyTotal"
          layout="total, sizes, prev, pager, next"
          :page-sizes="[10, 20, 50]"
          @change="onHistoryPageChange"
        />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import {
  confirmReportApi,
  createReleaseApi,
  deleteReleaseApi,
  exportRecordsApi,
  getBranchOptionsApi,
  getProjectEnabledApi,
  getReleasePageApi,
  previewReportApi,
  updateReleaseApi
} from '@/api/release'
import type { ReleaseProject, ReleaseRecordVO, ReportPreviewVO } from '@/types/release'

const REFRESH_INTERVAL = 60_000

const loading = ref(false)
const submitting = ref(false)
const exporting = ref(false)
const list = ref<ReleaseRecordVO[]>([])
const total = ref(0)
const branches = ref<string[]>([])
const projects = ref<ReleaseProject[]>([])

/* ---------- 自动刷新（等待新报告上传 / Jenkins 推送自动出现） ---------- */
const autoRefresh = ref(true)
const lastRefreshTime = ref('')
let refreshTimer: number | undefined

function fmtTime(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

function fmtDateTime(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

function dayRangeStr(offsetDays: number): [string, string] {
  const start = new Date()
  start.setHours(0, 0, 0, 0)
  start.setDate(start.getDate() + offsetDays)
  const end = new Date(start)
  end.setHours(23, 59, 59, 999)
  return [fmtDateTime(start), fmtDateTime(end)]
}

function lastNDaysStr(n: number): [string, string] {
  const end = new Date()
  end.setHours(23, 59, 59, 999)
  const start = new Date()
  start.setDate(start.getDate() - (n - 1))
  start.setHours(0, 0, 0, 0)
  return [fmtDateTime(start), fmtDateTime(end)]
}

const timeShortcuts = [
  { text: '今天', value: () => dayRangeStr(0) },
  { text: '昨天', value: () => dayRangeStr(-1) },
  { text: '近7天', value: () => lastNDaysStr(7) },
  { text: '近30天', value: () => lastNDaysStr(30) }
]

function startAutoRefresh() {
  stopAutoRefresh()
  refreshTimer = window.setInterval(() => loadData(true), REFRESH_INTERVAL)
}

function stopAutoRefresh() {
  if (refreshTimer) {
    window.clearInterval(refreshTimer)
    refreshTimer = undefined
  }
}

function onAutoRefreshChange(on: boolean | string | number) {
  if (on) {
    startAutoRefresh()
  } else {
    stopAutoRefresh()
  }
}

/* 切回页面时立即刷新，保证新上传 / Jenkins 推送及时可见 */
function onVisibilityChange() {
  if (!document.hidden && autoRefresh.value) {
    loadData(true)
  }
}

/* ---------- 主查询 ---------- */
const query = reactive({
  branch: '',
  result: undefined as number | undefined,
  source: undefined as number | undefined,
  projectId: undefined as number | undefined,
  timeRange: dayRangeStr(0),
  page: 1,
  size: 10
})

function buildParams(includePage: boolean): Record<string, unknown> {
  const params: Record<string, unknown> = {
    branch: query.branch || undefined,
    result: query.result,
    source: query.source,
    projectId: query.projectId
  }
  if (query.timeRange) {
    params.startTime = query.timeRange[0]
    params.endTime = query.timeRange[1]
  }
  if (includePage) {
    params.page = query.page
    params.size = query.size
  }
  return params
}

async function loadData(silent = false) {
  if (!silent) loading.value = true
  try {
    const res = await getReleasePageApi(buildParams(true))
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    lastRefreshTime.value = fmtTime(new Date())
    if (!silent) loading.value = false
  }
}

function onSearch() {
  query.page = 1
  loadData()
}

function onReset() {
  query.branch = ''
  query.result = undefined
  query.source = undefined
  query.projectId = undefined
  query.timeRange = dayRangeStr(0)
  onSearch()
}

function onPageChange() {
  loadData()
}

/* ---------- 导出 ---------- */
function download(blob: Blob) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  const stamp = `${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}${pad(now.getHours())}${pad(now.getMinutes())}${pad(now.getSeconds())}`
  a.href = url
  a.download = `发布记录_${stamp}.xlsx`
  a.click()
  URL.revokeObjectURL(url)
}

async function onExport() {
  exporting.value = true
  try {
    const blob = await exportRecordsApi(buildParams(false))
    download(blob)
    ElMessage.success('导出成功')
  } finally {
    exporting.value = false
  }
}

/* ---------- 历史发布 ---------- */
const historyVisible = ref(false)
const historyLoading = ref(false)
const historyExporting = ref(false)
const historyList = ref<ReleaseRecordVO[]>([])
const historyTotal = ref(0)
const historyQuery = reactive({
  branch: '',
  result: undefined as number | undefined,
  source: undefined as number | undefined,
  projectId: undefined as number | undefined,
  timeRange: null as [string, string] | null,
  page: 1,
  size: 10
})

function buildHistoryParams(includePage: boolean): Record<string, unknown> {
  const params: Record<string, unknown> = {
    branch: historyQuery.branch || undefined,
    result: historyQuery.result,
    source: historyQuery.source,
    projectId: historyQuery.projectId
  }
  if (historyQuery.timeRange) {
    params.startTime = historyQuery.timeRange[0]
    params.endTime = historyQuery.timeRange[1]
  }
  if (includePage) {
    params.page = historyQuery.page
    params.size = historyQuery.size
  }
  return params
}

function openHistory() {
  historyVisible.value = true
  loadHistory()
}

async function loadHistory() {
  historyLoading.value = true
  try {
    const res = await getReleasePageApi(buildHistoryParams(true))
    historyList.value = res.data.records
    historyTotal.value = res.data.total
  } finally {
    historyLoading.value = false
  }
}

function onHistorySearch() {
  historyQuery.page = 1
  loadHistory()
}

function onHistoryReset() {
  historyQuery.branch = ''
  historyQuery.result = undefined
  historyQuery.source = undefined
  historyQuery.projectId = undefined
  historyQuery.timeRange = null
  onHistorySearch()
}

function onHistoryPageChange() {
  loadHistory()
}

async function onHistoryExport() {
  historyExporting.value = true
  try {
    const blob = await exportRecordsApi(buildHistoryParams(false))
    download(blob)
    ElMessage.success('导出成功')
  } finally {
    historyExporting.value = false
  }
}

/* ---------- 上传预览 ---------- */
const fileInputRef = ref<HTMLInputElement>()
const previewVisible = ref(false)
const previewLoading = ref(false)
const preview = ref<ReportPreviewVO>({} as ReportPreviewVO)
const confirmFormRef = ref<FormInstance>()
const confirmForm = reactive({ previewToken: '', branch: '', version: '', projectIds: [] as number[], remark: '' })
const confirmRules = {
  branch: [{ required: true, message: '请输入代码分支', trigger: 'blur' }],
  projectIds: [{ required: true, type: 'array', min: 1, message: '请至少选择一个机型', trigger: 'change' }]
}

function onPickReport() {
  fileInputRef.value?.click()
}

async function onFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  previewLoading.value = true
  previewVisible.value = true
  try {
    const res = await previewReportApi(file)
    preview.value = res.data
    confirmForm.previewToken = res.data.previewToken
    confirmForm.branch = ''
    confirmForm.version = res.data.version || ''
    confirmForm.projectIds = []
    confirmForm.remark = ''
  } finally {
    previewLoading.value = false
  }
}

async function confirmReport() {
  await confirmFormRef.value?.validate()
  submitting.value = true
  try {
    await confirmReportApi({ ...confirmForm })
    ElMessage.success('入库成功')
    previewVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

/* ---------- 手动创建 ---------- */
const createVisible = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = reactive({ branch: '', version: '', result: 1, projectIds: [] as number[], remark: '' })
const recordRules = {
  branch: [{ required: true, message: '请输入代码分支', trigger: 'blur' }],
  projectIds: [{ required: true, type: 'array', min: 1, message: '请至少选择一个机型', trigger: 'change' }]
}

function openCreate() {
  Object.assign(createForm, { branch: '', version: '', result: 1, projectIds: [], remark: '' })
  createVisible.value = true
}

async function submitCreate() {
  await createFormRef.value?.validate()
  submitting.value = true
  try {
    await createReleaseApi({ ...createForm })
    ElMessage.success('创建成功')
    createVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

/* ---------- 编辑 ---------- */
const editVisible = ref(false)
const editFormRef = ref<FormInstance>()
const editForm = reactive({ id: 0, branch: '', version: '', projectIds: [] as number[], remark: '' })

function openEdit(row: ReleaseRecordVO) {
  Object.assign(editForm, {
    id: row.id,
    branch: row.branch,
    version: row.version || '',
    projectIds: [...(row.projectIds || [])],
    remark: row.remark || ''
  })
  editVisible.value = true
}

async function submitEdit() {
  await editFormRef.value?.validate()
  submitting.value = true
  try {
    await updateReleaseApi({ ...editForm })
    ElMessage.success('保存成功')
    editVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

/* ---------- 删除 ---------- */
async function onDelete(row: ReleaseRecordVO) {
  await ElMessageBox.confirm(`确认删除发布记录「${row.branch}${row.version ? '@' + row.version : ''}」？关联报告文件与失败任务将一并删除。`, '删除确认', { type: 'warning' })
  await deleteReleaseApi(row.id)
  ElMessage.success('删除成功')
  loadData()
}

/* ---------- 详情 ---------- */
const detailVisible = ref(false)
const detail = ref<ReleaseRecordVO>()

function openDetail(row: ReleaseRecordVO) {
  detail.value = row
  detailVisible.value = true
}

/* ---------- 工具 ---------- */
function formatSize(size?: number): string {
  if (size == null) return '-'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
  return (size / 1024 / 1024).toFixed(2) + ' MB'
}

async function loadProjects() {
  projects.value = (await getProjectEnabledApi()).data
}

async function loadBranches() {
  branches.value = (await getBranchOptionsApi()).data || []
}

onMounted(() => {
  loadData()
  loadProjects()
  loadBranches()
  if (autoRefresh.value) startAutoRefresh()
  document.addEventListener('visibilitychange', onVisibilityChange)
})

onBeforeUnmount(() => {
  stopAutoRefresh()
  document.removeEventListener('visibilitychange', onVisibilityChange)
})
</script>

<style scoped>
.mb-16 {
  margin-bottom: 16px;
}

.mt-16 {
  margin-top: 16px;
}

.mr-4 {
  margin-right: 4px;
}

.table-toolbar {
  margin-bottom: 14px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}

.toolbar-right {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
}

.auto-refresh-tip {
  margin-left: 8px;
  font-size: 12px;
}

.table-pagination {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}

.text-muted {
  color: #909399;
}

.ok-text {
  color: #67c23a;
}

.bad-text {
  color: #f56c6c;
}
</style>
