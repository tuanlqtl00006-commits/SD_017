// Phí vận chuyển theo nơi nhận hàng (cửa hàng ở Hà Nội). Khớp backend PhiVanChuyenTinh.java.
//  - Hà Nội: CÓ tính phí: giao tới phường 25.000 ₫, tới xã 30.000 ₫; chưa biết phường/xã thì tính như phường.
//  - Tỉnh/thành khác: mức phí tùy tỉnh (xa Hà Nội thì phí cao hơn).
//  - Đơn đạt ngưỡng miễn phí vận chuyển theo giá trị đơn (quản lý cài đặt) thì phí về 0.
// Danh sách theo 34 tỉnh/thành sau sáp nhập 07/2025.
import { chuanHoaTenDonVi } from './diaChi'

/** Phí khi không nhận ra tên tỉnh (vd địa chỉ cũ ghi tay). */
export const PHI_MAC_DINH = 35000
/** Giao trong Hà Nội: tới phường (nội thành) / tới xã (ngoại thành). */
export const PHI_HA_NOI = { phuong: 25000, xa: 30000 }

/** Bảng phí theo tỉnh (Hà Nội ở dòng riêng: xem BANG_PHI_HIEN_THI). */
export const NHOM_PHI = [
  [25000, ['Bắc Ninh', 'Hưng Yên', 'Phú Thọ', 'Thái Nguyên', 'Hải Phòng', 'Ninh Bình']], // lân cận Hà Nội
  [30000, ['Quảng Ninh', 'Lào Cai', 'Tuyên Quang', 'Lạng Sơn', 'Cao Bằng', 'Điện Biên', 'Lai Châu', 'Sơn La']], // miền núi phía Bắc
  [35000, ['Thanh Hóa', 'Nghệ An', 'Hà Tĩnh']], // Bắc Trung Bộ
  [40000, ['Quảng Trị', 'Huế', 'Đà Nẵng', 'Quảng Ngãi']], // Trung Bộ
  [45000, ['Gia Lai', 'Khánh Hòa', 'Đắk Lắk', 'Lâm Đồng']], // Nam Trung Bộ, Tây Nguyên
  [50000, ['Hồ Chí Minh', 'Đồng Nai', 'Tây Ninh']], // Đông Nam Bộ
  [55000, ['Cần Thơ', 'Vĩnh Long', 'Đồng Tháp', 'Cà Mau', 'An Giang']], // Tây Nam Bộ
]

/** Khóa so khớp: bỏ dấu, chữ thường, bỏ tiền tố Tỉnh / Thành phố / TP., bỏ khoảng trắng và dấu câu. */
export const khoa = (ten) =>
  chuanHoaTenDonVi(String(ten ?? '').trim().replace(/^tp\.?\s+/i, '')).replace(/[^a-z0-9]/g, '')

const KHOA_HA_NOI = khoa('Hà Nội')
export const BANG_PHI_TINH = new Map([[KHOA_HA_NOI, PHI_HA_NOI.phuong], ...NHOM_PHI.flatMap(([phi, ds]) => ds.map((t) => [khoa(t), phi]))])

/** Các dòng bảng phí để hiện cho khách / quản lý (trang Chính sách, Cài đặt free ship). */
export const BANG_PHI_HIEN_THI = [
  { tinh: 'Hà Nội - giao tới phường (nội thành)', phi: PHI_HA_NOI.phuong },
  { tinh: 'Hà Nội - giao tới xã (ngoại thành)', phi: PHI_HA_NOI.xa },
  ...NHOM_PHI.map(([phi, ds]) => ({ tinh: ds.join(', '), phi })),
]

/** "Xã Sóc Sơn" -> true; "Phường Hà Đông", trống -> false. */
export const laXa = (phuongXa) => /^xa\s/.test(chuanHoaTenDonVi(String(phuongXa ?? '').trim()))

/** Có nhận ra tên tỉnh/thành trong bảng phí không. */
export const nhanRaTinh = (tinhThanh) => BANG_PHI_TINH.has(khoa(tinhThanh))

/**
 * Phí giao hàng tới tỉnh/thành (và phường/xã, chỉ Hà Nội phân biệt); không nhận ra tỉnh thì PHI_MAC_DINH.
 * Chưa chọn tỉnh (trống) trả null (chưa tính được).
 */
export function phiVanChuyenTheoTinh(tinhThanh, phuongXa = '') {
  if (!String(tinhThanh ?? '').trim()) return null
  const k = khoa(tinhThanh)
  if (k === KHOA_HA_NOI) return laXa(phuongXa) ? PHI_HA_NOI.xa : PHI_HA_NOI.phuong
  return BANG_PHI_TINH.get(k) ?? PHI_MAC_DINH
}

/** Phí theo địa chỉ ghi liền một chuỗi: phần sau dấu phẩy cuối là tỉnh, tìm phường/xã ở các phần trước. Không nhận ra tỉnh thì null (giữ phí cũ). */
export function phiTheoDiaChi(diaChi) {
  const phan = String(diaChi ?? '').split(',').map((x) => x.trim()).filter(Boolean)
  if (!phan.length) return null
  const tinh = phan[phan.length - 1]
  let phuong = ''
  for (let i = phan.length - 2; i >= 0; i--) {
    const p = phan[i]
    if (/^(phường|xã|thị trấn|p\.|x\.|tt\.)/i.test(p)) {
      phuong = p
      break
    }
  }
  if (!phuong && phan.length >= 2) phuong = phan[phan.length - 2]
  return nhanRaTinh(tinh) ? phiVanChuyenTheoTinh(tinh, phuong) : null
}

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
// phiTheoDiaChiMoi (tùy chọn): phí tới địa chỉ mới máy chủ đã tính theo quãng đường (GET /cua-hang/phi-van-chuyen);
// không có thì tạm tính theo bảng tỉnh.
export function phiSauKhiSuaDon({ caiDat, diaChiMoi, diaChiCu, tienHang, tienHangCu, phiCu, phiTheoDiaChiMoi }) {
  const gon = (s) => String(s ?? '').trim().replace(/\s+/g, ' ')
  const cu = Number(phiCu) || 0
  const doiDiaChi = gon(diaChiMoi) !== gon(diaChiCu)
  if (!doiDiaChi && Number(tienHang) === Number(tienHangCu)) return cu
  if (duocMienPhiVanChuyen(caiDat, tienHang)) return 0
  const theoTinh = phiTheoDiaChiMoi !== undefined && phiTheoDiaChiMoi !== null ? Number(phiTheoDiaChiMoi) : phiTheoDiaChi(diaChiMoi)
  if (doiDiaChi) return theoTinh ?? cu
  if (cu === 0 && duocMienPhiVanChuyen(caiDat, tienHangCu)) return theoTinh ?? PHI_MAC_DINH
  return cu
}
