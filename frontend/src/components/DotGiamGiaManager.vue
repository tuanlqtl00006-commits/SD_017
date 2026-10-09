<template>
  <div>

        
        <!-- Bộ lọc Card -->
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
                <input type="text" class="form-control border-start-0 rounded-end-pill bg-light ps-0 text-muted" placeholder="Mã, tên, giá trị..." v-model="searchText" @keyup.enter="search">
              </div>

              <input type="date" min="2000-01-01" max="2099-12-31" class="form-control bg-light text-secondary rounded-pill" style="width: 150px;" v-model="searchStartDate" title="Ngày bắt đầu" data-no-auto-picker>
              <input type="date" min="2000-01-01" max="2099-12-31" class="form-control bg-light text-secondary rounded-pill" style="width: 150px;" v-model="searchEndDate" title="Ngày kết thúc" data-no-auto-picker>
              
              <select class="form-select bg-light text-secondary rounded-pill" style="width: 150px;" v-model="searchStatus">
                <option value="">Tất cả</option>
                <option value="1">Đang diễn ra</option>
                <option value="2">Sắp diễn ra</option>
                <option value="0">Đã kết thúc</option>
              </select>
              
              <button class="btn btn-outline-secondary d-flex align-items-center rounded-pill fw-medium custom-outline-btn text-muted" @click="reset">
                <i class="bi bi-arrow-counterclockwise me-2"></i> Đặt lại bộ lọc
              </button>
              
              <button type="button" class="btn btn-outline-secondary d-flex align-items-center rounded-pill fw-medium custom-outline-btn text-muted" @click="exportToExcel">
                <i class="bi bi-file-earmark-excel me-2"></i> Xuất Excel
              </button>
              
              <button class="btn btn-primary d-flex align-items-center rounded-pill fw-medium shadow-sm custom-solid-btn text-decoration-none text-white" style="background-color: #0d6efd; border-color: #0d6efd;" @click="openAddModal">
                <i class="bi bi-plus-lg me-2"></i> Tạo đợt giảm giá
              </button>
            </div>
          </div>
        </div>

        <!-- Danh sách Card -->
        <div class="card border-0 shadow-sm rounded-4">
          <div class="card-body p-0">
            <div class="d-flex justify-content-between align-items-center p-4 border-bottom">
              <h5 class="fw-bold mb-0 text-dark"><i class="bi bi-tags-fill text-primary me-2"></i>Danh sách các đợt giảm giá</h5>
            </div>


            <div class="table-responsive ad-grid-table mx-4 mt-4">
              <table class="table table-hover align-middle mb-0 custom-table">
                <thead class="bg-light text-secondary">
                  <tr>
                    <th class="ps-4 fw-bold text-dark border-0 py-3">STT</th>
                    <th class="fw-bold text-dark border-0 py-3">Mã đợt</th>
                    <th class="fw-bold text-dark border-0 py-3">Tên đợt giảm giá</th>
                    <th class="fw-bold text-dark border-0 py-3 text-center">Phần trăm giảm</th>
                    <th class="fw-bold text-dark border-0 py-3 text-center">Ngày bắt đầu</th>
                    <th class="fw-bold text-dark border-0 py-3 text-center">Ngày kết thúc</th>
                    <th class="fw-bold text-dark border-0 py-3 text-center">Trạng thái</th>
                    <th class="fw-bold text-dark border-0 py-3 text-center pe-4">Hành động</th>
                  </tr>
                </thead>
                <tbody class="border-top-0">
                  <tr v-for="(dgg, index) in paginatedList" :key="dgg.id">
                    <td class="ps-4 text-muted">{{ index + 1 + (currentPage - 1) * itemsPerPage }}</td>
                    <td class="fw-medium text-dark">{{ dgg.maDot }}</td>
                    <td>{{ dgg.tenDot }}</td>
                    <td class="text-center">{{ dgg.phanTramGiamDot }}%</td>
                    <td class="text-center">{{ formatDateOnly(dgg.ngayBatDau) }}</td>
                    <td class="text-center">{{ formatDateOnly(dgg.ngayKetThuc) }}</td>
                    <td class="text-center">
                      <span class="badge rounded-pill px-3 py-2 fw-medium" :class="statusBadgeClass(dgg.trangThai)">
                        {{ displayStatus(dgg.trangThai) }}
                      </span>
                    </td>
                    <td class="text-center pe-4">
                      <div class="d-flex justify-content-center gap-2">
                        <button class="btn btn-sm btn-light text-primary rounded-circle d-flex align-items-center justify-content-center" 
                                style="width: 32px; height: 32px; background-color: transparent; border: none;" 
                                title="Sửa" 
                                @click="openEditModal(dgg)">
                          <i class="bi bi-pencil"></i>
                        </button>
                        <button class="btn btn-sm btn-light text-secondary rounded-circle d-flex align-items-center justify-content-center" 
                                style="width: 32px; height: 32px; background-color: transparent; border: none;" 
                                title="Xem" 
                                @click="openViewModal(dgg)">
                          <i class="bi bi-eye"></i>
                        </button>
                      </div>
                    </td>
                  </tr>
                  <tr v-if="filteredList.length === 0">
                    <td colspan="8" class="text-center py-5 text-muted">Không có dữ liệu đợt giảm giá</td>
                  </tr>
                </tbody>
              </table>
            </div>

                        <!-- Pagination -->
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
                       :class="currentPage === page ? 'bg-primary text-white shadow-sm rounded-circle' : 'text-dark bg-transparent hover-bg-light rounded-circle'" 
                       href="#" @click.prevent="goToPage(page)" style="width: 35px; height: 35px; display: flex; align-items: center; justify-content: center;">
                      {{ page }}
                    </a>
                  </li>
                  
                  <li class="page-item" :class="{ disabled: currentPage === totalPages || totalPages === 0 }">
                    <a class="page-link border-0 text-muted bg-transparent" href="#" @click.prevent="nextPage"><i class="bi bi-chevron-right fs-6"></i></a>
                  </li>
                  <li class="page-item" :class="{ disabled: currentPage === totalPages || totalPages === 0 }">
                    <a class="page-link border-0 text-muted bg-transparent" href="#" @click.prevent="goToPage(totalPages)"><i class="bi bi-chevron-double-right fs-6"></i></a>
                  </li>
                </ul>
              </nav>
            </div>
          </div>
        </div>

        <!-- ADD/EDIT MODAL -->
        <div class="modal fade show d-block" tabindex="-1" v-if="showModal" style="background-color: rgba(0,0,0,0.5);">
          <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content border-0 shadow-lg rounded-4">
              <div class="modal-header border-bottom-0 pb-0">
                <h5 class="modal-title fw-bold text-dark">{{ isEdit ? 'Cập Nhật Đợt Giảm Giá' : (isView ? 'Chi Tiết Đợt Giảm Giá' : 'Thêm Mới Đợt Giảm Giá') }}</h5>
                <button type="button" class="btn-close" @click="closeModal"></button>
              </div>
              <div class="modal-body p-4">
                <form @submit.prevent="saveDGG">
                  <div class="mb-3">
                    <label class="form-label small text-dark fw-medium">Mã Đợt</label>
                    <input type="text" class="form-control rounded-3" v-model="formData.maDot" placeholder="Gõ mã tuỳ ý (hoặc để trống hệ thống sẽ tự sinh DGG001)" :disabled="isView || isEdit">
                  </div>
                  <div class="mb-3">
                    <label class="form-label small text-dark fw-medium">Tên Đợt Giảm Giá</label>
                    <input type="text" class="form-control rounded-3" v-model="formData.tenDot" :disabled="isView" required>
                  </div>
                  <div class="mb-3">
                    <label class="form-label small text-dark fw-medium">Phần Trăm Giảm</label>
                    <input type="text" class="form-control rounded-3" v-model="formData.phanTramGiamDot" placeholder="vd: 20%" :disabled="isView" required>
                    <div v-if="percentError" class="text-danger small mt-1">{{ percentError }}</div>

                  </div>
                  <div class="row">
                    <div class="col-6 mb-3">
                      <label class="form-label small text-dark fw-medium">Ngày bắt đầu</label>
                      <input type="date" min="2000-01-01" max="2099-12-31" class="form-control rounded-3" v-model="formData.ngayBatDau" :disabled="isView" required data-no-auto-picker>
                    </div>
                    <div class="col-6 mb-3">
                      <label class="form-label small text-dark fw-medium">Ngày kết thúc</label>
                      <input type="date" min="2000-01-01" max="2099-12-31" class="form-control rounded-3" v-model="formData.ngayKetThuc" :disabled="isView" required data-no-auto-picker>
                    </div>
                  </div>
                  <div class="mb-3">
                    <label class="form-label small text-dark fw-medium">Trạng Thái</label>
                    <input type="text" class="form-control rounded-3 text-muted" value="Hệ thống tự động cập nhật theo ngày" disabled>
                  </div>
                  <div class="d-flex justify-content-end gap-2 mt-4">
                    <button type="button" class="btn btn-secondary rounded-3 px-4 fw-medium" @click="closeModal">Hủy</button>
                    <button v-if="!isView" type="submit" class="btn btn-primary rounded-3 px-4 fw-medium" style="background-color: #0d6efd; border-color: #0d6efd;">Lưu</button>
                  </div>
                </form>
              </div>
            </div>
          </div>
        </div>
        
        <!-- Modal Chi Tiet San Pham -->
        <DotGiamGiaChiTietModal 
          v-if="showChiTietModal" 
          :campaign="selectedCampaign" 
          @close="showChiTietModal = false" 
        />
      
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import Sidebar from './Sidebar.vue'
import Header from './Header.vue'
import api from '../services/api'
import * as XLSX from 'xlsx'
import Swal from 'sweetalert2'

