<template>
  <el-container class="layout">
    <el-aside :width="isCollapse ? '64px' : '220px'" class="layout-aside">
      <div class="logo">
        <el-icon :size="22" color="#fff"><Platform /></el-icon>
        <span v-show="!isCollapse" class="logo-text">研发业务平台</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :collapse-transition="false"
        background-color="#1f2937"
        text-color="#cbd5e1"
        active-text-color="#ffffff"
        class="layout-menu"
        @select="handleMenuSelect"
      >
        <template v-for="entry in menuTree" :key="entry.path">
          <el-sub-menu v-if="entry.type === 'group'" :index="entry.path">
            <template #title>
              <el-icon><component :is="entry.icon" /></el-icon>
              <span>{{ entry.title }}</span>
            </template>
            <el-menu-item
              v-for="item in entry.children"
              :key="item.path"
              :index="item.drawer ? 'drawer:' + item.key : item.path"
            >
              <el-icon><component :is="item.icon" /></el-icon>
              <template #title>{{ item.title }}</template>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="entry.path">
            <el-icon><component :is="entry.icon" /></el-icon>
            <template #title>{{ entry.title }}</template>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="layout-header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="isCollapse = !isCollapse">
            <Expand v-if="isCollapse" />
            <Fold v-else />
          </el-icon>
          <span class="header-title">{{ currentTitle }}</span>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="30" class="user-avatar">{{ avatarText }}</el-avatar>
              <span class="user-name">{{ userStore.userInfo?.nickname || '未登录' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="myTask">我的待办</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>

  <!-- Daily_Sanity：右侧抽屉 -->
  <el-drawer v-model="dailySanityVisible" title="Daily_Sanity" size="85%" destroy-on-close>
    <TaskPage v-if="dailySanityVisible" />
  </el-drawer>

  <!-- Weekly_Sanity：右侧抽屉 -->
  <el-drawer v-model="weeklySanityVisible" title="Weekly_Sanity" size="85%" destroy-on-close>
    <WeeklyPage v-if="weeklySanityVisible" />
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { routes } from '@/router/routes'
import { useUserStore } from '@/store/user'
import { getPerms } from '@/utils/auth'
import TaskPage from '@/pages/task/index.vue'
import WeeklyPage from '@/pages/task/weekly.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isCollapse = ref(false)
const dailySanityVisible = ref(false)
const weeklySanityVisible = ref(false)

interface SubMenuItem {
  path: string
  key: string
  title: string
  icon: string
  drawer?: boolean
}

interface MenuEntry {
  type: 'group' | 'item'
  path: string
  title: string
  icon: string
  children?: SubMenuItem[]
}

/** 是否通过权限过滤（无 perm 视为公开） */
const permOk = (c: { meta?: { perm?: string } }, perms: string[]) => {
  const perm = c.meta?.perm
  return !perm || perms.includes(perm)
}

/** 菜单树：严格按路由表顺序生成，含非 hidden 子路由的生成一级分组，其余为平级菜单项 */
const menuTree = computed<MenuEntry[]>(() => {
  const root = routes.find((r) => r.path === '/')
  if (!root?.children) return []
  const perms = getPerms()
  return root.children
    .filter((c) => c.meta && !c.meta.hidden && permOk(c, perms))
    .map((c) => {
      const kids = (c.children ?? [])
        .filter((cc) => cc.meta && !cc.meta.hidden && permOk(cc, perms))
        .map<SubMenuItem>((cc) => ({
          path: `/${c.path}/${cc.path}`.replace(/\/+$/, ''),
          key: cc.path || 'index',
          title: (cc.meta?.title as string) || '',
          icon: (cc.meta?.icon as string) || 'Document',
          drawer: !!cc.meta?.drawer
        }))
      if (kids.length > 0) {
        return {
          type: 'group' as const,
          path: `/${c.path}`,
          title: (c.meta?.title as string) || '',
          icon: (c.meta?.icon as string) || 'Document',
          children: kids
        }
      }
      return {
        type: 'item' as const,
        path: `/${c.path}`,
        title: (c.meta?.title as string) || '',
        icon: (c.meta?.icon as string) || 'Document'
      }
    })
})

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => (route.meta?.title as string) || '')
const avatarText = computed(() => (userStore.userInfo?.nickname || '用').slice(0, 1))

/** 菜单点击：抽屉项打开右侧抽屉，其余路由跳转 */
const handleMenuSelect = (index: string) => {
  if (index.startsWith('drawer:')) {
    const key = index.replace('drawer:', '')
    if (key === 'weekly-sanity') {
      weeklySanityVisible.value = true
    } else {
      dailySanityVisible.value = true
    }
    return
  }
  router.push(index)
}

const handleCommand = async (command: string) => {
  if (command === 'logout') {
    await ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' })
    await userStore.logout()
    router.push('/login')
  } else if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'myTask') {
    router.push('/task/mine')
  }
}
</script>

<style scoped lang="scss">
.layout {
  height: 100%;
}

.layout-aside {
  background: #1f2937;
  transition: width 0.2s;
  overflow: hidden;
}

.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 56px;
  background: #111827;
}

.logo-text {
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  white-space: nowrap;
}

.layout-menu {
  border-right: none;
}

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid var(--border-color);
  padding: 0 16px;
  height: 56px;
}

.header-left {
  display: flex;
  align-items: center;
}

.collapse-btn {
  font-size: 18px;
  cursor: pointer;
  color: var(--text-sub);
}

.header-title {
  font-size: 15px;
  font-weight: 600;
  margin-left: 12px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}

.user-avatar {
  background: var(--brand-color);
}

.user-name {
  font-size: 14px;
  color: var(--text-main);
}

.layout-main {
  background: var(--bg-page);
  padding: 0;
  overflow-y: auto;
}
</style>
