// Các dòng tiền của một đơn: CÙNG TÊN, CÙNG THỨ TỰ ở mọi màn (bán quầy, giỏ hàng, thanh toán online, chi tiết hóa đơn,
// tra cứu đơn của khách, bản in) - review 05/11 "các màn chưa đồng bộ phần giá phiếu giảm giá, đợt giảm, phí ship".
//
// QUY ƯỚC (đừng đổi riêng ở một màn):
//  - Số lưu trong hóa đơn `tongTien` là tiền hàng ĐÃ trừ giảm theo đợt (Thống kê tính doanh thu theo số này).
//  - Trên màn hình, "Tổng tiền hàng" là tổng theo GIÁ GỐC = tiền hàng sau đợt + tiền giảm theo đợt, rồi trừ từng khoản:
//      Tổng tiền hàng - Giảm theo đợt - Phiếu giảm giá + Phí vận chuyển = Tổng thanh toán
//    nên người xem cộng trừ các dòng là ra đúng tổng (không có dòng nào "chỉ để xem").
//  - Tiền giảm theo đợt không bao giờ âm (giá tăng sau khi bán không thành "giảm" âm).

export const NHAN_TIEN = {
  tongTienHang: 'Tổng tiền hàng',
  giamDot: 'Giảm theo đợt',
  phieu: 'Phiếu giảm giá',
  phiVanChuyen: 'Phí vận chuyển',
  tongThanhToan: 'Tổng thanh toán',
}

const so = (v) => {
  const n = Math.round(Number(v) || 0)
  return Number.isFinite(n) ? n : 0
}

/**
 * @param {object} d
 * @param {number} d.tienHangSauDot  tiền hàng đã trừ đợt (hóa đơn: tongTien; giỏ: tổng giá bán)
 * @param {number} [d.giamTheoDot]   tiền giảm theo đợt (>= 0)
 * @param {number} [d.giamPhieu]     tiền giảm từ phiếu
 * @param {string} [d.maPhieu]       mã phiếu đang dùng ('' / null: không dùng)
 * @param {boolean} [d.coGiaoHang]   đơn có giao hàng (đơn tại quầy không có dòng phí vận chuyển)
 * @param {number|null} [d.phiVanChuyen] phí giao; null = chưa tính được (chưa chọn tỉnh)
 * @param {number} [d.tongThanhToan] tổng khách trả; bỏ trống thì tự tính từ các dòng
 * @returns {{ dong: Array<{khoa:string, nhan:string, so:number|null, dau:''|'-'|'+', ma?:string}>, tongGoc:number, tong:number, khop:boolean }}
 */
export function dongTienDon(d) {
  const sauDot = so(d.tienHangSauDot)
  const giamDot = Math.max(0, so(d.giamTheoDot))
  const giamPhieu = Math.max(0, so(d.giamPhieu))
  const coShip = !!d.coGiaoHang
  const phi = coShip && d.phiVanChuyen !== null && d.phiVanChuyen !== undefined ? Math.max(0, so(d.phiVanChuyen)) : 0
  const tongGoc = sauDot + giamDot
  const tinh = Math.max(0, tongGoc - giamDot - giamPhieu + phi)
  const tong = d.tongThanhToan === undefined || d.tongThanhToan === null ? tinh : so(d.tongThanhToan)

  const dong = [{ khoa: 'tong-tien-hang', nhan: NHAN_TIEN.tongTienHang, so: tongGoc, dau: '' }]
  if (giamDot > 0) dong.push({ khoa: 'giam-dot', nhan: NHAN_TIEN.giamDot, so: giamDot, dau: '-' })
  dong.push({ khoa: 'phieu', nhan: NHAN_TIEN.phieu, so: giamPhieu, dau: '-', ma: d.maPhieu || '' })
  if (coShip) {
    dong.push({
      khoa: 'phi-van-chuyen',
      nhan: NHAN_TIEN.phiVanChuyen,
      so: d.phiVanChuyen === null || d.phiVanChuyen === undefined ? null : phi,
      dau: '+',
    })
  }
  dong.push({ khoa: 'tong-thanh-toan', nhan: NHAN_TIEN.tongThanhToan, so: tong, dau: '' })
  return { dong, tongGoc, tong, khop: tong === tinh }
}

/** Tiền giảm theo đợt của các dòng hàng: tổng (giá gốc - đơn giá) x số lượng, dòng nào giá gốc thấp hơn thì tính 0. */
export function tienGiamTheoDotCuaDong(ds, { giaGoc = 'giaGoc', donGia = 'donGia', soLuong = 'soLuong' } = {}) {
  return (ds ?? []).reduce((t, x) => t + Math.max(0, so(x?.[giaGoc]) - so(x?.[donGia])) * so(x?.[soLuong]), 0)
}
