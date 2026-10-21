<script setup>
// Nhận ca / tổng kết ca / giao ca của người đang đăng nhập.
//  - Chưa nhận ca: nhập tiền đầu ca (gợi ý = tiền ca trước bàn giao; nhập khác thì bắt buộc ghi lý do), chọn ca trong lịch hôm nay.
//  - Đang trong ca: xem số hóa đơn, doanh thu, tiền mặt / chuyển khoản thu được; đếm tiền mặt thực tế, để lại tiền cho ca sau, giao ca.
//  - Giao xong: xem và in biên bản giao ca.
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import api from '../../services/api'
import BaseModal from '../common/BaseModal.vue'
import ConfirmDialog from '../common/ConfirmDialog.vue'
import MoneyInput from '../common/MoneyInput.vue'
import Select2 from '../common/Select2.vue'
import { useGiaoCa } from '../../composables/useGiaoCa'
import { useAuth } from '../../composables/useAuth'
import { useLoiForm } from '../../composables/useLoiForm'
import { useToast } from '../../composables/useToast'
import { formatMoney, formatNgayGio } from '../../utils/format'
import { inBienBanGiaoCa, moTaChenhLech, thoiLuong, tinhChenhLech } from '../../utils/giaoCa'
import { soHoaDonCho } from '../../utils/banHang'

const emit = defineEmits(['close'])
const toast = useToast()
const auth = useAuth()
const { trangThai, dangTrongCa, taiLai, nhanCa, ketCa } = useGiaoCa()

const TIEN_TOI_DA = 1_000_000_000
const dangGui = ref(false)
const ketQua = ref(null) // biên bản vừa giao ca
const bayGio = ref(new Date())
let henGio = null

onMounted(async () => {
  await taiLai()
  datMacDinhNhanCa()
  henGio = setInterval(() => (bayGio.value = new Date()), 30000)
})
onBeforeUnmount(() => clearInterval(henGio))

/* ===================== Nhận ca ===================== */
const nc = reactive({ tienDauCa: '', ghiChu: '', idLichLamViec: null })
function datMacDinhNhanCa() {
  if (nc.tienDauCa === '' && trangThai.goiYTienDauCa !== null) nc.tienDauCa = Number(trangThai.goiYTienDauCa)
}
const khacTienBanGiao = computed(
  () => trangThai.goiYTienDauCa !== null && nc.tienDauCa !== '' && Number(nc.tienDauCa) !== Number(trangThai.goiYTienDauCa),
)
const loiNhan = useLoiForm(() => {
  if (dangTrongCa.value || ketQua.value) return {}
  const e = {}
  if (nc.tienDauCa === '' || nc.tienDauCa === null) e.tienDauCa = 'Vui lòng nhập tiền đầu ca.'
  else if (Number(nc.tienDauCa) < 0) e.tienDauCa = 'Số tiền không được âm.'
  else if (Number(nc.tienDauCa) > TIEN_TOI_DA) e.tienDauCa = 'Số tiền tối đa 1.000.000.000 ₫.'
  const gc = nc.ghiChu.trim()
  if (gc.length > 500) e.ghiChu = 'Ghi chú tối đa 500 ký tự.'
  else if (!gc && khacTienBanGiao.value) {
    e.ghiChu = `Tiền đầu ca khác tiền ca trước bàn giao (${formatMoney(trangThai.goiYTienDauCa)}). Vui lòng ghi lý do.`
  }
  return e
})

async function guiNhanCa() {
  if (dangGui.value) return
  if (!loiNhan.batDauGui()) {
    loiNhan.focusLoiDau('#gc-nhan-form')
    return
  }
  dangGui.value = true
  try {
    const gc = await nhanCa({ tienDauCa: Number(nc.tienDauCa), ghiChu: nc.ghiChu.trim(), idLichLamViec: nc.idLichLamViec })
    toast.success(`Đã nhận ca ${gc.maGiaoCa}${gc.ngoaiLich ? ' (ngoài lịch)' : ` - ${gc.tenCa}`}.`)
    emit('close')
  } catch (e) {
    if (loiNhan.datLoiMayChu(e)) loiNhan.focusLoiDau('#gc-nhan-form')
    else toast.error(e.message || 'Không nhận ca được.')
    if (e.code === 'DANG_TRONG_CA') taiLai()
  } finally {
    dangGui.value = false
  }
}

