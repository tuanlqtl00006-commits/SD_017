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
            
            <button type="button" @click="exportExcel" class="btn btn-outline-secondary d-flex align-items-center rounded-pill fw-medium custom-outline-btn text-muted">
              <i class="bi bi-file-earmark-excel me-2"></i> Xuất Excel
            </button>
            
            <router-link to="/khach-hang/them" class="btn btn-primary d-flex align-items-center rounded-pill fw-medium shadow-sm custom-solid-btn text-decoration-none text-white" style="background-color: #0d6efd; border-color: #0d6efd;">
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
               <h4 class="fw-bold m-0 text-dark" style="font-size: 1.25rem;">Danh sách khách hàng</h4>
            </div>
            
            <div class="table-responsive">
              <table class="table align-middle text-start">
                <thead style="background-color: #f8f9fa;">
                  <tr>
                    <th class="py-3 fw-bold text-dark border-0" style="width: 60px;">STT</th>
                    <th class="py-3 fw-bold text-dark border-0">Họ tên</th>
                    <th class="py-3 fw-bold text-dark border-0">Email</th>
                    <th class="py-3 fw-bold text-dark border-0" style="width: 250px;">Địa chỉ</th>
                    <th class="py-3 fw-bold text-dark border-0">Số điện thoại</th>
                    <th class="py-3 text-center fw-bold text-dark border-0">Trạng thái</th>
                    <th class="py-3 text-center fw-bold text-dark border-0" style="width: 180px;">Hành động</th>
                  </tr>
                </thead>
                <tbody class="text-dark">
                  <tr v-if="customers.length === 0">
                    <td colspan="7" class="text-center py-5">
                      <div class="d-flex flex-column align-items-center justify-content-center opacity-50">
                        <i class="bi bi-search mb-3" style="font-size: 3rem; color: #dee2e6;"></i>
                        <h6 class="text-muted fw-medium">Không tìm thấy khách hàng nào</h6>
                        <span class="text-muted small">Vui lòng thử lại với từ khóa khác (Tên, SĐT, hoặc Email)</span>
                      </div>
                    </td>
                  </tr>
                  <tr v-for="(kh, index) in paginatedCustomers" :key="kh.id">
                    <td class="py-3" style="border-bottom: 1px solid #f0f0f0;">{{ kh.id }}</td>
                    <td class="py-3" style="border-bottom: 1px solid #f0f0f0;">{{ kh.hoTen || kh.ten }}</td>
                    <td class="py-3" style="border-bottom: 1px solid #f0f0f0;">{{ kh.email }}</td>
                    <td class="py-3 small" style="border-bottom: 1px solid #f0f0f0; white-space: normal;">
                      <span v-if="kh.diaChiList && kh.diaChiList.length > 0">
                        {{ getDefaultAddress(kh).diaChiCuThe ? getDefaultAddress(kh).diaChiCuThe + ', ' : '' }}{{ getDefaultAddress(kh).phuongXa }}, {{ getDefaultAddress(kh).quanHuyen }}, {{ getDefaultAddress(kh).tinhThanhPho }}
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
        <div v-if="showModal" style="position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; background: rgba(0,0,0,0.4); backdrop-filter: blur(4px); z-index: 1050; display: flex; align-items: center; justify-content: center;">
          <div class="modal-dialog bg-white rounded-4 shadow-lg" style="width: 750px; max-width: 95vw; pointer-events: auto; animation: slideDown 0.3s ease-out;">
            <div class="modal-header d-flex justify-content-between align-items-center px-4 pt-4 pb-2 border-bottom-0">
              <h4 class="modal-title fw-bold text-dark m-0">{{ isView ? 'Chi Tiết Khách Hàng' : 'Cập Nhật Khách Hàng' }}</h4>
              <button type="button" class="btn btn-light rounded-circle p-2 d-flex align-items-center justify-content-center" style="width: 35px; height: 35px;" @click="closeModal"><i class="bi bi-x-lg"></i></button>
            </div>
            <div class="modal-body px-4 pb-4 pt-2">
              <form @submit.prevent="saveCustomer">
                <div class="row g-3">
                  <div class="col-md-6">
                    <label class="form-label text-dark fw-medium mb-1">Mã KH</label>
                    <input type="text" class="form-control bg-light border-0 text-muted" v-model="formData.maKH" :disabled="isEdit || isView" placeholder="KH123456789">
                  </div>
                  <div class="col-md-6">
                    <label class="form-label text-dark fw-medium mb-1">Họ và Tên <span class="text-danger" v-if="!isView">*</span></label>
                    <input type="text" class="form-control bg-light border-0" v-model="formData.ten" required placeholder="Nguyễn Hữu Việt..." :disabled="isView">
                  </div>
                  <div class="col-md-6">
                    <label class="form-label text-dark fw-medium mb-1">Số Điện Thoại <span class="text-danger" v-if="!isView">*</span></label>
                    <input type="text" class="form-control bg-light border-0" :class="{'is-invalid': phoneErrorMsg}" v-model="formData.sdt" required placeholder="0936..." :disabled="isView" @input="phoneErrorMsg = ''">
                    <small v-if="phoneErrorMsg" class="text-danger mt-1 d-block">{{ phoneErrorMsg }}</small>
                  </div>
                  <div class="col-md-6">
                    <label class="form-label text-dark fw-medium mb-1">Email</label>
                    <input type="email" class="form-control bg-light border-0" v-model="formData.email" placeholder="email@gmail.com" :disabled="isView">
                  </div>
                  <div class="col-12">
                    <label class="form-label text-dark fw-medium mb-1">Ngày Sinh</label>
                    <input type="date" class="form-control bg-light border-0 text-muted" v-model="formData.ngaySinh" :disabled="isView" min="1900-01-01" max="9999-12-31">
                  </div>
                  
                  <div class="col-12 mt-3">
                    <h6 class="fw-bold text-dark border-bottom pb-2 mb-0">Địa chỉ giao hàng</h6>
                  </div>
                  
                  <div class="col-md-4">
                    <label class="form-label text-dark fw-medium mb-1">Tỉnh/Thành phố</label>
                    <select class="form-select bg-light border-0" v-model="formData.tinhThanh" :disabled="isView">
                      <option value="">Chọn Tỉnh/Thành phố</option>
                      <option v-for="t in provinces" :key="t.id" :value="t.full_name">{{ t.full_name }}</option>
                    </select>
                  </div>
                  <div class="col-md-4">
                    <label class="form-label text-dark fw-medium mb-1">Quận/Huyện</label>
                    <select class="form-select bg-light border-0" v-model="formData.quanHuyen" :disabled="!formData.tinhThanh || isView">
                      <option value="">Chọn Quận/Huyện</option>
                      <option v-for="q in districts" :key="q.id" :value="q.full_name">{{ q.full_name }}</option>
                    </select>
                  </div>
                  <div class="col-md-4">
                    <label class="form-label text-dark fw-medium mb-1">Phường/Xã</label>
                    <select class="form-select bg-light border-0" v-model="formData.phuongXa" :disabled="!formData.quanHuyen || isView">
                      <option value="">Chọn Phường/Xã</option>
                      <option v-for="p in wards" :key="p.id" :value="p.full_name">{{ p.full_name }}</option>
                    </select>
                  </div>
                  <div class="col-12">
                    <label class="form-label text-dark fw-medium mb-1">Địa chỉ cụ thể</label>
                    <textarea class="form-control bg-light border-0" v-model="formData.diaChiCuThe" rows="2" placeholder="Số nhà, tên đường..." :disabled="isView"></textarea>
                  </div>
                  
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
                  <button type="button" class="btn px-4 py-2 rounded-pill fw-bold border-0" :class="isView ? 'btn-primary text-white' : 'bg-light text-dark'" @click="closeModal" :style="isView ? 'background-color: #0d6efd;' : 'background-color: #f1f3f5 !important;'">{{ isView ? 'Đóng' : 'Quay lại' }}</button>
                  <button v-if="!isView" type="submit" class="btn btn-primary px-5 py-2 rounded-pill fw-bold shadow-sm" style="background-color: #0d6efd; border-color: #0d6efd;">Lưu thông tin</button>
                </div>
              </form>
            </div>
          </div>
        </div>

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
import Swal from 'sweetalert2';
const Toast = Swal.mixin({ toast: true, position: 'top-end', showConfirmButton: false, timer: 3000, timerProgressBar: true });
import { ref, onMounted } from 'vue'
import { utils, writeFile } from 'xlsx'
import Sidebar from './Sidebar.vue'
import Header from './Header.vue'
import KhachHangAddressModal from './KhachHangAddressModal.vue'
import api from '../services/api' // sử dụng cấu hình axios có sẵn

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
  quanHuyen: '',
  phuongXa: '',
  diaChiCuThe: ''
})

