// Vai trò lấy từ bảng vai_tro qua API /api/vai-tro nên không khai báo cứng ở đây.
// Tên vai trò quản lý phải khớp VaiTro.TEN_QUAN_LY ở backend.
import { SO_CHU_SO_TOI_THIEU, dinhDangMa } from '../utils/ma'

export const TEN_QUAN_LY = 'Quản lý'

export const GIOI_TINH = ['Nam', 'Nữ']

export const TRANG_THAI = {
  true: { label: 'Còn làm', cls: 'ad-pill-green' },
  false: { label: 'Đã nghỉ', cls: 'ad-pill-red' },
}

/** Bỏ dấu tiếng Việt (đ -> d) và mọi ký tự không phải chữ cái. Khớp MaNhanVienUtil.boDau ở backend. */
function boDau(s = '') {
  return String(s)
    .normalize('NFD')
    .replace(/\p{M}+/gu, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .replace(/[^A-Za-z\s]/g, ' ')
    .trim()
    .replace(/\s+/g, ' ')
}

/** Phần chữ của mã: chữ cái đầu của MỌI từ trong họ tên. 'Lê Quốc Tuấn' -> 'LQT'. Chưa có chữ cái nào -> ''. */
export function tienToMaNhanVien(hoTen = '') {
  const sach = boDau(hoTen)
  if (!sach) return ''
  return sach.split(' ').map((t) => t[0].toUpperCase()).join('')
}

/** Số chữ số tối thiểu của phần số thứ tự trong mã nhân viên. Khớp MaNhanVienUtil.SO_CHU_SO ở backend. */
export const SO_CHU_SO_MA = SO_CHU_SO_TOI_THIEU

/**
 * Mã nhân viên theo HỌ TÊN: chữ cái đầu của mọi từ + số thứ tự (tối thiểu 2 chữ số, quá 99 thì viết thẳng LQT100).
 *   Lê Quốc Tuấn -> LQT01 (người tiếp theo trùng chữ cái đầu -> LQT02), Nguyễn Văn An -> NVA01.
 * Số thứ tự = số lớn nhất đang dùng cho cùng tiền tố + 1 (đọc được cả mã cũ LQT0001). Mã mẫu NV01..NV10 giữ nguyên.
 * Chỉ để hiển thị trước; mã thật do backend cấp (MaNhanVienUtil) theo cùng quy tắc.
 */
export function taoMaTheoTen(hoTen, maDangCo = []) {
  const tienTo = tienToMaNhanVien(hoTen) || 'NV'
  const key = tienTo.toLowerCase()
  let max = 0
  for (const ma of maDangCo) {
    const m = String(ma ?? '').trim().toLowerCase()
    if (m.startsWith(key) && /^\d{1,9}$/.test(m.slice(key.length))) max = Math.max(max, Number(m.slice(key.length)))
  }
  return dinhDangMa(tienTo, max + 1)
}

/** Số tuổi tính từ 'yyyy-mm-dd'. */
export function tinhTuoi(iso) {
  const [y, m, d] = iso.split('-').map(Number)
  const t = new Date()
  let age = t.getFullYear() - y
  if (t.getMonth() + 1 < m || (t.getMonth() + 1 === m && t.getDate() < d)) age--
  return age
}
