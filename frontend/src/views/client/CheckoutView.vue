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
import { dongTienDon } from '../../utils/tienDon'
import { useBaoGiaGioHang } from '../../composables/useBaoGiaGioHang'
import api from '../../services/api'
import { phieuChoGio, phieuTotNhat, taiPhieuChoKhach } from '../../services/phieuKhachService'
import { useToast } from '../../composables/useToast'

const route = useRoute()
const router = useRouter()
const toast = useToast()
const cartStore = useCartStore()
// Giá trong giỏ cập nhật theo cửa hàng, số tiền lấy theo máy chủ (cùng cách tính với lúc đặt hàng) - review 05/11
useBaoGiaGioHang(cartStore)
// Các dòng tiền cùng tên, cùng thứ tự với giỏ hàng, bán quầy, chi tiết hóa đơn (utils/tienDon.js)
const tien = computed(() =>
  dongTienDon({
    tienHangSauDot: cartStore.subTotal,
    giamTheoDot: cartStore.giamTheoDot,
    giamPhieu: cartStore.discountAmount,
    maPhieu: cartStore.appliedVoucher?.ma,
    coGiaoHang: true,
    phiVanChuyen: cartStore.chuaCoTinh ? null : cartStore.shippingFee, // chưa chọn tỉnh: chưa tính phí giao
    tongThanhToan: cartStore.finalTotal,
  }),
)
const coGiamDot = (item) => Number(item.giaGoc) > Number(item.giaBan)
const coSanPhamKhongDuHang = computed(() =>
  cartStore.items.some(
    (i) => (Number(i.soLuongTon) ?? 999) < Number(i.soLuong) || (Number(i.soLuongTon) ?? 999) <= 0
  )
)
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

// Phí ship theo nơi khách chọn (Hà Nội: phường 25.000 / xã 30.000; tỉnh khác tùy tỉnh)
watch(() => form.value.tinhThanh, (tinh) => { cartStore.tinhGiao = tinh || '' }, { immediate: true })
watch(() => form.value.phuongXa, (p) => { cartStore.phuongGiao = p || '' }, { immediate: true })

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

  if (coSanPhamKhongDuHang.value) {
    submitError.value = 'Một số sản phẩm trong đơn đã hết hoặc không đủ tồn kho (do quầy hoặc đơn khác vừa mua). Vui lòng kiểm tra lại.'
    return false
  }

  errors.value = errs
  return Object.keys(errs).length === 0
}

/* ----- Kiểm tra lại trước khi đặt (cô test case khó 14/11 + nhóm góp ý) -----
 * Không tự đổi phiếu / giá sau lưng khách ("người ta không biết mình vừa chọn cái gì"). Bấm Đặt hàng thì hỏi lại máy chủ:
 * - giá vừa đổi theo đợt giảm giá  -> báo tổng mới, khách đồng ý mới đặt;
 * - phiếu đang dùng không còn dùng được -> hỏi đặt không dùng phiếu hay quay lại chọn phiếu khác;
 * - có phiếu giảm nhiều hơn (kể cả khi chưa dùng phiếu) -> hỏi có muốn áp dụng không; chọn không thì đặt như cũ. */
const hoiTruocKhiDat = ref(null) // { dong: [chuỗi], nut: [{ nhan, chinh, lam }] }

