import { layPhien } from '../services/phien'

// Đơn hàng khách đặt online được lưu tạm trên trình duyệt, TÁCH RIÊNG THEO TỪNG TÀI KHOẢN
// (đăng xuất / đổi tài khoản thì không thấy đơn của người khác). Khách chưa đăng nhập dùng khóa "khach".
const TIEN_TO = 'footstyle_orders'

export function khoaDonHang() {
  const nd = layPhien()?.nguoiDung
  return nd?.id ? `${TIEN_TO}_kh${nd.id}` : `${TIEN_TO}_khach`
}

export function docDonHang() {
  try {
    const raw = localStorage.getItem(khoaDonHang())
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

export function ghiDonHang(ds) {
  try {
    localStorage.setItem(khoaDonHang(), JSON.stringify(ds))
  } catch {
    // trình duyệt chặn bộ nhớ: bỏ qua
  }
}

/** Mọi đơn lưu trên máy (mọi tài khoản) - chỉ dùng cho tra cứu theo mã đơn. */
export function docTatCaDonHang() {
  const kq = []
  try {
    for (let i = 0; i < localStorage.length; i++) {
      const k = localStorage.key(i)
      if (k && k.startsWith(TIEN_TO)) {
        const ds = JSON.parse(localStorage.getItem(k) || '[]')
        if (Array.isArray(ds)) kq.push(...ds)
      }
    }
  } catch {
    // bỏ qua dữ liệu hỏng
  }
  return kq
}

// Trạng thái của backend (0 hủy, 1 chờ xác nhận, 2 đã xác nhận, 3 chờ lấy hàng, 4 đang giao, 5 đã giao, 6 hoàn thành, 8 giao thất bại)
// -> trạng thái dùng ở trang Tài khoản (0 chờ xác nhận, 1 đã xác nhận, 2 đang giao, 3 hoàn thành, 6 đã hủy)
const TRANG_THAI_TU_BACKEND = {
  0: [6, 'Đã hủy'],
  1: [0, 'Chờ xác nhận'],
  2: [1, 'Đã xác nhận'],
  3: [1, 'Chờ lấy hàng'],
  4: [2, 'Đang giao hàng'],
  5: [2, 'Đã giao hàng'],
  6: [3, 'Hoàn thành'],
  8: [2, 'Giao hàng thất bại'],
}

/**
 * Đơn lưu trên máy chỉ là bản sao lúc đặt; trạng thái thật do cửa hàng cập nhật ở backend.
 * Hỏi backend (đường dẫn công khai tra cứu) rồi cập nhật trạng thái từng đơn, lưu lại. Trả về danh sách mới.
 * `hoiBackend(maHoaDon)` trả về dữ liệu tra cứu của backend (hoặc ném lỗi nếu không có đơn / mất kết nối thì giữ nguyên).
 */
export async function dongBoTrangThaiDon(ds, hoiBackend) {
  let doi = false
  await Promise.all(
    ds.map(async (o) => {
      try {
        const d = await hoiBackend(o.maHoaDon)
        const m = TRANG_THAI_TU_BACKEND[Number(d?.trangThai)]
        if (m && (o.trangThai !== m[0] || o.trangThaiText !== m[1])) {
          o.trangThai = m[0]
          o.trangThaiText = m[1]
          doi = true
        }
      } catch {
        // không có đơn trên backend (đơn cũ chỉ lưu trên máy) hoặc mất kết nối: giữ trạng thái đang lưu
      }
    }),
  )
  if (doi) ghiDonHang(ds)
  return ds
}
