<template>
  <div>

        
        <!-- Bộ lọc và Công cụ -->
        <div class="card bg-white rounded-4 shadow-sm border-0 mb-4">
          <div class="card-body p-4">
            <div class="d-flex align-items-center mb-3">
              <i class="bi bi-funnel text-secondary fs-4 me-2"></i>
              <div>
                <h5 class="fw-bold mb-0 text-dark">Bộ lọc</h5>
                <small class="text-muted">Tra cứu nhanh dữ liệu.</small>
              </div>
            </div>
            <div class="d-flex flex-wrap align-items-center gap-3">
            
            <div class="input-group" style="flex: 1; min-width: 250px;">
              <span class="input-group-text border-end-0 text-muted rounded-start-pill bg-light" style="padding-left: 1rem;"><i class="bi bi-search"></i></span>
              <input type="text" class="form-control border-start-0 rounded-end-pill bg-light ps-0 text-muted" placeholder="Tìm theo tên đăng nhập, họ tên, email, SĐT..." v-model="searchQuery" @keyup.enter="search">
            </div>
            
            <select class="form-select bg-light text-secondary rounded-pill" style="width: 200px;" v-model="statusFilter" @change="currentPage = 1">
              <option selected value="">Tất cả</option>
              <option value="1">Hoạt động</option>
              <option value="0">Ngừng hoạt động</option>
            </select>
            
            <button class="btn btn-outline-secondary d-flex align-items-center rounded-pill fw-medium custom-outline-btn text-muted" @click="reset">
              <i class="bi bi-arrow-counterclockwise me-2"></i> Đặt lại bộ lọc
            </button>
            
            <button type="button" @click="exportFile" class="btn btn-outline-secondary d-flex align-items-center rounded-pill fw-medium custom-outline-btn text-muted">
              <i class="bi bi-file-earmark-excel me-2"></i> Xuất Excel
            </button>
            
            <router-link to="/khach-hang/them" class="btn btn-primary d-flex align-items-center rounded-pill fw-medium shadow-sm custom-solid-btn text-decoration-none text-white" style="background-color: #0977ec; border-color: #0977ec;">
              <i class="bi bi-plus-lg me-2"></i> Thêm khách hàng
            </router-link>
          </div>
        </div>
      </div>

        <div class="card border-0 shadow-sm rounded-4 bg-white">
          <div class="card-body p-4">
            
            <div class="d-flex align-items-center mb-4">
               <div class="bg-primary bg-opacity-10 text-primary rounded-circle d-flex align-items-center justify-content-center me-3" style="width: 45px; height: 45px;">
                 <i class="bi bi-people-fill fs-4"></i>
               </div>
               <h4 class="fw-bold m-0 text-dark me-auto" style="font-size: 1.25rem;">Danh sách khách hàng</h4>
               <span class="ad-count">{{ filteredCustomers.length }} khách hàng.</span>
            </div>
            
            <div class="table-responsive">
              <table class="table align-middle text-start">
                <thead style="background-color: #f8f9fa;">
                  <tr>
                    <th class="py-3 fw-bold text-dark border-0" style="width: 60px;">STT</th>
                    <th class="py-3 fw-bold text-dark border-0" style="width: 70px;">Ảnh</th>
                    <th class="py-3 fw-bold text-dark border-0">Họ tên</th>
                    <th class="py-3 fw-bold text-dark border-0">Tên người nhận</th>
                    <th class="py-3 fw-bold text-dark border-0">Email</th>
                    <th class="py-3 fw-bold text-dark border-0" style="width: 250px;">Địa chỉ</th>
                    <th class="py-3 fw-bold text-dark border-0">Số điện thoại</th>
                    <th class="py-3 text-center fw-bold text-dark border-0">Trạng thái</th>
                    <th class="py-3 text-center fw-bold text-dark border-0" style="width: 180px;">Hành động</th>
                  </tr>
                </thead>
                <tbody class="text-dark">
                  <tr v-if="customers.length === 0">
                    <td colspan="8" class="text-center py-5">
                      <div class="d-flex flex-column align-items-center justify-content-center opacity-50">
                        <i class="bi bi-search mb-3" style="font-size: 3rem; color: #dee2e6;"></i>
                        <h6 class="text-muted fw-medium">Không tìm thấy khách hàng nào</h6>
                        <span class="text-muted small">Vui lòng thử lại với từ khóa khác (Tên, SĐT, hoặc Email)</span>
                      </div>
                    </td>
                  </tr>
                  <tr v-for="(kh, index) in paginatedCustomers" :key="kh.id">
                    <td class="py-3" style="border-bottom: 1px solid #f0f0f0;">{{ (currentPage - 1) * itemsPerPage + index + 1 }}</td>
                    <td class="py-2" style="border-bottom: 1px solid #f0f0f0;">
                      <img v-if="kh.anhDaiDien" :src="kh.anhDaiDien" class="rounded-circle shadow-sm" style="width: 38px; height: 38px; object-fit: cover;">
                      <span v-else class="ad-avatar ad-avatar-soft">{{ initials(kh.hoTen) }}</span>
                    </td>
                    <td class="py-3" style="border-bottom: 1px solid #f0f0f0;">{{ kh.hoTen }}</td>
                    <td class="py-3 text-muted fw-medium" style="border-bottom: 1px solid #f0f0f0;">{{ getDefaultAddress(kh)?.tenNguoiNhan || "Chưa cập nhật" }}</td>
                    <td class="py-3" style="border-bottom: 1px solid #f0f0f0;">{{ kh.email }}</td>
                    <td class="py-3 small" style="border-bottom: 1px solid #f0f0f0; white-space: normal;">
                      <span v-if="kh.diaChiList && kh.diaChiList.length > 0">
                        {{ ghepDiaChi(getDefaultAddress(kh)) }}
                      </span>
                      <span v-else>
                        Chưa cập nhật địa chỉ
                      </span>
                    </td>
                    <td class="py-3 text-muted" style="border-bottom: 1px solid #f0f0f0;">{{ kh.sdt }}</td>
                    <td class="py-3 text-center" style="border-bottom: 1px solid #f0f0f0;">
                        <span class="badge rounded-pill fw-medium px-3 py-2" 
                              :class="kh.trangThai === 1 ? 'bg-success bg-opacity-10 text-success border-0' : 'bg-danger bg-opacity-10 text-danger border-0'" 
                              >
                          {{ kh.trangThai === 1 ? 'Hoạt động' : 'Ngừng hoạt động' }}
                        </span>
                      </td>
                    <td class="py-3 text-center" style="border-bottom: 1px solid #f0f0f0;">
                      <div class="d-flex justify-content-center gap-2">
                                                <button class="btn btn-sm btn-light rounded-circle action-btn" :class="kh.trangThai == 1 ? 'text-success bg-success bg-opacity-10' : 'text-danger bg-danger bg-opacity-10'" @click.prevent="toggleStatus(kh)" :title="kh.trangThai == 1 ? 'Ngừng hoạt động' : 'Kích hoạt'">
                          <i class="bi bi-power"></i>
                        </button>
                        <button class="btn btn-sm btn-light text-success rounded-circle action-btn" @click.prevent="openViewModal(kh)">
                          <i class="bi bi-eye"></i>
                        </button>
                        <button class="btn btn-sm btn-light text-primary rounded-circle action-btn" @click.prevent="openEditModal(kh)" title="Quản lý địa chỉ">
                          <i class="bi bi-geo-alt-fill"></i>
                        </button>
                        <!-- <button class="btn btn-sm btn-light text-danger rounded-circle action-btn" @click.prevent="deleteCustomer(kh)">
                          <i class="bi bi-trash-fill"></i>
                        </button> -->
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

            
            <div class="d-flex justify-content-end px-4 py-4 mt-2">
              <nav>
                <ul class="pagination custom-pagination m-0 gap-1 border-0">
                  <li class="page-item" :class="{ disabled: currentPage === 1 }">
                    <a class="page-link border-0 text-muted bg-transparent" href="#" @click.prevent="goToPage(1)"><i class="bi bi-chevron-double-left fs-6"></i></a>
                  </li>
                  <li class="page-item" :class="{ disabled: currentPage === 1 }">
                    <a class="page-link border-0 text-muted bg-transparent" href="#" @click.prevent="prevPage"><i class="bi bi-chevron-left fs-6"></i></a>
                  </li>
                  
                  <li v-for="page in totalPages" :key="page" class="page-item" :class="{ active: currentPage === page }">
                    <a class="page-link border-0 fw-medium" 
                       :class="currentPage === page ? 'rounded-3 bg-primary bg-opacity-10 text-primary fw-bold' : 'text-muted bg-transparent'" 
                       href="#" @click.prevent="goToPage(page)">{{ page }}</a>
                  </li>

                  <li class="page-item" :class="{ disabled: currentPage === totalPages }">
                    <a class="page-link border-0 text-muted bg-transparent" href="#" @click.prevent="nextPage"><i class="bi bi-chevron-right fs-6"></i></a>
                  </li>
                  <li class="page-item" :class="{ disabled: currentPage === totalPages }">
                    <a class="page-link border-0 text-muted bg-transparent" href="#" @click.prevent="goToPage(totalPages)"><i class="bi bi-chevron-double-right fs-6"></i></a>
                  </li>
                </ul>
              </nav>
            </div>

          </div>
        </div>

        <!-- Modal Sửa Khách Hàng -->
        <div v-if="showModal" style="position: fixed; top: 0; left: 0; width: calc(100vw / var(--ui-zoom)); height: calc(100vh / var(--ui-zoom)); background: rgba(0,0,0,0.4); backdrop-filter: blur(4px); z-index: 1050; display: flex; align-items: center; justify-content: center;">
          <div class="modal-dialog bg-white rounded-4 shadow-lg" style="width: 750px; max-width: 95vw; pointer-events: auto; animation: slideDown 0.3s ease-out;">
            <div class="modal-header d-flex justify-content-between align-items-center px-4 pt-4 pb-2 border-bottom-0">
              <h4 class="modal-title fw-bold text-dark m-0">{{ isView ? 'Chi Tiết Khách Hàng' : 'Cập Nhật Khách Hàng' }}</h4>
              <button type="button" class="btn btn-light rounded-circle p-2 d-flex align-items-center justify-content-center" style="width: 35px; height: 35px;" @click="closeModal"><i class="bi bi-x-lg"></i></button>
            </div>
            <div class="modal-body px-4 pb-4 pt-2">
              <form novalidate @submit.prevent="saveCustomer">
                <div v-if="editError" class="alert alert-danger bg-danger bg-opacity-10 text-danger border-0 rounded-3 py-2 px-3 small mb-3">{{ editError }}</div>
                <div class="row g-3">
                  <div class="col-md-6">
                    <label class="form-label text-dark fw-medium mb-1">Mã KH</label>
                    <input type="text" class="form-control bg-light border-0 text-muted" v-model="formData.maKH" :disabled="isEdit || isView" placeholder="KH123456789">
                  </div>
                  <div class="col-md-6">
                    <label class="form-label text-dark fw-medium mb-1">Họ và Tên <span class="text-danger" v-if="!isView">*</span></label>
                    <input type="text" class="form-control bg-light border-0" :class="{ 'is-invalid': editErrors.ten }" v-model="formData.ten" placeholder="Nguyễn Hữu Việt..." :maxlength="TEN_MAX + 20" :disabled="isView" @input="editErrors.ten = ''" @blur="kiemTraSua('ten')">
                    <small v-if="editErrors.ten" class="text-danger mt-1 d-block">{{ editErrors.ten }}</small>
                  </div>
                  <div class="col-md-6">
                    <label class="form-label text-dark fw-medium mb-1">Số Điện Thoại <span class="text-danger" v-if="!isView">*</span></label>
                    <input type="text" class="form-control bg-light border-0" :class="{ 'is-invalid': editErrors.sdt }" v-model="formData.sdt" placeholder="0936..." maxlength="20" :disabled="isView" @input="editErrors.sdt = ''" @blur="kiemTraSua('sdt')">
                    <small v-if="editErrors.sdt" class="text-danger mt-1 d-block">{{ editErrors.sdt }}</small>
                  </div>
                  <div class="col-md-6">
                    <label class="form-label text-dark fw-medium mb-1">Email <span class="text-danger" v-if="!isView">*</span></label>
                    <input type="email" class="form-control bg-light border-0" :class="{ 'is-invalid': editErrors.email }" v-model="formData.email" placeholder="email@gmail.com" :maxlength="EMAIL_MAX + 20" :disabled="isView" @input="editErrors.email = ''" @blur="kiemTraSua('email')">
                    <small v-if="editErrors.email" class="text-danger mt-1 d-block">{{ editErrors.email }}</small>
                  </div>
                  <div class="col-12">
                    <label class="form-label text-dark fw-medium mb-1">Ngày Sinh <span v-if="!isView" class="text-muted fw-normal small">(không bắt buộc, từ {{ TUOI_MIN }} đến {{ TUOI_MAX }} tuổi)</span></label>
                    <input type="date" class="form-control bg-light border-0 text-muted" :class="{ 'is-invalid': editErrors.ngaySinh }" v-model="formData.ngaySinh" :disabled="isView" :min="gioiHanNgay.min" :max="gioiHanNgay.max" @input="editErrors.ngaySinh = ''" @blur="kiemTraSua('ngaySinh')">
                    <small v-if="editErrors.ngaySinh" class="text-danger mt-1 d-block">{{ editErrors.ngaySinh }}</small>
                  </div>
                  
                  <!-- Địa chỉ giao hàng đã được gỡ bỏ -->
                  
                  <div class="col-12 mt-3">
                    <h6 class="fw-bold text-dark border-bottom pb-2 mb-0">Thông tin bổ sung</h6>
                  </div>

                  <div class="col-md-6">
                    <label class="form-label text-dark fw-medium mb-1">Giới Tính</label>
                    <select class="form-select bg-light border-0" v-model="formData.gioiTinh" :disabled="isView">
                      <option :value="1">Nam</option>
                      <option :value="0">Nữ</option>
                    </select>
                  </div>
                  <div class="col-md-6">
                    <label class="form-label text-dark fw-medium mb-1">Trạng Thái</label>
                    <select class="form-select bg-light border-0" v-model="formData.trangThai" :disabled="isView">
                      <option :value="1">Hoạt động</option>
                      <option :value="0">Ngừng hoạt động</option>
                    </select>
                  </div>
                </div>
                <div class="d-flex justify-content-end gap-3 mt-4">
                  <button type="button" class="btn px-4 py-2 rounded-pill fw-bold border-0" :class="isView ? 'btn-primary text-white' : 'bg-light text-dark'" @click="closeModal" :style="isView ? 'background-color: #0977ec;' : 'background-color: #f1f3f5 !important;'">{{ isView ? 'Đóng' : 'Quay lại' }}</button>
                  <button v-if="!isView" type="submit" class="btn btn-primary px-5 py-2 rounded-pill fw-bold shadow-sm" style="background-color: #0977ec; border-color: #0977ec;" :disabled="isSaving">
                    <span v-if="isSaving" class="spinner-border spinner-border-sm me-2" role="status"></span>{{ isSaving ? 'Đang lưu...' : 'Lưu thông tin' }}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>

        <ConfirmDialog
          v-if="confirmItem"
          :title="confirmItem.trangThai === 1 ? 'Ngừng hoạt động khách hàng' : 'Kích hoạt khách hàng'"
          :message="confirmItem.trangThai === 1
            ? `Bạn có chắc chắn muốn ngừng hoạt động khách hàng ${confirmItem.hoTen}?`
            : `Bạn có chắc chắn muốn kích hoạt lại khách hàng ${confirmItem.hoTen}?`"
          :confirm-text="confirmItem.trangThai === 1 ? 'Ngừng hoạt động' : 'Kích hoạt'"
          :variant="'primary'"
          :icon="confirmItem.trangThai === 1 ? 'warning' : 'success'"
          :loading="confirmLoading"
          @confirm="confirmToggle"
          @cancel="confirmItem = null"
        />

        <!-- Address Management Modal -->
        <KhachHangAddressModal 
          v-if="showAddressModal" 
          :customer="selectedCustomerForAddress" 
          @close="showAddressModal = false"
          @address-updated="fetchCustomers()" 
        />
      
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import ConfirmDialog from './common/ConfirmDialog.vue'
import KhachHangAddressModal from './KhachHangAddressModal.vue'
import api from '../services/api' // sử dụng cấu hình axios có sẵn
import DiaChiHanhChinhSelect from './common/DiaChiHanhChinhSelect.vue'
import { ghepDiaChi } from '../utils/diaChi'
import { useToast } from '../composables/useToast'
import { exportExcel } from '../utils/exportExcel'
import {
  TEN_MAX, EMAIL_MAX, TUOI_MIN, TUOI_MAX,
  chuanHoaKhoangTrang, chuanHoaSdt, chuanHoaEmail,
  kiemTraHoTen, kiemTraSdt, kiemTraEmail, kiemTraNgaySinh, gioiHanNgaySinh,
} from '../utils/khachHangValidate'

