<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import FilterCard from './common/FilterCard.vue'
import BasePagination from './common/BasePagination.vue'
import ConfirmDialog from './common/ConfirmDialog.vue'
import StatusSwitch from './common/StatusSwitch.vue'
import Select2 from './common/Select2.vue'
import { dotGiamGiaService } from '../services/dotGiamGiaService'
import { TRANG_THAI, dangHoatDong, daKetThuc, ngayIso, tinhTrangThai } from '../constants/dotGiamGia'
import { usePagination } from '../composables/usePagination'
import { useToast } from '../composables/useToast'
import { useAutoRefresh } from '../composables/useAutoRefresh'
import { todayIso } from '../utils/format'
import { bayGioDayDu, dinhDangNgayGio } from '../utils/ngayGio'
import { includesText } from '../utils/text'
import { exportExcel } from '../utils/exportExcel'
import { tuBangHang } from '../utils/luaChon'
import DateInput from './common/DateInput.vue'

/*
 * Danh sách đợt giảm giá (giống video): bộ lọc + bảng STT / Mã / Tên / Giá trị / Ngày giờ bắt đầu /
 * Ngày giờ kết thúc / Trạng thái / Hành động (bật-tắt, xem chi tiết).
 * - "Tạo đợt giảm giá" mở trang riêng /dot-giam-gia/them. Đợt mới nằm trên cùng (backend trả mới nhất trước).
 * - Hệ thống không xóa dữ liệu: nút nguồn chỉ bật / tắt hoạt động.
 * - Trạng thái tính theo NGÀY GIỜ và chạy theo đồng hồ: tới giờ bắt đầu / kết thúc thì tự đổi, không cần tải lại trang.
 */
const router = useRouter()
const toast = useToast()
const bayGio = ref(bayGioDayDu())
let dongHo = null
onMounted(() => {
  dongHo = setInterval(() => (bayGio.value = bayGioDayDu()), 30000)
})
onBeforeUnmount(() => clearInterval(dongHo))
const dsTrangThaiLoc = tuBangHang(TRANG_THAI, { tatCa: 'Tất cả trạng thái' })

const list = ref([])
const loading = ref(false)

/* ----- Bộ lọc (lọc ngay khi thay đổi) ----- */
const defaultFilters = () => ({ keyword: '', ngayBatDau: '', ngayKetThuc: '', trangThai: '' })
const filters = reactive(defaultFilters())
const resetFilters = () => Object.assign(filters, defaultFilters())

// 'yyyy-mm-ddTHH:mm:ss' -> 'dd/mm/yyyy HH:mm'
const formatNgay = dinhDangNgayGio

const filtered = computed(() =>
  list.value.filter((d) => {
    const f = filters
    if (f.keyword && !includesText(`${d.maDot} ${d.tenDot} ${d.phanTramGiamDot}%`, f.keyword)) return false
    if (f.trangThai && tinhTrangThai(d, bayGio.value) !== f.trangThai) return false
    // Ngày bắt đầu / kết thúc ở bộ lọc (so theo NGÀY): đợt bắt đầu từ ngày... và kết thúc đến hết ngày...
    if (f.ngayBatDau && ngayIso(d.ngayBatDau) < f.ngayBatDau) return false
    if (f.ngayKetThuc && ngayIso(d.ngayKetThuc) > f.ngayKetThuc) return false
    return true
  }),
)

const { page, pageSize, total, items: pageItems } = usePagination(filtered, 5)
watch(filters, () => {
  page.value = 1
})

/* ----- Tải dữ liệu ----- */
let loadSeq = 0
async function load(showLoading = true, quiet = false) {
  const seq = ++loadSeq
  if (showLoading) loading.value = true
  try {
    const ds = await dotGiamGiaService.getAll()
    if (seq !== loadSeq) return
    list.value = ds
  } catch (e) {
    if (seq === loadSeq && !(quiet && list.value.length)) toast.error(e.message || 'Không tải được danh sách đợt giảm giá. Vui lòng thử lại.')
  } finally {
    if (seq === loadSeq) loading.value = false
  }
}
onMounted(load)
useAutoRefresh(() => load(false, true))

function patchRow(daDoi) {
  if (!daDoi || daDoi.id === undefined) return
  loadSeq++
  const i = list.value.findIndex((x) => x.id === daDoi.id)
  if (i !== -1) list.value[i] = { ...list.value[i], ...daDoi }
}

