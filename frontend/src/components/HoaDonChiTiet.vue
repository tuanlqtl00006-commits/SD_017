<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import QRCode from 'qrcode'
import { useRoute } from 'vue-router'
import api from '../services/api'
import BaseModal from './common/BaseModal.vue'
import HoaDonNoiDung from './hoaDon/HoaDonNoiDung.vue'
import SuaDonModal from './hoaDon/SuaDonModal.vue'
import SuaGiaoHangModal from './hoaDon/SuaGiaoHangModal.vue'
import { useToast } from '../composables/useToast'
import { hoiXacNhan } from '../composables/useXacNhan'
import { useAuth } from '../composables/useAuth'
import { useChatChuaDoc } from '../composables/useChatChuaDoc'
import { useAutoRefresh } from '../composables/useAutoRefresh'
import { TT, laTrangThaiCuoi, nutChuyen, trangThaiOf } from '../constants/hoaDon'
import { formatMoney, formatNgayGio, formatGio, formatNgay } from '../utils/format'
import { taoHtmlHoaDon } from '../utils/hoaDonIn'

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

// Mỗi lần dữ liệu đổi (tải, đổi trạng thái, sửa đơn...) tăng số phiên: lần tải ngầm bắt đầu trước đó về sau thì bỏ, không đè số mới
let phienDuLieu = 0
function setData(d) {
  data.value = d
  phienDuLieu++
}

onMounted(load)

// Có tin thay đổi hóa đơn thì tự cập nhật ngay (đơn vừa được người khác xác nhận / khách hủy... hiện ra không cần F5), kèm hỏi lại định kỳ để phòng sót (xem useAutoRefresh).
// Không tải khi đang mở hộp thoại đổi trạng thái / sửa đơn / lịch sử / mã QR hoặc đang gửi thao tác.
async function taiNgam() {
  if (loading.value || !data.value) return
  const phien = phienDuLieu
  try {
    const res = await api.get(`/hoa-don/${idHoaDon}`)
    if (phien === phienDuLieu) setData(res.data)
  } catch {
    // lỗi tạm thời: giữ dữ liệu đang hiện
  }
}
useAutoRefresh(taiNgam, 1500, {
  chuKyMs: 4000,
  chuKyDuPhongMs: 5000,
  chuDe: ['hoa-don'],
  coTheTai: () => !action.open && !action.saving && !moSuaDon.value && !moSuaGiao.value && !showLichSu.value && !showQrModal.value,
})

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
const lichSu = computed(() => data.value?.lichSu ?? [])

/* ===================== Định dạng & trạng thái hiện tại ===================== */

const fmtDateTime = formatNgayGio
const trangThaiHienTai = computed(() => trangThaiOf(hd.value.trangThai))

/* ===================== Đổi trạng thái ===================== */
// Nút nào được hiện do BACKEND quyết định (hoaDon.chuyenDuoc); đơn đã hoàn thành / đã hủy thì không còn nút nào.

