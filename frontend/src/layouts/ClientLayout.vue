<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../stores/cart'
import '../assets/client.css'

const router = useRouter()
const cartStore = useCartStore()

const searchQuery = ref('')
const showProductDropdown = ref(false)
const showGuideDropdown = ref(false)

function handleSearch() {
  if (searchQuery.value.trim()) {
    router.push({ path: '/shop/san-pham', query: { search: searchQuery.value.trim() } })
  } else {
    router.push({ path: '/shop/san-pham' })
  }
}
</script>

<template>
  <div class="client-app">
    <!-- Top Header theo chuẩn giao diện mẫu -->
    <header class="vnb-top-header">
      <div class="vnb-container d-flex align-items-center justify-content-between gap-3 flex-nowrap">
        <!-- Logo -->
        <RouterLink to="/shop" class="d-flex align-items-center text-decoration-none flex-shrink-0">
          <img src="/logo.png" alt="FootStyle" class="vnb-logo" />
        </RouterLink>

        <!-- Hotline -->
        <a href="tel:0977508430" class="vnb-hotline text-decoration-none flex-shrink-0 d-none d-lg-flex" title="Bấm để gọi hotline hỗ trợ">
          <i class="bi bi-headset text-danger" style="font-size:1.2rem;"></i>
          <span>HOTLINE: <strong class="vnb-hotline-num">0977508430</strong></span>
        </a>

        <!-- Hệ thống cửa hàng -->
        <RouterLink to="/shop/lien-he" class="vnb-store-loc flex-shrink-0 d-none d-md-flex text-decoration-none" title="Xem danh sách chi nhánh FootStyle">
          <i class="bi bi-geo-alt-fill text-danger" style="font-size:1.1rem;"></i>
          <span>HỆ THỐNG CỬA HÀNG</span>
        </RouterLink>

        <!-- Ô Tìm kiếm -->
        <div class="vnb-search-wrap flex-grow-1" style="min-width:160px; max-width:340px;">
          <form @submit.prevent="handleSearch">
            <input
              type="text"
              v-model="searchQuery"
              class="form-control vnb-search-input"
              placeholder="Tìm sản phẩm..."
            />
            <button type="submit" class="vnb-search-icon-btn" aria-label="Tìm kiếm">
              <i class="bi bi-search"></i>
            </button>
          </form>
        </div>

        <!-- 3 Nút tròn: Tra cứu, Tài khoản, Giỏ hàng -->
        <div class="d-flex align-items-center gap-3 flex-shrink-0">
          <!-- Tra cứu đơn hàng -->
          <RouterLink to="/shop/tra-cuu" class="vnb-header-action" title="Tra cứu tiến độ đơn hàng">
            <div class="vnb-action-icon-circle">
              <i class="bi bi-binoculars-fill"></i>
            </div>
            <span>TRA CỨU</span>
          </RouterLink>

          <!-- Tài khoản cá nhân -->
          <RouterLink to="/shop/tai-khoan" class="vnb-header-action" title="Hồ sơ tài khoản & sổ địa chỉ">
            <div class="vnb-action-icon-circle">
              <i class="bi bi-person-fill"></i>
            </div>
            <span>TÀI KHOẢN</span>
          </RouterLink>

          <!-- Giỏ hàng -->
          <RouterLink to="/shop/gio-hang" class="vnb-header-action" title="Xem giỏ hàng">
            <div class="vnb-action-icon-circle">
              <i class="bi bi-cart3"></i>
              <span v-if="cartStore.totalCount > 0" class="vnb-cart-badge">
                {{ cartStore.totalCount }}
              </span>
              <span v-else class="vnb-cart-badge">0</span>
            </div>
            <span>GIỎ HÀNG</span>
          </RouterLink>
        </div>
      </div>
    </header>

    <!-- Navigation Bar màu xanh dương VNB / FootStyle -->
    <nav class="vnb-nav-bar">
      <div class="vnb-container d-flex justify-content-between align-items-center">
        <ul class="vnb-nav-list flex-grow-1" style="justify-content: space-evenly;">
          <!-- 1. TRANG CHỦ -->
          <li class="vnb-nav-item">
            <RouterLink to="/shop" class="vnb-nav-link">TRANG CHỦ</RouterLink>
          </li>

          <!-- 2. SẢN PHẨM (DROPDOWN) -->
          <li
            class="vnb-nav-item dropdown"
            @mouseenter="showProductDropdown = true"
            @mouseleave="showProductDropdown = false"
          >
            <RouterLink to="/shop/san-pham" class="vnb-nav-link d-flex align-items-center gap-1">
              SẢN PHẨM <i class="bi bi-chevron-down small" style="font-size: 0.7rem;"></i>
            </RouterLink>
            <ul
              class="dropdown-menu shadow rounded-3 border-0 m-0 py-2"
              :class="{ show: showProductDropdown }"
              style="font-size: 0.9rem;"
            >
              <li>
                <RouterLink to="/shop/san-pham" class="dropdown-item py-2 fw-semibold" @click="showProductDropdown = false">
                  Tất cả sản phẩm
                </RouterLink>
              </li>
              <li><hr class="dropdown-divider"></li>
              <li>
                <RouterLink :to="{ path: '/shop/san-pham', query: { search: 'Vợt' } }" class="dropdown-item py-2" @click="showProductDropdown = false">
                  <i class="bi bi-crosshair text-primary me-2"></i> Vợt cầu lông
                </RouterLink>
              </li>
              <li>
                <RouterLink :to="{ path: '/shop/san-pham', query: { search: 'Giày' } }" class="dropdown-item py-2" @click="showProductDropdown = false">
                  <i class="bi bi-lightning-charge text-danger me-2"></i> Giày cầu lông
                </RouterLink>
              </li>
              <li>
                <RouterLink :to="{ path: '/shop/san-pham', query: { search: 'Áo' } }" class="dropdown-item py-2" @click="showProductDropdown = false">
                  <i class="bi bi-tsunami text-success me-2"></i> Áo cầu lông
                </RouterLink>
              </li>
              <li>
                <RouterLink :to="{ path: '/shop/san-pham', query: { search: 'Váy' } }" class="dropdown-item py-2" @click="showProductDropdown = false">
                  <i class="bi bi-flower1 text-warning me-2"></i> Váy cầu lông
                </RouterLink>
              </li>
              <li>
                <RouterLink :to="{ path: '/shop/san-pham', query: { search: 'Quần' } }" class="dropdown-item py-2" @click="showProductDropdown = false">
                  <i class="bi bi-columns-gap text-info me-2"></i> Quần cầu lông
                </RouterLink>
              </li>
              <li>
                <RouterLink :to="{ path: '/shop/san-pham', query: { search: 'Cước' } }" class="dropdown-item py-2" @click="showProductDropdown = false">
                  <i class="bi bi-backpack text-secondary me-2"></i> Phụ kiện & Cước đan
                </RouterLink>
              </li>
            </ul>
          </li>

          <!-- 3. SALE OFF -->
          <li class="vnb-nav-item">
            <RouterLink :to="{ path: '/shop/san-pham', query: { khuyenMai: '1' } }" class="vnb-nav-link text-warning fw-black">
              SALE OFF
            </RouterLink>
          </li>

          <!-- 4. TIN TỨC -->
          <li class="vnb-nav-item">
            <RouterLink to="/shop/tin-tuc" class="vnb-nav-link">TIN TỨC</RouterLink>
          </li>

          <!-- 5. TUYỂN DỤNG -->
          <li class="vnb-nav-item">
            <RouterLink to="/shop/tuyen-dung" class="vnb-nav-link">TUYỂN DỤNG</RouterLink>
          </li>

          <!-- 6. CHÍNH SÁCH NHƯỢNG QUYỀN -->
          <li class="vnb-nav-item d-none d-xl-inline-block">
            <RouterLink to="/shop/nhuong-quyen" class="vnb-nav-link">CHÍNH SÁCH NHƯỢNG QUYỀN</RouterLink>
          </li>

          <!-- 7. HƯỚNG DẪN (DROPDOWN) -->
          <li
            class="vnb-nav-item dropdown"
            @mouseenter="showGuideDropdown = true"
            @mouseleave="showGuideDropdown = false"
          >
            <RouterLink to="/shop/huong-dan" class="vnb-nav-link d-flex align-items-center gap-1">
              HƯỚNG DẪN <i class="bi bi-chevron-down small" style="font-size: 0.7rem;"></i>
            </RouterLink>
            <ul
              class="dropdown-menu shadow rounded-3 border-0 m-0 py-2"
              :class="{ show: showGuideDropdown }"
              style="font-size: 0.9rem;"
            >
              <li>
                <RouterLink to="/shop/huong-dan" class="dropdown-item py-2 fw-semibold" @click="showGuideDropdown = false">
                  Trung tâm hướng dẫn
                </RouterLink>
              </li>
              <li><hr class="dropdown-divider"></li>
              <li>
                <RouterLink :to="{ path: '/shop/huong-dan', query: { tab: 'mua-hang' } }" class="dropdown-item py-2" @click="showGuideDropdown = false">
                  <i class="bi bi-cart-check text-primary me-2"></i> Hướng dẫn mua hàng & thanh toán
                </RouterLink>
              </li>
              <li>
                <RouterLink :to="{ path: '/shop/huong-dan', query: { tab: 'chon-vot' } }" class="dropdown-item py-2" @click="showGuideDropdown = false">
                  <i class="bi bi-crosshair text-danger me-2"></i> Hướng dẫn chọn vợt cầu lông
                </RouterLink>
              </li>
              <li>
                <RouterLink :to="{ path: '/shop/huong-dan', query: { tab: 'size-giay' } }" class="dropdown-item py-2" @click="showGuideDropdown = false">
                  <i class="bi bi-rulers text-success me-2"></i> Bảng quy đổi size giày
                </RouterLink>
              </li>
              <li>
                <RouterLink :to="{ path: '/shop/huong-dan', query: { tab: 'bao-hanh' } }" class="dropdown-item py-2" @click="showGuideDropdown = false">
                  <i class="bi bi-shield-check text-warning me-2"></i> Chính sách bảo hành & đổi trả
                </RouterLink>
              </li>
            </ul>
          </li>

          <!-- 8. GIỚI THIỆU -->
          <li class="vnb-nav-item d-none d-lg-inline-block">
            <RouterLink to="/shop/gioi-thieu" class="vnb-nav-link">GIỚI THIỆU</RouterLink>
          </li>

          <!-- 9. LIÊN HỆ -->
          <li class="vnb-nav-item">
            <RouterLink to="/shop/lien-he" class="vnb-nav-link">LIÊN HỆ</RouterLink>
          </li>
        </ul>
      </div>
    </nav>

    <!-- Main Content -->
    <main class="flex-grow-1 pb-5">
      <RouterView />
    </main>

    <!-- Footer -->
    <footer class="bg-dark text-white pt-5 pb-4 mt-auto" style="border-top: 4px solid var(--vnb-orange);">
      <div class="vnb-container">
        <div class="row g-4 mb-4">
          <div class="col-lg-4 col-md-6">
            <img src="/logo.png" alt="FootStyle" class="mb-3" style="height: 42px; filter: brightness(0) invert(1);" />
            <p class="small text-secondary mb-3">
              FootStyle - Hệ thống shop cầu lông toàn quốc chuyên phân phối vợt cầu lông, giày, trang phục và phụ kiện thể thao chính hãng.
            </p>
            <div class="small text-secondary">
              <p class="mb-1"><i class="bi bi-geo-alt-fill text-warning me-2"></i> Trụ sở: 123 Đường Cầu Diễn, Bắc Từ Liêm, Hà Nội</p>
              <p class="mb-1">
                <i class="bi bi-telephone-fill text-warning me-2"></i> Hotline: 
                <a href="tel:0977508430" class="text-white text-decoration-none fw-bold">0977508430</a> (8:00 - 21:30)
              </p>
              <p class="mb-0"><i class="bi bi-envelope-fill text-warning me-2"></i> Email: cskh@footstyle.vn</p>
            </div>
          </div>

          <div class="col-lg-2 col-md-6 col-6">
            <h6 class="text-uppercase fw-bold text-warning mb-3">Chính sách</h6>
            <ul class="list-unstyled small d-flex flex-column gap-2 text-secondary">
              <li>
                <RouterLink :to="{ path: '/shop/huong-dan', query: { tab: 'bao-hanh' } }" class="text-secondary text-decoration-none hover-white">
                  Chính sách bảo hành
                </RouterLink>
              </li>
              <li>
                <RouterLink :to="{ path: '/shop/huong-dan', query: { tab: 'bao-hanh' } }" class="text-secondary text-decoration-none hover-white">
                  Đổi trả trong 7 ngày
                </RouterLink>
              </li>
              <li>
                <RouterLink :to="{ path: '/shop/huong-dan', query: { tab: 'mua-hang' } }" class="text-secondary text-decoration-none hover-white">
                  Chính sách giao hàng
                </RouterLink>
              </li>
              <li>
                <RouterLink :to="{ path: '/shop/huong-dan', query: { tab: 'mua-hang' } }" class="text-secondary text-decoration-none hover-white">
                  Bảo mật thông tin
                </RouterLink>
              </li>
            </ul>
          </div>

          <div class="col-lg-3 col-md-6 col-6">
            <h6 class="text-uppercase fw-bold text-warning mb-3">Hỗ trợ khách hàng</h6>
            <ul class="list-unstyled small d-flex flex-column gap-2 text-secondary">
              <li><RouterLink to="/shop/tra-cuu" class="text-secondary text-decoration-none hover-white">Tra cứu đơn hàng</RouterLink></li>
              <li>
                <RouterLink :to="{ path: '/shop/huong-dan', query: { tab: 'chon-vot' } }" class="text-secondary text-decoration-none hover-white">
                  Hướng dẫn chọn vợt
                </RouterLink>
              </li>
              <li>
                <RouterLink :to="{ path: '/shop/huong-dan', query: { tab: 'size-giay' } }" class="text-secondary text-decoration-none hover-white">
                  Bảng quy đổi size giày
                </RouterLink>
              </li>
              <li>
                <RouterLink to="/shop/huong-dan" class="text-secondary text-decoration-none hover-white">
                  Câu hỏi thường gặp
                </RouterLink>
              </li>
            </ul>
          </div>

          <div class="col-lg-3 col-md-6">
            <h6 class="text-uppercase fw-bold text-warning mb-3">Thanh toán & Vận chuyển</h6>
            <div class="d-flex flex-wrap gap-2 mb-3">
              <span class="badge bg-secondary p-2">COD Tiền mặt</span>
              <span class="badge bg-secondary p-2">VietQR Banking</span>
            </div>
            <div class="small text-secondary mb-3">
              Vận chuyển hỏa tốc toàn quốc qua GHTK, GHN, ViettelPost.
            </div>
            <div>
              <RouterLink to="/shop/nhuong-quyen" class="btn btn-outline-warning btn-sm rounded-pill fw-bold">
                Hợp tác nhượng quyền →
              </RouterLink>
            </div>
          </div>
        </div>

        <div class="border-top border-secondary pt-3 text-center small text-secondary">
          © 2026 FootStyle Badminton Shop. Mọi quyền được bảo lưu.
        </div>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.hover-white:hover {
  color: #fff !important;
}

.fw-black {
  font-weight: 900;
}
</style>