const customers = ref([])
const searchQuery = ref('')

// Dữ liệu địa chỉ
const provinces = ref([])
const districts = ref([])
const wards = ref([])
const allLocations = ref([])

// Fetch địa chỉ từ ESGOO
const fetchLocations = async () => {
  try {
    const res = await api.get('https://esgoo.net/api-tinhthanh/4/0.htm')
    if (res.data.error === 0) {
      allLocations.value = res.data.data
      provinces.value = allLocations.value
    }
  } catch (error) {
    console.error('Lỗi khi tải dữ liệu địa chỉ:', error)
  }
}

// Watchers để cập nhật dropdown Quận/Huyện và Phường/Xã
import { watch } from 'vue'

watch(() => formData.value.tinhThanh, (newVal) => {
  if (newVal) {
    const selectedProv = allLocations.value.find(p => p.full_name === newVal)
    districts.value = selectedProv ? selectedProv.data2 : []
    // Không reset nếu đang load dữ liệu sửa (sẽ xử lý logic reset cẩn thận)
  } else {
    districts.value = []
    wards.value = []
  }
})

watch(() => formData.value.quanHuyen, (newVal) => {
  if (newVal) {
    const selectedDist = districts.value.find(d => d.full_name === newVal)
    wards.value = selectedDist ? selectedDist.data3 : []
  } else {
    wards.value = []
  }
})

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
  fetchLocations()
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


