<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../stores/cart'
import { useAuth } from '../composables/useAuth'
import { formatMoney } from '../utils/format'
import { cuaHangService } from '../services/cuaHangService'
import NutChatKhach from '../components/chat/NutChatKhach.vue'
import '../assets/client.css'

const router = useRouter()

// Đang ở trang sản phẩm khuyến mãi (SALE OFF): chỉ mục SALE OFF được gạch chân, mục SẢN PHẨM thì không (cùng đường dẫn nên router tính cả hai là "active")
const dangXemKhuyenMai = computed(() => !!router.currentRoute.value.query.khuyenMai)
const cartStore = useCartStore()
const auth = useAuth()

const searchQuery = ref('')
const showProductDropdown = ref(false)
const showGuideDropdown = ref(false)
const showLookupDropdown = ref(false)
const showAccountDropdown = ref(false)
const showCartDropdown = ref(false)

// Danh mục và thương hiệu thật từ CSDL
const danhMucList = ref([])
const thuongHieuList = ref([])

onMounted(async () => {
  try {
    const res = await cuaHangService.getDanhMucVaThuongHieu()
    if (res?.danhMuc?.length > 0) {
      danhMucList.value = res.danhMuc
    }
    if (res?.thuongHieu?.length > 0) {
      thuongHieuList.value = res.thuongHieu
    }
  } catch (err) {
    console.error('Lỗi khi tải danh mục client layout:', err)
  }
})

