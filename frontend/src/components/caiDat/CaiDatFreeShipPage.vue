<script setup>
// Cài đặt → Cài đặt free ship (cô review 14/11: đơn giá trị lớn vẫn thu phí ship; ngưỡng miễn phí phải cấu hình được).
// Đơn có tiền hàng (đã giảm theo đợt, chưa trừ phiếu) từ ngưỡng trở lên thì miễn phí vận chuyển, ở cả bán quầy có giao hàng lẫn đặt online.
// Chưa đạt ngưỡng thì phí vẫn tính theo tỉnh nhận. Chỉ Quản lý vào được màn này (router + PhanQuyen).
// Luồng theo video tham khảo cô gửi: khối trên chỉ để xem mức đang áp dụng, bấm "Chỉnh sửa" mới hiện ô nhập.
import { computed, onMounted, reactive, ref } from 'vue'
import api from '../../services/api'
import MoneyInput from '../common/MoneyInput.vue'
import StatusSwitch from '../common/StatusSwitch.vue'
import ConfirmDialog from '../common/ConfirmDialog.vue'
import { useLoiForm } from '../../composables/useLoiForm'
import { useToast } from '../../composables/useToast'
import { useSuKien } from '../../composables/useSuKien'
import { nhanCaiDatVanChuyen } from '../../services/caiDatVanChuyen'
import { CHU_DE } from '../../services/suKien'
import { formatMoney, formatNgayGio } from '../../utils/format'
import { NHOM_PHI, PHI_MAC_DINH, phiVanChuyenTheoTinh } from '../../utils/phiVanChuyenTinh'

const NGUONG_TOI_DA = 10_000_000_000 // bằng backend CaiDatVanChuyenService.NGUONG_TOI_DA

const toast = useToast()
const dangTai = ref(true)
const loiTai = ref('')
const dangLuu = ref(false)
const hoiLuu = ref(false)
const dangSua = ref(false)
const daLuu = reactive({ mienPhiBat: false, mienPhiTu: 0, capNhatLuc: null })
const form = reactive({ mienPhiBat: false, mienPhiTu: '' })

const { loi, sua, cham, batDauGui, datLoiMayChu } = useLoiForm(() => {
  const e = {}
  const v = form.mienPhiTu
  if (v === '' || v === null) {
    if (form.mienPhiBat) e.mienPhiTu = 'Vui lòng nhập giá trị đơn tối thiểu để được miễn phí vận chuyển.'
  } else if (!(Number(v) >= 1)) e.mienPhiTu = 'Giá trị đơn tối thiểu phải lớn hơn 0.'
  else if (Number(v) > NGUONG_TOI_DA) e.mienPhiTu = 'Số tiền quá lớn.'
  return e
})

const coThayDoi = computed(() => form.mienPhiBat !== daLuu.mienPhiBat || Number(form.mienPhiTu || 0) !== Number(daLuu.mienPhiTu || 0))
const cauHoi = computed(() =>
  form.mienPhiBat
    ? `Đơn có tiền hàng từ ${formatMoney(form.mienPhiTu)} trở lên sẽ được miễn phí vận chuyển (bán tại quầy có giao hàng và đặt online). Các đơn đã tạo không đổi.`
    : 'Tắt miễn phí vận chuyển theo giá trị đơn: mọi đơn tính phí theo tỉnh nhận. Các đơn đã tạo không đổi.',
)

// Ví dụ theo mức đang áp dụng (đang sửa thì theo mức đang nhập): một đơn dưới mức và một đơn đúng bằng mức, cùng giao tới một tỉnh có tính phí
const TINH_VI_DU = 'Đà Nẵng'
const viDu = computed(() => {
  const bat = dangSua.value ? form.mienPhiBat : daLuu.mienPhiBat
  const n = Number(dangSua.value ? form.mienPhiTu : daLuu.mienPhiTu)
  if (!bat || !(n >= 1)) return null
  const duoi = n >= 4000 ? Math.floor((n * 0.75) / 1000) * 1000 : n - 1
  return { duoi, dat: n, phiTinh: phiVanChuyenTheoTinh(TINH_VI_DU) }
})
// Bảng phí theo tỉnh đang áp dụng (utils/phiVanChuyenTinh.js, khớp backend PhiVanChuyenTinh), chỉ để quản lý tham khảo
const bangPhi = [...NHOM_PHI.map(([phi, ds]) => ({ tinh: ds.join(', '), phi })), { tinh: 'Địa chỉ không nhận ra tên tỉnh', phi: PHI_MAC_DINH }]

