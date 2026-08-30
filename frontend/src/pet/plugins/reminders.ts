import type { PetPlugin } from '../types'
import RemindersPanel from './RemindersPanel.vue'

/**
 * 示例插件：任务提醒（预留）。
 * 当前为占位面板；未来接入任务系统后在此扩展真实提醒能力。
 */
export const remindersPlugin: PetPlugin = {
  id: 'reminders',
  name: '任务提醒',
  icon: 'Bell',
  menuItems: [
    {
      key: 'reminders',
      label: '任务提醒',
      icon: 'Bell',
      action: (ctx) => ctx.openPanel(RemindersPanel, '任务提醒'),
    },
  ],
}
