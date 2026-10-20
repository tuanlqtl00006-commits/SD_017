<script setup>
// Form thêm / sửa phiếu giảm giá (làm lại theo bố cục form mẫu của nhóm đầu mà cô khen):
//   thẻ "Thông tin chung"  : mã, tên, loại giảm, giá trị, giảm tối đa, đơn tối thiểu, số lượng (CHỈ phiếu công khai),
//                            ngày GIỜ bắt đầu / kết thúc + "Trạng thái dự kiến" cập nhật ngay khi chọn giờ;
//   thẻ "Đối tượng áp dụng" : Tất cả (công khai) / Nhóm chỉ định (cá nhân) -> bảng chọn khách có Tổng đơn, Mua gần nhất, bộ lọc;
//   thẻ "Quy tắc áp dụng"   : cách chồng đợt giảm giá + phiếu (ví dụ tính tiền ngay trên form) và cảnh báo phiếu trùng thời gian.
// Lỗi hiện ngay dưới từng ô (khi đã chạm vào ô hoặc sau khi bấm lưu); form SỬA kiểm tra giống hệt form THÊM
// và khớp các điều kiện của backend (PhieuGiamGiaService + ThoiGianPhut).
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import MoneyInput from '../common/MoneyInput.vue'
import Select2 from '../common/Select2.vue'
import DateTimeInput from '../common/DateTimeInput.vue'
import KhachHangChonBang from './KhachHangChonBang.vue'
import { HINH_THUC, TRANG_THAI, formatGiaTri, isExpired, taoMaPhieu, tinhTrangThai } from '../../constants/phieuGiamGia'
import { phieuGiamGiaService } from '../../services/phieuGiamGiaService'
import { dotGiamGiaService } from '../../services/dotGiamGiaService'
import { formatMoney } from '../../utils/format'
import {
  bayGioDayDu,
  bayGioIso,
  dinhDangKhoangPhut,
  dinhDangNgayGio,
  giaoNhau,
  kiemTraKhoangThoiGian,
  mocBatDau,
  mocKetThuc,
  ngayGioGuiApi,
  soPhutGiua,
} from '../../utils/ngayGio'
import { viDuDonHang } from '../../utils/tinhPhieu'
import { formatPhanTram } from '../../utils/giamGia'
import { CHE_DO_CHONG_DOT } from '../../constants/dotGiamGia'

const props = defineProps({
  item: { type: Object, default: null }, // null: tạo mới; khi sửa có kèm khachHangs (lấy từ API chi tiết)
  all: { type: Array, default: () => [] }, // toàn bộ phiếu: kiểm tra trùng mã + cảnh báo phiếu trùng thời gian
  saving: { type: Boolean, default: false },
  asPage: { type: Boolean, default: true }, // form luôn hiển thị như một trang (giữ prop cho nơi gọi cũ)
})
const emit = defineEmits(['save', 'close'])

// Giới hạn giá trị (khớp PhieuGiamGiaService)
const TIEN_GIAM_TOI_DA = 100_000_000
const DON_TOI_THIEU_TOI_DA = 1_000_000_000
const SO_LUONG_TOI_DA = 1_000_000

const isEdit = computed(() => !!props.item)
const titleText = computed(() => (isEdit.value ? 'Chỉnh sửa phiếu giảm giá' : 'Thêm mới phiếu giảm giá'))
// Phiếu đã có người dùng thì không đổi hình thức (công khai / cá nhân) và không bỏ được khách đã dùng.
const daCoNguoiDung = computed(
  () => (props.item?.soLuongDaDung ?? 0) > 0 || (props.item?.khachHangs ?? []).some((k) => k.daDung),
)
const khachDaDung = computed(() => new Set((props.item?.khachHangs ?? []).filter((k) => k.daDung).map((k) => k.id)))

const dsLoaiGiam = [
  { value: 'PHAN_TRAM', label: 'Giảm theo phần trăm (%)' },
  { value: 'TIEN_MAT', label: 'Giảm theo số tiền (₫)' },
]
const DOI_TUONG = {
  CONG_KHAI: 'Tất cả (Công khai)',
  CA_NHAN: 'Nhóm chỉ định (Cá nhân)',
}

const form = reactive(
  props.item
    ? {
        ma: props.item.ma,
        ten: props.item.ten,
        hinhThuc: props.item.hinhThuc,
        loaiGiam: props.item.loaiGiam,
        giaTri: props.item.giaTri,
        giamToiDa: props.item.giamToiDa ?? '',
        donToiThieu: props.item.donToiThieu ?? '',
        // Số lượng chỉ có ở phiếu công khai; phiếu cá nhân thì số lượng = số khách được chọn nên không nhập
        soLuong: props.item.hinhThuc === 'CONG_KHAI' ? (props.item.soLuong ?? '') : '',
        gioiHanMoiKhach: props.item.hinhThuc === 'CONG_KHAI' ? (props.item.gioiHanMoiKhach ?? '') : '',
        ngayBatDau: props.item.ngayBatDau,
        ngayKetThuc: props.item.ngayKetThuc,
        khachHangIds: (props.item.khachHangs ?? []).map((k) => k.id),
      }
    : {
        ma: taoMaPhieu(props.all.map((p) => p.ma)),
        ten: '',
        hinhThuc: 'CONG_KHAI',
        loaiGiam: 'PHAN_TRAM',
        giaTri: '',
        giamToiDa: '',
        donToiThieu: '',
        soLuong: '',
        gioiHanMoiKhach: '',
        ngayBatDau: bayGioIso(), // mặc định: bắt đầu ngay bây giờ (đổi được)
        ngayKetThuc: '',
        khachHangIds: [],
      },
)

