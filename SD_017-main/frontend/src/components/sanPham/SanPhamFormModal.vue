<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import BaseModal from '../common/BaseModal.vue'
import { taoMa } from '../../constants/thuocTinh'

const props = defineProps({
  item: { type: Object, default: null }, // null: tạo mới
  all: { type: Array, default: () => [] }, // toàn bộ sản phẩm, để kiểm tra trùng mã
  options: { type: Function, required: true }, // (slug, idDangChon) => danh sách cho dropdown
  saving: { type: Boolean, default: false },
})
const emit = defineEmits(['save', 'close'])

const isEdit = computed(() => !!props.item)

// Mỗi thuộc tính của sản phẩm: field trong form <-> bảng ERD
const SELECTS = [
  { field: 'idDanhMuc', slug: 'danh-muc', label: 'Danh mục' },
  { field: 'idThuongHieu', slug: 'thuong-hieu', label: 'Thương hiệu' },
  { field: 'idXuatXu', slug: 'xuat-xu', label: 'Xuất xứ' },
  { field: 'idChatLieu', slug: 'chat-lieu', label: 'Chất liệu' },
  { field: 'idDoCung', slug: 'do-cung', label: 'Độ cứng' },
  { field: 'idDiemCanBang', slug: 'diem-can-bang', label: 'Điểm cân bằng' },
]

const form = reactive({
  ma: props.item?.ma ?? taoMa('SP', props.all.map((p) => p.ma)),
  ten: props.item?.ten ?? '',
  idDanhMuc: props.item?.idDanhMuc ?? '',
  idThuongHieu: props.item?.idThuongHieu ?? '',
  idXuatXu: props.item?.idXuatXu ?? '',
  idChatLieu: props.item?.idChatLieu ?? '',
  idDoCung: props.item?.idDoCung ?? '',
  idDiemCanBang: props.item?.idDiemCanBang ?? '',
  anhChinh: props.item?.anhChinh ?? '',
  moTa: props.item?.moTa ?? '',
})
const errors = reactive({})
const submitted = ref(false)

function validate() {
  const e = {}
  const ma = String(form.ma).trim().toUpperCase()
  if (!ma) e.ma = 'Nhập mã sản phẩm.'
  else if (!/^[A-Z0-9]{3,20}$/.test(ma)) e.ma = 'Mã gồm 3-20 ký tự chữ hoặc số, không dấu, không khoảng trắng.'
  else if (!isEdit.value && props.all.some((p) => p.ma === ma)) e.ma = 'Mã sản phẩm đã tồn tại.'

  const ten = String(form.ten).trim()
  if (!ten) e.ten = 'Nhập tên sản phẩm.'
  else if (ten.length > 255) e.ten = 'Tên sản phẩm tối đa 255 ký tự.'

  for (const s of SELECTS) if (form[s.field] === '') e[s.field] = `Chọn ${s.label.toLowerCase()}.`

  const url = String(form.anhChinh).trim()
  if (url && !/^https?:\/\/\S+$/i.test(url)) e.anhChinh = 'Đường dẫn ảnh phải bắt đầu bằng http:// hoặc https://.'
  else if (url.length > 500) e.anhChinh = 'Đường dẫn ảnh tối đa 500 ký tự.'
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
    nextTick(() => document.querySelector('#sp-form .is-invalid')?.focus())
    return
  }
  emit('save', {
    ma: String(form.ma).trim().toUpperCase(),
    ten: String(form.ten).trim(),
    idDanhMuc: Number(form.idDanhMuc),
    idThuongHieu: Number(form.idThuongHieu),
    idXuatXu: Number(form.idXuatXu),
    idChatLieu: Number(form.idChatLieu),
    idDoCung: Number(form.idDoCung),
    idDiemCanBang: Number(form.idDiemCanBang),
    anhChinh: String(form.anhChinh).trim(),
    moTa: String(form.moTa).trim(),
  })
}

onMounted(() => document.getElementById('sp-ten')?.focus())
</script>

<template>
  <BaseModal :title="isEdit ? 'Chỉnh sửa sản phẩm' : 'Thêm sản phẩm'" size="lg" static-backdrop @close="emit('close')">
    <form id="sp-form" novalidate @submit.prevent="submit">
      <div class="row g-3">
        <div class="col-md-4">
          <label class="ad-label" for="sp-ma">Mã sản phẩm <span class="ad-required">*</span></label>
          <input
            id="sp-ma"
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

        <div class="col-md-8">
          <label class="ad-label" for="sp-ten">Tên sản phẩm <span class="ad-required">*</span></label>
          <input
            id="sp-ten"
            v-model="form.ten"
            type="text"
            class="form-control ad-control"
            :class="{ 'is-invalid': errors.ten }"
            maxlength="255"
            placeholder="Ví dụ: Yonex Astrox 99 Pro"
            autocomplete="off"
            :aria-invalid="!!errors.ten"
          />
          <p v-if="errors.ten" class="ad-error">{{ errors.ten }}</p>
        </div>

        <div v-for="s in SELECTS" :key="s.field" class="col-md-4">
          <label class="ad-label" :for="`sp-${s.field}`">{{ s.label }} <span class="ad-required">*</span></label>
          <select
            :id="`sp-${s.field}`"
            v-model="form[s.field]"
            class="form-select ad-control"
            :class="{ 'is-invalid': errors[s.field] }"
            :aria-invalid="!!errors[s.field]"
          >
            <option value="">Chọn {{ s.label.toLowerCase() }}</option>
            <option v-for="o in options(s.slug, item?.[s.field])" :key="o.id" :value="o.id">{{ o.ten }}</option>
          </select>
          <p v-if="errors[s.field]" class="ad-error">{{ errors[s.field] }}</p>
        </div>

        <div class="col-12">
          <label class="ad-label" for="sp-anh">Đường dẫn ảnh chính</label>
          <input
            id="sp-anh"
            v-model="form.anhChinh"
            type="url"
            class="form-control ad-control"
            :class="{ 'is-invalid': errors.anhChinh }"
            maxlength="500"
            placeholder="https://…"
            autocomplete="off"
            :aria-invalid="!!errors.anhChinh"
          />
          <p v-if="errors.anhChinh" class="ad-error">{{ errors.anhChinh }}</p>
          <p v-else class="ad-hint">Không bắt buộc. Lưu vào bảng hình ảnh sản phẩm (ảnh chính).</p>
        </div>

        <div class="col-12">
          <label class="ad-label" for="sp-mo-ta">Mô tả</label>
          <textarea id="sp-mo-ta" v-model="form.moTa" rows="3" class="form-control ad-control" placeholder="Đặc điểm nổi bật, đối tượng phù hợp…"></textarea>
        </div>
      </div>
    </form>

    <template #footer>
      <button type="button" class="ad-btn" :disabled="saving" @click="emit('close')">Hủy</button>
      <button type="submit" form="sp-form" class="ad-btn ad-btn-primary" :disabled="saving">
        <span v-if="saving" class="spinner-border spinner-border-sm" aria-hidden="true"></span>
        {{ isEdit ? 'Lưu thay đổi' : 'Thêm sản phẩm' }}
      </button>
    </template>
  </BaseModal>
</template>