const toast = useToast()

// Kiểm tra dữ liệu modal sửa khách hàng
const gioiHanNgay = gioiHanNgaySinh()
const isSaving = ref(false)
const editError = ref('')
const editErrors = ref({ ten: '', sdt: '', email: '', ngaySinh: '' })
const kiemTraSua = (field) => {
  const f = formData.value
  const luat = {
    ten: () => kiemTraHoTen(f.ten),
    sdt: () => kiemTraSdt(f.sdt),
    email: () => kiemTraEmail(f.email),
    ngaySinh: () => kiemTraNgaySinh(f.ngaySinh),
  }
  editErrors.value[field] = luat[field]()
  return !editErrors.value[field]
}

const showModal = ref(false)
const showAddressModal = ref(false)
const selectedCustomerForAddress = ref(null)
const isEdit = ref(false)
const formData = ref({
  maKH: '',
  ten: '',
  sdt: '',
  email: '',
  ngaySinh: '',
  gioiTinh: 1,
  trangThai: 1,
  tinhThanh: '',
  phuongXa: '',
  diaChiCuThe: ''
})

const customers = ref([])
const searchQuery = ref('')

// Chữ cái đầu của họ tên (ký tự đầu + ký tự đầu của tên) để làm avatar
const initials = (name) => {
  const w = (name || '').trim().split(/\s+/).filter(Boolean)
  if (!w.length) return 'KH'
  return (w[0][0] + (w.length > 1 ? w[w.length - 1][0] : '')).toUpperCase()
}