const isPercent = computed(() => form.loaiGiam === 'PHAN_TRAM')
const isCaNhan = computed(() => form.hinhThuc === 'CA_NHAN')

/* ----- "Bây giờ" chạy theo đồng hồ (trạng thái dự kiến, khóa giờ bắt đầu, cảnh báo quá khứ cập nhật theo) ----- */
const bayGio = ref(bayGioDayDu())
const daChonBatDauThuCong = ref(false)

function onDoiNgayBatDau(v) {
  daChonBatDauThuCong.value = true
  form.ngayBatDau = v
  cham('ngayBatDau')
}

let dongHo = null
onMounted(() => {
  dongHo = setInterval(() => {
    bayGio.value = bayGioDayDu()
    if (!isEdit.value && !daChonBatDauThuCong.value) {
      const hienTai = bayGio.value.slice(0, 16)
      if (form.ngayBatDau !== hienTai) {
        form.ngayBatDau = hienTai
      }
    }
  }, 1000)
  document.getElementById('pgg-ten')?.focus()
})
onBeforeUnmount(() => clearInterval(dongHo))

const minBatDau = computed(() => (isEdit.value ? undefined : bayGio.value.slice(0, 16)))
const minKetThuc = computed(() => form.ngayBatDau || (isEdit.value ? undefined : bayGio.value.slice(0, 16)))

// Phiếu đã bắt đầu (khi SỬA) thì không đổi được giờ bắt đầu
const khoaBatDau = computed(() => isEdit.value && !!props.item?.ngayBatDau && mocBatDau(props.item.ngayBatDau) <= bayGio.value)

/* ----- Chọn khách hàng (chỉ phiếu cá nhân) ----- */
const khachHangAll = ref([])
const khachLoading = ref(false)
const khachError = ref('')

async function loadKhachHang() {
  if (khachHangAll.value.length || khachLoading.value) return
  khachLoading.value = true
  khachError.value = ''
  try {
    const ds = await phieuGiamGiaService.getKhachHang()
    // Khách đã được tặng phiếu này nhưng nay không còn "đang hoạt động" vẫn phải hiện trong bảng (để thấy / giữ đúng lựa chọn cũ)
    const daCo = new Set(ds.map((k) => k.id))
    khachHangAll.value = [...ds, ...(props.item?.khachHangs ?? []).filter((k) => !daCo.has(k.id))]
  } catch (e) {
    khachError.value = e.message || 'Không tải được danh sách khách hàng.'
  } finally {
    khachLoading.value = false
  }
}

// Phiếu cá nhân gửi mail cho khách được tặng: đếm khách có / chưa có email để báo trước khi lưu
const khachDuocChon = computed(() => khachHangAll.value.filter((k) => form.khachHangIds.includes(k.id)))
const soKhachCoEmail = computed(() => khachDuocChon.value.filter((k) => String(k.email ?? '').trim()).length)
const soKhachThieuEmail = computed(() => khachDuocChon.value.length - soKhachCoEmail.value)

/* ----- Trạng thái gửi mail (chỉ phiếu cá nhân): cảnh báo TRƯỚC khi lưu nếu Gmail chưa cấu hình / đăng nhập lỗi ----- */
const trangThaiMail = ref(null)
async function taiTrangThaiMail() {
  if (trangThaiMail.value) return
  try {
    trangThaiMail.value = await phieuGiamGiaService.trangThaiMail()
  } catch {
    trangThaiMail.value = { dangNhap: 'KHONG_RO' } // backend cũ chưa có API này: không cảnh báo
  }
}
// Sửa phiếu đã / đang diễn ra thì backend không gửi mail (chỉ phiếu còn "Sắp diễn ra" mới gửi khi sửa)
const suaKhongGuiMail = computed(() => isEdit.value && khoaBatDau.value)

// Chuyển sang Cá nhân: số lượng + giới hạn mỗi khách biến mất và số đã nhập bị xóa (số lượng = số khách được chọn)
watch(
  isCaNhan,
  (caNhan) => {
    if (caNhan) {
      form.soLuong = ''
      form.gioiHanMoiKhach = ''
      loadKhachHang()
      taiTrangThaiMail()
    }
  },
  { immediate: true },
)


function regenerateCode() {
  form.ma = taoMaPhieu(props.all.map((p) => p.ma))
}

