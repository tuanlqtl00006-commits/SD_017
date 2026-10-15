import { todayIso } from '../utils/format'
import { dinhDangMa } from '../utils/ma'

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

/** Đã qua ngày kết thúc (hôm nay > ngày kết thúc). Đợt đã kết thúc thì công tắc hiện TẮT và bị khóa. */
export const daKetThuc = (d, today = todayIso()) => today > ngayIso(d.ngayKetThuc)

/**
 * Trạng thái hiển thị:
 *  - công tắc đang tắt (kể cả đợt đã quá ngày kết thúc) -> Ngừng hoạt động
 *  - công tắc bật, quá ngày kết thúc                     -> Đã kết thúc
 *  - chưa tới ngày bắt đầu                               -> Sắp diễn ra
 *  - đang trong khoảng ngày                              -> Đang diễn ra
 */
export function tinhTrangThai(d, today = todayIso()) {
  if (!dangHoatDong(d)) return 'NGUNG_HOAT_DONG'
  if (daKetThuc(d, today)) return 'DA_KET_THUC'
  if (today < ngayIso(d.ngayBatDau)) return 'SAP_DIEN_RA'
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
