import api from './api'

// Gọi API backend: /api/nhan-vien (bảng nhan_vien) và /api/vai-tro (bảng vai_tro).
// Dữ liệu nhân viên trả về:
//   { id, ma, hoTen, gioiTinh: 'Nam'|'Nữ', ngaySinh, soDienThoai, email, diaChi, ngayVaoLam ('yyyy-mm-dd'),
//     idVaiTro, vaiTro (tên vai trò), hoatDong }
// Mật khẩu không bao giờ được trả về.
// Thêm mới (POST): gửi đủ hoTen, gioiTinh, ngaySinh, soDienThoai, diaChi, idVaiTro, email, ngayVaoLam, matKhau.
// Sửa (PUT): chỉ gửi hoTen, gioiTinh, ngaySinh, soDienThoai, diaChi, idVaiTro.
//            email, ngayVaoLam, matKhau không được sửa (backend bỏ qua nếu có gửi lên).

export const nhanVienService = {
  async getAll() {
    return (await api.get('/nhan-vien')).data
  },

  /** Danh sách vai trò đang dùng: [{ id, ten }] */
  async getVaiTro() {
    return (await api.get('/vai-tro')).data
  },

  async create(payload) {
    return (await api.post('/nhan-vien', payload)).data
  },

  async update(id, payload) {
    return (await api.put(`/nhan-vien/${id}`, payload)).data
  },

  async toggleActive(id) {
    return (await api.put(`/nhan-vien/${id}/trang-thai`)).data
  },
}