/* ----- Kiểm tra dữ liệu (cùng một bộ luật cho THÊM và SỬA) ----- */
const chamVao = reactive({}) // ô người dùng đã nhập / rời khỏi -> mới hiện lỗi của ô đó
const submitted = ref(false)
const cham = (khoa) => {
  chamVao[khoa] = true
}
watch(() => form.ngayKetThuc, () => cham('ngayKetThuc'))
watch(() => form.khachHangIds.length, () => cham('khachHang'))

// Đổi loại giảm thì giá trị cũ hết nghĩa (10% khác 10 ₫) nên xóa để nhập lại; giảm theo số tiền thì bỏ "giảm tối đa" (chỉ dùng cho giảm theo %).
watch(
  () => form.loaiGiam,
  (value) => {
    form.giaTri = ''
    chamVao.giaTri = false
    if (value !== 'PHAN_TRAM') form.giamToiDa = ''
  },
)

function validate() {
  const e = {}
  const ma = String(form.ma).trim().toUpperCase()
  if (!ma) e.ma = 'Nhập mã phiếu.'
  else if (!/^[A-Z0-9]{4,20}$/.test(ma)) e.ma = 'Mã gồm 4-20 ký tự chữ hoặc số, không dấu, không khoảng trắng.'
  else if (!isEdit.value && props.all.some((p) => String(p.ma).toUpperCase() === ma)) e.ma = 'Mã phiếu đã tồn tại.'

  const ten = String(form.ten).trim()
  if (!ten) e.ten = 'Nhập tên phiếu.'
  else if (ten.length > 100) e.ten = 'Tên phiếu tối đa 100 ký tự.'

  const giaTri = Number(form.giaTri)
  if (form.giaTri === '' || form.giaTri === null || Number.isNaN(giaTri)) e.giaTri = 'Nhập giá trị giảm.'
  else if (isPercent.value && !Number.isInteger(giaTri)) e.giaTri = 'Phần trăm giảm phải là số nguyên từ 1 đến 100.'
  else if (isPercent.value && (giaTri < 1 || giaTri > 100)) e.giaTri = 'Phần trăm giảm từ 1 đến 100.'
  else if (!isPercent.value && giaTri < 1000) e.giaTri = 'Số tiền giảm tối thiểu 1.000 ₫.'
  else if (!isPercent.value && giaTri > TIEN_GIAM_TOI_DA) e.giaTri = 'Số tiền giảm tối đa 100.000.000 ₫.'

  const donToiThieu = Number(form.donToiThieu === '' || form.donToiThieu === null ? 0 : form.donToiThieu)
  if (Number.isNaN(donToiThieu) || donToiThieu < 0) e.donToiThieu = 'Đơn tối thiểu không được âm.'
  else if (donToiThieu > DON_TOI_THIEU_TOI_DA) e.donToiThieu = 'Đơn tối thiểu không quá 1.000.000.000 ₫.'
  else if (!isPercent.value && !e.giaTri && donToiThieu < giaTri) {
    e.donToiThieu = 'Đơn tối thiểu phải lớn hơn hoặc bằng số tiền giảm.'
  }

  if (isPercent.value && form.giamToiDa !== '' && form.giamToiDa !== null) {
    const toiDa = Number(form.giamToiDa)
    if (!(toiDa > 0)) e.giamToiDa = 'Giảm tối đa phải lớn hơn 0, hoặc để trống nếu không giới hạn.'
    else if (toiDa > TIEN_GIAM_TOI_DA) e.giamToiDa = 'Giảm tối đa không quá 100.000.000 ₫.'
    else if (donToiThieu > 0 && toiDa > donToiThieu) {
      e.giamToiDa = 'Giảm tối đa không được lớn hơn đơn tối thiểu.'
    }
  }
  if (isPercent.value && form.giamToiDa !== '' && form.giamToiDa !== null && !e.donToiThieu && donToiThieu > 0 && Number(form.giamToiDa) > donToiThieu) {
    e.donToiThieu = 'Đơn tối thiểu phải lớn hơn hoặc bằng giảm tối đa.'
  }

  if (isCaNhan.value) {
    if (!form.khachHangIds.length) e.khachHang = 'Chọn ít nhất một khách hàng để tặng phiếu.'
  } else {
    const soLuong = Number(form.soLuong)
    const daDung = props.item?.soLuongDaDung ?? 0
    if (form.soLuong === '' || form.soLuong === null) e.soLuong = 'Nhập số lượng phiếu phát hành.'
    else if (!Number.isInteger(soLuong) || soLuong < 1) e.soLuong = 'Số lượng là số nguyên từ 1 trở lên.'
    else if (soLuong > SO_LUONG_TOI_DA) e.soLuong = 'Số lượng tối đa 1.000.000 phiếu.'
    else if (soLuong < daDung) e.soLuong = `Số lượng không được nhỏ hơn số phiếu đã dùng (${daDung}).`

    if (form.gioiHanMoiKhach !== '' && form.gioiHanMoiKhach !== null) {
      const gh = Number(form.gioiHanMoiKhach)
      if (!Number.isInteger(gh) || gh < 1) e.gioiHanMoiKhach = 'Giới hạn mỗi khách là số nguyên từ 1 trở lên, hoặc để trống.'
      else if (!e.soLuong && gh > soLuong) e.gioiHanMoiKhach = 'Giới hạn mỗi khách không được lớn hơn số lượng phát hành.'
    }
  }

  const loiGio = kiemTraKhoangThoiGian({
    batDau: form.ngayBatDau,
    ketThuc: form.ngayKetThuc,
    batDauCu: props.item?.ngayBatDau ?? null,
    ketThucCu: props.item?.ngayKetThuc ?? null,
    taoMoi: !isEdit.value,
    ten: 'Phiếu',
    bayGio: bayGio.value,
  })
  if (loiGio.batDau) e.ngayBatDau = loiGio.batDau
  if (loiGio.ketThuc) e.ngayKetThuc = loiGio.ketThuc

  return e
}

