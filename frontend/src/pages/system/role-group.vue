<template>
  <div>
    <el-card shadow="never">
      <div class="table-toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="组名关键字"
          clearable
          style="width: 220px; margin-right: 8px"
          @keyup.enter="onSearch"
        />
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button @click="onReset">重置</el-button>
        <el-button type="primary" style="float: right" @click="openCreate">新增组</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="groupName" label="组名" min-width="160" />
        <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.remark" class="text-muted">{{ row.remark }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="memberCount" label="成员数" width="90" />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row as GroupItem)">编辑</el-button>
            <el-button link type="success" @click="openMembers(row as GroupItem)">成员管理</el-button>
            <el-button link type="danger" @click="onDelete(row as GroupItem)">删除</el-button>
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
    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新增组' : '编辑组'" width="480px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="组名" prop="groupName">
          <el-input v-model="form.groupName" placeholder="如 软件研发一处" />
        </el-form-item>
        <el-form-item v-if="dialogMode === 'edit'" label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="正常" inactive-text="停用" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="组用途说明(选填)" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 成员管理抽屉 -->
    <el-drawer v-model="memberVisible" :title="`成员管理 - ${currentGroupName}`" size="640px">
      <div class="member-toolbar">
        <el-input
          v-model="memberQuery.keyword"
          placeholder="账号/姓名"
          clearable
          style="width: 200px; margin-right: 8px"
          @keyup.enter="onSearchMembers"
        />
        <el-button type="primary" @click="onSearchMembers">查询</el-button>
        <el-button type="primary" style="float: right" @click="openAddMember">添加成员</el-button>
      </div>

      <el-table v-loading="memberLoading" :data="members" border stripe>
        <el-table-column prop="username" label="账号" min-width="120" />
        <el-table-column prop="nickname" label="姓名" min-width="100" />
        <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.email" class="text-muted">{{ row.email }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" @click="removeMember(row as MemberItem)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-pagination">
        <el-pagination
          v-model:current-page="memberQuery.page"
          v-model:page-size="memberQuery.size"
          :total="memberTotal"
          layout="total, sizes, prev, pager, next"
          :page-sizes="[10, 20, 50]"
          @change="loadMembers"
        />
      </div>
    </el-drawer>

    <!-- 添加成员弹窗 -->
    <el-dialog v-model="addVisible" :title="`添加成员 - ${currentGroupName}`" width="520px" destroy-on-close>
      <el-select
        v-model="pendingUserIds"
        multiple
        filterable
        remote
        :remote-method="searchUsers"
        :loading="userLoading"
        placeholder="输入账号/姓名搜索后勾选"
        style="width: 100%"
      >
        <el-option
          v-for="u in userOptions"
          :key="u.id"
          :label="`${u.nickname}(${u.username})`"
          :value="u.id"
          :disabled="memberIds.includes(u.id)"
        />
      </el-select>
      <div class="member-tip">已在组内的用户显示为禁用状态，可直接搜索添加多人。</div>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingMembers" @click="confirmAdd">确认添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import {
  createGroupApi,
  deleteGroupApi,
  getGroupListApi,
  getGroupMemberIdsApi,
  getGroupMemberPageApi,
  getUserListApi,
  saveGroupMembersApi,
  updateGroupApi
} from '@/api/system'
import type { GroupItem, MemberItem, UserInfo } from '@/types/api'

const loading = ref(false)
const submitting = ref(false)
const list = ref<GroupItem[]>([])
const total = ref(0)
const query = reactive({ keyword: '', page: 1, size: 10 })

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const formRef = ref<FormInstance>()
const form = reactive({
  id: undefined as number | undefined,
  groupName: '',
  remark: '',
  status: 1
})
const rules = {
  groupName: [{ required: true, message: '请输入组名', trigger: 'blur' }]
}

// 成员管理
const memberVisible = ref(false)
const memberLoading = ref(false)
const currentGroupId = ref<number | null>(null)
const currentGroupName = ref('')
const members = ref<MemberItem[]>([])
const memberTotal = ref(0)
const memberQuery = reactive({ keyword: '', page: 1, size: 10 })
const memberIds = ref<number[]>([])