const exportExcel = () => {
  if (customers.value.length === 0) {
    Toast.fire({ icon: 'warning', title: 'Không có dữ liệu để xuất!' })
    return
  }

  // Chuẩn bị dữ liệu xuất
  const dataToExport = customers.value.map((kh, index) => {
    let diachi = ''
    if (kh.diaChiList && kh.diaChiList.length > 0) {
      const dc = getDefaultAddress(kh)
      diachi = [dc.diaChiCuThe, dc.phuongXa, dc.quanHuyen, dc.tinhThanhPho].filter(Boolean).join(', ')
    }
    
    let parsedNgaySinh = '';
    if (kh.ngaySinh) {
      if (Array.isArray(kh.ngaySinh)) {
        parsedNgaySinh = `${String(kh.ngaySinh[2]).padStart(2, '0')}/${String(kh.ngaySinh[1]).padStart(2, '0')}/${kh.ngaySinh[0]}`;
      } else {
        const parts = String(kh.ngaySinh).substring(0, 10).split('-');
        if (parts.length === 3) {
          parsedNgaySinh = `${parts[2]}/${parts[1]}/${parts[0]}`;
        }
      }
    }

    return {
      'STT': index + 1,
      'Mã Khách Hàng': kh.maKH || '',
      'Họ và Tên': kh.ten || '',
      'Số Điện Thoại': kh.sdt || '',
      'Email': kh.email || '',
      'Ngày Sinh': parsedNgaySinh,
      'Giới Tính': kh.gioiTinh === 1 ? 'Nam' : 'Nữ',
      'Địa Chỉ': diachi,
      'Trạng Thái': kh.trangThai === 1 ? 'Hoạt động' : 'Ngừng hoạt động'
    }
  })

  // Tạo worksheet
  const worksheet = utils.json_to_sheet(dataToExport)

  // Auto-size các cột cho đẹp
  const wscols = [
    { wch: 5 }, // STT
    { wch: 20 }, // Mã KH
    { wch: 25 }, // Tên
    { wch: 15 }, // SĐT
    { wch: 25 }, // Email
    { wch: 12 }, // Ngày Sinh
    { wch: 10 }, // Giới Tính
    { wch: 50 }, // Địa Chỉ
    { wch: 15 }  // Trạng Thái
  ];
  worksheet['!cols'] = wscols;

  // Tạo workbook và xuất file
  const workbook = utils.book_new()
  utils.book_append_sheet(workbook, worksheet, "DanhSachKhachHang")
  writeFile(workbook, "DanhSachKhachHang.xlsx")
  
  Toast.fire({ icon: 'success', title: 'Xuất file Excel thành công!' })
}

const isView = ref(false)

const openAddModal = () => {
  isEdit.value = false
  isView.value = false
  formData.value = {
    maKH: '',
    ten: '',
    sdt: '',
    email: '',
    ngaySinh: '',
    gioiTinh: 1,
    trangThai: 1,
    tinhThanh: '',
    quanHuyen: '',
    phuongXa: '',
    diaChiCuThe: ''
  }
  showModal.value = true
}

const openEditModal = (kh) => {
  selectedCustomerForAddress.value = kh;
  showAddressModal.value = true;
}


