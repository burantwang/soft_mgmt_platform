<template>
  <Teleport to="body">
    <!-- 舞台：默认右下角，可拖拽移动；docked 时藏到右边缘只露一点 -->
    <div
      v-if="config.enabled"
      ref="stageEl"
      class="pet-stage"
      :class="{ docked: config.docked }"
      :style="stageStyle"
    >
      <!-- 插件功能面板 -->
      <PetPanel
        v-if="panelComp"
        :title="panelTitle"
        :content="panelComp"
        :style="panelStyle"
        @close="closePanel"
      />

      <!-- 消息气泡 -->
      <Transition name="pet-pop">
        <div v-if="bubble" class="pet-bubble">{{ bubble }}</div>
      </Transition>

      <!-- 睡眠 Zzz -->
      <Transition name="pet-pop">
        <div v-if="isSleeping" class="pet-zzz">z&thinsp;Z&thinsp;Z</div>
      </Transition>

      <!-- 狮子本体 -->
      <div
        class="pet-body"
        @mousedown.prevent="onMouseDown"
        @dblclick.prevent="onDblClick"
        @contextmenu.prevent="onContextMenu"
      >
        <div class="pet-sprite-wrap" :class="{ 'facing-left': facingLeft }">
          <div class="pet-sprite" :class="behaviorClass">
            <LionSprite :sleeping="isSleeping" />
          </div>
        </div>
      </div>
    </div>

    <!-- 右键菜单 -->
    <PetContextMenu
      v-if="menu.visible"
      :items="menuItems"
      :x="menu.x"
      :y="menu.y"
      @select="onMenuSelect"
      @close="menu.visible = false"
    />
  </Teleport>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import type { Component } from 'vue'
import { PetEngine } from './engine'
import { getPetPlugins } from './plugins'
import type { PetConfig, PetMenuItem, PetPluginContext } from './types'
import LionSprite from './LionSprite.vue'
import PetContextMenu from './PetContextMenu.vue'
import PetPanel from './PetPanel.vue'

const CONFIG_KEY = 'pet-lion-config'
const DEFAULT_CONFIG: PetConfig = { enabled: true, docked: false, mute: false, scale: 1 }

/** 随机台词库（可扩展） */
const LINES = [
  '嗷呜～ 今天也要元气满满！',
  '工作再忙也要注意休息哦',
  '我在认真看着你写代码呢',
  '要不要和我玩一会儿？',
  '嘿嘿，发现你啦！',
  '任务做完了记得夸夸我～',
  '右键我可以把我藏到角落哦',
  '加油！你可以的！',
]

function loadConfig(): PetConfig {
  try {
    const raw = localStorage.getItem(CONFIG_KEY)
    if (raw) return { ...DEFAULT_CONFIG, ...(JSON.parse(raw) as Partial<PetConfig>) }
  } catch {
    /* 忽略损坏配置 */
  }
  return { ...DEFAULT_CONFIG }
}

const pick = <T>(arr: T[]): T => arr[Math.floor(Math.random() * arr.length)]

const engine = new PetEngine()
const config = ref<PetConfig>(loadConfig())
const stageEl = ref<HTMLDivElement | null>(null)
const bubble = ref('')
const panelComp = ref<Component | null>(null)
const panelTitle = ref('')
const menu = reactive({ visible: false, x: 0, y: 0 })

let bubbleTimer: ReturnType<typeof setTimeout> | undefined

/* ---------- 持久化 ---------- */
watch(
  config,
  (c) => {
    try {
      localStorage.setItem(CONFIG_KEY, JSON.stringify(c))
    } catch {
      /* noop */
    }
  },
  { deep: true }
)

/* docked 与引擎状态同步 */
watch(
  () => config.value.docked,
  (d) => {
    if (d) engine.setBehavior('docked')
    else if (engine.behavior.value === 'docked') engine.setBehavior('idle')
  }
)

/* ---------- 台词气泡 ---------- */
function say(text?: string) {
  if (config.value.mute) return
  bubble.value = text ?? pick(LINES)
  if (bubbleTimer) clearTimeout(bubbleTimer)
  bubbleTimer = setTimeout(() => {
    bubble.value = ''
  }, 2800)
}

/* ---------- 功能面板（插件打开） ---------- */
function openPanel(component: Component, title = '小狮子') {
  panelTitle.value = title
  panelComp.value = component
}
function closePanel() {
  panelComp.value = null
}

/* ---------- 插件上下文 ---------- */
const ctx: PetPluginContext = {
  get config() {
    return config.value
  },
  pet: {
    get behavior() {
      return engine.behavior.value
    },
    say,
    play() {
      engine.interact('play', 4200)
      say('哈哈，好开心！')
    },
    dock() {
      config.value.docked = true
    },
    undock() {
      config.value.docked = false
      say('我回来啦！')
    },
    toggleMute() {
      config.value.mute = !config.value.mute
    },
    setScale(s) {
      config.value.scale = Math.min(1.6, Math.max(0.7, Number(s.toFixed(1))))
    },
  },
  openPanel,
  closePanel,
}

