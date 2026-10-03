<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import BaseModal from '../common/BaseModal.vue'
import { GIOI_TINH, taoMaNhanVien, tinhTuoi } from '../../constants/nhanVien'
import { avatarColors, getInitials, toIso, todayIso } from '../../utils/format'

const props = defineProps({
  item: { type: Object, default: null }, // null: thêm mới
  all: { type: Array, default: () => [] }, // toàn bộ nhân viên, dùng để kiểm tra trùng
  vaiTro: { type: Array, default: () => [] }, // [{ id, ten }] lấy từ bảng vai_tro
  saving: { type: Boolean, default: false },
})
const emit = defineEmits(['save', 'close'])

const isEdit = computed(() => !!props.item)
const maNhanVien = computed(() => (props.item ? props.item.ma : taoMaNhanVien(props.all)))

// Mặc định vai trò "Nhân viên" (vai trò cuối danh sách không phải quản lý); nếu không có thì lấy vai trò đầu tiên.
const vaiTroMacDinh = () => (props.vaiTro.find((v) => v.ten !== 'Quản lý') ?? props.vaiTro[0])?.id ?? null

const form = reactive(
  props.item
    ? {
        hoTen: props.item.hoTen,
        email: props.item.email,
        soDienThoai: props.item.soDienThoai,
        gioiTinh: props.item.gioiTinh ?? 'Nam',
        ngaySinh: props.item.ngaySinh ?? '',
        diaChi: props.item.diaChi,
        ngayVaoLam: props.item.ngayVaoLam ?? '',
        idVaiTro: props.item.idVaiTro,
        matKhau: '',
      }
    : {
        hoTen: '',
        email: '',
        soDienThoai: '',
        gioiTinh: 'Nam',
        ngaySinh: '',
        diaChi: '',
        ngayVaoLam: todayIso(),
        idVaiTro: vaiTroMacDinh(),
        matKhau: '',
      },
)

const errors = reactive({})
const submitted = ref(false)
const showPassword = ref(false)

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

  if (!form.idVaiTro) e.idVaiTro = 'Chọn vai trò.'

  if (form.ngayVaoLam && form.ngaySinh) {
    const [y, m, d] = form.ngaySinh.split('-').map(Number)
    const dau18 = toIso(new Date(y + 18, m - 1, d))
    if (form.ngayVaoLam < dau18) e.ngayVaoLam = 'Ngày vào làm phải sau khi nhân viên đủ 18 tuổi.'
  }

  const diaChi = String(form.diaChi).trim()
  if (!diaChi) e.diaChi = 'Nhập địa chỉ.'
  else if (diaChi.length > 255) e.diaChi = 'Địa chỉ tối đa 255 ký tự.'

  // Thêm mới bắt buộc có mật khẩu; khi sửa để trống nghĩa là giữ mật khẩu cũ.
  if (!isEdit.value && !form.matKhau) e.matKhau = 'Nhập mật khẩu cho nhân viên mới.'
  else if (form.matKhau && (form.matKhau.length < 8 || form.matKhau.length > 50)) e.matKhau = 'Mật khẩu từ 8 đến 50 ký tự.'

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
    nextTick(() => document.querySelector('#nv-form .is-invalid')?.focus())
    return
  }
  emit('save', {
    hoTen: String(form.hoTen).trim().replace(/\s+/g, ' '),
    email: String(form.email).trim().toLowerCase(),
    soDienThoai: String(form.soDienThoai).trim(),
    gioiTinh: form.gioiTinh,
    ngaySinh: form.ngaySinh || null,
    diaChi: String(form.diaChi).trim(),
    ngayVaoLam: form.ngayVaoLam || null,
    idVaiTro: form.idVaiTro,
    matKhau: form.matKhau || null,
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
        <span class="ad-avatar ad-avatar-lg" :style="avatarColors(form.hoTen || '?')">{{ getInitials(form.hoTen) }}</span>
        <p class="ad-hint m-0">Ảnh đại diện hiển thị chữ cái đầu của họ tên.</p>
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
          <label class="ad-label" for="nv-email">Email (dùng để đăng nhập) <span class="ad-required">*</span></label>
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
          <label class="ad-label" for="nv-mat-khau">
            Mật khẩu <span v-if="!isEdit" class="ad-required">*</span>
          </label>
          <div class="ad-affix">
            <input
              id="nv-mat-khau"
              v-model="form.matKhau"
              :type="showPassword ? 'text' : 'password'"
              class="form-control ad-control has-btn"
              :class="{ 'is-invalid': errors.matKhau }"
              maxlength="50"
              :placeholder="isEdit ? 'Để trống nếu không đổi mật khẩu' : 'Từ 8 ký tự'"
              autocomplete="new-password"
              :aria-invalid="!!errors.matKhau"
              aria-describedby="nv-mat-khau-msg"
            />
            <button
              type="button"
              class="ad-affix-btn"
              :title="showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'"
              :aria-label="showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'"
              @click="showPassword = !showPassword"
            >
              <i class="bi" :class="showPassword ? 'bi-eye-slash' : 'bi-eye'" aria-hidden="true"></i>
            </button>
          </div>
          <p v-if="errors.matKhau" id="nv-mat-khau-msg" class="ad-error">{{ errors.matKhau }}</p>
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
          <label class="ad-label" for="nv-vai-tro-form">Vai trò <span class="ad-required">*</span></label>
          <select
            id="nv-vai-tro-form"
            v-model="form.idVaiTro"
            class="form-select ad-control"
            :class="{ 'is-invalid': errors.idVaiTro }"
            :aria-invalid="!!errors.idVaiTro"
            aria-describedby="nv-vai-tro-msg"
          >
            <option v-for="v in vaiTro" :key="v.id" :value="v.id">{{ v.ten }}</option>
          </select>
          <p v-if="errors.idVaiTro" id="nv-vai-tro-msg" class="ad-error">{{ errors.idVaiTro }}</p>
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
          <label class="ad-label" for="nv-ngay-vao-lam">Ngày vào làm</label>
          <input
            id="nv-ngay-vao-lam"
            v-model="form.ngayVaoLam"
            type="date"
            class="form-control ad-control"
            :class="{ 'is-invalid': errors.ngayVaoLam }"
            :aria-invalid="!!errors.ngayVaoLam"
            aria-describedby="nv-ngay-vao-lam-msg"
          />
          <p v-if="errors.ngayVaoLam" id="nv-ngay-vao-lam-msg" class="ad-error">{{ errors.ngayVaoLam }}</p>
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
