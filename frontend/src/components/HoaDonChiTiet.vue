<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import QRCode from 'qrcode'
import { useRoute } from 'vue-router'
import api from '../services/api'
import BaseModal from './common/BaseModal.vue'
import Select2 from './common/Select2.vue'
import VanChuyenLogo from './common/VanChuyenLogo.vue'
import { useToast } from '../composables/useToast'
import {
  BUOC_DA_HUY, BUOC_GIAO_THAT_BAI, LUONG_GIAO_HANG, LUONG_TAI_QUAY, TT,
  laTrangThaiCuoi, nutChuyen, trangThaiOf,
} from '../constants/hoaDon'
import { formatGio, formatMoney, formatNgay, formatNgayGio } from '../utils/format'
import { moTaMucGiam, taoHtmlHoaDon } from '../utils/hoaDonIn'
import { normalizeText } from '../utils/text'
import { nhanDienVanChuyen } from '../utils/vanChuyen'

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
}

onMounted(load)

const hd = computed(() => data.value?.hoaDon ?? {})

/* ===================== Mã QR hóa đơn ===================== */
// QR chứa mã hóa đơn (vd HD01) để quét tra cứu nhanh; tạo ngay trên trình duyệt, không cần mạng.
const qrUrl = ref('')
watch(
  () => hd.value.maHoaDon,
  async (ma) => {
    if (!ma) { qrUrl.value = ''; return }
    try {
      qrUrl.value = await QRCode.toDataURL(String(ma), { width: 240, margin: 1, errorCorrectionLevel: 'M' })
    } catch {
      qrUrl.value = ''
    }
  },
  { immediate: true },
)
const khach = computed(() => data.value?.khachHang ?? {})
const giao = computed(() => data.value?.giaoHang ?? {})
const sanPham = computed(() => data.value?.sanPham ?? [])
const thanhToan = computed(() => data.value?.thanhToan ?? [])
const lichSu = computed(() => data.value?.lichSu ?? [])
const laGiaoHang = computed(() => hd.value.loaiDon === 2)
// Khách lẻ (không gắn hồ sơ khách hàng) và không có SĐT: không hiện dòng SĐT / email (cô: "khách lẻ tại sao có số điện thoại")
const laKhachLe = computed(() => !!khach.value.khachLe && !khach.value.sdt)
const moTaGiam = computed(() => moTaMucGiam(hd.value))

// Hiển thị mức giảm giá theo yêu cầu:
// - Phần trăm: "-30% (- 30.000 ₫)"
// - Số tiền: "- 30.000 ₫"
const hienThiGiamGia = computed(() => {
  if (!hd.value.maPhieuGiamGia) return ''
  const isPercent = Number(hd.value.loaiPhieuGiamGia) === 1
  const pt = Number(hd.value.phanTramGiam || (isPercent ? hd.value.giaTriPhieuGiamGia : 0))
  if (pt > 0) {
    return `-${pt}% (- ${money(hd.value.soTienGiam)})`
  }
  return `- ${money(hd.value.soTienGiam)}`
})

const dvVanChuyen = computed(() => nhanDienVanChuyen(hd.value.donViVanChuyen))
const logoSummaryError = ref(false)
watch(() => hd.value.donViVanChuyen, () => { logoSummaryError.value = false })

/* ===================== Định dạng ===================== */

const fmtTime = formatGio
const fmtDay = formatNgay
const fmtDateTime = formatNgayGio
const money = (v) => formatMoney(v ?? 0)

/* ===================== Trạng thái & dòng thời gian ===================== */

const trangThaiHienTai = computed(() => trangThaiOf(hd.value.trangThai))

