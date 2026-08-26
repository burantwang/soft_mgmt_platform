/**
 * 版本发布模块类型定义
 */

/** 项目(机型) */
export interface ReleaseProject {
  id: number
  projectName: string
  projectCode: string
  description?: string
  /** 状态:1启用 0停用 */
  status: number
  createTime?: string
}

/** 发布记录列表项/详情 */
export interface ReleaseRecordVO {
  id: number
  /** 代码分支 */
  branch: string
  /** 镜像版本号 */
  version?: string
  /** 镜像地址 */
  imageUrl?: string
  /** 结果:1成功 2失败 */
  result: number
  resultDesc: string
  reportFileId?: number
  reportFileName?: string
  totalCount: number
  passedCount: number
  failedCount: number
  errorCount: number
  skippedCount: number
  /** 通过率(%) */
  passRate: number
  durationSec?: number
  reportTime?: string
  publisherId?: number
  publisherName?: string
  publishTime?: string
  /** 来源:1人工上传 2Jenkins推送 3手动创建 */
  source: number
  sourceDesc: string
  remark?: string
  projectIds: number[]
  projectNames: string[]
  /** 失败任务ID(结果失败时) */
  failTaskId?: number
}

/** 报告解析预览结果 */
export interface ReportPreviewVO {
  previewToken: string
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
  failCases: ReportFailCaseVO[]
}

/** 失败用例明细(预览) */
export interface ReportFailCaseVO {
  /** 用例状态:failed失败 error错误 */
  status?: string
  name: string
  log?: string
}

/** 发布记录创建入参 */
export interface ReleaseRecordCreateForm {
  branch: string
  version?: string
  result: number
  projectIds: number[]
  remark?: string
}

/** 发布记录编辑入参 */
export interface ReleaseRecordUpdateForm {
  id: number
  branch: string
  version?: string
  projectIds: number[]
  remark?: string
}

/** 报告确认入库入参 */
export interface ReportConfirmForm {
  previewToken: string
  branch: string
  version?: string
  projectIds: number[]
  remark?: string
}

/* ==================== 看板统计 ==================== */

/** 看板-总览 */
export interface DashboardOverview {
  totalRecords: number
  successCount: number
  failedCount: number
  successRate: number
  totalCases: number
  passedCases: number
  overallPassRate: number
  projectCount: number
}

/** 看板-按分支 */
export interface DashboardBranchStat {
  branch: string
  total: number
  success: number
  failed: number
  successRate: number
}

/** 看板-按机型 */
export interface DashboardProjectStat {
  projectId: number
  projectName: string
  total: number
  success: number
  failed: number
  successRate: number
}

/** 看板-趋势点 */
export interface DashboardTrendPoint {
  date: string
  total: number
  success: number
  failed: number
}

/** 看板-单日分支×机型统计 */
export interface DashboardDayStat {
  branch: string
  projectName: string
  totalCount: number
  passedCount: number
  failedCount: number
  passRate: number
  result: number
  resultDesc: string
  source: number
  sourceDesc: string
  version?: string
  imageUrl?: string
  publishTime?: string
}

/** 看板汇总 */
export interface DashboardSummary {
  /** 累计总览 */
  overview: DashboardOverview
  /** 单日统计日期 */
  dayDate: string
  /** 单日总览 */
  dayOverview: DashboardOverview
  /** 单日发布明细 */
  dayRecords: ReleaseRecordVO[]
  /** 单日分支×机型聚合统计 */
  dayStats: DashboardDayStat[]
  branchStats: DashboardBranchStat[]
  projectStats: DashboardProjectStat[]
  trend: DashboardTrendPoint[]
}

/* ==================== 失败任务追踪 ==================== */

/** 失败用例明细 */
export interface FailCaseVO {
  id: number
  taskId: number
  /** 用例类型:failed失败 error错误 */
  caseType?: string
  caseTypeDesc?: string
  caseName: string
  caseLog?: string
  /** 状态:1待处理 2处理中 3已修复 4非缺陷 */
  status: number
  statusDesc: string
  assigneeId?: number
  assigneeName?: string
  failReason?: string
  fixPlan?: string
  /** AI辅助分析描述 */
  aiAnalysis?: string
  handleTime?: string
}

/** 失败聚合任务（列表/详情） */
export interface FailTaskVO {
  id: number
  taskNo: string
  recordId?: number
  branch?: string
  version?: string
  projectNames: string[]
  /** 状态:1待处理 2处理中 3已完成 4已关闭 */
  status: number
  statusDesc: string
  assigneeId?: number
  assigneeName?: string
  summary: string
  failReason?: string
  fixPlan?: string
  creatorId?: number
  creatorName?: string
  caseTotal: number
  casePending: number
  caseProcessing: number
  caseDone: number
  handleTime?: string
  createTime?: string
}

/** 失败聚合任务详情（含用例明细） */
export interface FailTaskDetailVO extends FailTaskVO {
  cases: FailCaseVO[]
}

/** 用户下拉选项 */
export interface UserOption {
  id: number
  username: string
  nickname: string
}

/** 手动创建失败任务入参 */
export interface FailTaskCreateForm {
  recordId?: number
  summary: string
  failReason?: string
  fixPlan?: string
  assigneeId?: number
  cases?: { caseType?: string; caseName: string; caseLog?: string }[]
}

/** 失败用例更新表单 */
export interface FailCaseUpdateForm {
  status: number
  assigneeId?: number
  failReason?: string
  fixPlan?: string
  isBug?: number
  progress?: string
  conclusion?: string
  /** AI辅助分析描述 */
  aiAnalysis?: string
}

/** 分支×机型分组的失败用例明细 */
export interface GroupedFailCase {
  id: number
  taskId: number
  recordId: number
  caseType: string
  caseTypeDesc: string
  caseName: string
  caseLog?: string
  status: number
  statusDesc: string
  assigneeId?: number
  assigneeName?: string
  failReason?: string
  fixPlan?: string
  isBug?: number
  progress?: string
  conclusion?: string
  /** AI辅助分析描述（原因分析、修改建议等） */
  aiAnalysis?: string
  publishTime?: string
}

/** 原始测试报告文件项 */
export interface ReportFileItem {
  fileId: number
  fileName: string
}

/** 分支×机型失败用例分组 */
export interface FailCaseGrouped {
  branch: string
  projectName: string
  totalCount: number
  passedCount: number
  failedCount: number
  passRate: number
  cases: GroupedFailCase[]
  /** 该分组涉及的原始 HTML 测试报告 */
  reportFiles?: ReportFileItem[]
}

/** 处理失败用例入参 */
export interface FailCaseHandleForm {
  caseId: number
  status: number
  failReason?: string
  fixPlan?: string
}
