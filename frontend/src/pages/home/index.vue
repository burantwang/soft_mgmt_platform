<template>
  <div class="home-page">
    <!-- 欢迎横幅 -->
    <section class="hero">
      <div>
        <h2>欢迎回来，{{ nickname }}</h2>
        <p>这里是研发业务平台总览，聚合各板块最新动态与统计数据。</p>
      </div>
      <el-button type="primary" plain :icon="Refresh" circle title="刷新" @click="load" />
    </section>

    <!-- 统计卡片 -->
    <div class="stat-grid">
      <div class="stat-card" @click="router.push('/release')">
        <div class="sc-icon release"><el-icon><Promotion /></el-icon></div>
        <div class="sc-main">
          <div class="sc-value">{{ summary?.releaseTotal ?? '-' }}</div>
          <div class="sc-label">累计发布</div>
        </div>
        <div class="sc-sub">
          <span class="ok">今日 {{ summary?.releaseToday ?? 0 }}</span>
          <span class="ok">成功 {{ summary?.releaseSuccess ?? 0 }}</span>
          <span class="bad">失败 {{ summary?.releaseFailed ?? 0 }}</span>
        </div>
      </div>
      <div class="stat-card" @click="router.push('/task')">
        <div class="sc-icon task"><el-icon><Warning /></el-icon></div>
        <div class="sc-main">
          <div class="sc-value">{{ summary?.taskTotal ?? '-' }}</div>
          <div class="sc-label">Daily_Sanity</div>
        </div>
        <div class="sc-sub">
          <span class="warn">待处理 {{ summary?.taskPending ?? 0 }}</span>
          <span class="primary">处理中 {{ summary?.taskProcessing ?? 0 }}</span>
        </div>
      </div>
      <div class="stat-card" @click="router.push('/wiki')">
        <div class="sc-icon wiki"><el-icon><Reading /></el-icon></div>
        <div class="sc-main">
          <div class="sc-value">{{ summary?.wikiDocCount ?? '-' }}</div>
          <div class="sc-label">Wiki 文档</div>
        </div>
        <div class="sc-sub">
          <span class="text-muted">知识库文档总数</span>
        </div>
      </div>
      <div class="stat-card" @click="router.push('/files')">
        <div class="sc-icon file"><el-icon><FolderOpened /></el-icon></div>
        <div class="sc-main">
          <div class="sc-value">{{ summary?.fileCount ?? '-' }}</div>
          <div class="sc-label">文件资源</div>
        </div>
        <div class="sc-sub">
          <span class="text-muted">测试报告/附件总数</span>
        </div>
      </div>
    </div>

    <!-- 我的待办 + 快捷入口 -->
    <div class="quick-row">
      <el-card shadow="never" class="quick-card">
        <template #header>
          <div class="card-header">
            <span>我的任务待办</span>
            <el-button link type="primary" @click="router.push('/task/mine')">全部待办 →</el-button>
          </div>
        </template>
        <div class="todo-box">
          <div class="todo-num">{{ summary?.myTodo ?? 0 }}</div>
          <div class="todo-tip">待处理 + 处理中的任务数</div>
          <el-button type="primary" :icon="List" @click="router.push('/task/mine')">去处理</el-button>
        </div>
      </el-card>
      <el-card shadow="never" class="quick-card">
        <template #header>
          <div class="card-header"><span>快捷入口</span></div>
        </template>
        <div class="entry-grid">
          <div class="entry" @click="router.push('/release')"><el-icon><Promotion /></el-icon><span>版本发布</span></div>
          <div class="entry" @click="router.push('/task')"><el-icon><Warning /></el-icon><span>Daily_Sanity</span></div>
          <div class="entry" @click="router.push('/wiki')"><el-icon><Reading /></el-icon><span>Wiki 知识库</span></div>
          <div class="entry" @click="router.push('/files')"><el-icon><FolderOpened /></el-icon><span>文件资源</span></div>
          <div class="entry" @click="router.push('/dashboard')"><el-icon><Odometer /></el-icon><span>统计看板</span></div>
          <div class="entry" @click="router.push('/profile')"><el-icon><Setting /></el-icon><span>个人中心</span></div>
        </div>
      </el-card>
    </div>

    <!-- 最新动态 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header"><span>最新动态</span></div>
      </template>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="发布" name="release">
          <el-timeline v-if="summary?.recentReleases?.length">
            <el-timeline-item
              v-for="item in summary.recentReleases"
              :key="item.id"
              :timestamp="item.time"
              :type="item.subtitle?.includes('失败') ? 'danger' : 'success'"
            >
              <div class="recent-item" @click="router.push(item.path || '/release')">
                <span class="ri-title">{{ item.title }}</span>
                <span class="ri-sub">{{ item.subtitle }}</span>
              </div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无发布记录" :image-size="80" />
        </el-tab-pane>
        <el-tab-pane label="任务" name="task">
          <el-timeline v-if="summary?.recentTasks?.length">
            <el-timeline-item
              v-for="item in summary.recentTasks"
              :key="item.id"
              :timestamp="item.time"
              type="primary"
            >
              <div class="recent-item" @click="router.push(item.path || '/task')">
                <span class="ri-title">{{ item.title }}</span>
                <span class="ri-sub">{{ item.subtitle }}</span>
              </div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无任务" :image-size="80" />
        </el-tab-pane>
        <el-tab-pane label="知识库" name="wiki">
          <el-timeline v-if="summary?.recentWikis?.length">
            <el-timeline-item v-for="item in summary.recentWikis" :key="item.id" :timestamp="item.time" type="info">
              <div class="recent-item" @click="router.push(item.path || '/wiki')">
                <span class="ri-title">{{ item.title }}</span>
                <span class="ri-sub">{{ item.subtitle }}</span>
              </div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无文档" :image-size="80" />
        </el-tab-pane>
        <el-tab-pane label="文件" name="file">
          <el-timeline v-if="summary?.recentFiles?.length">
            <el-timeline-item v-for="item in summary.recentFiles" :key="item.id" :timestamp="item.time" type="warning">
              <div class="recent-item" @click="router.push(item.path || '/files')">
                <span class="ri-title">{{ item.title }}</span>
                <span class="ri-sub">{{ item.subtitle }}</span>
              </div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无文件" :image-size="80" />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh, List, Promotion, Warning, Reading, FolderOpened, Odometer, Setting } from '@element-plus/icons-vue'
