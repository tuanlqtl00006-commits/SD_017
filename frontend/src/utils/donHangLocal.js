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
