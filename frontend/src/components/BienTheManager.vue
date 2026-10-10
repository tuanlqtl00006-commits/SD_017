<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import FilterCard from './common/FilterCard.vue'
import BasePagination from './common/BasePagination.vue'
import ConfirmDialog from './common/ConfirmDialog.vue'
import ConfirmQuestion from './common/ConfirmQuestion.vue'
import BienTheFormModal from './sanPham/BienTheFormModal.vue'
import { sanPhamService, bienTheService } from '../services/sanPhamService'
import { useGiamGiaBienThe } from '../composables/useGiamGiaBienThe'
import { TRANG_THAI, trangThaiOf } from '../constants/thuocTinh'
import { useThuocTinh } from '../composables/useThuocTinh'
import { usePagination } from '../composables/usePagination'
import { useToast } from '../composables/useToast'
import { useAutoRefresh } from '../composables/useAutoRefresh'
import { formatMoney, todayIso } from '../utils/format'
import { includesText } from '../utils/text'
import { idKhongApDung, laDanhMucVot } from '../utils/danhMuc'
import { exportExcel } from '../utils/exportExcel'

const toast = useToast()
const today = todayIso()
const { data: tt, loadThuocTinh, tenOf, optionsOf } = useThuocTinh(['mau-sac', 'trong-luong', 'chu-vi', 'danh-muc'])

const list = ref([])
const sanPhams = ref([])
const loading = ref(false)
const saving = ref(false)

const spOf = (id) => sanPhams.value.find((p) => p.id === id)

// Sản phẩm thuộc danh mục vợt mới có trọng lượng + chu vi; danh mục khác chỉ có màu sắc (trọng lượng, chu vi = "Không áp dụng")
const laVotCuaSp = (idSanPham) => {
  const sp = spOf(idSanPham)
  return sp ? laDanhMucVot(tenOf('danh-muc', sp.idDanhMuc)) : true
}
const idKadOf = (slug) => idKhongApDung(tt[slug])

/* ----- Bộ lọc ----- */
const defaultFilters = () => ({ keyword: '', idSanPham: '', idMauSac: '', trangThai: '', conHang: '' })
const filters = reactive(defaultFilters())
const resetFilters = () => Object.assign(filters, defaultFilters())

const filtered = computed(() =>
  list.value.filter((b) => {
    const f = filters
    if (f.keyword && !includesText(`${b.ma} ${spOf(b.idSanPham)?.ten ?? ''}`, f.keyword)) return false
    if (f.idSanPham !== '' && b.idSanPham !== f.idSanPham) return false
    if (f.idMauSac !== '' && b.idMauSac !== f.idMauSac) return false
    if (f.trangThai && trangThaiOf(b) !== f.trangThai) return false
    if (f.conHang === 'CON' && b.soLuongTon <= 0) return false
    if (f.conHang === 'HET' && b.soLuongTon > 0) return false
    return true
  }),
)
const { page, pageSize, total, items: pageItems } = usePagination(filtered, 5)
watch(filters, () => { page.value = 1 })

/* ----- Tải dữ liệu ----- */
// loadSeq: mỗi lần tải có một số thứ tự. Nếu đã có lần tải / cập nhật mới hơn thì bỏ kết quả của lần cũ,
// để dữ liệu cũ về chậm không đè lên trạng thái ẩn / hiện vừa đổi.
// quiet = true: tải nền (không hiện "Đang tải…", lỗi mạng thì im lặng vì danh sách đang hiển thị vẫn dùng được).
// Tải nền chỉ lấy lại biến thể + sản phẩm (2 lời gọi), không tải lại các bảng thuộc tính.
let loadSeq = 0
const { loadGiamGia, pctOf, giaSauGiam } = useGiamGiaBienThe()

