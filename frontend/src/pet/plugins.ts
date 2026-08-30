import type { PetPlugin } from './types'
import { quickLinksPlugin } from './plugins/quick-links'
import { remindersPlugin } from './plugins/reminders'

/** 插件注册表 */
const registry = new Map<string, PetPlugin>()

/**
 * 注册一个宠物功能插件；返回注销函数。
 * 未来扩展新能力时调用：registerPetPlugin(myPlugin)
 */
export function registerPetPlugin(plugin: PetPlugin): () => void {
  registry.set(plugin.id, plugin)
  return () => {
    registry.delete(plugin.id)
  }
}

/** 获取全部已注册插件 */
export function getPetPlugins(): PetPlugin[] {
  return Array.from(registry.values())
}

/** 安装内置插件（应用启动时调用一次） */
export function installDefaultPetPlugins(): void {
  registerPetPlugin(quickLinksPlugin)
  registerPetPlugin(remindersPlugin)
}
