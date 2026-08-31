/**
 * 个人任务模块类型定义（聚合 DailySanity / WeeklySanity，未来新增追踪板块时扩展）
 */

/** 个人任务失败用例（统一视图） */
export interface MyTaskCase {
  /** 板块：daily / weekly */
  board: 'daily' | 'weekly' | string
  id: number
  /** 用例类型:failed失败 error错误 */
  caseType?: string
  caseTypeDesc?: string
  /** 用例全名 */
  caseName: string
  /** 用例运行日志 */
  caseLog?: string
  /** 状态:1待处理 2处理中 3已修复 4非缺陷 5已关闭 */
  status: number
  statusDesc?: string
  assigneeId?: number
  assigneeName?: string
  failReason?: string
  fixPlan?: string
  isBug?: number
  /** 分析进展 */
  progress?: string
  /** 结论 */
  conclusion?: string
  /** AI辅助分析描述 */
  aiAnalysis?: string
  /** AI分析是否正确:1是 0否 */
  aiAnalysisCorrect?: number
  aiRootCause?: string
  aiEvidence?: string
  aiSolution?: string
  /** Bug单号(Redmine) */
  bugNo?: string
  /** 问题分类 */
  issueCategory?: string
  /** 处理时间 */
  handleTime?: string
  /** 失败任务编号（daily） */
  taskNo?: string
  /** 代码分支 */
  branch?: string
  /** 镜像版本号 */
  version?: string
  /** 机型名 */
  projectName?: string
  /** 所属模块（weekly） */
  moduleName?: string
  /** 发布时间(daily)/报告上传时间(weekly) */
  publishTime?: string
}

/** 个人任务用例更新表单 */
export interface MyTaskCaseUpdateForm {
  status: number
  assigneeId?: number
  failReason?: string
  fixPlan?: string
  isBug?: number
  progress?: string
  conclusion?: string
  aiAnalysis?: string
  aiAnalysisCorrect?: number
  bugNo?: string
  issueCategory?: string
}
