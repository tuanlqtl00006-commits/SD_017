<template>
  <div class="bg-white border-end d-flex flex-column" style="width: 320px;">
    
    <div class="p-3 text-center mb-3 d-flex align-items-center justify-content-center" style="height: 130px;">
      <img src="/logo.png" alt="FootStyle" class="img-fluid" style="width: 95%; object-fit: contain; transform: scale(1.1);">
    </div>

    
    <ul class="nav flex-column fw-medium px-3" style="font-size: 1.05rem; gap: 0.5rem;">
      <li class="nav-item">
        <a href="#" class="nav-link text-dark px-3 py-3 rounded-3 d-flex align-items-center text-nowrap">
          <i class="bi bi-bar-chart-line me-3 fs-5 text-secondary" style="-webkit-text-stroke: 0.8px;"></i> Thống Kê
        </a>
      </li>
      <li class="nav-item">
        <a href="#" class="nav-link text-dark px-3 py-3 rounded-3 d-flex align-items-center text-nowrap">
          <i class="bi bi-bag me-3 fs-5 text-secondary" style="-webkit-text-stroke: 0.8px;"></i> Quản Lý Sản Phẩm
        </a>
      </li>
      <li class="nav-item">
        <a href="#" class="nav-link text-dark px-3 py-3 rounded-3 d-flex align-items-center text-nowrap">
          <i class="bi bi-person me-3 fs-5 text-secondary" style="-webkit-text-stroke: 0.8px;"></i> Quản Lý Nhân Viên
        </a>
      </li>
      <li class="nav-item">
        <router-link to="/khach-hang" class="nav-link px-3 py-3 rounded-3 d-flex align-items-center text-nowrap" active-class="active text-white bg-primary shadow-sm fw-semibold" :class="$route.path === '/khach-hang' ? '' : 'text-dark'">
          <i class="bi bi-people me-3 fs-5" :class="$route.path === '/khach-hang' ? 'text-white' : 'text-secondary'" style="-webkit-text-stroke: 0.8px;"></i> Quản Lý Khách Hàng
        </router-link>
      </li>
      <li class="nav-item">
        <router-link to="/hoa-don" class="nav-link px-3 py-3 rounded-3 d-flex align-items-center text-nowrap" active-class="active text-white bg-primary shadow-sm fw-semibold" :class="$route.path === '/hoa-don' ? '' : 'text-dark'">
          <i class="bi bi-receipt me-3 fs-5" :class="$route.path === '/hoa-don' ? 'text-white' : 'text-secondary'" style="-webkit-text-stroke: 0.8px;"></i> Quản Lý Hóa Đơn
        </router-link>
      </li>
      <li class="nav-item">
        <a href="#" class="nav-link text-dark px-3 py-3 rounded-3 d-flex align-items-center text-nowrap">
          <i class="bi bi-tag me-3 fs-5 text-secondary" style="-webkit-text-stroke: 0.8px;"></i> Quản Lý Phiếu Giảm Giá
        </a>
      </li>
      <li class="nav-item">
        <router-link to="/dot-giam-gia" class="nav-link px-3 py-3 rounded-3 d-flex align-items-center text-nowrap" active-class="active text-white bg-primary shadow-sm fw-semibold" :class="$route.path === '/dot-giam-gia' ? '' : 'text-dark'">
          <i class="bi bi-percent me-3 fs-5" :class="$route.path === '/dot-giam-gia' ? 'text-white' : 'text-secondary'" style="-webkit-text-stroke: 0.8px;"></i> Quản Lý Đợt Giảm Giá
        </router-link>
      </li>
    </ul>
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'
const route = useRoute()
</script>

<style scoped>
.nav-link.active {
  background-color: #0977ec !important;
}
</style>