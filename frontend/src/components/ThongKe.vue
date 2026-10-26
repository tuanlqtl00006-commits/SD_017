<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import api from '../services/api'
import { useToast } from '../composables/useToast'
import { useAutoRefresh } from '../composables/useAutoRefresh'
import { formatDate, formatMoney, todayIso } from '../utils/format'
import { TRANG_THAI_HOA_DON, TT } from '../constants/hoaDon'
import {
  KHOANG_NHANH, TT_HOAN_THANH, TT_HOAN_TIEN, TT_HUY, congNgay, gioLam, gomCot, khoangGuiMayChu, khungGioHienThi,
  kyTruoc, laHoaDonCho, lamTronTran, locChiTietHoanThanh, locTheoKhoang, nhanTienGon, phanTram, phanTramThayDoi,
  soNgay, theoDanhMuc, theoLoaiDon, theoThuongHieu, tinhKhoang, tongHop, tongHopUuDai, tongKetMocThoiGian,
  topSanPhamBanChay, tyLeCoMat, tyLeDungGio, xepHang,
} from '../utils/thongKe'
import DateInput from './common/DateInput.vue'
import { exportExcel } from '../utils/exportExcel'

// Trang Quản lý Thống kê: tính từ danh sách hóa đơn (GET /api/hoa-don), kết hợp thống kê GD3 (GET /api/thong-ke)
// và liên kết toàn hệ thống CSDL (GET /api/thong-ke/tong-quan).
const toast = useToast()
const hoaDon = ref([])
const chiTietHoaDon = ref([])
const uuDaiHoaDon = ref([])
const tonKhoCanhBao = ref([])
const heThong = ref(null)
const loading = ref(true)
const homNay = ref(todayIso())
const khoang = ref(30) // 1 | 7 | 30 | 90 | 0 (tất cả) | -1 (tự chọn)
const tuChon = ref(congNgay(todayIso(), -29))
const denChon = ref(todayIso())

// Bộ lọc & tìm kiếm cho các bảng chi tiết
const tieuChiTopSp = ref('soLuong') // 'soLuong' | 'doanhThu'
const gioiHanTopSp = ref(6)
const tuKhoaSp = ref('')
const boLocTonKho = ref('tatCa') // 'tatCa' | 'hetHang' | 'sapHet' | 'dangGiam'

const boDau = (s) =>
  String(s ?? '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .toLowerCase()
    .trim()

const ISO_RE = /^\d{4}-\d{2}-\d{2}$/

// Màu từng trạng thái trên biểu đồ; nhãn lấy từ constants/hoaDon.js để không lệch với trang Hóa đơn
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
    // Hóa đơn chờ tại quầy (chưa thanh toán, đang giữ hàng) không phải đơn bán: bỏ khỏi thống kê
    hoaDon.value = (res?.data?.content ?? []).filter((h) => !laHoaDonCho(h))
    const tkData = resTk?.data
    if (tkData && typeof tkData === 'object' && Array.isArray(tkData.chiTietHoaDon)) {
      chiTietHoaDon.value = tkData.chiTietHoaDon
      uuDaiHoaDon.value = Array.isArray(tkData.uuDaiHoaDon) ? tkData.uuDaiHoaDon : []
      tonKhoCanhBao.value = Array.isArray(tkData.tonKhoCanhBao) ? tkData.tonKhoCanhBao : []
      heThong.value = tkData.heThong ?? null
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

/* ----- Validate khoảng thời gian Tự chọn & Xử lý các case khó ----- */
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
  if (tu && den && tu > den) {
    return {
      loai: 'daoNguoc',
      text: `"Từ ngày" (${formatDate(tu)}) đang lớn hơn "Đến ngày" (${formatDate(den)}). Hệ thống đã tự động đảo lại thành ${formatDate(den)} – ${formatDate(tu)} để không bị trống số liệu.`,
    }
  }
  if ((tu && tu > homNay.value) || (den && den > homNay.value)) {
    return {
      loai: 'tuongLai',
      text: `Khoảng ngày đang chọn vượt quá ngày hiện tại (${formatDate(homNay.value)}). Các ngày trong tương lai chưa phát sinh doanh thu.`,
    }
  }
  if (tu && den && soNgay(tu, den) > 366) {
    return {
      loai: 'khoangRong',
      text: `Khoảng thời gian rộng (${soNgay(tu, den)} ngày). Biểu đồ đã tự động gom nhóm theo tháng để dễ quan sát.`,
    }
  }
  return null
})

function chuanHoaKhoangNgay() {
  if (tuChon.value && denChon.value && tuChon.value > denChon.value) {
    const tmp = tuChon.value
    tuChon.value = denChon.value
    denChon.value = tmp
  }
  if (denChon.value && denChon.value > homNay.value) denChon.value = homNay.value
  if (tuChon.value && tuChon.value > homNay.value) tuChon.value = homNay.value
}

