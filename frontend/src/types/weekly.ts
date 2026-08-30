/**
 * WeeklySanity 模块类型定义（独立于 DailySanity，数据不共享）
 */

/** 单模块(HTML文件)解析预览结果 */
export interface WeeklyPreviewItemVO {
  /** 原始文件名（作为模块名） */
  fileName: string
  fileSize: number
  version?: string
  /** 1成功 2失败 */
  result: number
  resultDesc: string
  totalCount: number
  passedCount: number
  failedCount: number
  errorCount: number
  skippedCount: number
  durationSec?: number
  reportTime?: string
  failCases: WeeklyPreviewFailCase[]
}

/** 失败用例明细(预览) */
export interface WeeklyPreviewFailCase {
  /** 用例状态:failed失败 error错误 */
  status?: string
  name: string
  log?: string
}

/** 多文件报告解析预览结果 */
export interface WeeklyReportPreviewVO {
  previewToken: string
  items: WeeklyPreviewItemVO[]
}

/** 报告确认入库入参 */
export interface WeeklyReportConfirmForm {
  previewToken: string
  branch: string
  version?: string
  projectIds: number[]
  remark?: string
}

/** 分组内模块(HTML)统计 */
export interface WeeklyModuleVO {
  /** 周度报告ID */
  reportId: number
  /** 模块名(HTML文件名) */
  moduleName: string
  /** 1成功 2失败 */
  result: number
  resultDesc: string
  totalCount: number
  passedCount: number
  failedCount: number
  errorCount: number
  skippedCount: number
  /** 通过率(%) */
  passRate: number
  durationSec?: number
  reportTime?: string
  reportFileId?: number
  reportFileName?: string
}

/** 失败用例分组明细项 */
export interface WeeklyFailCase {
  id: number
  /** 周度报告ID */
  reportId: number
  /** 所属模块(HTML文件名) */
  moduleName?: string
  caseType?: string
  caseTypeDesc?: string
  caseName: string
  caseLog?: string
  /** 状态:1待处理 2处理中 3已修复 4非缺陷 */
  status: number
  statusDesc?: string
  assigneeId?: number
  assigneeName?: string
  failReason?: string
  fixPlan?: string
  isBug?: number
  progress?: string
  conclusion?: string
  /** AI辅助分析描述 */
  aiAnalysis?: string
  /** AI分析是否正确:1是 0否 */
  aiAnalysisCorrect?: number
  publishTime?: string
}

/** 原始报告文件项 */
export interface WeeklyReportFileItem {
  fileId: number
  fileName: string
}

/** 分支×机型失败用例分组 */
export interface WeeklyFailCaseGrouped {
  branch: string
  projectName: string
  /** 该分组涉及的模块(HTML)统计列表 */
  modules: WeeklyModuleVO[]
  totalCount: number
  passedCount: number
  failedCount: number
  passRate: number
  cases: WeeklyFailCase[]
  /** 该分组涉及的原始 HTML 测试报告 */
  reportFiles: WeeklyReportFileItem[]
}

/** 最近某天分析完成统计 */
export interface WeeklyRecentDayStat {
  date: string
  totalCount: number
  analyzedCount: number
  /** 分析完成率(%)，当天无执行明细时为 null */
  rate: number | null
}

/** 失败用例更新表单 */
export interface WeeklyFailCaseUpdateForm {
  status: number
  assigneeId?: number
  failReason?: string
  fixPlan?: string
  isBug?: number
  progress?: string
  conclusion?: string
  /** AI辅助分析描述 */
  aiAnalysis?: string
  /** AI分析是否正确:1是 0否 */
  aiAnalysisCorrect?: number
}
