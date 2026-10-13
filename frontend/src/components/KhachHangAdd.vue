<template>
  <div>
      <div>
        <!-- Cảnh báo lỗi nếu có -->
        <div v-if="errorMessage" class="alert alert-danger bg-danger bg-opacity-10 text-danger border-0 rounded-3 d-flex align-items-center mb-4 p-3" role="alert">
          <span class="fw-medium">{{ errorMessage }}</span>
        </div>

        <div class="row g-4">
          <!-- Thẻ hồ sơ: avatar chữ cái đầu + tên, email đang nhập -->
          <div class="col-12 col-xl-3">
            <div class="ad-card text-center ad-profile-card">
              <span class="ad-avatar ad-avatar-xl ad-avatar-soft">{{ avatarText }}</span>
              <h2 class="ad-card-title mt-3">{{ formData.ten?.trim() || 'Khách hàng mới' }}</h2>
              <p class="ad-card-sub">{{ formData.email?.trim() || 'Chưa cập nhật email' }}</p>
              <p class="ad-card-sub mt-3">Mã khách hàng do hệ thống tự sinh.</p>
            </div>
          </div>
          <!-- Form thông tin -->
          <div class="col-12 col-xl-9">
            
            <!-- Thông tin cơ bản -->
            <div class="card border-0 shadow-sm rounded-4 mb-4 bg-white">
              <div class="card-body p-4">
                <div class="d-flex align-items-center mb-4">
                  <div class="bg-primary bg-opacity-10 text-primary rounded-circle d-flex align-items-center justify-content-center me-3" style="width: 40px; height: 40px;">
                    <i class="bi bi-person fs-5"></i>
                  </div>
                  <div>
                    <h6 class="fw-bold m-0 text-dark">Thông tin cơ bản</h6>
                    <span class="text-muted small">Họ tên, email, liên hệ và tài khoản.</span>
                  </div>
                </div>

                <div class="row g-3">
                  <div class="col-md-6">
                    <label class="form-label small text-muted fw-semibold">Họ và tên <span class="text-danger">*</span></label>
                    <input type="text" class="form-control bg-light border-0" v-model="formData.ten" placeholder="Nhập họ và tên">
                  </div>
                  <div class="col-md-6">
                    <label class="form-label small text-muted fw-semibold">Email <span class="text-danger">*</span></label>
                    <input type="email" class="form-control bg-light border-0" :class="{ 'is-invalid border-danger': emailErrorMsg, 'border': emailErrorMsg }" v-model="formData.email" placeholder="Nhập email">
                    <div v-if="emailErrorMsg" class="text-danger small mt-1">{{ emailErrorMsg }}</div>
                  </div>
                  <div class="col-md-6">
                    <label class="form-label small text-muted fw-semibold">Số điện thoại</label>
                    <input type="text" class="form-control bg-light border-0" :class="{'is-invalid': phoneErrorMsg}" v-model="formData.sdt" placeholder="Nhập số điện thoại" @input="phoneErrorMsg = ''">
                    <small v-if="phoneErrorMsg" class="text-danger mt-1 d-block">{{ phoneErrorMsg }}</small>
                  </div>
                  <div class="col-md-6">
                    <label class="form-label small text-muted fw-semibold">Giới tính</label>
                    <select class="form-select bg-light border-0" v-model="formData.gioiTinh">
                      <option :value="1">Nam</option>
                      <option :value="0">Nữ</option>
                    </select>
                  </div>
                  <div class="col-12">
                      <label class="form-label small text-muted fw-semibold">Ngày sinh</label>
                      <input type="date" class="form-control bg-light border-0" v-model="formData.ngaySinh" min="1900-01-01" max="9999-12-31">
                    </div>
                </div>
              </div>
            </div>

            <!-- Địa chỉ giao hàng --> 
            <div class="card border-0 shadow-sm rounded-4 mb-4 bg-white">
              <div class="card-body p-4">
                <div class="d-flex align-items-center mb-4">
                  <div class="bg-info bg-opacity-10 text-info rounded-circle d-flex align-items-center justify-content-center me-3" style="width: 40px; height: 40px;">
                    <i class="bi bi-geo-alt fs-5"></i>
                  </div>
                  <div>
                    <h6 class="fw-bold m-0 text-dark">Địa chỉ giao hàng <span class="text-danger">*</span></h6>
                    <span class="text-muted small">Địa chỉ mặc định của khách hàng mới.</span>
                  </div>
                </div>

                <div class="row g-3">
                  <div class="col-md-6">
                    <label class="form-label small text-muted fw-semibold">Họ tên người nhận <span class="text-danger">*</span></label>
                    <input type="text" class="form-control bg-light border-0 text-muted" style="cursor: not-allowed;" v-model="addressData.hoTenNguoiNhan" placeholder="Nhập họ tên người nhận" readonly>
                  </div>
                  <div class="col-md-6">
                    <label class="form-label small text-muted fw-semibold">Số điện thoại <span class="text-danger">*</span></label>
                    <input type="text" class="form-control bg-light border-0 text-muted" style="cursor: not-allowed;" v-model="addressData.sdtNguoiNhan" placeholder="Nhập số điện thoại nhận" readonly>
                  </div>
                  <!-- Địa chỉ hành chính sau sáp nhập (provinces.open-api.vn v2): Tỉnh/Thành phố -> Phường/Xã, không còn Quận/Huyện -->
                  <DiaChiHanhChinhSelect
                    v-model:tinh-thanh="addressData.tinhThanh"
                    v-model:phuong-xa="addressData.phuongXa"
                    id-prefix="kh-add"
                    required
                    col-class="col-md-6"
                    select-class="form-select bg-light border-0"
                    label-class="form-label small text-muted fw-semibold"
                  />
                  <div class="col-12">
                    <label class="form-label small text-muted fw-semibold">Địa chỉ cụ thể <span class="text-danger">*</span></label>
                    <input type="text" class="form-control bg-light border-0" v-model="addressData.diaChiChiTiet" placeholder="Số nhà, đường...">
                  </div>
                </div>
              </div>
            </div>

            <!-- Nút Tạo & Hủy -->
            <div class="d-flex justify-content-end mb-5 gap-3">
              <button class="btn btn-outline-secondary px-5 py-2 fw-bold shadow-sm" style="border-radius: 8px;" @click="router.push('/khach-hang')">
                  Quay lại
                </button>
              <button class="btn btn-primary px-5 py-2 fw-bold shadow-sm" style="border-radius: 8px; background-color: #0977ec; border-color: #0977ec;" @click="createCustomer">
                <i class="bi bi-save me-2"></i> Tạo khách hàng
              </button>
            </div>

          </div>
        </div>
      </div>
  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import api from '../services/api'
