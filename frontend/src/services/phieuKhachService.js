// Phiếu giảm giá khách xem được ở trang thanh toán online (dùng chung cho khối MÃ GIẢM GIÁ và bước kiểm tra trước khi đặt hàng).
// Khách đã đăng nhập: phiếu công khai + phiếu riêng của khách (/khach/phieu-giam-gia); chưa đăng nhập: chỉ phiếu công khai.
// Máy chủ đã bỏ phiếu hết hạn / chưa tới ngày / hết lượt. Mỗi phiếu: { ma, ten, caNhan, loaiGiam, giaTriGiam, giamToiDa,
// donToiThieu, dungDuoc, lyDo, soTienGiam }.
import api from './api'
import { layPhien } from './phien'
import { formatMoney } from '../utils/format'
import { tinhGiamPhieu } from '../utils/tinhPhieu'

export function laKhachDangNhap() {
  return layPhien()?.nguoiDung?.vaiTro === 'KHACH_HANG'
}

/**
 * Số tiền phiếu giảm cho đơn `tong` (phiếu công khai, tính tạm trên máy; máy chủ vẫn tính lại khi đặt hàng).
 * Dùng chung công thức tinhGiamPhieu (24/11, Task 2.2): chưa đủ đơn tối thiểu -> 0; % có trần giảm tối đa; kẹp trong [0, tong].
 */
export function tinhGiamTam(p, tong) {
  return tinhGiamPhieu({
    loaiGiam: p.hinhThucGiam === 1 ? 'PHAN_TRAM' : 'TIEN_MAT',
    giaTri: p.giaTriGiam,
    giamToiDa: p.giamToiDa,
    donToiThieu: p.donToiThieu,
  }, tong).giam
}

/** Danh sách phiếu cho đơn có tiền hàng `tong`; ẩn các phiếu đã hết hạn hoặc ngừng hoạt động; lỗi mạng thì trả []. */
export async function taiPhieuChoKhach(tong) {
  let list = []
  if (laKhachDangNhap()) {
    try {
      const ds = (await api.get('/khach/phieu-giam-gia', { params: { tongTien: tong } })).data
      if (Array.isArray(ds)) list = ds
    } catch {
      // rơi xuống lấy phiếu công khai bên dưới
    }
  }
  if (!list.length) {
    try {
      const ds = (await api.get('/cua-hang/phieu-giam-gia', { params: { tongTien: tong } })).data
      if (Array.isArray(ds)) {
        list = ds.map((p) => ({
          ma: p.maPhieu,
          ten: p.tenPhieu,
          caNhan: false,
          loaiGiam: p.hinhThucGiam,
          giaTriGiam: p.giaTriGiam,
          giamToiDa: p.giamToiDa,
          donToiThieu: p.donToiThieu,
          ngayKetThuc: p.ngayKetThuc,
          trangThai: p.trangThai ?? 1,
          dungDuoc: p.duDieuKien,
          lyDo: p.duDieuKien ? null : `Cần mua thêm ${formatMoney(p.donToiThieu - tong)}`,
          soTienGiam: p.duDieuKien ? tinhGiamTam(p, tong) : 0,
        }))
      }
    } catch {
      list = []
    }
  }
  // Ẩn các phiếu đã hết hạn và ngừng hoạt động khỏi danh sách chọn
  const now = new Date()
  return list.filter((p) => {
    if (p.trangThai !== undefined && p.trangThai !== null && Number(p.trangThai) === 0) return false
    if (p.ngayKetThuc && new Date(p.ngayKetThuc) < now) return false
    if (p.lyDo && (p.lyDo.includes('hết hạn') || p.lyDo.includes('ngừng hoạt động'))) return false
    return true
  })
}

/** Phiếu dùng được và giảm nhiều nhất; không có thì null. */
export function phieuTotNhat(ds) {
  let tot = null
  for (const v of ds || []) {
    if (v.dungDuoc && Number(v.soTienGiam) > 0 && (!tot || Number(v.soTienGiam) > Number(tot.soTienGiam))) tot = v
  }
  return tot
}

/** Dữ liệu phiếu lưu vào giỏ (cartStore.setVoucher). */
export function phieuChoGio(v) {
  return {
    ma: v.ma,
    ten: v.ten,
    loaiGiam: v.loaiGiam,
    giaTriGiam: v.giaTriGiam,
    giamToiDa: v.giamToiDa,
    donToiThieu: v.donToiThieu,
    soTienGiam: v.soTienGiam,
  }
}
