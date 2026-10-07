<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import api from '../services/api'
import BaseModal from './common/BaseModal.vue'
import { useToast } from '../composables/useToast'
import { formatMoney } from '../utils/format'
import { normalizeText } from '../utils/text'

const route = useRoute()
const toast = useToast()
const idHoaDon = route.params.id

const data = ref(null)
const loading = ref(true)
const loadError = ref('')

/* ===================== Tải dữ liệu ===================== */

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await api.get(`/hoa-don/${idHoaDon}`)
    setData(res.data)
  } catch (e) {
    loadError.value = e.message || 'Không tải được hóa đơn.'
  } finally {
    loading.value = false
  }
}

function setData(d) {
  data.value = d
  // Khoảng giá mặc định: toàn bộ
  price.min = 0
  price.max = priceCeil.value
}

onMounted(load)

const hd = computed(() => data.value?.hoaDon ?? {})
const khach = computed(() => data.value?.khachHang ?? {})
const giao = computed(() => data.value?.giaoHang ?? {})
const sanPham = computed(() => data.value?.sanPham ?? [])
const thanhToan = computed(() => data.value?.thanhToan ?? [])
const lichSu = computed(() => data.value?.lichSu ?? [])
const laGiaoHang = computed(() => hd.value.loaiDon === 2)

/* ===================== Định dạng ===================== */

const pad = (n) => String(n).padStart(2, '0')
function parseDate(v) {
  if (!v) return null
  const d = new Date(v)
  return Number.isNaN(d.getTime()) ? null : d
}
const fmtTime = (v) => { const d = parseDate(v); return d ? `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}` : '' }
const fmtDay = (v) => { const d = parseDate(v); return d ? `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}` : '' }
const fmtDateTime = (v) => (parseDate(v) ? `${fmtTime(v)} ${fmtDay(v)}` : '—')
const money = (v) => formatMoney(v ?? 0)

/* ===================== Trạng thái & dòng thời gian ===================== */

const TRANG_THAI = {
  0: { label: 'Đã hủy', pill: 'ad-pill-red' },
  1: { label: 'Chờ xác nhận', pill: 'ad-pill-amber' },
  2: { label: 'Đã xác nhận', pill: 'ad-pill-blue' },
  3: { label: 'Chờ lấy hàng', pill: 'ad-pill-purple' },
  4: { label: 'Đang giao hàng', pill: 'ad-pill-blue' },
  5: { label: 'Đã giao hàng', pill: 'ad-pill-green' },
  6: { label: 'Hoàn thành', pill: 'ad-pill-green' },
}
const trangThaiHienTai = computed(() => TRANG_THAI[hd.value.trangThai] ?? { label: 'Không xác định', pill: 'ad-pill-gray' })

// Đơn giao hàng đi đủ 6 bước; đơn tại quầy: tạo đơn -> hoàn thành (thanh toán ngay)
const FLOW_GIAO_HANG = [
  { code: 1, label: 'Chờ xác nhận', icon: 'bi-hourglass-split' },
  { code: 2, label: 'Đã xác nhận', icon: 'bi-clipboard-check' },
  { code: 3, label: 'Chờ lấy hàng', icon: 'bi-box-seam' },
  { code: 4, label: 'Đang giao hàng', icon: 'bi-truck' },
  { code: 5, label: 'Đã giao hàng', icon: 'bi-house-check' },
  { code: 6, label: 'Hoàn thành', icon: 'bi-patch-check' },
]
const FLOW_TAI_QUAY = [
  { code: 1, label: 'Tạo đơn hàng', icon: 'bi-receipt' },
  { code: 6, label: 'Hoàn thành', icon: 'bi-patch-check' },
]

const timeline = computed(() => {
  const flow = laGiaoHang.value ? FLOW_GIAO_HANG : FLOW_TAI_QUAY
  const status = hd.value.trangThai
  // Lần gần nhất đạt từng trạng thái (lấy giờ + người thao tác)
  const lanCuoi = {}
  for (const ls of lichSu.value) if (ls.trangThai !== null && ls.trangThai !== undefined) lanCuoi[ls.trangThai] = ls

  let viTri = flow.findIndex((s) => s.code === status)
  if (status === 0) {
    // Đơn hủy: bước xa nhất đã đi được trước khi hủy
    viTri = 0
    flow.forEach((s, i) => { if (lanCuoi[s.code]) viTri = i })
  }

  const steps = flow.map((s, i) => {
    let state = 'todo'
    if (i < viTri || (i === viTri && (status === 0 || status === 6))) state = 'done'
    else if (i === viTri) state = 'current'
    const ls = lanCuoi[s.code]
    const time = state === 'todo' ? null : ls?.thoiGian ?? (i === 0 ? hd.value.ngayTao : null)
    return { ...s, state, time, user: state === 'todo' ? null : ls?.nguoiThaoTac }
  })

  if (status === 0) {
    // Bỏ các bước chưa đi, thêm bước "Đã hủy"
    const ls = lanCuoi[0]
    return [...steps.slice(0, viTri + 1), { code: 0, label: 'Đã hủy', icon: 'bi-x-circle', state: 'cancel', time: ls?.thoiGian, user: ls?.nguoiThaoTac }]
  }
  return steps
})

/* ===================== Đổi trạng thái ===================== */

