<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import FilterCard from './common/FilterCard.vue'
import BasePagination from './common/BasePagination.vue'
import ConfirmDialog from './common/ConfirmDialog.vue'
import SanPhamDetailModal from './sanPham/SanPhamDetailModal.vue'
import { sanPhamService, bienTheService } from '../services/sanPhamService'
import { thuocTinhService } from '../services/thuocTinhService'
import { TRANG_THAI, trangThaiOf } from '../constants/thuocTinh'
import { useToast } from '../composables/useToast'
import { useAutoRefresh } from '../composables/useAutoRefresh'
import { formatMoney, todayIso } from '../utils/format'
import { includesText, layTen } from '../utils/text'
import { exportExcel } from '../utils/exportExcel'

const toast = useToast()
const router = useRouter()

/* ===== 1. DỮ LIỆU CỦA TRANG ===== */

const danhSachSanPham = ref([]) // dữ liệu bảng san_pham
const danhSachBienThe = ref([]) // dữ liệu bảng san_pham_chi_tiet
const dangTai = ref(false) // true: đang tải, hiện chữ "Đang tải dữ liệu…"

// Các bảng thuộc tính (danh mục, thương hiệu, ...) dùng để đổ vào dropdown và đổi id thành tên
const thuocTinh = reactive({
  'danh-muc': [],
  'thuong-hieu': [],
  'xuat-xu': [],
  'chat-lieu': [],
  'do-cung': [],
  'diem-can-bang': [],
  'mau-sac': [],
  'trong-luong': [],
  'chu-vi': [],
})

/* ===== 2. TẢI DỮ LIỆU ===== */

async function taiThuocTinh() {
  // Gọi API lấy 9 bảng thuộc tính cùng lúc rồi gán vào thuocTinh
  const cacLoai = Object.keys(thuocTinh)
  const ketQua = await Promise.all(cacLoai.map((loai) => thuocTinhService.getAll(loai)))
  cacLoai.forEach((loai, i) => {
    thuocTinh[loai] = ketQua[i]
  })
}

// seqTai: mỗi lần tải có một số thứ tự. Nếu đã có lần tải / cập nhật mới hơn thì bỏ kết quả của lần cũ,
// để dữ liệu cũ về chậm không đè lên trạng thái ngưng / kích hoạt vừa đổi.
// imLang = true: tải nền (không hiện "Đang tải…", chỉ lấy lại sản phẩm + biến thể, lỗi mạng thì im lặng).
let seqTai = 0
async function taiDuLieu(hienChuDangTai = true, imLang = false) {
  const seq = ++seqTai
  if (hienChuDangTai) dangTai.value = true
  try {
    // Các lời gọi API độc lập nhau nên chạy song song cho nhanh
    const [sanPham, bienThe] = await Promise.all([sanPhamService.getAll(), bienTheService.getAll(), imLang ? null : taiThuocTinh()])
    if (seq !== seqTai) return
    danhSachSanPham.value = sanPham
    danhSachBienThe.value = bienThe
  } catch (loi) {
    if (seq === seqTai && !(imLang && danhSachSanPham.value.length)) {
      toast.error(loi.message || 'Không tải được danh sách sản phẩm. Vui lòng thử lại.')
    }
  } finally {
    if (seq === seqTai) dangTai.value = false
  }
}

// Đổi trạng thái xong thì cập nhật ngay dòng đó bằng dữ liệu server vừa trả về (không chờ tải lại cả danh sách).
function capNhatDong(daDoi) {
  if (!daDoi || daDoi.id === undefined) return
  seqTai++ // hủy kết quả các lần tải đang chạy dở (có thể còn chứa trạng thái cũ)
  const i = danhSachSanPham.value.findIndex((x) => x.id === daDoi.id)
  if (i !== -1) danhSachSanPham.value[i] = { ...danhSachSanPham.value[i], ...daDoi }
}

// onMounted: chạy 1 lần khi trang vừa hiện ra -> tải dữ liệu
onMounted(() => taiDuLieu())