async function load(showLoading = true, quiet = false) {
  loadGiamGia()
  const seq = ++loadSeq
  if (showLoading) loading.value = true
  try {
    const [bt, sp] = await Promise.all([bienTheService.getAll(), sanPhamService.getAll(), quiet ? null : loadThuocTinh()])
    if (seq !== loadSeq) return
    list.value = bt
    sanPhams.value = sp
  } catch {
    if (seq === loadSeq && !(quiet && list.value.length)) toast.error('Không tải được danh sách biến thể. Vui lòng thử lại.')
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
const formState = ref(null)
// Thêm mà biến thể (cùng sản phẩm + màu + trọng lượng + chu vi) đã tồn tại: backend trả 409 mã BIEN_THE_DA_TON_TAI,
// hỏi người dùng có muốn cập nhật không rồi gửi lại với xacNhanCapNhat = true.
const hoiCapNhat = ref(null) // { message, payload }
async function save(payload, xacNhanCapNhat = false) {
  const editing = formState.value?.item
  saving.value = true
  try {
    if (editing) {
      await bienTheService.update(editing.id, payload)
      toast.success('Đã lưu thay đổi biến thể.')
    } else {
      await bienTheService.create({ ...payload, xacNhanCapNhat })
      toast.success(xacNhanCapNhat ? 'Đã cập nhật biến thể.' : 'Đã thêm biến thể.')
    }
    hoiCapNhat.value = null
    formState.value = null
    await load(false)
  } catch (e) {
    if (!editing && e.response?.status === 409 && e.response?.data?.code === 'BIEN_THE_DA_TON_TAI') {
      hoiCapNhat.value = { message: e.message, payload }
    } else {
      hoiCapNhat.value = null
      toast.error(e.message || 'Không thể lưu biến thể. Vui lòng thử lại.')
    }
  } finally {
    saving.value = false
  }
}

/* ----- Bật / tắt hoạt động ----- */
const confirmItem = ref(null)
const confirmLoading = ref(false)
const confirmContent = computed(() => {
  const b = confirmItem.value
  if (!b) return {}
  return b.hoatDong
    ? { title: 'Ngưng bán biến thể?', message: `Biến thể ${b.ma} sẽ không còn hiển thị để bán. Bạn có thể kích hoạt lại sau.`, confirmText: 'Ngưng hoạt động', variant: 'danger' }
    : { title: 'Kích hoạt lại biến thể?', message: `Biến thể ${b.ma} sẽ được bán trở lại.`, confirmText: 'Kích hoạt', variant: 'primary' }
})
async function confirmToggle() {
  const b = confirmItem.value
  confirmLoading.value = true
  try {
    const daDoi = await bienTheService.toggleActive(b.id)
    patchRow(daDoi) // đổi trạng thái ngay trên dòng, không chờ tải lại cả danh sách
    toast.success(b.hoatDong ? `Đã ngưng hoạt động biến thể ${b.ma}.` : `Đã kích hoạt lại biến thể ${b.ma}.`)
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
    toast.error('Không có biến thể nào để xuất.')
    return
  }
  exportExcel({
    filename: `bien-the-san-pham_${today}.xlsx`,
    sheetName: 'Biến thể sản phẩm',
    columns: [
      { header: 'STT', key: 'stt', width: 6 },
      { header: 'Mã biến thể', key: 'ma', width: 22 },
      { header: 'Sản phẩm', key: 'sanPham', width: 32 },
      { header: 'Màu sắc', key: 'mauSac', width: 14 },
      { header: 'Trọng lượng', key: 'trongLuong', width: 16 },
      { header: 'Chu vi', key: 'chuVi', width: 10 },
      { header: 'Giá bán (₫)', key: 'giaBan', width: 14 },
      { header: 'Tồn kho', key: 'soLuongTon', width: 10 },
      { header: 'Trạng thái', key: 'trangThai', width: 18 },
    ],
    rows: filtered.value.map((b, i) => ({
      stt: i + 1,
      ma: b.ma,
      sanPham: spOf(b.idSanPham)?.ten ?? '',
      mauSac: tenOf('mau-sac', b.idMauSac),
      trongLuong: tenOf('trong-luong', b.idTrongLuong),
      chuVi: tenOf('chu-vi', b.idChuVi),
      giaBan: b.giaBan,
      soLuongTon: b.soLuongTon,
      trangThai: TRANG_THAI[trangThaiOf(b)].label,
    })),
  })
  toast.success(`Đã xuất ${filtered.value.length} biến thể ra file Excel.`)
}
</script>

<template>
  <div class="ad-page">
    <FilterCard subtitle="Tra cứu nhanh dữ liệu.">
      <div class="ad-filter-grid">
        <div>
          <label class="ad-label" for="bt-keyword">Tìm kiếm</label>
          <div class="ad-affix">
            <i class="bi bi-search" aria-hidden="true"></i>
            <input id="bt-keyword" v-model.trim="filters.keyword" type="text" class="form-control ad-control" placeholder="Mã biến thể hoặc tên sản phẩm" autocomplete="off" />
          </div>
        </div>
        <div>
          <label class="ad-label" for="bt-f-sp">Sản phẩm</label>
          <select id="bt-f-sp" v-model="filters.idSanPham" class="form-select ad-control">
            <option value="">Tất cả sản phẩm</option>
            <option v-for="p in sanPhams" :key="p.id" :value="p.id">{{ p.ten }}</option>
          </select>
        </div>
        <div>
          <label class="ad-label" for="bt-f-mau">Màu sắc</label>
          <select id="bt-f-mau" v-model="filters.idMauSac" class="form-select ad-control">
            <option value="">Tất cả màu sắc</option>
            <option v-for="o in tt['mau-sac']" :key="o.id" :value="o.id">{{ o.ten }}</option>
          </select>
        </div>
        <div>
          <label class="ad-label" for="bt-f-ton">Tồn kho</label>
          <select id="bt-f-ton" v-model="filters.conHang" class="form-select ad-control">
            <option value="">Tất cả</option>
            <option value="CON">Còn hàng</option>
            <option value="HET">Hết hàng</option>
          </select>
        </div>
        <div>
          <label class="ad-label" for="bt-f-trang-thai">Trạng thái</label>
          <select id="bt-f-trang-thai" v-model="filters.trangThai" class="form-select ad-control">
            <option value="">Tất cả trạng thái</option>
            <option v-for="(item, key) in TRANG_THAI" :key="key" :value="key">{{ item.label }}</option>
          </select>
        </div>
      </div>

      <template #actions>
        <button type="button" class="ad-btn" @click="resetFilters"><i class="bi bi-arrow-counterclockwise" aria-hidden="true"></i> Đặt lại bộ lọc</button>
        <button type="button" class="ad-btn" @click="exportFile"><i class="bi bi-file-earmark-excel" aria-hidden="true"></i> Xuất Excel</button>
        <button type="button" class="ad-btn ad-btn-primary" @click="formState = { item: null }"><i class="bi bi-plus-lg" aria-hidden="true"></i> Thêm biến thể</button>
      </template>
    </FilterCard>

    <section class="ad-card">
      <header class="ad-list-head">
        <h2 class="ad-card-title">Danh sách biến thể sản phẩm</h2>
        <span class="ad-count">{{ total }} bản ghi hiển thị</span>
      </header>

      <div class="ad-table-wrap">
        <div class="ad-table-scroll">
          <table class="ad-table">
            <thead>
              <tr>
                <th class="ad-col-stt">STT</th>
                <th>Mã biến thể</th>
                <th>Sản phẩm</th>
                <th>Màu sắc</th>
                <th>Trọng lượng</th>
                <th>Chu vi</th>
                <th>Giá bán</th>
                <th>Giảm</th>
                <th>Tồn kho</th>
                <th>Trạng thái</th>
                <th class="ad-col-actions">Hành động</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading"><td colspan="11"><div class="ad-empty">Đang tải dữ liệu…</div></td></tr>
              <tr v-else-if="!pageItems.length">
                <td colspan="11">
                  <div class="ad-empty">
                    <i class="bi bi-inbox" aria-hidden="true"></i>
                    <strong>Không tìm thấy biến thể</strong>
                    <span>Thử đổi từ khóa hoặc bấm “Đặt lại bộ lọc”.</span>
                  </div>
                </td>
              </tr>
              <template v-else>
                <tr v-for="(b, i) in pageItems" :key="b.id">
                  <td class="ad-col-stt">{{ (page - 1) * pageSize + i + 1 }}</td>
                  <td><span class="ad-code ad-nowrap">{{ b.ma }}</span></td>
                  <td class="ad-cell-wrap">{{ spOf(b.idSanPham)?.ten ?? '—' }}</td>
                  <td>{{ tenOf('mau-sac', b.idMauSac) }}</td>
                  <td class="ad-nowrap">{{ tenOf('trong-luong', b.idTrongLuong) }}</td>
                  <td>{{ tenOf('chu-vi', b.idChuVi) }}</td>
                  <td class="ad-nowrap">
                    <template v-if="pctOf(b.id)">
                      <div class="fw-bold ad-price-sale">{{ formatMoney(giaSauGiam(b)) }}</div>
                      <div class="text-muted small text-decoration-line-through">{{ formatMoney(b.giaBan) }}</div>
                    </template>
                    <template v-else>{{ formatMoney(b.giaBan) }}</template>
                  </td>
                  <td>
                    <span v-if="pctOf(b.id)" class="ad-pill ad-pill-blue">-{{ pctOf(b.id) }}%</span>
                    <span v-else class="text-muted">—</span>
                  </td>
                  <td>
                    <span v-if="b.soLuongTon === 0" class="ad-pill ad-pill-amber">Hết hàng</span>
                    <span v-else>{{ b.soLuongTon }}</span>
                  </td>
                  <td><span class="ad-pill" :class="TRANG_THAI[trangThaiOf(b)].cls">{{ TRANG_THAI[trangThaiOf(b)].label }}</span></td>
                  <td class="ad-col-actions">
                    <div class="ad-actions">
                      <button
                        type="button"
                        class="ad-icon-btn"
                        :class="b.hoatDong ? 'is-danger' : 'is-success'"
                        :title="b.hoatDong ? 'Ngưng hoạt động' : 'Kích hoạt lại'"
                        :aria-label="`${b.hoatDong ? 'Ngưng hoạt động' : 'Kích hoạt lại'} biến thể ${b.ma}`"
                        @click="confirmItem = b"
                      >
                        <i class="bi bi-power" aria-hidden="true"></i>
                      </button>
                      <button type="button" class="ad-icon-btn" title="Chỉnh sửa" :aria-label="`Chỉnh sửa biến thể ${b.ma}`" @click="formState = { item: b }">
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

      <BasePagination v-if="total" v-model:page="page" :page-size="pageSize" :total="total" />
    </section>

    <BienTheFormModal v-if="formState" :item="formState.item" :all="list" :san-phams="sanPhams" :options="optionsOf" :la-vot-fn="laVotCuaSp" :id-kad-fn="idKadOf" :saving="saving" @save="save" @close="formState = null" />
    <ConfirmQuestion
      v-if="hoiCapNhat"
      title="Biến thể đã tồn tại"
      :message="hoiCapNhat.message"
      confirm-text="Cập nhật"
      :loading="saving"
      @confirm="save(hoiCapNhat.payload, true)"
      @cancel="hoiCapNhat = null"
    />
    <ConfirmDialog v-if="confirmItem" v-bind="confirmContent" :loading="confirmLoading" @confirm="confirmToggle" @cancel="confirmItem = null" />
  </div>
</template>
