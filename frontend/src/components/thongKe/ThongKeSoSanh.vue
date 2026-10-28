<script setup>
// Thống kê SO SÁNH (cô: nút "So sánh" - so theo tháng / quý / năm, so với kỳ trước và cùng kỳ năm trước, biểu đồ tăng trưởng,
// tải Excel, gửi email báo cáo kèm biểu đồ). Số liệu tính ở backend: GET /api/thong-ke/so-sanh?kieu=THANG|QUY|NAM&ky=...
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import api from '../../services/api'
import { useAuth } from '../../composables/useAuth'
import { useToast } from '../../composables/useToast'
import { exportExcel } from '../../utils/exportExcel'
import { formatDate, formatMoney, todayIso } from '../../utils/format'
import { lamTronTran, nhanTienGon } from '../../utils/thongKe'

const toast = useToast()
const auth = useAuth()
const homNay = todayIso()
const namNay = Number(homNay.slice(0, 4))

const KIEU = [
  { v: 'THANG', t: 'Theo tháng' },
  { v: 'QUY', t: 'Theo quý' },
  { v: 'NAM', t: 'Theo năm' },
]
const kieu = ref('THANG')
const thangHienTai = homNay.slice(0, 7)
const quyHienTai = Math.floor((Number(homNay.slice(5, 7)) - 1) / 3) + 1
const thang = ref(thangHienTai)
const namQuy = ref(namNay)
const quy = ref(quyHienTai)
const nam = ref(namNay)
const dsNam = Array.from({ length: 6 }, (_, i) => namNay - i)

const canhBaoKy = computed(() => {
  if (kieu.value === 'THANG') {
    const t = String(thang.value || '').trim()
    if (!/^\d{4}-(0[1-9]|1[0-2])$/.test(t)) return 'Tháng chọn không hợp lệ (cần định dạng YYYY-MM).'
    if (t > thangHienTai) return `Tháng đang chọn (${t}) vượt quá tháng hiện tại (${thangHienTai}).`
    if (t < '2000-01') return 'Vui lòng chọn tháng từ năm 2000 trở lại đây.'
  } else if (kieu.value === 'QUY') {
    if (namQuy.value === namNay && quy.value > quyHienTai) {
      return `Quý ${quy.value}/${namQuy.value} chưa diễn ra (hiện tại đang ở Quý ${quyHienTai}/${namNay}).`
    }
  }
  return ''
})

function datVeKyHienTai() {
  thang.value = thangHienTai
  namQuy.value = namNay
  quy.value = quyHienTai
  nam.value = namNay
}

const ky = computed(() => (kieu.value === 'THANG' ? thang.value : kieu.value === 'QUY' ? `${namQuy.value}-Q${quy.value}` : String(nam.value)))
const d = ref(null)
const dangTai = ref(false)
const loi = ref('')
let lan = 0

async function tai() {
  if (!ky.value) return
  if (kieu.value === 'THANG' && (!/^\d{4}-(0[1-9]|1[0-2])$/.test(thang.value) || thang.value < '2000-01')) return
  const l = ++lan
  dangTai.value = true
  loi.value = ''
  try {
    const res = await api.get('/thong-ke/so-sanh', { params: { kieu: kieu.value, ky: ky.value } })
    if (l === lan) d.value = res.data
  } catch (e) {
    if (l === lan) loi.value = e.message || 'Không tải được số liệu so sánh.'
  } finally {
    if (l === lan) dangTai.value = false
  }
}
onMounted(tai)
watch([kieu, ky], tai)

