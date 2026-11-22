<script setup>
// Khối "MÃ GIẢM GIÁ" ở trang thanh toán: nút nhỏ góc trên "Áp / Đổi phiếu tốt nhất" + ô nhập / chọn mã (mã tốt nhất xếp đầu, không tự áp).
// Dùng chung giỏ hàng (cart store) với trang giỏ hàng, nên mã áp ở đâu cũng được giữ lại.
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import api from '../../services/api'
import { laKhachDangNhap, phieuChoGio, phieuTotNhat, taiPhieuChoKhach } from '../../services/phieuKhachService'
import { useCartStore } from '../../stores/cart'
import { formatMoney } from '../../utils/format'

const cartStore = useCartStore()

const danhSachMa = ref([])
const loi = ref('')
const dangKiemTra = ref(false)
const moDanhSach = ref(false)
const comboRef = ref(null)

async function taiDanhSachMa() {
  danhSachMa.value = await taiPhieuChoKhach(Math.round(cartStore.subTotal))
}

// 1.500.000 -> 1,5tr ; 200.000 -> 200k
function tienGon(n) {
  const v = Number(n) || 0
  if (v >= 1_000_000) return `${+(v / 1_000_000).toFixed(2)}tr`.replace('.', ',')
  if (v >= 1000) return `${Math.round(v / 1000)}k`
  return `${v}đ`
}
function dieuKienMa(v) {
  const p = []
  if (v.loaiGiam === 1 && v.giamToiDa) p.push(`Tối đa ${tienGon(v.giamToiDa)}`)
  p.push(v.donToiThieu > 0 ? `Đơn từ ${tienGon(v.donToiThieu)}` : 'Mọi đơn hàng')
  return p.join(' · ')
}

const maHopLe = computed(() => {
  const now = new Date()
  return (danhSachMa.value || []).filter((v) => {
    if (v.trangThai !== undefined && v.trangThai !== null && Number(v.trangThai) === 0) return false
    if (v.ngayKetThuc && new Date(v.ngayKetThuc) < now) return false
    if (v.lyDo && (v.lyDo.includes('hết hạn') || v.lyDo.includes('ngừng hoạt động'))) return false
    // Ẩn toàn bộ các phiếu giảm giá không đủ điều kiện áp dụng
    if (!v.dungDuoc) return false
    return true
  })
})

const maSapXep = computed(() =>
  [...maHopLe.value].sort(
    (a, b) => Number(b.dungDuoc) - Number(a.dungDuoc) || Number(b.soTienGiam || 0) - Number(a.soTienGiam || 0),
  ),
)
// Khách chọn phiếu từ danh sách (góp ý 22/11: không bắt gõ / tìm mã), mã giảm nhiều nhất xếp đầu
const maLoc = maSapXep
const maTotNhat = computed(() => phieuTotNhat(danhSachMa.value))
// Nút nhỏ góc trên "Áp / Đổi phiếu tốt nhất" (góp ý 22/11): chỉ hiện khi mã tốt nhất CHƯA được áp dụng.
// Không tự áp phiếu nào: khách tự bấm nút hoặc chọn trong danh sách (mã giảm nhiều nhất xếp đầu).
const goiY = computed(() => (maTotNhat.value && cartStore.appliedVoucher?.ma !== maTotNhat.value.ma ? maTotNhat.value : null))

async function apDung(maNhap) {
  loi.value = ''
  const code = String(maNhap ?? '').trim().toUpperCase()
  if (!code) {
    moDanhSach.value = !moDanhSach.value
    loi.value = ''
    return
  }
  if (cartStore.appliedVoucher?.ma?.toUpperCase() === code) {
    moDanhSach.value = false
    return
  }
  dangKiemTra.value = true
  try {
    if (laKhachDangNhap()) {
      const found = (
        await api.get(`/khach/phieu-giam-gia/${encodeURIComponent(code)}`, { params: { tongTien: Math.round(cartStore.subTotal) } })
      ).data
      cartStore.setVoucher(phieuChoGio(found))
    } else {
      await taiDanhSachMa()
      const found = danhSachMa.value.find((v) => String(v.ma || '').toUpperCase() === code)
      if (!found) {
        loi.value = 'Mã giảm giá không tồn tại hoặc cần đăng nhập để sử dụng.'
        return
      }
      if (!found.dungDuoc) {
        loi.value = found.lyDo || 'Đơn hàng chưa đủ điều kiện áp dụng mã này.'
        return
      }
      cartStore.setVoucher(phieuChoGio(found))
    }
    moDanhSach.value = false
  } catch (err) {
    loi.value =
      err?.status === 401 || err?.status === 403
        ? 'Vui lòng đăng nhập tài khoản khách hàng để dùng mã giảm giá.'
        : err?.message || 'Không thể kiểm tra mã giảm giá lúc này.'
  } finally {
    dangKiemTra.value = false
  }
}

