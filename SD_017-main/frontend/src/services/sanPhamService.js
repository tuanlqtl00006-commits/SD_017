import { addDaysIso } from '../utils/format'
// import api from './api'
//
// HIỆN TẠI: dữ liệu mẫu lưu trong bộ nhớ (backend chưa có API). Các id_* trỏ tới dữ liệu mẫu của thuocTinhService
// (theo thứ tự seed trong constants/thuocTinh.js).
// KHI CÓ API: giữ nguyên tên hàm và dữ liệu trả về, chỉ thay phần thân bằng lời gọi api, ví dụ:
//   getAll: async () => (await api.get('/san-pham')).data

const delay = (ms = 150) => new Promise((resolve) => setTimeout(resolve, ms))

/* =================== SẢN PHẨM (bảng san_pham) =================== */

const sp = (id, ma, ten, idDanhMuc, idThuongHieu, idXuatXu, idChatLieu, idDoCung, idDiemCanBang, moTa, ngay) => ({
  id, ma, ten, idDanhMuc, idThuongHieu, idXuatXu, idChatLieu, idDoCung, idDiemCanBang, moTa,
  anhChinh: '', hoatDong: true, ngayTao: addDaysIso(ngay), ngayCapNhat: addDaysIso(ngay),
})

let sanPhams = [
  sp(6, 'SP006', 'Kumpoo Power Control', 1, 5, 3, 2, 2, 2, 'Vợt công thủ toàn diện, phù hợp người mới chơi.', -4),
  sp(5, 'SP005', 'Mizuno Wave Fang Pro', 2, 4, 1, 4, 2, 2, 'Giày cầu lông đế êm, bám sân tốt.', -12),
  sp(4, 'SP004', 'Yonex Power Cushion 65Z3', 2, 1, 1, 4, 2, 2, 'Giày cầu lông đệm Power Cushion giảm chấn.', -20),
  sp(3, 'SP003', 'Victor Thruster K 9900', 1, 3, 3, 2, 3, 1, 'Vợt thiên công, nặng đầu, đập cầu mạnh.', -30),
  sp(2, 'SP002', 'Lining Axforce 80', 1, 2, 2, 1, 4, 1, 'Vợt dành cho người chơi lực tay tốt.', -45),
  sp(1, 'SP001', 'Yonex Astrox 99 Pro', 1, 1, 1, 1, 3, 1, 'Vợt thiên công cao cấp, khung carbon.', -60),
]
let nextSanPhamId = 7

/* =================== BIẾN THỂ (bảng san_pham_chi_tiet) =================== */

const bt = (id, ma, idSanPham, idMauSac, idTrongLuong, idChuVi, giaBan, soLuongTon) => ({
  id, ma, idSanPham, idMauSac, idTrongLuong, idChuVi, giaBan, soLuongTon, hoatDong: true,
})

let bienThes = [
  bt(14, 'SP006-DEN-4U-G5', 6, 1, 3, 2, 1250000, 18),
  bt(13, 'SP005-TRG-40', 5, 2, 1, 1, 2350000, 9),
  bt(12, 'SP004-DEN-40', 4, 1, 1, 1, 2790000, 12),
  bt(11, 'SP003-DO-3U-G5', 3, 3, 2, 2, 3150000, 7),
  bt(10, 'SP003-DEN-4U-G5', 3, 1, 3, 2, 3150000, 10),
  bt(9, 'SP002-XDG-3U-G5', 2, 4, 2, 2, 2850000, 6),
  bt(8, 'SP002-DEN-4U-G4', 2, 1, 3, 1, 2850000, 11),
  bt(7, 'SP002-DEN-4U-G5', 2, 1, 3, 2, 2850000, 14),
  bt(6, 'SP001-VANG-4U-G6', 1, 5, 3, 3, 4290000, 0),
  bt(5, 'SP001-DO-3U-G5', 1, 3, 2, 2, 4290000, 5),
  bt(4, 'SP001-DO-4U-G5', 1, 3, 3, 2, 4290000, 8),
  bt(3, 'SP001-DEN-3U-G5', 1, 1, 2, 2, 4290000, 15),
  bt(2, 'SP001-DEN-4U-G4', 1, 1, 3, 1, 4290000, 10),
  bt(1, 'SP001-DEN-4U-G5', 1, 1, 3, 2, 4290000, 20),
]
let nextBienTheId = 15

const copy = (x) => ({ ...x })
const now = () => addDaysIso(0)

export const sanPhamService = {
  async getAll() {
    await delay()
    return sanPhams.map(copy)
  },

  async create(payload) {
    await delay()
    if (sanPhams.some((p) => p.ma === payload.ma)) throw new Error('Mã sản phẩm đã tồn tại.')
    const item = { ...payload, id: nextSanPhamId++, hoatDong: true, ngayTao: now(), ngayCapNhat: now() }
    sanPhams = [item, ...sanPhams]
    return copy(item)
  },

  async update(id, payload) {
    await delay()
    const index = sanPhams.findIndex((p) => p.id === id)
    if (index === -1) throw new Error('Không tìm thấy sản phẩm.')
    sanPhams[index] = { ...sanPhams[index], ...payload, id, ma: sanPhams[index].ma, ngayCapNhat: now() }
    return copy(sanPhams[index])
  },

  async toggleActive(id) {
    await delay()
    const item = sanPhams.find((p) => p.id === id)
    if (!item) throw new Error('Không tìm thấy sản phẩm.')
    item.hoatDong = !item.hoatDong
    item.ngayCapNhat = now()
    return copy(item)
  },
}

export const bienTheService = {
  async getAll() {
    await delay()
    return bienThes.map(copy)
  },

  async create(payload) {
    await delay()
    if (bienThes.some((b) => b.ma === payload.ma)) throw new Error('Mã biến thể đã tồn tại.')
    if (bienThes.some((b) => sameCombo(b, payload))) throw new Error('Biến thể (màu sắc, trọng lượng, chu vi) này đã có trong sản phẩm.')
    const item = { ...payload, id: nextBienTheId++, hoatDong: true }
    bienThes = [item, ...bienThes]
    return copy(item)
  },

  async update(id, payload) {
    await delay()
    const index = bienThes.findIndex((b) => b.id === id)
    if (index === -1) throw new Error('Không tìm thấy biến thể.')
    if (bienThes.some((b) => b.id !== id && sameCombo(b, payload))) throw new Error('Biến thể (màu sắc, trọng lượng, chu vi) này đã có trong sản phẩm.')
    bienThes[index] = { ...bienThes[index], ...payload, id, ma: bienThes[index].ma }
    return copy(bienThes[index])
  },

  async toggleActive(id) {
    await delay()
    const item = bienThes.find((b) => b.id === id)
    if (!item) throw new Error('Không tìm thấy biến thể.')
    item.hoatDong = !item.hoatDong
    return copy(item)
  },
}

const sameCombo = (a, b) =>
  a.idSanPham === b.idSanPham && a.idMauSac === b.idMauSac && a.idTrongLuong === b.idTrongLuong && a.idChuVi === b.idChuVi
