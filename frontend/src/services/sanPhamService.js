import api from './api'

// Gọi API backend cho phần quản lý sản phẩm:
//   /api/san-pham            -> bảng san_pham (+ hinh_anh_san_pham)
//   /api/bien-the-san-pham   -> bảng san_pham_chi_tiet (sản phẩm + màu sắc + trọng lượng + chu vi)
//
// Sản phẩm trả về: { id, ma, ten, idDanhMuc, idThuongHieu, idXuatXu, idChatLieu, idDoCung, idDiemCanBang,
//                    moTa, anhChinh, anhPhu: [url], ngayTao, ngayCapNhat ('yyyy-mm-dd'), hoatDong }
// Biến thể trả về:   { id, ma, idSanPham, idMauSac, idTrongLuong, idChuVi, giaBan, soLuongTon, hoatDong }
// Lỗi nghiệp vụ (trùng mã, trùng biến thể, ...) được api.js đưa vào error.message để trang chỉ cần toast.error(e.message).
// Ngày 2: sản phẩm có getAll, create, update (ngưng / kích hoạt làm ở ngày sau).

export const sanPhamService = {
  async getAll() {
    return (await api.get('/san-pham')).data
  },

  async create(payload) {
    return (await api.post('/san-pham', payload)).data
  },

  // Sửa không đổi mã sản phẩm (backend bỏ qua trường ma khi sửa)
  async update(id, payload) {
    return (await api.put(`/san-pham/${id}`, payload)).data
  },
}

export const bienTheService = {
  async getAll() {
    return (await api.get('/bien-the-san-pham')).data
  },

  async create(payload) {
    return (await api.post('/bien-the-san-pham', payload)).data
  },

  // Sửa không đổi mã và không đổi sản phẩm của biến thể
  async update(id, payload) {
    return (await api.put(`/bien-the-san-pham/${id}`, payload)).data
  },

  async toggleActive(id) {
    return (await api.put(`/bien-the-san-pham/${id}/trang-thai`)).data
  },
}
