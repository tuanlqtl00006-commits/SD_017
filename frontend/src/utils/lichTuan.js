// Hàm phụ cho lịch làm việc theo tuần (thứ 2 -> chủ nhật). Ngày dạng 'yyyy-mm-dd' theo giờ máy.
import { toIso } from './format'

export const TEN_THU = ['Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7', 'Chủ nhật']

function docNgay(iso) {
  const [y, m, d] = String(iso).slice(0, 10).split('-').map(Number)
  return new Date(y, m - 1, d)
}

export function congNgayIso(iso, soNgay) {
  const d = docNgay(iso)
  d.setDate(d.getDate() + soNgay)
  return toIso(d)
}

/** Ngày thứ 2 của tuần chứa ngày `iso`. */
export function thu2CuaTuan(iso) {
  const d = docNgay(iso)
  const thu = (d.getDay() + 6) % 7 // thứ 2 = 0 ... chủ nhật = 6
  d.setDate(d.getDate() - thu)
  return toIso(d)
}

/** 7 ngày của tuần bắt đầu từ thứ 2 `thu2`: [{ iso, thu: 'Thứ 2', ngay: '12/10' }]. */
export function ngayTrongTuan(thu2) {
  return Array.from({ length: 7 }, (_, i) => {
    const iso = congNgayIso(thu2, i)
    const [, m, d] = iso.split('-')
    return { iso, thu: TEN_THU[i], ngay: `${d}/${m}` }
  })
}

/** 'Tuần 12/10 - 18/10/2026' */
export function nhanTuan(thu2) {
  const cn = congNgayIso(thu2, 6)
  const [, m1, d1] = thu2.split('-')
  const [y2, m2, d2] = cn.split('-')
  return `Tuần ${d1}/${m1} - ${d2}/${m2}/${y2}`
}

// Màu của từng ca (theo thứ tự ca trong danh sách), dùng chung cho bảng lịch và lịch của tôi
const MAU = [
  { nen: '#e7f1fd', chu: '#075fc0', vien: '#c9def9' },
  { nen: '#fff4e5', chu: '#b45309', vien: '#fde3bd' },
  { nen: '#efe9fd', chu: '#6d28d9', vien: '#ddd2fb' },
  { nen: '#e8f6ee', chu: '#146c43', vien: '#c6ebd5' },
  { nen: '#fdecf3', chu: '#be185d', vien: '#f9cfe0' },
  { nen: '#eef1f5', chu: '#334155', vien: '#dde3ea' },
]
export function mauCa(viTri) {
  const m = MAU[((viTri % MAU.length) + MAU.length) % MAU.length]
  return { backgroundColor: m.nen, color: m.chu, borderColor: m.vien }
}

/** Gom lịch theo khóa `${idNhanVien}|${ngay}` -> [ô lịch], trong ô sắp theo giờ bắt đầu. */
export function gomLich(ds) {
  const kq = {}
  for (const l of ds || []) {
    const k = `${l.idNhanVien}|${l.ngayLam}`
    ;(kq[k] ||= []).push(l)
  }
  for (const k of Object.keys(kq)) kq[k].sort((a, b) => String(a.gioBatDau).localeCompare(String(b.gioBatDau)))
  return kq
}

/** Số người làm từng ca trong từng ngày: { 'yyyy-mm-dd': { idCa: số người } }. */
export function demTheoCa(ds) {
  const kq = {}
  for (const l of ds || []) {
    const ngay = (kq[l.ngayLam] ||= {})
    ngay[l.idCa] = (ngay[l.idCa] || 0) + 1
  }
  return kq
}

/** Hai khoảng giờ 'HH:mm' chồng nhau không (nối tiếp 12:00 - 12:00 không tính là chồng). */
export function chongGio(a, b) {
  return a.gioBatDau < b.gioKetThuc && b.gioBatDau < a.gioKetThuc
}
