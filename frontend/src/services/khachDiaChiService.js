import api from './api'

// Sổ địa chỉ của chính khách hàng đang đăng nhập (backend: /api/khach/dia-chi).
// Mỗi địa chỉ: { id, tenNguoiNhan, sdtNguoiNhan, tinhThanhPho, phuongXa, diaChiCuThe, laDiaChiMacDinh }
export const khachDiaChiService = {
  async getAll() {
    return (await api.get('/khach/dia-chi')).data
  },

  async create(payload) {
    return (await api.post('/khach/dia-chi', payload)).data
  },

  async setDefault(id) {
    return (await api.put(`/khach/dia-chi/${id}/mac-dinh`)).data
  },

  async remove(id) {
    return (await api.delete(`/khach/dia-chi/${id}`)).data
  },
}
