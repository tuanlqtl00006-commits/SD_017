import { createApp } from 'vue'
import { createPinia } from 'pinia'
import './style.css'
import './assets/admin.css'
import App from './App.vue'
import router from './router'
import { datXuLyHetPhien } from './services/phien'
import { useAuth } from './composables/useAuth'

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

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')