import { getHomeSummary } from '@/api/home'
import type { HomeSummary } from '@/types/home'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const nickname = userStore.userInfo?.nickname || userStore.userInfo?.username || '同学'

const summary = ref<HomeSummary>()
const activeTab = ref('release')

async function load() {
  try {
    const res = await getHomeSummary()
    if (res.code === 0) {
      summary.value = res.data
    }
  } catch {
    /* ignore */
  }
}

onMounted(load)
</script>

<style scoped lang="scss">
.home-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* ---------- 欢迎横幅 ---------- */
.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, #0f2557 0%, #1e3a8a 45%, #2b5be3 100%);
  border-radius: 14px;
  padding: 22px 24px;
  box-shadow: 0 10px 26px rgba(15, 37, 87, 0.3);

  h2 {
    margin: 0;
    font-size: 20px;
    color: #fff;
  }

  p {
    margin: 6px 0 0;
    font-size: 13px;
    color: rgba(255, 255, 255, 0.75);
  }
}

/* ---------- 统计卡片 ---------- */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  background: #fff;
  border-radius: 10px;
  padding: 16px 18px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1);
  }
}

.sc-icon {
  width: 46px;
  height: 46px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: #fff;

  &.release {
    background: #2b5be3;
  }
  &.task {
    background: #e6a23c;
  }
  &.wiki {
    background: #16a34a;
  }
  &.file {
    background: #8b5cf6;
  }
}

.sc-main {
  flex: 1;
}

.sc-value {
  font-size: 26px;
  font-weight: 700;
  line-height: 1;
  color: #1f2329;
}

.sc-label {
  margin-top: 4px;
  font-size: 13px;
  color: #6b7280;
}

.sc-sub {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
  color: #6b7280;
  text-align: right;
}

/* ---------- 待办 + 快捷入口 ---------- */
.quick-row {
  display: grid;
  grid-template-columns: 1fr 1.6fr;
  gap: 16px;
}

.quick-card {
  border-radius: 10px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}

.todo-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 10px 0;

  .todo-num {
    font-size: 40px;
    font-weight: 700;
    color: #2b5be3;
  }

  .todo-tip {
    font-size: 12px;
    color: #6b7280;
    margin-bottom: 8px;
  }
}

.entry-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.entry {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 14px 8px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  color: #4b5563;
  font-size: 13px;
  transition: all 0.15s;

  &:hover {
    border-color: #2b5be3;
    color: #2b5be3;
    background: #f0f4ff;
  }

  .el-icon {
    font-size: 20px;
  }
}

/* ---------- 最新动态 ---------- */
.recent-item {
  cursor: pointer;

  .ri-title {
    font-weight: 500;
    color: #1f2329;
  }

  .ri-sub {
    margin-left: 8px;
    font-size: 12px;
    color: #6b7280;
  }

  &:hover .ri-title {
    color: #2b5be3;
  }
}

/* ---------- 通用颜色 ---------- */
.ok {
  color: #16a34a;
}
.bad {
  color: #dc2626;
}
.warn {
  color: #e6a23c;
}
.primary {
  color: #2b5be3;
}
.text-muted {
  color: #9ca3af;
}
</style>
