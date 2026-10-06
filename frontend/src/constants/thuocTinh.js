
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

export const TRANG_THAI = {
  HOAT_DONG: { label: 'Đang hoạt động', cls: 'ad-pill-green' },
  NGUNG: { label: 'Ngưng hoạt động', cls: 'ad-pill-red' },
}
export const trangThaiOf = (item) => (item.hoatDong ? 'HOAT_DONG' : 'NGUNG')


export function taoMa(prefix, existing = []) {
  let max = 0
  for (const ma of existing) {
    const n = Number(String(ma ?? '').replace(/\D/g, ''))
    if (!Number.isNaN(n) && n > max) max = n
  }
  return `${prefix}${String(max + 1).padStart(3, '0')}`
}
