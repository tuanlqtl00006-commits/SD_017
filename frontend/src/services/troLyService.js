import api from './api'

// Trợ lý AI nội bộ (/api/tro-ly): trả lời từ dữ liệu thật của cửa hàng, phân quyền theo vai trò ở backend
export const troLyService = {
  chao: () => api.get('/tro-ly/chao').then((r) => r.data),
  hoi: (cauHoi) => api.post('/tro-ly/hoi', { cauHoi }).then((r) => r.data),
}
