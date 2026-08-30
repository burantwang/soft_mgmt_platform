/**
 * 电子宠物模块统一出口。
 *
 * 使用方式：
 *   1. 入口处调用 installDefaultPetPlugins() 安装内置插件
 *   2. 在布局组件中放置 <PetLion />
 *   3. 未来扩展：实现 PetPlugin 接口并调用 registerPetPlugin() 即可
 */
export * from './types'
export { PetEngine } from './engine'
export { registerPetPlugin, getPetPlugins, installDefaultPetPlugins } from './plugins'
export { default as PetLion } from './PetLion.vue'