/* ----- Điều hướng ----- */
const openCreate = () => router.push('/dot-giam-gia/them')
const openDetail = (d) => router.push(`/dot-giam-gia/${d.id}`)

/* ----- Xuất Excel (đúng theo danh sách đang lọc) ----- */
function exportFile() {
  if (!filtered.value.length) {
    toast.error('Không có đợt giảm giá nào để xuất.')
    return
  }
  exportExcel({
    filename: `dot_giam_gia_${todayIso()}.xlsx`,
    sheetName: 'Đợt giảm giá',
    columns: [
      { header: 'STT', key: 'stt', width: 6 },
      { header: 'Mã', key: 'ma', width: 16 },
      { header: 'Tên', key: 'ten', width: 36 },
      { header: 'Giá trị giảm', key: 'giaTri', width: 14 },
      { header: 'Ngày giờ bắt đầu', key: 'ngayBatDau', width: 18 },
      { header: 'Ngày giờ kết thúc', key: 'ngayKetThuc', width: 18 },
      { header: 'Trạng thái', key: 'trangThai', width: 18 },
      { header: 'Mô tả', key: 'moTa', width: 36 },
    ],
    rows: filtered.value.map((d, i) => ({
      stt: i + 1,
      ma: d.maDot,
      ten: d.tenDot,
      giaTri: `${d.phanTramGiamDot}%`,
      ngayBatDau: formatNgay(d.ngayBatDau),
      ngayKetThuc: formatNgay(d.ngayKetThuc),
      trangThai: TRANG_THAI[tinhTrangThai(d, bayGio.value)].label,
      moTa: d.moTa ?? '',
    })),
  })
  toast.success(`Đã xuất ${filtered.value.length} đợt giảm giá ra file Excel.`)
}

/* ----- Công tắc bật / tắt ----- */
// Đợt đã quá giờ kết thúc ("Đã kết thúc"): công tắc hiện TẮT và bị khóa, không bật / tắt được nữa.
const congTacBat = (d) => dangHoatDong(d) && !daKetThuc(d, bayGio.value)
const congTacKhoa = (d) => daKetThuc(d, bayGio.value)
function congTacNhan(d) {
  if (daKetThuc(d, bayGio.value)) return 'Đã kết thúc - công tắc tắt, không thể bật / tắt'
  return dangHoatDong(d) ? 'Đang bật - bấm để ngừng hoạt động' : 'Đang tắt - bấm để bật hoạt động'
}

/* ----- Bật / tắt hoạt động ----- */
const confirmItem = ref(null)
const confirmLoading = ref(false)

const confirmContent = computed(() => {
  const d = confirmItem.value
  if (!d) return {}
  return dangHoatDong(d)
    ? {
        title: 'Ngừng hoạt động đợt giảm giá?',
        message: `Đợt ${d.maDot} (${d.tenDot}) sẽ ngừng áp dụng giảm giá cho các sản phẩm. Dữ liệu vẫn được giữ lại, bạn có thể bật lại bất cứ lúc nào.`,
        confirmText: 'Ngừng hoạt động',
        variant: 'danger',
        icon: 'warning',
      }
    : {
        title: 'Bật hoạt động đợt giảm giá?',
        message: `Đợt ${d.maDot} (${d.tenDot}) sẽ áp dụng lại từ ${formatNgay(d.ngayBatDau)} đến ${formatNgay(d.ngayKetThuc)}.`,
        confirmText: 'Bật hoạt động',
        variant: 'primary',
        icon: 'success',
      }
})

