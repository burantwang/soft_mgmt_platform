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
        router
        background-color="#1f2937"
        text-color="#cbd5e1"
        active-text-color="#ffffff"
        class="layout-menu"
      >
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
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
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { routes } from '@/router/routes'
import { useUserStore } from '@/store/user'
import { getPerms } from '@/utils/auth'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isCollapse = ref(false)

interface MenuItem {
  path: string
  title: string
  icon: string
}

/** 菜单由路由表 meta 动态生成（hidden 过滤 + 权限过滤），新模块自动出现 */
const menuItems = computed<MenuItem[]>(() => {
  const root = routes.find((r) => r.path === '/')
  if (!root?.children) return []
  const perms = getPerms()
  return root.children
    .filter((c) => c.meta && !c.meta.hidden)
    .filter((c) => {
      const perm = c.meta?.perm as string | undefined
      return !perm || perms.includes(perm)
    })
    .map((c) => ({
      path: `/${c.path}`,
      title: (c.meta?.title as string) || '',
      icon: (c.meta?.icon as string) || 'Document'
    }))
})

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => (route.meta?.title as string) || '')
const avatarText = computed(() => (userStore.userInfo?.nickname || '用').slice(0, 1))

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
