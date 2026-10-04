import { reactive } from 'vue'

const toasts = reactive([])
let seq = 0

function remove(id) {
  const index = toasts.findIndex((t) => t.id === id)
  if (index !== -1) toasts.splice(index, 1)
}

function push(message, type = 'success', timeout = 3500) {
  const id = ++seq
  toasts.push({ id, message, type })
  if (timeout) setTimeout(() => remove(id), timeout)
}

/** Thông báo nổi dùng chung. Hiển thị bởi <ToastHost /> trong MainLayout. */
export function useToast() {
  return {
    toasts,
    remove,
    success: (message) => push(message, 'success'),
    error: (message) => push(message, 'error', 5500),
  }
}
