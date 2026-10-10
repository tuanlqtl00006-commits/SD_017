// Vai trò lấy từ bảng vai_tro qua API /api/vai-tro nên không khai báo cứng ở đây.
// Tên vai trò quản lý phải khớp VaiTro.TEN_QUAN_LY ở backend.
export const TEN_QUAN_LY = 'Quản lý'

export const GIOI_TINH = ['Nam', 'Nữ']

export const TRANG_THAI = {
  true: { label: 'Hoạt động', cls: 'ad-pill-green' },
  false: { label: 'Ngưng hoạt động', cls: 'ad-pill-red' },
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

/** Phần chữ của mã: Tên + chữ cái đầu của họ và tên đệm. 'Nguyễn Văn An' -> 'AnNV'. Chưa có chữ cái nào -> ''. */
export function tienToMaNhanVien(hoTen = '') {
  const sach = boDau(hoTen)
  if (!sach) return ''
  const tu = sach.split(' ')
  const ten = tu[tu.length - 1]
  return ten[0].toUpperCase() + ten.slice(1).toLowerCase() + tu.slice(0, -1).map((t) => t[0].toUpperCase()).join('')
}

/**
 * Mã nhân viên theo HỌ TÊN ĐẦY ĐỦ: Tên + chữ cái đầu của họ & tên đệm + số thứ tự 2 chữ số.
 *   Nguyễn Văn An -> AnNV01 (người tiếp theo trùng tiền tố -> AnNV02), Trần Thị Mai Lan -> LanTTM01.
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
  return tienTo + String(max + 1).padStart(2, '0')
}

/** Số tuổi tính từ 'yyyy-mm-dd'. */
export function tinhTuoi(iso) {
  const [y, m, d] = iso.split('-').map(Number)
  const t = new Date()
  let age = t.getFullYear() - y
  if (t.getMonth() + 1 < m || (t.getMonth() + 1 === m && t.getDate() < d)) age--
  return age
}