const Toast = Swal.mixin({
  toast: true,
  position: 'top-end',
  showConfirmButton: false,
  timer: 3000,
  timerProgressBar: true
})
import DotGiamGiaChiTietModal from './DotGiamGiaChiTietModal.vue'

const currentPage = ref(1)
const itemsPerPage = 5

const totalPages = computed(() => Math.ceil(filteredList.value.length / itemsPerPage) || 1)

const paginatedList = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage
  const end = start + itemsPerPage
  return filteredList.value.slice(start, end)
})

const prevPage = () => { if (currentPage.value > 1) currentPage.value-- }
const nextPage = () => { if (currentPage.value < totalPages.value) currentPage.value++ }
const goToPage = (page) => { currentPage.value = page }

const showModal = ref(false)
const isEdit = ref(false)
const isView = ref(false)

const showChiTietModal = ref(false)
const selectedCampaign = ref(null)

const openChiTietModal = (dgg) => {
  selectedCampaign.value = dgg;
  showChiTietModal.value = true;
}

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
const searchText = ref('')
const searchStartDate = ref('')
const searchEndDate = ref('')
const searchStatus = ref('')

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

const percentError = ref('')



const toggleStatus = async (item) => {
  const isCurrentlyActive = item.trangThai === 1;
  const actionText = isCurrentlyActive ? 'ngừng hoạt động' : 'bật hoạt động';
  
  Swal.fire({
    title: 'Xác nhận',
    text: `Bạn có chắc chắn muốn ${actionText} đợt giảm giá này không?`,
    icon: 'question',
    showCancelButton: true,
    confirmButtonColor: '#0d6efd',
    cancelButtonColor: '#6c757d',
    confirmButtonText: 'Đồng ý',
    cancelButtonText: 'Hủy',
    reverseButtons: true
  }).then(async (result) => {
    if (result.isConfirmed) {
      try {
        const newStatus = isCurrentlyActive ? 2 : 1;
        const payload = { ...item, trangThai: newStatus };
        
        // Convert array dates to string for backend parser
        if (Array.isArray(payload.ngayBatDau)) {
          payload.ngayBatDau = `${payload.ngayBatDau[0]}-${String(payload.ngayBatDau[1]).padStart(2, '0')}-${String(payload.ngayBatDau[2]).padStart(2, '0')}`;
        }
        if (Array.isArray(payload.ngayKetThuc)) {
          payload.ngayKetThuc = `${payload.ngayKetThuc[0]}-${String(payload.ngayKetThuc[1]).padStart(2, '0')}-${String(payload.ngayKetThuc[2]).padStart(2, '0')}`;
        }
        
        await api.put(`/dot-giam-gia/${item.id}`, payload);
        Toast.fire({ icon: 'success', title: 'Đổi trạng thái thành công!' });
        fetchDGG();
      } catch (error) {
        console.error(error);
        const errorMsg = error.response?.data?.message || error.response?.data?.error || 'Lỗi khi đổi trạng thái!';
        Toast.fire({ icon: 'error', title: errorMsg });
      }
    }
  });
};



