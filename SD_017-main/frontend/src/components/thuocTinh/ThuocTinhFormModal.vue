<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import BaseModal from '../common/BaseModal.vue'
import { taoMa } from '../../constants/thuocTinh'

const props = defineProps({
  cfg: { type: Object, required: true }, // cấu hình trong THUOC_TINH
  item: { type: Object, default: null }, // null: tạo mới
  all: { type: Array, default: () => [] }, // toàn bộ bản ghi, để kiểm tra trùng mã / tên
  saving: { type: Boolean, default: false },
})
const emit = defineEmits(['save', 'close'])

const isEdit = computed(() => !!props.item)
const form = reactive({
  ma: props.item?.ma ?? taoMa(props.cfg.prefix, props.all.map((x) => x.ma)),
  ten: props.item?.ten ?? '',
})
const errors = reactive({})
const submitted = ref(false)

function validate() {
  const e = {}
  const ma = String(form.ma).trim().toUpperCase()
  if (!ma) e.ma = 'Nhập mã.'
  else if (!/^[A-Z0-9]{2,20}$/.test(ma)) e.ma = 'Mã gồm 2-20 ký tự chữ hoặc số, không dấu, không khoảng trắng.'
  else if (!isEdit.value && props.all.some((x) => x.ma === ma)) e.ma = 'Mã đã tồn tại.'

  const ten = String(form.ten).trim()
  if (!ten) e.ten = `Nhập tên ${props.cfg.label.toLowerCase()}.`
  else if (ten.length > 100) e.ten = 'Tên tối đa 100 ký tự.'
  else if (props.all.some((x) => x.id !== props.item?.id && x.ten.toLowerCase() === ten.toLowerCase())) e.ten = 'Tên đã tồn tại.'
  return e
}

function refreshErrors() {
  const e = validate()
  Object.keys(errors).forEach((k) => delete errors[k])
  Object.assign(errors, e)
  return e
}
watch(form, () => {
  if (submitted.value) refreshErrors()
})

function submit() {
  submitted.value = true
  const e = refreshErrors()
  if (Object.keys(e).length) {
    nextTick(() => document.querySelector('#tt-form .is-invalid')?.focus())
    return
  }
  emit('save', { ma: String(form.ma).trim().toUpperCase(), ten: String(form.ten).trim() })
}

onMounted(() => document.getElementById('tt-ten')?.focus())
</script>

<template>
  <BaseModal :title="`${isEdit ? 'Chỉnh sửa' : 'Thêm'} ${cfg.label.toLowerCase()}`" static-backdrop @close="emit('close')">
    <form id="tt-form" novalidate @submit.prevent="submit">
      <div class="row g-3">
        <div class="col-md-5">
          <label class="ad-label" for="tt-ma">Mã <span class="ad-required">*</span></label>
          <input
            id="tt-ma"
            v-model="form.ma"
            type="text"
            class="form-control ad-control text-uppercase"
            :class="{ 'is-invalid': errors.ma }"
            :readonly="isEdit"
            maxlength="20"
            autocomplete="off"
            :aria-invalid="!!errors.ma"
          />
          <p v-if="errors.ma" class="ad-error">{{ errors.ma }}</p>
          <p v-else-if="isEdit" class="ad-hint">Không thể đổi mã sau khi tạo.</p>
        </div>
        <div class="col-md-7">
          <label class="ad-label" for="tt-ten">Tên {{ cfg.label.toLowerCase() }} <span class="ad-required">*</span></label>
          <input
            id="tt-ten"
            v-model="form.ten"
            type="text"
            class="form-control ad-control"
            :class="{ 'is-invalid': errors.ten }"
            maxlength="100"
            :placeholder="`Ví dụ: ${cfg.example}`"
            autocomplete="off"
            :aria-invalid="!!errors.ten"
          />
          <p v-if="errors.ten" class="ad-error">{{ errors.ten }}</p>
        </div>
      </div>
    </form>

    <template #footer>
      <button type="button" class="ad-btn" :disabled="saving" @click="emit('close')">Hủy</button>
      <button type="submit" form="tt-form" class="ad-btn ad-btn-primary" :disabled="saving">
        <span v-if="saving" class="spinner-border spinner-border-sm" aria-hidden="true"></span>
        {{ isEdit ? 'Lưu thay đổi' : 'Thêm mới' }}
      </button>
    </template>
  </BaseModal>
</template>
