<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import FilterCard from './common/FilterCard.vue'
import BasePagination from './common/BasePagination.vue'
import ConfirmDialog from './common/ConfirmDialog.vue'
import StatusSwitch from './common/StatusSwitch.vue'
import { useRouter } from 'vue-router'
import PhieuGiamGiaDetailModal from './phieuGiamGia/PhieuGiamGiaDetailModal.vue'
import { phieuGiamGiaService } from '../services/phieuGiamGiaService'
import { HINH_THUC, LOAI_GIAM, TRANG_THAI, tinhTrangThai, isExpired, formatGiaTri } from '../constants/phieuGiamGia'
import { usePagination } from '../composables/usePagination'
import { useToast } from '../composables/useToast'
import { useAutoRefresh } from '../composables/useAutoRefresh'
import { formatDate, todayIso } from '../utils/format'
import { includesText } from '../utils/text'
import { exportExcel } from '../utils/exportExcel'

const toast = useToast()
const router = useRouter()
const today = todayIso()

const list = ref([])
const loading = ref(false)
const saving = ref(false)

/* ----- Bộ lọc (lọc ngay khi thay đổi) ----- */
const defaultFilters = () => ({
  keyword: '',
  hinhThuc: '',
  ngayBatDau: '',
  ngayKetThuc: '',
  loaiGiam: '',
  trangThai: '',
})
const filters = reactive(defaultFilters())

function resetFilters() {
  Object.assign(filters, defaultFilters())
}

const rows = computed(() => list.value.map((p) => ({ ...p, trangThai: tinhTrangThai(p, today) })))

const filtered = computed(() =>
  rows.value.filter((p) => {
    const f = filters
    if (f.keyword && !includesText(`${p.ma} ${p.ten}`, f.keyword)) return false
    if (f.hinhThuc && p.hinhThuc !== f.hinhThuc) return false
    if (f.loaiGiam && p.loaiGiam !== f.loaiGiam) return false
    if (f.trangThai && p.trangThai !== f.trangThai) return false
    // Ngày bắt đầu / kết thúc ở bộ lọc: phiếu bắt đầu từ ngày... và kết thúc đến hết ngày...
    if (f.ngayBatDau && p.ngayBatDau < f.ngayBatDau) return false
    if (f.ngayKetThuc && p.ngayKetThuc > f.ngayKetThuc) return false
    return true
  }),
)

const { page, pageSize, total, items: pageItems } = usePagination(filtered, 5)
watch(filters, () => {
  page.value = 1
})

/* ----- Tải dữ liệu ----- */
// loadSeq: mỗi lần tải có một số thứ tự. Nếu đã có lần tải / cập nhật mới hơn thì bỏ kết quả của lần cũ,
// để dữ liệu cũ về chậm không đè lên trạng thái ẩn / hiện vừa đổi.
// quiet = true: tải nền (không hiện "Đang tải…", lỗi mạng thì im lặng vì danh sách đang hiển thị vẫn dùng được).
let loadSeq = 0
async function load(showLoading = true, quiet = false) {
  const seq = ++loadSeq
  if (showLoading) loading.value = true
  try {
    const ds = await phieuGiamGiaService.getAll()
    if (seq !== loadSeq) return
    list.value = ds
  } catch {
    if (seq === loadSeq && !(quiet && list.value.length)) toast.error('Không tải được danh sách phiếu giảm giá. Vui lòng thử lại.')
  } finally {
    if (seq === loadSeq) loading.value = false
  }
}
onMounted(load)

// Luôn lấy dữ liệu mới nhất từ server khi: đổi bộ lọc trạng thái (vd sang xem mục đã ẩn) và khi quay lại tab trình duyệt.
watch(() => filters.trangThai, () => load(false, true))
useAutoRefresh(() => load(false, true))

// Đổi trạng thái xong thì cập nhật ngay dòng đó bằng dữ liệu server vừa trả về (không chờ tải lại cả danh sách).
function patchRow(daDoi) {
  if (!daDoi || daDoi.id === undefined) return
  loadSeq++ // hủy kết quả các lần tải đang chạy dở (có thể còn chứa trạng thái cũ)
  const i = list.value.findIndex((x) => x.id === daDoi.id)
  if (i !== -1) list.value[i] = { ...list.value[i], ...daDoi }
}

/* ----- Tạo / sửa / xem chi tiết ----- */
const detailItem = ref(null)

function openCreate() {
  router.push('/phieu-giam-gia/them')
}
// Danh sách không kèm khách hàng được tặng nên lấy chi tiết từ API trước khi mở.
async function openDetail(row) {
  try {
    detailItem.value = { ...(await phieuGiamGiaService.getById(row.id)), trangThai: row.trangThai }
  } catch (e) {
    toast.error(e.message || 'Không tải được chi tiết phiếu giảm giá.')
  }
}
function editFromDetail() {
  const id = detailItem.value.id
  detailItem.value = null
  router.push(`/phieu-giam-gia/${id}/sua`)
}


