<template>
  <div class="my-task-page">
    <el-card shadow="never">
      <div class="page-header">
        <div class="header-left">
          <span class="page-title">个人任务</span>
          <span class="text-muted">仅展示指派给您的失败用例</span>
        </div>
        <div class="header-right">
          <el-switch v-model="showHistory" active-text="查看历史" inline-prompt @change="reload" />
          <el-button :icon="Refresh" circle title="刷新" @click="reload" />
        </div>
      </div>

      <el-tabs v-model="activeBoard" @tab-change="reload">
        <el-tab-pane v-for="b in boards" :key="b.key" :label="b.label" :name="b.key">
          <div v-loading="loading">
            <!-- 未完成：待处理 / 处理中 -->
            <FailCaseEditTable
              v-if="!loading && activeList.length > 0"
              :rows="activeList"
              :board="(b.key as 'daily' | 'weekly' | 'dvs')"
              :user-options="userOptions"
              :categories="categories"
              :redmine-prefix="redminePrefix"
              :is-admin="isAdmin"
              show-source
              @updated="reload"
            />

            <!-- 历史已修复问题：可折叠栏位，默认收起 -->
            <template v-if="!loading && showHistory && historyList.length > 0">
              <div
                class="history-divider"
                role="button"
                tabindex="0"
                @click="historyExpanded = !historyExpanded"
                @keydown.enter="historyExpanded = !historyExpanded"
              >
                <span class="history-arrow" :class="{ expanded: historyExpanded }">▸</span>
                <span class="history-label">历史已修复问题（{{ historyList.length }}）</span>
              </div>
              <div v-show="historyExpanded" class="history-body">
                <FailCaseEditTable
                  :rows="historyList"
                  :board="(b.key as 'daily' | 'weekly' | 'dvs')"
                  :user-options="userOptions"
                  :categories="categories"
                  :redmine-prefix="redminePrefix"
                  :is-admin="isAdmin"
                  show-source
                  @updated="reload"
                />
              </div>
            </template>

            <!-- 空状态 -->
            <el-empty
              v-if="!loading && activeList.length === 0 && (!showHistory || historyList.length === 0)"
              :description="emptyDesc"
              :image-size="90"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { getEnabledUsersApi, getFailCaseMetaApi, getMyDailyCasesApi } from '@/api/release'
import { getMyWeeklyCasesApi } from '@/api/weekly'
import { getMyDvsCasesApi } from '@/api/dvs'
import FailCaseEditTable from '@/components/FailCaseEditTable.vue'
import type { MyTaskCase } from '@/types/mytask'
import type { IssueCategory, UserOption } from '@/types/release'

const userStore = useUserStore()

const isAdmin = computed(() => {
  const codes = userStore.userInfo?.roleCodes || []
  return codes.includes('super_admin') || codes.includes('admin')
})

interface Board {
  key: string
  label: string
  fetch: (scope: 'active' | 'all') => Promise<MyTaskCase[]>
}

/**
 * 板块配置：个人任务页与任务追踪共用同一套板块来源。
 * 未来新增追踪板块时，只需在此追加一条，页面会自动出现对应 Tab。
 */
const boards: Board[] = [
  {
    key: 'daily',
    label: 'Daily_Sanity',
    fetch: async (scope) => (await getMyDailyCasesApi(scope)).data
  },
  {
    key: 'weekly',
    label: 'Weekly_Sanity',
    fetch: async (scope) => (await getMyWeeklyCasesApi(scope)).data
  },
  {
    key: 'dvs',
    label: 'DVS',
    fetch: async (scope) => (await getMyDvsCasesApi(scope)).data
  }
]

const activeBoard = ref(boards[0].key)
const showHistory = ref(true)
const historyExpanded = ref(false)
const loading = ref(false)
const list = ref<MyTaskCase[]>([])

/** 未完成（待处理/处理中） */
const activeList = computed(() => list.value.filter((c) => c.status === 1 || c.status === 2))
/** 已完成（已修复/非缺陷/已关闭） */
const historyList = computed(() => list.value.filter((c) => c.status === 3 || c.status === 4 || c.status === 5))
const emptyDesc = computed(() => {
  if (!showHistory.value) return '暂无指派给您的任务'
  if (activeList.value.length === 0 && historyList.value.length === 0) return '暂无历史任务'
  return '暂无未完成指派给您的任务'
})

const userOptions = ref<UserOption[]>([])
const categories = ref<IssueCategory[]>([])
const redminePrefix = ref('')

async function loadData() {
  const board = boards.find((b) => b.key === activeBoard.value) || boards[0]
  loading.value = true
  try {
    list.value = await board.fetch(showHistory.value ? 'all' : 'active')
  } finally {
    loading.value = false
  }
}

function reload() {
  loadData()
}

async function loadUsers() {
  try {
    const res = await getEnabledUsersApi()
    userOptions.value = res.data || []
  } catch {
    /* ignore */
  }
}

async function loadMeta() {
  try {
    const res = await getFailCaseMetaApi()
    categories.value = res.data?.categories || []
    redminePrefix.value = res.data?.redminePrefix || ''
  } catch {
    /* ignore */
  }
}

onMounted(async () => {
  // 刷新后用户信息为空，先拉取以获取角色编码（判断管理员豁免）
  if (!userStore.userInfo) {
    try {
      await userStore.fetchInfo()
    } catch {
      /* token 失效等错误由接口统一处理 */
    }
  }
  loadUsers()
  loadMeta()
  loadData()
})
</script>

<style scoped>
.my-task-page {
  padding: 16px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.header-left {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
}

.text-muted {
  color: #909399;
  font-size: 13px;
}

.history-divider {
  display: flex;
  align-items: center;
  margin: 20px 0 12px;
  padding: 8px 12px;
  cursor: pointer;
  user-select: none;
  border-radius: 6px;
  transition: background 0.2s;
}

.history-divider:hover {
  background: #f5f7fa;
}

.history-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #e4e7ed;
}

.history-arrow {
  display: inline-block;
  margin-right: 8px;
  font-size: 12px;
  color: #909399;
  line-height: 1;
  transition: transform 0.2s;
}

.history-arrow.expanded {
  transform: rotate(90deg);
}

.history-label {
  margin-right: 16px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.history-body {
  margin-bottom: 8px;
}
</style>
