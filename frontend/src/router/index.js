import { createRouter, createWebHistory } from 'vue-router'
import HoaDonManager from '../components/HoaDonManager.vue'
import KhachHangManager from '../components/KhachHangManager.vue'
import KhachHangAdd from '../components/KhachHangAdd.vue'
import DotGiamGiaManager from '../components/DotGiamGiaManager.vue'

const routes = [
  {
    path: '/',
    redirect: '/hoa-don'
  },
  {
    path: '/hoa-don',
    name: 'HoaDon',
    component: HoaDonManager
  },
  {
    path: '/khach-hang',
    name: 'KhachHang',
    component: KhachHangManager
  },
  {
    path: '/khach-hang/them',
    name: 'KhachHangAdd',
    component: KhachHangAdd
  },
  {
    path: '/dot-giam-gia',
    name: 'DotGiamGia',
    component: DotGiamGiaManager
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
