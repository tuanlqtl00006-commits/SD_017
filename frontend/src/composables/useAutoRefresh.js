import { onBeforeUnmount, onMounted } from 'vue'

/**
 * Tự gọi lại hàm tải dữ liệu (reload) mỗi khi người dùng quay lại tab trình duyệt,
 * để danh sách luôn khớp với server (vd: ẩn nhân viên ở máy / tab khác thì quay lại đây là thấy ngay).
 * minGapMs: khoảng cách tối thiểu giữa 2 lần tải, tránh gọi liên tục khi 'focus' và 'visibilitychange' nổ cùng lúc.
 */
export function useAutoRefresh(reload, minGapMs = 3000) {
  let lastRun = Date.now()

  function onBack() {
    if (document.visibilityState === 'hidden') return
    if (Date.now() - lastRun < minGapMs) return
    lastRun = Date.now()
    reload()
  }

  onMounted(() => {
    document.addEventListener('visibilitychange', onBack)
    window.addEventListener('focus', onBack)
  })
  onBeforeUnmount(() => {
    document.removeEventListener('visibilitychange', onBack)
    window.removeEventListener('focus', onBack)
  })
}
