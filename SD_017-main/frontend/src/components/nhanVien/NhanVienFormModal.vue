<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import BaseModal from '../common/BaseModal.vue'
import { GIOI_TINH, VAI_TRO, taoMaNhanVien, tinhTuoi } from '../../constants/nhanVien'
import { avatarColors, getInitials, toIso } from '../../utils/format'

const props = defineProps({
  item: { type: Object, default: null }, // null: thêm mới
  all: { type: Array, default: () => [] }, // toàn bộ nhân viên, dùng để kiểm tra trùng
  saving: { type: Boolean, default: false },
})
const emit = defineEmits(['save', 'close'])

const isEdit = computed(() => !!props.item)
const maNhanVien = computed(() => (props.item ? props.item.ma : taoMaNhanVien(props.all)))

const form = reactive(
  props.item
    ? {
        taiKhoan: props.item.taiKhoan,
        hoTen: props.item.hoTen,
        email: props.item.email,
        soDienThoai: props.item.soDienThoai,
        gioiTinh: props.item.gioiTinh,
        ngaySinh: props.item.ngaySinh ?? '',
        diaChi: props.item.diaChi,
        vaiTro: props.item.vaiTro,
        anh: props.item.anh ?? null,
      }
    : {
        taiKhoan: '',
        hoTen: '',
        email: '',
        soDienThoai: '',
        gioiTinh: 'Nam',
        ngaySinh: '',
        diaChi: '',
        vaiTro: 'NHAN_VIEN',
        anh: null,
      },
)

const errors = reactive({})
const anhError = ref('')
const submitted = ref(false)
const fileInput = ref(null)

// Ngày sinh tối đa: đủ 18 tuổi tính đến hôm nay
const maxBirthDate = computed(() => {
  const d = new Date()
  d.setFullYear(d.getFullYear() - 18)
  return toIso(d)
})

function isTaken(field, value) {
  const needle = String(value).trim().toLowerCase()
  return props.all.some((e) => e.id !== props.item?.id && String(e[field]).toLowerCase() === needle)
}

