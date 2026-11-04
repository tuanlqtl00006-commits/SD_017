<script setup>
// Bán hàng tại quầy: mỗi tab là một hóa đơn chờ (lưu ở CSDL, thêm sản phẩm là trừ kho).
// Tự chọn phiếu giảm nhiều nhất, có giao hàng, thanh toán tiền mặt / chuyển khoản / kết hợp. Phải vào ca mới bán được.
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import QRCode from 'qrcode'
import api from '../../services/api'
import ChonKhach from './ChonKhach.vue'
import TimSanPham from './TimSanPham.vue'
import QuetMaModal from './QuetMaModal.vue'
import BaseModal from '../common/BaseModal.vue'
import ConfirmDialog from '../common/ConfirmDialog.vue'
import MoneyInput from '../common/MoneyInput.vue'
import GiaoCaModal from '../giaoCa/GiaoCaModal.vue'
import DiaChiHanhChinhSelect from '../common/DiaChiHanhChinhSelect.vue'
import VanChuyenLogo from '../common/VanChuyenLogo.vue'
import { useGiaoCa } from '../../composables/useGiaoCa'
import { useAuth } from '../../composables/useAuth'
import { useToast } from '../../composables/useToast'
import { NGAN_HANG, PHUONG_THUC, daCauHinhNganHang } from '../../constants/thanhToan'
import { formatMoney } from '../../utils/format'
import { formatPhanTram } from '../../utils/giamGia'
import { loiSdt } from '../../utils/kiemTra'
import { taoHtmlHoaDon } from '../../utils/hoaDonIn'
import { SO_LUONG_TOI_DA, chuoiVietQR, dangGiamTheoDot, phiGiaoHang, tienGiamDot, tinhThanhToan, tongSoLuong, tongTienHang } from '../../utils/banHang'

const DON_VI_VAN_CHUYEN = 'Giao Hàng Nhanh'

const SO_HOA_DON_CHO_TOI_DA = 10
const router = useRouter()
const toast = useToast()
const giaoCa = useGiaoCa()
// Quản lý có toàn quyền: bán hàng không bắt nhận ca / giao ca; nhân viên phải vào ca trước
const auth = useAuth()
const canVaoCa = computed(() => !auth.laQuanLy.value && !giaoCa.dangTrongCa.value)

/* ===================== Hóa đơn chờ (CSDL) ===================== */
const dsCho = ref([]) // [{ id, maHoaDon, khach, items, tongTienHang, soLuong, coGiaDoi }]
const dangChon = ref(null)
const daTaiCho = ref(false)
const dangLam = ref(false) // đang gọi API thêm / sửa / xóa
// Phần nhập liệu thanh toán của từng hóa đơn chờ (chỉ ở màn hình, không ảnh hưởng tồn kho)
const phu = reactive({})
function phuMoi() {
  return {
    maPhieu: '',
    tuDongPhieu: true, // true: tự chọn phiếu giảm nhiều nhất; false: nhân viên đã tự chọn / bỏ phiếu
    ghiChu: '',
    phuongThuc: 'TIEN_MAT',
    tienMat: '',
    tienMatTay: false, // true: nhân viên đã tự sửa số tiền khách đưa, không tự điền lại
    chuyenKhoan: '',
    maGiaoDich: '',
    giaoHang: false,
    nguoiNhan: { hoTen: '', sdt: '', tinhThanh: '', phuongXa: '', diaChiCuThe: '' },
  }
}
const RONG = Object.freeze({ id: null, maHoaDon: '', khach: null, items: [], tongTienHang: 0, soLuong: 0, coGiaDoi: false })
const PHU_RONG = reactive(phuMoi()) // khi chưa có hóa đơn chờ nào
const hdServer = computed(() => dsCho.value.find((h) => h.id === dangChon.value) ?? dsCho.value[0] ?? RONG)
const hd = computed(() => {
  const h = hdServer.value
  return { ...h, ten: h.maHoaDon || 'Hóa đơn mới', gio: h.items, p: (h.id && phu[h.id]) || PHU_RONG }
})

function thayHoaDon(moi) {
  if (!phu[moi.id]) phu[moi.id] = phuMoi()
  const i = dsCho.value.findIndex((h) => h.id === moi.id)
  if (i >= 0) dsCho.value.splice(i, 1, moi)
  else dsCho.value.push(moi)
  if (moi.thongBao) toast.success(moi.thongBao, 'Giá đã thay đổi')
}

async function taiHoaDonCho() {
  try {
    dsCho.value = (await api.get('/ban-hang/hoa-don-cho')).data || []
    for (const h of dsCho.value) if (!phu[h.id]) phu[h.id] = phuMoi()
    if (!dsCho.value.some((h) => h.id === dangChon.value)) dangChon.value = dsCho.value[0]?.id ?? null
  } catch (e) {
    toast.error(e.message || 'Không tải được hóa đơn chờ.')
  } finally {
    daTaiCho.value = true
  }
}

/** Lỗi khi thao tác hóa đơn chờ: hóa đơn đã đóng ở cửa sổ khác thì tải lại danh sách. */
function baoLoiCho(e, macDinh) {
  toast.error(e.message || macDinh)
  if (e.code === 'HOA_DON_CHO_DA_DONG' || e.status === 404) taiHoaDonCho()
  if (e.code === 'HET_HANG') timSp.value?.taiLai()
  // Ca nhận từ hôm trước chưa giao: mở hộp Ca làm việc để giao ca đó
  if (e.code === 'CA_CU_CHUA_GIAO') moCa.value = true
}

async function themHoaDon() {
  if (dsCho.value.length >= SO_HOA_DON_CHO_TOI_DA) {
    toast.error(`Tối đa ${SO_HOA_DON_CHO_TOI_DA} hóa đơn chờ. Vui lòng thanh toán hoặc hủy bớt.`)
    return null
  }
  if (canVaoCa.value) {
    toast.error('Bạn chưa vào ca.')
    moCa.value = true
    return null
  }
  try {
    const moi = (await api.post('/ban-hang/hoa-don-cho')).data
    thayHoaDon(moi)
    dangChon.value = moi.id
    return moi
  } catch (e) {
    baoLoiCho(e, 'Không tạo được hóa đơn chờ.')
    return null
  }
}
const xoaHd = ref(null)
function hoiXoaHoaDon(h) {
  if (h.items.length) xoaHd.value = h
  else xoaHoaDon(h)
}
async function xoaHoaDon(h) {
  xoaHd.value = null
  try {
    await api.delete(`/ban-hang/hoa-don-cho/${h.id}`)
    if (h.items.length) toast.success(`Đã hủy ${h.maHoaDon}, trả ${h.soLuong} sản phẩm về kho.`)
    timSp.value?.taiLai()
  } catch (e) {
    if (e.code !== 'HOA_DON_CHO_DA_DONG' && e.status !== 404) {
      baoLoiCho(e, 'Không hủy được hóa đơn chờ.')
      return
    }
  }
  const i = dsCho.value.findIndex((x) => x.id === h.id)
  if (i >= 0) dsCho.value.splice(i, 1)
  delete phu[h.id]
  if (dangChon.value === h.id) dangChon.value = dsCho.value[Math.max(0, i - 1)]?.id ?? null
}

/* ===================== Giỏ hàng ===================== */
const timSp = ref(null) // bảng chọn sản phẩm (chỉ có khi hộp thoại đang mở)
const moChonSp = ref(false)

/* ----- Quét mã: máy quét cầm tay gõ mã + Enter vào ô, hoặc camera ----- */
const maQuet = ref('')
const oMa = ref(null)
const moCamera = ref(false)
async function quetMa(giaTri) {
  const m = String(giaTri ?? maQuet.value).trim()
  if (!m) return
  try {
    const sp = (await api.get(`/ban-hang/san-pham/ma/${encodeURIComponent(m)}`)).data
    await themSanPham(sp, null, 1)
    maQuet.value = ''
  } catch (e) {
    toast.error(e.message || `Không tìm thấy sản phẩm có mã ${m}.`)
  }
  oMa.value?.focus()
}
function onMaCamera(giaTri) {
  moCamera.value = false
  quetMa(giaTri)
}
async function themSanPham(sp, loiQuet, soLuong = 1) {
  if (!sp) {
    toast.error(loiQuet)
    return
  }
  if (dangLam.value) return
  dangLam.value = true
  try {
    // Chưa có hóa đơn chờ thì tự tạo rồi thêm sản phẩm vào
    let id = hdServer.value.id
    if (!id) id = (await themHoaDon())?.id
    if (!id) return
    thayHoaDon((await api.post(`/ban-hang/hoa-don-cho/${id}/san-pham`, { idBienThe: sp.id, soLuong })).data)
    timSp.value?.taiLai()
  } catch (e) {
    baoLoiCho(e, 'Không thêm được sản phẩm.')
  } finally {
    dangLam.value = false
  }
}
async function doiSl(d, giaTri) {
  const sl = Math.floor(Number(giaTri))
  const toiDa = Math.min(SO_LUONG_TOI_DA, d.soLuong + Number(d.tonKho || 0))
  if (!Number.isFinite(sl) || sl < 1) {
    toast.error('Số lượng tối thiểu là 1. Muốn bỏ sản phẩm thì bấm nút xóa.')
    return
  }
  if (sl > toiDa) {
    toast.error(`${d.ten} chỉ còn ${d.tonKho} sản phẩm trong kho (tối đa ${toiDa} cho dòng này).`)
    return
  }
  if (sl === d.soLuong || dangLam.value) return
  dangLam.value = true
  try {
    thayHoaDon((await api.put(`/ban-hang/hoa-don-cho/${hdServer.value.id}/chi-tiet/${d.id}`, { soLuong: sl })).data)
    timSp.value?.taiLai()
  } catch (e) {
    baoLoiCho(e, 'Không đổi được số lượng.')
  } finally {
    dangLam.value = false
  }
}
async function xoaDong(d) {
  if (dangLam.value) return
  dangLam.value = true
  try {
    thayHoaDon((await api.delete(`/ban-hang/hoa-don-cho/${hdServer.value.id}/chi-tiet/${d.id}`)).data)
    timSp.value?.taiLai()
  } catch (e) {
    baoLoiCho(e, 'Không bỏ được sản phẩm.')
  } finally {
    dangLam.value = false
  }
}
async function capNhatGiaMoi() {
  try {
    thayHoaDon((await api.post(`/ban-hang/hoa-don-cho/${hdServer.value.id}/cap-nhat-gia`)).data)
    toast.success('Đã cập nhật giá mới cho hóa đơn.')
  } catch (e) {
    baoLoiCho(e, 'Không cập nhật được giá.')
  }
}
const tienHang = computed(() => tongTienHang(hd.value.gio))
const giamDot = computed(() => tienGiamDot(hd.value.gio))

