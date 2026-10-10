<script setup>
/*
 * Công tắc bật / tắt hoạt động. Không tự đổi trạng thái: bấm vào chỉ phát sự kiện "toggle",
 * trang cha hỏi xác nhận rồi mới gọi API, nên công tắc luôn phản ánh đúng dữ liệu thật.
 */
defineProps({
  on: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  label: { type: String, default: 'Bật / tắt hoạt động' },
})
const emit = defineEmits(['toggle'])
</script>

<template>
  <button
    type="button"
    class="ad-switch"
    :class="{ 'is-on': on }"
    role="switch"
    :aria-checked="on"
    :aria-label="label"
    :title="label"
    :disabled="disabled"
    @click="emit('toggle')"
  ></button>
</template>
