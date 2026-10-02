<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import BaseModal from '../common/BaseModal.vue'
import { HINH_THUC, LOAI_GIAM, taoMaPhieu } from '../../constants/phieuGiamGia'
import { todayIso } from '../../utils/format'

const props = defineProps({
  item: { type: Object, default: null }, // null: tạo mới
  all: { type: Array, default: () => [] }, // toàn bộ phiếu, dùng để kiểm tra trùng mã
  saving: { type: Boolean, default: false },
})
const emit = defineEmits(['save', 'close'])

const isEdit = computed(() => !!props.item)

const form = reactive(
  props.item
    ? {
        ma: props.item.ma,
        ten: props.item.ten,
        hinhThuc: props.item.hinhThuc,
        loaiGiam: props.item.loaiGiam,
        giaTri: props.item.giaTri,
        giamToiDa: props.item.giamToiDa ?? '',
        donToiThieu: props.item.donToiThieu ?? 0,
        soLuong: props.item.soLuong,
        ngayBatDau: props.item.ngayBatDau,
        ngayKetThuc: props.item.ngayKetThuc,
        moTa: props.item.moTa ?? '',
      }
    : {
        ma: taoMaPhieu(props.all.map((p) => p.ma)),
        ten: '',
        hinhThuc: 'CONG_KHAI',
        loaiGiam: 'PHAN_TRAM',
        giaTri: '',
        giamToiDa: '',
        donToiThieu: 0,
        soLuong: 1,
        ngayBatDau: todayIso(),
        ngayKetThuc: '',
        moTa: '',
      },
)

const isPercent = computed(() => form.loaiGiam === 'PHAN_TRAM')
const errors = reactive({})
const submitted = ref(false)

// Đổi sang giảm theo số tiền thì bỏ "giảm tối đa" (chỉ dùng cho giảm theo %).
watch(
  () => form.loaiGiam,
  (value) => {
    if (value !== 'PHAN_TRAM') form.giamToiDa = ''
  },
)

function regenerateCode() {
  form.ma = taoMaPhieu(props.all.map((p) => p.ma))
}

function validate() {
  const e = {}
  const ma = String(form.ma).trim().toUpperCase()
  if (!ma) e.ma = 'Nhập mã phiếu.'
  else if (!/^[A-Z0-9]{4,20}$/.test(ma)) e.ma = 'Mã gồm 4-20 ký tự chữ hoặc số, không dấu, không khoảng trắng.'
  else if (!isEdit.value && props.all.some((p) => p.ma === ma)) e.ma = 'Mã phiếu đã tồn tại.'

  const ten = String(form.ten).trim()
  if (!ten) e.ten = 'Nhập tên phiếu.'
  else if (ten.length > 100) e.ten = 'Tên phiếu tối đa 100 ký tự.'

  const giaTri = Number(form.giaTri)
  if (form.giaTri === '' || Number.isNaN(giaTri)) e.giaTri = 'Nhập giá trị giảm.'
  else if (isPercent.value && (giaTri < 1 || giaTri > 100)) e.giaTri = 'Phần trăm giảm từ 1 đến 100.'
  else if (!isPercent.value && giaTri < 1000) e.giaTri = 'Số tiền giảm tối thiểu 1.000 ₫.'

  if (isPercent.value && form.giamToiDa !== '' && !(Number(form.giamToiDa) > 0)) {
    e.giamToiDa = 'Giảm tối đa phải lớn hơn 0, hoặc để trống nếu không giới hạn.'
  }

  const donToiThieu = Number(form.donToiThieu === '' ? 0 : form.donToiThieu)
  if (Number.isNaN(donToiThieu) || donToiThieu < 0) e.donToiThieu = 'Đơn tối thiểu không được âm.'
  else if (!isPercent.value && !e.giaTri && donToiThieu > 0 && giaTri > donToiThieu) {
    e.donToiThieu = 'Đơn tối thiểu phải lớn hơn hoặc bằng số tiền giảm.'
  }

  const soLuong = Number(form.soLuong)
  if (form.soLuong === '' || !Number.isInteger(soLuong) || soLuong < 1) e.soLuong = 'Số lượng là số nguyên từ 1 trở lên.'

  if (!form.ngayBatDau) e.ngayBatDau = 'Chọn ngày bắt đầu.'
  if (!form.ngayKetThuc) e.ngayKetThuc = 'Chọn ngày kết thúc.'
  else if (form.ngayBatDau && form.ngayKetThuc < form.ngayBatDau) e.ngayKetThuc = 'Ngày kết thúc phải sau hoặc cùng ngày bắt đầu.'

  if (String(form.moTa).length > 255) e.moTa = 'Mô tả tối đa 255 ký tự.'
  return e
}

function refreshErrors() {
  const e = validate()
  Object.keys(errors).forEach((k) => delete errors[k])
  Object.assign(errors, e)
  return e
}

// Sau lần bấm lưu đầu tiên, báo lỗi theo thời gian thực để người dùng thấy ngay khi đã sửa đúng.
watch(form, () => {
  if (submitted.value) refreshErrors()
})

