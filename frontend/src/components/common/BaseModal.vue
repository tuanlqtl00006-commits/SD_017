<script setup>
import { computed, onBeforeUnmount, onMounted, ref, useId } from 'vue'

const props = defineProps({
  title: { type: String, required: true },
  size: { type: String, default: 'md' },
  staticBackdrop: { type: Boolean, default: false },
})
const emit = defineEmits(['close'])

const titleId = useId()
const dialogRef = ref(null)
let previousFocus = null
let previousOverflow = ''

const sizeClass = computed(() => ({ sm: 'modal-sm', lg: 'modal-lg' })[props.size] ?? '')

function onKeydown(e) {
  if (e.key === 'Escape' && !props.staticBackdrop) emit('close')
}

function onBackdropMouseDown() {
  if (!props.staticBackdrop) emit('close')
}

onMounted(() => {
  previousFocus = document.activeElement
  previousOverflow = document.body.style.overflow
  document.body.style.overflow = 'hidden'
  document.addEventListener('keydown', onKeydown)
  dialogRef.value?.focus()
})

onBeforeUnmount(() => {
  document.body.style.overflow = previousOverflow
  document.removeEventListener('keydown', onKeydown)
  previousFocus?.focus?.()
})
</script>

<template>
  <Teleport to="body">
    <div
      class="modal fade show d-block"
      tabindex="-1"
      role="dialog"
      aria-modal="true"
      :aria-labelledby="titleId"
      @mousedown.self="onBackdropMouseDown"
    >
      <div
        ref="dialogRef"
        class="modal-dialog modal-dialog-centered modal-dialog-scrollable"
        :class="sizeClass"
        tabindex="-1"
      >
        <div class="modal-content ad-modal">
          <div class="modal-header ad-modal-header">
            <h2 :id="titleId" class="ad-modal-title">{{ title }}</h2>
            <button type="button" class="btn-close" aria-label="Đóng" @click="emit('close')"></button>
          </div>
          <div class="modal-body ad-modal-body">
            <slot />
          </div>
          <div v-if="$slots.footer" class="modal-footer ad-modal-footer">
            <slot name="footer" />
          </div>
        </div>
      </div>
    </div>
    <div class="modal-backdrop fade show"></div>
  </Teleport>
</template>