/* ===================== Giao ca ===================== */
const kc = reactive({ tienMatThucTe: '', tienBanGiao: '', idNhanVienNhanBanGiao: '', ghiChu: '' })
const dsNhanVien = ref([])
const xacNhan = ref(false)
watch(
  dangTrongCa,
  async (v) => {
    if (!v || dsNhanVien.value.length) return
    try {
      const res = await api.get('/giao-ca/nhan-vien')
      dsNhanVien.value = res.data || []
    } catch {
      dsNhanVien.value = []
    }
  },
  { immediate: true },
)
const luaChonNhanVien = computed(() => [
  { value: '', label: 'Không chọn (ca sau tự nhận)' },
  ...dsNhanVien.value.map((n) => ({ value: String(n.id), label: `${n.hoTen} (${n.maNhanVien})` })),
])
const tk = computed(() => trangThai.tongKet || {})
const chenhLech = computed(() => tinhChenhLech(kc.tienMatThucTe, tk.value.tienMatHeThong))
const moTaCl = computed(() => moTaChenhLech(chenhLech.value))
const daLam = computed(() => (trangThai.dangMo ? thoiLuong(trangThai.dangMo.thoiGianNhanCa, bayGio.value) : ''))
const hoaDonCho = computed(() => soHoaDonCho(auth.trangThai.nguoiDung?.id))

const loiKet = useLoiForm(() => {
  if (!dangTrongCa.value || ketQua.value) return {}
  const e = {}
  const tt = kc.tienMatThucTe
  if (tt === '' || tt === null) e.tienMatThucTe = 'Vui lòng nhập tiền mặt thực tế đếm được.'
  else if (Number(tt) < 0) e.tienMatThucTe = 'Số tiền không được âm.'
  else if (Number(tt) > TIEN_TOI_DA) e.tienMatThucTe = 'Số tiền tối đa 1.000.000.000 ₫.'
  const bg = kc.tienBanGiao
  if (bg === '' || bg === null) e.tienBanGiao = 'Vui lòng nhập tiền để lại cho ca sau (không để lại thì nhập 0).'
  else if (Number(bg) < 0) e.tienBanGiao = 'Số tiền không được âm.'
  else if (!e.tienMatThucTe && Number(bg) > Number(tt)) e.tienBanGiao = 'Tiền bàn giao không được lớn hơn tiền mặt thực tế.'
  const gc = kc.ghiChu.trim()
  if (gc.length > 500) e.ghiChu = 'Ghi chú tối đa 500 ký tự.'
  else if (!gc && chenhLech.value !== null && chenhLech.value !== 0) e.ghiChu = `Tiền mặt ${moTaCl.value.nhan.toLowerCase()} so với hệ thống. Vui lòng ghi lý do.`
  return e
})

function moXacNhan() {
  if (!loiKet.batDauGui()) {
    loiKet.focusLoiDau('#gc-ket-form')
    return
  }
  xacNhan.value = true
}

async function guiKetCa() {
  if (dangGui.value) return
  dangGui.value = true
  try {
    ketQua.value = await ketCa({
      tienMatThucTe: Number(kc.tienMatThucTe),
      tienBanGiao: Number(kc.tienBanGiao),
      idNhanVienNhanBanGiao: kc.idNhanVienNhanBanGiao ? Number(kc.idNhanVienNhanBanGiao) : null,
      ghiChu: kc.ghiChu.trim(),
    })
    xacNhan.value = false
    toast.success(`Đã giao ca ${ketQua.value.maGiaoCa}.`)
  } catch (e) {
    xacNhan.value = false
    if (loiKet.datLoiMayChu(e)) loiKet.focusLoiDau('#gc-ket-form')
    else toast.error(e.message || 'Không giao ca được.')
  } finally {
    dangGui.value = false
  }
}

