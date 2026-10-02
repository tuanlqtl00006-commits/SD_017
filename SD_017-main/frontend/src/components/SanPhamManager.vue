<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import FilterCard from './common/FilterCard.vue'
import BasePagination from './common/BasePagination.vue'
import ConfirmDialog from './common/ConfirmDialog.vue'
import SanPhamFormModal from './sanPham/SanPhamFormModal.vue'
import SanPhamDetailModal from './sanPham/SanPhamDetailModal.vue'
import { sanPhamService, bienTheService } from '../services/sanPhamService'
import { TRANG_THAI, trangThaiOf } from '../constants/thuocTinh'
import { useThuocTinh } from '../composables/useThuocTinh'
import { usePagination } from '../composables/usePagination'
import { useToast } from '../composables/useToast'
import { formatMoney, todayIso } from '../utils/format'
import { includesText } from '../utils/text'
import { exportExcel } from '../utils/exportExcel'

const toast = useToast()
const today = todayIso()
const { data: tt, loadThuocTinh, tenOf, optionsOf } = useThuocTinh(['danh-muc', 'thuong-hieu', 'xuat-xu', 'chat-lieu', 'do-cung', 'diem-can-bang', 'mau-sac', 'trong-luong', 'chu-vi'])

const list = ref([])
const bienThes = ref([])
const loading = ref(false)
const saving = ref(false)

/* ----- Bộ lọc ----- */
const defaultFilters = () => ({ keyword: '', idDanhMuc: '', idThuongHieu: '', trangThai: '' })
const filters = reactive(defaultFilters())
const resetFilters = () => Object.assign(filters, defaultFilters())

// Gắn thêm số biến thể, tổng tồn và giá thấp nhất từ các biến thể đang hoạt động
const rows = computed(() =>
  list.value.map((p) => {
    const bts = bienThes.value.filter((b) => b.idSanPham === p.id)
    const active = bts.filter((b) => b.hoatDong)
    return {
      ...p,
      soBienThe: bts.length,
      tongTon: bts.reduce((s, b) => s + b.soLuongTon, 0),
      giaTu: active.length ? Math.min(...active.map((b) => b.giaBan)) : null,
    }
  }),
)

const filtered = computed(() =>
  rows.value.filter((p) => {
    const f = filters
    if (f.keyword && !includesText(`${p.ma} ${p.ten}`, f.keyword)) return false
    if (f.idDanhMuc !== '' && p.idDanhMuc !== f.idDanhMuc) return false
    if (f.idThuongHieu !== '' && p.idThuongHieu !== f.idThuongHieu) return false
    if (f.trangThai && trangThaiOf(p) !== f.trangThai) return false
    return true
  }),
)
const { page, pageSize, total, items: pageItems } = usePagination(filtered, 5)
watch(filters, () => { page.value = 1 })

/* ----- Tải dữ liệu ----- */
async function load(showLoading = true) {
  if (showLoading) loading.value = true
  try {
    const [sp, bt] = await Promise.all([sanPhamService.getAll(), bienTheService.getAll(), loadThuocTinh()])
    list.value = sp
    bienThes.value = bt
  } catch {
    toast.error('Không tải được danh sách sản phẩm. Vui lòng thử lại.')
  } finally {
    loading.value = false
  }
}
onMounted(load)

/* ----- Thêm / sửa / xem chi tiết ----- */
const formState = ref(null)
const detailItem = ref(null)
const detailBienThes = computed(() => (detailItem.value ? bienThes.value.filter((b) => b.idSanPham === detailItem.value.id) : []))

function editFromDetail() {
  formState.value = { item: detailItem.value }
  detailItem.value = null
}

async function save(payload) {
  const editing = formState.value?.item
  saving.value = true
  try {
    if (editing) {
      await sanPhamService.update(editing.id, payload)
      toast.success('Đã lưu thay đổi sản phẩm.')
    } else {
      await sanPhamService.create(payload)
      toast.success('Đã thêm sản phẩm.')
    }
    formState.value = null
    await load(false)
  } catch (e) {
    toast.error(e.message || 'Không thể lưu sản phẩm. Vui lòng thử lại.')
  } finally {
    saving.value = false
  }
}

/* ----- Bật / tắt hoạt động ----- */
const confirmItem = ref(null)
const confirmLoading = ref(false)
const confirmContent = computed(() => {
  const p = confirmItem.value
  if (!p) return {}
  return p.hoatDong
    ? { title: 'Ngưng bán sản phẩm?', message: `Sản phẩm ${p.ma} sẽ không còn hiển thị để bán. Bạn có thể kích hoạt lại sau.`, confirmText: 'Ngưng hoạt động', variant: 'danger' }
    : { title: 'Kích hoạt lại sản phẩm?', message: `Sản phẩm ${p.ma} sẽ được bán trở lại.`, confirmText: 'Kích hoạt', variant: 'primary' }
})
async function confirmToggle() {
  const p = confirmItem.value
  confirmLoading.value = true
  try {
    await sanPhamService.toggleActive(p.id)
    toast.success(p.hoatDong ? `Đã ngưng hoạt động sản phẩm ${p.ma}.` : `Đã kích hoạt lại sản phẩm ${p.ma}.`)
    confirmItem.value = null
    await load(false)
  } catch (e) {
    toast.error(e.message || 'Không thể cập nhật trạng thái. Vui lòng thử lại.')
  } finally {
    confirmLoading.value = false
  }
}

