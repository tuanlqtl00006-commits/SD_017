<script setup>
// Ô nhập tiền: hiển thị có dấu chấm ngăn cách hàng nghìn (1.500.000) kèm đơn vị ₫, giá trị v-model là số (hoặc '' khi để trống).
import { computed } from 'vue'

const props = defineProps({
  modelValue: { type: [Number, String], default: '' },
  placeholder: { type: String, default: 'Ví dụ: 1.250.000' },
  disabled: { type: Boolean, default: false },
  invalid: { type: Boolean, default: false },
  small: { type: Boolean, default: false },
  id: { type: String, default: undefined },
  ariaLabel: { type: String, default: 'Giá bán' },
})
const emit = defineEmits(['update:modelValue'])

const hienThi = computed(() => {
  const v = props.modelValue
  if (v === '' || v === null || v === undefined || Number.isNaN(Number(v))) return ''
  return new Intl.NumberFormat('vi-VN').format(Number(v))
})

function onInput(e) {
  const so = e.target.value.replace(/\D/g, '').replace(/^0+(?=\d)/, '')
  const v = so === '' ? '' : Number(so)
  emit('update:modelValue', v)
  // Ghi lại ngay dạng đã định dạng để con trỏ không nhảy lung tung khi gõ
  e.target.value = v === '' ? '' : new Intl.NumberFormat('vi-VN').format(v)
}
</script>

<template>
  <div class="money-input">
    <input
      :id="id"
      type="text"
      inputmode="numeric"
      autocomplete="off"
      class="form-control ad-control"
      :class="{ 'form-control-sm': small, 'is-invalid': invalid }"
      :value="hienThi"
      :placeholder="placeholder"
      :disabled="disabled"
      :aria-label="ariaLabel"
      :aria-invalid="invalid"
      @input="onInput"
    />
    <span class="money-suffix" aria-hidden="true">₫</span>
  </div>
</template>

<style scoped>
.money-input { position: relative; }
.money-input input { padding-right: 1.9rem; text-align: left; }
.money-suffix { position: absolute; right: 0.7rem; top: 50%; transform: translateY(-50%); color: #7b8794; pointer-events: none; font-size: 0.9em; }
</style>
