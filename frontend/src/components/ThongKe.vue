<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import api from '../services/api'
import { useToast } from '../composables/useToast'
import { useAutoRefresh } from '../composables/useAutoRefresh'
import { formatDate, formatMoney, todayIso } from '../utils/format'
import { TRANG_THAI_HOA_DON, TT } from '../constants/hoaDon'
import {
  KHOANG_NHANH, TT_HOAN_THANH, congNgay, gioLam, gomCot, khoangGuiMayChu, khungGioHienThi, kyTruoc, lamTronTran,
  laHoaDonCho, locTheoKhoang, nhanTienGon, phanTram, phanTramThayDoi, theoLoaiDon, tinhKhoang, tongHop, tyLeCoMat, tyLeDungGio, xepHang,
} from '../utils/thongKe'
import DateInput from './common/DateInput.vue'
import { exportExcel } from '../utils/exportExcel'

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

async function load() {
  taiGd3()
  try {
    const res = await api.get('/hoa-don', { params: { page: 0, size: 2000 } })
    // Hóa đơn chờ tại quầy (chưa thanh toán, đang giữ hàng) không phải đơn bán: bỏ khỏi thống kê
    hoaDon.value = (res.data.content ?? []).filter((h) => !laHoaDonCho(h))
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
    { label: 'Doanh thu thực tế', value: formatMoney(a.doanhThu), icon: 'bi-cash-coin', note: 'Đơn đã hoàn thành, không gồm phí ship', so: soSanh(a.doanhThu, b.doanhThu) },
    {
      label: 'Doanh thu dự kiến', value: formatMoney(a.doanhThuDuKien), icon: 'bi-graph-up-arrow',
      note: a.doanhThuDuKien > a.doanhThu ? `Còn ${formatMoney(a.doanhThuDuKien - a.doanhThu)} từ đơn đang xử lý / giao` : 'Mọi đơn chưa hủy',
      so: soSanh(a.doanhThuDuKien, b.doanhThuDuKien),
    },
    {
      label: 'Đơn hoàn thành', value: `${a.hoanThanh}/${a.tongDon}`, icon: 'bi-check2-circle',
      note: a.hoanThanh ? `${pct(a.tyLeHoanThanh)} tổng đơn · TB ${formatMoney(a.trungBinh)}/đơn` : 'Chưa có đơn hoàn thành',
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
const ngayHienThi = (v) => { const s = String(v ?? '').slice(0, 10); return s ? formatDate(s) : '' }

/* ----- Giai đoạn 3: sản phẩm bán chạy, sắp hết hàng, phương thức thanh toán, ca / giờ, chuyên cần, giao ca (GET /api/thong-ke) ----- */
const gd3 = ref({})
const dangTaiGd3 = ref(true)
const loiGd3 = ref('')
const khoangGd3 = computed(() => khoangGuiMayChu(kq.value.tu, kq.value.den, homNay.value))
let lanTai = 0
async function taiGd3() {
  const lan = ++lanTai
  try {
    const { tuNgay, denNgay } = khoangGd3.value
    const res = await api.get('/thong-ke', { params: { tuNgay, denNgay } })
    if (lan !== lanTai) return
    gd3.value = res?.data && typeof res.data === 'object' ? res.data : {}
    loiGd3.value = ''
  } catch (e) {
    if (lan === lanTai) loiGd3.value = e.message || 'Không tải được thống kê bán hàng và ca làm việc.'
  } finally {
    if (lan === lanTai) dangTaiGd3.value = false
  }
}
watch(() => [khoangGd3.value.tuNgay, khoangGd3.value.denNgay], taiGd3)

const banChay = computed(() => (Array.isArray(gd3.value.sanPhamBanChay) ? gd3.value.sanPhamBanChay : []))
const sapHet = computed(() => (Array.isArray(gd3.value.sapHetHang) ? gd3.value.sapHetHang : []))
const theoPt = computed(() => {
  const ds = Array.isArray(gd3.value.theoPhuongThuc) ? gd3.value.theoPhuongThuc : []
  const tong = ds.reduce((s, x) => s + (Number(x.soTien) || 0), 0)
  return ds.map((x) => ({ ...x, pct: phanTram(x.soTien, tong) }))
})
const theoCa = computed(() => {
  const ds = Array.isArray(gd3.value.theoCa) ? gd3.value.theoCa : []
  const max = Math.max(1, ...ds.map((x) => Number(x.doanhThu) || 0))
  return ds.map((x) => ({ ...x, pct: ((Number(x.doanhThu) || 0) / max) * 100 }))
})
const theoGio = computed(() => {
  const ds = khungGioHienThi(Array.isArray(gd3.value.theoGio) ? gd3.value.theoGio : [])
  const max = Math.max(1, ...ds.map((x) => Number(x.soDon) || 0))
  return ds.map((x) => ({ ...x, pct: ((Number(x.soDon) || 0) / max) * 100 }))
})
const gioDongNhat = computed(() => theoGio.value.reduce((m, x) => (Number(x.soDon) > Number(m?.soDon ?? 0) ? x : m), null))
const chuyenCan = computed(() => (Array.isArray(gd3.value.chuyenCan) ? gd3.value.chuyenCan : []))
const giaoCa = computed(() => gd3.value.giaoCa ?? { soCaDaGiao: 0, soCaLech: 0, tongThieu: 0, tongThua: 0, caLech: [] })
const pctGon = (v) => (v === null || v === undefined ? '—' : `${String(v).replace('.', ',')}%`)

/* ----- Xuất Excel: đúng số liệu đang xem theo bộ lọc ở trên, mỗi bảng một sheet ----- */
function xuatExcel() {
  const a = tk.value
  const T = (header, key, width = 18, money = false) => ({ header, key, width, money })
  const sheets = [
    {
      sheetName: 'Tổng quan',
      columns: [T('Chỉ số', 'ten', 34), T('Giá trị', 'gt', 20, true), T('Ghi chú', 'ghiChu', 50)],
      rows: [
        { ten: 'Khoảng thời gian', gt: moTaKhoang.value },
        { ten: 'Doanh thu thực tế', gt: a.doanhThu, ghiChu: 'Đơn đã hoàn thành, không gồm phí ship' },
        { ten: 'Doanh thu dự kiến', gt: a.doanhThuDuKien, ghiChu: 'Mọi đơn chưa hủy (gồm đơn đang xử lý / đang giao)' },
        { ten: 'Tổng đơn', gt: a.tongDon },
        { ten: 'Đơn hoàn thành', gt: a.hoanThanh },
        { ten: 'Đơn đã hủy', gt: a.huy },
        { ten: 'Giá trị trung bình / đơn', gt: a.trungBinh },
      ],
    },
    {
      sheetName: `Doanh thu theo ${bieuDo.value.donVi}`,
      columns: [T('Mốc', 'nhan', 16), T('Số đơn hoàn thành', 'so'), T('Doanh thu thực tế', 'v', 20, true)],
      rows: cot.value.map((c) => ({ nhan: c.nhan, so: c.so, v: c.v })),
    },
    { sheetName: 'Theo loại đơn', columns: [T('Loại đơn', 'ten', 20), T('Doanh thu', 'v', 20, true), T('Tỷ trọng %', 'pct')], rows: theoLoai.value.map((l) => ({ ...l, pct: Math.round(l.pct * 10) / 10 })) },
    { sheetName: 'Nhân viên', columns: [T('Nhân viên', 'ten', 28), T('Số đơn', 'so'), T('Doanh thu', 'v', 20, true)], rows: topNhanVien.value },
    { sheetName: 'Khách hàng', columns: [T('Khách hàng', 'ten', 28), T('Số đơn', 'so'), T('Doanh thu', 'v', 20, true)], rows: topKhach.value },
    {
      sheetName: 'Sản phẩm bán chạy',
      columns: [T('Mã', 'maSanPham', 12), T('Sản phẩm', 'tenSanPham', 36), T('Bán chạy nhất', 'phanLoaiBanChay', 22), T('Số lượng', 'soLuong'), T('Số đơn', 'soDon'), T('Doanh thu', 'doanhThu', 20, true)],
      rows: banChay.value,
    },
    { sheetName: 'Sắp hết hàng', columns: [T('Mã', 'ma', 20), T('Sản phẩm', 'tenSanPham', 36), T('Phân loại', 'phanLoai', 22), T('Tồn kho', 'tonKho')], rows: sapHet.value },
    { sheetName: 'Phương thức TT', columns: [T('Phương thức', 'ten', 22), T('Số đơn', 'soDon'), T('Số tiền thu', 'soTien', 20, true)], rows: theoPt.value },
    { sheetName: 'Theo ca', columns: [T('Ca', 'tenCa', 18), T('Giờ', 'gio', 16), T('Số đơn', 'soDon'), T('Doanh thu', 'doanhThu', 20, true)], rows: theoCa.value },
    {
      sheetName: 'Chuyên cần',
      columns: [T('Mã NV', 'maNhanVien', 10), T('Nhân viên', 'hoTen', 26), T('Ca theo lịch', 'soCaLich'), T('Đã làm', 'soCaDaLam'), T('Vắng', 'soCaVang'), T('Đi muộn', 'soLanDiMuon'), T('Ngoài lịch', 'soCaNgoaiLich')],
      rows: chuyenCan.value,
    },
  ]
  const ten = kq.value.tu || kq.value.den ? `${kq.value.tu || 'dau'}_${kq.value.den || homNay.value}` : 'tat-ca'
  exportExcel({ filename: `thong-ke_${ten}.xlsx`, sheets })
  toast.success('Đã xuất file Excel thống kê.')
}
const mauTyLe = (v) => (v === null ? '' : v >= 90 ? 'tk-tot' : v >= 70 ? 'tk-vua' : 'tk-xau')
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
        <div class="d-flex gap-2">
          <button type="button" class="ad-btn ad-btn-sm" data-test="tk-xuat-excel" :disabled="loading" @click="xuatExcel"><i class="bi bi-file-earmark-excel" aria-hidden="true"></i> Xuất Excel</button>
          <RouterLink to="/thong-ke/so-sanh" class="ad-btn ad-btn-sm ad-btn-primary" data-test="tk-so-sanh"><i class="bi bi-bar-chart-line" aria-hidden="true"></i> So sánh</RouterLink>
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


      <!-- ===== Giai đoạn 3 ===== -->
      <div v-if="loiGd3" class="col-12"><div class="alert alert-warning small mb-0" role="alert" data-test="tk-loi-gd3">{{ loiGd3 }}</div></div>
      <div v-if="khoangGd3.catBot" class="col-12"><p class="text-muted small mb-0">Các mục bên dưới tính trong 1 năm gần nhất ({{ formatDate(khoangGd3.tuNgay) }} – {{ formatDate(khoangGd3.denNgay) }}).</p></div>

      <div class="col-12 col-xl-7">
        <section class="ad-card h-100 mb-0" data-test="tk-ban-chay">
          <h3 class="fs-6 fw-bold mb-3">Sản phẩm bán chạy</h3>
          <div v-if="dangTaiGd3" class="ad-empty">Đang tải…</div>
          <div v-else-if="!banChay.length" class="ad-empty">Chưa bán được sản phẩm nào trong khoảng này.</div>
          <div v-else class="ad-table-wrap">
            <div class="ad-table-scroll">
              <table class="ad-table">
                <thead><tr><th>#</th><th>Sản phẩm</th><th>Bán chạy nhất</th><th class="text-end">Số lượng</th><th class="text-end">Doanh thu</th></tr></thead>
                <tbody>
                  <tr v-for="(p, i) in banChay" :key="p.idSanPham">
                    <td><span class="tk-hang">{{ i + 1 }}</span></td>
                    <td>{{ p.tenSanPham }}<small class="text-muted d-block">{{ p.maSanPham }}<template v-if="p.thuongHieu"> · {{ p.thuongHieu }}</template> · {{ p.soDon }} đơn</small></td>
                    <td class="small">{{ p.phanLoaiBanChay || '—' }}</td>
                    <td class="text-end fw-semibold">{{ p.soLuong }}</td>
                    <td class="text-end ad-nowrap">{{ formatMoney(p.doanhThu) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </section>
      </div>

      <div class="col-12 col-xl-5">
        <section class="ad-card h-100 mb-0" data-test="tk-sap-het">
          <div class="d-flex align-items-center mb-3 gap-2">
            <h3 class="fs-6 fw-bold m-0 me-auto">Sắp hết hàng</h3>
            <span v-if="!dangTaiGd3" class="small text-muted">{{ gd3.soSapHet ?? 0 }} phân loại còn ≤ {{ gd3.nguongSapHet ?? 5 }}<template v-if="gd3.soHetHang"> · <b class="text-danger">{{ gd3.soHetHang }} hết hàng</b></template></span>
          </div>
          <div v-if="dangTaiGd3" class="ad-empty">Đang tải…</div>
          <div v-else-if="!sapHet.length" class="ad-empty">Không có sản phẩm nào sắp hết.</div>
          <ul v-else class="list-unstyled m-0 tk-sap-het">
            <li v-for="c in sapHet" :key="c.id">
              <span class="tk-ten"><RouterLink :to="`/san-pham/${c.idSanPham}/sua`">{{ c.tenSanPham }}</RouterLink><small class="text-muted d-block">{{ c.ma }}<template v-if="c.phanLoai"> · {{ c.phanLoai }}</template></small></span>
              <span class="ad-pill" :class="c.tonKho <= 0 ? 'ad-pill-red' : c.tonKho <= 2 ? 'ad-pill-orange' : 'ad-pill-amber'">{{ c.tonKho <= 0 ? 'Hết hàng' : `Còn ${c.tonKho}` }}</span>
            </li>
          </ul>
        </section>
      </div>

      <div class="col-12 col-lg-4">
        <section class="ad-card h-100 mb-0" data-test="tk-phuong-thuc">
          <h3 class="fs-6 fw-bold mb-1">Tiền thu theo phương thức thanh toán</h3>
          <p class="small text-muted mb-3">Số tiền khách thực trả (gồm cả phí ship), khác với doanh thu.</p>
          <div v-if="!theoPt.length" class="ad-empty">{{ dangTaiGd3 ? 'Đang tải…' : 'Chưa có dữ liệu.' }}</div>
          <div v-for="p in theoPt" :key="p.ten" class="mb-3">
            <div class="d-flex justify-content-between small mb-1"><span class="fw-medium">{{ p.ten }}</span><span>{{ formatMoney(p.soTien) }} · {{ pctGon(p.pct) }}</span></div>
            <div class="progress" style="height: 10px"><div class="progress-bar" :style="{ width: p.pct + '%' }"></div></div>
            <small class="text-muted">{{ p.soDon }} đơn</small>
          </div>
        </section>
      </div>

      <div class="col-12 col-md-6 col-lg-4">
        <section class="ad-card h-100 mb-0" data-test="tk-theo-ca">
          <h3 class="fs-6 fw-bold mb-3">Doanh thu theo ca làm việc</h3>
          <div v-if="!theoCa.length" class="ad-empty">{{ dangTaiGd3 ? 'Đang tải…' : 'Chưa có dữ liệu.' }}</div>
          <div v-for="c in theoCa" :key="c.tenCa" class="mb-3">
            <div class="d-flex justify-content-between small mb-1">
              <span class="fw-medium">{{ c.tenCa }}<small v-if="c.gio" class="text-muted"> ({{ c.gio }})</small></span><span>{{ formatMoney(c.doanhThu) }}</span>
            </div>
            <div class="progress" style="height: 10px"><div class="progress-bar bg-info" :style="{ width: c.pct + '%' }"></div></div>
            <small class="text-muted">{{ c.soDon }} đơn</small>
          </div>
        </section>
      </div>

      <div class="col-12 col-md-6 col-lg-4">
        <section class="ad-card h-100 mb-0" data-test="tk-theo-gio">
          <h3 class="fs-6 fw-bold mb-1">Khung giờ đông khách</h3>
          <p class="small text-muted mb-3">{{ gioDongNhat && gioDongNhat.soDon > 0 ? `Đông nhất ${gioDongNhat.gio}h - ${gioDongNhat.gio + 1}h (${gioDongNhat.soDon} đơn)` : 'Số đơn hoàn thành theo giờ tạo đơn' }}</p>
          <div class="tk-gio" role="img" aria-label="Biểu đồ số đơn theo giờ">
            <div v-for="g in theoGio" :key="g.gio" class="tk-gio-cot" :title="`${g.gio}h: ${g.soDon} đơn · ${formatMoney(g.doanhThu)}`">
              <span class="tk-gio-thanh" :style="{ height: Math.max(g.pct, g.soDon ? 4 : 0) + '%' }"></span>
              <small>{{ g.gio % 2 === 0 ? g.gio : '' }}</small>
            </div>
          </div>
        </section>
      </div>

      <div class="col-12 col-xl-8">
        <section class="ad-card h-100 mb-0" data-test="tk-chuyen-can">
          <div class="d-flex align-items-center mb-3 gap-2">
            <h3 class="fs-6 fw-bold m-0 me-auto">Chuyên cần theo lịch làm việc</h3>
            <RouterLink to="/lich-lam-viec" class="small">Xếp lịch</RouterLink>
          </div>
          <div v-if="dangTaiGd3" class="ad-empty">Đang tải…</div>
          <div v-else-if="!chuyenCan.length" class="ad-empty">Chưa có lịch làm việc / ca nào trong khoảng này.</div>
          <div v-else class="ad-table-wrap">
            <div class="ad-table-scroll">
              <table class="ad-table">
                <thead>
                  <tr>
                    <th>Nhân viên</th><th class="text-end">Ca theo lịch</th><th class="text-end">Đã làm</th><th class="text-end">Vắng</th>
                    <th class="text-end">Đi muộn</th><th class="text-end">Ngoài lịch</th><th class="text-end">Giờ làm</th><th class="text-end">Có mặt</th><th class="text-end">Đúng giờ</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="r in chuyenCan" :key="r.idNhanVien">
                    <td>{{ r.hoTen }}<small class="text-muted d-block">{{ r.maNhanVien }}</small></td>
                    <td class="text-end">{{ r.soCaLich }}<small v-if="r.soCaChuaToi" class="text-muted d-block">{{ r.soCaChuaToi }} chưa tới</small></td>
                    <td class="text-end">{{ r.soCaDaLam }}</td>
                    <td class="text-end" :class="{ 'text-danger fw-semibold': r.soCaVang }">{{ r.soCaVang }}</td>
                    <td class="text-end" :class="{ 'text-warning fw-semibold': r.soLanDiMuon }">{{ r.soLanDiMuon }}<small v-if="r.tongPhutMuon" class="text-muted d-block">{{ r.tongPhutMuon }} phút</small></td>
                    <td class="text-end">{{ r.soCaNgoaiLich }}</td>
                    <td class="text-end ad-nowrap">{{ gioLam(r.soPhutLam) }}</td>
                    <td class="text-end fw-semibold" :class="mauTyLe(tyLeCoMat(r))">{{ pctGon(tyLeCoMat(r)) }}</td>
                    <td class="text-end fw-semibold" :class="mauTyLe(tyLeDungGio(r))">{{ pctGon(tyLeDungGio(r)) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
          <p class="small text-muted mt-2 mb-0">Vắng: ca theo lịch đã kết thúc mà không nhận ca. Đi muộn: nhận ca trễ quá 15 phút.</p>
        </section>
      </div>

      <div class="col-12 col-xl-4">
        <section class="ad-card h-100 mb-0" data-test="tk-giao-ca">
          <div class="d-flex align-items-center mb-3 gap-2">
            <h3 class="fs-6 fw-bold m-0 me-auto">Giao ca &amp; đối soát tiền mặt</h3>
            <RouterLink to="/giao-ca" class="small">Lịch sử giao ca</RouterLink>
          </div>
          <div class="tk-gc-so">
            <div><span>Ca đã giao</span><b>{{ giaoCa.soCaDaGiao }}</b></div>
            <div><span>Ca lệch tiền</span><b :class="{ 'text-danger': giaoCa.soCaLech }">{{ giaoCa.soCaLech }}</b></div>
            <div><span>Tổng thiếu</span><b class="text-danger">{{ formatMoney(giaoCa.tongThieu || 0) }}</b></div>
            <div><span>Tổng thừa</span><b class="text-warning">{{ formatMoney(giaoCa.tongThua || 0) }}</b></div>
          </div>
          <div v-if="!(giaoCa.caLech || []).length" class="ad-empty mt-3">{{ dangTaiGd3 ? 'Đang tải…' : 'Không có ca nào lệch tiền. Tốt lắm!' }}</div>
          <ul v-else class="list-unstyled m-0 mt-3 tk-rank">
            <li v-for="g in giaoCa.caLech" :key="g.id">
              <span class="tk-ten">{{ g.maGiaoCa }} · {{ g.tenNhanVien }}<small class="text-muted d-block">{{ ngayHienThi(g.thoiGianKetCa) }}<template v-if="g.ghiChu"> · {{ g.ghiChu }}</template></small></span>
              <b :class="g.chenhLech < 0 ? 'text-danger' : 'text-warning'">{{ g.chenhLech > 0 ? '+' : '' }}{{ formatMoney(g.chenhLech) }}</b>
            </li>
          </ul>
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
.tk-vua { color: #d97706; }
.tk-sap-het li { display: flex; align-items: center; gap: 0.75rem; padding: 0.5rem 0; border-bottom: 1px solid #eef2f7; font-size: 0.9rem; }
.tk-sap-het li:last-child { border-bottom: 0; }
.tk-sap-het { max-height: 420px; overflow-y: auto; }
.tk-gio { display: flex; align-items: flex-end; gap: 3px; height: 170px; padding-top: 6px; }
.tk-gio-cot { flex: 1 1 0; display: flex; flex-direction: column; align-items: center; justify-content: flex-end; height: 100%; min-width: 0; }
.tk-gio-thanh { width: 100%; max-width: 22px; background: #0977ec; border-radius: 4px 4px 0 0; }
.tk-gio-cot small { font-size: 0.7rem; color: #7a8696; height: 1rem; margin-top: 4px; }
.tk-gc-so { display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem; }
.tk-gc-so div { background: #f7f9fc; border-radius: 10px; padding: 0.6rem 0.75rem; display: flex; flex-direction: column; }
.tk-gc-so span { font-size: 0.78rem; color: #7a8696; }
.tk-gc-so b { font-size: 1.05rem; }
</style>
