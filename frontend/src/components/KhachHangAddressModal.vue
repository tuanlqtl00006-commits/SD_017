<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import BaseModal from './common/BaseModal.vue'
import DiaChiHanhChinhSelect from './common/DiaChiHanhChinhSelect.vue'
import api from '../services/api'
import { useToast } from '../composables/useToast'
import {
  SO_DIA_CHI_MAX, TEN_MAX, DIA_CHI_MAX,
  chuanHoaKhoangTrang, chuanHoaSdt,
  kiemTraHoTen, kiemTraSdt, kiemTraTinhThanh, kiemTraPhuongXa, kiemTraDiaChiCuThe,
} from '../utils/khachHangValidate'

const props = defineProps({
  customer: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['close', 'address-updated'])
const toast = useToast()

const addresses = ref([])
const loading = ref(false)

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

const fetchAddresses = async () => {
  loading.value = true
  try {
    const { data } = await api.get(`/dia-chi/khach-hang/${props.customer.id}`)
    addresses.value = data
  } catch (error) {
    console.error(error)
    toast.error('Không thể tải danh sách địa chỉ!')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchAddresses()
})

const ghepDiaChi = (addr) => {
  if (!addr) return ''
  const parts = [addr.diaChiCuThe, addr.phuongXa, addr.quanHuyen, addr.tinhThanhPho]
  return parts.filter(p => p && p.trim() !== '').join(', ')
}

const saving = ref(false)
const busyId = ref(null) // id địa chỉ đang xóa / đặt mặc định, tránh bấm liên tục
const daDuDiaChi = computed(() => !formData.value.id && addresses.value.length >= SO_DIA_CHI_MAX)

// Lỗi của từng ô
const errors = ref({ tenNguoiNhan: '', sdtNguoiNhan: '', tinhThanhPho: '', phuongXa: '', diaChiCuThe: '' })
const xoaLoi = () => {
  errors.value = { tenNguoiNhan: '', sdtNguoiNhan: '', tinhThanhPho: '', phuongXa: '', diaChiCuThe: '' }
}
watch(() => formData.value.tinhThanhPho, () => { errors.value.tinhThanhPho = '' })
watch(() => formData.value.phuongXa, () => { errors.value.phuongXa = '' })

const kiemTra = (field) => {
  const f = formData.value
  const luat = {
    // Tên / SĐT người nhận để trống thì lấy của khách hàng, có nhập thì phải hợp lệ
    tenNguoiNhan: () => (chuanHoaKhoangTrang(f.tenNguoiNhan) ? kiemTraHoTen(f.tenNguoiNhan, 'Tên người nhận') : ''),
    sdtNguoiNhan: () => (chuanHoaSdt(f.sdtNguoiNhan) ? kiemTraSdt(f.sdtNguoiNhan, 'Số điện thoại người nhận') : ''),
    tinhThanhPho: () => kiemTraTinhThanh(f.tinhThanhPho),
    phuongXa: () => kiemTraPhuongXa(f.phuongXa),
    diaChiCuThe: () => kiemTraDiaChiCuThe(f.diaChiCuThe),
  }
  errors.value[field] = luat[field]()
  return !errors.value[field]
}

const formTrong = () => ({
  id: null,
  tenNguoiNhan: '',
  sdtNguoiNhan: '',
  tinhThanhPho: '',
  quanHuyen: '',
  phuongXa: '',
  diaChiCuThe: '',
  laDiaChiMacDinh: addresses.value.length === 0
})

const huySua = () => {
  formData.value = formTrong()
  xoaLoi()
}

const saveAddress = async () => {
  if (saving.value) return
  if (daDuDiaChi.value) {
    toast.error(`Mỗi khách hàng chỉ lưu tối đa ${SO_DIA_CHI_MAX} địa chỉ. Hãy xóa bớt địa chỉ không dùng.`)
    return
  }
  const hopLe = Object.keys(errors.value).map(kiemTra).every(Boolean)
  if (!hopLe) {
    toast.error('Vui lòng kiểm tra lại các ô được đánh dấu đỏ.')
    return
  }

  const ten = chuanHoaKhoangTrang(formData.value.tenNguoiNhan) || props.customer.hoTen || ''
  const sdt = chuanHoaSdt(formData.value.sdtNguoiNhan) || chuanHoaSdt(props.customer.sdt)
  // Để trống thì lấy của khách hàng, nhưng dữ liệu khách hàng cũ có thể không hợp lệ -> báo để nhập tay
  const loiTen = kiemTraHoTen(ten, 'Tên người nhận')
  const loiSdt = kiemTraSdt(sdt, 'Số điện thoại người nhận')
  if (loiTen || loiSdt) {
    errors.value.tenNguoiNhan = loiTen
    errors.value.sdtNguoiNhan = loiSdt
    toast.error('Thông tin người nhận của khách hàng chưa hợp lệ, vui lòng nhập tay.')
    return
  }

  saving.value = true
  try {
    const payload = {
      ...formData.value,
      tenNguoiNhan: ten,
      sdtNguoiNhan: sdt,
      diaChiCuThe: chuanHoaKhoangTrang(formData.value.diaChiCuThe),
    }

    if (payload.id) {
      await api.put(`/dia-chi/${payload.id}`, payload)
      toast.success('Đã cập nhật địa chỉ.')
    } else {
      await api.post(`/dia-chi/khach-hang/${props.customer.id}`, payload)
      toast.success('Đã thêm địa chỉ mới.')
    }

    await fetchAddresses()
    // Reset form after saving
    huySua()
    emit('address-updated')
  } catch (error) {
    console.error(error)
    toast.error(error.message || 'Có lỗi xảy ra khi lưu địa chỉ!')
  } finally {
    saving.value = false
  }
}

const deleteAddress = async (addr) => {
  if (busyId.value) return
  if (addr.laDiaChiMacDinh) {
    toast.error('Không thể xóa địa chỉ mặc định!')
    return
  }
  if (!confirm('Bạn có chắc chắn muốn xóa địa chỉ này không?')) return
  busyId.value = addr.id
  try {
    await api.delete(`/dia-chi/${addr.id}`)
    toast.success('Đã xóa địa chỉ.')
    // Đang sửa đúng địa chỉ vừa xóa thì đưa form về trạng thái thêm mới
    if (formData.value.id === addr.id) huySua()
    await fetchAddresses()
    emit('address-updated')
  } catch (error) {
    console.error(error)
    toast.error(error.message || 'Lỗi khi xóa địa chỉ!')
  } finally {
    busyId.value = null
  }
}

const setAsDefault = async (addr) => {
  if (addr.laDiaChiMacDinh || busyId.value) return
  busyId.value = addr.id
  try {
    // API riêng chỉ đổi mặc định, địa chỉ cũ nhập thiếu thông tin vẫn đặt được
    await api.put(`/dia-chi/${addr.id}/mac-dinh`)
    toast.success('Đã thiết lập địa chỉ mặc định.')
    await fetchAddresses()
    emit('address-updated')
  } catch (error) {
    console.error(error)
    toast.error(error.message || 'Lỗi khi thiết lập mặc định!')
  } finally {
    busyId.value = null
  }
}

const editAddress = (addr) => {
  formData.value = { ...addr }
  xoaLoi()
}
</script>

<template>
  <BaseModal title="" size="xl" @close="emit('close')">
    <template #header>
      <div>
        <div class="d-flex align-items-center gap-2 mb-1">
          <i class="bi bi-geo-alt text-danger fs-5"></i>
          <h5 class="fw-bold mb-0 text-dark">Sổ địa chỉ khách hàng</h5>
        </div>
        <div class="text-muted small ms-4">
          {{ customer.hoTen }} - {{ customer.sdt || 'Chưa có SĐT' }}
        </div>
      </div>
    </template>

    <div class="row g-4">
      <!-- Left Column: Address List -->
      <div class="col-md-6">
        <div class="card border-0 shadow-sm rounded-3 h-100">
          <div class="card-header bg-white border-bottom-0 pt-3 pb-0">
            <h6 class="fw-bold mb-0"><i class="bi bi-list-ul me-2"></i> Danh sách địa chỉ <span class="text-muted fw-normal small">({{ addresses.length }}/{{ SO_DIA_CHI_MAX }})</span></h6>
          </div>
          <div class="card-body p-0">
            <div v-if="loading" class="text-center py-4">
              <span class="spinner-border text-primary" role="status"></span>
            </div>
            <div v-else-if="addresses.length > 0" class="mt-3">
              <table class="table align-middle mb-0" style="font-size: 0.9rem;">
                <thead>
                  <tr class="text-muted" style="border-bottom: 2px solid #dee2e6;">
                    <th class="ps-3 border-0 pb-2" style="width: 50px;">STT</th>
                    <th class="border-0 pb-2">Địa chỉ</th>
                    <th class="pe-3 border-0 pb-2 text-end" style="width: 120px; white-space: nowrap;">Mặc định</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(addr, index) in addresses" :key="addr.id" class="border-bottom">
                    <td class="ps-3 py-3">{{ index + 1 }}</td>
                    <td class="py-3">
                      <div class="fw-medium text-dark mb-1">{{ ghepDiaChi(addr) }}</div>
                      <div class="text-muted small">Người nhận: {{ addr.tenNguoiNhan }} - SĐT: {{ addr.sdtNguoiNhan }}</div>
                      
                      <!-- Hidden actions on hover -->
                      <div class="mt-2 address-actions">
                        <button class="btn btn-sm btn-link text-primary p-0 me-3 text-decoration-none" @click="editAddress(addr)">Cập nhật</button>
                        <button v-if="!addr.laDiaChiMacDinh" class="btn btn-sm btn-link text-danger p-0 text-decoration-none" :disabled="!!busyId" @click="deleteAddress(addr)">Xóa</button>
                      </div>
                    </td>
                    <td class="pe-3 py-3 text-end">
                      <span v-if="addr.laDiaChiMacDinh" class="badge bg-danger bg-opacity-10 text-danger rounded-pill d-inline-flex align-items-center justify-content-center m-0" style="width: 85px; height: 30px; font-size: 0.8rem; font-weight: 500;">Mặc định</span>
                      <button v-else class="btn btn-outline-secondary rounded-pill d-inline-flex align-items-center justify-content-center m-0" style="width: 85px; height: 30px; font-size: 0.8rem; font-weight: 500; padding: 0;" :disabled="!!busyId" @click="setAsDefault(addr)">Thiết lập</button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div v-else class="text-center py-5 text-muted">
              Khách hàng chưa có địa chỉ nào.
            </div>
          </div>
        </div>
      </div>

      <!-- Right Column: Add/Edit Form -->
      <div class="col-md-6">
        <div class="card border-0 shadow-sm rounded-3 h-100 bg-white" style="border: 1px solid #e9ecef !important;">
          <div class="card-header bg-white border-bottom-0 pt-3 pb-2">
            <h6 class="fw-bold mb-0 text-dark"><i class="bi bi-cursor me-2"></i> {{ formData.id ? 'Cập nhật địa chỉ' : 'Thêm nhanh địa chỉ' }}</h6>
          </div>
          <div class="card-body">
            <div class="row g-3">
              <div v-if="daDuDiaChi" class="col-12">
                <div class="alert alert-warning py-2 px-3 small mb-0">
                  Khách hàng đã có đủ {{ SO_DIA_CHI_MAX }} địa chỉ. Hãy xóa bớt địa chỉ không dùng để thêm mới.
                </div>
              </div>
              <div class="col-md-6">
                <label class="form-label small text-muted mb-1">Họ tên người nhận</label>
                <input type="text" class="form-control form-control-sm ad-input" :class="{ 'is-invalid': errors.tenNguoiNhan }" v-model="formData.tenNguoiNhan" placeholder="Lấy tên KH nếu trống" :maxlength="TEN_MAX + 20" @input="errors.tenNguoiNhan = ''" @blur="kiemTra('tenNguoiNhan')">
                <small v-if="errors.tenNguoiNhan" class="text-danger d-block mt-1">{{ errors.tenNguoiNhan }}</small>
              </div>
              <div class="col-md-6">
                <label class="form-label small text-muted mb-1">Số điện thoại</label>
                <input type="text" class="form-control form-control-sm ad-input" :class="{ 'is-invalid': errors.sdtNguoiNhan }" v-model="formData.sdtNguoiNhan" placeholder="Lấy SĐT KH nếu trống" maxlength="20" @input="errors.sdtNguoiNhan = ''" @blur="kiemTra('sdtNguoiNhan')">
                <small v-if="errors.sdtNguoiNhan" class="text-danger d-block mt-1">{{ errors.sdtNguoiNhan }}</small>
              </div>
              
              <DiaChiHanhChinhSelect
                v-model:tinhThanh="formData.tinhThanhPho"
                v-model:phuongXa="formData.phuongXa"
                required
                select-class="form-select form-select-sm ad-input"
                :invalid-tinh="!!errors.tinhThanhPho"
                :invalid-phuong="!!errors.phuongXa"
              />
              <div v-if="errors.tinhThanhPho || errors.phuongXa" class="col-12 mt-1">
                <small class="text-danger d-block">{{ errors.tinhThanhPho || errors.phuongXa }}</small>
              </div>
              
              <div class="col-12 mt-3">
                <label class="form-label small text-muted mb-1">Địa chỉ cụ thể <span class="text-danger">*</span></label>
                <input type="text" class="form-control form-control-sm ad-input" :class="{ 'is-invalid': errors.diaChiCuThe }" v-model="formData.diaChiCuThe" placeholder="Số nhà, đường..." :maxlength="DIA_CHI_MAX + 20" @input="errors.diaChiCuThe = ''" @blur="kiemTra('diaChiCuThe')">
                <small v-if="errors.diaChiCuThe" class="text-danger d-block mt-1">{{ errors.diaChiCuThe }}</small>
              </div>
              
              <div class="col-12 mt-2">
                <div class="form-check">
                  <input class="form-check-input" type="checkbox" id="defaultAddressCb" v-model="formData.laDiaChiMacDinh" :disabled="addresses.length === 0">
                  <label class="form-check-label small text-muted" for="defaultAddressCb">
                    Đặt làm địa chỉ mặc định
                  </label>
                </div>
              </div>
              
              <div class="col-12 mt-4 d-flex gap-2">
                <button v-if="formData.id" class="btn btn-sm btn-light flex-grow-1" :disabled="saving" @click="huySua">Hủy</button>
                <button class="btn btn-sm btn-primary text-white flex-grow-1" :disabled="saving || daDuDiaChi" @click="saveAddress">
                  {{ saving ? 'Đang lưu...' : (formData.id ? 'Cập nhật' : 'Thêm nhanh') }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </BaseModal>
</template>

<style scoped>
.address-actions {
  opacity: 0;
  transition: opacity 0.2s;
}
tr:hover .address-actions {
  opacity: 1;
}
.ad-input {
  border-radius: 6px;
  border: 1px solid #ced4da;
  padding: 0.4rem 0.75rem;
}
.ad-input:focus {
  border-color: #86b7fe;
  box-shadow: 0 0 0 0.25rem rgba(13, 110, 253, 0.25);
}
.ad-input.is-invalid {
  border-color: #dc3545;
  background-color: #fff8f8;
}
select:disabled {
  background-color: #f8f9fa !important;
  opacity: 1 !important;
  color: #6c757d !important;
}
</style>
