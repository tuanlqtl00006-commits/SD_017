
export function normalizeText(value = '') {
  return String(value)
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .toLowerCase()
    .trim()
}


export function includesText(haystack, needle) {
  return normalizeText(haystack).includes(normalizeText(needle))
}


export function layTen(danhSach, id) {
  const found = danhSach.find((x) => x.id === id)
  if (found) return found.ten
  return '—'
}


export function layLuaChon(danhSach, idDangChon) {
  return danhSach.filter((x) => x.hoatDong || x.id === idDangChon)
}
