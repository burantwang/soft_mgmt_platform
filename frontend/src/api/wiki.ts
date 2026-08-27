/**
 * Wiki 知识库 + 文件资源 API（阶段5）
 */
import { http } from '@/utils/request'
import type { ApiResult, PageResult } from '@/types/api'
import type {
  WikiDocNode,
  WikiDocDetail,
  WikiDocForm,
  FileResourceItem,
  FileRenameForm,
} from '@/types/wiki'

/* ==================== Wiki 文档 ==================== */

/** 目录树 */
export function getWikiTree(): Promise<ApiResult<WikiDocNode[]>> {
  return http.get<WikiDocNode[]>('/wiki/docs/tree')
}

/** 文档详情 */
export function getWikiDetail(id: number): Promise<ApiResult<WikiDocDetail>> {
  return http.get<WikiDocDetail>(`/wiki/docs/${id}`)
}

/** 新建文档 */
export function createWikiDoc(data: WikiDocForm): Promise<ApiResult<number>> {
  return http.post<number>('/wiki/docs', data)
}

/** 更新文档 */
export function updateWikiDoc(id: number, data: WikiDocForm): Promise<ApiResult<void>> {
  return http.put<void>(`/wiki/docs/${id}`, data)
}

/** 删除文档（级联删除子文档） */
export function deleteWikiDoc(id: number): Promise<ApiResult<void>> {
  return http.del<void>(`/wiki/docs/${id}`)
}

/* ==================== 文件资源 ==================== */

export interface FileQuery {
  page?: number
  size?: number
  keyword?: string
  fileType?: number
  /** 按关联文档过滤 */
  docId?: number
}

/** 分页查询文件资源 */
export function getFileList(params: FileQuery): Promise<ApiResult<PageResult<FileResourceItem>>> {
  return http.get<PageResult<FileResourceItem>>('/wiki/files', params)
}

/** 上传普通附件（返回文件记录） */
export function uploadFile(
  file: File,
  docId?: number,
  onProgress?: (percent: number) => void
): Promise<ApiResult<FileResourceItem>> {
  const formData = new FormData()
  formData.append('file', file)
  if (docId) {
    formData.append('docId', String(docId))
  }
  return http.post<FileResourceItem>('/wiki/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: (e: { loaded?: number; total?: number }) => {
      if (e.total && onProgress) {
        onProgress(Math.round(((e.loaded || 0) / e.total) * 100))
      }
    }
  })
}

/** 重命名 */
export function renameFile(data: FileRenameForm): Promise<ApiResult<void>> {
  return http.put<void>('/wiki/files/rename', data)
}

/** 下载文件（blob 方式，走统一鉴权拦截器） */
export async function downloadFile(id: number, fileName: string): Promise<void> {
  const res = (await http.get<unknown>('/wiki/files/' + id + '/download', undefined, {
    responseType: 'blob',
    timeout: 60000
  })) as unknown as Blob
  const url = URL.createObjectURL(res)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

/** 删除文件 */
export function deleteFile(id: number): Promise<ApiResult<void>> {
  return http.del<void>(`/wiki/files/${id}`)
}
