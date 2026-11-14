<script setup>
// Thân màn Chi tiết hóa đơn: dòng thời gian trạng thái, tổng kết thanh toán, khách hàng, giao hàng, lịch sử thanh toán, danh sách sản phẩm.
// Dùng chung cho màn admin (/hoa-don/:id) và màn đơn hàng của khách (/shop/tra-cuu) để hai màn luôn y hệt nhau
// Màn admin truyền các nút đổi trạng thái qua slot "hanh-dong"; màn khách không có nút mà hiện gợi ý bước tiếp theo.
import { computed, reactive, ref, useSlots, watch } from 'vue'
import Select2 from '../common/Select2.vue'
import VanChuyenLogo from '../common/VanChuyenLogo.vue'
import { BUOC_DA_HUY, BUOC_GIAO_THAT_BAI, LUONG_GIAO_HANG, LUONG_TAI_QUAY, TT, trangThaiOf } from '../../constants/hoaDon'
import { formatGio, formatMoney, formatNgay, formatNgayGio } from '../../utils/format'
import { moTaMucGiam } from '../../utils/hoaDonIn'
import { normalizeText } from '../../utils/text'
import { nhanDienVanChuyen } from '../../utils/vanChuyen'
import { dongTienDon, tienGiamTheoDotCuaDong } from '../../utils/tienDon'

const props = defineProps({
  data: { type: Object, required: true },
  // Màn của khách: không có nút thao tác, hiện bước tiếp theo đang chờ cửa hàng xử lý
  cheDoKhach: { type: Boolean, default: false },
})
const slots = useSlots()

const hd = computed(() => props.data?.hoaDon ?? {})
const khach = computed(() => props.data?.khachHang ?? {})
const giao = computed(() => props.data?.giaoHang ?? {})
const sanPham = computed(() => props.data?.sanPham ?? [])
const thanhToan = computed(() => props.data?.thanhToan ?? [])
const lichSu = computed(() => props.data?.lichSu ?? [])
const laGiaoHang = computed(() => hd.value.loaiDon === 2)
// Khách lẻ (không gắn hồ sơ khách hàng) và không có SĐT: không hiện dòng SĐT / email
const laKhachLe = computed(() => !!khach.value.khachLe && !khach.value.sdt)
const moTaGiam = computed(() => moTaMucGiam(hd.value))

// Thông tin giao nhận hiển thị ở chế độ khách tra cứu (gộp thành 1 bảng, bỏ thông tin trùng)
const tenHienThi = computed(() => giao.value.hoTenNguoiNhan || khach.value.ten || '—')
const sdtHienThi = computed(() => giao.value.sdtNguoiNhan || khach.value.sdt || '—')
const coNguoiDatRieng = computed(() => {
  const tenNhan = (giao.value.hoTenNguoiNhan || '').trim()
  const tenDat = (khach.value.ten || '').trim()
  const sdtNhan = (giao.value.sdtNguoiNhan || '').trim()
  const sdtDat = (khach.value.sdt || '').trim()
  return (tenDat && tenNhan && tenDat !== tenNhan) || (sdtDat && sdtNhan && sdtDat !== sdtNhan)
})

const money = (v) => formatMoney(v ?? 0)
const fmtTime = formatGio
const fmtDay = formatNgay
const fmtDateTime = formatNgayGio

// Hiển thị mức giảm giá: phần trăm "-30% (- 30.000 ₫)", số tiền "- 30.000 ₫"
const hienThiGiamGia = computed(() => {
  if (!hd.value.maPhieuGiamGia) return ''
  const isPercent = Number(hd.value.loaiPhieuGiamGia) === 1
  const pt = Number(hd.value.phanTramGiam || (isPercent ? hd.value.giaTriPhieuGiamGia : 0))
  if (pt > 0) return `-${pt}% (- ${money(hd.value.soTienGiam)})`
  return `- ${money(hd.value.soTienGiam)}`
})

// Các dòng tiền cùng tên, cùng thứ tự với bán quầy / giỏ hàng / thanh toán online / bản in (utils/tienDon.js).
// Tiền giảm theo đợt do máy chủ tính theo giá gốc LÚC BÁN (hoaDon.tienGiamTheoDot); thiếu (dữ liệu gọn của khách) thì tính từ các dòng.
const tienDon = computed(() =>
  dongTienDon({
    tienHangSauDot: hd.value.tongTien,
    giamTheoDot: hd.value.tienGiamTheoDot ?? tienGiamTheoDotCuaDong(sanPham.value),
    giamPhieu: hd.value.soTienGiam,
    maPhieu: hd.value.maPhieuGiamGia,
    coGiaoHang: laGiaoHang.value,
    phiVanChuyen: hd.value.phiVanChuyen,
    tongThanhToan: hd.value.thanhTien,
  }),
)
const dongTien = (khoa) => tienDon.value.dong.find((d) => d.khoa === khoa)

