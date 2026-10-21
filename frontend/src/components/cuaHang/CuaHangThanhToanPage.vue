<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useCart } from '../../composables/useCart'
import { useAuth } from '../../composables/useAuth'
import { useToast } from '../../composables/useToast'

const router = useRouter()
const cart = useCart()
const auth = useAuth()
const toast = useToast()

const phieuList = ref([])
const maVoucherNhap = ref('')
const voucherDaChon = ref(null)
const dangDatHang = ref(false)
const donHangThanhCong = ref(null)

const form = ref({
  hoTen: auth.trangThai.nguoiDung?.hoTen || '',
  sdt: auth.trangThai.nguoiDung?.sdt || '',
  email: auth.trangThai.nguoiDung?.email || '',
  diaChi: '',
  tinhThanh: 'Hà Nội',
  phuongXa: '',
  ghiChu: '',
  phuongThucThanhToan: 1, // 1: COD, 2: VietQR
})

const loi = ref({})

async function taiPhieuGiamGia() {
  try {
    const res = await fetch(`/api/cua-hang/phieu-giam-gia?tongTien=${cart.tongTien.value}`)
    if (res.ok) {
      phieuList.value = await res.json()
    }
  } catch (e) {
    // ignore
  }
}

onMounted(() => {
  taiPhieuGiamGia()
})

const tienGiamVoucher = computed(() => {
  if (!voucherDaChon.value) return 0
  const v = voucherDaChon.value
  const tong = cart.tongTien.value
  if (tong < (v.donToiThieu || 0)) return 0

  if (v.hinhThucGiam === 1) {
    // %
    const giam = Math.round((tong * (v.giaTriGiam || 0)) / 100)
    if (v.giamToiDa && giam > v.giamToiDa) return v.giamToiDa
    return giam
  } else {
    // Tiền mặt
    return v.giaTriGiam || 0
  }
})

const phiVanChuyen = computed(() => {
  if (cart.tongTien.value <= 0) return 0
  return cart.tongTien.value >= 1000000 ? 0 : 30000
})

const thanhTienCuoi = computed(() => {
  const tong = cart.tongTien.value - tienGiamVoucher.value + phiVanChuyen.value
  return Math.max(0, tong)
})

function apDungVoucher(v) {
  if (cart.tongTien.value < (v.donToiThieu || 0)) {
    toast.error(`Đơn hàng tối thiểu ${formatTien(v.donToiThieu)} để dùng mã này.`)
    return
  }
  voucherDaChon.value = v
  maVoucherNhap.value = v.maPhieu
  toast.success(`Đã áp dụng mã giảm giá ${v.maPhieu}!`)
}

function apDungMaNhap() {
  const code = maVoucherNhap.value.trim().toUpperCase()
  if (!code) return
  const tim = phieuList.value.find((p) => p.maPhieu?.toUpperCase() === code)
  if (tim) {
    apDungVoucher(tim)
  } else {
    toast.error('Mã giảm giá không hợp lệ hoặc đã hết lượt dùng.')
  }
}

function goVoucher() {
  voucherDaChon.value = null
  maVoucherNhap.value = ''
}

function kiemTraForm() {
  loi.value = {}
  if (!form.value.hoTen.trim()) loi.value.hoTen = 'Vui lòng nhập họ và tên người nhận hàng.'
  const sdt = form.value.sdt.trim()
  if (!sdt) loi.value.sdt = 'Vui lòng nhập số điện thoại nhận hàng.'
  else if (!/^0\d{9}$/.test(sdt)) loi.value.sdt = 'Số điện thoại cần 10 chữ số, bắt đầu bằng số 0.'
  if (!form.value.diaChi.trim()) loi.value.diaChi = 'Vui lòng nhập địa chỉ cụ thể nhận hàng.'
  return Object.keys(loi.value).length === 0
}

