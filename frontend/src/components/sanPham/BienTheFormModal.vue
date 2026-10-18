<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import BaseModal from '../common/BaseModal.vue'
import MoneyInput from '../common/MoneyInput.vue'
import Select2 from '../common/Select2.vue'
import { tuDanhSach } from '../../utils/luaChon'

const props = defineProps({
  item: { type: Object, default: null }, // null: tạo mới
  all: { type: Array, default: () => [] }, // toàn bộ biến thể, để kiểm tra trùng mã / tổ hợp
  sanPhams: { type: Array, default: () => [] },
  options: { type: Function, required: true }, // (slug, idDangChon) => danh sách cho dropdown
  laVotFn: { type: Function, default: () => true }, // (idSanPham) => sản phẩm thuộc danh mục vợt (có trọng lượng + chu vi)?
  idKadFn: { type: Function, default: () => null }, // (slug) => id mục "Không áp dụng" của bảng thuộc tính
  saving: { type: Boolean, default: false },
  themNhanh: { type: Function, default: null }, // (slug, ten) => Promise<mục mới | null>: "Thêm nhanh" màu / trọng lượng / chu vi ngay trong ô chọn
  serverError: { type: String, default: '' },
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
const touched = reactive({})

function cham(f) {
  touched[f] = true
  refreshErrors()
}

// Danh mục vợt: chọn màu sắc + trọng lượng + chu vi. Danh mục khác (phụ kiện, túi - balo...): chỉ màu sắc,
// trọng lượng và chu vi tự là "Không áp dụng".
const laVot = computed(() => (form.idSanPham === '' ? true : props.laVotFn(form.idSanPham)))
const idTl = () => (laVot.value ? form.idTrongLuong : props.idKadFn('trong-luong') ?? '')
const idCv = () => (laVot.value ? form.idChuVi : props.idKadFn('chu-vi') ?? '')
// Chuyển sang sản phẩm là vợt mà đang giữ "Không áp dụng" thì bắt chọn lại
function lamSachTheoDanhMuc() {
  if (!laVot.value) return
  if (form.idTrongLuong === props.idKadFn('trong-luong')) form.idTrongLuong = ''
  if (form.idChuVi === props.idKadFn('chu-vi')) form.idChuVi = ''
}
lamSachTheoDanhMuc()
watch(laVot, lamSachTheoDanhMuc)

const sanPhamOptions = computed(() => props.sanPhams.filter((p) => p.hoatDong || p.id === props.item?.idSanPham))
const dsSanPham = computed(() => sanPhamOptions.value.map((p) => ({ value: p.id, label: `${p.ma} - ${p.ten}` })))
const dsThuocTinh = (slug) => tuDanhSach(props.options(slug, props.item?.[{ 'mau-sac': 'idMauSac', 'trong-luong': 'idTrongLuong', 'chu-vi': 'idChuVi' }[slug]]))
// "Thêm nhanh": tạo thuộc tính mới rồi chọn luôn vào ô
async function themNhanhVaChon(slug, field, ten) {
  const moi = await props.themNhanh?.(slug, ten)
  if (moi) form[field] = moi.id
}

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
  () => [form.idSanPham, form.idMauSac, form.idTrongLuong, form.idChuVi, laVot.value],
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
  if (form.idSanPham === '' || form.idSanPham === null) {
    e.idSanPham = 'Chọn sản phẩm.'
  } else {
    const spDaChon = props.sanPhams.find((p) => p.id === Number(form.idSanPham))
    if (spDaChon && !spDaChon.hoatDong && !isEdit.value) {
      e.idSanPham = 'Sản phẩm đã ngưng hoạt động, không thể thêm biến thể mới.'
    }
  }
  if (form.idMauSac === '' || form.idMauSac === null) e.idMauSac = 'Chọn màu sắc.'
  if (laVot.value && (form.idTrongLuong === '' || form.idTrongLuong === null)) e.idTrongLuong = 'Chọn trọng lượng.'
  if (laVot.value && (form.idChuVi === '' || form.idChuVi === null)) e.idChuVi = 'Chọn chu vi.'
  if (!laVot.value && (idTl() === '' || idCv() === '')) e.idSanPham = 'CSDL chưa có mục "Không áp dụng". Hãy khởi động lại backend rồi tải lại trang.'

  const trungBoBa =
    !e.idSanPham && !e.idMauSac && !e.idTrongLuong && !e.idChuVi &&
    props.all.some(
      (b) => b.id !== props.item?.id && b.idSanPham === Number(form.idSanPham) && b.idMauSac === Number(form.idMauSac) && b.idTrongLuong === Number(idTl()) && b.idChuVi === Number(idCv()),
    )

  const ma = String(form.ma || '').trim().toUpperCase()
  if (!ma) e.ma = 'Nhập mã biến thể.'
  else if (!/^[A-Z0-9-]{3,50}$/.test(ma)) e.ma = 'Mã gồm 3-50 ký tự chữ, số hoặc dấu gạch ngang, không dấu, không khoảng trắng.'
  else if (!(trungBoBa && !isEdit.value) && props.all.some((b) => b.id !== props.item?.id && b.ma === ma)) e.ma = 'Mã biến thể đã tồn tại.'

  const gia = Number(form.giaBan)
  if (form.giaBan === '' || form.giaBan === null || Number.isNaN(gia)) e.giaBan = 'Nhập giá bán.'
  else if (gia < 1000) e.giaBan = 'Giá bán tối thiểu 1.000 ₫.'
  else if (gia > 1000000000) e.giaBan = 'Giá bán tối đa 1.000.000.000 ₫.'

  const ton = Number(form.soLuongTon)
  if (form.soLuongTon === '' || form.soLuongTon === null || Number.isNaN(ton) || !Number.isInteger(ton) || ton < 0) {
    e.soLuongTon = 'Số lượng tồn là số nguyên từ 0 trở lên.'
  }

  if (trungBoBa && isEdit.value) e.idMauSac = laVot.value ? 'Sản phẩm đã có biến thể với màu sắc, trọng lượng và chu vi này.' : 'Sản phẩm đã có biến thể với màu sắc này.'

  if (props.serverError) {
    if (props.serverError.includes('Mã biến thể') || props.serverError.includes('mã')) e.ma = props.serverError
    else if (props.serverError.includes('Giá bán') || props.serverError.includes('giá')) e.giaBan = props.serverError
    else if (props.serverError.includes('Số lượng') || props.serverError.includes('tồn')) e.soLuongTon = props.serverError
    else if (props.serverError.includes('màu') || props.serverError.includes('tổ hợp')) e.idMauSac = props.serverError
    else e.idSanPham = props.serverError
  }

  return e
}

function refreshErrors() {
  const e = validate()
  Object.keys(errors).forEach((k) => delete errors[k])
  for (const [k, v] of Object.entries(e)) {
    if (submitted.value || touched[k] || (props.serverError && e[k] === props.serverError)) {
      errors[k] = v
    }
  }
  return e
}
watch(form, () => {
  refreshErrors()
})
watch(() => props.serverError, () => refreshErrors())

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
    idTrongLuong: Number(idTl()),
    idChuVi: Number(idCv()),
    ma: String(form.ma).trim().toUpperCase(),
    giaBan: Number(form.giaBan),
    soLuongTon: Number(form.soLuongTon),
  })
}