const timeline = computed(() => {
  const flow = laGiaoHang.value ? LUONG_GIAO_HANG : LUONG_TAI_QUAY
  const status = hd.value.trangThai
  // Lần gần nhất đạt từng trạng thái (lấy giờ + người thao tác); riêng "Đang giao hàng" lấy lần ĐẦU (lúc bàn giao cho vận chuyển)
  // để giờ không nhảy sau giờ "Giao hàng thất bại" khi đơn được giao lại.
  const lanCuoi = {}
  const lanDau = {}
  for (const ls of lichSu.value) {
    if (ls.trangThai === null || ls.trangThai === undefined) continue
    lanCuoi[ls.trangThai] = ls
    lanDau[ls.trangThai] ??= ls
  }
  const coThatBai = laGiaoHang.value && (status === TT.GIAO_THAT_BAI || !!lanCuoi[TT.GIAO_THAT_BAI])
  const viTriGiao = flow.findIndex((s) => s.code === TT.DANG_GIAO)

  let viTri = flow.findIndex((s) => s.code === status)
  if (status === TT.GIAO_THAT_BAI) viTri = Math.max(0, viTriGiao) // thất bại xảy ra khi đang giao
  if (status === TT.DA_HUY) {
    // Đơn hủy: bước xa nhất đã đi được trước khi hủy
    viTri = 0
    flow.forEach((s, i) => { if (lanCuoi[s.code]) viTri = i })
  }
  // Bước đang đứng đã "xong" (không còn đang chờ xử lý) khi đơn dừng ở đó
  const dungLai = status === TT.DA_HUY || status === TT.HOAN_THANH || status === TT.GIAO_THAT_BAI

  const steps = flow.map((s, i) => {
    let state = 'todo'
    if (i < viTri || (i === viTri && dungLai)) state = 'done'
    else if (i === viTri) state = 'current'
    const ls = s.code === TT.DANG_GIAO ? lanDau[s.code] : lanCuoi[s.code]
    const time = state === 'todo' ? null : ls?.thoiGian ?? (i === 0 ? hd.value.ngayTao : null)
    return { ...s, state, time, user: state === 'todo' ? null : ls?.nguoiThaoTac }
  })

  // Nhánh "Giao hàng thất bại" nằm ngay sau "Đang giao hàng" (cô: "trường hợp giao hàng thất bại chưa thể hiện")
  const chenThatBai = coThatBai && viTriGiao >= 0
  if (chenThatBai) {
    const ls = lanCuoi[TT.GIAO_THAT_BAI]
    steps.splice(viTriGiao + 1, 0, {
      ...BUOC_GIAO_THAT_BAI, state: 'fail', moi: status === TT.GIAO_THAT_BAI, time: ls?.thoiGian ?? null, user: ls?.nguoiThaoTac, ghiChu: ls?.moTa,
    })
  }

  if (status === TT.DA_HUY) {
    // Bỏ các bước chưa đi, thêm bước "Đã hủy"
    const ls = lanCuoi[TT.DA_HUY]
    const cat = viTri + 1 + (chenThatBai && viTriGiao <= viTri ? 1 : 0)
    return [...steps.slice(0, cat), { ...BUOC_DA_HUY, state: 'cancel', time: ls?.thoiGian, user: ls?.nguoiThaoTac, ghiChu: ls?.moTa }]
  }
  // Chỉ hiển thị các bước đã đạt được hoặc đang đứng; bước tương lai chưa chuyển đến thì không hiện
  return steps.filter((s) => s.state !== 'todo')
})

/* ===================== Đổi trạng thái ===================== */
// Nút nào được hiện do BACKEND quyết định (hoaDon.chuyenDuoc); đơn đã hoàn thành / đã hủy thì không còn nút nào.

const THU_TU_NUT = { nguyhiem: 0, canhbao: 1, chinh: 2 }
const dsNut = computed(() =>
  (hd.value.chuyenDuoc ?? [])
    .map((c) => ({ ...c, ...nutChuyen(hd.value.loaiDon, hd.value.trangThai, c.trangThai) }))
    .sort((a, b) => THU_TU_NUT[a.kieu] - THU_TU_NUT[b.kieu]),
)
const LOP_NUT = { chinh: 'ad-btn-primary', canhbao: 'hd-btn-outline-warning', nguyhiem: 'hd-btn-outline-danger' }
const daKhoa = computed(() => (hd.value.laTrangThaiCuoi ?? laTrangThaiCuoi(hd.value.trangThai)) === true)
const thongBaoKhoa = computed(() => {
  const ten = trangThaiHienTai.value.label.toLowerCase()
  return `Đơn đã ở trạng thái "${ten}" - là trạng thái cuối nên không thể đổi trạng thái nữa.`
})

const action = reactive({ open: false, trangThai: null, title: '', ghiChu: '', error: '', saving: false, batBuoc: false })