const THU_TU_NUT = { nguyhiem: 0, canhbao: 1, chinh: 2 }
// 24/11 (Task 3.2): đơn online chuyển khoản chưa thanh toán thì ẩn "Xác nhận đơn" (backend cũng chặn)
const dsNut = computed(() =>
  (hd.value.chuyenDuoc ?? [])
    .filter((c) => !(data.value?.choThanhToanOnline && c.trangThai === TT.DA_XAC_NHAN))
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

async function moXacNhan(trangThai) {
  // Đơn online thiếu hàng: bấm Xác nhận thì mở popup hết hàng thay cho hộp xác nhận (góp ý 16/11 [05:20–06:47])
  if (trangThai === TT.DA_XAC_NHAN && data.value?.duocBaoHetHang) {
    moHetHang(data.value.dsThieuHang)
    return
  }

  // Đơn COD: phải hiện thông báo xác nhận đã nhận tiền rồi mới xác nhận đã hoàn thành đơn
  if (trangThai === TT.HOAN_THANH && (data.value?.duocXacNhanTienCod || (hd.value.loaiDon === 2 && daThanhToan.value < Number(hd.value.thanhTien || 0)))) {
    const daNhanTien = await hoiXacNhan({
      title: 'Xác nhận đã nhận tiền',
      message: `Đơn hàng ${hd.value.maHoaDon} là đơn thanh toán khi nhận hàng (COD). Bạn xác nhận đã nhận đủ số tiền ${formatMoney(hd.value.thanhTien)} từ khách hàng / đơn vị giao hàng chưa?`,
      confirmText: 'Xác nhận đã nhận tiền',
      variant: 'primary',
    })
    if (!daNhanTien) return

    try {
      const res = await api.post(`/hoa-don/${idHoaDon}/xac-nhan-tien-cod`)
      setData(res.data)
      toast.success('Đã xác nhận nhận đủ tiền COD.')
    } catch (e) {
      toast.error(e.message || 'Không thể xác nhận tiền COD.')
      return
    }
  }

  const c = (hd.value.chuyenDuoc ?? []).find((x) => x.trangThai === trangThai)
  action.open = true
  action.trangThai = trangThai
  action.ghiChu = ''
  action.error = ''
  action.batBuoc = c ? !!c.batBuocLyDo : trangThai in THIEU_LY_DO
  const nut = nutChuyen(hd.value.loaiDon, hd.value.trangThai, trangThai)
  action.title = trangThai === TT.DA_HUY ? 'Hủy đơn hàng' : trangThai === TT.GIAO_THAT_BAI ? 'Báo giao hàng thất bại' : nut.text
}

// Tên người đang đăng nhập (backend cũng tự lấy theo phiên đăng nhập)
function nguoiThaoTac() {
  return useAuth().trangThai.nguoiDung?.hoTen || 'Quản lý'
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

  // Đơn COD: phải hiện thông báo xác nhận đã nhận tiền rồi mới xác nhận đã hoàn thành đơn
  if (action.trangThai === TT.HOAN_THANH && (data.value?.duocXacNhanTienCod || (hd.value.loaiDon === 2 && daThanhToan.value < Number(hd.value.thanhTien || 0)))) {
    const daNhanTien = await hoiXacNhan({
      title: 'Xác nhận đã nhận tiền',
      message: `Đơn hàng ${hd.value.maHoaDon} là đơn thanh toán khi nhận hàng (COD). Bạn xác nhận đã nhận đủ số tiền ${formatMoney(hd.value.thanhTien)} từ khách hàng / đơn vị giao hàng chưa?`,
      confirmText: 'Xác nhận đã nhận tiền',
      variant: 'primary',
    })
    if (!daNhanTien) return

    try {
      const resCod = await api.post(`/hoa-don/${idHoaDon}/xac-nhan-tien-cod`)
      setData(resCod.data)
      toast.success('Đã xác nhận nhận đủ tiền COD.')
    } catch (e) {
      action.error = e.message || 'Không thể xác nhận tiền COD.'
      return
    }
  }
  // Chuyển sang "Đã xác nhận" thì tự mở bản in / PDF hóa đơn.
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
    useChatChuaDoc().taiLai()
    const ma = hd.value.maHoaDon
    const ttThucTe = hd.value.trangThai

    if (ttThucTe === TT.DA_HUY) {
      cuaSoIn?.close?.()
      toast.success(`Đã hủy hóa đơn ${ma}`)
    } else if (ttThucTe === TT.GIAO_THAT_BAI) {
      cuaSoIn?.close?.()
      toast.success(`Hóa đơn ${ma}: đã ghi nhận giao hàng thất bại`)
    } else {
      toast.success(`Hóa đơn ${ma}: ${trangThaiOf(ttThucTe).label.toLowerCase()}`)
    }

    // Chỉ tự động in khi đơn hàng THỰC SỰ được chuyển sang Đã xác nhận thành công
    if (tuDongIn && ttThucTe === TT.DA_XAC_NHAN) {
      if (cuaSoIn) {
        inVaoCuaSo(cuaSoIn)
        toast.success('Đã mở bản in hóa đơn. Chọn "Lưu dưới dạng PDF" trong hộp thoại in để lưu file.')
      } else {
        toast.error('Đã xác nhận đơn nhưng trình duyệt chặn cửa sổ in. Cho phép cửa sổ bật lên rồi bấm "In hóa đơn".')
      }
    } else {
      cuaSoIn?.close?.()
    }
  } catch (e) {
    cuaSoIn?.close?.()
    if (e.code === 'THIEU_HANG') {
      // Kho vừa hết (quầy / đơn khác vừa bán) trong lúc đang mở hộp xác nhận: chuyển sang popup hết hàng
      action.open = false
      moHetHang(e.chiTiet?.thieu)
      taiNgam()
      return
    }
    action.error = e.message || 'Không cập nhật được trạng thái.'
  } finally {
    action.saving = false
  }
}

