<script setup>
// Danh sách khách hàng (làm lại cùng kiểu với Nhân viên / Sản phẩm):
//  - bộ lọc dùng Select2, lọc ngay khi gõ / chọn (không phân biệt hoa thường và dấu);
//  - phân trang rút gọn "1 2 3 … N", 5 dòng mỗi trang;
//  - thêm / xem-sửa khách hàng ở trang riêng; địa chỉ quản lý trong cửa sổ riêng.
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import FilterCard from './common/FilterCard.vue'
import BasePagination from './common/BasePagination.vue'
import ConfirmDialog from './common/ConfirmDialog.vue'
import Select2 from './common/Select2.vue'
import KhachHangAddressModal from './KhachHangAddressModal.vue'
import { khachHangService } from '../services/khachHangService'
import { GIOI_TINH_KHACH, TRANG_THAI_KHACH, nhanGioiTinhKhach } from '../constants/khachHang'
import { usePagination } from '../composables/usePagination'
import { useToast } from '../composables/useToast'
import { useAutoRefresh } from '../composables/useAutoRefresh'
import { avatarColors, formatDate, getInitials, todayIso } from '../utils/format'
import { ghepDiaChi } from '../utils/diaChi'
import { matchesWords } from '../utils/text'
import { exportExcel } from '../utils/exportExcel'
import { ngayTuApi } from '../utils/ngay'

const toast = useToast()
const router = useRouter()

const list = ref([])
const loading = ref(false)

/* ----- Bộ lọc (lọc ngay khi thay đổi) ----- */
const dsGioiTinhLoc = [
  { value: '', label: 'Tất cả giới tính' },
  { value: '1', label: GIOI_TINH_KHACH[1] },
  { value: '0', label: GIOI_TINH_KHACH[0] },
]
const dsTrangThaiLoc = [
  { value: '', label: 'Tất cả trạng thái' },
  { value: '1', label: TRANG_THAI_KHACH[1].label },
  { value: '0', label: TRANG_THAI_KHACH[0].label },
]
const defaultFilters = () => ({ keyword: '', gioiTinh: '', trangThai: '' })
const filters = reactive(defaultFilters())
const resetFilters = () => Object.assign(filters, defaultFilters())

const filtered = computed(() =>
  list.value.filter((k) => {
    const f = filters
    if (f.keyword && !matchesWords([k.maKhachHang, k.hoTen, k.email, k.sdt].join(' '), f.keyword)) return false
    if (f.gioiTinh !== '' && String(k.gioiTinh) !== f.gioiTinh) return false
    if (f.trangThai !== '' && String(k.trangThai) !== f.trangThai) return false
    return true
  }),
)

const { page, pageSize, total, items: pageItems } = usePagination(filtered, 5)
watch(filters, () => {
  page.value = 1
})

/* ----- Tải dữ liệu ----- */
// loadSeq: lần tải cũ về chậm không được đè lên kết quả của lần tải mới hơn / trạng thái vừa đổi.
let loadSeq = 0
async function load(showLoading = true, quiet = false) {
  const seq = ++loadSeq
  if (showLoading) loading.value = true
  try {
    const ds = await khachHangService.getAll()
    if (seq === loadSeq) list.value = ds
  } catch (e) {
    if (seq === loadSeq && !(quiet && list.value.length)) toast.error(e.message || 'Không tải được danh sách khách hàng.')
  } finally {
    if (seq === loadSeq) loading.value = false
  }
}
onMounted(load)
useAutoRefresh(() => load(false, true))

/* ----- Địa chỉ mặc định hiển thị ở bảng ----- */
function diaChiMacDinh(k) {
  const ds = k.diaChiList ?? []
  return ds.find((d) => d.laDiaChiMacDinh) ?? ds[0] ?? null
}
const diaChiChu = (k) => {
  const d = diaChiMacDinh(k)
  return d ? ghepDiaChi(d) : ''
}
// Người nhận hàng của địa chỉ mặc định (có thể khác tên khách hàng, vd mua giúp người thân)
const tenNguoiNhan = (k) => diaChiMacDinh(k)?.tenNguoiNhan || ''

// Ảnh đại diện không mở được (đường dẫn hỏng) -> quay về hiện chữ cái đầu
const anhHong = reactive({})

