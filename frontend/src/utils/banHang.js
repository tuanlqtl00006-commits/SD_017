// Hàm dùng chung cho Bán hàng tại quầy: giỏ hàng, tiền khách đưa, mã QR VietQR.
// Backend vẫn tính lại khi thanh toán, ở đây chỉ để hiển thị và báo lỗi sớm.

import { phiVanChuyenTheoTinh } from './phiVanChuyenTinh'
import { tinhGiamPhieu } from './tinhPhieu'

export const SO_LUONG_TOI_DA = 999

/* ===================== Gợi ý phiếu mới ===================== */

const giamTai = (p, tong) => (p ? tinhGiamPhieu(p, tong).giam : 0)

// Tiền giảm lớn nhất phiếu có thể đạt (phiếu % không có trần thì không giới hạn)
function tranGiam(p) {
  if (!p) return 0
  if (p.loaiGiam !== 'PHAN_TRAM') return Number(p.giaTri) || 0
  return Number(p.giamToiDa) > 0 ? Number(p.giamToiDa) : Infinity
}

// Tổng đơn nhỏ nhất (lớn hơn `tong`) mà phiếu p giảm nhiều hơn phiếu đang dùng; null nếu mua thêm bao nhiêu cũng không vượt
function tongDeVuot(p, dangDung, tong) {
  const tu = Math.max(tong + 1, Number(p.donToiThieu) || 0)
  if (giamTai(p, tu) > giamTai(dangDung, tu)) return tu
  // Chưa vượt ngay: chỉ còn trường hợp phiếu đang dùng đã chạm trần, phiếu % này tăng theo tiền đơn rồi vượt qua
  const tran = tranGiam(dangDung)
  const phanTram = Math.min(100, Number(p.giaTri) || 0)
  if (p.loaiGiam !== 'PHAN_TRAM' || phanTram <= 0 || tran === Infinity) return null
  const t = Math.max(tu, Math.ceil(((tran + 1) * 100) / phanTram))
  return giamTai(p, t) > giamTai(dangDung, t) ? t : null
}

/**
 * Ghi chú "mua thêm bao nhiêu thì được phiếu mới". Màn quầy luôn tự áp phiếu giảm nhiều nhất, nên phiếu mới là phiếu sẽ thay
 * phiếu đang dùng ở mức tiền đơn gần nhất. Phiếu chỉ xét theo tiền đơn; số sản phẩm là quy đổi số tiền còn thiếu theo giá và
 * tồn kho của các dòng đang có trong giỏ, ít sản phẩm nhất trước (không dòng nào đủ thì sanPham = null).
 * Trả về { ma, canThem, giam, sanPham: { ten, soLuong } | null }, hoặc null khi mua thêm cũng không có phiếu nào giảm nhiều hơn.
 */
export function goiYPhieuMoi(phieus, tong, gio, dangDung) {
  const dungDuoc = phieus.filter((p) => p.apDungDuoc || p.canMuaThem > 0)
  let tot = null
  for (const p of dungDuoc) {
    if (p.ma === dangDung?.ma) continue
    const t = tongDeVuot(p, dangDung, tong)
    if (t === null) continue
    const giam = giamTai(p, t)
    if (!tot || t < tot.t || (t === tot.t && giam > tot.giam)) tot = { p, t, giam }
  }
  if (!tot) return null

  const canThem = tot.t - tong
  let sanPham = null
  for (const d of gio) {
    if (!(d.donGia > 0)) continue
    const soLuong = Math.ceil(canThem / d.donGia)
    if (soLuong > Number(d.tonKho || 0) || d.soLuong + soLuong > SO_LUONG_TOI_DA) continue
    // Thêm đúng số này mà có phiếu khác giảm nhiều hơn thì câu "để dùng phiếu ..." không còn đúng: bỏ qua dòng này
    const sau = tong + soLuong * d.donGia
    if (giamTai(tot.p, sau) < Math.max(...dungDuoc.map((p) => giamTai(p, sau)))) continue
    if (!sanPham || soLuong < sanPham.soLuong || (soLuong === sanPham.soLuong && d.donGia < sanPham.donGia)) {
      sanPham = { ten: d.ten, soLuong, donGia: d.donGia }
    }
  }
  return { ma: tot.p.ma, canThem, giam: tot.giam, sanPham }
}

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
