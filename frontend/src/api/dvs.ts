import { http } from '@/utils/request'
import type { ApiResult } from '@/types/api'
import type { MyTaskCase } from '@/types/mytask'
import type {
  DvsFailCaseGrouped,
  DvsFailCaseUpdateForm,
  DvsRecentDayStat,
  DvsReportConfirmForm,
  DvsReportPreviewVO
} from '@/types/dvs'
import type { AiAnalysisResult, UserOption } from '@/types/release'

/* ==================== DVS 报告上传 ==================== */

/** 上传多个 HTML 报告并解析预览（每个文件为一个模块） */
export function previewDvsReportApi(files: File[]): Promise<ApiResult<DvsReportPreviewVO>> {
  const form = new FormData()
  files.forEach((f) => form.append('files', f))
  return http.post<DvsReportPreviewVO>('/dvs/report/preview', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 预览确认入库（为每个模块×机型生成独立 DVS 报告与失败用例） */
export function confirmDvsReportApi(data: DvsReportConfirmForm): Promise<ApiResult<number[]>> {
  return http.post<number[]>('/dvs/report/confirm', data)
}

/* ==================== 分组查询与统计 ==================== */

/** 按日期+分支×机型分组查询失败用例 */
export function getDvsGroupedCasesApi(params: {
  date?: string
  branch?: string
  projectName?: string
  caseName?: string
}): Promise<ApiResult<DvsFailCaseGrouped[]>> {
  return http.get<DvsFailCaseGrouped[]>('/dvs/report/grouped', params)
}

/** 最近7天分析完成统计 */
export function getDvsRecentWeekStatsApi(): Promise<ApiResult<DvsRecentDayStat[]>> {
  return http.get<DvsRecentDayStat[]>('/dvs/report/recent-week-stats')
}

/** 个人任务：当前用户被指派的 DVS 失败用例（scope=active 仅未完成，all 含全部） */
export function getMyDvsCasesApi(scope: 'active' | 'all'): Promise<ApiResult<MyTaskCase[]>> {
  return http.get<MyTaskCase[]>('/dvs/fail-cases/mine', { scope })
}

/** 获取原始测试报告文件内容（返回 Blob，用于新窗口打开） */
export async function getDvsReportContentApi(fileId: number): Promise<Blob> {
  const res = await http.get<Blob>(`/dvs/reports/${fileId}/content`, {}, { responseType: 'blob' })
  return res as unknown as Blob
}

/* ==================== 失败用例处理 ==================== */

/** 更新失败用例处理信息 */
export function updateDvsFailCaseApi(id: number, data: DvsFailCaseUpdateForm): Promise<ApiResult<null>> {
  return http.put<null>(`/dvs/fail-cases/${id}`, data)
}

/** 快速指派用例责任人（仅更新 assigneeId，立即生效） */
export function assignDvsFailCaseApi(id: number, assigneeId: number | null): Promise<ApiResult<null>> {
  return http.put<null>(`/dvs/fail-cases/${id}/assign`, { assigneeId })
}

/** 触发 AI 分析失败用例（同步等待结果；AI 模型调用较慢，设置 180s 超时） */
export function aiAnalyzeDvsFailCaseApi(id: number): Promise<ApiResult<AiAnalysisResult>> {
  return http.post<AiAnalysisResult>(`/dvs/fail-cases/${id}/ai-analyze`, undefined, { timeout: 180000 })
}

/** 导出某日期全部失败用例 Excel（blob 下载） */
export async function exportDvsExcelApi(date: string): Promise<void> {
  const res = (await http.get<unknown>('/dvs/report/export', { date }, {
    responseType: 'blob',
    timeout: 60000
  })) as unknown as Blob
  const url = URL.createObjectURL(res)
  const link = document.createElement('a')
  link.href = url
  link.download = `DVS_${date}_失败用例.xlsx`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

/** 启用用户下拉（指派责任人） */
export function getDvsEnabledUsersApi(): Promise<ApiResult<UserOption[]>> {
  return http.get<UserOption[]>('/release/users/enabled')
}
