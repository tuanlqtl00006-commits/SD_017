import { onBeforeUnmount, onMounted, watch } from 'vue'
import { useAutoRefresh } from './useAutoRefresh'

/**
 * Giỏ hàng / trang thanh toán: hỏi máy chủ tính tiền (giá đợt hiện tại, phiếu, phí giao) khi mở trang, khi giỏ hoặc mã phiếu đổi
 * (chờ 250ms sau lần đổi cuối) và khi quay lại tab. Số tiền trên màn hình lấy theo cart store (đã ưu tiên số máy chủ).
 */
export function useBaoGiaGioHang(cart) {
  let hen = null
  const hoi = () => {
    clearTimeout(hen)
    hen = setTimeout(() => cart.taiBaoGia(), 250)
  }
  watch(() => [cart.items.map((i) => `${i.bienTheId}:${i.soLuong}`).join(','), cart.appliedVoucher?.ma || '', cart.tinhGiao || ''], hoi)
  onMounted(() => cart.taiBaoGia())
  onBeforeUnmount(() => clearTimeout(hen))
  useAutoRefresh(() => cart.taiBaoGia(), 3000, { chuKyMs: 30_000, chuDe: ['san-pham', 'ton-kho', 'giam-gia', 'phieu-giam-gia'] })
}
