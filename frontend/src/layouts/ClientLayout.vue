<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../stores/cart'
import NutChatKhach from '../components/chat/NutChatKhach.vue'
import '../assets/client.css'

const router = useRouter()
const cartStore = useCartStore()

const searchQuery = ref('')
const showProductDropdown = ref(false)

// Dữ liệu bảng Mega Menu đầy đủ theo mẫu hình ảnh người dùng cung cấp
const megaMenuData = [
  // Hàng 1
  {
    key: 'vot-cau-long',
    title: 'VỢT CẦU LÔNG',
    search: 'Vợt',
    links: [
      'Vợt cầu lông Yonex',
      'Vợt cầu lông Victor',
      'Vợt cầu lông Lining',
      'Vợt Cầu Lông VS',
      'Vợt Cầu Lông Mizuno',
      'Vợt cầu lông Apacs',
      'Vợt Cầu Lông VNB',
      'Vợt cầu lông Proace',
      'Vợt cầu lông Forza',
      'Vợt Cầu Lông FlyPower',
    ],
    hasMore: true,
  },
  {
    key: 'giay-cau-long',
    title: 'GIÀY CẦU LÔNG',
    search: 'Giày',
    links: [
      'Giày cầu lông Yonex',
      'Giày cầu lông Victor',
      'Giày cầu lông Lining',
      'Giày cầu lông VS',
      'Giày cầu lông Kawasaki',
      'Giày Cầu Lông Mizuno',
      'Giày Cầu Lông Kumpoo',
      'Giày Cầu Lông Promax',
      'Giày cầu lông Babolat',
      'Giày Cầu Lông Sunbatta',
    ],
    hasMore: true,
  },
  {
    key: 'ao-cau-long',
    title: 'ÁO CẦU LÔNG',
    search: 'Áo',
    links: [
      'Áo cầu lông Yonex',
      'Áo cầu lông VNB',
      'Áo cầu lông Kamito',
      'Áo cầu lông VS',
      'Áo cầu lông Victor',
      'Áo cầu lông Lining',
      'Áo cầu lông DonexPro',
      'Áo Cầu Lông Alien Armour',
      'Áo thể thao SFD',
      'Áo cầu lông Kawasaki',
    ],
    hasMore: true,
  },
  {
    key: 'vay-cau-long',
    title: 'VÁY CẦU LÔNG',
    search: 'Váy',
    links: [
      'Váy cầu lông Yonex',
      'Váy cầu lông Victec',
      'Váy cầu lông Lining',
      'Váy cầu lông Donex Pro',
      'Váy cầu lông Victor',
      'Váy cầu lông Kamito',
      'Váy cầu lông Taro',
      'Váy cầu lông Vina Authentic',
      'Váy cầu lông Hundred',
    ],
    hasMore: false,
  },
  {
    key: 'quan-cau-long',
    title: 'QUẦN CẦU LÔNG',
    search: 'Quần',
    links: [
      'Quần cầu lông Yonex',
      'Quần cầu lông Victor',
      'Quần cầu lông Lining',
      'Quần cầu lông VNB',
      'Quần Cầu Lông SFD',
      'Quần cầu lông Donex Pro',
      'Quần Cầu Lông Apacs',
      'Quần cầu lông Alien Armour',
      'Quần cầu lông Mizuno',
      'Quần cầu lông Kawasaki',
    ],
    hasMore: true,
  },
  // Hàng 2
  {
    key: 'tui-vot-cau-long',
    title: 'TÚI VỢT CẦU LÔNG',
    search: 'Túi',
    links: [
      'Túi vợt cầu lông Yonex',
      'Túi đựng giày',
    ],
    hasMore: false,
  },
  {
    key: 'balo-cau-long',
    title: 'BALO CẦU LÔNG',
    search: 'Balo',
    links: [
      'Balo Cầu Lông Yonex',
      'Balo cầu lông VS',
    ],
    hasMore: false,
  },
  {
    key: 'phu-kien-cau-long',
    title: 'PHỤ KIỆN CẦU LÔNG',
    search: 'Phụ kiện',
    links: [
      'Vớ Cầu Lông',
      'Cước đan vợt cầu lông',
    ],
    hasMore: false,
  },
  {
    key: 'vot-pickleball',
    title: 'VỢT PICKLEBALL',
    search: 'Vợt Pickleball',
    links: [
      'Vợt PickleBall Head',
      'Vợt PickleBall Joola',
    ],
    hasMore: false,
  },
  {
    key: 'giay-pickleball',
    title: 'GIÀY PICKLEBALL',
    search: 'Giày Pickleball',
    links: [
      'Giày Pickleball Jogarbola',
      'Giày Pickleball Asics',
    ],
    hasMore: false,
  },
]

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

    <!-- Navigation Bar màu xanh dương FootStyle -->
    <div class="vnb-nav-wrapper">
      <nav class="vnb-nav-bar">
        <div class="vnb-container d-flex justify-content-between align-items-center">
          <ul class="vnb-nav-list flex-grow-1" style="justify-content: space-evenly;">
            <!-- 1. TRANG CHỦ -->
            <li class="vnb-nav-item">
              <RouterLink to="/shop" class="vnb-nav-link">TRANG CHỦ</RouterLink>
            </li>

            <!-- 2. GIỚI THIỆU -->
            <li class="vnb-nav-item">
              <RouterLink to="/shop/gioi-thieu" class="vnb-nav-link">GIỚI THIỆU</RouterLink>
            </li>

            <!-- 3. SẢN PHẨM (MEGA MENU) -->
            <li
              class="vnb-nav-item vnb-mega-trigger"
              @mouseenter="showProductDropdown = true"
              @mouseleave="showProductDropdown = false"
            >
              <RouterLink to="/shop/san-pham" class="vnb-nav-link d-flex align-items-center gap-1">
                SẢN PHẨM <i class="bi" :class="showProductDropdown ? 'bi-chevron-up' : 'bi-chevron-down'" style="font-size: 0.7rem;"></i>
              </RouterLink>
            </li>

            <!-- 4. ĐƠN HÀNG -->
            <li class="vnb-nav-item">
              <RouterLink :to="{ path: '/shop/tai-khoan', query: { tab: 'orders' } }" class="vnb-nav-link">
                ĐƠN HÀNG
              </RouterLink>
            </li>

            <!-- 5. TRA CỨU -->
            <li class="vnb-nav-item">
              <RouterLink to="/shop/tra-cuu" class="vnb-nav-link">TRA CỨU</RouterLink>
            </li>

            <!-- 6. GỢI Ý -->
            <li class="vnb-nav-item">
              <RouterLink to="/shop/goi-y-ai" class="vnb-nav-link">GỢI Ý</RouterLink>
            </li>

            <!-- 7. LIÊN HỆ -->
            <li class="vnb-nav-item">
              <RouterLink to="/shop/lien-he" class="vnb-nav-link">LIÊN HỆ</RouterLink>
            </li>
          </ul>
        </div>
      </nav>

      <!-- MEGA MENU PANEL (hover SẢN PHẨM) -->
      <div
        class="vnb-mega-menu"
        v-show="showProductDropdown"
        @mouseenter="showProductDropdown = true"
        @mouseleave="showProductDropdown = false"
      >
        <div class="vnb-container">
          <div class="vnb-mega-grid">
            <div
              v-for="cat in megaMenuData"
              :key="cat.title"
              class="vnb-mega-col"
            >
              <RouterLink
                :to="{ path: '/shop/san-pham', query: { danhMuc: cat.key } }"
                class="vnb-mega-title"
                @click="showProductDropdown = false"
              >
                {{ cat.title }}
              </RouterLink>
              <ul class="vnb-mega-links">
                <li v-for="link in cat.links" :key="link">
                  <RouterLink
                    :to="{ path: '/shop/san-pham', query: { danhMuc: cat.key, search: link } }"
                    class="vnb-mega-link"
                    @click="showProductDropdown = false"
                  >
                    {{ link }}
                  </RouterLink>
                </li>
                <li v-if="cat.hasMore">
                  <RouterLink
                    :to="{ path: '/shop/san-pham', query: { danhMuc: cat.key } }"
                    class="vnb-mega-more"
                    @click="showProductDropdown = false"
                  >
                    Xem thêm
                  </RouterLink>
                </li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>

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

    <!-- Chat AI: trợ lý ảo trả lời từ dữ liệu cửa hàng, khách bấm "Gặp nhân viên" để chuyển sang trang Hỗ trợ khách hàng của nhân viên -->
    <NutChatKhach />
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
