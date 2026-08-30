<template>
  <svg
    class="lion-svg"
    :class="{ sleeping }"
    viewBox="0 0 140 140"
    xmlns="http://www.w3.org/2000/svg"
  >
    <defs>
      <radialGradient id="lionHeadGrad" cx="38%" cy="30%" r="80%">
        <stop offset="0%" stop-color="#ffc56e" />
        <stop offset="100%" stop-color="#f59e42" />
      </radialGradient>
      <radialGradient id="lionBellyGrad" cx="50%" cy="30%" r="80%">
        <stop offset="0%" stop-color="#ffe3b3" />
        <stop offset="100%" stop-color="#ffd49b" />
      </radialGradient>
    </defs>

    <!-- 尾巴 -->
    <g class="lion-tail">
      <path
        d="M106 108 C 126 106, 132 84, 124 70"
        fill="none"
        stroke="#e07b26"
        stroke-width="7"
        stroke-linecap="round"
      />
      <circle cx="123" cy="68" r="8" fill="#e07b26" />
    </g>

    <!-- 身体 -->
    <ellipse
      cx="70"
      cy="112"
      rx="40"
      ry="24"
      fill="url(#lionBellyGrad)"
      stroke="#ef9d3c"
      stroke-width="1.5"
    />
    <!-- 前爪 -->
    <ellipse cx="50" cy="123" rx="11" ry="8" fill="#ffd49b" stroke="#ef9d3c" stroke-width="1.5" />
    <ellipse cx="90" cy="123" rx="11" ry="8" fill="#ffd49b" stroke="#ef9d3c" stroke-width="1.5" />

    <!-- 鬃毛 -->
    <g class="lion-mane">
      <circle cx="70" cy="64" r="46" fill="#e07b26" />
      <g v-for="i in 12" :key="i" :transform="`rotate(${i * 30} 70 64)`">
        <ellipse cx="70" cy="18" rx="12" ry="22" fill="#ea8730" />
      </g>
    </g>

    <!-- 耳朵 -->
    <g class="lion-ear" transform="rotate(-16 44 30)">
      <ellipse cx="44" cy="30" rx="12" ry="16" fill="#e07b26" />
      <ellipse cx="44" cy="33" rx="7" ry="10" fill="#ffb3a7" />
    </g>
    <g class="lion-ear" transform="rotate(16 96 30)">
      <ellipse cx="96" cy="30" rx="12" ry="16" fill="#e07b26" />
      <ellipse cx="96" cy="33" rx="7" ry="10" fill="#ffb3a7" />
    </g>

    <!-- 头 -->
    <ellipse
      cx="70"
      cy="64"
      rx="40"
      ry="38"
      fill="url(#lionHeadGrad)"
      stroke="#ef9d3c"
      stroke-width="1.5"
    />

    <!-- 眼睛（睁眼/眨眼） -->
    <template v-if="!sleeping">
      <g class="lion-eye">
        <ellipse cx="52" cy="60" rx="8" ry="10" fill="#fff" />
        <circle cx="54" cy="61" r="5" fill="#3a2b22" />
        <circle cx="56" cy="58" r="1.8" fill="#fff" />
      </g>
      <g class="lion-eye">
        <ellipse cx="88" cy="60" rx="8" ry="10" fill="#fff" />
        <circle cx="90" cy="61" r="5" fill="#3a2b22" />
        <circle cx="92" cy="58" r="1.8" fill="#fff" />
      </g>
    </template>
    <!-- 眼睛（睡觉闭眼） -->
    <template v-else>
      <path
        d="M46 60 q 6 5 12 0"
        fill="none"
        stroke="#3a2b22"
        stroke-width="2.6"
        stroke-linecap="round"
      />
      <path
        d="M82 60 q 6 5 12 0"
        fill="none"
        stroke="#3a2b22"
        stroke-width="2.6"
        stroke-linecap="round"
      />
    </template>

    <!-- 腮红 -->
    <ellipse cx="42" cy="75" rx="7" ry="5" fill="#ff9e9e" opacity="0.55" />
    <ellipse cx="98" cy="75" rx="7" ry="5" fill="#ff9e9e" opacity="0.55" />

    <!-- 鼻子 -->
    <path d="M65 72 q 5 -4 10 0 l -5 5 z" fill="#3a2b22" />

    <!-- 嘴巴 -->
    <path
      d="M64 79 q 6 6 12 0"
      fill="none"
      stroke="#3a2b22"
      stroke-width="2.4"
      stroke-linecap="round"
    />
    <path d="M70 77 v 3" stroke="#3a2b22" stroke-width="2" stroke-linecap="round" />

    <!-- 胡须 -->
    <g stroke="#fff" stroke-width="1.6" stroke-linecap="round" opacity="0.85">
      <line x1="34" y1="66" x2="14" y2="62" />
      <line x1="34" y1="72" x2="12" y2="74" />
      <line x1="106" y1="66" x2="126" y2="62" />
      <line x1="106" y1="72" x2="128" y2="74" />
    </g>
  </svg>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    /** 睡觉：闭眼 */
    sleeping?: boolean
  }>(),
  { sleeping: false }
)
</script>

<style scoped lang="scss">
.lion-svg {
  display: block;
  width: 100%;
  height: 100%;
  overflow: visible; /* 尾巴可略微超出边界 */
}

/* 眨眼 */
.lion-eye {
  transform-box: fill-box;
  transform-origin: center;
  animation: lion-blink 4.2s infinite;
}
@keyframes lion-blink {
  0%,
  90%,
  100% {
    transform: scaleY(1);
  }
  94% {
    transform: scaleY(0.08);
  }
}

/* 尾巴摆动 */
.lion-tail {
  transform-box: fill-box;
  transform-origin: left center;
  animation: lion-tail-wag 2.8s ease-in-out infinite;
}
@keyframes lion-tail-wag {
  0%,
  100% {
    transform: rotate(0deg);
  }
  50% {
    transform: rotate(14deg);
  }
}

/* 睡觉时尾巴静止、耳朵耷拉 */
.lion-svg.sleeping .lion-tail {
  animation: none;
}
.lion-svg.sleeping .lion-ear {
  transform-box: fill-box;
  transform-origin: center;
  animation: lion-ear-flop 4s ease-in-out infinite;
}
@keyframes lion-ear-flop {
  0%,
  100% {
    transform: rotate(-8deg);
  }
  50% {
    transform: rotate(10deg);
  }
}
</style>
