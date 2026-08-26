import { http } from '@/utils/request'
import type { ApiResult, LoginResult, UserInfo } from '@/types/api'

/** 登录入参 */
export interface LoginForm {
  username: string
  password: string
}

/** 登录 */
export function loginApi(data: LoginForm): Promise<ApiResult<LoginResult>> {
  return http.post<LoginResult>('/auth/login', data)
}

/** 获取当前用户信息 */
export function getInfoApi(): Promise<ApiResult<UserInfo>> {
  return http.get<UserInfo>('/auth/info')
}

/** 退出登录 */
export function logoutApi(): Promise<ApiResult<null>> {
  return http.post<null>('/auth/logout')
}

/** 修改密码 */
export function changePasswordApi(data: { oldPassword: string; newPassword: string }): Promise<ApiResult<null>> {
  return http.put<null>('/auth/password', data)
}
