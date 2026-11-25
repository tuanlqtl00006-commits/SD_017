// Màn hình khách hàng ở quầy (góp ý 25/11): màn bán hàng gửi "ảnh chụp" hóa đơn đang chọn sang một cửa sổ riêng quay về phía khách.
// Hai cửa sổ cùng trình duyệt nên dùng BroadcastChannel (cập nhật ngay), kèm lưu bản mới nhất vào localStorage để cửa sổ vừa mở
// (hoặc F5) có ngay dữ liệu; trình duyệt không có BroadcastChannel thì nghe sự kiện "storage" của localStorage.
// Màn khách chỉ hiển thị, không gọi API.

export const KENH_MAN_KHACH = 'footstyle-man-hinh-khach'
export const KHOA_MAN_KHACH = 'footstyle_man_hinh_khach'

function moKenh() {
  try {
    return typeof BroadcastChannel === 'function' ? new BroadcastChannel(KENH_MAN_KHACH) : null
  } catch {
    return null
  }
}

let kenhGui = null

/** Màn quầy gọi mỗi khi hóa đơn đang chọn đổi (thêm / xóa / đổi số lượng / đổi phiếu / chuyển hóa đơn chờ). */
export function guiManHinhKhach(banChup) {
  const goi = { ...banChup, luc: Date.now() }
  try {
    localStorage.setItem(KHOA_MAN_KHACH, JSON.stringify(goi))
  } catch {
    // trình duyệt chặn lưu: vẫn gửi qua kênh
  }
  if (!kenhGui) kenhGui = moKenh()
  try {
    kenhGui?.postMessage(goi)
  } catch {
    // kênh đã đóng: lần sau mở lại
    kenhGui = null
  }
}

/** Bản mới nhất đã lưu (cửa sổ khách vừa mở). */
export function docManHinhKhach() {
  try {
    const s = localStorage.getItem(KHOA_MAN_KHACH)
    return s ? JSON.parse(s) : null
  } catch {
    return null
  }
}

/** Màn khách nghe thay đổi; trả về hàm hủy nghe. */
export function ngheManHinhKhach(khiCo) {
  const kenh = moKenh()
  const quaKenh = (e) => khiCo(e.data)
  const quaStorage = (e) => {
    if (e.key !== KHOA_MAN_KHACH || !e.newValue) return
    try {
      khiCo(JSON.parse(e.newValue))
    } catch {
      // dữ liệu hỏng: bỏ qua
    }
  }
  if (kenh) kenh.addEventListener('message', quaKenh)
  window.addEventListener('storage', quaStorage)
  return () => {
    if (kenh) {
      kenh.removeEventListener('message', quaKenh)
      kenh.close()
    }
    window.removeEventListener('storage', quaStorage)
  }
}