/* ===== 3. BỘ LỌC ===== */

const filters = reactive({ keyword: '', idDanhMuc: '', idThuongHieu: '', trangThai: '' })

function datLaiBoLoc() {
  filters.keyword = ''
  filters.idDanhMuc = ''
  filters.idThuongHieu = ''
  filters.trangThai = ''
}

// Luôn lấy dữ liệu mới nhất từ server khi: đổi bộ lọc trạng thái (vd sang xem sản phẩm đã ngưng) và khi quay lại tab trình duyệt.
watch(() => filters.trangThai, () => taiDuLieu(false, true))
useAutoRefresh(() => taiDuLieu(false, true))

// Mỗi sản phẩm thêm thông tin tính từ biến thể: số biến thể, tổng tồn kho, giá thấp nhất và cao nhất.
// computed: tự tính lại mỗi khi danhSachSanPham hoặc danhSachBienThe thay đổi.
const sanPhamKemThongKe = computed(() => {
  const ketQua = []
  for (const sp of danhSachSanPham.value) {
    // Lấy các biến thể thuộc sản phẩm này
    const bienTheCuaSp = danhSachBienThe.value.filter((b) => b.idSanPham === sp.id)

    let tongTon = 0
    let giaCaoNhat = null
    let giaThapNhat = null // null = chưa có biến thể nào đang hoạt động
    for (const b of bienTheCuaSp) {
      // Chỉ tính biến thể đang hoạt động (biến thể đã ngưng không còn bán nên không tính vào tồn kho)
      if (b.hoatDong) {
        tongTon = tongTon + b.soLuongTon
        if (giaThapNhat === null || b.giaBan < giaThapNhat) {
          giaThapNhat = b.giaBan
        }
        if (giaCaoNhat === null || b.giaBan > giaCaoNhat) {
          giaCaoNhat = b.giaBan
        }
      }
    }

    ketQua.push({
      ...sp, // giữ nguyên các cột của sản phẩm
      soBienThe: bienTheCuaSp.length,
      tongTon: tongTon,
      giaTu: giaThapNhat,
      giaDen: giaCaoNhat,
    })
  }
  return ketQua
})

// Danh sách sau khi lọc: chỉ giữ sản phẩm khớp TẤT CẢ điều kiện đang chọn
const danhSachDaLoc = computed(() => {
  return sanPhamKemThongKe.value.filter((sp) => {
    // Ô tìm kiếm: tìm theo mã hoặc tên (không phân biệt hoa thường, dấu)
    if (filters.keyword !== '' && !includesText(sp.ma + ' ' + sp.ten, filters.keyword)) return false
    // Danh mục / thương hiệu: '' nghĩa là "Tất cả" nên không lọc
    if (filters.idDanhMuc !== '' && sp.idDanhMuc !== filters.idDanhMuc) return false
    if (filters.idThuongHieu !== '' && sp.idThuongHieu !== filters.idThuongHieu) return false
    if (filters.trangThai !== '' && trangThaiOf(sp) !== filters.trangThai) return false
    return true
  })
})

/* ===== 4. PHÂN TRANG ===== */

const page = ref(1) // trang hiện tại
const pageSize = ref(5) // số dòng mỗi trang
const total = computed(() => danhSachDaLoc.value.length) // tổng số bản ghi sau khi lọc

// Cắt đúng các dòng của trang hiện tại. Ví dụ trang 2, mỗi trang 5 dòng -> lấy dòng 5..9
const dongHienThi = computed(() => {
  const batDau = (page.value - 1) * pageSize.value
  return danhSachDaLoc.value.slice(batDau, batDau + pageSize.value)
})

// Đổi bộ lọc hoặc đổi số dòng/trang thì quay về trang 1
watch(filters, () => {
  page.value = 1
})
watch(pageSize, () => {
  page.value = 1
})

/* ===== 5. THÊM / SỬA / XEM CHI TIẾT ===== */
// Thêm và sửa là trang riêng (SanPhamFormPage.vue), giống video; ở đây chỉ chuyển trang.

