<template>
  <div class="portal-page">
    <!-- ============ 展示视图 ============ -->
    <template v-if="mode === 'view'">
      <!-- 顶部工具条 -->
      <div class="portal-header">
        <div class="portal-title">
          <h2>系统门户</h2>
          <p>部门常用系统快速入口，按板块分类，点击即达</p>
        </div>
        <div class="portal-actions">
          <el-input
            v-model="keyword"
            placeholder="搜索系统名称 / 简介"
            clearable
            style="width: 240px"
            :prefix-icon="Search"
          />
          <el-button v-if="canEdit" type="primary" :icon="Setting" @click="enterManage">管理门户</el-button>
        </div>
      </div>

      <!-- 板块区 -->
      <div v-loading="loading" class="portal-body">
        <template v-if="filteredCategories.length">
          <section v-for="cat in filteredCategories" :key="cat.id" class="category-card" :style="cardStyle(cat.color)">
            <header class="category-header">
              <span class="category-icon" :style="{ background: tintColor(cat.color), color: cat.color }">
                <el-icon v-if="isEpIcon(cat.icon)" :size="20"><component :is="cat.icon" /></el-icon>
                <span v-else class="icon-emoji">{{ cat.icon || '📁' }}</span>
              </span>
              <div class="category-meta">
                <h3>{{ cat.categoryName }}</h3>
                <p v-if="cat.description">{{ cat.description }}</p>
              </div>
              <span class="category-count">{{ cat.links.length }} 个系统</span>
            </header>
            <!-- 表格展示板块（如 其他地址清单） -->
            <template v-if="cat.layout === 'table'">
              <div v-if="cat.links.length" class="address-table-wrap">
                <el-table :data="cat.links" style="width: 100%" class="address-table">
                  <el-table-column label="地址" min-width="240">
                    <template #default="{ row }">
                      <div class="addr-cell">
                        <span class="addr-code">{{ row.url }}</span>
                        <el-button link :icon="CopyDocument" class="cell-action" title="复制地址" @click="copyText(row.url)" />
                      </div>
                    </template>
                  </el-table-column>
                  <el-table-column label="地址说明" min-width="220" show-overflow-tooltip>
                    <template #default="{ row }">
                      <span class="addr-desc">{{ row.description || '-' }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column label="用户名密码" min-width="280">
                    <template #default="{ row }">
                      <div class="cred-cell">
                        <span class="cred-user">{{ row.username || '-' }}</span>
                        <span v-if="row.password" class="cred-sep">/</span>
                        <span v-if="row.password" class="cred-pass">{{ pwdVisible.has(row.id) ? row.password : '••••••••' }}</span>
                        <el-button
                          v-if="row.password"
                          link
                          :icon="pwdVisible.has(row.id) ? Hide : View"
                          class="cell-action"
                          :title="pwdVisible.has(row.id) ? '隐藏密码' : '显示密码'"
                          @click="togglePwd(row.id)"
                        />
                        <el-button
                          v-if="row.username || row.password"
                          link
                          :icon="CopyDocument"
                          class="cell-action"
                          title="复制账号信息"
                          @click="copyText(formatCredential(row as PortalLinkItem))"
                        />
                      </div>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
              <el-empty v-else description="该板块暂未配置地址" :image-size="60" />
            </template>
            <!-- 卡片展示板块（默认） -->
            <template v-else>
              <div v-if="cat.links.length" class="link-grid">
                <a
                  v-for="link in cat.links"
                  :key="link.id"
                  class="link-card"
                  :style="cardStyle(link.color)"
                  @click.prevent="openLink(link)"
                >
                  <span class="link-icon" :style="{ background: tintColor(link.color), color: link.color }">
                    <el-icon v-if="isEpIcon(link.icon)" :size="22"><component :is="link.icon" /></el-icon>
                    <span v-else class="icon-emoji">{{ link.icon || '🔗' }}</span>
                  </span>
                  <span class="link-info">
                    <span class="link-name">{{ link.linkName }}</span>
                    <span v-if="link.description" class="link-desc">{{ link.description }}</span>
                  </span>
                  <el-icon class="link-arrow" :size="14" :style="{ color: link.color }"><Right /></el-icon>
                </a>
              </div>
              <el-empty v-else description="该板块暂未配置系统" :image-size="60" />
            </template>
          </section>
        </template>
        <el-empty v-else-if="!loading" description="暂无板块，请点击右上角「管理门户」开始配置" />
      </div>
    </template>

    <!-- ============ 管理视图 ============ -->
    <template v-else>
      <div class="manage-header">
        <el-button :icon="ArrowLeft" @click="mode = 'view'">返回门户</el-button>
        <el-button type="primary" :icon="Refresh" @click="loadAll">刷新</el-button>
      </div>
      <el-tabs v-model="activeTab" class="manage-tabs">
        <!-- 板块管理 -->
        <el-tab-pane label="板块管理" name="category">
          <el-card shadow="never">
            <div class="table-toolbar">
              <el-button type="primary" :icon="Plus" @click="openCategoryDialog()">新增板块</el-button>
              <span class="text-muted">板块支持上移/下移调整展示顺序，停用后门户页不再展示</span>
            </div>
            <el-table :data="categories" border stripe>
              <el-table-column label="排序" width="120" align="center">
                <template #default="{ $index }">
                  <el-button link :icon="Top" :disabled="$index === 0" @click="moveCategory($index, -1)" />
                  <el-button link :icon="Bottom" :disabled="$index === categories.length - 1" @click="moveCategory($index, 1)" />
                </template>
              </el-table-column>
              <el-table-column label="板块" min-width="180">
                <template #default="{ row }">
                  <div class="cell-category">
                    <span class="category-icon small" :style="{ background: tintColor(row.color), color: row.color }">
                      <el-icon v-if="isEpIcon(row.icon)" :size="16"><component :is="row.icon" /></el-icon>
                      <span v-else class="icon-emoji">{{ row.icon || '📁' }}</span>
                    </span>
                    <div>
                      <div class="cell-name">{{ row.categoryName }}</div>
                      <div v-if="row.description" class="cell-sub">{{ row.description }}</div>
                    </div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="linkCount" label="系统数" width="80" align="center" />
              <el-table-column label="状态" width="90">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                    {{ row.status === 1 ? '启用' : '停用' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="210" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="openCategoryDialog(row as PortalCategory)">编辑</el-button>
                  <el-button link :type="row.status === 1 ? 'warning' : 'success'" @click="toggleCategory(row as PortalCategory)">
                    {{ row.status === 1 ? '停用' : '启用' }}
                  </el-button>
                  <el-button link type="danger" @click="removeCategory(row as PortalCategory)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-tab-pane>

        <!-- 链接管理 -->
        <el-tab-pane label="系统链接管理" name="link">
          <el-card shadow="never">
            <div class="table-toolbar link-toolbar">
              <el-select v-model="linkFilterCategory" placeholder="全部板块" clearable style="width: 200px">
                <el-option v-for="c in categories" :key="c.id" :label="c.categoryName" :value="c.id" />
              </el-select>
              <el-button type="primary" :icon="Plus" @click="openLinkDialog()">新增系统</el-button>
              <span class="text-muted">链接支持上移/下移调整展示顺序</span>
            </div>
            <el-table :data="filteredLinks" border stripe>
              <el-table-column label="排序" width="120" align="center">
                <template #default="{ $index }">
                  <el-button link :icon="Top" :disabled="$index === 0" @click="moveLink($index, -1)" />
                  <el-button link :icon="Bottom" :disabled="$index === filteredLinks.length - 1" @click="moveLink($index, 1)" />
                </template>
              </el-table-column>
              <el-table-column label="系统名称" min-width="160">
                <template #default="{ row }">
                  <div class="cell-category">
                    <span class="category-icon small" :style="{ background: tintColor(row.color), color: row.color }">
                      <el-icon v-if="isEpIcon(row.icon)" :size="16"><component :is="row.icon" /></el-icon>
                      <span v-else class="icon-emoji">{{ row.icon || '🔗' }}</span>
                    </span>
                    <span class="cell-name">{{ row.linkName }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="访问地址" min-width="200" show-overflow-tooltip>
                <template #default="{ row }">
                  <span class="cell-url">{{ row.url }}</span>
                </template>
              </el-table-column>
              <el-table-column label="所属板块" width="130">
                <template #default="{ row }">{{ categoryName(row.categoryId) }}</template>
              </el-table-column>
              <el-table-column label="状态" width="90">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                    {{ row.status === 1 ? '启用' : '停用' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="210" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="openLinkDialog(row as PortalLinkItem)">编辑</el-button>
                  <el-button link :type="row.status === 1 ? 'warning' : 'success'" @click="toggleLink(row as PortalLinkItem)">
                    {{ row.status === 1 ? '停用' : '启用' }}
                  </el-button>
                  <el-button link type="danger" @click="removeLink(row as PortalLinkItem)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-tab-pane>
      </el-tabs>
    </template>

    <!-- ============ 板块编辑弹窗 ============ -->
    <el-dialog
      v-model="categoryDialog.visible"
      :title="categoryDialog.form.id ? '编辑板块' : '新增板块'"
      width="520px"
      destroy-on-close
    >
      <el-form ref="categoryFormRef" :model="categoryDialog.form" :rules="categoryRules" label-width="90px">
        <el-form-item label="板块名称" prop="categoryName">
          <el-input v-model="categoryDialog.form.categoryName" placeholder="如 研发相关 / 测试相关" maxlength="64" show-word-limit />
        </el-form-item>
        <el-form-item label="板块图标" prop="icon">
          <el-select
            v-model="categoryDialog.form.icon"
            filterable
            allow-create
            default-first-option
            clearable
            placeholder="选择或输入图标 / emoji"
            style="width: 100%"
          >
            <el-option v-for="name in PORTAL_ICON_OPTIONS" :key="name" :label="name" :value="name">
              <span class="icon-option"><el-icon><component :is="name" /></el-icon> {{ name }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="主题色" prop="color">
          <el-color-picker v-model="categoryDialog.form.color" :predefine="PORTAL_COLOR_OPTIONS" />
          <span class="text-muted" style="margin-left: 10px">用于板块图标与标题点缀</span>
        </el-form-item>
        <el-form-item label="板块描述" prop="description">
          <el-input v-model="categoryDialog.form.description" type="textarea" :rows="2" placeholder="板块用途说明（可选）" maxlength="255" show-word-limit />
        </el-form-item>
        <el-form-item label="展示方式" prop="layout">
          <el-radio-group v-model="categoryDialog.form.layout">
            <el-radio value="card">卡片展示（图标方块）</el-radio>
            <el-radio value="table">表格展示（地址清单）</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="categoryDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCategory">确定</el-button>
      </template>
    </el-dialog>

    <!-- ============ 链接编辑弹窗 ============ -->
    <el-dialog
      v-model="linkDialog.visible"
      :title="linkDialog.form.id ? '编辑系统' : '新增系统'"
      width="540px"
      destroy-on-close
    >
      <el-form ref="linkFormRef" :model="linkDialog.form" :rules="linkRules" label-width="90px">
        <el-form-item label="所属板块" prop="categoryId">
          <el-select v-model="linkDialog.form.categoryId" placeholder="选择板块" style="width: 100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.categoryName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="系统名称" prop="linkName">
          <el-input v-model="linkDialog.form.linkName" placeholder="如 GitLab / Jenkins" maxlength="64" show-word-limit />
        </el-form-item>
        <el-form-item label="访问地址" prop="url">
          <el-input v-model="linkDialog.form.url" placeholder="https://、站内路径 /wiki、或 IP:端口 如 192.168.1.100:22" maxlength="500" />
        </el-form-item>
        <el-form-item label="系统简介">
          <el-input v-model="linkDialog.form.description" type="textarea" :rows="2" placeholder="系统用途说明（可选）" maxlength="255" show-word-limit />
        </el-form-item>
        <el-form-item label="登录用户名">
          <el-input v-model="linkDialog.form.username" placeholder="表格板块展示，如 root（可选）" maxlength="128" />
        </el-form-item>
        <el-form-item label="登录密码">
          <el-input v-model="linkDialog.form.password" type="password" show-password placeholder="表格板块展示，默认掩码（可选）" maxlength="255" />
        </el-form-item>
        <el-form-item label="系统图标">
          <el-select
            v-model="linkDialog.form.icon"
            filterable
            allow-create
            default-first-option
            clearable
            placeholder="选择或输入图标 / emoji"
            style="width: 100%"
          >
            <el-option v-for="name in PORTAL_ICON_OPTIONS" :key="name" :label="name" :value="name">
              <span class="icon-option"><el-icon><component :is="name" /></el-icon> {{ name }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="主题色">
          <el-color-picker v-model="linkDialog.form.color" :predefine="PORTAL_COLOR_OPTIONS" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="linkDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitLink">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Search, Setting, ArrowLeft, Refresh, Plus, Top, Bottom, Right, View, Hide, CopyDocument } from '@element-plus/icons-vue'
import * as ElementPlusIcons from '@element-plus/icons-vue'
import { getPerms } from '@/utils/auth'
import {
  createPortalCategory,
  createPortalLink,
  deletePortalCategory,
  deletePortalLink,
  getPortalCategories,
  getPortalOverview,
  sortPortalCategories,
  sortPortalLinks,
  togglePortalCategoryStatus,
  togglePortalLinkStatus,
  updatePortalCategory,
  updatePortalLink
} from '@/api/portal'
import {
  PORTAL_ICON_OPTIONS,
  PORTAL_COLOR_OPTIONS,
  type PortalCategory,
  type PortalLinkForm,
  type PortalLinkItem
} from '@/types/portal'

const router = useRouter()
const canEdit = getPerms().includes('portal:edit')

/** 视图模式：view 展示 / manage 管理 */
const mode = ref<'view' | 'manage'>('view')
const activeTab = ref('category')
const loading = ref(false)

/* ---------------- 展示数据 ---------------- */
const overview = ref<PortalCategory[]>([])
const keyword = ref('')

const filteredCategories = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return overview.value
  return overview.value
    .map((cat) => ({
      ...cat,
      links: cat.links.filter(
        (l) => l.linkName.toLowerCase().includes(kw) || (l.description || '').toLowerCase().includes(kw)
      )
    }))
    .filter((cat) => cat.links.length > 0)
})

async function loadOverview() {
  loading.value = true
  try {
    const res = await getPortalOverview()
    overview.value = res.data
  } finally {
    loading.value = false
  }
}

/* ---------------- 管理数据 ---------------- */
const categories = ref<PortalCategory[]>([])
const linkFilterCategory = ref<number | undefined>(undefined)

const allLinks = computed<PortalLinkItem[]>(() => categories.value.flatMap((c) => c.links))
const filteredLinks = computed(() => {
  if (!linkFilterCategory.value) return allLinks.value
  return allLinks.value.filter((l) => l.categoryId === linkFilterCategory.value)
})

function categoryName(id: number): string {
  return categories.value.find((c) => c.id === id)?.categoryName || '-'
}

async function loadAll() {
  const res = await getPortalCategories()
  categories.value = res.data
}

function enterManage() {
  mode.value = 'manage'
  activeTab.value = 'category'
  loadAll()
}

/* ---------------- 板块：排序 / 启停 / 删除 ---------------- */
async function moveCategory(index: number, dir: -1 | 1) {
  const target = index + dir
  if (target < 0 || target >= categories.value.length) return
  const arr = categories.value
  ;[arr[index], arr[target]] = [arr[target], arr[index]]
  arr.forEach((c, i) => (c.sort = i))
  await sortPortalCategories(arr.map((c) => ({ id: c.id, sort: c.sort })))
  loadAll()
}

async function toggleCategory(row: PortalCategory) {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '启用' : '停用'
  await ElMessageBox.confirm(`确认${action}板块「${row.categoryName}」？`, `${action}确认`, { type: 'warning' })
  await togglePortalCategoryStatus({ id: row.id, status: newStatus })
  ElMessage.success(`${action}成功`)
  loadAll()
}

async function removeCategory(row: PortalCategory) {
  await ElMessageBox.confirm(`确认删除板块「${row.categoryName}」？板块下存在系统链接时将无法删除。`, '删除确认', { type: 'warning' })
  await deletePortalCategory(row.id)
  ElMessage.success('删除成功')
  loadAll()
}

/* ---------------- 链接：排序 / 启停 / 删除 ---------------- */
async function moveLink(index: number, dir: -1 | 1) {
  const target = index + dir
  if (target < 0 || target >= filteredLinks.value.length) return
  const arr = filteredLinks.value
  ;[arr[index], arr[target]] = [arr[target], arr[index]]
  arr.forEach((l, i) => (l.sort = i))
  await sortPortalLinks(arr.map((l) => ({ id: l.id, sort: l.sort })))
  loadAll()
}

async function toggleLink(row: PortalLinkItem) {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '启用' : '停用'
  await ElMessageBox.confirm(`确认${action}系统「${row.linkName}」？`, `${action}确认`, { type: 'warning' })
  await togglePortalLinkStatus({ id: row.id, status: newStatus })
  ElMessage.success(`${action}成功`)
  loadAll()
}

async function removeLink(row: PortalLinkItem) {
  await ElMessageBox.confirm(`确认删除系统「${row.linkName}」？`, '删除确认', { type: 'warning' })
  await deletePortalLink(row.id)
  ElMessage.success('删除成功')
  loadAll()
}

/* ---------------- 板块表单 ---------------- */
const submitting = ref(false)
const categoryFormRef = ref<FormInstance>()
const categoryDialog = reactive({
  visible: false,
  form: {
    id: undefined as number | undefined,
    categoryName: '',
    icon: 'Menu',
    color: '#409EFF',
    description: '',
    layout: 'card'
  }
})
const categoryRules = {
  categoryName: [{ required: true, message: '请输入板块名称', trigger: 'blur' }]
}

function openCategoryDialog(row?: PortalCategory) {
  categoryDialog.form = row
    ? {
        id: row.id,
        categoryName: row.categoryName,
        icon: row.icon || 'Menu',
        color: row.color || '#409EFF',
        description: row.description || '',
        layout: row.layout || 'card'
      }
    : { id: undefined, categoryName: '', icon: 'Menu', color: '#409EFF', description: '', layout: 'card' }
  categoryDialog.visible = true
}

async function submitCategory() {
  await categoryFormRef.value?.validate()
  submitting.value = true
  try {
    const { id, ...rest } = categoryDialog.form
    if (id) {
      await updatePortalCategory(id, rest)
      ElMessage.success('保存成功')
    } else {
      await createPortalCategory(rest)
      ElMessage.success('创建成功')
    }
    categoryDialog.visible = false
    loadAll()
  } finally {
    submitting.value = false
  }
}

/* ---------------- 链接表单 ---------------- */
const linkFormRef = ref<FormInstance>()
const linkDialog = reactive({
  visible: false,
  form: {
    id: undefined as number | undefined,
    categoryId: undefined as number | undefined,
    linkName: '',
    url: '',
    description: '',
    username: '',
    password: '',
    icon: 'Link',
    color: '#409EFF'
  }
})
const linkRules = {
  categoryId: [{ required: true, message: '请选择所属板块', trigger: 'change' }],
  linkName: [{ required: true, message: '请输入系统名称', trigger: 'blur' }],
  url: [{ required: true, message: '请输入访问地址', trigger: 'blur' }]
}

function openLinkDialog(row?: PortalLinkItem) {
  linkDialog.form = row
    ? {
        id: row.id,
        categoryId: row.categoryId,
        linkName: row.linkName,
        url: row.url,
        description: row.description || '',
        username: row.username || '',
        password: row.password || '',
        icon: row.icon || 'Link',
        color: row.color || '#409EFF'
      }
    : {
        id: undefined,
        categoryId: linkFilterCategory.value,
        linkName: '',
        url: '',
        description: '',
        username: '',
        password: '',
        icon: 'Link',
        color: '#409EFF'
      }
  linkDialog.visible = true
}

async function submitLink() {
  await linkFormRef.value?.validate()
  submitting.value = true
  try {
    const { id, categoryId, ...rest } = linkDialog.form
    // 表单校验已保证所属板块必填
    if (categoryId == null) return
    const payload: PortalLinkForm = { categoryId, ...rest }
    if (id) {
      await updatePortalLink(id, payload)
      ElMessage.success('保存成功')
    } else {
      await createPortalLink(payload)
      ElMessage.success('创建成功')
    }
    linkDialog.visible = false
    loadAll()
  } finally {
    submitting.value = false
  }
}

/* ---------------- 展示辅助 ---------------- */
/** 判断是否为 Element Plus 图标名（否则视为 emoji 文本渲染） */
function isEpIcon(name?: string): boolean {
  return !!name && name in ElementPlusIcons
}

/** 主题色淡色背景 */
function tintColor(color?: string): string {
  const hex = color || '#409EFF'
  const r = parseInt(hex.slice(1, 3), 16)
  const g = parseInt(hex.slice(3, 5), 16)
  const b = parseInt(hex.slice(5, 7), 16)
  return `rgba(${r}, ${g}, ${b}, 0.1)`
}

function cardStyle(color?: string) {
  return {
    '--link-color': color || '#409EFF'
  } as Record<string, string>
}

/** 打开链接：外部新窗口，站内路由跳转 */
function openLink(link: PortalLinkItem) {
  if (link.url.startsWith('/')) {
    router.push(link.url)
  } else {
    window.open(link.url, '_blank', 'noopener')
  }
}

/* ---------------- 表格板块辅助 ---------------- */
/** 当前可见密码的行 ID 集合 */
const pwdVisible = ref<Set<number>>(new Set())

function togglePwd(id: number) {
  const s = new Set(pwdVisible.value)
  if (s.has(id)) {
    s.delete(id)
  } else {
    s.add(id)
  }
  pwdVisible.value = s
}

/** 拼接 用户名 / 密码 便于复制 */
function formatCredential(row: PortalLinkItem): string {
  return [row.username, row.password].filter(Boolean).join(' / ')
}

/** 复制文本到剪贴板 */
async function copyText(text: string) {
  if (!text) return
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制')
  } catch {
    ElMessage.warning('复制失败，请手动复制')
  }
}

onMounted(loadOverview)
</script>

<style scoped lang="scss">
.portal-page {
  padding: 20px;
  min-height: 100%;
  box-sizing: border-box;
}

/* 顶部工具条 */
.portal-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 20px;

  .portal-title {
    h2 {
      margin: 0;
      font-size: 22px;
      color: var(--text-main);
    }
    p {
      margin: 6px 0 0;
      font-size: 13px;
      color: var(--text-sub);
    }
  }

  .portal-actions {
    display: flex;
    align-items: center;
    gap: 10px;
  }
}

.portal-body {
  min-height: 200px;
}

/* 板块卡片 */
.category-card {
  background: #f8f9fa;
  border-radius: 12px;
  padding: 20px 20px 24px;
  margin-bottom: 24px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  transition: box-shadow 0.2s;

  &:hover {
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  }
}

.category-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  padding-left: 14px;
  position: relative;

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 50%;
    transform: translateY(-50%);
    width: 4px;
    height: 36px;
    border-radius: 2px;
    background: var(--link-color);
  }

  .category-meta {
    flex: 1;
    h3 {
      margin: 0;
      font-size: 20px;
      font-weight: 700;
      color: var(--link-color);
    }
    p {
      margin: 4px 0 0;
      font-size: 12px;
      color: var(--text-sub);
    }
  }

  .category-count {
    font-size: 12px;
    color: var(--text-sub);
    background: #fff;
    border-radius: 10px;
    padding: 3px 10px;
  }
}

/* 板块图标圆底 */
.category-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 10px;
  flex-shrink: 0;

  &.small {
    width: 34px;
    height: 34px;
    border-radius: 8px;
  }

  .icon-emoji {
    font-size: 20px;
    line-height: 1;
  }
}

