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
const chiTietHoaDon = ref([])
const tonKhoCanhBao = ref([])
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
    const [res, resTk] = await Promise.all([
      api.get('/hoa-don', { params: { page: 0, size: 2000 } }),
      api.get('/thong-ke/tong-quan').catch(() => null),
    ])
    hoaDon.value = (res?.data?.content ?? []).filter((h) => !laHoaDonCho(h))
    const tkData = resTk?.data
    if (tkData && typeof tkData === 'object') {
      chiTietHoaDon.value = Array.isArray(tkData.chiTietHoaDon) ? tkData.chiTietHoaDon : []
      tonKhoCanhBao.value = Array.isArray(tkData.tonKhoCanhBao) ? tkData.tonKhoCanhBao : []
    }
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

// Khi lọc Thương hiệu hoặc Tìm sản phẩm và có chiTietHoaDon, lọc luôn các hóa đơn có chứa sản phẩm khớp bộ lọc (và tính đúng doanh thu theo dòng SP khớp)
const mapTienHoaDonKhopSp = computed(() => {
  const th = thuongHieuChon.value.trim()
  const kw = boDau(tuKhoaSp.value)
  if ((!th && !kw) || !chiTietHoaDon.value.length) return null
  const m = new Map()
  for (const c of chiTietHoaDon.value) {
    const khopTh = !th || String(c.tenThuongHieu || '').trim() === th
    const khopKw = !kw || boDau(`${c.maSanPham || ''} ${c.maBienThe || ''} ${c.tenSanPham || ''} ${c.tenBienThe || ''}`).includes(kw)
    if (khopTh && khopKw && c.idHoaDon != null) {
      const sl = Number(c.soLuong) || 0
      const dg = Number(c.donGia) || 0
      const tt = Number(c.thanhTien ?? sl * dg) || 0
      m.set(c.idHoaDon, (m.get(c.idHoaDon) || 0) + tt)
    }
  }
  return m
})

const hoaDonDaLoc = computed(() => {
  const m = mapTienHoaDonKhopSp.value
  if (!m) return hoaDon.value
  return hoaDon.value
    .filter((h) => m.has(h.id))
    .map((h) => {
      const tienSp = m.get(h.id) || 0
      return tienSp > 0 ? { ...h, doanhThu: tienSp, tongTien: tienSp, phiVanChuyen: 0 } : h
    })
})

const trongKhoang = computed(() => locTheoKhoang(hoaDonDaLoc.value, kq.value.tu, kq.value.den))
const tk = computed(() => tongHop(trongKhoang.value))
const ky = computed(() => kyTruoc(kq.value.tu, kq.value.den))
const tkTruoc = computed(() => (ky.value ? tongHop(locTheoKhoang(hoaDonDaLoc.value, ky.value.tu, ky.value.den)) : null))
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
const bieuDo = computed(() => gomCot(hoaDonDaLoc.value, kq.value.tu, kq.value.den, homNay.value, donViChon.value))
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
  for (const c of chiTietHoaDon.value) if (c.tenThuongHieu && c.tenThuongHieu !== 'Khác') tap.add(String(c.tenThuongHieu).trim())
  for (const b of tonKhoCanhBao.value) if (b.tenThuongHieu) tap.add(String(b.tenThuongHieu).trim())
  return [...tap].filter(Boolean).sort((a, b) => a.localeCompare(b, 'vi'))
})

