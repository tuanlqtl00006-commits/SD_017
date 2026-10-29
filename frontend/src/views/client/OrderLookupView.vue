<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '../../composables/useAuth'
import api from '../../services/api'
import { formatMoney, formatNgayGio } from '../../utils/format'
import { docDonHang } from '../../utils/donHangLocal'

const route = useRoute()
const router = useRouter()
const auth = useAuth()

const orderCode = ref('')
const loading = ref(false)
const orderResult = ref(null)
const searchError = ref('')

// Các bước hiển thị trên đường tiến trình
const STEPS = [
  { ten: 'Chờ xác nhận', icon: 'bi-receipt' },
  { ten: 'Đã xác nhận', icon: 'bi-box-seam' },
  { ten: 'Đang giao hàng', icon: 'bi-truck' },
  { ten: 'Hoàn thành', icon: 'bi-check2-circle' },
]

// Tra cứu đơn là của TÀI KHOẢN đang đăng nhập: chưa đăng nhập / vừa đăng xuất thì rời khỏi trang
function laKhachDangNhap() {
  return auth.trangThai.nguoiDung?.vaiTro === 'KHACH_HANG'
}

function roiKhoiTrang() {
  orderResult.value = null
  router.replace({ path: '/shop/dang-nhap', query: { quayLai: route.fullPath } })
}

onMounted(async () => {
  await auth.khoiTao()
  if (!laKhachDangNhap()) {
    roiKhoiTrang()
    return
  }
  if (route.query.ma) {
    orderCode.value = String(route.query.ma).trim()
    handleSearch()
  }
})

// Đăng xuất khi đang xem: xóa kết quả ngay
watch(
  () => auth.trangThai.nguoiDung,
  (nd) => {
    if (!nd) {
      orderResult.value = null
      router.replace('/shop')
    }
  },
)

function tenThanhToan(v) {
  if (!v) return ''
  const s = String(v).toUpperCase()
  if (s === 'COD') return 'Thanh toán khi nhận hàng (COD)'
  if (s === 'QR') return 'Chuyển khoản VietQR'
  return String(v)
}

// Mã trạng thái của backend (0 hủy, 1 chờ xác nhận, 2 đã xác nhận, 3 chờ lấy hàng, 4 đang giao, 5 đã giao, 6 hoàn thành, 8 giao thất bại)
// -> bước trên đường tiến trình (0..3) và có bị hủy không
function buocTuBackend(code) {
  const c = Number(code)
  if (c === 0) return { buoc: 0, huy: true }
  if (c === 2 || c === 3) return { buoc: 1, huy: false }
  if (c === 4 || c === 8) return { buoc: 2, huy: false }
  if (c === 5) return { buoc: 3, huy: false }
  if (c === 6) return { buoc: 3, huy: false, hoanTat: true }
  return { buoc: 0, huy: false }
}

async function handleSearch() {
  const code = orderCode.value.trim()
  searchError.value = ''
  orderResult.value = null

  if (!code) {
    searchError.value = 'Vui lòng nhập mã đơn hàng (Ví dụ: HD01).'
    return
  }

  loading.value = true

  // Ưu tiên trạng thái THẬT từ backend (cửa hàng cập nhật khi xác nhận / giao hàng); không có thì mới dùng bản lưu trên máy
  try {
    orderResult.value = await traCuuBackend(code)
  } catch (err) {
    console.error('Lỗi khi tra cứu đơn hàng:', err)
    const local = traCuuLocal(code)
    if (local) orderResult.value = local
    else searchError.value = `Không tìm thấy đơn hàng có mã "${code}". Vui lòng kiểm tra lại.`
  } finally {
    loading.value = false
  }
}