function datForm() {
  form.mienPhiBat = daLuu.mienPhiBat
  form.mienPhiTu = daLuu.mienPhiTu || ''
}

function nhan(d) {
  daLuu.mienPhiBat = !!d?.mienPhiBat
  daLuu.mienPhiTu = Number(d?.mienPhiTu) || 0
  daLuu.capNhatLuc = d?.capNhatLuc || null
  datForm()
}

async function tai() {
  try {
    nhan((await api.get('/cai-dat/van-chuyen')).data)
    loiTai.value = ''
  } catch (e) {
    loiTai.value = e.message || 'Không tải được cài đặt.'
  } finally {
    dangTai.value = false
  }
}
onMounted(tai)
// Quản lý khác vừa đổi cài đặt ở máy khác: tải lại nếu mình không đang sửa
useSuKien([CHU_DE.CAI_DAT], () => {
  if (!dangSua.value && !dangLuu.value) tai()
})

function moSua() {
  datForm()
  dangSua.value = true
}

function huySua() {
  datForm()
  dangSua.value = false
}

function nhapNguong(v) {
  form.mienPhiTu = v
  sua('mienPhiTu')
}

function bamLuu() {
  if (!batDauGui()) return
  hoiLuu.value = true
}

async function luu() {
  dangLuu.value = true
  try {
    const d = (await api.put('/cai-dat/van-chuyen', { mienPhiBat: form.mienPhiBat, mienPhiTu: form.mienPhiTu === '' ? null : Number(form.mienPhiTu) })).data
    nhan(d)
    nhanCaiDatVanChuyen(d) // tab này không nhận tin của chính nó nên tự cập nhật cho màn bán quầy
    dangSua.value = false
    toast.success('Đã lưu cài đặt miễn phí vận chuyển.')
  } catch (e) {
    if (!datLoiMayChu(e)) toast.error(e.message || 'Không lưu được cài đặt. Vui lòng thử lại.')
  } finally {
    dangLuu.value = false
    hoiLuu.value = false
  }
}
</script>

