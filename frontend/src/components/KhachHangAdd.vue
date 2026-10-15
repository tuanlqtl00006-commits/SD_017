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
              <div class="position-relative d-inline-block">
                <label for="avatar-upload" class="d-inline-block position-relative rounded-circle overflow-hidden shadow-sm" style="width: 120px; height: 120px; cursor: pointer; border: 3px solid #f1f3f5;">
                  <input type="file" id="avatar-upload" class="d-none" accept="image/*" @change="handleFileUpload" >
                  <img v-if="formData.anhDaiDien" :src="formData.anhDaiDien" class="w-100 h-100" style="object-fit: cover;">
                  <span v-else class="ad-avatar w-100 h-100 ad-avatar-soft d-flex align-items-center justify-content-center" style="font-size: 2.5rem; background-color: #f1f3f5; color: #adb5bd;">
                    <i class="bi bi-camera"></i>
                  </span>
                  
                  <!-- Overlay lúc đang tải -->
                  <div v-if="isUploading" class="position-absolute top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center bg-white bg-opacity-75">
                    <div class="spinner-border spinner-border-sm text-primary" role="status"></div>
                  </div>
                </label>
                <div  class="position-absolute bottom-0 end-0 bg-primary text-white rounded-circle d-flex align-items-center justify-content-center" style="width: 32px; height: 32px; transform: translate(-10px, -10px); pointer-events: none;">
                  <i class="bi bi-pencil-fill small"></i>
                </div>
              </div>
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
                    <input type="text" class="form-control bg-light border-0" :class="{ 'is-invalid': errors.ten }" v-model="formData.ten" placeholder="Nhập họ và tên" :maxlength="TEN_MAX + 20" @input="errors.ten = ''" @blur="kiemTra('ten')">
                    <small v-if="errors.ten" class="text-danger mt-1 d-block">{{ errors.ten }}</small>
                  </div>
                  <div class="col-md-6">
                    <label class="form-label small text-muted fw-semibold">Email <span class="text-danger">*</span></label>
                    <input type="email" class="form-control bg-light border-0" :class="{ 'is-invalid': errors.email }" v-model="formData.email" placeholder="Nhập email" :maxlength="EMAIL_MAX + 20" @input="errors.email = ''" @blur="kiemTra('email')">
                    <small v-if="errors.email" class="text-danger mt-1 d-block">{{ errors.email }}</small>
                  </div>
                  <div class="col-md-6">
                    <label class="form-label small text-muted fw-semibold">Số điện thoại <span class="text-danger">*</span></label>
                    <input type="text" class="form-control bg-light border-0" :class="{ 'is-invalid': errors.sdt }" v-model="formData.sdt" placeholder="Nhập số điện thoại" maxlength="20" @input="errors.sdt = ''" @blur="kiemTra('sdt')">
                    <small v-if="errors.sdt" class="text-danger mt-1 d-block">{{ errors.sdt }}</small>
                  </div>
                  <div class="col-md-6">
                    <label class="form-label small text-muted fw-semibold">Giới tính</label>
                    <select class="form-select bg-light border-0" v-model="formData.gioiTinh">
                      <option :value="1">Nam</option>
                      <option :value="0">Nữ</option>
                    </select>
                  </div>
                  <div class="col-12">
                      <label class="form-label small text-muted fw-semibold">Ngày sinh <span class="text-muted fw-normal">(không bắt buộc, từ {{ TUOI_MIN }} đến {{ TUOI_MAX }} tuổi)</span></label>
                      <input type="date" class="form-control bg-light border-0" :class="{ 'is-invalid': errors.ngaySinh }" v-model="formData.ngaySinh" :min="gioiHanNgay.min" :max="gioiHanNgay.max" @input="errors.ngaySinh = ''" @blur="kiemTra('ngaySinh')">
                      <small v-if="errors.ngaySinh" class="text-danger mt-1 d-block">{{ errors.ngaySinh }}</small>
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
                    :invalid-tinh="!!errors.tinhThanh"
                    :invalid-phuong="!!errors.phuongXa"
                  />
                  <div v-if="errors.tinhThanh || errors.phuongXa" class="col-12 mt-1">
                    <small class="text-danger d-block">{{ errors.tinhThanh || errors.phuongXa }}</small>
                  </div>
                  <div class="col-12">
                    <label class="form-label small text-muted fw-semibold">Địa chỉ cụ thể <span class="text-danger">*</span></label>
                    <input type="text" class="form-control bg-light border-0" :class="{ 'is-invalid': errors.diaChiChiTiet }" v-model="addressData.diaChiChiTiet" placeholder="Số nhà, đường..." :maxlength="DIA_CHI_MAX + 20" @input="errors.diaChiChiTiet = ''" @blur="kiemTra('diaChiChiTiet')">
                    <small v-if="errors.diaChiChiTiet" class="text-danger mt-1 d-block">{{ errors.diaChiChiTiet }}</small>
                  </div>
                </div>
              </div>
            </div>

            <!-- Nút Tạo & Hủy -->
            <div class="d-flex justify-content-end mb-5 gap-3">
              <button class="btn btn-outline-secondary px-5 py-2 fw-bold shadow-sm" style="border-radius: 8px;" :disabled="isSaving" @click="router.push('/khach-hang')">
                  Quay lại
                </button>
              <button class="btn btn-primary px-5 py-2 fw-bold shadow-sm" style="border-radius: 8px; background-color: #0977ec; border-color: #0977ec;" :disabled="isSaving || isUploading" @click="createCustomer">
                <span v-if="isSaving" class="spinner-border spinner-border-sm me-2" role="status"></span>
                <i v-else class="bi bi-save me-2"></i> {{ isSaving ? 'Đang lưu...' : 'Tạo khách hàng' }}
              </button>
            </div>

          </div>
        </div>
      </div>
  </div>
