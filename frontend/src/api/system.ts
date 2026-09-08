import { http } from '@/utils/request'
import type { ApiResult, GroupForm, GroupItem, MemberItem, PageResult, PageQuery, PermissionItem, RoleForm, RoleItem, UserForm, UserInfo } from '@/types/api'

/* ==================== 用户管理 ==================== */

/** 分页查询用户 */
export function getUserListApi(params: PageQuery): Promise<ApiResult<PageResult<UserInfo>>> {
  return http.get<PageResult<UserInfo>>('/system/users', params)
}

/** 用户详情 */
export function getUserDetailApi(id: number): Promise<ApiResult<UserInfo>> {
  return http.get<UserInfo>(`/system/users/${id}`)
}

/** 新增用户 */
export function createUserApi(data: UserForm): Promise<ApiResult<null>> {
  return http.post<null>('/system/users', data)
}

/** 编辑用户 */
export function updateUserApi(data: UserForm): Promise<ApiResult<null>> {
  return http.put<null>('/system/users', data)
}

/** 删除用户 */
export function deleteUserApi(id: number): Promise<ApiResult<null>> {
  return http.del<null>(`/system/users/${id}`)
}

/** 重置密码 */
export function resetPasswordApi(id: number, password?: string): Promise<ApiResult<null>> {
  return http.put<null>(`/system/users/${id}/password`, { password })
}

/** 启用/禁用用户 */
export function updateUserStatusApi(id: number, status: number): Promise<ApiResult<null>> {
  return http.put<null>(`/system/users/${id}/status`, { status })
}

/* ==================== 角色管理 ==================== */

/** 分页查询角色 */
export function getRoleListApi(params: PageQuery): Promise<ApiResult<PageResult<RoleItem>>> {
  return http.get<PageResult<RoleItem>>('/system/roles', params)
}

/** 全部启用角色(下拉) */
export function getAllRolesApi(): Promise<ApiResult<RoleItem[]>> {
  return http.get<RoleItem[]>('/system/roles/all')
}

/** 新增角色 */
export function createRoleApi(data: RoleForm): Promise<ApiResult<null>> {
  return http.post<null>('/system/roles', data)
}

/** 编辑角色 */
export function updateRoleApi(data: RoleForm): Promise<ApiResult<null>> {
  return http.put<null>('/system/roles', data)
}

/** 删除角色 */
export function deleteRoleApi(id: number): Promise<ApiResult<null>> {
  return http.del<null>(`/system/roles/${id}`)
}

/** 分配角色权限 */
export function assignPermissionsApi(id: number, permissionIds: number[]): Promise<ApiResult<null>> {
  return http.put<null>(`/system/roles/${id}/permissions`, { permissionIds })
}

/** 查询角色已分配权限ID */
export function getRolePermissionIdsApi(id: number): Promise<ApiResult<number[]>> {
  return http.get<number[]>(`/system/roles/${id}/permissions`)
}

/* ==================== 权限点管理 ==================== */

/** 权限点列表 */
export function getPermissionListApi(): Promise<ApiResult<PermissionItem[]>> {
  return http.get<PermissionItem[]>('/system/permissions')
}

/* ==================== 组管理(组织归属) ==================== */

/** 分页查询组 */
export function getGroupListApi(params: PageQuery): Promise<ApiResult<PageResult<GroupItem>>> {
  return http.get<PageResult<GroupItem>>('/system/groups', params)
}

/** 全部启用组(下拉,含人数) */
export function getAllGroupsApi(): Promise<ApiResult<GroupItem[]>> {
  return http.get<GroupItem[]>('/system/groups/all')
}

/** 组详情(含成员ID,编辑回填) */
export function getGroupDetailApi(id: number): Promise<ApiResult<GroupItem>> {
  return http.get<GroupItem>(`/system/groups/${id}`)
}

/** 新增组 */
export function createGroupApi(data: GroupForm): Promise<ApiResult<null>> {
  return http.post<null>('/system/groups', data)
}

/** 编辑组 */
export function updateGroupApi(data: GroupForm): Promise<ApiResult<null>> {
  return http.put<null>('/system/groups', data)
}

/** 删除组(解散,解除全部成员关联) */
export function deleteGroupApi(id: number): Promise<ApiResult<null>> {
  return http.del<null>(`/system/groups/${id}`)
}

/** 分页查询组内成员 */
export function getGroupMemberPageApi(id: number, params: PageQuery): Promise<ApiResult<PageResult<MemberItem>>> {
  return http.get<PageResult<MemberItem>>(`/system/groups/${id}/members`, params)
}

/** 查询组内已选成员ID */
export function getGroupMemberIdsApi(id: number): Promise<ApiResult<number[]>> {
  return http.get<number[]>(`/system/groups/${id}/member-ids`)
}

/** 整体替换组成员 */
export function saveGroupMembersApi(id: number, userIds: number[]): Promise<ApiResult<null>> {
  return http.put<null>(`/system/groups/${id}/members`, { userIds })
}