const showLichSu = ref(false)

/* ===================== Sửa đơn online 1 lần, xác nhận chuyển khoản ===================== */
const moSuaDon = ref(false)
// Sửa thông tin giao hàng (tách khỏi Chỉnh sửa đơn, mỗi đơn 1 lần)
const moSuaGiao = ref(false)
function daSuaGiao(d) {
  moSuaGiao.value = false
  setData(d)
  toast.success('Đã cập nhật thông tin giao hàng.')
}
function daSuaDon(d) {
  moSuaDon.value = false
  setData(d)
}
// 24/11 (Task 3.3): gửi mail nhắc thanh toán - chỉ đơn online chuyển khoản, chưa thanh toán, còn Chờ xác nhận (backend quyết định
// qua duocGuiMailNhacThanhToan). Không tạo QR mới, không gia hạn: tới lúc đặt + 5 phút đơn vẫn tự hủy.
const dangGuiNhac = ref(false)
const hanThanhToanText = computed(() => (data.value?.hanThanhToan ? formatGio(data.value.hanThanhToan) : ''))
// Tới hạn (lúc đặt + 5 phút) thì ẩn nút ngay trên máy, rồi tải lại để thấy đơn đã tự hủy (máy chủ quét mỗi 20 giây)
const quaHanTaiMay = ref(false)
let henQuaHan = null
watch(
  () => data.value?.hanThanhToan,
  (han) => {
    clearTimeout(henQuaHan)
    quaHanTaiMay.value = false
    if (!han) return
    const conLai = new Date(han).getTime() - Date.now()
    if (!(conLai > 0)) {
      quaHanTaiMay.value = true
      return
    }
    if (conLai > 2_000_000_000) return // setTimeout quá ~24 ngày bị tràn số và chạy ngay

    henQuaHan = setTimeout(() => {
      quaHanTaiMay.value = true
      setTimeout(taiNgam, 25000)
    }, conLai)
  },
)
onBeforeUnmount(() => clearTimeout(henQuaHan))
async function guiMailNhacThanhToan() {
  if (dangGuiNhac.value) return
  dangGuiNhac.value = true
  try {
    const res = await api.post(`/hoa-don/${idHoaDon}/gui-mail-nhac-thanh-toan`)
    setData(res.data)
    toast.success(res.data?.thongBao || 'Đã gửi mail nhắc thanh toán cho khách.')
  } catch (e) {
    toast.error(e.message || 'Không gửi được mail nhắc thanh toán.')
    taiNgam()
  } finally {
    dangGuiNhac.value = false
  }
}

// Đơn chuyển khoản chưa thanh toán: Admin đổi sang COD khi khách liên hệ (Requirement 4)
const dangDoiCod = ref(false)
async function adminDoiSangCod() {
  if (dangDoiCod.value) return
  if (!(await hoiXacNhan({
    title: 'Đổi sang thanh toán khi nhận hàng (COD)',
    message: 'Khách hàng đã liên hệ yêu cầu đổi sang hình thức thanh toán khi nhận hàng (COD)? Thao tác này sẽ cập nhật phương thức thanh toán của đơn.',
    confirmText: 'Đổi sang COD',
  }))) return
  dangDoiCod.value = true
  try {
    const res = await api.put(`/hoa-don/${idHoaDon}/doi-sang-cod`)
    setData(res.data)
    toast.success('Đã đổi đơn hàng sang thanh toán khi nhận hàng (COD).')
  } catch (e) {
    toast.error(e.message || 'Không đổi được sang COD.')
  } finally {
    dangDoiCod.value = false
  }
}

