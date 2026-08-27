<template>
  <div class="wiki-page">
    <el-container class="wiki-container">
      <!-- 左侧：目录树 -->
      <el-aside width="300px" class="wiki-tree-panel">
        <div class="wiki-tree-header">
          <el-input
            v-model="keyword"
            placeholder="搜索文档标题"
            clearable
            size="small"
            :prefix-icon="Search"
            @input="filterTree"
          />
        </div>
        <div v-if="canEdit" class="wiki-tree-actions">
          <el-button type="primary" size="small" plain :icon="Plus" @click="handleCreateRoot">
            新建
          </el-button>
          <el-button size="small" plain :icon="Upload" :loading="importing" @click="triggerImport">
            上传文档
          </el-button>
          <input
            ref="importInputRef"
            type="file"
            accept=".md,.markdown,.txt"
            hidden
            @change="onImportFile"
          />
        </div>
        <div class="wiki-tree-body">
          <el-tree
            ref="treeRef"
            v-loading="loadingTree"
            :data="treeData"
            node-key="id"
            highlight-current
            default-expand-all
            :expand-on-click-node="false"
            :props="{ label: 'title', children: 'children' }"
            empty-text="暂无文档，点击左上角新建"
            @node-click="handleNodeClick"
          >
            <template #default="{ data }">
              <div class="wiki-tree-node" :title="data.title">
                <el-icon v-if="!data.hasContent" :size="14" class="tree-folder-icon">
                  <Folder />
                </el-icon>
                <el-icon v-else :size="14" class="tree-doc-icon">
                  <Document />
                </el-icon>
                <span class="tree-label">{{ data.title }}</span>
              </div>
            </template>
          </el-tree>
        </div>
      </el-aside>

      <!-- 右侧：内容区 -->
      <el-main ref="mainRef" class="wiki-content-panel" @scroll="onViewerScroll">
        <!-- 未选中空态 -->
        <div v-if="!current && !editing" class="wiki-empty">
          <el-icon :size="64" color="#c0c4cc"><Notebook /></el-icon>
          <p>从左侧选择文档，或新建一篇文档</p>
        </div>

        <!-- 编辑模式 -->
        <div v-else-if="editing" class="wiki-editor">
          <div class="wiki-editor-title">
            <el-input v-model="form.title" placeholder="文档标题" maxlength="255" size="large" />
          </div>
          <div class="wiki-editor-toolbar">
            <Toolbar :editor="editorRef" :default-config="toolbarConfig" mode="default" />
          </div>
          <div class="wiki-editor-body">
            <Editor
              v-model="form.content"
              :default-config="editorConfig"
              mode="default"
              @on-created="handleEditorCreated"
            />
          </div>
          <div class="wiki-editor-footer">
            <el-button @click="cancelEdit">取消</el-button>
            <el-button type="primary" :loading="saving" @click="saveDoc">{{ form.id ? '保存修改' : '创建' }}</el-button>
          </div>
        </div>

        <!-- 阅读模式 -->
        <div v-else-if="current" class="wiki-viewer">
          <div class="wiki-viewer-header">
            <div class="wiki-viewer-title">
              <h2>{{ current.title }}</h2>
              <el-tag v-if="current.childCount > 0" size="small" type="info">
                子文档 {{ current.childCount }}
              </el-tag>
            </div>
            <div v-if="canEdit" class="wiki-viewer-actions">
              <el-button size="small" :icon="Plus" @click="handleCreateChild">新建子文档</el-button>
              <el-button size="small" type="primary" :icon="Edit" @click="startEdit">编辑</el-button>
              <el-button size="small" type="danger" :icon="Delete" @click="handleDelete">删除</el-button>
            </div>
          </div>
          <div class="wiki-viewer-meta">
            <span v-if="current.creatorName">创建：{{ current.creatorName }}</span>
            <span v-if="current.editorName">最后编辑：{{ current.editorName }}</span>
            <span v-if="current.updateTime">更新于 {{ formatTime(current.updateTime) }}</span>
          </div>
          <el-divider />
          <div class="wiki-viewer-body">
            <!-- 正文 -->
            <div class="wiki-viewer-main">
              <div v-if="current.hasContent" ref="contentRef" class="wiki-viewer-content" v-html="safeContent"></div>
              <el-empty v-else description="该文档暂无正文内容" :image-size="80" />
            </div>
            <!-- 目录大纲（Typora 风格） -->
            <aside v-if="tocItems.length" class="wiki-toc">
              <div class="toc-title">
                <el-icon :size="14"><Menu /></el-icon>
                目录大纲
              </div>
              <ul class="toc-list">
                <li
                  v-for="item in tocItems"
                  :key="item.id"
                  class="toc-item"
                  :class="[`toc-lv-${item.level}`, { active: activeHeading === item.id }]"
                  :title="item.text"
                  @click="scrollToHeading(item)"
                >
                  {{ item.text }}
                </li>
              </ul>
            </aside>
          </div>
        </div>
      </el-main>
    </el-container>

    <!-- 导入文档确认 -->
    <el-dialog v-model="importDialogVisible" title="导入文档" width="480px" append-to-body>
      <el-form label-width="80px" @submit.prevent>
        <el-form-item label="标题">
          <el-input v-model="importForm.title" maxlength="255" placeholder="文档标题（默认取文件名）" />
        </el-form-item>
        <el-form-item label="存放位置">
          <el-tree-select
            v-model="importForm.parentId"
            :data="treeData"
            node-key="id"
            :props="{ label: 'title', children: 'children' }"
            check-strictly
            default-expand-all
            :render-after-expand="false"
            placeholder="选择父节点（不选则为根目录）"
            style="width: 100%"
          />
          <div class="import-tip">提示：仅支持 .md / .markdown / .txt 文本，其他格式请先另存为 Markdown</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="importing" @click="confirmImport">导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, reactive, ref, shallowRef, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Delete,
  Document,
  Edit,
  Folder,
  Menu,
  Notebook,
  Plus,
  Search,
  Upload
} from '@element-plus/icons-vue'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import '@wangeditor/editor/dist/css/style.css'
import { Boot } from '@wangeditor/editor'
import formulaModule from '@wangeditor/plugin-formula'
import type { IDomEditor, IEditorConfig, IToolbarConfig } from '@wangeditor/editor'
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'
import katex from 'katex'
import 'katex/dist/katex.min.css'
import { getPerms } from '@/utils/auth'
import { sanitizeHtml } from '@/utils/sanitize'
import { FILE_SIZE_LIMIT } from '@/utils/constants'
import { createWikiDoc, deleteWikiDoc, getWikiDetail, getWikiTree, updateWikiDoc } from '@/api/wiki'
import type { WikiDocDetail, WikiDocForm, WikiDocNode } from '@/types/wiki'

