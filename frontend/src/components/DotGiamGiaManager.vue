<template>
  <div class="d-flex" style="min-height: 100vh; background-color: #f4f7f6;">
    <Sidebar />

    <div class="flex-grow-1 d-flex flex-column">
      <Header />

      <main class="p-4">
        
        <div class="mb-4">
          <h6 class="fw-medium mb-3 d-flex align-items-center text-secondary"><i class="bi bi-funnel me-2 fs-5"></i> Bộ lọc</h6>
          <div class="card border-0 shadow-sm rounded-3">
            <div class="card-body p-3">
              <div class="d-flex gap-3 align-items-center">
                <div class="flex-grow-1 position-relative">
                  <i class="bi bi-search position-absolute top-50 start-0 translate-middle-y ms-3 text-muted"></i>
                  <input type="text" class="form-control ps-5 rounded-2 bg-white border py-2" v-model="searchQuery" placeholder="Tìm theo tên đợt giảm giá, mã đợt...">
                </div>
                <div style="width: 150px;">
                  <select class="form-select rounded-2 bg-white border py-2" v-model="searchStatus">
                  <option value="">Tất cả</option>
                  <option value="1">Đang diễn ra</option>
                  <option value="0">Sắp diễn ra</option>
                  <option value="2">Hết hạn</option>
                </select>
              </div>
              <button class="btn btn-light rounded-3 text-dark fw-medium border py-2" @click="reset"><i class="bi bi-arrow-clockwise me-1"></i> Đặt lại bộ lọc</button>
              <button class="btn btn-light rounded-3 text-dark fw-medium border py-2"><i class="bi bi-file-earmark-excel me-1"></i> Xuất Excel</button>
              <button class="btn btn-primary rounded-3 fw-medium py-2 px-3" @click="openAddModal" style="background-color: #0d6efd; border-color: #0d6efd;"><i class="bi bi-plus-lg me-1"></i> Thêm đợt giảm giá</button>
            </div>
          </div>
        </div>
        </div>

        
        <div class="card rounded-3 shadow-sm bg-white border-0">
          <div class="card-body p-0">
            
            <div class="d-flex align-items-center justify-content-between px-4 pt-4 pb-3">
               <div class="d-flex align-items-center">
                 <div class="bg-primary bg-opacity-10 text-primary rounded-3 d-flex align-items-center justify-content-center me-3" style="width: 40px; height: 40px;">
                   <i class="bi bi-person-lines-fill fs-5"></i>
                 </div>
                 <h5 class="fw-bold m-0 text-dark">Danh sách đợt giảm giá</h5>
               </div>
            </div>

            
            <div class="table-responsive px-4 pb-3">
              <table class="table align-middle m-0 text-start table-hover border-bottom">
                <thead style="background-color: #f8f9fa;">
                  <tr>
                    <th class="py-3 fw-bold text-dark border-0 text-center" style="width: 50px;">STT</th>
                    <th class="py-3 fw-bold text-dark border-0">Mã đợt</th>
                    <th class="py-3 fw-bold text-dark border-0">Tên đợt giảm giá</th>
                    <th class="py-3 fw-bold text-dark border-0">Phần trăm giảm</th>
                    <th class="py-3 fw-bold text-dark border-0">Ngày bắt đầu</th>
                    <th class="py-3 fw-bold text-dark border-0">Ngày kết thúc</th>
                    <th class="py-3 fw-bold text-dark border-0 text-center">Trạng thái</th>
                    <th class="py-3 fw-bold text-dark border-0 text-center">Hành động</th>
                  </tr>
                </thead>
                <tbody class="text-dark">
                  <tr v-for="(dgg, index) in filteredList" :key="dgg.id">
                    <td class="py-4 text-center border-bottom-0">{{ index + 1 }}</td>
                    <td class="py-4 border-bottom-0">{{ dgg.maDot || 'Đợt ưu đãi...' }}</td>
                    <td class="py-4 border-bottom-0">{{ dgg.tenDot }}</td>
                    <td class="py-4 border-bottom-0">{{ dgg.phanTramGiamDot }}%</td>
                    <td class="py-4 border-bottom-0">{{ formatDateOnly(dgg.ngayBatDau) }}</td>
                    <td class="py-4 border-bottom-0">{{ formatDateOnly(dgg.ngayKetThuc) }}</td>
                    <td class="py-4 text-center border-bottom-0">
                      <span class="badge rounded-pill" :class="statusBadgeClass(dgg.trangThai)" style="padding: 0.5rem 1rem; font-weight: 500;">
                        {{ displayStatus(dgg.trangThai) }}
                      </span>
                    </td>
                    <td class="py-4 text-center border-bottom-0">
                      <div class="d-flex justify-content-center gap-2">
                        <a href="#" class="bg-light rounded p-2 text-success d-inline-flex align-items-center justify-content-center" style="width: 32px; height: 32px;" @click.prevent="openViewModal(dgg)">
                          <i class="bi bi-eye"></i>
                        </a>
                        <a href="#" class="bg-light rounded p-2 text-secondary d-inline-flex align-items-center justify-content-center" style="width: 32px; height: 32px;" @click.prevent="openEditModal(dgg)">
                          <i class="bi bi-pencil-fill"></i>
                        </a>
                        <a href="#" class="bg-danger bg-opacity-10 rounded p-2 text-danger d-inline-flex align-items-center justify-content-center" style="width: 32px; height: 32px;" @click.prevent="deleteDGG(dgg)">
                          <i class="bi bi-trash-fill"></i>
                        </a>
                      </div>
                    </td>
                  </tr>
                  <tr v-if="filteredList.length === 0">
                    <td colspan="8" class="text-center py-5 text-muted">Không có dữ liệu đợt giảm giá</td>
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

      
        <!-- Modal Thêm/Sửa Đợt Giảm Giá -->
        <div v-if="showModal" style="position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; background: rgba(0,0,0,0.4); backdrop-filter: blur(4px); z-index: 1050; display: flex; align-items: center; justify-content: center;">
          <div class="modal-dialog bg-white rounded-3 shadow" style="width: 500px; max-width: 90vw; pointer-events: auto;">
            <div class="modal-header d-flex justify-content-between align-items-center p-3 border-bottom">
              <h5 class="modal-title fw-bold text-primary m-0">{{ isView ? 'Chi Tiết Đợt Giảm Giá' : (isEdit ? 'Cập Nhật Đợt Giảm Giá' : 'Thêm Mới Đợt Giảm Giá') }}</h5>
              <button type="button" class="btn-close" @click="closeModal"></button>
            </div>
            <div class="modal-body p-4">
              <form @submit.prevent="saveDGG">
                <div class="mb-3">
                  <label class="form-label small text-dark fw-medium">Mã Đợt</label>
                  <input type="text" class="form-control" v-model="formData.maDot" :disabled="isEdit || isView" placeholder="Để trống để tự động tạo">
                </div>
                <div class="mb-3">
                  <label class="form-label small text-dark fw-medium">Tên Đợt Giảm Giá</label>
                  <input type="text" class="form-control" v-model="formData.tenDot" :disabled="isView" required>
                </div>
                <div class="mb-3">
                  <label class="form-label small text-dark fw-medium">Phần Trăm Giảm</label>
                  <input type="text" class="form-control" v-model="formData.phanTramGiamDot" :disabled="isView" required placeholder="vd: 20%">
                </div>
                <div class="row">
                  <div class="col-6 mb-3">
                    <label class="form-label small text-dark fw-medium">Ngày bắt đầu</label>
                    <input type="date" class="form-control" v-model="formData.ngayBatDau" :disabled="isView" required>
                  </div>
                  <div class="col-6 mb-3">
                    <label class="form-label small text-dark fw-medium">Ngày kết thúc</label>
                    <input type="date" class="form-control" v-model="formData.ngayKetThuc" :disabled="isView" required>
                  </div>
                </div>
                <div class="mb-3">
                  <label class="form-label small text-dark fw-medium">Trạng Thái</label>
                  <select class="form-select" v-model="formData.trangThai" :disabled="isView">
                    <option :value="0">Sắp diễn ra</option>
                    <option :value="1">Đang diễn ra</option>
                    <option :value="2">Hết hạn</option>
                  </select>
                </div>
                <div class="d-flex justify-content-end gap-2 mt-4">
                  <button type="button" :class="isView ? 'btn btn-primary px-4' : 'btn btn-secondary px-4'" @click="closeModal">{{ isView ? 'Đóng' : 'Hủy' }}</button>
                  <button v-if="!isView" type="submit" class="btn btn-primary px-4">Lưu</button>
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
import { ref, onMounted, computed } from 'vue'
import Sidebar from './Sidebar.vue'
import Header from './Header.vue'
import api from '../services/api'

