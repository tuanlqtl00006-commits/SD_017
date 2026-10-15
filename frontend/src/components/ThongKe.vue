<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import api from '../services/api'
import { useToast } from '../composables/useToast'
import { useAutoRefresh } from '../composables/useAutoRefresh'
import { formatDate, formatMoney, todayIso } from '../utils/format'
import { TRANG_THAI_HOA_DON, TT } from '../constants/hoaDon'
import {
  KHOANG_NHANH, TT_HOAN_THANH, congNgay, donGanNhat, gomCot, kyTruoc, lamTronTran, locTheoKhoang,
  nhanTienGon, phanTramThayDoi, theoLoaiDon, tinhKhoang, tongHop, xepHang,
} from '../utils/thongKe'
import DateInput from './common/DateInput.vue'

// Trang Thống kê: tính từ danh sách hóa đơn có sẵn (GET /api/hoa-don), không cần thêm API mới.
// Doanh thu chỉ tính đơn đã hoàn thành. Có so sánh với kỳ liền trước (cùng độ dài) khi chọn 7 / 30 / 90 ngày hoặc tự chọn đủ 2 đầu.
const toast = useToast()
const hoaDon = ref([])
const loading = ref(true)
const homNay = ref(todayIso())
const khoang = ref(30) // 7 | 30 | 90 | 0 (tất cả) | -1 (tự chọn)
const tuChon = ref(congNgay(todayIso(), -29))
const denChon = ref(todayIso())

// Màu từng trạng thái trên biểu đồ; nhãn lấy từ constants/hoaDon.js để không lệch với trang Hóa đơn
// (không đưa "Hoàn tiền" vào vì chưa có đơn nào ở trạng thái đó)
const MAU_TRANG_THAI = {
  [TT.DA_HUY]: '#e5484d',
  [TT.CHO_XAC_NHAN]: '#f5a524',
  [TT.DA_XAC_NHAN]: '#8bbdf5',
  [TT.CHO_LAY_HANG]: '#4a9af0',
  [TT.DANG_GIAO]: '#0977ec',
  [TT.GIAO_THAT_BAI]: '#e8751a',
  [TT.DA_GIAO]: '#1c2530',
  [TT.HOAN_THANH]: '#17a34a',
}
const TRANG_THAI = Object.fromEntries(
  Object.entries(MAU_TRANG_THAI).map(([ma, color]) => [ma, { label: TRANG_THAI_HOA_DON[ma].label, color }]),
)
const ttOf = (v) => TRANG_THAI[v] ?? { label: 'Không rõ', color: '#7a8696' }

async function load() {
  try {
    const res = await api.get('/hoa-don', { params: { page: 0, size: 2000 } })
    hoaDon.value = res.data.content ?? []
    homNay.value = todayIso()
  } catch (e) {
    toast.error(e.message || 'Không tải được dữ liệu thống kê.')
  } finally {
    loading.value = false
  }
}
onMounted(load)
useAutoRefresh(load)

const kq = computed(() => tinhKhoang(khoang.value, tuChon.value, denChon.value, homNay.value))
const trongKhoang = computed(() => locTheoKhoang(hoaDon.value, kq.value.tu, kq.value.den))
const tk = computed(() => tongHop(trongKhoang.value))
const ky = computed(() => kyTruoc(kq.value.tu, kq.value.den))
const tkTruoc = computed(() => (ky.value ? tongHop(locTheoKhoang(hoaDon.value, ky.value.tu, ky.value.den)) : null))
const moTaKhoang = computed(() => {
  const { tu, den } = kq.value
  if (!tu && !den) return 'Toàn bộ thời gian'
  if (tu && den) return `${formatDate(tu)} – ${formatDate(den)}`
  return tu ? `Từ ${formatDate(tu)}` : `Đến ${formatDate(den)}`
})

const pct = (x) => `${(Math.round(x * 10) / 10).toString().replace('.', ',')}%`

// xauKhiTang: đơn hủy tăng là điều không tốt nên tô đỏ
function soSanh(nay, truoc, xauKhiTang = false) {
  if (!tkTruoc.value) return null
  const p = phanTramThayDoi(nay, truoc)
  if (p === null) return { text: 'Kỳ trước chưa có', huong: '' }
  if (Math.abs(p) < 0.05) return { text: 'Không đổi', huong: '' }
  const tang = p > 0
  return { text: `${tang ? '▲' : '▼'} ${pct(Math.abs(p))}`, huong: tang !== xauKhiTang ? 'tot' : 'xau' }
}

