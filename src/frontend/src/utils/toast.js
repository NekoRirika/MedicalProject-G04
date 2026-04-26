import { reactive } from 'vue'

/** @typedef {'success'|'error'|'warning'|'info'} ToastType */

export const toastState = reactive({
  /** @type {Array<{ id: number, type: ToastType, message: string, exiting: boolean }>} */
  items: []
})

let seq = 0
const timers = new Map()

/**
 * @param {number} id
 */
export function dismissToast(id) {
  const item = toastState.items.find((t) => t.id === id)
  if (!item || item.exiting) return
  const t = timers.get(id)
  if (t) {
    clearTimeout(t)
    timers.delete(id)
  }
  item.exiting = true
  setTimeout(() => {
    const idx = toastState.items.findIndex((x) => x.id === id)
    if (idx !== -1) toastState.items.splice(idx, 1)
  }, 320)
}

/**
 * @param {ToastType} type
 * @param {string} message
 * @param {number} [durationMs]
 */
export function showToast(type, message, durationMs = 5000) {
  const id = ++seq
  toastState.items.push({
    id,
    type,
    message: String(message ?? ''),
    exiting: false
  })
  const timer = setTimeout(() => dismissToast(id), durationMs)
  timers.set(id, timer)
  return id
}

export const toast = {
  success: (msg, durationMs) => showToast('success', msg, durationMs ?? 5000),
  error: (msg, durationMs) => showToast('error', msg, durationMs ?? 5000),
  warning: (msg, durationMs) => showToast('warning', msg, durationMs ?? 5000),
  info: (msg, durationMs) => showToast('info', msg, durationMs ?? 5000)
}
