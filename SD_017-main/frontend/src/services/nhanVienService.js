import { taoMaNhanVien } from '../constants/nhanVien'
// import api from './api'
//
// HIỆN TẠI: dữ liệu mẫu lưu trong bộ nhớ (backend chưa có API), tải lại trang sẽ về dữ liệu ban đầu.
// KHI CÓ API: giữ nguyên tên hàm và dữ liệu trả về, chỉ thay phần thân từng hàm bằng lời gọi api, ví dụ:
//   getAll: async () => (await api.get('/nhan-vien')).data
// Ảnh đại diện hiện lưu dạng data URL trong bộ nhớ; khi có API nên gửi file bằng multipart/form-data.

const delay = (ms = 150) => new Promise((resolve) => setTimeout(resolve, ms))

const nv = (id, ma, taiKhoan, hoTen, gioiTinh, ngaySinh, soDienThoai, diaChi, vaiTro, hoatDong = true) => ({
  id, ma, taiKhoan, hoTen, gioiTinh, ngaySinh, soDienThoai, diaChi, vaiTro, hoatDong,
  email: `${taiKhoan}@footstyle.vn`,
  anh: null,
})

const seed = () => [
  nv(1, 'NV0001', 'long.nh', 'Nguyễn Hoàng Long', 'Nam', '1992-03-14', '0912000001', 'Số 15 ngõ 120 Trần Phú, phường Hà Đông, Hà Nội', 'ADMIN'),
  nv(2, 'NV0002', 'ha.tt', 'Trần Thu Hà', 'Nữ', '1994-07-22', '0912000002', 'Số 22 Nguyễn Trãi, quận Thanh Xuân, Hà Nội', 'ADMIN'),
  nv(3, 'NV0003', 'bao.lq', 'Lê Quốc Bảo', 'Nam', '1999-11-05', '0912000003', 'Số 8 đường Lê Văn Lương, quận Thanh Xuân, Hà Nội', 'NHAN_VIEN'),
  nv(4, 'NV0004', 'anh.pm', 'Phạm Minh Anh', 'Nữ', '2000-01-18', '0912000004', 'Số 31 phố Kim Mã, quận Ba Đình, Hà Nội', 'NHAN_VIEN'),
  nv(5, 'NV0005', 'nam.vh', 'Vũ Hoàng Nam', 'Nam', '1998-09-30', '0912000005', 'Thị trấn Quốc Oai, huyện Quốc Oai, Hà Nội', 'NHAN_VIEN'),
  nv(6, 'NV0006', 'trang.dt', 'Đỗ Thùy Trang', 'Nữ', '2001-05-09', '0912000006', 'Số 45 phố Trần Thái Tông, phường Cầu Giấy, Hà Nội', 'NHAN_VIEN'),
  nv(7, 'NV0007', 'manh.bd', 'Bùi Đức Mạnh', 'Nam', '1997-12-02', '0912000007', 'Số 3 ngõ 66 Nguyễn Văn Cừ, quận Long Biên, Hà Nội', 'NHAN_VIEN', false),
  nv(8, 'NV0008', 'chi.nl', 'Ngô Linh Chi', 'Nữ', '2002-04-27', '0912000008', 'Số 19 đường Phạm Văn Đồng, quận Bắc Từ Liêm, Hà Nội', 'NHAN_VIEN'),
  nv(9, 'NV0009', 'anh.ht', 'Hoàng Tuấn Anh', 'Nam', '1996-08-11', '0912000009', 'Số 7 phố Hoàng Quốc Việt, quận Cầu Giấy, Hà Nội', 'NHAN_VIEN'),
  nv(10, 'NV0010', 'huong.dm', 'Đặng Mai Hương', 'Nữ', '1995-02-25', '0912000010', 'Số 52 phố Minh Khai, quận Hai Bà Trưng, Hà Nội', 'NHAN_VIEN', false),
]

let store = seed()
let nextId = store.length + 1

export const nhanVienService = {
  async getAll() {
    await delay()
    return store.map((e) => ({ ...e }))
  },

  async create(payload) {
    await delay()
    const item = { ...payload, id: nextId++, ma: taoMaNhanVien(store), hoatDong: true }
    store = [item, ...store]
    return { ...item }
  },

  async update(id, payload) {
    await delay()
    const index = store.findIndex((e) => e.id === id)
    if (index === -1) throw new Error('Không tìm thấy nhân viên.')
    store[index] = { ...store[index], ...payload, id, ma: store[index].ma }
    return { ...store[index] }
  },

  async toggleActive(id) {
    await delay()
    const item = store.find((e) => e.id === id)
    if (!item) throw new Error('Không tìm thấy nhân viên.')
    if (item.vaiTro === 'ADMIN') throw new Error('Không thể ngưng hoạt động tài khoản quản trị viên.')
    item.hoatDong = !item.hoatDong
    return { ...item }
  },
}
