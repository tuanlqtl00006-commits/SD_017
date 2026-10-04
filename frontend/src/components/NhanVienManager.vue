<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import FilterCard from './common/FilterCard.vue'
import BasePagination from './common/BasePagination.vue'
import ConfirmDialog from './common/ConfirmDialog.vue'
import NhanVienFormModal from './nhanVien/NhanVienFormModal.vue'
import NhanVienDetailModal from './nhanVien/NhanVienDetailModal.vue'
import { nhanVienService } from '../services/nhanVienService'
import { TRANG_THAI } from '../constants/nhanVien'
import { usePagination } from '../composables/usePagination'
import { useToast } from '../composables/useToast'
import { avatarColors, formatDate, getInitials, todayIso } from '../utils/format'
import { includesText } from '../utils/text'
import { exportExcel } from '../utils/exportExcel'

const toast = useToast()

const list = ref([])
const vaiTroList = ref([]) // [{ id, ten }] từ bảng vai_tro
const loading = ref(false)
const saving = ref(false)

/* ----- Bộ lọc (lọc ngay khi thay đổi) ----- */
// "Tìm theo" nào thì chỉ so khớp đúng cột đó, không trộn các cột lại với nhau.
// key = tên thuộc tính của nhân viên (xem nhanVienService.js)
const TIM_THEO = [
  { key: 'hoTen', label: 'Họ tên', placeholder: 'Nhập họ tên, ví dụ: Lê Quốc Bảo' },
  { key: 'ma', label: 'Mã nhân viên', placeholder: 'Nhập mã, ví dụ: NV0003' },
  { key: 'soDienThoai', label: 'Số điện thoại', placeholder: 'Nhập số điện thoại, ví dụ: 0912000003' },
  { key: 'email', label: 'Email', placeholder: 'Nhập email, ví dụ: bao.lq@footstyle.vn' },
]

const defaultFilters = () => ({ timTheo: 'hoTen', keyword: '', idVaiTro: '', trangThai: '' })
const filters = reactive(defaultFilters())

// Gợi ý trong ô nhập đổi theo cột đang chọn
const placeholderTimKiem = computed(() => TIM_THEO.find((t) => t.key === filters.timTheo).placeholder)

function resetFilters() {
  Object.assign(filters, defaultFilters())
}

const filtered = computed(() =>
  list.value.filter((e) => {
    const f = filters
    // Chỉ lấy giá trị của đúng cột đang chọn (cột nào để trống thì coi như '')
    if (f.keyword && !includesText(e[f.timTheo] ?? '', f.keyword)) return false
    if (f.idVaiTro && e.idVaiTro !== f.idVaiTro) return false
    if (f.trangThai && String(e.hoatDong) !== f.trangThai) return false
    return true
  }),
)

const { page, pageSize, total, items: pageItems } = usePagination(filtered, 5)
watch(filters, () => {
  page.value = 1
})

/* ----- Tải dữ liệu ----- */
async function load(showLoading = true) {
  if (showLoading) loading.value = true
  try {
    const [nhanVien, vaiTro] = await Promise.all([nhanVienService.getAll(), nhanVienService.getVaiTro()])
    list.value = nhanVien
    vaiTroList.value = vaiTro
  } catch {
    toast.error('Không tải được danh sách nhân viên. Vui lòng thử lại.')
  } finally {
    loading.value = false
  }
}
onMounted(load)

/* ----- Thêm / sửa / xem chi tiết ----- */
const formState = ref(null) // null: đóng | { item: null }: thêm mới | { item }: chỉnh sửa
const detailItem = ref(null)

function openCreate() {
  formState.value = { item: null }
}
function openDetail(row) {
  detailItem.value = row
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
      await nhanVienService.update(editing.id, payload)
      toast.success('Đã lưu thay đổi nhân viên.')
    } else {
      await nhanVienService.create(payload)
      toast.success('Đã thêm nhân viên.')
    }
    formState.value = null
    await load(false)
  } catch (e) {
    toast.error(e.message || 'Không thể lưu nhân viên. Vui lòng thử lại.')
  } finally {
    saving.value = false
  }
}

