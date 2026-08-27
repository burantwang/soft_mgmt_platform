/**
 * 富文本安全渲染工具（XSS 白名单双防线之【前端防线】）
 * 后端 jsoup 已做白名单过滤，此处 DOMPurify 再做渲染前二次校验。
 */
import DOMPurify from 'dompurify'

// 允许 style 保留（wangeditor 依赖内联样式），DOMPurify 会剔除其中的危险 CSS
DOMPurify.setConfig({ USE_PROFILES: { html: true }, FORBID_TAGS: ['style', 'iframe', 'form'] })

/** 净化 HTML 后返回（用于 v-html 渲染） */
export function sanitizeHtml(html: string): string {
  if (!html) return ''
  return DOMPurify.sanitize(html)
}
