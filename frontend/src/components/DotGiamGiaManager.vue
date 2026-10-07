<template>
  <div class="d-flex" style="min-height: 100vh; background-color: #f4f7f6;">


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
                  <tr v-for="(dgg, index) in dsDotGiamGia" :key="dgg.id">
                    <td class="py-4 text-center" style="border-bottom: 1px solid #f0f0f0;">{{ index + 1 }}</td>
                    <td class="py-4" style="border-bottom: 1px solid #f0f0f0;">{{ dgg.maDot }}</td>
                    <td class="py-4" style="border-bottom: 1px solid #f0f0f0;">{{ dgg.tenDot }}</td>
                    <td class="py-4" style="border-bottom: 1px solid #f0f0f0;">{{ dgg.giaTriGiam }}</td>
                    <td class="py-4" style="border-bottom: 1px solid #f0f0f0;" v-html="formatDateTime(dgg.batDau)"></td>
                    <td class="py-4" style="border-bottom: 1px solid #f0f0f0;" v-html="formatDateTime(dgg.ketThuc)"></td>
                    <td class="py-4" style="border-bottom: 1px solid #f0f0f0;">
                      <span class="badge py-2 px-3 rounded-pill" :class="dgg.trangThai === 'Đang diễn ra' ? 'bg-success text-white' : dgg.trangThai === 'Sắp diễn ra' ? 'bg-warning text-dark' : 'bg-secondary text-white'">{{ dgg.trangThai }}</span>
                    </td>
                    <td class="py-4 text-center" style="border-bottom: 1px solid #f0f0f0;">
                      <a href="#" class="text-primary me-2" @click.prevent="openEditModal(dgg)"><i class="bi bi-pencil-square fs-5"></i></a>
                      <a href="#" class="text-danger" @click.prevent="deleteDGG(dgg)"><i class="bi bi-trash fs-5"></i></a>
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
          <div class="modal-dialog bg-white rounded-3 shadow" style="width: 500px; max-width: 90vw; pointer-events: auto;">
            <div class="modal-header d-flex justify-content-between align-items-center p-3 border-bottom">
              <h5 class="modal-title fw-bold text-dark m-0">{{ isView ? 'Chi Tiết Đợt Giảm Giá' : (isEdit ? 'Cập Nhật Đợt Giảm Giá' : 'Thêm Mới Đợt Giảm Giá') }}</h5>
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
                  <input type="text" class="form-control" v-model="formData.phanTramGiamDot" :disabled="isView" @input="percentError = ''" required placeholder="vd: 20%">
                  <small v-if="percentError" class="text-danger mt-1 d-block">{{ percentError }}</small>
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

      
        
        <div v-if="showModal" class="modal-backdrop" style="position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; background: rgba(0,0,0,0.5); z-index: 1050; display: flex; align-items: center; justify-content: center;">
          <div class="modal-dialog bg-white rounded-3 shadow" style="width: 500px; max-width: 90vw;">
            <div class="modal-header d-flex justify-content-between align-items-center p-3 border-bottom">
              <h5 class="modal-title fw-bold text-primary m-0">{{ isEdit ? 'Cập Nhật Đợt Giảm Giá' : 'Thêm Mới Đợt Giảm Giá' }}</h5>
              <button type="button" class="btn-close" @click="closeModal"></button>
            </div>
            <div class="modal-body p-4">
              <form @submit.prevent="saveDGG">
                <div class="mb-3">
                  <label class="form-label small text-dark fw-medium">Mã Đợt</label>
                  <input type="text" class="form-control" v-model="formData.maDot" required :disabled="isEdit">
                </div>
                <div class="mb-3">
                  <label class="form-label small text-dark fw-medium">Tên Đợt Giảm Giá</label>
                  <input type="text" class="form-control" v-model="formData.tenDot" required>
                </div>
                <div class="mb-3">
                  <label class="form-label small text-dark fw-medium">Giá Trị Giảm</label>
                  <input type="text" class="form-control" v-model="formData.giaTriGiam" required placeholder="vd: 20% hoặc 50000đ">
                </div>
                <div class="row">
                  <div class="col-6 mb-3">
                    <label class="form-label small text-dark fw-medium">Bắt đầu</label>
                    <input type="datetime-local" class="form-control" v-model="formData.batDau" required>
                  </div>
                  <div class="col-6 mb-3">
                    <label class="form-label small text-dark fw-medium">Kết thúc</label>
                    <input type="datetime-local" class="form-control" v-model="formData.ketThuc" required>
                  </div>
                </div>
                <div class="mb-3">
                  <label class="form-label small text-dark fw-medium">Trạng Thái</label>
                  <select class="form-select" v-model="formData.trangThai">
                    <option value="Sắp diễn ra">Sắp diễn ra</option>
                    <option value="Đang diễn ra">Đang diễn ra</option>
                    <option value="Đã kết thúc">Đã kết thúc</option>
                  </select>
                </div>
                <div class="d-flex justify-content-end gap-2 mt-4">
                  <button type="button" class="btn btn-secondary px-4" @click="closeModal">Hủy</button>
                  <button type="submit" class="btn btn-primary px-4">Lưu</button>
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
import { ref } from 'vue'
import Header from './Header.vue'