const NUT_TIEP_THEO = {
  2: { text: 'Xác nhận đơn', icon: 'bi-check2-circle' },
  3: { text: 'Chuẩn bị hàng', icon: 'bi-box-seam' },
  4: { text: 'Giao cho vận chuyển', icon: 'bi-truck' },
  5: { text: 'Xác nhận đã giao', icon: 'bi-house-check' },
  6: { text: 'Hoàn thành đơn', icon: 'bi-patch-check' },
}

const trangThaiTiepTheo = computed(() => {
  const s = hd.value.trangThai
  if (s === 0 || s === 6 || s === undefined || s === null) return null
  return laGiaoHang.value ? s + 1 : 6
})
const nutTiepTheo = computed(() => {
  const next = trangThaiTiepTheo.value
  if (next === null) return null
  if (!laGiaoHang.value) return { text: 'Xác nhận thanh toán', icon: 'bi-cash-coin' }
  return NUT_TIEP_THEO[next]
})
const coTheHuy = computed(() => [1, 2, 3].includes(hd.value.trangThai))

const action = reactive({ open: false, trangThai: null, title: '', ghiChu: '', error: '', saving: false })

function moXacNhan(trangThai) {
  action.open = true
  action.trangThai = trangThai
  action.ghiChu = ''
  action.error = ''
  action.title = trangThai === 0 ? 'Hủy đơn hàng' : nutTiepTheo.value?.text ?? 'Cập nhật trạng thái'
}

function nguoiThaoTac() {
  try {
    return localStorage.getItem('fs-role') === 'nhan-vien' ? 'Nhân viên' : 'Quản lý'
  } catch {
    return 'Quản lý'
  }
}

async function xacNhanDoiTrangThai() {
  const ghiChu = action.ghiChu.trim()
  if (action.trangThai === 0 && !ghiChu) {
    action.error = 'Vui lòng nhập lý do hủy đơn.'
    return
  }
  if (ghiChu.length > 500) {
    action.error = 'Ghi chú tối đa 500 ký tự.'
    return
  }
  action.saving = true
  try {
    const res = await api.put(`/hoa-don/${idHoaDon}/trang-thai`, {
      trangThaiMoi: action.trangThai,
      ghiChu,
      nguoiThaoTac: nguoiThaoTac(),
    })
    setData(res.data)
    action.open = false
    toast.success(action.trangThai === 0
      ? `Đã hủy hóa đơn ${hd.value.maHoaDon}`
      : `Hóa đơn ${hd.value.maHoaDon}: ${TRANG_THAI[action.trangThai].label.toLowerCase()}`)
  } catch (e) {
    action.error = e.message || 'Không cập nhật được trạng thái.'
  } finally {
    action.saving = false
  }
}

const showLichSu = ref(false)
const lichSuMoiTruoc = computed(() => [...lichSu.value].reverse())

/* ===================== Thanh toán ===================== */

const daThanhToan = computed(() => Number(data.value?.daThanhToan ?? 0))
const conPhaiTra = computed(() => Math.max(0, Number(hd.value.thanhTien ?? 0) - daThanhToan.value))

/* ===================== Danh sách sản phẩm: lọc / sắp xếp ===================== */

const filters = reactive({ q: '', loai: '', sort: 'mac-dinh' })
const price = reactive({ min: 0, max: 0 })

const loaiSanPham = computed(() => [...new Set(sanPham.value.map((x) => x.tenDanhMuc).filter(Boolean))])
const priceCeil = computed(() => Math.max(0, ...sanPham.value.map((x) => Number(x.donGia) || 0)))
const priceStep = computed(() => (priceCeil.value > 1_000_000 ? 10_000 : 1_000))

function onMinChange(e) {
  price.min = Math.min(Number(e.target.value), price.max)
  e.target.value = price.min
}
function onMaxChange(e) {
  price.max = Math.max(Number(e.target.value), price.min)
  e.target.value = price.max
}
const minPct = computed(() => (priceCeil.value ? (price.min / priceCeil.value) * 100 : 0))
const maxPct = computed(() => (priceCeil.value ? (price.max / priceCeil.value) * 100 : 100))

const dangLoc = computed(() => !!filters.q.trim() || !!filters.loai || filters.sort !== 'mac-dinh' || price.min > 0 || price.max < priceCeil.value)
function datLaiBoLoc() {
  filters.q = ''
  filters.loai = ''
  filters.sort = 'mac-dinh'
  price.min = 0
  price.max = priceCeil.value
}

const SORTERS = {
  'gia-tang': (a, b) => a.donGia - b.donGia,
  'gia-giam': (a, b) => b.donGia - a.donGia,
  'sl-giam': (a, b) => b.soLuong - a.soLuong,
  'tien-giam': (a, b) => b.thanhTien - a.thanhTien,
  'ten-az': (a, b) => a.tenSanPham.localeCompare(b.tenSanPham, 'vi'),
}

const sanPhamHienThi = computed(() => {
  const q = normalizeText(filters.q)
  const list = sanPham.value.filter((x) => {
    if (filters.loai && x.tenDanhMuc !== filters.loai) return false
    const gia = Number(x.donGia) || 0
    // Thanh trượt nhảy theo bước nên nới thêm 1 bước ở đầu trên để không mất sản phẩm đắt nhất
    if (gia < price.min || (price.max < priceCeil.value && gia > price.max)) return false
    if (!q) return true
    const text = normalizeText([x.maSpct, x.tenSanPham, x.tenThuongHieu, x.mauSac, x.trongLuong, x.chuVi].filter(Boolean).join(' '))
    return text.includes(q)
  })
  const sorter = SORTERS[filters.sort]
  return sorter ? [...list].sort(sorter) : list
})

