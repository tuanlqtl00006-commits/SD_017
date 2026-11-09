<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { cuaHangService } from '../../services/cuaHangService'
import { useCartStore } from '../../stores/cart'
import { useAuth } from '../../composables/useAuth'
import { formatMoney } from '../../utils/format'
import { useAutoRefresh } from '../../composables/useAutoRefresh'

const router = useRouter()
const cartStore = useCartStore()
const auth = useAuth()

const loading = ref(true)
const products = ref([])
const variants = ref([])
const discounts = ref({}) // { idBienThe: phanTram }

// 1. DỮ LIỆU BANNER SLIDER (Tone xanh dương đồng điệu)
const currentBannerIndex = ref(0)
const bannerList = [
  {
    tag: 'NEW ARRIVAL 2026',
    title: 'NEW ECLIPSION Z3',
    subtitle: 'CONFIDENCE. FROM EVERY DIRECTION. TO EVERY DIRECTION.',
    desc: 'Công nghệ Power Cushion+ độc quyền hỗ trợ bật nhảy tối đa, giảm chấn thương hoàn hảo cho từng bước di chuyển trên sân cầu.',
    btnText: 'KHÁM PHÁ NGAY',
    btnLink: '/shop/san-pham',
    bgGradient: 'linear-gradient(135deg, #0c4a6e 0%, #0284c7 50%, #38bdf8 100%)',
    badgeClass: 'bg-warning text-dark',
  },
  {
    tag: 'SIÊU PHẨM TẤN CÔNG',
    title: 'ASTROX 100ZZ KURENAI',
    subtitle: 'HYPER SLIM SHAFT - SMASH HỦY DIỆT SÂN CẦU',
    desc: 'Vũ khí tối thượng của số 1 thế giới Viktor Axelsen. Điểm cân bằng nặng đầu, đũa siêu mỏng tạo ra những cú đập cầu cắm sân không thể cản phá.',
    btnText: 'MUA NGAY - TẶNG CƯỚC BG65',
    btnLink: '/shop/san-pham?search=Astrox',
    bgGradient: 'linear-gradient(135deg, #1e3a8a 0%, #1d4ed8 50%, #60a5fa 100%)',
    badgeClass: 'bg-warning text-dark',
  },
  {
    tag: 'BỘ SƯU TẬP FOOTSTYLE PRO',
    title: 'SPEED & POWER 2026',
    subtitle: 'TRANG PHỤC & PHỤ KIỆN CHÍNH HÃNG TIÊU CHUẨN THI ĐẤU',
    desc: 'Chất liệu vải Cool-Dry thoáng khí kháng khuẩn, giày công nghệ chống lật cổ chân và các phụ kiện hỗ trợ tối đa cho lông thủ.',
    btnText: 'XEM BỘ SƯU TẬP',
    btnLink: '/shop/san-pham?khuyenMai=1',
    bgGradient: 'linear-gradient(135deg, #0369a1 0%, #0284c7 50%, #7dd3fc 100%)',
    badgeClass: 'bg-warning text-dark',
  },
]

let bannerTimer = null
function nextBanner() {
  currentBannerIndex.value = (currentBannerIndex.value + 1) % bannerList.length
}
function prevBanner() {
  currentBannerIndex.value =
    (currentBannerIndex.value - 1 + bannerList.length) % bannerList.length
}
function setBanner(index) {
  currentBannerIndex.value = index
}

// 2. TOAST THÔNG BÁO THÊM GIỎ HÀNG
const toastVisible = ref(false)
const toastText = ref('')
let toastTimer = null

function showNotification(msg) {
  toastText.value = msg
  toastVisible.value = true
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => {
    toastVisible.value = false
  }, 3000)
}

// 3. DANH MỤC TABS Ở "SẢN PHẨM MỚI" (Dữ liệu thật từ CSDL)
const homeCategories = ref([
  { id: 'ALL', ten: 'Tất cả' },
  { id: 1, ten: 'Vợt Cầu Lông' },
  { id: 2, ten: 'Vợt Cầu Lông Người Mới' },
  { id: 3, ten: 'Quần Áo Thể Thao' },
  { id: 4, ten: 'Túi - Balo' },
  { id: 5, ten: 'Phụ Kiện' },
  { id: 6, ten: 'Quả Cầu Lông' },
])
const selectedCatTab = ref('ALL')
const currentSlide = ref(0)

// 4. BEST SELLERS LIST (Top 4 bán chạy)
const bestSellerList = computed(() => {
  return enrichedProducts.value.slice(0, 4).map((p, idx) => ({
    rank: idx + 1,
    id: p.id,
    ten: p.ten,
    gia: p.minPrice,
    giaGoc: p.originalPrice || p.minPrice,
    daBan: `${100 + idx * 50}+`,
    danhGia: '5.0',
    anh: p.anhChinh || '/logo.png',
  }))
})

// 5. THƯƠNG HIỆU ĐỐI TÁC
const brandLogos = [
  { name: 'YONEX', slogan: 'Far beyond ordinary' },
  { name: 'LINING', slogan: 'Anything is possible' },
  { name: 'VICTOR', slogan: 'Ready to win' },
  { name: 'MIZUNO', slogan: 'Reach beyond' },
  { name: 'KUMPOO', slogan: 'Speed & Power' },
  { name: 'KAWASAKI', slogan: 'Pro Badminton' },
]

onMounted(async () => {
  bannerTimer = setInterval(nextBanner, 5000)

  try {
    loading.value = true
    const [spRes, btRes, dggRes, dmThRes] = await Promise.all([
      cuaHangService.getDanhSachSanPham().catch(() => []),
      Promise.resolve([]),
      Promise.resolve({}),
      cuaHangService.getDanhMucVaThuongHieu().catch(() => null),
    ])

    products.value = spRes || []
    variants.value = btRes || []
    discounts.value = dggRes || {}

    if (dmThRes?.danhMuc?.length > 0) {
      homeCategories.value = [
        { id: 'ALL', ten: 'Tất cả' },
        ...dmThRes.danhMuc.map((dm) => ({ id: dm.id, ten: dm.ten })),
      ]
    }
  } catch (err) {
    console.error('Lỗi khi tải trang chủ:', err)
  } finally {
    loading.value = false
  }
})

