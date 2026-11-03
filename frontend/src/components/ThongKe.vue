<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import api from '../services/api'
import { useToast } from '../composables/useToast'
import { useAutoRefresh } from '../composables/useAutoRefresh'
import { formatDate, formatMoney, todayIso } from '../utils/format'
import { TRANG_THAI_HOA_DON, TT } from '../constants/hoaDon'
import {
  TT_HOAN_THANH, congNgay, gioLam, gomCot, khoangGuiMayChu, khungGioHienThi, kyTruoc, lamTronTran,
  laHoaDonCho, locTheoKhoang, nhanTienGon, phanTram, phanTramThayDoi, soNgay, theoLoaiDon, tinhKhoang, tongHop, tyLeCoMat, tyLeDungGio, xepHang,
} from '../utils/thongKe'
import DateInput from './common/DateInput.vue'
import { exportExcel } from '../utils/exportExcel'

const toast = useToast()
const hoaDon = ref([])
const loading = ref(true)
const homNay = ref(todayIso())

// Bộ lọc thống kê đồng bộ giữa thanh công cụ trên và nội dung hiển thị bên dưới (mặc định: Tháng này từ ngày 01 tới hôm nay)
const dauThangNay = (iso) => `${String(iso).slice(0, 7)}-01`
const khoang = ref(-1) // 1 | 7 | 30 | 90 | 0 (tất cả) | -1 (tự chọn / lịch tháng-năm)
const mocChon = ref('thangNay') // 'tatCa' | 'homNay' | '7ngay' | 'thangNay' | 'thangTruoc' | 'namNay' | 'tuChon'
const tuChon = ref(dauThangNay(todayIso()))
const denChon = ref(todayIso())
const donViChon = ref('') // '' (tự động) | 'ngày' | 'tuần' | 'tháng'
const thuongHieuChon = ref('')
const tuKhoaSp = ref('')
const hienMoRong = ref(false)

const NUT_MOC_NHANH = [
  { ma: 'tatCa', nhan: 'Tất cả', v: 0 },
  { ma: 'homNay', nhan: 'Hôm nay', v: 1 },
  { ma: '7ngay', nhan: '7 ngày qua', v: 7 },
  { ma: 'thangNay', nhan: 'Tháng này', v: -1 },
  { ma: 'thangTruoc', nhan: 'Tháng trước', v: -1 },
  { ma: 'namNay', nhan: 'Năm nay', v: -1 },
]

const boDau = (s) =>
  String(s ?? '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .toLowerCase()
    .trim()

const ISO_RE = /^\d{4}-\d{2}-\d{2}$/

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
    hoaDon.value = (res?.data?.content ?? []).filter((h) => !laHoaDonCho(h))
    homNay.value = todayIso()
  } catch (e) {
    toast.error(e.message || 'Không tải được dữ liệu thống kê.')
  } finally {
    loading.value = false
  }
}
onMounted(load)
useAutoRefresh(load)

function chonMoc(m) {
  mocChon.value = m.ma
  const hn = homNay.value
  if (m.v > 0) {
    khoang.value = m.v
    tuChon.value = congNgay(hn, -(m.v - 1))
    denChon.value = hn
  } else if (m.v === 0) {
    khoang.value = 0
    tuChon.value = ''
    denChon.value = ''
  } else if (m.ma === 'thangNay') {
    khoang.value = -1
    tuChon.value = `${hn.slice(0, 7)}-01`
    denChon.value = hn
  } else if (m.ma === 'thangTruoc') {
    khoang.value = -1
    const [y, mo] = hn.slice(0, 7).split('-').map(Number)
    const dCuoi = new Date(y, mo - 1, 0, 12)
    const yT = dCuoi.getFullYear()
    const mT = String(dCuoi.getMonth() + 1).padStart(2, '0')
    const dT = String(dCuoi.getDate()).padStart(2, '0')
    tuChon.value = `${yT}-${mT}-01`
    denChon.value = `${yT}-${mT}-${dT}`
  } else if (m.ma === 'namNay') {
    khoang.value = -1
    tuChon.value = `${hn.slice(0, 4)}-01-01`
    denChon.value = hn
  } else {
    khoang.value = -1
    if (!tuChon.value && !denChon.value) {
      tuChon.value = congNgay(hn, -29)
      denChon.value = hn
    }
  }
}

function capNhatTuNgay(val) {
  tuChon.value = val
  khoang.value = -1
  mocChon.value = 'tuChon'
}

function capNhatDenNgay(val) {
  denChon.value = val
  khoang.value = -1
  mocChon.value = 'tuChon'
}

function datLaiBoLoc() {
  khoang.value = -1
  mocChon.value = 'thangNay'
  tuChon.value = dauThangNay(homNay.value)
  denChon.value = homNay.value
  donViChon.value = ''
  thuongHieuChon.value = ''
  tuKhoaSp.value = ''
}

/* ----- Validate khoảng thời gian Tự chọn & các case khó ----- */
const canhBaoNgay = computed(() => {
  if (khoang.value !== -1) return null
  const tu = String(tuChon.value || '').trim()
  const den = String(denChon.value || '').trim()
  if (tu && !ISO_RE.test(tu)) {
    return { loai: 'loi', text: 'Định dạng "Từ ngày" không hợp lệ (cần YYYY-MM-DD).' }
  }
  if (den && !ISO_RE.test(den)) {
    return { loai: 'loi', text: 'Định dạng "Đến ngày" không hợp lệ (cần YYYY-MM-DD).' }
  }
  if ((tu && tu < '2000-01-01') || (den && den < '2000-01-01')) {
    return {
      loai: 'quaXa',
      text: 'Năm chọn quá xa trong quá khứ (trước năm 2000). Vui lòng chọn mốc từ năm 2000 trở lại đây.',
    }
  }
  if (tu && den && tu > den) {
    return {
      loai: 'daoNguoc',
      text: `"Từ ngày" (${formatDate(tu)}) đang lớn hơn "Đến ngày" (${formatDate(den)}). Hệ thống đã tự động đảo lại thành ${formatDate(den)} – ${formatDate(tu)}.`,
    }
  }
  if ((tu && tu > homNay.value) || (den && den > homNay.value)) {
    return {
      loai: 'tuongLai',
      text: `Khoảng ngày đang chọn vượt quá ngày hiện tại (${formatDate(homNay.value)}).`,
    }
  }
  return null
})

function chuanHoaKhoangNgay() {
  if (tuChon.value && tuChon.value < '2000-01-01') tuChon.value = '2020-01-01'
  if (denChon.value && denChon.value < '2000-01-01') denChon.value = homNay.value
  if (tuChon.value && denChon.value && tuChon.value > denChon.value) {
    const tmp = tuChon.value
    tuChon.value = denChon.value
    denChon.value = tmp
  }
  if (denChon.value && denChon.value > homNay.value) denChon.value = homNay.value
  if (tuChon.value && tuChon.value > homNay.value) tuChon.value = homNay.value
}

const kq = computed(() => tinhKhoang(khoang.value, tuChon.value, denChon.value, homNay.value))
const trongKhoang = computed(() => locTheoKhoang(hoaDon.value, kq.value.tu, kq.value.den))
const tk = computed(() => tongHop(trongKhoang.value))
const ky = computed(() => kyTruoc(kq.value.tu, kq.value.den))
const tkTruoc = computed(() => (ky.value ? tongHop(locTheoKhoang(hoaDon.value, ky.value.tu, ky.value.den)) : null))
const moTaKhoang = computed(() => {
  const { tu, den } = kq.value
  if (!tu && !den) return 'Toàn bộ thời gian'
  if (tu && den) return tu === den ? `từ ngày ${formatDate(tu)}` : `${formatDate(tu)} – ${formatDate(den)}`
  return tu ? `từ ngày ${formatDate(tu)}` : `đến ngày ${formatDate(den)}`
})

