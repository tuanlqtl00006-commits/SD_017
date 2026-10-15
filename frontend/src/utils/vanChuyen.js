// Đơn vị vận chuyển của hóa đơn giao hàng: nhận ra GHN / GHTK / Viettel Post từ tên ghi trên hóa đơn để hiện logo.
// Cô: "logo của đơn vị giao hàng (Giao Hàng Nhanh, Giao Hàng Tiết Kiệm)... sau này các bạn kết nối bên nào thì in vào đây".
//
// Logo: KHÔNG có sẵn file logo chính thức của các hãng trong dự án (logo là tài sản của từng hãng, không tự vẽ lại).
// Muốn hiện logo thật: bỏ file PNG vào frontend/public/van-chuyen/ với đúng tên ghn.png, ghtk.png, vtp.png (xem README.txt trong thư mục đó).
// Chưa có file hoặc đơn vị khác (kết nối thêm sau này) thì hiện nhãn chữ tên viết tắt - màn hình và bản in đều tự chuyển sang nhãn chữ.
import { normalizeText } from './text'

export const THU_MUC_LOGO = '/van-chuyen'

export const DON_VI_VAN_CHUYEN = [
  { ma: 'GHN', ten: 'Giao Hàng Nhanh', logo: `${THU_MUC_LOGO}/ghn.png`, tuKhoa: ['ghn', 'giao hang nhanh'] },
  { ma: 'GHTK', ten: 'Giao Hàng Tiết Kiệm', logo: `${THU_MUC_LOGO}/ghtk.png`, tuKhoa: ['ghtk', 'giao hang tiet kiem'] },
  { ma: 'VTP', ten: 'Viettel Post', logo: `${THU_MUC_LOGO}/vtp.png`, tuKhoa: ['viettel post', 'viettelpost', 'vtp'] },
]

const khopTuKhoa = (chuan, tuKhoa) => new RegExp(`(^|[^a-z0-9])${tuKhoa}([^a-z0-9]|$)`).test(chuan)

/**
 * Tên đơn vị ghi trên hóa đơn -> { ma, ten, logo }.
 *  - Nhận ra: ma 'GHN' / 'GHTK' / 'VTP', ten đầy đủ, logo là đường dẫn file logo.
 *  - Đơn vị khác: ma '' (hiện nhãn chữ bằng chính tên), logo null.
 *  - Trống: null (không hiện gì).
 */
export function nhanDienVanChuyen(tenGhiTrenHoaDon) {
  const goc = String(tenGhiTrenHoaDon ?? '').replace(/\s+/g, ' ').trim()
  if (!goc) return null
  const chuan = normalizeText(goc)
  const dv = DON_VI_VAN_CHUYEN.find((d) => d.tuKhoa.some((k) => khopTuKhoa(chuan, k)))
  return dv ? { ma: dv.ma, ten: dv.ten, logo: dv.logo } : { ma: '', ten: goc, logo: null }
}
