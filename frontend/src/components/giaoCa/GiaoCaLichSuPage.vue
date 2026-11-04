<script setup>
// Lịch sử giao ca. Quản lý xem mọi ca (lọc theo nhân viên), nhân viên chỉ xem ca của mình.
// Chênh lệch tiền mặt tô màu (thiếu đỏ / thừa cam); bấm một dòng để xem chi tiết, các hóa đơn trong ca và in biên bản.
// Quản lý đóng ca hộ người đã về mà quên giao ca (cửa hàng một két tiền: ca đó chưa giao thì ca sau không vào được).
// Danh sách tự làm mới ngầm mỗi 10 giây (nhận ca / giao ca / đóng hộ hiện ra ngay, không phải bấm Xem lại).
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import BaseModal from '../common/BaseModal.vue'
import DateInput from '../common/DateInput.vue'
import FilterCard from '../common/FilterCard.vue'
import MoneyInput from '../common/MoneyInput.vue'
import Select2 from '../common/Select2.vue'
import api from '../../services/api'
import { lichLamViecService } from '../../services/lichLamViecService'
import { useAuth } from '../../composables/useAuth'
import { useToast } from '../../composables/useToast'
import { formatMoney, formatNgayGio, todayIso } from '../../utils/format'
import { inBienBanGiaoCa, moTaChenhLech, thoiLuong } from '../../utils/giaoCa'
import { congNgayIso } from '../../utils/lichTuan'

const toast = useToast()
const auth = useAuth()
const loc = reactive({ tuNgay: congNgayIso(todayIso(), -29), denNgay: todayIso(), idNhanVien: '' })
const ds = ref([])
const dangTai = ref(true)
const nhanViens = ref([])

const luaChonNv = computed(() => [{ value: '', label: 'Tất cả nhân viên' }, ...nhanViens.value.map((n) => ({ value: String(n.id), label: `${n.hoTen} (${n.maNhanVien})` }))])
const tong = computed(() => ({
  soCa: ds.value.length,
  doanhThu: ds.value.reduce((s, g) => s + Number(g.doanhThu || 0), 0),
  lech: ds.value.filter((g) => g.chenhLech !== null && Number(g.chenhLech) !== 0).length,
}))

// Bộ lọc đã áp dụng ở lần bấm Xem gần nhất: tự làm mới dùng bộ lọc này, không lấy ô đang gõ dở
let locApDung = { ...loc }
const capNhatLuc = ref('')
const goiDs = () => lichLamViecService.lichSuGiaoCa({
  tuNgay: locApDung.tuNgay || undefined,
  denNgay: locApDung.denNgay || undefined,
  idNhanVien: locApDung.idNhanVien || undefined,
})
function ghiGioCapNhat() {
  const d = new Date()
  capNhatLuc.value = [d.getHours(), d.getMinutes(), d.getSeconds()].map((n) => String(n).padStart(2, '0')).join(':')
}

async function tai() {
  if (loc.tuNgay && loc.denNgay && loc.tuNgay > loc.denNgay) {
    toast.error('Từ ngày phải trước hoặc cùng ngày với Đến ngày.')
    return
  }
  locApDung = { ...loc }
  dangTai.value = true
  try {
    ds.value = await goiDs()
    ghiGioCapNhat()
  } catch (e) {
    toast.error(e.message || 'Không tải được lịch sử giao ca.')
  } finally {
    dangTai.value = false
  }
}

/* ----- Tự làm mới ----- */
const CHU_KY_LAM_MOI_MS = 10_000
let henGio = null
let dangLamMoi = false
async function lamMoiNgam() {
  // tab đang ẩn, đang tải, đang đóng ca hộ thì thôi; lỗi mạng tạm thời thì bỏ qua, lần sau thử lại
  if (document.hidden || dangTai.value || dangLamMoi || dongHo.dangGui) return
  dangLamMoi = true
  try {
    ds.value = await goiDs()
    // đang xem chi tiết một ca chưa giao thì cập nhật số liệu của ca đó (trừ lúc quản lý đang nhập đóng ca hộ)
    if (chiTiet.value?.dangTrongCa && !dongHo.mo) chiTiet.value = await lichLamViecService.chiTietGiaoCa(chiTiet.value.id)
    ghiGioCapNhat()
  } catch {
    // giữ nguyên danh sách đang hiện
  } finally {
    dangLamMoi = false
  }
}
const khiTabHienLai = () => {
  if (!document.hidden) lamMoiNgam()
}
onMounted(async () => {
  if (auth.laQuanLy.value) {
    try {
      nhanViens.value = (await api.get('/lich-lam-viec/nhan-vien')).data
    } catch {
      nhanViens.value = []
    }
  }
  tai()
  henGio = setInterval(lamMoiNgam, CHU_KY_LAM_MOI_MS)
  document.addEventListener('visibilitychange', khiTabHienLai)
})
onBeforeUnmount(() => {
  clearInterval(henGio)
  document.removeEventListener('visibilitychange', khiTabHienLai)
})
function datLai() {
  Object.assign(loc, { tuNgay: congNgayIso(todayIso(), -29), denNgay: todayIso(), idNhanVien: '' })
  tai()
}