const spXemChiTiet = ref(null) // sản phẩm đang xem chi tiết (null: đóng)

// Biến thể của sản phẩm đang xem chi tiết
const bienTheCuaSanPhamDangXem = computed(() => {
  if (spXemChiTiet.value === null) return []
  return danhSachBienThe.value.filter((b) => b.idSanPham === spXemChiTiet.value.id)
})

function moTrangThem() {
  router.push('/san-pham/them')
}

// Bấm "Chỉnh sửa" trong màn chi tiết: chuyển sang trang sửa sản phẩm đó
function suaTuChiTiet() {
  const id = spXemChiTiet.value.id
  spXemChiTiet.value = null
  router.push('/san-pham/' + id + '/sua')
}

// Giá bán hiển thị: một giá, hoặc khoảng giá "từ - đến" khi các biến thể giá khác nhau
function hienGia(sp) {
  if (sp.giaTu === null) return '—'
  if (sp.giaTu === sp.giaDen) return formatMoney(sp.giaTu)
  return formatMoney(sp.giaTu) + ' - ' + formatMoney(sp.giaDen)
}

/* ===== 6. NGƯNG / KÍCH HOẠT LẠI ===== */
// Hệ thống không xóa cứng dữ liệu, chỉ đổi cột trạng thái (hoatDong)

const spCanDoiTrangThai = ref(null) // sản phẩm đang chờ xác nhận
const dangDoiTrangThai = ref(false)

// Nội dung hộp xác nhận thay đổi theo sản phẩm đang hoạt động hay đã ngưng
const noiDungXacNhan = computed(() => {
  const sp = spCanDoiTrangThai.value
  if (sp === null) return {}
  if (sp.hoatDong) {
    return {
      title: 'Ngưng bán sản phẩm?',
      message: 'Sản phẩm ' + sp.ma + ' sẽ không còn hiển thị để bán. Bạn có thể kích hoạt lại sau.',
      confirmText: 'Ngưng hoạt động',
      variant: 'danger',
    }
  }
  return {
    title: 'Kích hoạt lại sản phẩm?',
    message: 'Sản phẩm ' + sp.ma + ' sẽ được bán trở lại.',
    confirmText: 'Kích hoạt',
    variant: 'primary',
  }
})

async function doiTrangThai() {
  const sp = spCanDoiTrangThai.value
  dangDoiTrangThai.value = true
  try {
    const daDoi = await sanPhamService.toggleActive(sp.id)
    capNhatDong(daDoi) // dòng này đổi trạng thái ngay, sang xem sản phẩm đã ngưng là thấy luôn
    if (sp.hoatDong) {
      toast.success('Đã ngưng hoạt động sản phẩm ' + sp.ma + '.')
    } else {
      toast.success('Đã kích hoạt lại sản phẩm ' + sp.ma + '.')
    }
    spCanDoiTrangThai.value = null
    taiDuLieu(false, true) // đồng bộ lại với server ở nền
  } catch (loi) {
    toast.error(loi.message || 'Không thể cập nhật trạng thái. Vui lòng thử lại.')
  } finally {
    dangDoiTrangThai.value = false
  }
}

/* ===== 7. XUẤT EXCEL ===== */
// Xuất các sản phẩm đang hiển thị theo bộ lọc (không chỉ trang hiện tại)