const NHAN_GHI_CHU = { [TT.DA_HUY]: 'Lý do hủy', [TT.GIAO_THAT_BAI]: 'Lý do giao hàng thất bại' }
const GOI_Y_GHI_CHU = {
  [TT.DA_HUY]: 'Vd: Khách đổi ý, không muốn mua nữa',
  [TT.GIAO_THAT_BAI]: 'Vd: Khách không nghe máy 3 lần / từ chối nhận hàng / sai địa chỉ',
}
const THIEU_LY_DO = { [TT.DA_HUY]: 'Vui lòng nhập lý do hủy đơn.', [TT.GIAO_THAT_BAI]: 'Vui lòng nhập lý do giao hàng thất bại.' }

function moXacNhan(trangThai) {
  const c = (hd.value.chuyenDuoc ?? []).find((x) => x.trangThai === trangThai)
  action.open = true
  action.trangThai = trangThai
  action.ghiChu = ''
  action.error = ''
  action.batBuoc = c ? !!c.batBuocLyDo : trangThai in THIEU_LY_DO
  const nut = nutChuyen(hd.value.loaiDon, hd.value.trangThai, trangThai)
  action.title = trangThai === TT.DA_HUY ? 'Hủy đơn hàng' : trangThai === TT.GIAO_THAT_BAI ? 'Báo giao hàng thất bại' : nut.text
}

function nguoiThaoTac() {
  return 'Quản lý'
}

/* ===================== In hóa đơn ===================== */

function moCuaSoIn() {
  return window.open('', '_blank', 'width=820,height=900')
}

/** Viết hóa đơn vào cửa sổ đã mở; cửa sổ tự gọi hộp thoại in (chọn "Lưu dưới dạng PDF" để có file PDF). */
function inVaoCuaSo(w) {
  w.document.write(taoHtmlHoaDon({
    hd: hd.value, khach: khach.value, giao: giao.value, sanPham: sanPham.value,
    daThanhToan: daThanhToan.value, qrUrl: qrUrl.value, goc: location.origin,
  }))
  w.document.close()
}

function inHoaDon() {
  const w = moCuaSoIn()
  if (!w) {
    toast.error('Trình duyệt đã chặn cửa sổ in. Hãy cho phép cửa sổ bật lên rồi thử lại.')
    return
  }
  inVaoCuaSo(w)
}

async function xacNhanDoiTrangThai() {
  const ghiChu = action.ghiChu.trim()
  if (action.batBuoc && !ghiChu) {
    action.error = THIEU_LY_DO[action.trangThai] ?? 'Vui lòng nhập ghi chú.'
    return
  }
  if (ghiChu.length > 500) {
    action.error = 'Ghi chú tối đa 500 ký tự.'
    return
  }
  // Chuyển sang "Đã xác nhận" thì tự mở bản in / PDF hóa đơn (cô: "chuyển sang đã xác nhận thì tự động in PDF").
  // Cửa sổ in phải mở ngay lúc người dùng bấm (trước khi chờ máy chủ) thì trình duyệt mới không chặn; có lỗi thì đóng lại.
  const tuDongIn = action.trangThai === TT.DA_XAC_NHAN
  const cuaSoIn = tuDongIn ? moCuaSoIn() : null
  action.saving = true
  try {
    const res = await api.put(`/hoa-don/${idHoaDon}/trang-thai`, {
      trangThaiMoi: action.trangThai,
      ghiChu,
      nguoiThaoTac: nguoiThaoTac(),
    })
    setData(res.data)
    action.open = false
    const ma = hd.value.maHoaDon
    if (action.trangThai === TT.DA_HUY) toast.success(`Đã hủy hóa đơn ${ma}`)
    else if (action.trangThai === TT.GIAO_THAT_BAI) toast.success(`Hóa đơn ${ma}: đã ghi nhận giao hàng thất bại`)
    else toast.success(`Hóa đơn ${ma}: ${trangThaiOf(action.trangThai).label.toLowerCase()}`)
    if (tuDongIn) {
      if (cuaSoIn) {
        inVaoCuaSo(cuaSoIn)
        toast.success('Đã mở bản in hóa đơn. Chọn "Lưu dưới dạng PDF" trong hộp thoại in để lưu file.')
      } else {
        toast.error('Đã xác nhận đơn nhưng trình duyệt chặn cửa sổ in. Cho phép cửa sổ bật lên rồi bấm "In hóa đơn".')
      }
    }
  } catch (e) {
    cuaSoIn?.close?.()
    action.error = e.message || 'Không cập nhật được trạng thái.'
  } finally {
    action.saving = false
  }
}

