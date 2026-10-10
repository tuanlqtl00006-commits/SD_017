<script setup>
import { computed, onMounted, ref } from 'vue'
import api from '../services/api'
import { useToast } from '../composables/useToast'
import { useAutoRefresh } from '../composables/useAutoRefresh'
import { formatMoney, toIso } from '../utils/format'

// Trang Thống kê: tính từ danh sách hóa đơn có sẵn (GET /api/hoa-don), không cần thêm API mới.
const toast = useToast()
const hoaDon = ref([])
const loading = ref(true)
const khoang = ref(30) // 7 | 30 | 90 | 0 (tất cả)

const TRANG_THAI = {
  0: { label: 'Đã hủy', color: '#e5484d' },
  1: { label: 'Chờ xác nhận', color: '#f5a524' },
  2: { label: 'Đã xác nhận', color: '#8bbdf5' },
  3: { label: 'Chờ giao hàng', color: '#4a9af0' },
  4: { label: 'Đang giao hàng', color: '#0977ec' },
  5: { label: 'Đã giao hàng', color: '#1c2530' },
  6: { label: 'Đã hoàn thành', color: '#17a34a' },
}

async function load() {
  try {
    const res = await api.get('/hoa-don', { params: { page: 0, size: 2000 } })
    hoaDon.value = res.data.content ?? []
  } catch (e) {
    toast.error(e.message || 'Không tải được dữ liệu thống kê.')
  } finally {
    loading.value = false
  }
}
onMounted(load)
useAutoRefresh(load)

const tuNgay = computed(() => {
  if (!khoang.value) return ''
  const d = new Date()
  d.setDate(d.getDate() - (khoang.value - 1))
  return toIso(d)
})
const trongKhoang = computed(() => hoaDon.value.filter((h) => !tuNgay.value || String(h.ngayTao).slice(0, 10) >= tuNgay.value))
const hoanThanh = computed(() => trongKhoang.value.filter((h) => h.trangThai === 6))

const kpi = computed(() => {
  const dt = hoanThanh.value.reduce((s, h) => s + Number(h.tongTien || 0), 0)
  return [
    { label: 'Doanh thu', value: formatMoney(dt), icon: 'bi-cash-coin', note: 'Từ đơn đã hoàn thành' },
    { label: 'Tổng đơn hàng', value: trongKhoang.value.length, icon: 'bi-receipt', note: 'Trong khoảng đã chọn' },
    { label: 'Đơn hoàn thành', value: hoanThanh.value.length, icon: 'bi-check2-circle', note: hoanThanh.value.length ? `Trung bình ${formatMoney(Math.round(dt / hoanThanh.value.length))}/đơn` : 'Chưa có đơn' },
    { label: 'Đơn đã hủy', value: trongKhoang.value.filter((h) => h.trangThai === 0).length, icon: 'bi-x-circle', note: 'Cần xem lại lý do hủy' },
  ]
})

/* ----- Doanh thu theo ngày (cột) ----- */
const ngayCot = computed(() => {
  const soNgay = khoang.value || 30 // "tất cả" chỉ vẽ 30 ngày gần nhất cho dễ nhìn
  const map = {}
  for (const h of hoaDon.value) if (h.trangThai === 6) { const k = String(h.ngayTao).slice(0, 10); map[k] = (map[k] || 0) + Number(h.tongTien || 0) }
  return Array.from({ length: soNgay }, (_, i) => {
    const d = new Date(); d.setDate(d.getDate() - (soNgay - 1 - i))
    const k = toIso(d)
    return { k, nhan: `${k.slice(8)}/${k.slice(5, 7)}`, v: map[k] || 0 }
  })
})
const W = 700, H = 250, PAD = { l: 56, r: 10, t: 12, b: 28 }
const vMax = computed(() => { const m = Math.max(...ngayCot.value.map((x) => x.v), 0); return m ? Math.ceil(m / 1e6) * 1e6 : 1e6 })
const rongCot = computed(() => (W - PAD.l - PAD.r) / ngayCot.value.length)
const cao = (v) => (v / vMax.value) * (H - PAD.t - PAD.b)
const ticks = computed(() => [0, 0.25, 0.5, 0.75, 1].map((t) => ({ y: H - PAD.b - t * (H - PAD.t - PAD.b), nhan: t ? `${(vMax.value * t) / 1e6}tr` : '0' })))
const nhanTruc = computed(() => { const b = Math.ceil(ngayCot.value.length / 8); return ngayCot.value.map((x, i) => ((ngayCot.value.length - 1 - i) % b === 0 ? x.nhan : '')) })
const coDoanhThu = computed(() => ngayCot.value.some((x) => x.v > 0))

/* ----- Cơ cấu trạng thái (vòng) ----- */
const cauTrucTT = computed(() => {
  const tong = trongKhoang.value.length
  return Object.entries(TRANG_THAI).map(([k, t]) => ({ ...t, so: trongKhoang.value.filter((h) => h.trangThai === Number(k)).length })).map((t) => ({ ...t, pct: tong ? (t.so / tong) * 100 : 0 }))
})
const donut = computed(() => {
  let acc = 0
  const parts = cauTrucTT.value.filter((t) => t.so).map((t) => { const a = acc; acc += t.pct; return `${t.color} ${a}% ${acc}%` })
  return parts.length ? `conic-gradient(${parts.join(',')})` : 'conic-gradient(#e3eaf3 0 100%)'
})