function xuatExcel() {
  if (danhSachDaLoc.value.length === 0) {
    toast.error('Không có sản phẩm nào để xuất.')
    return
  }

  // Mỗi sản phẩm -> 1 dòng Excel
  const cacDong = []
  danhSachDaLoc.value.forEach((sp, index) => {
    cacDong.push({
      stt: index + 1,
      ma: sp.ma,
      ten: sp.ten,
      danhMuc: layTen(thuocTinh['danh-muc'], sp.idDanhMuc),
      thuongHieu: layTen(thuocTinh['thuong-hieu'], sp.idThuongHieu),
      xuatXu: layTen(thuocTinh['xuat-xu'], sp.idXuatXu),
      chatLieu: layTen(thuocTinh['chat-lieu'], sp.idChatLieu),
      doCung: layTen(thuocTinh['do-cung'], sp.idDoCung),
      diemCanBang: layTen(thuocTinh['diem-can-bang'], sp.idDiemCanBang),
      soBienThe: sp.soBienThe,
      tongTon: sp.tongTon,
      giaTu: sp.giaTu,
      trangThai: TRANG_THAI[trangThaiOf(sp)].label,
    })
  })

  exportExcel({
    filename: 'san-pham_' + todayIso() + '.xlsx',
    sheetName: 'Sản phẩm',
    columns: [
      { header: 'STT', key: 'stt', width: 6 },
      { header: 'Mã', key: 'ma', width: 12 },
      { header: 'Tên sản phẩm', key: 'ten', width: 36 },
      { header: 'Danh mục', key: 'danhMuc', width: 18 },
      { header: 'Thương hiệu', key: 'thuongHieu', width: 16 },
      { header: 'Xuất xứ', key: 'xuatXu', width: 14 },
      { header: 'Chất liệu', key: 'chatLieu', width: 14 },
      { header: 'Độ cứng', key: 'doCung', width: 12 },
      { header: 'Điểm cân bằng', key: 'diemCanBang', width: 16 },
      { header: 'Số biến thể', key: 'soBienThe', width: 12 },
      { header: 'Tổng tồn', key: 'tongTon', width: 10 },
      { header: 'Giá từ (₫)', key: 'giaTu', width: 14 },
      { header: 'Trạng thái', key: 'trangThai', width: 18 },
    ],
    rows: cacDong,
  })
  toast.success('Đã xuất ' + cacDong.length + ' sản phẩm ra file Excel.')
}
</script>

