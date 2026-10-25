import { createApp } from 'vue'
import { createPinia } from 'pinia'
import './style.css'
import './assets/admin.css'
import App from './App.vue'
import router from './router'
import { datXuLyChuaVaoCa, datXuLyHetPhien } from './services/phien'
import { useAuth } from './composables/useAuth'
import { datLaiGiaoCa } from './composables/useGiaoCa'

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

// Nhân viên chưa vào ca mà gọi chức năng (backend trả 409 CHUA_NHAN_CA): về trang Vào ca, vào xong quay lại trang đang mở
datXuLyChuaVaoCa(() => {
  if (useAuth().vaiTro.value !== 'nhan-vien') return
  const hienTai = router.currentRoute.value
  if (hienTai.name === 'vao-ca') return
  datLaiGiaoCa()
  router.replace({ path: '/vao-ca', query: { quayLai: hienTai.fullPath } })
})

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')

