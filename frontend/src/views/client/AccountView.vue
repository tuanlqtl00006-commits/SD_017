<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '../../composables/useAuth'
import api from '../../services/api'
import { loiMatKhau } from '../../utils/kiemTra'
import { formatMoney, formatNgayGio } from '../../utils/format'
import { docDonHang, ghiDonHang, dongBoTrangThaiDon } from '../../utils/donHangLocal'
import { khachDiaChiService } from '../../services/khachDiaChiService'
import DiaChiHanhChinhSelect from '../../components/common/DiaChiHanhChinhSelect.vue'
import ConfirmDialog from '../../components/common/ConfirmDialog.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuth()

const activeTab = ref('orders') // 'profile' | 'addresses' | 'orders'

// 1. Hồ sơ cá nhân (lấy từ tài khoản thực tế đang đăng nhập)
const profile = ref({
  hoTen: '',
  sdt: '',
  email: '',
  gioiTinh: 1, // 1: Nam, 0: Nữ
  ngaySinh: '',
})
const profileErrors = ref({})
const profileSaved = ref(false)

// Đổi mật khẩu
const pwForm = ref({ matKhauCu: '', matKhauMoi: '', nhapLai: '' })
const pwErrors = ref({})
const pwSaving = ref(false)
const pwSuccess = ref('')

// 2. Sổ địa chỉ
const addresses = ref([])

// 3. Đơn hàng
const orders = ref([])
const orderFilterStatus = ref('ALL') // 'ALL', 'CHO_XAC_NHAN', 'DANG_GIAO', 'HOAN_THANH', 'DA_HUY'

onMounted(async () => {
  await auth.khoiTao()
  // Nếu chưa đăng nhập: chuyển hướng ngay sang trang đăng nhập khách hàng
  if (!auth.daDangNhap.value) {
    router.replace({ path: '/shop/dang-nhap', query: { quayLai: route.fullPath } })
    return
  }

  const u = auth.trangThai.nguoiDung
  if (u) {
    profile.value.hoTen = u.hoTen || u.ten || 'Khách hàng'
    profile.value.sdt = u.sdt || ''
    profile.value.email = u.email || ''
  }
  await loadProfile()
  await loadAddresses()

  if (route.query.tab) {
    activeTab.value = String(route.query.tab)
  }
  loadOrders()
})

function nhanHoSo(h) {
  profile.value.hoTen = h.hoTen || ''
  profile.value.sdt = h.sdt || ''
  profile.value.email = h.email || ''
  profile.value.ngaySinh = h.ngaySinh || ''
  profile.value.gioiTinh = h.gioiTinh ?? 1
}

// Hồ sơ thật từ CSDL (họ tên, SĐT, email, ngày sinh, giới tính của khách đang đăng nhập)
async function loadProfile() {
  try {
    nhanHoSo((await api.get('/khach/tai-khoan')).data)
  } catch (e) {
    profileErrors.value = { chung: e.message || 'Không tải được thông tin tài khoản.' }
  }
}

// Sổ địa chỉ thật từ CSDL (của chính khách đang đăng nhập)
async function loadAddresses() {
  try {
    addresses.value = (await khachDiaChiService.getAll()) || []
  } catch (e) {
    addresses.value = []
    addressError.value = e.message || 'Không tải được sổ địa chỉ.'
  }
}

const addressError = ref('')
const showAddressForm = ref(false)
const addressSaving = ref(false)
const addressForm = ref({ tenNguoiNhan: '', sdtNguoiNhan: '', tinhThanhPho: '', phuongXa: '', diaChiCuThe: '', laDiaChiMacDinh: false })
const addressErrors = ref({})

function openAddressForm() {
  addressForm.value = {
    tenNguoiNhan: profile.value.hoTen || '',
    sdtNguoiNhan: profile.value.sdt || '',
    tinhThanhPho: '',
    phuongXa: '',
    diaChiCuThe: '',
    laDiaChiMacDinh: addresses.value.length === 0,
  }
  addressErrors.value = {}
  addressError.value = ''
  showAddressForm.value = true
}

async function saveAddress() {
  const f = addressForm.value
  const errs = {}
  if (!f.tenNguoiNhan.trim()) errs.tenNguoiNhan = 'Vui lòng nhập tên người nhận.'
  if (!/^0\d{9}$/.test(f.sdtNguoiNhan.trim())) errs.sdtNguoiNhan = 'Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0.'
  if (!f.tinhThanhPho) errs.tinhThanhPho = 'Vui lòng chọn Tỉnh/Thành phố.'
  if (!f.phuongXa) errs.phuongXa = 'Vui lòng chọn Phường/Xã.'
  if (!f.diaChiCuThe.trim()) errs.diaChiCuThe = 'Vui lòng nhập địa chỉ cụ thể (số nhà, tên đường...).'
  addressErrors.value = errs
  if (Object.keys(errs).length > 0) return

  addressSaving.value = true
  addressError.value = ''
  try {
    await khachDiaChiService.create({ ...f })
    showAddressForm.value = false
    await loadAddresses()
  } catch (e) {
    if (e.loiTheoTruong) addressErrors.value = { ...e.loiTheoTruong }
    addressError.value = e.message || 'Không thêm được địa chỉ.'
  } finally {
    addressSaving.value = false
  }
}

