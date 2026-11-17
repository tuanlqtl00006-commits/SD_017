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
    phiVanChuyen: cartStore.chuaCoPhi ? null : cartStore.shippingFee, // chưa tải được phí giao hàng: chưa tính
    tongThanhToan: cartStore.finalTotal,
  }),
)
const coGiamDot = (item) => Number(item.giaGoc) > Number(item.giaBan)
const phanTramGiam = (item) => Math.round((1 - Number(item.giaBan) / Number(item.giaGoc)) * 100)

const coSanPhamKhongDuHang = computed(() =>
  cartStore.items.some(
    (i) => (Number(i.soLuongTon) ?? 999) < Number(i.soLuong) || (Number(i.soLuongTon) ?? 999) <= 0
  )
)

function goToCheckout() {
  router.push('/shop/thanh-toan')
}

function onQuantityInput(item, event) {
  const maxStock = item.soLuongTon ?? 999
  let val = parseInt(event.target.value, 10)
  if (isNaN(val) || val < 1) val = 1
  if (val > maxStock) val = maxStock
  cartStore.updateQuantity(item.bienTheId, val)
  event.target.value = val
}
</script>

<template>
  <div class="cart-view gh-trang">
    <div class="vnb-container">
      <!-- Breadcrumb -->
      <nav aria-label="breadcrumb" class="gh-duong-dan">
        <ol class="breadcrumb mb-0 small">
          <li class="breadcrumb-item"><RouterLink to="/shop">Trang chủ</RouterLink></li>
          <li class="breadcrumb-item active" aria-current="page">Giỏ hàng của bạn</li>
        </ol>
      </nav>

      <h1 class="gh-tieu-de">Giỏ hàng <span v-if="cartStore.items.length">{{ cartStore.totalCount }} sản phẩm</span></h1>

      <!-- Giỏ hàng rỗng -->
      <div v-if="cartStore.items.length === 0" class="gh-trong">
        <span class="gh-trong-icon"><i class="bi bi-bag"></i></span>
        <h4>Giỏ hàng của bạn đang trống</h4>
        <p>Hãy dạo quanh cửa hàng và chọn những cây vợt ưng ý nhất nhé!</p>
        <RouterLink to="/shop/san-pham" class="btn gh-nut-chinh">
          Tiếp tục mua sắm <i class="bi bi-arrow-right"></i>
        </RouterLink>
      </div>

      <!-- Danh sách giỏ hàng -->
      <div v-else class="gh-luoi">
        <section class="gh-ds">
          <div class="gh-ds-dau">
            <span>Sản phẩm</span>
            <button type="button" class="gh-xoa-het" @click="cartStore.clearCart">
              <i class="bi bi-trash3"></i> Xóa tất cả
            </button>
          </div>

          <article v-for="item in cartStore.items" :key="item.bienTheId" class="gh-dong">
            <RouterLink :to="`/shop/san-pham/${item.sanPhamId}`" class="gh-anh">
              <img :src="item.anh || '/logo.png'" :alt="item.sanPhamTen" />
              <!-- cùng kiểu nhãn với bảng chọn sản phẩm ở Bán hàng tại quầy -->
              <span v-if="coGiamDot(item)" class="gh-anh-giam" data-testid="gio-giam-anh">-{{ phanTramGiam(item) }}%</span>
            </RouterLink>

            <div class="gh-thong-tin">
              <RouterLink :to="`/shop/san-pham/${item.sanPhamId}`" class="gh-ten">{{ item.sanPhamTen }}</RouterLink>
              <div class="gh-phien-ban">
                <span v-if="item.mauSac">{{ item.mauSac }}</span>
                <span v-if="item.trongLuong">{{ item.trongLuong }}</span>
                <span v-if="item.chuVi">{{ item.chuVi }}</span>
              </div>
              <!-- Số lượng tồn kho sản phẩm -->
              <div class="gh-ton">
                <span v-if="(item.soLuongTon ?? 1) <= 0" class="gh-ton-loi">
                  <i class="bi bi-x-circle-fill"></i> Hết hàng trong kho
                </span>
                <span v-else-if="item.soLuong > item.soLuongTon" class="gh-ton-loi">
                  <i class="bi bi-exclamation-triangle-fill"></i> Kho chỉ còn {{ item.soLuongTon }} sản phẩm
                </span>
                <span v-else-if="item.soLuongTon !== undefined && item.soLuongTon !== null" class="gh-ton-con">
                  Còn lại: {{ item.soLuongTon }} sản phẩm
                </span>
              </div>
            </div>

            <div class="gh-don-gia" data-testid="gio-don-gia">
              <div class="gh-gia" :class="{ 'is-giam': coGiamDot(item) }">{{ formatMoney(item.giaBan) }}</div>
              <s v-if="coGiamDot(item)">{{ formatMoney(item.giaGoc) }}</s>
            </div>

            <div class="gh-so-luong">
              <button
                type="button"
                aria-label="Giảm số lượng"
                @click="cartStore.updateQuantity(item.bienTheId, item.soLuong - 1)"
                :disabled="item.soLuong <= 1"
              >
                <i class="bi bi-dash"></i>
              </button>
              <input
                type="number"
                class="no-spinner"
                aria-label="Số lượng"
                :value="item.soLuong"
                :min="1"
                :max="item.soLuongTon ?? 999"
                @change="onQuantityInput(item, $event)"
                @blur="onQuantityInput(item, $event)"
                @keydown.enter="$event.target.blur()"
              />
              <button
                type="button"
                aria-label="Tăng số lượng"
                @click="cartStore.updateQuantity(item.bienTheId, item.soLuong + 1)"
                :disabled="item.soLuong >= (item.soLuongTon ?? 999)"
              >
                <i class="bi bi-plus"></i>
              </button>
            </div>

            <div class="gh-tam-tinh">{{ formatMoney(item.giaBan * item.soLuong) }}</div>

            <button type="button" class="gh-xoa" title="Xóa món này" aria-label="Xóa món này" @click="cartStore.removeItem(item.bienTheId)">
              <i class="bi bi-x-lg"></i>
            </button>
          </article>

          <RouterLink to="/shop/san-pham" class="gh-tiep-tuc">
            <i class="bi bi-arrow-left"></i> Tiếp tục chọn thêm sản phẩm khác
          </RouterLink>
        </section>

        <!-- Cột Tóm tắt thanh toán -->
        <aside class="gh-tom-tat">
          <h2>Tóm tắt đơn hàng</h2>

          <p v-if="cartStore.baoGia?.doiGia" class="gh-canh-bao" data-testid="gio-doi-gia">
            <i class="bi bi-arrow-repeat"></i> Giá một số sản phẩm vừa được cập nhật theo cửa hàng.
          </p>
          <template v-for="d in tien.dong.slice(0, -1)" :key="d.khoa">
            <div class="gh-tien-dong" :data-dong="d.khoa">
              <span :class="{ 'gh-xanh': d.dau === '-' && d.so > 0 }">
                {{ d.nhan }}<template v-if="d.ma"> ({{ d.ma }})</template>
              </span>
              <span v-if="d.khoa === 'phi-van-chuyen' && d.so === null" class="gh-mo">Chưa tính</span>
              <span v-else-if="d.khoa === 'phi-van-chuyen' && d.so === 0" class="gh-xanh fw-bold">Miễn phí</span>
              <span v-else-if="d.khoa === 'phieu' && !d.so" class="gh-mo">Không dùng phiếu</span>
              <span v-else class="fw-semibold" :class="{ 'gh-xanh': d.dau === '-' }">
                {{ d.dau === '-' ? '-' : '' }}{{ formatMoney(d.so) }}
              </span>
            </div>
          </template>
          <p v-if="cartStore.conThieuMienPhiShip > 0" class="gh-free-ship" data-testid="gio-con-thieu-free-ship">
            <i class="bi bi-truck"></i>
            <span>Mua thêm <strong>{{ formatMoney(cartStore.conThieuMienPhiShip) }}</strong> để được miễn phí vận chuyển.</span>
          </p>
          <p v-if="cartStore.loiPhieu" class="text-danger small mb-2" data-testid="gio-loi-phieu">{{ cartStore.loiPhieu }}</p>

          <div class="gh-tong">
            <span>{{ tien.dong.at(-1).nhan }}</span>
            <strong data-dong="tong-thanh-toan">{{ formatMoney(tien.tong) }}</strong>
          </div>

          <div v-if="coSanPhamKhongDuHang" class="gh-loi">
            <i class="bi bi-exclamation-triangle-fill"></i>
            Có sản phẩm vượt quá tồn kho hoặc đã hết hàng. Vui lòng kiểm tra lại số lượng trước khi thanh toán.
          </div>

          <button
            type="button"
            class="btn gh-nut-chinh w-100"
            :disabled="coSanPhamKhongDuHang"
            @click="goToCheckout"
          >
            Tiến hành thanh toán <i class="bi bi-arrow-right"></i>
          </button>
          <div class="gh-an-toan"><i class="bi bi-shield-lock"></i> Thanh toán khi nhận hàng hoặc chuyển khoản VietQR</div>
        </aside>
      </div>
    </div>
  </div>