/* ===================== Phiếu giảm giá ===================== */
const phieus = ref([])
const dangTaiPhieu = ref(false)
let henPhieu = null
async function taiPhieu() {
  const h = hd.value
  if (!h.gio.length) {
    phieus.value = []
    if (h.p.tuDongPhieu) h.p.maPhieu = ''
    return
  }
  dangTaiPhieu.value = true
  try {
    phieus.value = (await api.get('/ban-hang/phieu-giam-gia', { params: { idKhachHang: h.khach?.id, tongTien: tienHang.value } })).data
    // Tự áp phiếu giảm nhiều nhất (kể cả phiếu cá nhân của khách) cho tới khi nhân viên tự chọn
    if (h.p.tuDongPhieu) h.p.maPhieu = phieus.value.find((p) => p.apDungDuoc)?.ma ?? ''
  } catch (e) {
    phieus.value = []
    toast.error(e.message || 'Không tải được phiếu giảm giá.')
  } finally {
    dangTaiPhieu.value = false
  }
}
watch(
  () => [hd.value.id, hd.value.khach?.id, tienHang.value],
  () => {
    clearTimeout(henPhieu)
    henPhieu = setTimeout(taiPhieu, 300)
  },
  { immediate: true },
)
const phieuChon = computed(() => phieus.value.find((p) => p.ma === hd.value.p.maPhieu) ?? null)
const giamPhieu = computed(() => (phieuChon.value?.apDungDuoc ? Number(phieuChon.value.soTienGiam) : 0))
const loiPhieu = computed(() => {
  if (!hd.value.p.maPhieu || dangTaiPhieu.value) return ''
  if (!phieuChon.value) return `Phiếu ${hd.value.p.maPhieu} không còn dùng được cho đơn này.`
  return phieuChon.value.apDungDuoc ? '' : phieuChon.value.lyDo
})
function chonPhieu(ma) {
  hd.value.p.maPhieu = ma
  hd.value.p.tuDongPhieu = false
}
// Hộp thoại chọn phiếu
const moPhieu = ref(false)
// Phiếu dùng được cho đơn, giảm nhiều nhất lên đầu (máy chủ đã xếp, sắp lại cho chắc)
const phieuDung = computed(() => phieus.value.filter((p) => p.apDungDuoc).sort((a, b) => Number(b.soTienGiam) - Number(a.soTienGiam)))
const soPhieuDung = computed(() => phieuDung.value.length)
// Dòng phiếu bấm được (mở pop-up chọn / bỏ phiếu) khi đã có hàng và có phiếu dùng được hoặc đang áp một phiếu
const coTheChonPhieu = computed(() => hd.value.gio.length > 0 && (soPhieuDung.value > 0 || !!hd.value.p.maPhieu))
// Gợi ý mua thêm: phiếu chỉ còn thiếu đơn tối thiểu mà khi đủ sẽ giảm nhiều hơn phiếu tốt nhất hiện tại, thiếu ít nhất lên đầu
const goiYMuaThem = computed(() => {
  const totNhat = Number(phieuDung.value[0]?.soTienGiam || 0)
  return phieus.value
    .filter((p) => !p.apDungDuoc && p.canMuaThem > 0 && Number(p.giamKhiDu) > totNhat)
    .sort((a, b) => a.canMuaThem - b.canMuaThem)
    .slice(0, 3)
})
function chonPhieuMo(ma) {
  chonPhieu(ma)
  moPhieu.value = false
}
function moTaPhieu(p) {
  const giam = p.loaiGiam === 'PHAN_TRAM' ? `Giảm ${formatPhanTram(p.giaTri)}${p.giamToiDa ? `, tối đa ${formatMoney(p.giamToiDa)}` : ''}` : `Giảm ${formatMoney(p.giaTri)}`
  return `${giam}${p.donToiThieu ? ` · đơn từ ${formatMoney(p.donToiThieu)}` : ''}`
}

/* ===================== Khách ===================== */
async function chonKhach(k) {
  let id = hdServer.value.id
  if (!id) id = (await themHoaDon())?.id
  if (!id) return
  try {
    thayHoaDon((await api.put(`/ban-hang/hoa-don-cho/${id}/khach-hang`, { idKhachHang: k?.id ?? null })).data)
  } catch (e) {
    baoLoiCho(e, 'Không đổi được khách hàng.')
    return
  }
  const p = phu[id]
  if (!p.tuDongPhieu && phieuChon.value?.caNhan) {
    p.maPhieu = ''
    p.tuDongPhieu = true
  }
  if (p.giaoHang) await dienNguoiNhan()
}

/* ===================== Giao hàng ===================== */
// Phí giao theo tỉnh nhận: chưa chọn tỉnh thì null (chưa tính), đổi tỉnh là tính lại
const phiShipTheoTinh = computed(() => (hd.value.p.giaoHang ? phiGiaoHang(hd.value.p.nguoiNhan.tinhThanh) : 0))
const phiShip = computed(() => phiShipTheoTinh.value ?? 0)
const loiGiao = computed(() => {
  if (!hd.value.p.giaoHang) return {}
  const n = hd.value.p.nguoiNhan
  const e = {}
  if (!n.hoTen.trim()) e.hoTen = 'Vui lòng nhập tên người nhận.'
  const loiSdtNhan = loiSdt((n.sdt || '').replace(/[\s.-]/g, ''), 'Vui lòng nhập số điện thoại người nhận.')
  if (loiSdtNhan) e.sdt = loiSdtNhan
  if (!n.tinhThanh) e.tinhThanh = 'Vui lòng chọn Tỉnh/Thành phố.'
  if (!n.phuongXa) e.phuongXa = 'Vui lòng chọn Phường/Xã.'
  if (!n.diaChiCuThe.trim()) e.diaChiCuThe = 'Vui lòng nhập địa chỉ cụ thể (số nhà, tên đường).'
  return e
})
const daChamGiao = ref(false)
const daChamSdtNhan = ref(false) // rời ô SĐT người nhận thì báo lỗi ngay, không đợi bấm Thanh toán
// SĐT chỉ nhận chữ số, tối đa 10 số
function nhapSdtNhan(e) {
  const so = e.target.value.replace(/\D/g, '').slice(0, 10)
  hd.value.p.nguoiNhan.sdt = so
  e.target.value = so
}
/** Điền sẵn người nhận theo khách đã chọn (địa chỉ mặc định); khách lẻ thì để trống cho nhân viên nhập một địa chỉ. */
async function dienNguoiNhan() {
  const h = hd.value
  const n = h.p.nguoiNhan
  if (!h.khach) return
  n.hoTen = n.hoTen || h.khach.hoTen || ''
  n.sdt = n.sdt || h.khach.sdt || ''
  if (n.tinhThanh || n.diaChiCuThe) return
  try {
    const ds = (await api.get(`/dia-chi/khach-hang/${h.khach.id}`)).data || []
    const dc = ds.find((d) => d.laDiaChiMacDinh) || ds[0]
    if (dc) {
      n.tinhThanh = dc.tinhThanhPho || ''
      n.phuongXa = dc.phuongXa || ''
      n.diaChiCuThe = dc.diaChiCuThe || ''
      if (dc.tenNguoiNhan) n.hoTen = dc.tenNguoiNhan
      if (dc.sdtNguoiNhan) n.sdt = dc.sdtNguoiNhan
    }
  } catch {
    // không lấy được địa chỉ thì nhân viên tự nhập
  }
}
function batGiaoHang(bat) {
  hd.value.p.giaoHang = bat
  daChamGiao.value = false
  daChamSdtNhan.value = false
  if (bat) dienNguoiNhan()
}

/* ===================== Thanh toán ===================== */
const tt = computed(() =>
  tinhThanhToan({
    tongTien: tienHang.value + phiShip.value,
    giam: giamPhieu.value,
    phuongThuc: hd.value.p.phuongThuc,
    tienMat: hd.value.p.tienMat,
    chuyenKhoan: hd.value.p.chuyenKhoan,
  }),
)
const coQr = daCauHinhNganHang()
const qrUrl = ref('')
const noiDungCk = computed(() => `FOOTSTYLE ${hd.value.maHoaDon || ''}`.trim())
watch(
  () => [tt.value.chuyenKhoan, noiDungCk.value, hd.value.p.phuongThuc],
  async ([ck]) => {
    qrUrl.value = ''
    if (!coQr || !ck || hd.value.p.phuongThuc === 'TIEN_MAT') return
    try {
      qrUrl.value = await QRCode.toDataURL(chuoiVietQR({ bin: NGAN_HANG.bin, soTaiKhoan: NGAN_HANG.soTaiKhoan, soTien: ck, noiDung: noiDungCk.value }), { width: 220, margin: 1 })
    } catch {
      qrUrl.value = ''
    }
  },
  { immediate: true },
)
function chonPhuongThuc(ma) {
  hd.value.p.phuongThuc = ma
  if (ma === 'CHUYEN_KHOAN') {
    hd.value.p.tienMat = ''
    hd.value.p.tienMatTay = false
  }
}

