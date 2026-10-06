<script setup>
// Một trang dùng chung cho 9 bảng thuộc tính (danh mục, thương hiệu, xuất xứ, chất liệu, độ cứng,
// điểm cân bằng, màu sắc, trọng lượng, chu vi). Loại thuộc tính lấy từ route.meta.attr (xem router/index.js).
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import FilterCard from './common/FilterCard.vue'
import BasePagination from './common/BasePagination.vue'
import ConfirmDialog from './common/ConfirmDialog.vue'
import ThuocTinhFormModal from './thuocTinh/ThuocTinhFormModal.vue'
import { thuocTinhService } from '../services/thuocTinhService'
import { THUOC_TINH, TRANG_THAI, trangThaiOf } from '../constants/thuocTinh'
import { usePagination } from '../composables/usePagination'
import { useToast } from '../composables/useToast'
import { useAutoRefresh } from '../composables/useAutoRefresh'
import { includesText } from '../utils/text'
import { exportExcel } from '../utils/exportExcel'
import { todayIso } from '../utils/format'

const route = useRoute()
const toast = useToast()
const type = route.meta.attr
const cfg = THUOC_TINH[type]
const lower = cfg.label.toLowerCase()

const list = ref([])
const loading = ref(false)
const saving = ref(false)

const defaultFilters = () => ({ keyword: '', trangThai: '' })
const filters = reactive(defaultFilters())
const resetFilters = () => Object.assign(filters, defaultFilters())

const filtered = computed(() =>
  list.value.filter((x) => {
    if (filters.keyword && !includesText(`${x.ma} ${x.ten}`, filters.keyword)) return false
    if (filters.trangThai && trangThaiOf(x) !== filters.trangThai) return false
    return true
  }),
)
const { page, pageSize, total, items: pageItems } = usePagination(filtered, 5)
watch(filters, () => { page.value = 1 })