<template>
  <div class="ad-page">
    <section v-if="dangTai" class="ad-card"><div class="ad-empty">Đang tải dữ liệu…</div></section>
    <section v-else-if="loiTai" class="ad-card"><div class="ad-empty">{{ loiTai }}</div></section>

    <div v-else class="cd-luoi">
      <section class="ad-card" data-testid="cd-free-ship">
        <div class="ad-card-head">
          <span class="ad-icon-box" aria-hidden="true"><i class="bi bi-truck"></i></span>
          <h2 class="ad-card-title">Miễn phí vận chuyển theo giá trị đơn</h2>
        </div>

        <!-- Mức đang áp dụng: chỉ để xem -->
        <dl class="cd-thong-tin" data-testid="cd-thong-tin">
          <div>
            <dt>Trạng thái</dt>
            <dd>
              <span class="ad-pill" :class="daLuu.mienPhiBat ? 'ad-pill-green' : 'ad-pill-red'" data-testid="cd-trang-thai">{{ daLuu.mienPhiBat ? 'Đang bật' : 'Đang tắt' }}</span>
            </dd>
          </div>
          <div>
            <dt>Giá trị đơn tối thiểu</dt>
            <dd data-testid="cd-nguong-hien-tai">{{ formatMoney(daLuu.mienPhiTu) }}</dd>
          </div>
          <div>
            <dt>Cập nhật lần cuối</dt>
            <dd data-testid="cd-cap-nhat">{{ daLuu.capNhatLuc ? formatNgayGio(daLuu.capNhatLuc) : 'Chưa chỉnh sửa (đang dùng mức mặc định)' }}</dd>
          </div>
        </dl>
        <p class="cd-tom-tat" data-testid="cd-mo-ta">
          <template v-if="daLuu.mienPhiBat">
            Đơn có tiền hàng từ <strong>{{ formatMoney(daLuu.mienPhiTu) }}</strong> trở lên được miễn phí vận chuyển. Đơn dưới mức này tính phí theo tỉnh nhận.
          </template>
          <template v-else>Mọi đơn tính phí vận chuyển theo tỉnh nhận.</template>
        </p>

        <button v-if="!dangSua" type="button" class="ad-btn ad-btn-primary" data-testid="cd-sua" @click="moSua">
          <i class="bi bi-pencil-square" aria-hidden="true"></i>
          Chỉnh sửa
        </button>

        <!-- Bấm "Chỉnh sửa" mới hiện phần nhập -->
        <form v-else class="cd-form" novalidate data-testid="cd-form" @submit.prevent="bamLuu">
          <div class="cd-dong">
            <div>
              <div class="cd-nhan">Miễn phí vận chuyển cho đơn đạt mức tối thiểu</div>
              <div class="cd-phu" :class="{ 'is-bat': form.mienPhiBat }">{{ form.mienPhiBat ? 'Bật' : 'Tắt' }}</div>
            </div>
            <StatusSwitch :on="form.mienPhiBat" label="Bật / tắt miễn phí vận chuyển theo giá trị đơn" data-testid="cd-bat" @toggle="form.mienPhiBat = !form.mienPhiBat" />
          </div>

          <div class="cd-o" @focusout="cham('mienPhiTu')">
            <label class="ad-label" for="cd-nguong">Giá trị đơn tối thiểu <span v-if="form.mienPhiBat" class="ad-required">*</span></label>
            <MoneyInput
              id="cd-nguong"
              :model-value="form.mienPhiTu"
              :invalid="!!loi.mienPhiTu"
              aria-label="Giá trị đơn tối thiểu để được miễn phí vận chuyển"
              placeholder="Ví dụ: 2.000.000"
              @update:model-value="nhapNguong"
            />
            <p v-if="loi.mienPhiTu" class="ad-error" data-testid="cd-loi-nguong">{{ loi.mienPhiTu }}</p>
            <p v-else class="ad-hint">Tính trên tiền hàng sau khi giảm theo đợt, chưa trừ phiếu giảm giá.</p>
          </div>

          <div class="cd-chan">
            <button type="button" class="ad-btn" :disabled="dangLuu" data-testid="cd-huy" @click="huySua">Hủy</button>
            <button type="submit" class="ad-btn ad-btn-primary" :disabled="dangLuu || !coThayDoi" data-testid="cd-luu">
              <span v-if="dangLuu" class="spinner-border spinner-border-sm" aria-hidden="true"></span>
              <i v-else class="bi bi-save" aria-hidden="true"></i>
              Lưu cài đặt
            </button>
          </div>
        </form>

        <p class="ad-hint cd-ghi-chu">Áp dụng cho đơn bán tại quầy có giao hàng và đơn đặt online. Đơn đã tạo không đổi.</p>
      </section>

      <div class="cd-cot-phai">
        <section v-if="viDu" class="ad-card" data-testid="cd-vi-du">
          <div class="ad-card-head">
            <span class="ad-icon-box" aria-hidden="true"><i class="bi bi-calculator"></i></span>
            <h2 class="ad-card-title">{{ dangSua ? 'Ví dụ với mức đang nhập' : 'Ví dụ với mức đang áp dụng' }}</h2>
          </div>
          <p class="ad-hint cd-vi-du-ghi-chu">Đơn giao tới {{ TINH_VI_DU }} (phí theo tỉnh {{ formatMoney(viDu.phiTinh) }}).</p>
          <dl class="cd-vi-du">
            <div>
              <dt>Tiền hàng {{ formatMoney(viDu.duoi) }}</dt>
              <dd>Phí vận chuyển {{ formatMoney(viDu.phiTinh) }}</dd>
            </div>
            <div>
              <dt>Tiền hàng {{ formatMoney(viDu.dat) }}</dt>
              <dd class="cd-mien-phi">Miễn phí</dd>
            </div>
          </dl>
        </section>

        <section class="ad-card" data-testid="cd-bang-phi">
          <div class="ad-card-head">
            <span class="ad-icon-box" aria-hidden="true"><i class="bi bi-geo-alt"></i></span>
            <h2 class="ad-card-title">Phí theo tỉnh khi chưa đạt mức miễn phí</h2>
          </div>
          <table class="cd-bang">
            <tbody>
              <tr v-for="d in bangPhi" :key="d.tinh">
                <td>{{ d.tinh }}</td>
                <td class="cd-bang-phi" :class="{ 'cd-mien-phi': d.phi === 0 }">{{ d.phi === 0 ? 'Miễn phí' : formatMoney(d.phi) }}</td>
              </tr>
            </tbody>
          </table>
        </section>
      </div>
    </div>

    <ConfirmDialog
      v-if="hoiLuu"
      title="Lưu cài đặt miễn phí vận chuyển?"
      :message="cauHoi"
      confirm-text="Lưu"
      :loading="dangLuu"
      @confirm="luu"
      @cancel="hoiLuu = false"
    />
  </div>
