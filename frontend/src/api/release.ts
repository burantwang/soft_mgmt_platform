import { http } from '@/utils/request'
import type { ApiResult, PageQuery, PageResult } from '@/types/api'
import type {
  ReleaseProject,
  ReleaseRecordCreateForm,
  ReleaseRecordUpdateForm,
  ReleaseRecordVO,
  ReportConfirmForm,
  ReportPreviewVO
} from '@/types/release'

/* ==================== 项目(机型) ==================== */

/** 机型分页 */
export function getProjectPageApi(params: PageQuery): Promise<ApiResult<PageResult<ReleaseProject>>> {
  return http.get<PageResult<ReleaseProject>>('/release/projects', params)
}

/** 启用机型列表（下拉选择） */
export function getProjectEnabledApi(): Promise<ApiResult<ReleaseProject[]>> {
  return http.get<ReleaseProject[]>('/release/projects/enabled')
}

/** 新增机型 */
export function createProjectApi(data: Partial<ReleaseProject>): Promise<ApiResult<ReleaseProject>> {
  return http.post<ReleaseProject>('/release/projects', data)
}

/** 编辑机型 */
export function updateProjectApi(data: Partial<ReleaseProject>): Promise<ApiResult<ReleaseProject>> {
  return http.put<ReleaseProject>('/release/projects', data)
}

/** 删除机型 */
export function deleteProjectApi(id: number): Promise<ApiResult<null>> {
  return http.del<null>(`/release/projects/${id}`)
}

/* ==================== 发布记录 ==================== */

/** 发布记录分页 */
export function getReleasePageApi(params: PageQuery): Promise<ApiResult<PageResult<ReleaseRecordVO>>> {
  return http.get<PageResult<ReleaseRecordVO>>('/release/records', params)
}

/** 发布记录详情 */
export function getReleaseDetailApi(id: number): Promise<ApiResult<ReleaseRecordVO>> {
  return http.get<ReleaseRecordVO>(`/release/records/${id}`)
}

/** 手动创建发布记录 */
export function createReleaseApi(data: ReleaseRecordCreateForm): Promise<ApiResult<number>> {
  return http.post<number>('/release/records', data)
}

/** 编辑发布记录 */
export function updateReleaseApi(data: ReleaseRecordUpdateForm): Promise<ApiResult<null>> {
  return http.put<null>('/release/records', data)
}

/** 删除发布记录 */
export function deleteReleaseApi(id: number): Promise<ApiResult<null>> {
  return http.del<null>(`/release/records/${id}`)
}

/* ==================== 报告上传解析 ==================== */

/** 上传报告并解析预览 */
export function previewReportApi(file: File): Promise<ApiResult<ReportPreviewVO>> {
  const form = new FormData()
  form.append('file', file)
  return http.post<ReportPreviewVO>('/release/report/preview', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 预览确认入库（补录分支/机型） */
export function confirmReportApi(data: ReportConfirmForm): Promise<ApiResult<number>> {
  return http.post<number>('/release/report/confirm', data)
}
