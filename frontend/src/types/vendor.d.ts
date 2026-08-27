declare module '@wangeditor/editor-for-vue'
declare module 'katex' {
  export function render(tex: string, container: HTMLElement, options?: Record<string, unknown>): void
  export function renderToString(tex: string, options?: Record<string, unknown>): string
}
