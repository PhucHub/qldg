import { reactive } from 'vue'

export const toastState = reactive({
  toasts: []
})

let nextId = 1

export const toast = {
  show(message, type = 'success', duration = 3000) {
    const id = nextId++
    const item = { id, message, type }
    toastState.toasts.push(item)

    if (duration > 0) {
      setTimeout(() => {
        this.remove(id)
      }, duration)
    }
    return id
  },
  success(message, duration = 3000) {
    return this.show(message, 'success', duration)
  },
  error(message, duration = 3000) {
    return this.show(message, 'error', duration)
  },
  warning(message, duration = 3000) {
    return this.show(message, 'warning', duration)
  },
  info(message, duration = 3000) {
    return this.show(message, 'info', duration)
  },
  remove(id) {
    const index = toastState.toasts.findIndex(t => t.id === id)
    if (index !== -1) toastState.toasts.splice(index, 1)
  }
}