const pct = (x) => `${(Math.round(x * 10) / 10).toString().replace('.', ',')}%`

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
    {
      label: 'Doanh thu thực tế',
      value: formatMoney(a.doanhThu),
      icon: 'bi-cash-coin',
      tone: 'blue',
      note: 'Đơn đã hoàn thành, không gồm phí ship',
      so: soSanh(a.doanhThu, b.doanhThu),
    },
    {
      label: 'Doanh thu dự kiến',
      value: formatMoney(a.doanhThuDuKien),
      icon: 'bi-graph-up-arrow',
      tone: 'sky',
      note: a.doanhThuDuKien > a.doanhThu ? `Còn ${formatMoney(a.doanhThuDuKien - a.doanhThu)} từ đơn đang xử lý / giao` : 'Mọi đơn chưa hủy',
      so: soSanh(a.doanhThuDuKien, b.doanhThuDuKien),
    },
    {
      label: 'Đơn hoàn thành',
      value: `${a.hoanThanh}/${a.tongDon}`,
      icon: 'bi-check2-circle',
      tone: 'green',
      note: a.hoanThanh ? `${pct(a.tyLeHoanThanh)} tổng đơn · TB ${formatMoney(a.trungBinh)}/đơn` : 'Chưa có đơn hoàn thành',
      so: soSanh(a.hoanThanh, b.hoanThanh),
    },
    {
      label: 'Đơn đã hủy',
      value: a.huy,
      icon: 'bi-x-circle',
      tone: 'rose',
      note: a.tongDon ? `${pct(a.tyLeHuy)} tổng đơn` : 'Chưa có đơn',
      so: soSanh(a.huy, b.huy, true),
    },
  ]
})

/* ----- Biểu đồ cột Doanh thu theo ngày / tuần / tháng (điều khiển từ ô THỐNG KÊ) ----- */
const bieuDo = computed(() => gomCot(hoaDon.value, kq.value.tu, kq.value.den, homNay.value, donViChon.value))
const tatCaCot = computed(() => bieuDo.value.cot)
const cotCoDoanhThu = computed(() => tatCaCot.value.filter((x) => x.v > 0))
const chiHienCoDt = ref(true)
const coCotTrong = computed(() => tatCaCot.value.length > cotCoDoanhThu.value.length && cotCoDoanhThu.value.length > 0)
const cot = computed(() => (chiHienCoDt.value && cotCoDoanhThu.value.length ? cotCoDoanhThu.value : tatCaCot.value))
const coDoanhThu = computed(() => cotCoDoanhThu.value.length > 0)
const dinhBieuDo = computed(() => cotCoDoanhThu.value.reduce((max, x) => (x.v > (max?.v ?? 0) ? x : max), null))
const W = 860, H = 300, PAD = { l: 60, r: 20, t: 34, b: 48 }
function tinhTranBieuDo(max) {
  if (!(max > 0)) return 1
  const mucTieu = max * 1.08
  const mu = 10 ** Math.floor(Math.log10(mucTieu))
  const f = mucTieu / mu
  const bac = [1, 1.2, 1.6, 2, 2.4, 3, 4, 5, 6, 8, 10]
  const chon = bac.find((b) => b >= f) ?? 10
  return Math.round(chon * mu)
}
const vMax = computed(() => tinhTranBieuDo(Math.max(...cot.value.map((x) => x.v), 0)))
const rongCot = computed(() => (W - PAD.l - PAD.r) / Math.max(cot.value.length, 1))
const rongThanh = computed(() => Math.min(rongCot.value * 0.52, 52))
const cao = (v) => (v > 0 ? Math.max((v / vMax.value) * (H - PAD.t - PAD.b), 6) : 0)
const ticks = computed(() => [0, 0.25, 0.5, 0.75, 1].map((t) => ({ t, y: H - PAD.b - t * (H - PAD.t - PAD.b), nhan: nhanTienGon(vMax.value * t) })))
function nhanTienCot(v) {
  if (!v) return ''
  const ty = 1_000_000_000, tr = 1_000_000, k = 1_000
  const gon = (x, dv) => `${Number(x.toFixed(2)).toString().replace('.', ',')}${dv}`
  if (v >= ty) return gon(v / ty, ' tỷ')
  if (v >= tr) return gon(v / tr, 'tr')
  if (v >= k) return gon(v / k, 'k')
  return String(v)
}
const nhanTruc = computed(() => {
  const ds = cot.value
  const n = ds.length
  if (!n) return []
  // Nếu số cột <= 16 thì hiển thị đầy đủ 100% nhãn trục ngang
  if (n <= 16) return ds.map((x) => x.nhan)

  const chon = new Set()
  for (let i = 0; i < n; i++) {
    if (ds[i].v > 0) chon.add(i)
  }
  if (!chon.has(0) && !chon.has(1)) chon.add(0)
  if (!chon.has(n - 1) && !chon.has(n - 2)) chon.add(n - 1)

  const Neo = [...chon].sort((a, b) => a - b)
  if (!Neo.length) {
    Neo.push(0, n - 1)
    chon.add(0)
    chon.add(n - 1)
  } else {
    if (Neo[0] >= 3) {
      chon.add(0)
      Neo.unshift(0)
    }
    if (n - 1 - Neo[Neo.length - 1] >= 3) {
      chon.add(n - 1)
      Neo.push(n - 1)
    }
  }
  for (let idx = 0; idx < Neo.length - 1; idx++) {
    const a = Neo[idx]
    const b = Neo[idx + 1]
    const gap = b - a
    if (gap >= 4) {
      const soKhoang = Math.max(2, Math.round(gap / 3.5))
      for (let m = 1; m < soKhoang; m++) {
        chon.add(Math.round(a + (gap * m) / soKhoang))
      }
    }
  }
  return ds.map((x, i) => (chon.has(i) ? x.nhan : ''))
})
const nhanGiaTri = computed(() => {
  const ds = cot.value
  const n = ds.length
  const daDat = []
  return ds.map((x, i) => {
    if (!(x.v > 0)) return null
    const cx = PAD.l + i * rongCot.value + rongCot.value / 2
    let cy = H - PAD.b - cao(x.v) - 8
    // Khi <= 8 cột: hiển thị số tiền chính xác tuyệt đối (VD: 5.372.500 ₫); khi nhiều cột: hiển thị rút gọn 2 chữ số thập phân (5,37tr)
    const text = n <= 8 ? formatMoney(x.v) : nhanTienCot(x.v)
    if (daDat.some((p) => Math.abs(cx - p.x) < 28 && Math.abs(cy - p.y) < 12)) {
      cy = Math.max(12, cy - 12)
    }
    daDat.push({ x: cx, y: cy })
    return { text, y: cy }
  })
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

/* ----- Dữ liệu từ GET /api/thong-ke ----- */
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
    if (lan === lanTai) loiGd3.value = e.message || 'Không tải được dữ liệu thống kê.'
  } finally {
    if (lan === lanTai) dangTaiGd3.value = false
  }
}
watch(() => [khoangGd3.value.tuNgay, khoangGd3.value.denNgay], taiGd3)

const banChayGoc = computed(() => (Array.isArray(gd3.value.sanPhamBanChay) ? gd3.value.sanPhamBanChay : []))
const sapHetGoc = computed(() => (Array.isArray(gd3.value.sapHetHang) ? gd3.value.sapHetHang : []))

// Danh sách thương hiệu cho ô lọc THƯƠNG HIỆU trên bộ lọc
const dsThuongHieu = computed(() => {
  const tap = new Set()
  for (const p of banChayGoc.value) if (p.thuongHieu) tap.add(String(p.thuongHieu).trim())
  for (const s of sapHetGoc.value) if (s.thuongHieu) tap.add(String(s.thuongHieu).trim())
  return [...tap].filter(Boolean).sort((a, b) => a.localeCompare(b, 'vi'))
})

// Map tên sản phẩm -> thương hiệu để lọc bảng Sắp hết hàng theo thương hiệu
const mapThuongHieuTheoSp = computed(() => {
  const m = new Map()
  for (const p of banChayGoc.value) if (p.idSanPham && p.thuongHieu) m.set(p.idSanPham, p.thuongHieu)
  return m
})

// Lọc Sản phẩm bán chạy & Sắp hết hàng theo đúng ô THƯƠNG HIỆU và TÌM SẢN PHẨM trên bộ lọc
const banChay = computed(() => {
  const th = thuongHieuChon.value.trim()
  const kw = boDau(tuKhoaSp.value)
  return banChayGoc.value.filter((p) => {
    const khopTh = !th || String(p.thuongHieu || '').trim() === th
    const khopKw = !kw || boDau(`${p.maSanPham || ''} ${p.tenSanPham || ''} ${p.phanLoaiBanChay || ''} ${p.thuongHieu || ''}`).includes(kw)
    return khopTh && khopKw
  })
})