const exportToExcel = () => {
  if (filteredList.value.length === 0) {
    Toast.fire({ icon: 'warning', title: 'Không có dữ liệu để xuất!' })
    return;
  }
  
  const dataToExport = filteredList.value.map((item, index) => {
    // Format dates to DD/MM/YYYY
    let parsedNgayBatDau = '';
    if (item.ngayBatDau) {
      const parts = String(item.ngayBatDau).substring(0, 10).split('-');
      if (parts.length === 3) parsedNgayBatDau = `${parts[2]}/${parts[1]}/${parts[0]}`;
    }
    let parsedNgayKetThuc = '';
    if (item.ngayKetThuc) {
      const parts = String(item.ngayKetThuc).substring(0, 10).split('-');
      if (parts.length === 3) parsedNgayKetThuc = `${parts[2]}/${parts[1]}/${parts[0]}`;
    }

    return {
      'STT': index + 1,
      'Mã Đợt': item.maDot,
      'Tên Đợt Giảm Giá': item.tenDot,
      'Phần Trăm Giảm': item.phanTramGiamDot + '%',
      'Ngày Bắt Đầu': parsedNgayBatDau,
      'Ngày Kết Thúc': parsedNgayKetThuc,
      'Trạng Thái': displayStatus(item.trangThai)
    }
  });
  
  const worksheet = XLSX.utils.json_to_sheet(dataToExport);
  
  // Auto-size columns
  const wscols = [
    { wch: 5 },  // STT
    { wch: 20 }, // Mã Đợt
    { wch: 30 }, // Tên Đợt
    { wch: 20 }, // Phần Trăm Giảm
    { wch: 15 }, // Ngày Bắt Đầu
    { wch: 15 }, // Ngày Kết Thúc
    { wch: 20 }  // Trạng Thái
  ];
  worksheet['!cols'] = wscols;

  const workbook = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(workbook, worksheet, "DanhSachDotGiamGia");
  XLSX.writeFile(workbook, "Danh_Sach_Dot_Giam_Gia.xlsx");
  
  Toast.fire({ icon: 'success', title: 'Xuất file Excel thành công!' })
};