const CHI_SO = [
  { k: 'doanhThuThucTe', ten: 'Doanh thu thực tế', tien: true, icon: 'bi-cash-coin' },
  { k: 'doanhThuDuKien', ten: 'Doanh thu dự kiến', tien: true, icon: 'bi-graph-up-arrow' },
  { k: 'soDon', ten: 'Tổng đơn', icon: 'bi-receipt' },
  { k: 'soHoanThanh', ten: 'Đơn hoàn thành', icon: 'bi-check2-circle' },
  { k: 'soHuy', ten: 'Đơn hủy', xauKhiTang: true, icon: 'bi-x-circle' },
  { k: 'giaTriTrungBinh', ten: 'Giá trị TB / đơn', tien: true, icon: 'bi-calculator' },
  { k: 'soSanPhamBan', ten: 'Sản phẩm bán ra', icon: 'bi-box-seam' },
  { k: 'soKhachMua', ten: 'Khách hàng mua', icon: 'bi-people' },
]
const giaTri = (c, v) => (c.tien ? formatMoney(v ?? 0) : String(v ?? 0))
function moTaTang(p, xauKhiTang = false) {
  if (p === null || p === undefined) return { text: 'Kỳ so sánh chưa có', lop: '' }
  if (Math.abs(p) < 0.05) return { text: 'Không đổi', lop: '' }
  const tang = p > 0
  return { text: `${tang ? '▲' : '▼'} ${String(Math.abs(p)).replace('.', ',')}%`, lop: tang !== xauKhiTang ? 'ss-tot' : 'ss-xau' }
}

/* ----- Biểu đồ cột nhóm (SVG): kỳ này / cùng kỳ năm trước ----- */
const W = 760
const H = 280
const PAD = { l: 60, r: 12, t: 28, b: 30 }
function bieuDo(diem) {
  const ds = diem || []
  const max = lamTronTran(Math.max(0, ...ds.map((x) => Math.max(Number(x.doanhThu) || 0, Number(x.doanhThuNamTruoc) || 0))))
  const caoVe = H - PAD.t - PAD.b
  const nhom = (W - PAD.l - PAD.r) / Math.max(ds.length, 1)
  const coB = ds.some((x) => x.doanhThuNamTruoc !== null && x.doanhThuNamTruoc !== undefined)
  const rong = nhom * (coB ? 0.34 : 0.6)
  const buocNhan = Math.max(1, Math.ceil(ds.length / 16))
  const y = (v) => H - PAD.b - ((Number(v) || 0) / max) * caoVe
  return {
    coB,
    ticks: [0, 0.25, 0.5, 0.75, 1].map((t) => ({ y: H - PAD.b - t * caoVe, nhan: nhanTienGon(max * t) })),
    cot: ds.map((x, i) => {
      const x0 = PAD.l + i * nhom + nhom * (coB ? 0.14 : 0.2)
      return {
        ...x,
        xa: x0, ya: y(x.doanhThu), ha: H - PAD.b - y(x.doanhThu),
        xb: x0 + rong + 2, yb: y(x.doanhThuNamTruoc), hb: H - PAD.b - y(x.doanhThuNamTruoc),
        rong, giua: PAD.l + i * nhom + nhom / 2, hienNhan: i % buocNhan === 0,
      }
    }),
  }
}
const bdXu = computed(() => bieuDo(d.value?.xuHuong))
const bdCt = computed(() => bieuDo(d.value?.chiTietKy))
const tenDonVi = computed(() => (kieu.value === 'THANG' ? 'tháng' : kieu.value === 'QUY' ? 'quý' : 'năm'))
const nhanKy = computed(() => d.value?.ky?.nhan ?? '')

