// Đổi dữ liệu thành danh sách mục cho ô chọn Select2 ({ value, label }).

/**
 * [{ id, ten }, ...] -> [{ value: id, label: ten }, ...]. tatCa: thêm mục đầu "Tất cả ..." có value '' (dùng cho bộ lọc).
 * giaTri / nhan: tên trường khác mặc định (vd { giaTri: 'ma', nhan: 'ten' }).
 */
export function tuDanhSach(ds, { tatCa = '', giaTri = 'id', nhan = 'ten', phu = null } = {}) {
  const kq = (ds ?? []).map((o) => {
    const muc = { value: o[giaTri], label: String(o[nhan] ?? '') }
    if (phu && o[phu] !== undefined && o[phu] !== null && o[phu] !== '') muc.sub = String(o[phu])
    return muc
  })
  return tatCa ? [{ value: '', label: tatCa }, ...kq] : kq
}

/** { khoa: { label } } hoặc { khoa: 'Nhãn' } -> mục chọn. Dùng cho các bảng hằng số trạng thái. */
export function tuBangHang(bang, { tatCa = '', nhan = 'label' } = {}) {
  const kq = Object.entries(bang ?? {}).map(([k, v]) => ({ value: k, label: typeof v === 'string' ? v : String(v?.[nhan] ?? k) }))
  return tatCa ? [{ value: '', label: tatCa }, ...kq] : kq
}

/** Danh sách chữ -> mục chọn (value = label = chữ). */
export function tuChu(cacChu, { tatCa = '' } = {}) {
  const kq = (cacChu ?? []).map((c) => ({ value: c, label: String(c) }))
  return tatCa ? [{ value: '', label: tatCa }, ...kq] : kq
}
