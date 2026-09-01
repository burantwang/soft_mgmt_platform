/**
 * 系统门户类型定义
 * 结构：板块(portal_category) → 系统链接(portal_link) 两级
 */

/** 系统链接项 */
export interface PortalLinkItem {
  id: number
  categoryId: number
  linkName: string
  url: string
  description?: string
  /** Element Plus 图标名或 emoji */
  icon?: string
  /** 主题色 hex */
  color?: string
  sort: number
  /** 1启用 0停用 */
  status: number
}

/** 门户板块（含其下链接） */
export interface PortalCategory {
  id: number
  categoryName: string
  icon?: string
  color?: string
  description?: string
  sort: number
  status: number
  linkCount: number
  links: PortalLinkItem[]
}

/** 板块创建/更新入参 */
export interface PortalCategoryForm {
  categoryName: string
  icon?: string
  color?: string
  description?: string
  sort?: number
  status?: number
}

/** 链接创建/更新入参 */
export interface PortalLinkForm {
  categoryId: number
  linkName: string
  url: string
  description?: string
  icon?: string
  color?: string
  sort?: number
  status?: number
}

/** 启停入参 */
export interface PortalStatusForm {
  id: number
  status: number
}

/** 排序入参 */
export interface PortalSortItem {
  id: number
  sort: number
}

/** 门户表单可选图标（Element Plus 常用图标名，也可直接输入 emoji） */
export const PORTAL_ICON_OPTIONS = [
  'Monitor', 'DataAnalysis', 'DataBoard', 'DataLine', 'TrendCharts', 'Histogram',
  'Tools', 'SetUp', 'Setting', 'Operation', 'Menu', 'Grid',
  'Link', 'Connection', 'Position', 'Compass', 'Share', 'Paperclip',
  'Promotion', 'Notification', 'Bell', 'ChatDotRound', 'Message', 'MessageBox',
  'Reading', 'Document', 'FolderOpened', 'Collection', 'Memo', 'Files',
  'Box', 'Coin', 'Wallet', 'ShoppingBag', 'OfficeBuilding', 'Platform',
  'House', 'Cloudy', 'MostlyCloudy', 'Sunny', 'Cpu', 'Aim',
  'Github', 'User', 'Star', 'StarFilled', 'Suitcase', 'Headset',
  'Search', 'View', 'Timer', 'VideoCamera', 'Key', 'Lock',
  'Warning', 'CircleCheck', 'CircleClose', 'QuestionFilled', 'InfoFilled'
] as const

/** 门户主题色预设 */
export const PORTAL_COLOR_OPTIONS = [
  '#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399',
  '#8E44AD', '#16A085', '#2C3E50', '#E67E22', '#3498DB'
]
