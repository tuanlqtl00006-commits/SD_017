// Quy tắc kiểm tra + chuẩn hóa thông tin khách hàng và địa chỉ giao hàng.
// Giống backend: backend/src/main/java/com/footstyle/demo/service/KhachHangRules.java (sửa quy tắc thì sửa cả hai).
// Mỗi hàm kiểm tra trả về '' nếu hợp lệ, ngược lại trả về câu thông báo lỗi tiếng Việt.

export const TEN_MIN = 2
export const TEN_MAX = 100
export const EMAIL_MAX = 100
export const TUOI_MIN = 10
export const TUOI_MAX = 100
export const DIA_CHI_MIN = 5
export const DIA_CHI_MAX = 255
export const SO_DIA_CHI_MAX = 10

const HO_TEN = /^[\p{L}\p{M}]+( [\p{L}\p{M}]+)*$/u
const SDT_VN = /^0[35789]\d{8}$/
const EMAIL = /^[a-z0-9]+([._%+-][a-z0-9]+)*@[a-z0-9]+(-[a-z0-9]+)*(\.[a-z0-9]+(-[a-z0-9]+)*)*\.[a-z]{2,}$/
const CO_CHU_HOAC_SO = /[\p{L}\d]/u

/** Bỏ khoảng trắng đầu/cuối, gộp nhiều khoảng trắng thành 1, đưa chữ có dấu về dạng dựng sẵn (NFC). */
export function chuanHoaKhoangTrang(s) {
  return String(s ?? '').normalize('NFC').trim().replace(/\s+/g, ' ')
}

/** Bỏ khoảng trắng / dấu chấm / gạch ngang / ngoặc, đổi +84 hoặc 84 ở đầu thành 0. */
export function chuanHoaSdt(s) {
  let v = String(s ?? '').replace(/[\s.\-()]/g, '')
  if (v.startsWith('+84')) v = '0' + v.slice(3)
  else if (v.startsWith('84') && v.length === 11) v = '0' + v.slice(2)
  return v
}

export function chuanHoaEmail(s) {
  return String(s ?? '').trim().toLowerCase()
}

const vietThuong = (nhan) => nhan.toLocaleLowerCase('vi')

export function kiemTraHoTen(raw, nhan = 'Họ tên') {
  const v = chuanHoaKhoangTrang(raw)
  if (!v) return `Vui lòng nhập ${vietThuong(nhan)}.`
  if (v.length < TEN_MIN || v.length > TEN_MAX) return `${nhan} phải từ ${TEN_MIN} đến ${TEN_MAX} ký tự.`
  if (!HO_TEN.test(v)) return `${nhan} chỉ được chứa chữ cái và khoảng trắng (không chứa số, ký tự đặc biệt).`
  return ''
}

export function kiemTraSdt(raw, nhan = 'Số điện thoại') {
  const v = chuanHoaSdt(raw)
  if (!v) return `Vui lòng nhập ${vietThuong(nhan)}.`
  if (!SDT_VN.test(v)) return `${nhan} không hợp lệ (gồm 10 chữ số, bắt đầu bằng 03, 05, 07, 08 hoặc 09).`
  return ''
}

export function kiemTraEmail(raw) {
  const v = chuanHoaEmail(raw)
  if (!v) return 'Vui lòng nhập email.'
  if (v.length > EMAIL_MAX) return `Email tối đa ${EMAIL_MAX} ký tự.`
  if (!EMAIL.test(v)) return 'Email không đúng định dạng (ví dụ: ten@gmail.com).'
  return ''
}

/** Tuổi tròn tính đến ngày homNay (Date). ngaySinh dạng 'yyyy-MM-dd'. */
function tinhTuoi(ngaySinh, homNay) {
  const [y, m, d] = ngaySinh.split('-').map(Number)
  let tuoi = homNay.getFullYear() - y
  const thang = homNay.getMonth() + 1
  if (thang < m || (thang === m && homNay.getDate() < d)) tuoi--
  return tuoi
}

/** Ngày sinh không bắt buộc ('' hợp lệ). homNay để test truyền ngày cố định. */
export function kiemTraNgaySinh(ngaySinh, homNay = new Date()) {
  if (!ngaySinh) return ''
  const s = String(ngaySinh)
  if (!/^\d{4}-\d{2}-\d{2}$/.test(s)) return 'Ngày sinh không hợp lệ (năm phải đủ 4 chữ số).'
  const [y, m, d] = s.split('-').map(Number)
  const ngay = new Date(y, m - 1, d)
  if (ngay.getFullYear() !== y || ngay.getMonth() !== m - 1 || ngay.getDate() !== d) return 'Ngày sinh không tồn tại.'
  const homNay0 = new Date(homNay.getFullYear(), homNay.getMonth(), homNay.getDate())
  if (ngay > homNay0) return 'Ngày sinh không được ở tương lai.'
  const tuoi = tinhTuoi(s, homNay0)
  if (tuoi < TUOI_MIN) return `Khách hàng phải từ ${TUOI_MIN} tuổi trở lên.`
  if (tuoi > TUOI_MAX) return `Ngày sinh không hợp lệ (khách hàng không quá ${TUOI_MAX} tuổi).`
  return ''
}

/** Giới hạn cho ô chọn ngày (min / max) theo khoảng tuổi cho phép, dạng 'yyyy-MM-dd'. */
export function gioiHanNgaySinh(homNay = new Date()) {
  const fmt = (dt) =>
    `${dt.getFullYear()}-${String(dt.getMonth() + 1).padStart(2, '0')}-${String(dt.getDate()).padStart(2, '0')}`
  // Sớm nhất: còn 1 ngày nữa mới tròn 101 tuổi (vẫn tính là 100 tuổi)
  const min = new Date(homNay.getFullYear() - TUOI_MAX - 1, homNay.getMonth(), homNay.getDate() + 1)
  const max = new Date(homNay.getFullYear() - TUOI_MIN, homNay.getMonth(), homNay.getDate())
  return { min: fmt(min), max: fmt(max) }
}

export function kiemTraTinhThanh(v) {
  return chuanHoaKhoangTrang(v) ? '' : 'Vui lòng chọn Tỉnh/Thành phố.'
}

export function kiemTraPhuongXa(v) {
  return chuanHoaKhoangTrang(v) ? '' : 'Vui lòng chọn Phường/Xã.'
}

export function kiemTraDiaChiCuThe(raw) {
  const v = chuanHoaKhoangTrang(raw)
  if (!v) return 'Vui lòng nhập địa chỉ cụ thể (số nhà, tên đường...).'
  if (v.length < DIA_CHI_MIN || v.length > DIA_CHI_MAX) {
    return `Địa chỉ cụ thể phải từ ${DIA_CHI_MIN} đến ${DIA_CHI_MAX} ký tự.`
  }
  if (!CO_CHU_HOAC_SO.test(v)) return 'Địa chỉ cụ thể phải có chữ hoặc số.'
  return ''
}