// Tra cứu từ backend (đường dẫn công khai của cửa hàng online, khách không cần đăng nhập)
async function traCuuBackend(code) {
  const data = (await api.get(`/khach/don-hang/${encodeURIComponent(code)}`)).data
  const { buoc, huy, hoanTat } = buocTuBackend(data.trangThai)
  return {
    maHoaDon: data.maHoaDon || code,
    ngayTaoFormatted: data.ngayTao ? formatNgayGio(data.ngayTao) : '—',
    ten: data.hoTenNguoiNhan || 'Khách mua hàng',
    sdt: data.sdtNguoiNhan || '—',
    diaChi: data.diaChiGiaoHang || 'Nhận tại cửa hàng FootStyle',
    thanhToan: '',
    buoc,
    huy,
    hoanTat: !!hoanTat,
    trangThaiText: data.tenTrangThai || '',
    items: (data.chiTiet || []).map((i) => ({
      ten: i.tenSanPham || 'Sản phẩm',
      mau: i.mauSac || '',
      trongLuong: i.trongLuong || '',
      soLuong: i.soLuong,
      donGia: Number(i.donGia) || 0,
    })),
    tongTienHang: Number(data.tongTien) || 0,
    tienGiam: Number(data.soTienGiam) || 0,
    phiVanChuyen: Number(data.phiVanChuyen) || 0,
    tongThanhToan: Number(data.thanhTien) || 0,
  }
}

// Đơn lưu trên máy (đơn cũ chưa có trên backend, hoặc mất kết nối)
function traCuuLocal(code) {
  try {
    const f = docDonHang().find((o) => o.maHoaDon?.toUpperCase() === code.toUpperCase())
    if (!f) return null
    const t = Number(f.trangThai) || 0
    return {
      maHoaDon: f.maHoaDon,
      ngayTaoFormatted: formatNgayGio(f.ngayTao),
      ten: f.tenKhachHang || 'Khách mua hàng',
      sdt: f.sdtKhachHang || '—',
      diaChi: f.diaChiGiaoHang || 'Nhận tại cửa hàng FootStyle',
      thanhToan: tenThanhToan(f.phuongThucThanhToan),
      buoc: Math.min(t, 3),
      huy: t === 6 || t === 7,
      hoanTat: t === 3,
      trangThaiText: f.trangThaiText || '',
      items: (f.items || []).map((i) => ({
        ten: i.sanPhamTen || i.tenSanPham || 'Sản phẩm',
        mau: i.mauSac || '',
        trongLuong: i.trongLuong || '',
        soLuong: i.soLuong,
        donGia: i.giaBan || i.donGia || 0,
      })),
      tongTienHang: f.tongTienHang ?? null,
      tienGiam: f.tienGiam || 0,
      phiVanChuyen: f.phiVanChuyen ?? null,
      tongThanhToan: f.tongThanhToan || 0,
    }
  } catch (e) {
    console.error('Lỗi khi đọc local orders:', e)
    return null
  }
}

function stepClass(i, o) {
  if (o.huy) return ''
  if (o.hoanTat || o.buoc > i) return 'completed'
  if (o.buoc === i) return 'active'
  return ''
}
</script>