const openAddModal = () => {
  isEdit.value = false
  isView.value = false
  percentError.value = ''
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

const openViewModal = (dgg) => { isEdit.value = false; isView.value = true; percentError.value = ""; formData.value = { ...dgg }; if (formData.value.phanTramGiamDot) formData.value.phanTramGiamDot = formData.value.phanTramGiamDot + "%"; if (formData.value.ngayBatDau && formData.value.ngayBatDau.length > 10) formData.value.ngayBatDau = formData.value.ngayBatDau.substring(0, 10); if (formData.value.ngayKetThuc && formData.value.ngayKetThuc.length > 10) formData.value.ngayKetThuc = formData.value.ngayKetThuc.substring(0, 10); showModal.value = true; }; const openEditModal = (dgg) => {
  isEdit.value = true
  isView.value = false
  percentError.value = ''
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
    percentError.value = ''
    const payload = { ...formData.value }

    let parsedPercent = parseInt(String(payload.phanTramGiamDot).replace(/\D/g, ''), 10);
    if (isNaN(parsedPercent) || parsedPercent < 1 || parsedPercent > 100) {
      percentError.value = "Phần trăm giảm giá phải nằm trong khoảng từ 1% đến 100%!"
      return;
    }
    payload.phanTramGiamDot = parsedPercent;

    if (payload.ngayBatDau && payload.ngayKetThuc) {
      if (new Date(payload.ngayKetThuc) < new Date(payload.ngayBatDau)) {
        Toast.fire({ icon: 'warning', title: 'Ngày kết thúc phải sau ngày bắt đầu!' })
        return;
      }
    }

    if (payload.ngayBatDau && payload.ngayBatDau.length === 10) {
      payload.ngayBatDau = payload.ngayBatDau + 'T00:00:00';
    }
    if (payload.ngayKetThuc && payload.ngayKetThuc.length === 10) {
      payload.ngayKetThuc = payload.ngayKetThuc + 'T23:59:59';
    }

    if (isEdit.value) {
      await api.put(`/dot-giam-gia/${payload.id}`, payload)
      Toast.fire({ icon: 'success', title: 'Cập nhật đợt giảm giá thành công!' })
    } else {
      await api.post('/dot-giam-gia', payload)
      Toast.fire({ icon: 'success', title: 'Thêm đợt giảm giá thành công!' })
    }
    closeModal()
    fetchDGG()
  } catch (error) {
    console.error("Lỗi khi lưu đợt giảm giá:", error)
    Toast.fire({ icon: 'error', title: error.response?.data?.message || 'Có lỗi xảy ra khi lưu!' })
  }
}

