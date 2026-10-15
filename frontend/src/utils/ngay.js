// Ngày tháng dạng Việt Nam (dd/mm/yyyy) cho ô chọn ngày dùng chung (DateInput).
// Giá trị lưu / gửi API vẫn là 'yyyy-mm-dd' như <input type="date"> trước đây.
import { toIso } from './format'

const pad = (n) => String(n).padStart(2, '0')

/** 'yyyy-mm-dd' (hoặc 'yyyy-mm-ddTHH:mm:ss' từ API) -> 'dd/mm/yyyy' ('' nếu trống / sai). */
export function isoSangVn(iso) {
  const m = String(iso ?? '').match(/^(\d{4})-(\d{2})-(\d{2})(?:$|[T ])/)
  return m ? `${m[3]}/${m[2]}/${m[1]}` : ''
}

function hopLe(y, m, d) {
  if (y < 1000 || y > 9999 || m < 1 || m > 12 || d < 1) return false
  return d <= new Date(y, m, 0).getDate()
}

/**
 * Chữ người dùng gõ -> 'yyyy-mm-dd'. Nhận 'dd/mm/yyyy', 'd/m/yyyy', dấu '-' hoặc '.', và cả 'yyyy-mm-dd'.
 * Trả '' nếu trống, null nếu chưa đủ / không phải ngày có thật (vd 31/02/2026).
 */
export function vnSangIso(text) {
  const s = String(text ?? '').trim()
  if (s === '') return ''
  let y, m, d
  let k = s.match(/^(\d{4})-(\d{1,2})-(\d{1,2})$/)
  if (k) [y, m, d] = [+k[1], +k[2], +k[3]]
  else {
    k = s.match(/^(\d{1,2})[/.-](\d{1,2})[/.-](\d{4})$/)
    if (!k) return null
    ;[d, m, y] = [+k[1], +k[2], +k[3]]
  }
  return hopLe(y, m, d) ? `${y}-${pad(m)}-${pad(d)}` : null
}

/**
 * Định dạng dần khi gõ: chỉ giữ số, tự chèn '/' -> '13102026' thành '13/10/2026'.
 * Gõ '/' sau ngày / tháng 1 chữ số thì tự thêm 0 ('1/' -> '01/'). Dán 'yyyy-mm-dd' cũng được.
 */
export function dinhDangKhiGo(raw) {
  const s = String(raw ?? '')
  const iso = s.trim().match(/^(\d{4})-(\d{1,2})-(\d{1,2})$/)
  if (iso) return `${iso[3].padStart(2, '0')}/${iso[2].padStart(2, '0')}/${iso[1]}`
  const gioiHan = [2, 2, 4]
  const phan = ['', '', '']
  let i = 0
  for (const ch of s) {
    if (ch >= '0' && ch <= '9') {
      if (phan[i].length >= gioiHan[i]) {
        if (i === 2) break
        i++
      }
      phan[i] += ch
    } else if ('/.- '.includes(ch) && i < 2 && phan[i].length > 0) {
      phan[i] = phan[i].padStart(2, '0')
      i++
    }
  }
  let kq = phan[0]
  if (i >= 1) kq += '/' + phan[1]
  if (i >= 2) kq += '/' + phan[2]
  return kq
}

/** Hôm nay dạng 'yyyy-mm-dd' theo giờ máy. */
export const homNayIso = () => toIso(new Date())

/** 42 ô (6 tuần, bắt đầu thứ Hai) của tháng `thang` (0-11) năm `nam`. */
export function oLich(nam, thang) {
  const dau = new Date(nam, thang, 1)
  const lui = (dau.getDay() + 6) % 7 // thứ Hai = 0
  const kq = []
  for (let k = 0; k < 42; k++) {
    const d = new Date(nam, thang, 1 - lui + k)
    kq.push({ iso: toIso(d), ngay: d.getDate(), trongThang: d.getMonth() === thang })
  }
  return kq
}

/** Cộng / trừ n ngày cho 'yyyy-mm-dd'. */
export function congNgay(iso, n) {
  const [y, m, d] = iso.split('-').map(Number)
  return toIso(new Date(y, m - 1, d + n))
}

/**
 * Ngày từ API (LocalDate) -> 'yyyy-mm-dd'. Nhận chuỗi 'yyyy-mm-dd[Thh:mm:ss]' hoặc mảng [năm, tháng, ngày] (nếu Jackson ghi dạng số).
 * Không đọc được thì trả ''.
 */
export function ngayTuApi(v) {
  if (Array.isArray(v) && v.length >= 3) return `${v[0]}-${pad(v[1])}-${pad(v[2])}`
  const m = String(v ?? '').match(/^(\d{4})-(\d{2})-(\d{2})/)
  return m ? `${m[1]}-${m[2]}-${m[3]}` : ''
}