async function loadOrders() {
  orders.value = docDonHang()
  // Cập nhật trạng thái thật từ cửa hàng (nhân viên xác nhận / giao hàng ở trang quản lý)
  orders.value = [
    ...(await dongBoTrangThaiDon(orders.value, async (ma) =>
      (await api.get(`/khach/don-hang/${encodeURIComponent(ma)}`)).data,
    )),
  ]
}

const orderTabs = [
  { key: 'ALL', label: 'Tất cả' },
  { key: 'CHO_XAC_NHAN', label: 'Chờ xác nhận' },
  { key: 'DANG_GIAO', label: 'Đang giao' },
  { key: 'HOAN_THANH', label: 'Đã hoàn thành' },
  { key: 'DA_HUY', label: 'Đã hủy' },
]

function locTheoTab(key) {
  const ds = orders.value
  if (key === 'CHO_XAC_NHAN') return ds.filter((o) => o.trangThai === 0)
  if (key === 'DANG_GIAO') return ds.filter((o) => o.trangThai === 1 || o.trangThai === 2)
  if (key === 'HOAN_THANH') return ds.filter((o) => o.trangThai === 3)
  if (key === 'DA_HUY') return ds.filter((o) => o.trangThai === 6 || o.trangThai === 7)
  return ds
}

const demTheoTab = (key) => locTheoTab(key).length
const filteredOrders = computed(() => locTheoTab(orderFilterStatus.value))

function statusClass(tt) {
  if (tt === 0) return 'cho'
  if (tt === 1 || tt === 2) return 'giao'
  if (tt === 3) return 'xong'
  return 'huy'
}
function statusIcon(tt) {
  if (tt === 0) return 'bi-hourglass-split'
  if (tt === 1 || tt === 2) return 'bi-truck'
  if (tt === 3) return 'bi-check-circle-fill'
  return 'bi-x-circle-fill'
}

// Kiểm tra tuổi >= 15 theo quy tắc dự án
function validateProfile() {
  const errs = {}
  if (!profile.value.hoTen.trim()) {
    errs.hoTen = 'Họ tên không được để trống.'
  }
  if (!profile.value.sdt.trim() || !/^0\d{9}$/.test(profile.value.sdt.trim())) {
    errs.sdt = 'Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0.'
  }

  if (profile.value.ngaySinh) {
    const dob = new Date(profile.value.ngaySinh)
    const today = new Date()
    let age = today.getFullYear() - dob.getFullYear()
    const m = today.getMonth() - dob.getMonth()
    if (m < 0 || (m === 0 && today.getDate() < dob.getDate())) {
      age--
    }
    if (age < 15) {
      errs.ngaySinh = 'Theo quy tắc hệ thống, khách hàng phải từ đủ 15 tuổi (Bộ luật Dân sự 2015).'
    }
  }

  profileErrors.value = errs
  return Object.keys(errs).length === 0
}

async function saveProfile() {
  if (!validateProfile()) return false
  try {
    const p = profile.value
    const res = await api.put('/khach/tai-khoan', {
      hoTen: p.hoTen,
      sdt: p.sdt,
      email: p.email,
      ngaySinh: p.ngaySinh || null,
      gioiTinh: p.gioiTinh,
    })
    auth.ghiPhien(res.data)
    nhanHoSo(res.data.hoSo)
    profileSaved.value = true
    setTimeout(() => {
      profileSaved.value = false
    }, 3000)
    return true
  } catch (e) {
    profileErrors.value = { ...(e.loiTheoTruong || {}), chung: e.message || 'Không lưu được thông tin tài khoản.' }
    return false
  }
}

// Một nút duy nhất: lưu hồ sơ, và đổi mật khẩu nếu khách có nhập vào mục đổi mật khẩu
async function saveAll() {
  const muonDoiMatKhau = !!(pwForm.value.matKhauCu || pwForm.value.matKhauMoi || pwForm.value.nhapLai)
  const hoSoOk = validateProfile()
  const matKhauOk = muonDoiMatKhau ? validatePassword() : true
  if (!hoSoOk || !matKhauOk) return
  pwErrors.value = {}
  pwSuccess.value = ''
  if (muonDoiMatKhau) {
    const ok = await changePassword()
    if (!ok) return
  }
  saveProfile()
}

// Đổi mật khẩu: backend cấp token mới nên phải lưu lại phiên ngay
function validatePassword() {
  const f = pwForm.value
  const errs = {}
  if (!f.matKhauCu) errs.matKhauCu = 'Vui lòng nhập mật khẩu hiện tại.'
  const moi = loiMatKhau(f.matKhauMoi, 'Vui lòng nhập mật khẩu mới.')
  if (moi) errs.matKhauMoi = moi
  else if (f.matKhauMoi === f.matKhauCu) errs.matKhauMoi = 'Mật khẩu mới phải khác mật khẩu hiện tại.'
  if (!f.nhapLai) errs.nhapLai = 'Vui lòng nhập lại mật khẩu mới.'
  else if (f.nhapLai !== f.matKhauMoi) errs.nhapLai = 'Mật khẩu nhập lại không khớp.'
  pwErrors.value = errs
  return Object.keys(errs).length === 0
}

