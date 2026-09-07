import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'
import { permission } from './directives/permission'
import { installDefaultPetPlugins } from './pet'
import './styles/index.scss'

// 安装小狮子电子宠物内置插件（快捷窗口 / 任务提醒预留）
installDefaultPetPlugins()

const app = createApp(App)

// 注册 Element-Plus 图标（供菜单等动态组件使用）
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 抑制 Element Plus collapse-transition 在 Vue 3.5 下的已知刷屏警告
// 见：https://github.com/element-plus/element-plus/issues
const IGNORED_WARNINGS = [
  'Slot "default" invoked outside of the render function',
  'Slot "title" invoked outside of the render function'
]
app.config.warnHandler = (msg, instance, trace) => {
  if (IGNORED_WARNINGS.some((hint) => msg.includes(hint))) {
    return
  }
  console.warn(msg, instance, trace)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

// 按钮级权限指令：v-permission="'sonic:edit'"
app.directive('permission', permission)

app.mount('#app')