const deleteDGG = async (dgg) => {
  try {
    await api.delete(`/dot-giam-gia/${dgg.id}`)
    fetchDGG()
    Toast.fire({ icon: 'success', title: 'Xóa đợt giảm giá thành công!' })
  } catch (error) {
    console.error("Lỗi khi xóa đợt giảm giá:", error)
    Toast.fire({ icon: 'error', title: error.response?.data?.message || 'Có lỗi xảy ra khi xóa!' })
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

const displayStatus = (statusValue) => {
  if (statusValue === 1) return 'Đang diễn ra';
  if (statusValue === 2) return 'Sắp diễn ra';
  if (statusValue === 0) return 'Đã kết thúc';
  return 'Ngừng hoạt động';
}

const statusBadgeClass = (statusValue) => {
  if (statusValue === 1) return 'bg-success bg-opacity-10 text-success border-0';
  if (statusValue === 2) return 'bg-warning bg-opacity-10 text-warning border-0';
  if (statusValue === 0) return 'bg-secondary bg-opacity-10 text-secondary border-0';
  return 'bg-danger bg-opacity-10 text-danger border-0';
}

const reset = () => {
  searchQuery.value = ''
  searchText.value = ''
  searchStartDate.value = ''
  searchEndDate.value = ''
  searchStatus.value = ''
  fetchDGG()
}

const search = () => {
  searchQuery.value = searchText.value;
  currentPage.value = 1;
}

const toYmd = (v) => {
  if (!v) return ''
  if (Array.isArray(v)) return "${v[0]}-${String(v[1]).padStart(2, '0')}-${String(v[2]).padStart(2, '0')}"
  return String(v).substring(0, 10)
}

const filteredList = computed(() => {
  return dsDotGiamGia.value.filter(dgg => {
    let match = true;
    const q = (searchQuery.value || '').trim().toLowerCase();
    if (q) {
      const ten = (dgg.tenDot || '').toLowerCase();
      const ma = (dgg.maDot || '').toLowerCase();
      const pt = String(dgg.phanTramGiamDot || '');
      if (!ten.includes(q) && !ma.includes(q) && !pt.includes(q)) match = false;
    }
    
    if (searchStatus.value !== '' && String(dgg.trangThai) !== searchStatus.value) match = false;
    
    if (searchStartDate.value && dgg.ngayKetThuc) {
      if (toYmd(dgg.ngayKetThuc) < searchStartDate.value) match = false;
    }
    if (searchEndDate.value && dgg.ngayBatDau) {
      if (toYmd(dgg.ngayBatDau) > searchEndDate.value) match = false;
    }
    
    return match;
  })
})


watch([searchQuery, searchStartDate, searchEndDate, searchStatus], () => {
  currentPage.value = 1;
});

</script>

<style scoped>
/* Filter inputs */
.form-control, .form-select {
  font-size: 0.9rem;
}
.form-control::placeholder {
  color: #adb5bd;

}

/* Custom Table Design */
.custom-table td {
  font-size: 0.9rem;
  vertical-align: middle;
  border-bottom: 1px solid #f1f3f5;
  padding-top: 1rem;
  padding-bottom: 1rem;
}
.custom-table tbody tr:hover {
  background-color: #fcfcfc;
}

/* Status Badges */
.badge {
  font-weight: 600;
  letter-spacing: 0.3px;
  font-size: 0.8rem;
}


/* Icon Buttons */
.icon-btn {
  width: 32px;
  height: 32px;
  display: inline-flex;


  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}
.icon-btn:hover {
  background-color: #e9ecef !important;
  transform: translateY(-1px);
}

/* Button Danger specific */





/* Pagination (giống trang Khách hàng) */
.custom-pagination .page-link {
  color: #495057;
  border: none !important;
  width: 38px;
  height: 38px;
  display: flex;

  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}
.icon-btn:hover {
  background-color: #e9ecef !important;
  transform: translateY(-1px);
}

/* Button Danger specific */





/* Pagination */
.custom-pagination .page-link {
  transition: all 0.2s ease-in-out;
}
.custom-pagination .page-link:hover:not(.bg-primary) {
  background-color: #f8f9fa !important;
}
.custom-pagination .page-item.disabled .page-link {
  opacity: 0.5;
  pointer-events: none;
}
</style>