function inBienBan() {
  if (!inBienBanGiaoCa(ketQua.value)) toast.error('Trình duyệt chặn cửa sổ in. Hãy cho phép cửa sổ bật lên rồi thử lại.')
}

const tieuDe = computed(() => {
  if (ketQua.value) return `Đã giao ca ${ketQua.value.maGiaoCa}`
  if (dangTrongCa.value) return `Ca đang làm: ${trangThai.dangMo.maGiaoCa}`
  return 'Nhận ca'
})
</script>

<template>
  <BaseModal :title="tieuDe" size="lg" :static-backdrop="!!ketQua || dangGui" @close="emit('close')">
    <div v-if="!trangThai.daTai" class="ad-empty">Đang tải ca làm việc…</div>

    <!-- ===== Vừa giao ca xong ===== -->
    <div v-else-if="ketQua" class="gc-ket-qua">
      <div class="gc-thanh-cong"><i class="bi bi-check-circle-fill" aria-hidden="true"></i> Giao ca thành công lúc {{ formatNgayGio(ketQua.thoiGianKetCa) }}</div>
      <dl class="gc-luoi">
        <div><dt>Số hóa đơn</dt><dd>{{ ketQua.soHoaDon ?? 0 }}</dd></div>
        <div><dt>Doanh thu</dt><dd>{{ formatMoney(ketQua.doanhThu ?? 0) }}</dd></div>
        <div><dt>Tiền mặt hệ thống</dt><dd>{{ formatMoney(ketQua.tienMatHeThong ?? 0) }}</dd></div>
        <div><dt>Tiền mặt thực tế</dt><dd>{{ formatMoney(ketQua.tienMatThucTe ?? 0) }}</dd></div>
        <div><dt>Chênh lệch</dt><dd :class="`gc-cl is-${moTaChenhLech(ketQua.chenhLech).kieu}`">{{ moTaChenhLech(ketQua.chenhLech).nhan }}</dd></div>
        <div><dt>Bàn giao cho ca sau</dt><dd>{{ formatMoney(ketQua.tienBanGiao ?? 0) }}<small v-if="ketQua.tenNguoiNhanBanGiao"> · {{ ketQua.tenNguoiNhanBanGiao }}</small></dd></div>
      </dl>
    </div>

    <!-- ===== Chưa nhận ca ===== -->
    <form v-else-if="!dangTrongCa" id="gc-nhan-form" novalidate @submit.prevent="guiNhanCa">
      <div class="gc-ban-giao" role="note">
        <i class="bi bi-cash-stack" aria-hidden="true"></i>
        <span v-if="trangThai.goiYTienDauCa !== null">
          Ca trước do <b>{{ trangThai.nguoiBanGiao }}</b> bàn giao <b>{{ formatMoney(trangThai.goiYTienDauCa) }}</b>
          lúc {{ formatNgayGio(trangThai.thoiGianBanGiao) }}. Hãy đếm lại tiền trong két.
        </span>
        <span v-else>Chưa có ca nào bàn giao trước đó. Nhập số tiền mặt đang có trong két.</span>
      </div>

      <fieldset class="mb-3">
        <legend class="ad-label">Ca trong lịch hôm nay</legend>
        <p v-if="!trangThai.lichHomNay.length" class="ad-hint mb-0">Hôm nay bạn không có lịch làm. Ca sẽ được ghi là <b>ngoài lịch</b>.</p>
        <div v-else class="gc-lich">
          <label class="gc-lich-muc">
            <input v-model="nc.idLichLamViec" type="radio" :value="null" class="form-check-input" />
            <span>Tự chọn theo giờ hiện tại</span>
          </label>
          <label v-for="l in trangThai.lichHomNay" :key="l.id" class="gc-lich-muc">
            <input v-model="nc.idLichLamViec" type="radio" :value="l.id" class="form-check-input" />
            <span><b>{{ l.tenCa }}</b> {{ l.gioBatDau }} - {{ l.gioKetThuc }}</span>
          </label>
        </div>
      </fieldset>

      <label class="ad-label" for="gc-tien-dau">Tiền đầu ca <span class="ad-required">*</span></label>
      <MoneyInput
        id="gc-tien-dau"
        v-model="nc.tienDauCa"
        aria-label="Tiền đầu ca"
        placeholder="Số tiền mặt trong két khi nhận ca"
        :invalid="!!loiNhan.loi.value.tienDauCa"
        @update:model-value="loiNhan.sua('tienDauCa')"
      />
      <p v-if="loiNhan.loi.value.tienDauCa" class="ad-error">{{ loiNhan.loi.value.tienDauCa }}</p>

      <label class="ad-label mt-3" for="gc-ghi-chu-nhan">Ghi chú <span v-if="khacTienBanGiao" class="ad-required">*</span></label>
      <textarea
        id="gc-ghi-chu-nhan"
        v-model="nc.ghiChu"
        class="form-control ad-control"
        :class="{ 'is-invalid': loiNhan.loi.value.ghiChu }"
        rows="2"
        maxlength="500"
        placeholder="Bắt buộc khi tiền đầu ca khác tiền ca trước bàn giao"
        @input="loiNhan.sua('ghiChu')"
        @blur="loiNhan.cham('ghiChu')"
      ></textarea>
      <p v-if="loiNhan.loi.value.ghiChu" class="ad-error">{{ loiNhan.loi.value.ghiChu }}</p>
    </form>

    <!-- ===== Đang trong ca ===== -->
    <div v-else>
      <div class="gc-dau">
        <div>
          <div class="gc-ma">{{ trangThai.dangMo.maGiaoCa }} · {{ trangThai.dangMo.tenNhanVien }}</div>
          <div class="ad-hint">
            Nhận ca lúc {{ formatNgayGio(trangThai.dangMo.thoiGianNhanCa) }} · đã làm {{ daLam }}
          </div>
        </div>
        <div class="d-flex flex-wrap gap-2">
          <span v-if="trangThai.dangMo.ngoaiLich" class="ad-pill ad-pill-gray">Ngoài lịch</span>
          <span v-else class="ad-pill ad-pill-blue">{{ trangThai.dangMo.tenCa }} {{ trangThai.dangMo.gioCa }}</span>
          <span v-if="trangThai.dangMo.diMuon" class="ad-pill ad-pill-orange">Đi muộn {{ trangThai.dangMo.soPhutDiMuon }} phút</span>
        </div>
      </div>

      <dl class="gc-luoi">
        <div><dt>Số hóa đơn</dt><dd>{{ tk.soHoaDon ?? 0 }}</dd></div>
        <div><dt>Doanh thu</dt><dd>{{ formatMoney(tk.doanhThu ?? 0) }}</dd></div>
        <div><dt>Tiền mặt thu</dt><dd>{{ formatMoney(tk.tienMat ?? 0) }}</dd></div>
        <div><dt>Chuyển khoản thu</dt><dd>{{ formatMoney(tk.chuyenKhoan ?? 0) }}</dd></div>
        <div><dt>Tiền đầu ca</dt><dd>{{ formatMoney(tk.tienDauCa ?? 0) }}</dd></div>
        <div class="is-noi-bat"><dt>Tiền mặt phải có</dt><dd>{{ formatMoney(tk.tienMatHeThong ?? 0) }}</dd></div>
      </dl>
      <button type="button" class="ad-btn ad-btn-sm mb-3" :disabled="trangThai.dangTai" @click="taiLai">
        <i class="bi bi-arrow-clockwise" aria-hidden="true"></i> Cập nhật số liệu
      </button>

      <div v-if="hoaDonCho > 0" class="gc-canh-bao" role="alert">
        <i class="bi bi-exclamation-triangle" aria-hidden="true"></i>
        Còn {{ hoaDonCho }} hóa đơn chờ chưa thanh toán ở màn hình Bán hàng tại quầy. Hãy thanh toán hoặc xóa trước khi giao ca.
      </div>

      <form id="gc-ket-form" novalidate class="gc-ket" @submit.prevent="moXacNhan">
        <h3 class="gc-tieu-de-nho">Giao ca</h3>
        <div class="row g-3">
          <div class="col-md-6">
            <label class="ad-label" for="gc-thuc-te">Tiền mặt thực tế <span class="ad-required">*</span></label>
            <MoneyInput
              id="gc-thuc-te"
              v-model="kc.tienMatThucTe"
              aria-label="Tiền mặt thực tế"
              placeholder="Tiền mặt đếm được trong két"
              :invalid="!!loiKet.loi.value.tienMatThucTe"
              @update:model-value="loiKet.sua('tienMatThucTe')"
            />
            <p v-if="loiKet.loi.value.tienMatThucTe" class="ad-error">{{ loiKet.loi.value.tienMatThucTe }}</p>
            <p v-else class="ad-hint">Chênh lệch: <b :class="`gc-cl is-${moTaCl.kieu}`">{{ moTaCl.nhan }}</b></p>
          </div>
          <div class="col-md-6">
            <label class="ad-label" for="gc-ban-giao">Để lại cho ca sau <span class="ad-required">*</span></label>
            <MoneyInput
              id="gc-ban-giao"
              v-model="kc.tienBanGiao"
              aria-label="Tiền để lại cho ca sau"
              placeholder="Không để lại thì nhập 0"
              :invalid="!!loiKet.loi.value.tienBanGiao"
              @update:model-value="loiKet.sua('tienBanGiao')"
            />
            <p v-if="loiKet.loi.value.tienBanGiao" class="ad-error">{{ loiKet.loi.value.tienBanGiao }}</p>
            <div class="gc-goi-y">
              <button type="button" class="dn-chip" @click="kc.tienBanGiao = Number(tk.tienDauCa || 0); loiKet.sua('tienBanGiao')">Bằng tiền đầu ca</button>
              <button type="button" class="dn-chip" :disabled="kc.tienMatThucTe === ''" @click="kc.tienBanGiao = Number(kc.tienMatThucTe); loiKet.sua('tienBanGiao')">Để lại toàn bộ</button>
            </div>
          </div>
          <div class="col-md-6">
            <label class="ad-label" for="gc-nguoi-nhan">Người nhận bàn giao</label>
            <Select2 id="gc-nguoi-nhan" v-model="kc.idNhanVienNhanBanGiao" :options="luaChonNhanVien" class="ad-control" :invalid="!!loiKet.loi.value.idNhanVienNhanBanGiao" />
            <p v-if="loiKet.loi.value.idNhanVienNhanBanGiao" class="ad-error">{{ loiKet.loi.value.idNhanVienNhanBanGiao }}</p>
          </div>
          <div class="col-md-6">
            <label class="ad-label" for="gc-ghi-chu-ket">Ghi chú <span v-if="chenhLech !== null && chenhLech !== 0" class="ad-required">*</span></label>
            <textarea
              id="gc-ghi-chu-ket"
              v-model="kc.ghiChu"
              class="form-control ad-control"
              :class="{ 'is-invalid': loiKet.loi.value.ghiChu }"
              rows="2"
              maxlength="500"
              placeholder="Bắt buộc khi tiền mặt lệch so với hệ thống"
              @input="loiKet.sua('ghiChu')"
              @blur="loiKet.cham('ghiChu')"
            ></textarea>
            <p v-if="loiKet.loi.value.ghiChu" class="ad-error">{{ loiKet.loi.value.ghiChu }}</p>
          </div>
        </div>
      </form>
    </div>

    <template #footer>
      <template v-if="ketQua">
        <button type="button" class="ad-btn" @click="emit('close')">Đóng</button>
        <button type="button" class="ad-btn ad-btn-primary" @click="inBienBan"><i class="bi bi-printer" aria-hidden="true"></i> In biên bản giao ca</button>
      </template>
      <template v-else-if="trangThai.daTai && !dangTrongCa">
        <button type="button" class="ad-btn" @click="emit('close')">Để sau</button>
        <button type="submit" form="gc-nhan-form" class="ad-btn ad-btn-primary" :disabled="dangGui">Nhận ca</button>
      </template>
      <template v-else-if="trangThai.daTai">
        <button type="button" class="ad-btn" @click="emit('close')">Đóng</button>
        <button type="submit" form="gc-ket-form" class="ad-btn ad-btn-danger" :disabled="dangGui">Giao ca</button>
      </template>
    </template>
  </BaseModal>

  <ConfirmDialog
    v-if="xacNhan"
    title="Giao ca"
    :message="`Kết thúc ca ${trangThai.dangMo?.maGiaoCa}? Tiền mặt thực tế ${formatMoney(Number(kc.tienMatThucTe))} (${moTaCl.nhan.toLowerCase()}), để lại ${formatMoney(Number(kc.tienBanGiao))} cho ca sau. Sau khi giao ca không bán hàng tiếp trong ca này được.`"
    confirm-text="Giao ca"
    variant="danger"
    :loading="dangGui"
    @confirm="guiKetCa"
    @cancel="xacNhan = false"
  />