const showLichSu = ref(false)
const showQrModal = ref(false)
const lichSuMoiTruoc = computed(() => [...lichSu.value].reverse())

/* ===================== Thanh toán ===================== */

const daThanhToan = computed(() => Number(data.value?.daThanhToan ?? 0))
const conPhaiTra = computed(() => Math.max(0, Number(hd.value.thanhTien ?? 0) - daThanhToan.value))

/* ===================== Danh sách sản phẩm: lọc / sắp xếp ===================== */

const filters = reactive({ q: '', loai: '', sort: 'mac-dinh' })

const loaiSanPham = computed(() => [...new Set(sanPham.value.map((x) => x.tenDanhMuc).filter(Boolean))])
const dsLoaiLoc = computed(() => [{ value: '', label: 'Tất cả loại' }, ...loaiSanPham.value.map((l) => ({ value: l, label: l }))])
const DS_SAP_XEP = [
  { value: 'mac-dinh', label: 'Mặc định' },
  { value: 'gia-tang', label: 'Đơn giá tăng dần' },
  { value: 'gia-giam', label: 'Đơn giá giảm dần' },
  { value: 'sl-giam', label: 'Số lượng nhiều nhất' },
  { value: 'tien-giam', label: 'Thành tiền cao nhất' },
  { value: 'ten-az', label: 'Tên A → Z' },
]