Boot.registerModule(formulaModule)

const canEdit = getPerms().includes('wiki:edit')

const keyword = ref('')
const loadingTree = ref(false)
const treeData = ref<WikiDocNode[]>([])
const current = ref<WikiDocDetail | null>(null)
const saving = ref(false)
const editing = ref(false)
const form = reactive<WikiDocForm & { id?: number }>({ id: undefined, title: '', content: '', parentId: 0 })

const safeContent = computed(() => sanitizeHtml(current.value?.content || ''))

/* ---------------- 上传文档导入 ---------------- */

const importInputRef = ref<HTMLInputElement>()
const importing = ref(false)
const importDialogVisible = ref(false)
const importContent = ref('')
const importForm = reactive({ title: '', parentId: 0 })

function triggerImport() {
  importInputRef.value?.click()
}

async function onImportFile(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  const ext = file.name.includes('.') ? file.name.split('.').pop()!.toLowerCase() : ''
  if (!['md', 'markdown', 'txt'].includes(ext)) {
    ElMessage.warning('仅支持导入 .md / .markdown / .txt 文本，其他格式请先另存为 Markdown')
    return
  }
  if (file.size > FILE_SIZE_LIMIT) {
    ElMessage.warning('文件大小不能超过 50MB')
    return
  }
  importing.value = true
  try {
    const text = await file.text()
    let md = text

    // 1. 处理 [TOC]
    md = md.replace(/^\[TOC\]$/gim, '')

    // 2. 预处理块级公式 $$...$$
    const formulas: string[] = []
    md = md.replace(/\$\$([\s\S]+?)\$\$/g, (_, formula) => {
      formulas.push(formula.trim())
      return `<!--WIKI_FORMULA_${formulas.length - 1}-->`
    })

    // 3. 预处理脚注
    const footnotes: Record<string, string> = {}
    md = md.replace(/^\[\^(\w+)\]:\s*(.+?)(?=^\[\^|\n#{1,6}\s|$)/gms, (match, id, content) => {
      footnotes[id] = content.trim()
      return ''
    })
    let footnoteCount = 0
    const refMap: Record<string, number> = {}
    md = md.replace(/\[\^(\w+)\]/g, (match, id) => {
      if (!(id in refMap)) refMap[id] = ++footnoteCount
      const num = refMap[id]
      return `<sup class="footnote-ref"><a href="#fn-${id}" id="fnref-${id}">${num}</a></sup>`
    })

    // 4. marked 解析
    let html = (await marked.parse(md)) as string

    // 5. 恢复公式为 KaTeX 渲染
    formulas.forEach((formula, idx) => {
      try {
        const rendered = katex.renderToString(formula, { throwOnError: false, displayMode: true })
        html = html.replace(`<!--WIKI_FORMULA_${idx}-->`, rendered)
      } catch {
        html = html.replace(`<!--WIKI_FORMULA_${idx}-->`, `<pre><code>$$${formula}$$</code></pre>`)
      }
    })

    // 6. 追加脚注列表
    if (footnoteCount > 0) {
      const list = Object.entries(refMap)
        .sort((a, b) => a[1] - b[1])
        .map(([id, num]) => `<li id="fn-${id}"><a href="#fnref-${id}">^${num}</a> ${footnotes[id] || ''}</li>`)
        .join('')
      html += `\n<div class="footnotes"><hr><ol>${list}</ol></div>`
    }

    importContent.value = html
    importForm.title = file.name.replace(/\.(md|markdown|txt)$/i, '')
    importForm.parentId = current.value?.id ?? 0
    importDialogVisible.value = true
  } catch {
    ElMessage.error('文件读取失败，请确认文件为 UTF-8 编码')
  } finally {
    importing.value = false
  }
}

async function confirmImport() {
  if (!importForm.title.trim()) {
    ElMessage.warning('请填写文档标题')
    return
  }
  importing.value = true
  try {
    const payload: WikiDocForm = {
      title: importForm.title.trim(),
      content: importContent.value || '',
      parentId: importForm.parentId ?? 0
    }
    const res = await createWikiDoc(payload)
    ElMessage.success('文档导入成功')
    importDialogVisible.value = false
    editing.value = false
    await loadTree()
    await loadDetail(res.data)
  } finally {
    importing.value = false
  }
}

/* ---------------- 阅读模式：目录大纲 + 代码高亮 ---------------- */

interface TocItem {
  id: number
  level: number
  text: string
}

const mainRef = ref<HTMLElement>()
const contentRef = ref<HTMLElement>()
const tocItems = ref<TocItem[]>([])
const activeHeading = ref(-1)

function buildToc() {
  const el = contentRef.value
  if (!el) {
    tocItems.value = []
    activeHeading.value = -1
    return
  }
  const headings = Array.from(el.querySelectorAll('h1, h2, h3, h4, h5, h6'))
  tocItems.value = headings.map((h, i) => {
    h.id = `wiki-h-${i}`
    h.setAttribute('data-toc-id', String(i))
    return { id: i, level: Number(h.tagName.charAt(1)), text: h.textContent?.trim() || '' }
  })
  activeHeading.value = tocItems.value.length ? 0 : -1
}

function scrollToHeading(item: TocItem) {
  const el = contentRef.value?.querySelector(`#wiki-h-${item.id}`)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
    activeHeading.value = item.id
  }
}