<template>
  <div class="order-lookup-view py-4">
    <div class="container ol-container">
      <!-- Breadcrumb -->
      <nav aria-label="breadcrumb" class="mb-3">
        <ol class="breadcrumb mb-0 small">
          <li class="breadcrumb-item"><RouterLink to="/shop">Trang chủ</RouterLink></li>
          <li class="breadcrumb-item active" aria-current="page">Tra cứu đơn hàng</li>
        </ol>
      </nav>

      <!-- Form tra cứu -->
      <div class="ol-search card border-0 shadow-sm rounded-4 p-4 mb-4">
        <div class="d-flex align-items-center gap-3 mb-3">
          <div class="ol-search-icon"><i class="bi bi-search"></i></div>
          <div>
            <h4 class="fw-bold text-dark mb-0">Tra cứu đơn hàng</h4>
            <p class="text-muted small mb-0">Nhập mã đơn hàng để theo dõi hành trình giao nhận sản phẩm của bạn.</p>
          </div>
        </div>
        <form @submit.prevent="handleSearch" class="d-flex flex-column flex-sm-row gap-2">
          <input
            type="text"
            class="form-control form-control-lg text-uppercase ol-input"
            placeholder="Nhập mã đơn hàng (Ví dụ: HD01...)"
            v-model="orderCode"
          />
          <button class="btn btn-primary btn-lg px-4 fw-semibold text-nowrap rounded-3" type="submit" :disabled="loading">
            <span v-if="loading" class="spinner-border spinner-border-sm me-1"></span>
            <span>Tra cứu</span>
          </button>
        </form>
        <div v-if="searchError" class="alert alert-danger py-2 small mt-3 mb-0">
          <i class="bi bi-exclamation-triangle-fill me-1"></i> {{ searchError }}
        </div>
      </div>

      <!-- Kết quả tra cứu -->
      <div v-if="orderResult" class="card border-0 shadow-sm rounded-4 p-4">
        <div class="d-flex flex-wrap justify-content-between align-items-start gap-3 pb-3 mb-4 ol-head">
          <div>
            <div class="text-muted small">Mã đơn hàng</div>
            <h3 class="fw-bold text-primary mb-0">{{ orderResult.maHoaDon }}</h3>
          </div>
          <div class="text-sm-end">
            <span v-if="orderResult.huy" class="badge rounded-pill text-bg-danger px-3 py-2 mb-1">Đã hủy</span>
            <span v-else class="badge rounded-pill text-bg-primary px-3 py-2 mb-1">{{ STEPS[orderResult.buoc].ten }}</span>
            <div class="text-muted small"><i class="bi bi-clock me-1"></i>Đặt lúc {{ orderResult.ngayTaoFormatted }}</div>
          </div>
        </div>

        <!-- Tiến trình -->
        <div v-if="orderResult.huy" class="alert alert-danger d-flex align-items-center gap-2 mb-4">
          <i class="bi bi-x-circle-fill fs-5"></i>
          <div>Đơn hàng này đã bị hủy.</div>
        </div>
        <div v-else class="order-timeline mb-4">
          <div v-for="(s, i) in STEPS" :key="s.ten" class="order-step" :class="stepClass(i, orderResult)">
            <div class="order-step-icon"><i class="bi" :class="s.icon"></i></div>
            <div class="order-step-label">{{ s.ten }}</div>
          </div>
        </div>

        <!-- Thông tin giao hàng & sản phẩm -->
        <div class="row g-4">
          <div class="col-lg-5">
            <div class="ol-box h-100">
              <h6 class="fw-bold mb-3 text-dark"><i class="bi bi-geo-alt-fill text-primary me-2"></i>Thông tin người nhận</h6>
              <div class="ol-row"><span class="text-muted">Họ và tên</span><strong>{{ orderResult.ten }}</strong></div>
              <div class="ol-row"><span class="text-muted">Số điện thoại</span><strong>{{ orderResult.sdt }}</strong></div>
              <div class="ol-row ol-row-block">
                <span class="text-muted">Địa chỉ nhận</span>
                <strong>{{ orderResult.diaChi }}</strong>
              </div>
              <div v-if="orderResult.thanhToan" class="ol-row">
                <span class="text-muted">Thanh toán</span>
                <span class="badge text-bg-light border">{{ orderResult.thanhToan }}</span>
              </div>
            </div>
          </div>

          <div class="col-lg-7">
            <div class="ol-box h-100">
              <h6 class="fw-bold mb-3 text-dark"><i class="bi bi-bag-check-fill text-primary me-2"></i>Sản phẩm</h6>
              <div class="d-flex flex-column gap-2 mb-3">
                <div v-for="(item, idx) in orderResult.items" :key="idx" class="ol-item">
                  <div class="flex-grow-1">
                    <div class="fw-semibold">{{ item.ten }}</div>
                    <div v-if="item.mau || item.trongLuong" class="text-muted small">
                      <span v-if="item.mau">Màu: {{ item.mau }}</span>
                      <span v-if="item.trongLuong"> · {{ item.trongLuong }}</span>
                    </div>
                    <div class="text-muted small">{{ formatMoney(item.donGia) }} × {{ item.soLuong }}</div>
                  </div>
                  <div class="fw-semibold text-nowrap">{{ formatMoney(item.donGia * item.soLuong) }}</div>
                </div>
              </div>

              <div class="ol-summary">
                <div v-if="orderResult.tongTienHang !== null" class="d-flex justify-content-between small">
                  <span class="text-muted">Tạm tính</span><span>{{ formatMoney(orderResult.tongTienHang) }}</span>
                </div>
                <div v-if="orderResult.tienGiam > 0" class="d-flex justify-content-between small">
                  <span class="text-muted">Giảm giá</span><span class="text-success">-{{ formatMoney(orderResult.tienGiam) }}</span>
                </div>
                <div v-if="orderResult.phiVanChuyen !== null" class="d-flex justify-content-between small">
                  <span class="text-muted">Phí vận chuyển</span>
                  <span>{{ orderResult.phiVanChuyen ? formatMoney(orderResult.phiVanChuyen) : 'Miễn phí' }}</span>
                </div>
                <div class="d-flex justify-content-between align-items-center pt-2 mt-2 ol-total">
                  <span class="fw-bold">Tổng thanh toán</span>
                  <span class="fs-4 fw-bold text-danger">{{ formatMoney(orderResult.tongThanhToan) }}</span>
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
.ol-container {
  max-width: 980px;
}
.ol-search-icon {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: #e8f1ff;
  color: #0d6efd;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.3rem;
  flex-shrink: 0;
}
.ol-input {
  font-size: 1rem;
}
.ol-head {
  border-bottom: 1px solid #eef0f3;
}

