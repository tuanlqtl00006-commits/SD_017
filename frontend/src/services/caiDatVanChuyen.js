import { reactive } from 'vue'
import api from './api'
import { CHU_DE, nghe } from './suKien'

/*
 * Cài đặt miễn phí vận chuyển theo giá trị đơn, dùng chung cho màn bán quầy, giỏ hàng / thanh toán online, sửa đơn.
 * Tải một lần khi màn đầu tiên cần, và tải lại khi quản lý vừa đổi cài đặt (kênh su-kien, chủ đề "cai-dat").
 * Chưa tải được thì coi như đang tắt: màn hình hiện phí theo tỉnh, còn số chốt cuối cùng luôn do máy chủ tính.
 */
export const caiDatVanChuyen = reactive({ mienPhiBat: false, mienPhiTu: 0, daTai: false })

let dangTai = null
let daNghe = false

export function nhanCaiDatVanChuyen(d) {
  caiDatVanChuyen.mienPhiBat = !!d?.mienPhiBat
  caiDatVanChuyen.mienPhiTu = Number(d?.mienPhiTu) || 0
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
