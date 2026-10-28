<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCartStore } from '../../stores/cart'
import { useAuth } from '../../composables/useAuth'
import { khachDiaChiService } from '../../services/khachDiaChiService'
import DiaChiHanhChinhSelect from '../../components/common/DiaChiHanhChinhSelect.vue'
import { docDonHang, ghiDonHang } from '../../utils/donHangLocal'
import { formatMoney } from '../../utils/format'

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()
const auth = useAuth()

// Phải đăng nhập tài khoản khách hàng mới được thanh toán
function laKhachDaDangNhap() {
  return auth.trangThai.nguoiDung?.vaiTro === 'KHACH_HANG'
}

function chuyenDenDangNhap() {
  router.replace({ path: '/shop/dang-nhap', query: { quayLai: route.fullPath } })
}

// Thông tin khách hàng
const form = ref({
  hoTen: '',
  sdt: '',
  email: '',
  tinhThanh: '',
  quanHuyen: '',
  phuongXa: '',
  diaChiCuThe: '',
  ghiChu: '',
  phuongThucThanhToan: 'COD', // COD hoặc QR
})

// Sổ địa chỉ đã lưu của khách đang đăng nhập (tự điền địa chỉ mặc định)
const savedAddresses = ref([])
const selectedAddressId = ref(null)

function applySavedAddress() {
  const a = savedAddresses.value.find((x) => x.id === selectedAddressId.value)
  if (!a) return
  form.value.hoTen = a.tenNguoiNhan || form.value.hoTen
  form.value.sdt = a.sdtNguoiNhan || form.value.sdt
  form.value.tinhThanh = a.tinhThanhPho || ''
  form.value.quanHuyen = a.quanHuyen || ''
  form.value.phuongXa = a.phuongXa || ''
  form.value.diaChiCuThe = a.diaChiCuThe || ''
}

onMounted(async () => {
  await auth.khoiTao()
  if (!laKhachDaDangNhap()) {
    chuyenDenDangNhap()
    return
  }
  const u = auth.trangThai.nguoiDung
  form.value.hoTen = u.hoTen || ''
  form.value.sdt = u.sdt || ''
  form.value.email = u.email || ''
  try {
    savedAddresses.value = (await khachDiaChiService.getAll()) || []
    const macDinh = savedAddresses.value.find((a) => a.laDiaChiMacDinh) || savedAddresses.value[0]
    if (macDinh) {
      selectedAddressId.value = macDinh.id
      applySavedAddress()
    }
  } catch {
    // Không tải được sổ địa chỉ: khách vẫn nhập tay bình thường
  }
})

const errors = ref({})
const submitting = ref(false)

function validate() {
  const errs = {}
  if (!form.value.hoTen.trim()) {
    errs.hoTen = 'Vui lòng nhập họ và tên người nhận.'
  }
  if (!form.value.sdt.trim()) {
    errs.sdt = 'Vui lòng nhập số điện thoại.'
  } else if (!/^0\d{9}$/.test(form.value.sdt.trim())) {
    errs.sdt = 'Số điện thoại phải gồm đúng 10 chữ số và bắt đầu bằng số 0.'
  }
  if (form.value.email.trim() && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.value.email.trim())) {
    errs.email = 'Email không đúng định dạng.'
  }
  if (!form.value.diaChiCuThe.trim()) {
    errs.diaChiCuThe = 'Vui lòng nhập địa chỉ cụ thể (số nhà, ngõ/đường).'
  }

  errors.value = errs
  return Object.keys(errs).length === 0
}

function handleOrderSubmit() {
  if (!laKhachDaDangNhap()) {
    chuyenDenDangNhap()
    return
  }
  if (cartStore.items.length === 0) {
    router.push('/shop/san-pham')
    return
  }

  if (!validate()) {
    return
  }

  submitting.value = true

  // Sinh mã đơn hàng ngẫu nhiên định dạng HD + số
  const randomSuffix = Math.floor(1000 + Math.random() * 9000)
  const maDonHang = `HD${randomSuffix}`

  const fullAddress = [form.value.diaChiCuThe, form.value.phuongXa, form.value.quanHuyen, form.value.tinhThanh]
    .filter((x) => x && String(x).trim())
    .join(', ')

  const newOrder = {
    maHoaDon: maDonHang,
    ngayTao: new Date().toISOString(),
    tenKhachHang: form.value.hoTen.trim(),
    sdtKhachHang: form.value.sdt.trim(),
    emailKhachHang: form.value.email.trim(),
    diaChiGiaoHang: fullAddress,
    ghiChu: form.value.ghiChu.trim(),
    phuongThucThanhToan: form.value.phuongThucThanhToan,
    loaiDon: 2, // 2: Trực tuyến
    trangThai: 0, // 0: Chờ xác nhận
    trangThaiText: 'Chờ xác nhận',
    tongTienHang: cartStore.subTotal,
    tienGiam: cartStore.discountAmount,
    phiVanChuyen: cartStore.shippingFee,
    tongThanhToan: cartStore.finalTotal,
    voucher: cartStore.appliedVoucher ? cartStore.appliedVoucher.ma : null,
    items: [...cartStore.items],
  }

  // Lưu vào danh sách đơn hàng client (footstyle_orders)
  const existingOrders = docDonHang()
  existingOrders.unshift(newOrder)
  ghiDonHang(existingOrders)

  // Xóa giỏ hàng
  cartStore.clearCart()

  setTimeout(() => {
    submitting.value = false
    router.push({
      path: '/shop/dat-hang-thanh-cong',
      query: { ma: maDonHang },
    })
  }, 500)
}
</script>

