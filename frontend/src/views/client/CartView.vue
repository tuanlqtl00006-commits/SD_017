<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../../stores/cart'
import { phieuGiamGiaService } from '../../services/phieuGiamGiaService'
import { formatMoney } from '../../utils/format'

const router = useRouter()
const cartStore = useCartStore()

const voucherCode = ref('')
const voucherError = ref('')
const voucherSuccess = ref('')
const checkingVoucher = ref(false)

async function applyVoucher() {
  voucherError.value = ''
  voucherSuccess.value = ''
  const code = voucherCode.value.trim().toUpperCase()

  if (!code) {
    voucherError.value = 'Vui lòng nhập mã phiếu giảm giá.'
    return
  }

  try {
    checkingVoucher.value = true
    const list = await phieuGiamGiaService.getAll()
    const found = list.find((v) => v.maPhieu?.toUpperCase() === code)

    if (!found) {
      voucherError.value = 'Mã phiếu giảm giá không tồn tại hoặc đã hết hạn.'
      return
    }

    if (found.trangThai === 0 || found.trangThai === false) {
      voucherError.value = 'Phiếu giảm giá này hiện đang tạm ngưng hoạt động.'
      return
    }

    if (found.donToiThieu && cartStore.subTotal < Number(found.donToiThieu)) {
      voucherError.value = `Đơn hàng tối thiểu phải từ ${formatMoney(found.donToiThieu)} để áp dụng mã này.`
      return
    }

    cartStore.setVoucher({
      ma: found.maPhieu,
      ten: found.tenPhieu,
      loaiGiam: found.loaiGiam,
      giaTriGiam: found.giaTriGiam,
      giamToiDa: found.giamToiDa,
      donToiThieu: found.donToiThieu,
    })

    voucherSuccess.value = `Đã áp dụng mã "${found.maPhieu}" thành công!`
    voucherCode.value = ''
  } catch (err) {
    console.error('Lỗi khi kiểm tra voucher:', err)
    voucherError.value = 'Không thể kiểm tra mã giảm giá lúc này.'
  } finally {
    checkingVoucher.value = false
  }
}

function removeVoucher() {
  cartStore.removeVoucher()
  voucherSuccess.value = ''
  voucherError.value = ''
}

function goToCheckout() {
  router.push('/shop/thanh-toan')
}
</script>