</template>

<style scoped>
.gc-ban-giao,
.gc-canh-bao {
  display: flex;
  gap: 0.6rem;
  align-items: flex-start;
  padding: 0.75rem 0.9rem;
  border-radius: 10px;
  background: var(--fs-primary-soft, #e7f1fd);
  color: var(--fs-primary-dark, #075fc0);
  margin-bottom: 1rem;
}
.gc-canh-bao {
  background: #fff4e5;
  color: #92400e;
}
.gc-lich {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}
.gc-lich-muc {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  border: 1px solid var(--fs-line, #e6eaf0);
  border-radius: 10px;
  padding: 0.45rem 0.75rem;
  cursor: pointer;
}
.gc-dau {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
  margin-bottom: 1rem;
}
.gc-ma {
  font-weight: 700;
  font-size: 1.05rem;
}
.gc-luoi {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.75rem;
  margin: 0 0 1rem;
}
.gc-luoi > div {
  border: 1px solid var(--fs-line, #e6eaf0);
  border-radius: 12px;
  padding: 0.65rem 0.85rem;
  background: var(--fs-surface, #f7f9fc);
}
.gc-luoi > div.is-noi-bat {
  background: var(--fs-primary-soft, #e7f1fd);
  border-color: var(--fs-primary-line, #c9def9);
}
.gc-luoi dt {
  font-weight: 500;
  font-size: 0.85rem;
  color: var(--fs-muted, #7b8794);
}
.gc-luoi dd {
  margin: 0.15rem 0 0;
  font-weight: 700;
  font-size: 1.05rem;
}
.gc-ket {
  border-top: 1px solid var(--fs-line, #e6eaf0);
  padding-top: 1rem;
}
.gc-tieu-de-nho {
  font-size: 1rem;
  font-weight: 700;
  margin: 0 0 0.75rem;
}
.gc-cl.is-ok {
  color: #146c43;
}
.gc-cl.is-thieu {
  color: #b42318;
}
.gc-cl.is-thua {
  color: #b45309;
}
.gc-goi-y {
  display: flex;
  gap: 0.4rem;
  margin-top: 0.4rem;
}
.dn-chip {
  border: 1px solid var(--fs-line, #e6eaf0);
  background: #fff;
  border-radius: 999px;
  padding: 0.2rem 0.65rem;
  font-size: 0.82rem;
}
.dn-chip:disabled {
  opacity: 0.5;
}
.gc-thanh-cong {
  display: flex;
  gap: 0.5rem;
  align-items: center;
  color: #146c43;
  font-weight: 600;
  margin-bottom: 1rem;
}
@media (max-width: 700px) {
  .gc-luoi {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
