// Cách tính tiền giảm của PHIẾU giảm giá cho một đơn hàng, và ví dụ "đợt giảm giá + phiếu giảm giá" hiện trên form phiếu
// (để thể hiện rõ quy tắc chồng ưu đãi mà cô yêu cầu).
//
// Quy tắc (cũng được in trên form):
//  1. Mỗi đơn chỉ dùng MỘT phiếu giảm giá - các phiếu KHÔNG cộng dồn với nhau.
//  2. Đợt giảm giá giảm theo từng biến thể (giá bán). Phiếu giảm tính trên TỔNG TIỀN HÀNG SAU KHI đã trừ giảm giá của đợt.
//  3. Nhiều đợt cùng áp cho một biến thể trong cùng thời gian: gộp theo cấu hình backend (lấy cao nhất / cộng dồn / trung bình).
// Giá sau đợt làm tròn giống giá sau giảm của biến thể (tinhGiaSauGiam, backend LichGiamGiaService.giaSauGiam); gộp nhiều đợt
// theo GiamGiaChongDot.gop ở backend.
import { formatMoney } from './format'
import { tinhGiaSauGiam } from './giamGia'

/**
 * Tiền phiếu giảm cho đơn có tổng tiền hàng `tong` (đã trừ giảm giá của đợt, KHÔNG gồm phí ship) - kiểu Shopee (24/11, Task 2.2).
 * Đây là công thức DUY NHẤT ở frontend (bán quầy, giỏ hàng, thanh toán online dùng chung); backend: PhieuApDung.soTienGiam.
 *  - chưa đạt đơn tối thiểu -> { duDieuKien: false, giam: 0 }
 *  - giảm %: t x % làm tròn xuống tới đồng, không vượt "giảm tối đa" (nếu > 0)
 *  - giảm tiền: đúng giá trị giảm
 *  - luôn kẹp: không vượt tổng tiền hàng, không âm
 * Ví dụ 20%, tối đa 50.000, đơn từ 100.000: 80.000 -> 0; 100.000 -> 20.000; 250.000 -> 50.000; 300.000 -> 50.000.
 * @param {{loaiGiam: 'PHAN_TRAM'|'TIEN_MAT', giaTri: number, giamToiDa?: number|null, donToiThieu?: number|null}} phieu
 */
export function tinhGiamPhieu(phieu, tong) {
  const t = Math.max(0, Math.round(Number(tong) || 0))
  const giaTri = Number(phieu?.giaTri) || 0
  const donToiThieu = Number(phieu?.donToiThieu) || 0
  if (giaTri <= 0) return { duDieuKien: false, giam: 0, lyDo: 'Chưa có giá trị giảm.' }
  if (t < donToiThieu) {
    return { duDieuKien: false, giam: 0, lyDo: `Chưa đủ điều kiện: đơn tối thiểu ${formatMoney(donToiThieu)} (còn thiếu ${formatMoney(donToiThieu - t)}).` }
  }
  let giam
  if (phieu.loaiGiam === 'PHAN_TRAM') {
    giam = Math.floor((t * Math.min(100, giaTri)) / 100)
    const toiDa = Number(phieu.giamToiDa) || 0
    if (toiDa > 0) giam = Math.min(giam, toiDa)
  } else {
    giam = giaTri
  }
  return { duDieuKien: true, giam: Math.max(0, Math.min(giam, t)), lyDo: '' }
}

/**
 * Thông tin đủ của một phiếu để hiện cạnh khung "Đang áp dụng" (góp ý 25/11, quầy và online giống nhau):
 * "Giảm 20% · Tối đa 50.000 ₫ · Đơn từ 100.000 ₫" hoặc "Giảm 50.000 ₫ · Đơn từ 100.000 ₫" (đơn tối thiểu 0: "Mọi đơn hàng").
 * Nhận cả dạng của quầy ({loaiGiam: 'PHAN_TRAM'|'TIEN_MAT', giaTri}) lẫn online ({loaiGiam: 1|2, giaTriGiam}).
 */
export function thongTinPhieu(p) {
  if (!p) return ''
  const phanTram = p.loaiGiam === 'PHAN_TRAM' || p.loaiGiam === 1 || p.hinhThucGiam === 1
  const giaTri = Number(p.giaTri ?? p.giaTriGiam) || 0
  const toiDa = Number(p.giamToiDa) || 0
  const toiThieu = Number(p.donToiThieu) || 0
  const phan = [phanTram ? `Giảm ${giaTri}%` : `Giảm ${formatMoney(giaTri)}`]
  if (phanTram) phan.push(toiDa > 0 ? `Tối đa ${formatMoney(toiDa)}` : 'Không giới hạn mức giảm')
  phan.push(toiThieu > 0 ? `Đơn từ ${formatMoney(toiThieu)}` : 'Mọi đơn hàng')
  return phan.join(' · ')
}

/**
 * Ví dụ đủ các bước cho một đơn: tiền hàng -> giảm theo đợt (phanTramDot %) -> giảm theo phiếu -> khách phải trả.
 * Coi như mọi sản phẩm trong đơn cùng giảm phanTramDot % (đơn 1 sản phẩm thì khớp từng đồng với giá sau giảm của biến thể):
 * tiền sau đợt làm tròn tới đồng giống tinhGiaSauGiam (backend: LichGiamGiaService.giaSauGiam).
 * Trả về { tienHang, phanTramDot, giamDot, sauDot, phieu: { duDieuKien, giam, lyDo }, khachTra }.
 */
export function viDuDonHang(phieu, tienHang, phanTramDot = 0) {
  const goc = Math.max(0, Math.round(Number(tienHang) || 0))
  const pt = Math.round(Math.min(100, Math.max(0, Number(phanTramDot) || 0)) * 100) / 100
  const sauDot = tinhGiaSauGiam(goc, pt)
  const giamDot = goc - sauDot
  const kq = tinhGiamPhieu(phieu, sauDot)
  return { tienHang: goc, phanTramDot: pt, giamDot, sauDot, phieu: kq, khachTra: sauDot - kq.giam }
}
