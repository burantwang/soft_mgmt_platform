import axios, { type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { getToken, removeToken, removePerms } from './auth'
import type { ApiResult } from '@/types/api'

/**
 * axios 统一封装
 * - 请求拦截：注入 Token
 * - 响应拦截：统一处理业务码、鉴权失效、全局错误提示
 */
const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000
})

// 请求拦截
service.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截
service.interceptors.response.use(
  (response) => {
    const res = response.data as ApiResult
    // 后端统一返回体
    if (res && typeof res.code !== 'undefined') {
      if (res.code === 200) {
        return res
      }
      // 鉴权失效：清除登录态并跳转登录页
      if (res.code >= 40100 && res.code < 40200) {
        removeToken()
        removePerms()
        router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
        ElMessage.error(res.msg || '登录已失效，请重新登录')
        return Promise.reject(new Error(res.msg))
      }
      ElMessage.error(res.msg || '请求失败')
      return Promise.reject(new Error(res.msg))
    }
    return response.data
  },
  (error) => {
    if (error.response?.status === 401) {
      removeToken()
      removePerms()
      router.push('/login')
    }
    const msg = error.response?.data?.msg || error.message || '网络异常，请稍后重试'
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

/** 统一请求方法（返回后端统一响应体，调用方取 .data 获取业务数据） */
export default async function request<T = unknown>(config: AxiosRequestConfig): Promise<ApiResult<T>> {
  // 响应拦截器已把 AxiosResponse 解包为后端统一响应体，此处按类型收缩
  return (await service.request<ApiResult<T>>(config)) as unknown as ApiResult<T>
}

/** 便捷方法 */
export const http = {
  get: <T = unknown>(url: string, params?: Record<string, unknown>, config?: AxiosRequestConfig) =>
    request<T>({ method: 'get', url, params, ...config }),
  post: <T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig) =>
    request<T>({ method: 'post', url, data, ...config }),
  put: <T = unknown>(url: string, data?: unknown) =>
    request<T>({ method: 'put', url, data }),
  del: <T = unknown>(url: string, params?: Record<string, unknown>) =>
    request<T>({ method: 'delete', url, params })
}
