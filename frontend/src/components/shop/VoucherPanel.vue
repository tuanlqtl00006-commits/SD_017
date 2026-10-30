<script setup>
// Khối "MÃ GIẢM GIÁ" ở trang thanh toán: banner gợi ý mã giảm nhiều nhất (Dùng ngay) + ô nhập / chọn mã.
// Dùng chung giỏ hàng (cart store) với trang giỏ hàng, nên mã áp ở đâu cũng được giữ lại.
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import api from '../../services/api'
import { useCartStore } from '../../stores/cart'
import { formatMoney } from '../../utils/format'

const cartStore = useCartStore()

const danhSachMa = ref([])
const voucherCode = ref('')
const loi = ref('')
const dangKiemTra = ref(false)
const moDanhSach = ref(false)
const comboRef = ref(null)

async function taiDanhSachMa() {
  const tong = Math.round(cartStore.subTotal)
  try {
    danhSachMa.value = (await api.get('/khach/phieu-giam-gia', { params: { tongTien: tong } })).data
  } catch {
    // Chưa đăng nhập khách hàng: chỉ xem được các mã công khai
    try {
      const ds = (await api.get('/cua-hang/phieu-giam-gia', { params: { tongTien: tong } })).data
      danhSachMa.value = ds.map((p) => ({
        ma: p.maPhieu,
        ten: p.tenPhieu,
        caNhan: false,
        loaiGiam: p.hinhThucGiam,
        giaTriGiam: p.giaTriGiam,
        giamToiDa: p.giamToiDa,
        donToiThieu: p.donToiThieu,
        dungDuoc: p.duDieuKien,
        lyDo: p.duDieuKien ? null : `Cần mua thêm ${formatMoney(p.donToiThieu - tong)}`,
        soTienGiam: p.duDieuKien ? tinhGiamTam(p, tong) : 0,
      }))
    } catch {
      danhSachMa.value = []
    }
  }
}

