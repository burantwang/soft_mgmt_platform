<template>
  <div class="pet-reminders">
    <div v-if="items.length === 0" class="pet-reminders-empty">
      <el-icon :size="28" color="#cbd5e1"><Bell /></el-icon>
      <p class="pet-reminders-text">暂时没有待办提醒</p>
      <p class="pet-reminders-hint">未来小狮子会在这里提醒你的任务</p>
    </div>
    <div v-else v-for="item in items" :key="item.id" class="pet-reminders-item">
      <span class="pet-reminders-dot" :style="{ background: item.color }" />
      <span class="pet-reminders-text">{{ item.text }}</span>
      <span class="pet-reminders-time">{{ item.time }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

/**
 * ================= 预留接口说明 =================
 * 本插件为「任务提醒」预留：
 *   1. 未来接入任务系统后，在此通过 API 拉取真实待办/提醒填充 items
 *   2. 需要主动提醒时，在对应时机调用 ctx.pet.say('...') 让小狮子弹出气泡
 *   3. 可在此基础上扩展定时轮询 / 浏览器通知（Notification API）
 */
interface ReminderItem {
  id: number
  text: string
  time: string
  color: string
}

const items = ref<ReminderItem[]>([])
</script>

<style scoped lang="scss">
.pet-reminders {
  min-height: 100px;
}

.pet-reminders-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 24px 0;
  color: var(--text-sub);

  p {
    margin: 0;
  }
}

.pet-reminders-text {
  font-size: 14px;
  color: var(--text-main);
}

.pet-reminders-hint {
  font-size: 12px;
  color: var(--text-sub);
}

.pet-reminders-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 4px;
  border-bottom: 1px solid var(--border-color);
  font-size: 13px;

  &:last-child {
    border-bottom: none;
  }
}

.pet-reminders-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.pet-reminders-time {
  margin-left: auto;
  font-size: 12px;
  color: var(--text-sub);
}
</style>