const fetchCustomers = async (resetPage = false) => {
  try {
    const response = await api.get('/khach-hang', {
      params: { search: searchQuery.value }
    })
    customers.value = response.data
    if (resetPage) {
      currentPage.value = 1
    } else {
      const totalPages = Math.ceil(customers.value.length / itemsPerPage.value)
      if (currentPage.value > totalPages && totalPages > 0) {
        currentPage.value = totalPages
      }
    }
  } catch (error) {
    console.error("Lỗi khi tải danh sách khách hàng:", error)
    toast.error(error.message || 'Không tải được danh sách khách hàng.')
  }
}

// Cài đặt chức năng phân trang
const currentPage = ref(1)
const itemsPerPage = 5

import { computed } from 'vue'

const statusFilter = ref('')

const filteredCustomers = computed(() => {
  if (statusFilter.value === '') {
    return customers.value
  }
  const statusVal = Number(statusFilter.value)
  return customers.value.filter(c => c.trangThai === statusVal)
})

const totalPages = computed(() => {
  return Math.ceil(filteredCustomers.value.length / itemsPerPage) || 1
})

const paginatedCustomers = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage
  return filteredCustomers.value.slice(start, start + itemsPerPage)
})

const goToPage = (page) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
  }
}

const prevPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--
  }
}