// Giá / tồn kho / sản phẩm mới cập nhật ngầm
async function taiNgam() {
  if (loading.value) return
  const ds = await cuaHangService.getDanhSachSanPham()
  if (Array.isArray(ds) && ds.length) products.value = ds
}
useAutoRefresh(taiNgam, 3000, { chuKyMs: 30_000, chuDe: ['san-pham', 'ton-kho', 'giam-gia'] })

onUnmounted(() => {
  if (bannerTimer) clearInterval(bannerTimer)
  if (toastTimer) clearTimeout(toastTimer)
})

// Tính giá và khuyến mãi từ dữ liệu thật
const enrichedProducts = computed(() => {
  if (!products.value || products.value.length === 0) return []

  return products.value
    .filter((p) => p.hoatDong !== 0 && p.hoatDong !== false)
    .map((sp) => {
      let minPrice = sp.minPrice
      let originalPrice = sp.originalPrice
      let maxDiscountPercent = sp.maxDiscountPercent || 0

      if (minPrice === undefined || minPrice === null || minPrice === 0) {
        const spVariants = (sp.bienThe && sp.bienThe.length > 0)
          ? sp.bienThe
          : variants.value.filter(
              (v) => v.idSanPham === sp.id && v.hoatDong !== 0 && v.hoatDong !== false
            )

        if (spVariants.length > 0) {
          let min = Infinity
          let orig = Infinity
          for (const v of spVariants) {
            const giaGoc = Number(v.giaBan || v.giaBanGoc) || 0
            const ptGiam = Number(discounts.value[v.id]) || Number(v.phanTramGiam) || 0
            const giaSauGiam = ptGiam > 0 ? giaGoc * (1 - ptGiam / 100) : giaGoc

            if (giaSauGiam > 0 && giaSauGiam < min) {
              min = giaSauGiam
              orig = giaGoc
            }
            if (ptGiam > maxDiscountPercent) {
              maxDiscountPercent = ptGiam
            }
          }
          minPrice = min === Infinity ? 0 : min
          originalPrice = orig === Infinity ? 0 : orig
        } else {
          minPrice = 0
          originalPrice = 0
        }
      }

      const ten = sp.ten || sp.tenSanPham || 'Sản phẩm'

      return {
        ...sp,
        id: sp.id,
        ten,
        ma: sp.ma || sp.maSanPham || '',
        anhChinh: sp.anhChinh || (sp.danhSachAnh && sp.danhSachAnh[0]) || '/logo.png',
        minPrice: Number(minPrice) || 0,
        originalPrice: Number(originalPrice) || 0,
        maxDiscountPercent,
        hasDiscount: maxDiscountPercent > 0,
        rating: 5.0,
        soldCount: sp.tongSoLuongTon || 0,
        isNew: true,
        idDanhMuc: sp.idDanhMuc,
        tenDanhMuc: sp.tenDanhMuc || '',
      }
    })
})

// Lọc sản phẩm theo tab danh mục
const tabFilteredProducts = computed(() => {
  if (selectedCatTab.value === 'ALL') return enrichedProducts.value
  return enrichedProducts.value.filter((p) => {
    if (p.idDanhMuc != null && String(p.idDanhMuc) === String(selectedCatTab.value)) return true
    if (String(selectedCatTab.value) === '1' && (p.idDanhMuc === 1 || p.ten?.toLowerCase().includes('vợt'))) return true
    if (String(selectedCatTab.value) === '2' && (p.idDanhMuc === 2 || p.ten?.toLowerCase().includes('mới'))) return true
    if (String(selectedCatTab.value) === '3' && (p.idDanhMuc === 3 || p.ten?.toLowerCase().includes('áo') || p.ten?.toLowerCase().includes('quần'))) return true
    if (String(selectedCatTab.value) === '4' && (p.idDanhMuc === 4 || p.ten?.toLowerCase().includes('túi') || p.ten?.toLowerCase().includes('balo'))) return true
    if (String(selectedCatTab.value) === '5' && (p.idDanhMuc === 5 || p.ten?.toLowerCase().includes('phụ kiện') || p.ten?.toLowerCase().includes('cước'))) return true
    if (String(selectedCatTab.value) === '6' && (p.idDanhMuc === 6 || p.ten?.toLowerCase().includes('cầu'))) return true
    return false
  })
})

// Sản phẩm nổi bật (trái) và danh sách phụ (phải)
const spmFeatured = computed(() => {
  const list = tabFilteredProducts.value
  if (list.length === 0) return null
  return list[currentSlide.value] || list[0]
})

const spmSideList = computed(() => {
  const list = tabFilteredProducts.value
  if (list.length === 0) return []
  return list.slice(0, 5)
})

function selectFeaturedProduct(product) {
  const idx = tabFilteredProducts.value.findIndex((p) => p.id === product.id)
  if (idx !== -1) {
    currentSlide.value = idx
  }
}

function prevSlide() {
  if (tabFilteredProducts.value.length === 0) return
  if (currentSlide.value > 0) {
    currentSlide.value--
  } else {
    currentSlide.value = Math.max(0, tabFilteredProducts.value.length - 1)
  }
}

function nextSlide() {
  if (tabFilteredProducts.value.length === 0) return
  if (currentSlide.value < tabFilteredProducts.value.length - 1) {
    currentSlide.value++
  } else {
    currentSlide.value = 0
  }
}