function submit() {
  submitted.value = true
  const e = refreshErrors()
  if (Object.keys(e).length) {
    nextTick(() => document.querySelector('#pgg-form .is-invalid')?.focus())
    return
  }
  emit('save', {
    ma: String(form.ma).trim().toUpperCase(),
    ten: String(form.ten).trim(),
    hinhThuc: form.hinhThuc,
    loaiGiam: form.loaiGiam,
    giaTri: Number(form.giaTri),
    giamToiDa: isPercent.value && form.giamToiDa !== '' ? Number(form.giamToiDa) : null,
    donToiThieu: Number(form.donToiThieu === '' ? 0 : form.donToiThieu),
    soLuong: Number(form.soLuong),
    ngayBatDau: form.ngayBatDau,
    ngayKetThuc: form.ngayKetThuc,
    moTa: String(form.moTa).trim(),
  })
}

onMounted(() => {
  document.getElementById('pgg-ten')?.focus()
})
</script>

<template>
  <BaseModal :title="isEdit ? 'Chỉnh sửa phiếu giảm giá' : 'Tạo phiếu giảm giá'" size="lg" static-backdrop @close="emit('close')">
    <form id="pgg-form" novalidate @submit.prevent="submit">
      <div class="row g-3">
        <div class="col-md-5">
          <label class="ad-label" for="pgg-ma">Mã phiếu <span class="ad-required">*</span></label>
          <div class="ad-affix">
            <input
              id="pgg-ma"
              v-model="form.ma"
              type="text"
              class="form-control ad-control text-uppercase"
              :class="{ 'is-invalid': errors.ma, 'has-btn': !isEdit }"
              :readonly="isEdit"
              maxlength="20"
              autocomplete="off"
              :aria-invalid="!!errors.ma"
              aria-describedby="pgg-ma-msg"
            />
            <button
              v-if="!isEdit"
              type="button"
              class="ad-affix-btn"
              title="Tạo mã khác"
              aria-label="Tạo mã khác"
              @click="regenerateCode"
            >
              <i class="bi bi-arrow-repeat" aria-hidden="true"></i>
            </button>
          </div>
          <p v-if="errors.ma" id="pgg-ma-msg" class="ad-error">{{ errors.ma }}</p>
          <p v-else-if="isEdit" id="pgg-ma-msg" class="ad-hint">Không thể đổi mã sau khi tạo.</p>
        </div>

        <div class="col-md-7">
          <label class="ad-label" for="pgg-ten">Tên phiếu <span class="ad-required">*</span></label>
          <input
            id="pgg-ten"
            v-model="form.ten"
            type="text"
            class="form-control ad-control"
            :class="{ 'is-invalid': errors.ten }"
            maxlength="100"
            placeholder="Ví dụ: Giảm 10% vợt Yonex"
            autocomplete="off"
            :aria-invalid="!!errors.ten"
            aria-describedby="pgg-ten-msg"
          />
          <p v-if="errors.ten" id="pgg-ten-msg" class="ad-error">{{ errors.ten }}</p>
        </div>

        <div class="col-md-6">
          <span id="pgg-hinh-thuc-label" class="ad-label">Hình thức</span>
          <div class="ad-segment" role="radiogroup" aria-labelledby="pgg-hinh-thuc-label">
            <label v-for="(opt, key) in HINH_THUC" :key="key" class="ad-segment-item" :class="{ active: form.hinhThuc === key }">
              <input v-model="form.hinhThuc" type="radio" class="visually-hidden" name="hinhThuc" :value="key" />
              <i class="bi" :class="opt.icon" aria-hidden="true"></i> {{ opt.label }}
            </label>
          </div>
        </div>

        <div class="col-md-6">
          <span id="pgg-loai-giam-label" class="ad-label">Loại giảm</span>
          <div class="ad-segment" role="radiogroup" aria-labelledby="pgg-loai-giam-label">
            <label v-for="(opt, key) in LOAI_GIAM" :key="key" class="ad-segment-item" :class="{ active: form.loaiGiam === key }">
              <input v-model="form.loaiGiam" type="radio" class="visually-hidden" name="loaiGiam" :value="key" />
              {{ opt.label }}
            </label>
          </div>
        </div>

        <div class="col-md-6">
          <label class="ad-label" for="pgg-gia-tri">Giá trị giảm <span class="ad-required">*</span></label>
          <div class="ad-affix">
            <input
              id="pgg-gia-tri"
              v-model.number="form.giaTri"
              type="number"
              min="0"
              class="form-control ad-control has-suffix"
              :class="{ 'is-invalid': errors.giaTri }"
              :placeholder="isPercent ? 'Từ 1 đến 100' : 'Ví dụ: 50000'"
              :aria-invalid="!!errors.giaTri"
              aria-describedby="pgg-gia-tri-msg"
            />
            <span class="ad-suffix" aria-hidden="true">{{ isPercent ? '%' : '₫' }}</span>
          </div>
          <p v-if="errors.giaTri" id="pgg-gia-tri-msg" class="ad-error">{{ errors.giaTri }}</p>
        </div>

        <div class="col-md-6">
          <label class="ad-label" for="pgg-giam-toi-da">Giảm tối đa</label>
          <div class="ad-affix">
            <input
              id="pgg-giam-toi-da"
              v-model.number="form.giamToiDa"
              type="number"
              min="0"
              class="form-control ad-control has-suffix"
              :class="{ 'is-invalid': errors.giamToiDa }"
              :disabled="!isPercent"
              :placeholder="isPercent ? 'Để trống nếu không giới hạn' : 'Chỉ áp dụng khi giảm theo %'"
              :aria-invalid="!!errors.giamToiDa"
              aria-describedby="pgg-giam-toi-da-msg"
            />
            <span class="ad-suffix" aria-hidden="true">₫</span>
          </div>
          <p v-if="errors.giamToiDa" id="pgg-giam-toi-da-msg" class="ad-error">{{ errors.giamToiDa }}</p>
        </div>

        <div class="col-md-6">
          <label class="ad-label" for="pgg-don-toi-thieu">Đơn tối thiểu</label>
          <div class="ad-affix">
            <input
              id="pgg-don-toi-thieu"
              v-model.number="form.donToiThieu"
              type="number"
              min="0"
              class="form-control ad-control has-suffix"
              :class="{ 'is-invalid': errors.donToiThieu }"
              placeholder="0 nếu không yêu cầu"
              :aria-invalid="!!errors.donToiThieu"
              aria-describedby="pgg-don-toi-thieu-msg"
            />
            <span class="ad-suffix" aria-hidden="true">₫</span>
          </div>
          <p v-if="errors.donToiThieu" id="pgg-don-toi-thieu-msg" class="ad-error">{{ errors.donToiThieu }}</p>
        </div>

        <div class="col-md-6">
          <label class="ad-label" for="pgg-so-luong">Số lượng phát hành <span class="ad-required">*</span></label>
          <input
            id="pgg-so-luong"
            v-model.number="form.soLuong"
            type="number"
            min="1"
            step="1"
            class="form-control ad-control"
            :class="{ 'is-invalid': errors.soLuong }"
            :aria-invalid="!!errors.soLuong"
            aria-describedby="pgg-so-luong-msg"
          />
          <p v-if="errors.soLuong" id="pgg-so-luong-msg" class="ad-error">{{ errors.soLuong }}</p>
        </div>

        <div class="col-md-6">
          <label class="ad-label" for="pgg-ngay-bd">Ngày bắt đầu <span class="ad-required">*</span></label>
          <input
            id="pgg-ngay-bd"
            v-model="form.ngayBatDau"
            type="date"
            class="form-control ad-control"
            :class="{ 'is-invalid': errors.ngayBatDau }"
            :aria-invalid="!!errors.ngayBatDau"
            aria-describedby="pgg-ngay-bd-msg"
          />
          <p v-if="errors.ngayBatDau" id="pgg-ngay-bd-msg" class="ad-error">{{ errors.ngayBatDau }}</p>
        </div>

        <div class="col-md-6">
          <label class="ad-label" for="pgg-ngay-kt">Ngày kết thúc <span class="ad-required">*</span></label>
          <input
            id="pgg-ngay-kt"
            v-model="form.ngayKetThuc"
            type="date"
            :min="form.ngayBatDau || undefined"
            class="form-control ad-control"
            :class="{ 'is-invalid': errors.ngayKetThuc }"
            :aria-invalid="!!errors.ngayKetThuc"
            aria-describedby="pgg-ngay-kt-msg"
          />
          <p v-if="errors.ngayKetThuc" id="pgg-ngay-kt-msg" class="ad-error">{{ errors.ngayKetThuc }}</p>
        </div>

        <div class="col-12">
          <label class="ad-label" for="pgg-mo-ta">Mô tả</label>
          <textarea
            id="pgg-mo-ta"
            v-model="form.moTa"
            rows="3"
            maxlength="255"
            class="form-control ad-control"
            :class="{ 'is-invalid': errors.moTa }"
            placeholder="Điều kiện áp dụng, sản phẩm được giảm…"
            aria-describedby="pgg-mo-ta-msg"
          ></textarea>
          <p v-if="errors.moTa" id="pgg-mo-ta-msg" class="ad-error">{{ errors.moTa }}</p>
        </div>
      </div>
    </form>

    <template #footer>
      <button type="button" class="ad-btn" :disabled="saving" @click="emit('close')">Hủy</button>
      <button type="submit" form="pgg-form" class="ad-btn ad-btn-primary" :disabled="saving">
        <span v-if="saving" class="spinner-border spinner-border-sm" aria-hidden="true"></span>
        {{ isEdit ? 'Lưu thay đổi' : 'Tạo phiếu' }}
      </button>
    </template>
  </BaseModal>
</template>
