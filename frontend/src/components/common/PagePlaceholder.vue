<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">{{ title }}</span>
    </div>
    <div class="page-card">
      <el-empty :description="`${title}功能建设中（将在后续阶段实现）`" :image-size="140" />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{ title: string }>()
</script>
