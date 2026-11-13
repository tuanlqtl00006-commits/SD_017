import { computed, reactive, ref } from 'vue'
import api from '../services/api'

// Ca đang làm của người đăng nhập (dùng chung cho nút "Ca làm việc" trên header và trang Bán hàng tại quầy).
const trangThai = reactive({
  daTai: false,
  dangTai: false,
  dangMo: null, // ca đang mở (null = chưa nhận ca)
  tongKet: null, // { soHoaDon, doanhThu, tienMat, chuyenKhoan, tienDauCa, tienMatHeThong }
  goiYTienDauCa: null, // tiền ca trước bàn giao
  nguoiBanGiao: null,
  thoiGianBanGiao: null,
  maCaTruoc: null,
  tienMatCaTruoc: null, // tiền mặt ca trước đếm được khi kết ca
  chuyenKhoanCaTruoc: null, // chuyển khoản ca trước hệ thống ghi nhận (người vào ca phải đối soát)
  lichHomNay: [],
  caKhacDangMo: null, // ca của người khác chưa giao (cửa hàng một két: phải giao xong mới vào ca được)
  lyDoChuaNhanCa: null, // máy chủ báo vì sao lúc này chưa nhận ca được (không có lịch, chưa tới giờ, hết giờ, ca đã giao); null = nhận được
  loi: '',
})

async function taiLai() {
  trangThai.dangTai = true
  try {
    const res = await api.get('/giao-ca/hien-tai')
    Object.assign(trangThai, {
      dangMo: res.data?.dangMo ?? null,
      tongKet: res.data?.tongKet ?? null,
      goiYTienDauCa: res.data?.goiYTienDauCa ?? null,
      nguoiBanGiao: res.data?.nguoiBanGiao ?? null,
      thoiGianBanGiao: res.data?.thoiGianBanGiao ?? null,
      maCaTruoc: res.data?.maCaTruoc ?? null,
      tienMatCaTruoc: res.data?.tienMatCaTruoc ?? null,
      chuyenKhoanCaTruoc: res.data?.chuyenKhoanCaTruoc ?? null,
      lichHomNay: res.data?.lichHomNay ?? [],
      caKhacDangMo: res.data?.caKhacDangMo ?? null,
      lyDoChuaNhanCa: res.data?.lyDoChuaNhanCa ?? null,
      loi: '',
      daTai: true,
    })
  } catch (e) {
    trangThai.loi = e.message || 'Không tải được ca làm việc.'
  } finally {
    trangThai.dangTai = false
  }
}

async function nhanCa({ tienDauCa, ghiChu, idLichLamViec, chuyenKhoanCaTruoc = null }) {
  const res = await api.post('/giao-ca/nhan-ca', { tienDauCa, ghiChu, idLichLamViec, chuyenKhoanCaTruoc })
  await taiLai()
  return res.data
}

async function ketCa({ tienMatThucTe, tienBanGiao, idNhanVienNhanBanGiao, ghiChu }) {
  const res = await api.post('/giao-ca/ket-ca', { tienMatThucTe, tienBanGiao, idNhanVienNhanBanGiao, ghiChu })
  await taiLai()
  return res.data
}

const dangTrongCa = computed(() => !!trangThai.dangMo)

// Giờ hiện tại, cập nhật mỗi phút để nhắc giao ca khi quá giờ kết thúc ca
const bayGio = ref(Date.now())
if (typeof window !== 'undefined') setInterval(() => { bayGio.value = Date.now() }, 60_000)

/**
 * Ca đang mở đã quá giờ kết thúc theo lịch (hoặc nhận từ hôm trước) chưa: { tenCa, ketThuc: 'HH:mm', phut } | null.
 * Ca ngoài lịch không có giờ kết thúc nên không nhắc.
 */
const quaGioCa = computed(() => {
  const gc = trangThai.dangMo
  if (!gc?.thoiGianNhanCa) return null
  const ngayNhan = String(gc.thoiGianNhanCa).slice(0, 10)
  const homNay = new Date(bayGio.value - new Date().getTimezoneOffset() * 60_000).toISOString().slice(0, 10)
  const gioKetThuc = !gc.ngoaiLich && /(\d{2}:\d{2})\s*$/.exec(gc.gioCa || '')?.[1]
  if (!gioKetThuc) return ngayNhan < homNay ? { tenCa: 'Ca', ketThuc: null, phut: null, caCu: true } : null
  const ketThuc = new Date(`${ngayNhan}T${gioKetThuc}:00`).getTime()
  const phut = Math.floor((bayGio.value - ketThuc) / 60_000)
  return phut > 0 ? { tenCa: gc.tenCa, ketThuc: gioKetThuc, phut, caCu: ngayNhan < homNay } : null
})

export function useGiaoCa() {
  return { trangThai, dangTrongCa, quaGioCa, taiLai, nhanCa, ketCa }
}

/** Xóa trạng thái ca (đăng xuất / đổi tài khoản). */
export function datLaiGiaoCa() {
  Object.assign(trangThai, { daTai: false, dangTai: false, dangMo: null, tongKet: null, goiYTienDauCa: null, nguoiBanGiao: null, thoiGianBanGiao: null, maCaTruoc: null, tienMatCaTruoc: null, chuyenKhoanCaTruoc: null, lichHomNay: [], caKhacDangMo: null, lyDoChuaNhanCa: null, loi: '' })
}

/** Chỉ dùng trong test. */
export const _datLaiGiaoCa = datLaiGiaoCa
export const _datBayGio = (ms) => { bayGio.value = ms }
