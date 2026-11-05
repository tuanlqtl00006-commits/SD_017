// Phí vận chuyển theo Tỉnh/Thành phố nhận hàng (cửa hàng ở Hà Nội). PHẢI khớp backend PhiVanChuyenTinh.java.
//  - Hà Nội: miễn phí, bất kể giá trị đơn.
//  - Tỉnh/thành khác: luôn mất phí, mức phí tùy tỉnh (xa Hà Nội thì phí cao hơn), không còn mốc miễn phí theo giá trị đơn.
// Danh sách theo 34 tỉnh/thành sau sáp nhập 07/2025 (đúng danh sách khách chọn ở form địa chỉ).
import { chuanHoaTenDonVi } from './diaChi'

/** Phí khi không nhận ra tên tỉnh (vd địa chỉ cũ ghi tay). */
export const PHI_MAC_DINH = 35000

const NHOM_PHI = [
  [0, ['Hà Nội']],
  [25000, ['Bắc Ninh', 'Hưng Yên', 'Phú Thọ', 'Thái Nguyên', 'Hải Phòng', 'Ninh Bình']], // lân cận Hà Nội
  [30000, ['Quảng Ninh', 'Lào Cai', 'Tuyên Quang', 'Lạng Sơn', 'Cao Bằng', 'Điện Biên', 'Lai Châu', 'Sơn La']], // miền núi phía Bắc
  [35000, ['Thanh Hóa', 'Nghệ An', 'Hà Tĩnh']], // Bắc Trung Bộ
  [40000, ['Quảng Trị', 'Huế', 'Đà Nẵng', 'Quảng Ngãi']], // Trung Bộ
  [45000, ['Gia Lai', 'Khánh Hòa', 'Đắk Lắk', 'Lâm Đồng']], // Nam Trung Bộ, Tây Nguyên
  [50000, ['Hồ Chí Minh', 'Đồng Nai', 'Tây Ninh']], // Đông Nam Bộ
  [55000, ['Cần Thơ', 'Vĩnh Long', 'Đồng Tháp', 'Cà Mau', 'An Giang']], // Tây Nam Bộ
]

/** Khóa so khớp: bỏ dấu, chữ thường, bỏ tiền tố Tỉnh / Thành phố / TP., bỏ khoảng trắng và dấu câu. */
const khoa = (ten) =>
  chuanHoaTenDonVi(String(ten ?? '').trim().replace(/^tp\.?\s+/i, '')).replace(/[^a-z0-9]/g, '')

export const BANG_PHI_TINH = new Map(NHOM_PHI.flatMap(([phi, ds]) => ds.map((t) => [khoa(t), phi])))

/** Có nhận ra tên tỉnh/thành trong bảng phí không. */
export const nhanRaTinh = (tinhThanh) => BANG_PHI_TINH.has(khoa(tinhThanh))

/** Phí giao hàng tới tỉnh/thành này; không nhận ra tên thì PHI_MAC_DINH. Chưa chọn tỉnh (trống) trả null (chưa tính được). */
export function phiVanChuyenTheoTinh(tinhThanh) {
  if (!String(tinhThanh ?? '').trim()) return null
  return BANG_PHI_TINH.get(khoa(tinhThanh)) ?? PHI_MAC_DINH
}

/** Phí theo địa chỉ ghi liền một chuỗi: lấy phần sau dấu phẩy cuối làm tỉnh. Không nhận ra tỉnh thì null (giữ phí cũ). */
export function phiTheoDiaChi(diaChi) {
  const phan = String(diaChi ?? '').split(',')
  const tinh = phan[phan.length - 1]
  return nhanRaTinh(tinh) ? phiVanChuyenTheoTinh(tinh) : null
}