const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
  }
}

onMounted(() => {
  fetchCustomers()
})

const search = () => {
  fetchCustomers(true)
}

const getDefaultAddress = (kh) => {
  if (!kh.diaChiList || kh.diaChiList.length === 0) return null;
  const defaultAddr = kh.diaChiList.find(addr => addr.laDiaChiMacDinh);
  return defaultAddr || kh.diaChiList[0];
};

const reset = () => {
  searchQuery.value = ''
  statusFilter.value = ''
  fetchCustomers(true)
}


const exportFile = () => {
  if (filteredCustomers.value.length === 0) {
    toast.error('Không có khách hàng nào để xuất.')
    return
  }
  const fmtNgaySinh = (v) => {
    if (!v) return ''
    if (Array.isArray(v)) return `${String(v[2]).padStart(2, '0')}/${String(v[1]).padStart(2, '0')}/${v[0]}`
    const parts = String(v).substring(0, 10).split('-')
    return parts.length === 3 ? `${parts[2]}/${parts[1]}/${parts[0]}` : String(v)
  }
  exportExcel({
    filename: 'danh_sach_khach_hang.xlsx',
    sheetName: 'Khách hàng',
    columns: [
      { header: 'STT', key: 'stt', width: 6 },
      { header: 'Mã khách hàng', key: 'ma', width: 16 },
      { header: 'Họ và tên', key: 'ten', width: 26 },
      { header: 'Số điện thoại', key: 'sdt', width: 16 },
      { header: 'Email', key: 'email', width: 28 },
      { header: 'Ngày sinh', key: 'ngaySinh', width: 14 },
      { header: 'Giới tính', key: 'gioiTinh', width: 10 },
      { header: 'Địa chỉ', key: 'diaChi', width: 50 },
      { header: 'Trạng thái', key: 'trangThai', width: 16 },
    ],
    rows: filteredCustomers.value.map((kh, index) => {
      const dc = getDefaultAddress(kh)
      return {
        stt: index + 1,
        ma: kh.maKhachHang || '',
        ten: kh.hoTen || '',
        sdt: kh.sdt || '',
        email: kh.email || '',
        ngaySinh: fmtNgaySinh(kh.ngaySinh),
        gioiTinh: kh.gioiTinh === 1 ? 'Nam' : 'Nữ',
        diaChi: dc ? ghepDiaChi(dc) : '',
        trangThai: kh.trangThai === 1 ? 'Hoạt động' : 'Ngừng hoạt động',
      }
    }),
  })
  toast.success(`Đã xuất ${filteredCustomers.value.length} khách hàng ra file Excel.`)
}