/* ----- Xuất Excel (đúng theo danh sách đang lọc) ----- */
function exportFile() {
  if (!filtered.value.length) {
    toast.error('Không có phiếu giảm giá nào để xuất.')
    return
  }
  exportExcel({
    filename: `phieu_giam_gia_${todayIso()}.xlsx`,
    sheetName: 'Phiếu giảm giá',
    columns: [
      { header: 'STT', key: 'stt', width: 6 },
      { header: 'Mã', key: 'ma', width: 14 },
      { header: 'Tên phiếu', key: 'ten', width: 36 },
      { header: 'Hình thức', key: 'hinhThuc', width: 12 },
      { header: 'Giá trị giảm', key: 'giaTri', width: 16 },
      { header: 'Giảm tối đa (₫)', key: 'giamToiDa', width: 16 },
      { header: 'Đơn tối thiểu (₫)', key: 'donToiThieu', width: 18 },
      { header: 'Số lượng', key: 'soLuong', width: 10 },
      { header: 'Đã dùng', key: 'soLuongDaDung', width: 10 },
      { header: 'Giới hạn mỗi khách', key: 'gioiHanMoiKhach', width: 18 },
      { header: 'Ngày bắt đầu', key: 'ngayBatDau', width: 14 },
      { header: 'Ngày kết thúc', key: 'ngayKetThuc', width: 14 },
      { header: 'Trạng thái', key: 'trangThai', width: 18 },
    ],
    rows: filtered.value.map((p, i) => ({
      stt: i + 1,
      ma: p.ma,
      ten: p.ten,
      hinhThuc: HINH_THUC[p.hinhThuc].label,
      giaTri: formatGiaTri(p),
      giamToiDa: p.giamToiDa ?? '',
      donToiThieu: p.donToiThieu ?? '',
      soLuong: p.soLuong ?? '',
      soLuongDaDung: p.soLuongDaDung ?? '',
      gioiHanMoiKhach: p.gioiHanMoiKhach ?? '',
      ngayBatDau: formatDate(p.ngayBatDau),
      ngayKetThuc: formatDate(p.ngayKetThuc),
      trangThai: TRANG_THAI[p.trangThai].label,
    })),
  })
  toast.success(`Đã xuất ${filtered.value.length} phiếu giảm giá ra file Excel.`)
}

/* ----- Bật / tắt hoạt động ----- */
const confirmItem = ref(null)
const confirmLoading = ref(false)

const confirmContent = computed(() => {
  const p = confirmItem.value
  if (!p) return {}
  return p.hoatDong
    ? {
        title: 'Ẩn phiếu giảm giá?',
        message: `Phiếu ${p.ma} sẽ được ẩn và không còn áp dụng cho khách hàng. Dữ liệu vẫn được giữ lại, bạn có thể hiện lại bất cứ lúc nào.`,
        confirmText: 'Ẩn phiếu',
        variant: 'danger',
      }
    : {
        title: 'Hiện lại phiếu giảm giá?',
        message: `Phiếu ${p.ma} sẽ áp dụng lại trong khoảng ${formatDate(p.ngayBatDau)} - ${formatDate(p.ngayKetThuc)}.`,
        confirmText: 'Hiện lại',
        variant: 'primary',
      }
})

// Công tắc chỉ hiện khi phiếu chưa kết thúc (hôm nay <= ngày kết thúc). Sang ngày hôm sau của ngày kết thúc thì công tắc biến mất.
function toggleTitle(p) {
  return p.hoatDong ? 'Đang bật - bấm để ngưng hoạt động' : 'Đang tắt - bấm để bật hoạt động'
}

async function confirmToggle() {
  const p = confirmItem.value
  confirmLoading.value = true
  try {
    const daDoi = await phieuGiamGiaService.toggleActive(p.id)
    patchRow(daDoi) // đổi trạng thái ngay trên dòng, không chờ tải lại cả danh sách
    toast.success(p.hoatDong ? `Đã ẩn phiếu ${p.ma}.` : `Đã hiện lại phiếu ${p.ma}.`)
    confirmItem.value = null
    load(false, true) // đồng bộ lại với server ở nền
  } catch (e) {
    toast.error(e.message || 'Không thể cập nhật trạng thái. Vui lòng thử lại.')
  } finally {
    confirmLoading.value = false
  }
}
</script>