const kpi = computed(() => {
  const a = tk.value
  const b = tkTruoc.value ?? {}
  return [
    { label: 'Doanh thu', value: formatMoney(a.doanhThu), icon: 'bi-cash-coin', note: 'Từ đơn đã hoàn thành', so: soSanh(a.doanhThu, b.doanhThu) },
    { label: 'Tổng đơn hàng', value: a.tongDon, icon: 'bi-receipt', note: 'Trong khoảng đã chọn', so: soSanh(a.tongDon, b.tongDon) },
    {
      label: 'Đơn hoàn thành', value: a.hoanThanh, icon: 'bi-check2-circle',
      note: a.hoanThanh ? `${pct(a.tyLeHoanThanh)} đơn · TB ${formatMoney(a.trungBinh)}/đơn` : 'Chưa có đơn',
      so: soSanh(a.hoanThanh, b.hoanThanh),
    },
    {
      label: 'Đơn đã hủy', value: a.huy, icon: 'bi-x-circle',
      note: a.tongDon ? `${pct(a.tyLeHuy)} tổng đơn` : 'Chưa có đơn', so: soSanh(a.huy, b.huy, true),
    },
  ]
})

/* ----- Doanh thu theo ngày / tuần / tháng (cột) ----- */
const bieuDo = computed(() => gomCot(hoaDon.value, kq.value.tu, kq.value.den, homNay.value))
const cot = computed(() => bieuDo.value.cot)
const coDoanhThu = computed(() => cot.value.some((x) => x.v > 0))
const W = 700, H = 250, PAD = { l: 58, r: 10, t: 12, b: 28 }
const vMax = computed(() => lamTronTran(Math.max(...cot.value.map((x) => x.v), 0)))
const rongCot = computed(() => (W - PAD.l - PAD.r) / Math.max(cot.value.length, 1))
const cao = (v) => (v / vMax.value) * (H - PAD.t - PAD.b)
const ticks = computed(() => [0, 0.25, 0.5, 0.75, 1].map((t) => ({ y: H - PAD.b - t * (H - PAD.t - PAD.b), nhan: nhanTienGon(vMax.value * t) })))
const nhanTruc = computed(() => {
  const n = cot.value.length
  const b = Math.max(1, Math.ceil(n / 8))
  return cot.value.map((x, i) => ((n - 1 - i) % b === 0 ? x.nhan : ''))
})

/* ----- Cơ cấu trạng thái (vòng) ----- */
const cauTrucTT = computed(() => {
  const tong = trongKhoang.value.length
  return Object.entries(TRANG_THAI).map(([k, t]) => {
    const so = trongKhoang.value.filter((h) => h.trangThai === Number(k)).length
    return { ...t, so, pct: tong ? (so / tong) * 100 : 0 }
  })
})
const donut = computed(() => {
  let acc = 0
  const parts = cauTrucTT.value.filter((t) => t.so).map((t) => { const a = acc; acc += t.pct; return `${t.color} ${a}% ${acc}%` })
  return parts.length ? `conic-gradient(${parts.join(',')})` : 'conic-gradient(#e3eaf3 0 100%)'
})

const theoLoai = computed(() => theoLoaiDon(trongKhoang.value))
const topNhanVien = computed(() => xepHang(trongKhoang.value, (h) => h.tenNhanVien))
const topKhach = computed(() => xepHang(trongKhoang.value, (h) => h.tenKhachHang))
const ganDay = computed(() => donGanNhat(trongKhoang.value, 8))
const ngayHienThi = (v) => { const s = String(v ?? '').slice(0, 10); return s ? formatDate(s) : '' }
</script>