function goToDetail(id) {
  if (!id) return
  router.push(`/shop/san-pham/${id}`)
}

function scrollToTop() {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// Thêm nhanh vào giỏ hàng
function quickAddToCart(item, event) {
  if (event) event.stopPropagation()
  if (!item) return

  const bt = (item.bienThe || [])
    .filter((b) => Number(b.soLuongTon) > 0)
    .sort((a, b) => Number(a.giaBan) - Number(b.giaBan))[0]
  if (!bt) {
    goToDetail(item.id)
    return
  }

  cartStore.addItem({
    sanPhamId: item.id,
    sanPhamTen: item.ten,
    sanPhamMa: item.ma || '',
    bienTheId: bt.id,
    bienTheMa: bt.maSpct || '',
    mauSac: bt.tenMau || '',
    trongLuong: bt.trongLuong || '',
    chuVi: bt.chuVi || '',
    giaGoc: Number(bt.giaBanGoc) || Number(bt.giaBan) || 0,
    giaBan: Number(bt.giaBan) || 0,
    anh: item.anhChinh || '/logo.png',
    soLuong: 1,
    soLuongTon: Number(bt.soLuongTon),
  })

  showNotification(`Đã thêm "${item.ten}" vào giỏ hàng!`)
}
</script>

<template>
  <div class="home-view">
    <!-- TOAST NOTIFICATION NỔI -->
    <transition name="fade">
      <div v-if="toastVisible" class="vnb-floating-toast shadow-lg">
        <div class="d-flex align-items-center gap-2">
          <i class="bi bi-check-circle-fill text-success fs-5"></i>
          <div>
            <div class="fw-bold text-dark small">{{ toastText }}</div>
            <RouterLink to="/shop/gio-hang" class="text-primary text-decoration-none fw-semibold" style="font-size: 0.75rem;">
              Xem giỏ hàng & thanh toán →
            </RouterLink>
          </div>
        </div>
      </div>
    </transition>

    <!-- 1. HERO SLIDER BANNER (Tone Xanh Đậm Đà Đồng Bộ) -->
    <section class="vnb-hero-banner mb-4">
      <div
        class="position-relative overflow-hidden transition-bg"
        :style="{ background: bannerList[currentBannerIndex].bgGradient, minHeight: '400px' }"
      >
        <div class="vnb-container py-5">
          <div class="row align-items-center g-4">
            <div class="col-lg-7 text-white">
              <span
                class="badge px-3 py-1 fw-bold rounded-pill mb-3"
                :class="bannerList[currentBannerIndex].badgeClass"
              >
                {{ bannerList[currentBannerIndex].tag }}
              </span>
              <h1 class="display-4 fw-black text-uppercase fst-italic mb-2" style="font-weight: 900; letter-spacing: -1px;">
                {{ bannerList[currentBannerIndex].title }}
              </h1>
              <h5 class="fw-bold text-uppercase text-light mb-3 opacity-90">
                {{ bannerList[currentBannerIndex].subtitle }}
              </h5>
              <p class="lead opacity-90 small mb-4" style="max-width: 620px;">
                {{ bannerList[currentBannerIndex].desc }}
              </p>
              <div class="d-flex gap-3 flex-wrap">
                <RouterLink
                  :to="bannerList[currentBannerIndex].btnLink"
                  class="btn btn-warning btn-lg fw-bold px-4 py-2 rounded-pill text-dark shadow"
                >
                  {{ bannerList[currentBannerIndex].btnText }} <i class="bi bi-arrow-right ms-1"></i>
                </RouterLink>
                <RouterLink
                  to="/shop/san-pham"
                  class="btn btn-outline-light btn-lg fw-bold px-4 py-2 rounded-pill shadow-sm"
                >
                  XEM TẤT CẢ SẢN PHẨM
                </RouterLink>
              </div>
            </div>
            <div class="col-lg-5 text-center">
              <div class="hero-image-wrapper p-3">
                <img
                  src="/logo.png"
                  alt="Banner FootStyle"
                  class="img-fluid rounded-4 shadow-lg p-3 bg-white hero-banner-img"
                  style="max-height: 270px; object-fit: contain;"
                />
              </div>
            </div>
          </div>
        </div>

        <!-- Mũi tên chuyển Banner -->
        <button
          type="button"
          class="banner-nav-btn banner-prev"
          @click="prevBanner"
          aria-label="Banner trước"
        >
          <i class="bi bi-chevron-left"></i>
        </button>
        <button
          type="button"
          class="banner-nav-btn banner-next"
          @click="nextBanner"
          aria-label="Banner sau"
        >
          <i class="bi bi-chevron-right"></i>
        </button>

        <!-- Dots chỉ báo vị trí Slide -->
        <div class="banner-dots">
          <span
            v-for="(banner, idx) in bannerList"
            :key="idx"
            class="banner-dot"
            :class="{ active: currentBannerIndex === idx }"
            @click="setBanner(idx)"
          ></span>
        </div>
      </div>
    </section>

    <div class="vnb-container vnb-main-content">
      <!-- 2. SẢN PHẨM BÁN CHẠY NHẤT (Top 4 - Giữ nguyên nút ban đầu & nền xanh dương nhạt) -->
      <section class="vnb-section mb-4">
        <div class="vnb-section-header d-flex flex-wrap align-items-center justify-content-between gap-3">
          <div class="d-flex align-items-center gap-3">
            <div class="vnb-header-icon-box">
              <i class="bi bi-fire text-danger"></i>
            </div>
            <div>
              <div class="d-flex align-items-center gap-2">
                <h2 class="vnb-header-title mb-0">Sản Phẩm Bán Chạy Nhất</h2>
                <span class="badge bg-danger rounded-pill fw-bold px-2 py-1 small">HOT 🔥</span>
              </div>
              <p class="vnb-header-subtitle mb-0">Được khách hàng tin dùng & đánh giá cao nhất</p>
            </div>
          </div>
          <RouterLink to="/shop/san-pham" class="vnb-view-all-link">
            Xem tất cả <i class="bi bi-arrow-right ms-1"></i>
          </RouterLink>
        </div>

        <!-- 4 Card Bán Chạy Đều Đặn, Thanh Lịch -->
        <div class="row g-3">
          <div
            v-for="item in bestSellerList"
            :key="item.id"
            class="col-xl-3 col-md-6"
          >
            <div class="bestseller-card h-100">
              <!-- Rank badge góc trên trái -->
              <div
                class="rank-badge"
                :class="{
                  'rank-1': item.rank === 1,
                  'rank-2': item.rank === 2,
                  'rank-3': item.rank === 3,
                  'rank-other': item.rank > 3,
                }"
              >
                TOP {{ item.rank }}
              </div>

              <div class="text-center pt-2">
                <div class="bestseller-img-wrap mb-2" @click="goToDetail(item.id)">
                  <img :src="item.anh" :alt="item.ten" class="bestseller-img" />
                </div>
                <div class="bestseller-sold-badge mb-2">
                  <i class="bi bi-bag-check-fill me-1"></i>
                  <span>{{ item.daBan }} lượt mua</span>
                </div>
                <h6 class="fw-bold text-truncate-2 small mb-1 bestseller-name" :title="item.ten" @click="goToDetail(item.id)">
                  {{ item.ten }}
                </h6>
                <div class="text-warning small mb-2 d-flex align-items-center justify-content-center gap-1">
                  <i v-for="s in 5" :key="s" class="bi bi-star-fill"></i>
                  <span class="text-secondary fw-semibold ms-1">5.0</span>
                </div>
                <div class="mb-3 d-flex align-items-baseline justify-content-center gap-2">
                  <span class="fw-bold text-danger fs-6">{{ formatMoney(item.gia) }}</span>
                  <span v-if="item.giaGoc && item.giaGoc !== item.gia" class="text-muted text-decoration-line-through small">{{ formatMoney(item.giaGoc) }}</span>
                </div>
              </div>

              <!-- Hai nút nguyên bản: Mua ngay (xanh dương đậm) + Xem chi tiết (mắt) -->
              <div class="d-flex gap-2 mt-auto">
                <button
                  type="button"
                  class="btn btn-primary btn-sm flex-grow-1 rounded-pill fw-bold d-flex align-items-center justify-content-center gap-1 shadow-sm"
                  @click="goToDetail(item.id)"
                >
                  <i class="bi bi-cart-plus"></i> Mua ngay
                </button>
                <button
                  type="button"
                  class="btn btn-outline-secondary btn-sm rounded-pill px-3"
                  @click="goToDetail(item.id)"
                  title="Xem chi tiết"
                >
                  <i class="bi bi-eye"></i>
                </button>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 3. PHẦN SẢN PHẨM MỚI (Split Layout Tương Tác Hài Hòa) -->
      <section class="vnb-section mb-4">
        <div class="vnb-section-header d-flex flex-wrap align-items-center justify-content-between gap-3">
          <div class="d-flex align-items-center gap-3">
            <div class="vnb-header-icon-box">
              <i class="bi bi-stars text-primary"></i>
            </div>
            <div>
              <div class="d-flex align-items-center gap-2">
                <h2 class="vnb-header-title mb-0">Khám Phá Sản Phẩm Mới</h2>
                <span class="badge bg-primary rounded-pill fw-bold px-2 py-1 small">✦ MỚI</span>
              </div>
              <p class="vnb-header-subtitle mb-0">Hàng mới về — cập nhật liên tục mỗi ngày</p>
            </div>
          </div>

          <!-- Tab danh mục thật từ CSDL -->
          <div class="spm-tabs-wrap">
            <button
              v-for="cat in homeCategories"
              :key="cat.id"
              class="spm-tab"
              :class="{ active: String(selectedCatTab) === String(cat.id) }"
              @click="selectedCatTab = cat.id; currentSlide = 0"
            >
              {{ cat.ten }}
            </button>
          </div>
        </div>

        <!-- Loading -->
        <div v-if="loading" class="text-center py-5">
          <div class="spinner-border text-primary" role="status"></div>
          <p class="small text-muted mt-2">Đang tải sản phẩm mới...</p>
        </div>

        <!-- Empty -->
        <div v-else-if="tabFilteredProducts.length === 0" class="text-center py-5 text-muted small">
          Không có sản phẩm nào trong danh mục này.
        </div>

        <!-- Split layout -->
        <div v-else class="spm-split-layout">
          <!-- LEFT: Featured product -->
          <div class="spm-featured">
            <!-- Badge NEW + discount -->
            <div class="spm-featured-badges">
              <span class="badge bg-primary rounded-pill px-3 py-1 fw-bold shadow-sm">✦ MỚI RA MẮT</span>
              <span v-if="spmFeatured?.hasDiscount" class="badge bg-danger rounded-pill px-2 py-1 fw-bold shadow-sm">
                -{{ spmFeatured?.maxDiscountPercent }}%
              </span>
            </div>

            <!-- Image -->
            <div class="spm-featured-img-wrap" @click="goToDetail(spmFeatured?.id)">
              <img
                :src="spmFeatured?.anhChinh || '/logo.png'"
                :alt="spmFeatured?.ten"
                class="spm-featured-img"
              />
            </div>

            <!-- Info -->
            <div class="spm-featured-info">
              <div v-if="spmFeatured?.brandName" class="spm-featured-brand">
                {{ spmFeatured?.brandName }}
              </div>
              <h4 class="spm-featured-name" @click="goToDetail(spmFeatured?.id)">
                {{ spmFeatured?.ten }}
              </h4>

              <div class="spm-featured-stars mb-2">
                <i v-for="s in 5" :key="s" class="bi bi-star-fill text-warning"></i>
                <span class="ms-1 text-muted small">({{ spmFeatured?.soldCount || 100 }}+ đã bán)</span>
              </div>

              <div class="spm-featured-price-row mb-3">
                <span class="spm-featured-price text-danger fw-bold fs-4">{{ formatMoney(spmFeatured?.minPrice) }}</span>
                <span v-if="spmFeatured?.originalPrice > spmFeatured?.minPrice" class="text-muted text-decoration-line-through ms-2 small">
                  {{ formatMoney(spmFeatured?.originalPrice) }}
                </span>
              </div>

              <!-- Hai nút nguyên bản -->
              <div class="spm-featured-actions d-flex gap-2">
                <button
                  type="button"
                  class="btn btn-primary rounded-pill fw-bold px-4 py-2 shadow-sm d-flex align-items-center gap-1"
                  @click="quickAddToCart(spmFeatured, $event)"
                >
                  <i class="bi bi-cart-plus"></i> Thêm vào giỏ
                </button>
                <button
                  type="button"
                  class="btn btn-outline-secondary rounded-pill px-3 py-2 fw-semibold d-flex align-items-center gap-1"
                  @click="goToDetail(spmFeatured?.id)"
                >
                  <i class="bi bi-eye"></i> Xem chi tiết
                </button>
              </div>
            </div>

            <!-- Dots navigation -->
            <div class="spm-dots">
              <span
                v-for="(item, idx) in tabFilteredProducts.slice(0, Math.min(tabFilteredProducts.length, 6))"
                :key="item.id"
                class="spm-dot"
                :class="{ active: idx === currentSlide }"
                @click.stop="currentSlide = idx"
              ></span>
            </div>

            <!-- Prev / Next arrows -->
            <button class="spm-arrow spm-arrow-left" @click.stop="prevSlide" aria-label="Trước">
              <i class="bi bi-chevron-left"></i>
            </button>
            <button class="spm-arrow spm-arrow-right" @click.stop="nextSlide" aria-label="Sau">
              <i class="bi bi-chevron-right"></i>
            </button>
          </div>

          <!-- RIGHT: Interactive Side list -->
          <div class="spm-side-list">
            <div
              v-for="item in spmSideList"
              :key="item.id"
              class="spm-side-item"
              :class="{ active: spmFeatured?.id === item.id }"
              @click="selectFeaturedProduct(item)"
            >
              <div class="spm-side-img-wrap">
                <img :src="item.anhChinh || '/logo.png'" :alt="item.ten" class="spm-side-img" />
                <span v-if="item.hasDiscount" class="spm-side-badge">-{{ item.maxDiscountPercent }}%</span>
              </div>
              <div class="spm-side-info">
                <div class="spm-side-name">{{ item.ten }}</div>
                <div class="spm-side-price">{{ formatMoney(item.minPrice) }}</div>
              </div>
              <i class="bi bi-chevron-right spm-side-arrow"></i>
            </div>
          </div>
        </div>
      </section>

      <!-- 4. PHẦN SALE OFF (3 Banner khuyến mãi hàng ngang) -->
      <section class="vnb-section mb-4">
        <div class="vnb-section-header d-flex flex-wrap align-items-center justify-content-between gap-3">
          <div class="d-flex align-items-center gap-3">
            <div class="vnb-header-icon-box">
              <i class="bi bi-tag-fill text-warning"></i>
            </div>
            <div>
              <div class="d-flex align-items-center gap-2">
                <h2 class="vnb-header-title mb-0">Chương Trình Khuyến Mãi</h2>
                <span class="badge bg-warning text-dark rounded-pill fw-bold px-2 py-1 small">ƯU ĐÃI</span>
              </div>
              <p class="vnb-header-subtitle mb-0">Săn deal giá tốt cùng quà tặng chính hãng kèm theo</p>
            </div>
          </div>
          <RouterLink to="/shop/san-pham?khuyenMai=1" class="vnb-view-all-link">
            Xem ưu đãi <i class="bi bi-arrow-right ms-1"></i>
          </RouterLink>
        </div>

        <div class="row g-3">
          <div class="col-md-4">
            <div
              class="promo-banner-card rounded-4 p-4 text-white text-center shadow-sm cursor-pointer position-relative overflow-hidden"
              style="background: linear-gradient(135deg, #1e3a8a 0%, #2563eb 100%); min-height: 150px; display: flex; flex-direction: column; justify-content: center; align-items: center;"
              @click="router.push('/shop/san-pham?khuyenMai=1')"
            >
              <span class="badge bg-warning text-dark fw-bold px-2 py-1 mb-2" style="font-size: 0.75rem;">SALE OFF</span>
              <h5 class="fw-black text-uppercase mb-1" style="font-weight: 900;">VỢT CẦU LÔNG CHÍNH HÃNG</h5>
              <div class="text-warning fw-bold small">GIẢM TỪ 20% ĐẾN 50% - TẶNG CƯỚC BG65</div>
            </div>
          </div>

          <div class="col-md-4">
            <div
              class="promo-banner-card rounded-4 p-4 text-white text-center shadow-sm cursor-pointer position-relative overflow-hidden"
              style="background: linear-gradient(135deg, #0f172a 0%, #dc2626 100%); min-height: 150px; display: flex; flex-direction: column; justify-content: center; align-items: center;"
              @click="router.push('/shop/san-pham?khuyenMai=1')"
            >
              <span class="badge bg-white text-danger fw-bold px-2 py-1 mb-2" style="font-size: 0.75rem;">DISCOUNT</span>
              <h5 class="fw-black text-uppercase mb-1" style="font-weight: 900;">GIÀY CẦU LÔNG CAO CẤP</h5>
              <div class="text-white opacity-90 fw-bold small">GIẢM SỐC CÔNG NGHỆ POWER CUSHION+</div>
            </div>
          </div>

          <div class="col-md-4">
            <div
              class="promo-banner-card rounded-4 p-4 text-dark text-center shadow-sm cursor-pointer position-relative overflow-hidden"
              style="background: linear-gradient(135deg, #fef3c7 0%, #fed7aa 100%); min-height: 150px; display: flex; flex-direction: column; justify-content: center; align-items: center;"
              @click="router.push('/shop/san-pham?khuyenMai=1')"
            >
              <span class="badge bg-danger text-white fw-bold px-2 py-1 mb-2" style="font-size: 0.75rem;">COMBO HOT</span>
              <h5 class="fw-black text-uppercase mb-1 text-danger" style="font-weight: 900;">QUẦN ÁO THỂ THAO</h5>
              <div class="text-dark fw-bold small">MUA 2 ÁO TẶNG 1 QUẤN CÁN FOOTSTYLE</div>
            </div>
          </div>
        </div>
      </section>

      <!-- 5. PHẦN DANH MỤC CẦU LÔNG (4 Card danh mục) -->
      <section class="vnb-section mb-4">
        <div class="vnb-section-header d-flex flex-wrap align-items-center justify-content-between gap-3">
          <div class="d-flex align-items-center gap-3">
            <div class="vnb-header-icon-box">
              <i class="bi bi-grid-fill text-primary"></i>
            </div>
            <div>
              <h2 class="vnb-header-title mb-0">Danh Mục Nổi Bật</h2>
              <p class="vnb-header-subtitle mb-0">Trang thiết bị & phụ kiện thi đấu chuyên nghiệp</p>
            </div>
          </div>
          <RouterLink to="/shop/san-pham" class="vnb-view-all-link">
            Tất cả danh mục <i class="bi bi-arrow-right ms-1"></i>
          </RouterLink>
        </div>

        <div class="row g-4 justify-content-center">
          <!-- Card 1: VỢT CẦU LÔNG -->
          <div class="col-lg-3 col-6">
            <div class="vnb-cat-card dm-card" @click="router.push('/shop/san-pham')">
              <img src="/images/san-pham/sp13_lining_halbertec_9000.webp" alt="Vợt cầu lông" class="dm-img dm-img-vot" loading="lazy" />
              <div class="vnb-cat-ribbon dm-ribbon">VỢT CẦU LÔNG</div>
              <div class="dm-sub">Yonex, Lining, Victor</div>
            </div>
          </div>

          <!-- Card 2: GIÀY CẦU LÔNG -->
          <div class="col-lg-3 col-6">
            <div class="vnb-cat-card dm-card" @click="router.push('/shop/san-pham')">
              <img src="/images/danh-muc/giay_yonex_power_cushion_88_dial.webp" alt="Giày cầu lông" class="dm-img" loading="lazy" />
              <div class="vnb-cat-ribbon dm-ribbon">GIÀY CẦU LÔNG</div>
              <div class="dm-sub">Bật nhảy, giảm chấn</div>
            </div>
          </div>

          <!-- Card 3: PHỤ KIỆN & CƯỚC -->
          <div class="col-lg-3 col-6">
            <div class="vnb-cat-card dm-card" @click="router.push('/shop/san-pham')">
              <img src="/images/san-pham/sp42_tui_yonex_ba92431wz.webp" alt="Phụ kiện và cước" class="dm-img" loading="lazy" />
              <div class="vnb-cat-ribbon dm-ribbon">PHỤ KIỆN & CƯỚC</div>
              <div class="dm-sub">Cước đan, túi, quấn cán</div>
            </div>
          </div>
        </div>
      </section>

      <!-- 6. THƯƠNG HIỆU HÀNG ĐẦU (Brand Partners) -->
      <section class="vnb-section mb-4">
        <div class="vnb-section-header text-center pb-2">
          <div class="text-center w-100">
            <div class="vnb-header-icon-box mx-auto mb-2">
              <i class="bi bi-patch-check-fill text-primary"></i>
            </div>
            <h2 class="vnb-header-title mb-1">Thương Hiệu Đồng Hành</h2>
            <p class="vnb-header-subtitle mb-0">Đại lý ủy quyền phân phối thiết bị cầu lông chính hãng từ các thương hiệu hàng đầu</p>
          </div>
        </div>

        <div class="row g-3 justify-content-center align-items-center text-center">
          <div
            v-for="brand in brandLogos"
            :key="brand.name"
            class="col-lg-2 col-md-4 col-6"
          >
            <div
              class="brand-badge-box p-3 rounded-3 cursor-pointer"
              @click="router.push(`/shop/san-pham?search=${brand.name}`)"
            >
              <h5 class="fw-black text-dark m-0 brand-text" style="font-weight: 900; letter-spacing: 1px;">
                {{ brand.name }}
              </h5>
              <small class="text-muted" style="font-size: 0.7rem;">{{ brand.slogan }}</small>
            </div>
          </div>
        </div>
      </section>
    </div>

    <!-- Nút Cuộn Lên Đầu Trang -->
    <button
      type="button"
      class="vnb-back-to-top"
      @click="scrollToTop"
      aria-label="Cuộn lên đầu trang"
    >
      <i class="bi bi-chevron-up"></i>
    </button>
  </div>