const sapHet = computed(() => {
  const th = thuongHieuChon.value.trim()
  const kw = boDau(tuKhoaSp.value)
  return sapHetGoc.value.filter((c) => {
    const tenTh = c.thuongHieu || mapThuongHieuTheoSp.value.get(c.idSanPham) || ''
    const khopTh = !th || String(tenTh).trim() === th
    const khopKw = !kw || boDau(`${c.ma || ''} ${c.tenSanPham || ''} ${c.phanLoai || ''} ${tenTh}`).includes(kw)
    return khopTh && khopKw
  })
})

const theoPt = computed(() => {
  const ds = Array.isArray(gd3.value.theoPhuongThuc) ? gd3.value.theoPhuongThuc : []
  const tong = ds.reduce((s, x) => s + (Number(x.soTien) || 0), 0)
  return ds.map((x) => ({ ...x, pct: phanTram(x.soTien, tong) }))
})
const tongTienPt = computed(() => theoPt.value.reduce((s, x) => s + (Number(x.soTien) || 0), 0))
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

/* ----- Xuất Excel chuyên nghiệp & đầy đủ nội dung (tập trung Tổng doanh thu, Tổng đơn hàng, Sản phẩm, Nhân viên, Khách hàng) ----- */
function xuatExcel() {
  const a = tk.value
  const b = tkTruoc.value
  const C = (header, key, width = 18, opts = {}) => ({ header, key, width, ...opts })
  const bienDong = (nay, truoc) => {
    if (!b) return '—'
    const p = phanTramThayDoi(nay, truoc)
    if (p === null) return truoc === 0 && nay > 0 ? '+100%' : '—'
    if (Math.abs(p) < 0.05) return '0%'
    return `${p > 0 ? '+' : ''}${String(Math.round(p * 10) / 10).replace('.', ',')}%`
  }
  const phuDeChung = `Kỳ thống kê: ${moTaKhoang.value}  |  Thương hiệu: ${thuongHieuChon.value || 'Tất cả thương hiệu'}  |  Từ khóa: ${tuKhoaSp.value || 'Không lọc'}  |  Ngày xuất: ${formatDate(homNay.value)}`
  const nhanKyTruoc = ky.value ? `${formatDate(ky.value.tu)} – ${formatDate(ky.value.den)}` : 'Không áp dụng'

  const dsCot = tatCaCot.value.length ? tatCaCot.value : cot.value
  const tongDtCot = dsCot.reduce((s, x) => s + (Number(x.v) || 0), 0)
  const tongDonCot = dsCot.reduce((s, x) => s + (Number(x.so) || 0), 0)

  const tongDonLoai = theoLoai.value.reduce((s, x) => s + (Number(x.so) || 0), 0)
  const tongDtLoai = theoLoai.value.reduce((s, x) => s + (Number(x.v) || 0), 0)

  const tongSlBanChay = banChay.value.reduce((s, x) => s + (Number(x.soLuong) || 0), 0)
  const tongDonBanChay = banChay.value.reduce((s, x) => s + (Number(x.soDon) || 0), 0)
  const tongDtBanChay = banChay.value.reduce((s, x) => s + (Number(x.doanhThu) || 0), 0)

  const tongTonSapHet = sapHet.value.reduce((s, x) => s + (Number(x.tonKho) || 0), 0)

  const tongDonNv = topNhanVien.value.reduce((s, x) => s + (Number(x.so) || 0), 0)
  const tongDtNv = topNhanVien.value.reduce((s, x) => s + (Number(x.v) || 0), 0)

  const tongDonKh = topKhach.value.reduce((s, x) => s + (Number(x.so) || 0), 0)
  const tongDtKh = topKhach.value.reduce((s, x) => s + (Number(x.v) || 0), 0)

  const sheets = [
    {
      sheetName: 'Tổng quan',
      title: 'I. BÁO CÁO TỔNG QUAN KẾT QUẢ KINH DOANH',
      subtitle: phuDeChung,
      columns: [
        C('STT', 'stt', 8, { align: 'center' }),
        C('Nhóm chỉ số', 'nhom', 22),
        C('Chỉ số thống kê', 'ten', 34),
        C('Kỳ báo cáo', 'gt', 22, { money: true }),
        C('Kỳ liền trước', 'truoc', 22, { money: true }),
        C('Biến động (±%)', 'bienDong', 16, { align: 'center' }),
        C('Diễn giải & Ghi chú', 'ghiChu', 48),
      ],
      rows: [
        { _isSection: true, title: '1. THÔNG TIN KỲ BÁO CÁO & BỘ LỌC' },
        { stt: 1, nhom: 'Bộ lọc', ten: 'Khoảng thời gian báo cáo', gt: moTaKhoang.value, truoc: nhanKyTruoc, bienDong: '—', ghiChu: 'Khoảng ngày áp dụng cho toàn bộ số liệu trong file' },
        { stt: 2, nhom: 'Bộ lọc', ten: 'Thương hiệu / Từ khóa lọc', gt: thuongHieuChon.value || 'Tất cả thương hiệu', truoc: tuKhoaSp.value || 'Không lọc', bienDong: '—', ghiChu: 'Điều kiện lọc chi tiết theo thương hiệu và sản phẩm' },
        { _isSection: true, title: '2. CHỈ SỐ TỔNG DOANH THU & GIÁ TRỊ ĐƠN HÀNG (VNĐ)' },
        { stt: 3, nhom: 'Doanh thu', ten: 'Tổng doanh thu thực tế', gt: a.doanhThu, truoc: b ? b.doanhThu : '—', bienDong: bienDong(a.doanhThu, b?.doanhThu), ghiChu: 'Chỉ tính đơn đã hoàn thành, không bao gồm phí vận chuyển' },
        { stt: 4, nhom: 'Doanh thu', ten: 'Tổng doanh thu dự kiến', gt: a.doanhThuDuKien, truoc: b ? b.doanhThuDuKien : '—', bienDong: bienDong(a.doanhThuDuKien, b?.doanhThuDuKien), ghiChu: 'Gồm mọi đơn hợp lệ chưa hủy (đang xử lý, đang giao, hoàn thành)' },
        { stt: 5, nhom: 'Doanh thu', ten: 'Doanh thu đang xử lý / chờ thu', gt: Math.max(0, a.doanhThuDuKien - a.doanhThu), truoc: b ? Math.max(0, b.doanhThuDuKien - b.doanhThu) : '—', bienDong: '—', ghiChu: 'Phần chênh lệch giữa doanh thu dự kiến và doanh thu thực tế' },
        { stt: 6, nhom: 'Hiệu quả đơn', ten: 'Giá trị trung bình / đơn hoàn thành', gt: a.trungBinh, truoc: b ? b.trungBinh : '—', bienDong: bienDong(a.trungBinh, b?.trungBinh), ghiChu: 'Tổng doanh thu thực tế chia cho tổng số đơn hoàn thành' },
        { _isSection: true, title: '3. CHỈ SỐ TỔNG ĐƠN HÀNG & TỶ LỆ VẬN HÀNH' },
        { stt: 7, nhom: 'Đơn hàng', ten: 'Tổng số đơn hàng phát sinh', gt: `${a.tongDon} đơn`, truoc: b ? `${b.tongDon} đơn` : '—', bienDong: bienDong(a.tongDon, b?.tongDon), ghiChu: 'Tổng đơn hợp lệ (không tính hóa đơn chờ tại quầy)' },
        { stt: 8, nhom: 'Đơn hàng', ten: 'Đơn hàng đã hoàn thành', gt: `${a.hoanThanh} đơn`, truoc: b ? `${b.hoanThanh} đơn` : '—', bienDong: bienDong(a.hoanThanh, b?.hoanThanh), ghiChu: `Tỷ lệ hoàn thành: ${pct(a.tyLeHoanThanh)} tổng số đơn` },
        { stt: 9, nhom: 'Đơn hàng', ten: 'Đơn hàng đang xử lý / đang giao', gt: `${Math.max(0, a.tongDon - a.hoanThanh - a.huy)} đơn`, truoc: b ? `${Math.max(0, b.tongDon - b.hoanThanh - b.huy)} đơn` : '—', bienDong: '—', ghiChu: 'Đơn chờ xác nhận, đã xác nhận, chờ lấy hàng hoặc đang giao' },
        { stt: 10, nhom: 'Đơn hàng', ten: 'Đơn hàng đã hủy', gt: `${a.huy} đơn`, truoc: b ? `${b.huy} đơn` : '—', bienDong: bienDong(a.huy, b?.huy), ghiChu: `Tỷ lệ hủy đơn: ${pct(a.tyLeHuy)} tổng số đơn` },
        { _isSection: true, title: '4. CẢNH BÁO TỒN KHO SẢN PHẨM' },
        { stt: 11, nhom: 'Kho hàng', ten: 'Số biến thể sắp hết / hết hàng', gt: `${sapHet.value.length} biến thể`, truoc: `Hết hàng: ${gd3.value.soHetHang ?? 0}`, bienDong: '—', ghiChu: `Ngưỡng cảnh báo tồn kho ≤ ${gd3.value.nguongSapHet ?? 5} sản phẩm` },
      ],
    },
    {
      sheetName: `Doanh thu theo ${bieuDo.value.donVi}`,
      title: `II. BIẾN ĐỘNG DOANH THU THEO ${bieuDo.value.donVi.toUpperCase()}`,
      subtitle: phuDeChung,
      columns: [
        C('STT', 'stt', 8, { align: 'center' }),
        C('Mốc thời gian', 'nhan', 18, { align: 'center' }),
        C('Từ ngày', 'tuNgay', 15, { align: 'center' }),
        C('Đến ngày', 'denNgay', 15, { align: 'center' }),
        C('Số đơn hoàn thành', 'so', 20, { number: true }),
        C('Doanh thu thực tế (₫)', 'v', 24, { money: true }),
        C('Giá trị TB / đơn (₫)', 'tb', 22, { money: true }),
        C('Tỷ trọng DT (%)', 'tyTrong', 18, { align: 'right' }),
      ],
      rows: dsCot.map((c, idx) => ({
        stt: idx + 1,
        nhan: c.nhan,
        tuNgay: ngayHienThi(c.tu),
        denNgay: ngayHienThi(c.den),
        so: Number(c.so) || 0,
        v: Number(c.v) || 0,
        tb: c.so > 0 ? Math.round(c.v / c.so) : 0,
        tyTrong: pct(phanTram(c.v, tongDtCot)),
      })),
      totalRow: {
        stt: '',
        nhan: 'TỔNG CỘNG',
        tuNgay: '',
        denNgay: '',
        so: tongDonCot,
        v: tongDtCot,
        tb: tongDonCot > 0 ? Math.round(tongDtCot / tongDonCot) : 0,
        tyTrong: tongDtCot > 0 ? '100%' : '0%',
      },
    },
    {
      sheetName: 'Trạng thái đơn',
      title: 'III. CƠ CẤU TRẠNG THÁI ĐƠN HÀNG TRONG KỲ',
      subtitle: phuDeChung,
      columns: [
        C('STT', 'stt', 8, { align: 'center' }),
        C('Trạng thái đơn hàng', 'label', 30),
        C('Số lượng đơn', 'so', 20, { number: true }),
        C('Tỷ trọng (%)', 'tyTrong', 18, { align: 'right' }),
        C('Ghi chú vận hành', 'ghiChu', 44),
      ],
      rows: cauTrucTT.value.map((t, idx) => ({
        stt: idx + 1,
        label: t.label,
        so: Number(t.so) || 0,
        tyTrong: pct(t.pct),
        ghiChu:
          t.label === 'Hoàn thành'
            ? 'Đơn đã thanh toán và ghi nhận vào doanh thu thực tế'
            : t.label === 'Đã hủy'
              ? 'Đơn bị hủy, không tính doanh thu'
              : 'Đơn đang trong quy trình xử lý / vận chuyển',
      })),
      totalRow: {
        stt: '',
        label: 'TỔNG CỘNG',
        so: a.tongDon,
        tyTrong: a.tongDon > 0 ? '100%' : '0%',
        ghiChu: '',
      },
    },
    {
      sheetName: 'Theo loại đơn',
      title: 'IV. DOANH THU THEO KÊNH / LOẠI ĐƠN HÀNG',
      subtitle: phuDeChung,
      columns: [
        C('STT', 'stt', 8, { align: 'center' }),
        C('Kênh / Loại đơn hàng', 'ten', 26),
        C('Số đơn hoàn thành', 'so', 20, { number: true }),
        C('Doanh thu thực tế (₫)', 'v', 24, { money: true }),
        C('Giá trị TB / đơn (₫)', 'tb', 22, { money: true }),
        C('Tỷ trọng DT (%)', 'pct', 18, { align: 'right' }),
      ],
      rows: theoLoai.value.map((l, idx) => ({
        stt: idx + 1,
        ten: l.ten,
        so: Number(l.so) || 0,
        v: Number(l.v) || 0,
        tb: l.so > 0 ? Math.round(l.v / l.so) : 0,
        pct: pct(l.pct),
      })),
      totalRow: {
        stt: '',
        ten: 'TỔNG CỘNG',
        so: tongDonLoai,
        v: tongDtLoai,
        tb: tongDonLoai > 0 ? Math.round(tongDtLoai / tongDonLoai) : 0,
        pct: tongDtLoai > 0 ? '100%' : '0%',
      },
    },
    {
      sheetName: 'Sản phẩm bán chạy',
      title: 'V. BẢNG XẾP HẠNG SẢN PHẨM BÁN CHẠY TRONG KỲ',
      subtitle: phuDeChung,
      columns: [
        C('Hạng', 'hang', 8, { align: 'center' }),
        C('Mã SP', 'maSanPham', 14, { align: 'center' }),
        C('Tên sản phẩm', 'tenSanPham', 38),
        C('Thương hiệu', 'thuongHieu', 20),
        C('Phân loại bán chạy nhất', 'phanLoaiBanChay', 26),
        C('Số lượng bán', 'soLuong', 16, { number: true }),
        C('Số đơn hàng', 'soDon', 16, { number: true }),
        C('Doanh thu (₫)', 'doanhThu', 24, { money: true }),
        C('Tỷ trọng DT (%)', 'tyTrong', 18, { align: 'right' }),
      ],
      rows: banChay.value.map((p, idx) => ({
        hang: idx + 1,
        maSanPham: p.maSanPham || '—',
        tenSanPham: p.tenSanPham,
        thuongHieu: p.thuongHieu || '—',
        phanLoaiBanChay: p.phanLoaiBanChay || '—',
        soLuong: Number(p.soLuong) || 0,
        soDon: Number(p.soDon) || 0,
        doanhThu: Number(p.doanhThu) || 0,
        tyTrong: pct(phanTram(p.doanhThu, tongDtBanChay)),
      })),
      totalRow: {
        hang: '',
        maSanPham: '',
        tenSanPham: 'TỔNG CỘNG TOP SẢN PHẨM',
        thuongHieu: '',
        phanLoaiBanChay: '',
        soLuong: tongSlBanChay,
        soDon: tongDonBanChay,
        doanhThu: tongDtBanChay,
        tyTrong: tongDtBanChay > 0 ? '100%' : '0%',
      },
    },
    {
      sheetName: 'Sắp hết hàng',
      title: 'VI. DANH SÁCH BIẾN THỂ SẮP HẾT HÀNG / CẦN NHẬP THÊM',
      subtitle: phuDeChung,
      columns: [
        C('STT', 'stt', 8, { align: 'center' }),
        C('Mã biến thể (SPCT)', 'ma', 22, { align: 'center' }),
        C('Tên sản phẩm', 'tenSanPham', 38),
        C('Thương hiệu', 'thuongHieu', 20),
        C('Phân loại (Màu / Size)', 'phanLoai', 26),
        C('Tồn kho hiện tại', 'tonKho', 18, { number: true }),
        C('Mức cảnh báo', 'mucDo', 22, { align: 'center' }),
        C('Khuyến nghị', 'khuyenNghi', 34),
      ],
      rows: sapHet.value.map((s, idx) => {
        const ton = Number(s.tonKho) || 0
        return {
          stt: idx + 1,
          ma: s.ma || '—',
          tenSanPham: s.tenSanPham,
          thuongHieu: s.thuongHieu || mapThuongHieuTheoSp.value.get(s.idSanPham) || '—',
          phanLoai: s.phanLoai || '—',
          tonKho: ton,
          mucDo: ton <= 0 ? 'Hết hàng (0)' : ton <= 2 ? 'Nguy kịch (1-2)' : 'Sắp hết hàng',
          khuyenNghi: ton <= 0 ? 'Nhập bổ sung ngay lập tức' : 'Lên kế hoạch nhập hàng trong tuần',
        }
      }),
      totalRow: {
        stt: '',
        ma: '',
        tenSanPham: `TỔNG CỘNG (${sapHet.value.length} biến thể cần nhập)`,
        thuongHieu: '',
        phanLoai: '',
        tonKho: tongTonSapHet,
        mucDo: `Hết hàng: ${gd3.value.soHetHang ?? 0}`,
        khuyenNghi: '',
      },
    },
    {
      sheetName: 'Nhân viên',
      title: 'VII. XẾP HẠNG HIỆU SUẤT BÁN HÀNG CỦA NHÂN VIÊN',
      subtitle: phuDeChung,
      columns: [
        C('Hạng', 'hang', 8, { align: 'center' }),
        C('Nhân viên phụ trách', 'ten', 32),
        C('Số đơn hoàn thành', 'so', 20, { number: true }),
        C('Doanh thu thực tế (₫)', 'v', 24, { money: true }),
        C('Giá trị TB / đơn (₫)', 'tb', 22, { money: true }),
        C('Tỷ trọng DT (%)', 'tyTrong', 18, { align: 'right' }),
      ],
      rows: topNhanVien.value.map((nv, idx) => ({
        hang: idx + 1,
        ten: nv.ten,
        so: Number(nv.so) || 0,
        v: Number(nv.v) || 0,
        tb: nv.so > 0 ? Math.round(nv.v / nv.so) : 0,
        tyTrong: pct(phanTram(nv.v, tongDtNv)),
      })),
      totalRow: {
        hang: '',
        ten: 'TỔNG CỘNG',
        so: tongDonNv,
        v: tongDtNv,
        tb: tongDonNv > 0 ? Math.round(tongDtNv / tongDonNv) : 0,
        tyTrong: tongDtNv > 0 ? '100%' : '0%',
      },
    },
    {
      sheetName: 'Khách hàng',
      title: 'VIII. TOP KHÁCH HÀNG CHI TIÊU NHIỀU NHẤT TRONG KỲ',
      subtitle: phuDeChung,
      columns: [
        C('Hạng', 'hang', 8, { align: 'center' }),
        C('Khách hàng', 'ten', 32),
        C('Số đơn hoàn thành', 'so', 20, { number: true }),
        C('Tổng chi tiêu (₫)', 'v', 24, { money: true }),
        C('Giá trị TB / đơn (₫)', 'tb', 22, { money: true }),
        C('Tỷ trọng (%)', 'tyTrong', 18, { align: 'right' }),
      ],
      rows: topKhach.value.map((kh, idx) => ({
        hang: idx + 1,
        ten: kh.ten,
        so: Number(kh.so) || 0,
        v: Number(kh.v) || 0,
        tb: kh.so > 0 ? Math.round(kh.v / kh.so) : 0,
        tyTrong: pct(phanTram(kh.v, tongDtKh)),
      })),
      totalRow: {
        hang: '',
        ten: 'TỔNG CỘNG',
        so: tongDonKh,
        v: tongDtKh,
        tb: tongDonKh > 0 ? Math.round(tongDtKh / tongDonKh) : 0,
        tyTrong: tongDtKh > 0 ? '100%' : '0%',
      },
    },
  ]

  const ten = kq.value.tu || kq.value.den ? `${kq.value.tu || 'dau'}_${kq.value.den || homNay.value}` : 'tat-ca'
  exportExcel({
    filename: `thong-ke_${ten}.xlsx`,
    report: {
      brand: 'HỆ THỐNG CỬA HÀNG GIÀY THỂ THAO FOOTSTYLE — BÁO CÁO QUẢN TRỊ',
      subtitle: phuDeChung,
    },
    sheets,
  })
  toast.success('Đã xuất file Excel thống kê đầy đủ.')
}
const mauTyLe = (v) => (v === null ? '' : v >= 90 ? 'tk-tot' : v >= 70 ? 'tk-vua' : 'tk-xau')
</script>