async function kiemTraTruocKhiDat() {
  const bg = await cartStore.taiBaoGia()
  if (coSanPhamKhongDuHang.value) {
    submitError.value = 'Một số sản phẩm trong đơn đã hết hoặc không đủ tồn kho (do quầy hoặc đơn khác vừa mua). Vui lòng kiểm tra lại.'
    return null
  }
  const dong = []
  if (bg?.doiGia) {
    dong.push(`Giá một số sản phẩm vừa thay đổi theo đợt giảm giá hiện tại. Tổng thanh toán mới: ${formatMoney(cartStore.finalTotal)}.`)
  }
  const dangDung = cartStore.appliedVoucher
  if (dangDung && cartStore.loiPhieu) {
    dong.push(`Phiếu ${dangDung.ma} không còn dùng được: ${cartStore.loiPhieu}`)
    return {
      dong,
      nut: [
        { nhan: 'Đặt hàng không dùng phiếu', chinh: true, lam: async () => { cartStore.removeVoucher(); await cartStore.taiBaoGia(); await guiDon() } },
        { nhan: 'Quay lại chọn phiếu khác', lam: null },
      ],
    }
  }
  const tot = phieuTotNhat(await taiPhieuChoKhach(Math.round(cartStore.subTotal)))
  const dangGiam = dangDung ? Number(cartStore.discountAmount) || 0 : 0
  if (tot && tot.ma !== dangDung?.ma && Number(tot.soTienGiam) > dangGiam) {
    dong.push(
      dangDung
        ? `Có phiếu ${tot.ma} giảm ${formatMoney(tot.soTienGiam)}, nhiều hơn phiếu ${dangDung.ma} đang dùng (${formatMoney(dangGiam)}). Bạn có muốn áp dụng không?`
        : `Có phiếu ${tot.ma} giảm ${formatMoney(tot.soTienGiam)} cho đơn này. Bạn có muốn áp dụng không?`,
    )
    return {
      dong,
      nut: [
        { nhan: `Áp dụng ${tot.ma} và đặt hàng`, chinh: true, lam: async () => { cartStore.setVoucher(phieuChoGio(tot)); await cartStore.taiBaoGia(); await guiDon() } },
        { nhan: dangDung ? 'Không, giữ phiếu đang dùng' : 'Không, đặt hàng không dùng phiếu', lam: () => guiDon() },
      ],
    }
  }
  if (dong.length) {
    return { dong, nut: [{ nhan: 'Đặt hàng với giá mới', chinh: true, lam: () => guiDon() }, { nhan: 'Quay lại xem', lam: null }] }
  }
  return 'ok'
}

async function chonTrongHoi(n) {
  hoiTruocKhiDat.value = null
  if (!n.lam) return
  submitting.value = true
  submitError.value = ''
  await n.lam()
}

const moXacNhanDatHang = ref(false)
const tongSoLuongSanPham = computed(() => cartStore.items.reduce((s, i) => s + (Number(i.soLuong) || 0), 0))
const fullDiaChiHienThi = computed(() =>
  [form.value.diaChiCuThe, form.value.phuongXa, form.value.quanHuyen, form.value.tinhThanh]
    .filter((x) => x && String(x).trim())
    .join(', ')
)

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
  const kq = await kiemTraTruocKhiDat()
  submitting.value = false
  if (kq !== 'ok') {
    if (kq) hoiTruocKhiDat.value = kq
    return
  }
  moXacNhanDatHang.value = true
}

async function xacNhanDatHangVaGui() {
  moXacNhanDatHang.value = false
  await guiDon()
}

