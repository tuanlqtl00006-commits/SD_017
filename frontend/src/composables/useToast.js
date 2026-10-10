import { reactive } from 'vue'

const toasts = reactive([])
let seq = 0

function remove(id) {
  const index = toasts.findIndex((t) => t.id === id)
  if (index !== -1) toasts.splice(index, 1)
}

function push(message, type = 'success', timeout = 3500, title = '') {
  const id = ++seq
  toasts.push({ id, message, type, title })
  if (timeout) setTimeout(() => remove(id), timeout)
}

/** Thông báo nổi dùng chung. Hiển thị bởi <ToastHost /> trong MainLayout. */
export function useToast() {
  return {
    toasts,
    remove,
    // title (tùy chọn): dòng đậm phía trên nội dung, vd "Thành công!"
    success: (message, title = '') => push(message, 'success', 3500, title),
    error: (message, title = '') => push(message, 'error', 5500, title),
  }
}
