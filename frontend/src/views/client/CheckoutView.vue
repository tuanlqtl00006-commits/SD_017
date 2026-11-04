<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCartStore } from '../../stores/cart'
import { useAuth } from '../../composables/useAuth'
import { khachDiaChiService } from '../../services/khachDiaChiService'
import DiaChiHanhChinhSelect from '../../components/common/DiaChiHanhChinhSelect.vue'
import BaseModal from '../../components/common/BaseModal.vue'
import { docDonHang, ghiDonHang } from '../../utils/donHangLocal'
import VoucherPanel from '../../components/shop/VoucherPanel.vue'
import VanChuyenLogo from '../../components/common/VanChuyenLogo.vue'
import { formatMoney } from '../../utils/format'
import api from '../../services/api'

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()
const auth = useAuth()

// Khách vãng lai (chưa đăng nhập/đăng ký) vẫn được mua hàng; đăng nhập chỉ để tự điền thông tin và dùng sổ địa chỉ
function laKhachDaDangNhap() {
  return auth.trangThai.nguoiDung?.vaiTro === 'KHACH_HANG'
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

// Phí ship tính theo Tỉnh/Thành khách chọn (Hà Nội miễn phí, tỉnh khác tùy tỉnh)
watch(() => form.value.tinhThanh, (tinh) => { cartStore.tinhGiao = tinh || '' }, { immediate: true })

// Sổ địa chỉ đã lưu của khách đang đăng nhập (tự điền địa chỉ mặc định)
const savedAddresses = ref([])
const selectedAddressId = ref(null)
const luuVaoSo = ref(false)
// Chọn "Nhập địa chỉ mới" thì xóa trắng phần địa chỉ để khách nhập nhanh ngay tại đây
function onChonDiaChi() {
  if (selectedAddressId.value === null) {
    form.value.tinhThanh = ''
    form.value.quanHuyen = ''
    form.value.phuongXa = ''
    form.value.diaChiCuThe = ''
    luuVaoSo.value = true
    return
  }
  luuVaoSo.value = false
  applySavedAddress()
}

// Review 04/11: sổ địa chỉ không còn là các thẻ to; chỉ hiện gọn địa chỉ đang chọn, bấm "Thay đổi" mở pop-up (giống cửa sổ địa chỉ bên admin)
const moSoDiaChi = ref(false)
const diaChiDangChon = computed(() => savedAddresses.value.find((x) => x.id === selectedAddressId.value) ?? null)
const ghepDiaChi = (a) => [a.diaChiCuThe, a.phuongXa, a.quanHuyen, a.tinhThanhPho].filter(Boolean).join(', ')
function chonTuSoDiaChi(id) {
  selectedAddressId.value = id
  onChonDiaChi()
  moSoDiaChi.value = false
}

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
  if (!laKhachDaDangNhap()) return
  const u = auth.trangThai.nguoiDung
  form.value.hoTen = u.hoTen || ''
  form.value.sdt = u.sdt || ''
  form.value.email = u.email || ''
  try {
    savedAddresses.value = (await khachDiaChiService.getAll()) || []
    if (savedAddresses.value.length === 0) luuVaoSo.value = true
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
// Sau lần bấm "Đặt hàng" đầu tiên, nhập lại ô nào thì kiểm tra lại ngay: sửa đúng là dòng báo lỗi mất (review 04/11)
const daBamDat = ref(false)
watch([form, selectedAddressId], () => { if (daBamDat.value) validate() }, { deep: true })
const submitting = ref(false)
const submitError = ref('')

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
  // Review 04/11: đơn HD970 đặt được khi chưa chọn Tỉnh/Thành, Phường/Xã -> bắt buộc chọn đủ
  if (!form.value.tinhThanh) errs.tinhThanh = 'Vui lòng chọn Tỉnh/Thành phố.'
  if (!form.value.phuongXa) errs.phuongXa = 'Vui lòng chọn Phường/Xã.'
  // Địa chỉ đã lưu (dữ liệu cũ) thiếu tỉnh / phường: nhắc chọn địa chỉ khác vì các ô chọn tỉnh đang ẩn
  if (diaChiDangChon.value && (errs.tinhThanh || errs.phuongXa || errs.diaChiCuThe)) {
    errs.soDiaChi = 'Địa chỉ đã lưu này thiếu thông tin. Vui lòng bấm "Thay đổi" để chọn địa chỉ khác hoặc thêm địa chỉ mới.'
  }

  errors.value = errs
  return Object.keys(errs).length === 0
}

async function handleOrderSubmit() {
  if (cartStore.items.length === 0) {
    router.push('/shop/san-pham')
    return
  }

  daBamDat.value = true
  if (!validate()) {
    return
  }

  submitting.value = true
  submitError.value = ''

  // Gửi đơn lên backend: backend kiểm tra tồn kho, áp mã giảm giá, lưu hóa đơn thật và gửi mail xác nhận cho khách
  let ketQua
  try {
    ketQua = (
      await api.post('/cua-hang/dat-hang', {
        hoTenNguoiNhan: form.value.hoTen.trim(),
        sdtNguoiNhan: form.value.sdt.trim(),
        emailNguoiNhan: form.value.email.trim(),
        diaChiGiaoHang: form.value.diaChiCuThe.trim(),
        tinhThanh: form.value.tinhThanh,
        phuongXa: form.value.phuongXa,
        ghiChu: form.value.ghiChu.trim(),
        idKhachHang: laKhachDaDangNhap() ? (auth.trangThai.nguoiDung?.id ?? null) : null,
        idPhuongThucThanhToan: form.value.phuongThucThanhToan === 'QR' ? 2 : 1,
        maPhieuGiamGia: cartStore.appliedVoucher ? cartStore.appliedVoucher.ma : null,
        chiTiet: cartStore.items.map((i) => ({ idSanPhamChiTiet: i.bienTheId, soLuong: i.soLuong })),
      })
    ).data
  } catch (e) {
    submitError.value = e.message || 'Không đặt được hàng. Vui lòng thử lại.'
    submitting.value = false
    return
  }

  const maDonHang = ketQua.maHoaDon

  // Khách nhập địa chỉ mới và tick "lưu": thêm luôn vào sổ địa chỉ (lỗi lưu không ảnh hưởng đơn đã đặt)
  if (laKhachDaDangNhap() && luuVaoSo.value && selectedAddressId.value === null && form.value.tinhThanh && form.value.phuongXa) {
    try {
      await khachDiaChiService.create({
        tenNguoiNhan: form.value.hoTen.trim(),
        sdtNguoiNhan: form.value.sdt.trim(),
        tinhThanhPho: form.value.tinhThanh,
        phuongXa: form.value.phuongXa,
        diaChiCuThe: form.value.diaChiCuThe.trim(),
        laDiaChiMacDinh: savedAddresses.value.length === 0,
      })
    } catch {
      // bỏ qua
    }
  }

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
    tongTienHang: ketQua.tongTien,
    tienGiam: ketQua.soTienGiam,
    phiVanChuyen: ketQua.phiVanChuyen,
    tongThanhToan: ketQua.thanhTien,
    voucher: cartStore.appliedVoucher ? cartStore.appliedVoucher.ma : null,
    items: [...cartStore.items],
  }

  // Lưu vào danh sách đơn hàng client (footstyle_orders)
  const existingOrders = docDonHang()
  existingOrders.unshift(newOrder)
  ghiDonHang(existingOrders)

  // Báo cho tab quản trị của shop (nếu đang mở cùng trình duyệt) biết có đơn hàng mới ngay lập tức
  try {
    localStorage.setItem('footstyle_new_order_ping', JSON.stringify({ t: Date.now(), ma: maDonHang }))
  } catch {
    // bỏ qua
  }

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

            <!-- Sổ địa chỉ gọn: một dòng địa chỉ đang dùng + nút "Thay đổi" mở pop-up chọn địa chỉ (giống cửa sổ địa chỉ bên admin) -->
            <div v-if="savedAddresses.length > 0" class="dc-gon mb-3" :class="{ 'is-loi': errors.soDiaChi }" data-testid="ck-dia-chi-gon">
              <i class="bi dc-gon-icon" :class="diaChiDangChon ? 'bi-geo-alt-fill' : 'bi-plus-circle'" aria-hidden="true"></i>
              <div class="dc-gon-noi-dung">
                <template v-if="diaChiDangChon">
                  <div class="dc-gon-ten">
                    {{ diaChiDangChon.tenNguoiNhan }}
                    <span v-if="diaChiDangChon.sdtNguoiNhan" class="dc-phone">{{ diaChiDangChon.sdtNguoiNhan }}</span>
                    <span v-if="diaChiDangChon.laDiaChiMacDinh" class="dc-badge">Mặc định</span>
                  </div>
                  <div class="dc-addr">{{ ghepDiaChi(diaChiDangChon) }}</div>
                </template>
                <div v-else class="dc-gon-ten">Giao đến địa chỉ mới <span class="dc-addr fw-normal">(nhập bên dưới)</span></div>
              </div>
              <button type="button" class="dc-gon-nut" data-testid="ck-doi-dia-chi" @click="moSoDiaChi = true">
                {{ diaChiDangChon ? 'Thay đổi' : 'Chọn địa chỉ đã lưu' }}
              </button>
            </div>
            <div v-if="errors.soDiaChi" class="text-danger small mt-n2 mb-3">{{ errors.soDiaChi }}</div>

            <BaseModal v-if="moSoDiaChi" title="Chọn địa chỉ nhận hàng" size="md" @close="moSoDiaChi = false">
              <div class="dc-list" role="listbox" aria-label="Sổ địa chỉ">
                <button
                  v-for="a in savedAddresses"
                  :key="a.id"
                  type="button"
                  role="option"
                  class="dc-card"
                  :class="{ active: selectedAddressId === a.id }"
                  :aria-selected="selectedAddressId === a.id"
                  @click="chonTuSoDiaChi(a.id)"
                >
                  <span class="dc-icon"><i class="bi bi-house-door-fill"></i></span>
                  <span class="dc-body">
                    <span class="dc-name">
                      {{ a.tenNguoiNhan }}
                      <span v-if="a.sdtNguoiNhan" class="dc-phone">{{ a.sdtNguoiNhan }}</span>
                      <span v-if="a.laDiaChiMacDinh" class="dc-badge">Mặc định</span>
                    </span>
                    <span class="dc-addr">{{ ghepDiaChi(a) }}</span>
                  </span>
                  <i v-if="selectedAddressId === a.id" class="bi bi-check-circle-fill dc-check"></i>
                </button>
                <button
                  type="button"
                  role="option"
                  class="dc-card dc-new"
                  :class="{ active: selectedAddressId === null }"
                  :aria-selected="selectedAddressId === null"
                  @click="chonTuSoDiaChi(null)"
                >
                  <span class="dc-icon"><i class="bi bi-plus-lg"></i></span>
                  <span class="dc-body"><span class="dc-name">Thêm địa chỉ mới</span><span class="dc-addr">Nhập địa chỉ giao hàng khác</span></span>
                  <i v-if="selectedAddressId === null" class="bi bi-check-circle-fill dc-check"></i>
                </button>
              </div>
            </BaseModal>
            <div class="row g-3">
              <template v-if="savedAddresses.length === 0 || selectedAddressId === null">
              <DiaChiHanhChinhSelect
                v-model:tinh-thanh="form.tinhThanh"
                v-model:phuong-xa="form.phuongXa"
                id-prefix="ck-dc"
                col-class="col-md-6"
                required
                :error-tinh="errors.tinhThanh || ''"
                :error-phuong="errors.phuongXa || ''"
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
                <div class="form-check">
                  <input id="ck-luu-so" v-model="luuVaoSo" type="checkbox" class="form-check-input" />
                  <label class="form-check-label small" for="ck-luu-so">Lưu địa chỉ này vào sổ địa chỉ cho lần mua sau</label>
                </div>
              </div>
              </template>

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
          <div class="card border-0 shadow-sm rounded-4 p-4 sticky-top" style="top: 20px; z-index: 10;">
            <h5 class="co-title">Đơn hàng ({{ cartStore.totalCount }} sản phẩm)</h5>

            <!-- Danh sách sản phẩm -->
            <div class="co-items">
              <div v-for="item in cartStore.items" :key="item.bienTheId" class="co-item">
                <RouterLink :to="`/shop/san-pham/${item.sanPhamId}`" class="co-item-img">
                  <img :src="item.anh || '/logo.png'" :alt="item.sanPhamTen" />
                  <span class="co-badge">{{ item.soLuong }}</span>
                </RouterLink>
                <div class="co-item-body">
                  <div class="co-item-name">{{ item.sanPhamTen }}</div>
                  <div class="co-item-attrs">
                    <span v-if="item.mauSac">Màu: {{ item.mauSac }}</span>
                    <span v-if="item.trongLuong">Trọng lượng: {{ item.trongLuong }}</span>
                    <span v-if="item.chuVi">Cán: {{ item.chuVi }}</span>
                  </div>
                  <div v-if="item.soLuong > 1" class="co-item-unit">{{ formatMoney(item.giaBan) }} x {{ item.soLuong }}</div>
                </div>
                <div class="co-item-total">{{ formatMoney(item.giaBan * item.soLuong) }}</div>
              </div>
            </div>

            <!-- Mã giảm giá: gợi ý mã tốt nhất + nhập / chọn mã -->
            <VoucherPanel />

            <!-- Chi phí -->
            <div class="co-row">
              <span>Tạm tính</span>
              <span class="co-val">{{ formatMoney(cartStore.subTotal) }}</span>
            </div>
            <div v-if="cartStore.discountAmount > 0" class="co-row">
              <span class="co-green">Giảm giá voucher<template v-if="cartStore.appliedVoucher"> ({{ cartStore.appliedVoucher.ma }})</template></span>
              <span class="co-val co-green">-{{ formatMoney(cartStore.discountAmount) }}</span>
            </div>
            <div class="co-row">
              <span class="co-ship">Phí vận chuyển <VanChuyenLogo ten="Giao Hàng Nhanh" /></span>
              <span v-if="cartStore.chuaCoTinh" class="co-val text-muted small">Chọn Tỉnh/Thành phố để tính</span>
              <span v-else-if="cartStore.shippingFee === 0" class="co-val co-green">Miễn phí</span>
              <span v-else class="co-val">{{ formatMoney(cartStore.shippingFee) }}</span>
            </div>

            <div class="co-total">
              <span>Tổng cộng</span>
              <span class="co-total-val">{{ formatMoney(cartStore.finalTotal) }}</span>
            </div>

            <div v-if="submitError" class="alert alert-danger py-2 small mb-3">{{ submitError }}</div>

            <button type="button" class="co-submit" :disabled="submitting" @click="handleOrderSubmit">
              <span v-if="submitting" class="spinner-border spinner-border-sm me-2"></span>
              <span>ĐẶT HÀNG</span>
            </button>

            <RouterLink to="/shop/gio-hang" class="co-back">Quay lại giỏ hàng</RouterLink>

            <div class="co-note">
              <i class="bi bi-shield-check me-1"></i> Bằng việc đặt hàng, bạn đồng ý với Điều khoản dịch vụ và Chính sách của FootStyle.
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dc-gon {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border: 1px solid #dbe3ee;
  border-radius: 10px;
  background: #f8fafc;
}
.dc-gon.is-loi { border-color: #dc3545; }
.dc-gon-icon { flex: none; color: #1d4ed8; font-size: 1.05rem; }
.dc-gon-noi-dung { flex: 1; min-width: 0; line-height: 1.35; }
.dc-gon-ten { font-weight: 700; font-size: 0.88rem; color: #0f172a; display: flex; align-items: center; flex-wrap: wrap; gap: 6px; }
.dc-gon .dc-addr { font-size: 0.82rem; }
.dc-gon-nut {
  flex: none;
  padding: 3px 12px;
  border: 1px solid #1d4ed8;
  border-radius: 999px;
  background: #fff;
  color: #1d4ed8;
  font-size: 0.8rem;
  font-weight: 600;
  white-space: nowrap;
}
.dc-gon-nut:hover { background: #1d4ed8; color: #fff; }
.dc-list { display: grid; gap: 8px; }
.dc-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  text-align: left;
  padding: 10px 12px;
  border: 1.5px solid #e2e8f0;
  border-radius: 14px;
  background: #fff;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s, box-shadow 0.15s;
}
.dc-card:hover { border-color: #93c5fd; }
.dc-card.active {
  border-color: #1d4ed8;
  background: #eff6ff;
  box-shadow: 0 0 0 3px rgba(29, 78, 216, 0.1);
}
.dc-radio { position: absolute; opacity: 0; pointer-events: none; }
.dc-icon {
  flex: 0 0 40px;
  height: 40px;
  border-radius: 12px;
  background: #e0ecff;
  color: #1d4ed8;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.1rem;
}
.dc-card.active .dc-icon { background: #1d4ed8; color: #fff; }
.dc-new { border-style: dashed; }
.dc-body { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.dc-name { font-weight: 700; font-size: 0.92rem; color: #0f172a; display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }
.dc-phone { font-weight: 500; color: #64748b; font-size: 0.85rem; }
.dc-badge {
  font-size: 0.68rem;
  font-weight: 700;
  color: #1d4ed8;
  background: #dbeafe;
  padding: 2px 8px;
  border-radius: 9999px;
}
.dc-addr { font-size: 0.85rem; color: #64748b; }
.dc-check { color: #1d4ed8; font-size: 1.25rem; }

.co-title {
  margin: 0 0 1rem;
  padding-bottom: 0.8rem;
  font-size: 1rem;
  font-weight: 800;
  color: #111827;
  border-bottom: 1px solid #eef0f4;
}
.co-items {
  max-height: 300px;
  overflow-y: auto;
  margin-bottom: 1rem;
}
.co-item {
  display: flex;
  align-items: flex-start;
  gap: 0.8rem;
  padding: 0.5rem 0;
}
.co-item-img {
  position: relative;
  flex: 0 0 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border: 1px solid #e3ebf8;
  border-radius: 10px;
  padding: 3px;
}
.co-item-img img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}
.co-badge {
  position: absolute;
  top: -7px;
  right: -7px;
  min-width: 20px;
  height: 20px;
  padding: 0 5px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.68rem;
  font-weight: 700;
  color: #fff;
  background: #0d6efd;
  border-radius: 999px;
  border: 2px solid #fff;
}
.co-item-body {
  flex: 1;
  min-width: 0;
}
.co-item-name {
  font-size: 0.88rem;
  font-weight: 700;
  color: #111827;
  line-height: 1.3;
}
.co-item-attrs {
  display: flex;
  flex-wrap: wrap;
  gap: 0 0.7rem;
  margin-top: 0.15rem;
  font-size: 0.75rem;
  color: #6b7280;
}
.co-item-unit {
  margin-top: 0.15rem;
  font-size: 0.72rem;
  color: #9aa3b2;
}
.co-item-total {
  flex-shrink: 0;
  font-size: 0.88rem;
  font-weight: 700;
  color: #111827;
}
.co-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.7rem;
  font-size: 0.85rem;
  color: #6b7280;
}
.co-val {
  font-weight: 600;
  color: #111827;
}
.co-green {
  color: #16a34a;
  font-weight: 700;
}
.co-ship {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
}
.co-total {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin: 0.6rem 0 1.1rem;
  padding-top: 0.9rem;
  border-top: 1px solid #eef0f4;
  font-weight: 800;
  color: #111827;
}
.co-total-val {
  font-size: 1.5rem;
  color: #0d6efd;
}
.co-submit {
  width: 100%;
  padding: 0.85rem;
  font-size: 0.95rem;
  font-weight: 800;
  letter-spacing: 0.04em;
  color: #fff;
  background: #0d6efd;
  border: 0;
  border-radius: 10px;
  box-shadow: 0 8px 18px rgba(13, 110, 253, 0.28);
}
.co-submit:hover:not(:disabled) {
  background: #0b5ed7;
}
.co-submit:disabled {
  opacity: 0.7;
}
.co-back {
  display: block;
  margin-top: 0.9rem;
  text-align: center;
  font-size: 0.85rem;
  color: #6b7280;
  text-decoration: none;
}
.co-back:hover {
  color: #0d6efd;
}
.co-note {
  margin-top: 0.8rem;
  text-align: center;
  font-size: 0.72rem;
  color: #9aa3b2;
}
</style>