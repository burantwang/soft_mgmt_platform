/**
 * Wiki 知识库 + 文件资源类型定义（阶段5）
 * 注意：类型与常量放 .ts 而非 .d.ts，因为 FILE_TYPE_OPTIONS 需在运行时被组件 import。
 */
import type { PageResult } from './api'

/** 目录树节点 */
export interface WikiDocNode {
  id: number
  title: string
  parentId: number
  sort: number
  /** 是否含正文内容 */
  hasContent: boolean
  editorName?: string
  updateTime?: string
  children: WikiDocNode[]
}

/** 文档详情 */
export interface WikiDocDetail extends WikiDocNode {
  content: string
  creatorName?: string
  createTime?: string
  /** 子文档数量 */
  childCount: number
}

/** 文档创建/更新入参 */
export interface WikiDocForm {
  title: string
  content: string
  parentId: number
}

/** 文件资源列表项 */
export interface FileResourceItem {
  id: number
  fileName: string
  fileExt?: string
  mimeType?: string
  fileSize?: number
  /** 可读大小描述 */
  sizeDesc?: string
  /** 类型:1测试报告 2普通附件 */
  fileType: number
  fileTypeDesc: string
  docId?: number
  docTitle?: string
  uploaderId?: number
  uploaderName?: string
  downloadCount: number
  createTime?: string
}

/** 文件分页返回 */
export type FilePageResult = PageResult<FileResourceItem>

/** 文件类型筛选选项 */
export const FILE_TYPE_OPTIONS = [
  { value: 1, label: '测试报告' },
  { value: 2, label: '普通附件' },
]

/** 文件重命名入参 */
export interface FileRenameForm {
  fileId: number
  newName: string
}
