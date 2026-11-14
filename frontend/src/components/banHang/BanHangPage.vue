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
import KhachHangAddressModal from '../KhachHangAddressModal.vue'
import { useGiaoCa } from '../../composables/useGiaoCa'
import { useAuth } from '../../composables/useAuth'
import { useToast } from '../../composables/useToast'
import { NGAN_HANG, PHUONG_THUC, daCauHinhNganHang } from '../../constants/thanhToan'
import { formatMoney } from '../../utils/format'
import { dongTienDon } from '../../utils/tienDon'
import { formatPhanTram } from '../../utils/giamGia'
import { loiSdt } from '../../utils/kiemTra'
import { taoHtmlHoaDon } from '../../utils/hoaDonIn'
import { duocMienPhiVanChuyen } from '../../utils/phiVanChuyenTinh'
import { dungCaiDatVanChuyen } from '../../services/caiDatVanChuyen'
import { SO_LUONG_TOI_DA, chuoiVietQR, dangGiamTheoDot, goiYPhieuMoi, phiGiaoHang, tienGiamDot, tinhThanhToan, tongSoLuong, tongTienHang } from '../../utils/banHang'
import { useAutoRefresh } from '../../composables/useAutoRefresh'

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
    maPhieu: '', // luôn là phiếu giảm nhiều nhất do hệ thống tự áp (taiPhieu), nhân viên không chọn
    ghiChu: '',
    phuongThuc: 'TIEN_MAT',
    tienMat: '',
    tienMatTay: false, // true: nhân viên đã tự sửa số tiền khách đưa, không tự điền lại
    chuyenKhoan: '',
    maGiaoDich: '',
    giaoHang: false,
    phiShip: '', // số nhân viên gõ vào ô phí vận chuyển
    phiShipTay: false, // true: nhân viên đã sửa ô phí; false: phí tự theo tỉnh
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

// Tăng mỗi lần chính màn này đổi hóa đơn chờ: taiChoNgam dùng để bỏ kết quả đã cũ (tab vừa ghi không nhận tin của chính nó)
let lanDoiCho = 0

