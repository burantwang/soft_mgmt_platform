import type { Directive, DirectiveBinding } from 'vue'
import { getPerms } from '@/utils/auth'

/**
 * 按钮级权限指令
 * 用法：<el-button v-permission="'sonic:edit'">新增</el-button>
 * 无对应权限时从 DOM 移除元素（与后端权限注解一一对应）
 */
export const permission: Directive<HTMLElement, string> = {
  mounted(el: HTMLElement, binding: DirectiveBinding<string>) {
    const required = binding.value
    if (!required) return
    const perms = getPerms()
    if (!perms.includes(required)) {
      el.parentNode?.removeChild(el)
    }
  }
}
