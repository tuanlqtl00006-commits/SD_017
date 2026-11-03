import { reactive, ref } from 'vue'
import api from '../services/api'
import { useToast } from './useToast'
import { formatMoney } from '../utils/format'

// Số thông báo hiện trên menu / chuông:
// { donCho: đơn đặt online đang chờ xác nhận, hoTro: hội thoại khách đang chờ hoặc có tin mới, noiBo: tin nội bộ chưa đọc }.
const dem = reactive({ donCho: 0, hoTro: 0, noiBo: 0 })
const donChoList = ref([])
const CHU_KY = 6000
let hen = null
let soNguoiDung = 0
let daKhoiTaoDon = false
const daBietDonIds = new Set()

async function taiLai() {
  const [resChat, resDon] = await Promise.allSettled([
    api.get('/chat/chua-doc'),
    api.get('/hoa-don', { params: { loaiDon: 2, trangThai: 1, page: 0, size: 8 } }),
  ])

  if (resChat.status === 'fulfilled') {
    dem.hoTro = Number(resChat.value.data?.hoTro) || 0
    dem.noiBo = Number(resChat.value.data?.noiBo) || 0
  }

  if (resDon.status === 'fulfilled') {
    const data = resDon.value.data || {}
    const content = Array.isArray(data.content) ? data.content : []
    dem.donCho = Number(data.totalElements ?? content.length) || 0
    donChoList.value = content

    if (!daKhoiTaoDon) {
      daKhoiTaoDon = true
      for (const d of content) {
        if (d?.id != null) daBietDonIds.add(d.id)
      }
    } else {
      const toast = useToast()
      let coDonMoi = false
      // Duyệt từ đơn cũ hơn lên mới hơn để hiện thông báo đúng thứ tự
      for (const d of [...content].reverse()) {
        if (d?.id != null && !daBietDonIds.has(d.id)) {
          daBietDonIds.add(d.id)
          coDonMoi = true
          const tenKhach = d.tenKhachHang || 'Khách hàng'
          const tien = formatMoney(d.tongTien ?? 0)
          toast.success(
            `${tenKhach} vừa đặt đơn ${d.maHoaDon} (${tien}) — Đang chờ xác nhận.`,
            `Đơn hàng mới ${d.maHoaDon}!`,
            7000
          )
        }
      }
      if (coDonMoi && typeof window !== 'undefined') {
        window.dispatchEvent(new CustomEvent('footstyle:don-moi'))
      }
    }
  }
}

function onStoragePing(e) {
  if (e.key === 'footstyle_new_order_ping' && e.newValue) {
    taiLai()
  }
}

function batDau() {
  soNguoiDung++
  if (hen) return
  taiLai()
  if (typeof window !== 'undefined') {
    window.addEventListener('storage', onStoragePing)
  }
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
    if (typeof window !== 'undefined') {
      window.removeEventListener('storage', onStoragePing)
    }
  }
}

export function useChatChuaDoc() {
  return { dem, donChoList, taiLai, batDau, dung }
}
