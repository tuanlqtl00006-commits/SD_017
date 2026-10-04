import fs from 'fs';

const khachhangPath = 'src/components/KhachHangManager.vue';
let content = fs.readFileSync(khachhangPath, 'utf8');

const kh_modal_html = `
        
        <div v-if="showModal" class="modal-backdrop" style="position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; background: rgba(0,0,0,0.5); z-index: 1050; display: flex; align-items: center; justify-content: center;">
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
`;

const kh_script = `
<script setup>
import { ref } from 'vue'
import Sidebar from './Sidebar.vue'
import Header from './Header.vue'

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
      customers.value[index] = { ...formData.value }
    }
  } else {
    customers.value.push({ ...formData.value, id: Date.now() })
  }
  closeModal()
}

const deleteCustomer = (kh) => {
  if(confirm('Bạn có chắc chắn muốn xóa khách hàng này?')) {
    customers.value = customers.value.filter(c => c.maKH !== kh.maKH)
  }
}

const formatDate = (dateString) => {
  if (!dateString) return ''
  const parts = dateString.split('-')
  if (parts.length === 3) {
    return \`\${parts[2]}/\${parts[1]}/\${parts[0]}\`
  }
  return dateString
}
</script>
`;

const kh_table_body = `
                <tbody class="text-dark">
                  <tr v-for="(kh, index) in customers" :key="kh.id">
                    <td class="py-4 text-center" style="border-bottom: 1px solid #f0f0f0;">{{ index + 1 }}</td>
                    <td class="py-4" style="border-bottom: 1px solid #f0f0f0;">{{ kh.maKH }}</td>
                    <td class="py-4" style="border-bottom: 1px solid #f0f0f0;">{{ kh.ten }}</td>
                    <td class="py-4" style="border-bottom: 1px solid #f0f0f0;">{{ kh.sdt }}</td>
                    <td class="py-4" style="border-bottom: 1px solid #f0f0f0;">{{ kh.email }}</td>
                    <td class="py-4" style="border-bottom: 1px solid #f0f0f0;">{{ formatDate(kh.ngaySinh) }}</td>
                    <td class="py-4" style="border-bottom: 1px solid #f0f0f0;">{{ kh.gioiTinh }}</td>
                    <td class="py-4" style="border-bottom: 1px solid #f0f0f0;">
                      <span class="badge py-2 px-3 rounded-pill" :class="kh.trangThai === 'Hoạt động' ? 'bg-success text-white' : 'bg-secondary text-white'">{{ kh.trangThai }}</span>
                    </td>
                    <td class="py-4 text-center" style="border-bottom: 1px solid #f0f0f0;">
                      <a href="#" class="text-primary me-2" @click.prevent="openEditModal(kh)"><i class="bi bi-pencil-square fs-5"></i></a>
                      <a href="#" class="text-danger" @click.prevent="deleteCustomer(kh)"><i class="bi bi-trash fs-5"></i></a>
                    </td>
                  </tr>
                </tbody>
`;

content = content.replace('<button class="btn btn-success px-4 shadow-sm"><i class="bi bi-plus-circle me-1"></i> Thêm Khách Hàng</button>', '<button class="btn btn-success px-4 shadow-sm" @click="openAddModal"><i class="bi bi-plus-circle me-1"></i> Thêm Khách Hàng</button>');

content = content.replace(/<tbody class="text-dark">[\s\S]*?<\/tbody>/, kh_table_body.trim());

content = content.replace('</main>', kh_modal_html + '\n      </main>');

content = content.replace(/<script setup>[\s\S]*?<\/script>/, kh_script.trim());

fs.writeFileSync(khachhangPath, content, 'utf8');
console.log('KhachHangManager.vue updated successfully.');
