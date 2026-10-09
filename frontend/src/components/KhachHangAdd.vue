<template>
  <div>
        <!-- Cảnh báo lỗi nếu có -->
        <div v-if="errorMessage" class="alert alert-danger bg-danger bg-opacity-10 text-danger border-0 rounded-3 d-flex align-items-center mb-4 p-3" role="alert">
          <span class="fw-medium">{{ errorMessage }}</span>
        </div>

        <div class="row g-4">
          <!-- Ảnh đại diện (avatar) -->
          <div class="col-12 col-lg-4 col-xl-3">
            <div class="card border-0 shadow-sm bg-white" style="border-radius: 8px;">
              <div class="card-body p-4 text-center d-flex flex-column align-items-center">
                <div class="kh-avatar mb-3 position-relative" role="button" title="Bấm để chọn avatar" @click="chonAvatar" style="cursor: pointer;">
                  <img v-if="avatarPreview" :src="avatarPreview" alt="Avatar khách hàng" class="rounded-circle object-fit-cover" style="width: 160px; height: 160px; border: 1px solid #e9ecef;">
                  <div v-else class="rounded-circle d-flex align-items-center justify-content-center bg-light" style="width: 160px; height: 160px; border: 1px solid #e9ecef;">
                    <i class="bi bi-person-fill" style="font-size: 5rem; color: #ced4da;"></i>
                  </div>
                  
                  <!-- Lớp phủ mờ khi hover để biết có thể click -->
                  <div class="kh-avatar-overlay position-absolute top-0 start-0 w-100 h-100 rounded-circle d-flex align-items-center justify-content-center" style="background: rgba(0,0,0,0.3); opacity: 0; transition: opacity 0.2s;">
                    <i class="bi bi-camera-fill text-white fs-2"></i>
                  </div>
                </div>

                <input ref="avatarInput" type="file" class="d-none" accept="image/png,image/jpeg,image/webp,image/gif" @change="onAvatarChange">
                
                <h6 class="fw-bold mb-1 text-dark text-break" style="font-size: 1rem;">{{ formData.ten || 'Khách hàng mới' }}</h6>
                <div class="text-muted small text-break mb-1" style="color: #6c757d !important; font-size: 0.85rem;">{{ formData.email || 'Chưa có email' }}</div>
                <div class="text-muted small" style="color: #adb5bd !important; font-size: 0.8rem;">(Bấm vào ảnh để chọn avatar)</div>
                
                <small v-if="avatarError" class="text-danger d-block mt-2">{{ avatarError }}</small>
                
                <button v-if="avatarFile" type="button" class="btn btn-link btn-sm text-danger text-decoration-none p-0 mt-2" @click.stop="boAvatar">
                  Bỏ ảnh
                </button>
              </div>
            </div>
          </div>

          <!-- Form thông tin -->
          <div class="col-12 col-lg-8 col-xl-9">
            
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
                      <label class="form-label small text-muted fw-semibold">Ngày sinh <span class="text-danger">*</span></label>
                      <input type="date" class="form-control bg-light border-0" :class="{ 'is-invalid': ngaySinhError }" v-model="formData.ngaySinh" min="1900-01-01" max="9999-12-31" data-no-auto-picker>
                      <small v-if="ngaySinhError" class="text-danger mt-1 d-block">{{ ngaySinhError }}</small>
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
                  <div class="col-md-4">
                    <label class="form-label small text-muted fw-semibold">Tỉnh/Thành phố <span class="text-danger">*</span></label>
                    <select class="form-select bg-light border-0" v-model="addressData.tinhThanh">
                      <option value="" disabled selected>Chọn Tỉnh/Thành phố</option>
                      <option v-for="p in provinces" :key="p.id" :value="p.full_name">{{ p.full_name }}</option>
                    </select>
                  </div>
                  <div class="col-md-4">
                    <label class="form-label small text-muted fw-semibold">Quận/Huyện <span class="text-danger">*</span></label>
                    <select class="form-select bg-light border-0" v-model="addressData.quanHuyen">
                      <option value="" disabled selected>Chọn Quận/Huyện</option>
                      <option v-for="d in districts" :key="d.id" :value="d.full_name">{{ d.full_name }}</option>
                    </select>
                  </div>
                  <div class="col-md-4">
                    <label class="form-label small text-muted fw-semibold">Phường/Xã <span class="text-danger">*</span></label>
                    <select class="form-select bg-light border-0" v-model="addressData.phuongXa">
                      <option value="" disabled selected>Chọn Phường/Xã</option>
                      <option v-for="w in wards" :key="w.id" :value="w.full_name">{{ w.full_name }}</option>
                    </select>
                  </div>
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
              <button class="btn btn-primary px-5 py-2 fw-bold shadow-sm" style="border-radius: 8px; background-color: #0d6efd; border-color: #0d6efd;" @click="createCustomer">
                <i class="bi bi-save me-2"></i> Tạo khách hàng
              </button>
            </div>

          </div>
        </div>
      </div>
