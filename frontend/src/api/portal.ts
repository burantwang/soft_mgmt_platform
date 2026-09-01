/**
 * 系统门户 API
 */
import { http } from '@/utils/request'
import type { ApiResult } from '@/types/api'
import type {
  PortalCategory,
  PortalCategoryForm,
  PortalLinkForm,
  PortalSortItem,
  PortalStatusForm
} from '@/types/portal'

/* ==================== 查询 ==================== */

/** 门户展示数据（仅启用板块与链接） */
export function getPortalOverview(): Promise<ApiResult<PortalCategory[]>> {
  return http.get<PortalCategory[]>('/portal/overview')
}

/** 管理列表（全部板块与链接，含停用） */
export function getPortalCategories(): Promise<ApiResult<PortalCategory[]>> {
  return http.get<PortalCategory[]>('/portal/categories')
}

/* ==================== 板块管理 ==================== */

/** 新增板块 */
export function createPortalCategory(data: PortalCategoryForm): Promise<ApiResult<number>> {
  return http.post<number>('/portal/categories', data)
}

/** 更新板块 */
export function updatePortalCategory(id: number, data: PortalCategoryForm): Promise<ApiResult<void>> {
  return http.put<void>(`/portal/categories/${id}`, data)
}

/** 删除板块 */
export function deletePortalCategory(id: number): Promise<ApiResult<void>> {
  return http.del<void>(`/portal/categories/${id}`)
}

/** 板块启停 */
export function togglePortalCategoryStatus(data: PortalStatusForm): Promise<ApiResult<void>> {
  return http.put<void>('/portal/categories/status', data)
}

/** 板块排序 */
export function sortPortalCategories(items: PortalSortItem[]): Promise<ApiResult<void>> {
  return http.put<void>('/portal/categories/sort', { items })
}

/* ==================== 链接管理 ==================== */

/** 新增链接 */
export function createPortalLink(data: PortalLinkForm): Promise<ApiResult<number>> {
  return http.post<number>('/portal/links', data)
}

/** 更新链接 */
export function updatePortalLink(id: number, data: PortalLinkForm): Promise<ApiResult<void>> {
  return http.put<void>(`/portal/links/${id}`, data)
}

/** 删除链接 */
export function deletePortalLink(id: number): Promise<ApiResult<void>> {
  return http.del<void>(`/portal/links/${id}`)
}

/** 链接启停 */
export function togglePortalLinkStatus(data: PortalStatusForm): Promise<ApiResult<void>> {
  return http.put<void>('/portal/links/status', data)
}

/** 链接排序 */
export function sortPortalLinks(items: PortalSortItem[]): Promise<ApiResult<void>> {
  return http.put<void>('/portal/links/sort', { items })
}