// Dữ liệu menu sản phẩm chỉ lấy từ CSDL thật (loại bỏ hoàn toàn tất cả danh mục ảo và biến thể ảo)
const menuCategories = computed(() => {
  const dms = danhMucList.value.length > 0 ? danhMucList.value : [
    { id: 1, ten: 'Vợt cầu lông' },
    { id: 2, ten: 'Vợt cầu lông người mới' },
    { id: 3, ten: 'Quần áo thể thao' },
    { id: 4, ten: 'Túi - balo' },
    { id: 5, ten: 'Phụ kiện' },
    { id: 6, ten: 'Quả cầu lông' },
  ]
  const ths = thuongHieuList.value.length > 0 ? thuongHieuList.value : [
    { id: 1, ten: 'Yonex' },
    { id: 2, ten: 'Lining' },
    { id: 3, ten: 'Victor' },
    { id: 4, ten: 'Mizuno' },
    { id: 5, ten: 'Kumpoo' },
  ]

  return dms.map((dm) => ({
    id: dm.id,
    title: (dm.ten || '').toUpperCase(),
    query: { danhMuc: dm.id },
    links: ths.map((th) => ({
      label: `${dm.ten} ${th.ten}`,
      query: { danhMuc: dm.id, thuongHieu: th.id },
    })),
  }))
})

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
          <i class="bi bi-headset text-primary" style="font-size:1.2rem;"></i>
          <span>HOTLINE: <strong class="vnb-hotline-num text-primary">0977508430</strong></span>
        </a>

        <!-- Hệ thống cửa hàng -->
        <RouterLink to="/shop/lien-he" class="vnb-store-loc flex-shrink-0 d-none d-md-flex text-decoration-none" title="Xem danh sách chi nhánh FootStyle">
          <i class="bi bi-geo-alt-fill text-primary" style="font-size:1.1rem;"></i>
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
          <!-- Tra cứu đơn hàng & bảo hành: Hover hiện popover -->
          <div
            class="vnb-lookup-dropdown-wrap"
            @mouseenter="showLookupDropdown = true"
            @mouseleave="showLookupDropdown = false"
          >
            <RouterLink to="/shop/tra-cuu" class="vnb-header-action" title="Tra cứu đơn hàng & bảo hành">
              <div class="vnb-action-icon-circle">
                <i class="bi bi-binoculars-fill"></i>
              </div>
              <span>TRA CỨU</span>
            </RouterLink>

            <!-- Popup menu khi di chuột -->
            <div
              class="vnb-lookup-popover"
              :class="{ show: showLookupDropdown }"
              @mouseenter="showLookupDropdown = true"
              @mouseleave="showLookupDropdown = false"
            >
              <div class="vnb-lookup-arrow"></div>
              <ul class="vnb-lookup-menu">
                <li>
                  <RouterLink
                    to="/shop/tra-cuu"
                    class="vnb-lookup-item"
                    @click="showLookupDropdown = false"
                  >
                    Kiểm tra đơn hàng
                  </RouterLink>
                </li>
                <li>
                  <RouterLink
                    :to="{ path: '/shop/huong-dan', query: { tab: 'bao-hanh' } }"
                    class="vnb-lookup-item"
                    @click="showLookupDropdown = false"
                  >
                    Kiểm tra bảo hành
                  </RouterLink>
                </li>
              </ul>
            </div>
          </div>

          <!-- Tài khoản cá nhân: Hover hiện popover Đăng nhập / Đăng ký -->
          <div
            class="vnb-lookup-dropdown-wrap"
            @mouseenter="showAccountDropdown = true"
            @mouseleave="showAccountDropdown = false"
          >
            <RouterLink
              :to="auth.daDangNhap.value ? '/shop/tai-khoan' : '/shop/dang-nhap'"
              class="vnb-header-action"
              title="Tài khoản cá nhân"
            >
              <div class="vnb-action-icon-circle">
                <i class="bi bi-person-fill"></i>
              </div>
              <span>TÀI KHOẢN</span>
            </RouterLink>

            <!-- Popup menu khi di chuột -->
            <div
              class="vnb-lookup-popover"
              :class="{ show: showAccountDropdown }"
              @mouseenter="showAccountDropdown = true"
              @mouseleave="showAccountDropdown = false"
            >
              <div class="vnb-lookup-arrow"></div>
              <ul class="vnb-lookup-menu">
                <template v-if="!auth.daDangNhap.value">
                  <li>
                    <RouterLink
                      to="/shop/dang-nhap"
                      class="vnb-lookup-item d-flex align-items-center gap-2"
                      @click="showAccountDropdown = false"
                    >
                      <i class="bi bi-box-arrow-in-right fs-5 text-secondary"></i>
                      <span>Đăng nhập</span>
                    </RouterLink>
                  </li>
                  <li>
                    <RouterLink
                      :to="{ path: '/shop/dang-nhap', query: { tab: 'dang-ky' } }"
                      class="vnb-lookup-item d-flex align-items-center gap-2"
                      @click="showAccountDropdown = false"
                    >
                      <i class="bi bi-person-plus fs-5 text-secondary"></i>
                      <span>Đăng ký</span>
                    </RouterLink>
                  </li>
                </template>
                <template v-else>
                  <li>
                    <RouterLink
                      to="/shop/tai-khoan"
                      class="vnb-lookup-item d-flex align-items-center gap-2"
                      @click="showAccountDropdown = false"
                    >
                      <i class="bi bi-person-circle fs-5 text-primary"></i>
                      <span>{{ auth.trangThai.nguoiDung?.hoTen || 'Tài khoản của tôi' }}</span>
                    </RouterLink>
                  </li>
                  <li>
                    <RouterLink
                      :to="{ path: '/shop/tai-khoan', query: { tab: 'orders' } }"
                      class="vnb-lookup-item d-flex align-items-center gap-2"
                      @click="showAccountDropdown = false"
                    >
                      <i class="bi bi-bag-check fs-5 text-success"></i>
                      <span>Đơn hàng của tôi</span>
                    </RouterLink>
                  </li>
                  <li>
                    <a
                      href="#"
                      class="vnb-lookup-item d-flex align-items-center gap-2 text-danger"
                      @click.prevent="auth.dangXuat(); showAccountDropdown = false; router.push('/shop')"
                    >
                      <i class="bi bi-box-arrow-right fs-5"></i>
                      <span>Đăng xuất</span>
                    </a>
                  </li>
                </template>
              </ul>
            </div>
          </div>

          <!-- Giỏ hàng: Hover hiện popover xem nhanh giỏ hàng -->
          <div
            class="vnb-lookup-dropdown-wrap"
            @mouseenter="showCartDropdown = true"
            @mouseleave="showCartDropdown = false"
          >
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

            <!-- Popup giỏ hàng khi di chuột -->
            <div
              class="vnb-cart-popover"
              :class="{ show: showCartDropdown }"
              @mouseenter="showCartDropdown = true"
              @mouseleave="showCartDropdown = false"
            >
              <div class="vnb-cart-arrow"></div>

              <!-- Trường hợp giỏ hàng trống (chuẩn hình ảnh người dùng cung cấp) -->
              <div v-if="cartStore.totalCount === 0" class="text-center py-4 px-3">
                <svg width="86" height="86" viewBox="0 0 100 100" fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" class="mb-3 text-dark d-inline-block">
                  <!-- Hộp kiện nhô ra từ miệng túi -->
                  <rect x="27" y="24" width="22" height="24" rx="2" transform="rotate(-8 27 24)" fill="#fff" stroke="#111827" stroke-width="3"/>
                  <rect x="40" y="20" width="22" height="26" rx="2" fill="#fff" stroke="#111827" stroke-width="3"/>
                  <rect x="52" y="26" width="20" height="22" rx="2" transform="rotate(12 52 26)" fill="#fff" stroke="#111827" stroke-width="3"/>
                  <!-- Thân túi mua sắm -->
                  <path d="M22 45 L18 88 L82 88 L78 45 Z" fill="#fff" stroke="#111827" stroke-width="3.2"/>
                  <!-- Quai túi chữ U -->
                  <path d="M37 53 C37 70, 63 70, 63 53" fill="none" stroke="#111827" stroke-width="3.2"/>
                  <!-- Lỗ xỏ quai -->
                  <circle cx="37" cy="53" r="3" fill="#fff" stroke="#111827" stroke-width="2.6"/>
                  <circle cx="63" cy="53" r="3" fill="#fff" stroke="#111827" stroke-width="2.6"/>
                </svg>
                <p class="mb-0 text-dark" style="font-size: 0.92rem; font-weight: 500;">
                  Không có sản phẩm nào trong giỏ hàng của bạn
                </p>
              </div>

              <!-- Trường hợp có sản phẩm trong giỏ -->
              <div v-else class="p-3">
                <div class="small fw-bold text-dark mb-2 border-bottom pb-2 d-flex justify-content-between align-items-center">
                  <span>Sản phẩm vừa thêm ({{ cartStore.totalCount }})</span>
                  <RouterLink to="/shop/gio-hang" class="text-primary text-decoration-none small" @click="showCartDropdown = false">
                    Xem tất cả
                  </RouterLink>
                </div>
                <div class="vnb-cart-items-preview" style="max-height: 220px; overflow-y: auto;">
                  <div
                    v-for="(item, idx) in cartStore.items.slice(0, 4)"
                    :key="idx"
                    class="d-flex align-items-center gap-2 py-2 border-bottom"
                  >
                    <img
                      :src="item.anh || '/logo.png'"
                      class="rounded border bg-light"
                      style="width: 44px; height: 44px; object-fit: contain;"
                    />
                    <div class="flex-grow-1 overflow-hidden" style="font-size: 0.82rem;">
                      <div class="text-truncate fw-semibold text-dark">{{ item.sanPhamTen }}</div>
                      <div class="text-muted small">
                        SL: {{ item.soLuong }} × <span class="text-danger fw-bold">{{ formatMoney(item.giaBan) }}</span>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="d-flex justify-content-between align-items-center pt-3 pb-3">
                  <span class="text-muted small">Tổng tiền tạm tính:</span>
                  <span class="fw-bold text-danger fs-6">{{ formatMoney(cartStore.subTotal) }}</span>
                </div>
                <RouterLink
                  to="/shop/gio-hang"
                  class="btn btn-primary w-100 py-2 fw-bold rounded-2 text-uppercase"
                  style="font-size: 0.84rem;"
                  @click="showCartDropdown = false"
                >
                  Xem Giỏ Hàng
                </RouterLink>
              </div>
            </div>
          </div>
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

            <!-- 2. SẢN PHẨM (MEGA MENU) -->
            <li
              class="vnb-nav-item vnb-mega-trigger"
              @mouseenter="showProductDropdown = true"
              @mouseleave="showProductDropdown = false"
            >
              <RouterLink
                to="/shop/san-pham"
                class="vnb-nav-link d-flex align-items-center gap-1"
                :class="{ 'vnb-no-active': dangXemKhuyenMai }"
              >
                SẢN PHẨM <i class="bi" :class="showProductDropdown ? 'bi-chevron-up' : 'bi-chevron-down'" style="font-size: 0.7rem;"></i>
              </RouterLink>
            </li>

            <!-- 3. SALE OFF -->
            <li class="vnb-nav-item">
              <RouterLink
                :to="{ path: '/shop/san-pham', query: { khuyenMai: '1' } }"
                class="vnb-nav-link"
                :class="{ 'vnb-no-active': !dangXemKhuyenMai }"
              >
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
            <li class="vnb-nav-item">
              <RouterLink to="/shop/nhuong-quyen" class="vnb-nav-link">CHÍNH SÁCH NHƯỢNG QUYỀN</RouterLink>
            </li>

            <!-- 7. HƯỚNG DẪN (DROPDOWN) -->
            <li
              class="vnb-nav-item dropdown"
              @mouseenter="showGuideDropdown = true"
              @mouseleave="showGuideDropdown = false"
            >
              <RouterLink to="/shop/huong-dan" class="vnb-nav-link d-flex align-items-center gap-1">
                HƯỚNG DẪN <i class="bi" :class="showGuideDropdown ? 'bi-chevron-up' : 'bi-chevron-down'" style="font-size: 0.7rem;"></i>
              </RouterLink>
              <ul class="vnb-guide-menu" :class="{ show: showGuideDropdown }">
                <li class="vnb-guide-menu-li">
                  <RouterLink to="/shop/huong-dan" class="vnb-guide-link fw-bold" @click="showGuideDropdown = false">
                    Trung tâm hướng dẫn
                  </RouterLink>
                </li>
                <li class="vnb-guide-menu-li">
                  <RouterLink :to="{ path: '/shop/huong-dan', query: { tab: 'thanh-toan' } }" class="vnb-guide-link" @click="showGuideDropdown = false">
                    Hướng dẫn thanh toán
                  </RouterLink>
                </li>
                <li class="vnb-guide-menu-li">
                  <RouterLink :to="{ path: '/shop/huong-dan', query: { tab: 'mua-hang' } }" class="vnb-guide-link" @click="showGuideDropdown = false">
                    Hướng dẫn mua hàng
                  </RouterLink>
                </li>
                <li class="vnb-guide-menu-li">
                  <RouterLink :to="{ path: '/shop/huong-dan', query: { tab: 'chon-vot' } }" class="vnb-guide-link" @click="showGuideDropdown = false">
                    Hướng dẫn chọn vợt cầu lông
                  </RouterLink>
                </li>
                <li class="vnb-guide-menu-li">
                  <RouterLink :to="{ path: '/shop/huong-dan', query: { tab: 'size-giay' } }" class="vnb-guide-link" @click="showGuideDropdown = false">
                    Bảng quy đổi size giày
                  </RouterLink>
                </li>
                <li class="vnb-guide-menu-li">
                  <RouterLink :to="{ path: '/shop/huong-dan', query: { tab: 'bao-hanh' } }" class="vnb-guide-link" @click="showGuideDropdown = false">
                    Chính sách bảo hành & đổi trả
                  </RouterLink>
                </li>
              </ul>
            </li>

            <!-- 8. GIỚI THIỆU -->
            <li class="vnb-nav-item">
              <RouterLink to="/shop/gioi-thieu" class="vnb-nav-link">GIỚI THIỆU</RouterLink>
            </li>

            <!-- 9. LIÊN HỆ -->
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
              v-for="cat in menuCategories"
              :key="cat.id"
              class="vnb-mega-col"
            >
              <RouterLink
                :to="{ path: '/shop/san-pham', query: cat.query }"
                class="vnb-mega-title"
                @click="showProductDropdown = false"
              >
                {{ cat.title }}
              </RouterLink>
              <ul class="vnb-mega-links">
                <li v-for="link in cat.links" :key="link.label">
                  <RouterLink
                    :to="{ path: '/shop/san-pham', query: link.query }"
                    class="vnb-mega-link"
                    @click="showProductDropdown = false"
                  >
                    {{ link.label }}
                  </RouterLink>
                </li>
                <li>
                  <RouterLink
                    :to="{ path: '/shop/san-pham', query: cat.query }"
                    class="vnb-mega-more"
                    @click="showProductDropdown = false"
                  >
                    Xem thêm
                  </RouterLink>
                </li>
              </ul>
            </div>

            <!-- Cột thương hiệu chính hãng thật -->
            <div v-if="thuongHieuList && thuongHieuList.length > 0" class="vnb-mega-col">
              <RouterLink
                to="/shop/san-pham"
                class="vnb-mega-title"
                @click="showProductDropdown = false"
              >
                THƯƠNG HIỆU
              </RouterLink>
              <ul class="vnb-mega-links">
                <li v-for="brand in thuongHieuList" :key="brand.id">
                  <RouterLink
                    :to="{ path: '/shop/san-pham', query: { thuongHieu: brand.id } }"
                    class="vnb-mega-link"
                    @click="showProductDropdown = false"
                  >
                    {{ brand.ten }}
                  </RouterLink>
                </li>
                <li>
                  <RouterLink
                    to="/shop/san-pham"
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