const loiTatCa = computed(validate)
// Chỉ hiện lỗi của ô đã chạm vào (hoặc tất cả sau khi bấm lưu)
const errors = computed(() => {
  const kq = {}
  for (const [khoa, noiDung] of Object.entries(loiTatCa.value)) {
    if (submitted.value || chamVao[khoa]) kq[khoa] = noiDung
  }
  return kq
})

/* ----- Trạng thái dự kiến (xem trước ngay khi chọn giờ) ----- */
const khoangHopLe = computed(() => !!mocBatDau(form.ngayBatDau) && !!mocKetThuc(form.ngayKetThuc) && mocKetThuc(form.ngayKetThuc) > mocBatDau(form.ngayBatDau))
const trangThaiDuKien = computed(() => {
  if (!khoangHopLe.value) return null
  const hoatDong = isEdit.value ? props.item.hoatDong !== false : true
  return tinhTrangThai({ hoatDong, ngayBatDau: form.ngayBatDau, ngayKetThuc: form.ngayKetThuc }, bayGio.value)
})
const moTaTrangThai = computed(() => {
  const tt = trangThaiDuKien.value
  if (!tt) return ''
  if (tt === 'SAP_DIEN_RA') return `Bắt đầu lúc ${dinhDangNgayGio(form.ngayBatDau)}, còn ${dinhDangKhoangPhut(soPhutGiua(bayGio.value, form.ngayBatDau))} nữa.`
  if (tt === 'DANG_HOAT_DONG') return `Đang áp dụng, kết thúc lúc ${dinhDangNgayGio(form.ngayKetThuc)} (còn ${dinhDangKhoangPhut(soPhutGiua(bayGio.value, mocKetThuc(form.ngayKetThuc)))}).`
  if (tt === 'DA_KET_THUC') return `Đã quá giờ kết thúc ${dinhDangNgayGio(form.ngayKetThuc)}.`
  return 'Công tắc của phiếu đang tắt nên phiếu chưa áp dụng (bật lại ở danh sách phiếu).'
})

/* ----- Quy tắc chồng ưu đãi + ví dụ tính tiền + phiếu trùng thời gian ----- */
const cheDoChongDot = ref(null) // { cheDo, moTa } lấy từ backend (cấu hình app.giam-gia.che-do-chong-dot)
onMounted(async () => {
  try {
    const kq = await dotGiamGiaService.cheDo()
    cheDoChongDot.value = kq?.cheDo ? kq : null
  } catch {
    cheDoChongDot.value = null // backend cũ chưa có API này: chỉ không hiện dòng cấu hình
  }
})

// Tên dễ hiểu của cách tính đang cấu hình (vd 'Lấy mức cao nhất'), mã cấu hình để trong ngoặc cho người cài đặt đối chiếu
const tenCheDo = computed(() => CHE_DO_CHONG_DOT[cheDoChongDot.value?.cheDo]?.label || '')

const viDu = reactive({ tienHang: 2000000, phanTramDot: 10 })
const phieuDangSoan = computed(() => ({
  loaiGiam: form.loaiGiam,
  giaTri: form.giaTri === '' ? 0 : Number(form.giaTri),
  giamToiDa: form.giamToiDa === '' ? 0 : Number(form.giamToiDa),
  donToiThieu: form.donToiThieu === '' ? 0 : Number(form.donToiThieu),
}))
const ketQuaViDu = computed(() => viDuDonHang(phieuDangSoan.value, viDu.tienHang, viDu.phanTramDot))
// "VCH123 (10%, tối đa 200.000 ₫)" - hiện cả trần giảm để người xem hiểu vì sao số tiền giảm dừng ở đó
const nhanPhieuViDu = computed(() => {
  const ma = String(form.ma ?? '').trim().toUpperCase()
  const p = phieuDangSoan.value
  const giaTri = formatGiaTri(p) + (p.loaiGiam === 'PHAN_TRAM' && p.giamToiDa > 0 ? `, tối đa ${formatMoney(p.giamToiDa)}` : '')
  return `Phiếu ${ma || 'này'} (${giaTri})`
})
// Chỉ tính ví dụ khi các ô giá trị đang hợp lệ (tránh hiện số vô nghĩa như giảm 150%)
const coGiaTriDeTinh = computed(
  () => Number(form.giaTri) > 0 && !loiTatCa.value.giaTri && !loiTatCa.value.giamToiDa && !loiTatCa.value.donToiThieu,
)

