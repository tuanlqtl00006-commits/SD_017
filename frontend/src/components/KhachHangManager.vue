<template>
  <div>
        
        <div class="bg-white border rounded-3 shadow-sm py-3 px-4 mb-4">
          <h5 class="fw-bold text-primary m-0">Quản lý khách hàng</h5>
        </div>

        
        <div class="card border-0 shadow-sm rounded-4 mb-4">
          <div class="card-body p-4">
            <h6 class="fw-bold mb-3 d-flex align-items-center"><i class="bi bi-funnel text-muted me-2 fs-5"></i> Bộ Lọc</h6>
            <div class="row g-3">
              <div class="col-md-4">
                <label class="form-label small text-dark fw-medium">Tìm kiếm</label>
                <input type="text" class="form-control" placeholder="Nhập tên, số điện thoại, hoặc email...">
              </div>
              <div class="col-md-4">
                <label class="form-label small text-dark fw-medium">Giới Tính</label>
                <select class="form-select text-muted">
                  <option selected>Tất Cả</option>
                  <option value="1">Nam</option>
                  <option value="2">Nữ</option>
                </select>
              </div>
              <div class="col-md-4">
                <label class="form-label small text-dark fw-medium">Trạng Thái</label>
                <select class="form-select text-muted">
                  <option selected>Tất Cả</option>
                  <option value="1">Hoạt động</option>
                  <option value="0">Ngừng hoạt động</option>
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
                   <i class="bi bi-people fs-4 text-secondary"></i>
                 </div>
                 <h4 class="fw-bold m-0 text-dark" style="font-size: 1.25rem;">Danh Sách Khách Hàng</h4>
               </div>
               <button class="btn btn-success px-4 shadow-sm" @click="openAddModal"><i class="bi bi-plus-circle me-1"></i> Thêm Khách Hàng</button>
            </div>
            
            <div class="table-responsive px-2">
              <table class="table align-middle m-0 text-start">
                <thead style="background-color: #f4f6f9;">
                  <tr>
                    <th class="py-3 text-center fw-bold text-dark border-0" style="width: 80px;">STT</th>
                    <th class="py-3 fw-bold text-dark border-0">Mã KH</th>
                    <th class="py-3 fw-bold text-dark border-0">Họ và Tên</th>
                    <th class="py-3 fw-bold text-dark border-0">Số Điện Thoại</th>
                    <th class="py-3 fw-bold text-dark border-0">Email</th>
                    <th class="py-3 fw-bold text-dark border-0">Ngày Sinh</th>
                    <th class="py-3 fw-bold text-dark border-0">Giới Tính</th>
                    <th class="py-3 fw-bold text-dark border-0">Trạng Thái</th>
                    <th class="py-3 text-center fw-bold text-dark border-0" style="width: 120px;">Hành Động</th>
                  </tr>
                </thead>
                <tbody class="text-dark">
                  <tr v-for="(kh, index) in customers" :key="kh.id" :class="{ 'text-muted opacity-50': kh.an }">
                    <td class="py-4 text-center" style="border-bottom: 1px solid #f0f0f0;">{{ index + 1 }}</td>
                    <td class="py-4" style="border-bottom: 1px solid #f0f0f0;">{{ kh.maKH }}</td>
                    <td class="py-4" style="border-bottom: 1px solid #f0f0f0;">{{ kh.ten }}</td>
                    <td class="py-4" style="border-bottom: 1px solid #f0f0f0;">{{ kh.sdt }}</td>
                    <td class="py-4" style="border-bottom: 1px solid #f0f0f0;">{{ kh.email }}</td>
                    <td class="py-4" style="border-bottom: 1px solid #f0f0f0;">{{ formatDate(kh.ngaySinh) }}</td>
                    <td class="py-4" style="border-bottom: 1px solid #f0f0f0;">{{ kh.gioiTinh }}</td>
                    <td class="py-4" style="border-bottom: 1px solid #f0f0f0;">
                      <span v-if="kh.an" class="badge py-2 px-3 rounded-pill bg-secondary text-white">Đã ẩn</span>
                      <span v-else class="badge py-2 px-3 rounded-pill" :class="kh.trangThai === 'Hoạt động' ? 'bg-success text-white' : 'bg-secondary text-white'">{{ kh.trangThai }}</span>
                    </td>
                    <td class="py-4 text-center" style="border-bottom: 1px solid #f0f0f0;">
                      <a href="#" class="text-primary me-2" @click.prevent="openEditModal(kh)"><i class="bi bi-pencil-square fs-5"></i></a>
                      <a href="#" :class="kh.an ? 'text-success' : 'text-secondary'" :title="kh.an ? 'Hiện lại khách hàng' : 'Ẩn khách hàng'" @click.prevent="toggleAnKH(kh)"><i class="bi fs-5" :class="kh.an ? 'bi-arrow-counterclockwise' : 'bi-eye-slash'"></i></a>
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

      
        <!-- Modal Thêm/Sửa Khách Hàng -->
        <div v-if="showModal" class="modal-backdrop" style="position: fixed; top: 0; left: 0; width: calc(100vw / var(--ui-zoom)); height: calc(100vh / var(--ui-zoom)); background: rgba(0,0,0,0.5); z-index: 1050; display: flex; align-items: center; justify-content: center;">
          <div class="modal-dialog bg-white rounded-3 shadow" style="width: 500px; max-width: 90vw;">
            <div class="modal-header d-flex justify-content-between align-items-center p-3 border-bottom">
              <h5 class="modal-title fw-bold text-primary m-0">{{ isEdit ? 'Cập Nhật Khách Hàng' : 'Thêm Mới Khách Hàng' }}</h5>
              <button type="button" class="btn-close" @click="closeModal"></button>
            </div>
            <div class="modal-body p-4">
              <form @submit.prevent="saveCustomer">
                <div class="mb-3">
                  <label class="form-label small text-dark fw-medium">Mã KH</label>
                  <input type="text" class="form-control" v-model="formData.maKH" required :disabled="isEdit">
                </div>
                <div class="mb-3">
                  <label class="form-label small text-dark fw-medium">Họ và Tên</label>
                  <input type="text" class="form-control" v-model="formData.ten" required>
                </div>
                <div class="mb-3">
                  <label class="form-label small text-dark fw-medium">Số Điện Thoại</label>
                  <input type="text" class="form-control" v-model="formData.sdt" required>
                </div>
                <div class="mb-3">
                  <label class="form-label small text-dark fw-medium">Email</label>
                  <input type="email" class="form-control" v-model="formData.email">
                </div>
                <div class="mb-3">
                  <label class="form-label small text-dark fw-medium">Ngày Sinh</label>
                  <input type="date" class="form-control" v-model="formData.ngaySinh">
                </div>
                <div class="mb-3">
                  <label class="form-label small text-dark fw-medium">Giới Tính</label>
                  <select class="form-select" v-model="formData.gioiTinh">
                    <option value="Nam">Nam</option>
                    <option value="Nữ">Nữ</option>
                  </select>
                </div>
                <div class="mb-3">
                  <label class="form-label small text-dark fw-medium">Trạng Thái</label>
                  <select class="form-select" v-model="formData.trangThai">
                    <option value="Hoạt động">Hoạt động</option>
                    <option value="Ngừng hoạt động">Ngừng hoạt động</option>
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

      
        <!-- Modal Thêm/Sửa Khách Hàng -->
        <div v-if="showModal" class="modal-backdrop" style="position: fixed; top: 0; left: 0; width: calc(100vw / var(--ui-zoom)); height: calc(100vh / var(--ui-zoom)); background: rgba(0,0,0,0.5); z-index: 1050; display: flex; align-items: center; justify-content: center;">
          <div class="modal-dialog bg-white rounded-3 shadow" style="width: 500px; max-width: 90vw;">
            <div class="modal-header d-flex justify-content-between align-items-center p-3 border-bottom">
              <h5 class="modal-title fw-bold text-primary m-0">{{ isEdit ? 'Cập Nhật Khách Hàng' : 'Thêm Mới Khách Hàng' }}</h5>
              <button type="button" class="btn-close" @click="closeModal"></button>
            </div>
            <div class="modal-body p-4">
              <form @submit.prevent="saveCustomer">
                <div class="mb-3">
                  <label class="form-label small text-dark fw-medium">Mã KH</label>
                  <input type="text" class="form-control" v-model="formData.maKH" required :disabled="isEdit">
                </div>
                <div class="mb-3">
                  <label class="form-label small text-dark fw-medium">Họ và Tên</label>
                  <input type="text" class="form-control" v-model="formData.ten" required>
                </div>
                <div class="mb-3">
                  <label class="form-label small text-dark fw-medium">Số Điện Thoại</label>
                  <input type="text" class="form-control" v-model="formData.sdt" required>
                </div>
                <div class="mb-3">
                  <label class="form-label small text-dark fw-medium">Email</label>
                  <input type="email" class="form-control" v-model="formData.email">
                </div>
                <div class="mb-3">
                  <label class="form-label small text-dark fw-medium">Ngày Sinh</label>
                  <input type="date" class="form-control" v-model="formData.ngaySinh">
                </div>
                <div class="mb-3">
                  <label class="form-label small text-dark fw-medium">Giới Tính</label>
                  <select class="form-select" v-model="formData.gioiTinh">
                    <option value="Nam">Nam</option>
                    <option value="Nữ">Nữ</option>
                  </select>
                </div>
                <div class="mb-3">
                  <label class="form-label small text-dark fw-medium">Trạng Thái</label>
                  <select class="form-select" v-model="formData.trangThai">
                    <option value="Hoạt động">Hoạt động</option>
                    <option value="Ngừng hoạt động">Ngừng hoạt động</option>
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

      
        <!-- Modal Thêm/Sửa Khách Hàng -->
        <div v-if="showModal" class="modal-backdrop" style="position: fixed; top: 0; left: 0; width: calc(100vw / var(--ui-zoom)); height: calc(100vh / var(--ui-zoom)); background: rgba(0,0,0,0.5); z-index: 1050; display: flex; align-items: center; justify-content: center;">
          <div class="modal-dialog bg-white rounded-3 shadow" style="width: 500px; max-width: 90vw;">
            <div class="modal-header d-flex justify-content-between align-items-center p-3 border-bottom">
              <h5 class="modal-title fw-bold text-primary m-0">{{ isEdit ? 'Cập Nhật Khách Hàng' : 'Thêm Mới Khách Hàng' }}</h5>
              <button type="button" class="btn-close" @click="closeModal"></button>
            </div>
            <div class="modal-body p-4">
              <form @submit.prevent="saveCustomer">
                <div class="mb-3">
                  <label class="form-label small text-dark fw-medium">Mã KH</label>
                  <input type="text" class="form-control" v-model="formData.maKH" required :disabled="isEdit">
                </div>
                <div class="mb-3">
                  <label class="form-label small text-dark fw-medium">Họ và Tên</label>
                  <input type="text" class="form-control" v-model="formData.ten" required>
                </div>
                <div class="mb-3">
                  <label class="form-label small text-dark fw-medium">Số Điện Thoại</label>
                  <input type="text" class="form-control" v-model="formData.sdt" required>
                </div>
                <div class="mb-3">
                  <label class="form-label small text-dark fw-medium">Email</label>
                  <input type="email" class="form-control" v-model="formData.email">
                </div>
                <div class="mb-3">
                  <label class="form-label small text-dark fw-medium">Ngày Sinh</label>
                  <input type="date" class="form-control" v-model="formData.ngaySinh">
                </div>
                <div class="mb-3">
                  <label class="form-label small text-dark fw-medium">Giới Tính</label>
                  <select class="form-select" v-model="formData.gioiTinh">
                    <option value="Nam">Nam</option>
                    <option value="Nữ">Nữ</option>
                  </select>
                </div>
                <div class="mb-3">
                  <label class="form-label small text-dark fw-medium">Trạng Thái</label>
                  <select class="form-select" v-model="formData.trangThai">
                    <option value="Hoạt động">Hoạt động</option>
                    <option value="Ngừng hoạt động">Ngừng hoạt động</option>
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
  maKH: '',
  ten: '',
  sdt: '',
  email: '',
  ngaySinh: '',
  gioiTinh: 'Nam',
  trangThai: 'Hoạt động'
})

