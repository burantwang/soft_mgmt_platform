import type { PetPlugin } from '../types'
import QuickLinksPanel from './QuickLinksPanel.vue'

/**
 * 示例插件：快捷窗口。
 * 在小狮子右键菜单中提供系统页面快捷入口。
 */
export const quickLinksPlugin: PetPlugin = {
  id: 'quick-links',
  name: '快捷窗口',
  icon: 'Grid',
  menuItems: [
    {
      key: 'quick-links',
      label: '快捷窗口',
      icon: 'Grid',
      action: (ctx) => ctx.openPanel(QuickLinksPanel, '快捷窗口'),
    },
  ],
}