// Tiền mặt khách đưa tự điền đúng số khách cần trả (kết hợp: phần còn lại sau chuyển khoản); nhân viên sửa thì giữ số đã sửa
const tienMatTuDong = computed(() => {
  const p = hd.value.p
  if (p.phuongThuc === 'CHUYEN_KHOAN' || !hd.value.gio.length) return ''
  return Math.max(0, tt.value.phaiTra - (p.phuongThuc === 'KET_HOP' ? Math.min(tt.value.chuyenKhoan, tt.value.phaiTra) : 0))
})
watch(
  () => [hd.value.id, tienMatTuDong.value],
  () => {
    const p = hd.value.p
    if (p === PHU_RONG || p.tienMatTay) return
    p.tienMat = tienMatTuDong.value
  },
  { immediate: true },
)
function nhapTienMat(v) {
  const p = hd.value.p
  p.tienMat = v
  p.tienMatTay = v !== '' && v !== tienMatTuDong.value
}

/* ----- QR chuyển khoản giả lập (backend tắt giả lập thì dùng VietQR) ----- */
const ckGiaLap = ref(null) // { maHoaDon, soTien, daNhan, maGiaoDich, link }
const qrGiaLap = ref('')
let henCk = null
let hoiCk = null
function dungHoiCk() {
  clearInterval(hoiCk)
  hoiCk = null
}
async function taoQrGiaLap() {
  dungHoiCk()
  ckGiaLap.value = null
  qrGiaLap.value = ''
  const h = hd.value
  const ck = tt.value.chuyenKhoan
  if (!h.id || !ck || h.p.phuongThuc === 'TIEN_MAT') return
  try {
    const d = (await api.post(`/ban-hang/hoa-don-cho/${h.id}/chuyen-khoan`, { soTien: ck })).data
    if (!d?.maHoaDon || hd.value.id !== h.id) return
    // Mở web bằng localhost thì điện thoại không vào được -> dùng IP mạng LAN của máy (backend tự dò)
    const laLocal = ['localhost', '127.0.0.1'].includes(location.hostname)
    const goc = laLocal && d.diaChiLan ? `${location.protocol}//${d.diaChiLan}${location.port ? `:${location.port}` : ''}` : location.origin
    const link = `${goc}/thanh-toan-gia-lap?ma=${encodeURIComponent(d.maHoaDon)}&quay=1`
    ckGiaLap.value = { ...d, link }
    qrGiaLap.value = await QRCode.toDataURL(link, { width: 220, margin: 1 })
    if (d.daNhan) daNhanCk(d)
    else hoiCk = setInterval(kiemTraCk, 3000)
  } catch {
    ckGiaLap.value = null
  }
}
async function kiemTraCk() {
  const id = hd.value.id
  if (!ckGiaLap.value || !id) return dungHoiCk()
  try {
    const d = (await api.get(`/ban-hang/hoa-don-cho/${id}/chuyen-khoan`)).data
    if (d?.daNhan && hd.value.id === id) daNhanCk(d)
  } catch {
    // mất kết nối tạm thời: lần sau hỏi lại
  }
}
function daNhanCk(d) {
  dungHoiCk()
  ckGiaLap.value = { ...ckGiaLap.value, daNhan: true, maGiaoDich: d.maGiaoDich }
  hd.value.p.maGiaoDich = d.maGiaoDich || ''
  toast.success(`Đã nhận chuyển khoản ${formatMoney(d.soTien)}.`)
  // Nhận đủ tiền thì tự thanh toán luôn (kết hợp mà chưa nhập đủ tiền mặt thì chờ nhân viên bấm)
  if (!lyDoKhongThanhToan.value && !dangGui.value) thanhToan()
}
watch(
  () => [hd.value.id, tt.value.chuyenKhoan, hd.value.p.phuongThuc],
  () => {
    clearTimeout(henCk)
    henCk = setTimeout(taoQrGiaLap, 400)
  },
)
onBeforeUnmount(() => {
  dungHoiCk()
  clearTimeout(henCk)
})

const lyDoKhongThanhToan = computed(() => {
  if (canVaoCa.value) return 'Bạn chưa vào ca.'
  if (!hd.value.gio.length) return 'Hóa đơn chưa có sản phẩm.'
  if (hd.value.coGiaDoi) return 'Có sản phẩm đã đổi giá. Báo khách rồi bấm "Cập nhật giá".'
  if (loiPhieu.value) return loiPhieu.value
  if (Object.keys(loiGiao.value).length) return 'Vui lòng nhập đủ thông tin người nhận để giao hàng.'
  if (tt.value.loi === 'Vui lòng nhập tiền khách đưa.') return ''
  return tt.value.loi
})
const xacNhan = ref(false)
const dangGui = ref(false)

function moXacNhan() {
  if (hd.value.p.giaoHang) daChamGiao.value = true
  if (lyDoKhongThanhToan.value) {
    toast.error(lyDoKhongThanhToan.value)
    if (canVaoCa.value) moCa.value = true
    return
  }
  xacNhan.value = true
}

/* ----- In hóa đơn vừa bán (dùng mẫu in của trang Hóa đơn) ----- */
// Cửa sổ in phải mở ngay lúc người dùng bấm "Thanh toán" (trước khi chờ máy chủ) thì trình duyệt mới không chặn; có lỗi thì đóng lại.
function moCuaSoIn() {
  return window.open('', '_blank', 'width=820,height=900')
}

async function inVaoCuaSo(id, w) {
  const ct = (await api.get(`/hoa-don/${id}`)).data
  const qr = await QRCode.toDataURL(String(ct.hoaDon.maHoaDon), { width: 240, margin: 1 })
  w.document.write(taoHtmlHoaDon({ hd: ct.hoaDon, khach: ct.khachHang, giao: ct.giaoHang, sanPham: ct.sanPham, daThanhToan: ct.daThanhToan, qrUrl: qr, goc: location.origin }))
  w.document.close()
}

async function thanhToan() {
  if (dangGui.value) return
  dangGui.value = true
  const h = hd.value
  const p = h.p
  const tienMatGui =
    p.phuongThuc === 'CHUYEN_KHOAN'
      ? 0
      : p.tienMat !== '' && p.tienMat !== null && p.tienMat !== undefined
        ? tt.value.tienMat
        : Math.max(0, tt.value.phaiTra - (p.phuongThuc === 'KET_HOP' ? tt.value.chuyenKhoan : 0))
  const cuaSoIn = moCuaSoIn()
  try {
    const res = await api.post(`/ban-hang/hoa-don-cho/${h.id}/thanh-toan`, {
      maPhieu: p.maPhieu || null,
      soTienGiam: p.maPhieu ? giamPhieu.value : null,
      ghiChu: p.ghiChu?.trim() || null,
      tienMat: tienMatGui,
      chuyenKhoan: tt.value.chuyenKhoan,
      maGiaoDich: p.phuongThuc === 'TIEN_MAT' ? null : p.maGiaoDich?.trim() || null,
      giaoHang: p.giaoHang
        ? {
            hoTen: p.nguoiNhan.hoTen.trim(),
            sdt: p.nguoiNhan.sdt.trim(),
            diaChi: p.nguoiNhan.diaChiCuThe.trim(),
            tinhThanh: p.nguoiNhan.tinhThanh,
            phuongXa: p.nguoiNhan.phuongXa,
          }
        : null,
    })
    xacNhan.value = false
    const i = dsCho.value.findIndex((x) => x.id === h.id)
    if (i >= 0) dsCho.value.splice(i, 1)
    delete phu[h.id]
    dangChon.value = dsCho.value[Math.max(0, i - 1)]?.id ?? null
    giaoCa.taiLai()
    timSp.value?.taiLai()
    // Thanh toán xong: tự in hóa đơn và chuyển thẳng sang trang chi tiết, không hỏi lại
    if (cuaSoIn) {
      inVaoCuaSo(res.data.id, cuaSoIn).catch(() => {
        cuaSoIn.close()
        toast.error('Không in được hóa đơn. Vào trang chi tiết để in lại.')
      })
    } else {
      toast.error('Trình duyệt đã chặn cửa sổ in. Vào trang chi tiết để in lại.')
    }
    router.push(`/hoa-don/${res.data.id}`)
  } catch (e) {
    cuaSoIn?.close()
    xacNhan.value = false
    if (e.code === 'GIA_THAY_DOI') {
      toast.error(e.message)
      await taiHoaDonCho()
    } else if (e.code === 'CHUA_NHAN_CA' || e.code === 'CA_CU_CHUA_GIAO') {
      toast.error(e.message)
      await giaoCa.taiLai()
      moCa.value = true
    } else if (e.code === 'PHIEU_KHONG_HOP_LE') {
      toast.error(e.message)
      p.tuDongPhieu = true
      taiPhieu()
    } else if (e.code === 'PHIEU_THAY_DOI') {
      // Giữ phiếu đang chọn, tải lại để màn hình hiện mức giảm mới; nhân viên báo khách số tiền mới rồi bấm thanh toán lại
      toast.error(e.message)
      await taiPhieu()
    } else {
      baoLoiCho(e, 'Thanh toán không thành công.')
    }
  } finally {
    dangGui.value = false
  }
}