/* ---------- 派生状态 ---------- */
const isSleeping = computed(() => engine.behavior.value === 'sleep')
const facingLeft = computed(() => engine.facing.value === -1)
const behaviorClass = computed(() => `is-${engine.behavior.value}`)

/* ---------- 右键菜单 ---------- */
const menuItems = computed<PetMenuItem[]>(() => {
  const items: PetMenuItem[] = [
    { key: 'play', label: '陪小狮子玩', icon: 'MagicStick', action: () => ctx.pet.play() },
    { key: 'say', label: '和我说句话', icon: 'ChatDotRound', action: () => say() },
    {
      key: 'sleep',
      label: '睡觉啦',
      icon: 'Moon',
      action: () => {
        engine.interact('sleep', 8000)
        say('晚安，做个好梦～')
      },
    },
    {
      key: 'dock',
      label: config.value.docked ? '从边缘出来' : '藏到边缘',
      icon: 'Hide',
      divided: true,
      action: () => (config.value.docked ? ctx.pet.undock() : ctx.pet.dock()),
    },
  ]
  // 插件注入的菜单项
  for (const plugin of getPetPlugins()) {
    for (const mi of plugin.menuItems ?? []) {
      items.push({ ...mi, divided: true, action: (c) => mi.action?.(c) })
    }
  }
  items.push(
    {
      key: 'size-up',
      label: '变大一点',
      icon: 'ZoomIn',
      divided: true,
      action: () => ctx.pet.setScale(config.value.scale + 0.1),
    },
    {
      key: 'size-down',
      label: '变小一点',
      icon: 'ZoomOut',
      action: () => ctx.pet.setScale(config.value.scale - 0.1),
    },
    {
      key: 'mute',
      label: config.value.mute ? '打开气泡' : '关闭气泡',
      icon: 'Microphone',
      action: () => ctx.pet.toggleMute(),
    },
    {
      key: 'about',
      label: '关于小狮子',
      icon: 'InfoFilled',
      divided: true,
      action: () =>
        say('我是研发平台的小狮子～右键可以把我藏到边缘，未来还会有更多技能哦！'),
    },
  )
  return items
})

function onMenuSelect(item: PetMenuItem) {
  menu.visible = false
  item.action?.(ctx)
}

/* ---------- 舞台定位样式 ---------- */
const stageStyle = computed<Record<string, string>>(() => {
  const s: Record<string, string> = {
    '--pet-scale': String(config.value.scale),
    '--dock-offset': `${Math.round(56 * config.value.scale)}px`,
  }
  if (config.value.docked) {
    s.right = '0'
    s.bottom = '0'
  } else if (config.value.pos) {
    s.left = `${config.value.pos.x}px`
    s.top = `${config.value.pos.y}px`
  } else {
    s.right = '16px'
    s.bottom = '16px'
  }
  return s
})

/* 面板位置：贴在狮子右上方 */
const panelStyle = computed(() => ({
  bottom: `${Math.round(140 * config.value.scale) + 30}px`,
  right: '20px',
}))

/* ---------- 拖拽 ---------- */
let drag: {
  startX: number
  startY: number
  baseX: number
  baseY: number
  moved: boolean
} | null = null

function onMouseDown(e: MouseEvent) {
  if (e.button !== 0) return
  // 藏在边缘时，点击露出的狮子直接出来
  if (config.value.docked) {
    ctx.pet.undock()
    return
  }
  engine.setBehavior('dragging', true)
  const rect = stageEl.value?.getBoundingClientRect()
  const base = config.value.pos ?? { x: rect?.left ?? 0, y: rect?.top ?? 0 }
  drag = {
    startX: e.clientX,
    startY: e.clientY,
    baseX: base.x,
    baseY: base.y,
    moved: false,
  }
  window.addEventListener('mousemove', onDragMove)
  window.addEventListener('mouseup', onDragEnd)
}

function onDragMove(e: MouseEvent) {
  if (!drag) return
  const dx = e.clientX - drag.startX
  const dy = e.clientY - drag.startY
  if (Math.abs(dx) + Math.abs(dy) > 4) drag.moved = true
  if (!drag.moved) return
  config.value.pos = {
    x: Math.round(Math.min(Math.max(drag.baseX + dx, 0), window.innerWidth - 80)),
    y: Math.round(Math.min(Math.max(drag.baseY + dy, 0), window.innerHeight - 120)),
  }
}

function onDragEnd() {
  window.removeEventListener('mousemove', onDragMove)
  window.removeEventListener('mouseup', onDragEnd)
  const wasMoved = drag?.moved ?? false
  drag = null
  if (!wasMoved) say()
  engine.setBehavior('idle')
}