/* ----- Thêm / xem-sửa / địa chỉ ----- */
const openCreate = () => router.push('/khach-hang/them')
const openEdit = (k) => router.push(`/khach-hang/${k.id}/sua`)
const khachDiaChi = ref(null)

/* ----- Xuất Excel (đúng theo danh sách đang lọc) ----- */
function exportFile() {
  if (!filtered.value.length) {
    toast.error('Không có khách hàng nào để xuất.')
    return
  }
  exportExcel({
    filename: `khach_hang_${todayIso()}.xlsx`,
    sheetName: 'Khách hàng',
    columns: [
      { header: 'STT', key: 'stt', width: 6 },
      { header: 'Mã khách hàng', key: 'ma', width: 16 },
      { header: 'Họ và tên', key: 'ten', width: 26 },
      { header: 'Số điện thoại', key: 'sdt', width: 16 },
      { header: 'Email', key: 'email', width: 28 },
      { header: 'Ngày sinh', key: 'ngaySinh', width: 14 },
      { header: 'Giới tính', key: 'gioiTinh', width: 10 },
      { header: 'Tên người nhận', key: 'nguoiNhan', width: 24 },
      { header: 'Địa chỉ', key: 'diaChi', width: 50 },
      { header: 'Trạng thái', key: 'trangThai', width: 16 },
    ],
    rows: filtered.value.map((k, i) => ({
      stt: i + 1,
      ma: k.maKhachHang || '',
      ten: k.hoTen || '',
      sdt: k.sdt || '',
      email: k.email || '',
      ngaySinh: k.ngaySinh ? formatDate(ngayTuApi(k.ngaySinh)) : '',
      gioiTinh: nhanGioiTinhKhach(k.gioiTinh),
      nguoiNhan: tenNguoiNhan(k),
      diaChi: diaChiChu(k),
      trangThai: (TRANG_THAI_KHACH[k.trangThai] ?? TRANG_THAI_KHACH[1]).label,
    })),
  })
  toast.success(`Đã xuất ${filtered.value.length} khách hàng ra file Excel.`)
}

/* ----- Bật / tắt hoạt động ----- */
const confirmItem = ref(null)
const confirmLoading = ref(false)
const dangHoatDong = (k) => k.trangThai === 1

const confirmContent = computed(() => {
  const k = confirmItem.value
  if (!k) return {}
  return dangHoatDong(k)
    ? {
        title: 'Ngừng hoạt động khách hàng',
        message: `Bạn có chắc chắn muốn ngừng hoạt động khách hàng ${k.hoTen}?`,
        confirmText: 'Ngừng hoạt động',
        variant: 'danger',
        icon: 'warning',
      }
    : {
        title: 'Kích hoạt khách hàng',
        message: `Bạn có chắc chắn muốn kích hoạt lại khách hàng ${k.hoTen}?`,
        confirmText: 'Kích hoạt',
        variant: 'primary',
        icon: 'success',
      }
})
const tieuDeNutBat = (k) => (dangHoatDong(k) ? 'Ngừng hoạt động' : 'Kích hoạt')

