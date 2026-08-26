<template>
  <div>
    <el-card shadow="never">
      <div class="page-header">
        <span class="text-muted">
          当前用户被指派且未完成的失败任务（待处理 / 处理中），共 {{ total }} 条
        </span>
        <el-button :icon="Refresh" circle title="刷新" @click="loadData" />
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="taskNo" label="任务编号" width="150" fixed="left">
          <template #default="{ row }">
            <span class="task-no">{{ row.taskNo }}</span>
          </template>
        </el-table-column>
        <el-table-column label="关联发布" min-width="180">
          <template #default="{ row }">
            <template v-if="row.branch">
              <span>{{ row.branch }}</span>
              <el-tag v-if="row.version" size="small" type="info" class="ml-4">{{ row.version }}</el-tag>
              <template v-if="row.projectNames?.length">
                <div class="mt-4">
                  <el-tag v-for="p in row.projectNames" :key="p" size="small" class="mr-4">{{ p }}</el-tag>
                </div>
              </template>
            </template>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="summary" label="失败概述" min-width="220" show-overflow-tooltip />
        <el-table-column label="用例统计" width="150">
          <template #default="{ row }">
            <template v-if="row.caseTotal > 0">
              {{ row.caseTotal }} 条 ·
              <span class="warn-text">{{ row.casePending }}待处理</span> ·
              <span class="primary-text">{{ row.caseProcessing }}处理中</span> ·
              <span class="ok-text">{{ row.caseDone }}完成</span>
            </template>
            <span v-else class="text-muted">无明细</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ row.statusDesc }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="creatorName" label="创建人" width="100">
          <template #default="{ row }">{{ row.creatorName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row.id)">查看</el-button>
            <el-button v-if="row.status === 1" link type="primary" @click="changeStatus(row as FailTaskVO, 2)">开始处理</el-button>
            <el-button v-if="row.status === 2" link type="success" @click="changeStatus(row as FailTaskVO, 3)">完成</el-button>
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
          @change="loadData"
        />
      </div>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="失败任务详情" width="860px" destroy-on-close>
      <template v-if="detail">
        <el-descriptions :column="3" border class="mb-16">
          <el-descriptions-item label="任务编号">{{ detail.taskNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTagType(detail.status)" size="small">{{ detail.statusDesc }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="责任人">{{ detail.assigneeName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="关联发布">
            {{ detail.branch ? detail.branch + (detail.version ? '@' + detail.version : '') : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="创建人">{{ detail.creatorName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ detail.createTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="失败概述" :span="3">{{ detail.summary }}</el-descriptions-item>
          <el-descriptions-item label="失败原因" :span="3">{{ detail.failReason || '-' }}</el-descriptions-item>
          <el-descriptions-item label="修改方案" :span="3">{{ detail.fixPlan || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">失败用例明细（{{ detail.cases.length }}）</el-divider>
        <el-table :data="detail.cases" border size="small" v-loading="caseLoading">
          <el-table-column label="类型" width="76">
            <template #default="{ row }">
              <el-tag :type="caseTypeTag(row.caseType)" size="small">
                {{ row.caseTypeDesc || (row.caseType === 'error' ? '错误' : '失败') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="caseName" label="用例名称" min-width="200" show-overflow-tooltip />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="caseTagType(row.status)" size="small">{{ row.statusDesc }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="failReason" label="失败原因" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">{{ row.failReason || '-' }}</template>
          </el-table-column>
          <el-table-column prop="fixPlan" label="修改方案" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">{{ row.fixPlan || '-' }}</template>
          </el-table-column>
          <el-table-column prop="handleTime" label="处理时间" width="170">
            <template #default="{ row }">{{ row.handleTime || '-' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="80" fixed="right">
            <template #default="{ row }">
              <el-button v-if="detail.status < 3" link type="primary" @click="openHandleCase(row as FailCaseVO)">处理</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </el-dialog>

    <!-- 处理用例弹窗 -->
    <el-dialog v-model="handleVisible" title="处理失败用例" width="620px" destroy-on-close>
      <el-form :model="handleForm" label-width="90px">
        <el-form-item label="用例名称">
          <span>{{ handleForm.caseName }}</span>
        </el-form-item>
        <el-form-item label="处理状态" required>
          <el-radio-group v-model="handleForm.status">
            <el-radio :value="2">处理中</el-radio>
            <el-radio :value="3">已修复</el-radio>
            <el-radio :value="4">非缺陷</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="失败原因">
          <el-input v-model="handleForm.failReason" type="textarea" :rows="2" maxlength="1000" show-word-limit />
        </el-form-item>
        <el-form-item label="修改方案">
          <el-input v-model="handleForm.fixPlan" type="textarea" :rows="2" maxlength="1000" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onHandleCase">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import {
  changeFailTaskStatusApi,
  getFailTaskDetailApi,
  getMyFailTaskPageApi,
  handleFailCaseApi
} from '@/api/release'
import type { FailCaseHandleForm, FailCaseVO, FailTaskDetailVO, FailTaskVO } from '@/types/release'

type TagType = 'info' | 'warning' | 'success' | 'primary' | 'danger'

const loading = ref(false)
const saving = ref(false)
const caseLoading = ref(false)
const list = ref<FailTaskVO[]>([])
const total = ref(0)

const query = reactive({ page: 1, size: 10 })

async function loadData() {
  loading.value = true
  try {
    const res = await getMyFailTaskPageApi({ page: query.page, size: query.size })
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

/* ---------- 详情 ---------- */
const detailVisible = ref(false)
const detail = ref<FailTaskDetailVO>()

async function openDetail(id: number) {
  detailVisible.value = true
  caseLoading.value = true
  try {
    const res = await getFailTaskDetailApi(id)
    detail.value = res.data
  } finally {
    caseLoading.value = false
  }
}

/* ---------- 状态流转 ---------- */
async function changeStatus(row: FailTaskVO, status: number) {
  await changeFailTaskStatusApi(row.id, status)
  ElMessage.success(status === 2 ? '已开始处理' : '任务已完成')
  loadData()
}

/* ---------- 处理用例 ---------- */
const handleVisible = ref(false)
const handleForm = reactive<FailCaseHandleForm & { caseName: string }>({
  caseId: 0,
  caseName: '',
  status: 3,
  failReason: '',
  fixPlan: ''
})

function openHandleCase(row: FailCaseVO) {
  handleForm.caseId = row.id
  handleForm.caseName = row.caseName
  handleForm.status = row.status === 1 ? 2 : row.status
  handleForm.failReason = row.failReason || ''
  handleForm.fixPlan = row.fixPlan || ''
  handleVisible.value = true
}

async function onHandleCase() {
  if (handleForm.status >= 3 && !(handleForm.failReason || '').trim() && !(handleForm.fixPlan || '').trim()) {
    ElMessage.warning('请填写失败原因或修改方案')
    return
  }
  saving.value = true
  try {
    await handleFailCaseApi({
      caseId: handleForm.caseId,
      status: handleForm.status,
      failReason: handleForm.failReason || undefined,
      fixPlan: handleForm.fixPlan || undefined
    })
    ElMessage.success('处理成功')
    handleVisible.value = false
    if (detailVisible.value && detail.value) {
      await openDetail(detail.value.id)
    }
    loadData()
  } finally {
    saving.value = false
  }
}

/* ---------- 辅助 ---------- */
function statusTagType(status: number): TagType {
  return ({ 1: 'warning', 2: 'primary', 3: 'success', 4: 'info' } as Record<number, TagType>)[status] || 'info'
}

function caseTagType(status: number): TagType {
  return ({ 1: 'warning', 2: 'primary', 3: 'success', 4: 'info' } as Record<number, TagType>)[status] || 'info'
}

function caseTypeTag(type?: string): TagType {
  return type === 'error' ? 'danger' : 'warning'
}

onMounted(loadData)
</script>

<style scoped>
.mt-4 {
  margin-top: 4px;
}

.ml-4 {
  margin-left: 4px;
}

.mr-4 {
  margin-right: 4px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.table-pagination {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}

.text-muted {
  color: #909399;
}

.task-no {
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-weight: 500;
}

.ok-text {
  color: #67c23a;
}

.warn-text {
  color: #e6a23c;
}

.primary-text {
  color: #409eff;
}
</style>
