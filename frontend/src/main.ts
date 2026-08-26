import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'
import { permission } from './directives/permission'
import './styles/index.scss'

const app = createApp(App)

// 注册 Element-Plus 图标（供菜单等动态组件使用）
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

// 按钮级权限指令：v-permission="'sonic:edit'"
app.directive('permission', permission)

app.mount('#app')