</template>

<style scoped>
.cd-luoi {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 1rem;
  align-items: start;
}
.cd-cot-phai {
  display: grid;
  gap: 1rem;
}
.cd-thong-tin {
  margin: 0 0 1rem;
  border: 1px solid var(--fs-line, #e6eaf0);
  border-radius: 12px;
  overflow: hidden;
}
.cd-thong-tin > div {
  display: grid;
  grid-template-columns: 42% 1fr;
  border-top: 1px solid var(--fs-line-soft, #eef1f5);
}
.cd-thong-tin > div:first-child {
  border-top: 0;
}
.cd-thong-tin dt {
  padding: 0.7rem 0.9rem;
  background: var(--fs-surface, #f7f9fc);
  color: var(--fs-muted, #7b8794);
  font-weight: 500;
  font-size: 0.86rem;
}
.cd-thong-tin dd {
  margin: 0;
  padding: 0.7rem 0.9rem;
  font-weight: 600;
  color: var(--fs-text, #18242f);
}
.cd-tom-tat {
  margin: 0 0 1rem;
  padding: 0.7rem 0.9rem;
  border-radius: 10px;
  background: var(--fs-primary-soft, #e7f1fd);
  color: var(--fs-text, #18242f);
  font-size: 0.9rem;
}
.cd-form {
  padding-top: 1rem;
  border-top: 1px solid var(--fs-line-soft, #eef1f5);
}
.cd-dong {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.85rem 1rem;
  margin-bottom: 1.1rem;
  background: var(--fs-surface, #f7f9fc);
  border: 1px solid var(--fs-line-soft, #eef1f5);
  border-radius: 12px;
}
.cd-nhan {
  font-weight: 600;
  color: var(--fs-text, #18242f);
}
.cd-phu {
  margin-top: 0.15rem;
  font-size: 0.82rem;
  color: var(--fs-muted, #7b8794);
}
.cd-phu.is-bat {
  color: var(--fs-success, #198754);
  font-weight: 600;
}
.cd-o {
  max-width: 340px;
  margin-bottom: 1rem;
}
.cd-chan {
  display: flex;
  justify-content: flex-end;
  gap: 0.6rem;
}
.cd-ghi-chu {
  margin-top: 1rem;
}
.cd-vi-du-ghi-chu {
  margin: -0.5rem 0 0.75rem;
}
.cd-vi-du {
  margin: 0;
  display: grid;
  gap: 0.5rem;
}
.cd-vi-du > div {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.65rem 0.9rem;
  border: 1px solid var(--fs-line, #e6eaf0);
  border-radius: 10px;
  font-size: 0.9rem;
}
.cd-vi-du dt {
  font-weight: 500;
}
.cd-vi-du dd {
  margin: 0;
  font-weight: 600;
}
.cd-mien-phi {
  color: var(--fs-success, #198754);
  font-weight: 600;
}
.cd-bang {
  width: 100%;
  font-size: 0.86rem;
  border-collapse: collapse;
}
.cd-bang td {
  padding: 0.5rem 0;
  border-top: 1px solid var(--fs-line-soft, #eef1f5);
  vertical-align: top;
}
.cd-bang tr:first-child td {
  border-top: 0;
}
.cd-bang-phi {
  text-align: right;
  white-space: nowrap;
  padding-left: 1rem !important;
  font-weight: 600;
}
@media (max-width: 992px) {
  .cd-luoi {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
