<template>
  <div>
    <!-- 搜索栏 -->
    <el-card shadow="never" class="mb-16">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="名称/编码" clearable style="width: 200px" @keyup.enter="onSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">查询</el-button>
          <el-button @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表 -->
    <el-card shadow="never">
      <div class="table-toolbar">
        <el-button type="primary" @click="openCreate">新增机型</el-button>
        <span class="text-muted" style="margin-left: 8px">维护发布记录可选机型（如 Gaea / Ares / Gemini 等）</span>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="projectName" label="机型名称" min-width="120" />
        <el-table-column prop="projectCode" label="机型编码" min-width="110">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.projectCode }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.description">{{ row.description }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row as ReleaseProject)">编辑</el-button>
            <el-button link :type="row.status === 1 ? 'warning' : 'success'" @click="onToggleStatus(row as ReleaseProject)">
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button link type="danger" @click="onDelete(row as ReleaseProject)">删除</el-button>
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新增机型' : '编辑机型'" width="480px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="机型名称" prop="projectName">
          <el-input v-model="form.projectName" placeholder="如 Gaea" />
        </el-form-item>
        <el-form-item label="机型编码" prop="projectCode">
          <el-input v-model="form.projectCode" placeholder="如 GAEA" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="机型说明（可选）" />
        </el-form-item>
        <el-form-item v-if="dialogMode === 'edit'" label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { createProjectApi, deleteProjectApi, getProjectPageApi, updateProjectApi } from '@/api/release'
import type { ReleaseProject } from '@/types/release'

const loading = ref(false)
const submitting = ref(false)
const list = ref<ReleaseProject[]>([])
const total = ref(0)
const query = reactive({ keyword: '', status: undefined as number | undefined, page: 1, size: 10 })

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const formRef = ref<FormInstance>()
const form = reactive({
  id: undefined as number | undefined,
  projectName: '',
  projectCode: '',
  description: '',
  status: 1
})
const rules = {
  projectName: [{ required: true, message: '请输入机型名称', trigger: 'blur' }],
  projectCode: [{ required: true, message: '请输入机型编码', trigger: 'blur' }]
}

async function loadData() {
  loading.value = true
  try {
    const res = await getProjectPageApi({ ...query })
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function onSearch() {
  query.page = 1
  loadData()
}

function onReset() {
  query.keyword = ''
  query.status = undefined
  onSearch()
}

function openCreate() {
  dialogMode.value = 'create'
  Object.assign(form, { id: undefined, projectName: '', projectCode: '', description: '', status: 1 })
  dialogVisible.value = true
}

function openEdit(row: ReleaseProject) {
  dialogMode.value = 'edit'
  Object.assign(form, {
    id: row.id,
    projectName: row.projectName,
    projectCode: row.projectCode,
    description: row.description || '',
    status: row.status ?? 1
  })
  dialogVisible.value = true
}

async function submit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (dialogMode.value === 'create') {
      await createProjectApi({ ...form })
      ElMessage.success('创建成功')
    } else {
      const { id, ...rest } = form
      await updateProjectApi({ id, ...rest })
      ElMessage.success('保存成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

async function onToggleStatus(row: ReleaseProject) {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '启用' : '停用'
  await ElMessageBox.confirm(`确认${action}机型「${row.projectName}」？`, `${action}确认`, { type: 'warning' })
  await updateProjectApi({ id: row.id, projectName: row.projectName, projectCode: row.projectCode, description: row.description, status: newStatus })
  ElMessage.success(`${action}成功`)
  loadData()
}

async function onDelete(row: ReleaseProject) {
  await ElMessageBox.confirm(`确认删除机型「${row.projectName}」？已被发布记录引用时将无法删除。`, '删除确认', { type: 'warning' })
  await deleteProjectApi(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.mb-16 {
  margin-bottom: 16px;
}

.table-toolbar {
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
</style>