const dvVanChuyen = computed(() => nhanDienVanChuyen(hd.value.donViVanChuyen))
const logoSummaryError = ref(false)
watch(() => hd.value.donViVanChuyen, () => { logoSummaryError.value = false })

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

  // Nhánh "Giao hàng thất bại" nằm ngay sau "Đang giao hàng"
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
  // Chỉ hiển thị các bước đã đạt được hoặc đang đứng; bước tương lai chưa chuyển đến thì không hiện.
  // Màn khách không có nút "chuyển bước" như admin nên gợi ý thêm MỘT bước kế tiếp (mờ, viền đứt) để khách biết đơn sẽ đi đến đâu.
  const daQua = steps.filter((s) => s.state !== 'todo')
  if (props.cheDoKhach && status !== TT.GIAO_THAT_BAI) {
    const ke = steps.find((s) => s.state === 'todo')
    if (ke) daQua.push({ ...ke, state: 'next' })
  }
  return daQua
})

const buocTiepTheo = computed(() => timeline.value.find((s) => s.state === 'next') ?? null)

/* ===================== Thanh toán ===================== */

const daThanhToan = computed(() => Number(props.data?.daThanhToan ?? 0))
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
// Bỏ "Không áp dụng" (sản phẩm không phải vợt) để cột thông số không lặp chữ vô nghĩa
const thongSo = (x) => [x.trongLuong, x.chuVi].filter((v) => v && normalizeText(v) !== 'khong ap dung').join(' · ') || '—'
// Có giảm giá thật (đơn giá thấp hơn giá gốc) thì mới gạch giá gốc - không hiện "0%"
const coGiam = (x) => Number(x.giaGoc) > 0 && Number(x.donGia) < Number(x.giaGoc)
</script>

