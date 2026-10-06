// Các bảng thuộc tính của sản phẩm trong ERD (danh_muc, thuong_hieu, xuat_xu, chat_lieu, do_cung,
// diem_can_bang, mau_sac, trong_luong, chu_vi). Cả 9 bảng cùng cấu trúc: id, ma, ten, trang_thai
// nên dùng chung một trang quản lý (ThuocTinhManager.vue), chỉ khác cấu hình ở đây.
//   slug   : đường dẫn /san-pham/<slug>
//   prefix : tiền tố mã gợi ý khi tạo mới

export const THUOC_TINH = {
  'danh-muc': {
    slug: 'danh-muc', label: 'Danh mục', icon: 'bi-folder', prefix: 'DM', example: 'Vợt cầu lông',
  },
  'thuong-hieu': {
    slug: 'thuong-hieu', label: 'Thương hiệu', icon: 'bi-award', prefix: 'TH', example: 'Yonex',
  },
  'xuat-xu': {
    slug: 'xuat-xu', label: 'Xuất xứ', icon: 'bi-globe', prefix: 'XX', example: 'Nhật Bản',
  },
  'chat-lieu': {
    slug: 'chat-lieu', label: 'Chất liệu', icon: 'bi-diagram-3', prefix: 'CL', example: 'Carbon',
  },
  'do-cung': {
    slug: 'do-cung', label: 'Độ cứng', icon: 'bi-sliders', prefix: 'DC', example: 'Trung bình',
  },
  'diem-can-bang': {
    slug: 'diem-can-bang', label: 'Điểm cân bằng', icon: 'bi-bullseye', prefix: 'CB', example: 'Nặng đầu',
  },
  'mau-sac': {
    slug: 'mau-sac', label: 'Màu sắc', icon: 'bi-palette', prefix: 'MS', example: 'Đen',
  },
  'trong-luong': {
    slug: 'trong-luong', label: 'Trọng lượng', icon: 'bi-speedometer', prefix: 'TL', example: '4U (80-84g)',
  },
  'chu-vi': {
    slug: 'chu-vi', label: 'Chu vi', icon: 'bi-circle', prefix: 'CV', example: 'G5',
  },
}

export const THUOC_TINH_LIST = Object.values(THUOC_TINH)

// Trạng thái dùng chung cho thuộc tính, sản phẩm, biến thể (cột trang_thai: 1 = hoạt động, 0 = ngưng)
export const TRANG_THAI = {
  HOAT_DONG: { label: 'Đang hoạt động', cls: 'ad-pill-green' },
  NGUNG: { label: 'Ngưng hoạt động', cls: 'ad-pill-red' },
}
export const trangThaiOf = (item) => (item.hoatDong ? 'HOAT_DONG' : 'NGUNG')

/** Mã kế tiếp dạng DM001: lấy số lớn nhất đang có + 1 (giống cách backend cấp mã). Chỉ để hiển thị trước. */
export function taoMa(prefix, existing = []) {
  let max = 0
  for (const ma of existing) {
    const n = Number(String(ma ?? '').replace(/\D/g, ''))
    if (!Number.isNaN(n) && n > max) max = n
  }
  return `${prefix}${String(max + 1).padStart(3, '0')}`
}
