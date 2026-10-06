<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import BaseModal from '../common/BaseModal.vue'
import { taoMa } from '../../constants/thuocTinh'
import { layLuaChon } from '../../utils/text'

const props = defineProps({
  item: { type: Object, default: null },
  all: { type: Array, default: () => [] },
  thuocTinh: { type: Object, required: true },
  saving: { type: Boolean, default: false },
})
const emit = defineEmits(['save', 'close'])

const isEdit = computed(() => props.item !== null)

const SELECTS = [
  { field: 'idDanhMuc', slug: 'danh-muc', label: 'Danh mục' },
  { field: 'idThuongHieu', slug: 'thuong-hieu', label: 'Thương hiệu' },
  { field: 'idXuatXu', slug: 'xuat-xu', label: 'Xuất xứ' },
  { field: 'idChatLieu', slug: 'chat-lieu', label: 'Chất liệu' },
  { field: 'idDoCung', slug: 'do-cung', label: 'Độ cứng' },
  { field: 'idDiemCanBang', slug: 'diem-can-bang', label: 'Điểm cân bằng' },
]

const form = reactive({
  ma: '',
  ten: '',
  idDanhMuc: '',
  idThuongHieu: '',
  idXuatXu: '',
  idChatLieu: '',
  idDoCung: '',
  idDiemCanBang: '',
  anhChinh: '',
  anhPhu: '',
  moTa: '',
})

if (props.item) {
  form.ma = props.item.ma
  form.ten = props.item.ten
  form.idDanhMuc = props.item.idDanhMuc
  form.idThuongHieu = props.item.idThuongHieu
  form.idXuatXu = props.item.idXuatXu
  form.idChatLieu = props.item.idChatLieu
  form.idDoCung = props.item.idDoCung
  form.idDiemCanBang = props.item.idDiemCanBang
  form.anhChinh = props.item.anhChinh
  form.anhPhu = (props.item.anhPhu || []).join('\n')
  form.moTa = props.item.moTa
} else {
  const cacMaDaCo = props.all.map((p) => p.ma)
  form.ma = taoMa('SP', cacMaDaCo)
}



const errors = reactive({})
const daBamLuu = ref(false)

function tachAnhPhu() {
  return String(form.anhPhu)
    .split('\n')
    .map((dong) => dong.trim())
    .filter((dong) => dong !== '')
}

function kiemTra() {
  for (const key of Object.keys(errors)) {
    delete errors[key]
  }

  const ma = String(form.ma).trim().toUpperCase()
  if (ma === '') {
    errors.ma = 'Nhập mã sản phẩm.'
  } else if (!/^[A-Z0-9]{3,20}$/.test(ma)) {
    errors.ma = 'Mã gồm 3-20 ký tự chữ hoặc số, không dấu, không khoảng trắng.'
  } else if (!isEdit.value && props.all.some((p) => p.ma === ma)) {
    errors.ma = 'Mã sản phẩm đã tồn tại.'
  }

  const ten = String(form.ten).trim()
  if (ten === '') {
    errors.ten = 'Nhập tên sản phẩm.'
  } else if (ten.length > 255) {
    errors.ten = 'Tên sản phẩm tối đa 255 ký tự.'
  }

  for (const s of SELECTS) {
    if (form[s.field] === '') {
      errors[s.field] = 'Chọn ' + s.label.toLowerCase() + '.'
    }
  }

  const anh = String(form.anhChinh).trim()
  if (anh !== '' && !/^https?:\/\/\S+$/i.test(anh)) {
    errors.anhChinh = 'Đường dẫn ảnh phải bắt đầu bằng http:// hoặc https://.'
  } else if (anh.length > 500) {
    errors.anhChinh = 'Đường dẫn ảnh tối đa 500 ký tự.'
  }

  const dsPhu = tachAnhPhu()
  if (dsPhu.some((u) => !/^https?:\/\/\S+$/i.test(u))) {
    errors.anhPhu = 'Mỗi dòng là một đường dẫn bắt đầu bằng http:// hoặc https://.'
  } else if (dsPhu.some((u) => u.length > 500)) {
    errors.anhPhu = 'Mỗi đường dẫn ảnh tối đa 500 ký tự.'
  } else if (dsPhu.length > 0 && anh === '') {
    errors.anhPhu = 'Nhập ảnh chính trước khi thêm ảnh phụ.'
  } else if (dsPhu.length + (anh === '' ? 0 : 1) > 10) {
    errors.anhPhu = 'Mỗi sản phẩm tối đa 10 ảnh (gồm cả ảnh chính).'
  }

  return Object.keys(errors).length === 0
}

watch(form, () => {
  if (daBamLuu.value) kiemTra()
})

function guiForm() {
  daBamLuu.value = true
  if (!kiemTra()) {
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
    anhPhu: tachAnhPhu(),
    moTa: String(form.moTa).trim(),
  })
}

onMounted(() => {
  const o = document.getElementById('sp-ten')
  if (o) o.focus()
})
</script>

<template>
  <BaseModal :title="isEdit ? 'Chỉnh sửa sản phẩm' : 'Thêm sản phẩm'" size="lg" static-backdrop @close="emit('close')">
    <form id="sp-form" novalidate @submit.prevent="guiForm">
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
            <option v-for="o in layLuaChon(thuocTinh[s.slug], item ? item[s.field] : null)" :key="o.id" :value="o.id">{{ o.ten }}</option>
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
          <label class="ad-label" for="sp-anh-phu">Ảnh phụ</label>
          <textarea
            id="sp-anh-phu"
            v-model="form.anhPhu"
            rows="2"
            class="form-control ad-control"
            :class="{ 'is-invalid': errors.anhPhu }"
            placeholder="Mỗi dòng một đường dẫn ảnh (https://…)"
            :aria-invalid="!!errors.anhPhu"
          ></textarea>
          <p v-if="errors.anhPhu" class="ad-error">{{ errors.anhPhu }}</p>
          <p v-else class="ad-hint">Không bắt buộc, tối đa 10 ảnh kể cả ảnh chính.</p>
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