<template>
  <div class="hd-noi-dung">
    <!-- Hàng 1: Dòng thời gian + Tổng kết thanh toán -->
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
          <span class="ad-pill" :class="trangThaiHienTai.pill" data-testid="hd-trang-thai">{{ trangThaiHienTai.label }}</span>
        </div>

        <div v-if="data?.canHoanTien" class="alert alert-warning d-flex align-items-center gap-2 mb-3 mt-3">
          <i class="bi bi-exclamation-triangle-fill fs-5 text-warning flex-shrink-0"></i>
          <div>
            <strong>Khách đã thanh toán {{ money(data.soTienCanHoan) }} nhưng đơn đã bị hủy / hết hàng tồn kho.</strong>
            <span class="d-block small text-muted">Vui lòng liên hệ khách hàng để chuyển khoản hoàn tiền hoặc tư vấn đổi sang sản phẩm khác.</span>
          </div>
        </div>

        <ol class="hd-timeline" aria-label="Tiến trình đơn hàng">
          <li v-for="s in timeline" :key="s.code" class="hd-step" :class="[`is-${s.state}`, { 'is-latest': s.moi }]" :data-buoc="s.code">
            <span class="hd-step-dot"><i class="bi" :class="s.icon" aria-hidden="true"></i></span>
            <span class="hd-step-label">{{ s.label }}</span>
            <span v-if="s.state === 'next'" class="hd-step-meta">Bước tiếp theo</span>
            <template v-if="s.time">
              <span class="hd-step-meta">{{ fmtTime(s.time) }}</span>
              <span class="hd-step-meta">{{ fmtDay(s.time) }}</span>
            </template>
            <span v-if="s.user && !cheDoKhach" class="hd-step-user">{{ s.user }}</span>
            <span v-if="s.ghiChu && (s.state === 'fail' || s.state === 'cancel')" class="hd-step-note">{{ s.ghiChu }}</span>
          </li>
        </ol>

        <div v-if="slots['hanh-dong']" class="hd-actions">
          <slot name="hanh-dong" />
        </div>
        <p v-else-if="buocTiepTheo" class="hd-goi-y" data-testid="hd-goi-y-buoc-sau">
          <i class="bi bi-arrow-right-circle" aria-hidden="true"></i>
          <span>Đơn hàng đang <strong>{{ trangThaiHienTai.label.toLowerCase() }}</strong>, bước tiếp theo: <strong>{{ buocTiepTheo.label }}</strong>.</span>
        </p>
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
          <!-- Tổng tiền hàng (giá gốc) - Giảm theo đợt - Phiếu giảm giá + Phí vận chuyển = Tổng thanh toán -->
          <div class="hd-summary-row" data-dong="tong-tien-hang">
            <span class="hd-summary-label">{{ dongTien('tong-tien-hang').nhan }}</span>
            <span class="hd-summary-value">{{ money(dongTien('tong-tien-hang').so) }}</span>
          </div>
          <div v-if="dongTien('giam-dot')" class="hd-summary-row" data-dong="giam-dot">
            <span class="hd-summary-label">{{ dongTien('giam-dot').nhan }}</span>
            <span class="hd-summary-discount">- {{ money(dongTien('giam-dot').so) }}</span>
          </div>
          <div class="hd-summary-row" data-dong="giam-gia">
            <div class="hd-summary-left">
              <span class="hd-summary-label">{{ dongTien('phieu').nhan }}</span>
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
          <div v-if="dongTien('phi-van-chuyen')" class="hd-summary-row" data-dong="phi-van-chuyen">
            <div class="hd-summary-left">
              <span class="hd-summary-label">{{ dongTien('phi-van-chuyen').nhan }}</span>
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
            <span class="hd-summary-value">{{ dongTien('phi-van-chuyen').so > 0 ? '+ ' + money(dongTien('phi-van-chuyen').so) : 'Miễn phí' }}</span>
          </div>
          <div class="hd-summary-divider"></div>
          <div class="hd-summary-total" data-dong="tong-thanh-toan">
            <span class="hd-total-label">{{ dongTien('tong-thanh-toan').nhan }}</span>
            <span class="hd-total-amount">{{ money(hd.thanhTien) }}</span>
          </div>
          <!-- Còn phải trả (hỗ trợ kiểm tra tự động) -->
          <div v-if="hd.trangThai !== 0" class="visually-hidden">
            <span>Còn phải trả</span><span>{{ money(conPhaiTra) }}</span>
          </div>
        </div>
      </section>
    </div>

    <!-- Hàng 2: Ở chế độ khách tra cứu: 1 bảng thông tin nhận hàng gọn gàng, loại bỏ thông tin trùng -->
    <section v-if="cheDoKhach" class="ad-card hd-info-card hd-info-card--unified" aria-labelledby="hd-info-title">
      <div class="d-flex align-items-center gap-2 mb-3">
        <span class="ad-icon-box hd-icon-box--customer">
          <i class="bi bi-person-lines-fill" aria-hidden="true"></i>
        </span>
        <div>
          <h2 id="hd-info-title" class="ad-card-title mb-0">Thông tin nhận hàng</h2>
          <div class="text-muted small">Chi tiết người nhận và phương thức giao hàng</div>
        </div>
      </div>

      <div class="row g-2">
        <!-- Cột 1: Thông tin người nhận -->
        <div class="col-md-6 d-flex flex-column gap-2">
          <div class="hd-info-row">
            <span class="hd-info-label">Người nhận:</span>
            <strong class="hd-info-value text-dark">{{ tenHienThi }}</strong>
          </div>
          <div class="hd-info-row">
            <span class="hd-info-label">Số điện thoại:</span>
            <span class="hd-info-value fw-bold text-primary font-monospace">{{ sdtHienThi }}</span>
          </div>
          <div class="hd-info-row">
            <span class="hd-info-label">Email:</span>
            <span class="hd-info-value text-dark text-break">{{ khach.email || 'Không có' }}</span>
          </div>
          <div v-if="coNguoiDatRieng" class="hd-info-row">
            <span class="hd-info-label">Người đặt:</span>
            <span class="hd-info-value text-secondary">{{ khach.ten }} <span v-if="khach.sdt && khach.sdt !== sdtHienThi">({{ khach.sdt }})</span></span>
          </div>
        </div>

        <!-- Cột 2: Thông tin giao hàng & vận chuyển -->
        <div class="col-md-6 d-flex flex-column gap-2">
          <div class="hd-info-row align-items-start">
            <span class="hd-info-label flex-shrink-0 pt-0.5">Địa chỉ:</span>
            <span class="hd-info-value text-end text-break fw-medium text-dark ps-2">
              {{ giao.diaChi || (laGiaoHang ? 'Chưa có địa chỉ cụ thể' : 'Nhận hàng tại cửa hàng') }}
            </span>
          </div>
          <div class="hd-info-row">
            <span class="hd-info-label">Vận chuyển:</span>
            <span class="hd-info-value">
              <VanChuyenLogo v-if="laGiaoHang" :ten="hd.donViVanChuyen" />
              <span v-else class="badge bg-light text-dark border">Nhận tại quầy</span>
            </span>
          </div>
          <div class="hd-info-row">
            <span class="hd-info-label">Ghi chú:</span>
            <span class="hd-info-value text-dark fst-italic">{{ hd.ghiChu || '—' }}</span>
          </div>
        </div>
      </div>
    </section>

    <!-- Màn quản trị admin: giữ 3 khối riêng biệt cân đối -->
    <div v-else class="hd-grid hd-grid--3">
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

    <slot name="giua" />

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
                <th class="text-end">Đơn giá</th>
                <th class="text-end">Thành tiền</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!sanPhamHienThi.length">
                <td colspan="9">
                  <div class="ad-empty">
                    <i class="bi bi-inbox" aria-hidden="true"></i>
                    <strong>{{ sanPham.length ? 'Không có sản phẩm phù hợp' : 'Hóa đơn chưa có sản phẩm' }}</strong>
                    <span v-if="sanPham.length">Thử đổi từ khóa hoặc khoảng giá.</span>
                  </div>
                </td>
              </tr>
              <tr v-for="(x, i) in sanPhamHienThi" :key="x.id ?? i">
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
                <!-- Không ghi "Giảm giá 0%"; có giảm thì gạch giá gốc và ghi giá mới, không giảm thì chỉ một giá -->
                <td class="text-end ad-nowrap">
                  <div v-if="coGiam(x)" class="hd-old-price" data-test="hd-gia-goc">{{ money(x.giaGoc) }}</div>
                  <div :class="{ 'hd-new-price': coGiam(x) }">{{ money(x.donGia) }}</div>
                </td>
                <td class="text-end ad-nowrap fw-semibold hd-money">{{ money(x.thanhTien) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.hd-noi-dung {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
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
.hd-grid--2 {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}
.hd-grid--3 {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}
@media (max-width: 1199.98px) {
  .hd-grid--top,
  .hd-grid--2,
  .hd-grid--3 {
    grid-template-columns: minmax(0, 1fr);
  }
}

/* Card nổi bật cho màn tra cứu đơn hàng */
.hd-info-card {
  position: relative;
  border-radius: 20px;
  background: #ffffff;
  border: 1px solid rgba(15, 23, 42, 0.08);
  box-shadow: 0 10px 25px -5px rgba(15, 23, 42, 0.06), 0 8px 10px -6px rgba(15, 23, 42, 0.04);
  overflow: hidden;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.hd-info-card:hover {
  box-shadow: 0 18px 36px -8px rgba(15, 23, 42, 0.1);
}
.hd-info-card--unified,
.hd-info-card--customer {
  border-top: 4px solid var(--fs-primary);
}
.hd-info-card--delivery {
  border-top: 4px solid #10b981;
}

.hd-icon-box--unified,
.hd-icon-box--customer {
  background: rgba(9, 119, 236, 0.12) !important;
  color: var(--fs-primary) !important;
  border: 1px solid rgba(9, 119, 236, 0.2);
}
.hd-icon-box--delivery {
  background: rgba(16, 185, 129, 0.12) !important;
  color: #10b981 !important;
  border: 1px solid rgba(16, 185, 129, 0.2);
}

.hd-info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  padding: 0.75rem 1rem;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #f1f5f9;
  min-height: 48px;
  transition: background 0.15s ease, border-color 0.15s ease;
}
.hd-info-row:hover {
  background: #f1f5f9;
  border-color: #e2e8f0;
}
.hd-info-label {
  font-size: 0.88rem;
  font-weight: 500;
  color: #64748b;
  display: flex;
  align-items: center;
}
.hd-info-value {
  font-size: 0.92rem;
  text-align: right;
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
/* Bước tiếp theo (chỉ màn khách): mờ, viền đứt, đường nối đứt */
.hd-step.is-next::before {
  background: repeating-linear-gradient(90deg, var(--fs-line) 0 6px, transparent 6px 12px);
}
.hd-step.is-next .hd-step-dot {
  border-style: dashed;
  border-color: #9fb3c8;
  color: #9fb3c8;
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
/* Màn khách: thay cho các nút của admin là một dòng gợi ý bước tiếp theo, cùng vị trí */
.hd-goi-y {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin: 1.25rem 0 0;
  padding-top: 1.25rem;
  border-top: 1px dashed var(--fs-line);
  color: var(--fs-text-2);
  font-size: 0.9rem;
}
.hd-goi-y .bi {
  color: var(--fs-primary);
  font-size: 1.1rem;
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
.hd-new-price {
  color: var(--fs-danger, #dc2626);
  font-weight: 600;
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
  display: inline-flex;
  flex-direction: row;
  align-items: center;
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