// loadSeq: mỗi lần tải có một số thứ tự. Nếu đã có lần tải / cập nhật mới hơn thì bỏ kết quả của lần cũ,
// để dữ liệu cũ về chậm không đè lên trạng thái ẩn / hiện vừa đổi.
// quiet = true: tải nền (không hiện "Đang tải…", lỗi mạng thì im lặng vì danh sách đang hiển thị vẫn dùng được).
let loadSeq = 0
async function load(showLoading = true, quiet = false) {
  const seq = ++loadSeq
  if (showLoading) loading.value = true
  try {
    const ds = await thuocTinhService.getAll(type)
    if (seq !== loadSeq) return
    list.value = ds
  } catch {
    if (seq === loadSeq && !(quiet && list.value.length)) toast.error(`Không tải được danh sách ${lower}. Vui lòng thử lại.`)
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

/* ----- Thêm / sửa ----- */
const formState = ref(null) // null: đóng | { item: null }: thêm mới | { item }: chỉnh sửa
async function save(payload) {
  const editing = formState.value?.item
  saving.value = true
  try {
    if (editing) {
      await thuocTinhService.update(type, editing.id, payload)
      toast.success(`Đã lưu thay đổi ${lower}.`)
    } else {
      await thuocTinhService.create(type, payload)
      toast.success(`Đã thêm ${lower}.`)
    }
    formState.value = null
    await load(false)
  } catch (e) {
    toast.error(e.message || `Không thể lưu ${lower}. Vui lòng thử lại.`)
  } finally {
    saving.value = false
  }
}

/* ----- Bật / tắt hoạt động ----- */
const confirmItem = ref(null)
const confirmLoading = ref(false)
const confirmContent = computed(() => {
  const x = confirmItem.value
  if (!x) return {}
  return x.hoatDong
    ? {
        title: 'Ngưng hoạt động?',
        message: `${cfg.label} “${x.ten}” sẽ không còn được chọn khi tạo sản phẩm mới. Bạn có thể kích hoạt lại sau.`,
        confirmText: 'Ngưng hoạt động',
        variant: 'danger',
      }
    : {
        title: 'Kích hoạt lại?',
        message: `${cfg.label} “${x.ten}” sẽ được chọn lại khi tạo sản phẩm.`,
        confirmText: 'Kích hoạt',
        variant: 'primary',
      }
})
async function confirmToggle() {
  const x = confirmItem.value
  confirmLoading.value = true
  try {
    const daDoi = await thuocTinhService.toggleActive(type, x.id)
    patchRow(daDoi) // đổi trạng thái ngay trên dòng, không chờ tải lại cả danh sách
    toast.success(x.hoatDong ? `Đã ngưng hoạt động “${x.ten}”.` : `Đã kích hoạt lại “${x.ten}”.`)
    confirmItem.value = null
    load(false, true) // đồng bộ lại với server ở nền
  } catch (e) {
    toast.error(e.message || 'Không thể cập nhật trạng thái. Vui lòng thử lại.')
  } finally {
    confirmLoading.value = false
  }
}

/* ----- Xuất Excel ----- */
function exportFile() {
  if (!filtered.value.length) {
    toast.error(`Không có ${lower} nào để xuất.`)
    return
  }
  exportExcel({
    filename: `${cfg.slug}_${todayIso()}.xlsx`,
    sheetName: cfg.label,
    columns: [
      { header: 'STT', key: 'stt', width: 6 },
      { header: 'Mã', key: 'ma', width: 14 },
      { header: `Tên ${lower}`, key: 'ten', width: 36 },
      { header: 'Trạng thái', key: 'trangThai', width: 18 },
    ],
    rows: filtered.value.map((x, i) => ({ stt: i + 1, ma: x.ma, ten: x.ten, trangThai: TRANG_THAI[trangThaiOf(x)].label })),
  })
  toast.success(`Đã xuất ${filtered.value.length} ${lower} ra file Excel.`)
}
</script>

<template>
  <div class="ad-page">
    <FilterCard subtitle="Tra cứu nhanh dữ liệu.">
      <div class="ad-filter-grid">
        <div>
          <label class="ad-label" for="tt-keyword">Tìm kiếm</label>
          <div class="ad-affix">
            <i class="bi bi-search" aria-hidden="true"></i>
            <input id="tt-keyword" v-model.trim="filters.keyword" type="text" class="form-control ad-control" :placeholder="`Mã hoặc tên ${lower}`" autocomplete="off" />
          </div>
        </div>
        <div>
          <label class="ad-label" for="tt-trang-thai">Trạng thái</label>
          <select id="tt-trang-thai" v-model="filters.trangThai" class="form-select ad-control">
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
        <button type="button" class="ad-btn ad-btn-primary" @click="formState = { item: null }">
          <i class="bi bi-plus-lg" aria-hidden="true"></i> Thêm {{ lower }}
        </button>
      </template>
    </FilterCard>

    <section class="ad-card">
      <header class="ad-list-head">
        <h2 class="ad-card-title">Danh sách {{ lower }}</h2>
        <span class="ad-count">{{ total }} bản ghi hiển thị</span>
      </header>

      <div class="ad-table-wrap">
        <div class="ad-table-scroll">
          <table class="ad-table">
            <thead>
              <tr>
                <th class="ad-col-stt">STT</th>
                <th>Mã</th>
                <th>Tên {{ lower }}</th>
                <th>Trạng thái</th>
                <th class="ad-col-actions">Hành động</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading"><td colspan="5"><div class="ad-empty">Đang tải dữ liệu…</div></td></tr>
              <tr v-else-if="!pageItems.length">
                <td colspan="5">
                  <div class="ad-empty">
                    <i class="bi bi-inbox" aria-hidden="true"></i>
                    <strong>Không tìm thấy {{ lower }}</strong>
                    <span>Thử đổi từ khóa hoặc bấm “Đặt lại bộ lọc”.</span>
                  </div>
                </td>
              </tr>
              <template v-else>
                <tr v-for="(x, i) in pageItems" :key="x.id">
                  <td class="ad-col-stt">{{ (page - 1) * pageSize + i + 1 }}</td>
                  <td><span class="ad-code ad-nowrap">{{ x.ma }}</span></td>
                  <td class="ad-cell-wrap">{{ x.ten }}</td>
                  <td><span class="ad-pill" :class="TRANG_THAI[trangThaiOf(x)].cls">{{ TRANG_THAI[trangThaiOf(x)].label }}</span></td>
                  <td class="ad-col-actions">
                    <div class="ad-actions">
                      <button
                        type="button"
                        class="ad-icon-btn"
                        :class="x.hoatDong ? 'is-danger' : 'is-success'"
                        :title="x.hoatDong ? 'Ngưng hoạt động' : 'Kích hoạt lại'"
                        :aria-label="`${x.hoatDong ? 'Ngưng hoạt động' : 'Kích hoạt lại'} ${x.ten}`"
                        @click="confirmItem = x"
                      >
                        <i class="bi bi-power" aria-hidden="true"></i>
                      </button>
                      <button type="button" class="ad-icon-btn" title="Chỉnh sửa" :aria-label="`Chỉnh sửa ${x.ten}`" @click="formState = { item: x }">
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

      <BasePagination v-if="total" v-model:page="page" v-model:pageSize="pageSize" :total="total" />
    </section>

    <ThuocTinhFormModal v-if="formState" :cfg="cfg" :item="formState.item" :all="list" :saving="saving" @save="save" @close="formState = null" />
    <ConfirmDialog v-if="confirmItem" v-bind="confirmContent" :loading="confirmLoading" @confirm="confirmToggle" @cancel="confirmItem = null" />
  </div>
</template>
