/**
 * 全局 API 类型定义
 */

/** 统一返回体（与后端 Result 对应） */
export interface ApiResult<T = unknown> {
  code: number
  msg: string
  data: T
}

/** 分页返回结构 */
export interface PageResult<T> {
  total: number
  records: T[]
}

/** 分页入参 */
export interface PageQuery {
  page?: number
  size?: number
  [key: string]: unknown
}

/** 登录返回数据 */
export interface LoginResult {
  token: string
  userInfo: UserInfo
  permissions: string[]
  roles: string[]
}

/** 用户信息 / 用户列表项 */
export interface UserInfo {
  id: number
  username: string
  nickname: string
  email?: string
  phone?: string
  /** 状态:1正常 0禁用 */
  status?: number
  /** 是否强制改密:1是 0否 */
  mustChangePwd?: number
  remark?: string
  roleIds?: number[]
  roleNames?: string[]
  /** 角色编码列表：super_admin/admin/employee */
  roleCodes?: string[]
  createTime?: string
}

/** 用户新增/编辑入参 */
export interface UserForm {
  id?: number
  username?: string
  password?: string
  nickname?: string
  email?: string
  phone?: string
  remark?: string
  status?: number
  roleIds?: number[]
}

/** 角色列表项 */
export interface RoleItem {
  id: number
  roleCode: string
  roleName: string
  remark?: string
  /** 状态:1正常 0停用 */
  status?: number
  permissionIds?: number[]
  userCount?: number
  createTime?: string
}

/** 角色新增/编辑入参 */
export interface RoleForm {
  id?: number
  roleCode?: string
  roleName?: string
  remark?: string
  status?: number
}

/** 权限点 */
export interface PermissionItem {
  id: number
  permCode: string
  permName: string
  module: string
  moduleName: string
}
