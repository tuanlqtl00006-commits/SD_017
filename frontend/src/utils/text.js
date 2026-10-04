/** Bỏ dấu tiếng Việt + về chữ thường: 'Nguyễn Đức' -> 'nguyen duc'. */
export function normalizeText(value = '') {
  return String(value)
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .toLowerCase()
    .trim()
}

/** Tìm không phân biệt hoa thường và dấu. */
export function includesText(haystack, needle) {
  return normalizeText(haystack).includes(normalizeText(needle))
}

/** Tìm tên trong một danh sách theo id. Không thấy thì trả '—'. */
export function layTen(danhSach, id) {
  const found = danhSach.find((x) => x.id === id)
  if (found) return found.ten
  return '—'
}

/**
 * Danh sách cho ô chọn (dropdown): chỉ lấy mục đang hoạt động.
 * Nếu đang sửa mà giá trị cũ đã bị ngưng thì vẫn giữ lại để không bị mất giá trị.
 */
export function layLuaChon(danhSach, idDangChon) {
  return danhSach.filter((x) => x.hoatDong || x.id === idDangChon)
}