<template>
  <div class="checkout-view py-4">
    <div class="container">
      <!-- Breadcrumb -->
      <nav aria-label="breadcrumb" class="mb-4">
        <ol class="breadcrumb mb-0 small">
          <li class="breadcrumb-item"><RouterLink to="/shop">Trang chủ</RouterLink></li>
          <li class="breadcrumb-item"><RouterLink to="/shop/gio-hang">Giỏ hàng</RouterLink></li>
          <li class="breadcrumb-item active" aria-current="page">Thanh toán đơn hàng</li>
        </ol>
      </nav>

      <div v-if="cartStore.items.length === 0" class="card border-0 shadow-sm rounded-4 p-5 text-center my-4">
        <h4 class="fw-bold mb-3">Giỏ hàng của bạn đang trống</h4>
        <p class="text-muted mb-4">Vui lòng chọn sản phẩm vào giỏ hàng trước khi thanh toán.</p>
        <div>
          <RouterLink to="/shop/san-pham" class="btn btn-primary rounded-pill px-4">
            Mua sắm ngay
          </RouterLink>
        </div>
      </div>

      <div v-else class="row g-4">
        <!-- Cột Form Điền Thông Tin -->
        <div class="col-lg-7">
          <div class="card border-0 shadow-sm rounded-4 p-4 mb-4">
            <h5 class="fw-bold mb-3 text-dark d-flex align-items-center gap-2">
              <i class="bi bi-person-lines-fill text-primary"></i> 1. Thông tin người nhận hàng
            </h5>

            <div class="row g-3">
              <div class="col-md-6">
                <label class="form-label small fw-semibold">Họ và tên người nhận <span class="text-danger">*</span></label>
                <input
                  type="text"
                  class="form-control"
                  :class="{ 'is-invalid': errors.hoTen }"
                  placeholder="Ví dụ: Nguyễn Văn A"
                  v-model="form.hoTen"
                />
                <div v-if="errors.hoTen" class="invalid-feedback">{{ errors.hoTen }}</div>
              </div>

              <div class="col-md-6">
                <label class="form-label small fw-semibold">Số điện thoại liên hệ <span class="text-danger">*</span></label>
                <input
                  type="tel"
                  class="form-control"
                  :class="{ 'is-invalid': errors.sdt }"
                  placeholder="Ví dụ: 0912345678"
                  v-model="form.sdt"
                />
                <div v-if="errors.sdt" class="invalid-feedback">{{ errors.sdt }}</div>
              </div>

              <div class="col-12">
                <label class="form-label small fw-semibold">Email nhận thông báo đơn hàng</label>
                <input
                  type="email"
                  class="form-control"
                  :class="{ 'is-invalid': errors.email }"
                  placeholder="Ví dụ: khachhang@gmail.com"
                  v-model="form.email"
                />
                <div v-if="errors.email" class="invalid-feedback">{{ errors.email }}</div>
              </div>
            </div>

            <h5 class="fw-bold mt-4 mb-3 text-dark d-flex align-items-center gap-2">
              <i class="bi bi-geo-alt-fill text-primary"></i> 2. Địa chỉ nhận hàng
            </h5>

            <div class="row g-3">
              <DiaChiHanhChinhSelect
                v-model:tinh-thanh="form.tinhThanh"
                v-model:phuong-xa="form.phuongXa"
                id-prefix="ck-dc"
                col-class="col-md-6"
              />

              <div class="col-12">
                <label class="form-label small fw-semibold">Địa chỉ cụ thể (Số nhà, tên ngõ/đường) <span class="text-danger">*</span></label>
                <input
                  type="text"
                  class="form-control"
                  :class="{ 'is-invalid': errors.diaChiCuThe }"
                  placeholder="Ví dụ: Số 20 ngõ 56 Phố Nhổn"
                  v-model="form.diaChiCuThe"
                />
                <div v-if="errors.diaChiCuThe" class="invalid-feedback">{{ errors.diaChiCuThe }}</div>
              </div>

              <div class="col-12">
                <label class="form-label small fw-semibold">Ghi chú giao hàng</label>
                <textarea
                  class="form-control"
                  rows="2"
                  placeholder="Ví dụ: Giao vào giờ hành chính, gọi trước khi đến..."
                  v-model="form.ghiChu"
                ></textarea>
              </div>
            </div>

            <h5 class="fw-bold mt-4 mb-3 text-dark d-flex align-items-center gap-2">
              <i class="bi bi-credit-card-fill text-primary"></i> 3. Hình thức thanh toán
            </h5>

            <div class="d-flex flex-column gap-2">
              <label class="border rounded-3 p-3 d-flex align-items-center gap-3 cursor-pointer" :class="{ 'border-primary bg-light': form.phuongThucThanhToan === 'COD' }">
                <input
                  type="radio"
                  class="form-check-input mt-0"
                  name="payment"
                  value="COD"
                  v-model="form.phuongThucThanhToan"
                />
                <div class="flex-grow-1">
                  <div class="fw-bold small d-flex align-items-center gap-2">
                    <i class="bi bi-cash-stack text-success fs-5"></i> Thanh toán khi nhận hàng (COD)
                  </div>
                  <div class="text-muted" style="font-size: 0.8rem;">
                    Kiểm tra hàng thoải mái trước khi thanh toán tiền mặt cho nhân viên giao hàng.
                  </div>
                </div>
              </label>

              <label class="border rounded-3 p-3 d-flex align-items-center gap-3 cursor-pointer" :class="{ 'border-primary bg-light': form.phuongThucThanhToan === 'QR' }">
                <input
                  type="radio"
                  class="form-check-input mt-0"
                  name="payment"
                  value="QR"
                  v-model="form.phuongThucThanhToan"
                />
                <div class="flex-grow-1">
                  <div class="fw-bold small d-flex align-items-center gap-2">
                    <i class="bi bi-qr-code text-primary fs-5"></i> Chuyển khoản VietQR Ngân Hàng
                  </div>
                  <div class="text-muted" style="font-size: 0.8rem;">
                    Quét mã QR tự động điền số tiền và nội dung qua ứng dụng ngân hàng bất kỳ.
                  </div>
                </div>
              </label>
            </div>
          </div>
        </div>

        <!-- Cột Tóm Tắt Đơn Hàng -->
        <div class="col-lg-5">
          <div class="card border-0 shadow-sm rounded-4 p-4 sticky-top" style="top: 20px;">
            <h5 class="fw-bold mb-3 text-dark">Đơn hàng của bạn ({{ cartStore.totalCount }})</h5>

            <!-- Danh sách món tóm tắt -->
            <div class="border rounded-3 p-2 mb-3 bg-light overflow-y-auto" style="max-height: 250px;">
              <div
                v-for="item in cartStore.items"
                :key="item.bienTheId"
                class="d-flex align-items-center gap-2 py-2 border-bottom border-light-subtle last-border-0"
              >
                <img
                  :src="item.anh || '/logo.png'"
                  :alt="item.sanPhamTen"
                  class="rounded-2 border p-1 bg-white"
                  style="width: 44px; height: 44px; object-fit: contain;"
                />
                <div class="flex-grow-1 small">
                  <div class="fw-semibold text-dark text-truncate" style="max-width: 200px;">
                    {{ item.sanPhamTen }}
                  </div>
                  <div class="text-muted" style="font-size: 0.75rem;">
                    <span v-if="item.mauSac">{{ item.mauSac }}</span>
                    <span v-if="item.trongLuong"> • {{ item.trongLuong }}</span>
                    <span> | x{{ item.soLuong }}</span>
                  </div>
                </div>
                <div class="text-end small fw-bold text-dark">
                  {{ formatMoney(item.giaBan * item.soLuong) }}
                </div>
              </div>
            </div>

            <!-- Tóm tắt chi phí -->
            <div class="d-flex justify-content-between mb-2 small">
              <span class="text-muted">Tổng tiền hàng:</span>
              <span class="fw-semibold">{{ formatMoney(cartStore.subTotal) }}</span>
            </div>

            <div class="d-flex justify-content-between mb-2 small" v-if="cartStore.discountAmount > 0">
              <span class="text-success">Giảm giá voucher:</span>
              <span class="fw-bold text-success">-{{ formatMoney(cartStore.discountAmount) }}</span>
            </div>

            <div class="d-flex justify-content-between mb-3 small">
              <span class="text-muted">Phí vận chuyển:</span>
              <span v-if="cartStore.shippingFee === 0" class="text-success fw-bold">Miễn phí</span>
              <span v-else class="fw-semibold">{{ formatMoney(cartStore.shippingFee) }}</span>
            </div>

            <hr class="my-2" />

            <div class="d-flex justify-content-between align-items-baseline mb-4">
              <span class="fw-bold">Tổng thanh toán:</span>
              <span class="fs-4 fw-bold text-danger">{{ formatMoney(cartStore.finalTotal) }}</span>
            </div>

            <button
              type="button"
              class="btn text-white btn-lg w-100 rounded-pill fw-bold shadow-sm"
              style="background-color: #ea580c; border-color: #ea580c;"
              :disabled="submitting"
              @click="handleOrderSubmit"
            >
              <span v-if="submitting" class="spinner-border spinner-border-sm me-2"></span>
              <span>Xác nhận đặt hàng</span>
            </button>

            <div class="text-center text-muted mt-3" style="font-size: 0.775rem;">
              <i class="bi bi-shield-check me-1"></i> Bằng việc đặt hàng, bạn đồng ý với Điều khoản dịch vụ và Chính sách của FootStyle.
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