<template>
  <div class="ad-page">
    <FilterCard>
      <div class="ad-filter-top">
        <div class="ad-filter-top-search">
          <label class="ad-label" for="sp-keyword">Tìm kiếm</label>
          <div class="ad-affix">
            <i class="bi bi-search" aria-hidden="true"></i>
            <input id="sp-keyword" v-model.trim="filters.keyword" type="text" class="form-control ad-control" placeholder="Tìm theo mã SP / tên sản phẩm…" autocomplete="off" />
          </div>
        </div>
        <div class="ad-filter-top-btns">
          <button type="button" class="ad-btn" @click="datLaiBoLoc"><i class="bi bi-arrow-counterclockwise" aria-hidden="true"></i> Đặt lại bộ lọc</button>
          <button type="button" class="ad-btn" @click="xuatExcel"><i class="bi bi-file-earmark-excel" aria-hidden="true"></i> Xuất Excel</button>
          <button type="button" class="ad-btn ad-btn-primary" @click="moTrangThem"><i class="bi bi-plus-lg" aria-hidden="true"></i> Thêm sản phẩm</button>
        </div>
      </div>

      <div class="ad-filter-grid ad-filter-grid--3">
        <div>
          <label class="ad-label" for="sp-f-thuong-hieu">Thương hiệu</label>
          <select id="sp-f-thuong-hieu" v-model="filters.idThuongHieu" class="form-select ad-control">
            <option value="">Tất cả thương hiệu</option>
            <option v-for="o in thuocTinh['thuong-hieu']" :key="o.id" :value="o.id">{{ o.ten }}</option>
          </select>
        </div>
        <div>
          <label class="ad-label" for="sp-f-danh-muc">Danh mục</label>
          <select id="sp-f-danh-muc" v-model="filters.idDanhMuc" class="form-select ad-control">
            <option value="">Tất cả danh mục</option>
            <option v-for="o in thuocTinh['danh-muc']" :key="o.id" :value="o.id">{{ o.ten }}</option>
          </select>
        </div>
        <div>
          <label class="ad-label" for="sp-f-trang-thai">Trạng thái</label>
          <select id="sp-f-trang-thai" v-model="filters.trangThai" class="form-select ad-control">
            <option value="">Tất cả trạng thái</option>
            <option v-for="(item, key) in TRANG_THAI" :key="key" :value="key">{{ item.label }}</option>
          </select>
        </div>
      </div>
    </FilterCard>

    <section class="ad-card">
      <header class="ad-list-head">
        <h2 class="ad-card-title">Danh sách sản phẩm</h2>
        <span class="ad-count">{{ total }} bản ghi hiển thị</span>
      </header>

      <div class="ad-table-wrap">
        <div class="ad-table-scroll">
          <table class="ad-table">
            <thead>
              <tr>
                <th class="ad-col-stt">STT</th>
                <th>Mã SP</th>
                <th>Tên SP</th>
                <th>Thương hiệu</th>
                <th>Danh mục</th>
                <th>Số lượng</th>
                <th>Giá bán</th>
                <th>Trạng thái</th>
                <th class="ad-col-actions">Hành động</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="dangTai"><td colspan="9"><div class="ad-empty">Đang tải dữ liệu…</div></td></tr>
              <tr v-else-if="!dongHienThi.length">
                <td colspan="9">
                  <div class="ad-empty">
                    <i class="bi bi-inbox" aria-hidden="true"></i>
                    <strong>Không tìm thấy sản phẩm</strong>
                    <span>Thử đổi từ khóa hoặc bấm “Đặt lại bộ lọc”.</span>
                  </div>
                </td>
              </tr>
              <template v-else>
                <tr v-for="(p, i) in dongHienThi" :key="p.id">
                  <td class="ad-col-stt">{{ (page - 1) * pageSize + i + 1 }}</td>
                  <td><span class="ad-code ad-nowrap">{{ p.ma }}</span></td>
                  <td class="ad-cell-wrap">{{ p.ten }}</td>
                  <td>{{ layTen(thuocTinh['thuong-hieu'], p.idThuongHieu) }}</td>
                  <td>{{ layTen(thuocTinh['danh-muc'], p.idDanhMuc) }}</td>
                  <td>{{ p.tongTon }}</td>
                  <td class="ad-nowrap">{{ hienGia(p) }}</td>
                  <td><span class="ad-pill" :class="TRANG_THAI[trangThaiOf(p)].cls">{{ TRANG_THAI[trangThaiOf(p)].label }}</span></td>
                  <td class="ad-col-actions">
                    <div class="ad-actions">
                      <button
                        type="button"
                        class="ad-icon-btn"
                        :class="p.hoatDong ? 'is-danger' : 'is-success'"
                        :title="p.hoatDong ? 'Ngưng hoạt động' : 'Kích hoạt lại'"
                        :aria-label="`${p.hoatDong ? 'Ngưng hoạt động' : 'Kích hoạt lại'} sản phẩm ${p.ma}`"
                        @click="spCanDoiTrangThai = p"
                      >
                        <i class="bi bi-power" aria-hidden="true"></i>
                      </button>
                      <button type="button" class="ad-icon-btn" title="Xem chi tiết" :aria-label="`Xem chi tiết sản phẩm ${p.ma}`" @click="spXemChiTiet = p">
                        <i class="bi bi-eye" aria-hidden="true"></i>
                      </button>
                    </div>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>
      </div>

      <BasePagination v-if="total" v-model:page="page" v-model:pageSize="pageSize" :total="total" />
    </section>

    <!-- Xem chi tiết kèm danh sách biến thể của sản phẩm -->
    <SanPhamDetailModal
      v-if="spXemChiTiet"
      :item="spXemChiTiet"
      :bien-thes="bienTheCuaSanPhamDangXem"
      :thuoc-tinh="thuocTinh"
      @edit="suaTuChiTiet"
      @close="spXemChiTiet = null"
    />
    <!-- Hộp xác nhận ngưng / kích hoạt -->
    <ConfirmDialog
      v-if="spCanDoiTrangThai"
      v-bind="noiDungXacNhan"
      :loading="dangDoiTrangThai"
      @confirm="doiTrangThai"
      @cancel="spCanDoiTrangThai = null"
    />
  </div>
</template>
