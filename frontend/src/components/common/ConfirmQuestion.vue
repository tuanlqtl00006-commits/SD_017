<script setup>
import { onBeforeUnmount, onMounted } from 'vue'

/*
 * Hộp xác nhận giữa màn hình giống video: vòng tròn dấu "?", chữ "Xác nhận", nội dung, hai nút "Hủy" / "Đồng ý".
 * Dùng khi thêm mới / cập nhật dữ liệu.
 */
defineProps({
  title: { type: String, default: 'Xác nhận' },
  message: { type: String, required: true },
  confirmText: { type: String, default: 'Đồng ý' },
  cancelText: { type: String, default: 'Hủy' },
  loading: { type: Boolean, default: false },
})
const emit = defineEmits(['confirm', 'cancel'])

function onKeydown(e) {
  if (e.key === 'Escape') emit('cancel')
}
onMounted(() => document.addEventListener('keydown', onKeydown))
onBeforeUnmount(() => document.removeEventListener('keydown', onKeydown))
</script>

<template>
  <Teleport to="body">
    <div class="ad-q-backdrop" role="dialog" aria-modal="true" aria-labelledby="ad-q-title" @mousedown.self="emit('cancel')">
      <div class="ad-q-box">
        <span class="ad-q-icon" aria-hidden="true"><i class="bi bi-question-lg"></i></span>
        <h2 id="ad-q-title" class="ad-q-title">{{ title }}</h2>
        <p class="ad-q-text">{{ message }}</p>
        <div class="ad-q-actions">
          <button type="button" class="ad-q-btn" :disabled="loading" @click="emit('cancel')">{{ cancelText }}</button>
          <button type="button" class="ad-q-btn ad-q-btn-ok" :disabled="loading" @click="emit('confirm')">
            <span v-if="loading" class="spinner-border spinner-border-sm" aria-hidden="true"></span>
            {{ confirmText }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>