const showModal = ref(false)
const isEdit = ref(false)
const formData = ref({
  maDot: '',
  tenDot: '',
  giaTriGiam: '',
  batDau: '',
  ketThuc: '',
  trangThai: 'Sắp diễn ra'
})

const dsDotGiamGia = ref([
  { id: 1, maDot: 'DGG001', tenDot: 'Giảm giá mùa hè 2025', giaTriGiam: '20%', batDau: '2025-06-01T00:00', ketThuc: '2025-06-30T23:59', trangThai: 'Sắp diễn ra' },
  { id: 2, maDot: 'DGG002', tenDot: 'Sale Quốc Khánh', giaTriGiam: '50,000đ', batDau: '2025-09-01T00:00', ketThuc: '2025-09-05T23:59', trangThai: 'Đang diễn ra' }
])

const openAddModal = () => {
  isEdit.value = false
  formData.value = {
    maDot: '',
    tenDot: '',
    giaTriGiam: '',
    batDau: '',
    ketThuc: '',
    trangThai: 'Sắp diễn ra'
  }
  showModal.value = true
}

const openEditModal = (dgg) => {
  isEdit.value = true
  formData.value = { ...dgg }
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
}

const saveDGG = () => {
  if (isEdit.value) {
    const index = dsDotGiamGia.value.findIndex(d => d.maDot === formData.value.maDot)
    if (index !== -1) {
      dsDotGiamGia.value[index] = { ...formData.value }
    }
  } else {
    dsDotGiamGia.value.push({ ...formData.value, id: Date.now() })
  }
  closeModal()
}

const deleteDGG = (dgg) => {
  if(confirm('Bạn có chắc chắn muốn xóa đợt giảm giá này?')) {
    dsDotGiamGia.value = dsDotGiamGia.value.filter(d => d.maDot !== dgg.maDot)
  }
}

const formatDateTime = (dateTimeString) => {
  if (!dateTimeString) return ''
  const date = new Date(dateTimeString)
  if(isNaN(date.getTime())) return dateTimeString
  const d = date.getDate().toString().padStart(2, '0')
  const m = (date.getMonth() + 1).toString().padStart(2, '0')
  const y = date.getFullYear()
  const hh = date.getHours().toString().padStart(2, '0')
  const mm = date.getMinutes().toString().padStart(2, '0')
  const ss = date.getSeconds().toString().padStart(2, '0')
  return `${d}/${m}/${y}<br>${hh}:${mm}:${ss}`
}
</script>

<style scoped>

.form-control::placeholder {
  color: #ced4da;
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
