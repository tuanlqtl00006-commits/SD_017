// Hàm phụ cho địa chỉ hành chính Việt Nam (sau sáp nhập 07/2025: chỉ còn Tỉnh/Thành phố + Phường/Xã, không còn Quận/Huyện).

/** Bỏ dấu, chữ thường, bỏ tiền tố "Thành phố"/"Tỉnh" để so khớp tên (vd 'Thành phố Hà Nội' ~ 'Hà Nội'). */
export function chuanHoaTenDonVi(ten = '') {
  return String(ten)
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .toLowerCase()
    .replace(/^(thanh pho|tinh)\s+/, '')
    .replace(/\s+/g, ' ')
    .trim()
}

/** Ghép địa chỉ thành một dòng, bỏ phần trống: 'Số 1, Phường A, Thành phố B'. Cũ có Quận/Huyện thì vẫn hiện. */
export function ghepDiaChi(diaChi) {
  const { diaChiCuThe, phuongXa, quanHuyen, tinhThanh, tinhThanhPho } = diaChi ?? {}
  return [diaChiCuThe, phuongXa, quanHuyen, tinhThanh ?? tinhThanhPho]
    .map((x) => (x == null ? '' : String(x).trim()))
    .filter(Boolean)
    .join(', ')
}
