<template>
  <div style="position: fixed; top: 0; left: 0; width: calc(100vw / var(--ui-zoom)); height: calc(100vh / var(--ui-zoom)); background: rgba(0,0,0,0.4); backdrop-filter: blur(4px); z-index: 1050; display: flex; align-items: center; justify-content: center;">
    <div class="modal-dialog bg-white rounded-4 shadow-lg" style="width: 800px; max-width: 95vw; pointer-events: auto; animation: slideDown 0.3s ease-out;">
      
      <!-- Header -->
      <div class="modal-header d-flex justify-content-between align-items-center px-4 pt-4 pb-3 border-bottom border-light">
        <div class="d-flex align-items-center">
          <div class="bg-primary bg-opacity-10 text-primary rounded-circle d-flex align-items-center justify-content-center me-3" style="width: 40px; height: 40px;">
            <i class="bi bi-geo-alt-fill fs-5"></i>
          </div>
          <div>
            <h5 class="fw-bold mb-0 text-dark">Địa chỉ của {{ customer?.hoTen || 'Khách Hàng' }}</h5>
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
            <!-- Tỉnh/Thành phố -> Phường/Xã theo đơn vị hành chính sau sáp nhập (không còn Quận/Huyện) -->
            <DiaChiHanhChinhSelect
              v-model:tinh-thanh="formData.tinhThanhPho"
              v-model:phuong-xa="formData.phuongXa"
              id-prefix="kh-dc"
              col-class="col-md-6"
            />
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
              <button class="btn btn-primary" :disabled="saving" @click="saveAddress">Lưu địa chỉ</button>
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
                    <button class="btn btn-sm btn-outline-danger rounded-pill px-3" style="font-size: 0.8rem; font-weight: 500;" @click="deleteAddress(addr)">Xóa</button>
                    <button class="btn btn-sm btn-outline-primary rounded-pill px-3" style="font-size: 0.8rem; font-weight: 500;" @click="editAddress(addr)">Sửa</button>
                  </div>
                </div>
                <p class="mb-1 text-muted" style="font-size: 0.9rem;">{{ addr.sdtNguoiNhan }}</p>
                <p class="mb-0 text-muted" style="font-size: 0.9rem;">{{ ghepDiaChi(addr) }}</p>
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

    <ConfirmDialog
      v-if="confirmDelete"
      title="Xóa địa chỉ"
      message="Bạn có chắc chắn muốn xóa địa chỉ này không?"
      confirm-text="Xóa"
      variant="danger"
      :loading="deleting"
      @confirm="doDeleteAddress"
      @cancel="confirmDelete = null"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../services/api'
import ConfirmDialog from './common/ConfirmDialog.vue'
import DiaChiHanhChinhSelect from './common/DiaChiHanhChinhSelect.vue'
import { ghepDiaChi } from '../utils/diaChi'
import { useToast } from '../composables/useToast'

const toast = useToast()

const props = defineProps({
  customer: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['close', 'address-updated'])
const addresses = ref([])
const showForm = ref(false)
const formTitle = ref('')

const formData = ref({
  id: null,
  tenNguoiNhan: '',
  sdtNguoiNhan: '',
  tinhThanhPho: '',
  phuongXa: '',
  quanHuyen: '',
  diaChiCuThe: '',
  laDiaChiMacDinh: false
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
    toast.error(error.message || 'Không tải được danh sách địa chỉ.')
  }
}

const addNewAddress = () => {
  formData.value = {
    id: null,
    tenNguoiNhan: props.customer.hoTen || '',
    sdtNguoiNhan: props.customer.sdt || '',
    tinhThanhPho: '',
    phuongXa: '',
    diaChiCuThe: '',
    laDiaChiMacDinh: addresses.value.length === 0
  }
  formTitle.value = 'Thêm địa chỉ mới'
  showForm.value = true
}

const editAddress = (addr) => {
  // Tên tỉnh / phường xã lấy nguyên từ địa chỉ đã lưu. Địa chỉ cũ (trước sáp nhập) không còn trong danh sách mới
  // thì ô chọn vẫn hiện giá trị cũ kèm lời nhắc chọn lại; địa chỉ có Quận/Huyện cũ vẫn được giữ nguyên khi lưu.
  formData.value = {
    id: addr.id,
    tenNguoiNhan: addr.tenNguoiNhan,
    sdtNguoiNhan: addr.sdtNguoiNhan,
    tinhThanhPho: addr.tinhThanhPho || '',
    phuongXa: addr.phuongXa || '',
    quanHuyen: addr.quanHuyen || '',
    diaChiCuThe: addr.diaChiCuThe,
    laDiaChiMacDinh: addr.laDiaChiMacDinh
  }

  formTitle.value = 'Cập nhật địa chỉ'
  showForm.value = true
}

const saving = ref(false)

const saveAddress = async () => {
  if (!formData.value.tenNguoiNhan || !formData.value.sdtNguoiNhan || !formData.value.tinhThanhPho || !formData.value.phuongXa || !formData.value.diaChiCuThe) {
    toast.error('Vui lòng điền đủ họ tên, số điện thoại, tỉnh/thành phố, phường/xã và địa chỉ cụ thể!')
    return
  }
  if (!/^[0-9]{10,11}$/.test(formData.value.sdtNguoiNhan)) {
    toast.error('Số điện thoại người nhận không hợp lệ (chỉ gồm 10-11 chữ số).')
    return
  }
  saving.value = true
  try {
    // Địa chỉ mới (sau sáp nhập) không có Quận/Huyện: chỉ giữ lại quận/huyện cũ khi tỉnh/phường xã vẫn là giá trị cũ chưa đổi
    const goc = addresses.value.find((a) => a.id === formData.value.id)
    const giuQuanHuyenCu = goc && goc.tinhThanhPho === formData.value.tinhThanhPho && goc.phuongXa === formData.value.phuongXa
    formData.value.quanHuyen = giuQuanHuyenCu ? goc.quanHuyen || '' : ''
    if (formData.value.id) {
      await api.put(`/dia-chi/${formData.value.id}`, formData.value)
      toast.success('Đã cập nhật địa chỉ.')
    } else {
      await api.post(`/dia-chi/khach-hang/${props.customer.id}`, formData.value)
      toast.success('Đã thêm địa chỉ mới.')
    }
    showForm.value = false
    await fetchAddresses()
    emit('address-updated')
  } catch (error) {
    console.error(error)
    toast.error(error.message || 'Có lỗi xảy ra khi lưu địa chỉ!')
  } finally {
    saving.value = false
  }
}

const confirmDelete = ref(null)
const deleting = ref(false)

const deleteAddress = (addr) => {
  confirmDelete.value = addr
}

const doDeleteAddress = async () => {
  const addr = confirmDelete.value
  if (!addr) return
  deleting.value = true
  try {
    await api.delete(`/dia-chi/${addr.id}`)
    toast.success('Đã xóa địa chỉ.')
    confirmDelete.value = null
    await fetchAddresses()
    emit('address-updated')
  } catch (error) {
    console.error(error)
    toast.error(error.message || 'Không thể xóa địa chỉ này.')
  } finally {
    deleting.value = false
  }
}

onMounted(() => {
  fetchAddresses()
})
</script>

<style scoped>
@keyframes slideDown {
  from { opacity: 0; transform: translateY(-20px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
