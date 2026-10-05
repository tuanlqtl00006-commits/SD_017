import { formatMoney, todayIso } from '../utils/format'

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
 * Trạng thái hiển thị được tính từ cờ hoatDong (bật/tắt thủ công) và khoảng ngày áp dụng.
 * Phiếu bị tắt thủ công luôn là "Ngưng hoạt động", bất kể ngày.
 */
export function tinhTrangThai(p, today = todayIso()) {
  if (!p.hoatDong) return 'NGUNG_HOAT_DONG'
  if (today < p.ngayBatDau) return 'SAP_DIEN_RA'
  if (today > p.ngayKetThuc) return 'DA_KET_THUC'
  return 'DANG_HOAT_DONG'
}

export function isExpired(p, today = todayIso()) {
  return p.ngayKetThuc < today
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
