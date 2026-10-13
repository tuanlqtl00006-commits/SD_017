// Quy tắc chung cho MỌI mã tự cấp (khớp MaUtil.java ở backend):
//   TIỀN TỐ + số thứ tự, số thứ tự có TỐI THIỂU 2 chữ số, KHÔNG đệm thêm số 0.
//   HD01, HD02 ... HD09, HD10 ... HD99, HD100, HD1000 (không còn HD000001).
//   NV01, KH01, SP01, DGG01, DM01, TH01, LQT01 ...

/** Số chữ số tối thiểu của phần số thứ tự. */
export const SO_CHU_SO_TOI_THIEU = 2

/** 1 -> '01', 9 -> '09', 10 -> '10', 100 -> '100'. */
export function soThuTu(n) {
  return String(Math.max(0, Math.trunc(Number(n) || 0))).padStart(SO_CHU_SO_TOI_THIEU, '0')
}

/** ('HD', 1) -> 'HD01'; ('HD', 100) -> 'HD100'. */
export function dinhDangMa(tienTo, n) {
  return `${tienTo ?? ''}${soThuTu(n)}`
}

/**
 * Rút gọn mã cũ nhiều số 0: 'HD000001' -> 'HD01', 'NV0010' -> 'NV10', 'DC000' -> 'DC00'.
 * Mã không có dạng "chữ cái + số" (VCH7QX2YN, SP01-DEN-4U-G5...) thì giữ nguyên.
 */
export function rutGonMa(ma) {
  if (ma === null || ma === undefined) return ma
  const m = /^([A-Za-z]+)(\d{1,9})$/.exec(String(ma).trim())
  return m ? m[1] + soThuTu(Number(m[2])) : ma
}

/** 'SP001-DEN-4U-G5' -> 'SP01-DEN-4U-G5' (chỉ rút gọn phần mã sản phẩm ở đầu). */
export function rutGonMaBienThe(ma) {
  if (ma === null || ma === undefined) return ma
  const s = String(ma)
  const i = s.indexOf('-')
  return i <= 0 ? rutGonMa(s) : rutGonMa(s.slice(0, i)) + s.slice(i)
}
