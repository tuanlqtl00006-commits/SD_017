<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import BaseModal from '../common/BaseModal.vue'
import { taoMa } from '../../constants/thuocTinh'
import { NHOM_DANH_MUC } from '../../utils/danhMuc'

const props = defineProps({
  cfg: { type: Object, required: true }, // cấu hình trong THUOC_TINH
  item: { type: Object, default: null }, // null: tạo mới
  all: { type: Array, default: () => [] }, // toàn bộ bản ghi, để kiểm tra trùng mã / tên
  saving: { type: Boolean, default: false },
  serverError: { type: String, default: '' },
})
const emit = defineEmits(['save', 'close'])

const isEdit = computed(() => !!props.item)
// Chất liệu được gắn với các nhóm danh mục (vợt, quần áo, túi - balo...) để form sản phẩm chỉ hiện chất liệu hợp với danh mục đã chọn.
const laChatLieu = props.cfg.slug === 'chat-lieu'
const form = reactive({
  ten: props.item?.ten ?? '',
  // Chất liệu cũ chưa gắn nhóm nào = dùng cho mọi danh mục -> mở sửa thì tích sẵn tất cả
  nhom: laChatLieu ? (props.item ? (props.item.nhom?.length ? [...props.item.nhom] : NHOM_DANH_MUC.map((n) => n.ma)) : []) : [],
})
const baNhom = (ma) => {
  const i = form.nhom.indexOf(ma)
  if (i === -1) form.nhom.push(ma)
  else form.nhom.splice(i, 1)
  if (touched.nhom) refreshErrors()
}
// Mã do hệ thống tự cấp: thêm mới chỉ hiển thị mã dự kiến, sửa thì giữ mã cũ (không cho nhập).
const maHienThi = computed(() => (props.item ? props.item.ma : taoMa(props.cfg.prefix, props.all.map((x) => x.ma))))
const errors = reactive({})
const submitted = ref(false)
const touched = reactive({ ten: false, nhom: false })

function validate() {
  const e = {}
  const ten = String(form.ten).trim()
  if (touched.ten || submitted.value) {
    if (!ten) e.ten = `Nhập tên ${props.cfg.label.toLowerCase()}.`
    else if (ten.length > 100) e.ten = 'Tên tối đa 100 ký tự.'
    else if (props.all.some((x) => x.id !== props.item?.id && x.ten.toLowerCase() === ten.toLowerCase())) {
      e.ten = `Tên ${props.cfg.label.toLowerCase()} đã tồn tại.`
    }
  }
  if (props.serverError) {
    e.ten = props.serverError
  }
  if (laChatLieu && (touched.nhom || submitted.value) && !form.nhom.length) {
    e.nhom = 'Chọn ít nhất một danh mục sử dụng chất liệu này.'
  }
  return e
}

function refreshErrors() {
  const e = validate()
  Object.keys(errors).forEach((k) => delete errors[k])
  Object.assign(errors, e)
  return e
}
watch(form, () => {
  if (submitted.value || touched.ten || touched.nhom) refreshErrors()
})
watch(() => props.serverError, () => refreshErrors())

function submit() {
  submitted.value = true
  const e = refreshErrors()
  if (Object.keys(e).length) {
    nextTick(() => document.querySelector('#tt-form .is-invalid')?.focus())
    return
  }
  emit('save', laChatLieu ? { ten: String(form.ten).trim(), nhom: [...form.nhom] } : { ten: String(form.ten).trim() })
}

onMounted(() => document.getElementById('tt-ten')?.focus())

defineExpose({ form, touched, validate, refreshErrors })
</script>

<template>
  <BaseModal :title="`${isEdit ? 'Chỉnh sửa' : 'Thêm'} ${cfg.label.toLowerCase()}`" static-backdrop @close="emit('close')">
    <form id="tt-form" novalidate @submit.prevent="submit">
      <div class="row g-3">
        <div class="col-md-5">
          <label class="ad-label" for="tt-ma">Mã</label>
          <input id="tt-ma" type="text" class="form-control ad-control" :value="maHienThi" readonly aria-describedby="tt-ma-hint" />
          <p id="tt-ma-hint" class="ad-hint">{{ isEdit ? 'Không thể đổi mã sau khi tạo.' : 'Hệ thống tự cấp mã.' }}</p>
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
            @blur="touched.ten = true; refreshErrors()"
            @input="touched.ten && refreshErrors()"
          />
          <p v-if="errors.ten" class="ad-error">{{ errors.ten }}</p>
        </div>
        <div v-if="laChatLieu" class="col-12">
          <span class="ad-label">Dùng cho danh mục <span class="ad-required">*</span></span>
          <div class="d-flex flex-wrap gap-2" role="group" aria-label="Danh mục sử dụng chất liệu">
            <button
              v-for="n in NHOM_DANH_MUC"
              :key="n.ma"
              type="button"
              class="ad-btn"
              :class="{ 'ad-btn-primary': form.nhom.includes(n.ma) }"
              :aria-pressed="form.nhom.includes(n.ma)"
              @click="baNhom(n.ma)"
            >
              <i v-if="form.nhom.includes(n.ma)" class="bi bi-check2" aria-hidden="true"></i> {{ n.ten }}
            </button>
          </div>
          <p v-if="errors.nhom" class="ad-error">{{ errors.nhom }}</p>
          <p v-else class="ad-hint">Khi tạo sản phẩm, chọn danh mục nào thì chỉ hiện chất liệu được gắn với danh mục đó.</p>
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
