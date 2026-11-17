// Phí vận chuyển: một mức phí giao hàng mỗi đơn do quản lý cài ở Cài đặt → Cài đặt free ship (góp ý 16/11: bỏ bảng phí viết cứng
// theo tỉnh, cửa hàng chỉ cài phí ship theo hóa đơn). Giống backend CaiDatVanChuyenService.
// caiDat = { mienPhiBat, mienPhiTu, phiGiaoHang } (services/caiDatVanChuyen.js). Tiền hàng so với ngưỡng là tiền hàng đã giảm theo đợt,
// CHƯA trừ phiếu giảm giá. Số chốt cuối cùng luôn do máy chủ tính.

/** Phí giao hàng quản lý đang cài (chưa xét miễn phí). */
export function phiGiaoHangCaiDat(caiDat) {
  const phi = Number(caiDat?.phiGiaoHang)
  return Number.isFinite(phi) && phi > 0 ? phi : 0
}

/** Đơn có tiền hàng này có được miễn phí vận chuyển không. */
export function duocMienPhiVanChuyen(caiDat, tienHang) {
  const nguong = Number(caiDat?.mienPhiTu)
  const tien = Number(tienHang)
  return !!caiDat?.mienPhiBat && nguong > 0 && tien > 0 && tien >= nguong
}

/** Phí giao hàng của đơn có tiền hàng này: đạt ngưỡng miễn phí thì 0, chưa đạt thì phí quản lý cài. */
export function phiVanChuyenDon(caiDat, tienHang) {
  return duocMienPhiVanChuyen(caiDat, tienHang) ? 0 : phiGiaoHangCaiDat(caiDat)
}

/** Còn thiếu bao nhiêu tiền hàng để được miễn phí vận chuyển; 0 = đã đạt, hoặc cửa hàng đang tắt miễn phí. */
export function conThieuDeMienPhi(caiDat, tienHang) {
  const nguong = Number(caiDat?.mienPhiTu)
  if (!caiDat?.mienPhiBat || !(nguong > 0)) return 0
  return Math.max(0, nguong - (Number(tienHang) || 0))
}

/**
 * Phí giao hàng sau khi sửa đơn, giống backend HoaDonSuaService.phiSauKhiSua (phí không theo nơi nhận nên đổi địa chỉ không đổi phí):
 *  - tiền hàng giữ nguyên: giữ phí của đơn (có thể do nhân viên nhập lúc bán tại quầy);
 *  - tiền hàng mới đạt ngưỡng miễn phí: 0;
 *  - trước được miễn phí nhờ đạt ngưỡng, nay tụt dưới ngưỡng: phí quản lý đang cài; còn lại giữ phí cũ.
 */
export function phiSauKhiSuaDon({ caiDat, tienHang, tienHangCu, phiCu }) {
  const cu = Number(phiCu) || 0
  if (Number(tienHang) === Number(tienHangCu)) return cu
  if (duocMienPhiVanChuyen(caiDat, tienHang)) return 0
  if (cu === 0 && duocMienPhiVanChuyen(caiDat, tienHangCu)) return phiGiaoHangCaiDat(caiDat)
  return cu
}