/* Đường tiến trình nằm ngang, có đường nối giữa các bước */
.order-timeline {
  display: flex;
  justify-content: space-between;
  position: relative;
  padding: 8px 0;
}
.order-step {
  position: relative;
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 8px;
  z-index: 1;
}
.order-step::before {
  content: '';
  position: absolute;
  top: 23px;
  right: 50%;
  width: 100%;
  height: 3px;
  background: #e3e7ed;
  z-index: -1;
}
.order-step:first-child::before {
  display: none;
}
.order-step.completed::before,
.order-step.active::before {
  background: #0d6efd;
}
.order-step-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #fff;
  border: 3px solid #e3e7ed;
  color: #9aa4b2;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
}
.order-step.completed .order-step-icon {
  background: #0d6efd;
  border-color: #0d6efd;
  color: #fff;
}
.order-step.active .order-step-icon {
  border-color: #0d6efd;
  color: #0d6efd;
  box-shadow: 0 0 0 5px rgba(13, 110, 253, 0.15);
}
.order-step-label {
  font-size: 0.85rem;
  color: #7b8794;
  font-weight: 500;
}
.order-step.completed .order-step-label,
.order-step.active .order-step-label {
  color: #18242f;
  font-weight: 700;
}

.ol-box {
  background: #f8fafc;
  border: 1px solid #eef0f3;
  border-radius: 14px;
  padding: 18px;
}
.ol-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 0;
  font-size: 0.9rem;
  border-bottom: 1px dashed #e3e7ed;
}
.ol-row:last-child {
  border-bottom: 0;
}
.ol-row-block {
  flex-direction: column;
  gap: 2px;
}
.ol-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  background: #fff;
  border: 1px solid #eef0f3;
  border-radius: 10px;
  padding: 10px 12px;
}
.ol-summary {
  border-top: 1px solid #e3e7ed;
  padding-top: 12px;
}
.ol-total {
  border-top: 1px dashed #cfd6df;
}

@media (max-width: 576px) {
  .order-step-icon {
    width: 40px;
    height: 40px;
    font-size: 1rem;
  }
  .order-step::before {
    top: 19px;
  }
  .order-step-label {
    font-size: 0.72rem;
  }
}
</style>