const showModal = ref(false)
const isEdit = ref(false)
const isView = ref(false)

const formData = ref({
  maDot: '',
  tenDot: '',
  phanTramGiamDot: '',
  ngayBatDau: '',
  ngayKetThuc: '',
  trangThai: 1
})

const dsDotGiamGia = ref([])
const searchQuery = ref('')
const searchStartDate = ref('')
const searchEndDate = ref('')
const searchStatus = ref('') // 'Tất Cả' by default? Wait, I will use empty string

// Fetch list from API
const fetchDGG = async () => {
  try {
    const res = await api.get('/dot-giam-gia')
    dsDotGiamGia.value = res.data
  } catch (error) {
    console.error("Lỗi khi tải danh sách đợt giảm giá:", error)
  }
}

onMounted(() => {
  fetchDGG()
})

const openAddModal = () => {
  isEdit.value = false
  isView.value = false
  formData.value = {
    maDot: '',
    tenDot: '',
    phanTramGiamDot: '',
    ngayBatDau: '',
    ngayKetThuc: '',
    trangThai: 1
  }
  showModal.value = true
}

const openEditModal = (dgg) => {
  isEdit.value = true
  isView.value = false
  formData.value = { ...dgg }
  if (formData.value.phanTramGiamDot) formData.value.phanTramGiamDot = formData.value.phanTramGiamDot + '%';
  if (formData.value.ngayBatDau && formData.value.ngayBatDau.length > 10) formData.value.ngayBatDau = formData.value.ngayBatDau.substring(0, 10);
  if (formData.value.ngayKetThuc && formData.value.ngayKetThuc.length > 10) formData.value.ngayKetThuc = formData.value.ngayKetThuc.substring(0, 10);
  showModal.value = true
}

