<template>
  <el-tooltip
    :visible="hovering"
    trigger="manual"
    placement="top"
    :show-after="0"
    :hide-after="0"
    :enterable="false"
    popper-class="resize-tip-popper"
  >
    <template #content>
      <div class="resize-tip-content">{{ modelValue || '（无内容）' }}</div>
    </template>
    <div
      class="resize-tip-cell"
      @mousemove="onMove"
      @mousedown="onDown"
      @mouseleave="onLeave"
    >
      <el-input
        :model-value="modelValue"
        type="textarea"
        :autosize="autosize"
        :placeholder="placeholder"
        :disabled="disabled"
        @update:model-value="(v: string) => emit('update:modelValue', v)"
      />
    </div>
  </el-tooltip>
</template>

<script setup lang="ts">
import { ref } from 'vue'

/**
 * 可编辑的 textarea，并在右下角 resize handle 区域提供 hover 弹窗预览：
 * - 鼠标悬停到 textarea 右下角 ~16x16 像素的 resize handle 时，弹框显示完整内容
 * - 鼠标按下时立即关闭弹框，让浏览器原生 resize 操作正常接管
 */
const props = defineProps<{
  modelValue?: string
  autosize?: { minRows?: number; maxRows?: number }
  placeholder?: string
  disabled?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [v: string]
}>()

const hovering = ref(false)

/** 鼠标在右下角 16x16 像素区域内才认为悬停在 resize handle 上 */
function onMove(e: MouseEvent) {
  const el = e.currentTarget as HTMLElement
  const rect = el.getBoundingClientRect()
  const dx = rect.right - e.clientX
  const dy = rect.bottom - e.clientY
  const inHandle = dx >= 0 && dx <= 16 && dy >= 0 && dy <= 16
  if (inHandle !== hovering.value) {
    hovering.value = inHandle
  }
}

/** 按下立即关闭弹框，浏览器原生 resize 接管拖拽 */
function onDown() {
  if (hovering.value) hovering.value = false
}

/** 离开整个区域也要关闭 */
function onLeave() {
  if (hovering.value) hovering.value = false
}
</script>

<style scoped>
.resize-tip-cell {
  position: relative;
}
</style>

<style>
/* tooltip 渲染到 body 下，需要全局样式 */
.resize-tip-popper {
  max-width: 480px;
}
.resize-tip-popper .resize-tip-content {
  max-height: 320px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 13px;
  line-height: 1.6;
  /* el-tooltip 默认 dark 主题，背景 #303133，文字必须用浅色才看得见 */
  color: #ffffff;
  padding: 4px;
}
</style>
