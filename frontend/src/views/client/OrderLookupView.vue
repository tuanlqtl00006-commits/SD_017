<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '../../services/api'
import { formatMoney, formatNgayGio } from '../../utils/format'

const route = useRoute()

const orderCode = ref('')
const phoneNumber = ref('')
const loading = ref(false)
const orderResult = ref(null)
const searchError = ref('')

onMounted(() => {
  if (route.query.ma) {
    orderCode.value = String(route.query.ma).trim()
    handleSearch()
  }
})

async function handleSearch() {
  const code = orderCode.value.trim()
  searchError.value = ''
  orderResult.value = null

  if (!code) {
    searchError.value = 'Vui lòng nhập mã đơn hàng (Ví dụ: HD01).'
    return
  }

  loading.value = true

  // 1. Kiểm tra trong localStorage trước (các đơn vừa đặt ở client)
  try {
    const raw = localStorage.getItem('footstyle_orders')
    const localOrders = raw ? JSON.parse(raw) : []
    const foundLocal = localOrders.find(
      (o) => o.maHoaDon?.toUpperCase() === code.toUpperCase()
    )

    if (foundLocal) {
      orderResult.value = {
        ...foundLocal,
        ngayTaoFormatted: formatNgayGio(foundLocal.ngayTao),
      }
      loading.value = false
      return
    }
  } catch (e) {
    console.error('Lỗi khi đọc local orders:', e)
  }

  // 2. Tra cứu từ API Backend: /api/hoa-don/tra-cuu?ma=...
  try {
    const res = await api.get(`/hoa-don/tra-cuu?ma=${encodeURIComponent(code)}`)
    if (res.data) {
      const data = res.data
      orderResult.value = {
        maHoaDon: data.maHoaDon || code,
        ngayTaoFormatted: data.ngayTao ? formatNgayGio(data.ngayTao) : '—',
        tenKhachHang: data.tenKhachHang || 'Khách mua hàng',
        sdtKhachHang: data.sdtKhachHang || '—',
        diaChiGiaoHang: data.diaChiGiaoHang || 'Nhận tại cửa hàng FootStyle',
        trangThai: data.trangThai ?? 0,
        trangThaiText: data.tenTrangThai || 'Chờ xác nhận',
        tongThanhToan: data.tongTien || data.thanhTien || 0,
        items: data.chiTietList || [],
        phuongThucThanhToan: data.phuongThucThanhToan || 'COD',
      }
    } else {
      searchError.value = `Không tìm thấy đơn hàng có mã "${code}". Vui lòng kiểm tra lại.`
    }
  } catch (err) {
    console.error('Lỗi khi tra cứu đơn hàng:', err)
    searchError.value = `Không tìm thấy đơn hàng có mã "${code}". Vui lòng kiểm tra lại.`
  } finally {
    loading.value = false
  }
}

// Tính bước trạng thái cho timeline: 0: Chờ xác nhận, 1: Đã xác nhận, 2: Đang giao, 3: Đã giao/Hoàn thành
function getStepClass(stepIndex, currentStatus) {
  if (currentStatus === 6 || currentStatus === 'DA_HUY' || currentStatus === 7) {
    return 'cancelled'
  }
  const statusNum = Number(currentStatus) || 0
  if (statusNum > stepIndex) return 'completed'
  if (statusNum === stepIndex) return 'active'
  return ''
}
</script>