/* ----- Excel ----- */
function xuatExcel() {
  const v = d.value
  if (!v) return
  const T = (header, key, width = 18, money = false) => ({ header, key, width, money })
  const tang = (p) => (p === null || p === undefined ? '—' : `${p}%`)
  const cot = [T('Chỉ số', 'ten', 26), T(v.ky.nhan, 'a', 20), T(v.kyTruoc.ky.nhan, 'b', 20), T('± so với kỳ trước', 'pb', 18)]
  if (v.cungKyNamTruoc) cot.push(T(v.cungKyNamTruoc.ky.nhan, 'c', 20), T('± so với cùng kỳ', 'pc', 18))
  const sheets = [
    {
      sheetName: 'Tổng hợp',
      columns: cot,
      rows: CHI_SO.map((c) => ({
        ten: c.ten, a: v.hienTai[c.k], b: v.kyTruoc[c.k], pb: tang(v.tangSoVoiKyTruoc[c.k]),
        c: v.cungKyNamTruoc?.[c.k], pc: v.tangSoVoiCungKy ? tang(v.tangSoVoiCungKy[c.k]) : undefined,
      })),
    },
    {
      sheetName: `Xu hướng theo ${tenDonVi.value}`,
      columns: [T('Kỳ', 'nhan', 12), T('Doanh thu thực tế', 'doanhThu', 20, true), T('Cùng kỳ năm trước', 'doanhThuNamTruoc', 20, true), T('Tăng trưởng', 'tt', 14), T('Số đơn hoàn thành', 'soDon')],
      rows: v.xuHuong.map((x) => ({ ...x, tt: tang(x.tangTruong) })),
    },
    {
      sheetName: 'Chi tiết trong kỳ',
      columns: [T(kieu.value === 'THANG' ? 'Ngày' : 'Tháng', 'nhan', 12), T('Doanh thu thực tế', 'doanhThu', 20, true), T('Doanh thu dự kiến', 'doanhThuDuKien', 20, true), T('Cùng kỳ năm trước', 'doanhThuNamTruoc', 20, true), T('Số đơn hoàn thành', 'soDon')],
      rows: v.chiTietKy,
    },
    {
      sheetName: 'Sản phẩm',
      columns: [T('Mã', 'maSanPham', 12), T('Sản phẩm', 'tenSanPham', 36), T('Số lượng', 'soLuong'), T('Kỳ trước', 'soLuongKyTruoc'), T('Tăng trưởng', 'tt', 14), T('Doanh thu', 'doanhThu', 20, true)],
      rows: v.sanPham.map((x) => ({ ...x, tt: tang(x.tangTruong) })),
    },
  ]
  exportExcel({ filename: `so-sanh_${v.kieu.toLowerCase()}_${v.ky.ma}.xlsx`, sheets })
  toast.success('Đã xuất file Excel so sánh.')
}

/* ----- Gửi email ----- */
const moEmail = ref(false)
const email = ref('')
const loiEmail = ref('')
const dangGui = ref(false)
function batEmail() {
  email.value = email.value || auth.trangThai.nguoiDung?.email || ''
  loiEmail.value = ''
  moEmail.value = !moEmail.value
}
async function guiEmail() {
  const e = email.value.trim()
  if (!e) {
    loiEmail.value = 'Vui lòng nhập địa chỉ email nhận báo cáo.'
    return
  }
  if (e.length > 120 || !/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(e)) {
    loiEmail.value = 'Email không hợp lệ.'
    return
  }
  dangGui.value = true
  try {
    const res = (await api.post('/thong-ke/so-sanh/gui-email', { kieu: kieu.value, ky: ky.value, email: e })).data
    toast.success(res.giaLap ? `Chưa cấu hình Gmail nên thư tới ${e} chỉ được ghi vào log máy chủ.` : `Đã gửi báo cáo ${nhanKy.value} tới ${e}.`)
    moEmail.value = false
  } catch (err) {
    loiEmail.value = err.message || 'Không gửi được email.'
  } finally {
    dangGui.value = false
  }
}
</script>

