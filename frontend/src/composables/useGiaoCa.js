import { computed, reactive } from 'vue'
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

export function useGiaoCa() {
  return { trangThai, dangTrongCa, taiLai, nhanCa, ketCa }
}

/** Xóa trạng thái ca (đăng xuất / đổi tài khoản). */
export function datLaiGiaoCa() {
  Object.assign(trangThai, { daTai: false, dangTai: false, dangMo: null, tongKet: null, goiYTienDauCa: null, nguoiBanGiao: null, thoiGianBanGiao: null, maCaTruoc: null, tienMatCaTruoc: null, chuyenKhoanCaTruoc: null, lichHomNay: [], loi: '' })
}

/** Chỉ dùng trong test. */
export const _datLaiGiaoCa = datLaiGiaoCa