function onViewerScroll() {
  if (!tocItems.value.length || !contentRef.value) return
  const headings = Array.from(contentRef.value.querySelectorAll<HTMLElement>('[data-toc-id]'))
  if (!headings.length) return
  const baseTop = contentRef.value.getBoundingClientRect().top
  let active = 0
  for (const h of headings) {
    if (h.getBoundingClientRect().top - baseTop <= 80) {
      active = Number(h.dataset.tocId)
    }
  }
  activeHeading.value = active
}

function highlightCodes() {
  contentRef.value?.querySelectorAll('pre code').forEach((el) => {
    const codeEl = el as HTMLElement
    if (codeEl.dataset.highlighted) return
    try {
      hljs.highlightElement(codeEl)
      codeEl.dataset.highlighted = 'yes'
    } catch {
      // 忽略无法高亮的代码块
    }
  })
}

// 内容变化后：构建大纲 + 高亮代码
watch(safeContent, () => {
  nextTick(() => {
    buildToc()
    highlightCodes()
  })
})

/* ---------------- 目录树 ---------------- */

async function loadTree() {
  loadingTree.value = true
  try {
    const res = await getWikiTree()
    treeData.value = res.data || []
  } finally {
    loadingTree.value = false
  }
}

function filterTree() {
  // 简单过滤：命中保留节点，否则隐藏
  const kw = keyword.value.trim()
  if (!kw) {
    loadTree()
    return
  }
  const walk = (nodes: WikiDocNode[]): WikiDocNode[] =>
    nodes
      .map((n) => ({ ...n, children: walk(n.children || []) }))
      .filter((n) => n.title.includes(kw) || (n.children && n.children.length > 0))
  treeData.value = walk(treeData.value)
}

