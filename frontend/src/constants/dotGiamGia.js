import { dinhDangMa } from '../utils/ma'
import { bayGioDayDu, viTriTheoMoc } from '../utils/ngayGio'

// Trạng thái hiển thị của đợt giảm giá (giống phiếu giảm giá), tính từ NGÀY GIỜ bắt đầu - kết thúc + công tắc bật / tắt.
export const TRANG_THAI = {
  DANG_DIEN_RA: { label: 'Đang diễn ra', cls: 'ad-pill-green' },
  SAP_DIEN_RA: { label: 'Sắp diễn ra', cls: 'ad-pill-amber' },
  DA_KET_THUC: { label: 'Đã kết thúc', cls: 'ad-pill-gray' },
  NGUNG_HOAT_DONG: { label: 'Ngừng hoạt động', cls: 'ad-pill-red' },
}

/** Công tắc đang bật (cột trang_thai = 1). Chưa xét thời gian. */
export const dangHoatDong = (d) => d.trangThai === 1

/** Lấy 'yyyy-mm-dd' từ chuỗi ngày giờ backend trả ('2026-08-16T00:00:00'). */
export const ngayIso = (v) => (v ? String(v).substring(0, 10) : '')

/**
 * Đã quá GIỜ kết thúc. Đợt đã kết thúc thì công tắc hiện TẮT và bị khóa.
 * `bayGio` là 'yyyy-MM-ddTHH:mm:ss' (mặc định: bây giờ); chỉ có ngày thì tính từ 00:00. Đợt cũ lưu theo ngày
 * (00:00:00 - 23:59:59) vẫn chạy đúng.
 */
export const daKetThuc = (d, bayGio = bayGioDayDu()) => viTriTheoMoc(d.ngayBatDau, d.ngayKetThuc, bayGio) === 'KET_THUC'

/**
 * Trạng thái hiển thị (theo thời gian + công tắc):
 *  - công tắc đang tắt (kể cả đợt đã quá giờ kết thúc) -> Ngừng hoạt động
 *  - công tắc bật, quá giờ kết thúc                    -> Đã kết thúc
 *  - chưa tới giờ bắt đầu                              -> Sắp diễn ra
 *  - đang trong khoảng ngày giờ                        -> Đang diễn ra
 */
export function tinhTrangThai(d, bayGio = bayGioDayDu()) {
  if (!dangHoatDong(d)) return 'NGUNG_HOAT_DONG'
  const viTri = viTriTheoMoc(d.ngayBatDau, d.ngayKetThuc, bayGio)
  if (viTri === 'KET_THUC') return 'DA_KET_THUC'
  if (viTri === 'SAP') return 'SAP_DIEN_RA'
  return 'DANG_DIEN_RA'
}

/** Mã dự phòng khi chưa gọi được backend: DGG + số thứ tự (vd DGG09, DGG100), tính từ các mã đã có. */
export function taoMaDot(maDaCo = []) {
  let max = 0
  for (const m of maDaCo) {
    const r = /^DGG(\d{1,6})$/i.exec(String(m).trim())
    if (r) max = Math.max(max, Number(r[1]))
  }
  return dinhDangMa('DGG', max + 1)
}

/** Cách gộp khi nhiều đợt cùng áp cho một biến thể trong cùng thời gian (khớp GiamGiaChongDot.CheDo ở backend). */
export const CHE_DO_CHONG_DOT = {
  MAX: { label: 'Lấy mức cao nhất', ngan: 'cao nhất', moTa: 'Lấy mức giảm CAO NHẤT trong các đợt đang chồng nhau (không cộng dồn).' },
  CONG_DON: { label: 'Cộng dồn', ngan: 'cộng dồn', moTa: 'CỘNG DỒN mức giảm của các đợt đang chồng nhau (tối đa 100%).' },
  TRUNG_BINH: { label: 'Trung bình cộng', ngan: 'trung bình cộng', moTa: 'Lấy TRUNG BÌNH CỘNG mức giảm của các đợt đang chồng nhau.' },
}