const tongSoLuong = computed(() => sanPham.value.reduce((s, x) => s + (Number(x.soLuong) || 0), 0))

const MAU = {
  den: '#1f2937', trang: '#ffffff', do: '#dc3545', 'xanh duong': '#0977ec', vang: '#f5b301',
  'xanh la': '#1f9d55', hong: '#e75480', cam: '#fd7e14', tim: '#6f42c1', xam: '#9ca3af', bac: '#c0c6cf', nau: '#8b5a2b',
}
const mauHex = (ten) => MAU[normalizeText(ten)] ?? '#cbd5e1'
const thongSo = (x) => [x.trongLuong, x.chuVi].filter(Boolean).join(' · ') || '—'

/* ===================== In hóa đơn ===================== */

const esc = (v) => String(v ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c])

function inHoaDon() {
  const w = window.open('', '_blank', 'width=820,height=900')
  if (!w) {
    toast.error('Trình duyệt đã chặn cửa sổ in. Hãy cho phép cửa sổ bật lên rồi thử lại.')
    return
  }
  const h = hd.value
  const rows = sanPham.value.map((x, i) => `
    <tr>
      <td class="c">${i + 1}</td>
      <td>${esc(x.tenSanPham)}<div class="m">${esc(x.maSpct)} · ${esc(x.mauSac || '')} · ${esc(thongSo(x))}</div></td>
      <td class="c">${esc(x.soLuong)}</td>
      <td class="r">${esc(money(x.donGia))}</td>
      <td class="r">${esc(money(x.thanhTien))}</td>
    </tr>`).join('')
  const nguoiNhan = laGiaoHang.value
    ? `<p><b>Người nhận:</b> ${esc(giao.value.hoTenNguoiNhan || khach.value.ten)} - ${esc(giao.value.sdtNguoiNhan || '')}</p>
       <p><b>Địa chỉ:</b> ${esc(giao.value.diaChi || '—')}</p>`
    : ''
  w.document.write(`<!doctype html><html lang="vi"><head><meta charset="utf-8"><title>${esc(h.maHoaDon)}</title>
  <style>
    body{font-family:"Segoe UI",Roboto,Arial,sans-serif;color:#18242f;margin:32px;font-size:13px}
    .top{display:flex;justify-content:space-between;align-items:flex-start;border-bottom:2px solid #0977ec;padding-bottom:12px;margin-bottom:16px}
    .top img{height:56px}
    h1{font-size:20px;margin:0 0 4px;color:#0977ec}
    p{margin:3px 0}
    table{width:100%;border-collapse:collapse;margin-top:14px}
    th{background:#e7f1fd;text-align:left;padding:8px;font-size:12px}
    td{padding:8px;border-bottom:1px solid #e6eaf0;vertical-align:top}
    .c{text-align:center}.r{text-align:right;white-space:nowrap}.m{color:#7b8794;font-size:11px;margin-top:2px}
    .sum{margin-left:auto;width:300px;margin-top:14px}
    .sum div{display:flex;justify-content:space-between;padding:4px 0}
    .sum .t{border-top:1px solid #e6eaf0;margin-top:4px;padding-top:8px;font-weight:700;font-size:15px;color:#0977ec}
    .foot{text-align:center;color:#7b8794;margin-top:28px}
  </style></head><body>
    <div class="top">
      <div><img src="${esc(location.origin)}/logo.png" alt="FootStyle"><p>Shop cầu lông FootStyle</p></div>
      <div style="text-align:right"><h1>HÓA ĐƠN BÁN HÀNG</h1>
        <p><b>Mã:</b> ${esc(h.maHoaDon)}</p><p><b>Ngày:</b> ${esc(fmtDateTime(h.ngayTao))}</p><p><b>Loại đơn:</b> ${esc(h.tenLoaiDon)}</p></div>
    </div>
    <p><b>Khách hàng:</b> ${esc(khach.value.ten)}${khach.value.sdt ? ' - ' + esc(khach.value.sdt) : ''}</p>
    ${nguoiNhan}
    <p><b>Nhân viên:</b> ${esc(h.tenNhanVien || '—')}</p>
    <table><thead><tr><th class="c">STT</th><th>Sản phẩm</th><th class="c">SL</th><th class="r">Đơn giá</th><th class="r">Thành tiền</th></tr></thead>
      <tbody>${rows}</tbody></table>
    <div class="sum">
      <div><span>Tổng tiền hàng</span><span>${esc(money(h.tongTien))}</span></div>
      <div><span>Giảm giá${h.maPhieuGiamGia ? ' (' + esc(h.maPhieuGiamGia) + ')' : ''}</span><span>- ${esc(money(h.soTienGiam))}</span></div>
      <div><span>Phí vận chuyển</span><span>${esc(money(h.phiVanChuyen))}</span></div>
      <div class="t"><span>Tổng thanh toán</span><span>${esc(money(h.thanhTien))}</span></div>
      <div><span>Đã thanh toán</span><span>${esc(money(daThanhToan.value))}</span></div>
    </div>
    <p class="foot">Cảm ơn quý khách đã mua hàng tại FootStyle!</p>
    <script>window.onload=function(){window.print()}<\/script>
  </body></html>`)
  w.document.close()
}
</script>