/* 链接卡片网格 */
.link-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}

.link-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 16px;
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 12px;
  cursor: pointer;
  text-decoration: none;
  transition: all 0.2s;

  &:hover {
    border-color: var(--link-color);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    transform: translateY(-2px);

    .link-arrow {
      opacity: 1;
      transform: translateX(0);
    }
  }

  .link-icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 36px;
    min-height: 36px;
    padding: 3px 10px;
    border-radius: 10px;
    flex-shrink: 0;

    .el-icon {
      font-size: 18px;
    }

    .icon-emoji {
      font-size: 12px;
      font-weight: 600;
      line-height: 1;
      white-space: nowrap;
    }
  }

  .link-info {
    min-width: 0;
    max-width: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    text-align: center;

    .link-name {
      max-width: 100%;
      font-size: 14px;
      font-weight: 600;
      color: var(--text-main);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .link-desc {
      margin-top: 2px;
      font-size: 11px;
      color: var(--text-sub);
      display: -webkit-box;
      -webkit-line-clamp: 1;
      line-clamp: 1;
      -webkit-box-orient: vertical;
      overflow: hidden;
      max-width: 100%;
    }
  }

  .link-arrow {
    position: absolute;
    top: 10px;
    right: 10px;
    opacity: 0;
    transform: translateX(-4px);
    transition: all 0.2s;
    flex-shrink: 0;
  }
}