<template>
  <div class="cart-view py-4">
    <div class="container">
      <!-- Breadcrumb -->
      <nav aria-label="breadcrumb" class="mb-4">
        <ol class="breadcrumb mb-0 small">
          <li class="breadcrumb-item"><RouterLink to="/shop">Trang chủ</RouterLink></li>
          <li class="breadcrumb-item active" aria-current="page">Giỏ hàng của bạn</li>
        </ol>
      </nav>

      <!-- Giỏ hàng rỗng -->
      <div v-if="cartStore.items.length === 0" class="card border-0 shadow-sm rounded-4 p-5 text-center my-4">
        <div class="mb-3">
          <i class="bi bi-cart-x text-muted" style="font-size: 4rem;"></i>
        </div>
        <h4 class="fw-bold mb-2">Giỏ hàng của bạn đang trống</h4>
        <p class="text-muted mb-4">Hãy dạo quanh cửa hàng và chọn những cây vợt ưng ý nhất nhé!</p>
        <div>
          <RouterLink to="/shop/san-pham" class="btn btn-primary btn-lg rounded-pill px-4 fw-semibold shadow-sm">
            <i class="bi bi-arrow-left me-1"></i> Tiếp tục mua sắm
          </RouterLink>
        </div>
      </div>

      <!-- Danh sách giỏ hàng -->
      <div v-else class="row g-4">
        <div class="col-lg-8">
          <div class="card border-0 shadow-sm rounded-4 p-4 mb-4">
            <div class="d-flex justify-content-between align-items-center mb-3 pb-2 border-bottom">
              <h5 class="fw-bold mb-0 text-dark">
                Sản phẩm trong giỏ ({{ cartStore.totalCount }})
              </h5>
              <button
                type="button"
                @click="cartStore.clearCart"
                class="btn btn-link text-danger btn-sm text-decoration-none p-0"
              >
                <i class="bi bi-trash me-1"></i> Xóa tất cả
              </button>
            </div>

            <div class="table-responsive">
              <table class="table align-middle">
                <thead class="table-light small text-muted text-uppercase">
                  <tr>
                    <th style="min-width: 250px;">Sản phẩm</th>
                    <th class="text-center">Đơn giá</th>
                    <th class="text-center" style="width: 130px;">Số lượng</th>
                    <th class="text-end">Tạm tính</th>
                    <th style="width: 40px;"></th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in cartStore.items" :key="item.bienTheId">
                    <td>
                      <div class="d-flex align-items-center gap-3">
                        <img
                          :src="item.anh || '/logo.png'"
                          :alt="item.sanPhamTen"
                          class="rounded-3 border p-1"
                          style="width: 56px; height: 56px; object-fit: contain; background: #f8fafc;"
                        />
                        <div>
                          <RouterLink
                            :to="`/shop/san-pham/${item.sanPhamId}`"
                            class="fw-bold text-dark text-decoration-none small d-block mb-1"
                          >
                            {{ item.sanPhamTen }}
                          </RouterLink>
                          <div class="text-muted" style="font-size: 0.775rem;">
                            <span v-if="item.mauSac">Màu: <strong>{{ item.mauSac }}</strong></span>
                            <span v-if="item.trongLuong"> • Trọng lượng: <strong>{{ item.trongLuong }}</strong></span>
                            <span v-if="item.chuVi"> • Cán: <strong>{{ item.chuVi }}</strong></span>
                          </div>
                        </div>
                      </div>
                    </td>
                    <td class="text-center fw-semibold text-danger">
                      {{ formatMoney(item.giaBan) }}
                    </td>
                    <td class="text-center">
                      <div class="input-group input-group-sm">
                        <button
                          class="btn btn-outline-secondary"
                          type="button"
                          @click="cartStore.updateQuantity(item.bienTheId, item.soLuong - 1)"
                          :disabled="item.soLuong <= 1"
                        >
                          -
                        </button>
                        <input
                          type="text"
                          class="form-control text-center px-1"
                          :value="item.soLuong"
                          readonly
                        />
                        <button
                          class="btn btn-outline-secondary"
                          type="button"
                          @click="cartStore.updateQuantity(item.bienTheId, item.soLuong + 1)"
                          :disabled="item.soLuong >= item.soLuongTon"
                        >
                          +
                        </button>
                      </div>
                    </td>
                    <td class="text-end fw-bold text-dark">
                      {{ formatMoney(item.giaBan * item.soLuong) }}
                    </td>
                    <td class="text-end">
                      <button
                        type="button"
                        class="btn btn-link text-secondary p-0"
                        @click="cartStore.removeItem(item.bienTheId)"
                        title="Xóa món này"
                      >
                        <i class="bi bi-x-lg"></i>
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

            <div class="mt-3">
              <RouterLink to="/shop/san-pham" class="text-decoration-none small fw-semibold">
                <i class="bi bi-arrow-left me-1"></i> Tiếp tục chọn thêm sản phẩm khác
              </RouterLink>
            </div>
          </div>
        </div>

        <!-- Cột Tóm tắt thanh toán & Voucher -->
        <div class="col-lg-4">
          <!-- Nhập mã giảm giá -->
          <div class="card border-0 shadow-sm rounded-4 p-4 mb-3">
            <h6 class="fw-bold mb-3 d-flex align-items-center gap-2">
              <i class="bi bi-ticket-perforated-fill text-primary"></i> Mã phiếu giảm giá
            </h6>

            <div v-if="cartStore.appliedVoucher" class="alert alert-success d-flex justify-content-between align-items-center mb-0 p-2">
              <div>
                <strong class="d-block">{{ cartStore.appliedVoucher.ma }}</strong>
                <span class="small text-muted">{{ cartStore.appliedVoucher.ten }}</span>
              </div>
              <button
                type="button"
                class="btn btn-outline-danger btn-sm py-0 px-2"
                @click="removeVoucher"
              >
                Gỡ bỏ
              </button>
            </div>

            <div v-else>
              <div class="input-group mb-2">
                <input
                  type="text"
                  class="form-control form-control-sm text-uppercase"
                  placeholder="Nhập mã voucher (vd: VCH...)"
                  v-model="voucherCode"
                  @keyup.enter="applyVoucher"
                />
                <button
                  class="btn btn-primary btn-sm px-3"
                  type="button"
                  :disabled="checkingVoucher"
                  @click="applyVoucher"
                >
                  <span v-if="checkingVoucher" class="spinner-border spinner-border-sm"></span>
                  <span v-else>Áp dụng</span>
                </button>
              </div>
              <div v-if="voucherError" class="small text-danger mt-1">
                {{ voucherError }}
              </div>
              <div v-if="voucherSuccess" class="small text-success mt-1">
                {{ voucherSuccess }}
              </div>
            </div>
          </div>

          <!-- Tóm tắt đơn hàng -->
          <div class="card border-0 shadow-sm rounded-4 p-4">
            <h6 class="fw-bold mb-3">Tóm tắt đơn hàng</h6>

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

            <div class="alert alert-info py-2 px-3 small mb-3 border-0" v-if="cartStore.subTotal < 500000">
              <i class="bi bi-info-circle me-1"></i> Mua thêm <strong>{{ formatMoney(500000 - cartStore.subTotal) }}</strong> để được <strong>Miễn phí vận chuyển</strong>!
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
              @click="goToCheckout"
            >
              Tiến hành thanh toán <i class="bi bi-arrow-right ms-1"></i>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
