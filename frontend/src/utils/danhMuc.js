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
