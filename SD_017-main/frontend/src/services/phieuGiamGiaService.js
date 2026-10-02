import { addDaysIso } from '../utils/format'
// import api from './api'
//
// HIỆN TẠI: dữ liệu mẫu lưu trong bộ nhớ (backend chưa có API), tải lại trang sẽ về dữ liệu ban đầu.
// KHI CÓ API: giữ nguyên tên hàm và dữ liệu trả về, chỉ thay phần thân từng hàm bằng lời gọi api, ví dụ:
//   getAll: async () => (await api.get('/phieu-giam-gia')).data

const delay = (ms = 150) => new Promise((resolve) => setTimeout(resolve, ms))

// Ngày tính theo hôm nay để luôn có đủ các trạng thái: đang hoạt động / sắp diễn ra / đã kết thúc / ngưng.
const seed = () => [
  { id: 11, ma: 'VCHBF25SM', ten: 'Black Friday sớm', hinhThuc: 'CONG_KHAI', loaiGiam: 'PHAN_TRAM', giaTri: 25, giamToiDa: 400000, donToiThieu: 2500000, soLuong: 60, ngayBatDau: addDaysIso(40), ngayKetThuc: addDaysIso(45), moTa: 'Giảm 25% cho đơn từ 2.500.000 ₫ trong tuần Black Friday.', hoatDong: false },
  { id: 10, ma: 'VCHG30K01', ten: 'Giảm 30K giày cầu lông', hinhThuc: 'CONG_KHAI', loaiGiam: 'TIEN_MAT', giaTri: 30000, giamToiDa: null, donToiThieu: 600000, soLuong: 80, ngayBatDau: addDaysIso(3), ngayKetThuc: addDaysIso(30), moTa: 'Áp dụng cho các đơn có giày cầu lông.', hoatDong: true },
  { id: 9, ma: 'VCHNEWMB1', ten: 'Chào thành viên mới', hinhThuc: 'CA_NHAN', loaiGiam: 'TIEN_MAT', giaTri: 40000, giamToiDa: null, donToiThieu: 250000, soLuong: 500, ngayBatDau: addDaysIso(-30), ngayKetThuc: addDaysIso(90), moTa: 'Tặng cho khách hàng đăng ký tài khoản lần đầu.', hoatDong: true },
  { id: 8, ma: 'VCHHE12XN', ten: 'Hè xanh - giảm 12%', hinhThuc: 'CONG_KHAI', loaiGiam: 'PHAN_TRAM', giaTri: 12, giamToiDa: 150000, donToiThieu: 800000, soLuong: 120, ngayBatDau: addDaysIso(-90), ngayKetThuc: addDaysIso(-61), moTa: '', hoatDong: true },
  { id: 7, ma: 'VCHDOI100', ten: 'Tặng khách mua vợt đôi', hinhThuc: 'CA_NHAN', loaiGiam: 'TIEN_MAT', giaTri: 100000, giamToiDa: null, donToiThieu: 3000000, soLuong: 30, ngayBatDau: addDaysIso(-15), ngayKetThuc: addDaysIso(45), moTa: 'Dành cho khách mua từ 2 cây vợt trong một đơn.', hoatDong: false },
  { id: 6, ma: 'VCHPK20QC', ten: 'Giảm 20% phụ kiện, quả cầu', hinhThuc: 'CONG_KHAI', loaiGiam: 'PHAN_TRAM', giaTri: 20, giamToiDa: 100000, donToiThieu: 200000, soLuong: 150, ngayBatDau: addDaysIso(-60), ngayKetThuc: addDaysIso(-20), moTa: '', hoatDong: true },
  { id: 5, ma: 'VCHSN26FS', ten: 'Sinh nhật FootStyle', hinhThuc: 'CONG_KHAI', loaiGiam: 'PHAN_TRAM', giaTri: 15, giamToiDa: 300000, donToiThieu: 1000000, soLuong: 200, ngayBatDau: addDaysIso(7), ngayKetThuc: addDaysIso(14), moTa: 'Ưu đãi tuần lễ sinh nhật cửa hàng.', hoatDong: true },
  { id: 4, ma: 'VCHTT50KH', ten: 'Tri ân khách hàng thân thiết', hinhThuc: 'CA_NHAN', loaiGiam: 'TIEN_MAT', giaTri: 50000, giamToiDa: null, donToiThieu: 300000, soLuong: 50, ngayBatDau: addDaysIso(-2), ngayKetThuc: addDaysIso(60), moTa: 'Gửi riêng cho khách đã mua từ 5 đơn.', hoatDong: true },
  { id: 3, ma: 'VCH8DK3SO', ten: 'Giảm sốc cho giày cầu lông', hinhThuc: 'CONG_KHAI', loaiGiam: 'PHAN_TRAM', giaTri: 60, giamToiDa: 500000, donToiThieu: 2000000, soLuong: 20, ngayBatDau: addDaysIso(-45), ngayKetThuc: addDaysIso(-29), moTa: 'Đợt xả kho, đã tạm ngưng.', hoatDong: false },
  { id: 2, ma: 'VCHFS500K', ten: 'Miễn phí vận chuyển đơn từ 500K', hinhThuc: 'CONG_KHAI', loaiGiam: 'TIEN_MAT', giaTri: 30000, giamToiDa: null, donToiThieu: 500000, soLuong: 300, ngayBatDau: addDaysIso(-5), ngayKetThuc: addDaysIso(25), moTa: 'Hỗ trợ phí vận chuyển 30.000 ₫.', hoatDong: true },
  { id: 1, ma: 'VCH7QX2YN', ten: 'Giảm 10% vợt Yonex', hinhThuc: 'CONG_KHAI', loaiGiam: 'PHAN_TRAM', giaTri: 10, giamToiDa: 200000, donToiThieu: 1500000, soLuong: 100, ngayBatDau: addDaysIso(-10), ngayKetThuc: addDaysIso(20), moTa: 'Áp dụng cho toàn bộ vợt Yonex chính hãng.', hoatDong: true },
]

let store = seed()
let nextId = store.reduce((m, p) => Math.max(m, p.id), 0) + 1

export const phieuGiamGiaService = {
  async getAll() {
    await delay()
    return store.map((p) => ({ ...p }))
  },

  async create(payload) {
    await delay()
    if (store.some((p) => p.ma === payload.ma)) throw new Error('Mã phiếu đã tồn tại.')
    const item = { ...payload, id: nextId++, hoatDong: true }
    store = [item, ...store]
    return { ...item }
  },

  async update(id, payload) {
    await delay()
    const index = store.findIndex((p) => p.id === id)
    if (index === -1) throw new Error('Không tìm thấy phiếu giảm giá.')
    store[index] = { ...store[index], ...payload, id, ma: store[index].ma }
    return { ...store[index] }
  },

  async toggleActive(id) {
    await delay()
    const item = store.find((p) => p.id === id)
    if (!item) throw new Error('Không tìm thấy phiếu giảm giá.')
    item.hoatDong = !item.hoatDong
    return { ...item }
  },
}