async function changePassword() {
  if (pwSaving.value) return false
  pwSaving.value = true
  try {
    const res = await api.put('/khach/doi-mat-khau', { ...pwForm.value })
    auth.ghiPhien(res.data)
    pwForm.value = { matKhauCu: '', matKhauMoi: '', nhapLai: '' }
    pwSuccess.value = 'Đổi mật khẩu thành công! Các phiên đăng nhập cũ trên máy khác đã hết hiệu lực.'
    return true
  } catch (e) {
    pwErrors.value = { ...(e.loiTheoTruong || {}), chung: e.message || 'Không đổi được mật khẩu.' }
    return false
  } finally {
    pwSaving.value = false
  }
}

async function setDefaultAddress(id) {
  try {
    await khachDiaChiService.setDefault(id)
    await loadAddresses()
  } catch (e) {
    addressError.value = e.message || 'Không đặt được địa chỉ mặc định.'
  }
}

// Hộp thoại xác nhận + thông báo kết quả (thay cho confirm()/alert() mặc định của trình duyệt)
const xacNhan = ref(null) // { title, message, confirmText, variant, icon, chay }
const xacNhanLoading = ref(false)
const thongBao = ref(null) // { loai: 'success' | 'danger', noiDung }
let thongBaoTimer = null

function hienThongBao(loai, noiDung) {
  thongBao.value = { loai, noiDung }
  clearTimeout(thongBaoTimer)
  thongBaoTimer = setTimeout(() => (thongBao.value = null), 3500)
}

async function chayXacNhan() {
  const cfg = xacNhan.value
  if (!cfg) return
  xacNhanLoading.value = true
  try {
    await cfg.chay()
  } finally {
    xacNhanLoading.value = false
    xacNhan.value = null
  }
}

function deleteAddress(id) {
  xacNhan.value = {
    title: 'Xóa địa chỉ?',
    message: 'Địa chỉ này sẽ bị xóa khỏi sổ địa chỉ của bạn.',
    confirmText: 'Xóa địa chỉ',
    variant: 'danger',
    icon: 'warning',
    chay: async () => {
      try {
        await khachDiaChiService.remove(id)
        await loadAddresses()
        hienThongBao('success', 'Đã xóa địa chỉ.')
      } catch (e) {
        addressError.value = e.message || 'Không xóa được địa chỉ.'
      }
    },
  }
}

const LY_DO_HUY = [
  'Muốn thay đổi địa chỉ giao hàng',
  'Muốn nhập/thay đổi mã giảm giá',
  'Muốn đổi sản phẩm (size, màu, số lượng…)',
  'Thủ tục thanh toán quá rắc rối',
  'Tìm thấy giá rẻ hơn ở chỗ khác',
  'Đổi ý, không muốn mua nữa',
  'Đặt nhầm đơn hàng',
]
const LY_DO_KHAC = 'Lý do khác'

const huyDon = ref(null) // { ma, lyDo, lyDoKhac, loi }
const huyDonLoading = ref(false)

// Khóa cuộn trang nền khi hộp thoại đang mở (bù độ rộng thanh cuộn để trang không bị giật)
let overflowCu = ''
let paddingCu = ''
let dangKhoa = false
function khoaCuon(khoa) {
  const body = document.body
  if (khoa && !dangKhoa) {
    dangKhoa = true
    overflowCu = body.style.overflow
    paddingCu = body.style.paddingRight
    const rongThanhCuon = window.innerWidth - document.documentElement.clientWidth
    body.style.overflow = 'hidden'
    if (rongThanhCuon > 0) body.style.paddingRight = `${rongThanhCuon}px`
  } else if (!khoa && dangKhoa) {
    dangKhoa = false
    body.style.overflow = overflowCu
    body.style.paddingRight = paddingCu
  }
}
watch(() => !!huyDon.value, (mo) => khoaCuon(mo))
onBeforeUnmount(() => khoaCuon(false))

function cancelOrder(maHoaDon) {
  huyDon.value = { ma: maHoaDon, lyDo: '', lyDoKhac: '', loi: '' }
}

async function xacNhanHuyDon() {
  const h = huyDon.value
  if (!h) return
  const lyDo = h.lyDo === LY_DO_KHAC ? h.lyDoKhac.trim() : h.lyDo
  if (!lyDo) {
    h.loi = h.lyDo === LY_DO_KHAC ? 'Vui lòng nhập lý do hủy.' : 'Vui lòng chọn lý do hủy đơn.'
    return
  }
  huyDonLoading.value = true
  try {
    // Hủy thật trên backend để cửa hàng và trang tra cứu cùng thấy "Đã hủy"
    await api.put(`/khach/don-hang/${encodeURIComponent(h.ma)}/huy`, { lyDo })
  } catch (e) {
    // 404: đơn chỉ còn là bản lưu trên trình duyệt, cửa hàng không có đơn này -> chỉ cần hủy bản trên máy
    if (e?.status !== 404) {
      h.loi = e?.message || 'Không hủy được đơn hàng.'
      huyDonLoading.value = false
      return
    }
  }
  huyDonLoading.value = false
  const target = orders.value.find((o) => o.maHoaDon === h.ma)
  if (target) {
    target.trangThai = 6
    target.trangThaiText = 'Đã hủy'
    ghiDonHang(orders.value)
  }
  hienThongBao('success', `Đã hủy đơn hàng ${h.ma}.`)
  huyDon.value = null
}
</script>

