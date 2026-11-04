// Hàm dùng chung cho Bán hàng tại quầy: giỏ hàng, tiền khách đưa, mã QR VietQR.
// Backend vẫn tính lại khi thanh toán, ở đây chỉ để hiển thị và báo lỗi sớm.

import { phiVanChuyenTheoTinh } from './phiVanChuyenTinh'

export const SO_LUONG_TOI_DA = 999

/* ===================== Giỏ hàng ===================== */

export const tongSoLuong = (gio) => gio.reduce((s, d) => s + d.soLuong, 0)
export const tongTienHang = (gio) => gio.reduce((s, d) => s + d.donGia * d.soLuong, 0)
/** Tiền giảm theo đợt (giá gốc - giá bán). Dòng đang đổi giá thì chưa tính, cập nhật giá xong mới tính. */
export const dangGiamTheoDot = (d) => !d.giaDoi && d.donGia < d.giaGoc
export const tienGiamDot = (gio) => gio.reduce((s, d) => s + (dangGiamTheoDot(d) ? (d.giaGoc - d.donGia) * d.soLuong : 0), 0)

/* ===================== Tiền khách trả ===================== */

const so = (v) => (v === '' || v === null || v === undefined ? 0 : Math.max(0, Math.round(Number(v) || 0)))

/**
 * Theo phương thức: TIEN_MAT (chỉ tiền mặt), CHUYEN_KHOAN (chuyển đúng số phải trả), KET_HOP (một phần chuyển khoản, còn lại tiền mặt).
 * Trả về { phaiTra, tienMat, chuyenKhoan, tienThua, conThieu, loi }.
 */
export function tinhThanhToan({ tongTien, giam = 0, phuongThuc = 'TIEN_MAT', tienMat, chuyenKhoan }) {
  const phaiTra = Math.max(0, so(tongTien) - so(giam))
  let mat = so(tienMat)
  let ck = so(chuyenKhoan)
  if (phuongThuc === 'TIEN_MAT') ck = 0
  if (phuongThuc === 'CHUYEN_KHOAN') {
    ck = phaiTra
    mat = 0
  }
  let loi = ''
  if (ck > phaiTra) loi = 'Tiền chuyển khoản không được lớn hơn số tiền phải trả.'
  const conThieu = Math.max(0, phaiTra - mat - ck)
  if (!loi && conThieu > 0) loi = phuongThuc === 'TIEN_MAT' && mat === 0 ? 'Vui lòng nhập tiền khách đưa.' : 'Khách trả chưa đủ.'
  const tienThua = loi ? 0 : mat - (phaiTra - ck)
  return { phaiTra, tienMat: mat, chuyenKhoan: ck, tienThua: Math.max(0, tienThua), conThieu, loi }
}

/** Gợi ý tiền khách đưa: đúng số + làm tròn lên 10k / 50k / 100k / 200k / 500k (bỏ trùng), tối đa 5 nút. */
export function goiYTienKhachDua(phaiTra) {
  const p = so(phaiTra)
  if (p <= 0) return []
  const ds = [p]
  for (const buoc of [10_000, 50_000, 100_000, 200_000, 500_000]) ds.push(Math.ceil(p / buoc) * buoc)
  return [...new Set(ds)].sort((a, b) => a - b).slice(0, 5)
}

/* ===================== Mã QR chuyển khoản (chuẩn VietQR / EMVCo) ===================== */

const tlv = (id, giaTri) => {
  const v = String(giaTri)
  return `${id}${String(v.length).padStart(2, '0')}${v}`
}

/** CRC-16/CCITT-FALSE (đa thức 0x1021, khởi đầu 0xFFFF) - mã kiểm tra cuối chuỗi VietQR. */
export function crc16(chuoi) {
  let crc = 0xffff
  for (const b of new TextEncoder().encode(chuoi)) {
    crc ^= b << 8
    for (let i = 0; i < 8; i++) crc = crc & 0x8000 ? ((crc << 1) ^ 0x1021) & 0xffff : (crc << 1) & 0xffff
  }
  return crc.toString(16).toUpperCase().padStart(4, '0')
}

/** Bỏ dấu, chỉ giữ chữ / số / khoảng trắng, tối đa 25 ký tự (nội dung chuyển khoản ngân hàng chấp nhận). */
export function noiDungChuyenKhoan(s) {
  return String(s ?? '')
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .replace(/[^0-9A-Za-z ]/g, '')
    .replace(/\s+/g, ' ')
    .trim()
    .slice(0, 25)
}

/** Chuỗi VietQR (quét bằng app ngân hàng sẽ điền sẵn tài khoản, số tiền, nội dung). */
export function chuoiVietQR({ bin, soTaiKhoan, soTien, noiDung }) {
  const nguoiNhan = tlv('00', bin) + tlv('01', soTaiKhoan)
  const taiKhoan = tlv('00', 'A000000727') + tlv('01', nguoiNhan) + tlv('02', 'QRIBFTTA')
  let s = tlv('00', '01') + tlv('01', soTien ? '12' : '11') + tlv('38', taiKhoan) + tlv('53', '704')
  if (soTien) s += tlv('54', String(Math.round(soTien)))
  s += tlv('58', 'VN')
  const nd = noiDungChuyenKhoan(noiDung)
  if (nd) s += tlv('62', tlv('08', nd))
  s += '6304'
  return s + crc16(s)
}

/**
 * Phí giao hàng theo tỉnh nhận (giống backend PhiVanChuyenTinh): Hà Nội miễn phí, tỉnh khác tùy tỉnh.
 * Tỉnh còn trống thì trả null (chưa tính được).
 */
export function phiGiaoHang(tinhThanh) {
  return phiVanChuyenTheoTinh(tinhThanh)
}