function chonMa(v) {
  if (!v.dungDuoc || dangKiemTra.value) return
  moDanhSach.value = false
  apDung(v.ma)
}

function boMa() {
  cartStore.removeVoucher()
  loi.value = ''
}

function dongKhiBamNgoai(e) {
  if (comboRef.value && !comboRef.value.contains(e.target)) moDanhSach.value = false
}
onMounted(() => {
  taiDanhSachMa()
  document.addEventListener('click', dongKhiBamNgoai)
})
onBeforeUnmount(() => document.removeEventListener('click', dongKhiBamNgoai))
watch(() => cartStore.subTotal, taiDanhSachMa)
</script>

<template>
  <div class="vp">
    <div class="vp-title">
      <i class="bi bi-ticket-perforated-fill text-primary"></i> MÃ GIẢM GIÁ
      <button
        v-if="goiY"
        type="button"
        class="vp-nut-tot"
        :disabled="dangKiemTra"
        :title="`Áp mã ${goiY.ma}, tiết kiệm ${formatMoney(goiY.soTienGiam)}`"
        data-testid="vp-ap-phieu-tot"
        @click="apDung(goiY.ma)"
      >
        {{ cartStore.appliedVoucher ? 'Đổi phiếu tốt nhất' : 'Áp phiếu tốt nhất' }}
      </button>
    </div>

    <!-- Mã đang áp dụng: hiện ngay trên ô mã (cô review 14/11 [26:19]), bấm X để bỏ -->
    <div v-if="cartStore.appliedVoucher" class="vp-applied" data-testid="vp-dang-ap">
      <i class="bi bi-check-circle-fill" aria-hidden="true"></i>
      <span class="vp-applied-text">
        Đang áp dụng mã <strong>{{ cartStore.appliedVoucher.ma }}</strong>
        <template v-if="cartStore.discountAmount > 0"> · giảm {{ formatMoney(cartStore.discountAmount) }}</template>
      </span>
      <button type="button" class="vp-x" title="Bỏ mã này" aria-label="Bỏ mã giảm giá đang áp" @click="boMa"><i class="bi bi-x-lg"></i></button>
    </div>

    <div ref="comboRef" class="vp-combo">
      <button
        type="button"
        class="vp-chon"
        :class="{ 'is-mo': moDanhSach }"
        :disabled="dangKiemTra"
        aria-haspopup="listbox"
        :aria-expanded="moDanhSach"
        data-testid="vp-chon-phieu"
        @click="loi = ''; moDanhSach = !moDanhSach"
        @keyup.esc="moDanhSach = false"
      >
        <span v-if="dangKiemTra" class="spinner-border spinner-border-sm"></span>
        <span class="vp-chon-chu">
          <template v-if="cartStore.appliedVoucher">Đổi phiếu giảm giá</template>
          <template v-else-if="maHopLe.length">Chọn phiếu giảm giá · {{ maHopLe.length }} mã</template>
          <template v-else>Chưa có phiếu dùng được cho đơn này</template>
        </span>
        <i class="bi bi-chevron-down vp-chon-mui" aria-hidden="true"></i>
      </button>

      <!-- Bảng danh sách mã giảm giá dạng Coupon (chỉ hiện mã đủ điều kiện áp dụng) -->
      <div v-if="moDanhSach" class="vp-dropdown">
        <div class="vp-dropdown-head">
          <span><i class="bi bi-ticket-perforated-fill me-1"></i> Mã giảm giá đủ điều kiện ({{ maLoc.length }})</span>
          <button type="button" class="vp-close-dropdown-btn" @click="moDanhSach = false" aria-label="Đóng"><i class="bi bi-x-lg"></i></button>
        </div>
        <div v-if="!maLoc.length" class="vp-empty">
          <i class="bi bi-info-circle me-1"></i> Không có mã giảm giá nào đủ điều kiện áp dụng cho đơn hàng hiện tại.
        </div>
        <div v-else class="vp-list">
          <div
            v-for="v in maLoc"
            :key="v.ma"
            class="vp-coupon-card"
            :class="{ current: cartStore.appliedVoucher?.ma === v.ma }"
            role="button"
            tabindex="0"
            @click="chonMa(v)"
            @keyup.enter="chonMa(v)"
          >
            <!-- Phần bên trái: Huy hiệu mức giảm dạng cuống vé -->
            <div class="vp-coupon-left">
              <div class="vp-coupon-cut top"></div>
              <div class="vp-coupon-cut bottom"></div>
              <div class="vp-coupon-val">{{ v.loaiGiam === 1 ? `${v.giaTriGiam}%` : tienGon(v.giaTriGiam) }}</div>
              <div class="vp-coupon-lbl">GIẢM</div>
            </div>

            <!-- Phần bên phải: Chi tiết mã & nút bấm -->
            <div class="vp-coupon-right">
              <div class="vp-coupon-header">
                <span class="vp-coupon-code">{{ v.ma }}</span>
                <span v-if="cartStore.appliedVoucher?.ma === v.ma" class="vp-tag now">
                  <i class="bi bi-check-circle-fill me-1"></i>Đang dùng
                </span>
                <span v-else-if="maTotNhat?.ma === v.ma" class="vp-tag best">
                  <i class="bi bi-stars me-1"></i>Tốt nhất
                </span>
                <span v-else-if="v.caNhan" class="vp-tag">
                  <i class="bi bi-gift-fill me-1"></i>Tặng riêng
                </span>
              </div>
              <div class="vp-coupon-desc">{{ dieuKienMa(v) }}</div>
              <div class="vp-coupon-footer">
                <span class="vp-coupon-save">Tiết kiệm: <strong>-{{ formatMoney(v.soTienGiam) }}</strong></span>
                <button
                  type="button"
                  class="vp-coupon-btn"
                  :class="{ active: cartStore.appliedVoucher?.ma === v.ma }"
                  @click.stop="chonMa(v)"
                >
                  {{ cartStore.appliedVoucher?.ma === v.ma ? 'Đã chọn' : 'Áp dụng' }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div v-if="loi" class="vp-error">{{ loi }}</div>
  </div>
</template>

<style scoped>
.vp {
  margin-bottom: 1rem;
}
.vp-title {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  margin-bottom: 0.6rem;
  font-size: 0.78rem;
  font-weight: 800;
  letter-spacing: 0.04em;
  color: #1f2937;
  white-space: nowrap;
}

/* Nút nhỏ góc trên: áp / đổi sang mã giảm nhiều nhất */
.vp-nut-tot {
  margin-left: auto;
  flex-shrink: 0;
  padding: 0.12rem 0.55rem;
  font-size: 0.7rem;
  font-weight: 700;
  letter-spacing: 0;
  color: #fff;
  white-space: nowrap;
  background: #2563eb;
  border: 0;
  border-radius: 999px;
}
.vp-nut-tot:hover:not(:disabled) {
  background: #1d4ed8;
}
.vp-nut-tot:disabled {
  opacity: 0.6;
}

/* Mã đang áp dụng */
.vp-applied {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  margin-bottom: 0.6rem;
  font-size: 0.8rem;
  color: #166534;
  background: #ecf8f0;
  border: 1px solid #bfe5cc;
  border-radius: 10px;
}
.vp-applied-text {
  flex: 1;
  min-width: 0;
}
.vp-x {
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  padding: 0;
  font-size: 0.68rem;
  color: #6b7280;
  background: transparent;
  border: 0;
  border-radius: 50%;
}
.vp-x:hover {
  color: #b91c1c;
  background: #fde8e8;
}

/* Ô nhập */
.vp-combo {
  position: relative;
}
.vp-chon {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  width: 100%;
  padding: 0.55rem 0.8rem;
  font-size: 0.85rem;
  color: #1f2937;
  text-align: left;
  background: #fff;
  border: 1px solid #d5dbe5;
  border-radius: 8px;
}
.vp-chon:hover:not(:disabled),
.vp-chon.is-mo {
  border-color: #0d6efd;
  box-shadow: 0 0 0 3px rgba(13, 110, 253, 0.12);
}
.vp-chon-chu {
  flex: 1;
  min-width: 0;
}
.vp-chon-mui {
  color: #64748b;
  transition: transform 0.15s ease;
}
.vp-chon.is-mo .vp-chon-mui {
  transform: rotate(180deg);
}
.vp-error {
  margin-top: 0.4rem;
  font-size: 0.78rem;
  color: #dc2626;
}


/* Bảng chọn mã xổ xuống */
.vp-dropdown {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  z-index: 30;
  background: #fff;
  border: 1px solid #cbd5e1;
  border-radius: 14px;
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.16);
  overflow: hidden;
}
.vp-dropdown-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.55rem 0.85rem;
  font-size: 0.78rem;
  font-weight: 700;
  color: #0d6efd;
  background: linear-gradient(90deg, #eff6ff, #f8fafc);
  border-bottom: 1px solid #e2e8f0;
}
.vp-close-dropdown-btn {
  background: transparent;
  border: 0;
  padding: 0.2rem;
  color: #64748b;
  font-size: 0.75rem;
  line-height: 1;
  cursor: pointer;
}
.vp-close-dropdown-btn:hover {
  color: #0f172a;
}
.vp-empty {
  padding: 1.2rem 1rem;
  font-size: 0.82rem;
  color: #64748b;
  text-align: center;
  line-height: 1.5;
}
.vp-list {
  max-height: 290px;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: 0.6rem;
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
  scrollbar-width: thin;
}