const openViewModal = (dgg) => {
  isEdit.value = false
  isView.value = true
  formData.value = { ...dgg }
  if (formData.value.phanTramGiamDot) formData.value.phanTramGiamDot = formData.value.phanTramGiamDot + '%';
  if (formData.value.ngayBatDau && formData.value.ngayBatDau.length > 10) formData.value.ngayBatDau = formData.value.ngayBatDau.substring(0, 10);
  if (formData.value.ngayKetThuc && formData.value.ngayKetThuc.length > 10) formData.value.ngayKetThuc = formData.value.ngayKetThuc.substring(0, 10);
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
}

const saveDGG = async () => {
  try {
    const payload = { ...formData.value }

    // Parse the string into an integer (e.g. "20%" -> 20)
    let parsedPercent = parseInt(String(payload.phanTramGiamDot).replace(/\D/g, ''), 10);
    
    if (isNaN(parsedPercent) || parsedPercent < 1 || parsedPercent > 100) {
      alert("Phần trăm giảm giá phải nằm trong khoảng từ 1% đến 100%!");
      return;
    }
    payload.phanTramGiamDot = parsedPercent;

    // Ensure the backend receives LocalDateTime format by appending time if missing
    if (payload.ngayBatDau && payload.ngayBatDau.length === 10) {
      payload.ngayBatDau = payload.ngayBatDau + 'T00:00:00';
    }
    if (payload.ngayKetThuc && payload.ngayKetThuc.length === 10) {
      payload.ngayKetThuc = payload.ngayKetThuc + 'T23:59:59';
    }

    if (isEdit.value) {
      await api.put(`/dot-giam-gia/${payload.id}`, payload)
    } else {
      await api.post('/dot-giam-gia', payload)
    }
    closeModal()
    fetchDGG()
  } catch (error) {
    console.error("Lỗi khi lưu đợt giảm giá:", error)
    alert("Có lỗi xảy ra khi lưu đợt giảm giá!")
  }
}