const chiTiet = ref(null)
async function moChiTiet(g) {
  dongHo.mo = false
  try {
    chiTiet.value = await lichLamViecService.chiTietGiaoCa(g.id)
  } catch (e) {
    toast.error(e.message || 'Không tải được chi tiết ca.')
  }
}
/* ----- Quản lý đóng ca hộ ----- */
const dongHo = reactive({ mo: false, tienMatThucTe: '', tienBanGiao: '', ghiChu: '', loi: '', soHoaDonCho: 0, dangGui: false })
const coTheDongHo = computed(() => auth.laQuanLy.value && chiTiet.value?.dangTrongCa && chiTiet.value.idNhanVien !== auth.trangThai.nguoiDung?.id)
function moDongHo() {
  // Điền sẵn số hệ thống tính; quản lý đếm két rồi sửa lại nếu khác
  Object.assign(dongHo, {
    mo: true,
    tienMatThucTe: Number(chiTiet.value.tongKet?.tienMatHeThong ?? 0),
    tienBanGiao: Number(chiTiet.value.tienDauCa ?? 0),
    ghiChu: '',
    loi: '',
    soHoaDonCho: 0,
  })
}
async function taiLaiChiTiet() {
  chiTiet.value = await lichLamViecService.chiTietGiaoCa(chiTiet.value.id)
  tai()
}
async function guiDongHo() {
  if (dongHo.dangGui) return
  dongHo.loi = ''
  if (dongHo.tienMatThucTe === '' || dongHo.tienBanGiao === '') {
    dongHo.loi = 'Vui lòng nhập tiền mặt thực tế và tiền để lại cho ca sau.'
    return
  }
  if (Number(dongHo.tienBanGiao) > Number(dongHo.tienMatThucTe)) {
    dongHo.loi = 'Tiền để lại cho ca sau không được lớn hơn tiền mặt thực tế.'
    return
  }
  dongHo.dangGui = true
  try {
    await api.post(`/giao-ca/${chiTiet.value.id}/dong-ho`, {
      tienMatThucTe: Number(dongHo.tienMatThucTe),
      tienBanGiao: Number(dongHo.tienBanGiao),
      ghiChu: dongHo.ghiChu.trim() || null,
    })
    toast.success(`Đã đóng ca hộ ${chiTiet.value.maGiaoCa}. Người ca sau có thể vào ca.`)
    dongHo.mo = false
    await taiLaiChiTiet()
  } catch (e) {
    if (e.code === 'CON_HOA_DON_CHO') dongHo.soHoaDonCho = Number(e.chiTiet?.soHoaDonCho) || 1
    dongHo.loi = e.loiTheoTruong ? Object.values(e.loiTheoTruong).join(' ') : e.message || 'Không đóng ca hộ được.'
  } finally {
    dongHo.dangGui = false
  }
}
async function huyHoaDonCho() {
  try {
    const kq = (await api.post(`/giao-ca/${chiTiet.value.id}/huy-hoa-don-cho`)).data
    toast.success(`Đã hủy ${kq.soHoaDonDaHuy} hóa đơn chờ, trả hàng về kho.`)
    dongHo.soHoaDonCho = 0
    dongHo.loi = ''
    await taiLaiChiTiet()
  } catch (e) {
    toast.error(e.message || 'Không hủy được hóa đơn chờ.')
  }
}

function inBienBan() {
  if (!inBienBanGiaoCa(chiTiet.value)) toast.error('Trình duyệt chặn cửa sổ in. Vui lòng cho phép cửa sổ bật lên rồi thử lại.')
}
</script>

