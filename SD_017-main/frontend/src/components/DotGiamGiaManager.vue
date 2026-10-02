<template>
  <div>
        
        <div class="bg-white border rounded-3 shadow-sm py-3 px-4 mb-4">
          <h5 class="fw-bold text-primary m-0">Quản lý đợt giảm giá</h5>
        </div>

        
        <div class="card border-0 shadow-sm rounded-4 mb-4">
          <div class="card-body p-4">
            <h6 class="fw-bold mb-3 d-flex align-items-center"><i class="bi bi-funnel text-muted me-2 fs-5"></i> Bộ Lọc</h6>
            <div class="row g-3">
              <div class="col-md-3">
                <label class="form-label small text-dark fw-medium">Tên đợt giảm giá</label>
                <input type="text" class="form-control" placeholder="Nhập tên đợt giảm giá">
              </div>
              <div class="col-md-3">
                <label class="form-label small text-dark fw-medium">Thời gian bắt đầu</label>
                <input type="date" class="form-control text-muted">
              </div>
              <div class="col-md-3">
                <label class="form-label small text-dark fw-medium">Thời gian kết thúc</label>
                <input type="date" class="form-control text-muted">
              </div>
              <div class="col-md-3">
                <label class="form-label small text-dark fw-medium">Trạng Thái</label>
                <select class="form-select text-muted">
                  <option selected>Tất Cả</option>
                  <option value="1">Đang diễn ra</option>
                  <option value="2">Sắp diễn ra</option>
                  <option value="3">Đã kết thúc</option>
                </select>
              </div>
            </div>
            <div class="d-flex justify-content-end gap-3 mt-4">
              <button class="btn btn-primary px-4 shadow-sm">Tìm kiếm</button>
              <button class="btn btn-danger px-4 shadow-sm">Làm mới</button>
            </div>
          </div>
        </div>

        
        <div class="card rounded-3 shadow-sm bg-white" style="border: 2px solid #5b8deb;">
          <div class="card-body p-0">
            
            <div class="d-flex align-items-center justify-content-between px-4 pt-4 pb-3">
               <div class="d-flex align-items-center">
                 <div class="bg-secondary bg-opacity-25 text-primary rounded-3 d-flex align-items-center justify-content-center me-3" style="width: 45px; height: 45px;">
                   <i class="bi bi-percent fs-4 text-secondary"></i>
                 </div>
                 <h4 class="fw-bold m-0 text-dark" style="font-size: 1.25rem;">Danh Sách Đợt Giảm Giá</h4>
               </div>
               <button class="btn btn-success px-4 shadow-sm" @click="openAddModal"><i class="bi bi-plus-circle me-1"></i> Thêm Đợt Giảm Giá</button>
            </div>

            
            <div class="table-responsive px-2">
              <table class="table align-middle m-0 text-start">
                <thead style="background-color: #f4f6f9;">
                  <tr>
                    <th class="py-3 text-center fw-bold text-dark border-0" style="width: 80px;">STT</th>
                    <th class="py-3 fw-bold text-dark border-0">Mã Đợt</th>
                    <th class="py-3 fw-bold text-dark border-0">Tên Đợt Giảm Giá</th>
                    <th class="py-3 fw-bold text-dark border-0">Giá Trị Giảm</th>
                    <th class="py-3 fw-bold text-dark border-0">Bắt Đầu</th>
                    <th class="py-3 fw-bold text-dark border-0">Kết Thúc</th>
                    <th class="py-3 fw-bold text-dark border-0">Trạng Thái</th>
                    <th class="py-3 text-center fw-bold text-dark border-0" style="width: 120px;">Hành Động</th>
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
                <ul class="pagination custom-pagination m-0 gap-2">
                  <li class="page-item disabled">
                    <a class="page-link rounded-2 bg-white" href="#"><i class="bi bi-chevron-left"></i></a>
                  </li>
                  <li class="page-item active">
                    <a class="page-link rounded-2 bg-white text-dark shadow-sm" style="border: 1px solid #dee2e6;" href="#">1</a>
                  </li>
                  <li class="page-item">
                    <a class="page-link rounded-2 bg-white" href="#"><i class="bi bi-chevron-right"></i></a>
                  </li>
                </ul>
              </nav>
            </div>

          </div>
        </div>

      
        <!-- Modal Thêm/Sửa Đợt Giảm Giá -->
        <div v-if="showModal" class="modal-backdrop" style="position: fixed; top: 0; left: 0; width: calc(100vw / var(--ui-zoom)); height: calc(100vh / var(--ui-zoom)); background: rgba(0,0,0,0.5); z-index: 1050; display: flex; align-items: center; justify-content: center;">
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

      </div>
</template>

<script setup>
import { ref } from 'vue'

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
/* Form Input */
.form-control::placeholder {
  color: #ced4da;
}

/* Pagination */
.custom-pagination .page-link {
  color: #495057;
  border: 1px solid #e9ecef;
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