</template>

<style scoped>
/* ═══════════════════════════════════════════════════════════════════════
   LIGHT BLUE STOREFRONT DESIGN SYSTEM
   - Màu chủ đạo: Xanh dương nhạt (#f0f7ff, #e0f2fe, #eff6ff, #bfdbfe)
   - Nút hành động giữ nguyên màu ban đầu:
     + btn-primary: Xanh dương chuẩn (#1d4ed8)
     + btn-warning: Vàng (#fbbf24)
     + btn-outline-secondary: Viền xám thanh lịch
═══════════════════════════════════════════════════════════════════════ */

/* Nền toàn trang nhẹ nhàng mát mắt */
.home-view {
  background: #f0f7ff;
  min-height: 100vh;
}

.vnb-main-content {
  background: transparent;
  padding-top: 1rem;
  padding-bottom: 2.5rem;
}

/* KHUNG CHUNG CHO MỌI SECTION — Thẻ trắng viền xanh nhạt đồng bộ tuyệt đối */
.vnb-section {
  background: #ffffff;
  border-radius: 20px;
  border: 1.5px solid #e0f2fe;
  box-shadow: 0 4px 20px rgba(14, 165, 233, 0.05);
  padding: 1.75rem 2rem;
}

/* HEADER CHUNG CHO CẢ TRANG — Nhất quán từ đầu đến cuối */
.vnb-section-header {
  border-bottom: 1.5px solid #f0f7ff;
  padding-bottom: 0.85rem;
  margin-bottom: 1.5rem;
}

