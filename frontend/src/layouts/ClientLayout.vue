<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../stores/cart'
import { useAuth } from '../composables/useAuth'
import { formatMoney } from '../utils/format'
import { cuaHangService } from '../services/cuaHangService'
import NutChatKhach from '../components/chat/NutChatKhach.vue'
import '../assets/client.css'

const router = useRouter()

const cartStore = useCartStore()
const auth = useAuth()

const searchQuery = ref('')
const showAccountDropdown = ref(false)
const showCartDropdown = ref(false)

// ===== Gợi ý tìm kiếm =====
const showSuggest = ref(false)
const activeSuggest = ref(-1)
const allProducts = ref([])
let productsLoaded = false

function boDau(s) {
  return String(s || '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .toLowerCase()
}

async function taiSanPhamChoGoiY() {
  if (productsLoaded) return
  productsLoaded = true
  try {
    allProducts.value = (await cuaHangService.getDanhSachSanPham()) || []
  } catch {
    productsLoaded = false
  }
}

const suggestions = computed(() => {
  const kw = boDau(searchQuery.value).trim()
  if (!kw) return []
  const words = kw.split(/\s+/)
  return allProducts.value
    .filter((sp) => {
      const hay = boDau(`${sp.ten} ${sp.brandName} ${sp.tenDanhMuc} ${sp.ma}`)
      return words.every((w) => hay.includes(w))
    })
    .sort((a, b) => Number(boDau(b.ten).startsWith(kw)) - Number(boDau(a.ten).startsWith(kw)))
    .slice(0, 6)
})

watch(searchQuery, () => {
  activeSuggest.value = -1
  showSuggest.value = !!searchQuery.value.trim()
  taiSanPhamChoGoiY()
})

function moGoiY() {
  taiSanPhamChoGoiY()
  if (searchQuery.value.trim()) showSuggest.value = true
}

function dongGoiY() {
  showSuggest.value = false
}

function chonGoiY(sp) {
  showSuggest.value = false
  searchQuery.value = ''
  router.push(`/shop/san-pham/${sp.id}`)
}

function diChuyenGoiY(step) {
  const n = suggestions.value.length
  if (!n) return
  showSuggest.value = true
  activeSuggest.value = ((activeSuggest.value + 1 + step + (n + 1)) % (n + 1)) - 1
}

function handleSearch() {
  const chon = suggestions.value[activeSuggest.value]
  if (showSuggest.value && chon) {
    chonGoiY(chon)
    return
  }
  showSuggest.value = false
  if (searchQuery.value.trim()) {
    router.push({ path: '/shop/san-pham', query: { search: searchQuery.value.trim() } })
  } else {
    router.push({ path: '/shop/san-pham' })
  }
}
</script>

<template>
  <div class="client-app">
    <!-- Thanh điều hướng một hàng: menu trái, tìm kiếm + giỏ hàng + đăng nhập phải -->
    <div class="vnb-nav-wrapper">
      <nav class="vnb-header-bar">
        <div class="vnb-container d-flex align-items-center gap-4 flex-nowrap">
          <RouterLink to="/shop" class="d-flex align-items-center text-decoration-none flex-shrink-0">
            <img src="/logo.png" alt="FootStyle" class="vnb-logo" />
          </RouterLink>

          <ul class="vnb-nav-list flex-grow-1">
            <li class="vnb-nav-item">
              <RouterLink to="/shop" class="vnb-nav-link">TRANG CHỦ</RouterLink>
            </li>
            <li class="vnb-nav-item">
              <RouterLink to="/shop/san-pham" class="vnb-nav-link">SẢN PHẨM</RouterLink>
            </li>
            <li class="vnb-nav-item">
              <RouterLink to="/shop/uu-dai" class="vnb-nav-link">ƯU ĐÃI</RouterLink>
            </li>
            <li class="vnb-nav-item">
              <RouterLink :to="{ path: '/shop/tai-khoan', query: { tab: 'orders' } }" class="vnb-nav-link vnb-no-active">ĐƠN HÀNG</RouterLink>
            </li>
            <li class="vnb-nav-item">
              <RouterLink to="/shop/tra-cuu" class="vnb-nav-link">TRA CỨU</RouterLink>
            </li>
            <li class="vnb-nav-item">
              <RouterLink to="/shop/lien-he" class="vnb-nav-link">LIÊN HỆ</RouterLink>
            </li>
          </ul>

          <div class="vnb-search-wrap flex-shrink-0">
            <form @submit.prevent="handleSearch">
              <input
                type="text"
                v-model="searchQuery"
                class="form-control vnb-search-input"
                placeholder="Tìm kiếm sản phẩm..."
                autocomplete="off"
                @focus="moGoiY"
                @blur="dongGoiY"
                @keydown.down.prevent="diChuyenGoiY(1)"
                @keydown.up.prevent="diChuyenGoiY(-1)"
                @keydown.esc="dongGoiY"
              />
              <button type="submit" class="vnb-search-icon-btn" aria-label="Tìm kiếm">
                <i class="bi bi-search"></i>
              </button>
            </form>

            <!-- Gợi ý sản phẩm khi gõ -->
            <div v-if="showSuggest && searchQuery.trim()" class="vnb-suggest">
              <template v-if="suggestions.length">
                <div class="vnb-suggest-head">Sản phẩm gợi ý</div>
                <a
                  v-for="(sp, idx) in suggestions"
                  :key="sp.id"
                  :href="`/shop/san-pham/${sp.id}`"
                  class="vnb-suggest-item"
                  :class="{ active: idx === activeSuggest }"
                  @mousedown.prevent="chonGoiY(sp)"
                  @click.prevent
                  @mouseenter="activeSuggest = idx"
                >
                  <img v-if="sp.anhChinh" :src="sp.anhChinh" :alt="sp.ten" class="vnb-suggest-img" />
                  <span v-else class="vnb-suggest-img vnb-suggest-noimg"><i class="bi bi-image"></i></span>
                  <span class="vnb-suggest-info">
                    <span class="vnb-suggest-name">{{ sp.ten }}</span>
                    <span class="vnb-suggest-meta">{{ [sp.brandName, sp.tenDanhMuc].filter(Boolean).join(' · ') }}</span>
                  </span>
                  <span v-if="sp.minPrice > 0" class="vnb-suggest-price">{{ formatMoney(sp.minPrice) }}</span>
                </a>
                <a href="#" class="vnb-suggest-all" @mousedown.prevent="activeSuggest = -1; handleSearch()" @click.prevent>
                  Xem tất cả kết quả cho "{{ searchQuery.trim() }}" <i class="bi bi-arrow-right"></i>
                </a>
              </template>
              <div v-else class="vnb-suggest-empty">
                <i class="bi bi-search"></i> Không tìm thấy sản phẩm phù hợp
              </div>
            </div>          </div>

          <!-- Giỏ hàng -->
          <div
            class="vnb-lookup-dropdown-wrap flex-shrink-0"
            @mouseenter="showCartDropdown = true"
            @mouseleave="showCartDropdown = false"
          >
            <RouterLink to="/shop/gio-hang" class="vnb-cart-link" title="Xem giỏ hàng">
              <i class="bi bi-cart3"></i>
              <span class="vnb-cart-badge">{{ cartStore.totalCount }}</span>
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

          <!-- Đăng nhập / tài khoản -->
          <div
            class="vnb-lookup-dropdown-wrap flex-shrink-0"
            @mouseenter="showAccountDropdown = true"
            @mouseleave="showAccountDropdown = false"
          >
            <RouterLink
              :to="auth.daDangNhap.value ? '/shop/tai-khoan' : '/shop/dang-nhap'"
              class="vnb-login-pill"
              title="Tài khoản cá nhân"
            >
              <span class="vnb-login-avatar">
                <template v-if="auth.daDangNhap.value">{{ (auth.trangThai.nguoiDung?.hoTen || 'T').trim().charAt(0).toUpperCase() }}</template>
                <i v-else class="bi bi-person-fill"></i>
              </span>
              <span class="vnb-login-text">{{ auth.daDangNhap.value ? (auth.trangThai.nguoiDung?.hoTen || 'TÀI KHOẢN') : 'ĐĂNG NHẬP' }}</span>
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
        </div>
      </nav>
    </div>

    <!-- Main Content -->
    <main class="flex-grow-1 pb-5">
      <RouterView />
    </main>

    <!-- Footer -->
    <footer class="vnb-footer text-white pt-5 pb-4 mt-auto">
      <div class="vnb-container">
        <div class="row g-4 mb-4">
          <div class="col-lg-4 col-md-6">
            <img src="/logo.png" alt="FootStyle" class="vnb-footer-logo mb-3" />
            <p class="small vnb-footer-text mb-3">
              FootStyle - Hệ thống shop cầu lông toàn quốc chuyên phân phối vợt cầu lông chính hãng.
            </p>
            <div class="small vnb-footer-text">
              <p class="mb-1"><i class="bi bi-geo-alt-fill vnb-footer-icon me-2"></i> Trụ sở: 123 Đường Cầu Diễn, Bắc Từ Liêm, Hà Nội</p>
              <p class="mb-1">
                <i class="bi bi-telephone-fill vnb-footer-icon me-2"></i> Hotline: 
                <a href="tel:0977508430" class="text-white text-decoration-none fw-bold">0977508430</a> (8:00 - 21:30)
              </p>
              <p class="mb-0"><i class="bi bi-envelope-fill vnb-footer-icon me-2"></i> Email: cskh@footstyle.vn</p>
            </div>
          </div>

          <div class="col-lg-2 col-md-6 col-6">
            <h6 class="vnb-footer-title text-uppercase fw-bold mb-3">Chính sách</h6>
            <ul class="list-unstyled small d-flex flex-column gap-2 vnb-footer-text">
              <li>
                <RouterLink :to="{ path: '/shop/huong-dan', query: { tab: 'bao-hanh' } }" class="vnb-footer-link text-decoration-none">
                  Chính sách bảo hành
                </RouterLink>
              </li>
              <li>
                <RouterLink :to="{ path: '/shop/huong-dan', query: { tab: 'bao-hanh' } }" class="vnb-footer-link text-decoration-none">
                  Đổi trả trong 7 ngày
                </RouterLink>
              </li>
              <li>
                <RouterLink :to="{ path: '/shop/huong-dan', query: { tab: 'mua-hang' } }" class="vnb-footer-link text-decoration-none">
                  Chính sách giao hàng
                </RouterLink>
              </li>
              <li>
                <RouterLink :to="{ path: '/shop/huong-dan', query: { tab: 'mua-hang' } }" class="vnb-footer-link text-decoration-none">
                  Bảo mật thông tin
                </RouterLink>
              </li>
            </ul>
          </div>

          <div class="col-lg-3 col-md-6 col-6">
            <h6 class="vnb-footer-title text-uppercase fw-bold mb-3">Hỗ trợ khách hàng</h6>
            <ul class="list-unstyled small d-flex flex-column gap-2 vnb-footer-text">
              <li><RouterLink to="/shop/tra-cuu" class="vnb-footer-link text-decoration-none">Tra cứu đơn hàng</RouterLink></li>
              <li>
                <RouterLink :to="{ path: '/shop/huong-dan', query: { tab: 'chon-vot' } }" class="vnb-footer-link text-decoration-none">
                  Hướng dẫn chọn vợt
                </RouterLink>
              </li>

              <li>
                <RouterLink to="/shop/huong-dan" class="vnb-footer-link text-decoration-none">
                  Câu hỏi thường gặp
                </RouterLink>
              </li>
            </ul>
          </div>

          <div class="col-lg-3 col-md-6">
            <h6 class="vnb-footer-title text-uppercase fw-bold mb-3">Thanh toán & Vận chuyển</h6>
            <div class="d-flex flex-wrap gap-2 mb-3">
              <span class="badge vnb-footer-badge p-2">COD Tiền mặt</span>
              <span class="badge vnb-footer-badge p-2">VietQR Banking</span>
            </div>
            <div class="small vnb-footer-text mb-3">
              Vận chuyển hỏa tốc toàn quốc qua GHTK, GHN, ViettelPost.
            </div>
            <div>
              <RouterLink to="/shop/nhuong-quyen" class="btn btn-outline-light btn-sm rounded-pill fw-bold">
                Hợp tác nhượng quyền →
              </RouterLink>
            </div>
          </div>
        </div>

        <div class="border-top border-secondary pt-3 text-center small vnb-footer-text">
          © 2026 FootStyle Badminton Shop. Mọi quyền được bảo lưu.
        </div>
      </div>
    </footer>

    <!-- Nút chat nổi (trợ lý ảo) ở góc dưới bên phải -->
    <NutChatKhach />
  </div>
</template>

<style scoped>
/* Chân trang: cùng tông xanh với trang chủ (trước là nền đen chữ vàng, logo bị ép thành ô trắng) */
.vnb-footer {
  background: #0f2a5c;
  border-top: 4px solid var(--vnb-orange);
}
.vnb-footer-logo {
  height: 46px;
  padding: 6px 10px;
  background: #fff;
  border-radius: 10px;
}
.vnb-footer-title {
  color: #fff;
  letter-spacing: 0.02em;
}
.vnb-footer-text {
  color: #cbd5e1;
}
.vnb-footer-icon {
  color: #93c5fd;
}
.vnb-footer-link {
  color: #cbd5e1;
}
.vnb-footer-link:hover {
  color: #fff;
  text-decoration: underline !important;
}
.vnb-footer-badge {
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
  font-weight: 600;
}
.vnb-footer-cuoi {
  border-top: 1px solid rgba(255, 255, 255, 0.16);
}

.fw-black {
  font-weight: 900;
}
</style>