<template>
  <div class="ad-page hd-page">
    <!-- Đang tải / lỗi -->
    <div v-if="loading && !data" class="ad-card hd-state">
      <span class="spinner-border text-primary" aria-hidden="true"></span>
      <span>Đang tải hóa đơn…</span>
    </div>
    <div v-else-if="loadError && !data" class="ad-card">
      <div class="ad-empty">
        <i class="bi bi-exclamation-circle" aria-hidden="true"></i>
        <strong>Không tải được hóa đơn</strong>
        <span>{{ loadError }}</span>
        <div class="d-flex gap-2 mt-3">
          <RouterLink to="/hoa-don" class="ad-btn text-decoration-none"><i class="bi bi-arrow-left"></i> Về danh sách</RouterLink>
          <button type="button" class="ad-btn ad-btn-primary" @click="load"><i class="bi bi-arrow-clockwise"></i> Thử lại</button>
        </div>
      </div>
    </div>

    <template v-else-if="data">
      <!-- Hàng 1: Trạng thái đơn hàng + Thông tin đơn -->
      <div class="hd-grid hd-grid--top">
        <section class="ad-card" aria-labelledby="hd-status-title">
          <div class="hd-card-head">
            <div class="ad-card-head mb-0">
              <span class="ad-icon-box"><i class="bi bi-signpost-split" aria-hidden="true"></i></span>
              <div>
                <h2 id="hd-status-title" class="ad-card-title">Trạng thái đơn hàng</h2>
                <p class="ad-card-sub">{{ hd.maHoaDon }} · {{ hd.tenLoaiDon }}</p>
              </div>
            </div>
            <span class="ad-pill" :class="trangThaiHienTai.pill">{{ trangThaiHienTai.label }}</span>
          </div>

          <ol class="hd-timeline" aria-label="Tiến trình đơn hàng">
            <li v-for="s in timeline" :key="s.code" class="hd-step" :class="`is-${s.state}`">
              <span class="hd-step-dot"><i class="bi" :class="s.icon" aria-hidden="true"></i></span>
              <span class="hd-step-label">{{ s.label }}</span>
              <template v-if="s.time">
                <span class="hd-step-meta">{{ fmtTime(s.time) }}</span>
                <span class="hd-step-meta">{{ fmtDay(s.time) }}</span>
              </template>
              <span v-if="s.user" class="hd-step-user">{{ s.user }}</span>
            </li>
          </ol>

          <div class="hd-actions">
            <button v-if="coTheHuy" type="button" class="ad-btn hd-btn-outline-danger" @click="moXacNhan(0)">
              <i class="bi bi-x-circle" aria-hidden="true"></i> Hủy đơn
            </button>
            <button v-if="nutTiepTheo" type="button" class="ad-btn ad-btn-primary" @click="moXacNhan(trangThaiTiepTheo)">
              <i class="bi" :class="nutTiepTheo.icon" aria-hidden="true"></i> {{ nutTiepTheo.text }}
            </button>
            <button type="button" class="ad-btn" @click="showLichSu = true">
              <i class="bi bi-clock-history" aria-hidden="true"></i> Lịch sử thao tác
            </button>
          </div>
        </section>

        <section class="ad-card" aria-labelledby="hd-info-title">
          <div class="ad-card-head">
            <span class="ad-icon-box"><i class="bi bi-receipt" aria-hidden="true"></i></span>
            <h2 id="hd-info-title" class="ad-card-title">Thông tin đơn hàng</h2>
          </div>
          <dl class="hd-rows">
            <div><dt>Mã hóa đơn</dt><dd class="ad-code">{{ hd.maHoaDon }}</dd></div>
            <div><dt>Trạng thái</dt><dd><span class="ad-pill" :class="trangThaiHienTai.pill">{{ trangThaiHienTai.label }}</span></dd></div>
            <div><dt>Loại đơn</dt><dd><span class="ad-pill" :class="laGiaoHang ? 'ad-pill-purple' : 'ad-pill-blue'">{{ hd.tenLoaiDon }}</span></dd></div>
            <div><dt>Ngày tạo</dt><dd>{{ fmtDateTime(hd.ngayTao) }}</dd></div>
            <div><dt>Nhân viên</dt><dd>{{ hd.tenNhanVien || '—' }}</dd></div>
            <div><dt>Thanh toán</dt><dd>{{ hd.phuongThucThanhToan || '—' }}</dd></div>
          </dl>
        </section>
      </div>

      <!-- Hàng 2: Khách hàng / Giao hàng / Thanh toán -->
      <div class="hd-grid hd-grid--3">
        <section class="ad-card" aria-labelledby="hd-kh-title">
          <div class="ad-card-head">
            <span class="ad-icon-box"><i class="bi bi-person" aria-hidden="true"></i></span>
            <h2 id="hd-kh-title" class="ad-card-title">Thông tin khách hàng</h2>
          </div>
          <dl class="hd-rows">
            <div><dt>Tên khách hàng</dt><dd class="fw-semibold">{{ khach.ten }}</dd></div>
            <div><dt>Số điện thoại</dt><dd>{{ khach.sdt || '—' }}</dd></div>
            <div><dt>Email</dt><dd class="hd-break">{{ khach.email || 'Không có' }}</dd></div>
          </dl>
        </section>

        <section class="ad-card" aria-labelledby="hd-gh-title">
          <div class="ad-card-head">
            <span class="ad-icon-box"><i class="bi bi-geo-alt" aria-hidden="true"></i></span>
            <h2 id="hd-gh-title" class="ad-card-title">Thông tin giao hàng</h2>
          </div>
          <dl class="hd-rows">
            <template v-if="laGiaoHang">
              <div><dt>Người nhận</dt><dd class="fw-semibold">{{ giao.hoTenNguoiNhan || khach.ten }}</dd></div>
              <div><dt>SĐT nhận</dt><dd>{{ giao.sdtNguoiNhan || '—' }}</dd></div>
            </template>
            <div><dt>Địa chỉ</dt><dd class="hd-break">{{ giao.diaChi || '—' }}</dd></div>
            <div><dt>Loại đơn</dt><dd class="fw-semibold">{{ laGiaoHang ? 'Giao hàng' : 'Cửa hàng' }}</dd></div>
            <div v-if="laGiaoHang"><dt>Vận chuyển</dt><dd>{{ hd.donViVanChuyen || '—' }}</dd></div>
            <div><dt>Ghi chú</dt><dd class="hd-break">{{ hd.ghiChu || '—' }}</dd></div>
          </dl>
        </section>

        <div class="hd-pay-col">
          <section class="ad-card hd-pay-card" aria-labelledby="hd-tt-title">
            <div class="ad-card-head">
              <span class="ad-icon-box"><i class="bi bi-clock-history" aria-hidden="true"></i></span>
              <h2 id="hd-tt-title" class="ad-card-title">Lịch sử thanh toán</h2>
            </div>
            <ul v-if="thanhToan.length" class="hd-pay-list">
              <li v-for="p in thanhToan" :key="p.id" class="hd-pay-item">
                <div class="d-flex justify-content-between align-items-start gap-2">
                  <strong>{{ p.phuongThuc || 'Thanh toán' }}</strong>
                  <span class="ad-pill" :class="p.trangThai === 1 ? 'ad-pill-green' : 'ad-pill-amber'">
                    {{ p.trangThai === 1 ? 'Đã thanh toán' : 'Chờ xử lý' }}
                  </span>
                </div>
                <p class="hd-pay-desc">{{ p.moTa || '—' }}</p>
                <div class="d-flex justify-content-between align-items-end gap-2">
                  <span class="hd-pay-time">{{ fmtDateTime(p.thoiGian) }}<template v-if="p.maGiaoDich"> · {{ p.maGiaoDich }}</template></span>
                  <span class="hd-pay-money">{{ money(p.soTien) }}</span>
                </div>
              </li>
            </ul>
            <div v-else class="ad-empty py-4">
              <i class="bi bi-wallet2" aria-hidden="true"></i>
              <span>Chưa có lần thanh toán nào</span>
            </div>
          </section>
          <button type="button" class="ad-btn ad-btn-primary hd-print-btn" @click="inHoaDon">
            <i class="bi bi-printer" aria-hidden="true"></i> In hóa đơn
          </button>
        </div>
      </div>

      <!-- Hàng 3: Danh sách sản phẩm -->
      <section class="ad-card" aria-labelledby="hd-sp-title">
        <div class="ad-list-head">
          <div class="ad-card-head">
            <span class="ad-icon-box"><i class="bi bi-box-seam" aria-hidden="true"></i></span>
            <div>
              <h2 id="hd-sp-title" class="ad-card-title">Danh sách sản phẩm ({{ sanPham.length }})</h2>
              <p class="ad-card-sub">Tổng {{ tongSoLuong }} sản phẩm</p>
            </div>
          </div>
          <button v-if="dangLoc" type="button" class="ad-btn" @click="datLaiBoLoc">
            <i class="bi bi-arrow-counterclockwise" aria-hidden="true"></i> Đặt lại bộ lọc
          </button>
        </div>

        <div class="hd-filter">
          <div class="hd-filter-grid">
            <div>
              <label class="ad-label" for="hd-q">Tìm kiếm</label>
              <div class="hd-search">
                <i class="bi bi-search" aria-hidden="true"></i>
                <input id="hd-q" v-model="filters.q" type="search" class="form-control" placeholder="Tên sản phẩm, mã, màu, thông số…">
              </div>
            </div>
            <div>
              <label class="ad-label" for="hd-loai">Loại sản phẩm</label>
              <select id="hd-loai" v-model="filters.loai" class="form-select">
                <option value="">Tất cả loại</option>
                <option v-for="l in loaiSanPham" :key="l" :value="l">{{ l }}</option>
              </select>
            </div>
            <div>
              <label class="ad-label" for="hd-sort">Sắp xếp</label>
              <select id="hd-sort" v-model="filters.sort" class="form-select">
                <option value="mac-dinh">Mặc định</option>
                <option value="gia-tang">Đơn giá tăng dần</option>
                <option value="gia-giam">Đơn giá giảm dần</option>
                <option value="sl-giam">Số lượng nhiều nhất</option>
                <option value="tien-giam">Thành tiền cao nhất</option>
                <option value="ten-az">Tên A → Z</option>
              </select>
            </div>
          </div>

          <div class="hd-range">
            <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-2">
              <span class="ad-label mb-0">Khoảng giá</span>
              <span class="hd-range-val">{{ money(price.min) }} - {{ money(price.max) }} (Tối đa: {{ money(priceCeil) }})</span>
            </div>
            <div class="hd-range-track" :style="{ '--lo': minPct + '%', '--hi': maxPct + '%' }">
              <input type="range" min="0" :max="priceCeil" :step="priceStep" :value="price.min" aria-label="Giá thấp nhất" :disabled="!priceCeil" @input="onMinChange">
              <input type="range" min="0" :max="priceCeil" :step="priceStep" :value="price.max" aria-label="Giá cao nhất" :disabled="!priceCeil" @input="onMaxChange">
            </div>
          </div>
        </div>

        <div class="ad-table-wrap">
          <div class="ad-table-scroll">
            <table class="ad-table">
              <thead>
                <tr>
                  <th class="ad-col-stt">STT</th>
                  <th>Mã SPCT</th>
                  <th class="text-center">Ảnh</th>
                  <th>Sản phẩm</th>
                  <th>Màu sắc</th>
                  <th>Thông số</th>
                  <th class="text-center">Số lượng</th>
                  <th class="text-center">Giảm giá</th>
                  <th class="text-end">Đơn giá</th>
                  <th class="text-end">Thành tiền</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="!sanPhamHienThi.length">
                  <td colspan="10">
                    <div class="ad-empty">
                      <i class="bi bi-inbox" aria-hidden="true"></i>
                      <strong>{{ sanPham.length ? 'Không có sản phẩm phù hợp' : 'Hóa đơn chưa có sản phẩm' }}</strong>
                      <span v-if="sanPham.length">Thử đổi từ khóa hoặc khoảng giá.</span>
                    </div>
                  </td>
                </tr>
                <tr v-for="(x, i) in sanPhamHienThi" :key="x.id">
                  <td class="ad-col-stt">{{ i + 1 }}</td>
                  <td class="ad-code ad-nowrap">{{ x.maSpct }}</td>
                  <td class="text-center">
                    <span class="hd-thumb">
                      <img v-if="x.hinhAnh" :src="x.hinhAnh" :alt="x.tenSanPham" loading="lazy">
                      <i v-else class="bi bi-image" aria-hidden="true"></i>
                    </span>
                  </td>
                  <td class="ad-cell-wrap">
                    <div class="fw-semibold">{{ x.tenSanPham }}</div>
                    <div class="hd-sub">{{ [x.tenThuongHieu, x.tenDanhMuc].filter(Boolean).join(' · ') }}</div>
                  </td>
                  <td class="ad-nowrap">
                    <span class="hd-swatch" :style="{ background: mauHex(x.mauSac) }" aria-hidden="true"></span>{{ x.mauSac || '—' }}
                  </td>
                  <td class="ad-nowrap">{{ thongSo(x) }}</td>
                  <td class="text-center fw-semibold">{{ x.soLuong }}</td>
                  <td class="text-center">
                    <span v-if="x.phanTramGiam > 0" class="ad-pill ad-pill-red">-{{ x.phanTramGiam }}%</span>
                    <span v-else class="text-muted">—</span>
                  </td>
                  <td class="text-end ad-nowrap">
                    <div v-if="x.phanTramGiam > 0" class="hd-old-price">{{ money(x.giaGoc) }}</div>
                    <div>{{ money(x.donGia) }}</div>
                  </td>
                  <td class="text-end ad-nowrap fw-semibold hd-money">{{ money(x.thanhTien) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- Tổng tiền -->
        <div class="hd-summary">
          <div><span>Tổng tiền hàng</span><span>{{ money(hd.tongTien) }}</span></div>
          <div>
            <span>Giảm giá <span v-if="hd.maPhieuGiamGia" class="ad-pill ad-pill-blue hd-voucher" :title="hd.tenPhieuGiamGia">{{ hd.maPhieuGiamGia }}</span></span>
            <span class="text-danger">- {{ money(hd.soTienGiam) }}</span>
          </div>
          <div><span>Phí vận chuyển</span><span>{{ money(hd.phiVanChuyen) }}</span></div>
          <div class="hd-summary-total"><span>Tổng thanh toán</span><span>{{ money(hd.thanhTien) }}</span></div>
          <div><span>Đã thanh toán</span><span class="text-success fw-semibold">{{ money(daThanhToan) }}</span></div>
          <div v-if="hd.trangThai !== 0"><span>Còn phải trả</span><span :class="conPhaiTra > 0 ? 'text-danger fw-semibold' : ''">{{ money(conPhaiTra) }}</span></div>
        </div>
      </section>
    </template>

    <!-- Hộp thoại: lịch sử thao tác -->
    <BaseModal v-if="showLichSu" title="Lịch sử thao tác" size="lg" @close="showLichSu = false">
      <div class="ad-table-wrap">
        <div class="ad-table-scroll">
          <table class="ad-table">
            <thead>
              <tr>
                <th class="ad-col-stt">STT</th>
                <th>Hành động</th>
                <th>Người thao tác</th>
                <th>Thời gian</th>
                <th>Ghi chú</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!lichSuMoiTruoc.length">
                <td colspan="5"><div class="ad-empty"><i class="bi bi-clock" aria-hidden="true"></i><span>Chưa có thao tác nào</span></div></td>
              </tr>
              <tr v-for="(ls, i) in lichSuMoiTruoc" :key="ls.id">
                <td class="ad-col-stt">{{ i + 1 }}</td>
                <td><span class="ad-pill" :class="TRANG_THAI[ls.trangThai]?.pill ?? 'ad-pill-gray'">{{ ls.hanhDong }}</span></td>
                <td class="ad-nowrap">{{ ls.nguoiThaoTac || '—' }}</td>
                <td class="ad-nowrap">{{ fmtDateTime(ls.thoiGian) }}</td>
                <td class="ad-cell-wrap">{{ ls.moTa || '—' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </BaseModal>

    <!-- Hộp thoại: xác nhận đổi trạng thái / hủy -->
    <BaseModal v-if="action.open" :title="action.title" static-backdrop @close="action.open = false">
      <p class="mb-3">
        <template v-if="action.trangThai === 0">Hủy hóa đơn <strong>{{ hd.maHoaDon }}</strong>? Thao tác này không hoàn tác được.</template>
        <template v-else>Chuyển hóa đơn <strong>{{ hd.maHoaDon }}</strong> sang <strong>{{ TRANG_THAI[action.trangThai]?.label }}</strong>?</template>
      </p>
      <label class="ad-label" for="hd-ghi-chu">
        {{ action.trangThai === 0 ? 'Lý do hủy' : 'Ghi chú' }}
        <span v-if="action.trangThai === 0" class="ad-required">*</span>
      </label>
      <textarea
        id="hd-ghi-chu"
        v-model="action.ghiChu"
        class="form-control"
        :class="{ 'is-invalid': action.error }"
        rows="3"
        maxlength="500"
        :placeholder="action.trangThai === 0 ? 'Vd: Khách đổi ý, không muốn mua nữa' : 'Không bắt buộc'"
        @input="action.error = ''"
      ></textarea>
      <div v-if="action.error" class="ad-error mt-1" role="alert">{{ action.error }}</div>
      <template #footer>
        <button type="button" class="ad-btn" :disabled="action.saving" @click="action.open = false">Đóng</button>
        <button
          type="button"
          class="ad-btn"
          :class="action.trangThai === 0 ? 'ad-btn-danger' : 'ad-btn-primary'"
          :disabled="action.saving"
          @click="xacNhanDoiTrangThai"
        >
          <span v-if="action.saving" class="spinner-border spinner-border-sm" aria-hidden="true"></span>
          {{ action.trangThai === 0 ? 'Hủy đơn' : 'Xác nhận' }}
        </button>
      </template>
    </BaseModal>
  </div>
</template>

<style scoped>
.hd-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  min-height: 220px;
  color: var(--fs-muted);
}

.hd-grid {
  display: grid;
  gap: 1.5rem;
}
.hd-grid--top {
  grid-template-columns: minmax(0, 2fr) minmax(0, 1fr);
}
.hd-grid--3 {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}
@media (max-width: 1199.98px) {
  .hd-grid--top,
  .hd-grid--3 {
    grid-template-columns: minmax(0, 1fr);
  }
}

.hd-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

/* ---------- Dòng thời gian ---------- */
.hd-timeline {
  display: flex;
  margin: 0;
  padding: 0 0 0.25rem;
  list-style: none;
  overflow-x: auto;
}
.hd-step {
  position: relative;
  flex: 1 0 120px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.15rem;
  padding: 0 0.25rem;
  text-align: center;
}
/* Đường nối giữa các bước */
.hd-step:not(:first-child)::before {
  content: '';
  position: absolute;
  top: 27px;
  right: 50%;
  width: 100%;
  height: 3px;
  background: var(--fs-line);
  z-index: 0;
}
.hd-step.is-done::before,
.hd-step.is-current::before {
  background: var(--fs-primary);
}
.hd-step.is-cancel::before {
  background: var(--fs-danger);
}
.hd-step-dot {
  position: relative;
  z-index: 1;
  width: 56px;
  height: 56px;
  display: grid;
  place-items: center;
  margin-bottom: 0.55rem;
  border-radius: 50%;
  border: 2px solid var(--fs-line);
  background: #fff;
  color: #b8c0cc;
  font-size: 1.35rem;
}
.hd-step.is-done .hd-step-dot {
  border-color: var(--fs-primary);
  background: var(--fs-primary);
  color: #fff;
}
.hd-step.is-current .hd-step-dot {
  border-color: var(--fs-primary);
  background: var(--fs-primary-soft);
  color: var(--fs-primary);
  box-shadow: 0 0 0 6px rgba(9, 119, 236, 0.12);
}
.hd-step.is-cancel .hd-step-dot {
  border-color: var(--fs-danger);
  background: var(--fs-danger);
  color: #fff;
}
.hd-step-label {
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--fs-muted);
}
.hd-step.is-done .hd-step-label,
.hd-step.is-current .hd-step-label {
  color: var(--fs-text);
}
.hd-step.is-cancel .hd-step-label {
  color: var(--fs-danger);
}
.hd-step-meta {
  font-size: 0.75rem;
  color: var(--fs-muted);
}
.hd-step-user {
  font-size: 0.7rem;
  font-weight: 600;
  letter-spacing: 0.3px;
  text-transform: uppercase;
  color: var(--fs-text-2);
}

.hd-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 1.5rem;
  padding-top: 1.25rem;
  border-top: 1px dashed var(--fs-line);
}
.hd-btn-outline-danger {
  border-color: #f3c2c8;
  color: var(--fs-danger);
}
.hd-btn-outline-danger:hover:not(:disabled) {
  background: #fdecee;
  border-color: #f3c2c8;
  color: #b02a37;
}

/* ---------- Danh sách dạng nhãn - giá trị ---------- */
.hd-rows {
  margin: 0;
}
.hd-rows > div {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  padding: 0.8rem 0;
  border-bottom: 1px solid var(--fs-line-soft);
}
.hd-rows > div:last-child {
  border-bottom: 0;
}
.hd-rows dt {
  flex: none;
  font-weight: 400;
  font-size: 0.88rem;
  color: var(--fs-muted);
}
.hd-rows dd {
  margin: 0;
  text-align: right;
  font-size: 0.9rem;
  color: var(--fs-text);
}
.hd-break {
  word-break: break-word;
}

/* ---------- Thanh toán ---------- */
.hd-pay-col {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.hd-pay-card {
  flex: 1;
}
.hd-pay-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin: 0;
  padding: 0;
  list-style: none;
}
.hd-pay-item {
  padding: 0.9rem 1rem;
  border-radius: 14px;
  background: var(--fs-surface);
  border: 1px solid var(--fs-line-soft);
}
.hd-pay-item .ad-pill {
  font-size: 0.72rem;
  padding: 0.2rem 0.6rem;
}
.hd-pay-desc {
  margin: 0.3rem 0 0.5rem;
  font-size: 0.8rem;
  color: var(--fs-muted);
}
.hd-pay-time {
  font-size: 0.78rem;
  color: var(--fs-muted);
}
.hd-pay-money {
  font-weight: 700;
  color: var(--fs-primary);
  white-space: nowrap;
}
.hd-print-btn {
  width: 100%;
  height: 50px;
  font-size: 0.95rem;
}

/* ---------- Bộ lọc sản phẩm ---------- */
.hd-filter {
  padding: 1.1rem 1.25rem;
  margin-bottom: 1.25rem;
  border: 1px solid var(--fs-line-soft);
  border-radius: 16px;
  background: var(--fs-surface);
}
.hd-filter-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(0, 1fr) minmax(0, 1fr);
  gap: 1rem;
}
@media (max-width: 767.98px) {
  .hd-filter-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
.hd-filter .form-control,
.hd-filter .form-select {
  height: 44px;
  border-radius: 12px;
  border-color: var(--fs-line);
  font-size: 0.88rem;
}
.hd-filter .form-control:focus,
.hd-filter .form-select:focus {
  border-color: var(--fs-primary);
  box-shadow: 0 0 0 0.2rem rgba(9, 119, 236, 0.15);
}
.hd-search {
  position: relative;
}
.hd-search .bi {
  position: absolute;
  top: 50%;
  left: 0.9rem;
  transform: translateY(-50%);
  color: var(--fs-muted);
  pointer-events: none;
}
.hd-search .form-control {
  padding-left: 2.4rem;
}

.hd-range {
  margin-top: 1rem;
}
.hd-range-val {
  font-size: 0.8rem;
  color: var(--fs-text-2);
}
/* Thanh trượt 2 đầu: 2 input range chồng lên nhau, đoạn đã chọn tô màu xanh */
.hd-range-track {
  position: relative;
  height: 22px;
}
.hd-range-track::before,
.hd-range-track::after {
  content: '';
  position: absolute;
  top: 50%;
  height: 4px;
  border-radius: 4px;
  transform: translateY(-50%);
}
.hd-range-track::before {
  left: 0;
  right: 0;
  background: var(--fs-line);
}
.hd-range-track::after {
  left: var(--lo);
  right: calc(100% - var(--hi));
  background: var(--fs-primary);
}
.hd-range-track input {
  position: absolute;
  inset: 0;
  width: 100%;
  margin: 0;
  background: none;
  pointer-events: none;
  -webkit-appearance: none;
  appearance: none;
  z-index: 1;
}
.hd-range-track input::-webkit-slider-runnable-track {
  background: none;
}
.hd-range-track input::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 3px solid var(--fs-primary);
  background: #fff;
  box-shadow: 0 2px 6px rgba(31, 45, 61, 0.2);
  pointer-events: auto;
  cursor: pointer;
}
.hd-range-track input::-moz-range-track {
  background: none;
}
.hd-range-track input::-moz-range-thumb {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 3px solid var(--fs-primary);
  background: #fff;
  pointer-events: auto;
  cursor: pointer;
}
.hd-range-track input:focus-visible::-webkit-slider-thumb {
  outline: 2px solid var(--fs-primary);
  outline-offset: 2px;
}