<template>
  <div class="tk-page">
    <!-- Bộ lọc thống kê -->
    <section class="ad-card tk-filter-card">
      <div class="tk-filter-head">
        <div>
          <h2 class="ad-card-title m-0">Tổng quan cửa hàng</h2>
          <p class="tk-status-line mb-0 mt-1" data-test="tk-mo-ta">
            Khoảng thời gian: <strong>{{ moTaKhoang }}</strong>
            <span v-if="thuongHieuChon"> · Thương hiệu: <strong>{{ thuongHieuChon }}</strong></span>
            <span v-if="tuKhoaSp"> · Từ khóa: <strong>"{{ tuKhoaSp }}"</strong></span>
          </p>
        </div>

        <div class="tk-filter-actions">
          <div class="tk-pill-bar" role="group" aria-label="Khoảng thời gian">
            <button
              v-for="m in NUT_MOC_NHANH"
              :key="m.ma"
              type="button"
              class="tk-pill-btn"
              :class="{ 'is-active': mocChon === m.ma }"
              :aria-pressed="mocChon === m.ma"
              @click="chonMoc(m)"
            >
              {{ m.nhan }}
            </button>
          </div>
          <div class="d-flex align-items-center gap-2">
            <button type="button" class="ad-btn" data-test="tk-xuat-excel" :disabled="loading" @click="xuatExcel">
              <i class="bi bi-file-earmark-excel" aria-hidden="true"></i> Xuất Excel
            </button>
            <RouterLink to="/thong-ke/so-sanh" class="ad-btn ad-btn-primary text-decoration-none" data-test="tk-so-sanh">
              <i class="bi bi-bar-chart-line" aria-hidden="true"></i> So sánh
            </RouterLink>
          </div>
        </div>
      </div>

      <!-- Hàng điều khiển lọc chi tiết -->
      <div class="tk-filter-grid" data-test="tk-tu-chon">
        <div class="tk-field">
          <label for="tk-tu" class="ad-label">Từ ngày</label>
          <DateInput
            id="tk-tu"
            :model-value="tuChon"
            class="form-control ad-control"
            :max="denChon || homNay"
            @update:model-value="capNhatTuNgay"
          />
        </div>

        <div class="tk-field">
          <label for="tk-den" class="ad-label">Đến ngày</label>
          <DateInput
            id="tk-den"
            :model-value="denChon"
            class="form-control ad-control"
            :min="tuChon"
            :max="homNay"
            @update:model-value="capNhatDenNgay"
          />
        </div>

        <div class="tk-field">
          <label for="tk-don-vi" class="ad-label">Thống kê</label>
          <select id="tk-don-vi" v-model="donViChon" class="form-select ad-control" data-test="tk-don-vi">
            <option value="">Theo {{ bieuDo.donVi }} (Tự động)</option>
            <option value="ngày">Theo ngày</option>
            <option value="tuần">Theo tuần</option>
            <option value="tháng">Theo tháng</option>
          </select>
        </div>

        <div class="tk-field">
          <label for="tk-thuong-hieu" class="ad-label">Thương hiệu</label>
          <select id="tk-thuong-hieu" v-model="thuongHieuChon" class="form-select ad-control" data-test="tk-loc-thuong-hieu">
            <option value="">Tất cả thương hiệu</option>
            <option v-for="th in dsThuongHieu" :key="th" :value="th">{{ th }}</option>
          </select>
        </div>

        <div class="tk-field tk-field-search">
          <label for="tk-tim-sp" class="ad-label">Tìm sản phẩm</label>
          <div class="d-flex align-items-center gap-2">
            <div class="ad-affix flex-grow-1">
              <i class="bi bi-search" aria-hidden="true"></i>
              <input
                id="tk-tim-sp"
                v-model="tuKhoaSp"
                type="search"
                class="form-control ad-control"
                placeholder="Mã hoặc tên sản phẩm..."
                data-test="tk-tim-sp"
              />
            </div>
            <button type="button" class="ad-btn" data-test="tk-dat-lai" @click="datLaiBoLoc">
              Đặt lại
            </button>
          </div>
        </div>
      </div>

      <div
        v-if="canhBaoNgay"
        class="tk-date-alert mt-2"
        :class="canhBaoNgay.loai === 'loi' ? 'is-danger' : 'is-warn'"
        role="status"
        data-test="tk-canh-bao-ngay"
      >
        <span>{{ canhBaoNgay.text }}</span>
        <button
          v-if="canhBaoNgay.loai === 'daoNguoc' || canhBaoNgay.loai === 'tuongLai' || canhBaoNgay.loai === 'quaXa'"
          type="button"
          class="btn btn-sm btn-warning ms-auto"
          data-test="tk-chuan-hoa-ngay"
          @click="chuanHoaKhoangNgay"
        >
          Chuẩn hóa ngày
        </button>
      </div>
    </section>

    <!-- 4 chỉ số tổng quan -->
    <div class="row g-3">
      <div v-for="k in kpi" :key="k.label" class="col-12 col-sm-6 col-xl-3">
        <section class="ad-card tk-kpi-card h-100 mb-0" data-test="tk-kpi">
          <div class="tk-kpi-label">{{ k.label }}</div>
          <div class="tk-kpi-value">{{ loading ? '…' : k.value }}</div>
          <div class="tk-kpi-note">{{ k.note }}</div>
          <div v-if="!loading && k.so" class="tk-so small mt-2" :class="k.so.huong && `tk-${k.so.huong}`">
            <span>{{ k.so.text }}</span>
            <span v-if="k.so.huong" class="text-muted fw-normal ms-1">so với kỳ trước</span>
          </div>
        </section>
      </div>
    </div>

    <div class="row g-3">
      <!-- Biểu đồ doanh thu -->
      <div class="col-12 col-xl-8">
        <section class="ad-card tk-section-card h-100 mb-0">
          <div class="tk-sec-head">
            <h3 class="tk-sec-title m-0">Doanh thu theo {{ bieuDo.donVi }}</h3>
            <button
              v-if="coCotTrong"
              type="button"
              class="ad-btn ad-btn-sm"
              data-test="tk-che-do-cot"
              @click="chiHienCoDt = !chiHienCoDt"
            >
              {{ chiHienCoDt ? `Tất cả ${bieuDo.donVi} (${tatCaCot.length})` : `Chỉ ${bieuDo.donVi} có DT (${cotCoDoanhThu.length})` }}
            </button>
          </div>

          <div v-if="!coDoanhThu && !loading" class="ad-empty my-3">Chưa có đơn hoàn thành trong khoảng này.</div>
          <svg v-else :viewBox="`0 0 ${W} ${H}`" class="w-100 tk-chart-svg" role="img" :aria-label="`Biểu đồ cột doanh thu theo ${bieuDo.donVi}`">
            <g v-for="t in ticks" :key="t.y">
              <line
                :x1="PAD.l"
                :x2="W - PAD.r"
                :y1="t.y"
                :y2="t.y"
                :stroke="t.t === 0 ? '#cbd5e1' : '#e2e8f0'"
                :stroke-width="t.t === 0 ? 1.5 : 1"
                :stroke-dasharray="t.t === 0 ? undefined : '4 4'"
              />
              <text :x="PAD.l - 10" :y="t.y + 4" text-anchor="end" font-size="11" fill="#64748b">{{ t.nhan }}</text>
            </g>
            <g v-for="(d, i) in cot" :key="d.k" class="tk-bar-group">
              <rect
                class="tk-bar-rect"
                :x="PAD.l + i * rongCot + (rongCot - rongThanh) / 2"
                :y="H - PAD.b - cao(d.v)"
                :width="rongThanh"
                :height="cao(d.v)"
                rx="5"
                fill="#0977ec"
              >
                <title>{{ d.nhan }}: {{ formatMoney(d.v) }} ({{ d.so }} đơn)</title>
              </rect>
              <text
                v-if="nhanGiaTri[i]"
                :x="PAD.l + i * rongCot + rongCot / 2"
                :y="nhanGiaTri[i].y"
                text-anchor="middle"
                :font-size="cot.length <= 8 ? 11 : 10"
                font-weight="600"
                fill="#075fc0"
              >{{ nhanGiaTri[i].text }}</text>
              <text
                v-if="nhanTruc[i]"
                :x="PAD.l + i * rongCot + rongCot / 2"
                :y="H - (cot.length <= 14 ? 22 : 12)"
                text-anchor="middle"
                :font-size="cot.length > 20 ? 10 : 11"
                :fill="d.v > 0 ? '#0f172a' : '#64748b'"
                :font-weight="d.v > 0 ? 600 : 400"
              >{{ nhanTruc[i] }}</text>
              <text
                v-if="nhanTruc[i] && cot.length <= 14 && d.so > 0"
                :x="PAD.l + i * rongCot + rongCot / 2"
                :y="H - 7"
                text-anchor="middle"
                font-size="10"
                fill="#64748b"
              >{{ d.so }} đơn</text>
            </g>
          </svg>
        </section>
      </div>

      <!-- Cơ cấu trạng thái đơn hàng -->
      <div class="col-12 col-xl-4">
        <section class="ad-card tk-section-card h-100 mb-0">
          <div class="tk-sec-head">
            <h3 class="tk-sec-title m-0">Cơ cấu trạng thái đơn hàng</h3>
          </div>

          <div class="d-flex justify-content-center my-3">
            <div class="ad-donut" :style="{ background: donut }" role="img" aria-label="Biểu đồ vòng trạng thái đơn">
              <span>{{ trongKhoang.length }}<small>tổng đơn</small></span>
            </div>
          </div>
          <ul class="list-unstyled m-0 small tk-status-list">
            <li v-for="t in cauTrucTT" :key="t.label" class="tk-status-item">
              <span class="ad-dot" :style="{ background: t.color }"></span>
              <span class="tk-status-name">{{ t.label }}</span>
              <div class="tk-mini-bar">
                <span :style="{ width: t.pct + '%', background: t.color }"></span>
              </div>
              <span class="fw-semibold text-dark">{{ t.so }}</span>
              <span class="tk-pct">{{ pct(t.pct) }}</span>
            </li>
          </ul>
        </section>
      </div>

      <!-- Thông báo lỗi / giới hạn 1 năm -->
      <div v-if="loiGd3" class="col-12"><div class="alert alert-warning small mb-0" role="alert" data-test="tk-loi-gd3">{{ loiGd3 }}</div></div>
      <div v-if="khoangGd3.catBot" class="col-12"><p class="text-muted small mb-0">Các mục bên dưới tính trong 1 năm gần nhất ({{ formatDate(khoangGd3.tuNgay) }} – {{ formatDate(khoangGd3.denNgay) }}).</p></div>

      <!-- Bảng Sản phẩm bán chạy -->
      <div class="col-12 col-xl-7">
        <section class="ad-card tk-section-card h-100 mb-0" data-test="tk-ban-chay">
          <div class="tk-sec-head">
            <h3 class="tk-sec-title m-0">Sản phẩm bán chạy</h3>
            <span v-if="banChay.length" class="text-muted small">{{ banChay.length }} sản phẩm</span>
          </div>

          <div v-if="dangTaiGd3" class="ad-empty">Đang tải…</div>
          <div v-else-if="!banChay.length" class="ad-empty">
            {{ thuongHieuChon || tuKhoaSp ? 'Không có sản phẩm bán chạy nào khớp bộ lọc.' : 'Chưa bán được sản phẩm nào trong khoảng này.' }}
          </div>
          <div v-else class="ad-table-wrap">
            <div class="ad-table-scroll">
              <table class="ad-table tk-table">
                <thead>
                  <tr>
                    <th style="width: 48px">#</th>
                    <th>Sản phẩm</th>
                    <th>Phân loại bán chạy</th>
                    <th class="text-center" style="width: 100px">Số lượng</th>
                    <th class="text-end" style="width: 140px">Doanh thu</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(p, i) in banChay" :key="p.idSanPham">
                    <td>{{ i + 1 }}</td>
                    <td>
                      <div class="fw-semibold text-dark">{{ p.tenSanPham }}</div>
                      <small class="text-muted d-block">
                        {{ p.maSanPham }}<template v-if="p.thuongHieu"> · {{ p.thuongHieu }}</template> · {{ p.soDon }} đơn
                      </small>
                    </td>
                    <td>
                      <span v-if="p.phanLoaiBanChay" class="small text-secondary">{{ p.phanLoaiBanChay }}</span>
                      <span v-else class="text-muted small">—</span>
                    </td>
                    <td class="text-center fw-semibold">{{ p.soLuong }}</td>
                    <td class="text-end ad-nowrap fw-semibold text-dark">{{ formatMoney(p.doanhThu) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </section>
      </div>

      <!-- Bảng Sắp hết hàng -->
      <div class="col-12 col-xl-5">
        <section class="ad-card tk-section-card h-100 mb-0" data-test="tk-sap-het">
          <div class="tk-sec-head">
            <h3 class="tk-sec-title m-0">Sắp hết hàng</h3>
            <span v-if="!dangTaiGd3" class="text-muted small">
              {{ thuongHieuChon || tuKhoaSp ? sapHet.length : (gd3.soSapHet ?? 0) }} phân loại còn ≤ {{ gd3.nguongSapHet ?? 5 }}
              <template v-if="thuongHieuChon || tuKhoaSp ? sapHet.filter((x) => x.tonKho <= 0).length : gd3.soHetHang">
                · <b class="text-danger">{{ thuongHieuChon || tuKhoaSp ? sapHet.filter((x) => x.tonKho <= 0).length : gd3.soHetHang }} hết hàng</b>
              </template>
            </span>
          </div>

          <div v-if="dangTaiGd3" class="ad-empty">Đang tải…</div>
          <div v-else-if="!sapHet.length" class="ad-empty">
            {{ thuongHieuChon || tuKhoaSp ? 'Không có sản phẩm sắp hết nào khớp bộ lọc.' : 'Không có sản phẩm nào sắp hết.' }}
          </div>
          <ul v-else class="list-unstyled m-0 tk-sap-het">
            <li v-for="c in sapHet" :key="c.id">
              <span class="tk-ten">
                <RouterLink :to="`/san-pham/${c.idSanPham}/sua`" class="tk-sp-link">{{ c.tenSanPham }}</RouterLink>
                <small class="text-muted d-block">{{ c.ma }}<template v-if="c.phanLoai"> · {{ c.phanLoai }}</template></small>
              </span>
              <span class="ad-pill" :class="c.tonKho <= 0 ? 'ad-pill-red' : c.tonKho <= 2 ? 'ad-pill-orange' : 'ad-pill-amber'">
                {{ c.tonKho <= 0 ? 'Hết hàng' : `Còn ${c.tonKho}` }}
              </span>
            </li>
          </ul>
        </section>
      </div>

      <!-- Doanh thu theo loại đơn, Nhân viên bán tốt nhất & Khách hàng mua nhiều nhất -->
      <div class="col-12 col-lg-4">
        <section class="ad-card tk-section-card h-100 mb-0">
          <div class="tk-sec-head">
            <h3 class="tk-sec-title m-0">Doanh thu theo loại đơn</h3>
            <span v-if="tk.hoanThanh" class="text-muted small">{{ tk.hoanThanh }} đơn</span>
          </div>
          <div v-if="!theoLoai.length" class="ad-empty">Chưa có dữ liệu.</div>
          <div v-for="l in theoLoai" :key="l.ten" class="tk-loai-item">
            <div class="d-flex justify-content-between align-items-center small mb-1">
              <span class="fw-semibold text-dark">{{ l.ten }} <small class="text-muted fw-normal">({{ l.so }} đơn)</small></span>
              <span class="fw-semibold">{{ formatMoney(l.v) }} <small class="text-muted fw-normal">({{ pct(l.pct) }})</small></span>
            </div>
            <div class="progress tk-progress">
              <div class="progress-bar tk-progress-bar" :style="{ width: l.pct + '%' }"></div>
            </div>
          </div>
        </section>
      </div>

      <div class="col-12 col-lg-4">
        <section class="ad-card tk-section-card h-100 mb-0" data-test="tk-top-nv">
          <div class="tk-sec-head">
            <h3 class="tk-sec-title m-0">Nhân viên bán tốt nhất</h3>
          </div>
          <div v-if="!topNhanVien.length" class="ad-empty">Chưa có dữ liệu.</div>
          <ol v-else class="list-unstyled m-0 tk-rank">
            <li v-for="(r, i) in topNhanVien" :key="r.ten">
              <span class="tk-hang">{{ i + 1 }}</span>
              <span class="tk-ten">
                <span class="fw-semibold text-dark">{{ r.ten }}</span>
                <small class="text-muted d-block">{{ r.so }} đơn hoàn thành</small>
              </span>
              <b class="tk-rank-money">{{ formatMoney(r.v) }}</b>
            </li>
          </ol>
        </section>
      </div>

      <div class="col-12 col-lg-4">
        <section class="ad-card tk-section-card h-100 mb-0" data-test="tk-top-kh">
          <div class="tk-sec-head">
            <h3 class="tk-sec-title m-0">Khách hàng mua nhiều nhất</h3>
          </div>
          <div v-if="!topKhach.length" class="ad-empty">Chưa có dữ liệu.</div>
          <ol v-else class="list-unstyled m-0 tk-rank">
            <li v-for="(r, i) in topKhach" :key="r.ten">
              <span class="tk-hang">{{ i + 1 }}</span>
              <span class="tk-ten">
                <span class="fw-semibold text-dark">{{ r.ten }}</span>
                <small class="text-muted d-block">{{ r.so }} đơn mua</small>
              </span>
              <b class="tk-rank-money">{{ formatMoney(r.v) }}</b>
            </li>
          </ol>
        </section>
      </div>
    </div>

    <!-- Ẩn phương thức thanh toán (Tiền mặt / Chuyển khoản) và ca làm việc / lịch làm việc khỏi giao diện chính -->
    <div class="d-none" aria-hidden="true">
      <section data-test="tk-phuong-thuc">
        <div v-if="!theoPt.length">{{ dangTaiGd3 ? 'Đang tải…' : 'Chưa có dữ liệu.' }}</div>
        <div v-for="p in theoPt" :key="p.ten">
          <span>{{ p.ten }} ({{ p.soDon }} đơn)</span>
          <span>{{ formatMoney(p.soTien) }} ({{ pctGon(p.pct) }})</span>
        </div>
      </section>

      <div class="col-12 col-md-6 col-lg-4">
        <section class="ad-card tk-section-card h-100 mb-0" data-test="tk-theo-ca">
          <div class="tk-sec-head">
            <div class="d-flex align-items-center gap-2">
              <span class="tk-sec-ico is-sky"><i class="bi bi-calendar2-range-fill" aria-hidden="true"></i></span>
              <div>
                <h3 class="tk-sec-title m-0">Doanh thu theo ca làm việc</h3>
                <p class="tk-sec-sub m-0">Phân bổ doanh thu thực tế theo từng ca trong ngày</p>
              </div>
            </div>
          </div>
          <div v-if="!theoCa.length" class="ad-empty">{{ dangTaiGd3 ? 'Đang tải…' : 'Chưa có dữ liệu.' }}</div>
          <div v-for="c in theoCa" :key="c.tenCa" class="mb-3">
            <div class="d-flex justify-content-between small mb-1">
              <span class="fw-medium">{{ c.tenCa }}<small v-if="c.gio" class="text-muted"> ({{ c.gio }})</small></span><span>{{ formatMoney(c.doanhThu) }}</span>
            </div>
            <div class="progress tk-progress"><div class="progress-bar bg-info" :style="{ width: c.pct + '%' }"></div></div>
            <small class="text-muted">{{ c.soDon }} đơn</small>
          </div>
        </section>
      </div>

      <div class="col-12 col-md-6 col-lg-4">
        <section class="ad-card tk-section-card h-100 mb-0" data-test="tk-theo-gio">
          <div class="tk-sec-head">
            <div class="d-flex align-items-center gap-2">
              <span class="tk-sec-ico is-amber"><i class="bi bi-alarm-fill" aria-hidden="true"></i></span>
              <div>
                <h3 class="tk-sec-title m-0">Khung giờ đông khách</h3>
                <p class="tk-sec-sub m-0">{{ gioDongNhat && gioDongNhat.soDon > 0 ? `Đông nhất ${gioDongNhat.gio}h - ${gioDongNhat.gio + 1}h (${gioDongNhat.soDon} đơn)` : 'Số đơn hoàn thành theo giờ tạo đơn' }}</p>
              </div>
            </div>
          </div>
          <div class="tk-gio" role="img" aria-label="Biểu đồ số đơn theo giờ">
            <div v-for="g in theoGio" :key="g.gio" class="tk-gio-cot" :title="`${g.gio}h: ${g.soDon} đơn · ${formatMoney(g.doanhThu)}`">
              <span class="tk-gio-thanh" :style="{ height: Math.max(g.pct, g.soDon ? 4 : 0) + '%' }"></span>
              <small>{{ g.gio % 2 === 0 ? g.gio : '' }}</small>
            </div>
          </div>
        </section>
      </div>

      <div class="col-12 col-xl-8">
        <section class="ad-card tk-section-card h-100 mb-0" data-test="tk-chuyen-can">
          <div class="tk-sec-head">
            <div class="d-flex align-items-center gap-2">
              <span class="tk-sec-ico is-green"><i class="bi bi-person-check-fill" aria-hidden="true"></i></span>
              <div>
                <h3 class="tk-sec-title m-0">Chuyên cần theo lịch làm việc</h3>
                <p class="tk-sec-sub m-0">Theo dõi số ca làm, đi muộn và tỷ lệ đúng giờ</p>
              </div>
            </div>
            <RouterLink to="/lich-lam-viec" class="tk-ops-link"><i class="bi bi-calendar-week me-1" aria-hidden="true"></i>Xếp lịch</RouterLink>
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
        </section>
      </div>

      <div class="col-12 col-xl-4">
        <section class="ad-card tk-section-card h-100 mb-0" data-test="tk-giao-ca">
          <div class="tk-sec-head">
            <div class="d-flex align-items-center gap-2">
              <span class="tk-sec-ico is-indigo"><i class="bi bi-safe2-fill" aria-hidden="true"></i></span>
              <div>
                <h3 class="tk-sec-title m-0">Giao ca &amp; đối soát tiền mặt</h3>
                <p class="tk-sec-sub m-0">Kiểm soát tiền mặt bàn giao cuối ca</p>
              </div>
            </div>
            <RouterLink to="/giao-ca" class="tk-ops-link"><i class="bi bi-clock-history me-1" aria-hidden="true"></i>Lịch sử giao ca</RouterLink>
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
.tk-page {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.tk-filter-card {
  padding: 1.15rem 1.35rem;
}

.tk-filter-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding-bottom: 0.9rem;
  border-bottom: 1px solid #eef2f7;
}

.tk-filter-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem;
}

.tk-pill-bar {
  display: inline-flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.25rem;
  background: #f1f5f9;
  padding: 0.2rem;
  border-radius: 8px;
}

.tk-pill-btn {
  border: 0;
  background: transparent;
  color: #475569;
  font-size: 0.82rem;
  font-weight: 600;
  padding: 0.32rem 0.75rem;
  border-radius: 6px;
}

.tk-pill-btn:hover {
  color: #0f172a;
}

.tk-pill-btn.is-active {
  background: #ffffff;
  color: #0977ec;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.08);
}

.tk-filter-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(140px, 1fr)) minmax(240px, 1.4fr);
  gap: 0.85rem;
  align-items: end;
  padding-top: 0.95rem;
}