const isView = ref(false)

const openEditModal = (kh) => {
  selectedCustomerForAddress.value = kh;
  showAddressModal.value = true;
}


const populateForm = (kh) => {
  editError.value = ''
  editErrors.value = { ten: '', sdt: '', email: '', ngaySinh: '' }

  let tinhThanh = ''
  let phuongXa = ''
  let diaChiCuThe = ''

  if (kh.diaChiList && kh.diaChiList.length > 0) {
    const dc = getDefaultAddress(kh) || kh.diaChiList[0]
    tinhThanh = dc.tinhThanhPho || ''
    phuongXa = dc.phuongXa || ''
    diaChiCuThe = dc.diaChiCuThe || ''
  }

  formData.value = {
    id: kh.id,
    maKH: kh.maKhachHang || '',
    ten: kh.hoTen || '',
    sdt: kh.sdt || '',
    email: kh.email || '',
    ngaySinh: kh.ngaySinh || '',
    gioiTinh: kh.gioiTinh !== undefined ? kh.gioiTinh : 1,
    trangThai: kh.trangThai !== undefined ? kh.trangThai : 1,
    tinhThanh,
    phuongXa,
    diaChiCuThe
  }
}


const confirmItem = ref(null)
const confirmLoading = ref(false)

const toggleStatus = (kh) => {
  confirmItem.value = kh
}

