import { normalizeText } from './text'

/**
 * Mỗi danh mục có bộ thuộc tính riêng:
 *  - Vợt cầu lông (và Vợt cầu lông người mới): thêm Độ cứng, Điểm cân bằng; biến thể theo Màu sắc + Trọng lượng + Chu vi cán.
 *  - Các danh mục còn lại (Phụ kiện, Túi - balo, Quần áo thể thao, Quả cầu lông...): chỉ Thương hiệu, Xuất xứ, Chất liệu;
 *    biến thể chỉ theo Màu sắc.
 * CSDL vẫn bắt buộc có Độ cứng / Điểm cân bằng / Trọng lượng / Chu vi, nên với danh mục không phải vợt hệ thống tự gán mục
 * "Không áp dụng" (mã DC000, CB000, TL000, CV000). Mục này không hiện trong ô chọn và danh sách thuộc tính.
 */
export const KHONG_AP_DUNG = 'Không áp dụng'

/** Mục "Không áp dụng" của các bảng thuộc tính. */
export const laKhongApDung = (x) => !!x && normalizeText(x.ten ?? '') === 'khong ap dung'

/** Id của mục "Không áp dụng" trong một danh sách thuộc tính (null nếu CSDL chưa có). */
export const idKhongApDung = (danhSach = []) => danhSach.find(laKhongApDung)?.id ?? null

/** Danh mục là vợt (tên có chữ "vợt") thì có đủ bộ thuộc tính vợt. */
export const laDanhMucVot = (tenDanhMuc) => normalizeText(tenDanhMuc ?? '').split(/[^a-z0-9]+/).includes('vot')

/**
 * Chất liệu theo danh mục: mỗi chất liệu được gắn với một hoặc nhiều NHÓM danh mục (VOT, QUAN_AO, ...),
 * danh mục suy ra nhóm từ tên của nó. Chọn danh mục nào thì ô Chất liệu chỉ hiện chất liệu của nhóm đó
 * (vd Quần áo thể thao -> Cotton, Polyester...; không còn Nhôm / Titanium của khung vợt).
 * Danh mục lạ (không nhận ra nhóm) hoặc chất liệu chưa gắn nhóm nào thì không bị lọc.
 */
export const NHOM_DANH_MUC = [
  { ma: 'VOT', ten: 'Vợt cầu lông' },
  { ma: 'QUAN_AO', ten: 'Quần áo thể thao' },
  { ma: 'TUI_BALO', ten: 'Túi - balo' },
  { ma: 'PHU_KIEN', ten: 'Phụ kiện' },
  { ma: 'QUA_CAU', ten: 'Quả cầu lông' },
]

/** Nhóm của một danh mục theo tên (null nếu không nhận ra). Phải khớp NhomDanhMuc.java ở backend. */
export function nhomDanhMuc(tenDanhMuc) {
  const t = normalizeText(tenDanhMuc ?? '')
  const tu = t.split(/[^a-z0-9]+/).filter(Boolean)
  if (!tu.length) return null
  if (tu.includes('vot')) return 'VOT'
  if (t.includes('qua cau')) return 'QUA_CAU'
  if (tu.includes('quan') || tu.includes('ao')) return 'QUAN_AO'
  if (tu.includes('tui') || tu.includes('balo')) return 'TUI_BALO'
  if (t.includes('phu kien')) return 'PHU_KIEN'
  return null
}

/** Chất liệu có dùng được cho danh mục này không (chưa chọn danh mục / danh mục lạ / chưa gắn nhóm: dùng được). */
export function chatLieuHopDanhMuc(chatLieu, tenDanhMuc) {
  const nhom = nhomDanhMuc(tenDanhMuc)
  const ds = chatLieu?.nhom
  return !nhom || !Array.isArray(ds) || ds.length === 0 || ds.includes(nhom)
}
