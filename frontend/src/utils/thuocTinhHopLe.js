// Giá trị thuộc tính phải đúng chuẩn thị trường cầu lông Việt Nam (giống backend ThuocTinhHopLe.java):
//  - Màu sắc, độ cứng, điểm cân bằng, xuất xứ: chỉ chữ (có dấu), khoảng trắng, gạch nối / gạch chéo.
//  - Trọng lượng: 1U - 8U, có thể kèm khoảng gram đúng chuẩn, vd "4U (80-84g)".
//  - Chu vi cán: G1 - G6.
//  - Danh mục, thương hiệu, chất liệu: phải có chữ; chỉ chữ / số / khoảng trắng và - & . ' / ( ) % +.
//  - "Không áp dụng" luôn hợp lệ.
import { normalizeText } from './text'

const CHI_CHU = /^[\p{L}\p{M}]+(( ?[-/] ?| )[\p{L}\p{M}]+)*$/u
const CO_CHU = /[\p{L}\p{M}]/u
const TEN_CHUNG = /^[\p{L}\p{M}0-9 &.'/()%+-]+$/u
const TRONG_LUONG = /^([1-8])U(?: \((\d{2,3}) ?- ?(\d{2,3}) ?g\))?$/i
const CHU_VI = /^G([1-6])$/i

/** Chuẩn hóa cách viết: 4u -> 4U, g5 -> G5, "4U(80 - 84 g)" -> "4U (80-84g)". */
export function chuanHoaThuocTinh(loai, ten) {
  const t = String(ten ?? '').trim().replace(/\s+/g, ' ')
  if (loai === 'trong-luong') {
    const g = t.replace(/\s*\(\s*/g, ' (').replace(/\s*\)/g, ')').replace(/\s*-\s*/g, '-').replace(/(\d)\s*g\)/gi, '$1g)')
    const m = g.match(TRONG_LUONG)
    if (m) return `${m[1]}U${m[2] ? ` (${m[2]}-${m[3]}g)` : ''}`
  }
  if (loai === 'chu-vi' && CHU_VI.test(t)) return t.toUpperCase()
  return t
}

/** Câu báo lỗi nếu giá trị không hợp lệ với loại thuộc tính; hợp lệ thì ''. */
export function loiThuocTinh(loai, tenGoc) {
  const ten = chuanHoaThuocTinh(loai, tenGoc)
  if (!ten || normalizeText(ten) === 'khong ap dung') return ''
  switch (loai) {
    case 'mau-sac':
      return CHI_CHU.test(ten) ? '' : 'Tên màu chỉ gồm chữ (vd: Đen, Xanh dương, Trắng - Đỏ), không chứa số hay ký tự đặc biệt.'
    case 'do-cung':
      return CHI_CHU.test(ten) ? '' : 'Độ cứng chỉ gồm chữ (vd: Mềm, Trung bình, Cứng, Siêu cứng).'
    case 'diem-can-bang':
      return CHI_CHU.test(ten) ? '' : 'Điểm cân bằng chỉ gồm chữ (vd: Nặng đầu, Cân bằng, Nhẹ đầu).'
    case 'xuat-xu':
      return CHI_CHU.test(ten) ? '' : 'Xuất xứ là tên quốc gia / vùng lãnh thổ, chỉ gồm chữ (vd: Nhật Bản, Đài Loan).'
    case 'trong-luong': {
      const m = ten.match(TRONG_LUONG)
      if (!m) return 'Trọng lượng vợt theo chuẩn U: 1U - 8U, có thể kèm khoảng gram, vd: 4U hoặc 4U (80-84g).'
      if (!m[2]) return ''
      const u = Number(m[1])
      const tu = 100 - 5 * u
      const den = tu + 4
      return Number(m[2]) === tu && Number(m[3]) === den ? '' : `${u}U là ${tu}-${den}g. Hãy nhập ${u}U (${tu}-${den}g).`
    }
    case 'chu-vi':
      return CHU_VI.test(ten) ? '' : 'Chu vi cán theo chuẩn G1 - G6 (vd: G4, G5). Số càng lớn cán càng nhỏ.'
    default:
      if (!CO_CHU.test(ten)) return 'Tên phải có chữ, không chỉ gồm số hay ký tự đặc biệt.'
      return TEN_CHUNG.test(ten) ? '' : "Tên chỉ gồm chữ, số, khoảng trắng và các dấu - & . ' / ( ) %."
  }
}
