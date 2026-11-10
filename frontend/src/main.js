import { createApp } from 'vue'
import { createPinia } from 'pinia'
import './style.css'
import './assets/admin.css'
import App from './App.vue'
import router, { apDungVaiTroMoi } from './router'
import { datXuLyChuaVaoCa, datXuLyDoiVaiTro, datXuLyHetPhien } from './services/phien'
import { useAuth } from './composables/useAuth'
import { datLaiGiaoCa, useGiaoCa } from './composables/useGiaoCa'

// API báo hết phiên (401): xóa phiên, về trang đăng nhập kèm lý do, đăng nhập lại xong quay về đúng trang đang mở
datXuLyHetPhien((lyDo) => {
  useAuth().dangXuat()
  const hienTai = router.currentRoute.value
  if (hienTai.meta?.congKhai) return
  router.replace({
    path: '/dang-nhap',
    query: { quayLai: hienTai.fullPath, hetPhien: lyDo || 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.' },
  })
})

// Nhân viên chưa vào ca mà thêm / sửa / xóa (backend trả 409 CHUA_NHAN_CA): màn đang mở tự báo câu của máy chủ, vẫn ở lại để xem tiếp.
// Ở đây chỉ tải lại trạng thái ca (vd ca vừa bị quản lý đóng hộ) để nút "Ca làm việc" và màn bán hàng hiện đúng.
datXuLyChuaVaoCa(() => {
  if (!['nhan-vien', 'thu-ngan'].includes(useAuth().vaiTro.value)) return
  datLaiGiaoCa()
  useGiaoCa().taiLai()
})

// Quản lý đổi vai trò của người đang làm việc (case khó "chuyển quyền nhân viên"): xem apDungVaiTroMoi
datXuLyDoiVaiTro((vaiTroMoi) => apDungVaiTroMoi(vaiTroMoi, router.currentRoute.value, (den) => router.replace(den)))

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')