function chonNhanhTuChon(kieu) {
  const hn = homNay.value
  khoang.value = -1
  if (kieu === 'thangNay') {
    tuChon.value = `${hn.slice(0, 7)}-01`
    denChon.value = hn
  } else if (kieu === 'thangTruoc') {
    const [y, m] = hn.slice(0, 7).split('-').map(Number)
    const dCuoiThangTruoc = new Date(y, m - 1, 0, 12)
    const yT = dCuoiThangTruoc.getFullYear()
    const mT = String(dCuoiThangTruoc.getMonth() + 1).padStart(2, '0')
    const dT = String(dCuoiThangTruoc.getDate()).padStart(2, '0')
    tuChon.value = `${yT}-${mT}-01`
    denChon.value = `${yT}-${mT}-${dT}`
  } else if (kieu === 'namNay') {
    tuChon.value = `${hn.slice(0, 4)}-01-01`
    denChon.value = hn
  } else {
    tuChon.value = congNgay(hn, -29)
    denChon.value = hn
  }
}

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

/* ----- Liên kết đa bảng từ CSDL & Đối soát tài chính (xử lý các case khó) ----- */
const mapUuDaiTheoId = computed(() => new Map(uuDaiHoaDon.value.map((u) => [u.idHoaDon, u])))
const mapKhachTheoTen = computed(() => {
  const m = new Map()
  for (const u of uuDaiHoaDon.value) {
    if (u.tenKhachHang && u.idKhachHang) m.set(u.tenKhachHang.trim(), { id: u.idKhachHang, ma: u.maKhachHang, sdt: u.sdtKhachHang })
  }
  return m
})
const mapNhanVienTheoTen = computed(() => {
  const m = new Map()
  for (const u of uuDaiHoaDon.value) {
    if (u.tenNhanVien && u.idNhanVien) m.set(u.tenNhanVien.trim(), { id: u.idNhanVien, ma: u.maNhanVien })
  }
  return m
})

// Đối soát dòng tiền: tách bạch Tiền hàng gốc, Giảm giá Voucher, Phí ship, Thực thu ròng, Đơn đang xử lý và Đơn hủy/thất bại
const doiSoatTaiChinh = computed(() => {
  let tienHangHoanThanh = 0
  let tienGiamHoanThanh = 0
  let phiShipHoanThanh = 0
  let soDonDangXuLy = 0
  let tienDangXuLy = 0
  let soDonThatThoat = 0
  let tienThatThoat = 0

  for (const h of trongKhoang.value) {
    const tong = Math.max(0, Number(h.tongTien ?? h.doanhThu ?? 0))
    const u = mapUuDaiTheoId.value.get(h.id)
    const giam = Math.max(0, Number(u?.soTienGiam || 0))
    const ship = Math.max(0, Number(u?.phiVanChuyen || 0))

    if (h.trangThai === TT_HOAN_THANH) {
      tienHangHoanThanh += tong
      tienGiamHoanThanh += giam
      phiShipHoanThanh += ship
    } else if (h.trangThai === TT_HUY || h.trangThai === TT_HOAN_TIEN || h.trangThai === TT.GIAO_THAT_BAI) {
      soDonThatThoat++
      tienThatThoat += tong
    } else {
      soDonDangXuLy++
      tienDangXuLy += tong
    }
  }

  // Case khó: Voucher giảm tối đa không được làm âm tiền hàng của hóa đơn
  const thucThu = Math.max(0, tienHangHoanThanh - Math.min(tienGiamHoanThanh, tienHangHoanThanh) + phiShipHoanThanh)
  return {
    tienHangHoanThanh,
    tienGiamHoanThanh,
    phiShipHoanThanh,
    thucThu,
    soDonDangXuLy,
    tienDangXuLy,
    soDonThatThoat,
    tienThatThoat,
  }
})

const topSanPhamGoc = computed(() =>
  topSanPhamBanChay(chiTietHoaDon.value, kq.value.tu, kq.value.den, gioiHanTopSp.value, tieuChiTopSp.value),
)
const topSanPham = computed(() => {
  const kw = boDau(tuKhoaSp.value)
  if (!kw) return topSanPhamGoc.value
  return topSanPhamGoc.value.filter((sp) =>
    boDau(`${sp.tenSanPham} ${sp.maSanPham} ${sp.maBienThe} ${sp.tenBienThe} ${sp.tenThuongHieu}`).includes(kw),
  )
})

const tonKhoDaLoc = computed(() => {
  const ds = tonKhoCanhBao.value
  if (boLocTonKho.value === 'hetHang') return ds.filter((b) => Number(b.soLuongTon) <= 0)
  if (boLocTonKho.value === 'sapHet') return ds.filter((b) => Number(b.soLuongTon) > 0 && Number(b.soLuongTon) <= 5)
  if (boLocTonKho.value === 'dangGiam') return ds.filter((b) => Number(b.phanTramGiam) > 0)
  return ds
})

const doanhThuThuongHieu = computed(() => theoThuongHieu(chiTietHoaDon.value, kq.value.tu, kq.value.den))
const doanhThuDanhMuc = computed(() => theoDanhMuc(chiTietHoaDon.value, kq.value.tu, kq.value.den))
const thongKeUuDai = computed(() => tongHopUuDai(uuDaiHoaDon.value, kq.value.tu, kq.value.den, 5))
const mocThoiGian = computed(() => tongKetMocThoiGian(hoaDon.value, chiTietHoaDon.value, homNay.value))