// Map tên sản phẩm -> thương hiệu để lọc bảng Sắp hết hàng theo thương hiệu
const mapThuongHieuTheoSp = computed(() => {
  const m = new Map()
  for (const p of banChayGoc.value) if (p.idSanPham && p.thuongHieu) m.set(p.idSanPham, p.thuongHieu)
  for (const c of chiTietHoaDon.value) if (c.idSanPham && c.tenThuongHieu) m.set(c.idSanPham, c.tenThuongHieu)
  for (const b of tonKhoCanhBao.value) if (b.idSanPham && b.tenThuongHieu) m.set(b.idSanPham, b.tenThuongHieu)
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
  if (mapTienHoaDonKhopSp.value) {
    const m = new Map()
    for (const h of trongKhoang.value) {
      if (h.trangThai !== TT_HOAN_THANH) continue
      const ten = h.tenPhuongThucThanhToan || h.phuongThucThanhToan || (h.loaiDon === 'Tại quầy' ? 'Tiền mặt' : 'Chuyển khoản ngân hàng')
      const r = m.get(ten) ?? { ten, soTien: 0, soDon: 0 }
      r.soTien += Number(h.doanhThu ?? h.tongTien) || 0
      r.soDon += 1
      m.set(ten, r)
    }
    const dsLoc = [...m.values()].sort((a, b) => b.soTien - a.soTien || b.soDon - a.soDon)
    const tongLoc = dsLoc.reduce((s, x) => s + x.soTien, 0)
    return dsLoc.map((x) => ({ ...x, pct: phanTram(x.soTien, tongLoc) }))
  }
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

/* ----- Xuất Excel ----- */
function xuatExcel() {
  const a = tk.value
  const T = (header, key, width = 18, money = false) => ({ header, key, width, money })
  const sheets = [
    {
      sheetName: 'Tổng quan',
      columns: [T('Chỉ số', 'ten', 34), T('Giá trị', 'gt', 20, true), T('Ghi chú', 'ghiChu', 50)],
      rows: [
        { ten: 'Khoảng thời gian', gt: moTaKhoang.value },
        { ten: 'Thương hiệu lọc', gt: thuongHieuChon.value || 'Tất cả thương hiệu' },
        { ten: 'Từ khóa sản phẩm', gt: tuKhoaSp.value || '—' },
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
    { sheetName: 'Theo loại đơn', columns: [T('Loại đơn', 'ten', 20), T('Số đơn', 'so'), T('Doanh thu', 'v', 20, true), T('Tỷ trọng %', 'pct')], rows: theoLoai.value.map((l) => ({ ...l, pct: Math.round(l.pct * 10) / 10 })) },
    { sheetName: 'Nhân viên', columns: [T('Nhân viên', 'ten', 28), T('Số đơn', 'so'), T('Doanh thu', 'v', 20, true)], rows: topNhanVien.value },
    { sheetName: 'Khách hàng', columns: [T('Khách hàng', 'ten', 28), T('Số đơn', 'so'), T('Doanh thu', 'v', 20, true)], rows: topKhach.value },
    {
      sheetName: 'Sản phẩm bán chạy',
      columns: [T('Mã', 'maSanPham', 12), T('Sản phẩm', 'tenSanPham', 36), T('Bán chạy nhất', 'phanLoaiBanChay', 22), T('Số lượng', 'soLuong'), T('Số đơn', 'soDon'), T('Doanh thu', 'doanhThu', 20, true)],
      rows: banChay.value,
    },
    { sheetName: 'Sắp hết hàng', columns: [T('Mã', 'ma', 20), T('Sản phẩm', 'tenSanPham', 36), T('Phân loại', 'phanLoai', 22), T('Tồn kho', 'tonKho')], rows: sapHet.value },
    { sheetName: 'Phương thức TT', columns: [T('Phương thức', 'ten', 22), T('Số đơn', 'soDon'), T('Số tiền thu', 'soTien', 20, true)], rows: theoPt.value },
  ]
  const ten = kq.value.tu || kq.value.den ? `${kq.value.tu || 'dau'}_${kq.value.den || homNay.value}` : 'tat-ca'
  exportExcel({ filename: `thong-ke_${ten}.xlsx`, sheets })
  toast.success('Đã xuất file Excel thống kê.')
}
const mauTyLe = (v) => (v === null ? '' : v >= 90 ? 'tk-tot' : v >= 70 ? 'tk-vua' : 'tk-xau')
</script>

<template>
  <div class="tk-page">
    <!-- Khung Bộ lọc thống kê đồng bộ kiểu dáng và phông chữ với các bộ lọc khác -->
    <section class="ad-card tk-filter-card">
      <div class="tk-filter-head">
        <div class="ad-card-head mb-0">
          <span class="ad-icon-box" aria-hidden="true">
            <i class="bi bi-funnel"></i>
          </span>
          <div>
            <h2 class="ad-card-title">Bộ lọc thống kê</h2>
            <p class="ad-card-sub">Tra cứu nhanh doanh thu, đơn hàng và sản phẩm theo thời gian thực</p>
            <span class="visually-hidden">Tổng quan cửa hàng</span>
          </div>
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
            <button type="button" class="ad-btn tk-btn-excel" data-test="tk-xuat-excel" :disabled="loading" @click="xuatExcel">
              <i class="bi bi-file-earmark-excel-fill" aria-hidden="true"></i> Xuất Excel
            </button>
            <RouterLink to="/thong-ke/so-sanh" class="ad-btn ad-btn-primary text-decoration-none" data-test="tk-so-sanh">
              <i class="bi bi-bar-chart-line-fill" aria-hidden="true"></i> So sánh
            </RouterLink>
          </div>
        </div>
      </div>

      <!-- Hàng điều khiển lọc chi tiết: Từ ngày · Đến ngày · Thống kê · Thương hiệu · Tìm sản phẩm · Đặt lại -->
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
              <i class="bi bi-arrow-counterclockwise" aria-hidden="true"></i> Đặt lại
            </button>
          </div>
        </div>
      </div>

      <!-- Dòng trạng thái dữ liệu đang hiển thị -->
      <div class="tk-status-line" data-test="tk-mo-ta">
        <span class="tk-status-dot" aria-hidden="true"></span>
        <div class="d-flex flex-wrap align-items-center gap-2">
          <span>
            Đang hiển thị dữ liệu <strong>{{ moTaKhoang }}</strong>
          </span>
          <span v-if="donViChon" class="tk-active-chip">
            <i class="bi bi-bar-chart me-1" aria-hidden="true"></i>Biểu đồ: Theo {{ bieuDo.donVi }}
          </span>
          <span v-if="thuongHieuChon" class="tk-active-chip">
            <i class="bi bi-tag-fill me-1" aria-hidden="true"></i>Thương hiệu: <strong>{{ thuongHieuChon }}</strong>
          </span>
          <span v-if="tuKhoaSp" class="tk-active-chip">
            <i class="bi bi-search me-1" aria-hidden="true"></i>Sản phẩm: <strong>"{{ tuKhoaSp }}"</strong>
          </span>
        </div>
      </div>

      <div
        v-if="canhBaoNgay"
        class="tk-date-alert mt-2"
        :class="canhBaoNgay.loai === 'loi' ? 'is-danger' : 'is-warn'"
        role="status"
        data-test="tk-canh-bao-ngay"
      >
        <i class="bi bi-exclamation-circle-fill me-2" aria-hidden="true"></i>
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

    <!-- 4 chỉ số tổng quan (theo khoảng ngày & bộ lọc) -->
    <div class="row g-3">
      <div v-for="k in kpi" :key="k.label" class="col-12 col-sm-6 col-xl-3">
        <section class="ad-card tk-kpi-card h-100 mb-0" :class="`tk-kpi--${k.tone}`" data-test="tk-kpi">
          <div class="d-flex align-items-start gap-3">
            <span class="tk-kpi-icon" :class="`is-${k.tone}`">
              <i class="bi" :class="k.icon" aria-hidden="true"></i>
            </span>
            <div class="min-w-0 flex-grow-1">
              <div class="d-flex align-items-center justify-content-between gap-1">
                <div class="tk-kpi-label">{{ k.label }}</div>
              </div>
              <div class="tk-kpi-value">{{ loading ? '…' : k.value }}</div>
              <div class="tk-kpi-note">{{ k.note }}</div>
              <div v-if="!loading && k.so" class="tk-so small mt-2" :class="k.so.huong && `tk-${k.so.huong}`">
                <span class="tk-so-badge" :class="k.so.huong && `is-${k.so.huong}`">{{ k.so.text }}</span>
                <span v-if="k.so.huong" class="text-muted fw-normal ms-1">so với kỳ trước</span>
              </div>
            </div>
          </div>
        </section>
      </div>
    </div>

    <div class="row g-3">
      <!-- Biểu đồ doanh thu (đổi theo ô THỐNG KÊ: Theo ngày / Theo tuần / Theo tháng) -->
      <div class="col-12 col-xl-8">
        <section class="ad-card tk-section-card h-100 mb-0">
          <div class="tk-sec-head">
            <div class="d-flex align-items-center gap-2">
              <span class="tk-sec-ico is-blue"><i class="bi bi-bar-chart-fill" aria-hidden="true"></i></span>
              <div>
                <h3 class="tk-sec-title m-0">Doanh thu theo {{ bieuDo.donVi }}</h3>
                <p class="tk-sec-sub m-0">Biểu đồ doanh thu thực tế từ các đơn hàng đã hoàn thành</p>
              </div>
            </div>
            <div class="tk-chart-summary">
              <span class="tk-chart-pill">
                Tổng: <strong>{{ formatMoney(tk.doanhThu) }}</strong>
              </span>
              <span class="tk-chart-pill is-soft">
                {{ tk.hoanThanh }} đơn hoàn thành
              </span>
              <span v-if="dinhBieuDo && dinhBieuDo.v > 0" class="tk-chart-pill is-amber">
                Cao nhất ({{ dinhBieuDo.nhan }}): <strong>{{ formatMoney(dinhBieuDo.v) }}</strong>
              </span>
              <button
                v-if="coCotTrong"
                type="button"
                class="tk-chart-pill tk-chart-toggle-btn"
                data-test="tk-che-do-cot"
                @click="chiHienCoDt = !chiHienCoDt"
              >
                <i class="bi me-1" :class="chiHienCoDt ? 'bi-arrows-expand' : 'bi-funnel-fill'" aria-hidden="true"></i>
                {{ chiHienCoDt ? `Tất cả ${bieuDo.donVi} (${tatCaCot.length})` : `Chỉ ${bieuDo.donVi} có DT (${cotCoDoanhThu.length})` }}
              </button>
            </div>
          </div>

          <div v-if="!coDoanhThu && !loading" class="ad-empty my-3">Chưa có đơn hoàn thành trong khoảng này.</div>
          <svg v-else :viewBox="`0 0 ${W} ${H}`" class="w-100 tk-chart-svg" role="img" :aria-label="`Biểu đồ cột doanh thu theo ${bieuDo.donVi}`">
            <defs>
              <linearGradient id="tkBarGrad" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="#3b82f6" />
                <stop offset="100%" stop-color="#0977ec" />
              </linearGradient>
              <linearGradient id="tkBarPeakGrad" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="#0284c7" />
                <stop offset="100%" stop-color="#075fc0" />
              </linearGradient>
            </defs>
            <path
              :d="`M ${PAD.l} ${PAD.t} H ${W - PAD.r} V ${H - PAD.b} H ${PAD.l} Z`"
              fill="#f8fafc"
              opacity="0.7"
            />
            <g v-for="t in ticks" :key="t.y">
              <line
                :x1="PAD.l"
                :x2="W - PAD.r"
                :y1="t.y"
                :y2="t.y"
                :stroke="t.t === 0 ? '#94a3b8' : '#e2e8f0'"
                :stroke-width="t.t === 0 ? 1.5 : 1"
                :stroke-dasharray="t.t === 0 ? undefined : '4 4'"
              />
              <text :x="PAD.l - 10" :y="t.y + 4" text-anchor="end" font-size="11" fill="#475569" font-weight="600">{{ t.nhan }}</text>
            </g>
            <g v-for="(d, i) in cot" :key="d.k" class="tk-bar-group">
              <line
                v-if="d.v > 0 && cot.length <= 16"
                :x1="PAD.l + i * rongCot + rongCot / 2"
                :x2="PAD.l + i * rongCot + rongCot / 2"
                :y1="PAD.t"
                :y2="H - PAD.b"
                stroke="#e2e8f0"
                stroke-width="1"
                stroke-dasharray="2 3"
                opacity="0.65"
              />
              <rect
                class="tk-bar-rect"
                :x="PAD.l + i * rongCot + (rongCot - rongThanh) / 2"
                :y="H - PAD.b - cao(d.v)"
                :width="rongThanh"
                :height="cao(d.v)"
                rx="6"
                :fill="dinhBieuDo && d.k === dinhBieuDo.k && cot.length > 1 ? 'url(#tkBarPeakGrad)' : 'url(#tkBarGrad)'"
              >
                <title>{{ d.nhan }}: {{ formatMoney(d.v) }} ({{ d.so }} đơn)</title>
              </rect>
              <text
                v-if="nhanGiaTri[i]"
                :x="PAD.l + i * rongCot + rongCot / 2"
                :y="nhanGiaTri[i].y"
                text-anchor="middle"
                :font-size="cot.length <= 8 ? 11 : 10"
                font-weight="700"
                :fill="dinhBieuDo && d.k === dinhBieuDo.k && cot.length > 1 ? '#0369a1' : '#075fc0'"
              >{{ nhanGiaTri[i].text }}</text>
              <line
                :x1="PAD.l + i * rongCot + rongCot / 2"
                :x2="PAD.l + i * rongCot + rongCot / 2"
                :y1="H - PAD.b"
                :y2="H - PAD.b + (d.v > 0 ? 5 : nhanTruc[i] ? 4 : 2.5)"
                :stroke="d.v > 0 ? '#0977ec' : nhanTruc[i] ? '#94a3b8' : '#e2e8f0'"
                :stroke-width="d.v > 0 ? 1.5 : 1"
              />
              <text
                v-if="nhanTruc[i]"
                :x="PAD.l + i * rongCot + rongCot / 2"
                :y="H - (cot.length <= 14 ? 22 : 12)"
                text-anchor="middle"
                :font-size="cot.length > 20 ? 10 : 11.5"
                :fill="d.v > 0 ? '#0f172a' : '#64748b'"
                :font-weight="d.v > 0 ? 700 : 500"
              >{{ nhanTruc[i] }}</text>
              <text
                v-if="nhanTruc[i] && cot.length <= 14 && d.so > 0"
                :x="PAD.l + i * rongCot + rongCot / 2"
                :y="H - 7"
                text-anchor="middle"
                font-size="10"
                fill="#64748b"
                font-weight="500"
              >{{ d.so }} đơn</text>
            </g>
          </svg>
        </section>
      </div>

      <!-- Cơ cấu trạng thái đơn hàng -->
      <div class="col-12 col-xl-4">
        <section class="ad-card tk-section-card h-100 mb-0">
          <div class="tk-sec-head">
            <div class="d-flex align-items-center gap-2">
              <span class="tk-sec-ico is-indigo"><i class="bi bi-pie-chart-fill" aria-hidden="true"></i></span>
              <div>
                <h3 class="tk-sec-title m-0">Cơ cấu trạng thái đơn hàng</h3>
                <p class="tk-sec-sub m-0">Tỷ trọng theo từng trạng thái xử lý</p>
              </div>
            </div>
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
              <span class="fw-bold text-dark">{{ t.so }}</span>
              <span class="tk-pct">{{ pct(t.pct) }}</span>
            </li>
          </ul>
        </section>
      </div>

      <!-- Thông báo lỗi / giới hạn 1 năm -->
      <div v-if="loiGd3" class="col-12"><div class="alert alert-warning small mb-0" role="alert" data-test="tk-loi-gd3">{{ loiGd3 }}</div></div>
      <div v-if="khoangGd3.catBot" class="col-12"><p class="text-muted small mb-0">Các mục bên dưới tính trong 1 năm gần nhất ({{ formatDate(khoangGd3.tuNgay) }} – {{ formatDate(khoangGd3.denNgay) }}).</p></div>

      <!-- Bảng Sản phẩm bán chạy (lọc trực tiếp theo THƯƠNG HIỆU và TÌM SẢN PHẨM trên bộ lọc) -->
      <div class="col-12 col-xl-7">
        <section class="ad-card tk-section-card h-100 mb-0" data-test="tk-ban-chay">
          <div class="tk-sec-head">
            <div class="d-flex align-items-center gap-2">
              <span class="tk-sec-ico is-amber"><i class="bi bi-trophy-fill" aria-hidden="true"></i></span>
              <div>
                <h3 class="tk-sec-title m-0">Sản phẩm bán chạy</h3>
                <p class="tk-sec-sub m-0">Xếp hạng theo số lượng bán ra và doanh thu</p>
              </div>
            </div>
            <span v-if="thuongHieuChon || tuKhoaSp" class="tk-filter-tag">
              <i class="bi bi-funnel-fill me-1" aria-hidden="true"></i>
              Đang lọc: {{ [thuongHieuChon, tuKhoaSp].filter(Boolean).join(' · ') }} ({{ banChay.length }} SP)
            </span>
            <span v-else-if="banChay.length" class="tk-count-badge">{{ banChay.length }} sản phẩm</span>
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
                    <th style="width: 52px">#</th>
                    <th>Sản phẩm</th>
                    <th>Phân loại bán chạy</th>
                    <th class="text-center" style="width: 110px">Số lượng</th>
                    <th class="text-end" style="width: 150px">Doanh thu</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(p, i) in banChay" :key="p.idSanPham">
                    <td>
                      <span class="tk-hang" :class="i < 3 && `is-top-${i + 1}`">{{ i + 1 }}</span>
                    </td>
                    <td>
                      <div class="fw-semibold text-dark">{{ p.tenSanPham }}</div>
                      <small class="text-muted d-block">
                        {{ p.maSanPham }}<template v-if="p.thuongHieu"> · <span class="text-primary fw-medium">{{ p.thuongHieu }}</span></template> · {{ p.soDon }} đơn
                      </small>
                    </td>
                    <td>
                      <span v-if="p.phanLoaiBanChay" class="tk-variant-chip">{{ p.phanLoaiBanChay }}</span>
                      <span v-else class="text-muted small">—</span>
                    </td>
                    <td class="text-center">
                      <span class="tk-qty-badge">{{ p.soLuong }}</span>
                    </td>
                    <td class="text-end ad-nowrap fw-bold text-dark">{{ formatMoney(p.doanhThu) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </section>
      </div>

      <!-- Bảng Sắp hết hàng (lọc trực tiếp theo THƯƠNG HIỆU và TÌM SẢN PHẨM trên bộ lọc) -->
      <div class="col-12 col-xl-5">
        <section class="ad-card tk-section-card h-100 mb-0" data-test="tk-sap-het">
          <div class="tk-sec-head">
            <div class="d-flex align-items-center gap-2">
              <span class="tk-sec-ico is-rose"><i class="bi bi-exclamation-triangle-fill" aria-hidden="true"></i></span>
              <div>
                <h3 class="tk-sec-title m-0">Sắp hết hàng</h3>
                <p class="tk-sec-sub m-0">Cảnh báo các biến thể cần nhập thêm</p>
              </div>
            </div>
            <span v-if="!dangTaiGd3" class="tk-stock-summary">
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

      <!-- Hàng phân tích Kênh bán (Loại đơn) & Phương thức thanh toán (Tiền mặt / Chuyển khoản) -->
      <div class="col-12 col-lg-6">
        <section class="ad-card tk-section-card h-100 mb-0">
          <div class="tk-sec-head">
            <div class="d-flex align-items-center gap-2">
              <span class="tk-sec-ico is-sky"><i class="bi bi-bag-check-fill" aria-hidden="true"></i></span>
              <div>
                <h3 class="tk-sec-title m-0">Doanh thu theo loại đơn</h3>
                <p class="tk-sec-sub m-0">Tính trên {{ tk.hoanThanh }} đơn hoàn thành trong kỳ</p>
              </div>
            </div>
            <span v-if="tk.hoanThanh" class="tk-count-badge">{{ tk.hoanThanh }} đơn</span>
          </div>
          <div v-if="!theoLoai.length" class="ad-empty">Chưa có dữ liệu.</div>
          <div v-for="l in theoLoai" :key="l.ten" class="tk-loai-item">
            <div class="d-flex justify-content-between align-items-center small mb-2">
              <span class="fw-semibold text-dark">
                {{ l.ten }}
                <small class="text-muted fw-normal ms-1">({{ l.so }} đơn)</small>
              </span>
              <span class="fw-bold text-primary">{{ formatMoney(l.v) }} <small class="text-muted fw-normal">({{ pct(l.pct) }})</small></span>
            </div>
            <div class="progress tk-progress">
              <div class="progress-bar tk-progress-bar" :style="{ width: l.pct + '%' }"></div>
            </div>
          </div>
        </section>
      </div>

      <div class="col-12 col-lg-6">
        <section class="ad-card tk-section-card h-100 mb-0" data-test="tk-phuong-thuc">
          <div class="tk-sec-head">
            <div class="d-flex align-items-center gap-2">
              <span class="tk-sec-ico is-blue"><i class="bi bi-credit-card-2-front-fill" aria-hidden="true"></i></span>
              <div>
                <h3 class="tk-sec-title m-0">Tiền thu theo phương thức thanh toán</h3>
                <p class="tk-sec-sub m-0">Số tiền khách thực trả (Tiền mặt, Chuyển khoản, COD — gồm cả phí ship)</p>
              </div>
            </div>
            <span v-if="tongTienPt > 0" class="tk-count-badge">Tổng thu: {{ formatMoney(tongTienPt) }}</span>
          </div>
          <div v-if="!theoPt.length" class="ad-empty">{{ dangTaiGd3 ? 'Đang tải…' : 'Chưa có dữ liệu.' }}</div>
          <div v-for="p in theoPt" :key="p.ten" class="tk-loai-item">
            <div class="d-flex justify-content-between align-items-center small mb-2">
              <span class="fw-semibold text-dark">
                {{ p.ten }}
                <small class="text-muted fw-normal ms-1">({{ p.soDon }} đơn)</small>
              </span>
              <span class="fw-bold text-primary">{{ formatMoney(p.soTien) }} <small class="text-muted fw-normal">({{ pctGon(p.pct) }})</small></span>
            </div>
            <div class="progress tk-progress">
              <div class="progress-bar tk-progress-bar" :style="{ width: p.pct + '%' }"></div>
            </div>
          </div>
        </section>
      </div>

      <!-- Hàng xếp hạng Nhân viên & Khách hàng -->
      <div class="col-12 col-lg-6">
        <section class="ad-card tk-section-card h-100 mb-0" data-test="tk-top-nv">
          <div class="tk-sec-head">
            <div class="d-flex align-items-center gap-2">
              <span class="tk-sec-ico is-green"><i class="bi bi-person-badge-fill" aria-hidden="true"></i></span>
              <div>
                <h3 class="tk-sec-title m-0">Nhân viên bán tốt nhất</h3>
                <p class="tk-sec-sub m-0">Top nhân viên theo doanh thu đơn hoàn thành trong kỳ</p>
              </div>
            </div>
          </div>
          <div v-if="!topNhanVien.length" class="ad-empty">Chưa có dữ liệu.</div>
          <ol v-else class="list-unstyled m-0 tk-rank">
            <li v-for="(r, i) in topNhanVien" :key="r.ten">
              <span class="tk-hang" :class="i < 3 && `is-top-${i + 1}`">{{ i + 1 }}</span>
              <span class="tk-ten">
                <span class="fw-semibold text-dark">{{ r.ten }}</span>
                <small class="text-muted d-block">{{ r.so }} đơn hoàn thành</small>
              </span>
              <b class="tk-rank-money">{{ formatMoney(r.v) }}</b>
            </li>
          </ol>
        </section>
      </div>

      <div class="col-12 col-lg-6">
        <section class="ad-card tk-section-card h-100 mb-0" data-test="tk-top-kh">
          <div class="tk-sec-head">
            <div class="d-flex align-items-center gap-2">
              <span class="tk-sec-ico is-purple"><i class="bi bi-people-fill" aria-hidden="true"></i></span>
              <div>
                <h3 class="tk-sec-title m-0">Khách hàng mua nhiều nhất</h3>
                <p class="tk-sec-sub m-0">Top khách hàng thành viên theo doanh thu đơn hoàn thành (không tính khách lẻ)</p>
              </div>
            </div>
          </div>
          <div v-if="!topKhach.length" class="ad-empty">Chưa có dữ liệu.</div>
          <ol v-else class="list-unstyled m-0 tk-rank">
            <li v-for="(r, i) in topKhach" :key="r.ten">
              <span class="tk-hang" :class="i < 3 && `is-top-${i + 1}`">{{ i + 1 }}</span>
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

    <!-- Ẩn hoàn toàn phần ca làm việc / lịch làm việc khỏi giao diện Thống kê -->
    <div class="d-none" aria-hidden="true">

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
/* Bố cục trang thống kê thoáng đãng, đồng bộ hoàn toàn với admin.css */
.tk-page {
  display: flex;
  flex-direction: column;
  gap: 1.15rem;
}

/* ---------- Khung Bộ lọc thống kê ---------- */
.tk-filter-card {
  padding: 1.35rem 1.5rem;
  border-radius: 18px;
  border: 1px solid #e5ecf4;
  background: #ffffff;
  box-shadow: 0 8px 26px rgba(15, 23, 42, 0.045);
}

.tk-filter-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding-bottom: 1.1rem;
  border-bottom: 1px solid #eef2f7;
}

.tk-filter-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.65rem;
}

.tk-pill-bar {
  display: inline-flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.3rem;
  background: #f4f7fb;
  padding: 0.28rem;
  border-radius: 999px;
  border: 1px solid #e6ecf5;
}

.tk-pill-btn {
  border: 1px solid transparent;
  background: transparent;
  color: #475569;
  font-size: 0.82rem;
  font-weight: 600;
  padding: 0.36rem 0.82rem;
  border-radius: 999px;
  transition: all 0.16s ease;
}

.tk-pill-btn:hover {
  color: #0f172a;
  background: rgba(255, 255, 255, 0.75);
}

.tk-pill-btn.is-active {
  background: #0977ec;
  border-color: #0977ec;
  color: #ffffff;
  box-shadow: 0 2px 8px rgba(9, 119, 236, 0.28);
}

.tk-btn-excel {
  border-color: #a7f3d0;
  background: #ecfdf5;
  color: #047857;
}

.tk-btn-excel:hover:not(:disabled) {
  background: #d1fae5;
  border-color: #6ee7b7;
  color: #065f46;
}

.tk-filter-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(150px, 1fr)) minmax(260px, 1.45fr);
  gap: 1rem;
  align-items: end;
  padding-top: 1.1rem;
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
  display: flex;
  align-items: center;
  gap: 0.65rem;
  margin-top: 1rem;
  padding: 0.6rem 0.9rem;
  background: #f8fafc;
  border: 1px solid #eef2f7;
  border-radius: 11px;
  font-size: 0.83rem;
  color: #475569;
}

.tk-status-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #0977ec;
  box-shadow: 0 0 0 4px rgba(9, 119, 236, 0.16);
  flex: none;
}

.tk-active-chip {
  display: inline-flex;
  align-items: center;
  padding: 0.18rem 0.62rem;
  border-radius: 999px;
  background: #e7f1fd;
  color: #075fc0;
  font-size: 0.76rem;
  font-weight: 600;
}

/* ---------- 4 Thẻ chỉ số KPI ---------- */
.tk-kpi-card {
  position: relative;
  padding: 1.25rem 1.35rem;
  border-radius: 18px;
  border: 1px solid #e8eef5;
  overflow: hidden;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.tk-kpi-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.08);
}