/* ----- Xuất Excel ----- */
function exportFile() {
  if (!filtered.value.length) {
    toast.error('Không có sản phẩm nào để xuất.')
    return
  }
  exportExcel({
    filename: `san-pham_${today}.xlsx`,
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
    rows: filtered.value.map((p, i) => ({
      stt: i + 1,
      ma: p.ma,
      ten: p.ten,
      danhMuc: tenOf('danh-muc', p.idDanhMuc),
      thuongHieu: tenOf('thuong-hieu', p.idThuongHieu),
      xuatXu: tenOf('xuat-xu', p.idXuatXu),
      chatLieu: tenOf('chat-lieu', p.idChatLieu),
      doCung: tenOf('do-cung', p.idDoCung),
      diemCanBang: tenOf('diem-can-bang', p.idDiemCanBang),
      soBienThe: p.soBienThe,
      tongTon: p.tongTon,
      giaTu: p.giaTu,
      trangThai: TRANG_THAI[trangThaiOf(p)].label,
    })),
  })
  toast.success(`Đã xuất ${filtered.value.length} sản phẩm ra file Excel.`)
}
</script>

<template>
  <div class="ad-page">
    <FilterCard subtitle="Tra cứu nhanh dữ liệu.">
      <div class="ad-filter-grid">
        <div>
          <label class="ad-label" for="sp-keyword">Tìm kiếm</label>
          <div class="ad-affix">
            <i class="bi bi-search" aria-hidden="true"></i>
            <input id="sp-keyword" v-model.trim="filters.keyword" type="text" class="form-control ad-control" placeholder="Mã hoặc tên sản phẩm" autocomplete="off" />
          </div>
        </div>
        <div>
          <label class="ad-label" for="sp-f-danh-muc">Danh mục</label>
          <select id="sp-f-danh-muc" v-model="filters.idDanhMuc" class="form-select ad-control">
            <option value="">Tất cả danh mục</option>
            <option v-for="o in tt['danh-muc']" :key="o.id" :value="o.id">{{ o.ten }}</option>
          </select>
        </div>
        <div>
          <label class="ad-label" for="sp-f-thuong-hieu">Thương hiệu</label>
          <select id="sp-f-thuong-hieu" v-model="filters.idThuongHieu" class="form-select ad-control">
            <option value="">Tất cả thương hiệu</option>
            <option v-for="o in tt['thuong-hieu']" :key="o.id" :value="o.id">{{ o.ten }}</option>
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

      <template #actions>
        <button type="button" class="ad-btn" @click="resetFilters"><i class="bi bi-arrow-counterclockwise" aria-hidden="true"></i> Đặt lại bộ lọc</button>
        <button type="button" class="ad-btn" @click="exportFile"><i class="bi bi-file-earmark-excel" aria-hidden="true"></i> Xuất Excel</button>
        <button type="button" class="ad-btn ad-btn-primary" @click="formState = { item: null }"><i class="bi bi-plus-lg" aria-hidden="true"></i> Thêm sản phẩm</button>
      </template>
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
                <th>Mã</th>
                <th>Tên sản phẩm</th>
                <th>Danh mục</th>
                <th>Thương hiệu</th>
                <th>Biến thể</th>
                <th>Tồn kho</th>
                <th>Giá từ</th>
                <th>Trạng thái</th>
                <th class="ad-col-actions">Hành động</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading"><td colspan="10"><div class="ad-empty">Đang tải dữ liệu…</div></td></tr>
              <tr v-else-if="!pageItems.length">
                <td colspan="10">
                  <div class="ad-empty">
                    <i class="bi bi-inbox" aria-hidden="true"></i>
                    <strong>Không tìm thấy sản phẩm</strong>
                    <span>Thử đổi từ khóa hoặc bấm “Đặt lại bộ lọc”.</span>
                  </div>
                </td>
              </tr>
              <template v-else>
                <tr v-for="(p, i) in pageItems" :key="p.id">
                  <td class="ad-col-stt">{{ (page - 1) * pageSize + i + 1 }}</td>
                  <td><span class="ad-code ad-nowrap">{{ p.ma }}</span></td>
                  <td class="ad-cell-wrap">{{ p.ten }}</td>
                  <td>{{ tenOf('danh-muc', p.idDanhMuc) }}</td>
                  <td>{{ tenOf('thuong-hieu', p.idThuongHieu) }}</td>
                  <td>{{ p.soBienThe }}</td>
                  <td>{{ p.tongTon }}</td>
                  <td class="ad-nowrap">{{ p.giaTu ? formatMoney(p.giaTu) : '—' }}</td>
                  <td><span class="ad-pill" :class="TRANG_THAI[trangThaiOf(p)].cls">{{ TRANG_THAI[trangThaiOf(p)].label }}</span></td>
                  <td class="ad-col-actions">
                    <div class="ad-actions">
                      <button
                        type="button"
                        class="ad-icon-btn"
                        :class="p.hoatDong ? 'is-danger' : 'is-success'"
                        :title="p.hoatDong ? 'Ngưng hoạt động' : 'Kích hoạt lại'"
                        :aria-label="`${p.hoatDong ? 'Ngưng hoạt động' : 'Kích hoạt lại'} sản phẩm ${p.ma}`"
                        @click="confirmItem = p"
                      >
                        <i class="bi bi-power" aria-hidden="true"></i>
                      </button>
                      <button type="button" class="ad-icon-btn" title="Xem chi tiết" :aria-label="`Xem chi tiết sản phẩm ${p.ma}`" @click="detailItem = p">
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

    <SanPhamFormModal v-if="formState" :item="formState.item" :all="list" :options="optionsOf" :saving="saving" @save="save" @close="formState = null" />
    <SanPhamDetailModal v-if="detailItem" :item="detailItem" :bien-thes="detailBienThes" :ten-of="tenOf" @edit="editFromDetail" @close="detailItem = null" />
    <ConfirmDialog v-if="confirmItem" v-bind="confirmContent" :loading="confirmLoading" @confirm="confirmToggle" @cancel="confirmItem = null" />
  </div>
</template>