<template>
  <div>
    <section class="ad-card">
      <div class="d-flex flex-wrap align-items-center gap-3">
        <div class="ad-card-head mb-0 me-auto">
          <span class="ad-icon-box"><i class="bi bi-grid" aria-hidden="true"></i></span>
          <div>
            <h2 class="ad-card-title">Tổng quan cửa hàng</h2>
            <p class="ad-card-sub" data-test="tk-mo-ta">{{ moTaKhoang }}<template v-if="ky"> · so sánh với {{ formatDate(ky.tu) }} – {{ formatDate(ky.den) }}</template></p>
          </div>
        </div>
        <div class="btn-group flex-wrap" role="group" aria-label="Khoảng thời gian">
          <button v-for="o in KHOANG_NHANH" :key="o.v" type="button" class="btn btn-sm" :class="khoang === o.v ? 'btn-primary' : 'btn-outline-primary'" :aria-pressed="khoang === o.v" @click="khoang = o.v">{{ o.t }}</button>
        </div>
      </div>
      <div v-if="khoang === -1" class="d-flex flex-wrap align-items-end gap-3 mt-3" data-test="tk-tu-chon">
        <div>
          <label for="tk-tu" class="form-label small mb-1">Từ ngày</label>
          <DateInput id="tk-tu" v-model="tuChon" class="form-control form-control-sm" :max="denChon || homNay" />
        </div>
        <div>
          <label for="tk-den" class="form-label small mb-1">Đến ngày</label>
          <DateInput id="tk-den" v-model="denChon" class="form-control form-control-sm" :min="tuChon" />
        </div>
        <p v-if="!tuChon && !denChon" class="text-muted small mb-1">Chưa chọn ngày nào nên đang xem toàn bộ thời gian.</p>
      </div>
    </section>

    <div class="row g-3 mb-3">
      <div v-for="k in kpi" :key="k.label" class="col-12 col-sm-6 col-xl-3">
        <section class="ad-card h-100 mb-0" data-test="tk-kpi">
          <div class="d-flex align-items-center gap-3">
            <span class="ad-icon-box ad-icon-box-lg"><i class="bi" :class="k.icon" aria-hidden="true"></i></span>
            <div class="min-w-0">
              <div class="text-muted small">{{ k.label }}</div>
              <div class="fs-4 fw-bold lh-sm">{{ loading ? '…' : k.value }}</div>
              <div class="text-muted small">{{ k.note }}</div>
              <div v-if="!loading && k.so" class="tk-so small mt-1" :class="k.so.huong && `tk-${k.so.huong}`">
                {{ k.so.text }}<span v-if="k.so.huong" class="text-muted fw-normal"> so với kỳ trước</span>
              </div>
            </div>
          </div>
        </section>
      </div>
    </div>

    <div class="row g-3">
      <div class="col-12 col-xl-8">
        <section class="ad-card h-100 mb-0">
          <h3 class="fs-6 fw-bold mb-3">Doanh thu theo {{ bieuDo.donVi }}</h3>
          <div v-if="!coDoanhThu && !loading" class="ad-empty">Chưa có đơn hoàn thành trong khoảng này.</div>
          <svg v-else :viewBox="`0 0 ${W} ${H}`" class="w-100" role="img" :aria-label="`Biểu đồ cột doanh thu theo ${bieuDo.donVi}`">
            <g v-for="t in ticks" :key="t.y">
              <line :x1="PAD.l" :x2="W - PAD.r" :y1="t.y" :y2="t.y" stroke="#e3eaf3" />
              <text :x="PAD.l - 8" :y="t.y + 4" text-anchor="end" font-size="11" fill="#7a8696">{{ t.nhan }}</text>
            </g>
            <g v-for="(d, i) in cot" :key="d.k">
              <rect :x="PAD.l + i * rongCot + rongCot * 0.18" :y="H - PAD.b - cao(d.v)" :width="rongCot * 0.64" :height="cao(d.v)" rx="3" fill="#0977ec"><title>{{ d.nhan }}: {{ formatMoney(d.v) }} ({{ d.so }} đơn)</title></rect>
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
              <span class="ad-dot" :style="{ background: t.color }"></span>{{ t.label }}<span class="ms-auto fw-semibold">{{ t.so }}</span><span class="text-muted tk-pct">{{ pct(t.pct) }}</span>
            </li>
          </ul>
        </section>
      </div>

      <div class="col-12 col-lg-4">
        <section class="ad-card h-100 mb-0">
          <h3 class="fs-6 fw-bold mb-3">Doanh thu theo loại đơn</h3>
          <div v-if="!theoLoai.length" class="ad-empty">Chưa có dữ liệu.</div>
          <div v-for="l in theoLoai" :key="l.ten" class="mb-3">
            <div class="d-flex justify-content-between small mb-1"><span class="fw-medium">{{ l.ten }}</span><span>{{ formatMoney(l.v) }} · {{ l.pct.toFixed(0) }}%</span></div>
            <div class="progress" style="height: 10px"><div class="progress-bar" :style="{ width: l.pct + '%' }"></div></div>
          </div>
        </section>
      </div>

      <div class="col-12 col-md-6 col-lg-4">
        <section class="ad-card h-100 mb-0" data-test="tk-top-nv">
          <h3 class="fs-6 fw-bold mb-3">Nhân viên bán tốt nhất</h3>
          <div v-if="!topNhanVien.length" class="ad-empty">Chưa có dữ liệu.</div>
          <ol v-else class="list-unstyled m-0 tk-rank">
            <li v-for="(r, i) in topNhanVien" :key="r.ten"><span class="tk-hang">{{ i + 1 }}</span><span class="tk-ten">{{ r.ten }}<small class="text-muted d-block">{{ r.so }} đơn</small></span><b>{{ formatMoney(r.v) }}</b></li>
          </ol>
        </section>
      </div>

      <div class="col-12 col-md-6 col-lg-4">
        <section class="ad-card h-100 mb-0" data-test="tk-top-kh">
          <h3 class="fs-6 fw-bold mb-3">Khách hàng mua nhiều nhất</h3>
          <div v-if="!topKhach.length" class="ad-empty">Chưa có dữ liệu.</div>
          <ol v-else class="list-unstyled m-0 tk-rank">
            <li v-for="(r, i) in topKhach" :key="r.ten"><span class="tk-hang">{{ i + 1 }}</span><span class="tk-ten">{{ r.ten }}<small class="text-muted d-block">{{ r.so }} đơn</small></span><b>{{ formatMoney(r.v) }}</b></li>
          </ol>
        </section>
      </div>

      <div class="col-12">
        <section class="ad-card mb-0" data-test="tk-gan-day">
          <div class="d-flex align-items-center mb-3">
            <h3 class="fs-6 fw-bold m-0 me-auto">Đơn hàng gần đây</h3>
            <RouterLink to="/hoa-don" class="small">Xem tất cả</RouterLink>
          </div>
          <div v-if="!ganDay.length" class="ad-empty">Chưa có đơn trong khoảng này.</div>
          <div v-else class="ad-table-wrap">
            <div class="ad-table-scroll">
              <table class="ad-table">
                <thead><tr><th>Mã hóa đơn</th><th>Khách hàng</th><th>Ngày tạo</th><th>Loại đơn</th><th>Trạng thái</th><th class="text-end">Tổng tiền</th></tr></thead>
                <tbody>
                  <tr v-for="h in ganDay" :key="h.id ?? h.maHoaDon">
                    <td class="ad-nowrap"><RouterLink :to="`/hoa-don/${h.id}`" class="ad-code">{{ h.maHoaDon }}</RouterLink></td>
                    <td>{{ h.tenKhachHang || 'Khách vãng lai' }}</td>
                    <td class="ad-nowrap">{{ ngayHienThi(h.ngayTao) }}</td>
                    <td class="ad-nowrap">{{ h.loaiDon }}</td>
                    <td class="ad-nowrap"><span class="ad-dot me-2 d-inline-block" :style="{ background: ttOf(h.trangThai).color }"></span>{{ ttOf(h.trangThai).label }}</td>
                    <td class="ad-nowrap text-end fw-semibold" :class="{ 'text-decoration-line-through text-muted': h.trangThai === 0 }">{{ formatMoney(h.tongTien) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
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
.tk-pct { min-width: 3.2rem; text-align: right; }
.tk-so { font-weight: 600; color: #7a8696; }
.tk-tot { color: #17a34a; }
.tk-xau { color: #e5484d; }
.tk-rank li { display: flex; align-items: center; gap: 0.75rem; padding: 0.55rem 0; border-bottom: 1px solid #eef2f7; font-size: 0.9rem; }
.tk-rank li:last-child { border-bottom: 0; }
.tk-hang { width: 1.7rem; height: 1.7rem; border-radius: 50%; background: #eaf3fe; color: #0977ec; font-weight: 700; font-size: 0.8rem; display: grid; place-items: center; flex: none; }
.tk-ten { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; }
</style>