/* ---------- 其他交互 ---------- */
function onDblClick() {
  engine.interact('play', 4200)
  say('嘿嘿，再玩一次！')
}

function onContextMenu(e: MouseEvent) {
  menu.x = Math.max(8, Math.min(e.clientX, window.innerWidth - 214))
  menu.y = Math.max(8, Math.min(e.clientY, window.innerHeight - 400))
  menu.visible = true
}

/* ---------- 生命周期 ---------- */
onMounted(() => {
  if (config.value.docked) engine.setBehavior('docked')
  engine.start()
  for (const p of getPetPlugins()) p.onInit?.(ctx)
  setTimeout(() => say('嗷呜～ 你好呀，我是小狮子！'), 900)
})

onBeforeUnmount(() => {
  engine.stop()
  if (bubbleTimer) clearTimeout(bubbleTimer)
  for (const p of getPetPlugins()) p.onDispose?.()
})
</script>

<style scoped lang="scss">
/* 舞台：定位锚点，本身不占空间 */
.pet-stage {
  position: fixed;
  z-index: 3100;
  width: 0;
  height: 0;
  pointer-events: none;
  --pet-scale: 1;
  --dock-offset: 56px;
}

/* 狮子本体 */
.pet-body {
  position: absolute;
  right: 0;
  bottom: 0;
  width: calc(140px * var(--pet-scale));
  height: calc(140px * var(--pet-scale));
  cursor: grab;
  pointer-events: auto;
  user-select: none;
  transition: transform 0.4s cubic-bezier(0.34, 1.3, 0.64, 1);
  &:active {
    cursor: grabbing;
  }
}

/* 藏到边缘：向右滑出，只露 --dock-offset 宽度 */
.pet-stage.docked .pet-body {
  transform: translateX(calc(100% - var(--dock-offset)));
  cursor: pointer;
}

/* 朝向翻转（作用于精灵外层，不影响动画） */
.pet-sprite-wrap {
  width: 100%;
  height: 100%;
  transition: transform 0.25s ease;
}
.pet-sprite-wrap.facing-left {
  transform: scaleX(-1);
}

/* 行为动画（作用于精灵，独立于朝向/位移） */
.pet-sprite {
  width: 100%;
  height: 100%;
  transform-origin: center bottom;
}

.is-idle .pet-sprite {
  animation: pet-breath 3s ease-in-out infinite;
}
.is-walk .pet-sprite {
  animation: pet-walk 0.8s ease-in-out infinite;
}
.is-play .pet-sprite {
  animation: pet-jump 0.7s ease-in-out infinite;
}
.is-sleep .pet-sprite {
  animation: pet-sleep 4s ease-in-out infinite;
}
.is-docked .pet-sprite {
  animation: pet-sway 2.6s ease-in-out infinite;
}

@keyframes pet-breath {
  0%,
  100% {
    transform: scale(1) translateY(0);
  }
  50% {
    transform: scale(1.03) translateY(-2px);
  }
}
@keyframes pet-walk {
  0%,
  100% {
    transform: translateX(-7px) rotate(-3deg);
  }
  50% {
    transform: translateX(7px) rotate(3deg);
  }
}
@keyframes pet-jump {
  0%,
  100% {
    transform: translateY(0) rotate(0deg);
  }
  30% {
    transform: translateY(-26px) rotate(12deg);
  }
  65% {
    transform: translateY(0) rotate(-8deg);
  }
}
@keyframes pet-sleep {
  0%,
  100% {
    transform: translateY(0) rotate(-4deg);
  }
  50% {
    transform: translateY(2px) rotate(-6deg);
  }
}
@keyframes pet-sway {
  0%,
  100% {
    transform: translateX(0);
  }
  50% {
    transform: translateX(5px);
  }
}

/* 气泡 */
.pet-bubble {
  position: absolute;
  bottom: calc(100% + 10px);
  right: -8px;
  max-width: 220px;
  background: #fff;
  border-radius: 12px 12px 2px 12px;
  padding: 7px 12px;
  font-size: 13px;
  color: var(--text-main);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.14);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  pointer-events: none;
}

/* Zzz */
.pet-zzz {
  position: absolute;
  top: -16px;
  right: 6px;
  font-size: 16px;
  font-weight: 700;
  color: #93a1bd;
  pointer-events: none;
  animation: pet-zzz 1.8s ease-in-out infinite;
}
@keyframes pet-zzz {
  0%,
  100% {
    transform: translateY(0) scale(1);
    opacity: 0.7;
  }
  50% {
    transform: translateY(-8px) scale(1.2);
    opacity: 1;
  }
}

/* 气泡/Zzz 过渡 */
.pet-pop-enter-active,
.pet-pop-leave-active {
  transition: all 0.25s ease;
}
.pet-pop-enter-from,
.pet-pop-leave-to {
  opacity: 0;
  transform: translateY(6px) scale(0.9);
}
</style>
