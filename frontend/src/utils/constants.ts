/**
 * 全局业务常量
 * 禁止在页面/组件中散写业务常量，统一在此维护
 */

/** 应用名称 */
export const APP_NAME = '研发业务平台'

/** Token 本地存储 key */
export const TOKEN_KEY = 'DEV_PLATFORM_TOKEN'

/** 用户权限码本地存储 key */
export const PERMS_KEY = 'DEV_PLATFORM_PERMS'

/** 看板轮询间隔（毫秒），可配置 */
export const DASHBOARD_POLL_INTERVAL = 30 * 1000

/** 上传文件大小上限：50MB */
export const FILE_SIZE_LIMIT = 50 * 1024 * 1024

/** 上传文件白名单后缀 */
export const FILE_ALLOW_EXT = [
  'html', 'pdf',
  'jpg', 'jpeg', 'png', 'gif', 'webp', 'svg',
  'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx',
  'zip', 'rar', '7z', 'tar', 'gz'
] as const

/** 发布结果枚举（与后端 ReleaseResult 对齐） */
export const RELEASE_RESULT = {
  SUCCESS: 1,
  FAIL: 2
} as const

/** 任务状态枚举（与后端 FailTaskStatus 对齐） */
export const TASK_STATUS = {
  PENDING: 1,
  PROCESSING: 2,
  DONE: 3,
  CLOSED: 4
} as const

/** 任务状态文案与标签类型 */
export const TASK_STATUS_MAP: Record<number, { text: string; type: 'info' | 'warning' | 'success' | 'info' }> = {
  [TASK_STATUS.PENDING]: { text: '待处理', type: 'info' },
  [TASK_STATUS.PROCESSING]: { text: '处理中', type: 'warning' },
  [TASK_STATUS.DONE]: { text: '已完成', type: 'success' },
  [TASK_STATUS.CLOSED]: { text: '已关闭', type: 'info' }
}
