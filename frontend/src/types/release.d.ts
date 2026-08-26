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