const confirmToggle = async () => {
  const kh = confirmItem.value
  if (!kh) return
  const dangHoatDong = kh.trangThai === 1
  confirmLoading.value = true
  try {
    await api.put(`/khach-hang/${kh.id}/trang-thai`)
    await fetchCustomers()
    toast.success(dangHoatDong ? 'Đã ngừng hoạt động khách hàng.' : 'Đã kích hoạt lại khách hàng.')
    confirmItem.value = null
  } catch (error) {
    console.error(error)
    toast.error(error.message || 'Không thể thay đổi trạng thái khách hàng.')
  } finally {
    confirmLoading.value = false
  }
}

const openViewModal = (kh) => {
  populateForm(kh)
  isEdit.value = true
  isView.value = false
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
}

const saveCustomer = async () => {
  if (isSaving.value) return
  editError.value = ''

  // Kiểm tra hết các ô một lượt
  const hopLe = Object.keys(editErrors.value).map(kiemTraSua).every(Boolean)
  if (!hopLe) {
    editError.value = 'Vui lòng kiểm tra lại các ô được đánh dấu đỏ.'
    return
  }

  const payload = {
    hoTen: chuanHoaKhoangTrang(formData.value.ten),
    sdt: chuanHoaSdt(formData.value.sdt),
    email: chuanHoaEmail(formData.value.email),
    ngaySinh: formData.value.ngaySinh || null,
    gioiTinh: formData.value.gioiTinh,
    trangThai: formData.value.trangThai,
  }
  isSaving.value = true
  try {
    await api.put(`/khach-hang/${formData.value.id}`, payload)
    closeModal()
    fetchCustomers()
    toast.success('Đã cập nhật khách hàng.')
  } catch (error) {
    console.error('Lỗi khi lưu khách hàng:', error)
    const msg = error.message || 'Có lỗi xảy ra khi lưu khách hàng. Vui lòng kiểm tra lại dữ liệu.'
    // Đưa lỗi từ server về đúng ô nếu nhận ra được
    const m = msg.toLowerCase()
    if (m.includes('email')) editErrors.value.email = msg
    else if (m.includes('điện thoại')) editErrors.value.sdt = msg
    else if (m.includes('họ tên')) editErrors.value.ten = msg
    else if (m.includes('ngày sinh') || m.includes('tuổi')) editErrors.value.ngaySinh = msg
    editError.value = msg
  } finally {
    isSaving.value = false
  }
}

