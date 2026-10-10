<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import FilterCard from './common/FilterCard.vue'
import BasePagination from './common/BasePagination.vue'
import ConfirmDialog from './common/ConfirmDialog.vue'
import { nhanVienService } from '../services/nhanVienService'
import { TRANG_THAI } from '../constants/nhanVien'
import { usePagination } from '../composables/usePagination'
import { useToast } from '../composables/useToast'
import { useAutoRefresh } from '../composables/useAutoRefresh'
import { avatarColors, formatDate, getInitials, todayIso } from '../utils/format'
import { matchesWords } from '../utils/text'
import { exportExcel } from '../utils/exportExcel'

const toast = useToast()
const router = useRouter()

const list = ref([])
const vaiTroList = ref([]) // [{ id, ten }] từ bảng vai_tro
const loading = ref(false)

/* ----- Bộ lọc (lọc ngay khi thay đổi) ----- */
// Một ô tìm kiếm chung: gõ mã, họ tên, email, số điện thoại hoặc số CCCD đều tìm được (không phân biệt hoa thường, dấu).
const defaultFilters = () => ({ keyword: '', idVaiTro: '', trangThai: '' })
const filters = reactive(defaultFilters())

// Các vị trí làm việc của nhân viên, ngăn cách bằng dấu phẩy
const tenViTri = (e) => (e.viTri ?? []).map((v) => v.ten).join(', ')

function resetFilters() {
  Object.assign(filters, defaultFilters())
}

const filtered = computed(() =>
  list.value.filter((e) => {
    const f = filters
    if (f.keyword && !matchesWords([e.ma, e.hoTen, e.email, e.soDienThoai, e.cccd].join(' '), f.keyword)) return false
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
// loadSeq: mỗi lần tải có một số thứ tự. Nếu đã có lần tải / cập nhật mới hơn thì bỏ kết quả của lần cũ,
// để dữ liệu cũ về chậm không đè lên trạng thái ẩn / hiện vừa đổi.
// quiet = true: tải nền (không hiện "Đang tải…", lỗi mạng thì im lặng vì danh sách đang hiển thị vẫn dùng được).
let loadSeq = 0
async function load(showLoading = true, quiet = false) {
  const seq = ++loadSeq
  if (showLoading) loading.value = true
  try {
    const [nhanVien, vaiTro] = await Promise.all([nhanVienService.getAll(), nhanVienService.getVaiTro()])
    if (seq !== loadSeq) return
    list.value = nhanVien
    vaiTroList.value = vaiTro
  } catch {
    if (seq === loadSeq && !(quiet && list.value.length)) toast.error('Không tải được danh sách nhân viên. Vui lòng thử lại.')
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

/* ----- Thêm / xem chi tiết / sửa ----- */
// Là trang riêng (NhanVienFormPage.vue), giống video; ở đây chỉ chuyển trang.
function openCreate() {
  router.push('/nhan-vien/them')
}
function openDetail(row) {
  router.push('/nhan-vien/' + row.id)
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
      { header: 'Vị trí', key: 'viTri', width: 30 },
      { header: 'Số CCCD', key: 'cccd', width: 16 },
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
      viTri: tenViTri(e),
      cccd: e.cccd || '',
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
    const daDoi = await nhanVienService.toggleActive(e.id)
    patchRow(daDoi) // dòng này đổi trạng thái ngay: ẩn xong sang xem nhân viên đã ẩn là thấy luôn
    toast.success(e.hoatDong ? `Đã ẩn nhân viên ${e.hoTen}.` : `Đã hiện lại nhân viên ${e.hoTen}.`)
    confirmItem.value = null
    load(false, true) // đồng bộ lại với server ở nền
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
      <div class="ad-filter-grid ad-filter-grid--staff">
        <div>
          <label class="ad-label" for="nv-keyword">Tìm kiếm</label>
          <div class="ad-affix">
            <i class="bi bi-search" aria-hidden="true"></i>
            <input
              id="nv-keyword"
              v-model.trim="filters.keyword"
              type="text"
              class="form-control ad-control"
              placeholder="Tìm theo mã, họ tên, email, SĐT, CCCD…"
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
                <th>Vị trí</th>
                <th>Trạng thái</th>
                <th class="ad-col-actions">Hành động</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading">
                <td colspan="12"><div class="ad-empty">Đang tải dữ liệu…</div></td>
              </tr>
              <tr v-else-if="!pageItems.length">
                <td colspan="12">
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
                  <td><span class="ad-clamp-2" :title="tenViTri(e)">{{ tenViTri(e) || '—' }}</span></td>
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