import DiaChiHanhChinhSelect from './common/DiaChiHanhChinhSelect.vue'
import { useToast } from '../composables/useToast'

const router = useRouter()
const toast = useToast()

const errorMessage = ref('')
const phoneErrorMsg = ref('')
const emailErrorMsg = ref('')

const avatarText = computed(() => {
  const w = (formData.value.ten || '').trim().split(/\s+/).filter(Boolean)
  if (!w.length) return 'KH'
  return (w[0][0] + (w.length > 1 ? w[w.length - 1][0] : '')).toUpperCase()
})

const formData = ref({
  ten: '',
  sdt: '',
  email: '',
  ngaySinh: '',
  gioiTinh: 1,
  trangThai: 1
})

const addressData = ref({
  hoTenNguoiNhan: '',
  sdtNguoiNhan: '',
  diaChiChiTiet: '',
  phuongXa: '',
  tinhThanh: ''
})

// Đồng bộ 1 chiều từ trên xuống dưới
watch(() => formData.value.ten, (newVal) => {
  addressData.value.hoTenNguoiNhan = newVal
})

watch(() => formData.value.sdt, (newVal) => {
  addressData.value.sdtNguoiNhan = newVal
})

const createCustomer = async () => {
  errorMessage.value = ''
  phoneErrorMsg.value = ''
  emailErrorMsg.value = ''

  // Validate cơ bản
  if (!formData.value.ten || !formData.value.email) {
    errorMessage.value = 'Không thể tạo khách hàng: Vui lòng nhập đầy đủ thông tin.'
    return
  }
  
  if (!formData.value.sdt) {
    phoneErrorMsg.value = 'Vui lòng nhập Số điện thoại.'
    return
  }

  if (!/^[0-9]{10,11}$/.test(formData.value.sdt)) {
    phoneErrorMsg.value = 'Số điện thoại không hợp lệ (chỉ gồm 10-11 chữ số).'
    return
  }

  if (!addressData.value.hoTenNguoiNhan || !addressData.value.sdtNguoiNhan || !addressData.value.diaChiChiTiet || !addressData.value.tinhThanh || !addressData.value.phuongXa) {
    errorMessage.value = 'Không thể tạo khách hàng: Vui lòng nhập đầy đủ thông tin Địa chỉ giao hàng.'
    return
  }

  try {
    // Địa chỉ giao hàng nhập ở form này sẽ được backend lưu thành địa chỉ mặc định của khách hàng
    const payload = {
      hoTen: formData.value.ten.trim(),
      email: formData.value.email.trim(),
      sdt: formData.value.sdt.trim(),
      gioiTinh: formData.value.gioiTinh,
      trangThai: 1,
      ngaySinh: formData.value.ngaySinh || null,
      tenNguoiNhan: addressData.value.hoTenNguoiNhan,
      sdtNguoiNhan: addressData.value.sdtNguoiNhan,
      tinhThanh: addressData.value.tinhThanh,
      phuongXa: addressData.value.phuongXa,
      diaChiCuThe: addressData.value.diaChiChiTiet,
    }
    // Chặn lỗi nhập năm sinh quá 4 chữ số (ví dụ: năm 02024) từ thẻ HTML5 date picker
    if (payload.ngaySinh && payload.ngaySinh.length > 10) {
      errorMessage.value = 'Vui lòng nhập Ngày Sinh hợp lệ (năm không quá 4 chữ số).'
      return
    }

    await api.post('/khach-hang', payload)
    toast.success('Đã tạo khách hàng mới.')
    // Quay lại trang danh sách
    router.push('/khach-hang')
  } catch (error) {
    console.error(error)
    const status = error.response?.status
    const msg = error.message || ''
    if (status === 409) {
      errorMessage.value = 'Không thể tạo khách hàng: ' + msg
      if (msg.includes('Email')) {
        emailErrorMsg.value = 'Vui lòng kiểm tra lại email.'
      } else if (msg.includes('điện thoại')) {
        phoneErrorMsg.value = 'Vui lòng kiểm tra lại số điện thoại.'
      }
    } else if (status === 400) {
      errorMessage.value = msg || 'Dữ liệu không hợp lệ. Vui lòng kiểm tra lại Ngày sinh, Email hoặc Số điện thoại.'
    } else {
      errorMessage.value = 'Không thể tạo khách hàng: ' + (msg || 'Lỗi máy chủ.')
    }
  }
}
</script>

<style scoped>
.form-control:focus, .form-select:focus {
  box-shadow: 0 0 0 0.25rem rgba(9, 119, 236, 0.25);
  border: 1px solid #86b7fe !important;
}
.is-invalid {
  border: 1px solid #dc3545 !important;
  background-color: #fff8f8 !important;
}

/* Fix Chrome Autofill background color */
input:-webkit-autofill,
input:-webkit-autofill:hover, 
input:-webkit-autofill:focus, 
input:-webkit-autofill:active {
  -webkit-box-shadow: 0 0 0 30px #f8f9fa inset !important;
  -webkit-text-fill-color: #212529 !important;
  transition: background-color 5000s ease-in-out 0s;
}
</style>




