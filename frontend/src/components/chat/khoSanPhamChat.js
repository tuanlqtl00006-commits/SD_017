// Danh sách sản phẩm cửa hàng cho các thẻ sản phẩm trong khung chat: tải 1 lần dùng chung cho mọi tin, lỗi thì lần sau tải lại.
import { cuaHangService } from '../../services/cuaHangService'

let hua = null

export function taiSanPhamChat() {
  if (!hua) {
    hua = cuaHangService
      .getDanhSachSanPham()
      .then((ds) => (Array.isArray(ds) ? ds : []))
      .catch((e) => {
        hua = null
        throw e
      })
  }
  return hua
}

/** Chỉ dùng trong test: xóa bộ nhớ tạm. */
export function xoaKhoSanPhamChat() {
  hua = null
}