// Các phiếu KHÁC đang bật, chưa kết thúc và trùng thời gian với phiếu đang soạn
const phieuTrung = computed(() => {
  if (!khoangHopLe.value) return []
  const cua = { batDau: form.ngayBatDau, ketThuc: form.ngayKetThuc }
  return props.all.filter(
    (p) => p.id !== props.item?.id && p.hoatDong && !isExpired(p, bayGio.value) && giaoNhau(cua, { batDau: p.ngayBatDau, ketThuc: p.ngayKetThuc }),
  )
})

function submit() {
  // Ô bắt đầu còn là giờ mặc định (chưa ai đụng tới): lấy lại "bây giờ" mới nhất để không bị báo quá khứ khi điền form lâu
  if (!isEdit.value && !daChonBatDauThuCong.value) form.ngayBatDau = bayGioIso()
  submitted.value = true
  bayGio.value = bayGioDayDu()
  const e = validate()
  if (Object.keys(e).length) {
    nextTick(() => {
      const o = document.querySelector('#pgg-form .is-invalid, #pgg-form [aria-invalid="true"]')
      o?.scrollIntoView?.({ block: 'center' })
      o?.focus?.()
    })
    return
  }
  emit('save', {
    ma: String(form.ma).trim().toUpperCase(),
    ten: String(form.ten).trim(),
    hinhThuc: form.hinhThuc,
    loaiGiam: form.loaiGiam,
    giaTri: Number(form.giaTri),
    giamToiDa: isPercent.value && form.giamToiDa !== '' && form.giamToiDa !== null ? Number(form.giamToiDa) : null,
    donToiThieu: Number(form.donToiThieu === '' || form.donToiThieu === null ? 0 : form.donToiThieu),
    soLuong: isCaNhan.value ? form.khachHangIds.length : Number(form.soLuong),
    gioiHanMoiKhach: !isCaNhan.value && form.gioiHanMoiKhach !== '' && form.gioiHanMoiKhach !== null ? Number(form.gioiHanMoiKhach) : null,
    ngayBatDau: ngayGioGuiApi(form.ngayBatDau, '00:00'),
    ngayKetThuc: ngayGioGuiApi(form.ngayKetThuc, '23:59'),
    khachHangIds: isCaNhan.value ? [...form.khachHangIds] : [],
  })
}
</script>

