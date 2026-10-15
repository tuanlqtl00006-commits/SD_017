// Hằng số của trang khách hàng. gioiTinh / trangThai ở backend là số: giới tính 1 = Nam, 0 = Nữ; trạng thái 1 = hoạt động, 0 = ngừng.

export const GIOI_TINH_KHACH = {
  1: 'Nam',
  0: 'Nữ',
}

export const TRANG_THAI_KHACH = {
  1: { label: 'Hoạt động', cls: 'ad-pill-green' },
  0: { label: 'Ngừng hoạt động', cls: 'ad-pill-red' },
}

/** Nhãn giới tính; chưa khai (null) thì để trống. */
export function nhanGioiTinhKhach(v) {
  return v === null || v === undefined ? '' : (GIOI_TINH_KHACH[v] ?? '')
}