</template>

<script setup>
import Swal from 'sweetalert2';
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import api from '../services/api'
import { kiemTraNgaySinh } from '../utils/ngaySinhKhachHang'
import axios from 'axios'
import { hinhAnhService } from '../services/hinhAnhService'

const router = useRouter()

const provinces = ref([])
const districts = ref([])
const wards = ref([])

onMounted(async () => {
  try {
    const res = await axios.get('https://esgoo.net/api-tinhthanh/4/0.htm')
    if (res.data && res.data.error === 0) {
      provinces.value = res.data.data
    }
  } catch (error) {
    console.error("Lỗi tải tỉnh thành:", error)
  }
})

const errorMessage = ref('')
const phoneErrorMsg = ref('')
const emailErrorMsg = ref('')
const ngaySinhError = ref('')

const avatarInput = ref(null)
const avatarFile = ref(null)
const avatarPreview = ref('')
const avatarError = ref('')

const chonAvatar = () => {
  if (avatarInput.value) {
    avatarInput.value.click()
  }
}

const onAvatarChange = (e) => {
  const file = e.target.files[0]
  if (!file) return
  
  avatarError.value = ''
  
  if (!file.type.startsWith('image/')) {
    avatarError.value = 'Vui lòng chọn file hình ảnh hợp lệ.'
    e.target.value = ''
    return
  }
  
  if (file.size > 5 * 1024 * 1024) { // 5MB
    avatarError.value = 'Kích thước ảnh tối đa 5MB.'
    e.target.value = ''
    return
  }

  avatarFile.value = file
  if (avatarPreview.value) {
    URL.revokeObjectURL(avatarPreview.value)
  }
  avatarPreview.value = URL.createObjectURL(file)
}

const boAvatar = () => {
  avatarFile.value = null
  if (avatarPreview.value) {
    URL.revokeObjectURL(avatarPreview.value)
    avatarPreview.value = ''
  }
  avatarError.value = ''
  if (avatarInput.value) {
    avatarInput.value.value = ''
  }
}

onBeforeUnmount(() => {
  if (avatarPreview.value) {
    URL.revokeObjectURL(avatarPreview.value)
  }
})

const formData = ref({
  maKH: '',
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
  quanHuyen: '',
  tinhThanh: ''
})

watch(() => addressData.value.tinhThanh, (newProvinceName) => {
  addressData.value.quanHuyen = ''
  addressData.value.phuongXa = ''
  districts.value = []
  wards.value = []
  
  if (newProvinceName) {
    const province = provinces.value.find(p => p.full_name === newProvinceName)
    if (province && province.data2) {
      districts.value = province.data2
    }
  }
})

watch(() => addressData.value.quanHuyen, (newDistrictName) => {
  addressData.value.phuongXa = ''
  wards.value = []
  
  if (newDistrictName) {
    const district = districts.value.find(d => d.full_name === newDistrictName)
    if (district && district.data3) {
      wards.value = district.data3
    }
  }
})

// Đồng bộ 1 chiều từ trên xuống dưới
watch(() => formData.value.ten, (newVal) => {
  addressData.value.hoTenNguoiNhan = newVal
})

watch(() => formData.value.sdt, (newVal) => {
  addressData.value.sdtNguoiNhan = newVal
})

