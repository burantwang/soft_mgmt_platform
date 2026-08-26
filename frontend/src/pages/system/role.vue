<template>
  <div>
    <el-card shadow="never">
      <div class="table-toolbar">
        <el-button type="primary" @click="openCreate">新增角色</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="roleCode" label="角色编码" min-width="140" />
        <el-table-column prop="roleName" label="角色名称" min-width="140" />
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="userCount" label="用户数" width="90" />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row as RoleItem)">编辑</el-button>
            <el-button link type="warning" @click="openPermission(row as RoleItem)">分配权限</el-button>
            <el-button link type="danger" :disabled="row.roleCode === 'super_admin'" @click="onDelete(row as RoleItem)">删除</el-button>
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
    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新增角色' : '编辑角色'" width="480px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" :disabled="isSuperRole" placeholder="如 employee" />
        </el-form-item>
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="角色名称" />
        </el-form-item>
        <el-form-item v-if="dialogMode === 'edit'" label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="正常" inactive-text="停用" />
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

    <!-- 分配权限弹窗 -->
    <el-dialog v-model="permVisible" :title="`分配权限 - ${currentRole?.roleName || ''}`" width="520px" destroy-on-close>
      <el-tree
        ref="treeRef"
        :data="permTree"
        show-checkbox
        node-key="id"
        default-expand-all
        :props="{ label: 'label', children: 'children' }"
      />
      <div class="perm-tip">勾选权限后点击保存，角色下的用户将立即生效。</div>
      <template #footer>
        <el-button @click="permVisible = false">取消</el-button>
        <el-button type="primary" :loading="permSubmitting" @click="submitPermissions">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { assignPermissionsApi, createRoleApi, deleteRoleApi, getPermissionListApi, getRolePermissionIdsApi, getRoleListApi, updateRoleApi } from '@/api/system'
import type { PermissionItem, RoleItem } from '@/types/api'

interface TreeNode {
  id: number | string
  label: string
  disabled?: boolean
  children?: TreeNode[]
}

const loading = ref(false)
const submitting = ref(false)
const list = ref<RoleItem[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10 })

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const formRef = ref<FormInstance>()
const isSuperRole = ref(false)
const form = reactive({
  id: undefined as number | undefined,
  roleCode: '',
  roleName: '',
  remark: '',
  status: 1
})
const rules = {
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
}

// 分配权限
const permVisible = ref(false)
const permSubmitting = ref(false)
const currentRole = ref<RoleItem | null>(null)
const treeRef = ref()
const permTree = ref<TreeNode[]>([])

const isAdminPerm = computed(() => currentRole.value?.roleCode === 'super_admin')

async function loadData() {
  loading.value = true
  try {
    const res = await getRoleListApi({ ...query })
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function openCreate() {
  dialogMode.value = 'create'
  isSuperRole.value = false
  Object.assign(form, { id: undefined, roleCode: '', roleName: '', remark: '', status: 1 })
  dialogVisible.value = true
}

function openEdit(row: RoleItem) {
  dialogMode.value = 'edit'
  isSuperRole.value = row.roleCode === 'super_admin'
  Object.assign(form, { id: row.id, roleCode: row.roleCode, roleName: row.roleName, remark: row.remark || '', status: row.status ?? 1 })
  dialogVisible.value = true
}

async function submit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (dialogMode.value === 'create') {
      await createRoleApi({ ...form })
      ElMessage.success('创建成功')
    } else {
      await updateRoleApi({ ...form })
      ElMessage.success('保存成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

async function onDelete(row: RoleItem) {
  await ElMessageBox.confirm(`确认删除角色「${row.roleName}(${row.roleCode})」？`, '删除确认', { type: 'warning' })
  await deleteRoleApi(row.id)
  ElMessage.success('删除成功')
  loadData()
}

async function openPermission(row: RoleItem) {
  currentRole.value = row
  permVisible.value = true
  const perms = (await getPermissionListApi()).data
  // 按模块分组构建树
  const group = new Map<string, PermissionItem[]>()
  perms.forEach((p) => {
    if (!group.has(p.module)) group.set(p.module, [])
    group.get(p.module)!.push(p)
  })
  permTree.value = [...group.entries()].map(([module, items]) => ({
    id: `m_${module}`,
    label: items[0]?.moduleName || module,
    disabled: isAdminPerm.value,
    children: items.map((i) => ({ id: i.id, label: `${i.permName}(${i.permCode})` }))
  }))
  if (isAdminPerm.value) {
    treeRef.value?.setCheckedKeys([])
  } else {
    const checked = (await getRolePermissionIdsApi(row.id)).data
    treeRef.value?.setCheckedKeys(checked)
  }
}

async function submitPermissions() {
  if (!currentRole.value) return
  permSubmitting.value = true
  try {
    const checked = treeRef.value?.getCheckedKeys() || []
    const halfChecked = treeRef.value?.getHalfCheckedKeys() || []
    // 过滤模块级节点(id 为字符串)，只提交权限点 id
    const ids = [...checked, ...halfChecked].filter((k: number | string) => typeof k === 'number') as number[]
    await assignPermissionsApi(currentRole.value.id, ids)
    ElMessage.success('权限分配成功')
    permVisible.value = false
    loadData()
  } finally {
    permSubmitting.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.table-toolbar {
  margin-bottom: 14px;
}

.table-pagination {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}

.perm-tip {
  margin-top: 10px;
  font-size: 12px;
  color: #909399;
}
</style>
