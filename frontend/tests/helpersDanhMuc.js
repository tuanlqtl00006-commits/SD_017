import { vi } from 'vitest'
const t = (id, ma, ten, hoatDong = true) => ({ id, ma, ten, hoatDong })
export const THUOC_TINH = {
  'danh-muc': [t(1, 'DM001', 'Vợt cầu lông'), t(2, 'DM002', 'Vợt cầu lông người mới'), t(3, 'DM003', 'Quần áo thể thao'), t(4, 'DM004', 'Túi - balo'), t(5, 'DM005', 'Phụ kiện'), t(6, 'DM006', 'Quả cầu lông')],
  'thuong-hieu': [t(1, 'TH001', 'Yonex')],
  'xuat-xu': [t(1, 'XX001', 'Nhật Bản')],
  'chat-lieu': [t(1, 'CL001', 'Carbon')],
  'do-cung': [t(1, 'DC001', 'Cứng'), t(90, 'DC000', 'Không áp dụng')],
  'diem-can-bang': [t(1, 'CB001', 'Nặng đầu'), t(91, 'CB000', 'Không áp dụng')],
  'mau-sac': [t(1, 'MS001', 'Đen'), t(2, 'MS002', 'Đỏ')],
  'trong-luong': [t(1, 'TL001', '4U (80-84g)'), t(92, 'TL000', 'Không áp dụng')],
  'chu-vi': [t(1, 'CV001', 'G5'), t(93, 'CV000', 'Không áp dụng')],
}
export const SAN_PHAM = [
  { id: 1, ma: 'SP001', ten: 'Astrox 99', idDanhMuc: 1, idThuongHieu: 1, idXuatXu: 1, idChatLieu: 1, idDoCung: 1, idDiemCanBang: 1, hoatDong: true },
  { id: 2, ma: 'SP002', ten: 'Grip Yonex', idDanhMuc: 5, idThuongHieu: 1, idXuatXu: 1, idChatLieu: 1, idDoCung: 90, idDiemCanBang: 91, hoatDong: true },
]
export function mockApi(extra = {}) {
  const calls = { post: [], put: [], get: [] }
  const api = {
    get: vi.fn(async (url, cfg) => {
      calls.get.push([url, cfg])
      if (extra.get) { const r = extra.get(url, cfg); if (r !== undefined) return { data: r } }
      const m = /^\/thuoc-tinh\/(.+)$/.exec(url)
      if (m) return { data: THUOC_TINH[m[1]] }
      if (url === '/san-pham') return { data: SAN_PHAM }
      if (url === '/bien-the-san-pham') return { data: [] }
      return { data: [] }
    }),
    post: vi.fn(async (url, body) => { calls.post.push([url, body]); return { data: { id: 99 } } }),
    put: vi.fn(async (url, body) => { calls.put.push([url, body]); return { data: {} } }),
  }
  return { api, calls }
}
