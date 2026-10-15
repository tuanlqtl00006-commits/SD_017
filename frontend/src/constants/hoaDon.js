// Trạng thái hóa đơn - MỘT nơi dùng chung cho danh sách, chi tiết (dòng thời gian, nút bấm, bản in) và Thống kê.
// Mã trạng thái khớp backend entity/HoaDon.java (tests/hoaDonTrangThaiIn.test.js đọc thẳng file Java để kiểm tra hai bên không lệch).
// Luật "đang ở đâu thì chuyển được sang đâu" do BACKEND quyết định (service/HoaDonLuongTrangThai.java) và trả về trong
// GET /api/hoa-don/{id} -> hoaDon.chuyenDuoc; giao diện chỉ vẽ nút cho đúng các bước đó, không tự suy luận lại.

export const TT = {
  DA_HUY: 0,
  CHO_XAC_NHAN: 1,
  DA_XAC_NHAN: 2,
  CHO_LAY_HANG: 3, // hiển thị là "Chờ giao hàng"
  DANG_GIAO: 4,
  DA_GIAO: 5,
  HOAN_THANH: 6,
  HOAN_TIEN: 7, // tab "Hoàn tiền" theo thiết kế của nhóm, chưa có luồng chuyển sang
  GIAO_THAT_BAI: 8,
}

/** Nhãn + màu nhãn (class .ad-pill-*) của từng trạng thái. */
export const TRANG_THAI_HOA_DON = {
  [TT.DA_HUY]: { label: 'Đã hủy', pill: 'ad-pill-red' },
  [TT.CHO_XAC_NHAN]: { label: 'Chờ xác nhận', pill: 'ad-pill-amber' },
  [TT.DA_XAC_NHAN]: { label: 'Đã xác nhận', pill: 'ad-pill-blue' },
  [TT.CHO_LAY_HANG]: { label: 'Chờ giao hàng', pill: 'ad-pill-purple' },
  [TT.DANG_GIAO]: { label: 'Đang giao hàng', pill: 'ad-pill-blue' },
  [TT.DA_GIAO]: { label: 'Đã giao hàng', pill: 'ad-pill-green' },
  [TT.HOAN_THANH]: { label: 'Đã hoàn thành', pill: 'ad-pill-green' },
  [TT.HOAN_TIEN]: { label: 'Hoàn tiền', pill: 'ad-pill-gray' },
  [TT.GIAO_THAT_BAI]: { label: 'Giao hàng thất bại', pill: 'ad-pill-orange' },
}

export const KHONG_XAC_DINH = { label: 'Không xác định', pill: 'ad-pill-gray' }
export const trangThaiOf = (ma) => TRANG_THAI_HOA_DON[ma] ?? KHONG_XAC_DINH

/** Trạng thái cuối: đã vào đây là khóa, không có nút đổi trạng thái nào nữa (cô: "đã hoàn thành rồi sao còn chuyển trạng thái"). */
export const laTrangThaiCuoi = (ma) => ma === TT.DA_HUY || ma === TT.HOAN_THANH || ma === TT.HOAN_TIEN

/** Thứ tự các tab ở trang danh sách (null = tất cả). */
export const TAB_TRANG_THAI = [
  { value: null, label: 'Tất cả' },
  { value: TT.CHO_XAC_NHAN, label: 'Chờ xác nhận' },
  { value: TT.DA_XAC_NHAN, label: 'Đã xác nhận' },
  { value: TT.CHO_LAY_HANG, label: 'Chờ giao hàng' },
  { value: TT.DANG_GIAO, label: 'Đang giao hàng' },
  { value: TT.GIAO_THAT_BAI, label: 'Giao hàng thất bại' },
  { value: TT.DA_GIAO, label: 'Đã giao hàng' },
  { value: TT.HOAN_THANH, label: 'Đã hoàn thành' },
  { value: TT.DA_HUY, label: 'Đã hủy' },
  { value: TT.HOAN_TIEN, label: 'Hoàn tiền' },
]

/* ----- Dòng thời gian trên trang chi tiết ----- */

// Đơn giao hàng đi đủ 6 bước; đơn tại quầy: tạo đơn -> hoàn thành (thanh toán ngay)
export const LUONG_GIAO_HANG = [
  { code: TT.CHO_XAC_NHAN, label: 'Chờ xác nhận', icon: 'bi-hourglass-split' },
  { code: TT.DA_XAC_NHAN, label: 'Đã xác nhận', icon: 'bi-clipboard-check' },
  { code: TT.CHO_LAY_HANG, label: 'Chờ giao hàng', icon: 'bi-box-seam' },
  { code: TT.DANG_GIAO, label: 'Đang giao hàng', icon: 'bi-truck' },
  { code: TT.DA_GIAO, label: 'Đã giao hàng', icon: 'bi-house-check' },
  { code: TT.HOAN_THANH, label: 'Đã hoàn thành', icon: 'bi-patch-check' },
]
export const LUONG_TAI_QUAY = [
  { code: TT.CHO_XAC_NHAN, label: 'Tạo đơn hàng', icon: 'bi-receipt' },
  { code: TT.HOAN_THANH, label: 'Đã hoàn thành', icon: 'bi-patch-check' },
]
/** Nhánh rẽ nằm ngay sau "Đang giao hàng". */
export const BUOC_GIAO_THAT_BAI = { code: TT.GIAO_THAT_BAI, label: 'Giao hàng thất bại', icon: 'bi-exclamation-triangle' }
export const BUOC_DA_HUY = { code: TT.DA_HUY, label: 'Đã hủy', icon: 'bi-x-circle' }

/* ----- Nút đổi trạng thái ----- */

/**
 * Nút cho bước chuyển `hienTai` -> `sang` của đơn có loại `loaiDon` (2 = giao hàng, 1 = tại quầy).
 * kieu: chinh (xanh) | canhbao (cam) | nguyhiem (đỏ viền).
 */
export function nutChuyen(loaiDon, hienTai, sang) {
  if (sang === TT.DA_HUY) return { text: 'Hủy đơn', icon: 'bi-x-circle', kieu: 'nguyhiem' }
  if (sang === TT.GIAO_THAT_BAI) return { text: 'Giao hàng thất bại', icon: 'bi-exclamation-triangle', kieu: 'canhbao' }
  if (sang === TT.DANG_GIAO && hienTai === TT.GIAO_THAT_BAI) return { text: 'Giao lại', icon: 'bi-arrow-repeat', kieu: 'chinh' }
  if (sang === TT.HOAN_THANH && loaiDon !== 2) return { text: 'Xác nhận thanh toán', icon: 'bi-cash-coin', kieu: 'chinh' }
  const chung = {
    [TT.DA_XAC_NHAN]: { text: 'Xác nhận đơn', icon: 'bi-check2-circle' },
    [TT.CHO_LAY_HANG]: { text: 'Chuẩn bị hàng', icon: 'bi-box-seam' },
    [TT.DANG_GIAO]: { text: 'Giao cho vận chuyển', icon: 'bi-truck' },
    [TT.DA_GIAO]: { text: 'Xác nhận đã giao', icon: 'bi-house-check' },
    [TT.HOAN_THANH]: { text: 'Hoàn thành đơn', icon: 'bi-patch-check' },
  }[sang]
  return { ...(chung ?? { text: 'Cập nhật trạng thái', icon: 'bi-arrow-right-circle' }), kieu: 'chinh' }
}