/* ----- Xuất Excel (đúng theo danh sách đang lọc) ----- */
function exportFile() {
  if (!filtered.value.length) {
    toast.error('Không có nhân viên nào để xuất.')
    return
  }
  exportExcel({
    filename: `nhan_vien_${todayIso()}.xlsx`,
    sheetName: 'Nhân viên',
    columns: [
      { header: 'STT', key: 'stt', width: 6 },
      { header: 'Mã NV', key: 'ma', width: 12 },
      { header: 'Họ tên', key: 'hoTen', width: 28 },
      { header: 'Giới tính', key: 'gioiTinh', width: 10 },
      { header: 'Ngày sinh', key: 'ngaySinh', width: 14 },
      { header: 'SĐT', key: 'soDienThoai', width: 14 },
      { header: 'Email', key: 'email', width: 30 },
      { header: 'Địa chỉ', key: 'diaChi', width: 40 },
      { header: 'Ngày vào làm', key: 'ngayVaoLam', width: 14 },
      { header: 'Vai trò', key: 'vaiTro', width: 16 },
      { header: 'Trạng thái', key: 'trangThai', width: 18 },
    ],
    rows: filtered.value.map((e, i) => ({
      stt: i + 1,
      ma: e.ma,
      hoTen: e.hoTen,
      gioiTinh: e.gioiTinh || '',
      ngaySinh: e.ngaySinh ? formatDate(e.ngaySinh) : '',
      soDienThoai: e.soDienThoai,
      email: e.email,
      diaChi: e.diaChi,
      ngayVaoLam: e.ngayVaoLam ? formatDate(e.ngayVaoLam) : '',
      vaiTro: e.vaiTro,
      trangThai: TRANG_THAI[e.hoatDong].label,
    })),
  })
  toast.success(`Đã xuất ${filtered.value.length} nhân viên ra file Excel.`)
}

/* ----- Bật / tắt hoạt động ----- */
const confirmItem = ref(null)
const confirmLoading = ref(false)

const confirmContent = computed(() => {
  const e = confirmItem.value
  if (!e) return {}
  return e.hoatDong
    ? {
        title: 'Ẩn nhân viên?',
        message: `${e.hoTen} (${e.ma}) sẽ được ẩn và không thể đăng nhập vào hệ thống. Hồ sơ và lịch sử vẫn được giữ lại, bạn có thể hiện lại bất cứ lúc nào.`,
        confirmText: 'Ẩn nhân viên',
        variant: 'danger',
      }
    : {
        title: 'Hiện lại nhân viên?',
        message: `${e.hoTen} (${e.ma}) sẽ có thể đăng nhập và làm việc trở lại.`,
        confirmText: 'Hiện lại',
        variant: 'primary',
      }
})

// Backend chặn việc ẩn / đổi vai trò quản lý đang hoạt động cuối cùng và báo lỗi bằng toast.
function toggleTitle(e) {
  return e.hoatDong ? 'Ẩn nhân viên' : 'Hiện lại nhân viên'
}

