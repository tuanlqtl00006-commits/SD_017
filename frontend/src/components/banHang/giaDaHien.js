// Giá mỗi biến thể mà bảng "Chọn sản phẩm" ở quầy đã hiện cho nhân viên (góp ý 23/11): đợt giảm giá / giá đổi thì bảng
// KHÔNG nhảy giá ngay, vẫn hiện giá này; nhân viên bấm Thêm -> hỏi "sản phẩm đã đổi giá" -> OK mới nhận giá mới.
// Để ngoài component để đóng / mở lại hộp chọn vẫn nhớ (tải lại trang thì nhớ lại từ đầu).
import { reactive } from 'vue'

export const giaDaHien = reactive({})

/** Phần giá cần nhớ của một dòng trong bảng. */
export const phanGia = (sp) => ({ donGia: Number(sp.donGia), giaGoc: Number(sp.giaGoc) || Number(sp.donGia), phanTramGiam: Number(sp.phanTramGiam) || 0 })

/** Chỉ dùng trong test. */
export function xoaGiaDaHien() {
  for (const k of Object.keys(giaDaHien)) delete giaDaHien[k]
}
