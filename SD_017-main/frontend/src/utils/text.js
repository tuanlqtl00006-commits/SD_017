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
