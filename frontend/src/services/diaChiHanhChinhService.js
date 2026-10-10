import axios from 'axios'

// Địa chỉ hành chính lấy từ https://provinces.open-api.vn - PHIÊN BẢN 2 (sau sáp nhập tỉnh thành 07/2025):
//   GET /api/v2/p/            danh sách Tỉnh / Thành phố        -> [{ name, code, ... }]
//   GET /api/v2/p/{code}?depth=2  một tỉnh kèm Phường / Xã       -> { name, code, wards: [{ name, code, ... }] }
// Sau sáp nhập không còn cấp Quận/Huyện nên địa chỉ chỉ có Tỉnh/Thành phố -> Phường/Xã.
export const API_DIA_CHI = 'https://provinces.open-api.vn/api/v2'

const cache = { tinh: null, phuong: new Map() }

async function goi(duongDan, params) {
  const res = await axios.get(API_DIA_CHI + duongDan, { params, timeout: 20000 })
  return res.data
}

const soSanh = (a, b) => a.name.localeCompare(b.name, 'vi', { numeric: true })
const gon = (x) => ({ code: x.code, name: x.name })

export const diaChiHanhChinhService = {
  /** Danh sách Tỉnh / Thành phố (34 đơn vị sau sáp nhập), có nhớ lại để không gọi nhiều lần. */
  async layTinhThanh() {
    if (cache.tinh) return cache.tinh
    let ds
    try {
      ds = await goi('/p/')
    } catch {
      ds = await goi('/') // đường dẫn dự phòng của cùng API
    }
    if (!Array.isArray(ds)) throw new Error('Dữ liệu tỉnh/thành không đúng định dạng.')
    cache.tinh = ds.map(gon).sort(soSanh)
    return cache.tinh
  },

  /** Danh sách Phường / Xã thuộc một tỉnh (theo mã tỉnh). */
  async layPhuongXa(maTinh) {
    if (maTinh === undefined || maTinh === null || maTinh === '') return []
    if (cache.phuong.has(maTinh)) return cache.phuong.get(maTinh)
    let ds
    try {
      ds = (await goi(`/p/${maTinh}`, { depth: 2 })).wards
    } catch {
      ds = await goi('/w/', { province: maTinh }) // đường dẫn dự phòng
    }
    if (!Array.isArray(ds)) ds = []
    const ketQua = ds.map(gon).sort(soSanh)
    cache.phuong.set(maTinh, ketQua)
    return ketQua
  },

  /** Chỉ dùng khi kiểm thử: xóa dữ liệu đã nhớ. */
  xoaNho() {
    cache.tinh = null
    cache.phuong.clear()
  },
}
