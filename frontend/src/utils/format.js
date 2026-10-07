
const pad = (n) => String(n).padStart(2, '0')


export function toIso(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

export function todayIso() {
  return toIso(new Date())
}


export function addDaysIso(days) {
  const d = new Date()
  d.setDate(d.getDate() + days)
  return toIso(d)
}


export function formatDate(iso) {
  if (!iso) return ''
  const [y, m, d] = iso.split('-')
  return `${d}/${m}/${y}`
}


export function formatMoney(value) {
  if (value === null || value === undefined || value === '') return ''
  return `${new Intl.NumberFormat('vi-VN').format(value)} ₫`
}


export function getInitials(name = '') {
  const parts = name.normalize('NFC').trim().split(/\s+/).filter(Boolean)
  if (!parts.length) return '?'
  const first = parts[0][0]
  const last = parts.length > 1 ? parts[parts.length - 1][0] : ''
  return (first + last).toUpperCase()
}


export function avatarColors(name = '') {
  let hash = 0
  for (const ch of name) hash = (hash * 31 + ch.codePointAt(0)) % 360
  return {
    backgroundColor: `hsl(${hash} 70% 92%)`,
    color: `hsl(${hash} 55% 30%)`,
  }
}
