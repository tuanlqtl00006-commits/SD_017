import fs from 'fs';

const dggPath = 'd:/DATN/SD_017/frontend/src/components/DotGiamGiaManager.vue';
let content = fs.readFileSync(dggPath, 'utf8');

const dgg_modal_html = `
        
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
`;

const dgg_script = `
<script setup>
import { ref } from 'vue'
import Sidebar from './Sidebar.vue'
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
  return \`\${d}/\${m}/\${y}<br>\${hh}:\${mm}:\${ss}\`
}
</script>
`;

const dgg_table_body = `
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
`;

content = content.replace('<button class="btn btn-success px-4 shadow-sm"><i class="bi bi-plus-circle me-1"></i> Thêm Đợt Giảm Giá</button>', '<button class="btn btn-success px-4 shadow-sm" @click="openAddModal"><i class="bi bi-plus-circle me-1"></i> Thêm Đợt Giảm Giá</button>');

content = content.replace(/<tbody class="text-dark">[\s\S]*?<\/tbody>/, dgg_table_body.trim());

content = content.replace('</main>', dgg_modal_html + '\n      </main>');

content = content.replace(/<script setup>[\s\S]*?<\/script>/, dgg_script.trim());

fs.writeFileSync(dggPath, content, 'utf8');
console.log('DotGiamGiaManager.vue updated successfully.');