/* 表格展示板块（其他地址清单） */
.address-table-wrap {
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 12px;
  overflow: hidden;

  .address-table {
    --el-table-border-color: #f2f3f5;
    --el-table-header-bg-color: #fafbfc;
    --el-table-header-text-color: #606266;
    --el-table-row-hover-bg-color: #f8f9fa;

    :deep(.el-table__inner-wrapper::before) {
      display: none;
    }
  }

  .addr-cell,
  .cred-cell {
    display: flex;
    align-items: center;
    gap: 6px;
    min-height: 32px;
  }

  .addr-code {
    font-family: 'JetBrains Mono', Consolas, 'Courier New', monospace;
    font-size: 13px;
    color: var(--text-main);
    word-break: break-all;
  }

  .addr-desc {
    font-size: 13px;
    color: var(--text-sub);
  }

  .cred-user {
    font-family: 'JetBrains Mono', Consolas, monospace;
    font-size: 13px;
    font-weight: 500;
    color: var(--text-main);
  }

  .cred-sep {
    color: #c0c4cc;
    margin: 0 2px;
  }

  .cred-pass {
    font-family: 'JetBrains Mono', Consolas, monospace;
    font-size: 13px;
    color: var(--text-sub);
    letter-spacing: 1px;
  }

  .cell-action {
    color: #909399;
    opacity: 0.5;
    transition: opacity 0.2s;

    &:hover {
      opacity: 1;
      color: var(--el-color-primary);
    }
  }

  .addr-cell:hover .cell-action,
  .cred-cell:hover .cell-action {
    opacity: 1;
  }
}

/* 管理视图 */
.manage-header {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}

.table-toolbar {
  margin-bottom: 14px;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;

  .text-muted {
    font-size: 12px;
  }
}

.cell-category {
  display: flex;
  align-items: center;
  gap: 10px;

  .cell-name {
    font-size: 14px;
    font-weight: 500;
    color: var(--text-main);
  }

  .cell-sub {
    font-size: 12px;
    color: var(--text-sub);
    margin-top: 2px;
  }
}

.cell-url {
  font-size: 13px;
  color: var(--text-sub);
  word-break: break-all;
}

.icon-option {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.text-muted {
  color: #909399;
}
</style>