<template>
  <div class="ss-page">
    <section class="ad-card ss-filter-card">
      <div class="d-flex flex-wrap align-items-center gap-3">
        <div class="ad-card-head mb-0 me-auto">
          <span class="ad-icon-box"><i class="bi bi-bar-chart-line" aria-hidden="true"></i></span>
          <div>
            <h2 class="ad-card-title">So sánh doanh thu</h2>
            <p class="ad-card-sub">
              <RouterLink to="/thong-ke" class="fw-semibold text-decoration-none"><i class="bi bi-arrow-left" aria-hidden="true"></i> Quay lại Thống kê</RouterLink>
              <template v-if="d"> · <strong>{{ d.ky.nhan }}</strong> ({{ formatDate(d.ky.tuNgay) }} – {{ formatDate(d.ky.denNgay) }})</template>
            </p>
          </div>
        </div>
        <div class="btn-group" role="group" aria-label="Kiểu so sánh">
          <button v-for="o in KIEU" :key="o.v" type="button" class="btn btn-sm" :class="kieu === o.v ? 'btn-primary' : 'btn-outline-primary'" :aria-pressed="kieu === o.v" @click="kieu = o.v">{{ o.t }}</button>
        </div>
        <div class="d-flex align-items-center gap-2">
          <input v-if="kieu === 'THANG'" v-model="thang" type="month" class="form-control form-control-sm ss-o" :max="homNay.slice(0, 7)" aria-label="Chọn tháng" data-test="ss-thang" />
          <template v-else-if="kieu === 'QUY'">
            <select v-model.number="quy" class="form-select form-select-sm ss-o-nho" aria-label="Chọn quý">
              <option v-for="q in 4" :key="q" :value="q">Quý {{ q }}</option>
            </select>
            <select v-model.number="namQuy" class="form-select form-select-sm ss-o-nho" aria-label="Năm của quý">
              <option v-for="y in dsNam" :key="y" :value="y">{{ y }}</option>
            </select>
          </template>
          <select v-else v-model.number="nam" class="form-select form-select-sm ss-o-nho" aria-label="Chọn năm">
            <option v-for="y in dsNam" :key="y" :value="y">{{ y }}</option>
          </select>
        </div>
        <div class="d-flex gap-2">
          <button type="button" class="ad-btn ad-btn-sm" :disabled="!d" data-test="ss-excel" @click="xuatExcel"><i class="bi bi-file-earmark-excel" aria-hidden="true"></i> Xuất Excel</button>
          <button type="button" class="ad-btn ad-btn-sm ad-btn-primary" :disabled="!d" data-test="ss-email" @click="batEmail"><i class="bi bi-envelope" aria-hidden="true"></i> Gửi email</button>
        </div>
      </div>

      <div v-if="d" class="ss-ky-bar mt-3">
        <span class="ss-ky-chip is-main"><i class="bi bi-calendar-check me-1"></i>Kỳ đang xem: <strong>{{ d.ky.nhan }}</strong> ({{ formatDate(d.ky.tuNgay) }} – {{ formatDate(d.ky.denNgay) }})</span>
        <span class="ss-ky-chip"><i class="bi bi-arrow-left-right me-1"></i>Kỳ liền trước: <strong>{{ d.kyTruoc.ky.nhan }}</strong></span>
        <span v-if="d.cungKyNamTruoc" class="ss-ky-chip"><i class="bi bi-clock-history me-1"></i>Cùng kỳ năm trước: <strong>{{ d.cungKyNamTruoc.ky.nhan }}</strong></span>
        <span v-if="d.hienTai?.tongGiamGia !== undefined" class="ss-ky-chip"><i class="bi bi-ticket-perforated me-1"></i>Tổng giảm giá / KM: <strong>{{ formatMoney(d.hienTai.tongGiamGia || 0) }}</strong></span>
      </div>

      <div v-if="canhBaoKy" class="alert alert-warning d-flex align-items-center gap-2 py-2 px-3 mt-3 mb-0 small" role="status" data-test="ss-canh-bao-ky">
        <i class="bi bi-exclamation-triangle-fill" aria-hidden="true"></i>
        <span>{{ canhBaoKy }}</span>
        <button type="button" class="btn btn-sm btn-warning ms-auto" data-test="ss-ve-hien-tai" @click="datVeKyHienTai">Về kỳ hiện tại</button>
      </div>

      <form v-if="moEmail" class="ss-email" novalidate @submit.prevent="guiEmail">
        <label class="ad-label m-0" for="ss-email-o">Gửi báo cáo {{ nhanKy }} (phân tích + bảng so sánh + biểu đồ) tới</label>
        <input id="ss-email-o" v-model="email" type="email" class="form-control form-control-sm" :class="{ 'is-invalid': loiEmail }" placeholder="email@gmail.com" @input="loiEmail = ''" />
        <button type="submit" class="ad-btn ad-btn-sm ad-btn-primary" :disabled="dangGui">
          <span v-if="dangGui" class="spinner-border spinner-border-sm me-1" aria-hidden="true"></span>Gửi
        </button>
        <p v-if="loiEmail" class="ad-error w-100 mb-0">{{ loiEmail }}</p>
        <p class="ad-hint w-100 mb-0">Ngoài ra, hệ thống tự động gửi email báo cáo phân tích hằng ngày (21:00) và vào cuối mỗi tháng cho quản lý.</p>
      </form>
    </section>

    <div v-if="loi" class="alert alert-warning small" role="alert">{{ loi }}</div>
    <div v-if="dangTai && !d" class="ad-empty">Đang tải…</div>

    <template v-if="d">
      <div class="row g-3">
        <div v-for="c in CHI_SO" :key="c.k" class="col-12 col-sm-6 col-xl-3">
          <section class="ad-card ss-kpi-card h-100 mb-0" data-test="ss-chi-so">
            <div class="d-flex align-items-center gap-2 mb-2">
              <span class="ss-kpi-ico"><i class="bi" :class="c.icon" aria-hidden="true"></i></span>
              <span class="text-secondary fw-semibold small">{{ c.ten }}</span>
            </div>
            <div class="fs-4 fw-bold text-dark">{{ giaTri(c, d.hienTai[c.k]) }}</div>
            <div class="small mt-2">
              <span :class="moTaTang(d.tangSoVoiKyTruoc[c.k], c.xauKhiTang).lop" class="fw-bold">{{ moTaTang(d.tangSoVoiKyTruoc[c.k], c.xauKhiTang).text }}</span>
              <span class="text-muted"> so với {{ d.kyTruoc.ky.nhan }} ({{ giaTri(c, d.kyTruoc[c.k]) }})</span>
            </div>
            <div v-if="d.cungKyNamTruoc" class="small mt-1">
              <span :class="moTaTang(d.tangSoVoiCungKy[c.k], c.xauKhiTang).lop" class="fw-bold">{{ moTaTang(d.tangSoVoiCungKy[c.k], c.xauKhiTang).text }}</span>
              <span class="text-muted"> so với cùng kỳ {{ d.cungKyNamTruoc.ky.nhan }}</span>
            </div>
          </section>
        </div>
      </div>

      <div class="row g-3">
        <div class="col-12 col-xl-6">
          <section class="ad-card h-100 mb-0" data-test="ss-xu-huong">
            <h3 class="fs-6 fw-bold mb-1">Tăng trưởng doanh thu theo {{ tenDonVi }}</h3>
            <p class="small text-muted mb-2">
              <span class="ss-chu ss-chu-a"></span> {{ kieu === 'NAM' ? 'Doanh thu thực tế' : `Năm ${d.ky.tuNgay.slice(0, 4)}` }}
              <template v-if="bdXu.coB"><span class="ss-chu ss-chu-b ms-3"></span> Cùng kỳ năm trước</template>
              · số trên cột: % tăng trưởng
            </p>
            <svg :viewBox="`0 0 ${W} ${H}`" class="w-100" role="img" :aria-label="`Biểu đồ tăng trưởng doanh thu theo ${tenDonVi}`">
              <g v-for="t in bdXu.ticks" :key="t.y">
                <line :x1="PAD.l" :x2="W - PAD.r" :y1="t.y" :y2="t.y" stroke="#e3eaf3" />
                <text :x="PAD.l - 8" :y="t.y + 4" text-anchor="end" font-size="11" fill="#7a8696">{{ t.nhan }}</text>
              </g>
              <g v-for="c in bdXu.cot" :key="c.ma">
                <rect :x="c.xa" :y="c.ya" :width="c.rong" :height="c.ha" rx="3" :fill="c.ma === d.ky.ma ? '#0756b0' : '#0977ec'"><title>{{ c.nhan }}: {{ formatMoney(c.doanhThu) }}</title></rect>
                <rect v-if="bdXu.coB" :x="c.xb" :y="c.yb" :width="c.rong" :height="c.hb" rx="3" fill="#b8c4d6"><title>{{ c.nhan }} năm trước: {{ formatMoney(c.doanhThuNamTruoc || 0) }}</title></rect>
                <text v-if="c.tangTruong !== null && c.tangTruong !== undefined && c.doanhThu" :x="c.giua" :y="Math.min(c.ya, bdXu.coB ? c.yb : c.ya) - 6" text-anchor="middle" font-size="10" :fill="c.tangTruong >= 0 ? '#17a34a' : '#e5484d'">{{ c.tangTruong >= 0 ? '+' : '' }}{{ String(c.tangTruong).replace('.', ',') }}%</text>
                <text v-if="c.hienNhan" :x="c.giua" :y="H - 10" text-anchor="middle" font-size="11" :font-weight="c.ma === d.ky.ma ? 700 : 400" fill="#5b6b80">{{ c.nhan }}</text>
              </g>
            </svg>
          </section>
        </div>

        <div class="col-12 col-xl-6">
          <section class="ad-card h-100 mb-0" data-test="ss-chi-tiet">
            <h3 class="fs-6 fw-bold mb-1">Chi tiết {{ d.ky.nhan }}</h3>
            <p class="small text-muted mb-2">
              <span class="ss-chu ss-chu-a"></span> {{ d.ky.nhan }} <span class="ss-chu ss-chu-b ms-3"></span> Cùng kỳ năm trước
            </p>
            <svg :viewBox="`0 0 ${W} ${H}`" class="w-100" role="img" :aria-label="`Biểu đồ chi tiết ${d.ky.nhan}`">
              <g v-for="t in bdCt.ticks" :key="t.y">
                <line :x1="PAD.l" :x2="W - PAD.r" :y1="t.y" :y2="t.y" stroke="#e3eaf3" />
                <text :x="PAD.l - 8" :y="t.y + 4" text-anchor="end" font-size="11" fill="#7a8696">{{ t.nhan }}</text>
              </g>
              <g v-for="c in bdCt.cot" :key="c.ma">
                <rect :x="c.xa" :y="c.ya" :width="c.rong" :height="c.ha" rx="2" fill="#0977ec"><title>{{ c.nhan }}: {{ formatMoney(c.doanhThu) }} ({{ c.soDon }} đơn)</title></rect>
                <rect v-if="bdCt.coB" :x="c.xb" :y="c.yb" :width="c.rong" :height="c.hb" rx="2" fill="#b8c4d6"><title>{{ c.nhan }} năm trước: {{ formatMoney(c.doanhThuNamTruoc || 0) }}</title></rect>
                <text v-if="c.hienNhan" :x="c.giua" :y="H - 10" text-anchor="middle" font-size="11" fill="#5b6b80">{{ c.nhan }}</text>
              </g>
            </svg>
          </section>
        </div>

        <div class="col-12">
          <section class="ad-card mb-0" data-test="ss-san-pham">
            <h3 class="fs-6 fw-bold mb-3">Sản phẩm bán chạy {{ d.ky.nhan }} so với {{ d.kyTruoc.ky.nhan }}</h3>
            <div v-if="!d.sanPham.length" class="ad-empty">Chưa bán được sản phẩm nào trong kỳ này.</div>
            <div v-else class="ad-table-wrap">
              <div class="ad-table-scroll">
                <table class="ad-table">
                  <thead>
                    <tr><th class="ad-col-stt">STT</th><th>Sản phẩm</th><th class="text-end">Số lượng</th><th class="text-end">Kỳ trước</th><th class="text-end">Tăng trưởng</th><th class="text-end">Doanh thu</th></tr>
                  </thead>
                  <tbody>
                    <tr v-for="(p, i) in d.sanPham" :key="p.maSanPham">
                      <td class="ad-col-stt">{{ i + 1 }}</td>
                      <td>{{ p.tenSanPham }}<small class="text-muted d-block">{{ p.maSanPham }}</small></td>
                      <td class="text-end fw-semibold">{{ p.soLuong }}</td>
                      <td class="text-end">{{ p.soLuongKyTruoc }}</td>
                      <td class="text-end fw-semibold" :class="moTaTang(p.tangTruong).lop">{{ moTaTang(p.tangTruong).text }}</td>
                      <td class="text-end ad-nowrap">{{ formatMoney(p.doanhThu) }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </section>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.ss-page {
  display: flex;
  flex-direction: column;
  gap: 1.15rem;
}
.ss-page :deep(*),
.ss-page input,
.ss-page select,
.ss-page option,
.ss-page button,
.ss-page svg text {
  font-family: inherit !important;
}
.ss-filter-card {
  border-radius: 18px;
  border: 1px solid #e5ecf4;
}
.ss-ky-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  padding-top: 0.85rem;
  border-top: 1px solid #eef2f7;
}
.ss-ky-chip {
  display: inline-flex;
  align-items: center;
  padding: 0.3rem 0.75rem;
  border-radius: 999px;
  background: #f1f5f9;
  color: #475569;
  font-size: 0.8rem;
  font-weight: 500;
}
.ss-ky-chip.is-main {
  background: #e7f1fd;
  color: #075fc0;
}
.ss-kpi-card {
  position: relative;
  border-radius: 16px;
  border: 1px solid #e8eef5;
  overflow: hidden;
  transition: transform 0.16s ease, box-shadow 0.16s ease;
}
.ss-kpi-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.07);
}
.ss-kpi-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #0977ec, #38bdf8);
}
.ss-kpi-ico {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: #e8f3fe;
  color: #0977ec;
  display: grid;
  place-items: center;
  font-size: 1rem;
}
.ss-o,
.ss-o-nho {
  height: 38px;
  font-family: inherit !important;
  font-size: 0.875rem !important;
  font-weight: 400 !important;
  color: var(--fs-text, #18242f) !important;
  border-radius: 10px;
}
.ss-o {
  width: 170px;
}
.ss-o-nho {
  width: 120px;
}
.ss-tot {
  color: #15803d;
}
.ss-xau {
  color: #be123c;
}
.ss-chu {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 3px;
  vertical-align: middle;
}
.ss-chu-a {
  background: #0977ec;
}
.ss-chu-b {
  background: #b8c4d6;
}
.ss-email {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.75rem;
  padding-top: 0.75rem;
  border-top: 1px solid #eef2f7;
}
.ss-email input {
  max-width: 280px;
}
</style>

<style>
.ss-page input[type='month'],
.ss-page input[type='month']::-webkit-datetime-edit,
.ss-page input[type='month']::-webkit-datetime-edit-fields-wrapper,
.ss-page input[type='month']::-webkit-datetime-edit-month-field,
.ss-page input[type='month']::-webkit-datetime-edit-year-field,
.ss-page input[type='month']::-webkit-datetime-edit-text {
  font-family: inherit !important;
  font-size: 0.875rem !important;
  font-weight: 400 !important;
}
</style>