async function confirmToggle() {
  const k = confirmItem.value
  if (!k) return
  confirmLoading.value = true
  try {
    const daDoi = await khachHangService.toggleActive(k.id)
    loadSeq++ // hủy các lần tải đang chạy dở (có thể còn chứa trạng thái cũ)
    const i = list.value.findIndex((x) => x.id === k.id)
    if (i !== -1 && daDoi) list.value[i] = { ...list.value[i], ...daDoi }
    toast.success(dangHoatDong(k) ? 'Đã ngừng hoạt động khách hàng.' : 'Đã kích hoạt lại khách hàng.')
    confirmItem.value = null
    load(false, true)
  } catch (e) {
    toast.error(e.message || 'Không thể thay đổi trạng thái khách hàng.')
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
          <label class="ad-label" for="kh-keyword">Tìm kiếm</label>
          <div class="ad-affix">
            <i class="bi bi-search" aria-hidden="true"></i>
            <input
              id="kh-keyword"
              v-model.trim="filters.keyword"
              type="text"
              class="form-control ad-control"
              placeholder="Tìm theo mã, họ tên, email, SĐT…"
              autocomplete="off"
            />
          </div>
        </div>

        <div>
          <label class="ad-label" for="kh-loc-gioi-tinh">Giới tính</label>
          <Select2 id="kh-loc-gioi-tinh" v-model="filters.gioiTinh" :options="dsGioiTinhLoc" :min-search="99" class="ad-control" />
        </div>

        <div>
          <label class="ad-label" for="kh-loc-trang-thai">Trạng thái</label>
          <Select2 id="kh-loc-trang-thai" v-model="filters.trangThai" :options="dsTrangThaiLoc" :min-search="99" class="ad-control" />
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
          <i class="bi bi-plus-lg" aria-hidden="true"></i> Thêm khách hàng
        </button>
      </template>
    </FilterCard>

    <section class="ad-card">
      <header class="ad-list-head">
        <div class="ad-card-head">
          <span class="ad-icon-box" aria-hidden="true"><i class="bi bi-people"></i></span>
          <h2 class="ad-card-title">Danh sách khách hàng</h2>
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
                <th>Mã KH</th>
                <th>Họ tên</th>
                <th>Email</th>
                <th>SĐT</th>
                <th>Giới tính</th>
                <th>Tên người nhận</th>
                <th>Địa chỉ</th>
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
                    <strong>Không tìm thấy khách hàng</strong>
                    <span>Thử đổi từ khóa (tên, SĐT hoặc email) hoặc bấm “Đặt lại bộ lọc”.</span>
                  </div>
                </td>
              </tr>
              <template v-else>
                <tr v-for="(k, i) in pageItems" :key="k.id">
                  <td class="ad-col-stt">{{ (page - 1) * pageSize + i + 1 }}</td>
                  <td>
                    <span class="ad-avatar" :style="avatarColors(k.hoTen)">
                      <img v-if="k.anhDaiDien && !anhHong[k.id]" :src="k.anhDaiDien" :alt="`Ảnh đại diện của ${k.hoTen}`" @error="anhHong[k.id] = true" />
                      <template v-else>{{ getInitials(k.hoTen) }}</template>
                    </span>
                  </td>
                  <td><span class="ad-code ad-nowrap">{{ k.maKhachHang }}</span></td>
                  <td class="ad-nowrap">{{ k.hoTen }}</td>
                  <td><span class="ad-ellipsis" :title="k.email">{{ k.email }}</span></td>
                  <td class="ad-nowrap">{{ k.sdt }}</td>
                  <td>{{ nhanGioiTinhKhach(k.gioiTinh) }}</td>
                  <td class="ad-nowrap">
                    <span v-if="tenNguoiNhan(k)">{{ tenNguoiNhan(k) }}</span>
                    <span v-else class="text-muted">Chưa cập nhật</span>
                  </td>
                  <td>
                    <span v-if="diaChiChu(k)" class="ad-clamp-2" :title="diaChiChu(k)">{{ diaChiChu(k) }}</span>
                    <span v-else class="text-muted">Chưa cập nhật địa chỉ</span>
                  </td>
                  <td>
                    <span class="ad-pill" :class="(TRANG_THAI_KHACH[k.trangThai] ?? TRANG_THAI_KHACH[0]).cls">
                      {{ (TRANG_THAI_KHACH[k.trangThai] ?? TRANG_THAI_KHACH[0]).label }}
                    </span>
                  </td>
                  <td class="ad-col-actions">
                    <div class="ad-actions">
                      <button
                        type="button"
                        class="ad-icon-btn"
                        :class="dangHoatDong(k) ? 'is-success' : 'is-danger'"
                        :title="tieuDeNutBat(k)"
                        :aria-label="`${tieuDeNutBat(k)}: ${k.hoTen}`"
                        @click="confirmItem = k"
                      >
                        <i class="bi bi-power" aria-hidden="true"></i>
                      </button>
                      <button
                        type="button"
                        class="ad-icon-btn"
                        title="Xem / sửa khách hàng"
                        :aria-label="`Xem hoặc sửa khách hàng ${k.hoTen}`"
                        @click="openEdit(k)"
                      >
                        <i class="bi bi-pencil-square" aria-hidden="true"></i>
                      </button>
                      <button
                        type="button"
                        class="ad-icon-btn"
                        title="Quản lý địa chỉ"
                        :aria-label="`Quản lý địa chỉ của ${k.hoTen}`"
                        @click="khachDiaChi = k"
                      >
                        <i class="bi bi-geo-alt-fill" aria-hidden="true"></i>
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

    <KhachHangAddressModal v-if="khachDiaChi" :customer="khachDiaChi" @close="khachDiaChi = null" @address-updated="load(false, true)" />
  </div>
</template>
