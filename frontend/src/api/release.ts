import { http } from '@/utils/request'
import type { ApiResult, PageQuery, PageResult } from '@/types/api'
import type {
  DashboardSummary,
  FailCaseGrouped,
  FailCaseHandleForm,
  FailCaseUpdateForm,
  FailTaskCreateForm,
  FailTaskDetailVO,
  FailTaskVO,
  ReleaseProject,
  ReleaseRecordCreateForm,
  ReleaseRecordUpdateForm,
  ReleaseRecordVO,
  ReportConfirmForm,
  ReportPreviewVO,
  UserOption
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

/* ==================== 看板与历史导出 ==================== */

/** 看板统计汇总，date 不传默认今天（yyyy-MM-dd） */
export function getDashboardSummaryApi(date?: string): Promise<ApiResult<DashboardSummary>> {
  return http.get<DashboardSummary>('/release/dashboard/summary', date ? { date } : undefined)
}

/** 历史分支下拉 */
export function getBranchOptionsApi(): Promise<ApiResult<string[]>> {
  return http.get<string[]>('/release/records/branches')
}

/** 导出发布记录 Excel（返回 Blob） */
export async function exportRecordsApi(params: Record<string, unknown>): Promise<Blob> {
  // responseType=blob 时响应拦截器直接返回 Blob（不走统一响应体解包）
  const res = await http.get<Blob>('/release/records/export', params, { responseType: 'blob' })
  return res as unknown as Blob
}

/* ==================== DailySanity任务 ==================== */

/** 失败任务分页 */
export function getFailTaskPageApi(params: PageQuery): Promise<ApiResult<PageResult<FailTaskVO>>> {
  return http.get<PageResult<FailTaskVO>>('/release/fail-tasks', params)
}

/** 我的待办分页 */
export function getMyFailTaskPageApi(params: PageQuery): Promise<ApiResult<PageResult<FailTaskVO>>> {
  return http.get<PageResult<FailTaskVO>>('/release/fail-tasks/mine', params)
}

/** 失败任务详情（含用例明细） */
export function getFailTaskDetailApi(id: number): Promise<ApiResult<FailTaskDetailVO>> {
  return http.get<FailTaskDetailVO>(`/release/fail-tasks/${id}`)
}

/** 手动创建失败任务 */
export function createFailTaskApi(data: FailTaskCreateForm): Promise<ApiResult<number>> {
  return http.post<number>('/release/fail-tasks', data)
}

/** 编辑失败任务 */
export function updateFailTaskApi(data: {
  id: number
  summary?: string
  failReason?: string
  fixPlan?: string
}): Promise<ApiResult<null>> {
  return http.put<null>('/release/fail-tasks', data)
}

/** 删除失败任务 */
export function deleteFailTaskApi(id: number): Promise<ApiResult<null>> {
  return http.del<null>(`/release/fail-tasks/${id}`)
}

/** 指派失败任务责任人 */
export function assignFailTaskApi(id: number, assigneeId: number): Promise<ApiResult<null>> {
  return http.put<null>(`/release/fail-tasks/${id}/assign`, { assigneeId })
}

/** 失败任务状态流转 */
export function changeFailTaskStatusApi(id: number, status: number): Promise<ApiResult<null>> {
  return http.put<null>(`/release/fail-tasks/${id}/status`, { status })
}

/** 处理失败用例明细 */
export function handleFailCaseApi(data: FailCaseHandleForm): Promise<ApiResult<null>> {
  return http.put<null>('/release/fail-cases', data)
}

/** 启用用户下拉（指派责任人） */
export function getEnabledUsersApi(): Promise<ApiResult<UserOption[]>> {
  return http.get<UserOption[]>('/release/users/enabled')
}

/** 按日期+分支×机型分组查询失败用例 */
export function getGroupedFailCasesApi(params: { date?: string; branch?: string; projectName?: string; caseName?: string }): Promise<ApiResult<FailCaseGrouped[]>> {
  return http.get<FailCaseGrouped[]>('/release/fail-cases/grouped', params)
}

/** 更新失败用例处理信息 */
export function updateFailCaseApi(id: number, data: FailCaseUpdateForm): Promise<ApiResult<null>> {
  return http.put<null>(`/release/fail-cases/${id}`, data)
}

/** 获取原始测试报告文件内容（返回 Blob，用于新窗口打开） */
export async function getReportContentApi(fileId: number): Promise<Blob> {
  const res = await http.get<Blob>(`/release/records/reports/${fileId}/content`, {}, { responseType: 'blob' })
  return res as unknown as Blob
}