const populateForm = (kh) => {
  phoneErrorMsg.value = ''
  
  let tinhThanh = ''
  let quanHuyen = ''
  let phuongXa = ''
  let diaChiCuThe = ''

  if (kh.diaChiList && kh.diaChiList.length > 0) {
    const dc = getDefaultAddress(kh) || kh.diaChiList[0]
    tinhThanh = dc.tinhThanhPho || ''
    quanHuyen = dc.quanHuyen || ''
    phuongXa = dc.phuongXa || ''
    diaChiCuThe = dc.diaChiCuThe || ''
  }

  if (tinhThanh) {
    const selectedProv = allLocations.value.find(p => p.full_name === tinhThanh)
    districts.value = selectedProv ? selectedProv.data2 : []
  }
  if (quanHuyen) {
    const selectedDist = districts.value.find(d => d.full_name === quanHuyen)
    wards.value = selectedDist ? selectedDist.data3 : []
  }

  formData.value = {
    id: kh.id,
    maKH: kh.maKhachHang || kh.maKH || '',
    ten: kh.hoTen || kh.ten || '',
    sdt: kh.sdt || '',
    email: kh.email || '',
    ngaySinh: kh.ngaySinh || '',
    gioiTinh: kh.gioiTinh !== undefined ? kh.gioiTinh : 1,
    trangThai: kh.trangThai !== undefined ? kh.trangThai : 1,
    tinhThanh,
    quanHuyen,
    phuongXa,
    diaChiCuThe
  }
}


const toggleStatus = async (kh) => {
  const isActivating = kh.trangThai == 0;
  const actionText = isActivating ? 'kích hoạt' : 'ngừng';
  
  const result = await Swal.fire({
    title: 'Xác nhận',
    text: `Bạn có chắc chắn muốn ${actionText} hoạt động khách hàng này không?`,
    icon: 'question',
    showCancelButton: true,
    confirmButtonColor: '#0d6efd',
    cancelButtonColor: '#6c757d',
    confirmButtonText: 'Đồng ý',
    cancelButtonText: 'Hủy',
    reverseButtons: true
  });

  if (result.isConfirmed) {
    try {
      populateForm(kh);
      const payload = { ...formData.value, trangThai: isActivating ? 1 : 0 };
      await api.put(`/khach-hang/${kh.id}`, payload);
      fetchCustomers();
      Toast.fire({
        icon: 'success',
        title: `Đã ${actionText} hoạt động thành công!`
      });
    } catch (error) {
      console.error(error);
      Swal.fire('Lỗi', 'Không thể thay đổi trạng thái', 'error');
    }
  }
}

const openViewModal = (kh) => {
  populateForm(kh)
  isEdit.value = true
  isView.value = true
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
}

const phoneErrorMsg = ref('')

const saveCustomer = async () => {
  phoneErrorMsg.value = ''
  try {
    if (formData.value.sdt && !/^[0-9]{10,11}$/.test(formData.value.sdt)) {
      phoneErrorMsg.value = 'Số điện thoại không hợp lệ (chỉ gồm 10-11 chữ số).'
      return
    }

    const payload = { ...formData.value }
    payload.maKhachHang = payload.maKH;
    payload.hoTen = payload.ten;
    if (!payload.ngaySinh) {
      payload.ngaySinh = null
    } else {
      if (payload.ngaySinh.length > 10) {
        alert("Vui lòng nhập Ngày Sinh hợp lệ (năm không quá 4 chữ số).")
        return
      }
    }
    
    if (isEdit.value) {
      await api.put(`/khach-hang/${payload.id}`, payload)
    } else {
      await api.post('/khach-hang', payload)
    }
    closeModal()
    fetchCustomers(); Toast.fire({ icon: 'success', title: isEdit.value ? 'Cập nhật khách hàng thành công!' : 'Tạo khách hàng mới thành công!' })
  } catch (error) {
    console.error("Lỗi khi lưu khách hàng:", error)
    if (error.response && error.response.data && error.response.data.message) {
      Toast.fire({ icon: 'error', title: error.response.data.message })
    } else {
      Toast.fire({ icon: 'error', title: 'Có lỗi xảy ra khi lưu khách hàng! Vui lòng kiểm tra lại dữ liệu.' })
    }
  }
}

const deleteCustomer = async (kh) => {
  try {
    await api.delete(`/khach-hang/${kh.id}`)
    fetchCustomers(); Toast.fire({ icon: 'success', title: 'Xóa khách hàng thành công!' })
  } catch (error) {
    console.error("Lỗi khi xóa khách hàng:", error)
    alert("Có lỗi xảy ra khi xóa khách hàng!")
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
  background-color: #0b5ed7 !important;
  border-color: #0a58ca !important;
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