// Chọn/gõ xong ngày sinh là kiểm tra luôn (để trống thì chỉ báo khi bấm Tạo)
watch(() => formData.value.ngaySinh, (v) => {
  ngaySinhError.value = v ? kiemTraNgaySinh(v) : ''
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

  // Ngày sinh: bắt buộc, ngày có thật, không ở tương lai, từ 14 đến 120 tuổi
  ngaySinhError.value = kiemTraNgaySinh(formData.value.ngaySinh)
  if (ngaySinhError.value) return

  if (!addressData.value.hoTenNguoiNhan || !addressData.value.sdtNguoiNhan || !addressData.value.diaChiChiTiet || !addressData.value.tinhThanh) {
    errorMessage.value = 'Không thể tạo khách hàng: Vui lòng nhập đầy đủ thông tin Địa chỉ giao hàng.'
    return
  }

  try {
    let duongDanAvatar = null
    if (avatarFile.value) {
      try {
        duongDanAvatar = await hinhAnhService.upload(avatarFile.value)
      } catch (err) {
        errorMessage.value = 'Lỗi tải ảnh đại diện: ' + (err.response?.data?.message || err.message)
        return
      }
    }

    // Backend (entity KhachHang) dùng tên trường "hoTen", không phải "ten"
    const payload = {
      hoTen: formData.value.ten.trim(),
      sdt: formData.value.sdt.trim(),
      email: formData.value.email.trim(),
      ngaySinh: formData.value.ngaySinh, // dạng chuẩn yyyy-MM-dd
      gioiTinh: formData.value.gioiTinh,
      trangThai: formData.value.trangThai,
      anhDaiDien: duongDanAvatar
    }

    // Gộp địa chỉ mặc định vào payload (tên trường theo entity DiaChiKhachHang)
    payload.diaChiList = [{
      tenNguoiNhan: addressData.value.hoTenNguoiNhan,
      sdtNguoiNhan: addressData.value.sdtNguoiNhan,
      tinhThanhPho: addressData.value.tinhThanh,
      quanHuyen: addressData.value.quanHuyen,
      phuongXa: addressData.value.phuongXa,
      diaChiCuThe: addressData.value.diaChiChiTiet,
      laDiaChiMacDinh: true,
      trangThai: 1
    }]

    // Gọi API lưu Khách hàng và Địa chỉ 1 lần
    await api.post('/khach-hang', payload)
    Swal.fire({ toast: true, position: 'top-end', icon: 'success', title: 'Tạo khách hàng mới thành công!', showConfirmButton: false, timer: 3000, timerProgressBar: true });
    // Quay lại trang danh sách
    router.push('/khach-hang')
  } catch (error) {
    console.error(error)
    if (error.response && error.response.status === 400) {
      const backendMsg = error.response.data?.message
      if (backendMsg && /ngày sinh|tuổi/i.test(backendMsg)) {
        ngaySinhError.value = backendMsg
      } else {
        errorMessage.value = backendMsg || 'Dữ liệu không hợp lệ. Vui lòng kiểm tra lại Ngày sinh, Email hoặc Số điện thoại.'
      }
    } else if (error.response && error.response.status === 409) {
      const backendMsg = error.response.data.message;
      errorMessage.value = 'Không thể tạo khách hàng: ' + backendMsg;
      if (backendMsg.includes('Email')) {
         emailErrorMsg.value = 'Vui lòng kiểm tra lại email.';
         phoneErrorMsg.value = '';
      } else if (backendMsg.includes('điện thoại')) {
         phoneErrorMsg.value = 'Vui lòng kiểm tra lại số điện thoại.';
         emailErrorMsg.value = '';
      }
    } else {
      errorMessage.value = 'Không thể tạo khách hàng: Lỗi máy chủ.'
    }
  }
}
</script>

<style scoped>
.form-control:focus, .form-select:focus {
  box-shadow: 0 0 0 0.25rem rgba(13, 110, 253, 0.25);
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

.kh-avatar:hover .kh-avatar-overlay {
  opacity: 1 !important;
}
</style>