.vnb-header-icon-box {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: #eff6ff;
  border: 1.5px solid #dbeafe;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.25rem;
  color: #1d4ed8;
  flex-shrink: 0;
}

.vnb-header-title {
  font-size: 1.35rem;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: -0.2px;
}

.vnb-header-subtitle {
  font-size: 0.85rem;
  color: #64748b;
  font-weight: 500;
}

.vnb-view-all-link {
  display: inline-flex;
  align-items: center;
  padding: 6px 18px;
  border-radius: 999px;
  background: #eff6ff;
  border: 1.5px solid #bfdbfe;
  color: #1d4ed8;
  font-weight: 700;
  font-size: 0.85rem;
  text-decoration: none;
  transition: all 0.2s;
  white-space: nowrap;
}

.vnb-view-all-link:hover {
  background: #1d4ed8;
  border-color: #1d4ed8;
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(29, 78, 216, 0.25);
  transform: translateY(-1px);
}

.cursor-pointer {
  cursor: pointer;
}

.transition-bg {
  transition: background 0.6s ease-in-out;
}

/* ═══════════════════════════════════════════════
   HERO BANNER
═══════════════════════════════════════════════ */
.banner-nav-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.25);
  color: white;
  border: 1px solid rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.25rem;
  cursor: pointer;
  transition: all 0.2s;
  z-index: 10;
}

