<script setup>
import { onMounted, reactive, ref } from 'vue'
import SanPhamFormModal from './sanPham/SanPhamFormModal.vue'
import { sanPhamService } from '../services/sanPhamService'
import { thuocTinhService } from '../services/thuocTinhService'
import { TRANG_THAI, trangThaiOf } from '../constants/thuocTinh'
import { useToast } from '../composables/useToast'
import { layTen } from '../utils/text'

// NGÀY 2: Quản lý sản phẩm = THÊM + SỬA (và hiển thị danh sách để thấy kết quả).
// Các chức năng làm ở những ngày sau: lọc, phân trang, ngưng/kích hoạt, xem chi tiết, xuất Excel.

const toast = useToast()

/* ===== 1. DỮ LIỆU CỦA TRANG ===== */

const danhSachSanPham = ref([]) // dữ liệu bảng san_pham
const dangTai = ref(false) // true: đang tải, hiện chữ "Đang tải dữ liệu…"
const dangLuu = ref(false) // true: đang lưu form, khóa nút để không bấm 2 lần

// 6 bảng thuộc tính của sản phẩm: dùng để đổ vào dropdown của form và đổi id thành tên trong bảng
const thuocTinh = reactive({
  'danh-muc': [],
  'thuong-hieu': [],
  'xuat-xu': [],
  'chat-lieu': [],
  'do-cung': [],
  'diem-can-bang': [],
})

/* ===== 2. TẢI DỮ LIỆU ===== */

async function taiThuocTinh() {
  // Gọi API lấy 6 bảng thuộc tính cùng lúc rồi gán vào thuocTinh
  const cacLoai = Object.keys(thuocTinh)
  const ketQua = await Promise.all(cacLoai.map((loai) => thuocTinhService.getAll(loai)))
  cacLoai.forEach((loai, i) => {
    thuocTinh[loai] = ketQua[i]
  })
}

async function taiDuLieu(hienChuDangTai = true) {
  if (hienChuDangTai) dangTai.value = true
  try {
    // Hai lời gọi API độc lập nhau nên chạy song song cho nhanh
    const [sanPham] = await Promise.all([sanPhamService.getAll(), taiThuocTinh()])
    danhSachSanPham.value = sanPham
  } catch (loi) {
    toast.error(loi.message || 'Không tải được danh sách sản phẩm. Vui lòng thử lại.')
  } finally {
    dangTai.value = false
  }
}

// onMounted: chạy 1 lần khi trang vừa hiện ra -> tải dữ liệu
onMounted(() => taiDuLieu())

/* ===== 3. THÊM / SỬA ===== */

const hienForm = ref(false) // true: đang mở form thêm/sửa
const spDangSua = ref(null) // null: thêm mới; có giá trị: đang sửa sản phẩm đó

function moFormThem() {
  spDangSua.value = null
  hienForm.value = true
}

// Bấm nút "Chỉnh sửa" ngay trên dòng của bảng
function moFormSua(sp) {
  spDangSua.value = sp
  hienForm.value = true
}

// Form báo "save" kèm dữ liệu đã kiểm tra -> gọi service thêm hoặc sửa
async function luuSanPham(duLieu) {
  dangLuu.value = true
  try {
    if (spDangSua.value) {
      await sanPhamService.update(spDangSua.value.id, duLieu)
      toast.success('Đã lưu thay đổi sản phẩm.')
    } else {
      await sanPhamService.create(duLieu)
      toast.success('Đã thêm sản phẩm.')
    }
    hienForm.value = false
    await taiDuLieu(false) // tải lại danh sách để thấy dữ liệu mới
  } catch (loi) {
    toast.error(loi.message || 'Không thể lưu sản phẩm. Vui lòng thử lại.')
  } finally {
    dangLuu.value = false
  }
}
</script>

<template>
  <div class="ad-page">
    <section class="ad-card">
      <header class="ad-list-head">
        <h2 class="ad-card-title">Danh sách sản phẩm</h2>
        <div class="d-flex align-items-center gap-3">
          <span class="ad-count">{{ danhSachSanPham.length }} sản phẩm</span>
          <button type="button" class="ad-btn ad-btn-primary" @click="moFormThem">
            <i class="bi bi-plus-lg" aria-hidden="true"></i> Thêm sản phẩm
          </button>
        </div>
      </header>

      <div class="ad-table-wrap">
        <div class="ad-table-scroll">
          <table class="ad-table">
            <thead>
              <tr>
                <th class="ad-col-stt">STT</th>
                <th>Mã</th>
                <th>Tên sản phẩm</th>
                <th>Danh mục</th>
                <th>Thương hiệu</th>
                <th>Xuất xứ</th>
                <th>Trạng thái</th>
                <th class="ad-col-actions">Hành động</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="dangTai"><td colspan="8"><div class="ad-empty">Đang tải dữ liệu…</div></td></tr>
              <tr v-else-if="!danhSachSanPham.length">
                <td colspan="8">
                  <div class="ad-empty">
                    <i class="bi bi-inbox" aria-hidden="true"></i>
                    <strong>Chưa có sản phẩm</strong>
                    <span>Bấm “Thêm sản phẩm” để tạo sản phẩm đầu tiên.</span>
                  </div>
                </td>
              </tr>
              <template v-else>
                <tr v-for="(p, i) in danhSachSanPham" :key="p.id">
                  <td class="ad-col-stt">{{ i + 1 }}</td>
                  <td><span class="ad-code ad-nowrap">{{ p.ma }}</span></td>
                  <td class="ad-cell-wrap">{{ p.ten }}</td>
                  <td>{{ layTen(thuocTinh['danh-muc'], p.idDanhMuc) }}</td>
                  <td>{{ layTen(thuocTinh['thuong-hieu'], p.idThuongHieu) }}</td>
                  <td>{{ layTen(thuocTinh['xuat-xu'], p.idXuatXu) }}</td>
                  <td><span class="ad-pill" :class="TRANG_THAI[trangThaiOf(p)].cls">{{ TRANG_THAI[trangThaiOf(p)].label }}</span></td>
                  <td class="ad-col-actions">
                    <div class="ad-actions">
                      <button type="button" class="ad-icon-btn" title="Chỉnh sửa" :aria-label="`Chỉnh sửa sản phẩm ${p.ma}`" @click="moFormSua(p)">
                        <i class="bi bi-pencil-square" aria-hidden="true"></i>
                      </button>
                    </div>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>
      </div>
    </section>

    <!-- Form thêm / sửa: spDangSua = null là thêm mới, có giá trị là sửa sản phẩm đó -->
    <SanPhamFormModal
      v-if="hienForm"
      :item="spDangSua"
      :all="danhSachSanPham"
      :thuoc-tinh="thuocTinh"
      :saving="dangLuu"
      @save="luuSanPham"
      @close="hienForm = false"
    />
  </div>
</template>
