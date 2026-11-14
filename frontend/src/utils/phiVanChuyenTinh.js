// Phí vận chuyển theo Tỉnh/Thành phố nhận hàng (cửa hàng ở Hà Nội). PHẢI khớp backend PhiVanChuyenTinh.java.
//  - Hà Nội: miễn phí, bất kể giá trị đơn.
//  - Tỉnh/thành khác: mức phí tùy tỉnh (xa Hà Nội thì phí cao hơn).
//  - Đơn đạt ngưỡng miễn phí vận chuyển theo giá trị đơn (quản lý cài đặt) thì phí về 0: xem các hàm ở cuối tệp.
// Danh sách theo 34 tỉnh/thành sau sáp nhập 07/2025 (đúng danh sách khách chọn ở form địa chỉ).
import { chuanHoaTenDonVi } from './diaChi'

/** Phí khi không nhận ra tên tỉnh (vd địa chỉ cũ ghi tay). */
export const PHI_MAC_DINH = 35000

export const NHOM_PHI = [
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

/*
 * Miễn phí vận chuyển theo giá trị đơn (quản lý cài đặt ở Cài đặt → Cài đặt free ship; backend CaiDatVanChuyenService).
 * caiDat = { mienPhiBat, mienPhiTu }. Tiền hàng so với ngưỡng là tiền hàng đã giảm theo đợt, CHƯA trừ phiếu giảm giá.
 */

/** Đơn có tiền hàng này có được miễn phí vận chuyển không. */
export function duocMienPhiVanChuyen(caiDat, tienHang) {
  const nguong = Number(caiDat?.mienPhiTu)
  const tien = Number(tienHang)
  return !!caiDat?.mienPhiBat && nguong > 0 && tien > 0 && tien >= nguong
}

/** Còn thiếu bao nhiêu tiền hàng để được miễn phí vận chuyển; 0 = đã đạt, hoặc cửa hàng đang tắt miễn phí. */
export function conThieuDeMienPhi(caiDat, tienHang) {
  const nguong = Number(caiDat?.mienPhiTu)
  if (!caiDat?.mienPhiBat || !(nguong > 0)) return 0
  return Math.max(0, nguong - (Number(tienHang) || 0))
}

/**
 * Phí giao hàng sau khi sửa đơn, giống backend HoaDonSuaService.phiSauKhiSua:
 *  - địa chỉ và tiền hàng giữ nguyên: giữ phí của đơn (có thể do nhân viên nhập lúc bán tại quầy);
 *  - tiền hàng mới đạt ngưỡng miễn phí: 0;
 *  - đổi địa chỉ: theo tỉnh ở cuối địa chỉ mới (không nhận ra tỉnh thì giữ phí cũ);
 *  - chỉ đổi sản phẩm làm đơn tụt dưới ngưỡng mà trước đó được miễn phí nhờ đạt ngưỡng: tính lại theo tỉnh.
 */
export function phiSauKhiSuaDon({ caiDat, diaChiMoi, diaChiCu, tienHang, tienHangCu, phiCu }) {
  const gon = (s) => String(s ?? '').trim().replace(/\s+/g, ' ')
  const cu = Number(phiCu) || 0
  const doiDiaChi = gon(diaChiMoi) !== gon(diaChiCu)
  if (!doiDiaChi && Number(tienHang) === Number(tienHangCu)) return cu
  if (duocMienPhiVanChuyen(caiDat, tienHang)) return 0
  const theoTinh = phiTheoDiaChi(diaChiMoi)
  if (doiDiaChi) return theoTinh ?? cu
  if (cu === 0 && duocMienPhiVanChuyen(caiDat, tienHangCu)) return theoTinh ?? PHI_MAC_DINH
  return cu
}