/* ----- Ca làm việc ----- */
const moCa = ref(false)
onMounted(async () => {
  window.addEventListener('keydown', phimTat)
  if (!giaoCa.trangThai.daTai) await giaoCa.taiLai()
  await taiHoaDonCho()
})
onBeforeUnmount(() => window.removeEventListener('keydown', phimTat))
function phimTat(e) {
  if (e.key === 'F2') {
    e.preventDefault()
    if (moChonSp.value) timSp.value?.focusTim()
    else moChonSp.value = true
  } else if (e.key === 'F9') {
    e.preventDefault()
    moXacNhan()
  }
}
const soLuongTab = (h) => tongSoLuong(h.items)
</script>

<template>
  <div class="ad-page bh">
    <div v-if="giaoCa.trangThai.daTai && canVaoCa" class="bh-chua-ca" role="alert">
      <i class="bi bi-exclamation-circle" aria-hidden="true"></i>
      <span>Bạn chưa vào ca. Vui lòng <b>vào ca</b> (nhập tiền đầu ca) trước khi bán hàng.</span>
      <button type="button" class="ad-btn ad-btn-primary ad-btn-sm" @click="moCa = true">Vào ca</button>
    </div>
    <!-- Quá giờ kết thúc ca: nhắc giao ca để người ca sau vào được (cửa hàng một két tiền) -->
    <div v-else-if="giaoCa.quaGioCa.value" class="bh-chua-ca bh-qua-gio" role="alert" data-testid="bh-qua-gio">
      <i class="bi bi-alarm" aria-hidden="true"></i>
      <span v-if="giaoCa.quaGioCa.value.caCu">Ca {{ giaoCa.trangThai.dangMo.maGiaoCa }} nhận từ hôm trước chưa giao. Vui lòng <b>giao ca</b> rồi vào ca mới.</span>
      <span v-else>{{ giaoCa.quaGioCa.value.tenCa }} đã hết lúc {{ giaoCa.quaGioCa.value.ketThuc }} (quá {{ giaoCa.quaGioCa.value.phut }} phút). Vui lòng <b>giao ca</b> trước khi về để người ca sau vào ca được.</span>
      <button type="button" class="ad-btn ad-btn-primary ad-btn-sm" @click="moCa = true">Giao ca</button>
    </div>

    <div class="bh-luoi">
      <!-- Trái: hóa đơn chờ + giỏ hàng -->
      <div class="bh-trai">
        <!-- Hóa đơn chờ -->
        <section class="ad-card bh-khoi-cho">
          <div class="bh-cho-dau">
            <button type="button" class="ad-btn ad-btn-sm" data-testid="bh-them-hd" :disabled="dsCho.length >= SO_HOA_DON_CHO_TOI_DA" @click="themHoaDon">
              <i class="bi bi-plus-lg" aria-hidden="true"></i> Thêm hóa đơn chờ
            </button>
            <span class="bh-phim">F2: chọn sản phẩm · F9: thanh toán</span>
            <span class="ad-count">{{ dsCho.length }}/{{ SO_HOA_DON_CHO_TOI_DA }} hóa đơn</span>
          </div>
          <div class="bh-tabs" role="tablist" aria-label="Hóa đơn chờ">
            <p v-if="daTaiCho && !dsCho.length" class="ad-hint m-0">Chưa có hóa đơn chờ. Chọn sản phẩm là tự tạo hóa đơn mới.</p>
            <div v-for="h in dsCho" :key="h.id" class="bh-tab" :class="{ active: h.id === dangChon }">
              <button type="button" role="tab" :aria-selected="h.id === dangChon" class="bh-tab-ten" @click="dangChon = h.id">
                <i class="bi bi-receipt" aria-hidden="true"></i> {{ h.maHoaDon }}
                <span v-if="soLuongTab(h)" class="bh-tab-so">{{ soLuongTab(h) }}</span>
              </button>
              <button type="button" class="bh-tab-xoa" :aria-label="`Hủy ${h.maHoaDon}`" title="Hủy hóa đơn chờ" @click="hoiXoaHoaDon(h)"><i class="bi bi-x" aria-hidden="true"></i></button>
            </div>
          </div>
        </section>

        <section class="ad-card">
          <header class="ad-list-head bh-gio-dau">
            <div class="ad-card-head">
              <span class="ad-icon-box" aria-hidden="true"><i class="bi bi-cart3"></i></span>
              <h2 class="ad-card-title">Giỏ hàng</h2>
              <span class="ad-pill ad-pill-blue">{{ tongSoLuong(hd.gio) }} sản phẩm</span>
            </div>
            <div class="bh-gio-cong-cu">
              <form class="ad-affix bh-o-ma" @submit.prevent="quetMa()">
                <i class="bi bi-upc-scan" aria-hidden="true"></i>
                <input ref="oMa" v-model="maQuet" type="text" class="form-control ad-control" placeholder="Quét / nhập mã sản phẩm" aria-label="Quét mã sản phẩm" autocomplete="off" />
              </form>
              <button type="button" class="ad-btn" title="Quét bằng camera" @click="moCamera = true"><i class="bi bi-camera" aria-hidden="true"></i></button>
              <button type="button" class="ad-btn ad-btn-primary" data-testid="bh-chon-sp" @click="moChonSp = true">
                <i class="bi bi-plus-circle" aria-hidden="true"></i> Chọn sản phẩm
              </button>
            </div>
          </header>
          <div v-if="hd.coGiaDoi" class="bh-canh-gia" role="alert">
            <i class="bi bi-exclamation-triangle" aria-hidden="true"></i>
            <span>Giá của sản phẩm trong hóa đơn đã thay đổi so với lúc thêm. Báo khách giá mới rồi cập nhật trước khi thanh toán.</span>
            <button type="button" class="ad-btn ad-btn-sm ad-btn-primary" data-testid="bh-cap-nhat-gia" @click="capNhatGiaMoi">Cập nhật giá</button>
          </div>
          <div class="ad-table-wrap">
            <div class="ad-table-scroll">
              <table class="ad-table bh-gio">
                <thead>
                  <tr>
                    <th class="ad-col-stt">STT</th>
                    <th class="text-center bh-cot-anh">Ảnh</th>
                    <th>Sản phẩm</th>
                    <th>Phân loại</th>
                    <th>Đơn giá</th>
                    <th class="text-center">Số lượng</th>
                    <th>Thành tiền</th>
                    <th class="text-center bh-cot-thao-tac">Thao tác</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-if="!hd.gio.length">
                    <td colspan="8">
                      <div class="ad-empty">
                        <i class="bi bi-cart" aria-hidden="true"></i>
                        <strong>Chưa có sản phẩm</strong>
                        <span>Bấm "Chọn sản phẩm" hoặc quét mã để thêm vào hóa đơn.</span>
                      </div>
                    </td>
                  </tr>
                  <tr v-for="(d, i) in hd.gio" :key="d.id">
                    <td class="ad-col-stt">{{ i + 1 }}</td>
                    <td class="text-center">
                      <span class="bh-dong-anh"><img v-if="d.anh" :src="d.anh" :alt="d.ten" /><i v-else class="bi bi-image" aria-hidden="true"></i></span>
                    </td>
                    <td class="bh-cot-ten">
                      <span class="fw-semibold d-block">{{ d.ten }}</span>
                      <small class="ad-code">{{ d.ma }}</small>
                    </td>
                    <td class="text-muted">{{ d.phanLoai || '—' }}</td>
                    <td class="ad-nowrap">
                      <div class="fw-medium">{{ formatMoney(d.donGia) }}</div>
                      <small v-if="dangGiamTheoDot(d)" class="text-muted"><s>{{ formatMoney(d.giaGoc) }}</s></small>
                      <small v-if="d.giaDoi" class="bh-gia-doi d-block"><i class="bi bi-arrow-repeat" aria-hidden="true"></i> Giá mới {{ formatMoney(d.giaHienTai) }}</small>
                    </td>
                    <td class="text-center">
                      <div class="bh-cot-sl">
                        <div class="bh-sl">
                          <button type="button" :aria-label="`Giảm số lượng ${d.ten}`" :disabled="d.soLuong <= 1" @click="doiSl(d, d.soLuong - 1)"><i class="bi bi-dash"></i></button>
                          <input :value="d.soLuong" type="number" min="1" :max="d.soLuong + d.tonKho" :disabled="dangLam" :aria-label="`Số lượng ${d.ten}`" @change="doiSl(d, $event.target.value)" />
                          <button type="button" :aria-label="`Tăng số lượng ${d.ten}`" :disabled="dangLam || d.tonKho <= 0" @click="doiSl(d, d.soLuong + 1)"><i class="bi bi-plus"></i></button>
                        </div>
                        <small class="text-muted">Kho còn {{ d.tonKho }}</small>
                      </div>
                    </td>
                    <td class="fw-semibold ad-nowrap bh-thanh-tien">{{ formatMoney(d.donGia * d.soLuong) }}</td>
                    <td class="text-center">
                      <button type="button" class="ad-icon-btn is-danger" :aria-label="`Bỏ ${d.ten} khỏi hóa đơn`" title="Bỏ khỏi hóa đơn" @click="xoaDong(d)"><i class="bi bi-trash" aria-hidden="true"></i></button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </section>

        <!-- Giao hàng (bật ở "Thông tin đơn hàng"): người nhận + địa chỉ theo Tỉnh/Thành -> Phường/Xã, phí tính theo tỉnh -->
        <section v-if="hd.p.giaoHang" class="ad-card bh-khoi-giao" data-testid="bh-khoi-giao">
          <div class="bh-tieu-de-hang">
            <h3 class="bh-tieu-de m-0"><i class="bi bi-truck" aria-hidden="true"></i> Thông tin giao hàng</h3>
            <VanChuyenLogo :ten="DON_VI_VAN_CHUYEN" />
          </div>
          <div class="row g-2">
            <div class="col-md-6">
              <label class="ad-label" for="bh-nn-ten">Người nhận <span class="ad-required">*</span></label>
              <input id="bh-nn-ten" v-model="hd.p.nguoiNhan.hoTen" type="text" class="form-control ad-control" :class="{ 'is-invalid': daChamGiao && loiGiao.hoTen }" maxlength="255" placeholder="Họ tên người nhận" />
              <p v-if="daChamGiao && loiGiao.hoTen" class="ad-error">{{ loiGiao.hoTen }}</p>
            </div>
            <div class="col-md-6">
              <label class="ad-label" for="bh-nn-sdt">Số điện thoại <span class="ad-required">*</span></label>
              <input id="bh-nn-sdt" :value="hd.p.nguoiNhan.sdt" type="tel" inputmode="numeric" class="form-control ad-control" :class="{ 'is-invalid': (daChamGiao || daChamSdtNhan) && loiGiao.sdt }" maxlength="10" placeholder="Nhập số điện thoại" @input="nhapSdtNhan" @blur="daChamSdtNhan = true" />
              <p v-if="(daChamGiao || daChamSdtNhan) && loiGiao.sdt" class="ad-error">{{ loiGiao.sdt }}</p>
            </div>
            <DiaChiHanhChinhSelect
              v-model:tinh-thanh="hd.p.nguoiNhan.tinhThanh"
              v-model:phuong-xa="hd.p.nguoiNhan.phuongXa"
              id-prefix="bh-nn"
              col-class="col-md-6"
              select-class="ad-control"
              label-class="ad-label"
              required
              :error-tinh="daChamGiao ? loiGiao.tinhThanh || '' : ''"
              :error-phuong="daChamGiao ? loiGiao.phuongXa || '' : ''"
            />
            <div class="col-12">
              <label class="ad-label" for="bh-nn-dc">Địa chỉ cụ thể <span class="ad-required">*</span></label>
              <input id="bh-nn-dc" v-model="hd.p.nguoiNhan.diaChiCuThe" type="text" class="form-control ad-control" :class="{ 'is-invalid': daChamGiao && loiGiao.diaChiCuThe }" maxlength="255" placeholder="Số nhà, tên ngõ / đường" />
              <p v-if="daChamGiao && loiGiao.diaChiCuThe" class="ad-error">{{ loiGiao.diaChiCuThe }}</p>
            </div>
          </div>
          <div class="bh-giao-phi">
            <span>
              Phí giao hàng:
              <b v-if="phiShipTheoTinh === null" class="text-muted fw-normal">chọn Tỉnh/Thành phố để tính</b>
              <b v-else>{{ phiShip ? formatMoney(phiShip) : 'Miễn phí' }}</b>
            </span>
            <small class="text-muted">
              Hà Nội miễn phí, tỉnh/thành khác tính phí theo từng tỉnh
            </small>
          </div>
          <p class="ad-hint mb-0">Khách trả tiền tại quầy; thanh toán xong đơn chuyển sang <b>Chờ giao hàng</b> (chờ đơn vị vận chuyển đến lấy).</p>
        </section>
      </div>

      <!-- Phải: khách, phiếu, thanh toán -->
      <aside class="bh-phai">
        <section class="ad-card bh-khoi">
          <h3 class="bh-tieu-de"><i class="bi bi-person" aria-hidden="true"></i> Khách hàng</h3>
          <ChonKhach :khach="hd.khach" @chon="chonKhach" />
        </section>

        <section class="ad-card bh-khoi">
          <div class="bh-tieu-de-hang">
            <h3 class="bh-tieu-de m-0"><i class="bi bi-receipt-cutoff" aria-hidden="true"></i> Thông tin đơn hàng</h3>
            <label class="bh-cong-tac" title="Giao hàng cho khách">
              <span>Giao hàng</span>
              <input type="checkbox" role="switch" class="bh-cong-tac-o" :checked="hd.p.giaoHang" data-testid="bh-giao-hang" @change="batGiaoHang($event.target.checked)" />
              <span class="bh-cong-tac-ray" aria-hidden="true"><span class="bh-cong-tac-nut"></span></span>
            </label>
          </div>

          <!-- Phiếu giảm giá (cô review 04/11): không còn ô dạng combo box. Hệ thống tự áp phiếu giảm nhiều nhất và hiện luôn
               phiếu đang áp dụng thành một dòng giống dòng gợi ý "Mua thêm" bên dưới; muốn đổi / bỏ phiếu thì bấm vào chính dòng này
               (có mũi tên ở cuối) để mở pop-up, không có nút riêng. -->
          <div
            id="bh-phieu"
            class="bh-phieu-ap"
            :class="{ 'is-trong': !(phieuChon && giamPhieu), 'is-bam-duoc': coTheChonPhieu }"
            data-testid="bh-phieu-ap-dung"
            :role="coTheChonPhieu ? 'button' : undefined"
            :tabindex="coTheChonPhieu ? 0 : undefined"
            :aria-haspopup="coTheChonPhieu ? 'dialog' : undefined"
            :aria-label="coTheChonPhieu ? 'Chọn hoặc bỏ phiếu giảm giá' : undefined"
            @click="coTheChonPhieu && (moPhieu = true)"
            @keydown.enter.prevent="coTheChonPhieu && (moPhieu = true)"
            @keydown.space.prevent="coTheChonPhieu && (moPhieu = true)"
          >
            <i class="bi bi-ticket-perforated" aria-hidden="true"></i>
            <span class="bh-phieu-ap-chu" :title="phieuChon && giamPhieu ? phieuChon.ten : ''">
              <template v-if="phieuChon && giamPhieu">
                Đang áp dụng phiếu <b>{{ phieuChon.ma }}</b> (giảm <b>{{ formatMoney(giamPhieu) }}</b>)
                <span v-if="phieuChon.ma === phieuDung[0]?.ma" class="bh-phieu-tot">Tốt nhất</span>
              </template>
              <template v-else-if="!hd.gio.length">Phiếu giảm giá: chưa có sản phẩm</template>
              <template v-else-if="dangTaiPhieu && !phieus.length">Đang tìm phiếu giảm giá…</template>
              <template v-else-if="soPhieuDung">Không dùng phiếu ({{ soPhieuDung }} phiếu dùng được)</template>
              <template v-else>Không có phiếu dùng được cho đơn này</template>
            </span>
            <i v-if="coTheChonPhieu" class="bi bi-chevron-right bh-phieu-mui" aria-hidden="true"></i>
          </div>
          <p v-if="goiYMuaThem.length" class="bh-goi-y-ngan" data-testid="bh-goi-y-ngan">
            <i class="bi bi-lightbulb" aria-hidden="true"></i>
            Mua thêm <b>{{ formatMoney(goiYMuaThem[0].canMuaThem) }}</b> để dùng phiếu {{ goiYMuaThem[0].ma }} (giảm {{ formatMoney(goiYMuaThem[0].giamKhiDu) }})
          </p>
          <p v-if="loiPhieu" class="ad-error mb-0">{{ loiPhieu }}</p>

          <dl class="bh-tong bh-tong-don">
            <div><dt>Tổng tiền hàng ({{ tongSoLuong(hd.gio) }} sản phẩm):</dt><dd>{{ formatMoney(tienHang) }}</dd></div>
            <div v-if="giamDot > 0" class="bh-dong-phu"><dt>Đã giảm theo đợt giảm giá</dt><dd>{{ formatMoney(giamDot) }}</dd></div>
            <div v-if="hd.p.giaoHang" class="bh-dong-ship" data-testid="bh-phi-ship">
              <dt><VanChuyenLogo :ten="DON_VI_VAN_CHUYEN" class="bh-ghn" /> Phí vận chuyển:</dt>
              <dd>{{ phiShipTheoTinh === null ? 'Chưa tính phí' : phiShip ? formatMoney(phiShip) : 'Miễn phí' }}</dd>
            </div>
            <div><dt>Tiền giảm từ phiếu:</dt><dd class="bh-tien-giam">-{{ formatMoney(giamPhieu) }}</dd></div>
            <div class="bh-phai-tra"><dt>Khách cần trả:</dt><dd>{{ formatMoney(tt.phaiTra) }}</dd></div>
          </dl>

          <fieldset class="bh-pt">
            <legend class="bh-nhan-muc">Hình thức thanh toán</legend>
            <div v-for="p in PHUONG_THUC" :key="p.ma" class="form-check form-check-inline">
              <input :id="`bh-pt-${p.ma}`" class="form-check-input" type="radio" name="bh-pt" :value="p.ma" :checked="hd.p.phuongThuc === p.ma" @change="chonPhuongThuc(p.ma)" />
              <label class="form-check-label" :for="`bh-pt-${p.ma}`">{{ p.ten }}</label>
            </div>
          </fieldset>

          <div v-if="hd.p.phuongThuc === 'KET_HOP'" class="mt-2">
            <label class="bh-nhan-muc" for="bh-ck">Tiền chuyển khoản</label>
            <MoneyInput id="bh-ck" v-model="hd.p.chuyenKhoan" aria-label="Số tiền chuyển khoản" placeholder="Số tiền khách chuyển khoản" />
          </div>
          <div v-if="hd.p.phuongThuc !== 'CHUYEN_KHOAN'" class="mt-2">
            <label class="bh-nhan-muc" for="bh-tien-mat">Tiền mặt khách đưa</label>
            <MoneyInput id="bh-tien-mat" :model-value="hd.p.tienMat" aria-label="Tiền khách đưa" placeholder="Tự điền bằng số tiền khách cần trả" @update:model-value="nhapTienMat" />
            <p class="ad-hint mb-0 mt-1">Điền sẵn đúng số tiền cần trả. Có thể sửa nếu khách đưa số tiền khác.</p>
          </div>
          <!-- QR chuyển khoản (giả lập) -->
          <div v-if="hd.p.phuongThuc !== 'TIEN_MAT' && tt.chuyenKhoan > 0 && ckGiaLap" class="bh-qr" data-testid="bh-qr-gia-lap">
            <img v-if="qrGiaLap" :src="qrGiaLap" :alt="`Mã QR chuyển khoản ${formatMoney(tt.chuyenKhoan)}`" width="150" height="150" />
            <div>
              <div class="fw-semibold">Quét mã để thanh toán</div>
              <div class="small">Số tiền: <b>{{ formatMoney(ckGiaLap.soTien) }}</b></div>
              <div class="small">Nội dung: <b>{{ ckGiaLap.maHoaDon }}</b></div>
              <div v-if="ckGiaLap.daNhan" class="bh-ck-da-nhan"><i class="bi bi-check-circle-fill" aria-hidden="true"></i> Đã nhận tiền</div>
              <div v-else class="bh-ck-cho"><span class="spinner-border spinner-border-sm" aria-hidden="true"></span> Đang chờ khách chuyển khoản</div>
            </div>
          </div>
          <div v-else-if="hd.p.phuongThuc !== 'TIEN_MAT' && tt.chuyenKhoan > 0" class="bh-qr">
            <img v-if="qrUrl" :src="qrUrl" :alt="`Mã QR chuyển khoản ${formatMoney(tt.chuyenKhoan)}`" width="150" height="150" />
            <div>
              <template v-if="coQr">
                <div class="fw-semibold">{{ NGAN_HANG.tenNganHang || 'Ngân hàng' }} · {{ NGAN_HANG.soTaiKhoan }}</div>
                <div class="small">{{ NGAN_HANG.chuTaiKhoan }}</div>
                <div class="small">Số tiền: <b>{{ formatMoney(tt.chuyenKhoan) }}</b></div>
                <div class="small">Nội dung: <b>{{ noiDungCk }}</b></div>
              </template>
              <div v-else class="small text-muted">Chưa cấu hình tài khoản nhận tiền nên không hiện mã QR (xem frontend/src/constants/thanhToan.js).</div>
              <label class="ad-label mt-2" for="bh-ma-gd">Mã giao dịch</label>
              <input id="bh-ma-gd" v-model="hd.p.maGiaoDich" type="text" class="form-control ad-control" maxlength="100" placeholder="Mã giao dịch ngân hàng (nếu có)" />
            </div>
          </div>

          <dl class="bh-tong mt-2">
            <div v-if="tt.conThieu > 0 && hd.p.tienMat" class="text-danger fw-semibold"><dt>Còn thiếu</dt><dd>{{ formatMoney(tt.conThieu) }}</dd></div>
            <div v-if="tt.tienThua > 0" class="bh-thua"><dt>Tiền thừa trả khách</dt><dd>{{ formatMoney(tt.tienThua) }}</dd></div>
          </dl>
          <label class="ad-label" for="bh-ghi-chu">Ghi chú</label>
          <input id="bh-ghi-chu" v-model="hd.p.ghiChu" type="text" class="form-control ad-control" maxlength="500" placeholder="Ghi chú cho hóa đơn (không bắt buộc)" />

          <!-- Hủy hóa đơn chờ bằng dấu X trên tab hóa đơn -->
          <div class="bh-nut-hang">
            <button type="button" class="ad-btn ad-btn-primary bh-nut-tt" :disabled="dangGui" :title="lyDoKhongThanhToan" @click="moXacNhan">
              <i class="bi bi-check2-circle" aria-hidden="true"></i> Thanh toán {{ formatMoney(tt.phaiTra) }}
            </button>
          </div>
          <p v-if="lyDoKhongThanhToan && hd.gio.length" class="ad-hint text-center mb-0 mt-1">{{ lyDoKhongThanhToan }}</p>
        </section>
      </aside>
    </div>

    <!-- Hộp thoại chọn phiếu: chỉ phiếu đơn này dùng được (chung + cá nhân), tốt nhất lên đầu; kèm gợi ý mua thêm -->
    <BaseModal v-if="moPhieu" title="Chọn phiếu giảm giá" size="md" @close="moPhieu = false">
      <p class="bh-phieu-dong-tom">
        Tiền hàng <b>{{ formatMoney(tienHang) }}</b> · {{ soPhieuDung }} phiếu dùng được
      </p>
      <ul class="bh-phieu-ds" role="listbox" aria-label="Phiếu giảm giá dùng được">
        <li>
          <button type="button" role="option" class="bh-phieu-muc" :class="{ 'is-chon': !hd.p.maPhieu }" :aria-selected="!hd.p.maPhieu" @click="chonPhieuMo('')">
            <span class="bh-phieu-muc-chinh"><b>Không dùng phiếu</b></span>
            <i v-if="!hd.p.maPhieu" class="bi bi-check-lg bh-phieu-tick" aria-hidden="true"></i>
          </button>
        </li>
        <li v-for="(p, i) in phieuDung" :key="p.id">
          <button
            type="button"
            role="option"
            class="bh-phieu-muc"
            :class="{ 'is-chon': hd.p.maPhieu === p.ma }"
            :aria-selected="hd.p.maPhieu === p.ma"
            @click="chonPhieuMo(p.ma)"
          >
            <span class="bh-phieu-muc-chinh">
              <span class="d-flex align-items-center gap-2 flex-wrap">
                <b>{{ p.ma }}</b>
                <span v-if="i === 0" class="bh-nhan is-tot">Tốt nhất</span>
                <span v-if="p.caNhan" class="bh-nhan is-ca-nhan">Cá nhân</span>
              </span>
              <small class="d-block">{{ p.ten }} · {{ moTaPhieu(p) }}</small>
            </span>
            <b class="bh-phieu-giam">-{{ formatMoney(p.soTienGiam) }}</b>
            <i v-if="hd.p.maPhieu === p.ma" class="bi bi-check-lg bh-phieu-tick" aria-hidden="true"></i>
          </button>
        </li>
        <li v-if="!phieuDung.length" class="bh-phieu-trong">Chưa có phiếu nào đủ điều kiện cho đơn này.</li>
      </ul>
      <div v-if="goiYMuaThem.length" class="bh-goi-y" data-testid="bh-goi-y-mua-them">
        <ul>
          <li v-for="p in goiYMuaThem" :key="p.id">
            <i class="bi bi-lightbulb" aria-hidden="true"></i>
            Mua thêm <b>{{ formatMoney(p.canMuaThem) }}</b> để dùng <b>{{ p.ma }}</b>, giảm <b>{{ formatMoney(p.giamKhiDu) }}</b>
            <small class="text-muted">({{ moTaPhieu(p) }})</small>
          </li>
        </ul>
      </div>
    </BaseModal>

    <!-- Hộp thoại xác nhận thanh toán chuẩn nghiệp vụ POS -->
    <BaseModal v-if="xacNhan" title="Xác nhận thanh toán" size="md" @close="xacNhan = false">
      <div class="bh-xn-modal">
        <!-- Khối hiển thị số tiền thanh toán chính nổi bật -->
        <div class="bh-xn-tong">
          <div class="bh-xn-tong-nhan">TỔNG TIỀN THANH TOÁN</div>
          <div class="bh-xn-tong-so">{{ formatMoney(tt.phaiTra) }}</div>
        </div>

        <!-- Bảng chi tiết tóm tắt đơn thanh toán -->
        <div class="bh-xn-thong-tin">
          <div class="bh-xn-dong">
            <span class="bh-xn-nhan"><i class="bi bi-receipt me-1 text-primary"></i>Hóa đơn:</span>
            <span class="bh-xn-gia-tri fw-semibold">{{ hd.ten }} ({{ tongSoLuong(hd.gio) }} sản phẩm)</span>
          </div>
          <div class="bh-xn-dong">
            <span class="bh-xn-nhan"><i class="bi bi-person me-1 text-primary"></i>Khách hàng:</span>
            <span class="bh-xn-gia-tri">{{ hd.khach?.hoTen ? `${hd.khach.hoTen} (${hd.khach.sdt || '—'})` : 'Khách lẻ (Khách vãng lai)' }}</span>
          </div>
          <div v-if="hd.p.giaoHang" class="bh-xn-dong">
            <span class="bh-xn-nhan"><i class="bi bi-truck me-1 text-primary"></i>Giao tới:</span>
            <span class="bh-xn-gia-tri">{{ hd.p.nguoiNhan.hoTen }} · {{ hd.p.nguoiNhan.sdt }}<br />{{ hd.p.nguoiNhan.diaChi }}</span>
          </div>
          <div class="bh-xn-dong">
            <span class="bh-xn-nhan"><i class="bi bi-credit-card me-1 text-primary"></i>Phương thức:</span>
            <span class="bh-xn-gia-tri fw-medium">
              <span class="ad-pill" :class="hd.p.phuongThuc === 'CHUYEN_KHOAN' ? 'ad-pill-blue' : hd.p.phuongThuc === 'KET_HOP' ? 'ad-pill-purple' : 'ad-pill-green'">
                {{ hd.p.phuongThuc === 'CHUYEN_KHOAN' ? 'Chuyển khoản' : hd.p.phuongThuc === 'KET_HOP' ? 'Kết hợp' : 'Tiền mặt' }}
              </span>
            </span>
          </div>
          <div v-if="tt.chuyenKhoan > 0" class="bh-xn-dong">
            <span class="bh-xn-nhan">Tiền chuyển khoản:</span>
            <span class="bh-xn-gia-tri fw-semibold text-primary">{{ formatMoney(tt.chuyenKhoan) }}</span>
          </div>
          <div v-if="tt.tienMat > 0 && hd.p.phuongThuc !== 'CHUYEN_KHOAN'" class="bh-xn-dong">
            <span class="bh-xn-nhan">Tiền khách đưa:</span>
            <span class="bh-xn-gia-tri fw-semibold text-dark">{{ formatMoney(tt.tienMat) }}</span>
          </div>
          <div v-if="tt.tienThua > 0" class="bh-xn-dong bh-xn-dong-thua">
            <span class="bh-xn-nhan text-success fw-semibold"><i class="bi bi-cash-stack me-1"></i>Tiền thừa trả khách:</span>
            <span class="bh-xn-gia-tri text-success fw-bold fs-6">{{ formatMoney(tt.tienThua) }}</span>
          </div>
        </div>
      </div>

      <template #footer>
        <button type="button" class="ad-btn" :disabled="dangGui" @click="xacNhan = false">
          Hủy
        </button>
        <button
          type="button"
          class="ad-btn ad-btn-primary px-4 fw-medium"
          :disabled="dangGui"
          @click="thanhToan"
        ><span v-if="dangGui" class="spinner-border spinner-border-sm me-1" aria-hidden="true"></span>Thanh toán</button>
      </template>
    </BaseModal>
    <ConfirmDialog
      v-if="xoaHd"
      title="Xóa hóa đơn chờ"
      :message="`${xoaHd.maHoaDon} đang có ${xoaHd.soLuong} sản phẩm. Hủy hóa đơn chờ này? Sản phẩm sẽ được trả lại kho.`"
      confirm-text="Xóa"
      variant="danger"
      @confirm="xoaHoaDon(xoaHd)"
      @cancel="xoaHd = null"
    />
    <GiaoCaModal v-if="moCa" @close="moCa = false" />

    <!-- Chọn sản phẩm: hộp thoại có lọc + bảng + phân trang -->
    <BaseModal v-if="moChonSp" title="Chọn sản phẩm" size="xl" @close="moChonSp = false">
      <TimSanPham ref="timSp" trong-hop @chon="themSanPham" />
      <template #footer>
        <span class="me-auto small text-muted">Đang thêm vào <b>{{ hd.maHoaDon || 'hóa đơn mới' }}</b> · {{ tongSoLuong(hd.gio) }} sản phẩm trong giỏ</span>
        <button type="button" class="ad-btn" data-testid="bh-dong-chon-sp" @click="moChonSp = false">Đóng</button>
      </template>
    </BaseModal>
    <QuetMaModal v-if="moCamera" @close="moCamera = false" @ma="onMaCamera" />
  </div>
