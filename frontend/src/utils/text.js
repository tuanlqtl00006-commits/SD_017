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

/**
 * Tìm theo TỪ (không phân biệt hoa thường và dấu), dùng cho ô tìm kiếm người / mã:
 *  - Chữ (vd "anh", "nv0009"): phải là phần ĐẦU của một từ -> "Anh" ra "Hoàng Tuấn Anh", "Ánh", không ra "Mạnh" (manh có chứa anh).
 *  - Số điện thoại (toàn chữ số) hoặc có ký tự . @ - _ : tìm ở bất kỳ vị trí nào.
 *  - Gõ nhiều từ ("tuan anh"): mọi từ đều phải khớp.
 */
export function matchesWords(haystack, needle) {
  const keywords = normalizeText(needle).split(/\s+/).filter(Boolean)
  if (!keywords.length) return true
  const text = normalizeText(haystack)
  const words = text.split(/[^a-z0-9]+/).filter(Boolean)
  return keywords.every((k) =>
    /^[a-z0-9]*[a-z][a-z0-9]*$/.test(k) ? words.some((w) => w.startsWith(k)) : text.includes(k),
  )
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
