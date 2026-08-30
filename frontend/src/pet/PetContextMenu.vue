<template>
  <div class="pet-menu" :style="{ left: x + 'px', top: y + 'px' }">
    <div
      v-for="item in items"
      :key="item.key"
      class="pet-menu-item"
      :class="{ divided: item.divided, disabled: item.disabled }"
      @click="onSelect(item)"
    >
      <el-icon v-if="item.icon" :size="14" class="pet-menu-icon">
        <component :is="item.icon" />
      </el-icon>
      <span class="pet-menu-label">{{ item.label }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted } from 'vue'
import type { PetMenuItem } from './types'

const props = defineProps<{ items: PetMenuItem[]; x: number; y: number }>()
const emit = defineEmits<{
  (e: 'select', item: PetMenuItem): void
  (e: 'close'): void
}>()

function onSelect(item: PetMenuItem) {
  if (item.disabled) return
  emit('select', item)
}

function onWindowEvent() {
  emit('close')
}

onMounted(() => {
  window.addEventListener('click', onWindowEvent)
  window.addEventListener('contextmenu', onWindowEvent)
  window.addEventListener('resize', onWindowEvent)
})

onBeforeUnmount(() => {
  window.removeEventListener('click', onWindowEvent)
  window.removeEventListener('contextmenu', onWindowEvent)
  window.removeEventListener('resize', onWindowEvent)
})
</script>

<style scoped lang="scss">
.pet-menu {
  position: fixed;
  z-index: 3200;
  min-width: 188px;
  background: #fff;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.16);
  padding: 6px;
  user-select: none;
  animation: pet-menu-in 0.12s ease-out;
}
@keyframes pet-menu-in {
  from {
    opacity: 0;
    transform: translateY(-4px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

.pet-menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 6px;
  font-size: 13px;
  color: var(--text-main);
  cursor: pointer;
  transition: background 0.15s;

  &.divided {
    margin-top: 6px;
    padding-top: 10px;
    border-top: 1px solid var(--border-color);
  }
  &:hover {
    background: var(--brand-color-light);
  }
  &.disabled {
    color: #b6bcc6;
    cursor: not-allowed;
  }
}

.pet-menu-icon {
  color: var(--brand-color);
}
</style>