<template>
  <div class="order-lookup-view py-4">
    <div class="container">
      <!-- Breadcrumb -->
      <nav aria-label="breadcrumb" class="mb-4">
        <ol class="breadcrumb mb-0 small">
          <li class="breadcrumb-item"><RouterLink to="/shop">Trang chủ</RouterLink></li>
          <li class="breadcrumb-item active" aria-current="page">Tra cứu đơn hàng</li>
        </ol>
      </nav>

      <!-- Form tra cứu -->
      <div class="card border-0 shadow-sm rounded-4 p-4 mb-4">
        <div class="row align-items-center g-3">
          <div class="col-lg-6">
            <h4 class="fw-bold text-dark mb-1 d-flex align-items-center gap-2">
              <i class="bi bi-search text-primary"></i> Tra Cứu Đơn Hàng Nhanh
            </h4>
            <p class="text-muted small mb-0">
              Nhập mã hóa đơn để theo dõi hành trình giao nhận sản phẩm của bạn.
            </p>
          </div>
          <div class="col-lg-6">
            <form @submit.prevent="handleSearch" class="d-flex gap-2">
              <input
                type="text"
                class="form-control text-uppercase"
                placeholder="Nhập mã đơn hàng (Ví dụ: HD01...)"
                v-model="orderCode"
              />
              <button class="btn btn-primary px-4 fw-semibold text-nowrap rounded-3" type="submit" :disabled="loading">
                <span v-if="loading" class="spinner-border spinner-border-sm me-1"></span>
                <span>Tra cứu</span>
              </button>
            </form>
            <div v-if="searchError" class="small text-danger mt-2">
              <i class="bi bi-exclamation-triangle-fill me-1"></i> {{ searchError }}
            </div>
          </div>
        </div>
      </div>

      <!-- Kết quả tra cứu -->
      <div v-if="orderResult" class="card border-0 shadow-sm rounded-4 p-4">
        <div class="d-flex flex-wrap justify-content-between align-items-center border-bottom pb-3 mb-4 gap-2">
          <div>
            <span class="text-muted small">Mã đơn hàng:</span>
            <h4 class="fw-bold text-primary mb-0">{{ orderResult.maHoaDon }}</h4>
          </div>
          <div class="text-end">
            <span class="text-muted small d-block">Ngày đặt hàng</span>
            <strong>{{ orderResult.ngayTaoFormatted }}</strong>
          </div>
        </div>

        <!-- Tiến trình Timeline -->
        <div class="order-timeline">
          <div class="order-step" :class="getStepClass(0, orderResult.trangThai)">
            <div class="order-step-icon"><i class="bi bi-receipt"></i></div>
            <div class="order-step-label">Chờ xác nhận</div>
          </div>
          <div class="order-step" :class="getStepClass(1, orderResult.trangThai)">
            <div class="order-step-icon"><i class="bi bi-box-seam"></i></div>
            <div class="order-step-label">Đã xác nhận</div>
          </div>
          <div class="order-step" :class="getStepClass(2, orderResult.trangThai)">
            <div class="order-step-icon"><i class="bi bi-truck"></i></div>
            <div class="order-step-label">Đang giao hàng</div>
          </div>
          <div class="order-step" :class="getStepClass(3, orderResult.trangThai)">
            <div class="order-step-icon"><i class="bi bi-check2-circle"></i></div>
            <div class="order-step-label">Hoàn thành</div>
          </div>
        </div>

        <!-- Thông tin giao hàng & sản phẩm -->
        <div class="row g-4 mt-2">
          <div class="col-md-5">
            <div class="bg-light p-3 rounded-3 h-100">
              <h6 class="fw-bold mb-3 text-dark">Thông tin người nhận</h6>
              <div class="small mb-2">
                <span class="text-muted">Họ và tên:</span>
                <strong class="ms-1">{{ orderResult.tenKhachHang }}</strong>
              </div>
              <div class="small mb-2">
                <span class="text-muted">Số điện thoại:</span>
                <strong class="ms-1">{{ orderResult.sdtKhachHang }}</strong>
              </div>
              <div class="small mb-2">
                <span class="text-muted">Địa chỉ nhận:</span>
                <div class="fw-semibold mt-1">{{ orderResult.diaChiGiaoHang }}</div>
              </div>
              <div class="small mt-3">
                <span class="text-muted">Hình thức thanh toán:</span>
                <span class="badge bg-secondary ms-1">{{ orderResult.phuongThucThanhToan }}</span>
              </div>
            </div>
          </div>

          <div class="col-md-7">
            <div class="border rounded-3 p-3">
              <h6 class="fw-bold mb-3 text-dark">Danh sách sản phẩm</h6>
              <div class="table-responsive">
                <table class="table table-sm align-middle mb-0">
                  <thead class="small text-muted">
                    <tr>
                      <th>Sản phẩm</th>
                      <th class="text-center">Số lượng</th>
                      <th class="text-end">Đơn giá</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(item, idx) in orderResult.items" :key="idx">
                      <td class="small">
                        <strong>{{ item.sanPhamTen || item.tenSanPham || 'Sản phẩm' }}</strong>
                        <div class="text-muted" style="font-size: 0.75rem;" v-if="item.mauSac || item.trongLuong">
                          <span v-if="item.mauSac">Màu: {{ item.mauSac }}</span>
                          <span v-if="item.trongLuong"> | {{ item.trongLuong }}</span>
                        </div>
                      </td>
                      <td class="text-center small">{{ item.soLuong }}</td>
                      <td class="text-end small fw-semibold text-danger">
                        {{ formatMoney(item.giaBan || item.donGia || 0) }}
                      </td>
                    </tr>
                  </tbody>
                  <tfoot>
                    <tr>
                      <th colspan="2" class="text-end pt-3">Tổng thanh toán:</th>
                      <th class="text-end pt-3 fs-5 text-danger fw-bold">
                        {{ formatMoney(orderResult.tongThanhToan) }}
                      </th>
                    </tr>
                  </tfoot>
                </table>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