const customers = ref([
  { id: 1, maKH: 'KH001', ten: 'Nguyễn Văn A', sdt: '0987654321', email: 'nva@gmail.com', ngaySinh: '1990-05-12', gioiTinh: 'Nam', trangThai: 'Hoạt động' },
  { id: 2, maKH: 'KH002', ten: 'Trần Thị B', sdt: '0912345678', email: 'ttb@gmail.com', ngaySinh: '1995-10-22', gioiTinh: 'Nữ', trangThai: 'Hoạt động' }
])

const openAddModal = () => {
  isEdit.value = false
  formData.value = {
    maKH: '',
    ten: '',
    sdt: '',
    email: '',
    ngaySinh: '',
    gioiTinh: 'Nam',
    trangThai: 'Hoạt động'
  }
  showModal.value = true
}

const openEditModal = (kh) => {
  isEdit.value = true
  formData.value = { ...kh }
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
}

const saveCustomer = () => {
  if (isEdit.value) {
    const index = customers.value.findIndex(c => c.maKH === formData.value.maKH)
    if (index !== -1) {
      customers.value[index] = { ...customers.value[index], ...formData.value }
    }
  } else {
    customers.value.push({ ...formData.value, id: Date.now() })
  }
  closeModal()
}

// Không xóa dữ liệu, chỉ ẩn / hiện lại khách hàng
const toggleAnKH = (kh) => {
  const lyDo = kh.an ? 'hiện lại' : 'ẩn'
  if (confirm(`Bạn có chắc chắn muốn ${lyDo} khách hàng này?`)) {
    kh.an = !kh.an
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
