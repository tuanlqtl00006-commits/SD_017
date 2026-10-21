import api from './api'

// Chat hỗ trợ khách - phía khách (công khai). Mã truy cập của cuộc chat gửi qua header (GET) hoặc body (POST).
export const hoTroKhachApi = {
  batDau: (hoTen, sdt) => api.post('/ho-tro/khach/bat-dau', { hoTen, sdt }).then((r) => r.data),
  xem: (id, ma, sauId) =>
    api.get(`/ho-tro/khach/${id}`, { params: sauId ? { sauId } : {}, headers: { 'X-Ma-Hoi-Thoai': ma } }).then((r) => r.data),
  gui: (id, ma, noiDung) => api.post(`/ho-tro/khach/${id}/tin-nhan`, { ma, noiDung }).then((r) => r.data),
  gapNhanVien: (id, ma) => api.post(`/ho-tro/khach/${id}/gap-nhan-vien`, { ma }).then((r) => r.data),
  ketThuc: (id, ma) => api.post(`/ho-tro/khach/${id}/ket-thuc`, { ma }).then((r) => r.data),
}

// Chat hỗ trợ khách - phía nhân viên.
export const hoTroApi = {
  danhSach: (loc) => api.get('/ho-tro', { params: loc ? { loc } : {} }).then((r) => r.data),
  xem: (id, sauId) => api.get(`/ho-tro/${id}`, { params: sauId ? { sauId } : {} }).then((r) => r.data),
  nhan: (id) => api.post(`/ho-tro/${id}/nhan`).then((r) => r.data),
  gui: (id, noiDung) => api.post(`/ho-tro/${id}/tin-nhan`, { noiDung }).then((r) => r.data),
  dong: (id) => api.post(`/ho-tro/${id}/dong`).then((r) => r.data),
}

// Chat nội bộ giữa nhân viên.
export const chatNoiBoApi = {
  danhSach: () => api.get('/chat/noi-bo').then((r) => r.data),
  nhanVien: () => api.get('/chat/nhan-vien').then((r) => r.data),
  moRieng: (idNhanVien) => api.post('/chat/noi-bo/rieng', { idNhanVien }).then((r) => r.data),
  taoNhom: (tieuDe, idNhanViens) => api.post('/chat/noi-bo/nhom', { tieuDe, idNhanViens }).then((r) => r.data),
  xem: (id, sauId) => api.get(`/chat/noi-bo/${id}`, { params: sauId ? { sauId } : {} }).then((r) => r.data),
  gui: (id, noiDung) => api.post(`/chat/noi-bo/${id}/tin-nhan`, { noiDung }).then((r) => r.data),
}
