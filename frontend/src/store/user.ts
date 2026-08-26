import { defineStore } from 'pinia'
import { getToken, setToken, removeToken, setPerms, removePerms } from '@/utils/auth'
import { loginApi, getInfoApi, logoutApi, type LoginForm } from '@/api/auth'
import type { UserInfo } from '@/types/api'

/** 用户状态管理 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken() as string,
    userInfo: null as UserInfo | null,
    permissions: [] as string[],
    roles: [] as string[]
  }),

  getters: {
    isLogin: (state) => !!state.token
  },

  actions: {
    /** 登录 */
    async login(form: LoginForm) {
      const res = await loginApi(form)
      const { token, userInfo, permissions, roles } = res.data
      this.token = token
      this.userInfo = userInfo
      this.permissions = permissions
      this.roles = roles
      setToken(token)
      setPerms(permissions)
    },

    /** 拉取当前用户信息 */
    async fetchInfo() {
      const res = await getInfoApi()
      this.userInfo = res.data
      return res.data
    },

    /** 退出登录 */
    async logout() {
      try {
        await logoutApi()
      } catch {
        // 忽略退出接口异常，本地清理
      } finally {
        this.reset()
      }
    },

    /** 清理登录态 */
    reset() {
      this.token = ''
      this.userInfo = null
      this.permissions = []
      this.roles = []
      removeToken()
      removePerms()
    }
  }
})
