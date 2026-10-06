import api from './api'


export const phieuGiamGiaService = {
  async getAll() {
    return (await api.get('/phieu-giam-gia')).data
  },

  async getById(id) {
    return (await api.get(`/phieu-giam-gia/${id}`)).data
  },

  
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