</template>


<style scoped>
.bh-canh-gia {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  margin: 0 1rem 0.75rem;
  padding: 0.6rem 0.8rem;
  border-radius: 10px;
  background: #fff7ed;
  color: #9a3412;
  font-size: 0.9rem;
}
.bh-canh-gia span {
  flex: 1;
}
.bh-gia-doi {
  color: #c2410c;
  font-weight: 600;
}
.bh-khoi-giao .bh-tieu-de-hang {
  margin-bottom: 0.75rem;
}
.bh-giao-phi {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  justify-content: space-between;
  gap: 0.25rem 1rem;
  margin: 0.75rem 0 0.4rem;
  padding: 0.55rem 0.75rem;
  border-radius: 10px;
  background: #f1f5ff;
}
.bh-phieu-trong {
  padding: 0.6rem 0.75rem;
  color: #64748b;
  font-size: 0.85rem;
}
.bh-chua-ca {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  background: #fff4e5;
  color: #92400e;
  border-radius: 12px;
  padding: 0.75rem 1rem;
  margin-bottom: 1rem;
}
.bh-qua-gio {
  border: 1px solid #fdba74;
  background: #fff7ed;
  color: #9a3412;
}
.bh-chua-ca span {
  flex: 1;
}
.bh-tabs {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.4rem;
}
/* Khối hóa đơn chờ (trên giỏ hàng) */
.bh-khoi-cho {
  padding: 0.85rem 1rem;
}
.bh-cho-dau {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.65rem;
}
.bh-cho-dau .bh-phim {
  margin-left: auto;
}
/* Đầu giỏ hàng: ô quét mã + camera + Chọn sản phẩm */
.bh-gio-dau {
  flex-wrap: wrap;
  gap: 0.75rem;
}
.bh-gio-dau .ad-card-head {
  gap: 0.6rem;
}
.bh-gio-cong-cu {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem;
}
.bh-o-ma {
  width: 250px;
}
/* Bảng giỏ hàng: cột ảnh / thao tác hẹp và căn giữa, số lượng + tồn kho xếp trên dưới */
.bh-gio th,
.bh-gio td {
  vertical-align: middle;
}
.bh-cot-anh {
  width: 72px;
}
.bh-cot-thao-tac {
  width: 90px;
}
.bh-cot-ten {
  min-width: 180px;
}
.bh-thanh-tien {
  color: var(--fs-primary-dark, #075fc0);
}
.bh-cot-sl {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  gap: 0.25rem;
}
/* Tiêu đề Thông tin đơn hàng + công tắc Giao hàng */
.bh-tieu-de-hang {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 0.75rem;
}
.bh-cong-tac {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.85rem;
  font-weight: 600;
  color: #475569;
  cursor: pointer;
}
/* Công tắc gạt (ô checkbox thật ẩn đi, vẫn bấm / Tab / Space được) */
.bh-cong-tac {
  position: relative;
}
.bh-cong-tac-o {
  position: absolute;
  opacity: 0;
  width: 1px;
  height: 1px;
}
.bh-cong-tac-ray {
  position: relative;
  flex: none;
  width: 40px;
  height: 22px;
  border-radius: 999px;
  background: #cbd5e1;
  transition: background-color 0.2s ease;
}
.bh-cong-tac-nut {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.3);
  transition: transform 0.2s ease;
}
.bh-cong-tac-o:checked + .bh-cong-tac-ray {
  background: var(--fs-primary, #0977ec);
}
.bh-cong-tac-o:checked + .bh-cong-tac-ray .bh-cong-tac-nut {
  transform: translateX(18px);
}
.bh-cong-tac-o:focus-visible + .bh-cong-tac-ray {
  outline: 3px solid rgba(9, 119, 236, 0.35);
  outline-offset: 2px;
}
@media (prefers-reduced-motion: reduce) {
  .bh-cong-tac-ray,
  .bh-cong-tac-nut {
    transition: none;
  }
}
/* ----- Dòng phiếu giảm giá: nhãn bên trái, ô chọn bên phải ----- */
.bh-dong-phieu {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}
.bh-nhan-dong {
  flex: none;
  font-size: 0.86rem;
  color: var(--fs-muted, #64748b);
}
/* Phiếu đang áp dụng: cùng kiểu với dòng gợi ý "Mua thêm" (cô: hai dòng trình bày giống nhau) */
.bh-phieu-ap {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.45rem 0.6rem;
  border-radius: 8px;
  background: #ecfdf5;
  color: #1e293b;
  font-size: 0.85rem;
}
.bh-phieu-ap > .bi {
  flex: none;
  color: #16a34a;
  font-size: 1rem;
}
.bh-phieu-ap.is-trong {
  background: #f8fafc;
  color: var(--fs-muted, #64748b);
}
.bh-phieu-ap.is-trong > .bi {
  color: var(--fs-muted, #64748b);
}
.bh-phieu-ap-chu {
  flex: 1;
  min-width: 0;
}
.bh-phieu-ap-chu b {
  color: #15803d;
}
.bh-phieu-tot {
  display: inline-block;
  margin-left: 0.25rem;
  padding: 0 0.4rem;
  border-radius: 999px;
  background: #16a34a;
  color: #fff;
  font-size: 0.7rem;
  font-weight: 700;
  vertical-align: 1px;
}
/* Cả dòng bấm được để chọn / bỏ phiếu: mũi tên ở cuối, rê chuột thì đậm hơn */
.bh-phieu-ap.is-bam-duoc {
  cursor: pointer;
  transition: background-color 0.15s ease, box-shadow 0.15s ease;
}
.bh-phieu-ap.is-bam-duoc:hover {
  box-shadow: inset 0 0 0 1px #86efac;
}
.bh-phieu-ap.is-trong.is-bam-duoc:hover {
  box-shadow: inset 0 0 0 1px #cbd5e1;
}
.bh-phieu-ap.is-bam-duoc:focus-visible {
  outline: 2px solid var(--fs-primary, #0977ec);
  outline-offset: 2px;
}
.bh-phieu-mui {
  flex: none;
  color: #94a3b8;
  font-size: 0.85rem;
}
.bh-phieu-giam {
  flex: none;
  color: #16a34a;
  font-size: 0.95rem;
}
.bh-phieu-ds {
  max-height: 340px;
  overflow-y: auto;
  margin: 0;
  padding: 0.35rem;
  list-style: none;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  background: #fff;
}
.bh-phieu-dong-tom {
  margin: 0 0 0.6rem;
  color: #475569;
  font-size: 0.9rem;
}
/* Gợi ý mua thêm: nền xanh nhạt, chữ đậm cho dễ đọc */
.bh-goi-y {
  margin-top: 0.75rem;
  padding: 0.65rem 0.8rem;
  border: 1px solid #bfdbfe;
  border-radius: 12px;
  background: #eff6ff;
  color: #1e293b;
  font-size: 0.88rem;
}
.bh-goi-y ul {
  display: grid;
  gap: 0.4rem;
  margin: 0;
  padding: 0;
  list-style: none;
}
.bh-goi-y b,
.bh-goi-y-ngan b {
  color: var(--fs-primary-dark, #075fc0);
}
.bh-goi-y .bi-lightbulb,
.bh-goi-y-ngan .bi-lightbulb {
  color: #f59e0b;
}
.bh-goi-y-ngan {
  margin: 0.4rem 0 0;
  padding: 0.4rem 0.6rem;
  border-radius: 8px;
  background: #eff6ff;
  color: #1e293b;
  font-size: 0.82rem;
}
.bh-phieu-muc {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  width: 100%;
  padding: 0.6rem 0.7rem;
  border: 0;
  border-radius: 10px;
  background: transparent;
  text-align: left;
  cursor: pointer;
}
.bh-phieu-muc:hover:not(:disabled) {
  background: #f1f6fd;
}
.bh-phieu-muc.is-chon {
  background: var(--fs-primary-soft, #e7f1fd);
}
.bh-phieu-muc-chinh {
  flex: 1;
  min-width: 0;
}
.bh-phieu-muc-chinh small {
  color: var(--fs-muted, #64748b);
  font-size: 0.78rem;
}
.bh-phieu-tick {
  flex: none;
  color: var(--fs-primary, #0977ec);
  font-size: 1.05rem;
}
.bh-nhan {
  display: inline-block;
  padding: 0.05rem 0.45rem;
  border-radius: 999px;
  font-size: 0.68rem;
  font-weight: 700;
}
.bh-nhan.is-tot {
  background: #dcfce7;
  color: #15803d;
}
.bh-nhan.is-ca-nhan {
  background: #f3e8ff;
  color: #7e22ce;
}
.bh-nut-hang {
  display: grid;
  margin-top: 1rem;
}
.bh-nut-hang .ad-btn {
  min-width: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  padding-inline: 0.6rem;
}
.bh-nut-hang .bh-nut-tt {
  margin-top: 0;
  font-size: 0.98rem;
}
.bh-tab {
  display: flex;
  align-items: center;
  border: 1px solid var(--fs-line, #e6eaf0);
  border-radius: 10px;
  background: #fff;
  overflow: hidden;
}
.bh-tab.active {
  border-color: var(--fs-primary, #0977ec);
  background: var(--fs-primary-soft, #e7f1fd);
}
.bh-tab-ten {
  border: 0;
  background: transparent;
  padding: 0.45rem 0.4rem 0.45rem 0.75rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 0.4rem;
}
.bh-tab.active .bh-tab-ten {
  color: var(--fs-primary-dark, #075fc0);
}
.bh-tab-so {
  min-width: 1.3rem;
  border-radius: 999px;
  background: var(--fs-primary, #0977ec);
  color: #fff;
  font-size: 0.72rem;
  padding: 0 0.35rem;
}
/* Nút x hủy hóa đơn chờ: màu xanh thương hiệu (giống logo) */
.bh-tab-xoa {
  border: 0;
  background: transparent;
  padding: 0.45rem 0.55rem;
  color: var(--fs-primary, #0977ec);
}
.bh-tab-xoa:hover {
  color: var(--fs-primary-dark, #075fc0);
  background: var(--fs-primary-soft, #e7f1fd);
}
.bh-tab-them {
  border: 1px dashed #c9d3df;
  background: transparent;
  border-radius: 10px;
  padding: 0.45rem 0.75rem;
  font-weight: 600;
  color: var(--fs-text-2, #4b5563);
}
.bh-tab-them:disabled {
  opacity: 0.5;
}
.bh-phim {
  margin-left: auto;
  font-size: 0.8rem;
  color: var(--fs-muted, #7b8794);
}
.bh-luoi {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(360px, 420px);
  gap: 1rem;
  align-items: start;
}
.bh-trai {
  display: grid;
  gap: 1rem;
  min-width: 0;
}
.bh-phai {
  display: grid;
  gap: 1rem;
  position: sticky;
  top: 1rem;
  /* Không để chữ dài (tên phiếu, số tiền) đẩy cột rộng ra ngoài màn hình */
  min-width: 0;
}
.bh-phai > .ad-card {
  min-width: 0;
  max-width: 100%;
}
.bh-khoi {
  padding: 1rem;
}
.bh-tieu-de {
  font-size: 0.98rem;
  font-weight: 700;
  margin: 0 0 0.75rem;
  display: flex;
  gap: 0.45rem;
  align-items: center;
}
.bh-dong-sp {
  display: flex;
  gap: 0.6rem;
  align-items: center;
}
.bh-dong-anh {
  width: 46px;
  height: 46px;
  margin: 0 auto;
  border-radius: 8px;
  background: var(--fs-surface, #f7f9fc);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  color: #c3ccd7;
  flex-shrink: 0;
}
.bh-dong-anh img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}
.bh-sl {
  display: inline-flex;
  align-items: center;
  border: 1px solid var(--fs-line, #e6eaf0);
  border-radius: 9px;
  overflow: hidden;
}
.bh-sl button {
  border: 0;
  background: var(--fs-surface, #f7f9fc);
  width: 30px;
  height: 32px;
}
.bh-sl button:disabled {
  opacity: 0.4;
}
.bh-sl input {
  width: 52px;
  border: 0;
  text-align: center;
  height: 32px;
  -moz-appearance: textfield;
}
.bh-sl input::-webkit-outer-spin-button,
.bh-sl input::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

.bh-phieu {
  display: flex;
  gap: 0.6rem;
  align-items: flex-start;
  border: 1px solid var(--fs-line, #e6eaf0);
  border-radius: 10px;
  padding: 0.5rem 0.65rem;
  cursor: pointer;
}
.bh-phieu.active {
  border-color: var(--fs-primary, #0977ec);
  background: var(--fs-primary-soft, #e7f1fd);
}
.bh-phieu.is-khong {
  opacity: 0.7;
  cursor: not-allowed;
}
.bh-tong {
  margin: 0 0 0.75rem;
  display: grid;
  gap: 0.35rem;
}
.bh-tong > div {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
}
.bh-tong dt {
  font-weight: 500;
}
.bh-tong dd {
  margin: 0;
  font-weight: 600;
  white-space: nowrap;
}
.bh-phai-tra {
  border-top: 1px dashed var(--fs-line, #e6eaf0);
  padding-top: 0.45rem;
  font-size: 1.15rem;
  color: var(--fs-primary-dark, #075fc0);
}
.bh-thua {
  color: #15803d;
  font-weight: 700;
  font-size: 1.05rem;
}
/* Các dòng tiền của đơn: kẻ mảnh giữa các dòng, nhãn nhạt, số đậm */
.bh-tong-don {
  margin: 0.75rem 0;
  gap: 0;
}
.bh-tong-don > div {
  align-items: center;
  min-height: 2.6rem;
  border-bottom: 1px solid var(--fs-line, #eef1f5);
}
.bh-tong-don dt {
  font-weight: 400;
  color: var(--fs-muted, #64748b);
  font-size: 0.88rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.bh-tong-don .bh-dong-phu {
  min-height: 2rem;
  font-size: 0.82rem;
}
.bh-tong-don .bh-dong-phu dd {
  font-weight: 500;
  color: var(--fs-muted, #64748b);
}
.bh-ghn :deep(.vc-ten) {
  display: none;
}
.bh-ghn :deep(.vc-logo) {
  height: 18px;
}
.bh-tien-giam {
  color: #16a34a;
}
.bh-tong-don .bh-phai-tra {
  border-top: 0;
  border-bottom: 0;
  padding-top: 0.25rem;
  font-size: 1.05rem;
  color: inherit;
}
.bh-tong-don .bh-phai-tra dt {
  font-weight: 700;
  color: var(--fs-text, #0f172a);
  font-size: 1rem;
}
.bh-tong-don .bh-phai-tra dd {
  font-weight: 800;
}
.bh-nhan-muc {
  display: block;
  margin-bottom: 0.35rem;
  font-size: 0.86rem;
  font-weight: 600;
}
.bh-pt {
  margin: 0;
  padding: 0;
  border: 0;
}
.bh-pt .form-check-label {
  font-size: 0.88rem;
}
.bh-qr {
  display: flex;
  gap: 0.75rem;
  align-items: flex-start;
  margin-top: 0.75rem;
  padding: 0.65rem;
  border-radius: 12px;
  background: var(--fs-surface, #f7f9fc);
}
.bh-ck-cho,
.bh-ck-da-nhan {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  margin: 0.4rem 0 0.25rem;
  font-size: 0.85rem;
  font-weight: 600;
}
.bh-ck-cho {
  color: #b45309;
}
.bh-ck-da-nhan {
  color: #15803d;
}
.bh-nut-tt {
  width: 100%;
  justify-content: center;
  padding: 0.8rem;
  font-size: 1.05rem;
  margin-top: 1rem;
}
/* Modal xác nhận thanh toán */
.bh-xn-modal {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}
.bh-xn-tong {
  text-align: center;
  padding: 1rem;
  background: #f0f7ff;
  border: 1px solid #cbe2fe;
  border-radius: 12px;
}
.bh-xn-tong-nhan {
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: var(--fs-primary-dark, #075fc0);
  margin-bottom: 0.25rem;
}
.bh-xn-tong-so {
  font-size: 1.85rem;
  font-weight: 800;
  color: var(--fs-primary, #0977ec);
  line-height: 1.1;
}
.bh-xn-thong-tin {
  background: var(--fs-surface, #f8fafc);
  border: 1px solid var(--fs-line, #e6eaf0);
  border-radius: 12px;
  padding: 0.4rem 0.9rem;
}
.bh-xn-dong {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.5rem 0;
  border-bottom: 1px solid var(--fs-line-soft, #edf1f6);
  font-size: 0.92rem;
}
.bh-xn-dong:last-child {
  border-bottom: 0;
}
.bh-xn-nhan {
  color: #64748b;
}
.bh-xn-gia-tri {
  color: #1e293b;
  text-align: right;
}
.bh-xn-dong-thua {
  background: #f0fdf4;
  margin: 0.2rem -0.9rem -0.4rem;
  padding: 0.6rem 0.9rem;
  border-radius: 0 0 11px 11px;
  border-top: 1px solid #bbf7d0;
}
@media (max-width: 1200px) {
  .bh-luoi {
    grid-template-columns: 1fr;
  }
  .bh-phai {
    position: static;
  }
}
</style>
