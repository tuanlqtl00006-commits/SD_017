// Dãy nút phân trang rút gọn theo lời cô: luôn có trang 1, 2 và trang cuối, thêm trang đang xem và hai trang kề nó,
// khoảng hở lớn thì thay bằng dấu ba chấm. Ví dụ 20 trang: 1 2 … 9 10 11 … 20 (đang ở trang 1: 1 2 … 20).
export const DAU_BA_CHAM = '…'

/** @returns {(number|string)[]} số trang hoặc '…' */
export function danhSachTrang(hienTai, tongTrang) {
  const n = Math.max(1, Math.floor(Number(tongTrang)) || 1)
  const cur = Math.min(Math.max(1, Math.floor(Number(hienTai)) || 1), n)
  const giu = [...new Set([1, 2, n, cur - 1, cur, cur + 1].filter((p) => p >= 1 && p <= n))].sort((a, b) => a - b)
  const kq = []
  giu.forEach((p, i) => {
    if (i > 0) {
      const hoCach = p - giu[i - 1]
      if (hoCach === 2) kq.push(p - 1) // chỉ thiếu đúng một trang thì hiện luôn trang đó, khỏi dấu ba chấm
      else if (hoCach > 2) kq.push(DAU_BA_CHAM)
    }
    kq.push(p)
  })
  return kq
}
