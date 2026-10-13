import { normalizeText } from './text'

// Màu hiển thị (chấm màu) theo TÊN màu trong bảng mau_sac. Bảng mau_sac chỉ có tên nên đoán mã màu từ tên;
// tên lạ thì dùng màu xám nhạt. Dùng chung cho thêm sản phẩm (nhóm biến thể theo màu), hóa đơn, đợt giảm giá.
export const MAU_HEX = {
  den: '#1f2937',
  trang: '#ffffff',
  do: '#dc3545',
  'xanh duong': '#0977ec',
  'xanh': '#0977ec',
  'xanh la': '#1f9d55',
  'xanh ngoc': '#14b8a6',
  vang: '#f5b301',
  hong: '#e75480',
  cam: '#fd7e14',
  tim: '#6f42c1',
  xam: '#9ca3af',
  bac: '#c0c6cf',
  nau: '#8b5a2b',
  'vang dong': '#c9a227',
}

/** 'Xanh dương' -> '#0977ec'; tên chưa có trong bảng -> '#cbd5e1' */
export function mauHex(ten) {
  return MAU_HEX[normalizeText(ten)] ?? '#cbd5e1'
}
