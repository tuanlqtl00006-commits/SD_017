import api from './api'

// Tải ảnh sản phẩm từ máy lên backend (POST /api/hinh-anh/upload, mỗi lần 1 file).
// Trả về đường dẫn ảnh, ví dụ "/api/uploads/3f2a....jpg", đường dẫn này được lưu cùng sản phẩm.
// Lỗi (ảnh quá lớn, sai định dạng, ...) được api.js đưa vào error.message.
export const hinhAnhService = {
  async upload(file) {
    const data = new FormData()
    data.append('file', file)
    const res = await api.post('/hinh-anh/upload', data, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    return res.data.duongDan
  },
}
