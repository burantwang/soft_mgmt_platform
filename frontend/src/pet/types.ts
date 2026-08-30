import type { Component } from 'vue'

/** 宠物行为状态 */
export type PetBehavior =
  | 'idle' // 待机（呼吸、摇尾巴）
  | 'walk' // 走动
  | 'play' // 玩耍（跳跃）
  | 'sleep' // 睡觉
  | 'docked' // 隐藏在窗口边缘（只露一点）
  | 'dragging' // 被拖拽中

/** 宠物持久化配置（localStorage 存储） */
export interface PetConfig {
  /** 是否显示宠物 */
  enabled: boolean
  /** 是否隐藏到窗口边缘 */
  docked: boolean
  /** 是否静音（关闭气泡台词） */
  mute: boolean
  /** 缩放比例 0.7 ~ 1.6 */
  scale: number
  /** 自定义位置（屏幕坐标）；未设置时默认右下角 */
  pos?: { x: number; y: number }
}

/** 宠物右键菜单项 */
export interface PetMenuItem {
  key: string
  label: string
  /** Element Plus 图标组件名 */
  icon?: string
  /** 是否在顶部显示分割线 */
  divided?: boolean
  disabled?: boolean
  action?: (ctx: PetPluginContext) => void
}

/** 暴露给插件/菜单使用的宠物控制 API */
export interface PetPetApi {
  /** 当前行为状态 */
  readonly behavior: PetBehavior
  /** 让狮子说一句话（气泡） */
  say(text?: string): void
  /** 让狮子玩耍一会 */
  play(): void
  /** 隐藏到窗口边缘（只露一点） */
  dock(): void
  /** 从边缘出来 */
  undock(): void
  /** 开关气泡台词 */
  toggleMute(): void
  /** 调整大小（0.7 ~ 1.6） */
  setScale(scale: number): void
}

/** 插件上下文：插件在菜单点击 / 初始化时可使用的全部能力 */
export interface PetPluginContext {
  /** 当前配置（实时读取） */
  config: PetConfig
  /** 宠物控制 API */
  pet: PetPetApi
  /**
   * 打开一个功能面板（快捷窗口、任务提醒等）。
   * component 为面板内容组件，会收到 close 函数作为 prop 用于自行关闭。
   */
  openPanel(component: Component, title?: string): void
  /** 关闭当前面板 */
  closePanel(): void
}

/**
 * 宠物功能插件接口 —— 未来扩展的统一入口。
 *
 * 新增能力（快捷入口、任务提醒、语音、天气、节日彩蛋……）只需：
 *   1. 实现本接口（menuItems 注入右键菜单 / onInit 做初始化）
 *   2. 调用 registerPetPlugin(plugin) 注册
 * 即可自动出现在小狮子的右键菜单中，无需改动主组件。
 */
export interface PetPlugin {
  /** 全局唯一 id */
  id: string
  /** 插件名 */
  name: string
  /** 图标（Element Plus 图标名） */
  icon?: string
  /** 注入右键菜单的菜单项 */
  menuItems?: PetMenuItem[]
  /** 插件初始化（宠物挂载时调用一次） */
  onInit?: (ctx: PetPluginContext) => void
  /** 插件销毁（宠物卸载时调用） */
  onDispose?: () => void
}
