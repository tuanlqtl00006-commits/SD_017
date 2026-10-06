import api from './api'


export const sanPhamService = {
  async getAll() {
    return (await api.get('/san-pham')).data
  },

  async create(payload) {
    return (await api.post('/san-pham', payload)).data
  },

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

  async update(id, payload) {
    return (await api.put(`/bien-the-san-pham/${id}`, payload)).data
  },

  async toggleActive(id) {
    return (await api.put(`/bien-the-san-pham/${id}/trang-thai`)).data
  },
}