// Đơn chuyển khoản chưa thanh toán: Admin xác nhận đã nhận chuyển khoản (Requirement 4)
const moXacNhanChuyenKhoan = ref(false)
const maGiaoDichCk = ref('')
const dangXacNhanCk = ref(false)
async function adminXacNhanChuyenKhoan() {
  if (dangXacNhanCk.value) return
  dangXacNhanCk.value = true
  try {
    const res = await api.post(`/hoa-don/${idHoaDon}/xac-nhan-thanh-toan`, { maGiaoDich: maGiaoDichCk.value.trim() })
    setData(res.data)
    moXacNhanChuyenKhoan.value = false
    maGiaoDichCk.value = ''
    toast.success('Đã xác nhận thanh toán chuyển khoản thành công.')
  } catch (e) {
    toast.error(e.message || 'Không xác nhận được thanh toán.')
  } finally {
    dangXacNhanCk.value = false
  }
}

// Đơn COD: Xác nhận đã nhận tiền COD trước khi hoàn thành đơn (Requirement 6)
const dangXacNhanCod = ref(false)
async function adminXacNhanTienCod() {
  if (dangXacNhanCod.value) return
  if (!(await hoiXacNhan({
    title: 'Xác nhận đã nhận tiền COD',
    message: `Xác nhận đã nhận đủ tiền COD cho đơn hàng ${hd.value.maHoaDon}? Sau khi xác nhận tiền, bạn có thể chuyển đơn sang Hoàn thành.`,
    confirmText: 'Xác nhận đã nhận tiền',
  }))) return
  dangXacNhanCod.value = true
  try {
    const res = await api.post(`/hoa-don/${idHoaDon}/xac-nhan-tien-cod`)
    setData(res.data)
    toast.success('Đã xác nhận đã nhận đủ tiền COD.')
  } catch (e) {
    toast.error(e.message || 'Không xác nhận được tiền COD.')
  } finally {
    dangXacNhanCod.value = false
  }
}

const dangXacNhanHoan = ref(false)
const moXacNhanHoan = ref(false)
const maGdHoan = ref('')

async function xacNhanHoanTien() {
  if (dangXacNhanHoan.value) return
  dangXacNhanHoan.value = true
  try {
    setData((await api.post(`/hoa-don/${idHoaDon}/xac-nhan-hoan-tien`, { maGiaoDich: maGdHoan.value })).data)
    moXacNhanHoan.value = false
    maGdHoan.value = ''
    toast.success('Đã xác nhận hoàn tiền cho khách hàng.')
  } catch (e) {
    toast.error(e.message || 'Không xác nhận được hoàn tiền.')
  } finally {
    dangXacNhanHoan.value = false
  }
}
/* ----- Xác nhận đơn online bị thiếu hàng: popup thay cho nút "Báo hết hàng cho khách" (góp ý 16/11 [05:20–06:47]) -----
 * Giữ hàng / chờ nhập thêm: đóng popup, đơn giữ Chờ xác nhận.
 * Gửi mail báo hết hàng: thư chăm sóc khách, đơn KHÔNG hủy (luồng 14/11).
 * Hoàn tiền cho khách (đơn đã thanh toán): hủy đơn với lý do hết hàng, máy chủ gửi thư báo hết hàng + hoàn tiền;
 * chuyển trả tiền xong nhân viên bấm "Xác nhận đã hoàn tiền" như cũ. */
