/**
 * Home 总览 API（阶段6）
 */
import { http } from '@/utils/request'
import type { ApiResult } from '@/types/api'
import type { HomeSummary } from '@/types/home'

/** 总览聚合 */
export function getHomeSummary(): Promise<ApiResult<HomeSummary>> {
  return http.get<HomeSummary>('/home/summary')
}
