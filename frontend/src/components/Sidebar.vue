<template>
  <div class="bg-white border-end d-flex flex-column" style="width: 320px;">
    <!-- Logo -->
    <div class="p-3 text-center mb-3 d-flex align-items-center justify-content-center" style="height: 130px;">
      <img src="/logo.png" alt="FootStyle" class="img-fluid" style="width: 95%; object-fit: contain; transform: scale(1.1);">
    </div>

    <!-- Menu -->
    <ul class="nav flex-column fw-medium px-3" style="font-size: 1.05rem; gap: 0.5rem;">
      <li class="nav-item">
        <a href="#" class="nav-link text-dark px-3 py-3 rounded-3 d-flex align-items-center text-nowrap">
          Thống Kê
        </a>
      </li>
      <li class="nav-item">
        <a href="#" class="nav-link text-dark px-3 py-3 rounded-3 d-flex align-items-center text-nowrap">
          Quản Lý Sản Phẩm
        </a>
      </li>
      <li class="nav-item">
        <a href="#" class="nav-link text-dark px-3 py-3 rounded-3 d-flex align-items-center text-nowrap">
          Quản Lý Nhân Viên
        </a>
      </li>
      <li class="nav-item">
        <router-link to="/khach-hang" class="nav-link px-3 py-3 rounded-3 d-flex align-items-center text-nowrap" active-class="active text-white bg-primary shadow-sm fw-semibold" :class="$route.path === '/khach-hang' ? '' : 'text-dark'">
          Quản Lý Khách Hàng
        </router-link>
      </li>
      <li class="nav-item">
        <router-link to="/hoa-don" class="nav-link px-3 py-3 rounded-3 d-flex align-items-center text-nowrap" active-class="active text-white bg-primary shadow-sm fw-semibold" :class="$route.path === '/hoa-don' ? '' : 'text-dark'">
          Quản Lý Hóa Đơn
        </router-link>
      </li>
      <li class="nav-item">
        <a href="#" class="nav-link text-dark px-3 py-3 rounded-3 d-flex align-items-center text-nowrap">
          Quản Lý Phiếu Giảm Giá
        </a>
      </li>
      <li class="nav-item">
        <router-link to="/dot-giam-gia" class="nav-link px-3 py-3 rounded-3 d-flex align-items-center text-nowrap" active-class="active text-white bg-primary shadow-sm fw-semibold" :class="$route.path === '/dot-giam-gia' ? '' : 'text-dark'">
          Quản Lý Đợt Giảm Giá
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