const hetHang = reactive({ open: false, thieu: [], buoc: 'chon', dang: false })
function moHetHang(thieu) {
  hetHang.thieu = thieu?.length ? [...thieu] : [...(data.value?.dsThieuHang ?? [])]
  hetHang.buoc = 'chon'
  hetHang.dang = false
  hetHang.open = true
}
function giuHangCho() {
  hetHang.open = false
  toast.success('Đơn vẫn giữ ở Chờ xác nhận. Nhập thêm hàng rồi bấm Xác nhận lại.')
}
async function guiMailHetHang() {
  if (hetHang.dang) return
  if (!(await hoiXacNhan({ title: 'Gửi thư báo hết hàng', message: 'Bạn có chắc muốn gửi email báo hết hàng cho khách? Đơn vẫn giữ ở Chờ xác nhận.', confirmText: 'Gửi thư' }))) return
  hetHang.dang = true
  try {
    setData((await api.post(`/hoa-don/${idHoaDon}/bao-het-hang`)).data)
    hetHang.open = false
    toast.success('Đã gửi thư báo hết hàng cho khách. Đơn vẫn giữ ở Chờ xác nhận.')
  } catch (e) {
    toast.error(e.message || 'Không gửi được thư báo hết hàng.')
  } finally {
    hetHang.dang = false
  }
}
async function huyVaHoanTien() {
  if (hetHang.dang) return
  if (!(await hoiXacNhan({ title: 'Hủy đơn và hoàn tiền', message: 'Bạn có chắc muốn hủy đơn này vì hết hàng và hoàn tiền cho khách? Thao tác không hoàn tác được.', confirmText: 'Hủy đơn', variant: 'danger' }))) return
  hetHang.dang = true
  const ghiChu = `Hết hàng: ${hetHang.thieu.join('; ')}`.slice(0, 500)
  try {
    setData((await api.put(`/hoa-don/${idHoaDon}/trang-thai`, { trangThaiMoi: TT.DA_HUY, ghiChu, nguoiThaoTac: nguoiThaoTac() })).data)
    hetHang.open = false
    toast.success(`Đã hủy đơn ${hd.value.maHoaDon} và gửi thư báo hết hàng, hoàn tiền cho khách. Chuyển trả tiền xong thì bấm "Xác nhận đã hoàn tiền".`)
  } catch (e) {
    toast.error(e.message || 'Không hủy được đơn.')
  } finally {
    hetHang.dang = false
  }
}
const showQrModal = ref(false)
const lichSuMoiTruoc = computed(() => [...lichSu.value].reverse())

/* ===================== Thanh toán ===================== */

