<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import BaseModal from '../common/BaseModal.vue'

const props = defineProps({
  item: { type: Object, default: null }, // null: tạo mới
  all: { type: Array, default: () => [] }, // toàn bộ biến thể, để kiểm tra trùng mã / tổ hợp
  sanPhams: { type: Array, default: () => [] },
  options: { type: Function, required: true }, // (slug, idDangChon) => danh sách cho dropdown
  saving: { type: Boolean, default: false },
})
const emit = defineEmits(['save', 'close'])

const isEdit = computed(() => !!props.item)

const form = reactive({
  idSanPham: props.item?.idSanPham ?? '',
  idMauSac: props.item?.idMauSac ?? '',
  idTrongLuong: props.item?.idTrongLuong ?? '',
  idChuVi: props.item?.idChuVi ?? '',
  ma: props.item?.ma ?? '',
  giaBan: props.item?.giaBan ?? '',
  soLuongTon: props.item?.soLuongTon ?? 0,
})
const errors = reactive({})
const submitted = ref(false)

const sanPhamOptions = computed(() => props.sanPhams.filter((p) => p.hoatDong || p.id === props.item?.idSanPham))

// Mã biến thể không sửa tay được khi sửa. Nhưng nếu đổi sản phẩm mà mã gốc đang bắt đầu bằng mã sản phẩm gốc
// (vd SP008-DEN-4U-G5) thì phần đầu mã tự đổi sang mã sản phẩm mới (SP007-DEN-4U-G5), giống cách backend xử lý.
// Luôn tính lại từ mã gốc nên đổi qua đổi lại nhiều lần vẫn đúng.
watch(
  () => form.idSanPham,
  (idMoi) => {
    if (!isEdit.value) return
    form.ma = props.item.ma
    const spGoc = props.sanPhams.find((p) => p.id === props.item.idSanPham)
    const spMoi = props.sanPhams.find((p) => p.id === idMoi)
    if (!spGoc || !spMoi || spGoc.id === spMoi.id) return
    if (props.item.ma.toUpperCase().startsWith(spGoc.ma.toUpperCase() + '-')) {
      form.ma = spMoi.ma + props.item.ma.slice(spGoc.ma.length)
    }
  },
)

// Gợi ý mã khi tạo mới: <mã SP>-<màu>-<trọng lượng>-<chu vi>, người dùng vẫn sửa được.
const codeTouched = ref(isEdit.value)
const nameOf = (slug, id) => props.options(slug, id).find((o) => o.id === id)?.ten ?? ''
const slugify = (s) => s.normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/gi, 'd').replace(/[^A-Za-z0-9]+/g, '').toUpperCase()
watch(
  () => [form.idSanPham, form.idMauSac, form.idTrongLuong, form.idChuVi],
  () => {
    if (codeTouched.value) return
    const sp = props.sanPhams.find((p) => p.id === form.idSanPham)
    form.ma = [sp?.ma, slugify(nameOf('mau-sac', form.idMauSac)), slugify(nameOf('trong-luong', form.idTrongLuong).split(' ')[0]), slugify(nameOf('chu-vi', form.idChuVi))]
      .filter(Boolean)
      .join('-')
  },
)

function validate() {
  const e = {}
  if (form.idSanPham === '') e.idSanPham = 'Chọn sản phẩm.'
  if (form.idMauSac === '') e.idMauSac = 'Chọn màu sắc.'
  if (form.idTrongLuong === '') e.idTrongLuong = 'Chọn trọng lượng.'
  if (form.idChuVi === '') e.idChuVi = 'Chọn chu vi.'

  const ma = String(form.ma).trim().toUpperCase()
  if (!ma) e.ma = 'Nhập mã biến thể.'
  else if (!/^[A-Z0-9-]{3,50}$/.test(ma)) e.ma = 'Mã gồm 3-50 ký tự chữ, số hoặc dấu gạch ngang, không dấu, không khoảng trắng.'
  else if (props.all.some((b) => b.id !== props.item?.id && b.ma === ma)) e.ma = 'Mã biến thể đã tồn tại.' // khi sửa thì bỏ qua chính nó

  const gia = Number(form.giaBan)
  if (form.giaBan === '' || Number.isNaN(gia)) e.giaBan = 'Nhập giá bán.'
  else if (gia < 1000) e.giaBan = 'Giá bán tối thiểu 1.000 ₫.'

  const ton = Number(form.soLuongTon)
  if (form.soLuongTon === '' || !Number.isInteger(ton) || ton < 0) e.soLuongTon = 'Số lượng tồn là số nguyên từ 0 trở lên.'

  if (!e.idSanPham && !e.idMauSac && !e.idTrongLuong && !e.idChuVi) {
    const trung = props.all.some(
      (b) => b.id !== props.item?.id && b.idSanPham === form.idSanPham && b.idMauSac === form.idMauSac && b.idTrongLuong === form.idTrongLuong && b.idChuVi === form.idChuVi,
    )
    if (trung) e.idMauSac = 'Sản phẩm đã có biến thể với màu sắc, trọng lượng và chu vi này.'
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
  if (submitted.value) refreshErrors()
})

function submit() {
  submitted.value = true
  const e = refreshErrors()
  if (Object.keys(e).length) {
    nextTick(() => document.querySelector('#bt-form .is-invalid')?.focus())
    return
  }
  emit('save', {
    idSanPham: Number(form.idSanPham),
    idMauSac: Number(form.idMauSac),
    idTrongLuong: Number(form.idTrongLuong),
    idChuVi: Number(form.idChuVi),
    ma: String(form.ma).trim().toUpperCase(),
    giaBan: Number(form.giaBan),
    soLuongTon: Number(form.soLuongTon),
  })
}

