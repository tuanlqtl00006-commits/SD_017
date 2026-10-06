import api from './api'


export const thuocTinhService = {
  async getAll(type) {
    return (await api.get(`/thuoc-tinh/${type}`)).data
  },

  async create(type, payload) {
    return (await api.post(`/thuoc-tinh/${type}`, payload)).data
  },

  async update(type, id, payload) {
    return (await api.put(`/thuoc-tinh/${type}/${id}`, payload)).data
  },

  async toggleActive(type, id) {
    return (await api.put(`/thuoc-tinh/${type}/${id}/trang-thai`)).data
  },
}