const deleteDGG = async (dgg) => {
  if (confirm('Bạn có chắc chắn muốn xóa đợt giảm giá này?')) {
    try {
      await api.delete(`/dot-giam-gia/${dgg.id}`)
      fetchDGG()
    } catch (error) {
      console.error("Lỗi khi xóa đợt giảm giá:", error)
      alert("Có lỗi xảy ra khi xóa đợt giảm giá!")
    }
  }
}

const formatDateOnly = (dateTimeString) => {
  if (!dateTimeString) return ''
  const date = new Date(dateTimeString)
  if (isNaN(date.getTime())) return dateTimeString
  const d = date.getDate().toString().padStart(2, '0')
  const m = (date.getMonth() + 1).toString().padStart(2, '0')
  const y = date.getFullYear()
  return `${d}/${m}/${y}`
}

const formatDateTime = (dateTimeString) => {
  if (!dateTimeString) return ''
  const date = new Date(dateTimeString)
  if (isNaN(date.getTime())) return dateTimeString
  const d = date.getDate().toString().padStart(2, '0')
  const m = (date.getMonth() + 1).toString().padStart(2, '0')
  const y = date.getFullYear()
  const hh = date.getHours().toString().padStart(2, '0')
  const mm = date.getMinutes().toString().padStart(2, '0')
  const ss = date.getSeconds().toString().padStart(2, '0')
  return `${d}/${m}/${y}<br>${hh}:${mm}:${ss}`
}

const displayStatus = (statusValue) => {
  if (statusValue === 0) return 'Sắp diễn ra';
  if (statusValue === 1) return 'Đang diễn ra';
  if (statusValue === 2) return 'Hết hạn';
  return 'Không rõ';
}

const statusBadgeClass = (statusValue) => {
  if (statusValue === 1) return 'bg-success bg-opacity-10 text-success';
  if (statusValue === 0) return 'bg-warning bg-opacity-10 text-warning';
  return 'bg-warning bg-opacity-10 text-warning'; // "Hết hạn" in design is also orange/yellow
}

// Local Search logic (can be replaced with API search later)
const search = () => {
  // Let's just rely on computed filteredList or fetchDGG? 
  // The user asked for "reset", let's do a basic reset
}

const reset = () => {
  searchQuery.value = ''
  searchStartDate.value = ''
  searchEndDate.value = ''
  searchStatus.value = ''
  fetchDGG()
}

const filteredList = computed(() => {
  return dsDotGiamGia.value.filter(dgg => {
    let match = true;
    if (searchQuery.value && !dgg.tenDot?.toLowerCase().includes(searchQuery.value.toLowerCase()) && !dgg.maDot?.toLowerCase().includes(searchQuery.value.toLowerCase())) match = false;
    if (searchStatus.value !== '' && String(dgg.trangThai) !== searchStatus.value) match = false;
    
    // basic date logic
    if (searchStartDate.value && dgg.ngayBatDau) {
      if (dgg.ngayBatDau.substring(0, 10) < searchStartDate.value) match = false;
    }
    if (searchEndDate.value && dgg.ngayKetThuc) {
      if (dgg.ngayKetThuc.substring(0, 10) > searchEndDate.value) match = false;
    }
    
    return match;
  })
})

</script>

<style scoped>
/* Form Input */
.form-control::placeholder {
  color: #ced4da;
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
