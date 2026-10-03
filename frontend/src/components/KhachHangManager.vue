<template>
  <div class="d-flex" style="min-height: 100vh; background-color: #f4f7f6;">
    <Sidebar />

    <div class="flex-grow-1 d-flex flex-column">
      <Header />

      <main class="p-4">
        
        
        <div class="mb-4">
          <div class="d-flex align-items-center mb-3">
            <div class="bg-light rounded-circle d-flex align-items-center justify-content-center me-2" style="width: 35px; height: 35px;">
              <i class="bi bi-funnel text-muted"></i>
            </div>
            <span class="fw-bold text-muted fs-6">Bộ lọc</span>
          </div>
          
          <div class="bg-white rounded-3 shadow-sm p-3 d-flex flex-wrap align-items-center gap-3 border">
            
            <div class="input-group" style="flex: 1; min-width: 350px;">
              <span class="input-group-text border-end-0 text-muted rounded-start-pill bg-light" style="padding-left: 1rem;"><i class="bi bi-search"></i></span>
              <input type="text" class="form-control border-start-0 rounded-end-pill bg-light ps-0 text-muted" placeholder="Tìm theo tên đăng nhập, họ tên, email, SĐT..." v-model="searchQuery" @keyup.enter="search">
            </div>
            
            <select class="form-select bg-light text-secondary rounded-pill" style="width: 150px;">
              <option selected value="">Tất cả</option>
              <option value="1">Hoạt động</option>
              <option value="0">Ngừng hoạt động</option>
            </select>
            
            <button class="btn btn-outline-secondary d-flex align-items-center rounded-pill fw-medium custom-outline-btn text-muted" @click="reset">
              <i class="bi bi-arrow-counterclockwise me-2"></i> Đặt lại bộ lọc
            </button>
            
            <button class="btn btn-outline-secondary d-flex align-items-center rounded-pill fw-medium custom-outline-btn text-muted">
              <i class="bi bi-file-earmark-excel me-2"></i> Xuất Excel
            </button>
            
            <router-link to="/khach-hang/them" class="btn btn-primary d-flex align-items-center rounded-pill fw-medium shadow-sm custom-solid-btn text-decoration-none text-white" style="background-color: #0d6efd; border-color: #0d6efd;">
              <i class="bi bi-plus-lg me-2"></i> Thêm khách hàng
            </router-link>
            
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
                    <td colspan="7" class="text-center py-5 text-muted">
                      Không có dữ liệu
                    </td>
                  </tr>
                  <tr v-for="(kh, index) in customers" :key="kh.id">
                    <td class="py-3" style="border-bottom: 1px solid #f0f0f0;">{{ index + 1 }}</td>
                    <td class="py-3" style="border-bottom: 1px solid #f0f0f0;">{{ kh.ten }}</td>
                    <td class="py-3" style="border-bottom: 1px solid #f0f0f0;">{{ kh.email }}</td>
                    <td class="py-3 small" style="border-bottom: 1px solid #f0f0f0; white-space: normal;">
                      <span v-if="kh.diaChiList && kh.diaChiList.length > 0">
                        {{ kh.diaChiList[0].diaChiCuThe ? kh.diaChiList[0].diaChiCuThe + ', ' : '' }}{{ kh.diaChiList[0].phuongXa }}, {{ kh.diaChiList[0].quanHuyen }}, {{ kh.diaChiList[0].tinhThanhPho }}
                      </span>
                      <span v-else>
                        Chưa cập nhật địa chỉ
                      </span>
                    </td>
                    <td class="py-3 text-muted" style="border-bottom: 1px solid #f0f0f0;">{{ kh.sdt }}</td>
                    <td class="py-3 text-center" style="border-bottom: 1px solid #f0f0f0;">
                      <span class="badge rounded-pill fw-medium px-3 py-2" 
                            :class="kh.trangThai === 1 ? 'bg-success bg-opacity-10 text-success' : 'bg-secondary bg-opacity-10 text-secondary'" 
                            style="border: 1px solid; border-color: rgba(25, 135, 84, 0.2)">
                        {{ kh.trangThai === 1 ? 'Hoạt động' : 'Ngừng hoạt động' }}
                      </span>
                    </td>
                    <td class="py-3 text-center" style="border-bottom: 1px solid #f0f0f0;">
                      <div class="d-flex justify-content-center gap-2">
                        <button class="btn btn-sm btn-light text-success rounded-circle action-btn" @click.prevent="openViewModal(kh)">
                          <i class="bi bi-eye"></i>
                        </button>
                        <button class="btn btn-sm btn-light text-secondary rounded-circle action-btn" @click.prevent="openEditModal(kh)">
                          <i class="bi bi-pencil-fill"></i>
                        </button>
                        <button class="btn btn-sm btn-light text-danger rounded-circle action-btn" @click.prevent="deleteCustomer(kh)">
                          <i class="bi bi-trash-fill"></i>
                        </button>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

            
            <div class="d-flex justify-content-end px-4 py-4 mt-2">
              <nav>
                <ul class="pagination custom-pagination m-0 gap-1 border-0">
                  <li class="page-item disabled">
                    <a class="page-link border-0 text-muted bg-transparent" href="#"><i class="bi bi-chevron-double-left fs-6"></i></a>
                  </li>
                  <li class="page-item disabled">
                    <a class="page-link border-0 text-muted bg-transparent" href="#"><i class="bi bi-chevron-left fs-6"></i></a>
                  </li>
                  <li class="page-item active">
                    <a class="page-link border-0 rounded-3 bg-primary bg-opacity-10 text-primary fw-bold" href="#">1</a>
                  </li>
                  <li class="page-item">
                    <a class="page-link border-0 text-muted bg-transparent fw-medium" href="#">2</a>
                  </li>
                  <li class="page-item">
                    <a class="page-link border-0 text-muted bg-transparent fw-medium" href="#">3</a>
                  </li>
                  <li class="page-item">
                    <a class="page-link border-0 text-muted bg-transparent" href="#"><i class="bi bi-chevron-right fs-6"></i></a>
                  </li>
                  <li class="page-item">
                    <a class="page-link border-0 text-muted bg-transparent" href="#"><i class="bi bi-chevron-double-right fs-6"></i></a>
                  </li>
                </ul>
              </nav>
            </div>

          </div>
        </div>

        
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
                    <label class="form-label text-dark fw-medium mb-1">Ngày Sinh</label>
                    <input type="date" class="form-control bg-light border-0 text-muted" v-model="formData.ngaySinh" :disabled="isView">
                  </div>
                  <div class="col-12">
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
                  <button type="button" class="btn px-4 py-2 rounded-pill fw-bold border-0" :class="isView ? 'btn-primary text-white' : 'bg-light text-dark'" @click="closeModal" :style="isView ? 'background-color: #0d6efd;' : 'background-color: #f1f3f5 !important;'">{{ isView ? 'Đóng' : 'Hủy bỏ' }}</button>
                  <button v-if="!isView" type="submit" class="btn btn-primary px-5 py-2 rounded-pill fw-bold shadow-sm" style="background-color: #0d6efd; border-color: #0d6efd;">Lưu thông tin</button>
                </div>
              </form>
            </div>
          </div>
        </div>

      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import Sidebar from './Sidebar.vue'
