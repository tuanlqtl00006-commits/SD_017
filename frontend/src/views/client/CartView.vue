<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../../stores/cart'
import { formatMoney } from '../../utils/format'
import { dongTienDon } from '../../utils/tienDon'
import { useBaoGiaGioHang } from '../../composables/useBaoGiaGioHang'

const router = useRouter()
const cartStore = useCartStore()
// Giá trong giỏ được cập nhật theo cửa hàng (đợt giảm giá hết hạn, giá đổi) và các số tiền lấy theo máy chủ
useBaoGiaGioHang(cartStore)

// Các dòng tiền cùng tên, cùng thứ tự với trang thanh toán, bán quầy, chi tiết hóa đơn (utils/tienDon.js)
const tien = computed(() =>
  dongTienDon({
    tienHangSauDot: cartStore.subTotal,
    giamTheoDot: cartStore.giamTheoDot,
    giamPhieu: cartStore.discountAmount,
    maPhieu: cartStore.appliedVoucher?.ma,
    coGiaoHang: true,
    phiVanChuyen: cartStore.chuaCoTinh ? null : cartStore.shippingFee, // giỏ hàng chưa biết tỉnh: phí tính theo địa chỉ
    tongThanhToan: cartStore.finalTotal,
  }),
)
const coGiamDot = (item) => Number(item.giaGoc) > Number(item.giaBan)
const phanTramGiam = (item) => Math.round((1 - Number(item.giaBan) / Number(item.giaGoc)) * 100)

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
              <table class="table align-middle mb-0" style="min-width: 0;">
                <thead class="table-light small text-muted text-uppercase">
                  <tr>
                    <th>Sản phẩm</th>
                    <th class="text-center text-nowrap">Đơn giá</th>
                    <th class="text-center text-nowrap" style="width: 120px;">Số lượng</th>
                    <th class="text-end text-nowrap">Tạm tính</th>
                    <th style="width: 36px;"></th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in cartStore.items" :key="item.bienTheId">
                    <td>
                      <div class="d-flex align-items-center gap-3">
                        <!-- Nhãn -35% nằm góc trên của ảnh sản phẩm -->
                        <div class="position-relative flex-shrink-0 rounded-3 overflow-hidden">
                          <img
                            :src="item.anh || '/logo.png'"
                            :alt="item.sanPhamTen"
                            class="rounded-3 border p-1"
                            style="width: 56px; height: 56px; object-fit: contain; background: #f8fafc;"
                          />
                          <!-- cùng kiểu nhãn với bảng chọn sản phẩm ở Bán hàng tại quầy -->
                          <span
                            v-if="coGiamDot(item)"
                            class="position-absolute top-0 start-0 text-white fw-bold"
                            style="padding: 1px 4px; border-bottom-right-radius: 7px; background: #dc2626; font-size: 0.62rem; line-height: 1.4;"
                            data-testid="gio-giam-anh"
                          >-{{ phanTramGiam(item) }}%</span>
                        </div>
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
                    <td class="text-center text-nowrap" data-testid="gio-don-gia">
                      <div v-if="coGiamDot(item)" class="text-muted small">
                        <s>{{ formatMoney(item.giaGoc) }}</s>
                      </div>
                      <div class="fw-semibold text-danger">{{ formatMoney(item.giaBan) }}</div>
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
                    <td class="text-end fw-bold text-dark text-nowrap">
                      {{ formatMoney(item.giaBan * item.soLuong) }}
                    </td>
                    <td class="text-end">
                      <button
                        type="button"
                        @click="cartStore.removeItem(item.bienTheId)"
                        class="btn btn-link text-secondary p-0"
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
          <!-- Tóm tắt đơn hàng -->
          <div class="card border-0 shadow-sm rounded-4 p-4">
            <h6 class="fw-bold mb-3">Tóm tắt đơn hàng</h6>

            <p v-if="cartStore.baoGia?.doiGia" class="alert alert-warning py-2 px-3 small mb-3 border-0" data-testid="gio-doi-gia">
              <i class="bi bi-arrow-repeat me-1"></i> Giá một số sản phẩm vừa được cập nhật theo cửa hàng.
            </p>
            <template v-for="d in tien.dong.slice(0, -1)" :key="d.khoa">
              <div class="d-flex justify-content-between mb-2 small" :data-dong="d.khoa">
                <span :class="d.dau === '-' && d.so > 0 ? 'text-success' : 'text-muted'">
                  {{ d.nhan }}<template v-if="d.ma"> ({{ d.ma }})</template>:
                </span>
                <span v-if="d.khoa === 'phi-van-chuyen' && d.so === null" class="text-muted">Tính theo địa chỉ giao hàng</span>
                <span v-else-if="d.khoa === 'phi-van-chuyen' && d.so === 0" class="text-success fw-bold">Miễn phí</span>
                <span v-else-if="d.khoa === 'phieu' && !d.so" class="text-muted">Không dùng phiếu</span>
                <span v-else class="fw-semibold" :class="{ 'text-success': d.dau === '-' }">
                  {{ d.dau === '-' ? '-' : '' }}{{ formatMoney(d.so) }}
                </span>
              </div>
            </template>
            <p v-if="cartStore.loiPhieu" class="text-danger small mb-2" data-testid="gio-loi-phieu">{{ cartStore.loiPhieu }}</p>


            <hr class="my-2" />

            <div class="d-flex justify-content-between align-items-baseline mb-4">
              <span class="fw-bold">{{ tien.dong.at(-1).nhan }}:</span>
              <span class="fs-4 fw-bold text-danger" data-dong="tong-thanh-toan">{{ formatMoney(tien.tong) }}</span>
            </div>

            <button
              type="button"
              class="btn text-white btn-lg w-100 rounded-pill fw-bold shadow-sm"
              style="background-color: #0d6efd; border-color: #0d6efd;"
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