<template>
  <form id="pgg-form" class="ad-page" novalidate @submit.prevent="submit">
    <!-- ===== Thông tin chung ===== -->
    <section class="ad-card" data-testid="pgg-thong-tin-chung">
      <div class="ad-card-head">
        <span class="ad-icon-box" aria-hidden="true"><i class="bi bi-ticket-detailed"></i></span>
        <h2 class="ad-card-title">{{ titleText }}</h2>
        <span class="visually-hidden">Thông tin chung</span>
      </div>

      <div class="row g-3">
        <div class="col-md-5">
          <label class="ad-label" for="pgg-ma">Mã phiếu <span class="ad-required">*</span></label>
          <div class="ad-affix">
            <input
              id="pgg-ma"
              v-model="form.ma"
              type="text"
              class="form-control ad-control text-uppercase"
              :class="{ 'is-invalid': errors.ma, 'has-btn': !isEdit }"
              :readonly="isEdit"
              maxlength="20"
              autocomplete="off"
              :aria-invalid="!!errors.ma"
              aria-describedby="pgg-ma-msg"
              @input="cham('ma')"
              @blur="cham('ma')"
            />
            <button v-if="!isEdit" type="button" class="ad-affix-btn" title="Tạo mã khác" aria-label="Tạo mã khác" @click="regenerateCode">
              <i class="bi bi-arrow-repeat" aria-hidden="true"></i>
            </button>
          </div>
          <p v-if="errors.ma" id="pgg-ma-msg" class="ad-error">{{ errors.ma }}</p>
          <p v-else-if="isEdit" id="pgg-ma-msg" class="ad-hint">Không thể đổi mã sau khi tạo.</p>
        </div>

        <div class="col-md-7">
          <label class="ad-label" for="pgg-ten">Tên phiếu giảm giá <span class="ad-required">*</span></label>
          <input
            id="pgg-ten"
            v-model="form.ten"
            type="text"
            class="form-control ad-control"
            :class="{ 'is-invalid': errors.ten }"
            maxlength="100"
            placeholder="Ví dụ: Giảm 10% vợt Yonex"
            autocomplete="off"
            :aria-invalid="!!errors.ten"
            aria-describedby="pgg-ten-msg"
            @input="cham('ten')"
            @blur="cham('ten')"
          />
          <p v-if="errors.ten" id="pgg-ten-msg" class="ad-error">{{ errors.ten }}</p>
        </div>

        <div class="col-md-6">
          <label class="ad-label" for="pgg-loai-giam">Loại giảm <span class="ad-required">*</span></label>
          <Select2 id="pgg-loai-giam" v-model="form.loaiGiam" :options="dsLoaiGiam" class="ad-control" />
        </div>

        <div class="col-md-6">
          <label class="ad-label" for="pgg-gia-tri">Giá trị giảm <span class="ad-required">*</span></label>
          <div v-if="isPercent" class="ad-affix">
            <input
              id="pgg-gia-tri"
              v-model.number="form.giaTri"
              type="number"
              min="1"
              max="100"
              step="1"
              class="form-control ad-control has-suffix"
              :class="{ 'is-invalid': errors.giaTri }"
              placeholder="Từ 1 đến 100"
              :aria-invalid="!!errors.giaTri"
              aria-describedby="pgg-gia-tri-msg"
              @input="cham('giaTri')"
              @blur="cham('giaTri')"
            />
            <span class="ad-suffix" aria-hidden="true">%</span>
          </div>
          <MoneyInput
            v-else
            id="pgg-gia-tri"
            v-model="form.giaTri"
            placeholder="Ví dụ: 50.000"
            aria-label="Giá trị giảm"
            :invalid="!!errors.giaTri"
            @update:model-value="cham('giaTri')"
          />
          <p v-if="errors.giaTri" id="pgg-gia-tri-msg" class="ad-error">{{ errors.giaTri }}</p>
          <p v-else-if="!isPercent && form.giaTri" class="ad-hint">Giảm {{ formatMoney(form.giaTri) }} cho mỗi đơn.</p>
        </div>

        <div v-if="isPercent" class="col-md-6" data-testid="pgg-o-giam-toi-da">
          <label class="ad-label" for="pgg-giam-toi-da">Giảm tối đa</label>
          <MoneyInput
            id="pgg-giam-toi-da"
            v-model="form.giamToiDa"
            placeholder="Để trống nếu không giới hạn"
            aria-label="Giảm tối đa"
            :invalid="!!errors.giamToiDa"
            @update:model-value="cham('giamToiDa')"
          />
          <p v-if="errors.giamToiDa" id="pgg-giam-toi-da-msg" class="ad-error">{{ errors.giamToiDa }}</p>
        </div>

        <div class="col-md-6">
          <label class="ad-label" for="pgg-don-toi-thieu">Đơn hàng tối thiểu</label>
          <MoneyInput
            id="pgg-don-toi-thieu"
            v-model="form.donToiThieu"
            placeholder="Để trống nếu không yêu cầu"
            aria-label="Đơn tối thiểu"
            :invalid="!!errors.donToiThieu"
            @update:model-value="cham('donToiThieu')"
          />
          <p v-if="errors.donToiThieu" id="pgg-don-toi-thieu-msg" class="ad-error">{{ errors.donToiThieu }}</p>
        </div>

        <!-- Số lượng + giới hạn mỗi khách: CHỈ phiếu công khai (chuyển sang cá nhân thì ẩn và xóa số đã nhập) -->
        <template v-if="!isCaNhan">
          <div class="col-md-6" data-testid="pgg-o-so-luong">
            <label class="ad-label" for="pgg-so-luong">Số lượng phát hành <span class="ad-required">*</span></label>
            <input
              id="pgg-so-luong"
              v-model.number="form.soLuong"
              type="number"
              :min="Math.max(1, item?.soLuongDaDung ?? 1)"
              :max="SO_LUONG_TOI_DA"
              step="1"
              class="form-control ad-control"
              :class="{ 'is-invalid': errors.soLuong }"
              placeholder="Nhập số lượng phiếu phát hành…"
              :aria-invalid="!!errors.soLuong"
              aria-describedby="pgg-so-luong-msg"
              @input="cham('soLuong')"
              @blur="cham('soLuong')"
            />
            <p v-if="errors.soLuong" id="pgg-so-luong-msg" class="ad-error">{{ errors.soLuong }}</p>
          </div>

          <div class="col-md-6">
            <label class="ad-label" for="pgg-gioi-han">Giới hạn mỗi khách</label>
            <input
              id="pgg-gioi-han"
              v-model.number="form.gioiHanMoiKhach"
              type="number"
              min="1"
              step="1"
              class="form-control ad-control"
              :class="{ 'is-invalid': errors.gioiHanMoiKhach }"
              placeholder="Để trống nếu không giới hạn"
              :aria-invalid="!!errors.gioiHanMoiKhach"
              aria-describedby="pgg-gioi-han-msg"
              @input="cham('gioiHanMoiKhach')"
              @blur="cham('gioiHanMoiKhach')"
            />
            <p v-if="errors.gioiHanMoiKhach" id="pgg-gioi-han-msg" class="ad-error">{{ errors.gioiHanMoiKhach }}</p>
          </div>
        </template>

        <div class="col-md-6">
          <label class="ad-label" for="pgg-ngay-bd">Ngày giờ bắt đầu <span class="ad-required">*</span></label>
          <DateTimeInput
            id="pgg-ngay-bd"
            :model-value="form.ngayBatDau"
            :min="minBatDau"
            :invalid="!!errors.ngayBatDau"
            :disabled="khoaBatDau"
            default-time="00:00"
            @update:model-value="onDoiNgayBatDau"
          />
          <p v-if="errors.ngayBatDau" id="pgg-ngay-bd-msg" class="ad-error">{{ errors.ngayBatDau }}</p>
          <p v-else-if="khoaBatDau" class="ad-hint" data-testid="pgg-khoa-bat-dau">Phiếu đã bắt đầu nên không đổi được ngày giờ bắt đầu.</p>
        </div>

        <div class="col-md-6">
          <label class="ad-label" for="pgg-ngay-kt">Ngày giờ kết thúc <span class="ad-required">*</span></label>
          <DateTimeInput
            id="pgg-ngay-kt"
            v-model="form.ngayKetThuc"
            :min="minKetThuc"
            :invalid="!!errors.ngayKetThuc"
            :show-now="false"
            default-time="23:59"
          />
          <p v-if="errors.ngayKetThuc" id="pgg-ngay-kt-msg" class="ad-error">{{ errors.ngayKetThuc }}</p>
        </div>

      </div>
    </section>

    <!-- ===== Đối tượng áp dụng ===== -->
    <section class="ad-card" data-testid="pgg-doi-tuong">
      <div class="ad-card-head">
        <h2 class="ad-card-title">Đối tượng áp dụng</h2>
      </div>

      <div class="pgg-radios" role="radiogroup" aria-label="Đối tượng áp dụng">
        <label v-for="(nhan, key) in DOI_TUONG" :key="key" class="pgg-radio" :class="{ 'is-chon': form.hinhThuc === key, 'is-khoa': isEdit && daCoNguoiDung }">
          <input v-model="form.hinhThuc" type="radio" name="hinhThuc" :value="key" :disabled="isEdit && daCoNguoiDung" />
          <span>{{ nhan }}</span>
        </label>
      </div>
      <p v-if="isEdit && daCoNguoiDung" class="ad-hint">Phiếu đã có người dùng nên không đổi được đối tượng áp dụng.</p>
      <p v-else-if="!isCaNhan" class="ad-hint">Mọi khách hàng đều dùng được cho tới khi hết số lượng phát hành.</p>
      <p v-else class="ad-hint">Chỉ những khách hàng được chọn mới nhận phiếu (qua email) và dùng được 1 lần. Số lượng phiếu tự bằng số khách đã chọn nên không cần nhập.</p>

      <div v-if="isCaNhan" class="pgg-khach-khung">
        <KhachHangChonBang
          v-model="form.khachHangIds"
          :khach-hangs="khachHangAll"
          :da-dung="khachDaDung"
          :dang-tai="khachLoading"
          :loi="khachError"
          :invalid="!!errors.khachHang"
        />
        <p v-if="errors.khachHang" class="ad-error" data-testid="pgg-loi-khach">{{ errors.khachHang }}</p>
        <p v-else-if="form.khachHangIds.length && suaKhongGuiMail" class="ad-hint">
          <i class="bi bi-info-circle" aria-hidden="true"></i>
          Phiếu đã bắt đầu nên lưu thay đổi sẽ không gửi mail cho khách (chỉ phiếu còn "Sắp diễn ra" mới gửi mail khi sửa).
        </p>
        <p v-else-if="form.khachHangIds.length" class="ad-hint" data-testid="pgg-mail-note">
          <i class="bi bi-envelope-paper" aria-hidden="true"></i>
          Khi lưu, hệ thống gửi mail tặng phiếu cho {{ soKhachCoEmail }} khách đã chọn<template v-if="soKhachThieuEmail"> ({{ soKhachThieuEmail }} khách chưa có email nên không nhận được mail)</template>.
        </p>
        <p v-if="trangThaiMail?.dangNhap === 'CHUA_CAU_HINH'" class="pgg-mail-status is-warn" role="status" data-testid="pgg-mail-status">
          <i class="bi bi-exclamation-triangle" aria-hidden="true"></i>
          Chưa cấu hình Gmail: phiếu vẫn lưu bình thường nhưng mail <strong>chưa được gửi thật</strong>. Cách bật: điền email + mật khẩu ứng dụng vào <code>backend/src/main/resources/mail.properties</code> rồi chạy lại backend.
        </p>
        <p v-else-if="trangThaiMail?.dangNhap === 'THAT_BAI'" class="pgg-mail-status is-error" role="alert" data-testid="pgg-mail-status">
          <i class="bi bi-x-octagon" aria-hidden="true"></i>
          Gmail đăng nhập thử thất bại: {{ trangThaiMail.lyDo || 'xem log backend' }}. Phiếu vẫn lưu được nhưng mail sẽ không tới khách.
        </p>
        <p v-else-if="trangThaiMail?.dangNhap === 'THANH_CONG'" class="pgg-mail-status is-ok" data-testid="pgg-mail-status">
          <i class="bi bi-check-circle" aria-hidden="true"></i> Gmail đã sẵn sàng gửi mail.
        </p>
      </div>
    </section>



    <div class="pgg-footer">
      <button type="submit" class="ad-btn ad-btn-primary" :disabled="saving" data-testid="pgg-luu">
        <span v-if="saving" class="spinner-border spinner-border-sm" aria-hidden="true"></span>
        {{ isEdit ? 'Lưu thay đổi' : 'Thêm mới phiếu giảm giá' }}
      </button>
      <button type="button" class="ad-btn" :disabled="saving" @click="emit('close')">Hủy bỏ</button>
    </div>
  </form>