<template>
  <div class="account-view py-4">
    <ConfirmDialog
      v-if="xacNhan"
      :title="xacNhan.title"
      :message="xacNhan.message"
      :confirm-text="xacNhan.confirmText"
      :variant="xacNhan.variant"
      :icon="xacNhan.icon"
      :loading="xacNhanLoading"
      @confirm="chayXacNhan"
      @cancel="xacNhan = null"
    />
    <!-- Hủy đơn: chọn lý do (giống Shopee / TikTok Shop) -->
    <div v-if="huyDon" class="ac-modal-backdrop" @click.self="!huyDonLoading && (huyDon = null)">
      <div class="ac-modal" role="dialog" aria-modal="true" aria-labelledby="ac-huy-title">
        <div class="ac-modal-head">
          <h5 id="ac-huy-title" class="mb-0 fw-bold">
            Lý do hủy đơn <span class="ac-modal-code">{{ huyDon.ma }}</span>
          </h5>
          <button type="button" class="btn-close" aria-label="Đóng" :disabled="huyDonLoading" @click="huyDon = null"></button>
        </div>

        <div class="ac-reasons">
          <label
            v-for="r in [...LY_DO_HUY, LY_DO_KHAC]"
            :key="r"
            class="ac-reason"
            :class="{ active: huyDon.lyDo === r }"
          >
            <input v-model="huyDon.lyDo" type="radio" name="ly-do-huy" :value="r" @change="huyDon.loi = ''" />
            <span>{{ r }}</span>
          </label>
          <textarea
            v-if="huyDon.lyDo === LY_DO_KHAC"
            v-model="huyDon.lyDoKhac"
            class="form-control mt-1"
            rows="3"
            maxlength="400"
            placeholder="Nhập lý do hủy đơn của bạn..."
            @input="huyDon.loi = ''"
          ></textarea>
        </div>

        <div v-if="huyDon.loi" class="ac-modal-error"><i class="bi bi-exclamation-circle-fill me-1"></i>{{ huyDon.loi }}</div>

        <div class="ac-modal-foot">
          <button type="button" class="btn btn-light btn-sm rounded-pill px-3" :disabled="huyDonLoading" @click="huyDon = null">
            Không phải bây giờ
          </button>
          <button type="button" class="btn btn-danger btn-sm rounded-pill px-3" :disabled="huyDonLoading" @click="xacNhanHuyDon">
            <span v-if="huyDonLoading" class="spinner-border spinner-border-sm me-1" aria-hidden="true"></span>
            Hủy đơn hàng
          </button>
        </div>
        <p class="ac-modal-hint"><i class="bi bi-info-circle me-1"></i>Sau khi hủy, đơn hàng sẽ không thể khôi phục.</p>
      </div>
    </div>
    <Transition name="ac-toast">
      <div v-if="thongBao" class="ac-toast" :class="thongBao.loai" role="status">
        <i class="bi" :class="thongBao.loai === 'success' ? 'bi-check-circle-fill' : 'bi-exclamation-circle-fill'"></i>
        <span>{{ thongBao.noiDung }}</span>
      </div>
    </Transition>
    <div class="container">
      <!-- Breadcrumb -->
      <nav aria-label="breadcrumb" class="mb-4">
        <ol class="breadcrumb mb-0 small">
          <li class="breadcrumb-item"><RouterLink to="/shop">Trang chủ</RouterLink></li>
          <li class="breadcrumb-item active" aria-current="page">Tài khoản cá nhân</li>
        </ol>
      </nav>

      <div class="row g-4">
        <!-- Sidebar Menu tài khoản -->
        <div class="col-lg-3">
          <div class="card border-0 shadow-sm rounded-4 p-3 mb-4">
            <div class="d-flex align-items-center gap-3 p-2 mb-3 border-bottom pb-3">
              <div class="rounded-circle bg-primary text-white d-flex align-items-center justify-content-center fw-bold fs-4" style="width: 50px; height: 50px;">
                {{ profile.hoTen.charAt(0) }}
              </div>
              <div>
                <h6 class="fw-bold mb-0 text-dark">{{ profile.hoTen }}</h6>
                <span class="small text-muted">{{ profile.sdt }}</span>
              </div>
            </div>

            <div class="nav flex-column gap-1">
              <button
                class="btn text-start d-flex align-items-center gap-2 py-2 px-3 rounded-3"
                :class="activeTab === 'orders' ? 'btn-primary text-white fw-bold' : 'btn-light text-dark'"
                @click="activeTab = 'orders'"
              >
                <i class="bi bi-bag-check-fill"></i> Theo dõi đơn hàng
              </button>

              <button
                class="btn text-start d-flex align-items-center gap-2 py-2 px-3 rounded-3"
                :class="activeTab === 'profile' ? 'btn-primary text-white fw-bold' : 'btn-light text-dark'"
                @click="activeTab = 'profile'"
              >
                <i class="bi bi-person-fill"></i> Hồ sơ cá nhân
              </button>

              <button
                class="btn text-start d-flex align-items-center gap-2 py-2 px-3 rounded-3"
                :class="activeTab === 'addresses' ? 'btn-primary text-white fw-bold' : 'btn-light text-dark'"
                @click="activeTab = 'addresses'"
              >
                <i class="bi bi-geo-alt-fill"></i> Sổ địa chỉ nhận hàng
              </button>
            </div>
          </div>
        </div>

        <!-- Cột Nội dung Tab -->
        <div class="col-lg-9">
          <!-- TAB 1: THEO DÕI ĐƠN HÀNG (USE CASE 4) -->
          <div v-if="activeTab === 'orders'" class="card border-0 shadow-sm rounded-4 p-4">
            <h5 class="fw-bold mb-3 text-dark d-flex align-items-center gap-2">
              <i class="bi bi-bag-check-fill text-primary"></i> Đơn hàng của tôi
            </h5>

            <!-- Các bộ lọc trạng thái -->
            <div class="ac-tabs mb-4">
              <button
                v-for="t in orderTabs"
                :key="t.key"
                type="button"
                class="ac-tab"
                :class="{ active: orderFilterStatus === t.key }"
                @click="orderFilterStatus = t.key"
              >
                {{ t.label }}
                <span class="ac-tab-count">{{ demTheoTab(t.key) }}</span>
              </button>
            </div>

            <!-- Danh sách đơn hàng -->
            <div v-if="filteredOrders.length === 0" class="text-center py-5 text-muted">
              <i class="bi bi-clipboard-x display-4 text-muted mb-2"></i>
              <p>Chưa có đơn hàng nào trong mục này.</p>
            </div>

            <div v-else class="d-flex flex-column gap-3">
              <div v-for="order in filteredOrders" :key="order.maHoaDon" class="ac-order">
                <div class="ac-order-head">
                  <div class="d-flex align-items-center flex-wrap gap-2">
                    <i class="bi bi-receipt text-primary"></i>
                    <strong class="ac-order-code">{{ order.maHoaDon }}</strong>
                    <span class="ac-order-date"><i class="bi bi-clock me-1"></i>{{ formatNgayGio(order.ngayTao) }}</span>
                  </div>
                  <span class="ac-status" :class="statusClass(order.trangThai)">
                    <i class="bi me-1" :class="statusIcon(order.trangThai)"></i>{{ order.trangThaiText }}
                  </span>
                </div>

                <div class="ac-order-items">
                  <div v-for="(it, idx) in order.items" :key="idx" class="ac-item">
                    <div class="ac-item-name">
                      <strong>{{ it.sanPhamTen || it.tenSanPham || 'Sản phẩm' }}</strong>
                      <span v-if="it.mauSac" class="ac-item-meta">{{ it.mauSac }}<template v-if="it.trongLuong"> · {{ it.trongLuong }}</template></span>
                      <span class="ac-item-qty">x{{ it.soLuong }}</span>
                    </div>
                    <div class="fw-semibold">{{ formatMoney(it.giaBan || it.donGia || 0) }}</div>
                  </div>
                </div>

                <div class="ac-order-foot">
                  <div class="ac-total">
                    Tổng thanh toán
                    <strong>{{ formatMoney(order.tongThanhToan) }}</strong>
                  </div>
                  <div class="d-flex gap-2">
                    <button
                      v-if="order.trangThai === 0"
                      type="button"
                      class="btn btn-outline-danger btn-sm rounded-pill px-3"
                      @click="cancelOrder(order.maHoaDon)"
                    >
                      <i class="bi bi-x-circle me-1"></i>Hủy đơn
                    </button>
                    <RouterLink
                      :to="{ path: '/shop/tra-cuu', query: { ma: order.maHoaDon } }"
                      class="btn btn-primary btn-sm rounded-pill px-3"
                    >
                      <i class="bi bi-truck me-1"></i>Chi tiết hành trình
                    </RouterLink>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- TAB 2: HỒ SƠ CÁ NHÂN (USE CASE 5) -->
          <div v-else-if="activeTab === 'profile'" class="card border-0 shadow-sm rounded-4 p-4">
            <h5 class="fw-bold mb-3 text-dark d-flex align-items-center gap-2">
              <i class="bi bi-person-fill text-primary"></i> Thông tin tài khoản
            </h5>

            <div v-if="profileSaved" class="alert alert-success py-2 small mb-3">
              <i class="bi bi-check-circle-fill me-1"></i> Cập nhật thông tin tài khoản thành công!
            </div>

            <div v-if="profileErrors.chung" class="alert alert-danger py-2 small mb-3">{{ profileErrors.chung }}</div>

            <form novalidate @submit.prevent="saveAll">
              <div class="row g-3">
                <div class="col-md-6">
                  <label class="form-label small fw-semibold">Họ và tên <span class="text-danger">*</span></label>
                  <input
                    type="text"
                    class="form-control"
                    :class="{ 'is-invalid': profileErrors.hoTen }"
                    v-model="profile.hoTen"
                  />
                  <div v-if="profileErrors.hoTen" class="invalid-feedback">{{ profileErrors.hoTen }}</div>
                </div>

                <div class="col-md-6">
                  <label class="form-label small fw-semibold">Số điện thoại <span class="text-danger">*</span></label>
                  <input
                    type="tel"
                    class="form-control"
                    :class="{ 'is-invalid': profileErrors.sdt }"
                    v-model="profile.sdt"
                  />
                  <div v-if="profileErrors.sdt" class="invalid-feedback">{{ profileErrors.sdt }}</div>
                </div>

                <div class="col-md-6">
                  <label class="form-label small fw-semibold">Email</label>
                  <input type="email" class="form-control" v-model="profile.email" />
                </div>

                <div class="col-md-6">
                  <label class="form-label small fw-semibold">Ngày sinh</label>
                  <input
                    type="date"
                    class="form-control"
                    :class="{ 'is-invalid': profileErrors.ngaySinh }"
                    v-model="profile.ngaySinh"
                  />
                  <div v-if="profileErrors.ngaySinh" class="invalid-feedback">{{ profileErrors.ngaySinh }}</div>
                  <div class="text-muted small mt-1" style="font-size: 0.75rem;">
                    * Khách hàng phải từ đủ 15 tuổi theo quy định của FootStyle.
                  </div>
                </div>

                <div class="col-12">
                  <label class="form-label small fw-semibold d-block">Giới tính</label>
                  <div class="d-flex gap-4">
                    <label class="form-check-label d-flex align-items-center gap-1">
                      <input type="radio" class="form-check-input" :value="1" v-model="profile.gioiTinh" /> Nam
                    </label>
                    <label class="form-check-label d-flex align-items-center gap-1">
                      <input type="radio" class="form-check-input" :value="0" v-model="profile.gioiTinh" /> Nữ
                    </label>
                  </div>
                </div>

              </div>

            <!-- Đổi mật khẩu (tùy chọn) -->
            <hr class="my-4" />
            <h5 class="fw-bold mb-3 text-dark d-flex align-items-center gap-2">
              <i class="bi bi-key-fill text-primary"></i> Đổi mật khẩu
            </h5>

            <div v-if="pwSuccess" class="alert alert-success py-2 small mb-3">
              <i class="bi bi-check-circle-fill me-1"></i> {{ pwSuccess }}
            </div>
            <div v-if="pwErrors.chung" class="alert alert-danger py-2 small mb-3">{{ pwErrors.chung }}</div>

              <div class="row g-3">
                <div class="col-md-6">
                  <label class="form-label small fw-semibold">Mật khẩu hiện tại</label>
                  <input
                    type="password"
                    class="form-control"
                    :class="{ 'is-invalid': pwErrors.matKhauCu }"
                    v-model="pwForm.matKhauCu"
                    autocomplete="current-password"
                    maxlength="50"
                  />
                  <div v-if="pwErrors.matKhauCu" class="invalid-feedback">{{ pwErrors.matKhauCu }}</div>
                </div>
                <div class="col-md-6"></div>

                <div class="col-md-6">
                  <label class="form-label small fw-semibold">Mật khẩu mới</label>
                  <input
                    type="password"
                    class="form-control"
                    :class="{ 'is-invalid': pwErrors.matKhauMoi }"
                    v-model="pwForm.matKhauMoi"
                    autocomplete="new-password"
                    maxlength="50"
                  />
                  <div v-if="pwErrors.matKhauMoi" class="invalid-feedback">{{ pwErrors.matKhauMoi }}</div>
                  <div v-else class="text-muted small mt-1" style="font-size: 0.75rem;">
                    8-50 ký tự, có cả chữ và số, không có khoảng trắng.
                  </div>
                </div>

                <div class="col-md-6">
                  <label class="form-label small fw-semibold">Nhập lại mật khẩu mới</label>
                  <input
                    type="password"
                    class="form-control"
                    :class="{ 'is-invalid': pwErrors.nhapLai }"
                    v-model="pwForm.nhapLai"
                    autocomplete="new-password"
                    maxlength="50"
                  />
                  <div v-if="pwErrors.nhapLai" class="invalid-feedback">{{ pwErrors.nhapLai }}</div>
                </div>

                <div class="col-12 mt-3">
                  <button type="submit" class="btn btn-primary rounded-pill px-4 fw-semibold" :disabled="pwSaving">
                    {{ pwSaving ? 'Đang lưu...' : 'Lưu thay đổi' }}
                  </button>
                </div>
              </div>
            </form>
          </div>

          <!-- TAB 3: SỔ ĐỊA CHỈ (USE CASE 5) -->
          <div v-else-if="activeTab === 'addresses'" class="card border-0 shadow-sm rounded-4 p-4">
            <div class="d-flex justify-content-between align-items-center mb-4">
              <h5 class="fw-bold mb-0 text-dark d-flex align-items-center gap-2">
                <i class="bi bi-geo-alt-fill text-primary"></i> Sổ địa chỉ nhận hàng
              </h5>
              <button class="btn btn-outline-primary btn-sm rounded-pill" type="button" @click="openAddressForm">
                <i class="bi bi-plus-lg me-1"></i> Thêm địa chỉ mới
              </button>
            </div>

            <div v-if="addressError && !showAddressForm" class="alert alert-danger py-2 small">{{ addressError }}</div>

            <!-- Form thêm địa chỉ mới -->
            <form v-if="showAddressForm" class="border rounded-3 p-3 mb-3 bg-light-subtle" @submit.prevent="saveAddress">
              <h6 class="fw-bold mb-3">Thêm địa chỉ mới</h6>
              <div v-if="addressError" class="alert alert-danger py-2 small">{{ addressError }}</div>
              <div class="row g-3">
                <div class="col-md-6">
                  <label class="form-label small text-muted mb-1">Tên người nhận *</label>
                  <input v-model="addressForm.tenNguoiNhan" type="text" class="form-control" :class="{ 'is-invalid': addressErrors.tenNguoiNhan }" />
                  <div v-if="addressErrors.tenNguoiNhan" class="invalid-feedback">{{ addressErrors.tenNguoiNhan }}</div>
                </div>
                <div class="col-md-6">
                  <label class="form-label small text-muted mb-1">Số điện thoại *</label>
                  <input v-model="addressForm.sdtNguoiNhan" type="text" maxlength="10" class="form-control" :class="{ 'is-invalid': addressErrors.sdtNguoiNhan }" />
                  <div v-if="addressErrors.sdtNguoiNhan" class="invalid-feedback">{{ addressErrors.sdtNguoiNhan }}</div>
                </div>
                <DiaChiHanhChinhSelect
                  v-model:tinh-thanh="addressForm.tinhThanhPho"
                  v-model:phuong-xa="addressForm.phuongXa"
                  required
                  id-prefix="tk-dc"
                  :invalid-tinh="!!addressErrors.tinhThanhPho"
                  :invalid-phuong="!!addressErrors.phuongXa"
                  :error-tinh="addressErrors.tinhThanhPho || ''"
                  :error-phuong="addressErrors.phuongXa || ''"
                />
                <div class="col-12">
                  <label class="form-label small text-muted mb-1">Địa chỉ cụ thể *</label>
                  <input v-model="addressForm.diaChiCuThe" type="text" class="form-control" placeholder="Số nhà, tên đường..." :class="{ 'is-invalid': addressErrors.diaChiCuThe }" />
                  <div v-if="addressErrors.diaChiCuThe" class="invalid-feedback">{{ addressErrors.diaChiCuThe }}</div>
                </div>
                <div class="col-12">
                  <label class="form-check">
                    <input v-model="addressForm.laDiaChiMacDinh" type="checkbox" class="form-check-input" :disabled="addresses.length === 0" />
                    <span class="form-check-label small">Đặt làm địa chỉ mặc định</span>
                  </label>
                </div>
              </div>
              <div class="d-flex gap-2 justify-content-end mt-3">
                <button type="button" class="btn btn-light btn-sm rounded-pill px-3" @click="showAddressForm = false">Hủy</button>
                <button type="submit" class="btn btn-primary btn-sm rounded-pill px-3" :disabled="addressSaving">
                  {{ addressSaving ? 'Đang lưu...' : 'Lưu địa chỉ' }}
                </button>
              </div>
            </form>

            <div v-if="addresses.length === 0 && !showAddressForm" class="text-muted small text-center py-4">
              Bạn chưa có địa chỉ nhận hàng nào. Bấm "Thêm địa chỉ mới" để thêm.
            </div>

            <div class="d-flex flex-column gap-3">
              <div
                v-for="addr in addresses"
                :key="addr.id"
                class="border rounded-3 p-3 position-relative"
                :class="{ 'border-primary bg-light-subtle': addr.laDiaChiMacDinh }"
              >
                <div class="d-flex justify-content-between align-items-start mb-2">
                  <div>
                    <strong class="me-2">{{ addr.tenNguoiNhan }}</strong>
                    <span class="text-muted small">| {{ addr.sdtNguoiNhan }}</span>
                    <span v-if="addr.laDiaChiMacDinh" class="badge bg-primary ms-2">Mặc định</span>
                  </div>
                  <div class="d-flex gap-2">
                    <button
                      v-if="!addr.laDiaChiMacDinh"
                      class="btn btn-link btn-sm text-decoration-none p-0"
                      @click="setDefaultAddress(addr.id)"
                    >
                      Đặt mặc định
                    </button>
                    <button
                      v-if="!addr.laDiaChiMacDinh"
                      class="btn btn-link btn-sm text-danger text-decoration-none p-0"
                      @click="deleteAddress(addr.id)"
                    >
                      Xóa
                    </button>
                  </div>
                </div>

                <div class="small text-muted">
                  {{ addr.diaChiCuThe }}, {{ addr.phuongXa }}, {{ addr.tinhThanhPho }}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.ac-modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 1900;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
  overscroll-behavior: contain;
  background: rgba(15, 23, 42, 0.5);
  backdrop-filter: blur(2px);
}
.ac-modal {
  width: 100%;
  max-width: 440px;
  max-height: 92vh;
  overflow-y: auto;
  overscroll-behavior: contain;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.25);
  padding: 1rem 1.15rem 0.9rem;
}
.ac-modal-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.7rem;
}
.ac-modal-head h5 {
  font-size: 1rem;
}
.ac-modal-code {
  margin-left: 0.35rem;
  padding: 0.1rem 0.5rem;
  font-size: 0.78rem;
  color: #0d6efd;
  background: #e8f1ff;
  border-radius: 999px;
}
.ac-reasons {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}
.ac-reason {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.45rem 0.7rem;
  font-size: 0.86rem;
  line-height: 1.3;
  border: 1px solid #e9ecf1;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.15s ease;
}
.ac-reason:hover {
  border-color: #fca5a5;
  background: #fffafa;
}
.ac-reason.active {
  border-color: #dc2626;
  background: #fef2f2;
  color: #991b1b;
  font-weight: 600;
}
.ac-reason input {
  accent-color: #dc2626;
  width: 1rem;
  height: 1rem;
  flex-shrink: 0;
  margin: 0;
}
.ac-reasons textarea {
  font-size: 0.86rem;
  border-radius: 10px;
  resize: none;
}
.ac-modal-error {
  margin-top: 0.5rem;
  font-size: 0.82rem;
  color: #b91c1c;
}
.ac-modal-foot {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 0.8rem;
}
.ac-modal-hint {
  margin: 0.6rem 0 0;
  text-align: center;
  font-size: 0.74rem;
  color: #9aa3b2;
}

