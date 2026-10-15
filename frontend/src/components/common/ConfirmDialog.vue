<script setup>
import BaseModal from './BaseModal.vue'

defineProps({
  title: { type: String, required: true },
  message: { type: String, required: true },
  confirmText: { type: String, default: 'Xác nhận' },
  variant: { type: String, default: 'primary' }, // primary | danger
  loading: { type: Boolean, default: false },
  // Tùy chọn: 'warning' | 'success'. Có icon thì hộp thoại dùng giao diện căn giữa (biểu tượng tròn, 2 nút bằng nhau).
  // Không truyền thì giữ giao diện cũ như các trang khác đang dùng.
  icon: { type: String, default: '', validator: (v) => ['', 'warning', 'success'].includes(v) },
})
const emit = defineEmits(['confirm', 'cancel'])
</script>

<template>
  <BaseModal :title="title" :size="icon ? 'md' : 'sm'" :content-class="icon ? 'cf-dialog' : ''" @close="emit('cancel')">
    <!-- Giao diện có icon: tiêu đề hiện trong thân hộp thoại nên ở header chỉ giữ tên ẩn (cho trình đọc màn hình) và nút đóng -->
    <template v-if="icon" #header="{ titleId }">
      <span :id="titleId" class="visually-hidden">{{ title }}</span>
    </template>

    <div v-if="icon" class="cf-body" data-testid="cf-icon-dialog">
      <div class="cf-icon" :class="`cf-icon-${icon}`" aria-hidden="true">
        <svg v-if="icon === 'success'" width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="20 6 9 17 4 12" />
        </svg>
        <svg v-else width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <line x1="12" y1="7" x2="12" y2="13" />
          <line x1="12" y1="17" x2="12.01" y2="17" />
        </svg>
      </div>
      <h3 class="cf-title" aria-hidden="true">{{ title }}</h3>
      <p class="cf-text">{{ message }}</p>
    </div>
    <p v-else class="ad-confirm-text">{{ message }}</p>

    <template #footer>
      <button type="button" class="ad-btn" :disabled="loading" @click="emit('cancel')">Hủy</button>
      <button
        type="button"
        class="ad-btn"
        :class="variant === 'danger' ? 'ad-btn-danger' : 'ad-btn-primary'"
        :disabled="loading"
        @click="emit('confirm')"
      >
        <span v-if="loading" class="spinner-border spinner-border-sm" aria-hidden="true"></span>
        {{ confirmText }}
      </button>
    </template>
  </BaseModal>
</template>

<style>
/* Giao diện hộp thoại xác nhận căn giữa (chỉ áp dụng khi truyền prop icon) */
.modal-dialog:has(.cf-dialog) {
  max-width: 420px;
}
.cf-dialog {
  border: 0;
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.18);
}
.cf-dialog .ad-modal-header {
  border-bottom: 0;
  padding: 0.75rem 0.75rem 0;
  justify-content: flex-end;
}
.cf-dialog .ad-modal-body {
  padding: 0 2rem 1.5rem;
}
.cf-body {
  text-align: center;
}
.cf-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 1.1rem;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.cf-icon-warning {
  color: #f59e0b;
  background: #fef3c7;
  box-shadow: 0 0 0 8px #fffbeb;
}
.cf-icon-success {
  color: #16a34a;
  background: #dcfce7;
  box-shadow: 0 0 0 8px #f0fdf4;
}
.cf-title {
  margin: 0 0 0.5rem;
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--fs-text);
}
.cf-text {
  margin: 0;
  color: var(--fs-text-2);
  line-height: 1.6;
}
.cf-dialog .ad-modal-footer {
  display: flex;
  flex-wrap: nowrap;
  gap: 0.75rem;
  padding: 0 2rem 1.75rem;
  border-top: 0;
  background: transparent;
}
.cf-dialog .ad-modal-footer .ad-btn {
  flex: 1;
  min-width: 0;
  height: 42px;
  white-space: nowrap;
  justify-content: center;
}
</style>
