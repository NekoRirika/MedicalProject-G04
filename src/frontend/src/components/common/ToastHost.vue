<template>
  <div class="toast-host" aria-live="polite">
    <div class="toast-stack">
      <div
        v-for="t in toastState.items"
        :key="t.id"
        class="toast-item"
        :class="{ 'toast--exiting': t.exiting }"
        :style="itemStyle(t.type)"
        role="status"
      >
        <span class="toast-msg">{{ t.message }}</span>
        <button
          type="button"
          class="toast-close"
          aria-label="关闭"
          @click="close(t.id)"
        >
          ×
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import { toastState, dismissToast } from '@/utils/toast'

/** 各类型实心底色（医疗风绿 / 红 / 琥珀 / 蓝） */
const TOAST_BG = {
  success: '#2e7d32',
  error: '#c62828',
  warning: '#f9a825',
  info: '#1976d2'
}

function hexToRgb(hex) {
  let h = String(hex).replace('#', '').trim()
  if (h.length === 3) {
    h = h
      .split('')
      .map((c) => c + c)
      .join('')
  }
  const n = parseInt(h, 16)
  if (Number.isNaN(n)) return { r: 97, g: 97, b: 97 }
  return { r: (n >> 16) & 255, g: (n >> 8) & 255, b: n & 255 }
}

/** sRGB 相对亮度（WCAG），范围约 0～1 */
function relativeLuminance({ r, g, b }) {
  const lin = (c) => {
    c /= 255
    return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4)
  }
  const R = lin(r)
  const G = lin(g)
  const B = lin(b)
  return 0.2126 * R + 0.7152 * G + 0.0722 * B
}

/** 根据背景亮度自动选黑字或白字 */
function pickTextOnBg(hexBg) {
  const L = relativeLuminance(hexToRgb(hexBg))
  return L > 0.5 ? '#1a1a1a' : '#ffffff'
}

export default {
  name: 'ToastHost',
  data() {
    return { toastState }
  },
  methods: {
    close(id) {
      dismissToast(id)
    },
    itemStyle(type) {
      const bg = TOAST_BG[type] || '#616161'
      const color = pickTextOnBg(bg)
      return {
        backgroundColor: bg,
        color
      }
    }
  }
}
</script>

<style scoped>
.toast-host {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 10050;
}

.toast-stack {
  position: absolute;
  right: 24px;
  bottom: 24px;
  display: flex;
  flex-direction: column-reverse;
  align-items: flex-end;
  gap: 10px;
}

.toast-item {
  pointer-events: auto;
  display: flex;
  align-items: flex-start;
  gap: 10px;
  min-width: 280px;
  max-width: 400px;
  padding: 12px 12px 12px 14px;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
  border: none;
  transition: transform 0.32s ease, opacity 0.32s ease;
  transform: translateX(0);
  opacity: 1;
}

.toast-item.toast--exiting {
  transform: translateX(calc(100% + 40px));
  opacity: 0;
}

.toast-msg {
  flex: 1;
  font-size: 14px;
  line-height: 1.45;
  word-break: break-word;
  white-space: pre-wrap;
  max-height: 240px;
  overflow-y: auto;
}

.toast-close {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  margin: -4px -4px -4px 0;
  border: none;
  background: transparent;
  color: inherit;
  opacity: 0.85;
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
  border-radius: 4px;
  padding: 0;
}

.toast-close:hover {
  opacity: 1;
  background: rgba(0, 0, 0, 0.12);
}
</style>