.ac-toast {
  position: fixed;
  top: 1.25rem;
  left: 50%;
  transform: translateX(-50%);
  z-index: 2000;
  display: flex;
  align-items: center;
  gap: 0.6rem;
  max-width: 90vw;
  padding: 0.75rem 1.2rem;
  font-size: 0.92rem;
  font-weight: 500;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.18);
  border-left: 4px solid #16a34a;
}
.ac-toast.success i { color: #16a34a; }
.ac-toast.danger { border-left-color: #dc2626; }
.ac-toast.danger i { color: #dc2626; }
.ac-toast-enter-active,
.ac-toast-leave-active {
  transition: all 0.25s ease;
}
.ac-toast-enter-from,
.ac-toast-leave-to {
  opacity: 0;
  transform: translate(-50%, -12px);
}

.ac-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #eef0f4;
}
.ac-tab {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  border: 1px solid #dfe3ea;
  background: #fff;
  color: #4b5563;
  font-size: 0.85rem;
  font-weight: 500;
  padding: 0.4rem 0.9rem;
  border-radius: 999px;
  transition: all 0.15s ease;
}
.ac-tab:hover {
  border-color: #0d6efd;
  color: #0d6efd;
}
.ac-tab.active {
  background: #0d6efd;
  border-color: #0d6efd;
  color: #fff;
  box-shadow: 0 4px 10px rgba(13, 110, 253, 0.25);
}
.ac-tab-count {
  min-width: 1.4rem;
  padding: 0 0.4rem;
  font-size: 0.72rem;
  line-height: 1.3rem;
  text-align: center;
  border-radius: 999px;
  background: #eef2f7;
  color: #6b7280;
}
.ac-tab.active .ac-tab-count {
  background: rgba(255, 255, 255, 0.25);
  color: #fff;
}

.ac-order {
  border: 1px solid #e8ebf0;
  border-radius: 16px;
  background: #fff;
  overflow: hidden;
  transition: box-shadow 0.2s ease, transform 0.2s ease;
}
.ac-order:hover {
  box-shadow: 0 8px 22px rgba(15, 23, 42, 0.08);
  transform: translateY(-1px);
}
.ac-order-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.75rem;
  padding: 0.85rem 1.1rem;
  background: #f8fafc;
  border-bottom: 1px solid #eef0f4;
}
.ac-order-code {
  color: #0d6efd;
  font-size: 1rem;
}
.ac-order-date {
  color: #8a94a6;
  font-size: 0.8rem;
}
.ac-status {
  display: inline-flex;
  align-items: center;
  font-size: 0.78rem;
  font-weight: 600;
  padding: 0.3rem 0.75rem;
  border-radius: 999px;
  white-space: nowrap;
}
.ac-status.cho { background: #fff4d6; color: #a56a00; }
.ac-status.giao { background: #e0f2fe; color: #0369a1; }
.ac-status.xong { background: #dcfce7; color: #15803d; }
.ac-status.huy { background: #fee2e2; color: #b91c1c; }

.ac-order-items {
  padding: 0.5rem 1.1rem;
}
.ac-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  padding: 0.6rem 0;
  font-size: 0.9rem;
}
.ac-item + .ac-item {
  border-top: 1px dashed #eef0f4;
}
.ac-item-meta {
  margin-left: 0.5rem;
  padding: 0.1rem 0.5rem;
  font-size: 0.75rem;
  color: #6b7280;
  background: #f1f4f8;
  border-radius: 6px;
}
.ac-item-qty {
  margin-left: 0.5rem;
  color: #8a94a6;
  font-size: 0.82rem;
}

.ac-order-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.75rem;
  padding: 0.85rem 1.1rem;
  border-top: 1px solid #eef0f4;
}
.ac-total {
  font-size: 0.85rem;
  color: #6b7280;
}
.ac-total strong {
  margin-left: 0.4rem;
  font-size: 1.1rem;
  color: #dc3545;
}
</style>
