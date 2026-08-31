import type { RouteRecordRaw } from 'vue-router'

/**
 * 路由配置（菜单由本表 meta 动态生成）
 * meta: { title 标题, icon 图标名, perm 权限码, hidden 是否隐藏, drawer 是否以右侧抽屉打开 }
 * 扩展新模块：新增一条路由并配置 perm，主布局自动生成菜单
 */
export const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/pages/login/index.vue'),
    meta: { title: '登录', hidden: true }
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/pages/error/403.vue'),
    meta: { title: '无权限', hidden: true }
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/pages/error/404.vue'),
    meta: { title: '页面不存在', hidden: true }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/pages/dashboard/index.vue'),
        meta: { title: '发布统计看板', icon: 'Odometer' }
      },
      {
        path: 'release',
        name: 'Release',
        component: () => import('@/pages/release/index.vue'),
        meta: { title: '版本发布管理', icon: 'Promotion', perm: 'sonic:view' }
      },
      {
        path: 'task',
        name: 'TaskGroup',
        meta: { title: '任务追踪', icon: 'Warning', perm: 'sonic:view' },
        children: [
          {
            // 独立页面入口（保留 /task，供首页卡片等跳转；不在菜单显示）
            path: '',
            name: 'FailTask',
            component: () => import('@/pages/task/index.vue'),
            meta: { title: 'Daily_Sanity', hidden: true }
          },
          {
            // 二级菜单项：以右侧抽屉方式打开
            path: 'daily-sanity',
            name: 'DailySanity',
            component: () => import('@/pages/task/index.vue'),
            meta: { title: 'Daily_Sanity', drawer: true }
          },
          {
            path: 'weekly-sanity',
            name: 'WeeklySanity',
            component: () => import('@/pages/task/weekly.vue'),
            meta: { title: 'Weekly_Sanity', drawer: true }
          },
          {
            path: 'mine',
            name: 'MyTask',
            component: () => import('@/pages/task/mine.vue'),
            meta: { title: '个人任务', icon: 'List' }
          }
        ]
      },
      {
        path: 'wiki',
        name: 'Wiki',
        component: () => import('@/pages/wiki/index.vue'),
        meta: { title: 'Wiki 知识库', icon: 'Reading', perm: 'wiki:view' }
      },
      {
        path: 'files',
        name: 'Files',
        component: () => import('@/pages/files/index.vue'),
        meta: { title: '文件资源', icon: 'FolderOpened', perm: 'wiki:view' }
      },
      {
        path: 'system/user',
        name: 'SystemUser',
        component: () => import('@/pages/system/user.vue'),
        meta: { title: '用户管理', icon: 'User', perm: 'system:manage' }
      },
      {
        path: 'system/role',
        name: 'SystemRole',
        component: () => import('@/pages/system/role.vue'),
        meta: { title: '角色权限', icon: 'UserFilled', perm: 'system:manage' }
      },
      {
        path: 'system/project',
        name: 'SystemProject',
        component: () => import('@/pages/system/project.vue'),
        meta: { title: '项目(机型)管理', icon: 'SetUp', perm: 'sonic:edit' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/pages/profile/index.vue'),
        meta: { title: '个人中心', icon: 'Setting', hidden: true }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404'
  }
]