const dangLoc = computed(() => !!filters.q.trim() || !!filters.loai || filters.sort !== 'mac-dinh')
function datLaiBoLoc() {
  filters.q = ''
  filters.loai = ''
  filters.sort = 'mac-dinh'
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
      <!-- Thanh công cụ ở trên cùng: quay lại danh sách + mã QR + in hóa đơn -->
      <div class="hd-toolbar">
        <RouterLink to="/hoa-don" class="ad-btn text-decoration-none">
          <i class="bi bi-arrow-left" aria-hidden="true"></i> Về danh sách
        </RouterLink>
        <div class="d-flex gap-2">
          <button type="button" class="ad-btn" @click="showQrModal = true">
            <i class="bi bi-qr-code" aria-hidden="true"></i> Mã QR
          </button>
          <button type="button" class="ad-btn ad-btn-primary" @click="inHoaDon">
            <i class="bi bi-printer" aria-hidden="true"></i> In hóa đơn
          </button>
        </div>
      </div>

      <!-- Hàng 1: Dòng thời gian + Thông tin cơ bản của đơn -->
      <div class="hd-grid hd-grid--top">
        <section class="ad-card" aria-labelledby="hd-status-title">
          <div class="hd-card-head">
            <div class="d-flex align-items-center gap-2">
              <span class="ad-icon-box"><i class="bi bi-card-checklist" aria-hidden="true"></i></span>
              <div>
                <h2 id="hd-status-title" class="ad-card-title mb-0">Trạng thái đơn hàng</h2>
                <p class="ad-card-sub mb-0">{{ hd.maHoaDon }} · {{ hd.tenLoaiDon }}</p>
              </div>
            </div>
            <span class="ad-pill" :class="trangThaiHienTai.pill">{{ trangThaiHienTai.label }}</span>
          </div>

          <ol class="hd-timeline" aria-label="Tiến trình đơn hàng">
            <li v-for="s in timeline" :key="s.code" class="hd-step" :class="[`is-${s.state}`, { 'is-latest': s.moi }]" :data-buoc="s.code">
              <span class="hd-step-dot"><i class="bi" :class="s.icon" aria-hidden="true"></i></span>
              <span class="hd-step-label">{{ s.label }}</span>
              <template v-if="s.time">
                <span class="hd-step-meta">{{ fmtTime(s.time) }}</span>
                <span class="hd-step-meta">{{ fmtDay(s.time) }}</span>
              </template>
              <span v-if="s.user" class="hd-step-user">{{ s.user }}</span>
              <span v-if="s.ghiChu && (s.state === 'fail' || s.state === 'cancel')" class="hd-step-note">{{ s.ghiChu }}</span>
            </li>
          </ol>

          <div class="hd-actions">
            <!-- Đã hoàn thành / Đã hủy là trạng thái cuối: khóa, không còn nút đổi trạng thái -->
            <p v-if="daKhoa" class="hd-khoa" role="note">
              <i class="bi bi-lock-fill" aria-hidden="true"></i> {{ thongBaoKhoa }}
            </p>
            <button
              v-for="n in dsNut"
              :key="n.trangThai"
              type="button"
              class="ad-btn"
              :class="LOP_NUT[n.kieu]"
              :data-chuyen="n.trangThai"
              @click="moXacNhan(n.trangThai)"
            >
              <i class="bi" :class="n.icon" aria-hidden="true"></i> {{ n.text }}
            </button>
            <button type="button" class="ad-btn" @click="showLichSu = true">
              <i class="bi bi-clock-history" aria-hidden="true"></i> Lịch sử thao tác
            </button>
          </div>
        </section>

        <!-- Card: Tổng Kết Thanh Toán -->
        <section class="ad-card hd-summary-card" aria-labelledby="hd-info-title">
          <div class="ad-card-head mb-3">
            <div class="d-flex align-items-center gap-2">
              <span class="ad-icon-box"><i class="bi bi-credit-card" aria-hidden="true"></i></span>
              <h2 id="hd-info-title" class="ad-card-title mb-0">Tổng Kết Thanh Toán</h2>
            </div>
          </div>
          <div class="hd-summary">
            <div class="hd-summary-row">
              <span class="hd-summary-label">Tổng Tiền Hàng</span>
              <span class="hd-summary-value">{{ money(hd.tongTien) }}</span>
            </div>
            <div class="hd-summary-row" data-dong="giam-gia">
              <div class="hd-summary-left">
                <span class="hd-summary-label">Phiếu Giảm Giá</span>
                <span v-if="hd.maPhieuGiamGia" class="hd-summary-code hd-voucher hd-phieu" :title="hd.tenPhieuGiamGia">
                  {{ hd.maPhieuGiamGia }}
                  <span v-if="moTaGiam" class="visually-hidden">{{ moTaGiam }}</span>
                </span>
              </div>
              <span v-if="hd.maPhieuGiamGia" class="hd-summary-discount">
                {{ hienThiGiamGia }}
              </span>
              <span v-else class="text-muted">Không dùng phiếu</span>
            </div>
            <!-- Đơn tại quầy không có vận chuyển nên không có dòng phí vận chuyển -->
            <div v-if="laGiaoHang" class="hd-summary-row" data-dong="phi-van-chuyen">
              <div class="hd-summary-left">
                <span class="hd-summary-label">Phí Vận Chuyển</span>
                <span v-if="dvVanChuyen" class="hd-summary-vc">
                  <img
                    v-if="dvVanChuyen.logo && !logoSummaryError"
                    class="hd-summary-vc-img"
                    :src="dvVanChuyen.logo"
                    :alt="`Logo ${dvVanChuyen.ma}`"
                    @error="logoSummaryError = true"
                  >
                  <span v-else class="hd-summary-vc-badge">{{ dvVanChuyen.ma || dvVanChuyen.ten }}</span>
                </span>
              </div>
              <span class="hd-summary-value">{{ hd.phiVanChuyen > 0 ? '+ ' + money(hd.phiVanChuyen) : '0 ₫' }}</span>
            </div>
            <div class="hd-summary-divider"></div>
            <div class="hd-summary-total">
              <span class="hd-total-label">Tổng Tiền</span>
              <span class="hd-total-amount">{{ money(hd.thanhTien) }}</span>
            </div>
            <!-- Còn phải trả (hỗ trợ kiểm tra tự động) -->
            <div v-if="hd.trangThai !== 0" class="visually-hidden">
              <span>Còn phải trả</span><span>{{ money(conPhaiTra) }}</span>
            </div>
          </div>
        </section>

      </div>

      <!-- Hàng 2: Khách hàng / Giao hàng / Thanh toán (3 khối cân đối) -->
      <div class="hd-grid hd-grid--3">
        <section class="ad-card" aria-labelledby="hd-kh-title">
          <div class="ad-card-head">
            <span class="ad-icon-box"><i class="bi bi-person" aria-hidden="true"></i></span>
            <h2 id="hd-kh-title" class="ad-card-title">Thông tin khách hàng</h2>
          </div>
          <dl class="hd-rows">
            <div><dt>Tên khách hàng</dt><dd class="fw-semibold">{{ khach.ten }}</dd></div>
            <!-- Khách lẻ: không lưu số điện thoại / email nên không hiện hai dòng này -->
            <div v-if="laKhachLe"><dt>Loại khách</dt><dd class="text-muted">Khách lẻ, không lưu thông tin liên hệ</dd></div>
            <template v-else>
              <div><dt>Số điện thoại</dt><dd>{{ khach.sdt || '—' }}</dd></div>
              <div><dt>Email</dt><dd class="hd-break">{{ khach.email || 'Không có' }}</dd></div>
            </template>
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
              <div><dt>Địa chỉ</dt><dd class="hd-break">{{ giao.diaChi || '—' }}</dd></div>
              <div><dt>Vận chuyển</dt><dd><VanChuyenLogo :ten="hd.donViVanChuyen" /></dd></div>
            </template>
            <!-- Đơn tại quầy: nhận hàng ngay tại cửa hàng, không có địa chỉ giao / đơn vị vận chuyển / phí vận chuyển -->
            <div v-else><dt>Hình thức</dt><dd>Nhận hàng tại cửa hàng</dd></div>
            <div><dt>Ghi chú</dt><dd class="hd-break">{{ hd.ghiChu || '—' }}</dd></div>
          </dl>
        </section>

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
      </div>

      <!-- QR ẩn hỗ trợ quét / kiểm tra tự động -->
      <div class="hd-qr d-none">
        <img v-if="qrUrl" :src="qrUrl" :alt="`Mã QR hóa đơn ${hd.maHoaDon}`" width="168" height="168">
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
              <Select2 id="hd-loai" v-model="filters.loai" :options="dsLoaiLoc" />
            </div>
            <div>
              <label class="ad-label" for="hd-sort">Sắp xếp</label>
              <Select2 id="hd-sort" v-model="filters.sort" :options="DS_SAP_XEP" />
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
                <td><span class="ad-pill" :class="trangThaiOf(ls.trangThai).pill">{{ ls.hanhDong }}</span></td>
                <td class="ad-nowrap">{{ ls.nguoiThaoTac || '—' }}</td>
                <td class="ad-nowrap">{{ fmtDateTime(ls.thoiGian) }}</td>
                <td class="ad-cell-wrap">{{ ls.moTa || '—' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </BaseModal>

    <!-- Hộp thoại: xác nhận đổi trạng thái / hủy / báo giao hàng thất bại -->
    <BaseModal v-if="action.open" :title="action.title" static-backdrop @close="action.open = false">
      <p class="mb-3" data-noi-dung="xac-nhan">
        <template v-if="action.trangThai === TT.DA_HUY">
          Hủy hóa đơn <strong>{{ hd.maHoaDon }}</strong>? Thao tác này không hoàn tác được; số lượng hàng của đơn sẽ được cộng lại vào kho.
        </template>
        <template v-else-if="action.trangThai === TT.GIAO_THAT_BAI">
          Ghi nhận hóa đơn <strong>{{ hd.maHoaDon }}</strong> giao hàng <strong>thất bại</strong>? Sau đó có thể giao lại hoặc hủy đơn.
        </template>
        <template v-else-if="hd.trangThai === TT.GIAO_THAT_BAI && action.trangThai === TT.DANG_GIAO">
          Giao lại hóa đơn <strong>{{ hd.maHoaDon }}</strong> cho đơn vị vận chuyển?
        </template>
        <template v-else>Chuyển hóa đơn <strong>{{ hd.maHoaDon }}</strong> sang <strong>{{ trangThaiOf(action.trangThai).label }}</strong>?</template>
      </p>
      <p v-if="action.trangThai === TT.DA_XAC_NHAN" class="hd-in-tu-dong">
        <i class="bi bi-printer" aria-hidden="true"></i> Sau khi xác nhận, hệ thống tự mở bản in hóa đơn (chọn "Lưu dưới dạng PDF" để lưu file).
      </p>
      <label class="ad-label" for="hd-ghi-chu">
        {{ NHAN_GHI_CHU[action.trangThai] ?? 'Ghi chú' }}
        <span v-if="action.batBuoc" class="ad-required">*</span>
      </label>
      <textarea
        id="hd-ghi-chu"
        v-model="action.ghiChu"
        class="form-control"
        :class="{ 'is-invalid': action.error }"
        rows="3"
        maxlength="500"
        :placeholder="GOI_Y_GHI_CHU[action.trangThai] ?? 'Không bắt buộc'"
        @input="action.error = ''"
      ></textarea>
      <div v-if="action.error" class="ad-error mt-1" role="alert">{{ action.error }}</div>
      <template #footer>
        <button type="button" class="ad-btn" :disabled="action.saving" @click="action.open = false">Đóng</button>
        <button
          type="button"
          class="ad-btn"
          :class="action.trangThai === TT.DA_HUY ? 'ad-btn-danger' : action.trangThai === TT.GIAO_THAT_BAI ? 'hd-btn-warning' : 'ad-btn-primary'"
          :disabled="action.saving"
          @click="xacNhanDoiTrangThai"
        >
          <span v-if="action.saving" class="spinner-border spinner-border-sm" aria-hidden="true"></span>
          {{ action.trangThai === TT.DA_HUY ? 'Hủy đơn' : action.trangThai === TT.GIAO_THAT_BAI ? 'Ghi nhận thất bại' : 'Xác nhận' }}
        </button>
      </template>
    </BaseModal>

    <!-- Hộp thoại: Mã QR hóa đơn -->
    <BaseModal v-if="showQrModal" title="Mã QR hóa đơn" size="sm" @close="showQrModal = false">
      <div class="hd-qr text-center p-3">
        <img v-if="qrUrl" :src="qrUrl" :alt="`Mã QR hóa đơn ${hd.maHoaDon}`" width="180" height="180" class="img-fluid rounded border p-1 bg-white">
        <p class="hd-qr-cap mt-2 mb-0">Quét để lấy mã <strong>{{ hd.maHoaDon }}</strong></p>
      </div>
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
  align-items: start;
}
.hd-summary-card {
  align-self: start;
}
/* Hàng 2 có 4 khối: khách hàng, giao hàng, lịch sử thanh toán, mã QR (QR hẹp hơn) */
.hd-grid--3.hd-grid--4 {
  grid-template-columns: repeat(3, minmax(0, 1fr)) minmax(0, 0.8fr);
}
@media (max-width: 1399.98px) {
  .hd-grid--3.hd-grid--4 { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
.hd-toolbar { display: flex; justify-content: space-between; align-items: center; gap: 0.75rem; margin-bottom: 1.25rem; }
.hd-qr { display: flex; flex-direction: column; align-items: center; gap: 0.5rem; }
.hd-qr img { border: 1px solid var(--fs-line); border-radius: 0.5rem; padding: 6px; background: #fff; }
.hd-qr-cap { margin: 0; font-size: 0.85rem; color: var(--fs-muted); }
.hd-grid--3 {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}
@media (max-width: 1199.98px) {
  .hd-grid--top,
  .hd-grid--3,
  .hd-grid--3.hd-grid--4 {
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
  align-items: flex-start;
  justify-content: center;
  justify-content: safe center;
  margin: 0;
  padding: 1.5rem 1rem 1.25rem;
  background: var(--fs-surface);
  border-radius: 16px;
  border: 1px solid var(--fs-line-soft);
  list-style: none;
  overflow-x: auto;
}
.hd-step {
  position: relative;
  flex: 0 1 180px;
  min-width: 130px;
  max-width: 200px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.2rem;
  padding: 0 0.5rem;
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
  transition: all 0.2s ease;
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
  box-shadow: 0 0 0 6px rgba(9, 119, 236, 0.14);
}
.hd-step.is-cancel .hd-step-dot {
  border-color: var(--fs-danger);
  background: var(--fs-danger);
  color: #fff;
}
.hd-step-label {
  font-size: 0.88rem;
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
/* Nhánh "Giao hàng thất bại": màu cam để khác với bước bình thường (xanh) và hủy (đỏ) */
.hd-step.is-fail::before {
  background: #e8751a;
}
.hd-step.is-fail .hd-step-dot {
  border-color: #e8751a;
  background: #e8751a;
  color: #fff;
}
.hd-step.is-fail.is-latest .hd-step-dot {
  box-shadow: 0 0 0 6px rgba(232, 117, 26, 0.18);
}
.hd-step.is-fail .hd-step-label {
  color: #a1400a;
}
.hd-step-note {
  max-width: 150px;
  margin-top: 0.15rem;
  font-size: 0.72rem;
  line-height: 1.25;
  color: var(--fs-text-2);
  word-break: break-word;
}
.hd-step-meta {
  font-size: 0.75rem;
  color: var(--fs-muted);
}
.hd-step-user {
  display: inline-block;
  margin-top: 0.2rem;
  padding: 2px 8px;
  border-radius: 10px;
  background: rgba(15, 23, 42, 0.05);
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
  align-items: center;
  gap: 0.75rem;
  margin-top: 1.25rem;
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
.hd-btn-outline-warning {
  border-color: #f6c9a4;
  color: #a1400a;
}
.hd-btn-outline-warning:hover:not(:disabled) {
  background: #ffe9d6;
  border-color: #f6c9a4;
  color: #8a3405;
}
.hd-btn-warning {
  border-color: #e8751a;
  background: #e8751a;
  color: #fff;
}
.hd-btn-warning:hover:not(:disabled) {
  background: #cf6310;
  border-color: #cf6310;
  color: #fff;
}
/* Trạng thái cuối: thông báo khóa thay cho các nút đổi trạng thái */
.hd-khoa {
  flex: 1 1 100%;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin: 0;
  padding: 0.6rem 0.9rem;
  border-radius: 12px;
  background: var(--fs-surface);
  border: 1px dashed var(--fs-line);
  color: var(--fs-text-2);
  font-size: 0.88rem;
}
.hd-in-tu-dong {
  margin: -0.5rem 0 1rem;
  padding: 0.55rem 0.8rem;
  border-radius: 10px;
  background: var(--fs-primary-soft);
  color: var(--fs-primary-dark);
  font-size: 0.85rem;
}
.hd-phieu {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.15rem;
}
.hd-giam-mota {
  display: block;
  margin-top: 0.1rem;
  font-size: 0.75rem;
  color: var(--fs-muted);
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

/* ---------- Tổng kết thanh toán ---------- */
.hd-summary {
  width: 100%;
  font-size: 0.92rem;
}
.hd-summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.75rem;
  padding: 0.65rem 0;
}
.hd-summary-left {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  white-space: nowrap;
}
.hd-summary-label {
  color: #64748b;
  font-size: 0.92rem;
  font-weight: 500;
  white-space: nowrap;
}
.hd-summary-code {
  font-weight: 700;
  color: #1e293b;
  font-size: 0.95rem;
  letter-spacing: 0.02em;
  white-space: nowrap;
}
.hd-summary .hd-phieu {
  display: inline-flex !important;
  flex-direction: row !important;
  align-items: center !important;
}
.hd-summary-value {
  font-weight: 600;
  color: #1e293b;
  font-size: 0.95rem;
  white-space: nowrap;
}
.hd-summary-discount {
  font-weight: 700;
  color: #10b981;
  font-size: 0.95rem;
  white-space: nowrap;
}
.hd-summary-vc {
  display: inline-flex;
  align-items: center;
}
.hd-summary-vc-img {
  height: 22px;
  max-width: 75px;
  object-fit: contain;
}
.hd-summary-vc-badge {
  display: inline-block;
  padding: 0.1rem 0.45rem;
  border: 1.5px solid var(--fs-primary);
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 800;
  letter-spacing: 0.04em;
  color: var(--fs-primary-dark);
  background: var(--fs-primary-soft);
}
.hd-summary-divider {
  height: 1px;
  background-color: #f1f5f9;
  border-top: 1px solid var(--fs-line-soft);
  margin: 0.65rem 0;
}
.hd-summary-total {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 0.2rem;
}
.hd-summary-total .hd-total-label {
  color: #b02a37;
  font-size: 1.05rem;
  font-weight: 700;
}
.hd-summary-total .hd-total-amount {
  color: #b02a37;
  font-size: 1.35rem;
  font-weight: 700;
  letter-spacing: -0.01em;
  white-space: nowrap;
}
</style>