.banner-nav-btn:hover {
  background: white;
  color: #1d4ed8;
}

.banner-prev { left: 16px; }
.banner-next { right: 16px; }

.banner-dots {
  position: absolute;
  bottom: 16px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 8px;
  z-index: 10;
}

.banner-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.4);
  cursor: pointer;
  transition: all 0.25s;
}

.banner-dot.active {
  background: #fbbf24;
  width: 28px;
  border-radius: 9999px;
}

/* ═══════════════════════════════════════════════
   BEST SELLERS (Sản phẩm bán chạy nhất)
═══════════════════════════════════════════════ */
.bestseller-card {
  background: #ffffff;
  border: 1.5px solid #e0f2fe;
  border-radius: 18px;
  padding: 1.15rem;
  position: relative;
  transition: all 0.25s ease;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.03);
}

.bestseller-card:hover {
  transform: translateY(-4px);
  border-color: #93c5fa;
  box-shadow: 0 12px 28px rgba(29, 78, 216, 0.12);
}

.rank-badge {
  position: absolute;
  top: 10px;
  left: 10px;
  font-size: 0.72rem;
  font-weight: 800;
  padding: 3px 9px;
  border-radius: 6px;
  color: white;
  z-index: 2;
  letter-spacing: 0.3px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
}

.rank-1 { background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%); }
.rank-2 { background: linear-gradient(135deg, #94a3b8 0%, #64748b 100%); }
.rank-3 { background: linear-gradient(135deg, #b45309 0%, #78350f 100%); }
.rank-other { background-color: #1d4ed8; }

.bestseller-img-wrap {
  height: 165px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px;
  cursor: pointer;
}

.bestseller-img {
  max-height: 100%;
  max-width: 100%;
  object-fit: contain;
  transition: transform 0.3s;
}

.bestseller-card:hover .bestseller-img {
  transform: scale(1.06);
}

.bestseller-sold-badge {
  display: inline-flex;
  align-items: center;
  font-size: 0.72rem;
  font-weight: 600;
  color: #1d4ed8;
  background: #eff6ff;
  padding: 2px 8px;
  border-radius: 999px;
  border: 1px solid #dbeafe;
}

.bestseller-name {
  font-size: 0.92rem;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.35;
  height: 2.5rem;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  cursor: pointer;
}

.bestseller-name:hover {
  color: #1d4ed8;
}

/* ═══════════════════════════════════════════════
   SẢN PHẨM MỚI (Split Layout)
═══════════════════════════════════════════════ */
.spm-tabs-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.spm-tab {
  border: 1.5px solid #bfdbfe;
  border-radius: 999px;
  padding: 6px 16px;
  background: #ffffff;
  color: #1d4ed8;
  font-weight: 600;
  font-size: 0.82rem;
  cursor: pointer;
  transition: all 0.2s;
}

.spm-tab:hover {
  border-color: #1d4ed8;
  background: #eff6ff;
}

.spm-tab.active {
  background: #1d4ed8;
  border-color: #1d4ed8;
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(29, 78, 216, 0.25);
}

.spm-split-layout {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 20px;
  min-height: 440px;
}

@media (max-width: 991px) {
  .spm-split-layout {
    grid-template-columns: 1fr;
  }
  .spm-side-list {
    display: grid !important;
    grid-template-columns: 1fr 1fr;
    gap: 10px;
  }
}

/* Featured Card (Trái) */
.spm-featured {
  position: relative;
  border-radius: 18px;
  overflow: hidden;
  background: #ffffff;
  border: 1.5px solid #e0f2fe;
  display: flex;
  flex-direction: column;
  box-shadow: 0 4px 16px rgba(14, 165, 233, 0.05);
  transition: all 0.25s ease;
}

.spm-featured:hover {
  border-color: #93c5fd;
  box-shadow: 0 8px 24px rgba(29, 78, 216, 0.1);
}

.spm-featured-badges {
  position: absolute;
  top: 14px;
  left: 14px;
  display: flex;
  gap: 6px;
  z-index: 3;
}

.spm-featured-img-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 24px 20px;
  background: radial-gradient(ellipse at 50% 50%, #ffffff 0%, #f0f7ff 60%, #e0f2fe 100%);
  cursor: pointer;
}

.spm-featured-img {
  max-height: 230px;
  max-width: 100%;
  object-fit: contain;
  transition: transform 0.4s ease;
}

.spm-featured:hover .spm-featured-img {
  transform: scale(1.05);
}

.spm-featured-info {
  background: #ffffff;
  padding: 16px 20px 48px;
  position: relative;
  border-top: 1px solid #f0f7ff;
}

.spm-featured-brand {
  font-size: 0.75rem;
  font-weight: 800;
  color: #1d4ed8;
  text-transform: uppercase;
  letter-spacing: 0.8px;
  margin-bottom: 4px;
}

.spm-featured-name {
  font-size: 1.15rem;
  font-weight: 800;
  color: #0f172a;
  margin-bottom: 6px;
  cursor: pointer;
}

.spm-featured-name:hover {
  color: #1d4ed8;
}

.spm-dots {
  position: absolute;
  bottom: 14px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 6px;
  z-index: 4;
}

.spm-dot {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: #bae6fd;
  cursor: pointer;
  transition: all 0.25s;
}

.spm-dot.active {
  background: #1d4ed8;
  width: 22px;
}

.spm-arrow {
  position: absolute;
  top: 42%;
  transform: translateY(-50%);
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 1.5px solid #bfdbfe;
  background: #fff;
  color: #1d4ed8;
  font-size: 1rem;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 4;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(29, 78, 216, 0.12);
}

.spm-arrow:hover {
  background: #1d4ed8;
  border-color: #1d4ed8;
  color: #fff;
}

.spm-arrow-left { left: 10px; }
.spm-arrow-right { right: 10px; }

/* Side list (Phải) */
.spm-side-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.spm-side-item {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #ffffff;
  border-radius: 14px;
  padding: 10px 14px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1.5px solid #e0f2fe;
  position: relative;
}

.spm-side-item:hover {
  border-color: #93c5fd;
  background: #f8fbff;
  transform: translateX(3px);
}

.spm-side-item.active {
  background: #eff6ff;
  border-color: #1d4ed8;
  box-shadow: 0 4px 12px rgba(29, 78, 216, 0.12);
}

.spm-side-img-wrap {
  position: relative;
  flex-shrink: 0;
  width: 60px;
  height: 60px;
  border-radius: 10px;
  background: #f0f7ff;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 4px;
}

.spm-side-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.spm-side-badge {
  position: absolute;
  top: 2px;
  right: 2px;
  background: #dc2626;
  color: #fff;
  font-size: 0.6rem;
  font-weight: 800;
  padding: 1px 5px;
  border-radius: 4px;
}

.spm-side-info {
  flex: 1;
  min-width: 0;
}

.spm-side-name {
  font-size: 0.85rem;
  font-weight: 700;
  color: #0f172a;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.35;
  margin-bottom: 4px;
}

.spm-side-price {
  font-size: 0.9rem;
  font-weight: 800;
  color: #dc2626;
}

.spm-side-arrow {
  color: #93c5fd;
  font-size: 0.8rem;
  flex-shrink: 0;
  transition: color 0.2s;
}

.spm-side-item:hover .spm-side-arrow,
.spm-side-item.active .spm-side-arrow {
  color: #1d4ed8;
}

/* ═══════════════════════════════════════════════
   DANH MỤC NỔI BẬT
═══════════════════════════════════════════════ */
.dm-card {
  background: radial-gradient(circle at 50% 38%, #ffffff 0%, #f0f9ff 55%, #e0f2fe 100%);
  border: 1.5px solid #bae6fd;
}

.dm-img {
  position: absolute;
  top: 6%;
  left: 12%;
  width: 76%;
  height: 58%;
  object-fit: contain;
  mix-blend-mode: multiply;
  transition: transform 0.4s;
}

.dm-img-vot {
  transform: rotate(-28deg);
}

.dm-card:hover .dm-img {
  transform: scale(1.08);
}

.dm-card:hover .dm-img-vot {
  transform: rotate(-28deg) scale(1.08);
}

.dm-ribbon {
  top: 64%;
  background: linear-gradient(135deg, #1d4ed8 0%, #38bdf8 100%);
  color: #fff;
}

.dm-sub {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 7%;
  text-align: center;
  font-size: 0.85rem;
  font-weight: 600;
  color: #1d4ed8;
}

/* ═══════════════════════════════════════════════
   THƯƠNG HIỆU & TIN TỨC & TIỆN ÍCH
═══════════════════════════════════════════════ */
.brand-badge-box {
  background: #f0f7ff;
  border: 1.5px solid #e0f2fe;
  border-radius: 14px;
  transition: all 0.2s;
}

.brand-badge-box:hover {
  background: #e0f2fe;
  border-color: #93c5fd;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(29, 78, 216, 0.1);
}

.brand-text {
  transition: color 0.2s;
}

.brand-badge-box:hover .brand-text {
  color: #1d4ed8 !important;
}

.vnb-floating-toast {
  position: fixed;
  top: 24px;
  right: 24px;
  background: #ffffff;
  border-left: 5px solid #16a34a;
  border-radius: 10px;
  padding: 14px 20px;
  z-index: 1050;
  min-width: 280px;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s, transform 0.3s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>
