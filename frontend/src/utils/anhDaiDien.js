// Kiểm tra nhanh file ảnh đại diện trước khi tải lên (backend HinhAnhService kiểm tra lại: dung lượng + đọc byte đầu để biết đúng định dạng).
export const KIEU_ANH_DAI_DIEN = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']
export const ANH_DAI_DIEN_TOI_DA_MB = 5 // giống spring.servlet.multipart.max-file-size

/** Trả về câu báo lỗi, hoặc '' nếu file dùng được làm ảnh đại diện. */
export function loiFileAnhDaiDien(file) {
  if (!file) return 'Chưa chọn ảnh.'
  if (!KIEU_ANH_DAI_DIEN.includes(file.type)) return 'Chỉ nhận ảnh JPG, PNG, WEBP hoặc GIF.'
  if (file.size > ANH_DAI_DIEN_TOI_DA_MB * 1024 * 1024) return `Ảnh lớn hơn ${ANH_DAI_DIEN_TOI_DA_MB} MB.`
  return ''
}
