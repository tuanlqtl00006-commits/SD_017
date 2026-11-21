import { createApp, h, reactive } from 'vue'
import ConfirmDialog from '../components/common/ConfirmDialog.vue'

/*
 * Hộp xác nhận dùng chung (góp ý 21/11: mọi thao tác thêm / sửa / xóa / gửi phải hỏi lại trước khi làm).
 *   if (!(await hoiXacNhan({ title: 'Lưu khách hàng', message: 'Bạn có chắc muốn lưu?' }))) return
 * Hiện đúng ConfirmDialog sẵn có của dự án (giao diện như các hộp xác nhận khác). Trả về true khi bấm nút xác nhận,
 * false khi bấm Hủy / nút đóng / Esc / bấm ra ngoài. Tự gắn vào <body> ở lần hỏi đầu tiên (cả khi chạy test từng màn riêng).
 */
const hop = reactive({ mo: false, title: '', message: '', confirmText: 'Xác nhận', variant: 'primary', icon: '' })
let traLoi = null
let oGan = null

function ganVaoTrang() {
  if (oGan && document.body.contains(oGan)) return
  oGan = document.createElement('div')
  oGan.setAttribute('data-xac-nhan-chung', '')
  document.body.appendChild(oGan)
  createApp({
    render: () =>
      hop.mo
        ? h(ConfirmDialog, {
            title: hop.title,
            message: hop.message,
            confirmText: hop.confirmText,
            variant: hop.variant,
            icon: hop.icon,
            onConfirm: () => dong(true),
            onCancel: () => dong(false),
          })
        : null,
  }).mount(oGan)
}

function dong(ok) {
  const r = traLoi
  traLoi = null
  hop.mo = false
  r?.(ok)
}

/**
 * Hỏi xác nhận. variant 'danger' cho thao tác xóa / hủy (nút đỏ).
 * @returns {Promise<boolean>}
 */
export function hoiXacNhan({ title, message = '', confirmText = 'Xác nhận', variant = 'primary', icon = '' } = {}) {
  if (typeof document === 'undefined') return Promise.resolve(true)
  ganVaoTrang()
  if (traLoi) dong(false) // đang có hộp khác mở thì coi như Hủy hộp cũ
  Object.assign(hop, { mo: true, title, message, confirmText, variant, icon })
  return new Promise((resolve) => {
    traLoi = resolve
  })
}
