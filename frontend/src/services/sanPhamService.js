import api from './api'

// Gọi API backend cho phần quản lý sản phẩm:
//   /api/san-pham            -> bảng san_pham (+ hinh_anh_san_pham)
//   /api/bien-the-san-pham   -> bảng san_pham_chi_tiet (sản phẩm + màu sắc + trọng lượng + chu vi)
//
// Sản phẩm trả về: { id, ma, ten, idDanhMuc, idThuongHieu, idXuatXu, idChatLieu, idDoCung, idDiemCanBang,
//                    moTa, anhChinh, anhPhu: [url], ngayTao, ngayCapNhat ('yyyy-mm-dd'), hoatDong }
// Biến thể trả về:   { id, ma, idSanPham, idMauSac, idTrongLuong, idChuVi, giaBan, soLuongTon, hoatDong }
// Lỗi nghiệp vụ (trùng mã, trùng biến thể, ...) được api.js đưa vào error.message để trang chỉ cần toast.error(e.message).
// Hệ thống không xóa cứng dữ liệu: toggleActive chỉ đổi cột trang_thai.

export const sanPhamService = {
  async getAll() {
    return (await api.get('/san-pham')).data
  },

  async getById(id) {
    return (await api.get(`/san-pham/${id}`)).data
  },

  async create(payload) {
    return (await api.post('/san-pham', payload)).data
  },

  // Sửa không đổi mã sản phẩm (backend bỏ qua trường ma khi sửa)
  async update(id, payload) {
    return (await api.put(`/san-pham/${id}`, payload)).data
  },

  async toggleActive(id) {
    return (await api.put(`/san-pham/${id}/trang-thai`)).data
  },
}

export const bienTheService = {
  async getAll() {
    return (await api.get('/bien-the-san-pham')).data
  },

  async create(payload) {
    return (await api.post('/bien-the-san-pham', payload)).data
  },

  // Sửa được đổi sản phẩm của biến thể; mã không sửa tay (backend chỉ đổi phần đầu mã theo sản phẩm mới, kiểm tra trùng mã và trùng bộ màu/trọng lượng/chu vi)
  async update(id, payload) {
    return (await api.put(`/bien-the-san-pham/${id}`, payload)).data
  },

  async toggleActive(id) {
    return (await api.put(`/bien-the-san-pham/${id}/trang-thai`)).data
  },
}
