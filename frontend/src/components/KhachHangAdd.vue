<template>
  <div class="d-flex" style="min-height: 100vh; background-color: #f4f6f8;">
    <Sidebar />

    <div class="flex-grow-1 d-flex flex-column">
      <Header />

      <main class="p-4">
        
        <div v-if="errorMessage" class="alert alert-danger bg-danger bg-opacity-10 text-danger border-0 rounded-3 d-flex align-items-center mb-4 p-3" role="alert">
          <span class="fw-medium">{{ errorMessage }}</span>
        </div>

        <div class="row g-4 justify-content-center">
          
          <div class="col-12 col-xl-10">
            
            
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

            
            <div class="d-flex justify-content-end mb-5 gap-3">
              <button class="btn btn-outline-secondary px-5 py-2 fw-bold shadow-sm" style="border-radius: 8px;" @click="router.push('/khach-hang')">
                Hủy
              </button>
              <button class="btn btn-primary px-5 py-2 fw-bold shadow-sm" style="border-radius: 8px; background-color: #0d6efd; border-color: #0d6efd;" @click="createCustomer">
                <i class="bi bi-save me-2"></i> Tạo khách hàng
              </button>
            </div>

          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import Sidebar from './Sidebar.vue'
import Header from './Header.vue'
import api from '../services/api'
import axios from 'axios'

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

  if (!addressData.value.hoTenNguoiNhan || !addressData.value.sdtNguoiNhan || !addressData.value.diaChiChiTiet || !addressData.value.tinhThanh) {
    errorMessage.value = 'Không thể tạo khách hàng: Vui lòng nhập đầy đủ thông tin Địa chỉ giao hàng.'
    return
  }

  try {
    const payload = { ...formData.value }
    if (!payload.ngaySinh) {
      payload.ngaySinh = null
    } else {
      if (payload.ngaySinh.length > 10) {
        errorMessage.value = 'Vui lòng nhập Ngày Sinh hợp lệ (năm không quá 4 chữ số).'
        return
      }
    }

    payload.tinhThanh = addressData.value.tinhThanh
    payload.quanHuyen = addressData.value.quanHuyen
    payload.phuongXa = addressData.value.phuongXa
    payload.diaChiCuThe = addressData.value.diaChiChiTiet

    await api.post('/khach-hang', payload)
    
    router.push('/khach-hang')
  } catch (error) {
    console.error(error)
    if (error.response && error.response.status === 400) {
      errorMessage.value = 'Dữ liệu không hợp lệ. Vui lòng kiểm tra lại Ngày sinh, Email hoặc Số điện thoại.'
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


input:-webkit-autofill,
input:-webkit-autofill:hover, 
input:-webkit-autofill:focus, 
input:-webkit-autofill:active {
  -webkit-box-shadow: 0 0 0 30px #f8f9fa inset !important;
  -webkit-text-fill-color: #212529 !important;
  transition: background-color 5000s ease-in-out 0s;
}
</style>



