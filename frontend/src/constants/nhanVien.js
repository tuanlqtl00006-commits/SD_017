// Vai trò lấy từ bảng vai_tro qua API /api/vai-tro nên không khai báo cứng ở đây.
// Tên vai trò quản lý phải khớp VaiTro.TEN_QUAN_LY ở backend.
export const TEN_QUAN_LY = 'Quản lý'

export const GIOI_TINH = ['Nam', 'Nữ']

export const TRANG_THAI = {
  true: { label: 'Hoạt động', cls: 'ad-pill-green' },
  false: { label: 'Ngưng hoạt động', cls: 'ad-pill-red' },
}

/** Mã NV kế tiếp: NV0001, NV0002, ... (chỉ để hiển thị trước, mã thật do backend cấp) */
export function taoMaNhanVien(list = []) {
  const max = list.reduce((m, nv) => {
    const n = Number(String(nv.ma).replace(/\D/g, ''))
    return Number.isNaN(n) ? m : Math.max(m, n)
  }, 0)
  return 'NV' + String(max + 1).padStart(4, '0')
}

/** Số tuổi tính từ 'yyyy-mm-dd'. */
export function tinhTuoi(iso) {
  const [y, m, d] = iso.split('-').map(Number)
  const t = new Date()
  let age = t.getFullYear() - y
  if (t.getMonth() + 1 < m || (t.getMonth() + 1 === m && t.getDate() < d)) age--
  return age
}
