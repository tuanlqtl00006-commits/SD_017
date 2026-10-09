// Quy tắc kiểm tra ngày sinh khách hàng (dùng chung cho trang Thêm và form Sửa khách hàng)
// Backend (KhachHangController) cũng kiểm tra y hệt, nên nếu đổi số tuổi ở đây thì đổi cả bên đó.
export const KH_TUOI_MIN = 14
export const KH_TUOI_MAX = 120

const pad = (n) => String(n).padStart(2, '0')
const toYmd = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
export const ymdToVN = (ymd) => (ymd ? `${ymd.slice(8, 10)}/${ymd.slice(5, 7)}/${ymd.slice(0, 4)}` : '')

// Ngày cách hôm nay n năm (29/02 mà năm đó không nhuận thì lấy 28/02, giống Java minusYears)
const yearsAgo = (n) => {
  const t = new Date()
  let d = new Date(t.getFullYear() - n, t.getMonth(), t.getDate())
  if (d.getMonth() !== t.getMonth()) d = new Date(t.getFullYear() - n, t.getMonth() + 1, 0)
  return toYmd(d)
}

export const homNay = () => toYmd(new Date())
// Ngày sinh muộn nhất được phép (đủ 14 tuổi tính đến hôm nay)
export const ngaySinhMax = () => yearsAgo(KH_TUOI_MIN)
// Ngày sinh sớm nhất được phép (không quá 120 tuổi)
export const ngaySinhMin = () => yearsAgo(KH_TUOI_MAX)

// Đưa về chuỗi chuẩn yyyy-MM-dd (server có thể trả mảng [y, m, d])
export const chuanHoaNgay = (v) => {
  if (!v) return ''
  if (Array.isArray(v)) return `${v[0]}-${pad(v[1])}-${pad(v[2])}`
  return String(v).substring(0, 10)
}

// Trả về câu báo lỗi, hoặc '' nếu ngày sinh hợp lệ
export const kiemTraNgaySinh = (value) => {
  const ymd = chuanHoaNgay(value)
  if (!ymd) return 'Vui lòng chọn ngày sinh.'

  // Đúng định dạng và là ngày có thật trên lịch (chặn 30/02, 31/04...)
  const mt = /^(\d{4})-(\d{2})-(\d{2})$/.exec(ymd)
  if (!mt) return 'Ngày sinh không đúng định dạng dd/mm/yyyy.'
  const y = +mt[1], m = +mt[2] - 1, d = +mt[3]
  const dt = new Date(y, m, d)
  if (dt.getFullYear() !== y || dt.getMonth() !== m || dt.getDate() !== d) return 'Ngày sinh không tồn tại trên lịch.'

  if (ymd > homNay()) return 'Ngày sinh không được lớn hơn ngày hiện tại.'
  if (ymd > ngaySinhMax()) return `Khách hàng phải từ ${KH_TUOI_MIN} tuổi trở lên.`
  if (ymd < ngaySinhMin()) return `Tuổi tối đa là ${KH_TUOI_MAX} (ngày sinh không được trước ${ymdToVN(ngaySinhMin())}).`
  return ''
}