function chonMocThoiGian(m) {
  if (m.khoang > 0) {
    khoang.value = m.khoang
  } else {
    khoang.value = -1
    tuChon.value = m.tu
    denChon.value = m.den
  }
}

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
const topNhanVien = computed(() =>
  xepHang(trongKhoang.value, (h) => h.tenNhanVien).map((r) => ({
    ...r,
    info: mapNhanVienTheoTen.value.get(r.ten) ?? null,
  })),
)
const topKhach = computed(() =>
  xepHang(trongKhoang.value, (h) => h.tenKhachHang).map((r) => ({
    ...r,
    info: mapKhachTheoTen.value.get(r.ten) ?? null,
  })),
)
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
  return ds.map((x) => ({
    ...x,
    tenHienThi: x.ten || x.tenPhuongThuc || 'Phương thức',
    soDonHienThi: x.soDon ?? x.soGiaoDich ?? 0,
    pct: phanTram(x.soTien, tong),
  }))
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
  const ds6 = doiSoatTaiChinh.value
  const T = (header, key, width = 18, money = false) => ({ header, key, width, money })
  const sheets = [
    {
      sheetName: 'Tổng quan',
      columns: [T('Chỉ số', 'ten', 34), T('Giá trị', 'gt', 20, true), T('Ghi chú', 'ghiChu', 50)],
      rows: [
        { ten: 'Khoảng thời gian', gt: moTaKhoang.value },
        { ten: 'Doanh thu thực tế', gt: a.doanhThu, ghiChu: 'Đơn đã hoàn thành, không gồm phí ship' },
        { ten: 'Doanh thu dự kiến', gt: a.doanhThuDuKien, ghiChu: 'Mọi đơn chưa hủy (gồm đơn đang xử lý / đang giao)' },
        { ten: 'Thực thu ròng (Đã đối soát)', gt: ds6.thucThu, ghiChu: 'Tiền hàng hoàn thành - Giảm giá Voucher + Phí ship' },
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
    { sheetName: 'Phương thức TT', columns: [T('Phương thức', 'tenHienThi', 22), T('Số đơn', 'soDonHienThi'), T('Số tiền thu', 'soTien', 20, true)], rows: theoPt.value },
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

      <div v-if="khoang === -1" class="mt-3" data-test="tk-tu-chon">
        <div class="d-flex flex-wrap align-items-end gap-3">
          <div>
            <label for="tk-tu" class="form-label small mb-1">Từ ngày</label>
            <DateInput id="tk-tu" v-model="tuChon" class="form-control form-control-sm" :max="denChon || homNay" />
          </div>
          <div>
            <label for="tk-den" class="form-label small mb-1">Đến ngày</label>
            <DateInput id="tk-den" v-model="denChon" class="form-control form-control-sm" :min="tuChon" :max="homNay" />
          </div>
          <div class="d-flex flex-wrap align-items-center gap-1">
            <span class="text-muted small me-1">Chọn nhanh:</span>
            <button type="button" class="btn btn-sm btn-outline-secondary" @click="chonNhanhTuChon('thangNay')">Đầu tháng đến nay</button>
            <button type="button" class="btn btn-sm btn-outline-secondary" @click="chonNhanhTuChon('thangTruoc')">Tháng trước</button>
            <button type="button" class="btn btn-sm btn-outline-secondary" @click="chonNhanhTuChon('namNay')">Đầu năm đến nay</button>
          </div>
          <p v-if="!tuChon && !denChon" class="text-muted small mb-1">Chưa chọn ngày nào nên đang xem toàn bộ thời gian.</p>
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
            v-if="canhBaoNgay.loai === 'daoNguoc' || canhBaoNgay.loai === 'tuongLai'"
            type="button"
            class="btn btn-sm btn-warning ms-auto"
            data-test="tk-chuan-hoa-ngay"
            @click="chuanHoaKhoangNgay"
          >
            Chuẩn hóa ngày
          </button>
        </div>
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

    <!-- Tổng kết nhanh 4 mốc cố định: Hôm nay · 7 ngày qua · Tháng này · Năm nay -->
    <div class="row g-3 mb-3" data-test="tk-moc-thoi-gian">
      <div v-for="m in mocThoiGian" :key="m.ma" class="col-12 col-sm-6 col-xl-3">
        <button
          type="button"
          class="tk-moc-card w-100 text-start"
          :class="{ 'is-active': kq.tu === m.tu && kq.den === m.den }"
          @click="chonMocThoiGian(m)"
        >
          <div class="d-flex align-items-center justify-content-between mb-1">
            <span class="tk-moc-label">{{ m.nhan }}</span>
            <span class="tk-moc-badge">{{ m.hoanThanh }}/{{ m.tongDon }} đơn hoàn thành</span>
          </div>
          <div class="tk-moc-money">{{ loading ? '…' : formatMoney(m.doanhThu) }}</div>
          <div class="tk-moc-sub">
            <span v-if="m.soSanPham">Đã bán <strong>{{ m.soSanPham }}</strong> SP · </span>
            <span>Hủy: <strong>{{ m.huy }}</strong> đơn</span>
          </div>
        </button>
      </div>
    </div>

    <!-- Bảng Đối soát Dòng tiền & Đơn hàng trong kỳ -->
    <section class="ad-card mb-3" data-test="tk-doi-soat">
      <div class="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-2">
        <div class="d-flex align-items-center gap-2">
          <i class="bi bi-shield-check text-primary" aria-hidden="true"></i>
          <h3 class="fs-6 fw-bold m-0">Đối soát dòng tiền &amp; trạng thái dòng đơn trong kỳ</h3>
        </div>
        <span class="text-muted small">Công thức: Thực thu = Tiền hàng hoàn thành &minus; Giảm giá Voucher + Phí vận chuyển (không âm)</span>
      </div>
      <div class="tk-reconcile-grid">
        <div class="tk-rec-box">
          <div class="tk-rec-label">Tiền hàng (Đơn hoàn thành)</div>
          <div class="tk-rec-val text-dark">{{ formatMoney(doiSoatTaiChinh.tienHangHoanThanh) }}</div>
          <div class="tk-rec-note">{{ tk.hoanThanh }} đơn đã hoàn tất</div>
        </div>
        <div class="tk-rec-box">
          <div class="tk-rec-label">Giảm giá Voucher</div>
          <div class="tk-rec-val text-danger">&minus;{{ formatMoney(doiSoatTaiChinh.tienGiamHoanThanh) }}</div>
          <div class="tk-rec-note">{{ thongKeUuDai.soDonDungPhieu }} đơn áp dụng mã</div>
        </div>
        <div class="tk-rec-box">
          <div class="tk-rec-label">Phí vận chuyển</div>
          <div class="tk-rec-val text-secondary">+{{ formatMoney(doiSoatTaiChinh.phiShipHoanThanh) }}</div>
          <div class="tk-rec-note">Thu từ đơn giao hàng</div>
        </div>
        <div class="tk-rec-box is-highlight">
          <div class="tk-rec-label">Thực thu ròng (Đã đối soát)</div>
          <div class="tk-rec-val text-primary" data-test="tk-thuc-thu">{{ formatMoney(doiSoatTaiChinh.thucThu) }}</div>
          <div class="tk-rec-note">Sau khấu trừ khuyến mãi</div>
        </div>
        <div class="tk-rec-box">
          <div class="tk-rec-label">Đang xử lý / Đang giao</div>
          <div class="tk-rec-val text-warning-emphasis">{{ formatMoney(doiSoatTaiChinh.tienDangXuLy) }}</div>
          <div class="tk-rec-note">{{ doiSoatTaiChinh.soDonDangXuLy }} đơn chờ hoàn tất</div>
        </div>
        <div class="tk-rec-box">
          <div class="tk-rec-label">Đơn hủy / Giao thất bại</div>
          <div class="tk-rec-val text-muted">{{ formatMoney(doiSoatTaiChinh.tienThatThoat) }}</div>
          <div class="tk-rec-note">{{ doiSoatTaiChinh.soDonThatThoat }} đơn không ghi nhận DT</div>
        </div>
      </div>
    </section>

    <!-- Thanh liên kết trực tiếp với các phân hệ trong CSDL -->
    <div v-if="heThong" class="tk-db-strip mb-3" data-test="tk-he-thong">
      <RouterLink to="/san-pham" class="tk-db-chip">
        <span class="tk-db-icon"><i class="bi bi-box-seam" aria-hidden="true"></i></span>
        <div>
          <div class="tk-db-title">{{ heThong.tongSanPhamHoatDong }} sản phẩm · {{ heThong.tongBienTheHoatDong }} biến thể</div>
          <div class="tk-db-sub">Tổng tồn kho: <strong>{{ heThong.tongTonKho }}</strong> sản phẩm</div>
        </div>
        <i class="bi bi-chevron-right tk-db-arrow" aria-hidden="true"></i>
      </RouterLink>

      <RouterLink to="/san-pham/bien-the" class="tk-db-chip" :class="{ 'is-warn': heThong.soBienTheSapHet + heThong.soBienTheHetHang > 0 }">
        <span class="tk-db-icon"><i class="bi bi-exclamation-triangle" aria-hidden="true"></i></span>
        <div>
          <div class="tk-db-title">Cảnh báo tồn kho</div>
          <div class="tk-db-sub">
            <strong>{{ heThong.soBienTheHetHang }}</strong> hết hàng · <strong>{{ heThong.soBienTheSapHet }}</strong> sắp hết (&le; 5)
          </div>
        </div>
        <i class="bi bi-chevron-right tk-db-arrow" aria-hidden="true"></i>
      </RouterLink>

      <RouterLink to="/dot-giam-gia" class="tk-db-chip">
        <span class="tk-db-icon"><i class="bi bi-tags" aria-hidden="true"></i></span>
        <div>
          <div class="tk-db-title">{{ heThong.soDotDangDienRa }} đợt giảm giá đang chạy</div>
          <div class="tk-db-sub">{{ heThong.soDotSapDienRa }} đợt sắp diễn ra</div>
        </div>
        <i class="bi bi-chevron-right tk-db-arrow" aria-hidden="true"></i>
      </RouterLink>

      <RouterLink to="/phieu-giam-gia" class="tk-db-chip">
        <span class="tk-db-icon"><i class="bi bi-ticket-perforated" aria-hidden="true"></i></span>
        <div>
          <div class="tk-db-title">{{ heThong.soPhieuDangHoatDong }} phiếu giảm giá hoạt động</div>
          <div class="tk-db-sub">Đã giảm {{ formatMoney(thongKeUuDai.tongTienGiam) }} trong kỳ</div>
        </div>
        <i class="bi bi-chevron-right tk-db-arrow" aria-hidden="true"></i>
      </RouterLink>

      <RouterLink to="/khach-hang" class="tk-db-chip">
        <span class="tk-db-icon"><i class="bi bi-people" aria-hidden="true"></i></span>
        <div>
          <div class="tk-db-title">{{ heThong.tongKhachHangHoatDong }} khách hàng hoạt động</div>
          <div class="tk-db-sub">{{ heThong.tongNhanVienHoatDong }} nhân viên đang làm việc</div>
        </div>
        <i class="bi bi-chevron-right tk-db-arrow" aria-hidden="true"></i>
      </RouterLink>
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

          <template v-if="doanhThuThuongHieu.length">
            <hr class="my-3 text-muted opacity-25" />
            <div class="d-flex align-items-center justify-content-between mb-2">
              <h3 class="fs-6 fw-bold m-0">Doanh thu theo thương hiệu</h3>
              <RouterLink to="/san-pham/thuong-hieu" class="small text-decoration-none">Thương hiệu &rarr;</RouterLink>
            </div>
            <div v-for="th in doanhThuThuongHieu" :key="th.ten" class="mb-2" data-test="tk-thuong-hieu">
              <div class="d-flex justify-content-between small mb-1">
                <span class="fw-medium">{{ th.ten }} <small class="text-muted">({{ th.soLuong }} SP)</small></span>
                <span>{{ formatMoney(th.v) }} · {{ th.pct.toFixed(0) }}%</span>
              </div>
              <div class="progress" style="height: 8px"><div class="progress-bar bg-info" :style="{ width: th.pct + '%' }"></div></div>
            </div>
          </template>

          <template v-if="doanhThuDanhMuc.length">
            <hr class="my-3 text-muted opacity-25" />
            <div class="d-flex align-items-center justify-content-between mb-2">
              <h3 class="fs-6 fw-bold m-0">Doanh thu theo danh mục</h3>
              <RouterLink to="/san-pham/danh-muc" class="small text-decoration-none">Danh mục &rarr;</RouterLink>
            </div>
            <div v-for="dm in doanhThuDanhMuc" :key="dm.ten" class="mb-2" data-test="tk-danh-muc">
              <div class="d-flex justify-content-between small mb-1">
                <span class="fw-medium">{{ dm.ten }} <small class="text-muted">({{ dm.soLuong }} SP)</small></span>
                <span>{{ formatMoney(dm.v) }} · {{ dm.pct.toFixed(0) }}%</span>
              </div>
              <div class="progress" style="height: 8px"><div class="progress-bar bg-success" :style="{ width: dm.pct + '%' }"></div></div>
            </div>
          </template>
        </section>
      </div>

      <div class="col-12 col-md-6 col-lg-4">
        <section class="ad-card h-100 mb-0" data-test="tk-top-nv">
          <div class="d-flex align-items-center justify-content-between mb-3">
            <h3 class="fs-6 fw-bold m-0">Nhân viên bán tốt nhất</h3>
            <RouterLink to="/nhan-vien" class="small text-decoration-none">Xem tất cả</RouterLink>
          </div>
          <div v-if="!topNhanVien.length" class="ad-empty">Chưa có dữ liệu.</div>
          <ol v-else class="list-unstyled m-0 tk-rank">
            <li v-for="(r, i) in topNhanVien" :key="r.ten">
              <span class="tk-hang">{{ i + 1 }}</span>
              <span class="tk-ten">
                <RouterLink v-if="r.info?.id" :to="`/nhan-vien/${r.info.id}`" class="text-dark fw-semibold text-decoration-none tk-link-hover">{{ r.ten }}</RouterLink>
                <template v-else>{{ r.ten }}</template>
                <small class="text-muted d-block"><span v-if="r.info?.ma">{{ r.info.ma }} · </span>{{ r.so }} đơn</small>
              </span>
              <b>{{ formatMoney(r.v) }}</b>
            </li>
          </ol>
        </section>
      </div>

      <div class="col-12 col-md-6 col-lg-4">
        <section class="ad-card h-100 mb-0" data-test="tk-top-kh">
          <div class="d-flex align-items-center justify-content-between mb-3">
            <h3 class="fs-6 fw-bold m-0">Khách hàng mua nhiều nhất</h3>
            <RouterLink to="/khach-hang" class="small text-decoration-none">Xem tất cả</RouterLink>
          </div>
          <div v-if="!topKhach.length" class="ad-empty">Chưa có dữ liệu.</div>
          <ol v-else class="list-unstyled m-0 tk-rank">
            <li v-for="(r, i) in topKhach" :key="r.ten">
              <span class="tk-hang">{{ i + 1 }}</span>
              <span class="tk-ten">
                <RouterLink v-if="r.info?.id" :to="`/khach-hang/${r.info.id}/sua`" class="text-dark fw-semibold text-decoration-none tk-link-hover">{{ r.ten }}</RouterLink>
                <template v-else>{{ r.ten }}</template>
                <small class="text-muted d-block"><span v-if="r.info?.ma">{{ r.info.ma }} · </span>{{ r.so }} đơn</small>
              </span>
              <b>{{ formatMoney(r.v) }}</b>
            </li>
          </ol>
        </section>
      </div>

      <!-- Top Sản phẩm & Biến thể bán chạy (từ /api/thong-ke/tong-quan) -->
      <div v-if="chiTietHoaDon.length" class="col-12 col-xl-7">
        <section class="ad-card h-100 mb-0" data-test="tk-top-sp">
          <div class="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-3">
            <div>
              <h3 class="fs-6 fw-bold m-0">Sản phẩm &amp; biến thể bán chạy nhất (Chi tiết CSDL)</h3>
              <p class="text-muted small m-0">Tính từ chi tiết các hóa đơn hoàn thành trong khoảng đã chọn</p>
            </div>
            <div class="d-flex flex-wrap align-items-center gap-2">
              <input
                v-model="tuKhoaSp"
                type="search"
                class="form-control form-control-sm tk-search-input"
                placeholder="Tìm tên, mã SP, thương hiệu..."
                aria-label="Tìm sản phẩm bán chạy"
                data-test="tk-tim-sp"
              />
              <div class="btn-group btn-group-sm" role="group" aria-label="Tiêu chí xếp hạng sản phẩm">
                <button type="button" class="btn btn-sm" :class="tieuChiTopSp === 'soLuong' ? 'btn-primary' : 'btn-outline-primary'" @click="tieuChiTopSp = 'soLuong'">Theo SL</button>
                <button type="button" class="btn btn-sm" :class="tieuChiTopSp === 'doanhThu' ? 'btn-primary' : 'btn-outline-primary'" @click="tieuChiTopSp = 'doanhThu'">Theo DT</button>
              </div>
            </div>
          </div>
          <div v-if="!topSanPham.length" class="ad-empty">
            {{ tuKhoaSp ? 'Không tìm thấy sản phẩm phù hợp với từ khóa.' : 'Chưa có sản phẩm bán ra trong khoảng này.' }}
          </div>
          <div v-else class="ad-table-wrap">
            <div class="ad-table-scroll">
              <table class="ad-table">
                <thead>
                  <tr>
                    <th class="ad-col-stt">#</th>
                    <th>Sản phẩm / Biến thể</th>
                    <th>Thương hiệu</th>
                    <th class="text-center">Đã bán</th>
                    <th class="text-center">Tồn kho</th>
                    <th class="text-end">Doanh thu</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(sp, idx) in topSanPham" :key="sp.khoa">
                    <td class="ad-col-stt"><span class="tk-hang">{{ idx + 1 }}</span></td>
                    <td>
                      <RouterLink v-if="sp.idSanPham" :to="`/san-pham/${sp.idSanPham}/sua`" class="fw-semibold text-dark text-decoration-none tk-link-hover">
                        {{ sp.tenSanPham }}
                      </RouterLink>
                      <strong v-else>{{ sp.tenSanPham }}</strong>
                      <div class="small text-muted">
                        <span class="ad-code me-1">{{ sp.maBienThe || sp.maSanPham }}</span>
                        <span v-if="sp.tenBienThe">· {{ sp.tenBienThe }}</span>
                      </div>
                    </td>
                    <td class="ad-nowrap"><span class="ad-pill ad-pill-blue">{{ sp.tenThuongHieu }}</span></td>
                    <td class="text-center fw-bold">{{ sp.soLuong }} <small class="text-muted fw-normal">({{ sp.soDon }} đơn)</small></td>
                    <td class="text-center">
                      <span class="ad-pill" :class="sp.soLuongTon <= 0 ? 'ad-pill-Off' : sp.soLuongTon <= 5 ? 'ad-pill-warn' : 'ad-pill-on'">
                        {{ sp.soLuongTon }}
                      </span>
                    </td>
                    <td class="ad-nowrap text-end fw-bold text-primary">{{ formatMoney(sp.doanhThu) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </section>
      </div>

      <!-- Cảnh báo Tồn kho & Giá ưu đãi (từ /api/thong-ke/tong-quan) -->
      <div v-if="tonKhoCanhBao.length" class="col-12 col-xl-5">
        <section class="ad-card h-100 mb-0" data-test="tk-ton-kho">
          <div class="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-3">
            <div>
              <h3 class="fs-6 fw-bold m-0">Cảnh báo tồn kho &amp; giá ưu đãi</h3>
              <p class="text-muted small m-0">Các biến thể đang bán có tồn kho thấp (&le; 10)</p>
            </div>
            <RouterLink to="/san-pham/bien-the" class="small text-decoration-none">Nhập thêm kho &rarr;</RouterLink>
          </div>
          <div class="d-flex flex-wrap gap-1 mb-2" role="group" aria-label="Lọc cảnh báo tồn kho">
            <button type="button" class="btn btn-sm" :class="boLocTonKho === 'tatCa' ? 'btn-primary' : 'btn-outline-secondary'" @click="boLocTonKho = 'tatCa'">
              Tất cả ({{ tonKhoCanhBao.length }})
            </button>
            <button type="button" class="btn btn-sm" :class="boLocTonKho === 'hetHang' ? 'btn-danger' : 'btn-outline-danger'" data-test="tk-loc-het-hang" @click="boLocTonKho = 'hetHang'">
              Hết hàng ({{ tonKhoCanhBao.filter((b) => Number(b.soLuongTon) <= 0).length }})
            </button>
            <button type="button" class="btn btn-sm" :class="boLocTonKho === 'sapHet' ? 'btn-warning' : 'btn-outline-warning'" @click="boLocTonKho = 'sapHet'">
              Sắp hết &le; 5 ({{ tonKhoCanhBao.filter((b) => Number(b.soLuongTon) > 0 && Number(b.soLuongTon) <= 5).length }})
            </button>
            <button type="button" class="btn btn-sm" :class="boLocTonKho === 'dangGiam' ? 'btn-info text-white' : 'btn-outline-info'" @click="boLocTonKho = 'dangGiam'">
              Đang KM ({{ tonKhoCanhBao.filter((b) => Number(b.phanTramGiam) > 0).length }})
            </button>
          </div>
          <div v-if="!tonKhoDaLoc.length" class="ad-empty">Không có biến thể nào trong nhóm lọc này.</div>
          <div v-else class="ad-table-wrap">
            <div class="ad-table-scroll">
              <table class="ad-table">
                <thead>
                  <tr>
                    <th>Biến thể</th>
                    <th class="text-end">Giá hiện tại</th>
                    <th class="text-center">Tồn</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="b in tonKhoDaLoc" :key="b.idBienThe">
                    <td>
                      <RouterLink v-if="b.idSanPham" :to="`/san-pham/${b.idSanPham}/sua`" class="fw-semibold text-dark text-decoration-none tk-link-hover">
                        {{ b.tenSanPham }}
                      </RouterLink>
                      <div class="small text-muted">{{ b.maBienThe }} · {{ b.tenBienThe }}</div>
                    </td>
                    <td class="ad-nowrap text-end">
                      <template v-if="b.phanTramGiam > 0">
                        <div class="fw-bold text-primary">
                          {{ formatMoney(b.giaSauGiam) }}
                          <span class="ad-pill ad-pill-blue ms-1">-{{ b.phanTramGiam }}%</span>
                        </div>
                        <small class="text-muted text-decoration-line-through">{{ formatMoney(b.giaBan) }}</small>
                      </template>
                      <span v-else class="fw-semibold">{{ formatMoney(b.giaBan) }}</span>
                    </td>
                    <td class="text-center ad-nowrap">
                      <span class="ad-pill" :class="b.soLuongTon <= 0 ? 'ad-pill-Off' : b.soLuongTon <= 5 ? 'ad-pill-warn' : 'ad-pill-on'">
                        {{ b.soLuongTon <= 0 ? 'Hết hàng' : `Còn ${b.soLuongTon}` }}
                      </span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
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
          <div v-for="p in theoPt" :key="p.tenHienThi" class="mb-3">
            <div class="d-flex justify-content-between small mb-1"><span class="fw-medium">{{ p.tenHienThi }}</span><span>{{ formatMoney(p.soTien) }} · {{ pctGon(p.pct) }}</span></div>
            <div class="progress" style="height: 10px"><div class="progress-bar" :style="{ width: p.pct + '%' }"></div></div>
            <small class="text-muted">{{ p.soDonHienThi }} đơn</small>
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

      <!-- Hiệu quả Khuyến mãi: Đợt giảm giá & Phiếu giảm giá -->
      <div v-if="heThong" class="col-12">
        <section class="ad-card mb-0" data-test="tk-khuyen-mai">
          <div class="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-3">
            <div>
              <h3 class="fs-6 fw-bold m-0">Hiệu quả khuyến mãi: Đợt giảm giá &amp; Phiếu giảm giá</h3>
              <p class="text-muted small m-0">
                Trong kỳ đã áp dụng phiếu cho <strong>{{ thongKeUuDai.soDonDungPhieu }}</strong> đơn hoàn thành · Tổng giảm: <strong class="text-primary">{{ formatMoney(thongKeUuDai.tongTienGiam) }}</strong>
              </p>
            </div>
            <div class="d-flex gap-3 small">
              <RouterLink to="/dot-giam-gia" class="text-decoration-none">Quản lý đợt giảm giá &rarr;</RouterLink>
              <RouterLink to="/phieu-giam-gia" class="text-decoration-none">Quản lý phiếu giảm giá &rarr;</RouterLink>
            </div>
          </div>

          <div class="row g-3">
            <div class="col-12 col-lg-6">
              <div class="tk-subcard">
                <div class="tk-subcard-title"><i class="bi bi-tags me-1 text-primary"></i> Đợt giảm giá đang chạy &amp; sắp diễn ra</div>
                <div v-if="!heThong.dotDangChay?.length" class="text-muted small py-2">Hiện không có đợt giảm giá nào đang hoạt động.</div>
                <ul v-else class="list-unstyled m-0 tk-promo-list">
                  <li v-for="d in heThong.dotDangChay.slice(0, 5)" :key="d.id">
                    <div>
                      <RouterLink :to="`/dot-giam-gia/${d.id}`" class="ad-code me-2">{{ d.maDot }}</RouterLink>
                      <span class="fw-medium">{{ d.tenDot }}</span>
                    </div>
                    <div class="d-flex align-items-center gap-2">
                      <span class="ad-pill ad-pill-blue">-{{ d.phanTramGiamDot }}%</span>
                      <span class="ad-pill" :class="d.trangThaiText === 'Đang diễn ra' ? 'ad-pill-on' : 'ad-pill-warn'">{{ d.trangThaiText }}</span>
                    </div>
                  </li>
                </ul>
              </div>
            </div>

            <div class="col-12 col-lg-6">
              <div class="tk-subcard">
                <div class="tk-subcard-title"><i class="bi bi-ticket-perforated me-1 text-primary"></i> Phiếu giảm giá áp dụng nhiều nhất trong kỳ</div>
                <div v-if="!thongKeUuDai.topPhieu.length" class="text-muted small py-2">Chưa có đơn hoàn thành nào áp dụng phiếu giảm giá trong kỳ này.</div>
                <ul v-else class="list-unstyled m-0 tk-promo-list">
                  <li v-for="p in thongKeUuDai.topPhieu" :key="p.maPhieu">
                    <div>
                      <RouterLink v-if="p.id" :to="`/phieu-giam-gia/${p.id}/sua`" class="ad-code me-2">{{ p.maPhieu }}</RouterLink>
                      <span v-else class="ad-code me-2">{{ p.maPhieu }}</span>
                      <span class="fw-medium">{{ p.tenPhieu }}</span>
                    </div>
                    <div class="text-end small">
                      <span class="fw-semibold">{{ p.soDon }} đơn</span> · <strong class="text-primary">-{{ formatMoney(p.tongGiam) }}</strong>
                    </div>
                  </li>
                </ul>
              </div>
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
.tk-link-hover:hover { color: #0977ec !important; text-decoration: underline !important; }
.tk-search-input { width: 200px; }
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

/* Cảnh báo validate khoảng ngày */
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

/* Đối soát dòng tiền */
.tk-reconcile-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(175px, 1fr));
  gap: 0.65rem;
}
.tk-rec-box {
  padding: 0.65rem 0.85rem;
  background: #f8fafc;
  border: 1px solid #e6ecf5;
  border-radius: 10px;
}
.tk-rec-box.is-highlight {
  background: #f0f7ff;
  border-color: #bfdbfe;
}
.tk-rec-label {
  font-size: 0.75rem;
  color: #64748b;
  font-weight: 600;
}
.tk-rec-val {
  font-size: 1.05rem;
  font-weight: 700;
  line-height: 1.35;
  margin-top: 0.1rem;
}
.tk-rec-note {
  font-size: 0.72rem;
  color: #94a3b8;
}

/* Thanh liên kết CSDL */
.tk-db-strip {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
  gap: 0.75rem;
}
.tk-db-chip {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 0.9rem;
  background: #fff;
  border: 1px solid #e6ecf5;
  border-radius: 12px;
  text-decoration: none;
  color: inherit;
  transition: all 0.15s ease;
}
.tk-db-chip:hover {
  border-color: #0977ec;
  background: #f8fbff;
  transform: translateY(-1px);
}
.tk-db-chip.is-warn {
  border-color: #fde68a;
  background: #fffdf5;
}
.tk-db-icon {
  width: 2.25rem;
  height: 2.25rem;
  border-radius: 10px;
  background: #eaf3fe;
  color: #0977ec;
  display: grid;
  place-items: center;
  font-size: 1.05rem;
  flex: none;
}
.tk-db-chip.is-warn .tk-db-icon {
  background: #fef3c7;
  color: #d97706;
}
.tk-db-title {
  font-size: 0.85rem;
  font-weight: 700;
  color: #1c2530;
}
.tk-db-sub {
  font-size: 0.76rem;
  color: #64748b;
}
.tk-db-arrow {
  margin-left: auto;
  color: #94a3b8;
  font-size: 0.8rem;
}

.tk-subcard {
  padding: 0.85rem 1rem;
  background: #f8fafc;
  border: 1px solid #e8eef5;
  border-radius: 12px;
  height: 100%;
}
.tk-subcard-title {
  font-size: 0.85rem;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 0.65rem;
}
.tk-promo-list li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  padding: 0.45rem 0;
  border-bottom: 1px dashed #e2e8f0;
  font-size: 0.85rem;
}
.tk-promo-list li:last-child {
  border-bottom: 0;
  padding-bottom: 0;
}

.tk-moc-card {
  background: #fff;
  border: 1px solid #e6ecf5;
  border-radius: 12px;
  padding: 0.75rem 0.95rem;
  cursor: pointer;
  transition: all 0.15s ease;
}
.tk-moc-card:hover {
  border-color: #0977ec;
  background: #f8fbff;
}
.tk-moc-card.is-active {
  border-color: #0977ec;
  background: #f0f7ff;
  box-shadow: 0 0 0 2px rgba(9, 119, 236, 0.12);
}
.tk-moc-label {
  font-size: 0.8rem;
  font-weight: 700;
  color: #334155;
  text-transform: uppercase;
  letter-spacing: 0.02em;
}
.tk-moc-badge {
  font-size: 0.74rem;
  color: #0977ec;
  background: #eaf3fe;
  padding: 0.12rem 0.45rem;
  border-radius: 999px;
  font-weight: 600;
}
.tk-moc-money {
  font-size: 1.15rem;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.3;
}
.tk-moc-sub {
  font-size: 0.75rem;
  color: #64748b;
  margin-top: 0.15rem;
}
</style>