async function datHang() {
  if (cart.items.value.length === 0) {
    toast.error('Giỏ hàng rỗng. Vui lòng chọn sản phẩm trước.')
    return
  }
  if (!kiemTraForm()) return

  dangDatHang.value = true
  try {
    const payload = {
      hoTenNguoiNhan: form.value.hoTen.trim(),
      sdtNguoiNhan: form.value.sdt.trim(),
      emailNguoiNhan: form.value.email.trim() || null,
      diaChiGiaoHang: form.value.diaChi.trim(),
      tinhThanh: form.value.tinhThanh.trim(),
      phuongXa: form.value.phuongXa.trim(),
      ghiChu: form.value.ghiChu.trim() || null,
      idKhachHang: auth.trangThai.nguoiDung?.idKhachHang || null,
      idPhuongThucThanhToan: form.value.phuongThucThanhToan,
      maPhieuGiamGia: voucherDaChon.value ? voucherDaChon.value.maPhieu : null,
      chiTiet: cart.items.value.map((i) => ({
        idSanPhamChiTiet: i.id,
        soLuong: i.soLuong,
      })),
    }

    const res = await fetch('/api/cua-hang/dat-hang', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    })

    if (!res.ok) {
      const err = await res.json().catch(() => ({}))
      throw new Error(err.message || 'Không thể đặt hàng, vui lòng thử lại.')
    }

    const data = await res.json()
    donHangThanhCong.value = data
    cart.lamTrong()
    toast.success('Đặt hàng thành công! Đơn hàng của bạn đang được xử lý.')
  } catch (e) {
    toast.error(e.message || 'Đặt hàng thất bại.')
  } finally {
    dangDatHang.value = false
  }
}

function formatTien(v) {
  if (!v) return '0 ₫'
  return Number(v).toLocaleString('vi-VN') + ' ₫'
}
</script>