onMounted(() => document.getElementById('bt-san-pham')?.focus())

defineExpose({ form, touched, cham, validate, refreshErrors })
</script>

<template>
  <BaseModal :title="isEdit ? 'Chỉnh sửa biến thể' : 'Thêm biến thể sản phẩm'" size="lg" static-backdrop @close="emit('close')">
    <form id="bt-form" novalidate @submit.prevent="submit">
      <div class="row g-3">
        <div class="col-12">
          <label class="ad-label" for="bt-san-pham">Sản phẩm <span class="ad-required">*</span></label>
          <Select2
            id="bt-san-pham"
            v-model="form.idSanPham"
            :options="dsSanPham"
            class="ad-control"
            :class="{ 'is-invalid': errors.idSanPham }"
            placeholder="Chọn sản phẩm"
            :aria-invalid="!!errors.idSanPham"
          />
          <p v-if="errors.idSanPham" class="ad-error">{{ errors.idSanPham }}</p>
          <p v-else-if="isEdit" class="ad-hint">Có thể đổi sang sản phẩm khác (chỉ chọn được sản phẩm đang hoạt động).</p>
        </div>

        <div class="col-12"><h3 class="ad-form-section">Thuộc tính biến thể</h3></div>
        <div :class="laVot ? 'col-md-4' : 'col-md-12'">
          <label class="ad-label" for="bt-mau">Màu sắc <span class="ad-required">*</span></label>
          <Select2
            id="bt-mau"
            v-model="form.idMauSac"
            :options="dsThuocTinh('mau-sac')"
            class="ad-control"
            :class="{ 'is-invalid': errors.idMauSac }"
            placeholder="Chọn màu sắc"
            :allow-create="!!themNhanh"
            :aria-invalid="!!errors.idMauSac"
            @create="themNhanhVaChon('mau-sac', 'idMauSac', $event)"
          />
          <p v-if="errors.idMauSac" class="ad-error">{{ errors.idMauSac }}</p>
        </div>
        <div v-if="laVot" class="col-md-4">
          <label class="ad-label" for="bt-tl">Trọng lượng <span class="ad-required">*</span></label>
          <Select2
            id="bt-tl"
            v-model="form.idTrongLuong"
            :options="dsThuocTinh('trong-luong')"
            class="ad-control"
            :class="{ 'is-invalid': errors.idTrongLuong }"
            placeholder="Chọn trọng lượng"
            :allow-create="!!themNhanh"
            :aria-invalid="!!errors.idTrongLuong"
            @create="themNhanhVaChon('trong-luong', 'idTrongLuong', $event)"
          />
          <p v-if="errors.idTrongLuong" class="ad-error">{{ errors.idTrongLuong }}</p>
        </div>
        <div v-if="laVot" class="col-md-4">
          <label class="ad-label" for="bt-cv">Chu vi <span class="ad-required">*</span></label>
          <Select2
            id="bt-cv"
            v-model="form.idChuVi"
            :options="dsThuocTinh('chu-vi')"
            class="ad-control"
            :class="{ 'is-invalid': errors.idChuVi }"
            placeholder="Chọn chu vi"
            :allow-create="!!themNhanh"
            :aria-invalid="!!errors.idChuVi"
            @create="themNhanhVaChon('chu-vi', 'idChuVi', $event)"
          />
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
            @input="codeTouched = true; touched.ma && refreshErrors()"
            @blur="cham('ma')"
          />
          <p v-if="errors.ma" class="ad-error">{{ errors.ma }}</p>
          <p v-else-if="isEdit" class="ad-hint">Không sửa tay được. Nếu đổi sản phẩm thì phần đầu mã tự đổi theo (SP008-… thành SP007-…).</p>
          <p v-else class="ad-hint">Tự gợi ý theo sản phẩm, màu, trọng lượng, chu vi; có thể sửa.</p>
        </div>
        <div class="col-md-3">
          <label class="ad-label" for="bt-gia">Giá bán <span class="ad-required">*</span></label>
          <MoneyInput id="bt-gia" v-model="form.giaBan" :invalid="!!errors.giaBan" @blur="cham('giaBan')" />
          <p v-if="errors.giaBan" class="ad-error">{{ errors.giaBan }}</p>
        </div>
        <div class="col-md-3">
          <label class="ad-label" for="bt-ton">Tồn kho <span class="ad-required">*</span></label>
          <input
            id="bt-ton"
            v-model.number="form.soLuongTon"
            type="number"
            min="0"
            step="1"
            class="form-control ad-control"
            :class="{ 'is-invalid': errors.soLuongTon }"
            :aria-invalid="!!errors.soLuongTon"
            @blur="cham('soLuongTon')"
            @input="touched.soLuongTon && refreshErrors()"
          />
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