/* ---------- Bảng sản phẩm ---------- */
.hd-thumb {
  width: 52px;
  height: 52px;
  display: inline-grid;
  place-items: center;
  overflow: hidden;
  border-radius: 12px;
  border: 1px solid var(--fs-line-soft);
  background: var(--fs-surface);
  color: #b8c0cc;
  font-size: 1.3rem;
}
.hd-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.hd-sub {
  font-size: 0.78rem;
  color: var(--fs-muted);
}
.hd-swatch {
  display: inline-block;
  width: 14px;
  height: 14px;
  margin-right: 0.45rem;
  vertical-align: -2px;
  border-radius: 50%;
  border: 1px solid rgba(15, 23, 42, 0.15);
}
.hd-old-price {
  font-size: 0.78rem;
  color: var(--fs-muted);
  text-decoration: line-through;
}
.hd-money {
  color: var(--fs-primary);
}

/* ---------- Tổng tiền ---------- */
.hd-summary {
  width: min(100%, 380px);
  margin: 1.25rem 0 0 auto;
  font-size: 0.9rem;
}
.hd-summary > div {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  padding: 0.45rem 0;
  color: var(--fs-text-2);
}
.hd-summary > div > span:last-child {
  color: var(--fs-text);
  white-space: nowrap;
}
.hd-summary > div > span.text-danger {
  color: var(--fs-danger);
}
.hd-summary .hd-summary-total {
  margin: 0.4rem 0;
  padding-top: 0.75rem;
  border-top: 1px solid var(--fs-line);
  font-weight: 700;
  font-size: 1.05rem;
  color: var(--fs-text);
}
.hd-summary .hd-summary-total > span:last-child {
  color: var(--fs-primary);
  font-size: 1.2rem;
}
.hd-voucher {
  margin-left: 0.35rem;
  font-size: 0.72rem;
  padding: 0.15rem 0.55rem;
}
</style>