.tk-kpi-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3.5px;
}

.tk-kpi--blue::before { background: linear-gradient(90deg, #0977ec, #38bdf8); }
.tk-kpi--sky::before { background: linear-gradient(90deg, #0284c7, #38bdf8); }
.tk-kpi--green::before { background: linear-gradient(90deg, #16a34a, #4ade80); }
.tk-kpi--rose::before { background: linear-gradient(90deg, #e11d48, #fb7185); }

.tk-kpi-icon {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  font-size: 1.4rem;
  flex: none;
}

.tk-kpi-icon.is-blue { background: #e8f3fe; color: #0977ec; }
.tk-kpi-icon.is-sky { background: #e0f2fe; color: #0284c7; }
.tk-kpi-icon.is-green { background: #dcfce7; color: #15803d; }
.tk-kpi-icon.is-rose { background: #ffe4e6; color: #e11d48; }

.tk-kpi-label {
  font-size: 0.8rem;
  font-weight: 600;
  color: #64748b;
  margin-bottom: 0.2rem;
}

.tk-kpi-value {
  font-size: 1.45rem;
  font-weight: 700;
  color: var(--fs-text, #18242f);
  line-height: 1.25;
}

.tk-kpi-note {
  font-size: 0.77rem;
  color: #475569;
  font-weight: 500;
  margin-top: 0.25rem;
}

.tk-so {
  font-weight: 600;
  color: #64748b;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}

.tk-so-badge {
  display: inline-flex;
  align-items: center;
  padding: 0.12rem 0.5rem;
  border-radius: 999px;
  font-size: 0.74rem;
  font-weight: 700;
  background: #f1f5f9;
  color: #475569;
}

.tk-so-badge.is-tot {
  background: #dcfce7;
  color: #15803d;
}

.tk-so-badge.is-xau {
  background: #ffe4e6;
  color: #be123c;
}

.tk-tot { color: #15803d; }
.tk-xau { color: #be123c; }

/* ---------- Các thẻ nội dung & bảng bên dưới ---------- */
.tk-section-card {
  padding: 1.35rem 1.45rem;
  border-radius: 18px;
  border: 1px solid #e8eef5;
}

.tk-sec-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 1.15rem;
}

.tk-sec-ico {
  width: 38px;
  height: 38px;
  border-radius: 11px;
  display: grid;
  place-items: center;
  font-size: 1.05rem;
  flex: none;
}

.tk-sec-ico.is-blue { background: #e8f3fe; color: #0977ec; }
.tk-sec-ico.is-indigo { background: #e0e7ff; color: #4f46e5; }
.tk-sec-ico.is-amber { background: #fef3c7; color: #d97706; }
.tk-sec-ico.is-rose { background: #ffe4e6; color: #e11d48; }
.tk-sec-ico.is-sky { background: #e0f2fe; color: #0284c7; }
.tk-sec-ico.is-green { background: #dcfce7; color: #15803d; }
.tk-sec-ico.is-purple { background: #f3e8ff; color: #7e22ce; }

.tk-sec-title {
  font-size: 0.98rem;
  font-weight: 700;
  color: #0f172a;
}

.tk-sec-sub {
  font-size: 0.76rem;
  color: #64748b;
  margin-top: 0.1rem !important;
}

.tk-chart-summary {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.45rem;
}

.tk-chart-pill {
  padding: 0.28rem 0.72rem;
  border-radius: 999px;
  background: #f1f5f9;
  color: #334155;
  font-size: 0.78rem;
  font-weight: 500;
}

.tk-chart-pill strong {
  color: #0977ec;
}

.tk-chart-pill.is-soft {
  background: #ecfdf5;
  color: #047857;
  font-weight: 600;
}

.tk-chart-pill.is-amber {
  background: #fffbeb;
  border: 1px solid #fde68a;
  color: #92400e;
}

.tk-chart-pill.is-amber strong {
  color: #b45309;
}

.tk-chart-toggle-btn {
  border: 1px solid #cbd5e1;
  background: #ffffff;
  color: #334155;
  cursor: pointer;
  transition: all 0.15s ease;
}

.tk-chart-toggle-btn:hover {
  border-color: #0977ec;
  color: #0977ec;
  background: #f0f7ff;
}

.tk-bar-rect {
  transition: opacity 0.15s ease, filter 0.15s ease;
  cursor: pointer;
}

.tk-bar-rect:hover {
  filter: brightness(0.92);
  opacity: 0.95;
}

.ad-donut {
  width: 174px;
  height: 174px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  position: relative;
  box-shadow: inset 0 0 0 1px rgba(15, 23, 42, 0.05);
}

.ad-donut::after {
  content: '';
  position: absolute;
  inset: 28px;
  background: #ffffff;
  border-radius: 50%;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.06);
}

.ad-donut span {
  position: relative;
  z-index: 1;
  font-size: 1.65rem;
  font-weight: 800;
  color: #0f172a;
  text-align: center;
  line-height: 1.1;
}

.ad-donut small {
  display: block;
  font-size: 0.74rem;
  font-weight: 600;
  color: #64748b;
  margin-top: 2px;
}

.tk-status-list {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.tk-status-item {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  padding: 0.36rem 0.55rem;
  border-radius: 8px;
  transition: background-color 0.15s ease;
}

.tk-status-item:hover {
  background: #f8fafc;
}

.ad-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex: none;
}

.tk-status-name {
  color: #334155;
  font-weight: 500;
  min-width: 110px;
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
  min-width: 3.3rem;
  text-align: right;
  color: #64748b;
  font-weight: 600;
}

.tk-ops-banner {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem 1.35rem;
  border-radius: 16px;
  border: 1px solid #e5ecf4;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  cursor: pointer;
  user-select: none;
  transition: all 0.16s ease;
}

.tk-ops-banner:hover {
  border-color: #bfd7f6;
  box-shadow: 0 8px 22px rgba(9, 119, 236, 0.08);
}

.tk-ops-banner.is-open {
  border-color: #0977ec;
  background: linear-gradient(180deg, #f5f9ff 0%, #ffffff 100%);
}

.tk-ops-chip {
  display: inline-flex;
  align-items: center;
  padding: 0.28rem 0.72rem;
  border-radius: 999px;
  background: #f1f5f9;
  color: #475569;
  font-size: 0.78rem;
  font-weight: 600;
}

.tk-ops-chip i {
  color: #0977ec;
}

.tk-ops-chip.is-warn {
  background: #fffbeb;
  border: 1px solid #fde68a;
  color: #b45309;
}

.tk-ops-chip.is-warn i {
  color: #d97706;
}

.tk-ops-btn {
  display: inline-flex;
  align-items: center;
  padding: 0.36rem 0.9rem;
  border-radius: 999px;
  background: #e7f1fd;
  color: #075fc0;
  font-size: 0.8rem;
  font-weight: 600;
  transition: all 0.15s ease;
}

.tk-ops-banner:hover .tk-ops-btn,
.tk-ops-banner.is-open .tk-ops-btn {
  background: #0977ec;
  color: #ffffff;
}

.tk-ops-link {
  display: inline-flex;
  align-items: center;
  padding: 0.28rem 0.72rem;
  border-radius: 999px;
  background: #e7f1fd;
  color: #075fc0;
  font-size: 0.78rem;
  font-weight: 600;
  text-decoration: none;
  transition: all 0.15s ease;
}

.tk-ops-link:hover {
  background: #0977ec;
  color: #ffffff;
}

.tk-filter-tag {
  display: inline-flex;
  align-items: center;
  padding: 0.25rem 0.7rem;
  border-radius: 999px;
  background: #e7f1fd;
  color: #075fc0;
  font-size: 0.76rem;
  font-weight: 600;
}

.tk-count-badge {
  padding: 0.25rem 0.68rem;
  border-radius: 999px;
  background: #f1f5f9;
  color: #475569;
  font-size: 0.76rem;
  font-weight: 600;
}

.tk-stock-summary {
  padding: 0.25rem 0.7rem;
  border-radius: 999px;
  background: #fffbeb;
  border: 1px solid #fde68a;
  color: #92400e;
  font-size: 0.76rem;
  font-weight: 500;
}

.tk-variant-chip {
  display: inline-block;
  padding: 0.18rem 0.55rem;
  border-radius: 6px;
  background: #f1f5f9;
  color: #334155;
  font-size: 0.78rem;
  font-weight: 500;
}

.tk-qty-badge {
  display: inline-block;
  min-width: 2rem;
  text-align: center;
  padding: 0.16rem 0.5rem;
  border-radius: 6px;
  background: #eff6ff;
  color: #0977ec;
  font-weight: 700;
  font-size: 0.82rem;
}

.tk-rank li {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.62rem 0.4rem;
  border-bottom: 1px solid #f1f5f9;
  font-size: 0.9rem;
  border-radius: 8px;
  transition: background-color 0.15s ease;
}

.tk-rank li:hover {
  background: #f8fafc;
}

.tk-rank li:last-child {
  border-bottom: 0;
}

.tk-hang {
  width: 1.85rem;
  height: 1.85rem;
  border-radius: 50%;
  background: #eaf3fe;
  color: #0977ec;
  font-weight: 700;
  font-size: 0.8rem;
  display: grid;
  place-items: center;
  flex: none;
}

.tk-hang.is-top-1 {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  color: #b45309;
  box-shadow: 0 2px 6px rgba(245, 158, 11, 0.22);
}

.tk-hang.is-top-2 {
  background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%);
  color: #334155;
}

.tk-hang.is-top-3 {
  background: linear-gradient(135deg, #ffedd5 0%, #fed7aa 100%);
  color: #c2410c;
}

.tk-ten {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tk-rank-money {
  color: #0f172a;
  font-weight: 700;
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
  margin-bottom: 1rem;
}

.tk-loai-item:last-child {
  margin-bottom: 0;
}

.tk-progress {
  height: 10px;
  border-radius: 999px;
  background: #eef2f7;
  overflow: hidden;
}

.tk-progress-bar {
  background: linear-gradient(90deg, #0977ec 0%, #38bdf8 100%);
  border-radius: 999px;
}

.tk-vua { color: #d97706; }

.tk-sap-het li {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.6rem 0.5rem;
  border-bottom: 1px solid #f1f5f9;
  font-size: 0.9rem;
  border-radius: 8px;
  transition: background-color 0.15s ease;
}

.tk-sap-het li:hover {
  background: #f8fafc;
}

.tk-sap-het li:last-child {
  border-bottom: 0;
}

.tk-sap-het {
  max-height: 420px;
  overflow-y: auto;
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
  padding: 0.55rem 0.85rem;
  border-radius: 10px;
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
