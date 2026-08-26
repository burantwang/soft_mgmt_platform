<template>
  <div>
    <!-- 搜索栏 -->
    <el-card shadow="never" class="mb-16">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="账号/姓名" clearable style="width: 200px" @keyup.enter="onSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="正常" :value="1" />
            <el-option label="禁用" :value="0" />
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
        <el-button type="primary" @click="openCreate">新增用户</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="username" label="账号" min-width="120" />
        <el-table-column prop="nickname" label="姓名" min-width="100" />
        <el-table-column label="角色" min-width="150">
          <template #default="{ row }">
            <el-tag v-for="r in row.roleNames" :key="r" size="small" class="mr-4">{{ r }}</el-tag>
            <span v-if="!row.roleNames?.length" class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="email" label="邮箱" min-width="170" show-overflow-tooltip />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row as UserInfo)">编辑</el-button>
            <el-button link type="warning" @click="onResetPwd(row as UserInfo)">重置密码</el-button>
            <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="onToggleStatus(row as UserInfo)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button link type="danger" @click="onDelete(row as UserInfo)">删除</el-button>
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
    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新增用户' : '编辑用户'" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="登录账号" prop="username">
          <el-input v-model="form.username" :disabled="dialogMode === 'edit'" placeholder="登录账号" />
        </el-form-item>
        <el-form-item v-if="dialogMode === 'create'" label="初始密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="留空使用默认密码 123456" />
        </el-form-item>
        <el-form-item label="姓名" prop="nickname">
          <el-input v-model="form.nickname" placeholder="姓名" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleIds" multiple placeholder="选择角色" style="width: 100%">
            <el-option v-for="r in roles" :key="r.id" :label="r.roleName" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="手机号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="邮箱" />
        </el-form-item>
        <el-form-item v-if="dialogMode === 'edit'" label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="正常" inactive-text="禁用" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注" />
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
import { createUserApi, deleteUserApi, getAllRolesApi, getUserListApi, resetPasswordApi, updateUserApi, updateUserStatusApi } from '@/api/system'
import type { RoleItem, UserInfo } from '@/types/api'

const loading = ref(false)
const submitting = ref(false)
const list = ref<UserInfo[]>([])
const total = ref(0)
const query = reactive({ keyword: '', status: undefined as number | undefined, page: 1, size: 10 })
const roles = ref<RoleItem[]>([])

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const formRef = ref<FormInstance>()
const form = reactive({
  id: undefined as number | undefined,
  username: '',
  password: '',
  nickname: '',
  email: '',
  phone: '',
  remark: '',
  status: 1,
  roleIds: [] as number[]
})
const rules = {
  username: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
}

async function loadRoles() {
  roles.value = (await getAllRolesApi()).data
}

async function loadData() {
  loading.value = true
  try {
    const res = await getUserListApi({ ...query })
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
  Object.assign(form, {
    id: undefined, username: '', password: '', nickname: '', email: '',
    phone: '', remark: '', status: 1, roleIds: []
  })
  dialogVisible.value = true
}

function openEdit(row: UserInfo) {
  dialogMode.value = 'edit'
  Object.assign(form, {
    id: row.id,
    username: row.username,
    password: '',
    nickname: row.nickname,
    email: row.email || '',
    phone: row.phone || '',
    remark: row.remark || '',
    status: row.status ?? 1,
    roleIds: [...(row.roleIds || [])]
  })
  dialogVisible.value = true
}

async function submit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (dialogMode.value === 'create') {
      await createUserApi({ ...form })
      ElMessage.success('创建成功')
    } else {
      const { id, username, password, ...rest } = form
      void username
      void password
      await updateUserApi({ id, ...rest })
      ElMessage.success('保存成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

async function onDelete(row: UserInfo) {
  await ElMessageBox.confirm(`确认删除用户「${row.nickname}(${row.username})」？删除后不可恢复。`, '删除确认', { type: 'warning' })
  await deleteUserApi(row.id)
  ElMessage.success('删除成功')
  loadData()
}

async function onResetPwd(row: UserInfo) {
  const { value } = await ElMessageBox.prompt(
    `请输入用户「${row.username}」的新密码（留空使用默认密码 123456）：`,
    '重置密码',
    { inputType: 'password', inputPlaceholder: '默认 123456', confirmButtonText: '确定重置' }
  )
  await resetPasswordApi(row.id, value || undefined)
  ElMessage.success('密码已重置，该用户下次登录需修改密码')
}

async function onToggleStatus(row: UserInfo) {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '启用' : '禁用'
  await ElMessageBox.confirm(`确认${action}用户「${row.nickname}(${row.username})」？`, `${action}确认`, { type: 'warning' })
  await updateUserStatusApi(row.id, newStatus)
  ElMessage.success(`${action}成功`)
  loadData()
}

onMounted(() => {
  loadData()
  loadRoles()
})
</script>

<style scoped>
.mb-16 {
  margin-bottom: 16px;
}

.mr-4 {
  margin-right: 4px;
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