async function handleNodeClick(data: WikiDocNode) {
  editing.value = false
  await loadDetail(data.id)
}

async function loadDetail(id: number) {
  const res = await getWikiDetail(id)
  current.value = res.data
}

/* ---------------- 新建/编辑 ---------------- */

function handleCreateRoot() {
  startCreate(0)
}

function handleCreateChild() {
  if (current.value) {
    startCreate(current.value.id)
  }
}

function startCreate(parentId: number) {
  editing.value = true
  current.value = null
  form.id = undefined
  form.title = ''
  form.content = ''
  form.parentId = parentId
  // 编辑器内容置空
  if (editorRef.value) {
    editorRef.value.clear()
  }
}

function startEdit() {
  if (!current.value) return
  editing.value = true
  form.id = current.value.id
  form.title = current.value.title
  form.content = current.value.content || ''
  form.parentId = current.value.parentId
  if (editorRef.value) {
    editorRef.value.setHtml(form.content)
  }
}

function cancelEdit() {
  editing.value = false
  if (form.id) {
    loadDetail(form.id)
  }
}

async function saveDoc() {
  if (!form.title.trim()) {
    ElMessage.warning('请填写文档标题')
    return
  }
  saving.value = true
  try {
    const payload: WikiDocForm = {
      title: form.title.trim(),
      content: form.content || '',
      parentId: form.parentId ?? 0
    }
    if (form.id) {
      await updateWikiDoc(form.id, payload)
      ElMessage.success('文档已更新')
      editing.value = false
      await loadTree()
      await loadDetail(form.id)
    } else {
      const res = await createWikiDoc(payload)
      ElMessage.success('文档创建成功')
      editing.value = false
      await loadTree()
      await loadDetail(res.data)
    }
  } finally {
    saving.value = false
  }
}

/* ---------------- 删除 ---------------- */

