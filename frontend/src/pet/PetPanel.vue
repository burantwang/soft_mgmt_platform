<template>
  <div class="pet-panel">
    <div class="pet-panel-head">
      <span class="pet-panel-title">
        <el-icon :size="15"><MagicStick /></el-icon>
        {{ title }}
      </span>
      <button class="pet-panel-close" type="button" @click="$emit('close')">×</button>
    </div>
    <div class="pet-panel-body">
      <component :is="content" :close="() => $emit('close')" />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Component } from 'vue'

defineProps<{ title: string; content: Component }>()
defineEmits<{ (e: 'close'): void }>()
</script>

<style scoped lang="scss">
.pet-panel {
  position: fixed;
  z-index: 3150;
  width: 280px;
  background: #fff;
  border: 1px solid var(--border-color);
  border-radius: 14px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.18);
  overflow: hidden;
  animation: pet-panel-in 0.18s ease-out;
}
@keyframes pet-panel-in {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

.pet-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  background: linear-gradient(135deg, #f59e42, #ea8730);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
}

.pet-panel-title {
  display: flex;
  align-items: center;
  gap: 6px;
}

.pet-panel-close {
  border: none;
  background: transparent;
  color: #fff;
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 4px;
  &:hover {
    background: rgba(255, 255, 255, 0.22);
  }
}

.pet-panel-body {
  padding: 12px;
  max-height: 320px;
  overflow-y: auto;
}
</style>
