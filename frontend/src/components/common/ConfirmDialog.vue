<script setup>
import BaseModal from './BaseModal.vue'

defineProps({
  title: { type: String, required: true },
  message: { type: String, required: true },
  confirmText: { type: String, default: 'Xác nhận' },
  variant: { type: String, default: 'primary' }, // primary | danger
  loading: { type: Boolean, default: false },
})
const emit = defineEmits(['confirm', 'cancel'])
</script>

<template>
  <BaseModal :title="title" size="sm" @close="emit('cancel')">
    <p class="ad-confirm-text">{{ message }}</p>
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
