<script setup>
// Khung "trang thêm / sửa" dùng chung: thẻ trắng có tiêu đề + hàng nút ở cuối (nút chính đứng trước, nút Hủy sau).
defineProps({
  title: { type: String, required: true },
  icon: { type: String, default: 'bi-ticket-detailed' },
})
</script>

<template>
  <section class="ad-card ad-form-page">
    <div class="ad-card-head">
      <span class="ad-icon-box"><i class="bi" :class="icon" aria-hidden="true"></i></span>
      <h2 class="ad-card-title">{{ title }}</h2>
    </div>
    <slot />
    <div class="ad-form-footer"><slot name="footer" /></div>
  </section>
</template>

<style scoped>
.ad-form-footer {
  display: flex;
  flex-direction: row-reverse;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 1.5rem;
}
</style>