<template>
  <div class="ad-page">
    <FilterCard subtitle="Xem lại các ca đã nhận / giao, đối soát tiền mặt.">
      <div class="ad-filter-grid" :class="{ 'ad-filter-grid--3': auth.laQuanLy.value }">
        <div>
          <label class="ad-label" for="gcl-tu">Từ ngày</label>
          <DateInput id="gcl-tu" v-model="loc.tuNgay" class="form-control ad-control" :max="loc.denNgay || undefined" />
        </div>
        <div>
          <label class="ad-label" for="gcl-den">Đến ngày</label>
          <DateInput id="gcl-den" v-model="loc.denNgay" class="form-control ad-control" :min="loc.tuNgay || undefined" />
        </div>
        <div v-if="auth.laQuanLy.value">
          <label class="ad-label" for="gcl-nv">Nhân viên</label>
          <Select2 id="gcl-nv" v-model="loc.idNhanVien" :options="luaChonNv" class="ad-control" />
        </div>
      </div>
      <template #actions>
        <button type="button" class="ad-btn" @click="datLai"><i class="bi bi-arrow-counterclockwise" aria-hidden="true"></i> Đặt lại bộ lọc</button>
        <button type="button" class="ad-btn ad-btn-primary" @click="tai"><i class="bi bi-search" aria-hidden="true"></i> Xem</button>
      </template>
    </FilterCard>

    <section class="ad-card">
      <header class="ad-list-head">
        <div class="ad-card-head">
          <span class="ad-icon-box" aria-hidden="true"><i class="bi bi-arrow-left-right"></i></span>
          <h2 class="ad-card-title">Lịch sử giao ca</h2>
        </div>
        <span class="ad-count">
          {{ tong.soCa }} ca · doanh thu {{ formatMoney(tong.doanhThu) }} · {{ tong.lech }} ca lệch tiền
          <small v-if="capNhatLuc" class="gcl-tu-cap-nhat" data-testid="gcl-cap-nhat-luc"><i class="bi bi-arrow-repeat" aria-hidden="true"></i> Tự cập nhật · lúc {{ capNhatLuc }}</small>
        </span>
      </header>
      <div class="ad-table-wrap">
        <div class="ad-table-scroll">
          <table class="ad-table">
            <thead>
              <tr>
                <th>Mã ca</th>
                <th>Nhân viên</th>
                <th>Ca theo lịch</th>
                <th>Nhận ca</th>
                <th>Giao ca</th>
                <th class="text-end">Số HĐ</th>
                <th class="text-end">Doanh thu</th>
                <th class="text-end">Tiền đầu ca</th>
                <th class="text-end">Tiền mặt HT</th>
                <th class="text-end">Thực tế</th>
                <th>Chênh lệch</th>
                <th>Trạng thái</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="dangTai">
                <td colspan="12"><div class="ad-empty">Đang tải dữ liệu…</div></td>
              </tr>
              <tr v-else-if="!ds.length">
                <td colspan="12">
                  <div class="ad-empty">
                    <i class="bi bi-inbox" aria-hidden="true"></i>
                    <strong>Chưa có ca nào trong khoảng ngày này</strong>
                  </div>
                </td>
              </tr>
              <tr v-for="g in ds" v-else :key="g.id" class="gcl-dong" tabindex="0" @click="moChiTiet(g)" @keydown.enter="moChiTiet(g)">
                <td><span class="ad-code">{{ g.maGiaoCa }}</span></td>
                <td>{{ g.tenNhanVien }}<br /><small class="text-muted">{{ g.maNhanVien }}</small></td>
                <td>
                  <span v-if="g.ngoaiLich" class="ad-pill ad-pill-gray">Ngoài lịch</span>
                  <template v-else>{{ g.tenCa }}<br /><small class="text-muted">{{ g.gioCa }}</small></template>
                  <span v-if="g.diMuon" class="ad-pill ad-pill-orange ms-1">Muộn {{ g.soPhutDiMuon }}'</span>
                </td>
                <td class="ad-nowrap">{{ formatNgayGio(g.thoiGianNhanCa) }}</td>
                <td class="ad-nowrap">{{ g.thoiGianKetCa ? formatNgayGio(g.thoiGianKetCa) : '—' }}</td>
                <td class="text-end">{{ g.soHoaDon ?? '—' }}</td>
                <td class="text-end">{{ g.doanhThu !== null ? formatMoney(g.doanhThu) : '—' }}</td>
                <td class="text-end">{{ formatMoney(g.tienDauCa) }}</td>
                <td class="text-end">{{ g.tienMatHeThong !== null ? formatMoney(g.tienMatHeThong) : '—' }}</td>
                <td class="text-end">{{ g.tienMatThucTe !== null ? formatMoney(g.tienMatThucTe) : '—' }}</td>
                <td><span :class="`gcl-cl is-${moTaChenhLech(g.chenhLech).kieu}`">{{ moTaChenhLech(g.chenhLech).nhan }}</span></td>
                <td>
                  <span class="ad-pill" :class="g.dangTrongCa ? 'ad-pill-blue' : 'ad-pill-green'">{{ g.dangTrongCa ? 'Đang trong ca' : 'Đã giao ca' }}</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </section>

    <BaseModal v-if="chiTiet" :title="`Ca ${chiTiet.maGiaoCa} - ${chiTiet.tenNhanVien}`" size="lg" @close="chiTiet = null">
      <dl class="ad-dl gcl-dl">
        <div><dt>Ca theo lịch</dt><dd>{{ chiTiet.ngoaiLich ? 'Ngoài lịch' : `${chiTiet.tenCa} (${chiTiet.gioCa})` }}</dd></div>
        <div><dt>Nhận ca</dt><dd>{{ formatNgayGio(chiTiet.thoiGianNhanCa) }}<span v-if="chiTiet.soPhutDiMuon"> · muộn {{ chiTiet.soPhutDiMuon }} phút</span></dd></div>
        <div><dt>Giao ca</dt><dd>{{ chiTiet.thoiGianKetCa ? `${formatNgayGio(chiTiet.thoiGianKetCa)} (${thoiLuong(chiTiet.thoiGianNhanCa, chiTiet.thoiGianKetCa)})` : 'Đang trong ca' }}</dd></div>
        <div><dt>Tiền đầu ca</dt><dd>{{ formatMoney(chiTiet.tienDauCa) }}</dd></div>
        <div><dt>Tiền mặt / chuyển khoản thu</dt><dd>{{ formatMoney(chiTiet.tienMatThu ?? chiTiet.tongKet?.tienMat ?? 0) }} / {{ formatMoney(chiTiet.chuyenKhoanThu ?? chiTiet.tongKet?.chuyenKhoan ?? 0) }}</dd></div>
        <div><dt>Tiền mặt hệ thống / thực tế</dt><dd>{{ formatMoney(chiTiet.tienMatHeThong ?? chiTiet.tongKet?.tienMatHeThong ?? 0) }} / {{ chiTiet.tienMatThucTe !== null ? formatMoney(chiTiet.tienMatThucTe) : '—' }}</dd></div>
        <div><dt>Chênh lệch</dt><dd :class="`gcl-cl is-${moTaChenhLech(chiTiet.chenhLech).kieu}`">{{ moTaChenhLech(chiTiet.chenhLech).nhan }}</dd></div>
        <div><dt>Bàn giao cho ca sau</dt><dd>{{ chiTiet.tienBanGiao !== null ? formatMoney(chiTiet.tienBanGiao) : '—' }}{{ chiTiet.tenNguoiNhanBanGiao ? ` · ${chiTiet.tenNguoiNhanBanGiao}` : '' }}</dd></div>
        <div class="ad-dl-full"><dt>Ghi chú</dt><dd>{{ [chiTiet.ghiChuNhanCa, chiTiet.ghiChuKetCa].filter(Boolean).join(' · ') || '—' }}</dd></div>
      </dl>
      <h3 class="gcl-tieu-de">Hóa đơn trong ca ({{ chiTiet.hoaDons.length }})</h3>
      <table class="ad-table">
        <thead>
          <tr><th>Mã HĐ</th><th>Thời gian</th><th>Khách</th><th>Thanh toán</th><th class="text-end">Thành tiền</th></tr>
        </thead>
        <tbody>
          <tr v-if="!chiTiet.hoaDons.length"><td colspan="5"><div class="ad-empty">Chưa có hóa đơn nào.</div></td></tr>
          <tr v-for="h in chiTiet.hoaDons" :key="h.id">
            <td><RouterLink :to="`/hoa-don/${h.id}`" class="ad-code">{{ h.maHoaDon }}</RouterLink></td>
            <td>{{ formatNgayGio(h.ngayTao) }}</td>
            <td>{{ h.tenKhach }}</td>
            <td>{{ h.phuongThuc || '—' }}</td>
            <td class="text-end">{{ formatMoney(h.thanhTien) }}</td>
          </tr>
        </tbody>
      </table>
      <!-- Quản lý đóng ca hộ: đếm két và nhập như khi giao ca -->
      <form v-if="dongHo.mo && coTheDongHo" class="gcl-dong-ho" novalidate data-testid="gcl-dong-ho" @submit.prevent="guiDongHo">
        <h3 class="gcl-tieu-de mt-0">Đóng ca hộ {{ chiTiet.maGiaoCa }}</h3>
        <p class="ad-hint mb-2">Hệ thống tính tiền mặt trong két là <b>{{ formatMoney(chiTiet.tongKet?.tienMatHeThong ?? 0) }}</b>. Đếm lại két rồi nhập số thực tế.</p>
        <div class="row g-2">
          <div class="col-md-6">
            <label class="ad-label" for="gcl-tien-thuc-te">Tiền mặt thực tế <span class="ad-required">*</span></label>
            <MoneyInput id="gcl-tien-thuc-te" v-model="dongHo.tienMatThucTe" aria-label="Tiền mặt thực tế" placeholder="Tiền đếm được trong két" />
          </div>
          <div class="col-md-6">
            <label class="ad-label" for="gcl-ban-giao">Để lại cho ca sau <span class="ad-required">*</span></label>
            <MoneyInput id="gcl-ban-giao" v-model="dongHo.tienBanGiao" aria-label="Tiền để lại cho ca sau" placeholder="Không để lại thì nhập 0" />
          </div>
          <div class="col-12">
            <label class="ad-label" for="gcl-ghi-chu">Ghi chú</label>
            <input id="gcl-ghi-chu" v-model="dongHo.ghiChu" type="text" class="form-control ad-control" maxlength="400" placeholder="Bắt buộc khi tiền mặt lệch so với hệ thống" />
          </div>
        </div>
        <div v-if="dongHo.loi" class="ad-error mt-2" role="alert">
          {{ dongHo.loi }}
          <button v-if="dongHo.soHoaDonCho" type="button" class="ad-btn ad-btn-sm ms-2" data-testid="gcl-huy-hd-cho" @click="huyHoaDonCho">
            Hủy {{ dongHo.soHoaDonCho }} hóa đơn chờ (trả hàng về kho)
          </button>
        </div>
        <div class="d-flex justify-content-end gap-2 mt-2">
          <button type="button" class="ad-btn" @click="dongHo.mo = false">Thôi</button>
          <button type="submit" class="ad-btn ad-btn-danger" :disabled="dongHo.dangGui">Xác nhận đóng ca hộ</button>
        </div>
      </form>
      <template #footer>
        <button type="button" class="ad-btn" @click="chiTiet = null">Đóng</button>
        <button v-if="coTheDongHo && !dongHo.mo" type="button" class="ad-btn ad-btn-danger" data-testid="gcl-mo-dong-ho" @click="moDongHo">
          <i class="bi bi-lock" aria-hidden="true"></i> Đóng ca hộ
        </button>
        <button v-if="!chiTiet.dangTrongCa" type="button" class="ad-btn ad-btn-primary" @click="inBienBan"><i class="bi bi-printer" aria-hidden="true"></i> In biên bản</button>
      </template>
    </BaseModal>
  </div>
</template>

<style scoped>
.gcl-tu-cap-nhat {
  margin-left: 0.5rem;
  color: #15803d;
  white-space: nowrap;
}
.gcl-dong-ho {
  margin-top: 1rem;
  padding: 0.9rem 1rem;
  border: 1px solid #fecaca;
  border-radius: 12px;
  background: #fff5f5;
}
.gcl-dong {
  cursor: pointer;
}
.gcl-cl.is-ok {
  color: #146c43;
  font-weight: 600;
}
.gcl-cl.is-thieu {
  color: #b42318;
  font-weight: 600;
}
.gcl-cl.is-thua {
  color: #b45309;
  font-weight: 600;
}
.gcl-dl {
  margin-bottom: 1rem;
}
.gcl-tieu-de {
  font-size: 1rem;
  font-weight: 700;
  margin: 0.5rem 0;
}
</style>
