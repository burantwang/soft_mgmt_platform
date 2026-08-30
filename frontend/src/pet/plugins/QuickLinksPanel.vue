<template>
  <div class="pet-panel-grid">
    <div v-for="l in links" :key="l.path" class="pet-panel-link" @click="go(l.path)">
      <div class="pet-panel-link-icon" :style="{ background: l.color }">
        <el-icon :size="16" color="#fff"><component :is="l.icon" /></el-icon>
      </div>
      <span class="pet-panel-link-label">{{ l.label }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const props = defineProps<{ close: () => void }>()
const router = useRouter()

/** 快捷入口清单（与路由表对应） */
const links = [
  { path: '/dashboard', label: '发布统计看板', icon: 'Odometer', color: '#2b5be3' },
  { path: '/release', label: '版本发布管理', icon: 'Promotion', color: '#16a34a' },
  { path: '/task', label: '任务追踪', icon: 'Warning', color: '#d97706' },
  { path: '/task/mine', label: '我的待办', icon: 'List', color: '#7c3aed' },
  { path: '/profile', label: '个人中心', icon: 'User', color: '#0891b2' },
]

function go(path: string) {
  router.push(path)
  props.close()
}
</script>

<style scoped lang="scss">
.pet-panel-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.pet-panel-link {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px;
  border-radius: 10px;
  border: 1px solid var(--border-color);
  cursor: pointer;
  transition: all 0.15s;

  &:hover {
    border-color: var(--brand-color);
    background: var(--brand-color-light);
  }
}

.pet-panel-link-icon {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.pet-panel-link-label {
  font-size: 13px;
  color: var(--text-main);
}
</style>
