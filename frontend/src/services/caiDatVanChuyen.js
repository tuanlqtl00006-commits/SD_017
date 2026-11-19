import { reactive } from 'vue'
import api from './api'
import { CHU_DE, nghe } from './suKien'

/*
 * Cài đặt vận chuyển (phí giao hàng mỗi đơn + miễn phí theo giá trị đơn), dùng chung cho màn bán quầy, giỏ hàng / thanh toán online,
 * sửa đơn. Tải một lần khi màn đầu tiên cần, và tải lại khi quản lý vừa đổi cài đặt (kênh su-kien, chủ đề "cai-dat").
 * Chưa tải được thì màn hình hiện "chưa tính phí"; số chốt cuối cùng luôn do máy chủ tính.
 */
// tinhTheoKm / kmCoBan / buocKm / phiMoiBuoc: phí theo quãng đường (phiGiaoHang cho kmCoBan km đầu, mỗi buocKm km cộng phiMoiBuoc)
export const caiDatVanChuyen = reactive({ mienPhiBat: false, mienPhiTu: 0, phiGiaoHang: 0, tinhTheoKm: false, kmCoBan: 0, buocKm: 0, phiMoiBuoc: 0, daTai: false })

let dangTai = null
let daNghe = false

export function nhanCaiDatVanChuyen(d) {
  if (!d || typeof d !== 'object' || Array.isArray(d)) return // máy chủ không trả cài đặt: giữ "chưa tải", màn hình hiện chưa tính phí
  caiDatVanChuyen.mienPhiBat = !!d?.mienPhiBat
  caiDatVanChuyen.mienPhiTu = Number(d?.mienPhiTu) || 0
  caiDatVanChuyen.phiGiaoHang = Number(d?.phiGiaoHang) || 0
  caiDatVanChuyen.tinhTheoKm = !!d?.tinhTheoKm
  caiDatVanChuyen.kmCoBan = Number(d?.kmCoBan) || 0
  caiDatVanChuyen.buocKm = Number(d?.buocKm) || 0
  caiDatVanChuyen.phiMoiBuoc = Number(d?.phiMoiBuoc) || 0
  caiDatVanChuyen.daTai = true
}

export function taiCaiDatVanChuyen() {
  if (!dangTai) {
    // Promise.resolve().then(...): lỗi gọi API (kể cả lỗi ném ngay) không bao giờ làm hỏng màn đang mở
    dangTai = Promise.resolve()
      .then(() => api.get('/cua-hang/cai-dat-van-chuyen'))
      .then((r) => nhanCaiDatVanChuyen(r?.data))
      .catch(() => {})
      .finally(() => {
        dangTai = null
      })
  }
  return dangTai
}

/** Gọi ở màn cần biết ngưỡng miễn phí: tải nếu chưa có, và tự cập nhật khi quản lý đổi cài đặt. */
export function dungCaiDatVanChuyen() {
  if (!daNghe) {
    daNghe = true
    nghe([CHU_DE.CAI_DAT], () => taiCaiDatVanChuyen())
  }
  if (!caiDatVanChuyen.daTai) taiCaiDatVanChuyen()
  return caiDatVanChuyen
}
