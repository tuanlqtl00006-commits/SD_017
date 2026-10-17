import api from './api'

// Gọi API backend cho phần quản lý đợt giảm giá:
//   /api/dot-giam-gia                        -> bảng dot_giam_gia (danh sách trả bản ghi mới nhất lên đầu)
//   /api/dot-giam-gia-chi-tiet/campaign/{id} -> biến thể đang áp dụng trong đợt (bảng dot_giam_gia_chi_tiet)
//
// Đợt giảm giá trả về: { id, maDot, tenDot, phanTramGiamDot, ngayBatDau, ngayKetThuc ('yyyy-mm-ddTHH:mm:ss'), moTa, trangThai (1: đang hoạt động, 0: ngừng), ngayTao }
// Chi tiết trả về:     { id, idSanPhamChiTiet, phanTramGiamBienThe, trangThai }
// Gửi khi thêm / sửa:  { maDot, tenDot, phanTramGiamDot, ngayBatDau, ngayKetThuc, moTa, idBienThe: [id biến thể] }
//   (ngày GIỜ: 'yyyy-mm-ddTHH:mm:ss'; backend nhận cả ngày 'yyyy-mm-dd' và tên cũ idBienThes. Tính theo phút: bắt đầu từ đầu phút
//   bắt đầu, kết thúc tới hết phút kết thúc - xem ThoiGianPhut ở backend)
// Hệ thống không xóa cứng dữ liệu: toggleActive chỉ đổi cột trang_thai.

export const dotGiamGiaService = {
  async getAll() {
    return (await api.get('/dot-giam-gia')).data
  },

  async getById(id) {
    return (await api.get(`/dot-giam-gia/${id}`)).data
  },

  // Mã đợt do hệ thống sinh (DGG + số thứ tự, vd DGG09), không trùng mã đã có
  async maMoi() {
    return (await api.get('/dot-giam-gia/ma-moi')).data.ma
  },

  // Biến thể đang áp dụng trong đợt
  async getBienThe(id) {
    return (await api.get(`/dot-giam-gia-chi-tiet/campaign/${id}`)).data
  },

  // Lịch giảm giá theo khoảng ngày giờ của từng biến thể trong đợt (tính cả các đợt khác chồng lên):
  //   [{ idBienThe, maSpct, tenSanPham, giaBan,
  //      doan: [{ tuLuc, denLuc ('yyyy-mm-ddTHH:mm:ss', đều tính), phanTramApDung, phanTramCaoNhat, phanTramCongDon, phanTramTrungBinh, cacDot, giaSauGiam }] }]
  async getLichGiamGia(id) {
    return (await api.get(`/dot-giam-gia/${id}/lich-giam-gia`)).data
  },

  // Mức giảm đang áp dụng NGAY LÚC NÀY (tính tới giờ phút) của từng biến thể: { idBienThe: phanTram }
  async getApDungHomNay() {
    return (await api.get('/dot-giam-gia/ap-dung-hom-nay')).data
  },

  // Mức giảm của từng biến thể theo các đợt đang bật mà CHƯA kết thúc (đang diễn ra + sắp diễn ra), kèm cách gộp khi chồng nhau:
  //   { chinhSach: 'MAX' | 'CONG_DON' | 'TRUNG_BINH', muc: [{ idBienThe, idDot, maDot, tenDot, phanTram, ngayBatDau, ngayKetThuc ('yyyy-mm-ddTHH:mm:ss') }] }
  async apDung() {
    return (await api.get('/dot-giam-gia/ap-dung')).data
  },

  // Cách gộp khi nhiều đợt chồng nhau (cấu hình ở backend): { cheDo: 'MAX' | 'CONG_DON' | 'TRUNG_BINH', moTa }
  async cheDo() {
    return (await api.get('/dot-giam-gia/che-do')).data
  },

  async doiCheDo(cheDo) {
    return (await api.put('/dot-giam-gia/che-do', { cheDo })).data
  },

  async create(payload) {
    return (await api.post('/dot-giam-gia', payload)).data
  },

  // Sửa không đổi mã đợt. idBienThe gửi lên sẽ thay thế danh sách biến thể áp dụng.
  async update(id, payload) {
    return (await api.put(`/dot-giam-gia/${id}`, payload)).data
  },

  async toggleActive(id) {
    return (await api.put(`/dot-giam-gia/${id}/trang-thai`)).data
  },
}

// Tên cũ của getBienThe (bản trước dùng getChiTiet) - giữ lại để code cũ vẫn chạy
dotGiamGiaService.getChiTiet = dotGiamGiaService.getBienThe