async function handleDelete() {
  if (!current.value) return
  const node = current.value
  const tip = node.childCount > 0 ? `将同时删除 ${node.childCount} 个子文档及全部后代文档，且无法恢复！` : '删除后无法恢复！'
  try {
    await ElMessageBox.confirm(`确定删除文档「${node.title}」吗？${tip}`, '删除确认', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await deleteWikiDoc(node.id)
  ElMessage.success('文档已删除')
  current.value = null
  editing.value = false
  await loadTree()
}

/* ---------------- 编辑器 ---------------- */

const editorRef = shallowRef<IDomEditor>()
const toolbarConfig: Partial<IToolbarConfig> = {
  // 默认工具栏已涵盖：标题/字体字号/加粗/斜体/下划线/删除线/上标/下标/行内代码/颜色/背景色/
  // 行高/缩进/对齐/引用/有序无序列表/任务列表/表格/链接/图片/分割线/代码块/清除格式/撤销重做/全屏
  excludeKeys: ['group-video', 'insertVideo'],
  insertKeys: {
    index: 25,
    keys: ['insertFormula']
  }
}
const editorConfig: Partial<IEditorConfig> = {
  placeholder: '请输入文档内容…',
  MENU_CONF: {
    // 图片 2MB 以内以 base64 内嵌，避免额外上传接口
    uploadImage: {
      base64LimitSize: 2 * 1024 * 1024
    }
  }
}

function handleEditorCreated(editor: IDomEditor) {
  editorRef.value = editor
}

onBeforeUnmount(() => {
  editorRef.value?.destroy()
})

/* ---------------- 工具 ---------------- */

function formatTime(v?: string): string {
  if (!v) return ''
  const d = new Date(v)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/* ---------------- 初始化 ---------------- */

loadTree()
</script>

<style scoped>
.wiki-page {
  height: calc(100vh - 84px);
}
.wiki-container {
  height: 100%;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}
.wiki-tree-panel {
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--el-border-color-lighter);
  background: #fafafa;
}
.wiki-tree-header {
  padding: 12px 12px 4px;
}
.wiki-tree-actions {
  padding: 8px 12px 0;
  display: flex;
  gap: 8px;
}
.wiki-tree-body {
  flex: 1;
  overflow: auto;
  padding: 8px 6px 16px;
}
.wiki-tree-node {
  display: flex;
  align-items: center;
  gap: 4px;
  overflow: hidden;
}
.tree-folder-icon {
  color: #e6a23c;
}
.tree-doc-icon {
  color: #909399;
}
.tree-label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.wiki-content-panel {
  padding: 0;
  background: #fff;
}
.wiki-empty {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
}
.wiki-viewer {
  padding: 24px 32px;
  max-width: 1100px;
}
.wiki-viewer-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}
.wiki-viewer-title {
  display: flex;
  align-items: center;
  gap: 10px;
}
.wiki-viewer-title h2 {
  margin: 0;
  font-size: 22px;
  color: var(--el-text-color-primary);
}
.wiki-viewer-actions {
  flex-shrink: 0;
}
.wiki-viewer-meta {
  display: flex;
  gap: 16px;
  margin-top: 10px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.wiki-viewer-body {
  display: flex;
  gap: 28px;
  align-items: flex-start;
}
.wiki-viewer-main {
  flex: 1;
  min-width: 0;
}
.wiki-viewer-content {
  line-height: 1.8;
  color: var(--el-text-color-primary);
  word-break: break-word;
}
.wiki-viewer-content :deep(img) {
  max-width: 100%;
}
.wiki-viewer-content :deep(h1),
.wiki-viewer-content :deep(h2),
.wiki-viewer-content :deep(h3),
.wiki-viewer-content :deep(h4),
.wiki-viewer-content :deep(h5),
.wiki-viewer-content :deep(h6) {
  scroll-margin-top: 16px;
}
.wiki-viewer-content :deep(blockquote) {
  margin: 12px 0;
  padding: 8px 16px;
  border-left: 4px solid var(--el-color-primary-light-5);
  background: var(--el-color-primary-light-9);
  color: var(--el-text-color-secondary);
  border-radius: 0 4px 4px 0;
}
.wiki-viewer-content :deep(code) {
  background: #f2f3f5;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
  font-family: Consolas, Monaco, 'Courier New', monospace;
}
.wiki-viewer-content :deep(pre) {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 6px;
  overflow: auto;
}
.wiki-viewer-content :deep(pre code) {
  background: transparent;
  padding: 0;
}
.wiki-viewer-content :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 12px 0;
  font-size: 14px;
}
.wiki-viewer-content :deep(th),
.wiki-viewer-content :deep(td) {
  border: 1px solid var(--el-border-color-lighter);
  padding: 8px 12px;
  text-align: left;
}
.wiki-viewer-content :deep(th) {
  background: #fafafa;
  font-weight: 600;
}
.wiki-viewer-content :deep(ul),
.wiki-viewer-content :deep(ol) {
  padding-left: 24px;
}
.wiki-viewer-content :deep(.task-list-item) {
  list-style: none;
  margin-left: -20px;
}
.wiki-viewer-content :deep(input[type='checkbox']) {
  margin-right: 6px;
  accent-color: var(--el-color-primary);
}
.wiki-viewer-content :deep(hr) {
  border: none;
  border-top: 1px solid var(--el-border-color);
  margin: 20px 0;
}
.wiki-viewer-content :deep(.footnote-ref) {
  font-size: 12px;
  margin-left: 2px;
}
.wiki-viewer-content :deep(.footnote-ref a) {
  color: var(--el-color-primary);
  text-decoration: none;
}
.wiki-viewer-content :deep(.footnotes) {
  margin-top: 20px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.wiki-viewer-content :deep(.footnotes ol) {
  padding-left: 20px;
}
.wiki-viewer-content :deep(.footnotes li) {
  margin: 4px 0;
}
.wiki-viewer-content :deep(.footnotes li a) {
  color: var(--el-color-primary);
  text-decoration: none;
}
.wiki-viewer-content :deep(.katex) {
  font-size: 1.05em;
}
.wiki-viewer-content :deep(.katex-display) {
  margin: 16px 0;
  overflow-x: auto;
}

/* 目录大纲 */
.wiki-toc {
  width: 220px;
  flex-shrink: 0;
  position: sticky;
  top: 0;
  max-height: calc(100vh - 200px);
  overflow: auto;
  border-left: 1px solid var(--el-border-color-lighter);
  padding: 4px 0 12px 14px;
}
.toc-title {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-regular);
  padding: 8px 0;
}
.toc-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.toc-item {
  font-size: 13px;
  line-height: 1.7;
  color: var(--el-text-color-secondary);
  cursor: pointer;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  border-left: 2px solid transparent;
  padding-left: 8px;
  margin-left: 0;
  transition: color 0.2s;
}
.toc-item:hover {
  color: var(--el-color-primary);
}
.toc-item.active {
  color: var(--el-color-primary);
  border-left-color: var(--el-color-primary);
  font-weight: 600;
}
.toc-lv-2 {
  padding-left: 20px;
}
.toc-lv-3 {
  padding-left: 32px;
}
.toc-lv-4 {
  padding-left: 44px;
}
.toc-lv-5 {
  padding-left: 56px;
}
.toc-lv-6 {
  padding-left: 68px;
}

.wiki-editor {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.wiki-editor-title {
  padding: 12px 16px 8px;
}
.wiki-editor-toolbar {
  border-bottom: 1px solid var(--el-border-color-lighter);
  padding: 0 16px;
}
.wiki-editor-body {
  flex: 1;
  overflow: hidden;
  padding: 0 16px;
}
.wiki-editor-body :deep(.w-e-text-container) {
  height: 100% !important;
}
.wiki-editor-footer {
  padding: 12px 16px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.import-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
  line-height: 1.5;
}
</style>
