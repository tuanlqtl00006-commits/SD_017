// Các hàm định dạng dùng chung cho các trang quản lý.

const pad = (n) => String(n).padStart(2, '0')

/** Date -> 'yyyy-mm-dd' theo giờ máy (khớp với giá trị của <input type="date">). */
export function toIso(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

export function todayIso() {
  return toIso(new Date())
}

/** Ngày hôm nay cộng/trừ n ngày, trả về 'yyyy-mm-dd'. Dùng để tạo dữ liệu mẫu. */
export function addDaysIso(days) {
  const d = new Date()
  d.setDate(d.getDate() + days)
  return toIso(d)
}

/** 'yyyy-mm-dd' -> 'dd/mm/yyyy' */
export function formatDate(iso) {
  if (!iso) return ''
  const [y, m, d] = iso.split('-')
  return `${d}/${m}/${y}`
}

/** 1500000 -> '1.500.000 ₫' */
export function formatMoney(value) {
  if (value === null || value === undefined || value === '') return ''
  return `${new Intl.NumberFormat('vi-VN').format(value)} ₫`
}

function docNgayGio(v) {
  if (!v) return null
  const d = new Date(v)
  return Number.isNaN(d.getTime()) ? null : d
}

/** Giờ của một mốc thời gian ('2026-10-08T14:39:33' hoặc Date) -> '14:39:33'; không đọc được -> ''. */
export function formatGio(v) {
  const d = docNgayGio(v)
  return d ? `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}` : ''
}

/** Ngày của một mốc thời gian -> '08/10/2026'; không đọc được -> ''. */
export function formatNgay(v) {
  const d = docNgayGio(v)
  return d ? `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}` : ''
}

/** Mốc thời gian -> '14:39:33 08/10/2026'; không đọc được -> '—'. */
export function formatNgayGio(v) {
  return docNgayGio(v) ? `${formatGio(v)} ${formatNgay(v)}` : '—'
}

/** 'Nguyễn Hoàng Long' -> 'NL' (chữ đầu của họ + chữ đầu của tên). */
export function getInitials(name = '') {
  const parts = name.normalize('NFC').trim().split(/\s+/).filter(Boolean)
  if (!parts.length) return '?'
  const first = parts[0][0]
  const last = parts.length > 1 ? parts[parts.length - 1][0] : ''
  return (first + last).toUpperCase()
}

/** Tạo màu nền/chữ ổn định cho avatar chữ cái dựa trên tên. */
export function avatarColors(name = '') {
  let hash = 0
  for (const ch of name) hash = (hash * 31 + ch.codePointAt(0)) % 360
  return {
    backgroundColor: `hsl(${hash} 70% 92%)`,
    color: `hsl(${hash} 55% 30%)`,
  }
}
