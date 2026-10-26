<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '../../composables/useAuth'
import { formatMoney, formatNgayGio } from '../../utils/format'

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
    addresses.value = [
      {
        id: 1,
        tenNguoiNhan: u.hoTen || u.ten || 'Khách hàng',
        sdtNguoiNhan: u.sdt || '',
        diaChiCuThe: u.diaChi || 'Chưa cập nhật địa chỉ giao hàng',
        phuongXa: '',
        tinhThanhPho: '',
        laDiaChiMacDinh: true,
      },
    ]
  }

  if (route.query.tab) {
    activeTab.value = String(route.query.tab)
  }
  loadOrders()
})

function loadOrders() {
  try {
    const raw = localStorage.getItem('footstyle_orders')
    orders.value = raw ? JSON.parse(raw) : []
  } catch (e) {
    orders.value = []
  }
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

function setDefaultAddress(id) {
  addresses.value.forEach((a) => {
    a.laDiaChiMacDinh = a.id === id
  })
}

function deleteAddress(id) {
  addresses.value = addresses.value.filter((a) => a.id !== id)
}

function cancelOrder(maHoaDon) {
  if (confirm(`Bạn có chắc chắn muốn hủy đơn hàng ${maHoaDon}?`)) {
    const target = orders.value.find((o) => o.maHoaDon === maHoaDon)
    if (target) {
      target.trangThai = 6
      target.trangThaiText = 'Đã hủy'
      try {
        localStorage.setItem('footstyle_orders', JSON.stringify(orders.value))
      } catch (e) {}
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
              <button class="btn btn-outline-primary btn-sm rounded-pill">
                <i class="bi bi-plus-lg me-1"></i> Thêm địa chỉ mới
              </button>
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
