<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import FilterCard from './common/FilterCard.vue'
import BasePagination from './common/BasePagination.vue'
import ConfirmDialog from './common/ConfirmDialog.vue'
import PhieuGiamGiaFormModal from './phieuGiamGia/PhieuGiamGiaFormModal.vue'
import PhieuGiamGiaDetailModal from './phieuGiamGia/PhieuGiamGiaDetailModal.vue'
import { phieuGiamGiaService } from '../services/phieuGiamGiaService'
import { HINH_THUC, LOAI_GIAM, TRANG_THAI, tinhTrangThai, isExpired, formatGiaTri } from '../constants/phieuGiamGia'
import { usePagination } from '../composables/usePagination'
import { useToast } from '../composables/useToast'
import { formatDate, todayIso } from '../utils/format'
import { includesText } from '../utils/text'
import { exportExcel } from '../utils/exportExcel'

const toast = useToast()
const today = todayIso()

const list = ref([])
const loading = ref(false)
const saving = ref(false)


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
    if (f.ngayBatDau && p.ngayBatDau < f.ngayBatDau) return false
    if (f.ngayKetThuc && p.ngayKetThuc > f.ngayKetThuc) return false
    return true
  }),
)

const { page, pageSize, total, items: pageItems } = usePagination(filtered, 5)
watch(filters, () => {
  page.value = 1
})


async function load(showLoading = true) {
  if (showLoading) loading.value = true
  try {
    list.value = await phieuGiamGiaService.getAll()
  } catch {
    toast.error('Không tải được danh sách phiếu giảm giá. Vui lòng thử lại.')
  } finally {
    loading.value = false
  }
}
onMounted(load)


const formState = ref(null)
const detailItem = ref(null)

function openCreate() {
  formState.value = { item: null }
}
async function openDetail(row) {
  try {
    detailItem.value = { ...(await phieuGiamGiaService.getById(row.id)), trangThai: row.trangThai }
  } catch (e) {
    toast.error(e.message || 'Không tải được chi tiết phiếu giảm giá.')
  }
}
function editFromDetail() {
  formState.value = { item: detailItem.value }
  detailItem.value = null
}

async function save(payload) {
  const editing = formState.value?.item
  saving.value = true
  try {
    if (editing) {
      await phieuGiamGiaService.update(editing.id, payload)
      toast.success('Đã lưu thay đổi phiếu giảm giá.')
    } else {
      await phieuGiamGiaService.create(payload)
      toast.success('Đã tạo phiếu giảm giá.')
    }
    formState.value = null
    await load(false)
  } catch (e) {
    toast.error(e.message || 'Không thể lưu phiếu giảm giá. Vui lòng thử lại.')
  } finally {
    saving.value = false
  }
}


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

function toggleTitle(p) {
  if (isExpired(p, today)) return 'Phiếu đã kết thúc'
  return p.hoatDong ? 'Ẩn phiếu' : 'Hiện lại phiếu'
}

async function confirmToggle() {
  const p = confirmItem.value
  confirmLoading.value = true
  try {
    await phieuGiamGiaService.toggleActive(p.id)
    toast.success(p.hoatDong ? `Đã ẩn phiếu ${p.ma}.` : `Đã hiện lại phiếu ${p.ma}.`)
    confirmItem.value = null
    await load(false)
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
                      <button
                        type="button"
                        class="ad-icon-btn"
                        :class="p.hoatDong ? 'is-danger' : 'is-success'"
                        :disabled="isExpired(p, today)"
                        :title="toggleTitle(p)"
                        :aria-label="`${toggleTitle(p)} phiếu ${p.ma}`"
                        @click="confirmItem = p"
                      >
                        <i class="bi" :class="p.hoatDong ? 'bi-eye-slash' : 'bi-arrow-counterclockwise'" aria-hidden="true"></i>
                      </button>
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

      <BasePagination v-if="total" v-model:page="page" v-model:pageSize="pageSize" :total="total" />
    </section>

    <PhieuGiamGiaFormModal
      v-if="formState"
      :item="formState.item"
      :all="list"
      :saving="saving"
      @save="save"
      @close="formState = null"
    />
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