</template>

<style scoped>
.pgg-du-kien {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.6rem;
  font-size: 0.85rem;
}
.pgg-du-kien-nhan {
  font-weight: 600;
  color: var(--fs-text);
}
.pgg-du-kien-mo-ta {
  color: var(--fs-text-2);
}
.pgg-quy-uoc-gio {
  margin-top: 0 !important;
}
.pgg-radios {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin-bottom: 0.4rem;
}
.pgg-radio {
  display: inline-flex;
  align-items: center;
  gap: 0.55rem;
  margin: 0;
  padding: 0.65rem 1rem;
  border: 1px solid #dce3ec;
  border-radius: 12px;
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--fs-text);
  cursor: pointer;
  background: #fff;
}
.pgg-radio:hover:not(.is-khoa) {
  background: var(--fs-hover-bg);
}
.pgg-radio.is-chon {
  border-color: var(--fs-primary);
  background: var(--fs-primary-soft);
  color: var(--fs-primary-dark);
}
.pgg-radio.is-khoa {
  cursor: not-allowed;
  opacity: 0.7;
}
.pgg-khach-khung {
  margin-top: 1rem;
}
.pgg-mail-status {
  margin: 0.5rem 0 0;
  padding: 0.5rem 0.75rem;
  border-radius: 10px;
  font-size: 0.85rem;
}
.pgg-mail-status.is-warn { background: #fff7e6; color: #8a5a00; border: 1px solid #f3d9a4; }
.pgg-mail-status.is-error { background: #fdecee; color: #a61b29; border: 1px solid #f3c2c8; }
.pgg-mail-status.is-ok { background: #e9f7ef; color: #146c43; border: 1px solid #b7e2c8; }
.pgg-quy-tac {
  margin: 0 0 1rem;
  padding-left: 1.2rem;
  font-size: 0.88rem;
  color: var(--fs-text);
  display: grid;
  gap: 0.4rem;
}
.pgg-che-do {
  display: inline-block;
  margin-left: 0.3rem;
  padding: 0.05rem 0.5rem;
  border-radius: 999px;
  background: var(--fs-primary-soft);
  color: var(--fs-primary-dark);
  font-size: 0.74rem;
  font-weight: 600;
}
.pgg-vi-du {
  padding: 1rem;
  border: 1px dashed var(--fs-primary-line);
  border-radius: 14px;
  background: #fafcff;
}
.pgg-vi-du-tieu-de {
  margin: 0 0 0.75rem;
  font-size: 0.92rem;
  font-weight: 600;
}
.pgg-bang-tinh {
  margin: 1rem 0 0;
  display: grid;
  gap: 0.35rem;
  font-size: 0.88rem;
}
.pgg-bang-tinh > div {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
}
.pgg-bang-tinh dt {
  font-weight: 500;
  color: var(--fs-text-2);
}
.pgg-bang-tinh dd {
  margin: 0;
  font-weight: 600;
  text-align: left;
}
.pgg-bang-tinh .pgg-tong {
  margin-top: 0.3rem;
  padding-top: 0.5rem;
  border-top: 1px solid var(--fs-line);
  font-size: 0.95rem;
}
.pgg-bang-tinh .pgg-tong dt {
  color: var(--fs-text);
  font-weight: 700;
}
.pgg-bang-tinh .pgg-tong dd {
  color: var(--fs-primary-dark);
}
.pgg-khong-du {
  color: #a8610a;
  font-weight: 500 !important;
}
.pgg-trung {
  display: flex;
  gap: 0.7rem;
  margin-top: 1rem;
  padding: 0.8rem 1rem;
  border-radius: 12px;
  background: #fff7e6;
  border: 1px solid #f3d9a4;
  color: #6f4a00;
  font-size: 0.85rem;
}
.pgg-trung ul {
  margin: 0.4rem 0;
  padding-left: 1.1rem;
}
.pgg-footer {
  display: flex;
  flex-direction: row-reverse;
  justify-content: flex-start;
  gap: 0.75rem;
}
#pgg-form :deep(input),
#pgg-form :deep(.form-control),
#pgg-form :deep(.money-input input),
#pgg-form :deep(.dti-time) {
  text-align: left !important;
}
</style>