<template>
  <div class="container py-3">
    <!-- Breadcrumb -->
    <nav class="mb-4 small">
      <RouterLink to="/shop" class="text-decoration-none text-muted">Trang chủ</RouterLink>
      <span class="mx-2 text-muted">/</span>
      <span class="text-dark fw-bold">Giỏ hàng & Thanh toán</span>
    </nav>

    <!-- Màn hình ĐẶT HÀNG THÀNH CÔNG -->
    <div v-if="donHangThanhCong" class="card border-0 shadow-sm rounded-4 p-4 p-md-5 text-center bg-white">
      <div class="mb-3 text-success">
        <i class="bi bi-check-circle-fill display-3"></i>
      </div>
      <h2 class="fw-black text-dark mb-2">ĐẶT HÀNG THÀNH CÔNG!</h2>
      <p class="text-muted lead fs-6 mb-4">
        Cảm ơn bạn đã tin tưởng mua sắm tại <strong>FootStyle Badminton</strong>. Mã đơn hàng của bạn là:
      </p>

      <div class="d-inline-block bg-light border border-primary-subtle rounded-4 px-4 py-3 mb-4 mx-auto">
        <div class="small text-muted text-uppercase fw-bold">Mã Hóa Đơn Trực Tuyến</div>
        <div class="display-6 fw-black text-primary">{{ donHangThanhCong.maHoaDon }}</div>
        <small class="text-muted">Tổng thanh toán: <strong class="text-dark">{{ formatTien(donHangThanhCong.thanhTien) }}</strong></small>
      </div>

      <!-- Khối chuyển khoản VietQR nếu chọn Chuyển khoản -->
      <div v-if="donHangThanhCong.vietQrUrl" class="card border-primary-subtle bg-light p-4 rounded-4 max-w-500 mx-auto mb-4 text-start">
        <div class="d-flex align-items-center gap-2 mb-3">
          <i class="bi bi-qr-code-scan text-primary fs-4"></i>
          <h5 class="fw-bold mb-0 text-dark">Thanh Toán Chuyển Khoản Qua VietQR</h5>
        </div>
        <p class="small text-muted mb-3">
          Mở ứng dụng ngân hàng hoặc ví điện tử bất kỳ, quét mã QR dưới đây để thanh toán tức thì (hệ thống tự điền số tiền và nội dung):
        </p>

        <div class="text-center mb-3">
          <img :src="donHangThanhCong.vietQrUrl" alt="Mã VietQR Thanh Toán" class="img-fluid rounded-3 border bg-white p-2 shadow-sm" style="max-height: 280px;" />
        </div>

        <div class="bg-white p-3 rounded-3 border small">
          <div class="d-flex justify-content-between py-1 border-bottom">
            <span class="text-muted">Ngân hàng:</span>
            <strong>MB Bank (Quân Đội)</strong>
          </div>
          <div class="d-flex justify-content-between py-1 border-bottom">
            <span class="text-muted">Số tài khoản:</span>
            <strong class="text-primary fs-6">0348888899</strong>
          </div>
          <div class="d-flex justify-content-between py-1 border-bottom">
            <span class="text-muted">Chủ tài khoản:</span>
            <strong>SHOP CAU LONG FOOTSTYLE</strong>
          </div>
          <div class="d-flex justify-content-between py-1 border-bottom">
            <span class="text-muted">Số tiền:</span>
            <strong class="text-danger fs-6">{{ formatTien(donHangThanhCong.thanhTien) }}</strong>
          </div>
          <div class="d-flex justify-content-between py-1">
            <span class="text-muted">Nội dung chuyển:</span>
            <strong class="text-dark">{{ donHangThanhCong.maHoaDon }} FOOTSTYLE</strong>
          </div>
        </div>
      </div>

      <div class="d-flex justify-content-center gap-3 mt-2">
        <RouterLink to="/shop" class="btn btn-primary text-white fw-bold px-4 py-2 rounded-pill">
          <i class="bi bi-arrow-left me-1"></i> Tiếp Tục Mua Sắm
        </RouterLink>
      </div>
    </div>

    <!-- Màn hình GIỎ HÀNG RỖNG -->
    <div v-else-if="cart.items.value.length === 0" class="card border-0 shadow-sm rounded-4 p-5 text-center bg-white">
      <div class="mb-3 text-muted">
        <i class="bi bi-cart-x display-3"></i>
      </div>
      <h4 class="fw-bold text-dark">Giỏ hàng của bạn đang trống</h4>
      <p class="text-muted">Hãy khám phá bộ sưu tập vợt cầu lông Yonex, Lining và phụ kiện chính hãng tại cửa hàng.</p>
      <div>
        <RouterLink to="/shop" class="btn btn-primary text-white fw-bold px-4 py-2 rounded-pill shadow-sm">
          🏸 Mua Vợt & Phụ Kiện Ngay
        </RouterLink>
      </div>
    </div>

    <!-- Màn hình GIỎ HÀNG & FORM THANH TOÁN -->
    <div v-else class="row g-4">
      <!-- Cột Trái: Form Thông Tin Giao Hàng & Thanh Toán -->
      <div class="col-lg-7">
        <div class="card border-0 shadow-sm rounded-4 p-4 bg-white mb-4">
          <h5 class="fw-bold text-dark mb-3 d-flex align-items-center gap-2">
            <i class="bi bi-geo-alt-fill text-primary"></i> Thông Tin Người Nhận Hàng
          </h5>

          <div class="row g-3">
            <div class="col-md-6">
              <label class="form-label small fw-semibold">Họ và tên người nhận <span class="text-danger">*</span></label>
              <input v-model="form.hoTen" type="text" class="form-control" :class="{ 'is-invalid': loi.hoTen }" placeholder="Nguyễn Văn A" />
              <div v-if="loi.hoTen" class="invalid-feedback">{{ loi.hoTen }}</div>
            </div>

            <div class="col-md-6">
              <label class="form-label small fw-semibold">Số điện thoại liên hệ <span class="text-danger">*</span></label>
              <input v-model="form.sdt" type="text" class="form-control" :class="{ 'is-invalid': loi.sdt }" placeholder="0987654321" />
              <div v-if="loi.sdt" class="invalid-feedback">{{ loi.sdt }}</div>
            </div>

            <div class="col-12">
              <label class="form-label small fw-semibold">Email nhận hóa đơn (tùy chọn)</label>
              <input v-model="form.email" type="email" class="form-control" placeholder="vidu@gmail.com" />
            </div>

            <div class="col-md-6">
              <label class="form-label small fw-semibold">Tỉnh / Thành phố <span class="text-danger">*</span></label>
              <input v-model="form.tinhThanh" type="text" class="form-control" placeholder="Hà Nội, TP.HCM..." />
            </div>

            <div class="col-md-6">
              <label class="form-label small fw-semibold">Phường / Xã</label>
              <input v-model="form.phuongXa" type="text" class="form-control" placeholder="Dịch Vọng Hậu, Bến Nghé..." />
            </div>

            <div class="col-12">
              <label class="form-label small fw-semibold">Địa chỉ giao hàng chi tiết <span class="text-danger">*</span></label>
              <input v-model="form.diaChi" type="text" class="form-control" :class="{ 'is-invalid': loi.diaChi }" placeholder="Số nhà, tên đường, ngõ..." />
              <div v-if="loi.diaChi" class="invalid-feedback">{{ loi.diaChi }}</div>
            </div>

            <div class="col-12">
              <label class="form-label small fw-semibold">Ghi chú giao hàng</label>
              <textarea v-model="form.ghiChu" class="form-control" rows="2" placeholder="VD: Giao giờ hành chính, gọi trước khi giao..."></textarea>
            </div>
          </div>
        </div>

        <!-- Phương Thức Thanh Toán -->
        <div class="card border-0 shadow-sm rounded-4 p-4 bg-white">
          <h5 class="fw-bold text-dark mb-3 d-flex align-items-center gap-2">
            <i class="bi bi-credit-card-2-front-fill text-primary"></i> Phương Thức Thanh Toán
          </h5>

          <div class="d-flex flex-column gap-2">
            <!-- COD -->
            <label class="card p-3 border rounded-3 d-flex flex-row align-items-center gap-3 cursor-pointer" :class="{ 'border-primary bg-light': form.phuongThucThanhToan === 1 }">
              <input v-model="form.phuongThucThanhToan" type="radio" :value="1" class="form-check-input mt-0" />
              <div>
                <div class="fw-bold text-dark"><i class="bi bi-cash-stack text-success me-1"></i> Thanh toán khi nhận hàng (COD)</div>
                <small class="text-muted">Bạn thanh toán bằng tiền mặt cho nhân viên giao hàng khi nhận hàng.</small>
              </div>
            </label>

            <!-- VietQR -->
            <label class="card p-3 border rounded-3 d-flex flex-row align-items-center gap-3 cursor-pointer" :class="{ 'border-primary bg-light': form.phuongThucThanhToan === 2 }">
              <input v-model="form.phuongThucThanhToan" type="radio" :value="2" class="form-check-input mt-0" />
              <div>
                <div class="fw-bold text-dark"><i class="bi bi-qr-code-scan text-primary me-1"></i> Chuyển khoản ngân hàng qua mã VietQR</div>
                <small class="text-muted">Quét mã QR bằng ứng dụng ngân hàng bất kỳ sau khi bấm Đặt Hàng.</small>
              </div>
            </label>
          </div>
        </div>
      </div>

      <!-- Cột Phải: Tóm Tắt Đơn Hàng & Giỏ Hàng -->
      <div class="col-lg-5">
        <div class="card border-0 shadow-sm rounded-4 p-4 bg-white sticky-top" style="top: 90px;">
          <h5 class="fw-bold text-dark mb-3 d-flex justify-content-between align-items-center">
            <span>Đơn Hàng Của Bạn</span>
            <span class="badge bg-primary text-white">{{ cart.tongSoLuong.value }} món</span>
          </h5>

          <!-- Danh sách món trong giỏ -->
          <div class="vnb-cart-items-list mb-3" style="max-height: 280px; overflow-y: auto;">
            <div v-for="item in cart.items.value" :key="item.id" class="d-flex align-items-center gap-3 py-2 border-bottom">
              <img :src="item.anh" :alt="item.tenSanPham" class="rounded-3 border p-1" style="width: 50px; height: 50px; object-fit: contain;" />
              <div class="flex-grow-1 min-w-0">
                <div class="small fw-bold text-truncate text-dark">{{ item.tenSanPham }}</div>
                <div class="small text-muted" style="font-size: 0.75rem;">
                  {{ item.tenMau }} · {{ item.trongLuong }} · {{ item.chuVi }}
                </div>
                <div class="d-flex align-items-center justify-content-between mt-1">
                  <div class="input-group input-group-sm" style="width: 85px;">
                    <button class="btn btn-outline-secondary px-2" type="button" @click="cart.suaSoLuong(item.id, item.soLuong - 1)">-</button>
                    <span class="input-group-text px-2 bg-white">{{ item.soLuong }}</span>
                    <button class="btn btn-outline-secondary px-2" type="button" @click="cart.suaSoLuong(item.id, item.soLuong + 1)">+</button>
                  </div>
                  <span class="small fw-bold text-danger">{{ formatTien(item.giaBan * item.soLuong) }}</span>
                </div>
              </div>
              <button type="button" class="btn btn-sm btn-link text-danger p-0" title="Xóa" @click="cart.xoa(item.id)">
                <i class="bi bi-trash"></i>
              </button>
            </div>
          </div>

          <!-- Áp dụng mã Voucher -->
          <div class="mb-3">
            <label class="form-label small fw-bold text-muted mb-1">Mã giảm giá (Voucher)</label>
            <div class="input-group">
              <input v-model="maVoucherNhap" type="text" class="form-control form-control-sm text-uppercase" placeholder="Nhập mã voucher" :disabled="!!voucherDaChon" />
              <button v-if="!voucherDaChon" class="btn btn-dark btn-sm px-3" type="button" @click="apDungMaNhap">Áp dụng</button>
              <button v-else class="btn btn-outline-danger btn-sm px-3" type="button" @click="goVoucher">Gỡ bỏ</button>
            </div>

            <!-- Gợi ý voucher công khai -->
            <div v-if="phieuList.length > 0 && !voucherDaChon" class="mt-2 d-flex flex-wrap gap-1">
              <button
                v-for="p in phieuList.slice(0, 3)"
                :key="p.id"
                type="button"
                class="badge bg-light text-primary border border-primary-subtle text-decoration-none btn-sm"
                @click="apDungVoucher(p)"
              >
                🏷️ {{ p.maPhieu }} ({{ p.hinhThucGiam === 1 ? `-${p.giaTriGiam}%` : `-${formatTien(p.giaTriGiam)}` }})
              </button>
            </div>
          </div>

          <!-- Bảng Chi Phí -->
          <div class="bg-light p-3 rounded-3 mb-4 small">
            <div class="d-flex justify-content-between mb-2">
              <span class="text-muted">Tạm tính:</span>
              <span class="fw-semibold">{{ formatTien(cart.tongTien.value) }}</span>
            </div>
            <div v-if="tienGiamVoucher > 0" class="d-flex justify-content-between mb-2 text-success">
              <span>Giảm giá voucher:</span>
              <span>-{{ formatTien(tienGiamVoucher) }}</span>
            </div>
            <div class="d-flex justify-content-between mb-2">
              <span class="text-muted">Phí vận chuyển:</span>
              <span :class="phiVanChuyen === 0 ? 'text-success fw-bold' : ''">
                {{ phiVanChuyen === 0 ? 'MIỄN PHÍ' : formatTien(phiVanChuyen) }}
              </span>
            </div>
            <div class="d-flex justify-content-between pt-2 border-top fs-6">
              <span class="fw-bold text-dark">Tổng cộng:</span>
              <span class="fw-black text-danger fs-5">{{ formatTien(thanhTienCuoi) }}</span>
            </div>
          </div>

          <!-- Nút Đặt Hàng -->
          <button
            type="button"
            class="btn vnb-btn-hero btn-lg w-100 fw-bold py-3 shadow-sm rounded-pill"
            :disabled="dangDatHang"
            @click="datHang"
          >
            <span v-if="dangDatHang" class="spinner-border spinner-border-sm me-2"></span>
            <i v-else class="bi bi-shield-check me-2"></i>
            XÁC NHẬN ĐẶT HÀNG ({{ formatTien(thanhTienCuoi) }})
          </button>
          <small class="text-muted text-center d-block mt-2" style="font-size: 0.72rem;">
            Nhấn đặt hàng đồng nghĩa bạn đồng ý với Điều khoản mua sắm của FootStyle
          </small>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.fw-black {
  font-weight: 900;
}
.cursor-pointer {
  cursor: pointer;
}
.max-w-500 {
  max-width: 500px;
}
.vnb-btn-hero {
  background: var(--fs-primary, #0977ec);
  color: #fff;
  border: none;
  transition: all 0.2s;
}
.vnb-btn-hero:hover {
  background: var(--fs-primary-dark, #075fc0);
  color: #fff;
}
</style>