</template>

<style scoped>
.no-spinner::-webkit-inner-spin-button,
.no-spinner::-webkit-outer-spin-button {
  -webkit-appearance: none;
  margin: 0;
}
.no-spinner {
  -moz-appearance: textfield;
  appearance: textfield;
}
.gh-trang {
  min-height: calc(100vh - 260px);
  padding: 18px 0 72px;
  background: var(--ft-bg, #f6f7f9);
}
.gh-duong-dan {
  margin-bottom: 10px;
}
.gh-tieu-de {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin: 0 0 22px;
  font-size: 2rem;
  font-weight: 800;
  letter-spacing: -0.02em;
  color: var(--ft-ink, #0f172a);
}
.gh-tieu-de span {
  font-size: 1rem;
  font-weight: 500;
  letter-spacing: 0;
  color: #64748b;
}
.gh-trong {
  padding: 64px 20px;
  text-align: center;
  background: #fff;
  border: 1px solid var(--ft-line-2, #eef0f4);
  border-radius: 24px;
}
.gh-trong-icon {
  display: inline-grid;
  place-items: center;
  width: 84px;
  height: 84px;
  margin-bottom: 16px;
  font-size: 2.2rem;
  color: var(--ft-primary, #1d4ed8);
  background: #eff6ff;
  border-radius: 50%;
}
.gh-trong h4 {
  font-weight: 800;
  color: var(--ft-ink, #0f172a);
}
.gh-trong p {
  margin-bottom: 22px;
  color: #64748b;
}
.gh-luoi {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 380px;
  gap: 24px;
  align-items: start;
}
.gh-ds {
  padding: 8px 24px 20px;
  background: #fff;
  border: 1px solid var(--ft-line-2, #eef0f4);
  border-radius: 24px;
}
.gh-ds-dau {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 0;
  font-size: 0.85rem;
  font-weight: 600;
  color: #64748b;
  border-bottom: 1px solid var(--ft-line-2, #eef0f4);
}
.gh-xoa-het {
  padding: 0;
  font-weight: 600;
  color: #e11d48;
  background: none;
  border: 0;
}
.gh-xoa-het:hover {
  text-decoration: underline;
}
.gh-dong {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr) 110px 120px 110px 32px;
  gap: 16px;
  align-items: center;
  padding: 18px 0;
  border-bottom: 1px solid var(--ft-line-2, #eef0f4);
}
.gh-anh {
  position: relative;
  display: grid;
  place-items: center;
  width: 92px;
  height: 92px;
  padding: 8px;
  overflow: hidden;
  background: #f3f4f6;
  border-radius: 16px;
}
.gh-anh img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  mix-blend-mode: multiply;
}
.gh-anh-giam {
  position: absolute;
  top: 6px;
  left: 6px;
  padding: 1px 7px;
  font-size: 0.68rem;
  font-weight: 700;
  color: #fff;
  background: #e11d48;
  border-radius: 999px;
}
.gh-thong-tin {
  min-width: 0;
}
.gh-ten {
  display: -webkit-box;
  margin-bottom: 6px;
  overflow: hidden;
  font-weight: 700;
  line-height: 1.35;
  color: var(--ft-ink, #0f172a);
  text-decoration: none;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.gh-ten:hover {
  color: var(--ft-primary, #1d4ed8);
}
.gh-phien-ban {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 6px;
}
.gh-phien-ban span {
  padding: 2px 9px;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--ft-ink-2, #334155);
  background: #f1f5f9;
  border-radius: 999px;
}
.gh-ton {
  font-size: 0.78rem;
}
.gh-ton-con {
  color: #64748b;
}
.gh-ton-loi {
  font-weight: 600;
  color: #dc2626;
}
.gh-don-gia {
  text-align: right;
}
.gh-gia {
  font-weight: 700;
  color: var(--ft-ink, #0f172a);
}
.gh-gia.is-giam {
  color: #e11d48;
}
.gh-don-gia s {
  font-size: 0.8rem;
  color: #94a3b8;
}
.gh-so-luong {
  display: inline-flex;
  align-items: center;
  justify-self: center;
  padding: 3px;
  background: #f1f5f9;
  border-radius: 999px;
}
.gh-so-luong button {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  color: var(--ft-ink, #0f172a);
  background: #fff;
  border: 0;
  border-radius: 50%;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.08);
}
.gh-so-luong button:disabled {
  color: #cbd5e1;
  box-shadow: none;
}
.gh-so-luong input {
  width: 40px;
  font-weight: 700;
  text-align: center;
  background: transparent;
  border: 0;
  outline: none;
}
.gh-tam-tinh {
  font-weight: 800;
  text-align: right;
  color: var(--ft-ink, #0f172a);
}
.gh-xoa {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  color: #94a3b8;
  background: none;
  border: 0;
  border-radius: 50%;
}
.gh-xoa:hover {
  color: #e11d48;
  background: #fff1f2;
}
.gh-tiep-tuc {
  display: inline-flex;
  gap: 8px;
  margin-top: 18px;
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--ft-ink, #0f172a);
  text-decoration: none;
}
.gh-tiep-tuc:hover {
  color: var(--ft-primary, #1d4ed8);
}
.gh-tom-tat {
  position: sticky;
  top: 120px;
  padding: 24px;
  background: #fff;
  border: 1px solid var(--ft-line-2, #eef0f4);
  border-radius: 24px;
}
.gh-tom-tat h2 {
  margin: 0 0 18px;
  font-size: 1.2rem;
  font-weight: 800;
  color: var(--ft-ink, #0f172a);
}
.gh-canh-bao {
  display: flex;
  gap: 8px;
  padding: 10px 12px;
  font-size: 0.85rem;
  color: #92400e;
  background: #fffbeb;
  border-radius: 12px;
}
.gh-tien-dong {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  font-size: 0.92rem;
  color: #475569;
}
.gh-tien-dong > span:first-child {
  flex-shrink: 0;
  white-space: nowrap;
}
.gh-tien-dong > span:last-child {
  text-align: right;
  color: var(--ft-ink, #0f172a);
}
.gh-tien-dong > span.gh-mo {
  color: #94a3b8;
}
.gh-xanh,
.gh-tien-dong > span.gh-xanh {
  color: #15803d;
}
.gh-free-ship {
  display: flex;
  gap: 10px;
  padding: 12px;
  font-size: 0.85rem;
  color: #1e3a8a;
  background: #eff6ff;
  border-radius: 14px;
}
.gh-free-ship i {
  font-size: 1.1rem;
}
.gh-tong {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin: 18px 0 20px;
  padding-top: 18px;
  font-weight: 700;
  color: var(--ft-ink, #0f172a);
  border-top: 1px dashed #cbd5e1;
}
.gh-tong strong {
  font-size: 1.6rem;
  font-weight: 800;
  letter-spacing: -0.02em;
  color: #e11d48;
}
.gh-loi {
  margin-bottom: 14px;
  padding: 10px 12px;
  font-size: 0.85rem;
  color: #b91c1c;
  background: #fef2f2;
  border-radius: 12px;
}
.gh-nut-chinh {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 54px;
  padding: 0 28px;
  font-size: 1rem;
  font-weight: 700;
  color: #fff;
  background: var(--ft-primary, #1d4ed8);
  border: 0;
  border-radius: 999px;
  box-shadow: 0 10px 24px rgba(29, 78, 216, 0.25);
}
.gh-nut-chinh:hover:not(:disabled) {
  color: #fff;
  background: #1e40af;
}
.gh-nut-chinh:disabled {
  color: #fff;
  background: #94a3b8;
  box-shadow: none;
}
.gh-an-toan {
  margin-top: 12px;
  font-size: 0.8rem;
  text-align: center;
  color: #64748b;
}
@media (max-width: 1199px) {
  .gh-luoi {
    grid-template-columns: minmax(0, 1fr);
  }
  .gh-tom-tat {
    position: static;
  }
}
@media (max-width: 767px) {
  .gh-tieu-de {
    font-size: 1.6rem;
  }
  .gh-ds {
    padding: 4px 14px 16px;
    border-radius: 20px;
  }
  .gh-dong {
    position: relative;
    grid-template-columns: 76px minmax(0, 1fr) auto;
    grid-template-areas:
      'anh tin tin'
      'anh gia gia'
      'anh sl tt';
    gap: 6px 12px;
    align-items: start;
  }
  .gh-anh {
    grid-area: anh;
    width: 76px;
    height: 76px;
  }
  .gh-thong-tin {
    grid-area: tin;
  }
  .gh-don-gia {
    grid-area: gia;
    display: flex;
    gap: 8px;
    align-items: baseline;
    text-align: left;
  }
  .gh-so-luong {
    grid-area: sl;
    justify-self: start;
  }
  .gh-tam-tinh {
    grid-area: tt;
    align-self: center;
  }
  .gh-thong-tin {
    padding-right: 30px;
  }
  .gh-xoa {
    position: absolute;
    top: 14px;
    right: 0;
  }
  .gh-tom-tat {
    padding: 20px;
    border-radius: 20px;
  }
}
</style>
