import { reactive } from 'vue'
import api from '../services/api'

// Số tin chưa đọc hiện trên menu / chuông: { hoTro: hội thoại khách đang chờ hoặc có tin mới, noiBo: tin nội bộ chưa đọc }.
// Hỏi lại máy chủ mỗi 15 giây khi đang mở trang quản trị (và ngay khi trang chat vừa đọc xong tin).
const dem = reactive({ hoTro: 0, noiBo: 0 })
const CHU_KY = 15000
let hen = null
let soNguoiDung = 0

async function taiLai() {
  try {
    const res = await api.get('/chat/chua-doc')
    dem.hoTro = Number(res.data?.hoTro) || 0
    dem.noiBo = Number(res.data?.noiBo) || 0
  } catch {
    // mất kết nối / chưa đăng nhập: giữ số cũ, lần sau hỏi lại
  }
}

function batDau() {
  soNguoiDung++
  if (hen) return
  taiLai()
  hen = setInterval(() => {
    if (typeof document !== 'undefined' && document.visibilityState === 'hidden') return
    taiLai()
  }, CHU_KY)
}

function dung() {
  soNguoiDung = Math.max(0, soNguoiDung - 1)
  if (soNguoiDung === 0 && hen) {
    clearInterval(hen)
    hen = null
  }
}

export function useChatChuaDoc() {
  return { dem, taiLai, batDau, dung }
}
