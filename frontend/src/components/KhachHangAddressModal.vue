<template>

  <div style="position: fixed; top: 0; left: 0; width: calc(100vw / var(--ui-zoom, 1)); height: calc(100vh / var(--ui-zoom, 1)); background: rgba(0,0,0,0.4); backdrop-filter: blur(4px); z-index: 1050; display: flex; align-items: center; justify-content: center;">

    <div class="modal-dialog bg-white rounded-4 shadow-lg" style="width: 800px; max-width: 95vw; pointer-events: auto; animation: slideDown 0.3s ease-out;">
      
      <!-- Header -->
      <div class="modal-header d-flex justify-content-between align-items-center px-4 pt-4 pb-3 border-bottom border-light">
        <div class="d-flex align-items-center">
          <div class="bg-primary bg-opacity-10 text-primary rounded-circle d-flex align-items-center justify-content-center me-3" style="width: 40px; height: 40px;">
            <i class="bi bi-geo-alt-fill fs-5"></i>
          </div>
          <div>
            <h5 class="fw-bold mb-0 text-dark">Địa chỉ của {{ customerName || 'Khách Hàng' }}</h5>

            <small class="text-muted">Quản lý địa chỉ giao hàng</small>
          </div>
        </div>
        <button type="button" class="btn btn-light rounded-circle p-2 d-flex align-items-center justify-content-center text-muted" style="width: 35px; height: 35px;" @click="$emit('close')"><i class="bi bi-x-lg"></i></button>
      </div>
      
      <!-- Body -->
      <div class="modal-body px-4 py-4" style="max-height: 60vh; overflow-y: auto;">
        
        <!-- Form Sửa / Thêm -->
        <div v-if="showForm" class="card border-0 bg-light rounded-4 p-3 mb-3">
          <h6 class="fw-bold mb-3">{{ formTitle }}</h6>
          <div class="row g-3">
            <div class="col-md-6">
              <label class="form-label small text-muted mb-1">Tên người nhận</label>
              <input type="text" class="form-control" v-model="formData.tenNguoiNhan">
            </div>
            <div class="col-md-6">
              <label class="form-label small text-muted mb-1">Số điện thoại</label>
              <input type="text" class="form-control" v-model="formData.sdtNguoiNhan">
            </div>
            <div class="col-md-4">
              <label class="form-label small text-muted mb-1">Tỉnh/Thành</label>
              <select class="form-select" v-model="formData.tinhThanhPho">
                <option value="">Chọn</option>
                <option v-for="p in provinces" :key="p.id" :value="p.full_name">{{ p.full_name }}</option>
              </select>
            </div>
            <div class="col-md-4">
              <label class="form-label small text-muted mb-1">Quận/Huyện</label>
              <select class="form-select" v-model="formData.quanHuyen">
                <option value="">Chọn</option>
                <option v-for="d in districts" :key="d.id" :value="d.full_name">{{ d.full_name }}</option>
              </select>
            </div>
            <div class="col-md-4">
              <label class="form-label small text-muted mb-1">Phường/Xã</label>
              <select class="form-select" v-model="formData.phuongXa">
                <option value="">Chọn</option>
                <option v-for="w in wards" :key="w.id" :value="w.full_name">{{ w.full_name }}</option>
              </select>
            </div>
            <div class="col-12">
              <label class="form-label small text-muted mb-1">Địa chỉ cụ thể</label>
              <input type="text" class="form-control" v-model="formData.diaChiCuThe">
            </div>
            <div class="col-12 mt-2">
              <div class="form-check">
                <input class="form-check-input" type="checkbox" v-model="formData.laDiaChiMacDinh" id="defaultAddress">
                <label class="form-check-label" for="defaultAddress">Đặt làm địa chỉ mặc định</label>
              </div>
            </div>
            <div class="col-12 d-flex justify-content-end gap-2 mt-3">
              <button class="btn btn-light" @click="showForm = false">Hủy</button>
              <button class="btn btn-primary" @click="saveAddress">Lưu địa chỉ</button>
            </div>
          </div>
        </div>

        <!-- List of Addresses -->
        <div v-else>
          <div v-if="addresses.length > 0">
            <div v-for="(addr, index) in addresses" :key="addr.id" class="card mb-3 rounded-4" :class="addr.laDiaChiMacDinh ? 'border-success' : 'border-light'" style="border-width: 1px; border-style: solid; background-color: #fcfcfc; box-shadow: 0 2px 4px rgba(0,0,0,0.02);">
              <div class="card-body p-3">
                <div class="d-flex justify-content-between align-items-start mb-2">
                  <div class="d-flex align-items-center gap-2">
                    <h6 class="fw-bold mb-0">{{ addr.tenNguoiNhan }}</h6>
                    <span v-if="addr.laDiaChiMacDinh" class="badge bg-success bg-opacity-10 text-success rounded-pill fw-medium px-2 py-1" style="font-size: 0.75rem;"><i class="bi bi-check-circle-fill me-1"></i> Mặc định</span>
                  </div>
                  <div class="d-flex gap-2">
                    <button class="btn btn-sm btn-outline-danger rounded-pill px-3" style="font-size: 0.8rem; font-weight: 500;" @click="deleteAddress(addr.id)">Xóa</button>
                    <button class="btn btn-sm btn-outline-primary rounded-pill px-3" style="font-size: 0.8rem; font-weight: 500;" @click="editAddress(addr)">Sửa</button>
                  </div>
                </div>
                <p class="mb-1 text-muted" style="font-size: 0.9rem;">{{ addr.sdtNguoiNhan }}</p>
                <p class="mb-0 text-muted" style="font-size: 0.9rem;">
                  {{ addr.diaChiCuThe ? addr.diaChiCuThe + ', ' : '' }}
                  {{ addr.phuongXa }}, {{ addr.quanHuyen }}, {{ addr.tinhThanhPho }}
                </p>
              </div>
            </div>
          </div>
          <div v-else class="text-center py-4 text-muted">
            Khách hàng chưa có địa chỉ nào.
          </div>
          
          <!-- Add new address button -->
          <button class="btn btn-light w-100 rounded-4 py-3 mt-2 d-flex align-items-center justify-content-center text-secondary fw-medium" style="border: 2px dashed #dee2e6; background-color: #f8f9fa;" @click="addNewAddress">
            <i class="bi bi-plus-lg me-2"></i> Thêm địa chỉ mới
          </button>
        </div>
      </div>
      
    </div>
  </div>