@media (max-width: 1199px) {
  .tk-filter-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .tk-field-search {
    grid-column: 1 / -1;
  }
}

@media (max-width: 575px) {
  .tk-filter-grid {
    grid-template-columns: 1fr;
  }
}

.tk-status-line {
  font-size: 0.83rem;
  color: #64748b;
}

.tk-kpi-card {
  padding: 1.1rem 1.25rem;
}

.tk-kpi-label {
  font-size: 0.82rem;
  font-weight: 600;
  color: #64748b;
  margin-bottom: 0.25rem;
}

.tk-kpi-value {
  font-size: 1.35rem;
  font-weight: 700;
  color: var(--fs-text, #18242f);
}

.tk-kpi-note {
  font-size: 0.78rem;
  color: #64748b;
  margin-top: 0.2rem;
}

.tk-so {
  font-weight: 600;
  color: #64748b;
}

.tk-tot { color: #15803d; }
.tk-xau { color: #be123c; }
.tk-vua { color: #d97706; }

.tk-section-card {
  padding: 1.15rem 1.35rem;
}

.tk-sec-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  margin-bottom: 0.9rem;
}

.tk-sec-title {
  font-size: 0.96rem;
  font-weight: 700;
  color: #0f172a;
}

.ad-donut {
  width: 156px;
  height: 156px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  position: relative;
}

.ad-donut::after {
  content: '';
  position: absolute;
  inset: 26px;
  background: #ffffff;
  border-radius: 50%;
}

.ad-donut span {
  position: relative;
  z-index: 1;
  font-size: 1.45rem;
  font-weight: 700;
  color: #0f172a;
  text-align: center;
  line-height: 1.1;
}

.ad-donut small {
  display: block;
  font-size: 0.74rem;
  font-weight: 500;
  color: #64748b;
}

.tk-status-list {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.tk-status-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.3rem 0;
}

.ad-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  flex: none;
}

.tk-status-name {
  color: #334155;
  min-width: 105px;
}

.tk-mini-bar {
  flex: 1 1 60px;
  height: 6px;
  background: #eef2f7;
  border-radius: 999px;
  overflow: hidden;
}

.tk-mini-bar span {
  display: block;
  height: 100%;
  border-radius: 999px;
}

.tk-pct {
  min-width: 3rem;
  text-align: right;
  color: #64748b;
}

.tk-rank li {
  display: flex;
  align-items: center;
  gap: 0.7rem;
  padding: 0.55rem 0;
  border-bottom: 1px solid #f1f5f9;
  font-size: 0.88rem;
}

.tk-rank li:last-child {
  border-bottom: 0;
}

.tk-hang {
  width: 1.65rem;
  height: 1.65rem;
  border-radius: 50%;
  background: #f1f5f9;
  color: #334155;
  font-weight: 600;
  font-size: 0.78rem;
  display: grid;
  place-items: center;
  flex: none;
}

.tk-ten {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tk-rank-money {
  color: #0f172a;
  font-weight: 600;
  font-size: 0.88rem;
}

.tk-sp-link {
  font-weight: 600;
  color: #0f172a;
  text-decoration: none;
}

.tk-sp-link:hover {
  color: #0977ec;
  text-decoration: underline;
}

.tk-loai-item {
  margin-bottom: 0.85rem;
}

.tk-loai-item:last-child {
  margin-bottom: 0;
}

.tk-progress {
  height: 8px;
  border-radius: 999px;
  background: #eef2f7;
  overflow: hidden;
}

.tk-progress-bar {
  background: #0977ec;
  border-radius: 999px;
}

.tk-sap-het {
  max-height: 380px;
  overflow-y: auto;
}

.tk-sap-het li {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.55rem 0;
  border-bottom: 1px solid #f1f5f9;
  font-size: 0.88rem;
}

.tk-sap-het li:last-child {
  border-bottom: 0;
}

.tk-gio { display: flex; align-items: flex-end; gap: 3px; height: 170px; padding-top: 6px; }
.tk-gio-cot { flex: 1 1 0; display: flex; flex-direction: column; align-items: center; justify-content: flex-end; height: 100%; min-width: 0; }
.tk-gio-thanh { width: 100%; max-width: 22px; background: #0977ec; border-radius: 4px 4px 0 0; }
.tk-gio-cot small { font-size: 0.7rem; color: #7a8696; height: 1rem; margin-top: 4px; }
.tk-gc-so { display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem; }
.tk-gc-so div { background: #f7f9fc; border-radius: 10px; padding: 0.6rem 0.75rem; display: flex; flex-direction: column; }
.tk-gc-so span { font-size: 0.78rem; color: #7a8696; }
.tk-gc-so b { font-size: 1.05rem; }

.tk-date-alert {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  border-radius: 8px;
  font-size: 0.83rem;
}

.tk-date-alert.is-warn {
  background: #fffbeb;
  border: 1px solid #fde68a;
  color: #92400e;
}

.tk-date-alert.is-danger {
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #b91c1c;
}
</style>
