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
 * Tìm CHÍNH XÁC từng chữ (dùng cho thương hiệu, xuất xứ - tên đã đăng ký phải gõ đúng):
 *  - Phân biệt chữ HOA / thường: gõ "yonex" không ra "Yonex", phải gõ "Yonex" (hoặc "Y", "Yon"...).
 *  - Phân biệt dấu: gõ "Nhat Ban" không ra "Nhật Bản".
 *  - Chỉ bỏ khoảng trắng thừa ở đầu / cuối / giữa; riêng dấu tiếng Việt gõ kiểu nào (dựng sẵn hay tổ hợp) cũng
 *    được coi là một (chuẩn hóa Unicode NFC) để không bị "gõ đúng mà không ra".
 *  - Ô tìm trống -> khớp tất cả.
 */
const chuanHoaChinhXac = (v) => String(v ?? '').normalize('NFC').replace(/\s+/g, ' ').trim()
export function includesExact(haystack, needle) {
  const k = chuanHoaChinhXac(needle)
  if (!k) return true
  return chuanHoaChinhXac(haystack).includes(k)
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
  if (found && normalizeText(found.ten ?? '') !== 'khong ap dung') return found.ten
  return '—'
}

/**
 * Danh sách cho ô chọn (dropdown): chỉ lấy mục đang hoạt động.
 * Nếu đang sửa mà giá trị cũ đã bị ngưng thì vẫn giữ lại để không bị mất giá trị.
 */
export function layLuaChon(danhSach, idDangChon) {
  // Mục "Không áp dụng" do hệ thống tự gán cho danh mục không phải vợt, không cho chọn tay
  return danhSach.filter((x) => (x.hoatDong || x.id === idDangChon) && normalizeText(x.ten ?? '') !== 'khong ap dung')
}