async function guiDon() {
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

  toast.success(`Đặt hàng thành công! Mã đơn hàng: ${maDonHang}`, 'Đặt hàng thành công')

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
  <div class="checkout-view tt-trang">
    <div class="vnb-container">
      <!-- Breadcrumb -->
      <nav aria-label="breadcrumb" class="tt-duong-dan">
        <ol class="breadcrumb mb-0 small">
          <li class="breadcrumb-item"><RouterLink to="/shop">Trang chủ</RouterLink></li>
          <li class="breadcrumb-item"><RouterLink to="/shop/gio-hang">Giỏ hàng</RouterLink></li>
          <li class="breadcrumb-item active" aria-current="page">Thanh toán đơn hàng</li>
        </ol>
      </nav>

      <div class="tt-dau">
        <h1 class="tt-tieu-de">Thanh toán</h1>
        <ol class="tt-buoc" aria-label="Các bước đặt hàng">
          <li class="is-xong"><span><i class="bi bi-check-lg"></i></span> Giỏ hàng</li>
          <li class="is-dang"><span>2</span> Thông tin đặt hàng</li>
          <li><span>3</span> Hoàn tất</li>
        </ol>
      </div>

      <div v-if="cartStore.items.length === 0" class="tt-the tt-trong">
        <h4 class="fw-bold mb-3">Giỏ hàng của bạn đang trống</h4>
        <p class="text-muted mb-4">Vui lòng chọn sản phẩm vào giỏ hàng trước khi thanh toán.</p>
        <div>
          <RouterLink to="/shop/san-pham" class="btn btn-primary rounded-pill px-4">
            Mua sắm ngay
          </RouterLink>
        </div>
      </div>

      <div v-else class="tt-luoi">
        <!-- Cột Form Điền Thông Tin -->
        <div class="tt-cot-form">
          <div class="tt-the">
            <h5 class="tt-muc"><span>1</span> Thông tin người nhận hàng</h5>

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

          </div>

          <div class="tt-the">
            <h5 class="tt-muc"><span>2</span> Địa chỉ nhận hàng</h5>

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

          </div>

          <div class="tt-the">
            <h5 class="tt-muc"><span>3</span> Hình thức thanh toán</h5>

            <div class="tt-pttt-ds">
              <label class="tt-pttt" :class="{ 'is-chon': form.phuongThucThanhToan === 'COD' }">
                <input
                  type="radio"
                  class="form-check-input mt-0"
                  name="payment"
                  value="COD"
                  v-model="form.phuongThucThanhToan"
                />
                <div class="flex-grow-1">
                  <div class="tt-pttt-ten">Thanh toán khi nhận hàng (COD)</div>
                  <div class="tt-pttt-mo-ta">
                    Kiểm tra hàng thoải mái trước khi thanh toán tiền mặt cho nhân viên giao hàng.
                  </div>
                </div>
                <span class="tt-pttt-icon is-cod"><i class="bi bi-cash-stack"></i></span>
              </label>

              <label class="tt-pttt" :class="{ 'is-chon': form.phuongThucThanhToan === 'QR' }">
                <input
                  type="radio"
                  class="form-check-input mt-0"
                  name="payment"
                  value="QR"
                  v-model="form.phuongThucThanhToan"
                />
                <div class="flex-grow-1">
                  <div class="tt-pttt-ten">Chuyển khoản VietQR Ngân Hàng</div>
                  <div class="tt-pttt-mo-ta">
                    Quét mã QR tự động điền số tiền và nội dung qua ứng dụng ngân hàng bất kỳ.
                  </div>
                </div>
                <span class="tt-pttt-icon"><i class="bi bi-qr-code"></i></span>
              </label>
            </div>
          </div>
        </div>

        <!-- Cột Tóm Tắt Đơn Hàng -->
        <div class="tt-cot-tom-tat">
          <div class="tt-the tt-tom-tat">
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
                  <div class="co-item-unit" data-testid="co-don-gia">
                    <s v-if="coGiamDot(item)" class="me-1">{{ formatMoney(item.giaGoc) }}</s>{{ formatMoney(item.giaBan) }} x {{ item.soLuong }}
                  </div>
                  <div v-if="(item.soLuongTon ?? 1) <= 0" class="text-danger small mt-1 fw-semibold">
                    <i class="bi bi-x-circle me-1"></i>Hết hàng (quầy vừa bán hết)
                  </div>
                  <div v-else-if="item.soLuong > item.soLuongTon" class="text-danger small mt-1 fw-semibold">
                    <i class="bi bi-exclamation-triangle me-1"></i>Kho chỉ còn {{ item.soLuongTon }} sản phẩm
                  </div>
                </div>
                <div class="co-item-total">{{ formatMoney(item.giaBan * item.soLuong) }}</div>
              </div>
            </div>

            <!-- Mã giảm giá: gợi ý mã tốt nhất + nhập / chọn mã -->
            <VoucherPanel />

            <!-- Chi phí: cùng các dòng với giỏ hàng / bán quầy / chi tiết hóa đơn -->
            <p v-if="cartStore.baoGia?.doiGia" class="co-doi-gia" data-testid="co-doi-gia">
              <i class="bi bi-arrow-repeat"></i> Giá một số sản phẩm vừa được cập nhật theo cửa hàng.
            </p>
            <div v-for="d in tien.dong.slice(0, -1)" :key="d.khoa" class="co-row" :data-dong="d.khoa">
              <span :class="{ 'co-green': d.dau === '-' && d.so > 0, 'co-ship': d.khoa === 'phi-van-chuyen' }">
                {{ d.nhan }}<template v-if="d.ma"> ({{ d.ma }})</template>
                <VanChuyenLogo v-if="d.khoa === 'phi-van-chuyen'" ten="Giao Hàng Nhanh" />
              </span>
              <span v-if="d.khoa === 'phi-van-chuyen' && d.so === null" class="co-val text-muted small">Chọn Tỉnh/Thành phố để tính</span>
              <span v-else-if="d.khoa === 'phi-van-chuyen' && d.so === 0" class="co-val co-green">Miễn phí</span>
              <span v-else-if="d.khoa === 'phieu' && !d.so" class="co-val text-muted">Không dùng phiếu</span>
              <span v-else class="co-val" :class="{ 'co-green': d.dau === '-' }">{{ d.dau === '-' ? '-' : '' }}{{ formatMoney(d.so) }}</span>
            </div>
            <p v-if="cartStore.conThieuMienPhiShip > 0" class="text-muted small mb-2" data-testid="co-con-thieu-free-ship">
              <i class="bi bi-truck me-1"></i> Mua thêm <strong>{{ formatMoney(cartStore.conThieuMienPhiShip) }}</strong> để được miễn phí vận chuyển.
            </p>
            <p v-if="cartStore.loiPhieu" class="text-danger small mb-2" data-testid="co-loi-phieu">{{ cartStore.loiPhieu }}</p>

            <div class="co-total" data-dong="tong-thanh-toan">
              <span>{{ tien.dong.at(-1).nhan }}</span>
              <span class="co-total-val">{{ formatMoney(tien.tong) }}</span>
            </div>

            <div v-if="coSanPhamKhongDuHang" class="alert alert-danger py-2 small mb-3 border-0">
              <i class="bi bi-exclamation-triangle-fill me-1"></i>
              Có sản phẩm vượt quá tồn kho hoặc đã hết hàng (do quầy hoặc đơn khác vừa mua). Vui lòng quay lại
              <RouterLink to="/shop/gio-hang" class="fw-bold text-danger text-decoration-underline">giỏ hàng</RouterLink> để điều chỉnh số lượng.
            </div>

            <div v-if="submitError" class="alert alert-danger py-2 small mb-3">{{ submitError }}</div>

            <!-- Hỏi lại trước khi đặt: giá / phiếu vừa thay đổi -->
            <BaseModal v-if="hoiTruocKhiDat" title="Kiểm tra lại trước khi đặt hàng" size="md" @close="hoiTruocKhiDat = null">
              <div data-testid="hoi-truoc-khi-dat">
                <p v-for="(d, i) in hoiTruocKhiDat.dong" :key="i" class="tt-hoi-dong">
                  <i class="bi bi-info-circle-fill"></i> <span>{{ d }}</span>
                </p>
                <div class="tt-hoi-nut">
                  <button
                    v-for="n in hoiTruocKhiDat.nut"
                    :key="n.nhan"
                    type="button"
                    class="btn"
                    :class="n.chinh ? 'btn-primary' : 'btn-outline-secondary'"
                    @click="chonTrongHoi(n)"
                  >
                    {{ n.nhan }}
                  </button>
                </div>
              </div>
            </BaseModal>

            <!-- Hộp thoại xác nhận đặt hàng -->
            <BaseModal v-if="moXacNhanDatHang" title="Xác nhận đặt hàng" size="md" @close="moXacNhanDatHang = false">
              <div data-testid="xac-nhan-dat-hang" class="co-xn-modal">
                <div class="d-flex align-items-center gap-3 p-3 mb-3 bg-light rounded-3 border">
                  <div class="rounded-circle bg-primary-subtle text-primary p-2 d-flex align-items-center justify-content-center flex-shrink-0" style="width: 48px; height: 48px;">
                    <i class="bi bi-cart-check-fill fs-4"></i>
                  </div>
                  <div>
                    <h6 class="fw-bold mb-1">Xác nhận thông tin đặt hàng</h6>
                    <p class="text-muted small mb-0">Quý khách vui lòng kiểm tra lại thông tin đơn hàng trước khi hoàn tất đặt hàng.</p>
                  </div>
                </div>

                <div class="co-xn-info small mb-3">
                  <div class="d-flex justify-content-between py-2 border-bottom">
                    <span class="text-muted">Người nhận:</span>
                    <strong class="text-end">{{ form.hoTen }} · {{ form.sdt }}</strong>
                  </div>
                  <div class="d-flex justify-content-between py-2 border-bottom">
                    <span class="text-muted">Địa chỉ nhận hàng:</span>
                    <span class="text-end fw-semibold text-break ms-3" style="max-width: 65%;">
                      {{ fullDiaChiHienThi }}
                    </span>
                  </div>
                  <div class="d-flex justify-content-between py-2 border-bottom">
                    <span class="text-muted">Hình thức thanh toán:</span>
                    <span class="badge bg-secondary-subtle text-dark fw-semibold">
                      {{ form.phuongThucThanhToan === 'QR' ? 'Chuyển khoản VietQR' : 'Thanh toán khi nhận hàng (COD)' }}
                    </span>
                  </div>
                  <div class="d-flex justify-content-between py-2 border-bottom">
                    <span class="text-muted">Số lượng sản phẩm:</span>
                    <strong>{{ tongSoLuongSanPham }} sản phẩm</strong>
                  </div>
                  <div class="d-flex justify-content-between py-2 align-items-center pt-3">
                    <span class="fw-bold fs-6">Tổng thanh toán:</span>
                    <strong class="fs-5 text-danger">{{ formatMoney(cartStore.finalTotal) }}</strong>
                  </div>
                </div>

                <p class="text-muted small text-center mb-0">
                  <i class="bi bi-question-circle me-1"></i>
                  Bạn có chắc chắn muốn tiến hành đặt đơn hàng này?
                </p>
              </div>

              <template #footer>
                <button type="button" class="btn btn-outline-secondary rounded-pill px-4" :disabled="submitting" @click="moXacNhanDatHang = false">
                  Hủy
                </button>
                <button
                  type="button"
                  class="btn btn-primary rounded-pill px-4 fw-bold"
                  data-testid="xn-dat-hang-dong-y"
                  :disabled="submitting"
                  @click="xacNhanDatHangVaGui"
                >
                  <span v-if="submitting" class="spinner-border spinner-border-sm me-1"></span>
                  Xác nhận đặt hàng
                </button>
              </template>
            </BaseModal>

            <button type="button" class="co-submit" :disabled="submitting || coSanPhamKhongDuHang" @click="handleOrderSubmit">
              <span v-if="submitting" class="spinner-border spinner-border-sm me-2"></span>
              <span>Đặt hàng</span>
              <i v-if="!submitting" class="bi bi-arrow-right"></i>
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
.co-xn-modal {
  padding: 0.25rem 0;
}
.co-xn-info {
  background: #f8fafc;
  border-radius: 12px;
  padding: 0.75rem 1rem;
  border: 1px solid #e2e8f0;
}
.co-doi-gia {
  margin: 0 0 0.6rem;
  padding: 0.45rem 0.7rem;
  border-radius: 8px;
  background: #fff7ed;
  color: #9a3412;
  font-size: 0.82rem;
}
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
  background: var(--vnb-orange);
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
  color: var(--vnb-orange);
}
.co-submit {
  width: 100%;
  padding: 0.85rem;
  font-size: 0.95rem;
  font-weight: 800;
  letter-spacing: 0.04em;
  color: #fff;
  background: var(--vnb-orange);
  border: 0;
  border-radius: 10px;
  box-shadow: 0 8px 18px rgba(13, 110, 253, 0.28);
}
.co-submit:hover:not(:disabled) {
  background: var(--vnb-orange-hover);
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
  color: var(--vnb-orange);
}
.co-note {
  margin-top: 0.8rem;
  text-align: center;
  font-size: 0.72rem;
  color: #9aa3b2;
}
.tt-hoi-dong {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
  padding: 12px 14px;
  font-size: 0.92rem;
  color: #1e3a8a;
  background: #eff6ff;
  border-radius: 12px;
}
.tt-hoi-nut {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 6px;
}
.tt-hoi-nut .btn {
  border-radius: 999px;
  font-weight: 600;
}
/* ===== Giao diện mới 15/11 ===== */
.tt-trang {
  min-height: calc(100vh - 260px);
  padding: 18px 0 72px;
  background: var(--ft-bg, #f6f7f9);
}
.tt-duong-dan {
  margin-bottom: 10px;
}
.tt-dau {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px 24px;
  margin-bottom: 22px;
}
.tt-tieu-de {
  margin: 0;
  font-size: 2rem;
  font-weight: 800;
  letter-spacing: -0.02em;
  color: var(--ft-ink, #0f172a);
}
.tt-buoc {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
  font-size: 0.88rem;
  font-weight: 600;
  color: #94a3b8;
}
.tt-buoc li {
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}
.tt-buoc li + li::before {
  content: '';
  width: 32px;
  height: 2px;
  margin-right: 2px;
  background: #e2e8f0;
  border-radius: 2px;
}
.tt-buoc li > span {
  display: grid;
  place-items: center;
  width: 26px;
  height: 26px;
  font-size: 0.78rem;
  color: #94a3b8;
  background: #fff;
  border: 1.5px solid #e2e8f0;
  border-radius: 50%;
}
.tt-buoc li.is-xong,
.tt-buoc li.is-dang {
  color: var(--ft-ink, #0f172a);
}
.tt-buoc li.is-xong > span {
  color: #fff;
  background: #16a34a;
  border-color: #16a34a;
}
.tt-buoc li.is-dang > span {
  color: #fff;
  background: var(--ft-primary, #1d4ed8);
  border-color: var(--ft-primary, #1d4ed8);
}
.tt-the {
  margin-bottom: 18px;
  padding: 24px 26px;
  background: #fff;
  border: 1px solid var(--ft-line-2, #eef0f4);
  border-radius: 24px;
}
.tt-trong {
  padding: 56px 20px;
  text-align: center;
}
.tt-luoi {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 420px;
  gap: 24px;
  align-items: start;
}
.tt-muc {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0 0 18px;
  font-size: 1.1rem;
  font-weight: 800;
  color: var(--ft-ink, #0f172a);
}
.tt-muc > span {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  font-size: 0.85rem;
  color: #fff;
  background: var(--ft-ink, #0f172a);
  border-radius: 50%;
}
.tt-the :deep(.form-label) {
  margin-bottom: 6px;
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--ft-ink-2, #334155);
}
.tt-the :deep(.form-control),
.tt-the :deep(.form-select) {
  min-height: 46px;
  padding-left: 14px;
  font-size: 0.95rem;
  background-color: #f8fafc;
  border-color: #e2e8f0;
  border-radius: 12px;
}
.tt-the :deep(textarea.form-control) {
  min-height: 0;
}
.tt-the :deep(.form-control:focus),
.tt-the :deep(.form-select:focus) {
  background-color: #fff;
  border-color: var(--ft-primary, #1d4ed8);
  box-shadow: 0 0 0 4px rgba(29, 78, 216, 0.12);
}
.tt-pttt-ds {
  display: grid;
  gap: 10px;
}
.tt-pttt {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  cursor: pointer;
  background: #fff;
  border: 1.5px solid #e2e8f0;
  border-radius: 16px;
  transition: border-color 0.2s ease, background-color 0.2s ease, box-shadow 0.2s ease;
}
.tt-pttt:hover {
  border-color: #cbd5e1;
}
.tt-pttt.is-chon {
  background: #f8fbff;
  border-color: var(--ft-primary, #1d4ed8);
  box-shadow: 0 0 0 4px rgba(29, 78, 216, 0.08);
}
.tt-pttt .form-check-input {
  flex-shrink: 0;
  width: 1.2em;
  height: 1.2em;
}
.tt-pttt-ten {
  font-weight: 700;
  color: var(--ft-ink, #0f172a);
}
.tt-pttt-mo-ta {
  margin-top: 2px;
  font-size: 0.82rem;
  color: #64748b;
}
.tt-pttt-icon {
  display: grid;
  flex-shrink: 0;
  place-items: center;
  width: 44px;
  height: 44px;
  font-size: 1.3rem;
  color: var(--ft-primary, #1d4ed8);
  background: #eff6ff;
  border-radius: 12px;
}
.tt-pttt-icon.is-cod {
  color: #15803d;
  background: #f0fdf4;
}
.tt-tom-tat {
  position: sticky;
  top: 120px;
  margin-bottom: 0;
}
.co-title {
  font-size: 1.15rem;
}
.co-item-img {
  flex-basis: 64px;
  height: 64px;
  padding: 6px;
  background: #f3f4f6;
  border: 0;
  border-radius: 14px;
}
.co-item-img img {
  mix-blend-mode: multiply;
}
.co-badge {
  background: var(--ft-ink, #0f172a);
}
.co-row {
  gap: 12px;
  font-size: 0.9rem;
  color: #475569;
}
.co-row > span:first-child {
  flex-shrink: 0;
}
.co-row > .co-val {
  text-align: right;
}
.co-ship :deep(.vc) {
  flex-wrap: nowrap;
}
.co-ship :deep(.vc-ten) {
  display: none;
}
.co-total {
  border-top: 1px dashed #cbd5e1;
}
.co-total-val {
  font-size: 1.6rem;
  letter-spacing: -0.02em;
  color: #e11d48;
}
.co-submit {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 56px;
  font-size: 1.05rem;
  letter-spacing: 0;
  background: var(--ft-primary, #1d4ed8);
  border-radius: 999px;
  box-shadow: 0 10px 24px rgba(29, 78, 216, 0.25);
}
.co-submit:hover:not(:disabled) {
  background: #1e40af;
}
@media (max-width: 1199px) {
  .tt-luoi {
    grid-template-columns: minmax(0, 1fr);
  }
  .tt-tom-tat {
    position: static;
  }
}
@media (max-width: 767px) {
  .tt-tieu-de {
    font-size: 1.6rem;
  }
  .tt-buoc {
    font-size: 0.78rem;
  }
  .tt-buoc li + li::before {
    width: 14px;
  }
  /* điện thoại: chỉ ghi chữ ở bước đang làm, các bước khác còn vòng số */
  .tt-buoc li:not(.is-dang) {
    gap: 0;
    font-size: 0;
  }
  .tt-buoc li:not(.is-dang) + li::before,
  .tt-buoc li + li:not(.is-dang)::before {
    margin-right: 8px;
  }
  .tt-the {
    padding: 18px 16px;
    border-radius: 20px;
  }
}
</style>