async function confirmToggle() {
  const d = confirmItem.value
  if (!d) return
  confirmLoading.value = true
  try {
    const daDoi = await dotGiamGiaService.toggleActive(d.id)
    patchRow(daDoi)
    toast.success(dangHoatDong(d) ? `Đã ngừng hoạt động đợt ${d.maDot}.` : `Đã bật hoạt động đợt ${d.maDot}.`)
    confirmItem.value = null
    load(false, true)
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
      <div class="ad-filter-grid ad-filter-grid--dot">
        <div>
          <label class="ad-label" for="dgg-keyword">Tìm kiếm</label>
          <div class="ad-affix">
            <i class="bi bi-search" aria-hidden="true"></i>
            <input
              id="dgg-keyword"
              v-model.trim="filters.keyword"
              type="text"
              class="form-control ad-control"
              placeholder="Mã, tên, giá trị..."
              autocomplete="off"
            />
          </div>
        </div>

        <div>
          <label class="ad-label" for="dgg-tu-ngay">Ngày bắt đầu</label>
          <DateInput id="dgg-tu-ngay" v-model="filters.ngayBatDau" class="form-control ad-control" />
        </div>

        <div>
          <label class="ad-label" for="dgg-den-ngay">Ngày kết thúc</label>
          <DateInput id="dgg-den-ngay" v-model="filters.ngayKetThuc" class="form-control ad-control" />
        </div>

        <div>
          <label class="ad-label" for="dgg-trang-thai">Trạng thái</label>
          <Select2 id="dgg-trang-thai" v-model="filters.trangThai" :options="dsTrangThaiLoc" class="ad-control" />
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
          <i class="bi bi-plus-lg" aria-hidden="true"></i> Tạo đợt giảm giá
        </button>
      </template>
    </FilterCard>

    <section class="ad-card">
      <header class="ad-list-head">
        <h2 class="ad-card-title">Danh sách các đợt giảm giá</h2>
        <span class="ad-count">{{ total }} bản ghi hiển thị.</span>
      </header>

      <div class="ad-table-wrap">
        <div class="ad-table-scroll">
          <table class="ad-table">
            <thead>
              <tr>
                <th class="ad-col-stt">STT</th>
                <th>Mã</th>
                <th>Tên</th>
                <th>Giá trị</th>
                <th>Ngày giờ bắt đầu</th>
                <th>Ngày giờ kết thúc</th>
                <th>Trạng thái</th>
                <th class="ad-col-actions">Hành động</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading">
                <td colspan="8"><div class="ad-empty">Đang tải dữ liệu…</div></td>
              </tr>
              <tr v-else-if="!pageItems.length">
                <td colspan="8">
                  <div class="ad-empty">
                    <i class="bi bi-inbox" aria-hidden="true"></i>
                    <strong>Không tìm thấy đợt giảm giá</strong>
                    <span>Thử đổi từ khóa hoặc bấm “Đặt lại bộ lọc”.</span>
                  </div>
                </td>
              </tr>
              <template v-else>
                <tr v-for="(d, i) in pageItems" :key="d.id">
                  <td class="ad-col-stt">{{ (page - 1) * pageSize + i + 1 }}</td>
                  <td><span class="ad-code ad-nowrap">{{ d.maDot }}</span></td>
                  <td class="ad-cell-wrap">{{ d.tenDot }}</td>
                  <td class="ad-nowrap">{{ d.phanTramGiamDot }}%</td>
                  <td class="ad-nowrap">{{ formatNgay(d.ngayBatDau) }}</td>
                  <td class="ad-nowrap">{{ formatNgay(d.ngayKetThuc) }}</td>
                  <td>
                    <span class="ad-pill" :class="TRANG_THAI[tinhTrangThai(d, bayGio)].cls">{{ TRANG_THAI[tinhTrangThai(d, bayGio)].label }}</span>
                  </td>
                  <td class="ad-col-actions">
                    <div class="ad-actions">
                      <!-- Công tắc bật / tắt: đợt đã kết thúc thì công tắc TẮT và khóa -->
                      <span class="ad-switch-slot" :title="congTacNhan(d)">
                        <StatusSwitch
                          :on="congTacBat(d)"
                          :disabled="congTacKhoa(d)"
                          :label="congTacNhan(d)"
                          @toggle="confirmItem = d"
                        />
                      </span>
                      <button
                        type="button"
                        class="ad-icon-btn"
                        title="Xem chi tiết / sửa đợt giảm giá"
                        :aria-label="`Xem chi tiết đợt ${d.maDot}`"
                        @click="openDetail(d)"
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

    <ConfirmDialog
      v-if="confirmItem"
      v-bind="confirmContent"
      :loading="confirmLoading"
      @confirm="confirmToggle"
      @cancel="confirmItem = null"
    />
  </div>
</template>

<style scoped>
/* Bộ lọc đợt giảm giá: 4 ô trên một hàng, giống video */
.ad-filter-grid--dot {
  display: grid;
  grid-template-columns: minmax(0, 2fr) repeat(3, minmax(0, 1fr));
  gap: 1rem;
}
@media (max-width: 991px) {
  .ad-filter-grid--dot {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 575px) {
  .ad-filter-grid--dot {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
