import { onBeforeUnmount, onMounted } from 'vue'
import { dangKetNoi, nghe } from '../services/suKien'
import { TRE_GOP_MS } from './useSuKien'

/** Chu kỳ tự cập nhật mặc định của các màn danh sách / chi tiết (review 05/11: dữ liệu tự cập nhật nhanh, không cần F5). */
export const CHU_KY_TU_CAP_NHAT_MS = 10_000
/** Màn có nghe kênh báo thay đổi (chuDe) và kênh đang sống: chỉ hỏi lại định kỳ thưa thế này để phòng sót. */
export const CHU_KY_DU_PHONG_MS = 60_000
/** Có tin thay đổi mà người dùng đang thao tác: cứ chừng này kiểm tra lại, thao tác xong thì tải. */
const CHO_THAO_TAC_MS = 1000

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
 *  - NGAY khi có tin thay đổi của các chủ đề `chuDe` (kênh báo thay đổi services/suKien.js: bán quầy, đặt online, đổi trạng thái đơn...);
 *  - khi người dùng quay lại tab trình duyệt;
 *  - và tải ngầm định kỳ mỗi `chuKyMs` (0 = tắt); màn có `chuDe` mà kênh đang sống thì chỉ 60 giây một lần để phòng sót.
 * Quy tắc tải ngầm (dùng chung cho mọi màn, đừng viết lại ở từng màn):
 *  - không tải khi tab ẩn, khi đang mở hộp thoại, khi con trỏ đang ở ô nhập, hoặc khi `coTheTai()` trả false (form của màn đang mở...);
 *    có tin thay đổi lúc đó thì chờ người dùng thao tác xong rồi tải (không bỏ mất tin);
 *  - lần tải trước chưa xong thì không gọi chồng (có tin mới trong lúc đang tải thì tải thêm một lần sau khi xong);
 *  - `reload` phải là bản tải NGẦM của màn: không hiện "Đang tải", không đổi trang / bộ lọc, có chống kết quả cũ đè kết quả mới.
 * minGapMs: khoảng cách tối thiểu giữa 2 lần tải (focus và visibilitychange hay nổ cùng lúc); tin thay đổi không bị giới hạn này.
 */
export function useAutoRefresh(reload, minGapMs = 3000, { chuKyMs = CHU_KY_TU_CAP_NHAT_MS, coTheTai = () => true, chuDe = null } = {}) {
  let lastRun = Date.now()
  let dangChay = false
  let taiThem = false
  let hen = null
  let henGop = null
  let henCho = null
  let huyNghe = null
  let daRoi = false

  async function chay(theoTin = false) {
    if (dangChay) {
      if (theoTin) taiThem = true
      return
    }
    clearTimeout(henCho)
    dangChay = true
    lastRun = Date.now()
    try {
      await reload()
    } catch {
      // tải ngầm lỗi (mất mạng tạm thời): giữ dữ liệu đang hiện, lần sau thử lại
    } finally {
      dangChay = false
      if (taiThem) {
        taiThem = false
        khiCoTin()
      }
    }
  }

  function khiCoTin() {
    clearTimeout(henCho)
    if (daRoi) return
    if (dangThaoTac() || !coTheTai()) {
      henCho = setTimeout(khiCoTin, CHO_THAO_TAC_MS)
      return
    }
    chay(true)
  }

  function onBack() {
    if (document.visibilityState === 'hidden') return
    if (Date.now() - lastRun < minGapMs) return
    chay()
  }

  function dinhKy() {
    if (dangThaoTac() || !coTheTai()) return
    if (Date.now() - lastRun < minGapMs) return
    if (chuDe && dangKetNoi.value && Date.now() - lastRun < CHU_KY_DU_PHONG_MS) return
    chay()
  }

  onMounted(() => {
    document.addEventListener('visibilitychange', onBack)
    window.addEventListener('focus', onBack)
    if (chuKyMs > 0) hen = setInterval(dinhKy, chuKyMs)
    if (chuDe) {
      huyNghe = nghe(chuDe, () => {
        clearTimeout(henGop)
        henGop = setTimeout(khiCoTin, TRE_GOP_MS)
      })
    }
  })
  onBeforeUnmount(() => {
    daRoi = true
    document.removeEventListener('visibilitychange', onBack)
    window.removeEventListener('focus', onBack)
    clearInterval(hen)
    clearTimeout(henGop)
    clearTimeout(henCho)
    huyNghe?.()
  })
}