import Header from './Header.vue'
import api from '../services/api' 

const showModal = ref(false)
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


const provinces = ref([])
const districts = ref([])
const wards = ref([])
const allLocations = ref([])


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


import { watch } from 'vue'

watch(() => formData.value.tinhThanh, (newVal) => {
  if (newVal) {
    const selectedProv = allLocations.value.find(p => p.full_name === newVal)
    districts.value = selectedProv ? selectedProv.data2 : []
    
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

const fetchCustomers = async () => {
  try {
    const response = await api.get('/khach-hang', {
      params: { search: searchQuery.value }
    })
    customers.value = response.data
  } catch (error) {
    console.error("Lỗi khi tải danh sách khách hàng:", error)
  }
}

onMounted(() => {
  fetchCustomers()
  fetchLocations()
})

const search = () => {
  fetchCustomers()
}

const reset = () => {
  searchQuery.value = ''
  fetchCustomers()
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
  isEdit.value = true
  isView.value = false
  phoneErrorMsg.value = ''
  
  let tinhThanh = ''
  let quanHuyen = ''
  let phuongXa = ''
  let diaChiCuThe = ''

  if (kh.diaChiList && kh.diaChiList.length > 0) {
    const dc = kh.diaChiList[0]
    tinhThanh = dc.tinhThanhPho || ''
    quanHuyen = dc.quanHuyen || ''
    phuongXa = dc.phuongXa || ''
    diaChiCuThe = dc.diaChiCuThe || ''
  }

  formData.value = { 
    ...kh,
    tinhThanh: tinhThanh,
    quanHuyen: quanHuyen,
    phuongXa: phuongXa,
    diaChiCuThe: diaChiCuThe
  }
  showModal.value = true
}

const openViewModal = (kh) => {
  openEditModal(kh)
  isView.value = true
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
    fetchCustomers()
  } catch (error) {
    console.error("Lỗi khi lưu khách hàng:", error)
    alert("Có lỗi xảy ra khi lưu khách hàng! Vui lòng kiểm tra lại dữ liệu.")
  }
}

const toggleStatus = async (kh) => {
  try {
    kh.trangThai = kh.trangThai === 1 ? 0 : 1;
    await api.put(`/khach-hang/${kh.id}`, kh)
  } catch (error) {
    console.error(error)
    kh.trangThai = kh.trangThai === 1 ? 0 : 1; 
  }
}

const deleteCustomer = async (kh) => {
  try {
    await api.delete(`/khach-hang/${kh.id}`)
    fetchCustomers()
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

.form-control::placeholder {
  color: #ced4da;
}


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