onMounted(() => document.getElementById('bt-san-pham')?.focus())
</script>

<template>
  <BaseModal :title="isEdit ? 'Chỉnh sửa biến thể' : 'Thêm biến thể sản phẩm'" size="lg" static-backdrop @close="emit('close')">
    <form id="bt-form" novalidate @submit.prevent="submit">
      <div class="row g-3">
        <div class="col-12">
          <label class="ad-label" for="bt-san-pham">Sản phẩm <span class="ad-required">*</span></label>
          <select id="bt-san-pham" v-model="form.idSanPham" class="form-select ad-control" :class="{ 'is-invalid': errors.idSanPham }" :aria-invalid="!!errors.idSanPham">
            <option value="">Chọn sản phẩm</option>
            <option v-for="p in sanPhamOptions" :key="p.id" :value="p.id">{{ p.ma }} - {{ p.ten }}</option>
          </select>
          <p v-if="errors.idSanPham" class="ad-error">{{ errors.idSanPham }}</p>
          <p v-else-if="isEdit" class="ad-hint">Có thể đổi sang sản phẩm khác (chỉ chọn được sản phẩm đang hoạt động).</p>
        </div>

        <div class="col-12"><h3 class="ad-form-section">Thuộc tính biến thể</h3></div>
        <div class="col-md-4">
          <label class="ad-label" for="bt-mau">Màu sắc <span class="ad-required">*</span></label>
          <select id="bt-mau" v-model="form.idMauSac" class="form-select ad-control" :class="{ 'is-invalid': errors.idMauSac }" :aria-invalid="!!errors.idMauSac">
            <option value="">Chọn màu sắc</option>
            <option v-for="o in options('mau-sac', item?.idMauSac)" :key="o.id" :value="o.id">{{ o.ten }}</option>
          </select>
          <p v-if="errors.idMauSac" class="ad-error">{{ errors.idMauSac }}</p>
        </div>
        <div class="col-md-4">
          <label class="ad-label" for="bt-tl">Trọng lượng <span class="ad-required">*</span></label>
          <select id="bt-tl" v-model="form.idTrongLuong" class="form-select ad-control" :class="{ 'is-invalid': errors.idTrongLuong }" :aria-invalid="!!errors.idTrongLuong">
            <option value="">Chọn trọng lượng</option>
            <option v-for="o in options('trong-luong', item?.idTrongLuong)" :key="o.id" :value="o.id">{{ o.ten }}</option>
          </select>
          <p v-if="errors.idTrongLuong" class="ad-error">{{ errors.idTrongLuong }}</p>
        </div>
        <div class="col-md-4">
          <label class="ad-label" for="bt-cv">Chu vi <span class="ad-required">*</span></label>
          <select id="bt-cv" v-model="form.idChuVi" class="form-select ad-control" :class="{ 'is-invalid': errors.idChuVi }" :aria-invalid="!!errors.idChuVi">
            <option value="">Chọn chu vi</option>
            <option v-for="o in options('chu-vi', item?.idChuVi)" :key="o.id" :value="o.id">{{ o.ten }}</option>
          </select>
          <p v-if="errors.idChuVi" class="ad-error">{{ errors.idChuVi }}</p>
        </div>

        <div class="col-12"><h3 class="ad-form-section">Mã, giá bán và tồn kho</h3></div>
        <div class="col-md-6">
          <label class="ad-label" for="bt-ma">Mã biến thể <span class="ad-required">*</span></label>
          <input
            id="bt-ma"
            v-model="form.ma"
            type="text"
            class="form-control ad-control ad-input-code text-uppercase"
            :class="{ 'is-invalid': errors.ma }"
            :readonly="isEdit"
            maxlength="50"
            autocomplete="off"
            :aria-invalid="!!errors.ma"
            @input="codeTouched = true"
          />
          <p v-if="errors.ma" class="ad-error">{{ errors.ma }}</p>
          <p v-else-if="isEdit" class="ad-hint">Không sửa tay được. Nếu đổi sản phẩm thì phần đầu mã tự đổi theo (SP008-… thành SP007-…).</p>
          <p v-else class="ad-hint">Tự gợi ý theo sản phẩm, màu, trọng lượng, chu vi; có thể sửa.</p>
        </div>
        <div class="col-md-3">
          <label class="ad-label" for="bt-gia">Giá bán <span class="ad-required">*</span></label>
          <div class="ad-affix">
            <input id="bt-gia" v-model.number="form.giaBan" type="number" min="0" step="1000" class="form-control ad-control has-suffix" :class="{ 'is-invalid': errors.giaBan }" :aria-invalid="!!errors.giaBan" />
            <span class="ad-suffix" aria-hidden="true">₫</span>
          </div>
          <p v-if="errors.giaBan" class="ad-error">{{ errors.giaBan }}</p>
        </div>
        <div class="col-md-3">
          <label class="ad-label" for="bt-ton">Tồn kho <span class="ad-required">*</span></label>
          <input id="bt-ton" v-model.number="form.soLuongTon" type="number" min="0" step="1" class="form-control ad-control" :class="{ 'is-invalid': errors.soLuongTon }" :aria-invalid="!!errors.soLuongTon" />
          <p v-if="errors.soLuongTon" class="ad-error">{{ errors.soLuongTon }}</p>
        </div>
      </div>
    </form>

    <template #footer>
      <button type="button" class="ad-btn" :disabled="saving" @click="emit('close')">Hủy</button>
      <button type="submit" form="bt-form" class="ad-btn ad-btn-primary" :disabled="saving">
        <span v-if="saving" class="spinner-border spinner-border-sm" aria-hidden="true"></span>
        {{ isEdit ? 'Lưu thay đổi' : 'Thêm biến thể' }}
      </button>
    </template>
  </BaseModal>
</template>
