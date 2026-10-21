<script setup>
// Lịch sử giao ca. Quản lý xem mọi ca (lọc theo nhân viên), nhân viên chỉ xem ca của mình.
// Chênh lệch tiền mặt tô màu (thiếu đỏ / thừa cam); bấm một dòng để xem chi tiết, các hóa đơn trong ca và in biên bản.
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import BaseModal from '../common/BaseModal.vue'
import DateInput from '../common/DateInput.vue'
import FilterCard from '../common/FilterCard.vue'
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

async function tai() {
  if (loc.tuNgay && loc.denNgay && loc.tuNgay > loc.denNgay) {
    toast.error('Từ ngày phải trước hoặc cùng ngày với Đến ngày.')
    return
  }
  dangTai.value = true
  try {
    ds.value = await lichLamViecService.lichSuGiaoCa({
      tuNgay: loc.tuNgay || undefined,
      denNgay: loc.denNgay || undefined,
      idNhanVien: loc.idNhanVien || undefined,
    })
  } catch (e) {
    toast.error(e.message || 'Không tải được lịch sử giao ca.')
  } finally {
    dangTai.value = false
  }
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
})
function datLai() {
  Object.assign(loc, { tuNgay: congNgayIso(todayIso(), -29), denNgay: todayIso(), idNhanVien: '' })
  tai()
}

const chiTiet = ref(null)
async function moChiTiet(g) {
  try {
    chiTiet.value = await lichLamViecService.chiTietGiaoCa(g.id)
  } catch (e) {
    toast.error(e.message || 'Không tải được chi tiết ca.')
  }
}
function inBienBan() {
  if (!inBienBanGiaoCa(chiTiet.value)) toast.error('Trình duyệt chặn cửa sổ in. Hãy cho phép cửa sổ bật lên rồi thử lại.')
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
        <span class="ad-count">{{ tong.soCa }} ca · doanh thu {{ formatMoney(tong.doanhThu) }} · {{ tong.lech }} ca lệch tiền</span>
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
      <template #footer>
        <button type="button" class="ad-btn" @click="chiTiet = null">Đóng</button>
        <button v-if="!chiTiet.dangTrongCa" type="button" class="ad-btn ad-btn-primary" @click="inBienBan"><i class="bi bi-printer" aria-hidden="true"></i> In biên bản</button>
      </template>
    </BaseModal>
  </div>
</template>

<style scoped>
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