/* ----- Theo loại đơn ----- */
const theoLoai = computed(() => {
  const m = {}
  for (const h of hoanThanh.value) m[h.loaiDon] = (m[h.loaiDon] || 0) + Number(h.tongTien || 0)
  const tong = Object.values(m).reduce((a, b) => a + b, 0)
  return Object.entries(m).map(([ten, v]) => ({ ten, v, pct: tong ? (v / tong) * 100 : 0 })).sort((a, b) => b.v - a.v)
})
</script>

<template>
  <div>
    <section class="ad-card">
      <div class="d-flex flex-wrap align-items-center gap-3">
        <div class="ad-card-head mb-0 me-auto">
          <span class="ad-icon-box"><i class="bi bi-grid" aria-hidden="true"></i></span>
          <div>
            <h2 class="ad-card-title">Tổng quan cửa hàng</h2>
            <p class="ad-card-sub">Số liệu tính từ hóa đơn trong khoảng thời gian đã chọn.</p>
          </div>
        </div>
        <div class="btn-group" role="group" aria-label="Khoảng thời gian">
          <button v-for="o in [{ v: 7, t: '7 ngày' }, { v: 30, t: '30 ngày' }, { v: 90, t: '90 ngày' }, { v: 0, t: 'Tất cả' }]" :key="o.v" type="button" class="btn btn-sm" :class="khoang === o.v ? 'btn-primary' : 'btn-outline-primary'" @click="khoang = o.v">{{ o.t }}</button>
        </div>
      </div>
    </section>

    <div class="row g-3 mb-3">
      <div v-for="k in kpi" :key="k.label" class="col-12 col-sm-6 col-xl-3">
        <section class="ad-card h-100 mb-0">
          <div class="d-flex align-items-center gap-3">
            <span class="ad-icon-box ad-icon-box-lg"><i class="bi" :class="k.icon" aria-hidden="true"></i></span>
            <div>
              <div class="text-muted small">{{ k.label }}</div>
              <div class="fs-4 fw-bold lh-sm">{{ loading ? '…' : k.value }}</div>
              <div class="text-muted small">{{ k.note }}</div>
            </div>
          </div>
        </section>
      </div>
    </div>

    <div class="row g-3">
      <div class="col-12 col-xl-8">
        <section class="ad-card h-100 mb-0">
          <h3 class="fs-6 fw-bold mb-3">Doanh thu theo ngày</h3>
          <div v-if="!coDoanhThu && !loading" class="ad-empty">Chưa có đơn hoàn thành trong khoảng này.</div>
          <svg v-else :viewBox="`0 0 ${W} ${H}`" class="w-100" role="img" aria-label="Biểu đồ cột doanh thu theo ngày">
            <g v-for="t in ticks" :key="t.y">
              <line :x1="PAD.l" :x2="W - PAD.r" :y1="t.y" :y2="t.y" stroke="#e3eaf3" />
              <text :x="PAD.l - 8" :y="t.y + 4" text-anchor="end" font-size="11" fill="#7a8696">{{ t.nhan }}</text>
            </g>
            <g v-for="(d, i) in ngayCot" :key="d.k">
              <rect :x="PAD.l + i * rongCot + rongCot * 0.18" :y="H - PAD.b - cao(d.v)" :width="rongCot * 0.64" :height="cao(d.v)" rx="4" fill="#0977ec"><title>{{ d.nhan }}: {{ formatMoney(d.v) }}</title></rect>
              <text :x="PAD.l + i * rongCot + rongCot / 2" :y="H - 8" text-anchor="middle" font-size="11" fill="#7a8696">{{ nhanTruc[i] }}</text>
            </g>
          </svg>
        </section>
      </div>

      <div class="col-12 col-xl-4">
        <section class="ad-card h-100 mb-0">
          <h3 class="fs-6 fw-bold mb-3">Cơ cấu trạng thái đơn hàng</h3>
          <div class="d-flex justify-content-center mb-3"><div class="ad-donut" :style="{ background: donut }" role="img" aria-label="Biểu đồ vòng trạng thái đơn"><span>{{ trongKhoang.length }}<small>đơn</small></span></div></div>
          <ul class="list-unstyled m-0 small">
            <li v-for="t in cauTrucTT" :key="t.label" class="d-flex align-items-center gap-2 py-1">
              <span class="ad-dot" :style="{ background: t.color }"></span>{{ t.label }}<span class="ms-auto fw-semibold">{{ t.so }}</span>
            </li>
          </ul>
        </section>
      </div>

      <div class="col-12">
        <section class="ad-card mb-0">
          <h3 class="fs-6 fw-bold mb-3">Doanh thu theo loại đơn</h3>
          <div v-if="!theoLoai.length" class="ad-empty">Chưa có dữ liệu.</div>
          <div v-for="l in theoLoai" :key="l.ten" class="mb-3">
            <div class="d-flex justify-content-between small mb-1"><span class="fw-medium">{{ l.ten }}</span><span>{{ formatMoney(l.v) }} · {{ l.pct.toFixed(0) }}%</span></div>
            <div class="progress" style="height: 10px"><div class="progress-bar" :style="{ width: l.pct + '%' }"></div></div>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<style scoped>
.ad-donut { width: 170px; height: 170px; border-radius: 50%; display: grid; place-items: center; position: relative; }
.ad-donut::after { content: ''; position: absolute; inset: 28px; background: #fff; border-radius: 50%; }
.ad-donut span { position: relative; z-index: 1; font-size: 1.6rem; font-weight: 700; text-align: center; line-height: 1.1; }
.ad-donut small { display: block; font-size: 0.75rem; font-weight: 500; color: #7a8696; }
.ad-dot { width: 10px; height: 10px; border-radius: 50%; flex: none; }
</style>
