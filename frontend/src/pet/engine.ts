import { ref, type Ref } from 'vue'
import type { PetBehavior } from './types'

/** 行为随机规则 */
interface BehaviorRule {
  behavior: PetBehavior
  weight: number
  min: number
  max: number
}

/**
 * 日常行为权重表。
 * 扩展新行为：在此添加一条规则，并在 PetLion.vue 的 CSS 中补充对应动画类即可。
 */
const RULES: BehaviorRule[] = [
  { behavior: 'idle', weight: 55, min: 4000, max: 9000 },
  { behavior: 'walk', weight: 25, min: 3000, max: 6500 },
  { behavior: 'sleep', weight: 12, min: 7000, max: 14000 },
  { behavior: 'play', weight: 8, min: 2500, max: 4500 },
]

/** 隐藏在边缘时的行为规则（几乎只保持 docked，偶尔摆动） */
const DOCK_RULES: BehaviorRule[] = [{ behavior: 'docked', weight: 100, min: 6000, max: 12000 }]

/**
 * 宠物行为引擎：状态机 + 随机调度。
 * 与视图完全解耦，可独立测试；通过 start()/stop() 控制生命周期。
 */
export class PetEngine {
  /** 当前行为 */
  readonly behavior: Ref<PetBehavior> = ref('idle')
  /** 朝向：1 朝右，-1 朝左 */
  readonly facing: Ref<1 | -1> = ref(1)
  /** 是否在行走摆动（供视图播放步伐动画） */
  readonly pacing: Ref<boolean> = ref(false)

  private timer?: ReturnType<typeof setTimeout>

  /** 启动自动行为调度（组件 onMounted 时调用） */
  start(): void {
    this.schedule()
  }

  /** 停止自动调度（组件卸载时调用） */
  stop(): void {
    if (this.timer) {
      clearTimeout(this.timer)
      this.timer = undefined
    }
    this.pacing.value = false
  }

  /**
   * 强制切换行为。
   * @param keep true 时暂停自动调度，由调用方负责恢复（interact 内部使用）
   */
  setBehavior(b: PetBehavior, keep = false): void {
    this.behavior.value = b
    if (b === 'walk') {
      this.facing.value = Math.random() > 0.5 ? 1 : -1
      this.pacing.value = true
    } else {
      this.pacing.value = false
    }
    if (!keep) this.schedule()
  }

  /** 互动行为：立即执行某个行为 duration 毫秒后自动回到待机并恢复调度 */
  interact(b: PetBehavior, duration = 3500): void {
    this.setBehavior(b, true)
    if (this.timer) {
      clearTimeout(this.timer)
      this.timer = undefined
    }
    this.timer = setTimeout(() => {
      this.setBehavior('idle')
    }, duration)
  }

  /** 随机调度下一个行为 */
  private schedule(): void {
    if (this.timer) {
      clearTimeout(this.timer)
      this.timer = undefined
    }
    const rules = this.behavior.value === 'docked' ? DOCK_RULES : RULES
    const pool: BehaviorRule[] = []
    for (const r of rules) {
      for (let i = 0; i < r.weight; i++) pool.push(r)
    }
    const rule = pool[Math.floor(Math.random() * pool.length)]
    const delay = rule.min + Math.random() * (rule.max - rule.min)
    this.timer = setTimeout(() => this.setBehavior(rule.behavior), delay)
  }
}
