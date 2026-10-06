export const TEN_QUAN_LY = 'Quản lý'

export const GIOI_TINH = ['Nam', 'Nữ']

export const TRANG_THAI = {
  true: { label: 'Hoạt động', cls: 'ad-pill-green' },
  false: { label: 'Ngưng hoạt động', cls: 'ad-pill-red' },
}


export function taoMaNhanVien(list = []) {
  const max = list.reduce((m, nv) => {
    const n = Number(String(nv.ma).replace(/\D/g, ''))
    return Number.isNaN(n) ? m : Math.max(m, n)
  }, 0)
  return 'NV' + String(max + 1).padStart(4, '0')
}


export function tinhTuoi(iso) {
  const [y, m, d] = iso.split('-').map(Number)
  const t = new Date()
  let age = t.getFullYear() - y
  if (t.getMonth() + 1 < m || (t.getMonth() + 1 === m && t.getDate() < d)) age--
  return age
}
