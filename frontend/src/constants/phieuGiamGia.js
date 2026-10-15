import { formatMoney } from '../utils/format'
import { bayGioDayDu, viTriTheoMoc } from '../utils/ngayGio'

export const HINH_THUC = {
  CONG_KHAI: { label: 'Công khai', icon: 'bi-globe2', cls: 'ad-pill-blue' },
  CA_NHAN: { label: 'Cá nhân', icon: 'bi-person-fill', cls: 'ad-pill-purple' },
}

export const LOAI_GIAM = {
  PHAN_TRAM: { label: 'Phần trăm (%)' },
  TIEN_MAT: { label: 'Số tiền (₫)' },
}

export const TRANG_THAI = {
  DANG_HOAT_DONG: { label: 'Đang hoạt động', cls: 'ad-pill-green' },
  SAP_DIEN_RA: { label: 'Sắp diễn ra', cls: 'ad-pill-amber' },
  DA_KET_THUC: { label: 'Đã kết thúc', cls: 'ad-pill-gray' },
  NGUNG_HOAT_DONG: { label: 'Ngưng hoạt động', cls: 'ad-pill-red' },
}

/**
 * Trạng thái hiển thị được tính từ NGÀY GIỜ áp dụng và cờ hoatDong (công tắc bật/tắt):
 *  - công tắc đang tắt (kể cả phiếu đã quá giờ kết thúc) -> "Ngưng hoạt động"
 *  - công tắc bật, quá giờ kết thúc                       -> "Đã kết thúc"
 *  - chưa tới giờ bắt đầu                                 -> "Sắp diễn ra"
 *  - đang trong khoảng ngày giờ                           -> "Đang hoạt động"
 * Thời điểm so sánh `bayGio` là 'yyyy-MM-ddTHH:mm:ss' (hoặc chỉ ngày: tính từ 00:00). Phiếu cũ lưu theo ngày vẫn chạy đúng
 * (bắt đầu 00:00, kết thúc hết ngày 23:59:59).
 */
export function tinhTrangThai(p, bayGio = bayGioDayDu()) {
  if (!p.hoatDong) return 'NGUNG_HOAT_DONG'
  const viTri = viTriTheoMoc(p.ngayBatDau, p.ngayKetThuc, bayGio)
  if (viTri === 'KET_THUC') return 'DA_KET_THUC'
  if (viTri === 'SAP') return 'SAP_DIEN_RA'
  return 'DANG_HOAT_DONG'
}

/** Đã quá giờ kết thúc: công tắc hiện TẮT và bị khóa. */
export function isExpired(p, bayGio = bayGioDayDu()) {
  return viTriTheoMoc(p.ngayBatDau, p.ngayKetThuc, bayGio) === 'KET_THUC'
}

/** 60 -> '60%', 50000 -> '50.000 ₫' */
export function formatGiaTri(p) {
  return p.loaiGiam === 'PHAN_TRAM' ? `${p.giaTri}%` : formatMoney(p.giaTri)
}

const CODE_CHARS = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'

/** Sinh mã dạng VCH + 6 ký tự, không trùng với các mã đã có. */
export function taoMaPhieu(existing = []) {
  let code
  do {
    code = 'VCH' + Array.from({ length: 6 }, () => CODE_CHARS[Math.floor(Math.random() * CODE_CHARS.length)]).join('')
  } while (existing.includes(code))
  return code
}
