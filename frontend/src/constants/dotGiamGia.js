import { todayIso } from '../utils/format'

// Trạng thái hiển thị của đợt giảm giá (giống phiếu giảm giá), tính từ khoảng ngày + công tắc bật / tắt.
export const TRANG_THAI = {
  DANG_DIEN_RA: { label: 'Đang diễn ra', cls: 'ad-pill-green' },
  SAP_DIEN_RA: { label: 'Sắp diễn ra', cls: 'ad-pill-amber' },
  DA_KET_THUC: { label: 'Đã kết thúc', cls: 'ad-pill-gray' },
  NGUNG_HOAT_DONG: { label: 'Ngừng hoạt động', cls: 'ad-pill-red' },
}

/** Công tắc đang bật (cột trang_thai = 1). Chưa xét ngày. */
export const dangHoatDong = (d) => d.trangThai === 1

/** Lấy 'yyyy-mm-dd' từ chuỗi ngày giờ backend trả ('2026-08-16T00:00:00'). */
export const ngayIso = (v) => (v ? String(v).substring(0, 10) : '')

/** Đã qua ngày kết thúc (hôm nay > ngày kết thúc): đợt kết thúc, công tắc biến mất. */
export const daKetThuc = (d, today = todayIso()) => today > ngayIso(d.ngayKetThuc)

/**
 * Trạng thái hiển thị:
 *  - quá ngày kết thúc               -> Đã kết thúc (không phụ thuộc công tắc)
 *  - còn hạn nhưng công tắc đang tắt  -> Ngừng hoạt động
 *  - chưa tới ngày bắt đầu            -> Sắp diễn ra
 *  - đang trong khoảng ngày           -> Đang diễn ra
 */
export function tinhTrangThai(d, today = todayIso()) {
  if (daKetThuc(d, today)) return 'DA_KET_THUC'
  if (!dangHoatDong(d)) return 'NGUNG_HOAT_DONG'
  if (today < ngayIso(d.ngayBatDau)) return 'SAP_DIEN_RA'
  return 'DANG_DIEN_RA'
}

/** Mã dự phòng khi chưa gọi được backend: DGG + số thứ tự 3 chữ số (vd DGG009), tính từ các mã đã có. */
export function taoMaDot(maDaCo = []) {
  let max = 0
  for (const m of maDaCo) {
    const r = /^DGG(\d{1,6})$/i.exec(String(m).trim())
    if (r) max = Math.max(max, Number(r[1]))
  }
  return 'DGG' + String(max + 1).padStart(3, '0')
}