const daThanhToan = computed(() => Number(data.value?.daThanhToan ?? 0))
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

      <!-- Thân màn dùng chung với màn đơn hàng của khách (/shop/tra-cuu) để hai màn luôn y hệt nhau -->
      <HoaDonNoiDung :data="data" @sua-giao-hang="moSuaGiao = true">
        <template #hanh-dong>
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
          <button v-if="hd.duocChinhSua" type="button" class="ad-btn" data-testid="hd-sua-don" @click="moSuaDon = true">
            <i class="bi bi-pencil-square" aria-hidden="true"></i> Chỉnh sửa đơn
          </button>

          <button
            v-if="data.duocGuiMailNhacThanhToan && !quaHanTaiMay"
            type="button"
            class="ad-btn hd-btn-outline-warning"
            data-testid="hd-gui-mail-nhac"
            :disabled="dangGuiNhac"
            @click="guiMailNhacThanhToan"
          >
            <span v-if="dangGuiNhac" class="spinner-border spinner-border-sm" aria-hidden="true"></span>
            <i v-else class="bi bi-envelope" aria-hidden="true"></i> Gửi mail nhắc thanh toán
          </button>
          <p v-if="data.choThanhToanOnline && !data.donTreo && !quaHanTaiMay" class="hd-khoa w-100 mb-0" role="note" data-testid="hd-cho-thanh-toan">
            <i class="bi bi-hourglass-split" aria-hidden="true"></i> Chờ khách thanh toán online: chưa xác nhận đơn được.<template v-if="hanThanhToanText"> Hạn {{ hanThanhToanText }}, quá hạn đơn tự hủy.</template>
          </p>
          <p v-if="data.donTreo || (data.choThanhToanOnline && quaHanTaiMay)" class="hd-khoa w-100 mb-0" role="note" data-testid="hd-don-treo">
            <i class="bi bi-hourglass-bottom" aria-hidden="true"></i> Đã quá 5 phút khách chưa thanh toán online: hệ thống đang tự hủy đơn.
          </p>

          <button v-if="data.duocXacNhanHoanTien" type="button" class="ad-btn btn-warning text-dark fw-medium" data-testid="hd-xac-nhan-hoan-tien" :disabled="dangXacNhanHoan" @click="moXacNhanHoan = true">
            <i class="bi bi-cash-stack" aria-hidden="true"></i> Xác nhận đã hoàn tiền
          </button>
          <button type="button" class="ad-btn" @click="showLichSu = true">
            <i class="bi bi-clock-history" aria-hidden="true"></i> Lịch sử thao tác
          </button>
          <p v-if="hd.daChinhSua" class="hd-khoa w-100 mb-0" role="note">
            <i class="bi bi-pencil" aria-hidden="true"></i> Đơn đã được chỉnh sửa 1 lần (xem Lịch sử thao tác), không sửa thêm được.
          </p>
        </template>
        <template #giua>
          <!-- QR ẩn hỗ trợ quét / kiểm tra tự động -->
          <div class="hd-qr d-none">
            <img v-if="qrUrl" :src="qrUrl" :alt="`Mã QR hóa đơn ${hd.maHoaDon}`" width="168" height="168">
          </div>
        </template>
      </HoaDonNoiDung>
    </template>

    <SuaDonModal v-if="moSuaDon && data" :data="data" @close="moSuaDon = false" @saved="daSuaDon" />
    <SuaGiaoHangModal v-if="moSuaGiao && data" :data="data" :url="`/hoa-don/${idHoaDon}/sua-giao-hang`" @close="moSuaGiao = false" @saved="daSuaGiao" />

    <!-- Hộp thoại: lịch sử thao tác -->
    <BaseModal v-if="showLichSu" title="Lịch sử thao tác" size="xl" @close="showLichSu = false">
      <div class="ad-table-wrap">
        <div class="ad-table-scroll">
          <table class="ad-table hd-table-lich-su">
            <thead>
              <tr>
                <th class="ad-col-stt">STT</th>
                <th style="min-width: 140px;">Hành động</th>
                <th style="min-width: 130px;">Người thao tác</th>
                <th style="min-width: 110px;">Thời gian</th>
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
                <td class="ad-nowrap">
                  <div class="fw-semibold text-dark">{{ formatGio(ls.thoiGian) || '—' }}</div>
                  <div v-if="formatNgay(ls.thoiGian)" class="small text-muted">{{ formatNgay(ls.thoiGian) }}</div>
                </td>
                <td class="hd-ls-ghi-chu">{{ ls.moTa || '—' }}</td>
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

    <!-- Popup: xác nhận đơn online mà kho không đủ hàng -->
    <BaseModal v-if="hetHang.open" title="Không đủ hàng trong kho để xác nhận" size="md" @close="hetHang.open = false">
      <div data-testid="hd-het-hang">
        <p class="mb-2">Đơn <strong>{{ hd.maHoaDon }}</strong> có sản phẩm không đủ hàng trong kho:</p>
        <ul class="hd-het-hang-ds">
          <li v-for="t in hetHang.thieu" :key="t">{{ t }}</li>
        </ul>
        <div class="alert alert-warning small my-3" role="alert">
          <i class="bi bi-exclamation-triangle-fill me-1"></i>
          Hệ thống không cho phép xác nhận khi không đủ hàng tồn kho. Vui lòng liên hệ với khách hàng để thông báo và thực hiện <strong>Hủy đơn</strong>.
        </div>
        <p v-if="daThanhToan > 0" class="small mb-0">Khách đã thanh toán <strong>{{ formatMoney(daThanhToan) }}</strong> (hủy đơn để thực hiện hoàn tiền).</p>
        <p v-else class="small text-muted mb-0">Khách chưa thanh toán.</p>
      </div>
      <template #footer>
        <button type="button" class="ad-btn" :disabled="hetHang.dang" @click="hetHang.open = false">Đóng</button>
        <button type="button" class="ad-btn ad-btn-danger" data-testid="hd-het-hang-hoan-ok" :disabled="hetHang.dang" @click="huyVaHoanTien">
          <span v-if="hetHang.dang" class="spinner-border spinner-border-sm me-1" aria-hidden="true"></span>
          {{ daThanhToan > 0 ? 'Hủy đơn và hoàn tiền' : 'Hủy đơn' }}
        </button>
      </template>
    </BaseModal>

    <!-- Hộp thoại: Xác nhận hoàn tiền cho khách -->
    <BaseModal v-if="moXacNhanHoan" title="Xác nhận hoàn tiền cho khách" size="md" @close="moXacNhanHoan = false">
      <div class="p-3">
        <p>
          Khách hàng đã thanh toán <strong>{{ formatMoney(data.soTienCanHoan) }}</strong> cho đơn <strong>{{ hd.maHoaDon }}</strong>.
        </p>
        <p class="text-muted small">
          Sau khi đã chuyển khoản trả lại tiền cho khách qua ngân hàng, nhập mã giao dịch hoàn tiền (tùy chọn) và bấm xác nhận để chuyển hóa đơn sang trạng thái <strong>Hoàn tiền</strong>.
        </p>
        <div class="mb-2">
          <label class="form-label small fw-semibold" for="hd-ma-gd-hoan">Mã giao dịch chuyển khoản hoàn tiền (nếu có)</label>
          <input
            id="hd-ma-gd-hoan"
            v-model="maGdHoan"
            type="text"
            class="form-control"
            placeholder="Ví dụ: FT2400012345"
            maxlength="100"
          />
        </div>
      </div>
      <template #footer>
        <button type="button" class="ad-btn" :disabled="dangXacNhanHoan" @click="moXacNhanHoan = false">Hủy</button>
        <button type="button" class="ad-btn ad-btn-primary" :disabled="dangXacNhanHoan" @click="xacNhanHoanTien">
          <span v-if="dangXacNhanHoan" class="spinner-border spinner-border-sm me-1" aria-hidden="true"></span>
          Xác nhận đã hoàn tiền
        </button>
      </template>
    </BaseModal>

    <!-- Hộp thoại: Mã QR hóa đơn -->
    <BaseModal v-if="showQrModal" title="Mã QR hóa đơn" size="sm" @close="showQrModal = false">
      <div class="hd-qr text-center p-3">
        <a
          v-if="qrUrl"
          :href="qrUrl"
          :download="`QR_${hd.maHoaDon || 'hoa_don'}.png`"
          class="d-inline-block text-decoration-none"
          title="Bấm vào ảnh mã QR để tải về máy"
        >
          <img :src="qrUrl" :alt="`Mã QR hóa đơn ${hd.maHoaDon}`" width="180" height="180" class="img-fluid rounded border p-1 bg-white hd-qr-img">
        </a>
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
/* Bố cục và giao diện thân màn nằm ở hoaDon/HoaDonNoiDung.vue (dùng chung với màn đơn hàng của khách) */
.hd-toolbar { display: flex; justify-content: space-between; align-items: center; gap: 0.75rem; margin-bottom: 1.25rem; }
.hd-qr { display: flex; flex-direction: column; align-items: center; gap: 0.5rem; }
.hd-qr img { border: 1px solid var(--fs-line); border-radius: 0.5rem; padding: 6px; background: #fff; }
.hd-qr-img { cursor: pointer; transition: transform 0.15s ease, box-shadow 0.15s ease; }
.hd-qr-img:hover { transform: scale(1.02); box-shadow: 0 4px 12px rgba(9, 119, 236, 0.15); }
.hd-qr-cap { margin: 0; font-size: 0.85rem; color: var(--fs-muted); }

/* Nút đổi trạng thái (truyền vào thân màn qua slot "hanh-dong") */
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
.hd-het-hang-ds {
  margin: 0 0 0.75rem;
  padding-left: 1.2rem;
  color: #b42318;
  font-weight: 500;
}
.hd-in-tu-dong {
  margin: -0.5rem 0 1rem;
  padding: 0.55rem 0.8rem;
  border-radius: 10px;
  background: var(--fs-primary-soft);
  color: var(--fs-primary-dark);
  font-size: 0.85rem;
}
.hd-table-lich-su .hd-ls-ghi-chu {
  min-width: 280px;
  max-width: none;
  word-break: break-word;
  line-height: 1.5;
}
</style>
