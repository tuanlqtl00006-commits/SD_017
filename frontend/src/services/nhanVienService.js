import api from './api'

// Gọi API backend: /api/nhan-vien (bảng nhan_vien) và /api/vai-tro (bảng vai_tro).
// Dữ liệu nhân viên trả về:
//   { id, ma, hoTen, gioiTinh: 'Nam'|'Nữ', ngaySinh, soDienThoai, email, diaChi, ngayVaoLam ('yyyy-mm-dd'),
//     idVaiTro, vaiTro (tên vai trò), hoatDong, canCuoc (số CCCD hoặc null),
//     diaChis: [{ id, tinhThanh, phuongXa, diaChiCuThe, macDinh }] (địa chỉ chính đứng đầu; diaChi = địa chỉ chính ghép một dòng) }
// hoatDong: true = "Còn làm", false = "Đã nghỉ".
// Mật khẩu không bao giờ được trả về.
// Thêm mới (POST): gửi đủ hoTen, gioiTinh, ngaySinh, soDienThoai, canCuoc, diaChis, idVaiTro, email, ngayVaoLam, matKhau.
//            KHÔNG gửi mã: backend tạo mã theo họ tên (Lê Quốc Tuấn -> LQT0001) và trả về trong `ma`.
//            diaChis: id = null là địa chỉ mới, đúng 1 địa chỉ macDinh = true; không có API xóa địa chỉ nhân viên.
// Sửa (PUT): chỉ gửi hoTen, gioiTinh, ngaySinh, soDienThoai, canCuoc, diaChis, idVaiTro.
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

  /** Đổi mật khẩu nhân viên: nhân viên đăng nhập tự đổi mật khẩu cá nhân */
  async doiMatKhau(id, matKhau) {
    return (await api.put(`/nhan-vien/${id}/doi-mat-khau`, { matKhau })).data
  },

  /**
   * Nhập HÀNG LOẠT từ file Excel (.xlsx). chiKiemTra = true: chỉ kiểm tra file, chưa lưu gì.
   * File sai dù chỉ một dòng -> HTTP 422, error.code = 'FILE_KHONG_HOP_LE', error.chiTiet = { loi: [{ dong, truong, cot, thongBao }], tongDong, soDongLoi }.
   * Hợp lệ -> { daLuu, soNhanVien, xemTruoc: [{ dong, ma, hoTen, email, vaiTro }], taiKhoan: [{ ma, hoTen, email, vaiTro, matKhauTam }], maLo, soMail, mailDaCauHinh }.
   */
  async importExcel(file, { chiKiemTra = false } = {}) {
    const data = new FormData()
    data.append('file', file)
    data.append('chiKiemTra', chiKiemTra ? 'true' : 'false')
    return (await api.post('/nhan-vien/import', data, { headers: { 'Content-Type': 'multipart/form-data' } })).data
  },

  /** Tiến độ gửi mail tài khoản của một lần nhập: { tong, daGui, loi, giaLap, xong, lyDo }. */
  async ketQuaMailImport(lo) {
    return (await api.get('/nhan-vien/import/ket-qua-mail', { params: { lo } })).data
  },
}
