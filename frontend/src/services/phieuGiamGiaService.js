import api from './api'

// Gọi API backend: /api/phieu-giam-gia (bảng phieu_giam_gia + phieu_giam_gia_khach_hang).
// Dữ liệu trả về có dạng:
//   { id, ma, ten, hinhThuc: 'CONG_KHAI'|'CA_NHAN', loaiGiam: 'PHAN_TRAM'|'TIEN_MAT', giaTri, giamToiDa, donToiThieu,
//     soLuong, soLuongDaDung, gioiHanMoiKhach, ngayBatDau, ngayKetThuc ('yyyy-mm-dd'), hoatDong, khachHangs: [...] }
// Danh sách (getAll) không kèm khachHangs; dùng getById để lấy danh sách khách được tặng của phiếu cá nhân.

export const phieuGiamGiaService = {
  async getAll() {
    return (await api.get('/phieu-giam-gia')).data
  },

  async getById(id) {
    return (await api.get(`/phieu-giam-gia/${id}`)).data
  },

  /** Khách hàng có thể chọn khi tặng phiếu cá nhân: [{ id, ma, hoTen, soDienThoai, email }] */
  async getKhachHang() {
    return (await api.get('/phieu-giam-gia/khach-hang')).data
  },

  async create(payload) {
    return (await api.post('/phieu-giam-gia', payload)).data
  },

  async update(id, payload) {
    return (await api.put(`/phieu-giam-gia/${id}`, payload)).data
  },

  async toggleActive(id) {
    return (await api.put(`/phieu-giam-gia/${id}/trang-thai`)).data
  },
}