async function confirmToggle() {
  const e = confirmItem.value
  confirmLoading.value = true
  try {
    await nhanVienService.toggleActive(e.id)
    toast.success(e.hoatDong ? `Đã ẩn nhân viên ${e.hoTen}.` : `Đã hiện lại nhân viên ${e.hoTen}.`)
    confirmItem.value = null
    await load(false)
  } catch (err) {
    toast.error(err.message || 'Không thể cập nhật trạng thái. Vui lòng thử lại.')
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
          <label class="ad-label" for="nv-tim-theo">Tìm theo</label>
          <select id="nv-tim-theo" v-model="filters.timTheo" class="form-select ad-control">
            <option v-for="t in TIM_THEO" :key="t.key" :value="t.key">{{ t.label }}</option>
          </select>
        </div>

        <div>
          <label class="ad-label" for="nv-keyword">Từ khóa</label>
          <div class="ad-affix">
            <i class="bi bi-search" aria-hidden="true"></i>
            <input
              id="nv-keyword"
              v-model.trim="filters.keyword"
              type="text"
              class="form-control ad-control"
              :placeholder="placeholderTimKiem"
              autocomplete="off"
            />
          </div>
        </div>

        <div>
          <label class="ad-label" for="nv-vai-tro">Vai trò</label>
          <select id="nv-vai-tro" v-model="filters.idVaiTro" class="form-select ad-control">
            <option value="">Tất cả vai trò</option>
            <option v-for="v in vaiTroList" :key="v.id" :value="v.id">{{ v.ten }}</option>
          </select>
        </div>

        <div>
          <label class="ad-label" for="nv-trang-thai">Trạng thái</label>
          <select id="nv-trang-thai" v-model="filters.trangThai" class="form-select ad-control">
            <option value="">Tất cả trạng thái</option>
            <option value="true">Hoạt động</option>
            <option value="false">Ngưng hoạt động</option>
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
          <i class="bi bi-plus-lg" aria-hidden="true"></i> Thêm nhân viên
        </button>
      </template>
    </FilterCard>

    <section class="ad-card">
      <header class="ad-list-head">
        <div class="ad-card-head">
          <span class="ad-icon-box" aria-hidden="true"><i class="bi bi-people"></i></span>
          <h2 class="ad-card-title">Danh sách nhân viên</h2>
        </div>
        <span class="ad-count">{{ total }} bản ghi hiển thị</span>
      </header>

      <div class="ad-table-wrap">
        <div class="ad-table-scroll">
          <table class="ad-table">
            <thead>
              <tr>
                <th class="ad-col-stt">STT</th>
                <th></th>
                <th>Mã NV</th>
                <th>Họ tên</th>
                <th>Email</th>
                <th>Giới tính</th>
                <th>SĐT</th>
                <th>Địa chỉ</th>
                <th>Vai trò</th>
                <th>Trạng thái</th>
                <th class="ad-col-actions">Hành động</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading">
                <td colspan="11"><div class="ad-empty">Đang tải dữ liệu…</div></td>
              </tr>
              <tr v-else-if="!pageItems.length">
                <td colspan="11">
                  <div class="ad-empty">
                    <i class="bi bi-inbox" aria-hidden="true"></i>
                    <strong>Không tìm thấy nhân viên</strong>
                    <span>Thử đổi từ khóa hoặc bấm “Đặt lại bộ lọc”.</span>
                  </div>
                </td>
              </tr>
              <template v-else>
                <tr v-for="(e, i) in pageItems" :key="e.id">
                  <td class="ad-col-stt">{{ (page - 1) * pageSize + i + 1 }}</td>
                  <td>
                    <span class="ad-avatar" :style="avatarColors(e.hoTen)">{{ getInitials(e.hoTen) }}</span>
                  </td>
                  <td><span class="ad-code ad-nowrap">{{ e.ma }}</span></td>
                  <td class="ad-nowrap">{{ e.hoTen }}</td>
                  <td><span class="ad-ellipsis" :title="e.email">{{ e.email }}</span></td>
                  <td>{{ e.gioiTinh || '' }}</td>
                  <td class="ad-nowrap">{{ e.soDienThoai }}</td>
                  <td><span class="ad-clamp-2" :title="e.diaChi">{{ e.diaChi }}</span></td>
                  <td class="ad-nowrap">{{ e.vaiTro }}</td>
                  <td>
                    <span class="ad-pill" :class="TRANG_THAI[e.hoatDong].cls">{{ TRANG_THAI[e.hoatDong].label }}</span>
                  </td>
                  <td class="ad-col-actions">
                    <div class="ad-actions">
                      <button
                        type="button"
                        class="ad-icon-btn"
                        :class="e.hoatDong ? 'is-danger' : 'is-success'"
                        :title="toggleTitle(e)"
                        :aria-label="`${toggleTitle(e)}: ${e.hoTen}`"
                        @click="confirmItem = e"
                      >
                        <i class="bi" :class="e.hoatDong ? 'bi-eye-slash' : 'bi-arrow-counterclockwise'" aria-hidden="true"></i>
                      </button>
                      <button
                        type="button"
                        class="ad-icon-btn"
                        title="Xem chi tiết"
                        :aria-label="`Xem chi tiết nhân viên ${e.hoTen}`"
                        @click="openDetail(e)"
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

    <NhanVienFormModal
      v-if="formState"
      :item="formState.item"
      :all="list"
      :vai-tro="vaiTroList"
      :saving="saving"
      @save="save"
      @close="formState = null"
    />
    <NhanVienDetailModal v-if="detailItem" :item="detailItem" @edit="editFromDetail" @close="detailItem = null" />
    <ConfirmDialog
      v-if="confirmItem"
      v-bind="confirmContent"
      :loading="confirmLoading"
      @confirm="confirmToggle"
      @cancel="confirmItem = null"
    />
  </div>
</template>
