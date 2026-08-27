/**
 * Home 总览类型定义（阶段6）
 */

/** 最新动态条目 */
export interface RecentItem {
  id: number
  title: string
  subtitle?: string
  time?: string
  /** 前端跳转地址 */
  path?: string
}

/** Home 总览聚合 */
export interface HomeSummary {
  /* ---- 发布板块 ---- */
  releaseTotal: number
  releaseToday: number
  releaseSuccess: number
  releaseFailed: number

  /* ---- 任务追踪板块 ---- */
  taskPending: number
  taskProcessing: number
  taskTotal: number
  /** 我的待办 */
  myTodo: number

  /* ---- Wiki / 文件 ---- */
  wikiDocCount: number
  fileCount: number

  /* ---- 最新动态 ---- */
  recentReleases: RecentItem[]
  recentTasks: RecentItem[]
  recentWikis: RecentItem[]
  recentFiles: RecentItem[]
}
