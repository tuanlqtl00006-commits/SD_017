// Các bảng thuộc tính của sản phẩm trong ERD (danh_muc, thuong_hieu, xuat_xu, chat_lieu, do_cung,
// diem_can_bang, mau_sac, trong_luong, chu_vi). Cả 9 bảng cùng cấu trúc: id, ma, ten, trang_thai
// nên dùng chung một trang quản lý (ThuocTinhManager.vue), chỉ khác cấu hình ở đây.
//   slug   : đường dẫn /san-pham/<slug>
//   prefix : tiền tố mã gợi ý khi tạo mới
//   seed   : dữ liệu mẫu (backend chưa có API)

export const THUOC_TINH = {
  'danh-muc': {
    slug: 'danh-muc', label: 'Danh mục', icon: 'bi-folder', prefix: 'DM', example: 'Vợt cầu lông',
    seed: ['Vợt cầu lông', 'Giày cầu lông', 'Quần áo thể thao', 'Túi - balo', 'Phụ kiện', 'Quả cầu lông'],
  },
  'thuong-hieu': {
    slug: 'thuong-hieu', label: 'Thương hiệu', icon: 'bi-award', prefix: 'TH', example: 'Yonex',
    seed: ['Yonex', 'Lining', 'Victor', 'Mizuno', 'Kumpoo'],
  },
  'xuat-xu': {
    slug: 'xuat-xu', label: 'Xuất xứ', icon: 'bi-globe', prefix: 'XX', example: 'Nhật Bản',
    seed: ['Nhật Bản', 'Trung Quốc', 'Đài Loan', 'Việt Nam'],
  },
  'chat-lieu': {
    slug: 'chat-lieu', label: 'Chất liệu', icon: 'bi-diagram-3', prefix: 'CL', example: 'Carbon',
    seed: ['Carbon', 'Graphite', 'Nhôm', 'Vải lưới', 'Da PU'],
  },
  'do-cung': {
    slug: 'do-cung', label: 'Độ cứng', icon: 'bi-sliders', prefix: 'DC', example: 'Trung bình',
    seed: ['Mềm', 'Trung bình', 'Cứng', 'Rất cứng'],
  },
  'diem-can-bang': {
    slug: 'diem-can-bang', label: 'Điểm cân bằng', icon: 'bi-bullseye', prefix: 'CB', example: 'Nặng đầu',
    seed: ['Nặng đầu', 'Cân bằng', 'Nhẹ đầu'],
  },
  'mau-sac': {
    slug: 'mau-sac', label: 'Màu sắc', icon: 'bi-palette', prefix: 'MS', example: 'Đen',
    seed: ['Đen', 'Trắng', 'Đỏ', 'Xanh dương', 'Vàng'],
  },
  'trong-luong': {
    slug: 'trong-luong', label: 'Trọng lượng', icon: 'bi-speedometer', prefix: 'TL', example: '4U (80-84g)',
    seed: ['2U (90-94g)', '3U (85-89g)', '4U (80-84g)', '5U (75-79g)'],
  },
  'chu-vi': {
    slug: 'chu-vi', label: 'Chu vi', icon: 'bi-circle', prefix: 'CV', example: 'G5',
    seed: ['G4', 'G5', 'G6'],
  },
}

export const THUOC_TINH_LIST = Object.values(THUOC_TINH)

// Trạng thái dùng chung cho thuộc tính, sản phẩm, biến thể (cột trang_thai: 1 = hoạt động, 0 = ngưng)
export const TRANG_THAI = {
  HOAT_DONG: { label: 'Đang hoạt động', cls: 'ad-pill-green' },
  NGUNG: { label: 'Ngưng hoạt động', cls: 'ad-pill-red' },
}
export const trangThaiOf = (item) => (item.hoatDong ? 'HOAT_DONG' : 'NGUNG')

/** Sinh mã gợi ý dạng DM001, không trùng với mã đã có. */
export function taoMa(prefix, existing = []) {
  let n = existing.length + 1
  let code
  do {
    code = `${prefix}${String(n++).padStart(3, '0')}`
  } while (existing.includes(code))
  return code
}