function tinhGiamTam(p, tong) {
  const gt = Number(p.giaTriGiam) || 0
  if (p.hinhThucGiam === 1) {
    const giam = Math.floor((tong * Math.min(100, gt)) / 100)
    return p.giamToiDa ? Math.min(giam, Number(p.giamToiDa)) : giam
  }
  return Math.min(gt, tong)
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

const maSapXep = computed(() =>
  [...danhSachMa.value].sort(
    (a, b) => Number(b.dungDuoc) - Number(a.dungDuoc) || Number(b.soTienGiam || 0) - Number(a.soTienGiam || 0),
  ),
)
const maLoc = computed(() => {
  const q = voucherCode.value.trim().toUpperCase()
  return q ? maSapXep.value.filter((v) => v.ma.toUpperCase().includes(q)) : maSapXep.value
})
const maTotNhat = computed(() => {
  let tot = null
  for (const v of danhSachMa.value) {
    if (v.dungDuoc && Number(v.soTienGiam) > 0 && (!tot || Number(v.soTienGiam) > Number(tot.soTienGiam))) tot = v
  }
  return tot
})
// Banner gợi ý chỉ hiện khi mã tốt nhất CHƯA được áp dụng
const goiY = computed(() => (maTotNhat.value && cartStore.appliedVoucher?.ma !== maTotNhat.value.ma ? maTotNhat.value : null))

async function apDung(maNhap) {
  loi.value = ''
  const code = String(maNhap ?? voucherCode.value).trim().toUpperCase()
  if (!code) {
    if (cartStore.appliedVoucher) moDanhSach.value = false
    else loi.value = 'Vui lòng nhập mã giảm giá.'
    return
  }
  if (cartStore.appliedVoucher?.ma?.toUpperCase() === code) {
    voucherCode.value = ''
    moDanhSach.value = false
    return
  }
  dangKiemTra.value = true
  try {
    const found = (
      await api.get(`/khach/phieu-giam-gia/${encodeURIComponent(code)}`, { params: { tongTien: Math.round(cartStore.subTotal) } })
    ).data
    cartStore.setVoucher({
      ma: found.ma,
      ten: found.ten,
      loaiGiam: found.loaiGiam,
      giaTriGiam: found.giaTriGiam,
      giamToiDa: found.giamToiDa,
      donToiThieu: found.donToiThieu,
    })
    voucherCode.value = ''
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
    <div class="vp-title"><i class="bi bi-ticket-perforated"></i> MÃ GIẢM GIÁ</div>

    <!-- Gợi ý mã giảm nhiều nhất -->
    <div v-if="goiY" class="vp-banner">
      <i class="bi bi-stars vp-banner-icon"></i>
      <div class="vp-banner-text">
        <div class="vp-banner-main">Có mã <strong>{{ goiY.ma }}</strong> tốt nhất!</div>
        <div class="vp-banner-sub">Tiết kiệm được <strong>{{ formatMoney(goiY.soTienGiam) }}</strong></div>
      </div>
      <button type="button" class="vp-banner-btn" :disabled="dangKiemTra" @click="apDung(goiY.ma)">Dùng ngay</button>
    </div>

    <!-- Mã đang áp dụng -->
    <div v-if="cartStore.appliedVoucher" class="vp-applied">
      <i class="bi bi-check-circle-fill"></i>
      <div class="vp-applied-text">
        Đã áp dụng <strong>{{ cartStore.appliedVoucher.ma }}</strong>
        · Tiết kiệm <strong>{{ formatMoney(cartStore.discountAmount) }}</strong>
      </div>
      <button type="button" class="vp-x" title="Bỏ mã này" aria-label="Bỏ mã giảm giá" @click="boMa">
        <i class="bi bi-x-lg"></i>
      </button>
    </div>

    <!-- Ô nhập / chọn mã -->
    <div ref="comboRef" class="vp-combo">
      <div class="vp-input">
        <input
          v-model="voucherCode"
          type="text"
          class="text-uppercase"
          placeholder="Nhập hoặc chọn mã giảm giá..."
          autocomplete="off"
          @focus="moDanhSach = true; loi = ''"
          @click="moDanhSach = true"
          @keyup.enter="apDung()"
          @keyup.esc="moDanhSach = false"
        />
        <button type="button" :disabled="dangKiemTra" @click="apDung()">
          <span v-if="dangKiemTra" class="spinner-border spinner-border-sm"></span>
          <span v-else>Áp dụng</span>
        </button>
      </div>

      <div v-if="moDanhSach && danhSachMa.length" class="vp-dropdown">
        <div class="vp-dropdown-head">Mã giảm giá có sẵn ({{ maLoc.length }})</div>
        <div v-if="!maLoc.length" class="vp-empty">Không có mã nào khớp.</div>
        <div class="vp-list">
          <div
            v-for="v in maLoc"
            :key="v.ma"
            class="vp-voucher"
            :class="{ off: !v.dungDuoc, current: cartStore.appliedVoucher?.ma === v.ma }"
            role="button"
            :tabindex="v.dungDuoc ? 0 : -1"
            :aria-disabled="!v.dungDuoc"
            @click="chonMa(v)"
            @keyup.enter="chonMa(v)"
          >
            <div class="vp-voucher-info">
              <div class="vp-voucher-code">
                {{ v.ma }}
                <span class="vp-voucher-value">{{ v.loaiGiam === 1 ? `Giảm ${v.giaTriGiam}%` : `Giảm ${tienGon(v.giaTriGiam)}` }}</span>
                <span v-if="cartStore.appliedVoucher?.ma === v.ma" class="vp-tag now">Đang dùng</span>
                <span v-else-if="v.caNhan" class="vp-tag">Tặng riêng</span>
                <span v-if="maTotNhat?.ma === v.ma" class="vp-tag best">Giảm nhiều nhất</span>
              </div>
              <div class="vp-voucher-desc">{{ dieuKienMa(v) }}</div>
              <div v-if="!v.dungDuoc && v.lyDo" class="vp-voucher-why">{{ v.lyDo }}</div>
            </div>
            <div v-if="v.dungDuoc" class="vp-voucher-save">-{{ formatMoney(v.soTienGiam) }}</div>
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
}

/* Banner gợi ý mã tốt nhất */
.vp-banner {
  display: flex;
  align-items: center;
  gap: 0.7rem;
  padding: 0.7rem 0.8rem;
  margin-bottom: 0.6rem;
  background: #f2f7ff;
  border: 1px dashed #9ec5fe;
  border-radius: 12px;
}
.vp-banner-icon {
  font-size: 1.3rem;
  color: #0d6efd;
}
.vp-banner-text {
  flex: 1;
  min-width: 0;
}
.vp-banner-main {
  font-size: 0.88rem;
  font-weight: 700;
  color: #1f2937;
}
.vp-banner-main strong {
  color: #0d6efd;
}
.vp-banner-sub {
  font-size: 0.74rem;
  color: #6b7280;
}
.vp-banner-sub strong {
  color: #0d6efd;
}
.vp-banner-btn {
  flex-shrink: 0;
  padding: 0.4rem 0.9rem;
  font-size: 0.8rem;
  font-weight: 800;
  color: #fff;
  background: #0d6efd;
  border: 0;
  border-radius: 999px;
  box-shadow: 0 4px 10px rgba(13, 110, 253, 0.3);
}
.vp-banner-btn:disabled {
  opacity: 0.7;
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
.vp-input {
  display: flex;
}
.vp-input input {
  flex: 1;
  min-width: 0;
  padding: 0.55rem 0.8rem;
  font-size: 0.85rem;
  border: 1px solid #d5dbe5;
  border-right: 0;
  border-radius: 8px 0 0 8px;
  outline: none;
}
.vp-input input:focus {
  border-color: #0d6efd;
  box-shadow: 0 0 0 3px rgba(13, 110, 253, 0.12);
}
.vp-input button {
  padding: 0 1.1rem;
  font-size: 0.85rem;
  font-weight: 700;
  color: #fff;
  background: #0d6efd;
  border: 0;
  border-radius: 0 8px 8px 0;
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
  border: 1px solid #e3ebf8;
  border-radius: 14px;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.18);
  overflow: hidden;
}
.vp-dropdown-head {
  padding: 0.5rem 0.85rem;
  font-size: 0.74rem;
  font-weight: 700;
  color: #0d6efd;
  background: linear-gradient(90deg, #eef5ff, #fff);
  border-bottom: 1px solid #eef2f9;
}
.vp-empty {
  padding: 0.9rem;
  font-size: 0.82rem;
  color: #9aa3b2;
  text-align: center;
}
.vp-list {
  max-height: 250px;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: 0.5rem;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  scrollbar-width: thin;
}
.vp-voucher {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.55rem 0.7rem;
  background: #fff;
  border: 1px solid #e8eef8;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.15s ease;
}
.vp-voucher:hover:not(.off) {
  border-color: #9ec5fe;
  box-shadow: 0 4px 12px rgba(13, 110, 253, 0.12);
}
.vp-voucher:focus-visible {
  outline: 2px solid #0d6efd;
}
.vp-voucher.off {
  cursor: not-allowed;
  background: #f4f5f7;
  border-color: #eceef1;
}
.vp-voucher.off .vp-voucher-code,
.vp-voucher.off .vp-voucher-desc {
  opacity: 0.6;
}
.vp-voucher.off .vp-voucher-value,
.vp-voucher.off .vp-voucher-save,
.vp-voucher.off .vp-tag {
  color: #9ca3af;
  background: transparent;
}
.vp-voucher.current {
  background: #ecf8f0;
  border-color: #bfe5cc;
}
.vp-voucher-info {
  flex: 1;
  min-width: 0;
}
.vp-voucher-code {
  font-size: 0.82rem;
  font-weight: 800;
  color: #1f2937;
}
.vp-voucher-value {
  margin-left: 0.4rem;
  font-size: 0.76rem;
  font-weight: 700;
  color: #0d6efd;
}
.vp-voucher-desc {
  font-size: 0.72rem;
  color: #6b7280;
}
.vp-voucher-why {
  font-size: 0.7rem;
  color: #64748b;
}
.vp-voucher-save {
  flex-shrink: 0;
  font-size: 0.82rem;
  font-weight: 800;
  color: #0d6efd;
}
.vp-tag {
  margin-left: 0.3rem;
  padding: 0.05rem 0.45rem;
  font-size: 0.6rem;
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
