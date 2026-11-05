import { onBeforeUnmount, onMounted } from 'vue'

/** Chu kỳ tự cập nhật mặc định của các màn danh sách / chi tiết (review 05/11: dữ liệu tự cập nhật nhanh, không cần F5). */
export const CHU_KY_TU_CAP_NHAT_MS = 10_000

/** Người dùng đang thao tác thì KHÔNG tải ngầm (tránh đè form / hộp thoại / ô đang gõ). */
function dangThaoTac() {
  if (typeof document === 'undefined') return true
  if (document.visibilityState === 'hidden') return true
  if (document.querySelector('[aria-modal="true"]')) return true // đang mở hộp thoại (BaseModal...)
  const a = document.activeElement
  return !!a && (a.isContentEditable || /^(INPUT|TEXTAREA|SELECT)$/.test(a.tagName))
}

/**
 * Tự gọi lại hàm tải dữ liệu (reload):
 *  - khi người dùng quay lại tab trình duyệt (như trước);
 *  - và tải ngầm định kỳ mỗi `chuKyMs` (0 = tắt) để dữ liệu mới hiện ra mà không phải F5.
 * Quy tắc tải ngầm (dùng chung cho mọi màn, đừng viết lại ở từng màn):
 *  - không tải khi tab ẩn, khi đang mở hộp thoại, khi con trỏ đang ở ô nhập, hoặc khi `coTheTai()` trả false (form của màn đang mở...);
 *  - lần tải trước chưa xong thì bỏ qua (không gọi chồng);
 *  - `reload` phải là bản tải NGẦM của màn: không hiện "Đang tải", không đổi trang / bộ lọc, có chống kết quả cũ đè kết quả mới.
 * minGapMs: khoảng cách tối thiểu giữa 2 lần tải (focus và visibilitychange hay nổ cùng lúc).
 */
export function useAutoRefresh(reload, minGapMs = 3000, { chuKyMs = CHU_KY_TU_CAP_NHAT_MS, coTheTai = () => true } = {}) {
  let lastRun = Date.now()
  let dangChay = false
  let hen = null

  async function chay() {
    if (dangChay) return
    dangChay = true
    lastRun = Date.now()
    try {
      await reload()
    } catch {
      // tải ngầm lỗi (mất mạng tạm thời): giữ dữ liệu đang hiện, lần sau thử lại
    } finally {
      dangChay = false
    }
  }

  function onBack() {
    if (document.visibilityState === 'hidden') return
    if (Date.now() - lastRun < minGapMs) return
    chay()
  }

  function dinhKy() {
    if (dangThaoTac() || !coTheTai()) return
    if (Date.now() - lastRun < minGapMs) return
    chay()
  }

  onMounted(() => {
    document.addEventListener('visibilitychange', onBack)
    window.addEventListener('focus', onBack)
    if (chuKyMs > 0) hen = setInterval(dinhKy, chuKyMs)
  })
  onBeforeUnmount(() => {
    document.removeEventListener('visibilitychange', onBack)
    window.removeEventListener('focus', onBack)
    clearInterval(hen)
  })
}
