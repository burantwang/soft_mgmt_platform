import { createRouter, createWebHistory } from 'vue-router'
import { routes } from './routes'
import { getToken, getPerms } from '@/utils/auth'

const router = createRouter({
  history: createWebHistory(),
  routes
})

/**
 * 路由守卫：
 * 1. 未登录访问业务页 → 跳转登录
 * 2. 已登录访问登录页 → 跳转首页
 * 3. 路由级权限：meta.perm 不在用户权限码中 → 403
 */
router.beforeEach((to) => {
  const token = getToken()

  if (to.path === '/login') {
    return token ? '/' : true
  }

  if (!token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  const perm = to.meta?.perm as string | undefined
  if (perm && !getPerms().includes(perm)) {
    return '/403'
  }

  return true
})

export default router