</template>

<script setup>

import { ref, onMounted, watch, computed } from 'vue'

import api from '../services/api'
import Swal from 'sweetalert2'

const props = defineProps({
  customer: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['close', 'address-updated'])


// Backend trả về "hoTen"; "ten" chỉ là tên biến cũ phía form
const customerName = computed(() => props.customer?.hoTen || props.customer?.ten || '')

// Thông báo nhỏ góc trên phải, tự tắt sau 3s (giống "Tạo khách hàng mới thành công!")
const showToast = (title, icon = 'success') => {
  Swal.fire({ toast: true, position: 'top-end', icon, title, showConfirmButton: false, timer: 3000, timerProgressBar: true })
}
const addresses = ref([])
const showForm = ref(false)
const formTitle = ref('')

const formData = ref({
  id: null,
  tenNguoiNhan: '',
  sdtNguoiNhan: '',
  tinhThanhPho: '',
  quanHuyen: '',
  phuongXa: '',
  diaChiCuThe: '',
  laDiaChiMacDinh: false
})

// Location data
const allLocations = ref([])
const provinces = ref([])
const districts = ref([])
const wards = ref([])

const fetchLocations = async () => {
  try {
    const response = await fetch('https://esgoo.net/api-tinhthanh/4/0.htm')
    const data = await response.json()
    if (data.error === 0) {
      allLocations.value = data.data
      provinces.value = data.data
    }
  } catch (error) {
    console.error("Error fetching locations:", error)
  }
}

watch(() => formData.value.tinhThanhPho, (newVal) => {
  if (newVal) {
    const selectedProv = allLocations.value.find(p => p.full_name === newVal)
    districts.value = selectedProv ? selectedProv.data2 : []
    
    if (!selectedProv?.data2?.find(d => d.full_name === formData.value.quanHuyen)) {
        formData.value.quanHuyen = ''
        formData.value.phuongXa = ''
        wards.value = []
    }
  } else {
    districts.value = []
    wards.value = []
    formData.value.quanHuyen = ''
    formData.value.phuongXa = ''
  }
})

watch(() => formData.value.quanHuyen, (newVal) => {
  if (newVal) {
    const selectedDist = districts.value.find(d => d.full_name === newVal)
    wards.value = selectedDist ? selectedDist.data3 : []
    
    if (!selectedDist?.data3?.find(w => w.full_name === formData.value.phuongXa)) {
        formData.value.phuongXa = ''
    }
  } else {
    wards.value = []
    formData.value.phuongXa = ''
  }
})

const fetchAddresses = async () => {
  try {
    const response = await api.get(`/dia-chi/khach-hang/${props.customer.id}`)
    addresses.value = response.data || []
    
    addresses.value.sort((a, b) => {
      if (a.laDiaChiMacDinh) return -1;
      if (b.laDiaChiMacDinh) return 1;
      return 0;
    })
  } catch (error) {
    console.error("Error fetching addresses:", error)
  }
}

const addNewAddress = () => {
  formData.value = {
    id: null,

    tenNguoiNhan: customerName.value,

    sdtNguoiNhan: props.customer.sdt || '',
    tinhThanhPho: '',
    quanHuyen: '',
    phuongXa: '',
    diaChiCuThe: '',
    laDiaChiMacDinh: addresses.value.length === 0
  }
  formTitle.value = 'Thêm địa chỉ mới'
  showForm.value = true
}

const editAddress = (addr) => {
  let matchedProv = addr.tinhThanhPho;
  let matchedDist = addr.quanHuyen;
  let matchedWard = addr.phuongXa;

  // Try to find matching locations even if prefixes are missing
  const prov = allLocations.value.find(p => p.full_name === addr.tinhThanhPho || p.name === addr.tinhThanhPho || p.full_name.includes(addr.tinhThanhPho));
  if (prov) {
    matchedProv = prov.full_name;
    districts.value = prov.data2;
    
    const dist = prov.data2.find(d => d.full_name === addr.quanHuyen || d.name === addr.quanHuyen || d.full_name.includes(addr.quanHuyen));
    if (dist) {
      matchedDist = dist.full_name;
      wards.value = dist.data3;
      
      const ward = dist.data3.find(w => w.full_name === addr.phuongXa || w.name === addr.phuongXa || w.full_name.includes(addr.phuongXa));
      if (ward) {
        matchedWard = ward.full_name;
      }
    }
  }

  formData.value = {
    id: addr.id,

    tenNguoiNhan: addr.tenNguoiNhan,
    sdtNguoiNhan: addr.sdtNguoiNhan,

    tenNguoiNhan: addr.tenNguoiNhan || customerName.value,
    sdtNguoiNhan: addr.sdtNguoiNhan || props.customer.sdt || '',
    tinhThanhPho: matchedProv,
    quanHuyen: matchedDist,
    phuongXa: matchedWard,
    diaChiCuThe: addr.diaChiCuThe,
    laDiaChiMacDinh: addr.laDiaChiMacDinh
  }
  
  formTitle.value = 'Cập nhật địa chỉ'
  showForm.value = true
}

const saveAddress = async () => {
  try {
    if (!formData.value.tenNguoiNhan || !formData.value.sdtNguoiNhan || !formData.value.tinhThanhPho) {
      Swal.fire({ icon: 'warning', title: 'Vui lòng điền đủ thông tin!' })
      return
    }
    
    if (formData.value.id) {
      await api.put(`/dia-chi/${formData.value.id}`, formData.value)
      showToast('Cập nhật địa chỉ thành công!')
    } else {
      await api.post(`/dia-chi/khach-hang/${props.customer.id}`, formData.value)
      showToast('Thêm địa chỉ mới thành công!')
    }
    
    showForm.value = false
    fetchAddresses()
    emit('address-updated')
  } catch (error) {
    Swal.fire({ icon: 'error', title: 'Có lỗi xảy ra!' })
  }
}

const deleteAddress = async (id) => {
  const result = await Swal.fire({
    title: 'Xóa địa chỉ',
    text: "Bạn có chắc chắn muốn xóa địa chỉ này không?",
    icon: 'question',
    showCancelButton: true,
    confirmButtonColor: '#0d6efd',
    cancelButtonColor: '#6c757d',
    confirmButtonText: 'Đồng ý',
    cancelButtonText: 'Hủy',
    reverseButtons: true
  })
  
  if (result.isConfirmed) {
    try {
      await api.delete(`/dia-chi/${id}`)


      showToast('Xóa địa chỉ thành công!')

      // Swal.fire('Đã xóa!', 'Địa chỉ đã được xóa.', 'success')
      fetchAddresses()
      emit('address-updated')
    } catch (error) {
      Swal.fire('Lỗi!', 'Không thể xóa địa chỉ này.', 'error')
    }
  }
}

onMounted(() => {
  fetchLocations()
  fetchAddresses()
})
</script>

<style scoped>
@keyframes slideDown {
  from { opacity: 0; transform: translateY(-20px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
