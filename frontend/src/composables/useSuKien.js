import { onBeforeUnmount, onMounted } from 'vue'
import { nghe } from '../services/suKien'

/** Nhiều tin sát nhau (một thao tác gọi vài API ghi) gộp thành một lần tải. */
export const TRE_GOP_MS = 150

/**
 * Trong lúc component đang hiện: có tin thay đổi của các chủ đề `chuDe` thì gọi `cb` (gộp các tin cách nhau dưới TRE_GOP_MS).
 * Dùng cho chỗ cần tải lại ngay cả khi đang mở hộp thoại (vd bảng chọn sản phẩm tại quầy); màn danh sách / chi tiết thường
 * dùng useAutoRefresh(..., { chuDe }) để có sẵn quy tắc không đè lên việc người dùng đang làm.
 */
export function useSuKien(chuDe, cb, { treMs = TRE_GOP_MS } = {}) {
  let huy = null
  let hen = null
  onMounted(() => {
    huy = nghe(chuDe, () => {
      clearTimeout(hen)
      hen = setTimeout(cb, treMs)
    })
  })
  onBeforeUnmount(() => {
    huy?.()
    clearTimeout(hen)
  })
}
