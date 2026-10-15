import api from './api'

// Gọi API backend: /api/khach-hang (bảng khach_hang + địa chỉ trong dia_chi_khach_hang).
// Khách hàng trả về: { id, maKhachHang, hoTen, sdt, email, ngaySinh ('yyyy-mm-dd' hoặc null), gioiTinh (1 Nam / 0 Nữ), trangThai (1 hoạt động / 0 ngừng),
//   ngayTao, ngayCapNhat, diaChiList: [{ id, tenNguoiNhan, sdtNguoiNhan, tinhThanhPho, quanHuyen (cũ), phuongXa, diaChiCuThe, laDiaChiMacDinh }] }
// Thêm mới (POST): hoTen, sdt, email (bắt buộc), ngaySinh (không bắt buộc, nhưng đã nhập phải từ đủ 15 tuổi), gioiTinh, trangThai,
//   và địa chỉ giao hàng đầu tiên: tenNguoiNhan, sdtNguoiNhan, tinhThanh, phuongXa, diaChiCuThe (không gửi quanHuyen: địa chỉ sau sáp nhập).
// Sửa (PUT): hoTen, sdt, email, ngaySinh, gioiTinh, trangThai (địa chỉ sửa riêng qua /api/dia-chi).
// Lỗi kiểm tra dữ liệu trả về 400/409 kèm error.loiTheoTruong = { tenO: 'câu báo lỗi' } (xem services/api.js).
export const khachHangService = {
  async getAll() {
    return (await api.get('/khach-hang')).data
  },

  async getById(id) {
    return (await api.get(`/khach-hang/${id}`)).data
  },

  async create(payload) {
    return (await api.post('/khach-hang', payload)).data
  },

  async update(id, payload) {
    return (await api.put(`/khach-hang/${id}`, payload)).data
  },

  async toggleActive(id) {
    return (await api.put(`/khach-hang/${id}/trang-thai`)).data
  },
}
