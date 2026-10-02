<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import FilterCard from './common/FilterCard.vue'
import BasePagination from './common/BasePagination.vue'
import ConfirmDialog from './common/ConfirmDialog.vue'
import NhanVienFormModal from './nhanVien/NhanVienFormModal.vue'
import NhanVienDetailModal from './nhanVien/NhanVienDetailModal.vue'
import { nhanVienService } from '../services/nhanVienService'
import { VAI_TRO, TRANG_THAI } from '../constants/nhanVien'
import { usePagination } from '../composables/usePagination'
import { useToast } from '../composables/useToast'
import { avatarColors, formatDate, getInitials, todayIso } from '../utils/format'
import { includesText } from '../utils/text'
import { exportExcel } from '../utils/exportExcel'

const toast = useToast()

const list = ref([])
const loading = ref(false)
const saving = ref(false)

/* ----- Bộ lọc (lọc ngay khi thay đổi) ----- */
const defaultFilters = () => ({ keyword: '', vaiTro: '', trangThai: '' })
const filters = reactive(defaultFilters())

function resetFilters() {
  Object.assign(filters, defaultFilters())
}

const filtered = computed(() =>
  list.value.filter((e) => {
    const f = filters
    if (f.keyword && !includesText(`${e.ma} ${e.hoTen} ${e.taiKhoan} ${e.soDienThoai} ${e.email}`, f.keyword)) return false
    if (f.vaiTro && e.vaiTro !== f.vaiTro) return false
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
    list.value = await nhanVienService.getAll()
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

/* ----- Bật / tắt hoạt động ----- */
const confirmItem = ref(null)
const confirmLoading = ref(false)

const confirmContent = computed(() => {
  const e = confirmItem.value
  if (!e) return {}
  return e.hoatDong
    ? {
        title: 'Ngưng hoạt động nhân viên?',
        message: `${e.hoTen} (${e.ma}) sẽ không thể đăng nhập vào hệ thống. Bạn có thể kích hoạt lại sau.`,
        confirmText: 'Ngưng hoạt động',
        variant: 'danger',
      }
    : {
        title: 'Kích hoạt lại nhân viên?',
        message: `${e.hoTen} (${e.ma}) sẽ có thể đăng nhập và làm việc trở lại.`,
        confirmText: 'Kích hoạt',
        variant: 'primary',
      }
})

const isAdmin = (e) => e.vaiTro === 'ADMIN'

function toggleTitle(e) {
  if (isAdmin(e)) return 'Không thể ngưng hoạt động quản trị viên'
  return e.hoatDong ? 'Ngưng hoạt động' : 'Kích hoạt lại'
}

async function confirmToggle() {
  const e = confirmItem.value
  confirmLoading.value = true
  try {
    await nhanVienService.toggleActive(e.id)
    toast.success(e.hoatDong ? `Đã ngưng hoạt động ${e.hoTen}.` : `Đã kích hoạt lại ${e.hoTen}.`)
    confirmItem.value = null
    await load(false)
  } catch (err) {
    toast.error(err.message || 'Không thể cập nhật trạng thái. Vui lòng thử lại.')
  } finally {
    confirmLoading.value = false
  }
}

/* ----- Xuất Excel (theo kết quả đang lọc) ----- */
function exportFile() {
  if (!filtered.value.length) {
    toast.error('Không có nhân viên nào để xuất.')
    return
  }
  exportExcel({
    filename: `nhan-vien_${todayIso()}.xlsx`,
    sheetName: 'Nhân viên',
    columns: [
      { header: 'STT', key: 'stt', width: 6 },
      { header: 'Mã NV', key: 'ma', width: 12 },
      { header: 'Họ tên', key: 'hoTen', width: 26 },
      { header: 'Tài khoản', key: 'taiKhoan', width: 16 },
      { header: 'Email', key: 'email', width: 30 },
      { header: 'Giới tính', key: 'gioiTinh', width: 10 },
      { header: 'SĐT', key: 'soDienThoai', width: 14 },
      { header: 'Ngày sinh', key: 'ngaySinh', width: 12 },
      { header: 'Địa chỉ', key: 'diaChi', width: 50 },
      { header: 'Vai trò', key: 'vaiTro', width: 16 },
      { header: 'Trạng thái', key: 'trangThai', width: 18 },
    ],
    rows: filtered.value.map((e, i) => ({
      stt: i + 1,
      ma: e.ma,
      hoTen: e.hoTen,
      taiKhoan: e.taiKhoan,
      email: e.email,
      gioiTinh: e.gioiTinh,
      soDienThoai: e.soDienThoai,
      ngaySinh: formatDate(e.ngaySinh),
      diaChi: e.diaChi,
      vaiTro: VAI_TRO[e.vaiTro].label,
      trangThai: TRANG_THAI[e.hoatDong].label,
    })),
  })
  toast.success(`Đã xuất ${filtered.value.length} nhân viên ra file Excel.`)
}
</script>

<template>
  <div class="ad-page">
    <FilterCard>
      <div class="ad-filter-grid ad-filter-grid--staff">
        <div class="ad-affix">
          <label class="visually-hidden" for="nv-keyword">Tìm kiếm nhân viên</label>
          <i class="bi bi-search" aria-hidden="true"></i>
          <input
            id="nv-keyword"
            v-model.trim="filters.keyword"
            type="text"
            class="form-control ad-control"
            placeholder="Tìm theo mã, họ tên, tài khoản, SĐT, email…"
            autocomplete="off"
          />
        </div>

        <div>
          <label class="visually-hidden" for="nv-vai-tro">Vai trò</label>
          <select id="nv-vai-tro" v-model="filters.vaiTro" class="form-select ad-control">
            <option value="">Tất cả vai trò</option>
            <option v-for="(item, key) in VAI_TRO" :key="key" :value="key">{{ item.label }}</option>
          </select>
        </div>

        <div>
          <label class="visually-hidden" for="nv-trang-thai">Trạng thái</label>
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
                <th>Ảnh</th>
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
                    <span class="ad-avatar" :style="e.anh ? undefined : avatarColors(e.hoTen)">
                      <img v-if="e.anh" :src="e.anh" alt="" />
                      <template v-else>{{ getInitials(e.hoTen) }}</template>
                    </span>
                  </td>
                  <td><span class="ad-code ad-nowrap">{{ e.ma }}</span></td>
                  <td class="ad-nowrap">{{ e.hoTen }}</td>
                  <td><span class="ad-ellipsis" :title="e.email">{{ e.email }}</span></td>
                  <td>{{ e.gioiTinh }}</td>
                  <td class="ad-nowrap">{{ e.soDienThoai }}</td>
                  <td><span class="ad-clamp-2" :title="e.diaChi">{{ e.diaChi }}</span></td>
                  <td class="ad-nowrap">{{ VAI_TRO[e.vaiTro].label }}</td>
                  <td>
                    <span class="ad-pill" :class="TRANG_THAI[e.hoatDong].cls">{{ TRANG_THAI[e.hoatDong].label }}</span>
                  </td>
                  <td class="ad-col-actions">
                    <div class="ad-actions">
                      <button
                        type="button"
                        class="ad-icon-btn"
                        :class="e.hoatDong ? 'is-danger' : 'is-success'"
                        :disabled="isAdmin(e)"
                        :title="toggleTitle(e)"
                        :aria-label="`${toggleTitle(e)}: ${e.hoTen}`"
                        @click="confirmItem = e"
                      >
                        <i class="bi bi-power" aria-hidden="true"></i>
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
