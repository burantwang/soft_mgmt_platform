<template>
  <div class="files-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="filter-card">
      <div class="filter-bar">
        <el-input
          v-model="query.keyword"
          placeholder="按文件名搜索"
          clearable
          style="width: 240px"
          :prefix-icon="Search"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-select v-model="query.fileType" placeholder="文件类型" clearable style="width: 160px">
          <el-option v-for="opt in FILE_TYPE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        <div class="spacer"></div>
        <el-button v-if="canEdit" type="primary" :icon="Upload" @click="chooseFile">上传文件</el-button>
        <input ref="fileInputRef" type="file" hidden @change="onFileChange" />
      </div>
    </el-card>

    <!-- 文件列表 -->
    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column label="文件名" min-width="260">
          <template #default="{ row }">
            <div class="file-cell">
              <span class="ext-badge" :class="extClass(row.fileExt)">{{ (row.fileExt || 'file').toUpperCase() }}</span>
              <span class="file-name" :title="row.fileName">{{ row.fileName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="fileTypeDesc" label="类型" width="100" align="center" />
        <el-table-column prop="sizeDesc" label="大小" width="100" align="right" />
        <el-table-column prop="uploaderName" label="上传人" width="110" align="center" />
        <el-table-column prop="downloadCount" label="下载" width="80" align="center" />
        <el-table-column label="关联文档" min-width="140">
          <template #default="{ row }">
            <el-tag v-if="row.docTitle" size="small" type="info">{{ row.docTitle }}</el-tag>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="上传时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Download" @click="handleDownload(toFile(row))">下载</el-button>
            <el-button v-if="canEdit" link type="primary" :icon="Edit" @click="openRename(toFile(row))">重命名</el-button>
            <el-button v-if="canEdit" link type="danger" :icon="Delete" @click="handleDelete(toFile(row))">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无文件，点击右上角上传" />
        </template>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- 重命名弹窗 -->
    <el-dialog v-model="renameVisible" title="重命名文件" width="420px" :close-on-click-modal="false">
      <el-form label-width="70px">
        <el-form-item label="新文件名">
          <el-input v-model="renameForm.newName" placeholder="仅可修改主名，扩展名保持不变" maxlength="255" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="renameVisible = false">取消</el-button>
        <el-button type="primary" :loading="renaming" @click="submitRename">确定</el-button>
      </template>
    </el-dialog>

    <!-- 上传进度 -->
    <el-dialog v-model="uploadVisible" title="上传中" width="420px" :close-on-click-modal="false" :show-close="false">
      <el-progress :percentage="uploadPercent" :status="uploadPercent >= 100 ? 'success' : undefined" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Download, Edit, Refresh, Search, Upload } from '@element-plus/icons-vue'
import { getPerms } from '@/utils/auth'
import { deleteFile, downloadFile, getFileList, renameFile, uploadFile } from '@/api/wiki'
import { FILE_TYPE_OPTIONS } from '@/types/wiki'
import type { FileResourceItem } from '@/types/wiki'
import { ATTACHMENT_ALLOW_EXT, FILE_SIZE_LIMIT } from '@/utils/constants'

const canEdit = getPerms().includes('wiki:edit')

const loading = ref(false)
const list = ref<FileResourceItem[]>([])
const total = ref(0)
const query = reactive<{ page: number; size: number; keyword?: string; fileType?: number }>({
  page: 1,
  size: 10,
  keyword: undefined,
  fileType: undefined
})

/* ---------------- 列表 ---------------- */

async function loadData() {
  loading.value = true
  try {
    const res = await getFileList(query)
    list.value = res.data.records as FileResourceItem[]
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.page = 1
  loadData()
}

function handleReset() {
  query.keyword = undefined
  query.fileType = undefined
  query.page = 1
  loadData()
}

/* ---------------- 上传 ---------------- */

const fileInputRef = ref<HTMLInputElement>()
const uploadVisible = ref(false)
const uploadPercent = ref(0)

function chooseFile() {
  fileInputRef.value?.click()
}

async function onFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  // 前端校验
  const ext = file.name.includes('.') ? file.name.split('.').pop()!.toLowerCase() : ''
  if (!(ATTACHMENT_ALLOW_EXT as readonly string[]).includes(ext)) {
    ElMessage.warning(`不支持的文件类型 .${ext || '(无后缀)'}`)
    return
  }
  if (file.size > FILE_SIZE_LIMIT) {
    ElMessage.warning('文件大小不能超过 50MB')
    return
  }
  uploadVisible.value = true
  uploadPercent.value = 0
  try {
    await uploadFile(file, undefined, (p) => {
      uploadPercent.value = p
    })
    ElMessage.success('上传成功')
    query.page = 1
    await loadData()
  } catch {
    // 拦截器已提示
  } finally {
    uploadVisible.value = false
    uploadPercent.value = 0
  }
}

/** 将 el-table 插槽的 DefaultRow 转换为强类型文件对象 */
function toFile(row: any): FileResourceItem {
  return row as FileResourceItem
}

/* ---------------- 下载 ---------------- */

async function handleDownload(row: FileResourceItem) {
  try {
    await downloadFile(row.id, row.fileName)
  } catch {
    // 拦截器已提示
  }
}

/* ---------------- 重命名 ---------------- */

const renameVisible = ref(false)
const renaming = ref(false)
const renameForm = reactive<{ fileId: number; newName: string }>({ fileId: 0, newName: '' })

function openRename(row: FileResourceItem) {
  renameForm.fileId = row.id
  renameForm.newName = row.fileName.replace(/\.([^.]+)$/, '')
  renameVisible.value = true
}

async function submitRename() {
  if (!renameForm.newName.trim()) {
    ElMessage.warning('请输入新文件名')
    return
  }
  renaming.value = true
  try {
    await renameFile({ fileId: renameForm.fileId, newName: renameForm.newName.trim() })
    ElMessage.success('重命名成功')
    renameVisible.value = false
    await loadData()
  } finally {
    renaming.value = false
  }
}

/* ---------------- 删除 ---------------- */

async function handleDelete(row: FileResourceItem) {
  try {
    await ElMessageBox.confirm(`确定删除文件「${row.fileName}」吗？物理文件将一并清理，无法恢复！`, '删除确认', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await deleteFile(row.id)
  ElMessage.success('文件已删除')
  if (list.value.length === 1 && query.page > 1) {
    query.page -= 1
  }
  await loadData()
}

/* ---------------- 工具 ---------------- */

function formatTime(v?: string): string {
  if (!v) return ''
  const d = new Date(v)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function extClass(ext?: string): string {
  const e = (ext || '').toLowerCase()
  if (['png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp'].includes(e)) return 'ext-image'
  if (['zip', 'rar', '7z', 'tar', 'gz'].includes(e)) return 'ext-archive'
  if (['pdf'].includes(e)) return 'ext-pdf'
  if (['doc', 'docx'].includes(e)) return 'ext-word'
  if (['xls', 'xlsx'].includes(e)) return 'ext-excel'
  if (['ppt', 'pptx'].includes(e)) return 'ext-ppt'
  if (['txt', 'md', 'log', 'sql', 'xml', 'json', 'yml', 'yaml', 'ini', 'conf', 'properties', 'csv'].includes(e)) return 'ext-code'
  return 'ext-default'
}

onMounted(loadData)
</script>

<style scoped>
.files-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.filter-card :deep(.el-card__body) {
  padding: 14px 16px;
}
.filter-bar {
  display: flex;
  align-items: center;
  gap: 10px;
}
.spacer {
  flex: 1;
}
.table-card :deep(.el-card__body) {
  padding: 8px 16px 16px;
}
.file-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
.ext-badge {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 40px;
  height: 20px;
  padding: 0 5px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
  color: #fff;
  background: #909399;
}
.ext-image {
  background: #67c23a;
}
.ext-archive {
  background: #e6a23c;
}
.ext-pdf {
  background: #f56c6c;
}
.ext-word {
  background: #409eff;
}
.ext-excel {
  background: #2f9e44;
}
.ext-ppt {
  background: #e8590c;
}
.ext-code {
  background: #7048e8;
}
.file-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.muted {
  color: var(--el-text-color-placeholder);
}
.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}
</style>
