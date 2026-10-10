import api from './api'

// Gọi API backend: /api/nhan-vien (bảng nhan_vien) và /api/vai-tro (bảng vai_tro).
// Dữ liệu nhân viên trả về:
//   { id, ma, hoTen, gioiTinh: 'Nam'|'Nữ', ngaySinh, soDienThoai, email, diaChi, ngayVaoLam ('yyyy-mm-dd'),
//     idVaiTro, vaiTro (tên vai trò), hoatDong, cccd (số CCCD hoặc null), viTri: [{ id, ten }] (nhiều vị trí),
//     diaChis: [{ id, tinhThanh, phuongXa, diaChiCuThe, macDinh }] (địa chỉ chính đứng đầu; diaChi = địa chỉ chính ghép một dòng) }
// Mật khẩu không bao giờ được trả về.
// Thêm mới (POST): gửi đủ hoTen, gioiTinh, ngaySinh, soDienThoai, cccd, diaChis, idVaiTro, idViTri[], email, ngayVaoLam, matKhau.
//            KHÔNG gửi mã: backend tạo mã theo họ tên đầy đủ (Nguyễn Văn An -> AnNV01) và trả về trong `ma`.
//            diaChis: id = null là địa chỉ mới, đúng 1 địa chỉ macDinh = true; không có API xóa địa chỉ nhân viên.
// Sửa (PUT): chỉ gửi hoTen, gioiTinh, ngaySinh, soDienThoai, cccd, diaChis, idVaiTro, idViTri[].
//            email, ngayVaoLam, matKhau không được sửa (backend bỏ qua nếu có gửi lên).

export const nhanVienService = {
  async getAll() {
    return (await api.get('/nhan-vien')).data
  },

  /** Danh sách vai trò đang dùng: [{ id, ten }] */
  async getVaiTro() {
    return (await api.get('/vai-tro')).data
  },

  /** Vị trí làm việc đã thêm: [{ id, ten }] (chỉ xem và thêm, không có xóa) */
  async getViTri() {
    return (await api.get('/vi-tri')).data
  },

  /** Thêm vị trí mới: trả về { id, ten } */
  async themViTri(ten) {
    return (await api.post('/vi-tri', { ten })).data
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
