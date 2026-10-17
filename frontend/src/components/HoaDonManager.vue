<script setup>
import { onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import api from '../services/api'
import FilterCard from './common/FilterCard.vue'
import BasePagination from './common/BasePagination.vue'
import { useToast } from '../composables/useToast'
import { useAutoRefresh } from '../composables/useAutoRefresh'
import { formatMoney, todayIso } from '../utils/format'
import { exportExcel } from '../utils/exportExcel'
import DateInput from './common/DateInput.vue'
import Select2 from './common/Select2.vue'
import { TAB_TRANG_THAI, trangThaiOf } from '../constants/hoaDon'

const router = useRouter()
const toast = useToast()
const dsLoaiDon = [
  { value: '', label: 'Tất cả' },
  { value: '1', label: 'Tại quầy' },
  { value: '2', label: 'Trực tuyến' },
]

/* ----- Trạng thái hóa đơn: nhãn + màu nhãn dùng chung với trang chi tiết (constants/hoaDon.js) ----- */
const TABS = TAB_TRANG_THAI

/* ----- Bộ lọc: đổi là tự lọc, không cần bấm Tìm kiếm ----- */
// Từ ngày / Đến ngày mặc định là NGÀY HIỆN TẠI (theo giờ máy): mở trang thấy ngay hóa đơn hôm nay;
// "Đặt lại bộ lọc" cũng đưa về hôm nay. Muốn xem mọi ngày thì xóa trống hai ô ngày.
// Ngày hiện tại được cập nhật liên tục: để trang mở qua 0h (hoặc máy ngủ rồi mở lại) thì ô nào đang
// là "hôm nay" tự nhảy sang ngày mới và danh sách tải lại; ô người dùng đã tự chọn ngày khác thì giữ nguyên.
const defaultFilters = () => {
  const homNay = todayIso()
  return { maHoaDon: '', tuNgay: homNay, denNgay: homNay, loaiDon: '' }
}
const filters = reactive(defaultFilters())
let homNayDangDung = filters.tuNgay
const KIEM_TRA_NGAY_MS = 30 * 1000

/** Qua ngày mới thì đưa các ô đang là "hôm nay" sang ngày mới. Trả về true nếu bộ lọc có đổi (watch sẽ tự tải lại). */
function capNhatNgayHienTai() {
  const moi = todayIso()
  if (moi === homNayDangDung) return false
  const cu = homNayDangDung
  homNayDangDung = moi
  let doi = false
  if (filters.tuNgay === cu) { filters.tuNgay = moi; doi = true }
  if (filters.denNgay === cu) { filters.denNgay = moi; doi = true }
  return doi
}
const activeTab = ref(null)

const list = ref([])
const dem = ref({}) // số hóa đơn theo từng tab: { tatCa, 0..6 } (áp cùng bộ lọc, trừ tab trạng thái)
const loading = ref(false)
const page = ref(1)
const pageSize = ref(5)
const total = ref(0)

function buildParams(pageIndex, size) {
  return {
    page: pageIndex,
    size,
    maHoaDon: filters.maHoaDon.trim() || null,
    tuNgay: filters.tuNgay || null,
    denNgay: filters.denNgay || null,
    loaiDon: filters.loaiDon !== '' ? filters.loaiDon : null,
    trangThai: activeTab.value,
  }
}

// Số hóa đơn hiện trên từng tab (lỗi thì giữ số cũ, không làm hỏng trang)
let demSeq = 0
async function loadDem() {
  const seq = ++demSeq
  try {
    const { data } = await api.get('/hoa-don/dem-trang-thai', {
      params: {
        maHoaDon: filters.maHoaDon.trim() || null,
        tuNgay: filters.tuNgay || null,
        denNgay: filters.denNgay || null,
        loaiDon: filters.loaiDon !== '' ? filters.loaiDon : null,
      },
    })
    if (seq === demSeq) dem.value = data || {}
  } catch {
    // bỏ qua
  }
}
const demCua = (tab) => dem.value[tab.value === null ? 'tatCa' : String(tab.value)]

// loadSeq: lần tải cũ về chậm không được ghi đè kết quả của lần tải mới hơn
let loadSeq = 0
async function load(quiet = false) {
  const seq = ++loadSeq
  if (!quiet) loading.value = true
  loadDem()
  try {
    const { data } = await api.get('/hoa-don', { params: buildParams(page.value - 1, pageSize.value) })
    if (seq !== loadSeq) return
    list.value = data.content
    total.value = data.totalElements
    // Đang ở trang không còn dữ liệu (vd lọc ra ít hơn) thì lùi về trang cuối có dữ liệu
    const trangCuoi = Math.max(1, Math.ceil(data.totalElements / pageSize.value))
    if (page.value > trangCuoi) page.value = trangCuoi
  } catch (e) {
    // Lỗi bộ lọc (vd Từ ngày sau Đến ngày khi gõ tay) thì hiện đúng lý do backend trả về
    if (seq === loadSeq && !quiet) toast.error(e?.response?.data?.message || 'Không tải được danh sách hóa đơn. Vui lòng thử lại.')
  } finally {
    if (seq === loadSeq) loading.value = false
  }
}

// Đổi bộ lọc / tab: về trang 1 rồi tải lại. Ô mã hóa đơn chờ 300ms sau khi ngừng gõ.
let timer = null
watch(
  () => [filters.maHoaDon, filters.tuNgay, filters.denNgay, filters.loaiDon, activeTab.value],
  (moi, cu) => {
    clearTimeout(timer)
    const chiDoiMa = moi[0] !== cu[0] && moi.slice(1).every((v, i) => v === cu[i + 1])
    const chay = () => {
      if (page.value !== 1) page.value = 1 // đổi trang sẽ tự tải lại
      else load()
    }
    if (chiDoiMa) timer = setTimeout(chay, 300)
    else chay()
  },
)
watch([page, pageSize], () => load())
let dongHoNgay = null
onBeforeUnmount(() => {
  clearTimeout(timer)
  clearInterval(dongHoNgay)
})
onMounted(() => {
  load()
  dongHoNgay = setInterval(capNhatNgayHienTai, KIEM_TRA_NGAY_MS)
})
// Quay lại tab: kiểm tra ngày trước (đổi ngày thì watch tải lại), không đổi thì tải lại lặng lẽ như cũ
useAutoRefresh(() => {
  if (!capNhatNgayHienTai()) load(true)
})

const resetFilters = () => {
  Object.assign(filters, defaultFilters())
  homNayDangDung = filters.tuNgay
}

const xemChiTiet = (id) => router.push(`/hoa-don/${id}`)

/* ----- Hiển thị ----- */
const formatNgay = (v) => {
  if (!v) return ''
  const d = new Date(v)
  return `${d.getDate()}/${d.getMonth() + 1}/${d.getFullYear()}`
}
const tenKhach = (hd) => hd.tenKhachHang || 'Khách vãng lai'

/* ----- Xuất Excel: toàn bộ hóa đơn khớp bộ lọc hiện tại (không chỉ trang đang xem) ----- */
const exporting = ref(false)
async function exportFile() {
  if (!total.value) {
    toast.error('Không có hóa đơn nào để xuất.')
    return
  }
  exporting.value = true
  try {
    const rows = []
    const size = 100 // backend cho tối đa 100 dòng mỗi lần
    const soTrang = Math.ceil(total.value / size)
    for (let p = 0; p < soTrang; p++) {
      const { data } = await api.get('/hoa-don', { params: buildParams(p, size) })
      rows.push(...data.content)
    }
    exportExcel({
      filename: `hoa_don_${todayIso()}.xlsx`,
      sheetName: 'Hóa đơn',
      columns: [
        { header: 'STT', key: 'stt', width: 6 },
        { header: 'Mã HD', key: 'ma', width: 14 },
        { header: 'Mã NV', key: 'maNv', width: 12 },
        { header: 'Tên KH', key: 'tenKh', width: 24 },
        { header: 'SĐT KH', key: 'sdt', width: 14 },
        { header: 'Tổng tiền TT (₫)', key: 'tien', width: 18, money: true },
        { header: 'Loại đơn', key: 'loai', width: 12 },
        { header: 'Ngày tạo', key: 'ngay', width: 12 },
        { header: 'Trạng thái', key: 'trangThai', width: 16 },
      ],
      rows: rows.map((hd, i) => ({
        stt: i + 1,
        ma: hd.maHoaDon,
        maNv: hd.maNhanVien || '',
        tenKh: tenKhach(hd),
        sdt: hd.sdtKhachHang || '',
        tien: Number(hd.tongTien) || 0, // để dạng SỐ để Excel cộng / lọc được
        loai: hd.loaiDon,
        ngay: formatNgay(hd.ngayTao),
        trangThai: trangThaiOf(hd.trangThai).label,
      })),
    })
    toast.success(`Đã xuất ${rows.length} hóa đơn ra file Excel.`)
  } catch {
    toast.error('Không thể xuất file. Vui lòng thử lại.')
  } finally {
    exporting.value = false
  }
}
</script>

<template>
  <div class="ad-page">
    <FilterCard title="Bộ lọc">
      <div class="ad-filter-grid ad-filter-grid--hd">
        <div>
          <label class="ad-label" for="hd-ma">Mã hóa đơn</label>
          <input id="hd-ma" v-model="filters.maHoaDon" type="text" class="form-control ad-control" autocomplete="off" />
        </div>
        <div>
          <label class="ad-label" for="hd-tu-ngay">Từ ngày</label>
          <DateInput id="hd-tu-ngay" v-model="filters.tuNgay" class="form-control ad-control" :max="filters.denNgay || undefined" />
        </div>
        <div>
          <label class="ad-label" for="hd-den-ngay">Đến ngày</label>
          <DateInput id="hd-den-ngay" v-model="filters.denNgay" class="form-control ad-control" :min="filters.tuNgay || undefined" />
        </div>
        <div>
          <label class="ad-label" for="hd-loai">Loại đơn</label>
          <Select2 id="hd-loai" v-model="filters.loaiDon" :options="dsLoaiDon" class="ad-control" />
        </div>
        <div class="ad-filter-reset">
          <button type="button" class="ad-btn" @click="resetFilters">
            <i class="bi bi-arrow-counterclockwise" aria-hidden="true"></i> Đặt lại bộ lọc
          </button>
        </div>
      </div>
    </FilterCard>

    <section class="ad-card">
      <header class="ad-list-head">
        <h2 class="ad-card-title">Danh sách hóa đơn</h2>
        <button type="button" class="ad-btn ad-btn-primary" :disabled="exporting" @click="exportFile">
          <span v-if="exporting" class="spinner-border spinner-border-sm" aria-hidden="true"></span>
          <i v-else class="bi bi-box-arrow-up" aria-hidden="true"></i>
          Xuất Excel
        </button>
      </header>

      <div class="ad-status-tabs" role="tablist" aria-label="Lọc theo trạng thái hóa đơn">
        <button
          v-for="t in TABS"
          :key="String(t.value)"
          type="button"
          role="tab"
          class="ad-status-tab"
          :class="{ active: activeTab === t.value }"
          :aria-selected="activeTab === t.value"
          @click="activeTab = t.value"
        >
          {{ t.label }}
          <span v-if="demCua(t) !== undefined" class="ad-status-tab-count">{{ demCua(t) }}</span>
        </button>
      </div>

      <div class="ad-table-wrap">
        <div class="ad-table-scroll">
          <table class="ad-table">
            <thead>
              <tr>
                <th class="ad-col-stt">STT</th>
                <th>Mã HD</th>
                <th>Mã NV</th>
                <th>Tên KH</th>
                <th>SĐT KH</th>
                <th>Tổng tiền TT</th>
                <th>Loại đơn</th>
                <th>Ngày tạo</th>
                <th>Trạng thái</th>
                <th class="ad-col-actions">Hành động</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading">
                <td colspan="10">
                  <div class="ad-empty">
                    <span class="spinner-border spinner-border-sm" aria-hidden="true"></span> Đang tải dữ liệu...
                  </div>
                </td>
              </tr>
              <tr v-else-if="!list.length">
                <td colspan="10">
                  <div class="ad-empty">Không tìm thấy hóa đơn nào phù hợp với điều kiện lọc.</div>
                </td>
              </tr>
              <template v-else>
                <tr v-for="(hd, i) in list" :key="hd.id">
                  <td class="ad-col-stt">{{ (page - 1) * pageSize + i + 1 }}</td>
                  <td><span class="ad-code ad-nowrap">{{ hd.maHoaDon }}</span></td>
                  <td class="ad-nowrap">{{ hd.maNhanVien || '—' }}</td>
                  <td class="ad-cell-wrap">{{ tenKhach(hd) }}</td>
                  <td class="ad-nowrap">{{ hd.sdtKhachHang || '—' }}</td>
                  <td class="ad-nowrap hd-money">{{ formatMoney(hd.tongTien) }}</td>
                  <td class="ad-nowrap">{{ hd.loaiDon }}</td>
                  <td class="ad-nowrap">{{ formatNgay(hd.ngayTao) }}</td>
                  <td>
                    <span class="ad-pill" :class="trangThaiOf(hd.trangThai).pill">{{ trangThaiOf(hd.trangThai).label }}</span>
                  </td>
                  <td class="ad-col-actions">
                    <div class="ad-actions">
                      <button
                        type="button"
                        class="ad-icon-btn"
                        title="Xem chi tiết"
                        :aria-label="`Xem chi tiết hóa đơn ${hd.maHoaDon}`"
                        @click="xemChiTiet(hd.id)"
                      >
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

      <BasePagination v-if="total" v-model:page="page" :page-size="pageSize" :total="total" />
    </section>
  </div>
</template>

<style scoped>
.hd-money {
  color: #c62836;
  font-weight: 700;
}
</style>