const formatDate = (dateString) => {
  if (!dateString) return ''
  const parts = dateString.split('-')
  if (parts.length === 3) {
    return `${parts[2]}/${parts[1]}/${parts[0]}`
  }
  return dateString
}
</script>

<style scoped>
/* Form Input */
.form-control::placeholder {
  color: #ced4da;
}
.form-control.is-invalid {
  border: 1px solid #dc3545 !important;
  background-color: #fff8f8 !important;
}

/* Action Buttons */
.action-btn {
  width: 32px;
  height: 32px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f8f9fa;
  border: 1px solid transparent;
  transition: all 0.2s;
}
.action-btn:hover {
  background-color: white;
  border-color: #dee2e6;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}

/* Custom Buttons */
.custom-outline-btn {
  border-color: #dee2e6;
  padding: 0.5rem 1.25rem;
  background-color: white;
}
.custom-outline-btn:hover {
  background-color: #f8f9fa;
  color: #495057 !important;
}

.custom-solid-btn {
  padding: 0.5rem 1.25rem;
}
.custom-solid-btn:hover {
  background-color: #075fc0 !important;
  border-color: #075fc0 !important;
}

/* Pagination */
.custom-pagination .page-link {
  color: #495057;
  border: none !important;
  width: 38px;
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 500;
}
.custom-pagination .page-item.disabled .page-link {
  color: #adb5bd;
  background-color: #fff;
}
</style>
