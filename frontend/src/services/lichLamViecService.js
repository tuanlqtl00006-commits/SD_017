import api from './api'

// Ca làm việc (/api/ca-lam-viec) + lịch làm việc (/api/lich-lam-viec) + giao ca (/api/giao-ca)
export const lichLamViecService = {
  dsCa: () => api.get('/ca-lam-viec').then((r) => r.data),
  themCa: (body) => api.post('/ca-lam-viec', body).then((r) => r.data),
  suaCa: (id, body) => api.put(`/ca-lam-viec/${id}`, body).then((r) => r.data),
  doiTrangThaiCa: (id) => api.put(`/ca-lam-viec/${id}/trang-thai`).then((r) => r.data),

  lich: (tuNgay, denNgay, idNhanVien) => api.get('/lich-lam-viec', { params: { tuNgay, denNgay, idNhanVien } }).then((r) => r.data),
  lichCuaToi: (tuNgay, denNgay) => api.get('/lich-lam-viec/cua-toi', { params: { tuNgay, denNgay } }).then((r) => r.data),
  nhanVien: () => api.get('/lich-lam-viec/nhan-vien').then((r) => r.data),
  xepLich: (body) => api.post('/lich-lam-viec', body).then((r) => r.data),
  xoaLich: (id) => api.delete(`/lich-lam-viec/${id}`).then((r) => r.data),
  saoChepTuan: (tuanNguon, tuanDich) => api.post('/lich-lam-viec/sao-chep-tuan', { tuanNguon, tuanDich }).then((r) => r.data),

  lichSuGiaoCa: (params) => api.get('/giao-ca', { params }).then((r) => r.data),
  chiTietGiaoCa: (id) => api.get(`/giao-ca/${id}`).then((r) => r.data),
}
