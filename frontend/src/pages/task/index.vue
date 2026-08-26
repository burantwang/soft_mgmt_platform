<template>
  <div class="fail-task-page">
    <el-card shadow="never" class="mb-16">
      <template #header>
        <div class="page-title">失败任务追踪</div>
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
              <span class="group-name">【{{ group.branch }} - {{ group.projectName }}】执行明细</span>
              <span class="group-stat">
                用例总数：<b>{{ group.totalCount }}</b>
                成功：<b class="ok">{{ group.passedCount }}</b>
                失败：<b class="bad">{{ group.failedCount }}</b>
                通过率：<b :class="group.passRate >= 100 ? 'ok' : 'warn'">{{ group.passRate }}%</b>
              </span>
            </div>
          </template>

          <div class="group-filter">
            <label>责任人筛选</label>
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
            <el-button size="small" text @click="assigneeFilter[groupKey(group)] = undefined">重置</el-button>
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
                <el-input v-model="editing[row.id].failReason" type="textarea" :rows="2" placeholder="填写失败原因" />
              </template>
            </el-table-column>
            <el-table-column label="分析进展" min-width="160">
              <template #default="{ row }">
                <el-input v-model="editing[row.id].progress" type="textarea" :rows="2" placeholder="填写分析进展" />
              </template>
            </el-table-column>
            <el-table-column label="结论" min-width="160">
              <template #default="{ row }">
                <el-input v-model="editing[row.id].conclusion" type="textarea" :rows="2" placeholder="填写结论" />
              </template>
            </el-table-column>
            <el-table-column label="是否提 Bug" width="100" align="center">
              <template #default="{ row }">
                <el-switch
                  v-model="editing[row.id].isBug"
                  :active-value="1"
                  :inactive-value="0"
                  active-text="是"
                  inactive-text="否"
                  inline-prompt
                />
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
                <el-select v-model="editing[row.id].status" placeholder="状态">
                  <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="90" align="center" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" size="small" :loading="saving[row.id]" @click="saveCase(row)">保存</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-collapse-item>
      </el-collapse>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getEnabledUsersApi, getGroupedFailCasesApi, getReportContentApi, updateFailCaseApi } from '@/api/release'
import type { FailCaseGrouped, GroupedFailCase, UserOption, FailCaseUpdateForm } from '@/types/release'

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

// 每个分组当前选中的原始报告文件ID（仅用于展示选中项）
const reportSel = reactive<Record<string, number | undefined>>({})

function groupKey(group: FailCaseGrouped) {
  return `${group.branch}@@${group.projectName}`
}

function visibleCases(group: FailCaseGrouped): GroupedFailCase[] {
  const f = assigneeFilter[groupKey(group)]
  if (f === undefined) return group.cases
  if (f === 0) return group.cases.filter((c) => c.assigneeId == null)
  return group.cases.filter((c) => c.assigneeId === f)
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
  return {
    status: item.status,
    assigneeId: item.assigneeId,
    failReason: item.failReason || '',
    fixPlan: item.fixPlan || '',
    isBug: item.isBug ?? 0,
    progress: item.progress || '',
    conclusion: item.conclusion || ''
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

onMounted(() => {
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
  align-items: center;
  gap: 24px;
  padding-right: 12px;
  font-size: 14px;
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
</style>
