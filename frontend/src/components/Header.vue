<template>
  <header class="bg-white border-bottom px-4 py-3 d-flex justify-content-between align-items-center">
    <div class="d-flex align-items-center">
      <div class="bg-light border rounded d-flex align-items-center justify-content-center me-3" style="width: 32px; height: 32px; cursor: pointer;">
        <i class="bi bi-list fs-5 text-dark"></i>
      </div>
      
      <h5 class="m-0 d-flex align-items-center fs-5">
        <template v-if="route.path.includes('/khach-hang/them')">
          <router-link to="/khach-hang" class="text-secondary text-decoration-none fw-medium" style="font-size: 1.1rem;">Khách hàng</router-link>
          <span class="text-secondary mx-2 fw-medium" style="font-size: 1.1rem;">/</span>
          <span class="text-dark fw-bold" style="font-size: 1.1rem;">Thêm khách hàng</span>
        </template>
        <template v-else-if="route.path.includes('/khach-hang')">
          <span class="text-dark fw-bold" style="font-size: 1.1rem;">Khách hàng</span>
        </template>
        <template v-else>
          <span class="text-dark fw-bold" style="font-size: 1.1rem;">{{ pageTitle }}</span>
        </template>
      </h5>
    </div>
    
    <div class="d-flex align-items-center gap-3">
      <button class="btn btn-outline-secondary btn-sm rounded-pill d-flex align-items-center px-3 text-muted border-light-subtle">
        <i class="bi bi-clock me-2"></i> Ca làm việc <i class="bi bi-chevron-down ms-2"></i>
      </button>
      <button class="btn btn-light rounded-circle p-2 lh-1 text-muted"><i class="bi bi-gear"></i></button>
      <button class="btn btn-light rounded-circle p-2 lh-1 text-muted"><i class="bi bi-moon"></i></button>
      <div class="d-flex align-items-center ms-2">
        <span class="text-dark fw-medium me-2">Admin</span>
        <div class="rounded-circle bg-secondary overflow-hidden" style="width: 35px; height: 35px;">
          <img src="https://ui-avatars.com/api/?name=Admin&background=random" alt="Admin" class="w-100 h-100 object-fit-cover">
        </div>
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const pageTitle = computed(() => {
  if (route.path.includes('/khach-hang/them')) return 'Thêm khách hàng'
  if (route.path.includes('/khach-hang')) return 'Khách hàng'
  if (route.path.includes('/hoa-don')) return 'Quản lý Hóa đơn'
  if (route.path.includes('/dot-giam-gia')) return 'Quản Lý Đợt Giảm Giá'
  return 'Trang chủ'
})
</script>