<template>
  <div class="ad-page">
    <FilterCard subtitle="Tra cứu nhanh dữ liệu.">
      <div class="ad-filter-grid">
        <div>
          <label class="ad-label" for="pgg-keyword">Tìm kiếm</label>
          <div class="ad-affix">
            <i class="bi bi-search" aria-hidden="true"></i>
            <input
              id="pgg-keyword"
              v-model.trim="filters.keyword"
              type="text"
              class="form-control ad-control"
              placeholder="Mã hoặc tên phiếu"
              autocomplete="off"
            />
          </div>
        </div>

        <div>
          <label class="ad-label" for="pgg-hinh-thuc">Hình thức</label>
          <select id="pgg-hinh-thuc" v-model="filters.hinhThuc" class="form-select ad-control">
            <option value="">Tất cả hình thức</option>
            <option v-for="(item, key) in HINH_THUC" :key="key" :value="key">{{ item.label }}</option>
          </select>
        </div>

        <div>
          <label class="ad-label" for="pgg-tu-ngay">Ngày bắt đầu</label>
          <input id="pgg-tu-ngay" v-model="filters.ngayBatDau" type="date" class="form-control ad-control" />
        </div>

        <div>
          <label class="ad-label" for="pgg-den-ngay">Ngày kết thúc</label>
          <input id="pgg-den-ngay" v-model="filters.ngayKetThuc" type="date" class="form-control ad-control" />
        </div>

        <div>
          <label class="ad-label" for="pgg-loai-giam">Loại giảm</label>
          <select id="pgg-loai-giam" v-model="filters.loaiGiam" class="form-select ad-control">
            <option value="">Tất cả loại giảm</option>
            <option v-for="(item, key) in LOAI_GIAM" :key="key" :value="key">{{ item.label }}</option>
          </select>
        </div>

        <div>
          <label class="ad-label" for="pgg-trang-thai">Trạng thái</label>
          <select id="pgg-trang-thai" v-model="filters.trangThai" class="form-select ad-control">
            <option value="">Tất cả trạng thái</option>
            <option v-for="(item, key) in TRANG_THAI" :key="key" :value="key">{{ item.label }}</option>
          </select>
        </div>
      </div>

      <template #actions>
        <button type="button" class="ad-btn" @click="resetFilters">
          <i class="bi bi-arrow-counterclockwise" aria-hidden="true"></i> Đặt lại bộ lọc
        </button>
        <button type="button" class="ad-btn" @click="exportFile">
          <i class="bi bi-file-earmark-excel" aria-hidden="true"></i> Xuất Excel
        </button>
        <button type="button" class="ad-btn ad-btn-primary" @click="openCreate">
          <i class="bi bi-plus-lg" aria-hidden="true"></i> Tạo phiếu mới
        </button>
      </template>
    </FilterCard>

    <section class="ad-card">
      <header class="ad-list-head">
        <h2 class="ad-card-title">Danh sách phiếu giảm giá</h2>
        <span class="ad-count">{{ total }} bản ghi hiển thị</span>
      </header>

      <div class="ad-table-wrap">
        <div class="ad-table-scroll">
          <table class="ad-table">
            <thead>
              <tr>
                <th class="ad-col-stt">STT</th>
                <th>Mã</th>
                <th>Tên phiếu</th>
                <th>Hình thức</th>
                <th>Giá trị giảm</th>
                <th>Ngày bắt đầu</th>
                <th>Ngày kết thúc</th>
                <th>Trạng thái</th>
                <th class="ad-col-actions">Hành động</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading">
                <td colspan="9"><div class="ad-empty">Đang tải dữ liệu…</div></td>
              </tr>
              <tr v-else-if="!pageItems.length">
                <td colspan="9">
                  <div class="ad-empty">
                    <i class="bi bi-inbox" aria-hidden="true"></i>
                    <strong>Không tìm thấy phiếu giảm giá</strong>
                    <span>Thử đổi từ khóa hoặc bấm “Đặt lại bộ lọc”.</span>
                  </div>
                </td>
              </tr>
              <template v-else>
                <tr v-for="(p, i) in pageItems" :key="p.id">
                  <td class="ad-col-stt">{{ (page - 1) * pageSize + i + 1 }}</td>
                  <td><span class="ad-code ad-nowrap">{{ p.ma }}</span></td>
                  <td class="ad-cell-wrap">{{ p.ten }}</td>
                  <td>
                    <span class="ad-pill" :class="HINH_THUC[p.hinhThuc].cls">
                      <i class="bi" :class="HINH_THUC[p.hinhThuc].icon" aria-hidden="true"></i>
                      {{ HINH_THUC[p.hinhThuc].label }}
                    </span>
                  </td>
                  <td class="ad-nowrap">{{ formatGiaTri(p) }}</td>
                  <td class="ad-nowrap">{{ formatDate(p.ngayBatDau) }}</td>
                  <td class="ad-nowrap">{{ formatDate(p.ngayKetThuc) }}</td>
                  <td>
                    <span class="ad-pill" :class="TRANG_THAI[p.trangThai].cls">{{ TRANG_THAI[p.trangThai].label }}</span>
                  </td>
                  <td class="ad-col-actions">
                    <div class="ad-actions">
                      <span class="ad-switch-slot">
                        <StatusSwitch
                          v-if="!isExpired(p, today)"
                          :on="p.hoatDong"
                          :label="toggleTitle(p)"
                          @toggle="confirmItem = p"
                        />
                      </span>
                      <button
                        type="button"
                        class="ad-icon-btn"
                        title="Xem chi tiết"
                        :aria-label="`Xem chi tiết phiếu ${p.ma}`"
                        @click="openDetail(p)"
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

    <PhieuGiamGiaDetailModal v-if="detailItem" :item="detailItem" @edit="editFromDetail" @close="detailItem = null" />
    <ConfirmDialog
      v-if="confirmItem"
      v-bind="confirmContent"
      :loading="confirmLoading"
      @confirm="confirmToggle"
      @cancel="confirmItem = null"
    />
  </div>
</template>
