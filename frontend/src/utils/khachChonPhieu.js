// Lọc danh sách khách hàng ở bảng "Danh sách khách hàng nhận phiếu" (phiếu giảm giá CÁ NHÂN) theo lời cô:
// lọc theo giới tính, tháng sinh / sắp sinh nhật, số đơn đã mua, ngày mua gần nhất. Hàm thuần, không phụ thuộc Vue để test riêng.
//
// Khách hàng: { id, ma, hoTen, soDienThoai, email, ngaySinh: 'yyyy-MM-dd'|null, gioiTinh: 1 (Nam) | 0 (Nữ) | null,
//               tongDon: số đơn ĐÃ HOÀN THÀNH, muaGanNhat: 'yyyy-MM-ddTHH:mm:ss'|null (đơn hoàn thành gần nhất) }
import { includesText } from './text'

/** 1 -> 'Nam', 0 -> 'Nữ', còn lại (chưa khai báo) -> ''. */
export const tenGioiTinh = (g) => (g === 1 || g === '1' ? 'Nam' : g === 0 || g === '0' ? 'Nữ' : '')

export const LOC_GIOI_TINH = [
  { value: '', label: 'Tất cả giới tính' },
  { value: '1', label: 'Nam' },
  { value: '0', label: 'Nữ' },
  { value: 'chua', label: 'Chưa khai báo' },
]

export const LOC_THANG_SINH = [
  { value: '', label: 'Tất cả tháng sinh' },
  ...Array.from({ length: 12 }, (_, i) => ({ value: String(i + 1), label: `Tháng ${i + 1}` })),
]

export const LOC_SINH_NHAT = [
  { value: '', label: 'Mọi ngày sinh' },
  { value: '7', label: 'Sinh nhật trong 7 ngày tới' },
  { value: '30', label: 'Sinh nhật trong 30 ngày tới' },
]

/** Khoảng số đơn đã hoàn thành: 'min-max' ('' = không giới hạn bên đó). */
export const LOC_SO_DON = [
  { value: '', label: 'Mọi số đơn' },
  { value: '0-0', label: 'Chưa mua (0 đơn)' },
  { value: '1-2', label: '1 – 2 đơn' },
  { value: '3-5', label: '3 – 5 đơn' },
  { value: '6-', label: 'Từ 6 đơn trở lên' },
]

export const locMacDinh = () => ({ tuKhoa: '', gioiTinh: '', thangSinh: '', sinhNhat: '', soDon: '', muaTu: '', muaDen: '' })

/** Có đang lọc gì không (để hiện nút "Đặt lại lọc"). */
export const dangLoc = (loc) => Object.values(loc).some((v) => v !== '' && v !== null && v !== undefined)

const laNamNhuan = (y) => (y % 4 === 0 && y % 100 !== 0) || y % 400 === 0

/**
 * Số ngày từ hôm nay tới sinh nhật sắp tới (0 = đúng hôm nay; sinh nhật hôm qua thì tính của năm sau).
 * Sinh ngày 29/2 mà năm đó không nhuận thì tính vào 28/2. Không có / sai ngày sinh -> null.
 */
export function ngayToiSinhNhat(ngaySinh, homNay = new Date()) {
  const m = String(ngaySinh ?? '').match(/^(\d{4})-(\d{2})-(\d{2})/)
  if (!m) return null
  const thang = Number(m[2])
  const ngay = Number(m[3])
  const dau = new Date(homNay.getFullYear(), homNay.getMonth(), homNay.getDate())
  const sinhNhatNam = (nam) => {
    const d = thang === 2 && ngay === 29 && !laNamNhuan(nam) ? 28 : ngay
    return new Date(nam, thang - 1, d)
  }
  let moc = sinhNhatNam(dau.getFullYear())
  if (moc < dau) moc = sinhNhatNam(dau.getFullYear() + 1)
  return Math.round((moc - dau) / 86400000)
}

const thangSinhCua = (ngaySinh) => Number(String(ngaySinh ?? '').match(/^\d{4}-(\d{2})-\d{2}/)?.[1]) || null

function trongKhoangDon(tongDon, soDon) {
  if (!soDon) return true
  const [tu, den] = soDon.split('-')
  if (tu !== '' && tongDon < Number(tu)) return false
  if (den !== '' && den !== undefined && tongDon > Number(den)) return false
  return true
}

/** Danh sách khách thỏa MỌI điều kiện lọc (điều kiện trống thì bỏ qua). Khách chưa mua bao giờ bị loại khi lọc theo ngày mua. */
export function locKhach(khachs, loc, homNay = new Date()) {
  const l = { ...locMacDinh(), ...loc }
  const muaTu = l.muaTu || ''
  const muaDen = l.muaDen || ''
  return (khachs ?? []).filter((k) => {
    if (l.tuKhoa && !includesText(`${k.ma} ${k.hoTen} ${k.soDienThoai ?? ''} ${k.email ?? ''}`, l.tuKhoa)) return false
    if (l.gioiTinh !== '') {
      const g = k.gioiTinh === 1 || k.gioiTinh === 0 ? String(k.gioiTinh) : 'chua'
      if (g !== l.gioiTinh) return false
    }
    if (l.thangSinh !== '' && thangSinhCua(k.ngaySinh) !== Number(l.thangSinh)) return false
    if (l.sinhNhat !== '') {
      const con = ngayToiSinhNhat(k.ngaySinh, homNay)
      if (con === null || con > Number(l.sinhNhat)) return false
    }
    if (!trongKhoangDon(Number(k.tongDon) || 0, l.soDon)) return false
    if (muaTu || muaDen) {
      const ngayMua = String(k.muaGanNhat ?? '').slice(0, 10)
      if (!ngayMua) return false
      if (muaTu && ngayMua < muaTu) return false
      if (muaDen && ngayMua > muaDen) return false
    }
    return true
  })
}

/** Ghi chú ngắn cạnh ngày sinh khi sắp tới sinh nhật: 'Hôm nay sinh nhật', 'Sinh nhật sau 3 ngày'; xa hơn thì ''. */
export function ghiChuSinhNhat(ngaySinh, homNay = new Date(), trongNgay = 7) {
  const con = ngayToiSinhNhat(ngaySinh, homNay)
  if (con === null || con > trongNgay) return ''
  return con === 0 ? 'Hôm nay sinh nhật' : `Sinh nhật sau ${con} ngày`
}

/** 'yyyy-MM-dd' -> 'dd/MM/yyyy' ('' nếu trống) */
export function ngaySinhVn(ngaySinh) {
  const m = String(ngaySinh ?? '').match(/^(\d{4})-(\d{2})-(\d{2})/)
  return m ? `${m[3]}/${m[2]}/${m[1]}` : ''
}
