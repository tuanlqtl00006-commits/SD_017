<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '../../composables/useAuth'
import { formatMoney, formatNgayGio } from '../../utils/format'
import { docDonHang, ghiDonHang } from '../../utils/donHangLocal'
import { khachDiaChiService } from '../../services/khachDiaChiService'
import DiaChiHanhChinhSelect from '../../components/common/DiaChiHanhChinhSelect.vue'

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
  await loadAddresses()

  if (route.query.tab) {
    activeTab.value = String(route.query.tab)
  }
  loadOrders()
})

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

function loadOrders() {
  orders.value = docDonHang()
}

const filteredOrders = computed(() => {
  if (orderFilterStatus.value === 'ALL') return orders.value
  if (orderFilterStatus.value === 'CHO_XAC_NHAN') {
    return orders.value.filter((o) => o.trangThai === 0)
  }
  if (orderFilterStatus.value === 'DANG_GIAO') {
    return orders.value.filter((o) => o.trangThai === 1 || o.trangThai === 2)
  }
  if (orderFilterStatus.value === 'HOAN_THANH') {
    return orders.value.filter((o) => o.trangThai === 3)
  }
  if (orderFilterStatus.value === 'DA_HUY') {
    return orders.value.filter((o) => o.trangThai === 6 || o.trangThai === 7)
  }
  return orders.value
})

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

function saveProfile() {
  if (!validateProfile()) return
  profileSaved.value = true
  setTimeout(() => {
    profileSaved.value = false
  }, 3000)
}

async function setDefaultAddress(id) {
  try {
    await khachDiaChiService.setDefault(id)
    await loadAddresses()
  } catch (e) {
    addressError.value = e.message || 'Không đặt được địa chỉ mặc định.'
  }
}

async function deleteAddress(id) {
  if (!confirm('Bạn có chắc chắn muốn xóa địa chỉ này?')) return
  try {
    await khachDiaChiService.remove(id)
    await loadAddresses()
  } catch (e) {
    addressError.value = e.message || 'Không xóa được địa chỉ.'
  }
}

function cancelOrder(maHoaDon) {
  if (confirm(`Bạn có chắc chắn muốn hủy đơn hàng ${maHoaDon}?`)) {
    const target = orders.value.find((o) => o.maHoaDon === maHoaDon)
    if (target) {
      target.trangThai = 6
      target.trangThaiText = 'Đã hủy'
      ghiDonHang(orders.value)
    }
  }
}
</script>

<template>
  <div class="account-view py-4">
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
            <div class="d-flex flex-wrap gap-2 mb-4 border-bottom pb-3">
              <button
                class="btn btn-sm rounded-pill px-3"
                :class="orderFilterStatus === 'ALL' ? 'btn-primary' : 'btn-outline-secondary'"
                @click="orderFilterStatus = 'ALL'"
              >
                Tất cả
              </button>
              <button
                class="btn btn-sm rounded-pill px-3"
                :class="orderFilterStatus === 'CHO_XAC_NHAN' ? 'btn-primary' : 'btn-outline-secondary'"
                @click="orderFilterStatus = 'CHO_XAC_NHAN'"
              >
                Chờ xác nhận
              </button>
              <button
                class="btn btn-sm rounded-pill px-3"
                :class="orderFilterStatus === 'DANG_GIAO' ? 'btn-primary' : 'btn-outline-secondary'"
                @click="orderFilterStatus = 'DANG_GIAO'"
              >
                Đang giao
              </button>
              <button
                class="btn btn-sm rounded-pill px-3"
                :class="orderFilterStatus === 'HOAN_THANH' ? 'btn-primary' : 'btn-outline-secondary'"
                @click="orderFilterStatus = 'HOAN_THANH'"
              >
                Đã hoàn thành
              </button>
              <button
                class="btn btn-sm rounded-pill px-3"
                :class="orderFilterStatus === 'DA_HUY' ? 'btn-primary' : 'btn-outline-secondary'"
                @click="orderFilterStatus = 'DA_HUY'"
              >
                Đã hủy
              </button>
            </div>

            <!-- Danh sách đơn hàng -->
            <div v-if="filteredOrders.length === 0" class="text-center py-5 text-muted">
              <i class="bi bi-clipboard-x display-4 text-muted mb-2"></i>
              <p>Chưa có đơn hàng nào trong mục này.</p>
            </div>

            <div v-else class="d-flex flex-column gap-3">
              <div
                v-for="order in filteredOrders"
                :key="order.maHoaDon"
                class="border rounded-3 p-3 bg-white hover-shadow"
              >
                <div class="d-flex justify-content-between align-items-center border-bottom pb-2 mb-2">
                  <div>
                    <span class="small text-muted">Mã đơn: </span>
                    <strong class="text-primary">{{ order.maHoaDon }}</strong>
                    <span class="small text-muted ms-3">{{ formatNgayGio(order.ngayTao) }}</span>
                  </div>
                  <div>
                    <span
                      class="badge"
                      :class="{
                        'bg-warning text-dark': order.trangThai === 0,
                        'bg-info text-dark': order.trangThai === 1 || order.trangThai === 2,
                        'bg-success': order.trangThai === 3,
                        'bg-danger': order.trangThai === 6 || order.trangThai === 7
                      }"
                    >
                      {{ order.trangThaiText }}
                    </span>
                  </div>
                </div>

                <!-- Danh sách sản phẩm tóm tắt -->
                <div class="mb-3">
                  <div
                    v-for="(it, idx) in order.items"
                    :key="idx"
                    class="d-flex justify-content-between align-items-center py-1 small"
                  >
                    <div>
                      <strong>{{ it.sanPhamTen || it.tenSanPham }}</strong>
                      <span class="text-muted ms-2" v-if="it.mauSac">({{ it.mauSac }} - {{ it.trongLuong }})</span>
                      <span class="text-muted ms-2">x{{ it.soLuong }}</span>
                    </div>
                    <div class="fw-semibold">{{ formatMoney(it.giaBan || it.donGia || 0) }}</div>
                  </div>
                </div>

                <div class="d-flex justify-content-between align-items-center pt-2 border-top">
                  <div class="small">
                    Tổng thanh toán: <strong class="fs-6 text-danger">{{ formatMoney(order.tongThanhToan) }}</strong>
                  </div>
                  <div class="d-flex gap-2">
                    <button
                      v-if="order.trangThai === 0"
                      class="btn btn-outline-danger btn-sm"
                      @click="cancelOrder(order.maHoaDon)"
                    >
                      Hủy đơn
                    </button>
                    <RouterLink
                      :to="{ path: '/shop/tra-cuu', query: { ma: order.maHoaDon } }"
                      class="btn btn-outline-primary btn-sm"
                    >
                      Chi tiết hành trình
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

            <form @submit.prevent="saveProfile">
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

                <div class="col-12 mt-4">
                  <button type="submit" class="btn btn-primary rounded-pill px-4 fw-semibold">
                    Lưu thay đổi
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