function thayHoaDon(moi) {
  lanDoiCho++
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

// Hóa đơn chờ đổi ở cửa sổ / máy khác, hoặc giá / đợt giảm giá vừa đổi (tuCapNhatGia tự đưa về giá mới): cập nhật, không cần F5.
// Hỏi lại định kỳ để bắt đợt giảm giá tự bắt đầu / kết thúc theo giờ (không có tin báo).
// Phiếu giảm giá KHÔNG tải lại ở đây: mức giảm chỉ đổi khi nhân viên đã được báo lúc thanh toán (PHIEU_THAY_DOI)
async function taiChoNgam() {
  let lan, ds
  do {
    lan = lanDoiCho
    ds = (await api.get('/ban-hang/hoa-don-cho')).data || []
  } while (lan !== lanDoiCho) // vừa quét thêm hàng trong lúc đang tải: kết quả vừa nhận đã cũ, lấy lại
  dsCho.value = ds
  for (const h of ds) if (!phu[h.id]) phu[h.id] = phuMoi()
  if (!ds.some((h) => h.id === dangChon.value)) dangChon.value = ds[0]?.id ?? null
}
useAutoRefresh(taiChoNgam, 3000, {
  chuKyMs: 30_000,
  chuDe: ['hoa-don', 'san-pham', 'giam-gia'],
  coTheTai: () => daTaiCho.value && !dangGui.value,
  // Con trỏ thường nằm sẵn ở ô quét mã. Ô quét mã, tiền khách đưa, người nhận lưu riêng nên tải lại không đè;
  // chỉ ô số lượng (type=number) hiện giá trị từ máy chủ nên đang gõ dở thì mới chờ
  oChanTai: (o) => o.type === 'number',
})

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
  lanDoiCho++
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
const dongSapBo = ref(null) // dòng đang chờ nhân viên xác nhận bỏ khỏi hóa đơn
async function xoaDong(d) {
  dongSapBo.value = null
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
    h.p.maPhieu = ''
    return
  }
  dangTaiPhieu.value = true
  try {
    phieus.value = (await api.get('/ban-hang/phieu-giam-gia', { params: { idKhachHang: h.khach?.id, tongTien: tienHang.value } })).data
    // Luôn tự áp phiếu giảm nhiều nhất (kể cả phiếu cá nhân của khách); máy chủ đã xếp phiếu dùng được, giảm nhiều nhất lên đầu
    h.p.maPhieu = phieus.value.find((p) => p.apDungDuoc)?.ma ?? ''
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
// Ghi chú dưới dòng phiếu: mua thêm bao nhiêu (tiền và số sản phẩm trong giỏ) thì hệ thống đổi sang phiếu mới giảm nhiều hơn
const goiYPhieu = computed(() => goiYPhieuMoi(phieus.value, tienHang.value, hd.value.gio, phieuChon.value))

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
  // Khi người dùng bấm về Khách lẻ (k === null): xóa sạch thông tin người nhận và địa chỉ giao hàng
  if (!k) {
    if (phu[id]) {
      Object.assign(phu[id].nguoiNhan, { hoTen: '', sdt: '', tinhThanh: '', phuongXa: '', diaChiCuThe: '' })
      phu[id].phiShip = ''
      phu[id].phiShipTay = false
    }
    daChamGiao.value = false
    daChamSdtNhan.value = false
  } else if (phu[id]?.giaoHang) {
    // Khi chọn khách mới và đang bật giao hàng: tự động nạp thông tin khách mới và địa chỉ mặc định
    await dienNguoiNhan(true)
  }
}

/* ===================== Giao hàng ===================== */
// Phí giao theo tỉnh nhận: chưa chọn tỉnh thì null (chưa tính), đổi tỉnh là tính lại
const phiShipTheoTinh = computed(() => (hd.value.p.giaoHang ? phiGiaoHang(hd.value.p.nguoiNhan.tinhThanh) : 0))
// Ô phí vận chuyển: tự điền theo tỉnh, nhân viên sửa thì lấy số đã nhập; xóa trống ô thì về phí theo tỉnh
const coPhiNhapTay = computed(() => hd.value.p.giaoHang && hd.value.p.phiShipTay && hd.value.p.phiShip !== '')
// Đơn đạt ngưỡng miễn phí vận chuyển (Cài đặt → Cài đặt free ship): không thu phí giao, ô phí thay bằng chữ "Miễn phí" (máy chủ cũng tính 0)
const caiDatShip = dungCaiDatVanChuyen()
const mienPhiShip = computed(() => hd.value.p.giaoHang && duocMienPhiVanChuyen(caiDatShip, tienHang.value))
const phiShipDon = computed(() => (mienPhiShip.value ? 0 : coPhiNhapTay.value ? Number(hd.value.p.phiShip) : phiShipTheoTinh.value))
const phiShip = computed(() => phiShipDon.value ?? 0)
const goiYOPhiShip = computed(() => (phiShipTheoTinh.value === null ? 'Chưa tính phí' : phiShipTheoTinh.value ? formatMoney(phiShipTheoTinh.value) : 'Miễn phí'))
function nhapPhiShip(v) {
  hd.value.p.phiShip = v
  hd.value.p.phiShipTay = true
}
watch(
  () => [hd.value.id, hd.value.p.nguoiNhan.tinhThanh],
  ([id, tinh], [idCu, tinhCu]) => {
    if (id !== idCu || tinh === tinhCu) return
    hd.value.p.phiShip = ''
    hd.value.p.phiShipTay = false
  },
)
const diaChiGiao = computed(() => {
  const n = hd.value.p.nguoiNhan
  return [n.diaChiCuThe.trim(), n.phuongXa, n.tinhThanh].filter(Boolean).join(', ')
})
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
async function dienNguoiNhan(epBuoc = false) {
  const h = hd.value
  const n = h.p.nguoiNhan
  if (!h.khach) {
    if (epBuoc) {
      Object.assign(n, { hoTen: '', sdt: '', tinhThanh: '', phuongXa: '', diaChiCuThe: '' })
      h.p.phiShip = ''
      h.p.phiShipTay = false
      daChamGiao.value = false
      daChamSdtNhan.value = false
    }
    return
  }
  if (epBuoc) {
    n.hoTen = h.khach.hoTen || ''
    n.sdt = h.khach.sdt || ''
    n.tinhThanh = ''
    n.phuongXa = ''
    n.diaChiCuThe = ''
    h.p.phiShip = ''
    h.p.phiShipTay = false
    daChamGiao.value = false
    daChamSdtNhan.value = false
  } else {
    n.hoTen = n.hoTen || h.khach.hoTen || ''
    n.sdt = n.sdt || h.khach.sdt || ''
    if (n.tinhThanh || n.diaChiCuThe) return
  }
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

function xoaTrangDiaChiGiao() {
  const n = hd.value.p.nguoiNhan
  Object.assign(n, { hoTen: '', sdt: '', tinhThanh: '', phuongXa: '', diaChiCuThe: '' })
  hd.value.p.phiShip = ''
  hd.value.p.phiShipTay = false
  daChamGiao.value = false
  daChamSdtNhan.value = false
  toast.success('Đã xóa trắng thông tin giao hàng.')
}

/* Sổ địa chỉ khách hàng trong bán hàng tại quầy: chọn địa chỉ cũ hoặc thêm địa chỉ mới */
const modalDiaChiKhach = ref({ open: false, moThemMoi: false })

function moChonDiaChiCu() {
  if (!hd.value.khach) {
    toast.error('Vui lòng chọn khách hàng thành viên trước để sử dụng sổ địa chỉ.')
    return
  }
  modalDiaChiKhach.value = { open: true, moThemMoi: false }
}

function moThemDiaChiMoi() {
  if (!hd.value.khach) {
    toast.error('Vui lòng chọn khách hàng thành viên trước để thêm địa chỉ mới vào sổ địa chỉ.')
    return
  }
  modalDiaChiKhach.value = { open: true, moThemMoi: true }
}

function apDungDiaChiGiao(dc) {
  if (!dc) return
  const n = hd.value.p.nguoiNhan
  n.hoTen = dc.tenNguoiNhan || hd.value.khach?.hoTen || n.hoTen
  n.sdt = dc.sdtNguoiNhan || hd.value.khach?.sdt || n.sdt
  n.tinhThanh = dc.tinhThanhPho || ''
  n.phuongXa = dc.phuongXa || ''
  n.diaChiCuThe = dc.diaChiCuThe || ''
  // Reset phí tự nhập tay (nếu có) để phí giao tự tính lại theo tỉnh mới
  hd.value.p.phiShip = ''
  hd.value.p.phiShipTay = false
  daChamGiao.value = false
  daChamSdtNhan.value = false
  toast.success('Đã áp dụng địa chỉ giao hàng.')
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
// Các dòng tiền cùng tên, cùng thứ tự với giỏ hàng / thanh toán online / chi tiết hóa đơn (utils/tienDon.js, review 05/11).
// "Tổng tiền hàng" theo giá gốc rồi trừ giảm theo đợt, trừ phiếu, cộng phí giao = tổng thanh toán (số lưu trong hóa đơn không đổi).
const tienDon = computed(() =>
  dongTienDon({
    tienHangSauDot: tienHang.value,
    giamTheoDot: giamDot.value,
    giamPhieu: giamPhieu.value,
    maPhieu: phieuChon.value && giamPhieu.value ? phieuChon.value.ma : '',
    coGiaoHang: hd.value.p.giaoHang,
    phiVanChuyen: phiShipDon.value,
    tongThanhToan: tt.value.phaiTra,
  }),
)
// Số tiền từng dòng trong hộp xác nhận: cùng cách ghi với khối "Thông tin đơn hàng"
function chuDongTien(d) {
  if (d.khoa === 'phi-van-chuyen') return d.so === null ? 'Chưa tính phí' : d.so ? formatMoney(d.so) : 'Miễn phí'
  if (d.khoa === 'phieu') return d.so ? `-${formatMoney(d.so)}` : 'Không dùng phiếu'
  return `${d.dau === '-' ? '-' : ''}${formatMoney(d.so)}`
}
const coQr = daCauHinhNganHang()
const qrUrl = ref('')
const noiDungCk = computed(() => `FOOTSTYLE ${hd.value.maHoaDon || ''}`.trim())
watch(
  () => [tt.value.chuyenKhoan, noiDungCk.value, hd.value.p.phuongThuc],
  async ([ck]) => {
    qrUrl.value = ''
    if (!coQr || !ck || hd.value.p.phuongThuc === 'TIEN_MAT') return
    try {
      qrUrl.value = await QRCode.toDataURL(chuoiVietQR({ bin: NGAN_HANG.bin, soTaiKhoan: NGAN_HANG.soTaiKhoan, soTien: ck, noiDung: noiDungCk.value }), { width: 360, margin: 1 })
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
    qrGiaLap.value = await QRCode.toDataURL(link, { width: 360, margin: 1 })
    taiTkGiaLap()
    if (d.daNhan) daNhanCk(d)
    else hoiCk = setInterval(kiemTraCk, 3000)
  } catch {
    ckGiaLap.value = null
  }
}
// Tài khoản nhận tiền hiện cạnh mã QR giả lập (máy chủ cấu hình, mặc định là tài khoản mẫu), tải một lần
const tkGiaLap = ref(null)
async function taiTkGiaLap() {
  if (tkGiaLap.value) return
  try {
    const d = (await api.get('/cua-hang/thong-tin-thanh-toan')).data
    if (d?.soTaiKhoan) tkGiaLap.value = d
  } catch {
    // không tải được thì vẫn hiện số tiền và nội dung chuyển khoản
  }
}
// Review: ô chuyển khoản ghi đủ thông tin (ngân hàng, số tài khoản, chủ tài khoản, số tiền, nội dung) cạnh mã QR to hơn
const thongTinCk = computed(() => {
  if (ckGiaLap.value) {
    const tk = tkGiaLap.value
    return {
      nganHang: tk?.nganHang || '', soTaiKhoan: tk?.soTaiKhoan || '', chuTaiKhoan: tk?.tenChuTaiKhoan || '',
      soTien: ckGiaLap.value.soTien, noiDung: ckGiaLap.value.maHoaDon,
    }
  }
  return {
    nganHang: NGAN_HANG.tenNganHang, soTaiKhoan: NGAN_HANG.soTaiKhoan, chuTaiKhoan: NGAN_HANG.chuTaiKhoan,
    soTien: tt.value.chuyenKhoan, noiDung: noiDungCk.value,
  }
})
const moQrTo = ref(false)
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
  // Nhận đủ tiền thì tự thanh toán và in luôn (kết hợp mà chưa nhập đủ tiền mặt thì chờ nhân viên bấm)
  if (!lyDoKhongThanhToan.value && !dangGui.value) thanhToan({ tuDong: true })
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

const LOI_THIEU_NGUOI_NHAN = 'Vui lòng nhập đủ thông tin người nhận để giao hàng.'
const lyDoKhongThanhToan = computed(() => {
  if (canVaoCa.value) return 'Bạn chưa vào ca.'
  if (!hd.value.gio.length) return 'Hóa đơn chưa có sản phẩm.'
  if (hd.value.coGiaDoi) return 'Có sản phẩm đã đổi giá. Báo khách rồi bấm "Cập nhật giá".'
  if (loiPhieu.value) return loiPhieu.value
  if (Object.keys(loiGiao.value).length) return LOI_THIEU_NGUOI_NHAN
  if (tt.value.loi === 'Vui lòng nhập tiền khách đưa.') return ''
  return tt.value.loi
})
// Dòng nhắc dưới nút Thanh toán: thiếu thông tin người nhận thì không nhắc sẵn (vẫn chặn thanh toán; bấm nút mới báo lỗi từng ô)
const goiYThanhToan = computed(() => (lyDoKhongThanhToan.value === LOI_THIEU_NGUOI_NHAN ? '' : lyDoKhongThanhToan.value))
const xacNhan = ref(false)
const dangGui = ref(false)

/* ----- Giá đổi: hóa đơn tự theo giá mới, không cần bấm "Cập nhật giá" -----
 * Máy chủ vẫn chặn thanh toán theo giá cũ (GIA_THAY_DOI); tự cập nhật xong thì báo rõ giá cũ -> mới để nhân viên báo lại khách.
 * Đang gửi thanh toán / đang mở hộp xác nhận thì chờ xong mới cập nhật (không đổi số tiền trước mắt nhân viên).
 * Lỗi thì không thử lại liên tục: nút "Cập nhật giá" vẫn còn để bấm tay. */
let dangTuCapNhatGia = false
const daLoiCapNhatGia = new Set()
async function tuCapNhatGia() {
  const h = hdServer.value
  if (!h.id || !h.coGiaDoi || dangTuCapNhatGia || dangGui.value || xacNhan.value) return
  const doi = h.items.filter((d) => d.giaDoi)
  const khoa = `${h.id}:${doi.map((d) => `${d.id}@${d.giaHienTai}`).join(',')}`
  if (daLoiCapNhatGia.has(khoa)) return
  dangTuCapNhatGia = true
  try {
    thayHoaDon((await api.post(`/ban-hang/hoa-don-cho/${h.id}/cap-nhat-gia`)).data)
    toast.success(
      doi.length === 1
        ? `Giá ${doi[0].ma} đổi từ ${formatMoney(doi[0].donGia)} thành ${formatMoney(doi[0].giaHienTai)}. Hóa đơn đã theo giá mới, báo lại khách số tiền.`
        : `Giá của ${doi.length} sản phẩm đã đổi. Hóa đơn đã theo giá mới, báo lại khách số tiền.`,
      'Giá đã thay đổi',
    )
  } catch (e) {
    daLoiCapNhatGia.add(khoa)
    baoLoiCho(e, 'Không tự cập nhật được giá. Vui lòng bấm "Cập nhật giá".')
  } finally {
    dangTuCapNhatGia = false
  }
}
watch(() => [hdServer.value.id, hdServer.value.coGiaDoi, dangGui.value, xacNhan.value], tuCapNhatGia, { immediate: true })

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
// Khách quét mã chuyển khoản xong thì thanh toán tự chạy, không có cú bấm nào nên trình duyệt chặn cửa sổ mới: in qua khung ẩn trong trang
function moKhungIn() {
  const khung = document.createElement('iframe')
  khung.setAttribute('aria-hidden', 'true')
  khung.style.cssText = 'position:fixed;right:0;bottom:0;width:0;height:0;border:0'
  document.body.appendChild(khung)
  return khung.contentWindow
}
function dongCuaSoIn(w) {
  if (w.frameElement) w.frameElement.remove()
  else w.close()
}

async function inVaoCuaSo(id, w) {
  const ct = (await api.get(`/hoa-don/${id}`)).data
  const qr = await QRCode.toDataURL(String(ct.hoaDon.maHoaDon), { width: 240, margin: 1 })
  w.document.write(taoHtmlHoaDon({ hd: ct.hoaDon, khach: ct.khachHang, giao: ct.giaoHang, sanPham: ct.sanPham, daThanhToan: ct.daThanhToan, qrUrl: qr, goc: location.origin }))
  w.document.close()
  // Khung ẩn: in xong thì gỡ khỏi trang
  if (w.frameElement) w.addEventListener('afterprint', () => setTimeout(() => dongCuaSoIn(w)))
}

async function thanhToan({ tuDong = false } = {}) {
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
  const cuaSoIn = tuDong ? moKhungIn() : moCuaSoIn()
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
            phiVanChuyen: coPhiNhapTay.value && !mienPhiShip.value ? Number(p.phiShip) : null, // null: máy chủ tính (theo tỉnh, đạt ngưỡng thì miễn phí)
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
        dongCuaSoIn(cuaSoIn)
        toast.error('Không in được hóa đơn. Vào trang chi tiết để in lại.')
      })
    } else {
      toast.error('Trình duyệt đã chặn cửa sổ in. Vào trang chi tiết để in lại.')
    }
    router.push(`/hoa-don/${res.data.id}`)
  } catch (e) {
    if (cuaSoIn) dongCuaSoIn(cuaSoIn)
    xacNhan.value = false
    if (e.code === 'GIA_THAY_DOI') {
      toast.error('Giá vừa thay đổi nên chưa thanh toán. Hóa đơn sẽ tự theo giá mới, báo khách rồi bấm Thanh toán lại.')
      await taiHoaDonCho()
    } else if (e.code === 'CHUA_NHAN_CA' || e.code === 'CA_CU_CHUA_GIAO') {
      toast.error(e.message)
      await giaoCa.taiLai()
      moCa.value = true
    } else if (e.code === 'PHIEU_KHONG_HOP_LE') {
      toast.error(e.message)
      taiPhieu()
    } else if (e.code === 'PHIEU_THAY_DOI') {
      // Tải lại để màn hình tự áp lại phiếu tốt nhất với mức giảm mới; nhân viên báo khách số tiền mới rồi bấm thanh toán lại
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
      <span>
        Bạn chưa vào ca nên mới chỉ xem được. Vui lòng <b>vào ca</b> (nhập tiền đầu ca) trước khi bán hàng.
      </span>
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
            <!-- Review: bỏ bớt chữ hướng dẫn nhỏ (phím tắt F2 / F9 vẫn dùng được, ghi trong chú thích của nút) -->
          </div>
          <div class="bh-tabs" role="tablist" aria-label="Hóa đơn chờ">
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
              <button type="button" class="ad-btn ad-btn-primary" data-testid="bh-chon-sp" title="Chọn sản phẩm (F2)" @click="moChonSp = true">
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
                      <span class="bh-dong-anh">
                        <img v-if="d.anh" :src="d.anh" :alt="d.ten" /><i v-else class="bi bi-image" aria-hidden="true"></i>
                        <span v-if="dangGiamTheoDot(d)" class="bh-giam-anh" data-testid="bh-dong-giam">-{{ formatPhanTram(d.phanTramGiam) }}</span>
                      </span>
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
                      <button type="button" class="ad-icon-btn is-danger" :aria-label="`Bỏ ${d.ten} khỏi hóa đơn`" title="Bỏ khỏi hóa đơn" @click="dongSapBo = d"><i class="bi bi-trash" aria-hidden="true"></i></button>
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
            <div class="bh-giao-actions">
              <button
                type="button"
                class="ad-btn ad-btn-sm bh-btn-dc"
                title="Quản lý sổ địa chỉ của khách hàng"
                data-testid="bh-btn-dc-cu"
                @click="moChonDiaChiCu"
              >
                <i class="bi bi-geo-alt-fill text-primary" aria-hidden="true"></i>
                <span>Địa chỉ khách hàng</span>
              </button>
              <button
                v-if="hd.p.nguoiNhan.hoTen || hd.p.nguoiNhan.sdt || hd.p.nguoiNhan.tinhThanh || hd.p.nguoiNhan.diaChiCuThe"
                type="button"
                class="ad-btn ad-btn-sm bh-btn-dc"
                title="Xóa trắng thông tin giao hàng"
                data-testid="bh-btn-xoa-trang-dc"
                @click="xoaTrangDiaChiGiao"
              >
                <i class="bi bi-eraser text-danger" aria-hidden="true"></i>
                <span>Xóa trắng</span>
              </button>
              <VanChuyenLogo :ten="DON_VI_VAN_CHUYEN" />
            </div>
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

          <!-- Phiếu giảm giá: hệ thống luôn tự áp phiếu giảm nhiều nhất, nhân viên chỉ xem, không chọn / bỏ phiếu.
               Dòng ghi chú bên dưới báo mua thêm bao nhiêu thì được phiếu mới giảm nhiều hơn. -->
          <div id="bh-phieu" class="bh-phieu-ap" :class="{ 'is-trong': !(phieuChon && giamPhieu) }" data-testid="bh-phieu-ap-dung">
            <i class="bi bi-ticket-perforated" aria-hidden="true"></i>
            <span class="bh-phieu-ap-chu" :title="phieuChon && giamPhieu ? phieuChon.ten : ''">
              <template v-if="phieuChon && giamPhieu">
                Đang áp dụng phiếu <b>{{ phieuChon.ma }}</b> (giảm <b>{{ formatMoney(giamPhieu) }}</b>)
                <span class="bh-phieu-tot">Tốt nhất</span>
              </template>
              <template v-else-if="!hd.gio.length">Phiếu giảm giá: chưa có sản phẩm</template>
              <template v-else-if="dangTaiPhieu && !phieus.length">Đang tìm phiếu giảm giá…</template>
              <template v-else>Không có phiếu dùng được cho đơn này</template>
            </span>
          </div>
          <p v-if="goiYPhieu" class="bh-goi-y-ngan" data-testid="bh-goi-y-ngan">
            <i class="bi bi-lightbulb" aria-hidden="true"></i>
            Mua thêm <b>{{ formatMoney(goiYPhieu.canThem) }}</b>
            <template v-if="goiYPhieu.sanPham"> (thêm <b>{{ goiYPhieu.sanPham.soLuong }} sản phẩm</b> {{ goiYPhieu.sanPham.ten }} là đủ)</template>
            để dùng phiếu {{ goiYPhieu.ma }} (giảm {{ formatMoney(goiYPhieu.giam) }})
          </p>
          <p v-if="loiPhieu" class="ad-error mb-0">{{ loiPhieu }}</p>

          <!-- Cùng các dòng với giỏ hàng / thanh toán online / chi tiết hóa đơn: Tổng tiền hàng (giá gốc) - đợt - phiếu + phí giao = tổng -->
          <dl class="bh-tong bh-tong-don">
            <div v-for="d in tienDon.dong" :key="d.khoa" :data-dong="d.khoa" :class="{
              'bh-dong-ship': d.khoa === 'phi-van-chuyen',
              'bh-phai-tra': d.khoa === 'tong-thanh-toan',
            }" :data-testid="d.khoa === 'phi-van-chuyen' ? 'bh-phi-ship' : undefined">
              <dt>
                <VanChuyenLogo v-if="d.khoa === 'phi-van-chuyen'" :ten="DON_VI_VAN_CHUYEN" class="bh-ghn" />
                {{ d.nhan }}<template v-if="d.khoa === 'tong-tien-hang'"> ({{ tongSoLuong(hd.gio) }} sản phẩm)</template><template v-if="d.ma"> ({{ d.ma }})</template>:
              </dt>
              <dd v-if="d.khoa === 'phi-van-chuyen' && mienPhiShip" class="bh-mien-phi-ship" data-testid="bh-mien-phi-ship" :title="`Đơn từ ${formatMoney(caiDatShip.mienPhiTu)} được miễn phí vận chuyển`">Miễn phí</dd>
              <dd v-else-if="d.khoa === 'phi-van-chuyen'" class="bh-o-phi-ship">
                <MoneyInput id="bh-phi-ship" small :model-value="hd.p.phiShipTay ? hd.p.phiShip : d.so || ''" aria-label="Phí vận chuyển" :placeholder="goiYOPhiShip" @update:model-value="nhapPhiShip" />
              </dd>
              <dd v-else-if="d.khoa === 'phieu'" class="bh-tien-giam">{{ d.so ? `-${formatMoney(d.so)}` : 'Không dùng phiếu' }}</dd>
              <dd v-else :class="{ 'bh-tien-giam-dot': d.dau === '-' }">{{ d.dau === '-' ? '-' : '' }}{{ formatMoney(d.so) }}</dd>
            </div>
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
          </div>
          <!-- QR chuyển khoản (giả lập) -->
          <!-- QR chuyển khoản: mã QR to ở trên, dưới là đủ thông tin chuyển khoản; bấm mã QR để phóng to cho khách quét -->
          <div v-if="hd.p.phuongThuc !== 'TIEN_MAT' && tt.chuyenKhoan > 0 && (ckGiaLap || coQr)" class="bh-qr" :data-testid="ckGiaLap ? 'bh-qr-gia-lap' : 'bh-qr-vietqr'">
            <button v-if="ckGiaLap ? qrGiaLap : qrUrl" type="button" class="bh-qr-anh" title="Bấm để phóng to mã QR" @click="moQrTo = true">
              <img :src="ckGiaLap ? qrGiaLap : qrUrl" :alt="`Mã QR chuyển khoản ${formatMoney(thongTinCk.soTien)}`" width="210" height="210" />
              <span class="bh-qr-phong"><i class="bi bi-arrows-fullscreen" aria-hidden="true"></i> Phóng to</span>
            </button>
            <p class="bh-qr-tieu-de">Quét mã để chuyển khoản</p>
            <dl class="bh-qr-tt" data-testid="bh-qr-thong-tin">
              <div v-if="thongTinCk.nganHang"><dt>Ngân hàng</dt><dd>{{ thongTinCk.nganHang }}</dd></div>
              <div v-if="thongTinCk.soTaiKhoan"><dt>Số tài khoản</dt><dd class="bh-qr-stk">{{ thongTinCk.soTaiKhoan }}</dd></div>
              <div v-if="thongTinCk.chuTaiKhoan"><dt>Chủ tài khoản</dt><dd>{{ thongTinCk.chuTaiKhoan }}</dd></div>
              <div><dt>Số tiền</dt><dd class="bh-qr-tien">{{ formatMoney(thongTinCk.soTien) }}</dd></div>
              <div><dt>Nội dung</dt><dd class="fw-bold">{{ thongTinCk.noiDung }}</dd></div>
            </dl>
            <!-- Đủ thông tin sản phẩm của hóa đơn đang thu tiền: tên, mã, phân loại, số lượng, đơn giá, thành tiền -->
            <div v-if="hd.gio.length" class="bh-qr-sp" data-testid="bh-qr-san-pham">
              <p class="bh-qr-sp-tieu-de">Sản phẩm ({{ tongSoLuong(hd.gio) }})</p>
              <ul>
                <li v-for="d in hd.gio" :key="d.id">
                  <span class="bh-qr-sp-ten">
                    <b>{{ d.ten }}</b>
                    <small class="text-muted d-block">{{ d.ma }}<template v-if="d.phanLoai"> · {{ d.phanLoai }}</template></small>
                  </span>
                  <span class="bh-qr-sp-tien">
                    {{ d.soLuong }} × {{ formatMoney(d.donGia) }}
                    <b class="d-block">{{ formatMoney(d.donGia * d.soLuong) }}</b>
                  </span>
                </li>
              </ul>
            </div>
            <template v-if="ckGiaLap">
              <div v-if="ckGiaLap.daNhan" class="bh-ck-da-nhan"><i class="bi bi-check-circle-fill" aria-hidden="true"></i> Đã nhận tiền<template v-if="ckGiaLap.maGiaoDich"> · {{ ckGiaLap.maGiaoDich }}</template></div>
              <div v-else class="bh-ck-cho"><span class="spinner-border spinner-border-sm" aria-hidden="true"></span> Đang chờ khách chuyển khoản</div>
            </template>
            <template v-else>
              <label class="ad-label mt-2" for="bh-ma-gd">Mã giao dịch</label>
              <input id="bh-ma-gd" v-model="hd.p.maGiaoDich" type="text" class="form-control ad-control" maxlength="100" placeholder="Mã giao dịch ngân hàng (nếu có)" />
            </template>
          </div>
          <div v-else-if="hd.p.phuongThuc !== 'TIEN_MAT' && tt.chuyenKhoan > 0" class="bh-qr">
            <div class="small text-muted">Chưa cấu hình tài khoản nhận tiền nên không hiện mã QR (xem frontend/src/constants/thanhToan.js).</div>
            <label class="ad-label mt-2" for="bh-ma-gd">Mã giao dịch</label>
            <input id="bh-ma-gd" v-model="hd.p.maGiaoDich" type="text" class="form-control ad-control" maxlength="100" placeholder="Mã giao dịch ngân hàng (nếu có)" />
          </div>
          <BaseModal v-if="moQrTo && (ckGiaLap ? qrGiaLap : qrUrl)" title="Quét mã để chuyển khoản" size="md" @close="moQrTo = false">
            <div class="bh-qr-to" data-testid="bh-qr-to">
              <img :src="ckGiaLap ? qrGiaLap : qrUrl" :alt="`Mã QR chuyển khoản ${formatMoney(thongTinCk.soTien)}`" width="340" height="340" />
              <dl class="bh-qr-tt">
                <div v-if="thongTinCk.nganHang"><dt>Ngân hàng</dt><dd>{{ thongTinCk.nganHang }}</dd></div>
                <div v-if="thongTinCk.soTaiKhoan"><dt>Số tài khoản</dt><dd class="bh-qr-stk">{{ thongTinCk.soTaiKhoan }}</dd></div>
                <div v-if="thongTinCk.chuTaiKhoan"><dt>Chủ tài khoản</dt><dd>{{ thongTinCk.chuTaiKhoan }}</dd></div>
                <div><dt>Số tiền</dt><dd class="bh-qr-tien">{{ formatMoney(thongTinCk.soTien) }}</dd></div>
                <div><dt>Nội dung</dt><dd class="fw-bold">{{ thongTinCk.noiDung }}</dd></div>
              </dl>
              <div v-if="hd.gio.length" class="bh-qr-sp">
                <p class="bh-qr-sp-tieu-de">Sản phẩm ({{ tongSoLuong(hd.gio) }})</p>
                <ul>
                  <li v-for="d in hd.gio" :key="d.id">
                    <span class="bh-qr-sp-ten">
                      <b>{{ d.ten }}</b>
                      <small class="text-muted d-block">{{ d.ma }}<template v-if="d.phanLoai"> · {{ d.phanLoai }}</template></small>
                    </span>
                    <span class="bh-qr-sp-tien">
                      {{ d.soLuong }} × {{ formatMoney(d.donGia) }}
                      <b class="d-block">{{ formatMoney(d.donGia * d.soLuong) }}</b>
                    </span>
                  </li>
                </ul>
              </div>
            </div>
          </BaseModal>

          <dl class="bh-tong mt-2">
            <div v-if="tt.conThieu > 0 && hd.p.tienMat" class="text-danger fw-semibold"><dt>Còn thiếu</dt><dd>{{ formatMoney(tt.conThieu) }}</dd></div>
            <div v-if="tt.tienThua > 0" class="bh-thua"><dt>Tiền thừa trả khách</dt><dd>{{ formatMoney(tt.tienThua) }}</dd></div>
          </dl>
          <label class="ad-label" for="bh-ghi-chu">Ghi chú</label>
          <input id="bh-ghi-chu" v-model="hd.p.ghiChu" type="text" class="form-control ad-control" maxlength="500" placeholder="Ghi chú cho hóa đơn (không bắt buộc)" />

          <!-- Hủy hóa đơn chờ bằng dấu X trên tab hóa đơn -->
          <div class="bh-nut-hang">
            <button type="button" class="ad-btn ad-btn-primary bh-nut-tt" :disabled="dangGui" :title="lyDoKhongThanhToan || 'Thanh toán (F9)'" @click="moXacNhan">
              <i class="bi bi-check2-circle" aria-hidden="true"></i> Thanh toán {{ formatMoney(tt.phaiTra) }}
            </button>
          </div>
          <p v-if="goiYThanhToan && hd.gio.length" class="ad-hint text-center mb-0 mt-1">{{ goiYThanhToan }}</p>
        </section>
      </aside>
    </div>

    <!-- Hộp thoại xác nhận thanh toán chuẩn nghiệp vụ POS -->
    <BaseModal v-if="xacNhan" title="Xác nhận thanh toán" size="md" @close="xacNhan = false">
      <div class="bh-xn-modal">
        <!-- Khối hiển thị số tiền thanh toán chính nổi bật -->
        <div class="bh-xn-tong">
          <div class="bh-xn-tong-nhan">TỔNG TIỀN THANH TOÁN</div>
          <div class="bh-xn-tong-so">{{ formatMoney(tt.phaiTra) }}</div>
        </div>

        <!-- Đủ thông tin trước khi chốt: đơn và người nhận, từng sản phẩm, các dòng tiền, cách trả -->
        <div class="bh-xn-thong-tin" data-testid="bh-xn-don">
          <div class="bh-xn-dong">
            <span class="bh-xn-nhan"><i class="bi bi-receipt me-1 text-primary"></i>Hóa đơn:</span>
            <span class="bh-xn-gia-tri fw-semibold">{{ hd.ten }} ({{ tongSoLuong(hd.gio) }} sản phẩm)</span>
          </div>
          <div v-if="auth.trangThai.nguoiDung?.hoTen" class="bh-xn-dong">
            <span class="bh-xn-nhan"><i class="bi bi-person-badge me-1 text-primary"></i>Nhân viên:</span>
            <span class="bh-xn-gia-tri">{{ auth.trangThai.nguoiDung.hoTen }}</span>
          </div>
          <div class="bh-xn-dong">
            <span class="bh-xn-nhan"><i class="bi bi-person me-1 text-primary"></i>Khách hàng:</span>
            <span class="bh-xn-gia-tri">{{ hd.khach?.hoTen ? `${hd.khach.hoTen} (${hd.khach.sdt || '—'})` : 'Khách lẻ (Khách vãng lai)' }}</span>
          </div>
          <div v-if="hd.p.giaoHang" class="bh-xn-dong">
            <span class="bh-xn-nhan"><i class="bi bi-truck me-1 text-primary"></i>Giao tới:</span>
            <span class="bh-xn-gia-tri">{{ hd.p.nguoiNhan.hoTen }} · {{ hd.p.nguoiNhan.sdt }}<br />{{ diaChiGiao }}</span>
          </div>
          <div v-if="hd.p.ghiChu?.trim()" class="bh-xn-dong">
            <span class="bh-xn-nhan"><i class="bi bi-pencil me-1 text-primary"></i>Ghi chú:</span>
            <span class="bh-xn-gia-tri">{{ hd.p.ghiChu.trim() }}</span>
          </div>
        </div>

        <div class="bh-xn-thong-tin bh-xn-sp" data-testid="bh-xn-san-pham">
          <p class="bh-qr-sp-tieu-de">Sản phẩm ({{ tongSoLuong(hd.gio) }})</p>
          <ul>
            <li v-for="d in hd.gio" :key="d.id">
              <span class="bh-qr-sp-ten">
                <b>{{ d.ten }}</b>
                <small class="text-muted d-block">{{ d.ma }}<template v-if="d.phanLoai"> · {{ d.phanLoai }}</template></small>
              </span>
              <span class="bh-qr-sp-tien">
                {{ d.soLuong }} × {{ formatMoney(d.donGia) }}
                <b class="d-block">{{ formatMoney(d.donGia * d.soLuong) }}</b>
              </span>
            </li>
          </ul>
        </div>

        <div class="bh-xn-thong-tin" data-testid="bh-xn-tien">
          <template v-for="d in tienDon.dong" :key="d.khoa">
            <div v-if="d.khoa !== 'tong-thanh-toan'" class="bh-xn-dong">
              <span class="bh-xn-nhan">{{ d.nhan }}<template v-if="d.khoa === 'tong-tien-hang'"> ({{ tongSoLuong(hd.gio) }} sản phẩm)</template><template v-if="d.ma"> ({{ d.ma }})</template>:</span>
              <span class="bh-xn-gia-tri fw-semibold">{{ chuDongTien(d) }}</span>
            </div>
          </template>
        </div>

        <div class="bh-xn-thong-tin" data-testid="bh-xn-thanh-toan">
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
          <div v-if="hd.p.phuongThuc !== 'TIEN_MAT' && hd.p.maGiaoDich?.trim()" class="bh-xn-dong">
            <span class="bh-xn-nhan">Mã giao dịch:</span>
            <span class="bh-xn-gia-tri">{{ hd.p.maGiaoDich.trim() }}</span>
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
      v-if="dongSapBo"
      title="Bỏ sản phẩm khỏi hóa đơn"
      :message="`Bỏ ${dongSapBo.ten}${dongSapBo.phanLoai ? ` (${dongSapBo.phanLoai})` : ''}, số lượng ${dongSapBo.soLuong}, khỏi ${hd.ten}? Sản phẩm sẽ được trả lại kho.`"
      confirm-text="Bỏ"
      variant="danger"
      @confirm="xoaDong(dongSapBo)"
      @cancel="dongSapBo = null"
    />
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
    <KhachHangAddressModal
      v-if="modalDiaChiKhach.open && hd.khach"
      :customer="hd.khach"
      chon
      :mo-them-moi="modalDiaChiKhach.moThemMoi"
      @select="apDungDiaChiGiao"
      @close="modalDiaChiKhach.open = false"
    />
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
.bh-giao-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem;
}
.bh-btn-dc {
  height: 32px;
  font-size: 0.8rem;
  font-weight: 600;
  padding: 0 0.75rem;
  border-radius: 9px;
  border: 1px solid var(--fs-line, #e2e8f0);
  background: #fff;
  color: #334155;
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.05);
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  line-height: 1;
  transition: all 0.15s ease;
}
.bh-btn-dc:hover:not(:disabled) {
  background: var(--fs-hover-bg, #f8fafc);
  border-color: var(--fs-primary-line, #93c5fd);
  color: var(--fs-primary-dark, #075fc0);
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
/* Ghi chú mua thêm để được phiếu mới: nền xanh nhạt, chữ đậm cho dễ đọc */
.bh-goi-y-ngan b {
  color: var(--fs-primary-dark, #075fc0);
}
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
  position: relative;
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
.bh-giam-anh {
  position: absolute;
  top: 0;
  left: 0;
  padding: 1px 4px;
  border-bottom-right-radius: 7px;
  background: #dc2626;
  color: #fff;
  font-size: 0.62rem;
  font-weight: 700;
  line-height: 1.4;
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
.bh-tong-don .bh-tien-giam-dot {
  color: #16a34a;
}
.bh-o-phi-ship .money-input {
  width: 9rem;
}
.bh-dong-phu {
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
.bh-tien-giam,
.bh-mien-phi-ship {
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
  flex-direction: column;
  align-items: stretch;
  gap: 0.35rem;
  margin-top: 0.75rem;
  padding: 0.85rem;
  border: 1px solid var(--fs-line-soft, #e8edf3);
  border-radius: 12px;
  background: var(--fs-surface, #f7f9fc);
}
.bh-qr-anh {
  position: relative;
  align-self: center;
  padding: 6px;
  border: 1px solid var(--fs-line, #dbe3ee);
  border-radius: 12px;
  background: #fff;
  cursor: zoom-in;
}
.bh-qr-anh img {
  display: block;
  width: 210px;
  height: 210px;
}
.bh-qr-phong {
  position: absolute;
  right: 8px;
  bottom: 8px;
  padding: 1px 7px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.7);
  color: #fff;
  font-size: 0.7rem;
  opacity: 0;
  transition: opacity 0.15s ease;
}
.bh-qr-anh:hover .bh-qr-phong,
.bh-qr-anh:focus-visible .bh-qr-phong {
  opacity: 1;
}
.bh-qr-tieu-de {
  margin: 0.25rem 0 0;
  text-align: center;
  font-weight: 700;
  font-size: 0.92rem;
}
.bh-qr-tt {
  margin: 0;
  font-size: 0.86rem;
}
.bh-qr-tt > div {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.3rem 0;
  border-bottom: 1px dashed var(--fs-line, #dbe3ee);
}
.bh-qr-tt > div:last-child {
  border-bottom: 0;
}
.bh-qr-sp {
  margin-top: 0.4rem;
  padding-top: 0.5rem;
  border-top: 1px solid var(--fs-line, #dbe3ee);
  font-size: 0.84rem;
}
.bh-qr-sp-tieu-de {
  margin: 0 0 0.3rem;
  font-weight: 700;
}
.bh-qr-sp ul,
.bh-xn-sp ul {
  margin: 0;
  padding: 0;
  list-style: none;
}
.bh-qr-sp li,
.bh-xn-sp li {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.3rem 0;
  border-bottom: 1px dashed var(--fs-line, #dbe3ee);
}
.bh-qr-sp li:last-child,
.bh-xn-sp li:last-child {
  border-bottom: 0;
}
.bh-xn-sp {
  padding-top: 0.6rem;
  padding-bottom: 0.5rem;
  font-size: 0.86rem;
}
.bh-qr-sp-ten {
  min-width: 0;
  word-break: break-word;
}
.bh-qr-sp-tien {
  flex-shrink: 0;
  text-align: right;
  white-space: nowrap;
}
.bh-qr-tt dt {
  font-weight: 400;
  color: var(--fs-muted, #64748b);
}
.bh-qr-tt dd {
  margin: 0;
  text-align: right;
  word-break: break-word;
}
.bh-qr-stk {
  font-weight: 700;
  letter-spacing: 0.04em;
}
.bh-qr-tien {
  font-weight: 800;
  color: var(--fs-primary, #0977ec);
}
.bh-qr-to {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
}
.bh-qr-to img {
  width: min(340px, 100%);
  height: auto;
  padding: 8px;
  border: 1px solid var(--fs-line, #dbe3ee);
  border-radius: 14px;
  background: #fff;
}
.bh-qr-to .bh-qr-tt {
  width: 100%;
  max-width: 360px;
  font-size: 0.95rem;
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
  gap: 1rem;
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