</template>

<script setup>
import { ref, reactive, watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import api from '../services/api'
import DiaChiHanhChinhSelect from './common/DiaChiHanhChinhSelect.vue'
import { useToast } from '../composables/useToast'
import {
  TEN_MAX, EMAIL_MAX, DIA_CHI_MAX, TUOI_MIN, TUOI_MAX,
  chuanHoaKhoangTrang, chuanHoaSdt, chuanHoaEmail,
  kiemTraHoTen, kiemTraSdt, kiemTraEmail, kiemTraNgaySinh,
  kiemTraTinhThanh, kiemTraPhuongXa, kiemTraDiaChiCuThe, gioiHanNgaySinh,
} from '../utils/khachHangValidate'

const router = useRouter()
const toast = useToast()

const errorMessage = ref('')
const isSaving = ref(false)
const gioiHanNgay = gioiHanNgaySinh()

// Lỗi của từng ô, '' là hợp lệ
const errors = reactive({
  ten: '', email: '', sdt: '', ngaySinh: '',
  tinhThanh: '', phuongXa: '', diaChiChiTiet: '',
})

const isUploading = ref(false)

const handleFileUpload = async (e) => {
  
  const file = e.target.files[0]
  if (!file) return

  // Backend chỉ nhận JPG, PNG, WEBP, GIF và tối đa 5 MB
  const loaiHopLe = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']
  if (!loaiHopLe.includes(file.type)) {
    toast.error('Chỉ nhận ảnh JPG, PNG, WEBP hoặc GIF.')
    e.target.value = ''
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    toast.error(`Ảnh quá lớn (${(file.size / 1024 / 1024).toFixed(1)} MB), tối đa 5 MB.`)
    e.target.value = ''
    return
  }
  
  isUploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', file)
    const { data } = await api.post('/hinh-anh/upload', fd, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    formData.value.anhDaiDien = data.duongDan
    toast.success('Tải ảnh lên thành công!')
  } catch (err) {
    console.error(err)
    const lyDo = err.response ? err.message : 'Không kết nối được máy chủ, kiểm tra Backend đã chạy chưa.'
    toast.error('Lỗi khi tải ảnh lên: ' + (lyDo || 'không rõ nguyên nhân'))
  } finally {
    isUploading.value = false
    e.target.value = ''
  }
}

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
  trangThai: 1,
    anhDaiDien: ''
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

// Chọn lại tỉnh / phường thì xóa lỗi cũ
watch(() => addressData.value.tinhThanh, () => { errors.tinhThanh = '' })
watch(() => addressData.value.phuongXa, () => { errors.phuongXa = '' })

// Kiểm tra 1 ô, trả về true nếu hợp lệ
const kiemTra = (field) => {
  const f = formData.value
  const a = addressData.value
  const luat = {
    ten: () => kiemTraHoTen(f.ten),
    email: () => kiemTraEmail(f.email),
    sdt: () => kiemTraSdt(f.sdt),
    ngaySinh: () => kiemTraNgaySinh(f.ngaySinh),
    tinhThanh: () => kiemTraTinhThanh(a.tinhThanh),
    phuongXa: () => kiemTraPhuongXa(a.phuongXa),
    diaChiChiTiet: () => kiemTraDiaChiCuThe(a.diaChiChiTiet),
  }
  errors[field] = luat[field]()
  return !errors[field]
}

// Đưa câu lỗi từ server về đúng ô (server kiểm tra lại y hệt, phòng khi gọi API trực tiếp)
const ganLoiTuServer = (msg) => {
  const m = msg.toLowerCase()
  if (m.includes('email')) errors.email = msg
  else if (m.includes('điện thoại')) errors.sdt = msg
  else if (m.includes('họ tên')) errors.ten = msg
  else if (m.includes('ngày sinh') || m.includes('tuổi')) errors.ngaySinh = msg
  else if (m.includes('tỉnh')) errors.tinhThanh = msg
  else if (m.includes('phường')) errors.phuongXa = msg
  else if (m.includes('địa chỉ')) errors.diaChiChiTiet = msg
  else return false
  return true
}

const createCustomer = async () => {
  if (isSaving.value) return
  errorMessage.value = ''

  // Kiểm tra hết các ô một lượt để người dùng thấy tất cả lỗi
  const hopLe = Object.keys(errors).map(kiemTra).every(Boolean)
  if (!hopLe) {
    errorMessage.value = 'Vui lòng kiểm tra lại các ô được đánh dấu đỏ.'
    return
  }

  const sdt = chuanHoaSdt(formData.value.sdt)
  const hoTen = chuanHoaKhoangTrang(formData.value.ten)
  // Địa chỉ giao hàng nhập ở form này sẽ được backend lưu thành địa chỉ mặc định của khách hàng
  const payload = {
    hoTen,
    email: chuanHoaEmail(formData.value.email),
    sdt,
    gioiTinh: formData.value.gioiTinh,
    trangThai: 1,
    anhDaiDien: formData.value.anhDaiDien || null,
    ngaySinh: formData.value.ngaySinh || null,
    tenNguoiNhan: hoTen,
    sdtNguoiNhan: sdt,
    tinhThanh: addressData.value.tinhThanh,
    phuongXa: addressData.value.phuongXa,
    diaChiCuThe: chuanHoaKhoangTrang(addressData.value.diaChiChiTiet),
  }

  isSaving.value = true
  try {
    await api.post('/khach-hang', payload)
    toast.success('Đã tạo khách hàng mới.')
    // Quay lại trang danh sách
    router.push('/khach-hang')
  } catch (error) {
    console.error(error)
    const msg = error.message || ''
    if (!error.response) {
      errorMessage.value = msg || 'Không kết nối được máy chủ.'
    } else if (ganLoiTuServer(msg)) {
      errorMessage.value = 'Không thể tạo khách hàng: ' + msg
    } else {
      errorMessage.value = 'Không thể tạo khách hàng: ' + (msg || 'Lỗi máy chủ.')
    }
  } finally {
    isSaving.value = false
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