/* Thẻ coupon phiếu giảm giá. flex-shrink: 0: danh sách là cột flex có max-height, thẻ có overflow hidden
   nên nếu cho co thì các thẻ bị ép thấp lại và cắt mất dòng "Tiết kiệm" + nút Áp dụng (để danh sách tự cuộn). */
.vp-coupon-card {
  flex-shrink: 0;
  display: flex;
  align-items: stretch;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  transition: all 0.2s ease;
}
.vp-coupon-card:hover {
  border-color: #0d6efd;
  box-shadow: 0 4px 14px rgba(13, 110, 253, 0.12);
  transform: translateY(-1px);
}
.vp-coupon-card.current {
  border-color: #22c55e;
  background: #f0fdf4;
}

/* Cột cuống vé bên trái */
.vp-coupon-left {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 82px;
  flex-shrink: 0;
  background: linear-gradient(135deg, #1e40af, #0d6efd);
  color: #fff;
  padding: 0.5rem 0.2rem;
  border-right: 1px dashed rgba(255, 255, 255, 0.5);
}
.vp-coupon-card.current .vp-coupon-left {
  background: linear-gradient(135deg, #15803d, #22c55e);
}
.vp-coupon-val {
  font-size: 0.95rem;
  font-weight: 800;
  line-height: 1.1;
  text-align: center;
}
.vp-coupon-lbl {
  font-size: 0.58rem;
  font-weight: 700;
  letter-spacing: 1px;
  opacity: 0.9;
  margin-top: 2px;
}
.vp-coupon-cut {
  position: absolute;
  right: -6px;
  width: 11px;
  height: 11px;
  background: #fff;
  border-radius: 50%;
  z-index: 2;
}
.vp-coupon-cut.top {
  top: -6px;
}
.vp-coupon-cut.bottom {
  bottom: -6px;
}

/* Cột thông tin bên phải */
.vp-coupon-right {
  flex: 1;
  min-width: 0;
  padding: 0.6rem 0.75rem;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 0.25rem;
}
.vp-coupon-header {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.35rem;
}
.vp-coupon-code {
  font-size: 0.88rem;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: 0.5px;
}
.vp-coupon-desc {
  font-size: 0.74rem;
  color: #64748b;
  line-height: 1.3;
}
.vp-coupon-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 0.35rem;
  margin-top: 0.25rem;
  padding-top: 0.35rem;
  border-top: 1px dashed #f1f5f9;
}
.vp-coupon-save {
  font-size: 0.75rem;
  color: #0f172a;
}
.vp-coupon-save strong {
  color: #0d6efd;
  font-size: 0.82rem;
}
.vp-coupon-btn {
  padding: 0.22rem 0.65rem;
  font-size: 0.72rem;
  font-weight: 700;
  color: #fff;
  background: #0d6efd;
  border: 0;
  border-radius: 6px;
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.15s ease;
}
.vp-coupon-btn:hover {
  background: #0b5ed7;
}
.vp-coupon-btn.active {
  background: #16a34a;
}

/* Huy hiệu thẻ */
.vp-tag {
  display: inline-flex;
  align-items: center;
  padding: 0.1rem 0.45rem;
  font-size: 0.64rem;
  font-weight: 700;
  color: #1e40af;
  background: #e0edff;
  border-radius: 999px;
  white-space: nowrap;
}
.vp-tag.now {
  color: #166534;
  background: #d1f0dc;
}
.vp-tag.best {
  color: #fff;
  background: #0d6efd;
}
</style>