// 添加成员
const addVisible = ref(false)
const userLoading = ref(false)
const userOptions = ref<UserInfo[]>([])
const pendingUserIds = ref<number[]>([])
const savingMembers = ref(false)

async function loadData() {
  loading.value = true
  try {
    const res = await getGroupListApi({ ...query })
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
  onSearch()
}

function openCreate() {
  dialogMode.value = 'create'
  Object.assign(form, { id: undefined, groupName: '', remark: '', status: 1 })
  dialogVisible.value = true
}

function openEdit(row: GroupItem) {
  dialogMode.value = 'edit'
  Object.assign(form, { id: row.id, groupName: row.groupName, remark: row.remark || '', status: row.status ?? 1 })
  dialogVisible.value = true
}

async function submit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (dialogMode.value === 'create') {
      await createGroupApi({ ...form })
      ElMessage.success('创建成功')
    } else {
      await updateGroupApi({ ...form })
      ElMessage.success('保存成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

async function onDelete(row: GroupItem) {
  await ElMessageBox.confirm(
    `确认删除组「${row.groupName}」？删除即解散该组，将解除组内全部成员的归属，不影响用户本身及其角色。`,
    '删除确认',
    { type: 'warning' }
  )
  await deleteGroupApi(row.id)
  ElMessage.success('删除成功')
  loadData()
}

async function openMembers(row: GroupItem) {
  currentGroupId.value = row.id
  currentGroupName.value = row.groupName
  memberQuery.keyword = ''
  memberQuery.page = 1
  memberVisible.value = true
  await loadMemberIds()
  loadMembers()
}

async function loadMemberIds() {
  if (currentGroupId.value == null) return
  memberIds.value = (await getGroupMemberIdsApi(currentGroupId.value)).data || []
}

async function loadMembers() {
  if (currentGroupId.value == null) return
  memberLoading.value = true
  try {
    const res = await getGroupMemberPageApi(currentGroupId.value, { ...memberQuery })
    members.value = res.data.records
    memberTotal.value = res.data.total
  } finally {
    memberLoading.value = false
  }
}

function onSearchMembers() {
  memberQuery.page = 1
  loadMembers()
}

async function removeMember(row: MemberItem) {
  if (currentGroupId.value == null) return
  await ElMessageBox.confirm(`确认将「${row.nickname}(${row.username})」移出该组？`, '移除成员', { type: 'warning' })
  const nextIds = memberIds.value.filter((id) => id !== row.id)
  await saveGroupMembersApi(currentGroupId.value, nextIds)
  ElMessage.success('已移除')
  await loadMemberIds()
  loadMembers()
  loadData()
}

async function openAddMember() {
  pendingUserIds.value = []
  addVisible.value = true
  userOptions.value = []
  searchUsers('')
}

async function searchUsers(keyword: string) {
  userLoading.value = true
  try {
    const res = await getUserListApi({ keyword: keyword || '', status: 1, page: 1, size: 100 })
    userOptions.value = res.data.records
  } finally {
    userLoading.value = false
  }
}

async function confirmAdd() {
  if (currentGroupId.value == null) return
  if (!pendingUserIds.value.length) {
    ElMessage.warning('请先选择要添加的用户')
    return
  }
  savingMembers.value = true
  try {
    const nextIds = Array.from(new Set([...memberIds.value, ...pendingUserIds.value]))
    await saveGroupMembersApi(currentGroupId.value, nextIds)
    ElMessage.success('成员已添加')
    addVisible.value = false
    await loadMemberIds()
    loadMembers()
    loadData()
  } finally {
    savingMembers.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.table-toolbar {
  margin-bottom: 14px;
}

.member-toolbar {
  margin-bottom: 14px;
}

.table-pagination {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}

.member-tip {
  margin-top: 10px;
  font-size: 12px;
  color: #909399;
}

.text-muted {
  color: #909399;
}
</style>
