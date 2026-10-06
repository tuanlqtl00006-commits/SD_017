import api from './api'


export const nhanVienService = {
  async getAll() {
    return (await api.get('/nhan-vien')).data
  },

  
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
