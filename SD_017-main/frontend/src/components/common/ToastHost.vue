<script setup>
import { useToast } from '../../composables/useToast'

const { toasts, remove } = useToast()
</script>

<template>
  <div class="ad-toast-host">
    <div
      v-for="t in toasts"
      :key="t.id"
      class="ad-toast"
      :class="`ad-toast-${t.type}`"
      :role="t.type === 'error' ? 'alert' : 'status'"
    >
      <i class="bi" :class="t.type === 'error' ? 'bi-exclamation-triangle-fill' : 'bi-check-circle-fill'" aria-hidden="true"></i>
      <span class="ad-toast-text">{{ t.message }}</span>
      <button type="button" class="ad-toast-close" aria-label="Đóng thông báo" @click="remove(t.id)">
        <i class="bi bi-x-lg" aria-hidden="true"></i>
      </button>
    </div>
  </div>
</template>
