import { http } from '@/utils/request'
import type { ApiResult } from '@/types/api'
import type {
  WeeklyFailCaseGrouped,
  WeeklyFailCaseUpdateForm,
  WeeklyRecentDayStat,
  WeeklyReportConfirmForm,
  WeeklyReportPreviewVO
} from '@/types/weekly'
import type { AiAnalysisResult, UserOption } from '@/types/release'

/* ==================== WeeklySanity 报告上传 ==================== */

/** 上传多个 HTML 报告并解析预览（每个文件为一个模块） */
export function previewWeeklyReportApi(files: File[]): Promise<ApiResult<WeeklyReportPreviewVO>> {
  const form = new FormData()
  files.forEach((f) => form.append('files', f))
  return http.post<WeeklyReportPreviewVO>('/weekly/report/preview', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 预览确认入库（为每个模块×机型生成独立周度报告与失败用例） */
export function confirmWeeklyReportApi(data: WeeklyReportConfirmForm): Promise<ApiResult<number[]>> {
  return http.post<number[]>('/weekly/report/confirm', data)
}

/* ==================== 分组查询与统计 ==================== */

/** 按日期+分支×机型分组查询失败用例 */
export function getWeeklyGroupedCasesApi(params: {
  date?: string
  branch?: string
  projectName?: string
  caseName?: string
}): Promise<ApiResult<WeeklyFailCaseGrouped[]>> {
  return http.get<WeeklyFailCaseGrouped[]>('/weekly/report/grouped', params)
}

/** 最近7天分析完成统计 */
export function getWeeklyRecentWeekStatsApi(): Promise<ApiResult<WeeklyRecentDayStat[]>> {
  return http.get<WeeklyRecentDayStat[]>('/weekly/report/recent-week-stats')
}

/** 获取原始测试报告文件内容（返回 Blob，用于新窗口打开） */
export async function getWeeklyReportContentApi(fileId: number): Promise<Blob> {
  const res = await http.get<Blob>(`/weekly/reports/${fileId}/content`, {}, { responseType: 'blob' })
  return res as unknown as Blob
}

/* ==================== 失败用例处理 ==================== */

/** 更新失败用例处理信息 */
export function updateWeeklyFailCaseApi(id: number, data: WeeklyFailCaseUpdateForm): Promise<ApiResult<null>> {
  return http.put<null>(`/weekly/fail-cases/${id}`, data)
}

/** 快速指派用例责任人（仅更新 assigneeId，立即生效） */
export function assignWeeklyFailCaseApi(id: number, assigneeId: number | null): Promise<ApiResult<null>> {
  return http.put<null>(`/weekly/fail-cases/${id}/assign`, { assigneeId })
}

/** 触发 AI 分析失败用例（同步等待结果；AI 模型调用较慢，设置 180s 超时） */
export function aiAnalyzeWeeklyFailCaseApi(id: number): Promise<ApiResult<AiAnalysisResult>> {
  return http.post<AiAnalysisResult>(`/weekly/fail-cases/${id}/ai-analyze`, undefined, { timeout: 180000 })
}

/** 导出某日期全部失败用例 Excel（blob 下载） */
export async function exportWeeklyExcelApi(date: string): Promise<void> {
  const res = (await http.get<unknown>('/weekly/report/export', { date }, {
    responseType: 'blob',
    timeout: 60000
  })) as unknown as Blob
  const url = URL.createObjectURL(res)
  const link = document.createElement('a')
  link.href = url
  link.download = `Weekly_Sanity_${date}_失败用例.xlsx`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

/** 启用用户下拉（指派责任人） */
export function getWeeklyEnabledUsersApi(): Promise<ApiResult<UserOption[]>> {
  return http.get<UserOption[]>('/release/users/enabled')
}
