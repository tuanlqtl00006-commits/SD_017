import api from './api'

// Gọi API backend: /api/thuoc-tinh/{loai}, loai = 'danh-muc' | 'thuong-hieu' | 'xuat-xu' | 'chat-lieu' | 'do-cung'
//                                                  | 'diem-can-bang' | 'mau-sac' | 'trong-luong' | 'chu-vi'
// (9 bảng cùng cấu trúc nên dùng chung một API). Dữ liệu trả về: { id, ma, ten, hoatDong }.
// Thuộc tính đã được sản phẩm dùng nên không xóa, chỉ ẩn / hiện (toggleActive).

export const thuocTinhService = {
  async getAll(type) {
    return (await api.get(`/thuoc-tinh/${type}`)).data
  },

  async create(type, payload) {
    return (await api.post(`/thuoc-tinh/${type}`, payload)).data
  },

  // Sửa chỉ đổi tên, mã giữ nguyên
  async update(type, id, payload) {
    return (await api.put(`/thuoc-tinh/${type}/${id}`, payload)).data
  },

  async toggleActive(type, id) {
    return (await api.put(`/thuoc-tinh/${type}/${id}/trang-thai`)).data
  },
}