function validate() {
  const e = {}

  const taiKhoan = String(form.taiKhoan).trim()
  if (!taiKhoan) e.taiKhoan = 'Nhập tên tài khoản.'
  else if (!/^[a-zA-Z0-9._-]{4,30}$/.test(taiKhoan)) e.taiKhoan = 'Tài khoản gồm 4-30 ký tự: chữ, số, dấu chấm, gạch dưới hoặc gạch ngang.'
  else if (isTaken('taiKhoan', taiKhoan)) e.taiKhoan = 'Tên tài khoản đã được sử dụng.'

  const hoTen = String(form.hoTen).trim()
  if (!hoTen) e.hoTen = 'Nhập họ tên.'
  else if (hoTen.length < 2 || hoTen.length > 60) e.hoTen = 'Họ tên từ 2 đến 60 ký tự.'

  const email = String(form.email).trim()
  if (!email) e.email = 'Nhập email.'
  else if (!/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(email)) e.email = 'Email chưa đúng định dạng, ví dụ: ten@footstyle.vn.'
  else if (isTaken('email', email)) e.email = 'Email đã được sử dụng.'

  const sdt = String(form.soDienThoai).trim()
  if (!sdt) e.soDienThoai = 'Nhập số điện thoại.'
  else if (!/^0[35789]\d{8}$/.test(sdt)) e.soDienThoai = 'Số điện thoại gồm 10 chữ số, bắt đầu bằng 03, 05, 07, 08 hoặc 09.'
  else if (isTaken('soDienThoai', sdt)) e.soDienThoai = 'Số điện thoại đã được sử dụng.'

  if (form.ngaySinh && tinhTuoi(form.ngaySinh) < 18) e.ngaySinh = 'Nhân viên phải từ đủ 18 tuổi.'

  const diaChi = String(form.diaChi).trim()
  if (!diaChi) e.diaChi = 'Nhập địa chỉ.'
  else if (diaChi.length > 255) e.diaChi = 'Địa chỉ tối đa 255 ký tự.'

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

function onPickImage(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  if (!/^image\/(jpeg|png|webp)$/.test(file.type)) {
    anhError.value = 'Chỉ nhận ảnh JPG, PNG hoặc WebP.'
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    anhError.value = 'Ảnh tối đa 2 MB.'
    return
  }
  const reader = new FileReader()
  reader.onload = () => {
    form.anh = reader.result
    anhError.value = ''
  }
  reader.onerror = () => {
    anhError.value = 'Không đọc được ảnh. Vui lòng chọn ảnh khác.'
  }
  reader.readAsDataURL(file)
}

function removeImage() {
  form.anh = null
  anhError.value = ''
}

function submit() {
  submitted.value = true
  const e = refreshErrors()
  if (Object.keys(e).length) {
    nextTick(() => document.querySelector('#nv-form .is-invalid')?.focus())
    return
  }
  emit('save', {
    taiKhoan: String(form.taiKhoan).trim(),
    hoTen: String(form.hoTen).trim().replace(/\s+/g, ' '),
    email: String(form.email).trim().toLowerCase(),
    soDienThoai: String(form.soDienThoai).trim(),
    gioiTinh: form.gioiTinh,
    ngaySinh: form.ngaySinh || null,
    diaChi: String(form.diaChi).trim(),
    vaiTro: form.vaiTro,
    anh: form.anh,
  })
}

onMounted(() => {
  document.getElementById('nv-ho-ten')?.focus()
})
</script>

<template>
  <BaseModal :title="isEdit ? 'Chỉnh sửa nhân viên' : 'Thêm nhân viên'" size="lg" static-backdrop @close="emit('close')">
    <form id="nv-form" novalidate @submit.prevent="submit">
      <div class="ad-avatar-upload">
        <span class="ad-avatar ad-avatar-lg" :style="form.anh ? undefined : avatarColors(form.hoTen || '?')">
          <img v-if="form.anh" :src="form.anh" alt="Ảnh đại diện đang chọn" />
          <template v-else>{{ getInitials(form.hoTen) }}</template>
        </span>
        <div>
          <div class="d-flex flex-wrap gap-2">
            <button type="button" class="ad-btn" @click="fileInput.click()">
              <i class="bi bi-camera" aria-hidden="true"></i> {{ form.anh ? 'Đổi ảnh' : 'Chọn ảnh' }}
            </button>
            <button v-if="form.anh" type="button" class="ad-btn" @click="removeImage">Xóa ảnh</button>
          </div>
          <input ref="fileInput" type="file" class="d-none" accept="image/jpeg,image/png,image/webp" @change="onPickImage" />
          <p v-if="anhError" class="ad-error" role="alert">{{ anhError }}</p>
          <p v-else class="ad-hint">JPG, PNG hoặc WebP, tối đa 2 MB. Không có ảnh sẽ hiện chữ cái đầu của tên.</p>
        </div>
      </div>

      <div class="row g-3">
        <div class="col-md-4">
          <label class="ad-label" for="nv-ma">Mã nhân viên</label>
          <input id="nv-ma" type="text" class="form-control ad-control" :value="maNhanVien" readonly aria-describedby="nv-ma-hint" />
          <p id="nv-ma-hint" class="ad-hint">{{ isEdit ? 'Không thể đổi mã sau khi tạo.' : 'Hệ thống tự cấp mã.' }}</p>
        </div>

        <div class="col-md-8">
          <label class="ad-label" for="nv-ho-ten">Họ tên <span class="ad-required">*</span></label>
          <input
            id="nv-ho-ten"
            v-model="form.hoTen"
            type="text"
            class="form-control ad-control"
            :class="{ 'is-invalid': errors.hoTen }"
            maxlength="60"
            placeholder="Ví dụ: Nguyễn Văn An"
            autocomplete="off"
            :aria-invalid="!!errors.hoTen"
            aria-describedby="nv-ho-ten-msg"
          />
          <p v-if="errors.hoTen" id="nv-ho-ten-msg" class="ad-error">{{ errors.hoTen }}</p>
        </div>

        <div class="col-md-6">
          <label class="ad-label" for="nv-tai-khoan">Tài khoản <span class="ad-required">*</span></label>
          <input
            id="nv-tai-khoan"
            v-model="form.taiKhoan"
            type="text"
            class="form-control ad-control"
            :class="{ 'is-invalid': errors.taiKhoan }"
            maxlength="30"
            placeholder="Ví dụ: an.nv"
            autocomplete="off"
            :aria-invalid="!!errors.taiKhoan"
            aria-describedby="nv-tai-khoan-msg"
          />
          <p v-if="errors.taiKhoan" id="nv-tai-khoan-msg" class="ad-error">{{ errors.taiKhoan }}</p>
        </div>

        <div class="col-md-6">
          <label class="ad-label" for="nv-email">Email <span class="ad-required">*</span></label>
          <input
            id="nv-email"
            v-model="form.email"
            type="email"
            class="form-control ad-control"
            :class="{ 'is-invalid': errors.email }"
            placeholder="ten@footstyle.vn"
            autocomplete="off"
            :aria-invalid="!!errors.email"
            aria-describedby="nv-email-msg"
          />
          <p v-if="errors.email" id="nv-email-msg" class="ad-error">{{ errors.email }}</p>
        </div>

        <div class="col-md-6">
          <label class="ad-label" for="nv-sdt">Số điện thoại <span class="ad-required">*</span></label>
          <input
            id="nv-sdt"
            v-model="form.soDienThoai"
            type="tel"
            inputmode="numeric"
            class="form-control ad-control"
            :class="{ 'is-invalid': errors.soDienThoai }"
            maxlength="10"
            placeholder="0912345678"
            autocomplete="off"
            :aria-invalid="!!errors.soDienThoai"
            aria-describedby="nv-sdt-msg"
          />
          <p v-if="errors.soDienThoai" id="nv-sdt-msg" class="ad-error">{{ errors.soDienThoai }}</p>
        </div>

        <div class="col-md-6">
          <label class="ad-label" for="nv-ngay-sinh">Ngày sinh</label>
          <input
            id="nv-ngay-sinh"
            v-model="form.ngaySinh"
            type="date"
            :max="maxBirthDate"
            class="form-control ad-control"
            :class="{ 'is-invalid': errors.ngaySinh }"
            :aria-invalid="!!errors.ngaySinh"
            aria-describedby="nv-ngay-sinh-msg"
          />
          <p v-if="errors.ngaySinh" id="nv-ngay-sinh-msg" class="ad-error">{{ errors.ngaySinh }}</p>
        </div>

        <div class="col-md-6">
          <span id="nv-gioi-tinh-label" class="ad-label">Giới tính</span>
          <div class="ad-segment" role="radiogroup" aria-labelledby="nv-gioi-tinh-label">
            <label v-for="g in GIOI_TINH" :key="g" class="ad-segment-item" :class="{ active: form.gioiTinh === g }">
              <input v-model="form.gioiTinh" type="radio" class="visually-hidden" name="gioiTinh" :value="g" />
              {{ g }}
            </label>
          </div>
        </div>

        <div class="col-md-6">
          <label class="ad-label" for="nv-vai-tro-form">Vai trò</label>
          <select id="nv-vai-tro-form" v-model="form.vaiTro" class="form-select ad-control">
            <option v-for="(opt, key) in VAI_TRO" :key="key" :value="key">{{ opt.label }}</option>
          </select>
        </div>

        <div class="col-12">
          <label class="ad-label" for="nv-dia-chi">Địa chỉ <span class="ad-required">*</span></label>
          <textarea
            id="nv-dia-chi"
            v-model="form.diaChi"
            rows="2"
            maxlength="255"
            class="form-control ad-control"
            :class="{ 'is-invalid': errors.diaChi }"
            placeholder="Số nhà, đường, phường/xã, quận/huyện, tỉnh/thành phố"
            :aria-invalid="!!errors.diaChi"
            aria-describedby="nv-dia-chi-msg"
          ></textarea>
          <p v-if="errors.diaChi" id="nv-dia-chi-msg" class="ad-error">{{ errors.diaChi }}</p>
        </div>
      </div>
    </form>

    <template #footer>
      <button type="button" class="ad-btn" :disabled="saving" @click="emit('close')">Hủy</button>
      <button type="submit" form="nv-form" class="ad-btn ad-btn-primary" :disabled="saving">
        <span v-if="saving" class="spinner-border spinner-border-sm" aria-hidden="true"></span>
        {{ isEdit ? 'Lưu thay đổi' : 'Thêm nhân viên' }}
      </button>
    </template>
  </BaseModal>
</template>
